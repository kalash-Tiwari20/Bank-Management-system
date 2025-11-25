package com.example.bank.dao;

import com.example.bank.model.User;

public interface UserDao {
    int create(User user) throws Exception;
    User findById(int id) throws Exception;
    User findByEmail(String email) throws Exception;
}
