package jorge.rinha.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(int totalRequest, BigDecimal amount) {

}
