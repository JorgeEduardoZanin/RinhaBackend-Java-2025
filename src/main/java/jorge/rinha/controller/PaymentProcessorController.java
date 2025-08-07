package jorge.rinha.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.service.PaymentProcessorService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payments")
public class PaymentProcessorController {

	private final PaymentProcessorService paymentService;

	public PaymentProcessorController(PaymentProcessorService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(value = HttpStatus.ACCEPTED)
	public Mono<Void> payment(@RequestBody Mono<PaymentProcessorRequest> reqMono) {
		try {
			return reqMono.doOnNext(req -> paymentService.getInQueue(paymentService.ReqToJsonString(req))).then(); 	
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
																																														
	}

}
