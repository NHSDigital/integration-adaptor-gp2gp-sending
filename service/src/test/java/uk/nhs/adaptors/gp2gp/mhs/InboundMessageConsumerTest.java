package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

@ExtendWith(MockitoExtension.class)
class InboundMessageConsumerTest {

    @Mock
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message mockMessage;
    @Mock
    private Session mockSession;
    @Mock
    private MDCService mdcService;
    @InjectMocks
    private InboundMessageConsumer inboundMessageConsumer;

    @Test
    @SneakyThrows
    void When_MessageHandlerReturnsTrue_Expect_MessageIsAcknowledged() {
        stubHandleResult(true);

        callReceive();

        verifyMessageHandled();
        verify(mockMessage, times(1)).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_MessageHandlerReturnsFalse_Expect_MessageIsNotAcknowledged() {
        stubHandleResult(false);

        callReceive();

        verifyMessageHandled();
        verify(mockMessage, never()).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_MessageHandlerReturnsFalse_Expect_SessionToBeRolledBack() {
        stubHandleResult(false);

        callReceive();

        verifyMessageHandled();
        verify(mockSession, times(1)).rollback();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_MessageHandlerThrowsException_Expect_MessageIsNotAcknowledged() {
        stubHandleThrows(new RuntimeException());

        callReceive();

        verifyMessageHandled();
        verify(mockMessage, never()).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_MessageHandlerThrowsJMSException_Expect_SessionIsRolledBack() {
        stubHandleThrows(new JMSRuntimeException("Test"));

        callReceive();

        verifyMessageHandled();
        verify(mockSession, times(1)).rollback();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_MessageHandlerThrowsDataAccessResourceFailure_Expect_ExceptionIsThrown() {
        stubHandleThrows(new DataAccessResourceFailureException("Test exception"));

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
            .isThrownBy(this::callReceive);

        verify(mockSession, never()).rollback();
        verify(mockMessage, never()).acknowledge();
        verifyMdcReset();
    }

    @SneakyThrows
    private void callReceive() {
        inboundMessageConsumer.receive(mockMessage, mockSession);
    }

    @SneakyThrows
    private void stubHandleResult(boolean shouldAcknowledge) {
        when(inboundMessageHandler.handle(any())).thenReturn(shouldAcknowledge);
    }

    @SneakyThrows
    private void stubHandleThrows(RuntimeException exception) {
        doThrow(exception).when(inboundMessageHandler).handle(mockMessage);
    }

    @SneakyThrows
    private void verifyMessageHandled() {
        verify(inboundMessageHandler).handle(mockMessage);
    }

    private void verifyMdcReset() {
        verify(mdcService).resetAllMdcKeys();
    }
}
