/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Kevin Gath
 * @since 1.0
 */
public class TaskExecutorBuilderTest {

	@Test
	public void threadNamePrefix_noDashIsAdded() {
		TaskExecutorBuilder builder = new TaskExecutorBuilder();

		ThreadPoolTaskExecutor build = (ThreadPoolTaskExecutor) builder
				.withThreadNamePrefix("testPrefix").build();

		assertThat(build).isNotNull()
				.matches(b -> b.getThreadNamePrefix().equals("testPrefix"));
	}

	@Nested
	class withQueueCapacity {

		@Test
		public void setNegative_assertionFails() {
			TaskExecutorBuilder builder = new TaskExecutorBuilder();

			builder.withQueueCapacity(-1);

			Throwable thrown = Assertions.catchThrowable(builder::build);

			assertThat(thrown).isInstanceOf(IllegalStateException.class)
					.hasMessage("The queue capacity should not be negative");
		}

		@ParameterizedTest
		@ValueSource(ints = { 0, 1, 5, 10 })
		public void isAllowed(int capacity) {
			TaskExecutorBuilder builder = new TaskExecutorBuilder();

			AsyncTaskExecutor build = builder.withQueueCapacity(capacity).build();

			assertThat(build).isNotNull();
		}
	}

	@Nested
	class withSize {

		@ParameterizedTest
		@CsvSource({ "-1, 2", "0, 2", "5, 2", "5, 0", "5, -2", })
		public void assertionFails(int corePoolSize, int maxPoolSize) {
			TaskExecutorBuilder builder = new TaskExecutorBuilder();

			Throwable thrown = Assertions
					.catchThrowable(() -> builder.withSize(corePoolSize, maxPoolSize));

			assertThat(thrown).isInstanceOf(IllegalStateException.class);
		}

		@ParameterizedTest
		@CsvSource({ "1, 2", "1, 1" })
		public void isAllowed(int capacity) {
			TaskExecutorBuilder builder = new TaskExecutorBuilder();

			AsyncTaskExecutor build = builder.withQueueCapacity(capacity).build();

			assertThat(build).isNotNull();
		}
	}
}
