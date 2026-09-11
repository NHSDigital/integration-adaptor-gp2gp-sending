package uk.nhs.adaptors.gp2gp.common.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class StorageConnectorService {
    private final StorageConnector storageConnector;
    private final ObjectMapper objectMapper;

    @SneakyThrows({JsonProcessingException.class, IOException.class})
    public void uploadFile(StorageDataWrapper response, String filename) {
        LOGGER.info("Uploading file to storage: filename={}", filename);
        try {
            String jsonStringResponse = objectMapper.writeValueAsString(response);
            var responseBytes = jsonStringResponse.getBytes(UTF_8);
            LOGGER.debug("Serialized response payload size: {} bytes", responseBytes.length);

            try (var responseInputStream = new ByteArrayInputStream(responseBytes)) {
                storageConnector.uploadToStorage(responseInputStream, responseBytes.length, filename);
                LOGGER.info("File uploaded successfully: filename={}", filename);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to upload file: filename={}", filename, e);
            throw e;
        }
    }

    @SneakyThrows
    public StorageDataWrapper downloadFile(String filename) {
        LOGGER.info("Downloading file from storage: filename={}", filename);
        try {
            String stringDownload;
            try (var inputStream = storageConnector.downloadFromStorage(filename)) {
                stringDownload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                LOGGER.debug("Downloaded file size: {} characters", stringDownload.length());
            }
            StorageDataWrapper result = objectMapper.readValue(stringDownload, StorageDataWrapper.class);
            LOGGER.info("File downloaded and deserialized successfully: filename={}", filename);
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to download file: filename={}", filename, e);
            throw e;
        }
    }
}
