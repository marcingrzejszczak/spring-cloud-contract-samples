/*
 * Copyright 2018-2019 the original author or authors.
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

package com.example;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nullable;
import org.assertj.core.api.BDDAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {TestConfig.class, Application.class}, properties = "stubrunner.amqp.mockConnection=false")
@AutoConfigureStubRunner(ids = "com.example:beer-api-producer-rabbit-middleware", stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Testcontainers
@ActiveProfiles("test")
public class ApplicationTests {


	@Container
	static RabbitMQContainer rabbit = new RabbitMQContainer();

	@DynamicPropertySource
	static void rabbitProperties(DynamicPropertyRegistry registry) {
		rabbit.start();
		registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
	}

	@Autowired
	StubTrigger trigger;
	@Autowired
	Application application;

	@Test
	public void contextLoads() {
		this.trigger.trigger("trigger");

		Awaitility.await().untilAsserted(() -> {
			BDDAssertions.then(this.application.storedFoo).isNotNull();
			BDDAssertions.then(this.application.storedFoo.getFoo()).isEqualTo("example");
		});
	}
}


@Configuration
class TestConfig {

	private static final Logger log = LoggerFactory.getLogger(TestConfig.class);

	@Bean
	MessageVerifierSender<org.springframework.messaging.Message<?>> testMessageVerifier(RabbitTemplate rabbitTemplate) {
		return new MessageVerifierSender<>() {

			@Override
			public void send(org.springframework.messaging.Message<?> message, String destination, @Nullable YamlContract contract) {
				log.info("Sending a message to destination [{}]", destination);
				rabbitTemplate.send(destination, toMessage(message));
			}

			@Override
			public <T> void send(T payload, Map<String, Object> headers, String destination, @Nullable YamlContract contract) {
				log.info("Sending a message to destination [{}]", destination);
				send(org.springframework.messaging.support.MessageBuilder.withPayload(payload).copyHeaders(headers)
						.build(), destination, contract);
			}

			private Message toMessage(org.springframework.messaging.Message<?> msg) {
				Object payload = msg.getPayload();
				MessageHeaders headers = msg.getHeaders();
				Map<String, Object> newHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
				MessageProperties messageProperties = new MessageProperties();
				newHeaders.forEach(messageProperties::setHeader);
				if (payload instanceof String) {
					String json = (String) payload;
					Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
							.andProperties(messageProperties).build();
					return message;
				}
				else {
					throw new IllegalStateException("Payload is not a String");
				}
			}
		};

	}
}
