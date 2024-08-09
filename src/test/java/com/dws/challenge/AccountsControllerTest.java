package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.service.AccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }
  
  @Test
  void transferMoney() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"1";
	    Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"2";
	    Account toAccountInp = new Account(uniqueToAccountId, new BigDecimal("100"));
	    
	    this.accountsService.createAccount(fromAccountInp);
	    this.accountsService.createAccount(toAccountInp);
	    
	    TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueToAccountId,new BigDecimal("50"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isCreated());

    	Account fromAccount = accountsService.getAccount(uniqueFromAccountId);
    	Account toAccount = accountsService.getAccount(uniqueToAccountId);
    	
    	assertThat(fromAccount.getAccountId()).isEqualTo(uniqueFromAccountId);
    	assertThat(fromAccount.getBalance()).isEqualByComparingTo("50");
    	
    	assertThat(toAccount.getAccountId()).isEqualTo(uniqueToAccountId);
    	assertThat(toAccount.getBalance()).isEqualByComparingTo("150");
    	
  }
  
  @Test
  void transferMoneyInsufficientFund() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"3";
	    Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"4";
	    Account toAccountInp = new Account(uniqueToAccountId, new BigDecimal("100"));
	    
	    this.accountsService.createAccount(fromAccountInp);
	    this.accountsService.createAccount(toAccountInp);
	    
	    TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueToAccountId,new BigDecimal("1000"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyNegativeTransferAmount() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"5";
	    Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"6";
	    Account toAccountInp = new Account(uniqueToAccountId, new BigDecimal("100"));
	    
	    this.accountsService.createAccount(fromAccountInp);
	    this.accountsService.createAccount(toAccountInp);
	    
	    TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueToAccountId,new BigDecimal("-100"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneySameAccount() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"7";
	    Account fromAccountInp = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(fromAccountInp);
	    TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,uniqueFromAccountId,new BigDecimal("100"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyInvalidFromAccount() throws Exception {
	  	String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"8";
	    Account toAccount = new Account(uniqueToAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(toAccount);
	    TransferRequest transferRequestObj=new TransferRequest("00000",uniqueToAccountId,new BigDecimal("100"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyInvalidToAccount() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"9";
	    Account fromAccount = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(fromAccount);
	    TransferRequest transferRequestObj=new TransferRequest(uniqueFromAccountId,"00000",new BigDecimal("100"));
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyNoFromAccount() throws Exception {
	  	String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"10";
	    Account toAccount = new Account(uniqueToAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(toAccount);
	    TransferRequest transferRequestObj=new TransferRequest();
	    transferRequestObj.setAmount(new BigDecimal("100"));
	    transferRequestObj.setToAccountId(uniqueToAccountId);
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyNoToAccount() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"11";
	    Account fromAccount = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(fromAccount);
	    TransferRequest transferRequestObj=new TransferRequest();
	    transferRequestObj.setAmount(new BigDecimal("100"));
	    transferRequestObj.setFromAccountId(uniqueFromAccountId);
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyNoAmount() throws Exception {
	  	String uniqueFromAccountId = "Id-" + System.currentTimeMillis()+"-"+"12";
	    Account fromAccount = new Account(uniqueFromAccountId, new BigDecimal("100"));
	    String uniqueToAccountId = "Id-" + System.currentTimeMillis()+"-"+"13";
	    Account toAccount = new Account(uniqueToAccountId, new BigDecimal("100"));
	    this.accountsService.createAccount(fromAccount);
	    this.accountsService.createAccount(toAccount);
	    TransferRequest transferRequestObj=new TransferRequest();
	    transferRequestObj.setFromAccountId(uniqueFromAccountId);
	    transferRequestObj.setToAccountId(uniqueToAccountId);
	    ObjectMapper mapper = new ObjectMapper();  
	    String tranferObjJsonStr=mapper.writeValueAsString(transferRequestObj);
	    
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
      .content(tranferObjJsonStr)).andExpect(status().isBadRequest());
  }
  
  @Test
  void transferMoneyNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }
  
  
}
