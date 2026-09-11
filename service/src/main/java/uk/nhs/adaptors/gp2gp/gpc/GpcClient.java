package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.utils.LogSanitizer;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {

    private static final String ODS_CODE_PLACEHOLDER = "@ODS_CODE@";
    private static final String STRUCTURED_LOG_TEMPLATE = "Gpc Access Structured Request, toASID: {}, fromASID: {}, Gpc Url: {}";
    private static final String DOCUMENT_LOG_TEMPLATE = "Gpc Access Document Request, toASID: {}, fromASID: {}, Gpc Url: {}";

    private final GpcConfiguration gpcConfiguration;
    private final GpcRequestBuilder gpcRequestBuilder;

    public String getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Getting structured record for conversation {}", structuredTaskDefinition.getConversationId());

        try {
            String gpcBaseUrlWithOds = buildGpcBaseUrl(structuredTaskDefinition);
            var requestBody = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
            var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBody, structuredTaskDefinition, gpcBaseUrlWithOds);

            logRequest(STRUCTURED_LOG_TEMPLATE, structuredTaskDefinition,
                gpcConfiguration.getUrl() + gpcConfiguration.getMigrateStructuredEndpoint());

            long startTime = System.currentTimeMillis();
            String responseBody;
            if (LOGGER.isDebugEnabled()) {
                responseBody = performRequest(request);
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.debug("Get StructuredRecord response body: {}", LogSanitizer.sanitize(responseBody));
                LOGGER.debug("Structured record retrieved, duration: {}ms, response size: {} bytes",
                    duration, responseBody != null ? responseBody.length() : 0);
                return responseBody;
            }
            responseBody = performRequest(request);
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Structured record retrieved successfully for conversation {}, duration: {}ms",
                structuredTaskDefinition.getConversationId(), duration);
            return responseBody;
        } catch (Exception e) {
            LOGGER.error("Error retrieving structured record for conversation {}",
                structuredTaskDefinition.getConversationId(), e);
            throw e;
        }
    }

    public String getDocumentRecord(GetGpcDocumentTaskDefinition documentReferencesTaskDefinition) {
        LOGGER.info("Getting document record for conversation {}", documentReferencesTaskDefinition.getConversationId());

        try {
            String gpcBaseUrlWithOds = buildGpcBaseUrl(documentReferencesTaskDefinition);
            var request = gpcRequestBuilder.buildGetDocumentRecordRequest(documentReferencesTaskDefinition, gpcBaseUrlWithOds);

            logRequest(DOCUMENT_LOG_TEMPLATE, documentReferencesTaskDefinition, gpcBaseUrlWithOds);

            long startTime = System.currentTimeMillis();
            String responseBody;
            if (LOGGER.isDebugEnabled()) {
                responseBody = performRequest(request);
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.debug("Get Document response body: {}", LogSanitizer.sanitize(responseBody));
                LOGGER.debug("Document record retrieved, duration: {}ms, response size: {} bytes",
                    duration, responseBody != null ? responseBody.length() : 0);
                return responseBody;
            }
            responseBody = performRequest(request);
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Document record retrieved successfully for conversation {}, duration: {}ms",
                documentReferencesTaskDefinition.getConversationId(), duration);
            return responseBody;
        } catch (Exception e) {
            LOGGER.error("Error retrieving document record for conversation {}",
                    documentReferencesTaskDefinition.getConversationId(), e);
            throw e;
        }
    }

    private void logRequest(String logTemplate, TaskDefinition taskDefinition, String url) {
        LOGGER.debug(logTemplate,
            taskDefinition.getToAsid(),
            taskDefinition.getFromAsid(),
            url);
    }

    private String performRequest(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        LOGGER.debug("Executing GPC HTTP request");
        try {
            long startTime = System.currentTimeMillis();
            var response = request.retrieve();
            String responseBody = response.bodyToMono(String.class).block();
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.debug("GPC HTTP request completed, duration: {}ms", duration);
            return responseBody;
        } catch (Exception e) {
            LOGGER.error("GPC HTTP request failed", e);
            throw e;
        }
    }

    private String buildGpcBaseUrl(TaskDefinition taskDefinition) {
        String builtUrl = gpcConfiguration.getUrl().replace(ODS_CODE_PLACEHOLDER, taskDefinition.getToOdsCode());
        LOGGER.debug("Built GPC base URL for ODS code: {}", taskDefinition.getToOdsCode());
        return builtUrl;
    }
}
