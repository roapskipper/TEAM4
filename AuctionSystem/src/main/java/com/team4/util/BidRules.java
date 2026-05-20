package com.team4.util;

import java.math.BigDecimal;

/**
 * Utility class containing centralized rules for bidding caps.
 */
public class BidRules {

    public static final BigDecimal ABSOLUTE_MAX = new BigDecimal("500000000");

    /**
     * Calculates the allowed maximum bid based on the current price.
     * Uses a predefined multiplier table to prevent excessive bidding.
     *
     * @param currentPrice The current price of the auction.
     * @return The maximum allowed bid, clamped to ABSOLUTE_MAX.
     */
    public static BigDecimal allowedMaxFor(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ABSOLUTE_MAX;
        }
        if (currentPrice.compareTo(ABSOLUTE_MAX) >= 0) {
            return ABSOLUTE_MAX;
        }

        BigDecimal multiplier;

        if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            multiplier = new BigDecimal("5");
        } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
            multiplier = new BigDecimal("4");
        } else if (currentPrice.compareTo(new BigDecimal("25000000")) < 0) {
            multiplier = new BigDecimal("3");
        } else if (currentPrice.compareTo(new BigDecimal("50000000")) < 0) {
            multiplier = new BigDecimal("2");
        } else if (currentPrice.compareTo(new BigDecimal("100000000")) < 0) {
            multiplier = new BigDecimal("1.8");
        } else if (currentPrice.compareTo(new BigDecimal("250000000")) < 0) {
            multiplier = new BigDecimal("1.5");
        } else {
            multiplier = new BigDecimal("1.3");
        }

        BigDecimal maxAllowed = currentPrice.multiply(multiplier);

        if (maxAllowed.compareTo(ABSOLUTE_MAX) > 0) {
            return ABSOLUTE_MAX;
        }
        return maxAllowed;
    }
}
