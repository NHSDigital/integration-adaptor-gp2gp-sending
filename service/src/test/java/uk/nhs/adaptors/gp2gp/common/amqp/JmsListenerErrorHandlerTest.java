package uk.nhs.adaptors.gp2gp.common.amqp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessResourceFailureException;

import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

class JmsListenerErrorHandlerTest {

    private final JmsListenerErrorHandler jmsListenerErrorHandler = new JmsListenerErrorHandler();

    @Test
    void When_HandleError_WithNoCause_Expect_NoExceptionThrown() {
        assertThatCode(() -> jmsListenerErrorHandler.handleError(new RuntimeException("outer")))
            .doesNotThrowAnyException();
    }

    @Test
    void When_HandleError_WithNonRetryableCause_Expect_NoExceptionThrown() {
        var throwable = new RuntimeException("outer", new IllegalArgumentException("inner"));

        assertThatCode(() -> jmsListenerErrorHandler.handleError(throwable))
            .doesNotThrowAnyException();
    }

    @Test
    void When_HandleError_WithDataAccessResourceFailureCause_Expect_RuntimeExceptionThrown() {
        var throwable = new RuntimeException("outer", new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> jmsListenerErrorHandler.handleError(throwable))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unable to access database");
    }

    @Test
    void When_HandleError_WithMhsConnectionCause_Expect_RuntimeExceptionThrown() {
        var throwable = new RuntimeException("outer", new MhsConnectionException("MHS down"));

        assertThatThrownBy(() -> jmsListenerErrorHandler.handleError(throwable))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unable to connect to MHS Outbound");
    }
}
