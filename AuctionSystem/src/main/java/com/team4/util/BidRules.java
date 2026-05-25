package com.team4.util;

import java.math.BigDecimal;

/**
 * Utility class containing centralized rules for bidding caps.
 */
public class BidRules {

    public static final BigDecimal ABSOLUTE_MAX = new BigDecimal("500000000");

    private static final BigDecimal BD_1_MILLION = new BigDecimal("1000000");
    private static final BigDecimal BD_5_MILLION = new BigDecimal("5000000");
    private static final BigDecimal BD_25_MILLION = new BigDecimal("25000000");
    private static final BigDecimal BD_50_MILLION = new BigDecimal("50000000");
    private static final BigDecimal BD_100_MILLION = new BigDecimal("100000000");
    private static final BigDecimal BD_250_MILLION = new BigDecimal("250000000");

    private static final BigDecimal MULTIPLIER_5 = new BigDecimal("5");
    private static final BigDecimal MULTIPLIER_4 = new BigDecimal("4");
    private static final BigDecimal MULTIPLIER_3 = new BigDecimal("3");
    private static final BigDecimal MULTIPLIER_2 = new BigDecimal("2");
    private static final BigDecimal MULTIPLIER_1_8 = new BigDecimal("1.8");
    private static final BigDecimal MULTIPLIER_1_5 = new BigDecimal("1.5");
    private static final BigDecimal MULTIPLIER_1_3 = new BigDecimal("1.3");

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

        if (currentPrice.compareTo(BD_1_MILLION) < 0) {
            multiplier = MULTIPLIER_5;
        } else if (currentPrice.compareTo(BD_5_MILLION) < 0) {
            multiplier = MULTIPLIER_4;
        } else if (currentPrice.compareTo(BD_25_MILLION) < 0) {
            multiplier = MULTIPLIER_3;
        } else if (currentPrice.compareTo(BD_50_MILLION) < 0) {
            multiplier = MULTIPLIER_2;
        } else if (currentPrice.compareTo(BD_100_MILLION) < 0) {
            multiplier = MULTIPLIER_1_8;
        } else if (currentPrice.compareTo(BD_250_MILLION) < 0) {
            multiplier = MULTIPLIER_1_5;
        } else {
            multiplier = MULTIPLIER_1_3;
        }

        BigDecimal maxAllowed = currentPrice.multiply(multiplier);

        if (maxAllowed.compareTo(ABSOLUTE_MAX) > 0) {
            return ABSOLUTE_MAX;
        }
        return maxAllowed;
    }
}
