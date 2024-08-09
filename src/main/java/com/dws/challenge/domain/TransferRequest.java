package com.dws.challenge.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransferRequest {
	@NotNull
	@NotEmpty
	private String fromAccountId;
	@NotNull
	@NotEmpty
	private String toAccountId;
	@NotNull
	@Min(value = 0, message = "Amount to transfer should be positive")
	private BigDecimal amount;
}
