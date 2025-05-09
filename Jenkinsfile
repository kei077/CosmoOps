pipeline {
    agent none

    tools {
        maven 'Maven 3'
    }

    environment {
        DOCKER_BUILDKIT = '1'
        IMAGE_NAME      = 'kei077/cosmo-backend'
        SONAR_HOST      = 'http://10.1.3.43:9000'
    }

    stages {
        stage('Checkout') {
            agent any                
            steps {
                checkout scm
            }
        }

        stage('Verify Structure') {
            agent { label 'docker' }
            steps {
                sh 'ls -la ${WORKSPACE}/backend'
            }
        }

        stage('Build Jar') {
            agent { label 'docker' }
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Docker Image') {
            agent { label 'docker' }
            steps {
                sh """
                    docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} ./backend
                    docker tag   ${IMAGE_NAME}:${BUILD_NUMBER} ${IMAGE_NAME}:latest
                """
            }
        }

        stage('Push Docker Image') {
            agent { label 'docker' }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${BUILD_NUMBER}
                        docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            agent { label 'docker' }
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([string(
                        credentialsId: 'SONAR_TOKEN',
                        variable: 'SONAR_TOKEN'
                    )]) {
                        sh """
                            cd backend && \
                            mvn sonar:sonar \
                                -Dsonar.projectKey=cosmo-backend \
                                -Dsonar.host.url=${SONAR_HOST} \
                                -Dsonar.login=$SONAR_TOKEN
                        """
                    }
                }
            }
        }

        stage('Run App') {
            agent { label 'docker' }
            steps {
                echo 'Launching app with Docker Compose…'
                sh 'docker compose up -d'
            }
        }
    }

    post {

        always {
            echo 'Cleaning up Docker containers…'
            sh 'docker compose down || true'
        }
        failure {
            echo 'Pipeline failed. Please check the logs above.'
        }
    }
}
