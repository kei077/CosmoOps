pipeline {
    agent any

    environment {
        DOCKER_BUILDKIT = '1'
    }

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

        stage('Build Docker Images') {
            steps {
                echo "Building Docker images..."
                sh 'docker-compose build'
            }
        }

        stage('SonarQube Analysis') {
            tools {
                maven 'Maven 3'
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                        sh """
                            cd backend && \
                            mvn sonar:sonar \
                            -Dsonar.projectKey=cosmo-backend \
                            -Dsonar.host.url=http://192.168.240.198:9000 \
                            -Dsonar.login=$SONAR_TOKEN
                        """
                    }
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

        stage('Run Tests') {
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
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