package jorge.rinha.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentSummaryResponse(@JsonProperty("default") PaymentResponse defaultResponse, PaymentResponse fallback) {
}
