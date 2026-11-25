pipeline {
    agent any 

    options {
        timestamps()
        disableConcurrentBuilds()
    }
    parameters {
        booleanParam(name: 'SIMULATE_K8S_FAILURE', defaultValue: false, description: 'Simulate Kubernetes deployment failure')
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
        stage('Verify Kubernetes') {
            steps {
                echo 'Checking Kubernetes access...'

                bat '''
                    where kubectl
                    kubectl version --client
                    kubectl config current-context
                    kubectl get nodes
                    kubectl get deployments -n cloud-order
                '''
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
        stage('Push Docker Image') {
            steps {
                echo 'Pushing image to GitHub Container Registry...'

                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-credentials',
                        usernameVariable: 'GHCR_USERNAME',
                        passwordVariable: 'GHCR_TOKEN'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        $image = "ghcr.io/tb-2000/product-service:$env:BUILD_NUMBER"

                        $configDir = Join-Path ([System.IO.Path]::GetTempPath()) `
                            ("jenkins-docker-" + [guid]::NewGuid())

                        New-Item -ItemType Directory `
                            -Path $configDir -Force | Out-Null

                        try {
                            $credentials = "$($env:GHCR_USERNAME):$($env:GHCR_TOKEN)"

                            $encoded = [Convert]::ToBase64String(
                                [Text.Encoding]::UTF8.GetBytes($credentials)
                            )

                            $config = @{
                                auths = @{
                                    "ghcr.io" = @{
                                        auth = $encoded
                                    }
                                }
                            }

                            $config | ConvertTo-Json -Depth 5 |
                                Set-Content `
                                    -Path (Join-Path $configDir "config.json") `
                                    -Encoding ASCII

                            $env:DOCKER_CONFIG = $configDir

                            Remove-Item Env:DOCKER_CONTEXT `
                                -ErrorAction SilentlyContinue

                            $env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"

                            docker tag "cloudorder/product-service:$env:BUILD_NUMBER" $image

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker tagging failed"
                            }

                            docker push $image

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker push failed"
                            }

                            Write-Host "Docker image successfully pushed"
                        }
                        finally {
                            Remove-Item -Path $configDir -Recurse -Force
                        }
                    '''
                }
            }
        }
        stage('Capture Deployment Revision') {
            steps {
                script {
                    echo 'Capturing current Kubernetes deployment...'

                    env.PREVIOUS_REVISION = powershell(
                        returnStdout: true,
                        script: '''
                            kubectl rollout status deployment/product-service `
                                -n cloud-order --timeout=180s | Out-Host

                            if ($LASTEXITCODE -ne 0) {
                                throw "Current deployment is not healthy"
                            }

                            $deployment = kubectl get deployment product-service `
                                -n cloud-order -o json | ConvertFrom-Json

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not read deployment"
                            }

                            $revision = $deployment.metadata.annotations.'deployment.kubernetes.io/revision'

                            if (-not $revision) {
                                throw "No deployment revision found"
                            }

                            Write-Output $revision
                        '''
                    ).trim()

                    env.PREVIOUS_IMAGE = powershell(
                        returnStdout: true,
                        script: '''
                            $deployment = kubectl get deployment product-service `
                                -n cloud-order -o json | ConvertFrom-Json

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not read deployment"
                            }

                            Write-Output $deployment.spec.template.spec.containers[0].image
                        '''
                    ).trim()

                    echo "Previous revision: ${env.PREVIOUS_REVISION}"
                    echo "Previous image: ${env.PREVIOUS_IMAGE}"
                }
            }
        }
        stage('Kubernetes Deployment') {
            stages {
                stage('Deploy Kubernetes') {
                    steps {
                        script {
                            env.DEPLOY_TAG = params.SIMULATE_K8S_FAILURE
                                ? "rollback-test-missing-${env.BUILD_NUMBER}"
                                : env.BUILD_NUMBER

                            echo "Deploying image tag: ${env.DEPLOY_TAG}"
                        }

                        powershell '''
                            $path = "infrastructure/kubernetes/product-service/kustomization.yaml"

                            $content = Get-Content $path -Raw

                            $updated = $content.Replace(
                                'newTag: "dev"',
                                'newTag: "' + $env:DEPLOY_TAG + '"'
                            )

                            if ($updated -eq $content) {
                                throw "Image tag placeholder not found"
                            }

                            try {
                                Set-Content -Path $path -Value $updated -Encoding utf8

                                kubectl kustomize infrastructure/kubernetes/product-service

                                if ($LASTEXITCODE -ne 0) {
                                    throw "Kustomize rendering failed"
                                }

                                kubectl apply -k infrastructure/kubernetes/product-service

                                if ($LASTEXITCODE -ne 0) {
                                    throw "Kubernetes deployment failed"
                                }
                            }
                            finally {
                                Set-Content -Path $path -Value $content -Encoding utf8
                            }
                        '''
                    }
                }

                stage('Verify Kubernetes Rollout') {
                    steps {
                        echo 'Waiting for Kubernetes rollout...'

                        powershell '''
                            kubectl rollout status deployment/product-service `
                                -n cloud-order `
                                --timeout=180s

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes rollout failed"
                            }
                        '''
                    }
                }

                stage('Verify Kubernetes Health') {
                    steps {
                        echo 'Checking Product Service health...'

                        powershell '''
                            kubectl wait --for=condition=Ready `
                                pod -l app=product-service `
                                -n cloud-order `
                                --timeout=120s

                            if ($LASTEXITCODE -ne 0) {
                                throw "Product Service pods are not ready"
                            }

                            kubectl delete job product-service-healthcheck `
                                -n cloud-order `
                                --ignore-not-found=true `
                                --wait=true

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not delete previous healthcheck job"
                            }

                            kubectl apply -f infrastructure/kubernetes/healthcheck-job.yaml

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not create healthcheck job"
                            }

                            kubectl wait --for=condition=Complete `
                                job/product-service-healthcheck `
                                -n cloud-order `
                                --timeout=120s

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes healthcheck failed"
                            }

                            kubectl logs job/product-service-healthcheck -n cloud-order

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not retrieve healthcheck logs"
                            }
                        '''
                    }
                }
            }

            post {
                failure {
                    echo 'Kubernetes deployment failed. Initiating rollback...'

                    powershell '''
                        if (-not $env:PREVIOUS_REVISION -or -not $env:PREVIOUS_IMAGE) {
                            throw "No previous deployment information available"
                        }

                        $deployment = kubectl get deployment product-service `
                            -n cloud-order -o json | ConvertFrom-Json

                        if ($LASTEXITCODE -ne 0) {
                            throw "Could not inspect current deployment"
                        }

                        $currentRevision = $deployment.metadata.annotations.'deployment.kubernetes.io/revision'
                        $currentImage = $deployment.spec.template.spec.containers[0].image

                        if ($currentRevision -eq $env:PREVIOUS_REVISION -and
                            $currentImage -eq $env:PREVIOUS_IMAGE) {

                            Write-Host "Deployment has not changed. Rollback not required."
                        }
                        else {
                            Write-Host "Rolling back to revision $env:PREVIOUS_REVISION"
                            Write-Host "Restoring image $env:PREVIOUS_IMAGE"

                            kubectl rollout undo deployment/product-service `
                                -n cloud-order `
                                --to-revision=$env:PREVIOUS_REVISION

                            if ($LASTEXITCODE -ne 0) {
                                throw "Rollback command failed"
                            }
                        }

                        kubectl rollout status deployment/product-service `
                            -n cloud-order `
                            --timeout=180s

                        if ($LASTEXITCODE -ne 0) {
                            throw "Rollback rollout failed"
                        }

                        $restored = kubectl get deployment product-service `
                            -n cloud-order -o json | ConvertFrom-Json

                        if ($LASTEXITCODE -ne 0) {
                            throw "Could not verify restored deployment"
                        }

                        $restoredImage = $restored.spec.template.spec.containers[0].image

                        if ($restoredImage -ne $env:PREVIOUS_IMAGE) {
                            throw "Rollback restored an unexpected image"
                        }

                        Write-Host "Rollback rollout successfully verified"
                        Write-Host "Restored image: $restoredImage"
                    '''
                }
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