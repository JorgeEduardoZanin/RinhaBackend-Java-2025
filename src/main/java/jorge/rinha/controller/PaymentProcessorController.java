package jorge.rinha.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.service.PaymentProcessorService;

@RestController
@RequestMapping("/payments")
public class PaymentProcessorController {

	private final PaymentProcessorService paymentService;
		
	public PaymentProcessorController(PaymentProcessorService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
    public void payment(@RequestBody PaymentProcessorRequest paymentRequest) {
        paymentService.getInQueue(paymentService.ReqToJsonString(paymentRequest));      
	}

}
