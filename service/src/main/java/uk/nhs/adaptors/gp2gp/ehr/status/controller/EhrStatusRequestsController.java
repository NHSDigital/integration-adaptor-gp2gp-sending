package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusRequestsService;

@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/requests")
public class EhrStatusRequestsController {

    private EhrStatusRequestsService ehrRequestsService;

    @PostMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EhrStatusRequest>> getEhrRequestsEncodedForm(EhrStatusRequestQuery request) {
        LOGGER.info("Received form-encoded request for EHR status requests");
        try {
            Optional<List<EhrStatusRequest>> ehrRequestOptional = ehrRequestsService.getEhrStatusRequests(request);

            if (ehrRequestOptional.isPresent()) {
                List<EhrStatusRequest> requests = ehrRequestOptional.get();
                LOGGER.info("Found {} EHR status requests", requests.size());
                return ResponseEntity.ok(requests);
            } else {
                LOGGER.warn("No EHR status requests found");
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving EHR status requests (form-encoded)", e);
            throw e;
        }
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EhrStatusRequest>> getEhrRequests(@RequestBody EhrStatusRequestQuery request) {
        LOGGER.info("Received JSON request for EHR status requests");
        try {
            Optional<List<EhrStatusRequest>> ehrRequestOptional = ehrRequestsService.getEhrStatusRequests(request);

            if (ehrRequestOptional.isPresent()) {
                List<EhrStatusRequest> requests = ehrRequestOptional.get();
                LOGGER.info("Found {} EHR status requests", requests.size());
                return ResponseEntity.ok(requests);
            } else {
                LOGGER.warn("No EHR status requests found");
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving EHR status requests (JSON)", e);
            throw e;
        }
    }

}