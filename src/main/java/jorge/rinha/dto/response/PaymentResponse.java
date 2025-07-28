package jorge.rinha.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(BigDecimal totalAmount, int totalRequests) {

}
