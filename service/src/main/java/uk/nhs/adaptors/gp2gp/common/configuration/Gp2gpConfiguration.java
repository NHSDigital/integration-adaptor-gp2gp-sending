package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.springframework.scheduling.annotation.EnableScheduling;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gp2gp")
@EnableScheduling
public class Gp2gpConfiguration {
    private int largeAttachmentThreshold;
    private int largeEhrExtractThreshold;
}
