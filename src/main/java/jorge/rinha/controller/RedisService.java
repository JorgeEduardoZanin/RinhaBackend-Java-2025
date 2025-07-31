package jorge.rinha.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.repository.RedisRepository;

@RestController
@RequestMapping("/payments-summary")
public class RedisService {

   
    private final RedisRepository redisService;
    
        public RedisService(RedisRepository redisService) {
		this.redisService = redisService;
	}

	@GetMapping
    public PaymentSummaryResponse getSummary(
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to
    ) {
        return redisService.findSummary(from, to);
    }
	
	
	
	
}
	
	

