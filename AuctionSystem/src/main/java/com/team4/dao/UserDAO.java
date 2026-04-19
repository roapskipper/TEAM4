package com.team4.dao;

import com.team4.model.User;
import java.util.List;

public interface UserDAO {
    User findById(String id);
    User findByUsername(String username);
    boolean insert(User user);
    boolean update(User user);
    List<User> findAll();
}