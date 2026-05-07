package com.team4.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

public abstract class Entity {
    private final String id;
    private final LocalDateTime createdAt;

    // Tạo đối tượng mới hoàn toàn
    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Lấy đối tượng đã tạo trước đó từ DB
    protected Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    // Dùng equals và hashCode để so sánh các đối tượng dựa trên id (UUID) thay vì tham chiếu bộ nhớ
    // Mục đích: để chương trình biết 2 obj có đại diện cho cùng 1 thục thể không
    // Tránh trường hợp đọc cùng một User từ DB hai lần sẽ ra hai object khác nhau trong RAM
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Dùng getClass để kiểm tra xem 2 đối tượng có cùng kiểu hay không
        if (o == null || getClass() != o.getClass()) return false;
        Entity other = (Entity) o;
        return Objects.equals(id, other.id);
    }
    // Dùng id để định danh
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Getter
    public String getId() {
        return id;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
