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

package com.example;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.cloud.contract.verifier.messaging.noop.NoOpStubMessages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {TestConfig.class, Application.class}, properties = "stubrunner.amqp.mockConnection=false")
@Testcontainers
@AutoConfigureMessageVerifier
public abstract class BaseClass {

	
	@Container
	static RabbitMQContainer rabbit = new RabbitMQContainer();

	@DynamicPropertySource
	static void rabbitProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
	}

	@Autowired
	Controller controller;

	public void trigger() {
		
		this.controller.sendFoo("example");
		
	}
}


@Configuration
class TestConfig {

	@Bean
	RabbitMessageVerifier rabbitTemplateMessageVerifier() {
		return new RabbitMessageVerifier();
	}

	@Bean
	ContractVerifierMessaging<Message> rabbitContractVerifierMessaging(RabbitMessageVerifier messageVerifier) {
		return new ContractVerifierMessaging<>(new NoOpStubMessages<>(), messageVerifier) {

			@Override
			protected ContractVerifierMessage convert(Message message) {
				if (message == null) {
					return null;
				}
				return new ContractVerifierMessage(message.getPayload(), message.getHeaders());
			}

		};
	}

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	Exchange myExchange() {
		return new TopicExchange("topic1");
	}

	@Bean
	Queue myQueue() {
		return new Queue("topic1");
	}

	@Bean
	Binding myBinding() {
		return BindingBuilder.bind(myQueue()).to(myExchange())
				.with("#").noargs();
	}

}

class RabbitMessageVerifier implements MessageVerifierReceiver<Message> {

	private static final Logger log = LoggerFactory.getLogger(RabbitMessageVerifier.class);

	private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();

	@Override
	public Message receive(String destination, long timeout, TimeUnit timeUnit, @Nullable YamlContract contract) {
		try {
			return queue.poll(timeout, timeUnit);
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	@RabbitListener(id = "foo", queues = "topic1")
	public void listen(Message message) {
		log.info("Got a message! [{}]", message);
		queue.add(message);
	}

	@Override
	public Message receive(String destination, YamlContract contract) {
		return receive(destination, 1, TimeUnit.SECONDS, contract);
	}

}
