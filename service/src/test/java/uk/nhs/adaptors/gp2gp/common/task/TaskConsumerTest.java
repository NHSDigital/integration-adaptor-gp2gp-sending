package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

@ExtendWith(MockitoExtension.class)
class TaskConsumerTest {

    @Mock
    private TaskHandler taskHandler;

    @InjectMocks
    private TaskConsumer taskConsumer;

    @Mock
    private MDCService mdcService;

    @Mock
    private Message message;

    @Mock
    private Session session;

    @Test
    @SneakyThrows
    void When_TaskHandlerReturnsTrue_Expect_MessageAcknowledged() {
        stubHandleResult(true);

        callReceive();

        verifyTaskHandled();
        verify(message, times(1)).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerReturnsFalse_Expect_MessageNotAcknowledged() {
        stubHandleResult(false);

        callReceive();

        verifyTaskHandled();
        verify(message, never()).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerReturnsFalse_Expect_SessionRolledBack() {
        stubHandleResult(false);

        callReceive();

        verifyTaskHandled();
        verify(session, times(1)).rollback();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerThrowsException_Expect_MessageNotAcknowledged() {
        stubHandleThrows(RuntimeException.class);

        callReceive();

        verifyTaskHandled();
        verify(message, never()).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerThrowsException_Expect_SessionRolledBack() {
        stubHandleThrows(RuntimeException.class);

        callReceive();

        verifyTaskHandled();
        verify(session, times(1)).rollback();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerThrowsDataResourceAccessFailureException_Expect_ExceptionIsThrown() {
        stubHandleThrows(DataAccessResourceFailureException.class);

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
            .isThrownBy(this::callReceive);

        verify(session, never()).rollback();
        verify(message, never()).acknowledge();
        verifyMdcReset();
    }

    @Test
    @SneakyThrows
    void When_TaskHandlerThrowsMhsConnectionException_Expect_ExceptionIsThrown() {
        stubHandleThrows(MhsConnectionException.class);

        assertThatExceptionOfType(MhsConnectionException.class)
            .isThrownBy(this::callReceive);

        verify(session, never()).rollback();
        verify(message, never()).acknowledge();
        verifyMdcReset();
    }

    @SneakyThrows
    private void callReceive() {
        taskConsumer.receive(message, session);
    }

    @SneakyThrows
    private void stubHandleResult(boolean result) {
        when(taskHandler.handle(any())).thenReturn(result);
    }

    @SneakyThrows
    private void stubHandleThrows(Class<? extends RuntimeException> exceptionType) {
        doThrow(exceptionType).when(taskHandler).handle(message);
    }

    @SneakyThrows
    private void verifyTaskHandled() {
        verify(taskHandler).handle(message);
    }

    private void verifyMdcReset() {
        verify(mdcService).resetAllMdcKeys();
    }
}
