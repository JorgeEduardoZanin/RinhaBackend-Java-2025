package jorge.rinha.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.service.MemoryDBService;

@RestController
@RequestMapping("/payments-summary")
public class MemoryDBController {

   
    private final MemoryDBService summaryService;
    
        public MemoryDBController(MemoryDBService summaryService) {
		this.summaryService = summaryService;
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
        return summaryService.getSummary(from, to);
    }
}
	
	

