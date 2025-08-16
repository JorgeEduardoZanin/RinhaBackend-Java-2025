package jorge.rinha.dto.response;


import java.math.BigDecimal;
import java.time.Instant;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.enums.PaymentType;

public record MemoryDatabaseResponse(Instant now, BigDecimal amount, PaymentType paymentType) {

}
