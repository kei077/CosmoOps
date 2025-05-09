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
                FE_IMAGE_NAME = 'cosmo-frontend'         
                FE_IMAGE_TAG  = "${env.BUILD_NUMBER}"     
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
                REGISTRY      = 'docker.io'          
                REPOSITORY    = 'kei077'             
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

        stage('Frontend - push image') {
            environment {
                REGISTRY      = 'docker.io'            
                REPOSITORY    = 'kei077'               
                FE_IMAGE_NAME = 'cosmo-frontend'
                FE_IMAGE_TAG  = "${env.BUILD_NUMBER}"
                DOCKER_CREDS  = credentials('dockerhub-login')
            }
            steps {
                sh '''
                  echo ">> Logging in to registry"
                  echo "$DOCKER_CREDS_PSW" | docker login $REGISTRY \
                       -u "$DOCKER_CREDS_USR" --password-stdin

                  echo ">> Tagging and pushing frontend image"
                  docker tag  ${FE_IMAGE_NAME}:${FE_IMAGE_TAG} \
                              $REPOSITORY/${FE_IMAGE_NAME}:${FE_IMAGE_TAG}
                  docker tag  ${FE_IMAGE_NAME}:${FE_IMAGE_TAG} \
                              $REPOSITORY/${FE_IMAGE_NAME}:latest

                  docker push $REPOSITORY/${FE_IMAGE_NAME}:${FE_IMAGE_TAG}
                  docker push $REPOSITORY/${FE_IMAGE_NAME}:latest
                '''
            }
        }

        stage('Deploy & Smoke-test') {
            steps {
                sh '''
                # clean previous run
                docker compose -f docker-compose.prod.yml down --remove-orphans

                # pull fresh images & start stack
                docker compose -f docker-compose.prod.yml pull
                docker compose -f docker-compose.prod.yml up -d

                # wait (max ~1 min) for backend to report healthy
                echo ">> Waiting for backend health …"
                for i in {1..20}; do
                    if curl -fs http://localhost:8081/actuator/health | grep -q '"UP"'; then
                    echo "Backend is UP";
                    exit 0
                    fi
                    sleep 3
                done
                echo "Backend failed to become healthy";
                exit 1
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
