package com.team4.dao;

import com.team4.model.User;
import java.util.List;
import java.math.BigDecimal;

public interface UserDAO {
    User findById(String id); // tìm theo id
    User findByUsername(String username); // tìm khi login
    List<User> findAll();
    boolean insert(User user); // đăng kí tài khoản mới
    boolean update(User user); // cập nhật profile
    boolean updateBalance(String id, BigDecimal newBalance); // nạp/rút tiền
    // Không có ban/xóa user do liên quan đến vật phẩm,cuộc đấu giá
}