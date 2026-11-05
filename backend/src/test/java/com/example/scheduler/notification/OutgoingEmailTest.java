package com.example.scheduler.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutgoingEmailTest {
    private static final String HTML = "<p>Usuario: ana.perez</p><p>Contraseña: S3cret!pw</p>";

    @Test
    void shouldMaskRecipientAndOmitHtmlBodyWhenToStringIsCalled() {
        var email = new OutgoingEmail("patient@example.com", "Tu cuenta en Norte", HTML);

        var text = email.toString();

        assertThat(text)
                .contains("to=p***@example.com")
                .contains("subject=Tu cuenta en Norte")
                .contains("html=<" + HTML.length() + " chars>")
                .doesNotContain("patient@example.com")
                .doesNotContain("S3cret!pw")
                .doesNotContain("<p>");
    }

    @Test
    void shouldMaskEmailsAndStripLineBreaksInSubjectWhenToStringIsCalled() {
        var email = new OutgoingEmail("a@b.com", "Hola juan.lopez@example.com\r\nBcc: x", "<p>x</p>");

        assertThat(email.toString())
                .doesNotContain("juan.lopez@example.com")
                .doesNotContain("\n")
                .doesNotContain("\r")
                .contains("j***@example.com");
    }

    @Test
    void shouldHandleNullFieldsWhenToStringIsCalled() {
        var email = new OutgoingEmail(null, null, null);

        assertThat(email.toString()).isEqualTo("OutgoingEmail[to=null, subject=, html=<0 chars>]");
    }

    @Test
    void shouldFullyMaskRecipientWithoutAtSignWhenToStringIsCalled() {
        assertThat(new OutgoingEmail("not-an-email", "s", "h").toString())
                .contains("to=***")
                .doesNotContain("not-an-email");
    }
}
