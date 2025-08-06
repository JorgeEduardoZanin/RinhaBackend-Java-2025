package jorge.rinha.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.service.MemoryDBService;

@RestController
@RequestMapping("/payments-summary")
public class MemoryDBController {
	
	@Value("${url.instance}")
	private String value222;

    private final MemoryDBService memoryDbService;
    
	public MemoryDBController(MemoryDBService memoryDbService) {

		this.memoryDbService = memoryDbService;
	}


	@GetMapping
    public PaymentSummaryResponse getSummary(@RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)Instant to
    ) {
		System.out.println(value222);
        return memoryDbService.findSummary(from, to);
    }
	
	@GetMapping("/internal")
    public PaymentSummaryResponse internalSummary(
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
		System.out.println("VIADODADOR DE CUUUUUUUUUUUUUUUUUUUUUUUUUUU");
        return memoryDbService.findInternal(from, to);
    }
	
	
	 @DeleteMapping
	    public void clearAll() {
		
	    }
	
}
	
	
