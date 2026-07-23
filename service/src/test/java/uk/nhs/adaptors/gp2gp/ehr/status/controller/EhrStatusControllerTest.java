package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrStatusControllerTest {
    private static final String CONVERSATION_ID = "conversation_123";
    public static final int SUCCESS_NO_CONTENT_204 = 204;

    @Mock
    private EhrStatusService ehrStatusService;

    @InjectMocks
    private EhrStatusController controller;

    @Test
    void When_EhrStatusExists_Expect_OkResponseWithStatus() {
        EhrStatus mockStatus = EhrStatus.builder().build();
        when(ehrStatusService.getEhrStatus(CONVERSATION_ID)).thenReturn(Optional.of(mockStatus));

        ResponseEntity<EhrStatus> response = controller.getEhrStatus(CONVERSATION_ID);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(mockStatus, response.getBody());
        verify(ehrStatusService).getEhrStatus(CONVERSATION_ID);
    }

    @Test
    void When_EhrStatusNotFound_Expect_NoContentResponse() {
        when(ehrStatusService.getEhrStatus(CONVERSATION_ID)).thenReturn(Optional.empty());

        ResponseEntity<EhrStatus> response = controller.getEhrStatus(CONVERSATION_ID);

        assertEquals(SUCCESS_NO_CONTENT_204, response.getStatusCode().value());
        verify(ehrStatusService).getEhrStatus(CONVERSATION_ID);
    }

    @Test
    void When_ServiceThrowsException_Expect_ExceptionPropagated() {
        when(ehrStatusService.getEhrStatus(CONVERSATION_ID)).thenThrow(new RuntimeException("Service error"));

        Exception exception = assertThrows(RuntimeException.class, () -> controller.getEhrStatus(CONVERSATION_ID));

        assertEquals("Service error", exception.getMessage());
    }
}

