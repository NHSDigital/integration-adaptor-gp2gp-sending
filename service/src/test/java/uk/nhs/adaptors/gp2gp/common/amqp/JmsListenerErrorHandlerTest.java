package uk.nhs.adaptors.gp2gp.common.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

class JmsListenerErrorHandlerTest {

    private final JmsListenerErrorHandler jmsListenerErrorHandler = new JmsListenerErrorHandler();
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(JmsListenerErrorHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void When_HandleError_WithNoCause_Expect_NoExceptionThrown() {
        assertThatCode(() -> jmsListenerErrorHandler.handleError(new RuntimeException("outer")))
            .doesNotThrowAnyException();

        assertErrorContains("Handling JMS message error due to [java.lang.RuntimeException] with message [outer]");
        assertThat(getErrorMessages())
            .noneMatch(message -> message.contains("Caught Error cause of type"));
    }

    @Test
    void When_HandleError_WithNonRetryableCause_Expect_NoExceptionThrown() {
        var throwable = new RuntimeException("outer", new IllegalArgumentException("inner"));

        assertThatCode(() -> jmsListenerErrorHandler.handleError(throwable))
            .doesNotThrowAnyException();

        assertErrorContains("Handling JMS message error due to [java.lang.RuntimeException] with message [outer]");
        assertErrorContains("Caught Error cause of type: [class java.lang.IllegalArgumentException], with message: [inner]");
    }

    @Test
    void When_HandleError_WithDataAccessResourceFailureCause_Expect_RuntimeExceptionThrown() {
        var throwable = new RuntimeException("outer", new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> jmsListenerErrorHandler.handleError(throwable))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unable to access database");

        assertErrorContains("Handling JMS message error due to [java.lang.RuntimeException] with message [outer]");
        assertErrorContains("Caught Error cause of type: [class org.springframework.dao.DataAccessResourceFailureException], "
            + "with message: [db down]");
    }

    @Test
    void When_HandleError_WithMhsConnectionCause_Expect_RuntimeExceptionThrown() {
        var throwable = new RuntimeException("outer", new MhsConnectionException("MHS down"));

        assertThatThrownBy(() -> jmsListenerErrorHandler.handleError(throwable))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unable to connect to MHS Outbound");

        assertErrorContains("Handling JMS message error due to [java.lang.RuntimeException] with message [outer]");
        assertErrorContains("Caught Error cause of type: [class uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException], "
            + "with message: [MHS down]");
    }

    private void assertErrorContains(String expectedMessagePart) {
        assertThat(getErrorMessages()).anyMatch(message -> message.contains(expectedMessagePart));
    }

    private List<String> getErrorMessages() {
        return logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
