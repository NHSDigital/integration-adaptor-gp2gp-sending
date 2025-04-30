package uk.nhs.adaptors.gp2gp.common.amqp;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsTextMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JmsReader {

    public static String readMessage(Message message) throws JMSException {

        if (message instanceof JmsTextMessage jmsTextMessage) {
            return readTextMessage(jmsTextMessage);
        }

        if (message instanceof JmsBytesMessage jmsBytesMessage) {
            return readBytesMessage(jmsBytesMessage);
        }

        if (message != null) {
            return message.getBody(String.class);
        }
        return null;
    }

    private static String readBytesMessage(JmsBytesMessage message) throws JMSException {
        byte[] bytes = new byte[(int) message.getBodyLength()];
        message.readBytes(bytes);
        return new String(bytes, UTF_8);
    }

    private static String readTextMessage(JmsTextMessage message) throws JMSException {
        return message.getText();
    }
}
