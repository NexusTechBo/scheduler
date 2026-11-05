package com.example.scheduler.notification.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.scheduler.config.AppMailProperties;
import com.example.scheduler.exception.MailDeliveryException;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.provider.MailProvider;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpMailTransportTest {
    private static final String FROM = "no-reply@clinicflow.test";
    private static final String FROM_NAME = "Clínica Norte";
    private static final OutgoingEmail EMAIL =
            new OutgoingEmail("patient@example.com", "Cita confirmada - Norte", "<p>Hola <strong>Juan</strong></p>");

    @Mock
    private JavaMailSender mailSender;

    private MimeMessage message;
    private SmtpMailTransport transport;

    @BeforeEach
    void setUp() {
        message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        transport = new SmtpMailTransport(
                mailSender,
                new AppMailProperties(MailProvider.SMTP, FROM, FROM_NAME, new AppMailProperties.Brevo("unused", "")));
    }

    @Test
    void shouldSendHtmlMessageWithSenderRecipientAndSubjectWhenSending() throws Exception {
        transport.send(EMAIL);

        verify(mailSender).send(message);
        var from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo(FROM);
        assertThat(from.getPersonal()).isEqualTo(FROM_NAME);
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(address -> ((InternetAddress) address).getAddress())
                .containsExactly("patient@example.com");
        assertThat(message.getSubject()).isEqualTo(EMAIL.subject());
        assertThat(message.getContent()).isEqualTo(EMAIL.html());
        assertThat(message.getDataHandler().getContentType())
                .startsWith("text/html")
                .containsIgnoringCase("utf-8");
    }

    @Test
    void shouldWrapMessagingExceptionAndNotSendWhenRecipientIsInvalid() {
        var invalid = new OutgoingEmail("first@example.com, second@example.com", "s", "<p>x</p>");

        var thrown = catchThrowable(() -> transport.send(invalid));

        assertThat(thrown)
                .isInstanceOf(MailDeliveryException.class)
                .hasMessageStartingWith("SMTP message could not be built")
                .hasCauseInstanceOf(MessagingException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldPropagateSpringMailExceptionUnchangedWhenSendFails() {
        var failure = new MailSendException("connection refused");
        doThrow(failure).when(mailSender).send(any(MimeMessage.class));

        var thrown = catchThrowable(() -> transport.send(EMAIL));

        assertThat(thrown).isSameAs(failure);
    }
}
