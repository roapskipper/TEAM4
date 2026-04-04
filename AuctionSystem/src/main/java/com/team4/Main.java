package com.team4;

import com.team4.db.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("---- KIEM TRA HE THONG ----");
        try {
            // Goi Singleton cua Hai Anh
            DatabaseManager db = DatabaseManager.getInstance();
            if (db.testConnection()) {
                System.out.println("[SUCCESS] Database da ket noi thanh cong!");
            } else {
                System.out.println("[FAIL] Vui long kiem tra mat khau tai file .properties");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}