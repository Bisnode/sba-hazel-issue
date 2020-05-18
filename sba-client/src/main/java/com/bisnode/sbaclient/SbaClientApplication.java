package com.bisnode.sbaclient;

import de.codecentric.boot.admin.client.registration.ApplicationRegistrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SbaClientApplication {

	private final ApplicationRegistrator applicationRegistrator;

	@Autowired
	public SbaClientApplication(ApplicationRegistrator applicationRegistrator) {
		this.applicationRegistrator = applicationRegistrator;
	}

	@GetMapping("/register")
	public void register() {
		applicationRegistrator.register();
	}

	@GetMapping("/deregister")
	public void deregister() {
		applicationRegistrator.deregister();
	}

	public static void main(String[] args) {
		SpringApplication.run(SbaClientApplication.class, args);
	}

}
