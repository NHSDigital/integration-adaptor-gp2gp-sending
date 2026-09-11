package uk.nhs.adaptors.mockmhsservice.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MDCService {
    private static final String MDC_CONVERSATION_ID_KEY = "ConversationId";
    private static final String MDC_CORRELATION_ID_KEY = "CorrelationId";
    private static final String MDC_MESSAGE_ID_KEY = "MessageId";

    public void applyConversationId(String id) {
        MDC.put(MDC_CONVERSATION_ID_KEY, id);
        MDC.put(MDC_CORRELATION_ID_KEY, id);
    }

    public void applyMessageId(String id) {
        MDC.put(MDC_MESSAGE_ID_KEY, id);
    }

    public void resetAllMdcKeys() {
        MDC.clear();
    }
}