package com.team4.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Entity implements Serializable {
    protected String id;
    protected LocalDateTime createdAt;

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }
}