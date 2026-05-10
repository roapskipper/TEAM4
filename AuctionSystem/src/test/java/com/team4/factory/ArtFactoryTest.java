package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class ArtFactoryTest {

    private ArtFactory artFactory;

    @BeforeEach
    public void setUp() {
        artFactory = new ArtFactory();
    }

    @Test
    public void testCreateItem_Success() {
        // 1. Chuẩn bị dữ liệu đầu vào bằng Gson (vì ItemRequest không có setter)
        String json = "{ \"name\": \"Bức tranh Mona Lisa\", \"startingPrice\": 1000.00, \"description\": \"Tranh sơn dầu nổi tiếng\", \"ownerId\": \"user-123\", \"artist\": \"Leonardo da Vinci\", \"creationYear\": 1503, \"medium\": \"OIL_PAINT\", \"dimensions\": \"77x53 cm\", \"category\": \"ART\" }";
        com.google.gson.Gson gson = new com.google.gson.Gson();
        ItemRequest request = gson.fromJson(json, ItemRequest.class);

        // 2. Thực thi phương thức
        Item result = artFactory.createItem(request);

        // 3. Kiểm tra kết quả (Assertions)
        assertNotNull(result, "Item không được null");
        assertTrue(result instanceof Art, "Phải tạo ra đối tượng thuộc lớp Art");
        
        Art art = (Art) result;
        assertEquals("Bức tranh Mona Lisa", art.getName());
        assertEquals(new BigDecimal("1000.00"), art.getStartingPrice());
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getCreationYear());
    }
}
