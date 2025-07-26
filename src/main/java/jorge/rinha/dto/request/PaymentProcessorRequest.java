package jorge.rinha.dto.request;

import java.math.BigDecimal;

public record PaymentProcessorRequest(String correlationId, BigDecimal amount) {

}
