package uk.nhs.adaptors.gp2gp.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;
import uk.nhs.adaptors.gp2gp.common.validation.ValidStorageConnectorConfiguration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gp2gp.storage")
@Validated
@ValidStorageConnectorConfiguration
public class StorageConnectorConfiguration {

    private String type;
    private String containerName;
    private String azureConnectionString;
    private String trustStoreUrl;
    private String trustStorePassword;

}
