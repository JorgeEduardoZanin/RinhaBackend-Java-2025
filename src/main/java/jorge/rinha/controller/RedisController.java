package jorge.rinha.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.service.RedisService;


@RestController
@RequestMapping("/redis")
public class RedisController {

    private final RedisService redisService;;
    
    
	
	 public RedisController(RedisService redisService) {
		this.redisService = redisService;
	}

	    @GetMapping("/get")
	    public ResponseEntity<PaymentSummaryResponse> get() {
	    	var value = redisService.findSummary();
	        
	        return ResponseEntity.ok(value);
	    }
	
}
