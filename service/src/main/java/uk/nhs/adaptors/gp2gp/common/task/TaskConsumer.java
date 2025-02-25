package uk.nhs.adaptors.gp2gp.common.task;

import jakarta.jms.Message;
import jakarta.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskConsumer {

    private final TaskHandler taskHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.taskQueueName}", concurrency = "${gp2gp.amqp.taskQueueConsumerConcurrency}",
        containerFactory = "transactedJmsListenerContainerFactory")
    @SneakyThrows
    public void receive(Message message, Session session) {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received taskQueue message_id: {}", messageID);

        try {
            if (taskHandler.handle(message)) {
                message.acknowledge();
                LOGGER.info("Acknowledged taskQueue message_id: {}", messageID);
            } else {
                LOGGER.info("Unable to handle taskQueue message_id: {}", messageID);
                session.rollback();
            }
        } catch (DataAccessResourceFailureException | MhsConnectionException e) {
            LOGGER.trace("Caught {} and re-throwing it for the error handler", e.getClass().getName());
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error while processing taskQueue message_id: {}", messageID, e);
            session.rollback();
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
