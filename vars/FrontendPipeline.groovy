def call (body = null) {
  def settings = [:]
  if (body) {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()
  }

  pipeline {
    agent {
      kubernetes {
        yamlFile 'jenkinsPod.yaml'
      }
    }
    stages {
      stage('Lint Format Test') {
        steps {
          NodeLintFormatTest { }
        }
        when {
          anyOf {
            branch pattern: 'feature-*'
            branch pattern: 'develop'
            branch pattern: 'hotfix-*'
            branch pattern: 'release-*'
          }
        }
      }
      stage('Sonarqube Scan') {
        environment {
          SONAR_HOST_URL = 'http://sonarqube.henriqzimer.com.br'
          SONAR_TOKEN    = credentials('sonar-scanner-cli')
        }
        steps {
          SonarqubeScan { }
        }
        when {
          anyOf {
            branch pattern: 'feature-*'
            branch pattern: 'develop'
            branch pattern: 'hotfix-*'
            branch pattern: 'release-*'
          }
        }
      }
      stage('Build and Push') {
        environment {
          REGISTRY = 'harbor.henriqzimer.com.br'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          KanikoBuildPush { }
        }
        when {
          anyOf {
            branch pattern: 'develop'
            branch pattern: 'hotfix-*'
          }
        }
      }
      stage('Harbor Security Scan') {
        environment {
          HARBOR_CREDENTIALS = credentials('harbor-credentials')
          REGISTRY = 'harbor.henriqzimer.com.br'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          HarborSecurityScan { }
        }
        when {
          anyOf {
            branch pattern: 'develop'
            branch pattern: 'hotfix-*'
          }
        }
      }
      stage('Artifact Promotion') {
        environment {
          REGISTRY = 'harbor.henriqzimer.com.br'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          CraneArtifactPromotion { }
        }
        when {
          anyOf {
            branch pattern: 'release-*'
            branch pattern: 'v*'
          }
        }
      }
      stage('Infrastructure Tests on K8s') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
          GIT_URL = 'github.com'
          GIT_SSH_URL = 'git@github.com:HenriqZimer/helm-applications.git'
          NAMESPACE = 'testes-ci'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
          PORT = '3000'
          HEALTHCHECK_ENDPOINT = '/'
        }
        steps {
          InfraTestK8s { }
        }
        when {
          anyOf {
            branch pattern: 'develop'
            branch pattern: 'hotfix-*'
          }
        }
        post {
          always {
            container('helm') {
              sh '''
                helm uninstall ${PROJECT}-${PROJECT_MODE} --namespace ${NAMESPACE}
              '''
            }
          }
        }
      }
      stage('Deploy to Development') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
          GIT_URL = 'github.com'
          GIT_EMAIL = 'jenkins@henriqzimer.com.br'
          GIT_USER = 'Jenkins CI'
          GIT_SSH_URL = 'git@github.com:HenriqZimer/helm-applications.git'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          DeployDev { }
        }
        when {
          anyOf {
            branch pattern: 'develop'
          }
        }
      }
      stage('Deploy to Staging') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
          GIT_URL = 'github.com'
          GIT_EMAIL = 'jenkins@henriqzimer.com.br'
          GIT_USER = 'Jenkins CI'
          GIT_SSH_URL = 'git@github.com:HenriqZimer/helm-applications.git'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          DeployStg { }
        }
        when {
          anyOf {
            branch pattern: 'release-*'
            branch pattern: 'hotfix-*'
          }
        }
      }
      stage('Create Tag?') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
          GIT_URL = 'github.com'
          GIT_EMAIL = 'jenkins@henriqzimer.com.br'
          GIT_USER = 'Jenkins CI'
          GIT_SSH_URL = 'git@github.com:HenriqZimer/meu-site-frontend.git'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'backend'
        }
        steps {
          input message: 'Would you like to promote to production?'
          CreateTag { }
        }
        when {
          anyOf {
            branch pattern: 'release-*'
            branch pattern: 'hotfix-*'
          }
        }
      }
      stage('Deploy to Production') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
          GIT_URL = 'github.com'
          GIT_EMAIL = 'jenkins@henriqzimer.com.br'
          GIT_USER = 'Jenkins CI'
          GIT_SSH_URL = 'git@github.com:HenriqZimer/helm-applications.git'
          PROJECT = 'meu-site'
          PROJECT_MODE = 'frontend'
        }
        steps {
          input message: 'Deploy to Production?'
          DeployPro { }
        }
        when {
          anyOf {
            branch pattern: 'v*'
          }
        }
      }
    }
  }
}
