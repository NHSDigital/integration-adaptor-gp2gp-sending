package uk.nhs.adaptors.gp2gp.ehr.status;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.status.controller.EhrStatusController;
import uk.nhs.adaptors.gp2gp.ehr.status.controller.EhrStatusRequestsController;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusRequestsService;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusService;

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableMongoRepositories(basePackageClasses = EhrExtractStatusRepository.class)
@Import({
    EhrStatusController.class,
    EhrStatusRequestsController.class,
    EhrStatusService.class,
    EhrStatusRequestsService.class
})
class EhrStatusEndpointsTestApplication {
}

