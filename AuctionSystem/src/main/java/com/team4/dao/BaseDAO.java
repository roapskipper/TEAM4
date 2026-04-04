package com.team4.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interface Generic cho tất cả các đối tượng DAO
 * @param <T> Kiểu của Thực thể (Model) - Ví dụ: User, Item, Auction
 * Tương ứng với yêu cầu về Abstraction & Polymorphism
 */
public interface BaseDAO<T> {

    /**
     * Lưu một thực thể mới vào Database
     * @param entity Đối tượng cần lưu
     * @return true nếu thành công
     */
    boolean save(T entity);

    /**
     * Tìm kiếm thực thể theo ID (String UUID)
     * @param id ID của đối tượng
     * @return Optional chứa đối tượng nếu tìm thấy
     */
    Optional<T> findById(String id);

    /**
     * Lấy tất cả danh sách các thực thể từ Database
     * @return List các đối tượng
     */
    List<T> findAll();

    /**
     * Cập nhật thông tin đối tượng đã tồn tại
     * @param entity Đối tượng kèm thông tin mới
     * @return true nếu cập nhật thành công
     */
    boolean update(T entity);

    /**
     * Xóa một thực thể khỏi Database bằng ID
     * @param id ID của đối tượng cần xóa
     * @return true nếu xóa thành công
     */
    boolean delete(String id);
}