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
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'adelkuanysheva' -p '$TOKEN' docker.io"
                        dir("${service}") {
                            sh "docker build -t ${dockerRepoName}:latest --tag adelkuanysheva/${dockerRepoName}:${imageName} ."
                        }
                        sh "docker push adelkuanysheva/${dockerRepoName}:${imageName}"
                    }
                }
            }

            stage('Scan image') {
                neuvector registrySelection: 'Local', repository: 'alpine'
            }




        }
    }
}
