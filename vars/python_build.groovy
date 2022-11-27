def call(service, dockerRepoName, imageName) {
    pipeline {
        agent any
        stages {
            stage('build') {
                steps {
                    sh "pip install -r requirements.txt"
                }
            }
            stage('Python Lint') {
                steps {
                    sh "pylint-fail-under --fail_under 5.0 ${service}/app.py"
                }
            }

            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/master' }
                }
                steps {
                    withCredentials([string(credentialsId: 'AdelDockerhub', variable: 'TOKEN')]) {
                        sh "docker login -u 'adelkuanysheva' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag adelkuanysheva/${dockerRepoName}:latest ."
                        sh "docker push adelkuanysheva/${dockerRepoName}:latest"
                    }
                }
            }

            stage('Zip Artifacts') {
                steps {
                    sh 'zip app.zip *.py'
                }
                post {
                    always {
                        archiveArtifacts artifacts: 'app.zip'
                    }
                }
            }

            stage('Deploy') {
                steps {
                    sshagent(credentials : ['AdelVM3855']) {
                        sh "ssh -o StrictHostKeyChecking=no azureuser@kafka-acit3855.westus3.cloudapp.azure.com 'cd acit3855/deployment/; docker-compose down; docker image rm adelkuanysheva/${dockerRepoName}:latest;'"
                        sh "ssh -o StrictHostKeyChecking=no azureuser@kafka-acit3855.westus3.cloudapp.azure.com 'docker pull adelkuanysheva/${dockerRepoName}:latest;'"
                        sh "ssh -o StrictHostKeyChecking=no azureuser@kafka-acit3855.westus3.cloudapp.azure.com 'cd acit3855/deployment/; docker-compose up -d'"
                    }
                }
            }

        }
    }
}
