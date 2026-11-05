package com.example.scheduler.notification.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.example.scheduler.config.AppMailProperties;
import com.example.scheduler.exception.MailDeliveryException;
import com.example.scheduler.exchange.BrevoExchange;
import com.example.scheduler.exchange.contract.BrevoContact;
import com.example.scheduler.exchange.contract.BrevoEmailRequest;
import com.example.scheduler.exchange.contract.BrevoEmailResponse;
import com.example.scheduler.exchange.contract.BrevoErrorResponse;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.provider.MailProvider;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class BrevoMailTransportTest {
    private static final String API_KEY = "xkeysib-super-secret-key-123";
    private static final String FROM = "no-reply@clinicflow.test";
    private static final String FROM_NAME = "ClinicFlow";
    private static final String BASE_URL = "https://api.brevo.test";
    private static final OutgoingEmail EMAIL =
            new OutgoingEmail("patient@example.com", "Cita confirmada - Norte", "<p>Hola</p>");
    private static final String UNAUTHORIZED_BODY = "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}";

    private static AppMailProperties properties(String apiKey) {
        return properties(FROM, apiKey);
    }

    private static AppMailProperties properties(String from, String apiKey) {
        return new AppMailProperties(
                MailProvider.BREVO, from, FROM_NAME, new AppMailProperties.Brevo(BASE_URL, apiKey));
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class WithMockedExchange {
        @Mock
        private BrevoExchange brevoExchange;

        private BrevoMailTransport transport;

        @BeforeEach
        void setUp() {
            transport = new BrevoMailTransport(brevoExchange, properties(API_KEY));
        }

        @Test
        void shouldSendRequestWithSenderSingleRecipientSubjectAndHtmlWhenSending() {
            when(brevoExchange.sendEmail(any())).thenReturn(new BrevoEmailResponse("<id@smtp-relay>"));

            transport.send(EMAIL);

            var captor = ArgumentCaptor.forClass(BrevoEmailRequest.class);
            verify(brevoExchange).sendEmail(captor.capture());
            var request = captor.getValue();
            assertThat(request.sender()).isEqualTo(new BrevoContact(FROM, FROM_NAME));
            assertThat(request.to()).containsExactly(new BrevoContact("patient@example.com", null));
            assertThat(request.subject()).isEqualTo(EMAIL.subject());
            assertThat(request.htmlContent()).isEqualTo(EMAIL.html());
        }

        @Test
        void shouldNotFailWhenBrevoReturnsNoBody() {
            when(brevoExchange.sendEmail(any())).thenReturn(null);

            assertThatCode(() -> transport.send(EMAIL)).doesNotThrowAnyException();
        }

        @Test
        void shouldRethrowAsMailDeliveryExceptionWithBrevoMessageWhenBodyIsJsonError() {
            var error = clientError(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
            error.setBodyConvertFunction(
                    convertTo(BrevoErrorResponse.class, new BrevoErrorResponse("unauthorized", "Key not found")));
            when(brevoExchange.sendEmail(any())).thenThrow(error);

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage())
                    .isEqualTo("Brevo responded 401: Key not found")
                    .doesNotContain(API_KEY);
            assertThat(thrown.getCause()).isSameAs(error);
        }

        @Test
        void shouldFallBackToRawBodyWhenBodyIsNotJson() {
            var error = new HttpServerErrorException(
                    HttpStatus.BAD_GATEWAY,
                    "Bad Gateway",
                    new HttpHeaders(),
                    "<html>upstream down</html>".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
            error.setBodyConvertFunction(_ -> {
                throw new IllegalStateException("not JSON");
            });
            when(brevoExchange.sendEmail(any())).thenThrow(error);

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage())
                    .isEqualTo("Brevo responded 502: <html>upstream down</html>")
                    .doesNotContain(API_KEY);
        }

        @Test
        void shouldFallBackToRawBodyWhenNoBodyConverterIsSet() {
            when(brevoExchange.sendEmail(any())).thenThrow(clientError(HttpStatus.BAD_REQUEST, "plain text error"));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage()).isEqualTo("Brevo responded 400: plain text error");
        }

        @Test
        void shouldFallBackToStatusTextWhenBodyIsEmpty() {
            when(brevoExchange.sendEmail(any())).thenThrow(clientError(HttpStatus.FORBIDDEN, ""));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage()).isEqualTo("Brevo responded 403: Forbidden");
        }

        @Test
        void shouldTruncateErrorDetailWhenBodyIsLongerThanLimit() {
            var longBody = "x".repeat(500);
            when(brevoExchange.sendEmail(any())).thenThrow(clientError(HttpStatus.BAD_REQUEST, longBody));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage()).isEqualTo("Brevo responded 400: " + "x".repeat(300) + "...");
        }

        @Test
        void shouldWrapAsUnreachableWhenNonHttpRestClientExceptionIsThrown() {
            var ioFailure = new ResourceAccessException("I/O error on POST request: connect timed out");
            when(brevoExchange.sendEmail(any())).thenThrow(ioFailure);

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage()).isEqualTo("Brevo unreachable: ResourceAccessException");
            assertThat(thrown.getCause()).isSameAs(ioFailure);
        }

        @Test
        void shouldWrapAsUnreachableWhenGenericRestClientExceptionIsThrown() {
            var failure = new RestClientException("could not extract response");
            when(brevoExchange.sendEmail(any())).thenThrow(failure);

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage()).isEqualTo("Brevo unreachable: RestClientException");
            assertThat(thrown.getCause()).isSameAs(failure);
        }

        @Test
        void shouldPropagateOtherRuntimeExceptionsUnchanged() {
            var failure = new IllegalStateException("unexpected");
            when(brevoExchange.sendEmail(any())).thenThrow(failure);

            var thrown = catchThrowableOfType(RuntimeException.class, () -> transport.send(EMAIL));

            assertThat(thrown).isSameAs(failure);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class MissingConfiguration {
        @Mock
        private BrevoExchange brevoExchange;

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldThrowWithoutCallingBrevoWhenApiKeyIsBlank(String apiKey) {
            var transport = new BrevoMailTransport(brevoExchange, properties(FROM, apiKey));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown).hasMessage("Brevo API key not configured").hasNoCause();
            verifyNoInteractions(brevoExchange);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldThrowWithoutCallingBrevoWhenFromIsBlank(String from) {
            var transport = new BrevoMailTransport(brevoExchange, properties(from, API_KEY));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown)
                    .hasMessage("Mail sender (MAIL_FROM) not configured")
                    .hasNoCause();
            verifyNoInteractions(brevoExchange);
        }

        @Test
        void shouldReportApiKeyFirstWhenBothApiKeyAndFromAreBlank() {
            var transport = new BrevoMailTransport(brevoExchange, properties("", ""));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown).hasMessage("Brevo API key not configured");
            verifyNoInteractions(brevoExchange);
        }
    }

    @Nested
    @ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
    class Construction {
        @Mock
        private BrevoExchange brevoExchange;

        @Test
        void shouldWarnWithoutThrowingWhenApiKeyIsBlank(CapturedOutput output) {
            assertThatCode(() -> new BrevoMailTransport(brevoExchange, properties("  ")))
                    .doesNotThrowAnyException();

            assertThat(output.getAll()).contains("Brevo API key is not configured");
        }

        @Test
        void shouldWarnWithoutThrowingWhenFromIsBlank(CapturedOutput output) {
            assertThatCode(() -> new BrevoMailTransport(brevoExchange, properties("", API_KEY)))
                    .doesNotThrowAnyException();

            assertThat(output.getAll()).contains("Mail sender is not configured");
        }

        @Test
        void shouldNotWarnWhenApiKeyAndFromArePresent(CapturedOutput output) {
            new BrevoMailTransport(brevoExchange, properties(API_KEY));

            assertThat(output.getAll())
                    .doesNotContain("Brevo API key is not configured")
                    .doesNotContain("Mail sender is not configured")
                    .doesNotContain(API_KEY);
        }
    }

    /**
     * Exercises the real {@link BrevoExchange} HTTP interface and Jackson conversion against a mock server. The
     * client is built like {@code BrevoExchangeConfig} (base URL and api-key default header), but the config's own
     * JDK request factory cannot be bound to {@link MockRestServiceServer}.
     */
    @Nested
    class OverHttp {
        private MockRestServiceServer server;
        private BrevoMailTransport transport;

        @BeforeEach
        void setUp() {
            var builder = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("api-key", API_KEY)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            server = MockRestServiceServer.bindTo(builder).build();
            var exchange = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                    .build()
                    .createClient(BrevoExchange.class);
            transport = new BrevoMailTransport(exchange, properties(API_KEY));
        }

        @Test
        void shouldPostJsonToSmtpEmailEndpointWhenSending() {
            server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("api-key", API_KEY))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sender.email").value(FROM))
                    .andExpect(jsonPath("$.sender.name").value(FROM_NAME))
                    .andExpect(jsonPath("$.to.length()").value(1))
                    .andExpect(jsonPath("$.to[0].email").value("patient@example.com"))
                    .andExpect(jsonPath("$.to[0].name").doesNotExist())
                    .andExpect(jsonPath("$.subject").value(EMAIL.subject()))
                    .andExpect(jsonPath("$.htmlContent").value(EMAIL.html()))
                    .andRespond(withStatus(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"messageId\":\"<id@smtp-relay>\"}"));

            transport.send(EMAIL);

            server.verify();
        }

        @Test
        void shouldExtractBrevoMessageWithoutLeakingApiKeyWhenUnauthorized() {
            server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                    .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(UNAUTHORIZED_BODY));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage())
                    .isEqualTo("Brevo responded 401: Key not found")
                    .doesNotContain(API_KEY);
            assertThat(thrown.getCause()).isInstanceOf(RestClientResponseException.class);
        }

        @Test
        void shouldUseRawBodyWhenErrorBodyIsNotJson() {
            server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Service temporarily unavailable"));

            var thrown = catchThrowableOfType(MailDeliveryException.class, () -> transport.send(EMAIL));

            assertThat(thrown.getMessage())
                    .isEqualTo("Brevo responded 503: Service temporarily unavailable")
                    .doesNotContain(API_KEY);
        }
    }

    private static HttpClientErrorException clientError(HttpStatus status, String body) {
        return HttpClientErrorException.create(
                status,
                status.getReasonPhrase(),
                new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    private static Function<ResolvableType, Object> convertTo(Class<?> type, Object value) {
        return requested -> {
            if (requested.toClass().equals(type)) return value;
            throw new IllegalStateException("Unexpected type " + requested);
        };
    }
}
