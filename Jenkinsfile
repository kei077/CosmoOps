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
                    withCredentials([usernamePassword(
                            credentialsId: 'db-creds',
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASS')]) {

                        // Wrapping the whole block so we can intercept failures
                        script {
                            try {
                                sh '''
                                    set -euo pipefail
                                    echo "▶ exporting DB credentials for compose"
                                    export DATABASE_USERNAME=$DB_USER
                                    export DATABASE_PASSWORD=$DB_PASS

                                    echo "▶ Tearing down previous stack"
                                    docker compose -f docker-compose.prod.yml down --remove-orphans

                                    echo "▶ Pulling images"
                                    docker compose -f docker-compose.prod.yml pull

                                    echo "▶ Starting stack"
                                    docker compose -f docker-compose.prod.yml up -d

                                    echo "▶ Waiting for backend health check"
                                    for i in $(seq 1 20); do
                                    if curl -fs http://localhost:8081/actuator/health | grep -q '"UP"'; then
                                        echo "Backend is UP ✔ (waited $((i*3))s)"
                                        exit 0
                                    fi
                                    sleep 3
                                    done

                                    # ----- If we reach here the backend never became healthy -----
                                    echo "❌ Backend failed to become healthy in time"
                                    exit 1
                                '''
                            } catch (err) {
                                // --- 🔍 Extra debugging on failure ---
                                sh '''
                                    echo "▶ Debugging container connectivity"

                                    echo "→ docker ps -a"
                                    docker ps -a || true

                                    # Get the real container ID for the backend service
                                    BACK_ID=$(docker compose -f docker-compose.prod.yml ps -q backend || true)

                                    if [ -n "$BACK_ID" ]; then
                                    echo; echo "→ Network settings for backend ($BACK_ID)"
                                    docker inspect "$BACK_ID" --format '{{json .NetworkSettings}}' | jq .

                                    echo; echo "→ Curl from Jenkins host"
                                    curl -vv http://localhost:8081/actuator/health || true

                                    echo; echo "→ Curl from inside backend container"
                                    docker exec "$BACK_ID" curl -vv http://localhost:8081/actuator/health || true

                                    echo; echo "→ Logs (last 200 lines)"
                                    docker logs --tail 200 "$BACK_ID" || true
                                    else
                                    echo "Backend container ID could not be determined."
                                    fi
                                '''
                                throw err         // re-throw so the stage still fails
                            }
                        }
                    }
                }
            }

    }

    post {
        failure {
            echo 'Pipeline failed – check the stage logs above.'
        }
    }
}
