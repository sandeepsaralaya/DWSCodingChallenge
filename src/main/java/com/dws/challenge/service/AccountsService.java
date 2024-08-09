package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.TranserMoneyValidationException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.util.Constants;
import com.dws.challenge.util.MoneyTransferValidator;
import com.dws.challenge.web.AccountsController;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  
  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService=notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }
  
  /*
   * Method will fetch accounts , validate transfer request and process transfer request 
   * Transfer given amount from fromacoount to toaccount if request is valid 
   * @Parameter transferRequest:TransferRequest
   * @Return void
   * @throws TranserMoneyValidationException if any validation error found
   */
  public void transferMoney(TransferRequest transferRequest) throws TranserMoneyValidationException, Exception
  { 
	  	// lock object reference
	    Account parentLockObject;
		Account childLockObject;
		
		// fetch from and to account from repository
	    Account fromAccount = getAccount(transferRequest.getFromAccountId());
		Account toAccount = getAccount(transferRequest.getToAccountId());
		
		//basic validation on accounts
		MoneyTransferValidator.validateAccounts(fromAccount,toAccount);
		
		// order the accounts , so that we can take ordered lock every time to avoid deadlock
		Account[] ordredeAccounts=compareAcounts(fromAccount,toAccount);
		parentLockObject=ordredeAccounts[0];
		childLockObject=ordredeAccounts[1];
		
		// used for dead lock testing,to test dead lock scenario we can uncomment below line if we r working on single core machine
		// Thread.currentThread().sleep(5L);
		
		
		//acquiring ordered lock on from and to account to avoid dead lock and to process transfer without interruption
		synchronized(parentLockObject)
		{
			log.debug("aquired lovk on parentlockobject : {}",parentLockObject);
			synchronized(childLockObject)
			{
				log.debug("aquired lock on childlockobject : {}",childLockObject);
				//validate Balance in from account, to avoid negative balance
				MoneyTransferValidator.validateSenderBalance(fromAccount,transferRequest.getAmount());
				debitMoney(fromAccount,transferRequest.getAmount());
				creditMoney(toAccount,transferRequest.getAmount());
			}
		}
		
		log.info("Successfully completed transfer request: {}",transferRequest);
   }
	  
	  
    
  /*
   * Method will debit given amount from fromaccount and will notify FromAccount about transfer
   * @parameter fromAccount:Account , amount:BigDecimal
   * @returns void
   */
  private void debitMoney(Account fromAccount ,BigDecimal amount)
  {
	  fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
	  this.notificationService.notifyAboutTransfer(fromAccount,Constants.DEBIT_NOTIFICATION_MESSAGE+amount+" "+Constants.CURRENT_BALANCE+fromAccount.getBalance());
  }
  
  /*
   * Method will credit given amount to toaccount and will notify Toaccount about transfer
   * @parameter toAccount:Account , amount:BigDecimal
   * @returns void
   */
  private void creditMoney(Account toAccount ,BigDecimal amount)
  {
	  toAccount.setBalance(toAccount.getBalance().add(amount));
	  this.notificationService.notifyAboutTransfer(toAccount,Constants.CREDIT_NOTIFICATION_MESSAGE+amount+" "+Constants.CURRENT_BALANCE+toAccount.getBalance());
  }
  
  /*
   * Method will compare and sort given accounts based on their account id and will return sorted array
   * @parameter fromAccount , toAccount
   * @returns sortedAccountArray
   */
  private Account[] compareAcounts(Account fromAccount,Account toAccount)
  {
	  Account[] soretedAccountArr=new Account[2];
	  if(fromAccount.getAccountId().compareTo(toAccount.getAccountId())<0)
		{
		  soretedAccountArr[0]=fromAccount;
		  soretedAccountArr[1]=toAccount;
		}
		else
		{
			soretedAccountArr[0]=toAccount;
			soretedAccountArr[1]=fromAccount;
		}
	  return soretedAccountArr;
  }

}
