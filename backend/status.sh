#!/usr/bin/env bash

HEALTH_URL="http://localhost:8080/q/health"

# Use curl or wget to check health endpoint
if command -v curl &> /dev/null; then
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
elif command -v wget &> /dev/null; then
    HTTP_STATUS=$(wget --server-response --spider --quiet "$HEALTH_URL" 2>&1 | awk '/HTTP\// {print $2}' | tail -n 1)
else
    HTTP_STATUS="000"
fi

if [ "$HTTP_STATUS" = "200" ]; then
    echo "================================================="
    echo "✅ Severus Finance backend is RUNNING"
    echo "API:      http://localhost:8080"
    echo "Health:   http://localhost:8080/q/health"
    echo "Database: PostgreSQL connected"
    echo "================================================="
    exit 0
else
    echo "================================================="
    echo "❌ Severus Finance backend is not running"
    echo "================================================="
    exit 1
fi
