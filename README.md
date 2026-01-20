# 🔧 Jenkins Shared Libraries

[![Jenkins](https://img.shields.io/badge/Jenkins-2.400+-D24939?logo=jenkins&logoColor=white)](https://www.jenkins.io/)
[![Groovy](https://img.shields.io/badge/Groovy-4.0+-4298B8?logo=apache-groovy&logoColor=white)](https://groovy-lang.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive collection of reusable Jenkins pipeline libraries for modern CI/CD workflows, featuring container builds, security scanning, deployments, and multi-language support.

## ✨ Features

### 🏗️ Pipeline Templates
- **BackendPipeline** - Complete CI/CD workflow for backend applications
- **FrontendPipeline** - Frontend application pipeline with modern tooling
- **PythonPipeline** - Python-specific build and deployment pipeline

### 🐳 Container & Artifact Management
- **KanikoBuildPush** - Kaniko-based Docker image builds without Docker daemon
- **CraneArtifactPromotion** - Efficient container image promotion between registries
- **HarborSecurityScan** - Harbor registry security vulnerability scanning

### 🚀 Deployment Automation
- **DeployDev** - Automated development environment deployments
- **DeployStg** - Staging environment deployment pipeline
- **DeployPro** - Production deployment with safety checks

### 🔍 Code Quality & Testing
- **SonarqubeScan** - Code quality analysis and static code scanning
- **PythonUnitTest** - Python test execution with coverage reporting
- **NodeLintFormatTest** - Node.js linting, formatting, and testing
- **InfraTestK8s** - Kubernetes infrastructure testing

### 📦 Version Control & Tagging
- **CreateTag** - Git tag creation and semantic versioning
- **EmailNotification** - Pipeline status notifications via email

## 📋 Prerequisites

- **Jenkins**: v2.400+ with Pipeline plugin
- **Kubernetes Cluster**: v1.24+ (for deployments)
- **Docker Registry**: Harbor, Docker Hub, or compatible registry
- **SonarQube**: v9.0+ (for code quality scanning)
- **Git**: v2.30+

### Required Jenkins Plugins

```groovy
- workflow-aggregator (Pipeline)
- kubernetes (Kubernetes Plugin)
- git
- credentials-binding
- email-ext
- sonar
```

## 🚀 Quick Start

### 1. Configure Jenkins Shared Library

In Jenkins, go to **Manage Jenkins** → **Configure System** → **Global Pipeline Libraries**:

```groovy
Name: jenkins-shared-libraries
Default version: main
Retrieval method: Modern SCM
  - Git
  - Project Repository: https://github.com/yourusername/jenkins-shared-libraries.git
  - Credentials: <your-git-credentials>
```

Check:
- ✅ Load implicitly
- ✅ Allow default version to be overridden

### 2. Use in Your Jenkinsfile

```groovy
@Library('jenkins-shared-libraries@main') _

// Backend Pipeline Example
BackendPipeline {
    gitRepo = 'https://github.com/yourorg/backend-app.git'
    dockerRegistry = 'harbor.yourdomain.com'
    sonarUrl = 'https://sonarqube.yourdomain.com'
    deployNamespace = 'backend-dev'
}
```

### 3. Frontend Application Example

```groovy
@Library('jenkins-shared-libraries@main') _

FrontendPipeline {
    gitRepo = 'https://github.com/yourorg/frontend-app.git'
    nodeVersion = '18'
    buildCommand = 'npm run build'
    dockerRegistry = 'harbor.yourdomain.com'
    deployEnv = 'staging'
}
```

### 4. Python Application Example

```groovy
@Library('jenkins-shared-libraries@main') _

pythonPipeline {
    gitRepo = 'https://github.com/yourorg/python-api.git'
    pythonVersion = '3.11'
    requirementsFile = 'requirements.txt'
    testCommand = 'pytest'
}
```

## 📁 Library Structure

```
vars/
├── BackendPipeline.groovy          # Complete backend CI/CD pipeline
├── FrontendPipeline.groovy         # Frontend application pipeline
├── pythonPipeline.groovy           # Python application pipeline
├── KanikoBuildPush.groovy          # Container image building
├── CraneArtifactPromotion.groovy   # Image promotion between registries
├── HarborSecurityScan.groovy       # Security vulnerability scanning
├── SonarqubeScan.groovy            # Code quality analysis
├── DeployDev.groovy                # Development deployment
├── DeployStg.groovy                # Staging deployment
├── DeployPro.groovy                # Production deployment
├── PythonUnitTest.groovy           # Python testing utilities
├── NodeLintFormatTest.groovy       # Node.js quality checks
├── InfraTestK8s.groovy             # Kubernetes testing
├── CreateTag.groovy                # Git tagging automation
└── EmailNotification.groovy        # Email notification utilities
```

## 🔧 Pipeline Components

### BackendPipeline

Complete CI/CD pipeline for backend applications with build, test, scan, and deploy stages.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

BackendPipeline {
    gitRepo = 'https://github.com/yourorg/backend-app.git'
    gitBranch = 'main'
    dockerRegistry = 'harbor.yourdomain.com'
    dockerImage = 'backend-app'
    sonarUrl = 'https://sonarqube.yourdomain.com'
    sonarProjectKey = 'backend-app'
    deployNamespace = 'production'
    healthCheckUrl = 'https://api.yourdomain.com/health'
}
```

**Features:**
- ✅ Git checkout and branch management
- ✅ Multi-stage Docker builds with Kaniko
- ✅ SonarQube code quality gates
- ✅ Harbor security scanning
- ✅ Kubernetes deployment
- ✅ Health check verification
- ✅ Email notifications

### FrontendPipeline

Optimized pipeline for modern frontend frameworks (React, Vue, Angular, etc.).

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

FrontendPipeline {
    gitRepo = 'https://github.com/yourorg/frontend-app.git'
    nodeVersion = '18'
    packageManager = 'npm'  // or 'yarn', 'pnpm'
    buildCommand = 'npm run build'
    testCommand = 'npm run test:ci'
    lintCommand = 'npm run lint'
    dockerRegistry = 'harbor.yourdomain.com'
    deployEnv = 'production'
}
```

**Features:**
- ✅ Node.js version management
- ✅ Dependency installation and caching
- ✅ Linting and code formatting
- ✅ Unit and integration tests
- ✅ Production build optimization
- ✅ Static asset deployment
- ✅ CDN integration support

### KanikoBuildPush

Build and push container images using Kaniko (no Docker daemon required).

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:latest
"""
        }
    }
    stages {
        stage('Build & Push') {
            steps {
                KanikoBuildPush(
                    dockerfile: 'Dockerfile',
                    context: '.',
                    registry: 'harbor.yourdomain.com',
                    image: 'myapp',
                    tag: "${env.BUILD_NUMBER}",
                    credentials: 'harbor-credentials'
                )
            }
        }
    }
}
```

**Features:**
- ✅ Daemonless Docker builds
- ✅ Multi-platform support
- ✅ Layer caching
- ✅ Build argument injection
- ✅ Registry authentication

### HarborSecurityScan

Automated security vulnerability scanning using Harbor registry.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

HarborSecurityScan {
    harborUrl = 'https://harbor.yourdomain.com'
    projectName = 'production'
    imageName = 'backend-app'
    imageTag = "${env.BUILD_NUMBER}"
    credentials = 'harbor-admin'
    severityThreshold = 'HIGH'  // CRITICAL, HIGH, MEDIUM, LOW
    failOnVulnerabilities = true
}
```

**Features:**
- ✅ CVE vulnerability detection
- ✅ Severity-based filtering
- ✅ Quality gate enforcement
- ✅ Detailed vulnerability reports
- ✅ Policy compliance checking

### SonarqubeScan

Code quality analysis and security scanning with SonarQube.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

SonarqubeScan {
    sonarUrl = 'https://sonarqube.yourdomain.com'
    projectKey = 'my-project'
    projectName = 'My Project'
    sources = 'src'
    exclusions = '**/test/**,**/node_modules/**'
    credentials = 'sonarqube-token'
    qualityGate = true
}
```

**Features:**
- ✅ Code coverage analysis
- ✅ Code smell detection
- ✅ Security hotspot identification
- ✅ Technical debt calculation
- ✅ Quality gate enforcement
- ✅ Multi-language support

### Deployment Pipelines

#### DeployDev
```groovy
DeployDev {
    namespace = 'development'
    appName = 'backend-api'
    imageTag = "${env.BUILD_NUMBER}"
    replicas = 1
    resources = [
        requests: [cpu: '100m', memory: '256Mi'],
        limits: [cpu: '500m', memory: '512Mi']
    ]
}
```

#### DeployStg
```groovy
DeployStg {
    namespace = 'staging'
    appName = 'backend-api'
    imageTag = "${env.BUILD_NUMBER}"
    replicas = 2
    approvalRequired = true
}
```

#### DeployPro
```groovy
DeployPro {
    namespace = 'production'
    appName = 'backend-api'
    imageTag = "${env.BUILD_NUMBER}"
    replicas = 3
    approvalRequired = true
    approvers = 'admin-team'
    rollbackOnFailure = true
    healthCheckUrl = 'https://api.yourdomain.com/health'
}
```

### PythonUnitTest

Execute Python tests with coverage reporting.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

PythonUnitTest {
    pythonVersion = '3.11'
    testFramework = 'pytest'  // or 'unittest'
    testPath = 'tests/'
    coverageThreshold = 80
    requirements = 'requirements-dev.txt'
    generateReport = true
}
```

### NodeLintFormatTest

Node.js code quality and testing utilities.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

NodeLintFormatTest {
    nodeVersion = '18'
    linter = 'eslint'  // or 'prettier', 'tslint'
    formatter = 'prettier'
    testCommand = 'npm run test:coverage'
    failOnLintError = true
}
```

### CreateTag

Automated Git tagging with semantic versioning.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

CreateTag {
    tagName = "v1.2.3"
    message = "Release version 1.2.3"
    credentials = 'git-credentials'
    pushTag = true
}
```

### EmailNotification

Pipeline status notifications.

**Usage:**
```groovy
@Library('jenkins-shared-libraries@main') _

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                // Your build steps
            }
        }
    }
    post {
        success {
            EmailNotification(
                status: 'SUCCESS',
                recipients: 'team@yourdomain.com',
                subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            )
        }
        failure {
            EmailNotification(
                status: 'FAILURE',
                recipients: 'team@yourdomain.com',
                subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            )
        }
    }
}
```

## 🔐 Required Credentials

Configure the following credentials in Jenkins:

| Credential ID | Type | Description |
|--------------|------|-------------|
| `git-credentials` | Username/Password or SSH | Git repository access |
| `harbor-credentials` | Username/Password | Harbor registry authentication |
| `sonarqube-token` | Secret text | SonarQube authentication token |
| `k8s-config` | Secret file | Kubernetes kubeconfig file |
| `email-smtp` | Username/Password | SMTP server credentials |

## 🛠️ Configuration Examples

### Complete Multi-Environment Pipeline

```groovy
@Library('jenkins-shared-libraries@main') _

pipeline {
    agent any
    
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'stg', 'pro'], description: 'Deployment environment')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }
    
    stages {
        stage('Build') {
            steps {
                KanikoBuildPush(
                    dockerfile: 'Dockerfile',
                    registry: 'harbor.yourdomain.com',
                    image: 'myapp',
                    tag: "${params.IMAGE_TAG}"
                )
            }
        }
        
        stage('Security Scan') {
            steps {
                HarborSecurityScan {
                    imageName = 'myapp'
                    imageTag = "${params.IMAGE_TAG}"
                    severityThreshold = 'HIGH'
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                SonarqubeScan {
                    projectKey = 'myapp'
                    qualityGate = true
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    switch(params.ENVIRONMENT) {
                        case 'dev':
                            DeployDev {
                                appName = 'myapp'
                                imageTag = "${params.IMAGE_TAG}"
                            }
                            break
                        case 'stg':
                            DeployStg {
                                appName = 'myapp'
                                imageTag = "${params.IMAGE_TAG}"
                            }
                            break
                        case 'pro':
                            DeployPro {
                                appName = 'myapp'
                                imageTag = "${params.IMAGE_TAG}"
                                approvalRequired = true
                            }
                            break
                    }
                }
            }
        }
        
        stage('Create Tag') {
            when {
                expression { params.ENVIRONMENT == 'pro' }
            }
            steps {
                CreateTag {
                    tagName = "release-${params.IMAGE_TAG}"
                    message = "Production release ${params.IMAGE_TAG}"
                }
            }
        }
    }
    
    post {
        success {
            EmailNotification(
                status: 'SUCCESS',
                recipients: 'devops-team@yourdomain.com'
            )
        }
        failure {
            EmailNotification(
                status: 'FAILURE',
                recipients: 'devops-team@yourdomain.com'
            )
        }
    }
}
```

## 🐛 Troubleshooting

### Kaniko Build Fails

```bash
# Check Kaniko pod logs
kubectl logs -n jenkins <kaniko-pod-name>

# Verify registry credentials
kubectl get secret harbor-credentials -n jenkins -o yaml

# Test registry connectivity
docker login harbor.yourdomain.com
```

### SonarQube Quality Gate Failure

```groovy
// Adjust quality gate in Jenkinsfile
SonarqubeScan {
    qualityGate = false  // Temporarily disable
    waitForQualityGate = true
    timeout = 10  // minutes
}
```

### Deployment Fails

```bash
# Check Kubernetes deployment
kubectl get deployments -n <namespace>
kubectl describe deployment <app-name> -n <namespace>
kubectl logs -n <namespace> -l app=<app-name>

# Verify kubeconfig
kubectl config view
kubectl cluster-info
```

### Harbor Security Scan Timeout

```groovy
HarborSecurityScan {
    scanTimeout = 600  // Increase timeout to 10 minutes
    retryCount = 3
}
```

## 🧪 Testing

### Local Development

```bash
# Validate Groovy syntax
groovy -c vars/*.groovy

# Run with Jenkins Pipeline Unit Testing Framework
./gradlew test
```

### Pipeline Validation

```groovy
// Use declarative linter
pipeline {
    agent any
    options {
        skipDefaultCheckout()
    }
    stages {
        stage('Validate') {
            steps {
                validateDeclarativePipeline('Jenkinsfile')
            }
        }
    }
}
```

## 📚 Best Practices

### 1. Version Control
```groovy
// Always specify library version
@Library('jenkins-shared-libraries@v1.2.3') _
```

### 2. Credential Management
```groovy
// Never hardcode credentials
withCredentials([string(credentialsId: 'api-token', variable: 'TOKEN')]) {
    // Use ${TOKEN}
}
```

### 3. Resource Cleanup
```groovy
// Always clean up resources
post {
    always {
        cleanWs()
    }
}
```

### 4. Error Handling
```groovy
// Implement proper error handling
try {
    KanikoBuildPush(...)
} catch (Exception e) {
    echo "Build failed: ${e.message}"
    currentBuild.result = 'FAILURE'
    throw e
}
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/new-pipeline`
3. **Test your changes locally**
4. **Commit with descriptive messages**: `git commit -m "Add Python Django pipeline"`
5. **Push to your fork**: `git push origin feature/new-pipeline`
6. **Create a Pull Request**

### Code Style

- Follow [Groovy style guidelines](https://groovy-lang.org/style-guide.html)
- Add comprehensive comments
- Include parameter documentation
- Provide usage examples

## 🤝 Acknowledgments

This library leverages the following technologies:

- [Jenkins](https://www.jenkins.io/)
- [Kubernetes](https://kubernetes.io/)
- [Kaniko](https://github.com/GoogleContainerTools/kaniko)
- [Harbor](https://goharbor.io/)
- [SonarQube](https://www.sonarqube.org/)
- [Crane](https://github.com/google/go-containerregistry/tree/main/cmd/crane)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ for DevOps Teams**
