package org.springframework.cloud.aws.context.annotation;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static java.lang.Boolean.TRUE;
import static org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION;

/**
 * @author Anwar Chirakkattil
 */
public class OnAwsEnabledCondition implements ConfigurationCondition {

    private static final String SPRING_CLOUD_AWS_ENABLED = "spring.cloud.aws.enabled";

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return PARSE_CONFIGURATION;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean awsEnabled = Boolean.valueOf(context.getEnvironment().getProperty(SPRING_CLOUD_AWS_ENABLED, "true"));
        return TRUE.equals(awsEnabled);
    }
}
