package uk.nhs.adaptors.gp2gp.common.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.common.configuration.WebClientConfiguration;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WebClientFilterServiceTest {

    public static final int TEN_SECONDS = 10;
    public static final int THREE_ATTEMPTS = 3;
    public static final int FIVE_SECONDS = 5;
    public static final int THIRTY_SECONDS = 30;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(WebClientFilterService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Nested
    class SuccessfulResponseLogging {

        @Test
        void shouldLogSuccessWithParameterisedMessage() {

            var filters = new ArrayList<ExchangeFilterFunction>();
            var clientConfig = mockWebClientConfiguration();

            WebClientFilterService.addWebClientFilters(
                filters,
                WebClientFilterService.RequestType.GPC,
                HttpStatus.OK,
                clientConfig
            );

            var composedFilter = filters.stream()
                .reduce(ExchangeFilterFunction::andThen)
                .orElseThrow();

            var clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/test"))
                .build();

            var clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .build();

            ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            composedFilter.filter(clientRequest, exchangeFunction).block();

            List<ILoggingEvent> infoLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .toList();

            assertThat(infoLogs)
                .anyMatch(event -> {
                    String msg = event.getFormattedMessage();
                    return msg.contains("GPC")
                        && msg.contains("request successful")
                        && msg.contains("200");
                });

            assertThat(infoLogs).anyMatch(event -> event.getMessage().contains("{}"));
        }

        @Test
        void shouldLogMhsOutboundRequestTypeOnSuccess() {

            var filters = new ArrayList<ExchangeFilterFunction>();
            var clientConfig = mockWebClientConfiguration();

            WebClientFilterService.addWebClientFilters(
                filters,
                WebClientFilterService.RequestType.MHS_OUTBOUND,
                HttpStatus.ACCEPTED,
                clientConfig
            );

            var composedFilter = filters.stream()
                .reduce(ExchangeFilterFunction::andThen)
                .orElseThrow();

            var clientRequest = ClientRequest.create(HttpMethod.POST, URI.create("http://localhost/mhs"))
                .build();

            var clientResponse = ClientResponse.create(HttpStatus.ACCEPTED).build();

            ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            composedFilter.filter(clientRequest, exchangeFunction).block();

            List<ILoggingEvent> infoLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .toList();

            assertThat(infoLogs)
                .anyMatch(event -> {
                    String msg = event.getFormattedMessage();
                    return msg.contains("MHS_OUTBOUND")
                        && msg.contains("request successful")
                        && msg.contains("202");
                });
        }
    }

    @Nested
    class DebugLogging {

        @Test
        void shouldNotLogRequestResponseHeadersWhenDebugIsDisabled() {

            logger.setLevel(Level.INFO);

            var filters = new ArrayList<ExchangeFilterFunction>();
            var clientConfig = mockWebClientConfiguration();

            WebClientFilterService.addWebClientFilters(
                filters,
                WebClientFilterService.RequestType.GPC,
                HttpStatus.OK,
                clientConfig
            );

            var composedFilter = filters.stream()
                .reduce(ExchangeFilterFunction::andThen)
                .orElseThrow();

            var clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/test"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer secret-token")
                .build();

            var clientResponse = ClientResponse.create(HttpStatus.OK).build();

            ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            composedFilter.filter(clientRequest, exchangeFunction).block();

            List<ILoggingEvent> debugLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.DEBUG)
                .toList();

            assertThat(debugLogs).isEmpty();
        }

        @Test
        void shouldLogRequestResponseHeadersWhenDebugIsEnabled() {

            logger.setLevel(Level.DEBUG);

            var filters = new ArrayList<ExchangeFilterFunction>();
            var clientConfig = mockWebClientConfiguration();

            WebClientFilterService.addWebClientFilters(
                filters,
                WebClientFilterService.RequestType.GPC,
                HttpStatus.OK,
                clientConfig
            );

            var composedFilter = filters.stream()
                .reduce(ExchangeFilterFunction::andThen)
                .orElseThrow();

            var clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/test"))
                .header("X-Custom-Header", "value123")
                .build();

            var clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("X-Response-Header", "resp-value")
                .build();

            ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            composedFilter.filter(clientRequest, exchangeFunction).block();

            List<ILoggingEvent> debugLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.DEBUG)
                .toList();

            assertThat(debugLogs)
                .anyMatch(event -> event.getFormattedMessage().contains("Request:"))
                .anyMatch(event -> event.getFormattedMessage().contains("Response:"));
        }
    }

    @Nested
    class RetryLogging {

        @Test
        void shouldLogRetryWithParameterisedFormatOnFailure() {

            var filters = new ArrayList<ExchangeFilterFunction>();
            var clientConfig = mockWebClientConfiguration(2, 1);

            WebClientFilterService.addWebClientFilters(
                filters,
                WebClientFilterService.RequestType.GPC,
                HttpStatus.OK,
                clientConfig
            );

            var composedFilter = filters.stream()
                .reduce(ExchangeFilterFunction::andThen)
                .orElseThrow();

            var clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/test"))
                .build();

            var serverErrorResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("internal error")
                .build();

            ExchangeFunction exchangeFunction = Mockito.mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(serverErrorResponse));

            try {
                composedFilter.filter(clientRequest, exchangeFunction).block(Duration.ofSeconds(TEN_SECONDS));
            } catch (uk.nhs.adaptors.gp2gp.common.exception.RetryLimitReachedException e) {
                assertThat(e).isNotNull();
            }

            List<ILoggingEvent> retryLogs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> event.getFormattedMessage().contains("retrying request"))
                .toList();

            assertThat(retryLogs).isNotEmpty();
            assertThat(retryLogs).allMatch(event -> event.getMessage().contains("{}"));

            assertThat(retryLogs)
                .anyMatch(event -> {
                    String msg = event.getFormattedMessage();
                    return msg.contains("Request to `GPC` failed")
                        && msg.contains("retrying request");
                });
        }
    }

    private static WebClientConfiguration mockWebClientConfiguration() {
        return mockWebClientConfiguration(THREE_ATTEMPTS, FIVE_SECONDS);
    }

    private static WebClientConfiguration mockWebClientConfiguration(int maxAttempts, int minBackoffSeconds) {
        var config = Mockito.mock(WebClientConfiguration.class);
        when(config.getMaxBackoffAttempts()).thenReturn(maxAttempts);
        when(config.getMinBackOff()).thenReturn(Duration.ofSeconds(minBackoffSeconds));
        when(config.getTimeout()).thenReturn(Duration.ofSeconds(THIRTY_SECONDS));
        return config;
    }
}


