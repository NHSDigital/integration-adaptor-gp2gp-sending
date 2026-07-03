package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusRequestsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrStatusRequestsControllerTest {

    private static final String CONVERSATION_ID = "conversation_456";
    public static final int SUCCESS_NO_CONTENT_204 = 204;

    @Mock
    private EhrStatusRequestsService ehrRequestsService;

    @InjectMocks
    private EhrStatusRequestsController controller;

    @Test
    void When_EhrRequestsExist_Expect_OkResponseWithRequests() {
        EhrStatusRequest request1 = EhrStatusRequest.builder().conversationId(CONVERSATION_ID).build();
        List<EhrStatusRequest> requests = List.of(request1);
        EhrStatusRequestQuery query = new EhrStatusRequestQuery();

        when(ehrRequestsService.getEhrStatusRequests(query)).thenReturn(Optional.of(requests));

        ResponseEntity<List<EhrStatusRequest>> response = controller.getEhrRequests(query);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(requests, response.getBody());
        assertThat(response.getBody()).hasSize(1);
        verify(ehrRequestsService).getEhrStatusRequests(query);
    }

    @Test
    void When_EhrRequestsNotFound_Expect_NoContentResponse() {
        EhrStatusRequestQuery query = new EhrStatusRequestQuery();
        when(ehrRequestsService.getEhrStatusRequests(query)).thenReturn(Optional.empty());

        ResponseEntity<List<EhrStatusRequest>> response = controller.getEhrRequests(query);

        assertEquals(SUCCESS_NO_CONTENT_204, response.getStatusCode().value());
        verify(ehrRequestsService).getEhrStatusRequests(query);
    }

    @Test
    void When_EhrRequestsExistFormEncoded_Expect_OkResponseWithRequests() {
        EhrStatusRequest request1 = EhrStatusRequest.builder().conversationId(CONVERSATION_ID).build();
        List<EhrStatusRequest> requests = List.of(request1);
        EhrStatusRequestQuery query = new EhrStatusRequestQuery();

        when(ehrRequestsService.getEhrStatusRequests(query)).thenReturn(Optional.of(requests));

        ResponseEntity<List<EhrStatusRequest>> response = controller.getEhrRequestsEncodedForm(query);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(requests, response.getBody());
        assertThat(response.getBody()).hasSize(1);
        verify(ehrRequestsService).getEhrStatusRequests(query);
    }

    @Test
    void When_ServiceThrowsException_Expect_ExceptionPropagated() {
        EhrStatusRequestQuery query = new EhrStatusRequestQuery();
        when(ehrRequestsService.getEhrStatusRequests(query)).thenThrow(new RuntimeException("Service error"));

        var exception = assertThrows(RuntimeException.class, () -> controller.getEhrRequests(query));

        assertEquals("Service error", exception.getMessage());
    }
}

