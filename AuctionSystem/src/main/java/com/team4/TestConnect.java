package com.team4;

import com.team4.db.DatabaseManager;

public class TestConnect {
    public static void main(String[] args) {
        DatabaseManager.initialize();
        System.out.println("Setup thành công!");
    }
}