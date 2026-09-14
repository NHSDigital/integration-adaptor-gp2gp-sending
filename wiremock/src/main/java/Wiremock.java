import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;


class Wiremock {
    private static final Logger LOGGER = LoggerFactory.getLogger(Wiremock.class);

    public static void main(String[] args) {
        WireMockServer wireMockServer = new WireMockServer(getOptions());
        wireMockServer.start();
    }

    private static WireMockConfiguration getOptions() {
        WireMockConfiguration wireMockConfiguration = options()
                .port(8080)
                .usingFilesUnderDirectory("./stubs")
                .globalTemplating(true)
                .disableRequestJournal();

        var pds = getPds();
        if (pds != null) {
            wireMockConfiguration.extensions(new PatientTemplateDataProvider(pds));
        }
        return wireMockConfiguration;
    }

    private static PatientDemographicsServiceClient getPds() {
        var apiKey = System.getenv("PDS_API_KEY");
        var keyId = System.getenv("PDS_KEY_ID");
        var privateKey = System.getenv("PDS_PRIVATE_KEY");
        if (apiKey != null && keyId != null && privateKey != null) {
            return new PatientDemographicsServiceClient(privateKey, apiKey, keyId);
        }
        LOGGER.info("PDS lookups disabled, one or more of PDS_API_KEY, PDS_KEY_ID, PDS_PRIVATE_KEY is not configured");
        return null;
    }
}