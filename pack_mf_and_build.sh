cd aggregate || exit
mvn clean install -X
cd ../osgi-bundles || exit
mvn clean install -X