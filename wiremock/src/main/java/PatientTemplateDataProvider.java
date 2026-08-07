import com.github.tomakehurst.wiremock.extension.TemplateModelDataProviderExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class PatientTemplateDataProvider implements TemplateModelDataProviderExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientTemplateDataProvider.class);

    final PatientDemographicsServiceClient pds;

    PatientTemplateDataProvider(PatientDemographicsServiceClient pds) {
        this.pds = pds;
    }

    @Override
    public Map<String, Object> provideTemplateModelData(ServeEvent serveEvent) {
        try {
            var nhsNumber = getNhsNumber(serveEvent);
            if (nhsNumber != null) {
                LOGGER.debug("Fetching patient details for {}", nhsNumber);
                return Map.of("patient", pds.patient(nhsNumber));
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to fetch patient details from PDS", e);
        }
        return Map.of();
    }

    private static String getNhsNumber(ServeEvent serveEvent) {
        try {
            JSONObject object = new JSONObject(serveEvent.getRequest().getBodyAsString());
            return object.getJSONArray("parameter").getJSONObject(0).getJSONObject("valueIdentifier").getString("value");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "personal-demographics-service";
    }
}
