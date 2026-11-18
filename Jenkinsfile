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
        stage('Build Docker') {
            steps {
                echo 'Building Docker image...'
                bat '''
                    docker build ^
                    -t cloudorder/product-service:%BUILD_NUMBER% ^
                    -t cloudorder/product-service:latest ^
                    ./services/product-service
                '''
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying Product Service with Docker Compose...'
                bat '''
                    docker-compose -f infrastructure/docker/compose.yaml up -d --no-deps --force-recreate product-service
                '''
            }
        }
        stage('Health Check') {
            steps {
                echo 'Performing health check...'
                powershell '''
                    $url = "http://localhost:8082/actuator/health"
                    $maxAttempts = 20
                    $healthy = $false

                    for ($i = 1; $i -le $maxAttempts; $i++) {
                        Write-Host "Healthcheck attempt $i of $maxAttempts"

                        try {
                            $response = Invoke-RestMethod -Uri $url -TimeoutSec 5

                            if ($response.status -eq "UP") {
                                Write-Host "Product Service is healthy!"
                                $healthy = $true
                                break
                            }

                            Write-Host "Health status: $($response.status)"
                        }
                        catch {
                            Write-Host "Product Service not ready yet."
                        }

                        Start-Sleep -Seconds 5
                    }

                    if (-not $healthy) {
                        Write-Error "Healthcheck failed: Product Service did not become healthy."
                        exit 1
                    }
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        success {
            archiveArtifacts(artifacts: 'services/product-service/target/*.jar', fingerprint: true)
            echo 'CI pipeline completed successfully!'
        }
        failure {
            echo 'CI pipeline failed!'
        }
    }
}