package com.example.bank.servlet;

import com.example.bank.dao.UserDao;
import com.example.bank.dao.impl.JdbcUserDao;
import com.example.bank.model.User;
import com.example.bank.util.PasswordUtil;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private final UserDao userDao = new JdbcUserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo(); // /signup or /login
        resp.setContentType("application/json");

        try {
            String body = req.getReader().lines().reduce("", (a,b)->a+b);
            JsonReader jr = Json.createReader(new StringReader(body));
            JsonObject jo = jr.readObject();

            if ("/signup".equals(path)) {
                handleSignup(jo, resp);
            } else if ("/login".equals(path)) {
                handleLogin(jo, req, resp);
            } else {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\":\"Unknown auth endpoint\"}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter()
                .write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void handleSignup(JsonObject jo, HttpServletResponse resp) throws Exception {
        String firstName  = jo.getString("firstName", "");
        String lastName   = jo.getString("lastName", "");
        String fatherName = jo.getString("fatherName", "");
        String motherName = jo.getString("motherName", "");
        String gender     = jo.getString("gender", "");
        int age           = jo.getInt("age", 0);
        String phone      = jo.getString("phone", "");
        String aadhaar    = jo.getString("aadhaar", "");
        String pan        = jo.getString("pan", "");
        String email      = jo.getString("email", "");
        String password   = jo.getString("password", "");

        if (email.isEmpty() || password.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Email and password required\"}");
            return;
        }

        if (userDao.findByEmail(email) != null) {
            resp.setStatus(409);
            resp.getWriter().write("{\"error\":\"User already exists\"}");
            return;
        }

        byte[] salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password.toCharArray(), salt);

        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setFatherName(fatherName);
        u.setMotherName(motherName);
        u.setGender(gender);
        u.setAge(age);
        u.setPhone(phone);
        u.setAadhaar(aadhaar);
        u.setPan(pan);
        u.setEmail(email);
        u.setPasswordHash(hash);

        int id = userDao.create(u);

        resp.setStatus(201);
        JsonObject res = Json.createObjectBuilder()
                .add("userId", id)
                .add("email", email)
                .build();
        try (PrintWriter w = resp.getWriter()) {
            w.write(res.toString());
        }
    }

    private void handleLogin(JsonObject jo,
                             HttpServletRequest req,
                             HttpServletResponse resp) throws Exception {

        String email    = jo.getString("email", "");
        String password = jo.getString("password", "");

        if (email.isEmpty() || password.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Email and password required\"}");
            return;
        }

        User u = userDao.findByEmail(email);
        if (u == null || u.getPasswordHash() == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"error\":\"Invalid credentials\"}");
            return;
        }

        boolean ok = PasswordUtil.verifyPassword(password.toCharArray(), u.getPasswordHash());
        if (!ok) {
            resp.setStatus(401);
            resp.getWriter().write("{\"error\":\"Invalid credentials\"}");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("userId", u.getUserId());

        JsonObject res = Json.createObjectBuilder()
                .add("userId", u.getUserId())
                .add("email", u.getEmail())
                .add("firstName", u.getFirstName())
                .build();
        resp.getWriter().write(res.toString());
    }
}


// package com.example.bank.servlet;

// import com.example.bank.dao.UserDao;
// import com.example.bank.dao.impl.JdbcUserDao;
// import com.example.bank.model.User;
// import com.example.bank.util.PasswordUtil;

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
// import java.io.PrintWriter;
// import java.io.StringReader;

// @WebServlet("/api/auth/*")
// public class AuthServlet extends HttpServlet {
//     private final UserDao userDao = new JdbcUserDao();

//     @Override
//     protected void doPost(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws ServletException, IOException {
//         String path = req.getPathInfo(); // /signup or /login
//         resp.setContentType("application/json");
//         try {
//             String body = req.getReader().lines().reduce("", (a,b)->a+b);
//             JsonReader jr = Json.createReader(new StringReader(body));
//             JsonObject jo = jr.readObject();

//             if ("/signup".equals(path)) {
//                 handleSignup(jo, resp);
//             } else if ("/login".equals(path)) {
//                 handleLogin(jo, req, resp);
//             } else {
//                 resp.setStatus(404);
//                 resp.getWriter().write("{\"error\":\"Unknown auth endpoint\"}");
//             }
//         } catch (Exception e) {
//             resp.setStatus(500);
//             resp.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
//         }
//     }

//     private void handleSignup(JsonObject jo, javax.servlet.http.HttpServletResponse resp) throws Exception {
//         String firstName = jo.getString("firstName", "");
//         String lastName = jo.getString("lastName", "");
//         String fatherName = jo.getString("fatherName", "");
//         String motherName = jo.getString("motherName", "");
//         String gender = jo.getString("gender", "");
//         int age = jo.getInt("age", 0);
//         String phone = jo.getString("phone", "");
//         String aadhaar = jo.getString("aadhaar", "");
//         String pan = jo.getString("pan", "");
//         String email = jo.getString("email", "");
//         String password = jo.getString("password", "");

//         if (email.isEmpty() || password.isEmpty()) {
//             resp.setStatus(400);
//             resp.getWriter().write("{\"error\":\"Email and password required\"}");
//             return;
//         }

//         if (userDao.findByEmail(email) != null) {
//             resp.setStatus(409);
//             resp.getWriter().write("{\"error\":\"User already exists\"}");
//             return;
//         }

//         byte[] salt = PasswordUtil.generateSalt();
//         String hash = PasswordUtil.hashPassword(password.toCharArray(), salt);

//         User u = new User();
//         u.setFirstName(firstName);
//         u.setLastName(lastName);
//         u.setFatherName(fatherName);
//         u.setMotherName(motherName);
//         u.setGender(gender);
//         u.setAge(age);
//         u.setPhone(phone);
//         u.setAadhaar(aadhaar);
//         u.setPan(pan);
//         u.setEmail(email);
//         u.setPasswordHash(hash);

//         int id = userDao.create(u);

//         resp.setStatus(201);
//         JsonObject res = Json.createObjectBuilder().add("userId", id).add("email", email).build();
//         try (PrintWriter w = resp.getWriter()) { w.write(res.toString()); }
//     }

//     private void handleLogin(JsonObject jo, javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws Exception {
//         String email = jo.getString("email", "");
//         String password = jo.getString("password", "");

//         if (email.isEmpty() || password.isEmpty()) {
//             resp.setStatus(400);
//             resp.getWriter().write("{\"error\":\"Email and password required\"}");
//             return;
//         }

//         User u = userDao.findByEmail(email);
//         if (u == null || u.getPasswordHash() == null) {
//             resp.setStatus(401);
//             resp.getWriter().write("{\"error\":\"Invalid credentials\"}");
//             return;
//         }

//         boolean ok = PasswordUtil.verifyPassword(password.toCharArray(), u.getPasswordHash());
//         if (!ok) {
//             resp.setStatus(401);
//             resp.getWriter().write("{\"error\":\"Invalid credentials\"}");
//             return;
//         }

//         // create HTTP session
//         HttpSession session = req.getSession(true);
//         session.setAttribute("userId", u.getUserId());

//         JsonObject res = Json.createObjectBuilder().add("userId", u.getUserId()).add("email", u.getEmail()).add("firstName", u.getFirstName()).build();
//         resp.getWriter().write(res.toString());
//     }
// }


