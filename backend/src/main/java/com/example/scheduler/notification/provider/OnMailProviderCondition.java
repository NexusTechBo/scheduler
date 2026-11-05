package com.example.scheduler.notification.provider;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

class OnMailProviderCondition extends SpringBootCondition {
    static final String PROPERTY = "app.mail.provider";

    @Override
    public @NonNull ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var attributes = metadata.getAnnotationAttributes(ConditionalOnMailProvider.class.getName());
        var required = attributes != null ? (MailProvider) attributes.get("value") : MailProvider.DEFAULT;
        var raw = context.getEnvironment().getProperty(PROPERTY);
        var active = MailProvider.fromProperty(raw);
        if (active.isPresent() && active.get() == required) {
            return ConditionOutcome.match(PROPERTY + " resolves to " + required);
        }
        return ConditionOutcome.noMatch(PROPERTY + " does not resolve to " + required);
    }
}
