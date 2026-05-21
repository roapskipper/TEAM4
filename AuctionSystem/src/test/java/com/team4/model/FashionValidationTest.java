package com.team4.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FashionValidationTest {

    private Fashion createTestFashion(Fashion.Size size, Fashion.ConditionGrade condition, Fashion.Gender gender) {
        return new Fashion(
                "Test Fashion",
                new BigDecimal("500000"),
                "A test fashion piece",
                "seller1",
                "Nike",
                size,
                "Cotton",
                "Black",
                gender,
                condition,
                true
        );
    }

    @Test
    void testValidFashionCreation() {
        Fashion item = createTestFashion(Fashion.Size.M, Fashion.ConditionGrade.MINT, Fashion.Gender.MALE);
        assertNotNull(item);
        assertEquals(Fashion.Size.M, item.getSize());
        assertEquals(Fashion.ConditionGrade.MINT, item.getCondition());
        assertEquals(Fashion.Gender.MALE, item.getGender());
    }

    @Test
    void testMissingSize() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestFashion(null, Fashion.ConditionGrade.MINT, Fashion.Gender.MALE);
        });
        assertTrue(exception.getMessage().contains("size") || 
                   exception.getMessage().toLowerCase().contains("size"));
    }

    @Test
    void testMissingCondition() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestFashion(Fashion.Size.M, null, Fashion.Gender.MALE);
        });
        assertTrue(exception.getMessage().contains("condition") || 
                   exception.getMessage().toLowerCase().contains("condition"));
    }

    @Test
    void testMissingGenderDefaultsToUnisex() {
        Fashion item = createTestFashion(Fashion.Size.M, Fashion.ConditionGrade.MINT, null);
        assertEquals(Fashion.Gender.UNISEX, item.getGender());
    }
}
