package com.example.scheduler.config;

import com.example.scheduler.exchange.BrevoExchange;
import com.example.scheduler.notification.provider.ConditionalOnMailProvider;
import com.example.scheduler.notification.provider.MailProvider;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@ConditionalOnMailProvider(MailProvider.BREVO)
public class BrevoExchangeConfig {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final String API_KEY_HEADER = "api-key";

    @Bean
    public BrevoExchange brevoExchange(AppMailProperties properties) {
        var httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        var brevo = properties.brevo();
        var builder = RestClient.builder()
                .baseUrl(brevo.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (brevo.hasApiKey()) {
            builder.defaultHeader(API_KEY_HEADER, brevo.apiKey());
        }

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(BrevoExchange.class);
    }
}
