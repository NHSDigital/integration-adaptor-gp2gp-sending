package uk.nhs.adaptors.gp2gp.gpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.mustachejava.Mustache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpcTemplateUtilsTest {

    private static final String EXISTING_TEMPLATE = "jwt.header.mustache";

    @Mock
    private Mustache template;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GpcTemplateUtils.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void When_LoadTemplate_Expect_DebugLogContainsTemplateName() {
        GpcTemplateUtils.loadTemplate(EXISTING_TEMPLATE);

        assertContains(Level.DEBUG, "Loading GPC mustache template " + EXISTING_TEMPLATE);
    }

    @Test
    void When_LoadTemplate_DebugDisabled_Expect_NoDebugLog() {
        logger.setLevel(Level.INFO);

        GpcTemplateUtils.loadTemplate(EXISTING_TEMPLATE);

        List<ILoggingEvent> debugEvents = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.DEBUG)
            .toList();
        assertThat(debugEvents).isEmpty();
    }

    @Test
    void When_FillTemplateThrowsIOException_Expect_ErrorLogAndGpConnectException() {
        Writer throwingWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) { }

            @Override
            public void flush() throws IOException {
                throw new IOException("simulated flush failure");
            }

            @Override
            public void close() { }
        };
        when(template.execute(any(Writer.class), (Object) any())).thenReturn(throwingWriter);

        assertThrows(GpConnectException.class, () -> GpcTemplateUtils.fillTemplate(template, new Object()));

        assertContains(Level.ERROR, "Unable to fill GPC mustache template for");
    }

    private void assertContains(Level level, String expectedMessage) {
        List<String> messages = logAppender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertThat(messages).anyMatch(message -> message.contains(expectedMessage));
    }
}
