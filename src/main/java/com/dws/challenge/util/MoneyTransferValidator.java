package com.dws.challenge.util;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.TranserMoneyValidationException;

public class MoneyTransferValidator {
	
	// method checks fromaccount has sufficient balance for debit
	public static void validateSenderBalance(Account fromAccount,BigDecimal amount)
	{
		if(fromAccount.getBalance().compareTo(amount)<0)
			throw new TranserMoneyValidationException(Constants.INSUFFICIENT_FUNDS_ERROR_MESSAGE);
	}
	
	// method validates accounts, if they are same or null thows TranserMoneyValidationException
	public static void validateAccounts(Account fromAccount,Account toAccount ) throws TranserMoneyValidationException
	{
		if(fromAccount==null || toAccount==null)
			throw new TranserMoneyValidationException(Constants.INVALID_ACCOUNTS_ERROR_MESSAGE);
		if(fromAccount==toAccount)
			throw new TranserMoneyValidationException(Constants.SAME_ACCOUNT_TRANSFER_ERROR_MESSAGE);
	}
}
