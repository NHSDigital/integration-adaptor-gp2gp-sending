package uk.nhs.adaptors.gp2gp.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gp2gp.storage")
public class StorageConnectorConfiguration {

    private String type;
    private String containerName;
    private String azureConnectionString;
    private String trustStoreUrl;
    private String trustStorePassword;

}
