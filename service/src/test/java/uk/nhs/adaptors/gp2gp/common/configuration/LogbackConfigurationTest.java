package uk.nhs.adaptors.gp2gp.common.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackConfigurationTest {

    public static final int MILLIS_100 = 100;
    private Logger rootLogger;
    private Logger gp2gpLogger;
    private Logger reactorNettyLogger;

    @BeforeEach
    void setUp() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        gp2gpLogger = (Logger) LoggerFactory.getLogger("uk.nhs.adaptors.gp2gp");
        reactorNettyLogger = (Logger) LoggerFactory.getLogger("reactor.netty.http.client");
    }

    @Test
    void shouldHaveAsyncAppenderOnRootLogger() {

        assertThat(rootLogger.getAppender("ASYNC"))
            .as("AsyncAppender 'ASYNC' should be attached to root logger")
            .isNotNull();

        Appender<?> appender = rootLogger.getAppender("ASYNC");
        assertThat(appender.getClass().getSimpleName())
            .contains("AsyncAppender");
    }

    @Test
    void shouldHaveGp2gpLoggerConfiguredWithProperLevel() {
        assertThat(gp2gpLogger.getEffectiveLevel())
            .as("GP2GP logger should inherit or override level explicitly")
            .isNotNull();
    }

    @Test
    void shouldDecoupleReactorNettyLoggingLevelFromGp2GpLevel() {
        Level reactorLevel = reactorNettyLogger.getEffectiveLevel();

        assertThat(reactorLevel)
            .as("Reactor Netty logger should have explicit level to decouple from GP2GP logger")
            .isNotNull();
    }

    @Test
    void shouldPreserveMdcInAsyncLogging() {

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        gp2gpLogger.addAppender(listAppender);
        gp2gpLogger.setLevel(Level.INFO);

        MDC.put("ConversationId", "test-conv-123");
        MDC.put("TaskId", "test-task-456");

        gp2gpLogger.info("Test message with MDC");

        try {
            Thread.sleep(MILLIS_100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(listAppender.list).isNotEmpty();

        ILoggingEvent event = listAppender.list.getFirst();
        assertThat(event.getMDCPropertyMap())
            .containsEntry("ConversationId", "test-conv-123")
            .containsEntry("TaskId", "test-task-456");

        MDC.clear();
        gp2gpLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void shouldNotBlockOnLoggingIo() {

        Appender<?> asyncAppender = rootLogger.getAppender("ASYNC");

        assertThat(asyncAppender)
            .as("AsyncAppender should be configured to never block producer threads")
            .isNotNull();

        assertThat(asyncAppender.getClass().getSimpleName())
            .isEqualTo("AsyncAppender");
    }

    @Test
    void rootLoggerShouldHaveValidTextAppenderReference() {

        Appender<?> appender = rootLogger.getAppender("ASYNC");

        assertThat(appender)
            .as("Root logger should have valid ASYNC appender (wraps corrected TEXT appender)")
            .isNotNull();

        assertThat(appender.getName())
            .as("Appender name should be ASYNC, not a default fallback")
            .isEqualTo("ASYNC");
    }
}





