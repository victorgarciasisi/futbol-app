pipeline {
    agent any

	parameters {
        choice(name: 'ACTION', choices: ['SonarQube Analysis', 'Deploy'], description: 'Selecciona la acción a realizar')
        string(name: 'DEPLOY_IP', defaultValue: '', description: 'Introduce la IP del servidor para despliegue (Solo si seleccionas Deploy)')
    }
	
    environment {
        SONARQUBE_SERVER = 'sonar' // Nombre del servidor SonarQube configurado en Jenkins
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/victorgarciasisi/futbol-admindb.git', branch: 'main'
            }
        }

        stage('SonarQube Analysis') {
		    when {
                expression {
                    return params.ACTION == 'SonarQube Analysis'
                }
            }
            steps {
                withSonarQubeEnv('sonar') {
                    script {
                        def scannerHome = tool 'sonar' // Nombre del Sonar Scanner configurado
                        sh """
                        ${scannerHome}/bin/sonar-scanner \
                        -Dsonar.projectKey=futbol-admindb \
                        -Dsonar.projectName=Futbol-admindb \
                        -Dsonar.projectVersion=1.0 \
                        -Dsonar.sources=./ \
                        -Dsonar.exclusions=venv/**,tests/** \
                        -Dsonar.language=py \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.login=$SONAR_AUTH_TOKEN
                        """
                    }
                }
            }
        }
		
        stage('Deploy') {
            when {
                expression {
                    return params.ACTION == 'Deploy' && params.DEPLOY_IP?.trim()
                }
            }
steps {
    sshagent(['ubuntu']) { // Reemplaza 'ubuntu' con el ID correcto de las credenciales SSH configuradas en Jenkins
        sh """
        ssh -o StrictHostKeyChecking=no ubuntu@${params.DEPLOY_IP} << 'EOF'
        docker stop futbol_admindb
        docker rm futbol_admindb
        cd /home/ubuntu/futbol-admindb
        git pull
        cd /home/ubuntu
        docker compose up -d admindb
EOF
        """
    }
}
        }
		
    }

    post {
        always {
            echo 'Pipeline completado.'
        }
        success {
            echo 'Pipeline ejecutado con éxito.'
        }
        failure {
            echo 'Pipeline falló. Por favor, revisa los errores.'
        }
    }
}
