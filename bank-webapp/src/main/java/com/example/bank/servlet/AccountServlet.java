package com.example.bank.servlet;

import com.example.bank.DBConnectionManager;
import com.example.bank.dao.AccountDao;
import com.example.bank.dao.UserDao;
import com.example.bank.dao.impl.JdbcAccountDao;
import com.example.bank.dao.impl.JdbcUserDao;
import com.example.bank.model.Account;
import com.example.bank.model.User;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/api/accounts/*")
public class AccountServlet extends HttpServlet {

    private final AccountDao accountDao = new JdbcAccountDao();
    private final UserDao userDao = new JdbcUserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo(); // /create or /{id}/deposit etc
        resp.setContentType("application/json");

        try {
            String body = req.getReader().lines().reduce("", (a, b) -> a + b);
            JsonReader jr = Json.createReader(new StringReader(body));
            JsonObject jo = jr.readObject();

            if (path != null && path.equals("/create")) {
                createAccount(jo, resp);
                return;
            }

            if (path != null && path.matches("/\\d+/deposit")) {
                int id = Integer.parseInt(path.split("/")[1]);
                BigDecimal amount =
                        new BigDecimal(jo.getJsonNumber("amount").toString());
                deposit(id, amount, resp);
                return;
            }

            if (path != null && path.matches("/\\d+/withdraw")) {
                int id = Integer.parseInt(path.split("/")[1]);
                BigDecimal amount =
                        new BigDecimal(jo.getJsonNumber("amount").toString());
                withdraw(id, amount, resp);
                return;
            }

            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"Unknown account endpoint\"}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter()
                .write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // GET /api/accounts/{id}
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (path != null && path.matches("/\\d+")) {
                int id = Integer.parseInt(path.substring(1));
                Account account = accountDao.findById(id);

                if (account == null) {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"error\":\"Account not found\"}");
                    return;
                }

                JsonObject jo = Json.createObjectBuilder()
                        .add("accountId", account.getAccountId())
                        .add("accountNumber", account.getAccountNumber())
                        .add("accountType", account.getAccountType())
                        .add("balance", account.getBalance().toString())
                        .build();

                resp.getWriter().write(jo.toString());
                return;
            }

            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"Unknown endpoint\"}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter()
                .write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void createAccount(JsonObject jo, HttpServletResponse resp)
            throws Exception {

        int userId = jo.getInt("userId", 0);
        String accountType = jo.getString("accountType", "Savings");
        BigDecimal initialDeposit = BigDecimal.ZERO;

        if (jo.containsKey("initialDeposit")) {
            initialDeposit =
                    new BigDecimal(jo.getJsonNumber("initialDeposit").toString());
        }

        User u = userDao.findById(userId);
        if (u == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"User not found\"}");
            return;
        }

        Account a = new Account();
        a.setUserId(userId);
        a.setAccountType(accountType);
        a.setBalance(initialDeposit);
        a.setAccountNumber(generateAccountNumber(userId));

        int accountId = accountDao.create(a);

        JsonObject out = Json.createObjectBuilder()
                .add("accountId", accountId)
                .add("accountNumber", a.getAccountNumber())
                .add("balance", a.getBalance().toString())
                .build();

        resp.setStatus(201);
        resp.getWriter().write(out.toString());
    }

    private void deposit(int accountId, BigDecimal amount, HttpServletResponse resp)
            throws Exception {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Amount must be positive\"}");
            return;
        }

        try (Connection conn = DBConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            Account a = accountDao.findById(accountId);
            if (a == null) {
                resp.setStatus(404);
                conn.rollback();
                resp.getWriter().write("{\"error\":\"Account not found\"}");
                return;
            }

            BigDecimal newBal = a.getBalance().add(amount);
            accountDao.updateBalance(accountId, newBal, conn);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions " +
                    "(from_account_id,to_account_id,amount,txn_type,description," +
                    " post_balance,status) VALUES (?,?,?,?,?,?,?)")) {

                ps.setObject(1, null);
                ps.setInt(2, accountId);
                ps.setBigDecimal(3, amount);
                ps.setString(4, "DEPOSIT");
                ps.setString(5, "Deposit via web");
                ps.setBigDecimal(6, newBal);
                ps.setString(7, "SUCCESS");
                ps.executeUpdate();
            }

            conn.commit();

            JsonObject out = Json.createObjectBuilder()
                    .add("accountId", accountId)
                    .add("balance", newBal.toString())
                    .build();

            resp.getWriter().write(out.toString());
        }
    }

    private void withdraw(int accountId, BigDecimal amount,
                          HttpServletResponse resp) throws Exception {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Amount must be positive\"}");
            return;
        }

        try (Connection conn = DBConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            Account a = accountDao.findById(accountId);
            if (a == null) {
                resp.setStatus(404);
                conn.rollback();
                resp.getWriter().write("{\"error\":\"Account not found\"}");
                return;
            }

            if (a.getBalance().compareTo(amount) < 0) {
                resp.setStatus(400);
                conn.rollback();
                resp.getWriter().write("{\"error\":\"Insufficient funds\"}");
                return;
            }

            BigDecimal newBal = a.getBalance().subtract(amount);
            accountDao.updateBalance(accountId, newBal, conn);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions " +
                    "(from_account_id,to_account_id,amount,txn_type,description," +
                    " post_balance,status) VALUES (?,?,?,?,?,?,?)")) {

                ps.setInt(1, accountId);
                ps.setObject(2, null);
                ps.setBigDecimal(3, amount);
                ps.setString(4, "WITHDRAWAL");
                ps.setString(5, "Withdrawal via web");
                ps.setBigDecimal(6, newBal);
                ps.setString(7, "SUCCESS");
                ps.executeUpdate();
            }

            conn.commit();

            JsonObject out = Json.createObjectBuilder()
                    .add("accountId", accountId)
                    .add("balance", newBal.toString())
                    .build();

            resp.getWriter().write(out.toString());
        }
    }

    private String generateAccountNumber(int userId) {
        long t = System.currentTimeMillis() % 1_000_000_000L;
        return String.format("AC%04d%09d", userId, t);
    }
}


// package com.example.bank.servlet;

// import com.example.bank.DBConnectionManager;
// import com.example.bank.dao.AccountDao;
// import com.example.bank.dao.UserDao;
// import com.example.bank.dao.impl.JdbcAccountDao;
// import com.example.bank.dao.impl.JdbcUserDao;
// import com.example.bank.model.Account;
// import com.example.bank.model.User;

// // import javax.json.Json;
// // import javax.json.JsonObject;
// // import javax.json.JsonReader;
// // import javax.servlet.annotation.WebServlet;
// // import javax.servlet.http.*;
// // import javax.servlet.ServletException;

// import jakarta.json.Json;
// import jakarta.json.JsonObject;
// import jakarta.json.JsonReader;
// import jakarta.servlet.annotation.WebServlet;
// import jakarta.servlet.http.*;
// import jakarta.servlet.ServletException;

// import java.io.IOException;
// import java.io.StringReader;
// import java.math.BigDecimal;
// import java.sql.Connection;
// import java.sql.PreparedStatement;

// @WebServlet("/api/accounts/*")
// public class AccountServlet extends HttpServlet {
//     private final AccountDao accountDao = new JdbcAccountDao();
//     private final UserDao userDao = new JdbcUserDao();

//     @Override
//     protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//         String path = req.getPathInfo(); // /create or /{id}/deposit etc
//         resp.setContentType("application/json");
//         try {
//             String body = req.getReader().lines().reduce("", (a,b)->a+b);
//             JsonReader jr = Json.createReader(new StringReader(body));
//             JsonObject jo = jr.readObject();

//             if (path != null && path.equals("/create")) {
//                 createAccount(jo, resp);
//                 return;
//             }

//             if (path != null && path.matches("/\\d+/deposit")) {
//                 int id = Integer.parseInt(path.split("/")[1]);
//                 BigDecimal amount = new BigDecimal(jo.getJsonNumber("amount").toString());
//                 deposit(id, amount, resp);
//                 return;
//             }

//             if (path != null && path.matches("/\\d+/withdraw")) {
//                 int id = Integer.parseInt(path.split("/")[1]);
//                 BigDecimal amount = new BigDecimal(jo.getJsonNumber("amount").toString());
//                 withdraw(id, amount, resp);
//                 return;
//             }

//             resp.setStatus(404);
//             resp.getWriter().write("{\"error\":\"Unknown account endpoint\"}");
//         } catch (Exception e) {
//             resp.setStatus(500);
//             resp.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
//         }
//     }

//     @Override
//     protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//         // GET /api/accounts/{id}
//         String path = req.getPathInfo();
//         resp.setContentType("application/json");
//         try {
//             if (path != null && path.matches("/\\d+")) {
//                 int id = Integer.parseInt(path.substring(1));
//                 Account account = accountDao.findById(id);
//                 if (account == null) {
//                     resp.setStatus(404);
//                     resp.getWriter().write("{\"error\":\"Account not found\"}");
//                     return;
//                 }
//                 JsonObject jo = Json.createObjectBuilder()
//                         .add("accountId", account.getAccountId())
//                         .add("accountNumber", account.getAccountNumber())
//                         .add("accountType", account.getAccountType())
//                         .add("balance", account.getBalance().toString())
//                         .build();
//                 resp.getWriter().write(jo.toString());
//                 return;
//             }
//             resp.setStatus(404);
//             resp.getWriter().write("{\"error\":\"Unknown endpoint\"}");
//         } catch (Exception e) {
//             resp.setStatus(500);
//             resp.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
//         }
//     }

//     private void createAccount(JsonObject jo, HttpServletResponse resp) throws Exception {
//         int userId = jo.getInt("userId", 0);
//         String accountType = jo.getString("accountType", "Savings");
//         BigDecimal initialDeposit = BigDecimal.ZERO;
//         if (jo.containsKey("initialDeposit")) initialDeposit = new BigDecimal(jo.getJsonNumber("initialDeposit").toString());

//         User u = userDao.findById(userId);
//         if (u == null) {
//             resp.setStatus(404);
//             resp.getWriter().write("{\"error\":\"User not found\"}");
//             return;
//         }

//         Account a = new Account();
//         a.setUserId(userId);
//         a.setAccountType(accountType);
//         a.setBalance(initialDeposit);
//         a.setAccountNumber(generateAccountNumber(userId));
//         int accountId = accountDao.create(a);

//         JsonObject out = Json.createObjectBuilder().add("accountId", accountId).add("accountNumber", a.getAccountNumber()).add("balance", a.getBalance().toString()).build();
//         resp.setStatus(201);
//         resp.getWriter().write(out.toString());
//     }

//     private void deposit(int accountId, BigDecimal amount, HttpServletResponse resp) throws Exception {
//         if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//             resp.setStatus(400);
//             resp.getWriter().write("{\"error\":\"Amount must be positive\"}");
//             return;
//         }

//         try (Connection conn = DBConnectionManager.getConnection()) {
//             conn.setAutoCommit(false);
//             Account a = accountDao.findById(accountId);
//             if (a == null) { resp.setStatus(404); conn.rollback(); resp.getWriter().write("{\"error\":\"Account not found\"}"); return; }
//             BigDecimal newBal = a.getBalance().add(amount);
//             accountDao.updateBalance(accountId, newBal, conn);

//             try (PreparedStatement ps = conn.prepareStatement(
//                     "INSERT INTO transactions (from_account_id,to_account_id,amount,txn_type,description,post_balance,status) VALUES (?,?,?,?,?,?,?)")) {
//                 ps.setObject(1, null);
//                 ps.setInt(2, accountId);
//                 ps.setBigDecimal(3, amount);
//                 ps.setString(4, "DEPOSIT");
//                 ps.setString(5, "Deposit via web");
//                 ps.setBigDecimal(6, newBal);
//                 ps.setString(7, "SUCCESS");
//                 ps.executeUpdate();
//             }

//             conn.commit();
//             JsonObject out = Json.createObjectBuilder().add("accountId", accountId).add("balance", newBal.toString()).build();
//             resp.getWriter().write(out.toString());
//         }
//     }

//     private void withdraw(int accountId, BigDecimal amount, HttpServletResponse resp) throws Exception {
//         if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//             resp.setStatus(400);
//             resp.getWriter().write("{\"error\":\"Amount must be positive\"}");
//             return;
//         }

//         try (Connection conn = DBConnectionManager.getConnection()) {
//             conn.setAutoCommit(false);
//             Account a = accountDao.findById(accountId);
//             if (a == null) { resp.setStatus(404); conn.rollback(); resp.getWriter().write("{\"error\":\"Account not found\"}"); return; }
//             if (a.getBalance().compareTo(amount) < 0) { resp.setStatus(400); conn.rollback(); resp.getWriter().write("{\"error\":\"Insufficient funds\"}"); return; }

//             BigDecimal newBal = a.getBalance().subtract(amount);
//             accountDao.updateBalance(accountId, newBal, conn);

//             try (PreparedStatement ps = conn.prepareStatement(
//                     "INSERT INTO transactions (from_account_id,to_account_id,amount,txn_type,description,post_balance,status) VALUES (?,?,?,?,?,?,?)")) {
//                 ps.setInt(1, accountId);
//                 ps.setObject(2, null);
//                 ps.setBigDecimal(3, amount);
//                 ps.setString(4, "WITHDRAWAL");
//                 ps.setString(5, "Withdrawal via web");
//                 ps.setBigDecimal(6, newBal);
//                 ps.setString(7, "SUCCESS");
//                 ps.executeUpdate();
//             }

//             conn.commit();
//             JsonObject out = Json.createObjectBuilder().add("accountId", accountId).add("balance", newBal.toString()).build();
//             resp.getWriter().write(out.toString());
//         }
//     }

//     private String generateAccountNumber(int userId) {
//         long t = System.currentTimeMillis() % 1_000_000_000L;
//         return String.format("AC%04d%09d", userId, t);
//     }
// }
