-- =========================================================================
-- SECTION 1: ADMIN ACCOUNTS (1 Super Admin, 3 Moderators)
-- =========================================================================

-- Super Admin: admin123 & adminteam4
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('root-admin', CURRENT_TIMESTAMP, 'superadmin', '$2a$10$3FnYMPlU7ubngFBfaLmY7.GaozAoSHhxmkYx7vhIuWsmt4Cq3Almi', 'Trung Tran', 'admin@auctionsystem.com', 'ADMIN', 0, '$2a$10$vQ4RTsfsmWKTRKHsRboTxuhrnf8Sq5TUIuHbV9FrQh9oCe5jFpShi', 2);

-- Moderators: mod123 & mod-code-01/02/03
INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-001', CURRENT_TIMESTAMP, 'moderator1', '$2a$10$jgym2Ka5mu8NziXsENcni.ad8BfT1U.hRJqrQmRF7pzD9WBWByjWe', 'Cac Le', 'mod1@gmail.com', 'ADMIN', 0, '$2a$10$kBnkaGClcePTE6FwActYJOvEq8WatWVru4uKzVLgo8gU6UlzIav4C', 1);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-002', CURRENT_TIMESTAMP, 'moderator2', '$2a$10$jgym2Ka5mu8NziXsENcni.ad8BfT1U.hRJqrQmRF7pzD9WBWByjWe', 'Hieu Le', 'mod2@gmail.com', 'ADMIN', 0, '$2a$10$lKx1JCDjKeEtMOfn2as.lu96792D.rOXyYYw/cYrvrEiInHMWJWm.', 1);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, admin_code_hash, access_level)
VALUES ('mod-003', CURRENT_TIMESTAMP, 'moderator3', '$2a$10$jgym2Ka5mu8NziXsENcni.ad8BfT1U.hRJqrQmRF7pzD9WBWByjWe', 'Du Di Min', 'mod3@gmail.com', 'ADMIN', 0, '$2a$10$uyaOmo.D9ONPa05tlpMVY.BJFeQh31YNb6Xl.78UkLx5DDQtMSBJO', 1);


-- =========================================================================
-- SECTION 2: SELLER ACCOUNTS (3 Sellers)
-- All sellers use password: seller123
-- =========================================================================

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-001', CURRENT_TIMESTAMP, 'heritage_seller', '$2a$10$g/21snU80tiBtt6TkkZxteXPiKxCqltvqbLXQ39ADPwJQtQgsUkru', 'Olivia Carter', 'seller1@auctionsystem.com', 'SELLER', 0, 'Heritage Gallery', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-002', CURRENT_TIMESTAMP, 'premium_seller', '$2a$10$g/21snU80tiBtt6TkkZxteXPiKxCqltvqbLXQ39ADPwJQtQgsUkru', 'Ethan Brooks', 'seller2@auctionsystem.com', 'SELLER', 0, 'Premium Finds', 5.0);

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, store_name, rating)
VALUES ('seller-003', CURRENT_TIMESTAMP, 'curated_seller', '$2a$10$g/21snU80tiBtt6TkkZxteXPiKxCqltvqbLXQ39ADPwJQtQgsUkru', 'Sophia Reed', 'seller3@auctionsystem.com', 'SELLER', 0, 'Curated Classics', 4.8);


-- =========================================================================
-- SECTION 3: BIDDER ACCOUNTS (5 Bidders)
-- All bidders use password: bidder123
-- =========================================================================

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-001', CURRENT_TIMESTAMP, 'heritage_bidder', '$2a$10$ys5UkKJhmgmxsGRgjwYgIu/SMjPvJjG0gCqbSY3vwBNIpYn32Wic2', 'Noah Bennett', 'buyer1@auctionsystem.com', 'BIDDER', 10000000, '36 Heritage Lane, New York');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-002', CURRENT_TIMESTAMP, 'active_bidder', '$2a$10$ys5UkKJhmgmxsGRgjwYgIu/SMjPvJjG0gCqbSY3vwBNIpYn32Wic2', 'Mia Anderson', 'buyer2@auctionsystem.com', 'BIDDER', 100000000, '12 Market Street, Chicago');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-003', CURRENT_TIMESTAMP, 'power_bidder', '$2a$10$ys5UkKJhmgmxsGRgjwYgIu/SMjPvJjG0gCqbSY3vwBNIpYn32Wic2', 'Lucas Morgan', 'buyer3@auctionsystem.com', 'BIDDER', 50000000, '1 Harbor Road, San Francisco');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-004', CURRENT_TIMESTAMP, 'rare_hunter', '$2a$10$ys5UkKJhmgmxsGRgjwYgIu/SMjPvJjG0gCqbSY3vwBNIpYn32Wic2', 'Ava Wilson', 'buyer4@auctionsystem.com', 'BIDDER', 20000000, '8 Gallery Avenue, Seattle');

INSERT IGNORE INTO users (id, created_at, username, password_hash, full_name, email, role, balance, shipping_address)
VALUES ('bidder-005', CURRENT_TIMESTAMP, 'student_collector', '$2a$10$ys5UkKJhmgmxsGRgjwYgIu/SMjPvJjG0gCqbSY3vwBNIpYn32Wic2', 'Emma Taylor', 'buyer5@auctionsystem.com', 'BIDDER', 2000000, 'Campus Residence, Boston');


-- =========================================================================
-- SECTION 4: ITEMS AND AUCTIONS (3 Running, 2 Pending)
-- =========================================================================

-- Combo 1: Art / Running
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, artist, creation_year, medium, dimensions)
VALUES ('item-001', CURRENT_TIMESTAMP, 'ART', 'seller-001', 'Dong Ho Folk Painting', 'Famous Vietnamese Dong Ho folk painting', 50000, 'Dong Ho Artisan', 1980, 'OIL_PAINT', '60x80cm');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-001', CURRENT_TIMESTAMP, 'item-001', 'seller-001', 50000, 50000, 5000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 7 DAY), 'RUNNING');

-- Combo 2: Electronics / Running
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand, model, condition_grade, warranty_months, fully_functional)
VALUES ('item-002', CURRENT_TIMESTAMP, 'ELECTRONICS', 'seller-001', 'MacBook Pro M3 Max', 'Latest 2024 MacBook model', 50000000, 'Apple', 'MacBook Pro M3 Max', 'MINT', 12, true);
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-002', CURRENT_TIMESTAMP, 'item-002', 'seller-001', 50000000, 50000000, 1000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 3 DAY), 'RUNNING');

-- Combo 3: Vehicle / Pending
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand, model, manufacturing_year, odo, engine_type, color, has_legal_papers, transmission)
VALUES ('item-003', CURRENT_TIMESTAMP, 'VEHICLE', 'seller-002', 'Honda SH 150i', 'Premium scooter', 80000000, 'Honda', 'SH 150i ABS', 2023, 5000, 'GASOLINE', 'Black', true, 'AUTOMATIC');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-003', CURRENT_TIMESTAMP, 'item-003', 'seller-002', 80000000, 80000000, 2000000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 5 DAY), 'PENDING');

-- Combo 4: Fashion / Running
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, brand, size, material, color, gender, condition_grade, authentic)
VALUES ('item-004', CURRENT_TIMESTAMP, 'FASHION', 'seller-003', 'Authentic Gucci Bag', 'Gucci designer bag from Italy', 15000000, 'Gucci', 'OTHER', 'Genuine leather', 'Brown', 'UNISEX', 'VERY_GOOD', true);
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-004', CURRENT_TIMESTAMP, 'item-004', 'seller-003', 15000000, 15000000, 500000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 2 DAY), 'RUNNING');

-- Combo 5: Collectible / Pending
INSERT IGNORE INTO items (id, created_at, category, owner_id, name, description, starting_price, year_of_origin, rarity_level, condition_grade, has_certificate, origin)
VALUES ('item-005', CURRENT_TIMESTAMP, 'COLLECTIBLE', 'seller-001', 'Indochina Vintage Stamp Collection', 'Rare stamps from the 20th century', 3000000, 1945, 'RARE', 'GOOD', true, 'Vietnam');
INSERT IGNORE INTO auctions (id, created_at, item_id, seller_id, starting_price, current_price, bid_increment, start_time, end_time, status)
VALUES ('auction-005', CURRENT_TIMESTAMP, 'item-005', 'seller-001', 3000000, 3000000, 100000, CURRENT_TIMESTAMP, DATE_ADD(NOW(), INTERVAL 10 DAY), 'PENDING');
