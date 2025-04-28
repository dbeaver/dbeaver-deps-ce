cd maven-osgi-packer || { echo "Directory maven-osgi-packer not found"; exit 1; }
echo "Running mvn install for maven-osgi-packer..."
mvn install || { echo "Maven install failed"; exit 1; }
# Step 2: Go back to the original directory
cd - || exit
# Step 3: Loop through all bundles in bundles/*
for bundle in bundles/*; do
  # Check if it's a directory
  if [ -d "$bundle" ]; then
    echo "Running JAR for bundle: $bundle"
    cd "$bundle" || { echo "Failed to change directory to $bundle"; exit 1; }
    mvn dependency:copy-dependencies -Pdeps -DoutputDirectory=lib || { echo "Maven dependency copy failed for $bundle"; exit 1; }
    cd - || exit
    # bundle full path
    bundle_full_path=$(realpath "$bundle")
    java -jar maven-osgi-packer/target/maven-osgi-packer-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f "$bundle_full_path" || { echo "Failed to run JAR for $bundle"; exit 1; }
  fi
# go back to root
cd - || exit
mvn clean install -Pbuild
done
