# deploy.ps1

# Ensure we are in the project root (assuming script is in /deploy)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $scriptPath
Set-Location $projectRoot

Write-Host "Switching to develop branch..."
git checkout develop
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to checkout develop branch."
    exit 1
}

Write-Host "Pulling latest changes..."
git pull origin develop

Write-Host "`nRecent 5 tags:"
git tag --sort=-v:refname | Select-Object -First 5

$version = Read-Host "`nPlease enter the new version (e.g., v1.0.1)"
if ([string]::IsNullOrWhiteSpace($version)) {
    Write-Error "Version cannot be empty."
    exit 1
}

$confirmation = Read-Host "Confirm version '$version'? (y/n)"
if ($confirmation -ne 'y') {
    Write-Host "Deployment cancelled."
    exit 0
}

Write-Host "Creating tag $version..."
git tag $version
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create tag."
    exit 1
}

Write-Host "Switching to master branch..."
git checkout master
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to checkout master branch."
    exit 1
}

Write-Host "Pulling latest master..."
git pull origin master

Write-Host "Merging develop into master..."
git merge develop
if ($LASTEXITCODE -ne 0) {
    Write-Error "Merge failed. Please resolve conflicts manually."
    exit 1
}

Write-Host "Pushing to master..."
git push origin master
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push to master."
    exit 1
}

Write-Host "Pushing tag $version..."
git push origin $version
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push tag."
    exit 1
}

Write-Host "Deployment successful! GitHub Action should be triggered."

git checkout develop