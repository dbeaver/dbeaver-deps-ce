cd aggregate || exit
mvn clean install
cd ../osgi-bundles || exit
mvn clean install