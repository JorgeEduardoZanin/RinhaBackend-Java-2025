package jorge.rinha.dto.request;

import java.math.BigDecimal;

public record PaymentCompleted(BigDecimal amount, PaymentType type) {
    public enum PaymentType {
        DEFAULT, FALLBACK
    }
}
