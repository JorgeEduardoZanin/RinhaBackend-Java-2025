package jorge.rinha.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

	@GetMapping("/")
	  public String hello() {
	    return "Hello from API1!";
	  }
	
}
