-- Thêm Admin. Mật khẩu admin123
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, access_level)
VALUES ('root-admin', CURRENT_TIMESTAMP, 'superadmin', '$2a$10$Ew.YIn0n3k6S22qP8u68N.0q/sL/7m693Q06z3FqK3v.h9C2e4w1W', 'Trần Minh Trung', 'admin@auctionsystem.com', 'ADMIN', 0, 1);

-- Thêm Seller & Bidder. Mật khẩu seller123 và bidder123
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-001', CURRENT_TIMESTAMP, 'domixi', '$2a$10$K8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK/r5sH6g7R9x1/4L8z', 'Phùng Thanh Độ', 'ban1@gmail.com', 'SELLER', 0, 'Mixi Shop', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-001', CURRENT_TIMESTAMP, 'ueteee', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Trần Văn Si', 'mua1@gmail.com', 'BIDDER', 10000000, 'Nhà 36,120 Yên Lãng,Hà Nội');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-002', CURRENT_TIMESTAMP, 'daucatmoi', '$2a$10$K8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK/r5sH6g7R9x1/4L8z', 'Kim Ri Cha', 'ban2@gmail.com', 'SELLER', 0, 'Shop Chất', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-002', CURRENT_TIMESTAMP, 'thichthimua', '$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK', 'Hà Duy Kiên', 'mua2@gmail.com', 'BIDDER', 100000000, 'Nhà 12,34 Phạm Văn Đồng, Hà Nội');

-- Thêm một Item và một Auction
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, starting_price, artist, medium)
VALUES ('item-001', CURRENT_TIMESTAMP, 'ART', 'seller-001', 'Tranh Đông Hồ', 50000, '', 'OIL_PAINTING');

INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-001', CURRENT_TIMESTAMP, 'item-001', 'seller-001', 50000, 5000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 7 DAY), 'RUNNING');
       -- ITEM 2: Đồ điện tử (ELECTRONICS)
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, starting_price, brand)
VALUES ('item-002', CURRENT_TIMESTAMP, 'ELECTRONICS', 'seller-001', 'MacBook Pro M3 Max', 50000000, 'Apple');

-- AUCTION 2: Đang chạy (RUNNING) - Thích hợp test đặt giá ngay
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-002', CURRENT_TIMESTAMP, 'item-002', 'seller-001', 50000000, 1000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 3 DAY), 'RUNNING');


-- ITEM 3: Xe cộ (VEHICLE)
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, starting_price, brand, creation_year)
VALUES ('item-003', CURRENT_TIMESTAMP, 'VEHICLE', 'seller-002', 'Honda SH 150i', 80000000, 'Honda', 2023);

-- AUCTION 3: Chờ duyệt (PENDING) - Thích hợp test chức năng duyệt của AdminService
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-003', CURRENT_TIMESTAMP, 'item-003', 'seller-002', 80000000, 2000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 5 DAY), 'PENDING');

