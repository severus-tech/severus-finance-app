#!/usr/bin/env bash
set -e

# Ensure running from backend directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "================================================="
echo "🚀 Starting Severus Finance Backend in Dev Mode..."
echo "================================================="

# Check for local .env file
if [ ! -f ".env" ]; then
    echo "❌ Error: Missing '.env' file in backend folder."
    echo "Please copy '.env.example' to '.env' before running."
    echo "Command: cp .env.example .env"
    exit 1
fi

# Export environment variables from .env
set -o allexport
source .env
set +o allexport

echo "✅ Environment variables loaded from .env"

# Verify Java availability
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH."
    exit 1
fi

JAVA_VER="$(java -version 2>&1 | head -n 1)"
echo "☕ Java Runtime: $JAVA_VER"

# Verify Maven wrapper availability
if [ -f "./mvnw" ]; then
    MAVEN_CMD="./mvnw"
elif command -v mvn &> /dev/null; then
    MAVEN_CMD="mvn"
else
    echo "❌ Error: Neither './mvnw' nor 'mvn' executable found."
    exit 1
fi

echo "📦 Using Maven Command: $MAVEN_CMD"
echo "🚀 Launching Quarkus Dev Server (skipping test compilation for fast startup)..."
echo "================================================="

# Run Quarkus dev mode skipping tests
exec $MAVEN_CMD -Dmaven.test.skip=true quarkus:dev
