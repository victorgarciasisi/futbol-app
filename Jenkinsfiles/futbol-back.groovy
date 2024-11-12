pipeline {
    agent any
	
	parameters {
        choice(name: 'ACTION', choices: ['SonarQube Analysis', 'Deploy'], description: 'Selecciona la acción a realizar')
        string(name: 'DEPLOY_IP', defaultValue: '', description: 'Introduce la IP del servidor para despliegue (Solo si seleccionas Deploy)')
    }

    environment {
        SONARQUBE_SERVER = 'sonar' // Nombre del servidor SonarQube configurado
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/victorgarciasisi/futbol-back.git', branch: 'master'
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
                        -Dsonar.projectKey=futbol-back \
                        -Dsonar.projectName=Futbol-Back \
                        -Dsonar.projectVersion=1.0 \
                        -Dsonar.sources=app,public \
                        -Dsonar.exclusions=vendor/**,system/** \
                        -Dsonar.language=php \
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
        docker stop futbol_backend
        docker rm futbol_backend
        cd /home/ubuntu/futbol-back
        git pull
        cd /home/ubuntu
        docker compose up -d backend
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
