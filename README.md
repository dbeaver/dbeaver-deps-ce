# DBeaver Community third party dependencies

We convert plain Maven artifacts into osgi bundles here.
Maven plugin [maven-osgi-packer](https://github.com/dbeaver/dbeaver-osgi-common/maven-osgi-packer) parses maven dependencies and produces OSGI bundles. 
Which than packed into [P2](https://help.sonatype.com/en/p2-repositories.html) repository.

Target P2 repository location: https://repo.dbeaver.net/p2/ce

These dependencies are used in [DBeaver](https://github.com/dbeaver/dbeaver), [CloudBeaver](https://github.com/dbeaver/cloudbeaver) and other products.

## Build

- Run `mvn package` in repo root. It will build dependent OSGI bundles
- Go to `p2` folder and run `mvn package`. This will build P2 repository.

## TODO

- Add icu.base sources
- Repackage GIS WKG
