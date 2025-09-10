package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EhrExtractResourceMapperTest {

    @Mock
    private MessageContext messageContext;

    @Mock
    private InputBundle inputBundleHolder;

    @Mock
    private IdMapper idMapper;

    @InjectMocks
    private NonConsultationResourceMapper resourceMapper;

    @BeforeEach
    public void setUp() {
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundleHolder);
        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(idMapper.hasIdBeenMapped(any(), any())).thenReturn(false);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    void When_ReferencedResourceIsEmpty_Expect_MapMedicationRequest() {

        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId("MedicationRequest/1");
        medRequest.addBasedOn(new Reference("ServiceRequest/123"));
        when(inputBundleHolder.getResource(new IdType("ServiceRequest/123"))).thenReturn(Optional.empty());

        boolean result = resourceMapper.shouldMapResource(medRequest);

        assertTrue(result);
    }

    @Test
    void When_ReferencedResourceHasNotBeenMapped_Expect_MapMedicationRequest() {

        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId("MedicationRequest/1");
        medRequest.addBasedOn(new Reference("ServiceRequest/123"));
        when(inputBundleHolder.getResource(new IdType("ServiceRequest/123"))).thenReturn(
            Optional.ofNullable(new MedicationRequest().setId("111")));

        boolean result = resourceMapper.shouldMapResource(medRequest);

        assertTrue(result);
    }

    @Test
    void When_ReferencedResourceHasBeenMapped_Expect_MedicationRequestIsNotMapped() {

        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId("MedicationRequest/1");
        medRequest.addBasedOn(new Reference("ServiceRequest/123"));
        when(inputBundleHolder.getResource(new IdType("ServiceRequest/123"))).thenReturn(Optional.empty());
        when(idMapper.hasIdBeenMapped(any(), any())).thenReturn(true);

        boolean result = resourceMapper.shouldMapResource(medRequest);

        assertFalse(result);
    }

}
