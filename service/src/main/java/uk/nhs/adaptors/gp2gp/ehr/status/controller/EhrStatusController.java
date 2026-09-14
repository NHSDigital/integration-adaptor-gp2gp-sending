package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusService;

@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/ehr-status")
public class EhrStatusController {

    private EhrStatusService ehrStatusService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<EhrStatus> getEhrStatus(@PathVariable String conversationId) {
        LOGGER.info("Received request to get EHR status: conversationId={}", conversationId);
        try {
            Optional<EhrStatus> ehrStatusOptional = ehrStatusService.getEhrStatus(conversationId);

            if (ehrStatusOptional.isPresent()) {
                LOGGER.info("EHR status found: conversationId={}", conversationId);
                return ResponseEntity.ok(ehrStatusOptional.get());
            } else {
                LOGGER.warn("EHR status not found: conversationId={}", conversationId);
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving EHR status: conversationId={}", conversationId, e);
            throw e;
        }
    }

}
