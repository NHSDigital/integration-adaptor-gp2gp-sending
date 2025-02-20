package uk.nhs.adaptors.gp2gp.gpc;

public class GpcFilenameUtils {
    private static final String PATH_SEPARATOR = "/";
    public static final String JSON_EXTENSION = ".json";
    public static final String GZIP_EXTENSION = ".gzip";
    public static final String GPC_STRUCTURED_FILE_EXTENSION = "_gpc_structured" + JSON_EXTENSION;

    public static String generateStructuredRecordFilename(String conversationId) {
        return conversationId.concat(PATH_SEPARATOR).concat(conversationId).concat(GPC_STRUCTURED_FILE_EXTENSION);
    }

    public static String generateDocumentStoragePath(String conversationId, String documentId) {
        return conversationId.concat(PATH_SEPARATOR).concat(documentId).concat(JSON_EXTENSION);
    }

    public static String generateLargeExrExtractFilename(String documentId) {
        return documentId.concat(GZIP_EXTENSION);
    }
}
