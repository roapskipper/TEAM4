package com.team4.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ElectronicsValidationTest {

    private Electronics createTestElectronics(String brand, String model, Electronics.ConditionGrade condition, int warrantyMonths) {
        return new Electronics(
                "Test Electronics",
                new BigDecimal("500000"),
                "A test electronics piece",
                "seller1",
                brand,
                model,
                condition,
                warrantyMonths,
                true
        );
    }

    @Test
    void testValidElectronicsCreation() {
        Electronics item = createTestElectronics("Sony", "PlayStation 5", Electronics.ConditionGrade.MINT, 12);
        assertNotNull(item);
        assertEquals("Sony", item.getBrand());
        assertEquals("PlayStation 5", item.getModel());
        assertEquals(Electronics.ConditionGrade.MINT, item.getItemCondition());
        assertEquals(12, item.getWarrantyMonths());
        assertTrue(item.isFullyFunctional());
    }

    @Test
    void testMissingCondition() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestElectronics("Sony", "PlayStation 5", null, 12);
        });
        assertTrue(exception.getMessage().contains("condition") || 
                   exception.getMessage().toLowerCase().contains("blank"));
    }

    @Test
    void testInvalidWarranty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestElectronics("Sony", "PlayStation 5", Electronics.ConditionGrade.GOOD, -1);
        });
        assertTrue(exception.getMessage().contains("Warranty") || 
                   exception.getMessage().toLowerCase().contains("negative"));
    }

    @Test
    void testDefaultBrandAndModelBehavior() {
        // Blank brand and model -> Unknown
        Electronics item1 = createTestElectronics("   ", "", Electronics.ConditionGrade.FAIR, 0);
        assertEquals("Unknown", item1.getBrand());
        assertEquals("Unknown", item1.getModel());

        // Null brand and model -> Unknown
        Electronics item2 = createTestElectronics(null, null, Electronics.ConditionGrade.FAIR, 0);
        assertEquals("Unknown", item2.getBrand());
        assertEquals("Unknown", item2.getModel());
    }
}
