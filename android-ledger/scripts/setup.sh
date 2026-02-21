#!/bin/bash
echo "Setting up Android Ledger Project..."
if ! command -v git &> /dev/null; then
    echo "Git not found. Please install git."
    exit 1
fi
if ! command -v java &> /dev/null; then
    echo "Java not found. Please install JDK 17."
    exit 1
fi
echo "Setup complete. You can now build the project."
