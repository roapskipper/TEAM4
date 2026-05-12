-- =========================================================================
-- PHẦN 1: TÀI KHOẢN ADMIN (1 Super Admin, 3 Moderator)
-- =========================================================================

-- Super Admin: admin123 & adminteam4
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('root-admin', CURRENT_TIMESTAMP, 'superadmin', '$2a$10$Ew.YIn0n3k6S22qP8u68N.0q/sL/7m693Q06z3FqK3v.h9C2e4w1W', 'Trần Minh Trung', 'admin@auctionsystem.com', 'ADMIN', 0, '$2a$10$7Z2P.M8J6L.Qk/7q7g4FMe.K1z9Y3V5x8T0w2S4u6R8p0N2m4O6q', 1);

-- 3 Moderator (Access Level = 2)
       -- mod123 & mod-code-01
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-001', CURRENT_TIMESTAMP, 'moderator1', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Lê Văn Cấc', 'mod1@gmail.com', 'ADMIN', 0, '$2a$10$K7Xk9B5aH3R8V2u1S4t6Q.9p0N2m4O6q8r0N2m4O6q8r0N2m4O6q', 2);
       -- mod123 & mod-code-02
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-002', CURRENT_TIMESTAMP, 'moderator2', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Lê Minh Hiếu', 'mod2@gmail.com', 'ADMIN', 0, '$2a$10$T5v8Y7w3Q4x1V2u3S6t9R.0qK/r5sH6g7R9x1/4L8t1u.5/K9z2Z', 2);
       -- mod123 & mod-code-03
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-003', CURRENT_TIMESTAMP, 'moderator3', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Du Di Min', 'mod3@gmail.com', 'ADMIN', 0, '$2a$10$M8J6L.Qk/7q7g4FMe.K1z.9Y3V5x8T0w2S4u6R8p0N2m4O6q8r0N', 2);


-- =========================================================================
-- PHẦN 2: TÀI KHOẢN SELLER (Tổng 3 Seller)
-- Tất cả Seller đều dùng chung Mật khẩu: seller123
-- =========================================================================

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-001', CURRENT_TIMESTAMP, 'domixi', '$2a$10$K8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK/r5sH6g7R9x1/4L8z', 'Phùng Thanh Độ', 'ban1@gmail.com', 'SELLER', 0, 'Mixi Shop', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-002', CURRENT_TIMESTAMP, 'daucatmoi', '$2a$10$K8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK/r5sH6g7R9x1/4L8z', 'Kim Ri Cha', 'ban2@gmail.com', 'SELLER', 0, 'Shop Chất', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-003', CURRENT_TIMESTAMP, 'j.bieber', '$2a$10$K8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK/r5sH6g7R9x1/4L8z', 'Đô Năm Trăm', 'ban3@gmail.com', 'SELLER', 0, 'JQK', 4.8);


-- =========================================================================
-- PHẦN 3: TÀI KHOẢN BIDDER (Tổng 5 Bidder)
-- Tất cả Bidder đều dùng chung Mật khẩu: bidder123
-- =========================================================================

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-001', CURRENT_TIMESTAMP, 'ueteee', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Trần Văn Si', 'mua1@gmail.com', 'BIDDER', 10000000, 'Nhà 36, 120 Yên Lãng, Hà Nội');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-002', CURRENT_TIMESTAMP, 'thichthimua', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Hà Duy Kiên', 'mua2@gmail.com', 'BIDDER', 100000000, 'Nhà 12, 34 Phạm Văn Đồng, Hà Nội');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-003', CURRENT_TIMESTAMP, 'nguoimua3', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Lê Mua Nhiều', 'mua3@gmail.com', 'BIDDER', 50000000, 'Quận 1, TP. HCM');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-004', CURRENT_TIMESTAMP, 'sanhangdoc', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Hoàng Công Tử', 'mua4@gmail.com', 'BIDDER', 20000000, 'Đà Nẵng');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-005', CURRENT_TIMESTAMP, 'svngheo', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Nguyễn Sinh Viên', 'mua5@gmail.com', 'BIDDER', 2000000, 'Ký túc xá VNU');


-- =========================================================================
-- PHẦN 4 & 5: MẶT HÀNG (5 Items) & PHIÊN ĐẤU GIÁ (3 Running, 2 Pending)
-- =========================================================================

-- Combo 1: Tranh / Đang chạy
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, artist, medium)
VALUES ('item-001', CURRENT_TIMESTAMP, 'ART', 'seller-001', 'Tranh Đông Hồ', 'Tranh dân gian Đông Hồ nổi tiếng', 50000, 'Nghệ nhân Đông Hồ', 'OIL_PAINT');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-001', CURRENT_TIMESTAMP, 'item-001', 'seller-001', 50000, 50000, 5000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 7 DAY), 'RUNNING');

-- Combo 2: Macbook / Đang chạy
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand)
VALUES ('item-002', CURRENT_TIMESTAMP, 'ELECTRONICS', 'seller-001', 'MacBook Pro M3 Max', 'Macbook đời mới nhất 2024', 50000000, 'Apple');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-002', CURRENT_TIMESTAMP, 'item-002', 'seller-001', 50000000, 50000000, 1000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 3 DAY), 'RUNNING');

-- Combo 3: Xe cộ / Chờ duyệt
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand, manufacturing_year)
VALUES ('item-003', CURRENT_TIMESTAMP, 'VEHICLE', 'seller-002', 'Honda SH 150i', 'Xe tay ga hạng sang', 80000000, 'Honda', 2023);
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-003', CURRENT_TIMESTAMP, 'item-003', 'seller-002', 80000000, 80000000, 2000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 5 DAY), 'PENDING');

-- Combo 4: Fashion / Đang chạy
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand)
VALUES ('item-004', CURRENT_TIMESTAMP, 'FASHION', 'seller-003', 'Túi Gucci Chính Hãng', 'Túi hiệu Gucci từ Italy', 15000000, 'Gucci');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-004', CURRENT_TIMESTAMP, 'item-004', 'seller-003', 15000000, 15000000, 500000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 2 DAY), 'RUNNING');

-- Combo 5: Sưu tầm / Chờ duyệt
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price)
VALUES ('item-005', CURRENT_TIMESTAMP, 'COLLECTIBLE', 'seller-001', 'Bộ tem cổ thời Đông Dương', 'Tem hiếm từ thế kỷ 20', 3000000);
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-005', CURRENT_TIMESTAMP, 'item-005', 'seller-001', 3000000, 3000000, 100000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 10 DAY), 'PENDING');