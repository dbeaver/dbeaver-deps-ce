cd aggregate || exit
mvn clean install
cd ../osgi-root || exit
mvn clean install