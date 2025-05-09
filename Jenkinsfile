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

        stage('Docker build (local)') {
            environment {
                IMAGE_NAME = 'cosmo-backend'          
                IMAGE_TAG  = "${env.BUILD_NUMBER}"  
            }
            steps {
                sh '''
                  echo ">> Docker client & server:"
                  docker version

                  echo ">> Building backend image:"
                  docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ${PROJECT_DIR}
                '''
            }
        }

        stage('Frontend Docker build (local)') {
            environment {
                FE_IMAGE_NAME = 'cosmo-frontend'          // pick any tag name
                FE_IMAGE_TAG  = "${env.BUILD_NUMBER}"     // keep in sync
            }
            steps {
                sh '''
                  echo ">> Building frontend image:"
                  docker build -t ${FE_IMAGE_NAME}:${FE_IMAGE_TAG} frontend
                '''
            }
        }

        stage('Push image to registry') {
            environment {
                REGISTRY      = 'docker.io'           // or ghcr.io
                REPOSITORY    = 'kei077'              // your account/org
                IMAGE_NAME    = 'cosmo-backend'
                IMAGE_TAG     = "${env.BUILD_NUMBER}"
                DOCKER_CREDS  = credentials('dockerhub-login')
            }
            steps {
                sh '''
                  echo ">> Logging in to registry"
                  echo "$DOCKER_CREDS_PSW" | docker login $REGISTRY \
                       -u "$DOCKER_CREDS_USR" --password-stdin

                  echo ">> Tagging and pushing"
                  docker tag  ${IMAGE_NAME}:${IMAGE_TAG} \
                              $REPOSITORY/${IMAGE_NAME}:${IMAGE_TAG}
                  docker tag  ${IMAGE_NAME}:${IMAGE_TAG} \
                              $REPOSITORY/${IMAGE_NAME}:latest

                  docker push $REPOSITORY/${IMAGE_NAME}:${IMAGE_TAG}
                  docker push $REPOSITORY/${IMAGE_NAME}:latest
                '''
            }
        }

    }

    post {
        failure {
            echo 'Pipeline failed – check the stage logs above.'
        }
    }
}
