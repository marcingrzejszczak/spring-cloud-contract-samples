package com.example.rest;

import com.example.security.UserDetails;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class ProducerController {

	private final PersonCheckingService personCheckingService;

	public ProducerController(PersonCheckingService personCheckingService) {
		this.personCheckingService = personCheckingService;
	}

	@PostMapping(path = "/check", produces = APPLICATION_JSON_VALUE)
	public Response check(Authentication authentication) {
		
		if (this.personCheckingService
				.shouldGetBeer(currentUserDetails(authentication))) {
			return new Response(BeerCheckStatus.OK);
		}
		return new Response(BeerCheckStatus.NOT_OK);
		
	}

	/**
	 * <p>
	 * Method to receive current user details.
	 * </p>
	 * @param authentication authentication token information
	 * @return UserDetails
	 */
	private UserDetails currentUserDetails(Authentication authentication) {
		if (authentication == null) {
			return null;
		}
		return (UserDetails) authentication.getPrincipal();
	}

}
