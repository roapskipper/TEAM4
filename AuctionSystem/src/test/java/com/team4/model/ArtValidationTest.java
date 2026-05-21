package com.team4.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ArtValidationTest {

    private Art createTestArt(String artist, int creationYear, Art.Medium medium, String dimensions) {
        return new Art(
                "Test Art",
                new BigDecimal("50000"),
                "A test art piece",
                "seller1",
                artist,
                creationYear,
                medium,
                dimensions
        );
    }

    @Test
    void testValidArtCreation() {
        Art art = createTestArt("Leonardo", 1503, Art.Medium.OIL_PAINT, "77x53 cm");
        assertNotNull(art);
        assertEquals("Leonardo", art.getArtist());
        assertEquals(1503, art.getCreationYear());
        assertEquals(Art.Medium.OIL_PAINT, art.getMedium());
        assertEquals("77x53 cm", art.getDimensions());
    }

    @Test
    void testMissingMedium() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestArt("Leonardo", 1503, null, "77x53 cm");
        });
        assertTrue(exception.getMessage().contains("Medium must not be null") || 
                   exception.getMessage().toLowerCase().contains("medium"));
    }

    @Test
    void testInvalidCreationYear() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createTestArt("Leonardo", -4000, Art.Medium.OIL_PAINT, "77x53 cm");
        });
        assertTrue(exception.getMessage().contains("Invalid creation year") || 
                   exception.getMessage().toLowerCase().contains("creation year"));

        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            createTestArt("Leonardo", 3000, Art.Medium.OIL_PAINT, "77x53 cm");
        });
        assertTrue(exception2.getMessage().contains("Invalid creation year") || 
                   exception2.getMessage().toLowerCase().contains("creation year"));
    }

    @Test
    void testDefaultAuthorAndYearBehavior() {
        // Blank artist -> Unknown
        Art art1 = createTestArt("   ", 0, Art.Medium.OIL_PAINT, null);
        assertEquals("Unknown", art1.getArtist());
        assertEquals(0, art1.getCreationYear());

        // Null artist -> Unknown
        Art art2 = createTestArt(null, 0, Art.Medium.OIL_PAINT, null);
        assertEquals("Unknown", art2.getArtist());
        assertEquals(0, art2.getCreationYear());
    }
}
