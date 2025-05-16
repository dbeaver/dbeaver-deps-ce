cd aggregate || exit
mvn clean install
cd ../target-bundles || exit
mvn clean install