package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TranserMoneyValidationException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.util.Constants;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }
  
  @Test
  void transferMoney() throws TranserMoneyValidationException, Exception {
	  String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"13";
	  Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	  String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"14";
	  Account toAccountInp = new Account(uniqueToAccountId, new BigDecimal("100"));
	  this.accountsService.createAccount(fromAccountInp);
	  this.accountsService.createAccount(toAccountInp);
	  TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueToAccountId,new BigDecimal("50"));
	  this.accountsService.transferMoney(transferRequestObj);
	  assertThat(this.accountsService.getAccount(uniqueFromAccountId).getBalance()).isEqualByComparingTo("50");
	  assertThat(this.accountsService.getAccount(uniqueToAccountId).getBalance()).isEqualByComparingTo("150");
  }
  
  @Test
  void transferMoney_failsOnInsufficientBalance() throws Exception {
	  String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"15";
	  Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	  String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"16";
	  Account toAccountInp = new Account(uniqueToAccountId, new BigDecimal("100"));
	  this.accountsService.createAccount(fromAccountInp);
	  this.accountsService.createAccount(toAccountInp);
	  TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueToAccountId,new BigDecimal("1000"));
	  try
	  {
		  this.accountsService.transferMoney(transferRequestObj);
	  }
	  catch(TranserMoneyValidationException ex)
	  {
		  assertThat(ex.getMessage()).isEqualTo(Constants.INSUFFICIENT_BALANCE_ERROR_MESSAGE);
	  }
  }
  
  @Test
  void transferMoney_failsOnSameToAndFromAccount() throws Exception {
	  String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"17";
	  Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	  this.accountsService.createAccount(fromAccountInp);
	  TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueFromAccountId,new BigDecimal("50"));
	  try
	  {
		  this.accountsService.transferMoney(transferRequestObj);
	  }
	  catch(TranserMoneyValidationException ex)
	  {
		  assertThat(ex.getMessage()).isEqualTo(Constants.SAME_ACCOUNT_TRANSFER_ERROR_MESSAGE);
	  }
  }
  
  @Test
  void transferMoney_failsOnInvalidAccount() throws Exception {
	  String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"18";
	  Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	  this.accountsService.createAccount(fromAccountInp);
	  TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,"00000",new BigDecimal("50"));
	  try
	  {
		  this.accountsService.transferMoney(transferRequestObj);
	  }
	  catch(TranserMoneyValidationException ex)
	  {
		  assertThat(ex.getMessage()).isEqualTo(Constants.INVALID_ACCOUNTS_ERROR_MESSAGE);
	  }
  }
  
  @Test
  void transferMoneyDeadLockScenario() throws InterruptedException, ExecutionException {
	  AccountsService tmpAccountsService=this.accountsService;
	  String uniqueAccountId1 = "Id-" + System.currentTimeMillis()+"-"+"19";
	  Account account1 = new Account(uniqueAccountId1, new BigDecimal("100"));
	  String uniqueAccountId2 = "Id-" + System.currentTimeMillis()+"-"+"20";
	  Account account2 = new Account(uniqueAccountId2, new BigDecimal("70"));
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);
	  
	  TransferRequest transferRequestObj1=new TransferRequest(uniqueAccountId1,uniqueAccountId2,new BigDecimal("40"));
	  TransferRequest transferRequestObj2=new TransferRequest(uniqueAccountId2,uniqueAccountId1,new BigDecimal("30"));
	  
	  ExecutorService executorService=Executors.newFixedThreadPool(2);
	  List<Callable<TransferRequest>> tasks=new ArrayList<>();
	  tasks.add(new Callable<TransferRequest>() {  
          public TransferRequest call() throws Exception {
        	  log.info("Started processing test request {}",transferRequestObj1);
        	  tmpAccountsService.transferMoney(transferRequestObj1);
        	  return transferRequestObj1;
          }  
      });  
	  
	  tasks.add(new Callable<TransferRequest>() { 
          public TransferRequest call() throws Exception {  
        	  log.info("Started processing test request {}",transferRequestObj2);
        	  tmpAccountsService.transferMoney(transferRequestObj2);
        	  return transferRequestObj2;
          }  
      });  
	  List<Future<TransferRequest>> futures = executorService.invokeAll(tasks);
	  for(Future<TransferRequest> future : futures){  
		  TransferRequest req=future.get();
          log.info("completed transferMoneyDeadLockScenario for obj {}",req);  
      }  
	  executorService.shutdown();  
	  
	  assertThat(this.accountsService.getAccount(uniqueAccountId1).getBalance()).isEqualByComparingTo("90");
	  assertThat(this.accountsService.getAccount(uniqueAccountId2).getBalance()).isEqualByComparingTo("80");
  }
  
  @Test
  void transferMoneyParallelDifferentAccountTransaction() throws InterruptedException, ExecutionException {
	  AccountsService tmpAccountsService=this.accountsService;
	  String uniqueAccountId1 = "Id-" + System.currentTimeMillis()+"-"+"21";
	  Account account1 = new Account(uniqueAccountId1, new BigDecimal("100"));
	  String uniqueAccountId2 = "Id-" + System.currentTimeMillis()+"-"+"22";
	  Account account2 = new Account(uniqueAccountId2, new BigDecimal("70"));
	  String uniqueAccountId3 = "Id-" + System.currentTimeMillis()+"-"+"23";
	  Account account3 = new Account(uniqueAccountId3, new BigDecimal("100"));
	  
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);
	  this.accountsService.createAccount(account3);
	  
	  TransferRequest transferRequestObj1=new TransferRequest(uniqueAccountId1,uniqueAccountId2,new BigDecimal("40"));
	  TransferRequest transferRequestObj2=new TransferRequest(uniqueAccountId2,uniqueAccountId3,new BigDecimal("30"));
	  
	  ExecutorService executorService=Executors.newFixedThreadPool(2);
	  List<Callable<TransferRequest>> tasks=new ArrayList<>();
	  tasks.add(new Callable<TransferRequest>() {  
          public TransferRequest call() throws Exception {
        	  log.debug("Started processing test request {}",transferRequestObj1);
        	  tmpAccountsService.transferMoney(transferRequestObj1);
        	  return transferRequestObj1;
          }  
      });  
	  
	  tasks.add(new Callable<TransferRequest>() { 
          public TransferRequest call() throws Exception {  
        	  log.debug("Started processing test request {}",transferRequestObj2);
        	  tmpAccountsService.transferMoney(transferRequestObj2);
        	  return transferRequestObj2;
          }  
      });  
	   
	  List<Future<TransferRequest>> futures = executorService.invokeAll(tasks);
	  for(Future<TransferRequest> future : futures){  
		  TransferRequest req=future.get();
          log.debug("completed transferMoneyDeadLockScenario for obj {}",req);  
      }  
	  executorService.shutdown();  
	  
	  assertThat(this.accountsService.getAccount(uniqueAccountId1).getBalance()).isEqualByComparingTo("60");
	  assertThat(this.accountsService.getAccount(uniqueAccountId2).getBalance()).isEqualByComparingTo("80");
	  assertThat(this.accountsService.getAccount(uniqueAccountId3).getBalance()).isEqualByComparingTo("130");
  }
}
