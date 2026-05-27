package uk.nhs.adaptors.gp2gp.testcontainers;

import org.testcontainers.containers.GenericContainer;

public final class ActiveMqContainer extends GenericContainer<ActiveMqContainer> {

    public static final int ACTIVEMQ_PORT = 5672;
    private static ActiveMqContainer container;

    private ActiveMqContainer() {
        super("docker-activemq:latest");
        addExposedPort(ACTIVEMQ_PORT);
        // withReuse(true) keeps the container alive after the JVM exits so the next
        // test run can reconnect to it instead of starting a new one (~15 s saving).
        // Reuse is only activated when testcontainers.reuse.enable=true in
        // ~/.testcontainers.properties (see .testcontainers.properties.template).
        // In CI that property is absent so containers always start fresh.
        withReuse(true);
    }

    public static ActiveMqContainer getInstance() {
        if (container == null) {
            container = new ActiveMqContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        var containerBrokerUri = "amqp://" + getHost() + ":" + getMappedPort(ACTIVEMQ_PORT);
        System.setProperty("GP2GP_AMQP_BROKERS", containerBrokerUri);
    }
}
