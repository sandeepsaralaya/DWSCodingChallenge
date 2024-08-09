package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TranserMoneyValidationException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.util.Constants;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable("accountId") String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }
  
  
  @PostMapping(path = "/transfer",consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferMoney(@Valid @RequestBody TransferRequest transferRequest) {
    log.info("Recieved transfer request {}", transferRequest);
    try 
    {
    	this.accountsService.transferMoney(transferRequest);
    } 
    catch (TranserMoneyValidationException validationException) 
    {
      log.error("Caught TranserMoneyValidationException exception for transferMoney request {}",validationException.getMessage());
      return new ResponseEntity<>(validationException.getMessage(), HttpStatus.BAD_REQUEST);
    }
    catch (Exception e)
    {
    	log.error("Caught Exception  for transferMoney request {}",e.getMessage());
      return new ResponseEntity<>(Constants.INTERNAL_SERVER_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return new ResponseEntity<>(Constants.TRANSACTION_SUCCESSFULL_MESSAGE,HttpStatus.CREATED);
  }
}
