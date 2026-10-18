#!/bin/bash

LATEST_VERSION=$1 # Needs to be passed as an argument

set -e # Ensure the script stops on error

#----------------------------------------------------------
# Step 1: Check if LATEST_VERSION is provided

if [[ -z "$LATEST_VERSION" ]]; then
    echo "Error: LATEST_VERSION must be provided as an argument."
    echo "Usage: $0 <LATEST_VERSION>"
    exit 1
fi

#----------------------------------------------------------
# Step 2: Setup page for the new version

# Create a new version variable with the dots replaced by underscores
NEW_FILE_NAME=$(echo $LATEST_VERSION | tr . _)

# Copy the docs/version-template.md and name it after the new version
cp docs/version-template.md docs/$NEW_FILE_NAME.md

# Replace the placeholder __NEW_VERSION__ with the latest version in the new file
sed -i "s/__NEW_VERSION__/$LATEST_VERSION/g" docs/$NEW_FILE_NAME.md

# Add the new version to the mkdocs.yml file
sed -i "/- Versions:/a\      - $LATEST_VERSION: $NEW_FILE_NAME.md" mkdocs.yml

echo "Setup of the page for version $LATEST_VERSION is complete."

#----------------------------------------------------------
# Step 3: Setup the doc and plugin for the new version

# Create a directory for the version in the /docs folder
mkdir -p docs/$LATEST_VERSION

# Download the latest version of the documentation
wget -O docs/$LATEST_VERSION/doc.zip https://github.com/nickchecan/zjoule/releases/download/$LATEST_VERSION/doc.zip
unzip -o docs/$LATEST_VERSION/doc.zip -d docs/$LATEST_VERSION/doc && rm docs/$LATEST_VERSION/doc.zip
echo "Setup of the Javadoc for version $LATEST_VERSION is complete."

# Download the latest version of the plugin
wget -O docs/$LATEST_VERSION/plugin.zip https://github.com/nickchecan/zjoule/releases/download/$LATEST_VERSION/plugin.zip
unzip -o docs/$LATEST_VERSION/plugin.zip -d docs/$LATEST_VERSION/plugin && rm docs/$LATEST_VERSION/plugin.zip
echo "Setup of the plugin for version $LATEST_VERSION is complete."

#----------------------------------------------------------
# Step 4: Update installation instructions at the home page

# Replace the plugin version in the URL with the new version
sed -i "s|https://zjoule.com/.*/plugin|https://zjoule.com/$LATEST_VERSION/plugin|g" docs/index.md
echo "Setup of the index for version $LATEST_VERSION is complete."