pipeline {
    agent any 

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out...'
                checkout scm
            }
        }
        stage('Verify Environment') {
            steps {
                echo 'Verifying environment...'
                bat 'java --version'
                bat 'mvn --version'
                bat 'docker info'
            }
        }
        stage('Compile') {
            steps {
                echo 'Compiling...'
                bat 'mvn -B -ntp clean compile'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
                bat 'mvn -B -ntp test'
            }
        }
        stage('Package') {
            steps {
                echo 'Packaging...'
                bat 'mvn -B -ntp -DskipTests package'
            }
        }

        post {
            always {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            }
            success {
                ArchiveArtifacts(artifacts: 'services/product-service/target/*.jar', fingerprint: true)
                echo 'CI pipeline completed successfully!'
            }
            failure {
                echo 'CI pipeline failed!'
            }
        }
    }
}