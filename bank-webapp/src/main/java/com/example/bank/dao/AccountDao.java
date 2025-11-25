package com.example.bank.dao;

import com.example.bank.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;

public interface AccountDao {
    int create(Account account) throws Exception;
    Account findById(int id) throws Exception;
    Account findByAccountNumber(String accNo) throws Exception;
    void updateBalance(int accountId, BigDecimal newBalance, Connection conn) throws Exception;
}
