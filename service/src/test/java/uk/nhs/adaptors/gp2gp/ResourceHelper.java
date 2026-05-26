package uk.nhs.adaptors.gp2gp;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.w3c.dom.Document;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;

public class ResourceHelper {

    public static String loadClasspathResourceAsString(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Classpath resource path must be provided");
        }

        var resourceStream = ResourceHelper.class.getResourceAsStream(path);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + path);
        }

        try (var scanner = new Scanner(resourceStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @SneakyThrows
    public static Document loadClasspathResourceAsXml(String path) {
        return new XPathService().parseDocumentFromXml(loadClasspathResourceAsString(path));
    }
}