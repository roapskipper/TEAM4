package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.factory.ItemRequest;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;
    @Mock
    private UserDAO userDAO;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemDAO, userDAO);
    }

    @Test
    void testCreateItem_Success() {
        String sellerId = "seller1";
        ItemRequest request = new ItemRequest("Phone", "Desc", Item.Category.ELECTRONICS, sellerId);
        Seller mockSeller = mock(Seller.class);
        
        when(userDAO.findById(sellerId)).thenReturn(mockSeller);
        when(itemDAO.insert(any(Item.class))).thenReturn(true);

        Item result = itemService.createItem(sellerId, request);

        assertNotNull(result);
        assertEquals("Phone", result.getName());
        verify(itemDAO).insert(any(Item.class));
    }

    @Test
    void testUpdateItem_Unauthorized() {
        String sellerId = "seller1";
        String itemId = "item1";
        Item mockItem = mock(Item.class);
        when(mockItem.getOwnerId()).thenReturn("otherSeller");
        when(itemDAO.findById(itemId)).thenReturn(mockItem);

        assertThrows(BusinessException.class, () -> 
            itemService.updateItem(sellerId, itemId, "New Name", "New Desc")
        );
    }

    @Test
    void testDeleteItem_Success() {
        String sellerId = "seller1";
        String itemId = "item1";
        Item mockItem = mock(Item.class);
        when(mockItem.getOwnerId()).thenReturn(sellerId);
        when(itemDAO.findById(itemId)).thenReturn(mockItem);
        when(itemDAO.delete(itemId)).thenReturn(true);

        itemService.deleteItem(itemId, sellerId);

        verify(itemDAO).delete(itemId);
    }
}
