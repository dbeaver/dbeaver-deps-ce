package org.dbeaver.packer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.sql.rowset.spi.XmlReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class MavenOSGIFragmentPacker  {

    public static void main(String[] args) {
        try {
            Params params = new Params();
            params.init(args);
            // parse pom.xml
            Path basedir = params.target;
            Path pomFile = basedir.resolve("pom.xml");
            ParseResult parseResult = parsePomFile(pomFile);
            // Assuming project base dir is current dir
            Path metaFolder = basedir.resolve("META-INF");
            File fragPath = metaFolder.resolve("FRAG.FMF").toFile();
            if (!metaFolder.toFile().exists()) {
                return;
            }
            Path manifestPath = basedir.resolve("META-INF").resolve("MANIFEST.MF");
            Files.deleteIfExists(manifestPath);
            if (fragPath.exists() && fragPath.isFile()) {
                System.out.println("Found FRAG.FMF at: " + fragPath.getAbsolutePath());
            } else {
                System.out.println("FRAG.FMF not found in META-INF directory.");
            }
            String moduleVersion = parseResult.version;
            String symbolicName = parseResult.artifactId;
            String moduleName = String.valueOf(basedir.getFileName());
            List<String> classpaths = new ArrayList<>();
            Path path = basedir.resolve("lib");
            if (Files.exists(path)) {
                try (Stream<Path> list = Files.list(path)) {
                    list.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".jar"))
                        .forEach(p -> {
                            classpaths.add(p.toAbsolutePath().toString());
                        });
                }
            }
            String s = buildManifest(symbolicName, moduleName, moduleVersion, classpaths, params.target, fragPath.toPath());
            Files.createFile(manifestPath);
            Files.write(manifestPath, s.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error while packing OSGI fragment", e);
        }
    }

    // pars
    private static ParseResult parsePomFile(Path pom) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (InputStream is = Files.newInputStream(pom)) {
            doc = builder.parse(is);
        }
        doc.getDocumentElement().normalize();

        // Extract main artifact details.
        String groupId = getTagValue(doc, "groupId");
        // Fallback: if groupId is not defined on the project, check the parent.
        if (groupId == null || groupId.isEmpty()) {
            NodeList parentNodes = doc.getElementsByTagName("parent");
            if (parentNodes != null && parentNodes.getLength() > 0) {
                Element parentElement = (Element) parentNodes.item(0);
                groupId = getTagValue(parentElement, "groupId");
            }
        }
        String artifactId = getTagValue(doc, "artifactId");
        String version = getTagValue(doc, "version");
        return new ParseResult(groupId, artifactId, version, null);
    }
    // Helper method to retrieve the text content of a given tag from an Element.
    private static String getTagValue(Element element, String tag) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }

    // Helper method to retrieve the text content of the first occurrence of a given tag from the Document.
    private static String getTagValue(Document doc, String tag) {
        NodeList nodeList = doc.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }
    // Utility method to extract the value of a given tag
    private static String getTagValue(String tag, Document document) {
        NodeList nodeList = document.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;  // Return null if the tag is not found
    }
    private static String buildManifest(
        String symbolicName,
        String moduleName,
        String moduleVersion,
        List<String> classpaths,
        Path basedir,
        Path fragPath)
        throws IOException {

        StringBuilder manifest = new StringBuilder();
        manifest.append("Manifest-Version: 1.0\n");
        manifest.append("Bundle-ManifestVersion: 2\n");
        manifest.append("Bundle-SymbolicName: ").append(symbolicName).append("\n");
        manifest.append("Bundle-Version: ").append(adaptVersion(moduleVersion)).append("\n");
        manifest.append("Bundle-Name: ").append(moduleName).append("\n");
        manifest.append("Bundle-ActivationPolicy: lazy\n");
        manifest.append("Bundle-RequiredExecutionEnvironment: JavaSE-17\n");
        // write classpath
        manifest.append("Bundle-ClassPath: \n");

        for (String classpath : classpaths) {
            manifest.append(" ").append(basedir.relativize(Paths.get(classpath)));
            if (!classpaths.get(classpaths.size() - 1).equals(classpath)) {
                manifest.append(",\n");
            }

        }
        manifest.append("\n");
        // Real all jar files in classpath, extract packages as exports, if packages have no exports in manifest get all packages
        final boolean[] hasExportPackage = {false};

        for (String classpath : classpaths) {
            try (JarFile jarFile = new JarFile(classpath)) {
                Manifest mf = jarFile.getManifest();
                if (mf != null) {
                    mf.getMainAttributes().forEach((key, value) -> {
                        splitByCommaOutsideQuotes(value.toString()).forEach(v -> {
                            if (key.toString().startsWith("Export-Package")) {
                                if (!hasExportPackage[0]) {
                                    manifest.append(key).append(": \n ").append(v);
                                    hasExportPackage[0] = true;
                                } else {
                                    manifest.append(",\n ").append(v);
                                }
                            }
                        });});
                }
            } catch (IOException e) {
                System.out.println("Error reading jar file: " + e.getMessage());
            }
        }
        manifest.append("\n");
        // dependencies, extract parameters from fragPath .MF file\
        provideDependencies(fragPath, manifest);
        manifest.append("\n");
        return manifest.toString();
    }
    public static List<String> splitByCommaOutsideQuotes(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes; // Toggle quote mode
                current.append(c);
            } else if (c == ',' && !insideQuotes) {
                result.add(current.toString());
                current.setLength(0); // Reset buffer
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private static void provideDependencies(Path fragPath, StringBuilder manifest) {
        try (FileInputStream fos = new FileInputStream(fragPath.toFile())) {
            Manifest mf = new Manifest(fos);
            mf.getMainAttributes().forEach((key, value) -> {
                boolean isFirst = true;
                List<String> strings = splitByCommaOutsideQuotes(value.toString());
                for (String string : strings) {
                    if (isFirst) {
                        manifest.append(key).append(": \n ").append(string);
                        isFirst = false;
                    } else  {
                        manifest.append(",\n ").append(string);
                    }
                }
            manifest.append("\n");
            });
        } catch (Exception e) {
            System.out.println("Error extracting dependencies: " + e.getMessage());
        }}

    private static String adaptVersion(String moduleVersion) {
        //adapt version to OSGI format
        String[] parts = moduleVersion.split("\\.");
        StringBuilder adaptedVersion = new StringBuilder();
        boolean isFirst = true;
        for (String part : parts) {
            if (part.matches("\\d+")) {
                if (isFirst) {
                    adaptedVersion.append(part);
                    isFirst = false;
                } else {
                    adaptedVersion.append(".").append(part);
                }
            } else {
                if (isFirst) {
                    adaptedVersion.append(part.replaceAll("[^a-zA-Z0-9]", "_"));
                    isFirst = false;
                } else {
                    adaptedVersion.append(".").append(part.replaceAll("[^a-zA-Z0-9]", "_"));
                }

            }
        }
        // SNAPSHOT versions are not allowed in OSGI remove them
        if (adaptedVersion.toString().endsWith("_SNAPSHOT")) {
            adaptedVersion.delete(adaptedVersion.length() - 9, adaptedVersion.length());
        }
        return adaptedVersion.toString();
    }

    private record ParseResult(
        String groupId,
        String artifactId,
        String version,
        String classifier
    ) {
    }
}
