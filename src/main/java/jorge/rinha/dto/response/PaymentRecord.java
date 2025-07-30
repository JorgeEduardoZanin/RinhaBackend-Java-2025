package jorge.rinha.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentRecord {
    public final Instant timestamp;
    public final BigDecimal amount;
    public final boolean fallback;

    public PaymentRecord(Instant timestamp, BigDecimal amount, boolean fallback) {
        this.timestamp = timestamp;
        this.amount    = amount;
        this.fallback  = fallback;
    }
}
