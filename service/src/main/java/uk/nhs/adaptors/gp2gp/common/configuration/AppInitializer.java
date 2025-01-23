package uk.nhs.adaptors.gp2gp.common.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import javax.naming.ConfigurationException;

@Component(value = "appInitializer")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AppInitializer implements InitializingBean {

    private static final String S3_PREFIX = "s3";
    private final StorageConnectorConfiguration storageConnectorConfiguration;

    @Override
    public void afterPropertiesSet() throws ConfigurationException {
        LOGGER.info("Running app initializer");
        if (StringUtils.isNotBlank(storageConnectorConfiguration.getTrustStoreUrl())) {
            LOGGER.info("Adding custom TrustStore to default one");
            final CustomTrustStore customTrustStore = new CustomTrustStore(getS3Client());
            customTrustStore.addToDefault(storageConnectorConfiguration.getTrustStoreUrl(),
                storageConnectorConfiguration.getTrustStorePassword());
        } else {
            LOGGER.warn("Trust store URL is not set. Running service without the trust store.");
        }
    }

    public S3Client getS3Client() throws ConfigurationException {
        if (StringUtils.isNotBlank(storageConnectorConfiguration.getTrustStoreUrl())
            && storageConnectorConfiguration.getTrustStoreUrl().startsWith(S3_PREFIX)) {
            return S3Client.builder().build();
        }

        throw new ConfigurationException("S3Client cannot be instantiated: "
                                         + "Trust store URL is either not set or does not start with the 's3://' prefix.");
    }
}
