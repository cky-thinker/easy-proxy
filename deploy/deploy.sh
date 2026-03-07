#!/bin/bash

# Ensure the script exits on any error
set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Navigate to project root
cd "$PROJECT_ROOT"

echo "Switching to develop branch..."
git checkout develop
echo "Pulling latest changes..."
git pull origin develop

echo -e "\nRecent 5 tags:"
git tag --sort=-v:refname | head -n 5

echo -e "\nPlease enter the new version (e.g., v1.0.1):"
read VERSION

if [ -z "$VERSION" ]; then
    echo "Version cannot be empty."
    exit 1
fi

echo "Confirm version '$VERSION'? (y/n)"
read CONFIRMATION

if [ "$CONFIRMATION" != "y" ]; then
    echo "Deployment cancelled."
    exit 0
fi

echo "Creating tag $VERSION..."
git tag "$VERSION"

echo "Switching to master branch..."
git checkout master
echo "Pulling latest master..."
git pull origin master

echo "Merging develop into master..."
git merge develop

echo "Pushing to master..."
git push origin master

echo "Pushing tag $VERSION..."
git push origin "$VERSION"

echo "Deployment successful! GitHub Action should be triggered."
