package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;
    @Mock
    private ItemDAO itemDAO;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(auctionDAO, itemDAO);
    }

    @Test
    void testCreateAuction_Success() {
        // Arrange
        String itemId = "item1";
        String sellerId = "seller1";
        Item mockItem = mock(Item.class);
        when(mockItem.getOwnerId()).thenReturn(sellerId);
        when(itemDAO.findById(itemId)).thenReturn(mockItem);

        // Act
        Auction result = auctionService.createAuction(itemId, sellerId, new BigDecimal("100"), new BigDecimal("10"), LocalDateTime.now().plusDays(1));

        // Assert
        assertNotNull(result);
        verify(auctionDAO).insert(any(Auction.class));
    }

    @Test
    void testApproveAuction_Success() {
        // Arrange
        String auctionId = "auc1";
        Auction mockAuction = mock(Auction.class);
        when(mockAuction.getStatus()).thenReturn(Auction.AuctionStatus.PENDING);
        when(auctionDAO.findById(auctionId)).thenReturn(mockAuction);

        // Act
        auctionService.approveAuction(auctionId);

        // Assert
        verify(mockAuction).approve();
        verify(auctionDAO).updateStatus(eq(auctionId), eq(Auction.AuctionStatus.RUNNING));
    }
}
