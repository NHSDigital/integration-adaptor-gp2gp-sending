package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_INCUMBENT;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_NME;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;

@ExtendWith(MockitoExtension.class)
public class EhrStatusServiceTest {

    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";

    private static final EhrExtractStatus COMPLETE_EHR_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().conversationClosed(Instant.now()).build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_NME_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AE").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AE").build())
        .error(EhrExtractStatus.Error.builder().code("18").occurredAt(Instant.now()).build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_INCUMBENT_EXTRACT_STATUS_1 = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                    .code("30")
                    .display("Test Error")
                    .build()))
            .build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_INCUMBENT_EXTRACT_STATUS_2 = EhrExtractStatus.builder()
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                    .code("30")
                    .display("Test Error")
                    .build()))
            .build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus IN_PROGRESS_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    @Mock
    private EhrExtractStatusRepository extractStatusRepository;
    @InjectMocks
    private EhrStatusService ehrStatusService;

    @Test
    public void When_GetEhrStatus_WithCompleteMigration_Expect_CompleteStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(COMPLETE_EHR_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(COMPLETE);

    }

    @Test
    public void When_GetEhrStatus_WithFailedNME_Expect_FailedNameStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_NME_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_NME);
    }

    @Test
    public void When_GetEhrStatus_WithFailedIncumbent_Expect_FailedIncumbentStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_INCUMBENT_EXTRACT_STATUS_1));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    @Test
    public void When_GetEhrStatus_WithFailedIncumbentBeforeContinue_Expect_FailedIncumbentStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_INCUMBENT_EXTRACT_STATUS_2));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    @Test
    public void When_GetEhrStatus_WithInProgress_Expect_InProgressStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(IN_PROGRESS_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    public void When_GetEhrStatus_Expect_AsidCodesArePresent() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(COMPLETE_EHR_EXTRACT_STATUS));

        Optional<EhrStatus> statusOptional = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        EhrStatus status = statusOptional.orElseThrow();

        assertThat(status.getToAsid()).isEqualTo(TO_ASID_CODE);
        assertThat(status.getFromAsid()).isEqualTo(FROM_ASID_CODE);

    }

}
