package com.example.bank.dao.impl;

import com.example.bank.DBConnectionManager;
import com.example.bank.dao.AccountDao;
import com.example.bank.model.Account;

import java.math.BigDecimal;
import java.sql.*;

public class JdbcAccountDao implements AccountDao {

    @Override
    public int create(Account account) throws Exception {
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, status) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, account.getUserId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getAccountType());
            ps.setBigDecimal(4, account.getBalance() == null ? BigDecimal.ZERO : account.getBalance());
            ps.setString(5, account.getStatus() == null ? "ACTIVE" : account.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Account id not generated");
            }
        }
    }

    @Override
    public Account findById(int id) throws Exception {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    @Override
    public Account findByAccountNumber(String accNo) throws Exception {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    @Override
    public void updateBalance(int accountId, BigDecimal newBalance, Connection conn) throws Exception {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, accountId);
            if (ps.executeUpdate() != 1) throw new SQLException("Failed to update balance");
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId(rs.getInt("account_id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setOpenedAt(rs.getTimestamp("opened_at"));
        a.setStatus(rs.getString("status"));
        return a;
    }
}

