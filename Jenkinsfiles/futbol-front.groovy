pipeline {
    agent any

    parameters {
        choice(name: 'ACTION', choices: ['SonarQube Analysis', 'Deploy'], description: 'Selecciona la acción a realizar')
        string(name: 'DEPLOY_IP', defaultValue: '', description: 'Introduce la IP del servidor para despliegue (Solo si seleccionas Deploy)')
    }

    environment {
        SONARQUBE_SERVER = 'sonar' // Nombre del servidor SonarQube configurado en Jenkins
        NODE_HOME = tool 'node' // Nombre configurado en Jenkins
        PATH = "${NODE_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/victorgarciasisi/futbol-front.git', branch: 'master'
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
                        -Dsonar.projectKey=futbol-front \
                        -Dsonar.projectName=Futbol-front \
                        -Dsonar.projectVersion=1.0 \
                        -Dsonar.sources=src \
                        -Dsonar.exclusions=dist/**,node_modules/** \
                        -Dsonar.language=js \
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
        docker stop futbol_frontend
        docker rm futbol_frontend
        cd /home/ubuntu/futbol-front
        git pull
        cd /home/ubuntu
        docker compose up -d frontend
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
