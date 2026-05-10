package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.UserDAO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoBiddingServiceTest {

    @Mock
    private AutoBiddingDAO autoBiddingDAO;
    @Mock
    private AuctionDAO auctionDAO;
    @Mock
    private UserDAO userDAO;

    private AutoBiddingService autoBiddingService;

    @BeforeEach
    void setUp() {
        autoBiddingService = new AutoBiddingService(autoBiddingDAO, auctionDAO, userDAO);
    }

    @Test
    void testEnableAutoBidding_Success() {
        String bidderId = "bid1";
        String auctionId = "auc1";
        BigDecimal limit = new BigDecimal("1000");

        Auction mockAuction = mock(Auction.class);
        when(mockAuction.getStatus()).thenReturn(Auction.AuctionStatus.RUNNING);
        when(mockAuction.getCurrentPrice()).thenReturn(new BigDecimal("500"));
        when(mockAuction.getSellerId()).thenReturn("seller1");
        
        User mockBidder = mock(Bidder.class);
        when(mockBidder.getRole()).thenReturn(User.Role.BIDDER);

        when(auctionDAO.findById(auctionId)).thenReturn(mockAuction);
        when(userDAO.findById(bidderId)).thenReturn(mockBidder);
        when(autoBiddingDAO.insert(any(AutoBidding.class))).thenReturn(true);

        AutoBidding result = autoBiddingService.enableAutoBidding(bidderId, auctionId, limit);

        assertNotNull(result);
        assertEquals(limit, result.getMaxLimit());
    }
}
