/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.aws.actuate.health.SqsListenerHealthIndicator;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSns;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SQS and SNS messaging.
 * <p>
 * This configuration class is active only when the Spring Cloud AWS Messaging library
 * is on the classpath.
 * <p>
 * Registers the following beans:
 * <ul>
 * <li>all beans registered with {@link EnableSqs} if there
 * is no other bean of type {@link SimpleMessageListenerContainer} in the context.</li>
 * <li>{@link SqsListenerHealthIndicator} instance if Spring Cloud AWS Actuator is on the classpath
 * and there is no other bean of the same type in the context.</li>
 * <li>all beans registered with {@link EnableSns} if there
 * is {@link com.amazonaws.services.sns.AmazonSNS AmazonSNS} on the classpath</li>
 * </ul>
 * <p>
 *
 * @author Alain Sahli
 * @author Agim Emruli
 * @author Maciej Walkowiak
 */
@ConditionalOnClass(name = "org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer")
@Configuration
public class MessagingAutoConfiguration {

    @ConditionalOnMissingBean(type = "org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer")
    @EnableSqs
    @Configuration
    public static class SqsAutoConfiguration {
    }


    @ConditionalOnMissingBean(SqsListenerHealthIndicator.class)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @Configuration
    public static class SqsHealthAutoConfiguration {

        @Bean
        SqsListenerHealthIndicator sqsListenerHealthIndicator(SimpleMessageListenerContainer container, AmazonSQS amazonSQS) {
            return new SqsListenerHealthIndicator(container, amazonSQS);
        }
    }

    @ConditionalOnClass(name = "com.amazonaws.services.sns.AmazonSNS")
    @EnableSns
    @Configuration
    public static class SnsAutoConfiguration {

    }
}
