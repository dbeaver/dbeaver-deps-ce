# DBeaver Community third party dependencies

We convert plain Maven artifacts into osgi bundles here.
Maven plugin [maven-osgi-packer](https://github.com/dbeaver/dbeaver-osgi-common/maven-osgi-packer) parses maven dependencies and produces OSGI bundles. 
Which than packed into [P2](https://help.sonatype.com/en/p2-repositories.html) repository.

Target P2 repository location: https://repo.dbeaver.net/p2/ce

These dependencies are used in [DBeaver](https://github.com/dbeaver/dbeaver), [CloudBeaver](https://github.com/dbeaver/cloudbeaver) and other products.

## TODO

- Add icu.base sources
- Repackage GIS WKG
- Get rid of jfreechart.swt (add our custom ChartComposite in dbeaver codebase)
- Fix jetty.server
 