package jorge.rinha.dto.request;

import java.math.BigDecimal;
import java.time.Instant;


public record FullPaymentProcessorRequest(byte[] json, Instant now, String correlationId, BigDecimal amount) {

}
