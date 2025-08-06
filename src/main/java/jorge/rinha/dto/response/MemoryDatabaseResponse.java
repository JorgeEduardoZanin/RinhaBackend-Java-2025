package jorge.rinha.dto.response;


import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.enums.PaymentType;

public record MemoryDatabaseResponse(FullPaymentProcessorRequest req, PaymentType paymentType) {

}
