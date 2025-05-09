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

    stage('Run Tests') {
      agent {
        docker {
          image 'maven:3.9.1-openjdk-17'
          args  '-v /var/run/docker.sock:/var/run/docker.sock \
                 -v /var/jenkins_home/.m2:/root/.m2'
        }
      }
      steps {
        dir('backend') {
          sh 'mvn test'
        }
      }
    }

    stage('Build Jar') {
      agent {
        docker {
          image 'maven:3.9.1-openjdk-17'
          args  '-v /var/jenkins_home/.m2:/root/.m2'
        }
      }
      steps {
        dir('backend') {
          sh 'mvn clean package -DskipTests'
        }
      }
    }

    stage('Build Docker Image') {
      agent {
        docker {
          image 'docker:24.0.5'
          args  '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
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
      agent {
        docker {
          image 'docker:24.0.5'
          args  '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
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
      agent {
        docker {
          image 'maven:3.9.1-openjdk-17'
          args  '-v /var/jenkins_home/.m2:/root/.m2'
        }
      }
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
      agent {
        docker {
          image 'docker/compose:2.18.1'
          args  '-v /var/run/docker.sock:/var/run/docker.sock'
        }
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
      script {
        docker.image('docker/compose:2.18.1').inside(
          '-v /var/run/docker.sock:/var/run/docker.sock'
        ) {
          sh 'docker-compose down --remove-orphans || true'
        }
      }
    }
    failure {
      echo "Pipeline failed. Please check the logs above."
    }
  }
}
