pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Structure') {
            steps {
                sh 'ls -la ${WORKSPACE}/backend'
            }
        }

        stage('Build Jar') {
            tools {
                maven 'Maven 3'
            }
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('SonarQube Analysis') {
            tools {
                maven 'Maven 3'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                        sh """
                            cd backend && \
                            mvn sonar:sonar \
                            -Dsonar.projectKey=cosmo-backend \
                            -Dsonar.host.url=http://10.1.3.43:9000 \
                            -Dsonar.login=squ_929a420eb1f9d80b3c38b92ddb06510a6a40e0c7
                        """
                }
            }
        }

        stage('Run App') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                echo "Launching app with Docker Compose..."
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        always {
            echo "Cleaning up Docker containers..."
            sh 'docker-compose down || true'
        }
        failure {
            echo "Pipeline failed. Please check the logs above."
        }
    }
}