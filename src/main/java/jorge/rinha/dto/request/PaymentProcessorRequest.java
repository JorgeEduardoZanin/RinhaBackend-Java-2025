package jorge.rinha.dto.request;

import java.math.BigDecimal;


import com.fasterxml.jackson.annotation.JsonProperty;


public record PaymentProcessorRequest(
		@JsonProperty("correlationId") String correlationId, @JsonProperty("amount")  BigDecimal amount) {

}
