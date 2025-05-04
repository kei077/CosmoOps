pipeline {
    agent any

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        DOCKER_COMPOSE = './docker-compose.yml'
        IMAGE_TAG = 'latest'
    }

    stages {
        stage('Git Checkout') {
            steps {
                git branch: 'main', changelog: false, poll: false, url: 'https://github.com/kei077/CosmoOps.git'           
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker-compose build'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    sh '''${SCANNER_HOME}/bin/sonar-scanner \
                        -Dsonar.projectKey=cosmo-tracker \
                        -Dsonar.projectName=cosmo-tracker \
                        -Dsonar.sources=src \
                        -Dsonar.java.binaries=target \
                        -Dsonar.host.url=http://192.168.43.163:9000 \
                        -Dsonar.login=squ_f68c3c1d56ff4c07b513fd92e8d18970ab0a7cb9'''
                }
            }
        }

        stage('Run App') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        always {
            echo 'Cleaning up Docker containers...'
            node {
		sh 'docker-compose down'
        }
    }
}

