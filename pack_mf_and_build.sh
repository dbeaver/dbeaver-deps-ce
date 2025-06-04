cd aggregate || exit
mvn clean package
cd ../target-bundles || exit
mvn clean package
cd ../features/com.ce.bundle.feature || exit
mvn clean package
cd ../../repository || exit
mvn clean package