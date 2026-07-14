package uk.nhs.adaptors.gp2gp.gpc.builder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

@ExtendWith(MockitoExtension.class)
class GpcTokenBuilderTest {
    private static final int EXPIRY_TIME_ADDITION = 300;
    private static final long EPOCH_SECOND = 1613734770L;
    private static final String ODS_CODE = "ODS-CODE";

    @Mock
    private GpcConfiguration gpcConfiguration;

    @Mock
    private TimestampService timestampService;

    @InjectMocks
    private GpcTokenBuilder gpcTokenBuilder;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUpLogCapture() {
        logger = (Logger) LoggerFactory.getLogger(GpcTokenBuilder.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDownLogCapture() {
        logger.detachAppender(logAppender);
        logAppender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void When_GpcJwtTokenIsCreated_Expect_IatAndExpAreIntegerSeconds() {
        Instant timestamp = Instant.ofEpochSecond(EPOCH_SECOND);
        when(timestampService.now()).thenReturn(timestamp);
        when(gpcConfiguration.getUrl()).thenReturn("http://aud");
        String token = gpcTokenBuilder.buildToken(ODS_CODE);
        String[] tokenParts = token.split("\\.");
        String payloadBase64 = tokenParts[1];
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);

        String expectedExp = String.format("\"exp\": %d", timestamp.getEpochSecond() + EXPIRY_TIME_ADDITION);
        String expectedIat = String.format("\"iat\": %d", timestamp.getEpochSecond());
        assertThat(payloadJson).contains(expectedExp, expectedIat);
    }
    @Test
    void When_GpcJwtTokenIsCreated_Expect_RequestingPractitionerElements() {
        Instant timestamp = Instant.ofEpochSecond(EPOCH_SECOND);
        when(timestampService.now()).thenReturn(timestamp);
        when(gpcConfiguration.getUrl()).thenReturn("http://aud");
        String token = gpcTokenBuilder.buildToken(ODS_CODE);
        String[] tokenParts = token.split("\\.");
        String payloadBase64 = tokenParts[1];
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);

        assertThat(payloadJson).contains("https://fhir.nhs.uk/Id/sds-user-id",
                                         "https://fhir.nhs.uk/Id/sds-role-profile-id",
                                         "family",
                                         "given");
    }

    @Test
    void When_GpcJwtTokenIsBuilt_Expect_DebugLogsForStartAndCompletion() {
        Instant timestamp = Instant.ofEpochSecond(EPOCH_SECOND);
        when(timestampService.now()).thenReturn(timestamp);
        when(gpcConfiguration.getUrl()).thenReturn("http://aud");

        gpcTokenBuilder.buildToken(ODS_CODE);

        assertContains(Level.DEBUG, "Building GPC bearer token for source ODS code " + ODS_CODE);
        assertContains(Level.DEBUG, "Built GPC bearer token payload for source ODS code " + ODS_CODE);
    }

    @Test
    void When_GpcJwtTokenBuildFails_Expect_ErrorLogWithOdsCode() {
        RuntimeException exception = new RuntimeException("timestamp failure");
        when(timestampService.now()).thenThrow(exception);

        assertThrows(RuntimeException.class, () -> gpcTokenBuilder.buildToken(ODS_CODE));

        assertContains(Level.DEBUG, "Building GPC bearer token for source ODS code " + ODS_CODE);
        assertContains(Level.ERROR, "Failed to build GPC bearer token for source ODS code " + ODS_CODE);
    }

    private void assertContains(Level level, String expectedMessagePart) {
        List<String> messages = logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();

        assertThat(messages).anyMatch(message -> message.contains(expectedMessagePart));
    }

}
