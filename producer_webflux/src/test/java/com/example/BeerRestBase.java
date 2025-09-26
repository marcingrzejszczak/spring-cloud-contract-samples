
package com.example;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// tag::annotations[]
@SpringBootTest(classes = BeerRestBase.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "server.port=0")
public abstract class BeerRestBase {
// end::annotations[]

	@LocalServerPort
	int port;

	@BeforeEach
	public void setup() {
		
		RestAssured.baseURI = "http://localhost:" + this.port;
		
	}

	// tag::config[]
	@Configuration
	@EnableAutoConfiguration
	static class Config {

		@Bean
		PersonCheckingService personCheckingService()  {
			return personToCheck -> personToCheck.age >= 20;
		}

		@Bean
		ProducerController producerController() {
			return new ProducerController(personCheckingService());
		}
	}
	// end::config[]

}
