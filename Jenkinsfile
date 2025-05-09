pipeline {
  agent {
    docker {
      image 'docker:dind'
      args '--privileged'
    }
  }

  environment {
    DOCKER_BUILDKIT = '1'
    PATH = "/usr/local/bin:/usr/bin:/bin:${PATH}"

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

    stage('Run Tests') {
      steps {
        dir('backend') {
          sh 'mvn test'
        }
      }
    }

    stage('Build Jar') {
      steps {
        dir('backend') {
          sh 'mvn clean package -DskipTests'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        dir('backend') {
          sh """
            docker build \
              --progress=plain \
              -t kei077/cosmo-backend:${BUILD_NUMBER} .
            docker tag \
              kei077/cosmo-backend:${BUILD_NUMBER} \
              kei077/cosmo-backend:latest
          """
        }
      }
    }

    stage('Push Docker Image') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'dockerhub-cred',
          usernameVariable: 'DOCKER_USER',
          passwordVariable: 'DOCKER_PASS'
        )]) {
          sh '''
            echo "$DOCKER_PASS" | docker login \
              --username "$DOCKER_USER" --password-stdin
            docker push kei077/cosmo-backend:${BUILD_NUMBER}
            docker push kei077/cosmo-backend:latest
          '''
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('SonarQube') {
          withCredentials([string(
            credentialsId: 'SONAR_TOKEN',
            variable: 'SONAR_TOKEN'
          )]) {
            dir('backend') {
              sh """
                mvn sonar:sonar \
                  -Dsonar.projectKey=cosmo-backend \
                  -Dsonar.host.url=http://192.168.240.198:9000 \
                  -Dsonar.login=$SONAR_TOKEN
              """
            }
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
  }

  post {
    always {
      echo "Cleaning up Docker containers..."
      sh 'docker-compose down --remove-orphans || true'
    }
    failure {
      echo "Pipeline failed. Please check the logs above."
    }
  }
}
