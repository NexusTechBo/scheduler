package com.example.scheduler.exchange.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrevoEmailRequestTest {
    private static final String HTML = "<a href=\"https://app.example.com/activar?token=tok-secret-123\">Activar</a>";

    @Test
    void shouldMaskRecipientsAndOmitHtmlContentWhenToStringIsCalled() {
        var request = new BrevoEmailRequest(
                new BrevoContact("no-reply@clinicflow.test", "ClinicFlow"),
                List.of(new BrevoContact("patient@example.com", "Juan Lopez")),
                "Activa tu clínica Norte",
                HTML);

        var text = request.toString();

        assertThat(text)
                .contains("sender=no-reply@clinicflow.test")
                .contains("to=[p***@example.com]")
                .contains("subject=Activa tu clínica Norte")
                .contains("htmlContent=<" + HTML.length() + " chars>")
                .doesNotContain("patient@example.com")
                .doesNotContain("Juan Lopez")
                .doesNotContain("tok-secret-123");
    }

    @Test
    void shouldHandleNullFieldsWhenToStringIsCalled() {
        var request = new BrevoEmailRequest(null, null, null, null);

        assertThat(request.toString())
                .isEqualTo("BrevoEmailRequest[sender=null, to=[], subject=, htmlContent=<0 chars>]");
    }

    @Test
    void shouldRenderNullForNullRecipientEntriesWhenToStringIsCalled() {
        var request = new BrevoEmailRequest(null, Arrays.asList(null, new BrevoContact(null, "Name only")), "s", "h");

        assertThat(request.toString()).contains("to=[null, null]").doesNotContain("Name only");
    }
}
