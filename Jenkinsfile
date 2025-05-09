pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    environment {
        PROJECT_DIR = 'backend'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Verify Structure') {
            steps { sh 'ls -la $PROJECT_DIR' }
        }

        stage('Build JAR') {
            steps {
                dir("$PROJECT_DIR") {
                    sh 'mvn -B clean package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "$PROJECT_DIR/target/*.jar", fingerprint: true
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        dir("$PROJECT_DIR") {
                            sh '''
                                mvn -B sonar:sonar \
                                  -Dsonar.projectKey=cosmo-backend \
                                  -Dsonar.login=$SONAR_TOKEN
                            '''
                        }
                    }
                }
            }
        }

        stage('Run App') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Launching app with Docker Compose…'
                sh 'docker compose up -d --build'
            }
        }
    }

    post {
        always {
            echo 'Cleaning up Docker containers…'
            sh 'docker compose down || true'
        }
        failure {
            echo 'Pipeline failed – check the stage logs above.'
        }
    }
}
