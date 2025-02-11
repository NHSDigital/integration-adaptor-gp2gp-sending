package uk.nhs.adaptors.gp2gp.common.task;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext
@SuppressWarnings("checkstyle:VisibilityModifier")
public abstract class BaseTaskTest {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final FhirParseService FHIR_PARSE_SERVICE = new FhirParseService();

    @MockitoBean
    protected TaskDispatcher taskDispatcher;
    @MockitoBean
    protected TaskConsumer taskConsumer;
}
