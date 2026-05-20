package com.team4;

import com.team4.util.PasswordHasher;

public class GetHashPassword {
    public static void main(String[] args) {
        System.out.println(PasswordHasher.hashPassword("bidder123"));
    }
}
