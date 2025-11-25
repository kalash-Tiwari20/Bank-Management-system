package com.example.bank.dao.impl;

import com.example.bank.DBConnectionManager;
import com.example.bank.dao.UserDao;
import com.example.bank.model.User;

import java.sql.*;

public class JdbcUserDao implements UserDao {

    @Override
    public int create(User user) throws Exception {
        String sql = "INSERT INTO users (first_name,last_name,father_name,mother_name,gender,age,phone,aadhaar,pan,email,password_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getFatherName());
            ps.setString(4, user.getMotherName());
            ps.setString(5, user.getGender());
            ps.setInt(6, user.getAge());
            ps.setString(7, user.getPhone());
            ps.setString(8, user.getAadhaar());
            ps.setString(9, user.getPan());
            ps.setString(10, user.getEmail());
            ps.setString(11, user.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("User id not generated");
            }
        }
    }

    @Override
    public User findById(int id) throws Exception {
        String sql = "SELECT * FROM users WHERE user_id = ?";
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
    public User findByEmail(String email) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setFatherName(rs.getString("father_name"));
        u.setMotherName(rs.getString("mother_name"));
        u.setGender(rs.getString("gender"));
        u.setAge(rs.getInt("age"));
        u.setPhone(rs.getString("phone"));
        u.setAadhaar(rs.getString("aadhaar"));
        u.setPan(rs.getString("pan"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        return u;
    }
}
