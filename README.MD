# How to add new bundles

To add new bundles to the project, you need to add them to `bundles` and add `pom.xml` to the bundle folder. The `pom.xml` should contain the following:
```xml
    <groupId>bundle_group</groupId>
    <artifactId>bundle_id</artifactId>
    <version>bundle_version</version>
    <packaging>${packaging.type}</packaging>
    <parent>
        <groupId>com.dbeaver.osgi</groupId>
        <artifactId>external-bundles</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
```
They should also provide maven `<dependencies>` and be added as module to the `pom.xml` in the `external-bundles` folder.

# How to build bundles
launch pack_mf_and_build.sh script in the root of the project. It will download dependencies, generate `MANIFEST.MF` files and build the bundles.
`target` folder will be created in all bundles and will contain osgi jar with the library and `MANIFEST.MF` file. The `MANIFEST.MF` file will contain all dependencies of the bundle. 