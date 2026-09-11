package uk.nhs.adaptors.gp2gp.common.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MDCService {
    private static final String MDC_CONVERSATION_ID_KEY = "ConversationId";
    private static final String MDC_CORRELATION_ID_KEY = "CorrelationId";
    private static final String MDC_TASK_ID_KEY = "TaskId";
    private static final String MDC_TASK_TYPE_KEY = "TaskType";
    private static final String MDC_MESSAGE_ID_KEY = "MessageId";

    public void applyConversationId(String id) {
        MDC.put(MDC_CONVERSATION_ID_KEY, id);
        MDC.put(MDC_CORRELATION_ID_KEY, id);
    }

    public void applyTaskId(String id) {
        MDC.put(MDC_TASK_ID_KEY, id);
    }

    public void applyTaskType(String taskType) {
        MDC.put(MDC_TASK_TYPE_KEY, taskType);
    }

    public void applyMessageId(String messageId) {
        MDC.put(MDC_MESSAGE_ID_KEY, messageId);
    }

    public void resetAllMdcKeys() {
        MDC.clear();
    }
}
