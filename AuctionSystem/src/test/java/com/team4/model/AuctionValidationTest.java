package com.team4.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho validation và business rules của model Auction.
 * Kiểm tra: constructor validation, state transitions, anti-sniping.
 */
@DisplayName("Kiểm thử validation và nghiệp vụ Auction")
public class AuctionValidationTest {

    // Helper tạo Auction PENDING mặc định
    private Auction newAuction(BigDecimal starting, BigDecimal increment, LocalDateTime end) {
        return new Auction("item-1", "seller-1", starting, increment, end);
    }

    private Auction runningAuction() {
        Auction a = newAuction(
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                LocalDateTime.now().plusHours(2)
        );
        a.approve();
        return a;
    }

    // =========================================================================
    // Constructor validation
    // =========================================================================
    @Nested
    @DisplayName("Constructor validation")
    class ConstructorTests {

        @Test
        @DisplayName("Auction hợp lệ → OK, status = PENDING")
        void validAuction_createdWithPendingStatus() {
            Auction a = newAuction(
                    new BigDecimal("500.00"),
                    new BigDecimal("50.00"),
                    LocalDateTime.now().plusDays(3)
            );
            assertEquals(Auction.AuctionStatus.PENDING, a.getStatus());
            assertEquals(new BigDecimal("500.00"), a.getCurrentPrice());
        }

        @Test
        @DisplayName("startingPrice = 0 → IllegalArgumentException")
        void startingPrice_zero_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    newAuction(BigDecimal.ZERO,
                            new BigDecimal("10.00"),
                            LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("startingPrice âm → IllegalArgumentException")
        void startingPrice_negative_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    newAuction(new BigDecimal("-1.00"),
                            new BigDecimal("10.00"),
                            LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("bidIncrement = 0 → IllegalArgumentException")
        void bidIncrement_zero_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    newAuction(new BigDecimal("100.00"),
                            BigDecimal.ZERO,
                            LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("endTime null → IllegalArgumentException")
        void endTime_null_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    newAuction(new BigDecimal("100.00"), new BigDecimal("10.00"), null));
        }

        @Test
        @DisplayName("itemId null → IllegalArgumentException")
        void itemId_null_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Auction(null, "seller-1",
                            new BigDecimal("100.00"), new BigDecimal("10.00"),
                            LocalDateTime.now().plusDays(1)));
        }
    }

    // =========================================================================
    // State transitions
    // =========================================================================
    @Nested
    @DisplayName("Chuyển trạng thái (State Transitions)")
    class StateTransitionTests {

        @Test
        @DisplayName("PENDING → approve() → RUNNING")
        void approve_changesStatusToRunning() {
            Auction a = newAuction(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            a.approve();
            assertEquals(Auction.AuctionStatus.RUNNING, a.getStatus());
        }

        @Test
        @DisplayName("RUNNING → close() → FINISHED")
        void close_changesStatusToFinished() {
            Auction a = runningAuction();
            a.close();
            assertEquals(Auction.AuctionStatus.FINISHED, a.getStatus());
        }

        @Test
        @DisplayName("PENDING → close() → IllegalStateException")
        void close_fromPending_throwsException() {
            Auction a = newAuction(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            assertThrows(IllegalStateException.class, a::close);
        }

        @Test
        @DisplayName("FINISHED → markPaid() → PAID")
        void markPaid_changesStatusToPaid() {
            Auction a = runningAuction();
            a.close();
            a.markPaid();
            assertEquals(Auction.AuctionStatus.PAID, a.getStatus());
        }

        @Test
        @DisplayName("RUNNING → markPaid() → IllegalStateException")
        void markPaid_fromRunning_throwsException() {
            Auction a = runningAuction();
            assertThrows(IllegalStateException.class, a::markPaid);
        }

        @Test
        @DisplayName("PAID → cancel() → IllegalStateException")
        void cancel_fromPaid_throwsException() {
            Auction a = runningAuction();
            a.close();
            a.markPaid();
            assertThrows(IllegalStateException.class, a::cancel);
        }

        @Test
        @DisplayName("PENDING → cancel() → CANCELLED")
        void cancel_fromPending_ok() {
            Auction a = newAuction(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            a.cancel();
            assertEquals(Auction.AuctionStatus.CANCELLED, a.getStatus());
        }
    }

    // =========================================================================
    // canBid / applyBid
    // =========================================================================
    @Nested
    @DisplayName("Đặt giá (canBid / applyBid)")
    class BidTests {

        @Test
        @DisplayName("canBid() = true khi RUNNING và chưa hết giờ")
        void canBid_running_notExpired_returnsTrue() {
            assertTrue(runningAuction().canBid());
        }

        @Test
        @DisplayName("canBid() = false khi PENDING")
        void canBid_pending_returnsFalse() {
            Auction a = newAuction(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            assertFalse(a.canBid());
        }

        @Test
        @DisplayName("canBid() = false khi đã hết giờ (quá endTime)")
        void canBid_expired_returnsFalse() {
            Auction a = new Auction("item-1", "seller-1",
                    BigDecimal.TEN, BigDecimal.ONE,
                    LocalDateTime.now().minusMinutes(1)); // endTime đã qua
            a.approve();
            assertFalse(a.canBid());
        }

        @Test
        @DisplayName("applyBid() cập nhật giá và người dẫn đầu")
        void applyBid_updatesCurrentPriceAndBidder() {
            Auction a = runningAuction();
            BigDecimal newPrice = new BigDecimal("250.00");

            a.applyBid("bidder-1", newPrice);

            assertEquals(newPrice, a.getCurrentPrice());
            assertEquals("bidder-1", a.getCurrentHighestBidderId());
        }

        @Test
        @DisplayName("applyBid() khi PENDING → IllegalStateException")
        void applyBid_whenPending_throwsException() {
            Auction a = newAuction(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            assertThrows(IllegalStateException.class,
                    () -> a.applyBid("bidder-1", new BigDecimal("20.00")));
        }
    }

    // =========================================================================
    // Anti-sniping
    // =========================================================================
    @Nested
    @DisplayName("Anti-sniping logic")
    class AntiSnipingTests {

        @Test
        @DisplayName("Bid trong 5 phút cuối → endTime tăng thêm 30 phút, trả về true")
        void antiSniping_triggeredInLastFiveMinutes_extendsTime() {
            // endTime = 3 phút nữa (< THRESHOLD 5 phút)
            LocalDateTime originalEnd = LocalDateTime.now().plusMinutes(3);
            Auction a = new Auction("item-1", "seller-1",
                    BigDecimal.TEN, BigDecimal.ONE, originalEnd);
            a.approve();

            boolean extended = a.applyAntiSniping();

            assertTrue(extended, "Phải trả về true khi gia hạn");
            // endTime phải được cộng thêm ít nhất 29 phút (tolerance ±1 phút)
            assertTrue(a.getEndTime().isAfter(originalEnd.plusMinutes(29)),
                    "endTime phải tăng thêm ~30 phút");
        }

        @Test
        @DisplayName("Bid sớm (còn 1 giờ) → endTime không thay đổi, trả về false")
        void antiSniping_notTriggeredEarly_noChange() {
            LocalDateTime originalEnd = LocalDateTime.now().plusHours(1);
            Auction a = new Auction("item-1", "seller-1",
                    BigDecimal.TEN, BigDecimal.ONE, originalEnd);
            a.approve();

            boolean extended = a.applyAntiSniping();

            assertFalse(extended, "Không nên gia hạn khi còn nhiều thời gian");
            assertEquals(originalEnd, a.getEndTime(), "endTime không được thay đổi");
        }

        @Test
        @DisplayName("Bid sau khi đã hết giờ → endTime không thay đổi, trả về false")
        void antiSniping_afterExpiry_noChange() {
            // Dùng constructor DB để tạo auction với endTime đã qua
            LocalDateTime pastEnd = LocalDateTime.now().minusHours(2);
            Auction expired = new Auction(
                    "expired-id", LocalDateTime.now(),
                    "item-1", "seller-1", null,
                    new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("1.00"),
                    LocalDateTime.now().minusHours(3),  // startTime
                    pastEnd,                            // endTime đã qua
                    Auction.AuctionStatus.RUNNING
            );

            boolean extended = expired.applyAntiSniping();

            assertFalse(extended, "Auction đã hết giờ không nên được gia hạn");
        }

    }
}
