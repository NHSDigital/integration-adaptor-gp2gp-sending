package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;
import uk.nhs.adaptors.gp2gp.common.validation.ValidMongoClientConfiguration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gp2gp")
@EnableScheduling

public class Gp2gpConfiguration {
    private int largeAttachmentThreshold;
    private int largeEhrExtractThreshold;
}
