#!/usr/bin/env bash
set -e

echo "============================================================"
echo " Rural Health Network Platform - Startup"
echo "============================================================"
echo

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java was not found on your PATH."
  echo "        Install JDK 17+ (e.g. 'brew install openjdk@17' or your package manager) and try again."
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven was not found on your PATH."
  echo "        Install it (e.g. 'brew install maven' or 'sudo apt install maven') and try again."
  exit 1
fi

echo "[OK] Java and Maven were found."
echo
echo "Make sure MySQL is running locally before continuing"
echo "  (default expected: localhost:3306, user 'root', password 'root' -"
echo "   edit src/main/resources/application.properties to change this)."
echo
read -p "Press Enter once MySQL is running to continue..."

echo
echo "Starting the application..."
echo "Website:   http://localhost:8080"
echo "API login: admin / admin123  (see README.md to change)"
echo
echo "Press Ctrl+C to stop the server."
echo "============================================================"
echo

mvn spring-boot:run
