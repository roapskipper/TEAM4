CREATE DATABASE IF NOT EXISTS auction_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_system;

-- =========================================================
-- USERS
-- Single-table inheritance cho Admin / Seller / Bidder
-- =========================================================
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,

    -- Admin
    access_level TINYINT UNSIGNED NULL, -- Tiết kiệm bộ nhớ, số nguyên từ 0-255
    admin_code_hash VARCHAR(128) NULL,

    -- Bidder
    shipping_address VARCHAR(255) NULL,
    phone_number VARCHAR(20) NULL,

    -- Seller
    store_name VARCHAR(100) NULL,
    rating DECIMAL(3,2) NULL DEFAULT 5.00,

    -- Dùng CONSTRAINT để dễ bảo trì/debug
    -- Không kiểm tra regex vì hiệu năng kém
    CONSTRAINT chk_users_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_users_access_level CHECK (access_level IS NULL OR access_level IN (1, 2)),
    CONSTRAINT chk_users_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5))
    );
-- lọc người dùng theo vai trò
CREATE INDEX idx_users_role ON users(role);

-- =========================================================
-- ITEMS
-- Single-table inheritance cho Art / Collectible / Electronics / Fashion / Vehicle
-- =========================================================
CREATE TABLE IF NOT EXISTS items (
                                     id VARCHAR(36) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    starting_price DECIMAL(19,2) NOT NULL,
    category ENUM('ART', 'ELECTRONICS', 'FASHION', 'VEHICLE', 'COLLECTIBLE') NOT NULL,
    owner_id VARCHAR(36) NOT NULL,

    brand VARCHAR(120) NULL,
    model VARCHAR(120) NULL,
    color VARCHAR(50) NULL,
    condition_grade ENUM('POOR', 'FAIR', 'GOOD', 'VERY_GOOD', 'EXCELLENT', 'MINT') NULL,

    -- Art
    artist VARCHAR(50) NULL,
    creation_year INT NULL,
    medium ENUM(
                   'OIL_PAINT',
                   'WATERCOLOR',
                   'ACRYLIC',
                   'GOUACHE',
                   'PASTEL',
                   'INK',
                   'SCULPTURE_MARBLE',
                   'SCULPTURE_WOOD',
                   'SCULPTURE_CERAMIC',
                   'PHOTOGRAPHY',
                   'MIXED_MEDIA',
                   'OTHER'
               ) NULL,
    dimensions VARCHAR(50) NULL,

    -- Collectible
    year_of_origin INT NULL,
    rarity_level ENUM('COMMON', 'UNCOMMON', 'RARE', 'VERY_RARE', 'ULTRA_RARE') NULL,
    has_certificate BOOLEAN NULL,
    origin VARCHAR(120) NULL,

    -- Electronics
    warranty_months INT NULL,
    fully_functional BOOLEAN NULL,

    -- Fashion
    size ENUM('XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL', 'OTHER') NULL,
    material VARCHAR(120) NULL,
    gender ENUM('UNISEX', 'MALE', 'FEMALE') NULL,
    authentic BOOLEAN NULL,

    -- Vehicle
    manufacturing_year INT NULL,
    odo INT NULL,
    engine_type ENUM(
                        'GASOLINE',
                        'DIESEL',
                        'ELECTRIC',
                        'HYBRID',
                        'PLUG_IN_HYBRID',
                        'HYDROGEN',
                        'OTHER'
                    ) NULL,
    has_legal_papers BOOLEAN NULL,
    transmission ENUM('MANUAL', 'AUTOMATIC', 'CVT', 'DCT', 'OTHER') NULL,

    CONSTRAINT fk_items_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE RESTRICT -- Không cho phép xóa người dùng nếu họ vẫn còn sản phẩm trong hệ thống
    ON UPDATE CASCADE, -- Nếu id của người dùng thay đổi, giá trị owner_id ở bảng sản phẩm sẽ tự động cập nhật theo

    CONSTRAINT chk_items_starting_price_non_negative CHECK (starting_price >= 0),
    CONSTRAINT chk_items_warranty_non_negative CHECK (warranty_months IS NULL OR warranty_months >= 0),
    CONSTRAINT chk_items_odo_non_negative CHECK (odo IS NULL OR odo >= 0)
    );

CREATE INDEX idx_items_category ON items(category); -- Tăng tốc khi người dùng lọc sản phẩm theo danh mục
CREATE INDEX idx_items_owner_id ON items(owner_id); -- Tăng tốc khi hiển thị danh sách "Sản phẩm của tôi" cho người bán

-- =========================================================
-- AUCTIONS
-- =========================================================
CREATE TABLE IF NOT EXISTS auctions (
                                        id VARCHAR(36) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    item_id VARCHAR(36) NOT NULL,
    seller_id VARCHAR(36) NOT NULL,
    current_highest_bidder_id VARCHAR(36) NULL,

    starting_price DECIMAL(19,2) NOT NULL,
    bid_increment DECIMAL(19,2) NOT NULL,
    current_price DECIMAL(19,2) NOT NULL,

    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('PENDING', 'RUNNING', 'FINISHED', 'PAID', 'CANCELLED') NOT NULL,

    CONSTRAINT fk_auctions_item
    FOREIGN KEY (item_id) REFERENCES items(id)
    ON DELETE RESTRICT -- Ngăn chặn việc xóa sản phẩm nếu sản phẩm đó đã hoặc đang nằm trong một phiên đấu giá để bảo vệ lịch sử giao dịch.
    ON UPDATE CASCADE, -- Tự động cập nhật mã sản phẩm trong bảng đấu giá nếu mã id ở bảng sản phẩm thay đổi.

    CONSTRAINT fk_auctions_seller
    FOREIGN KEY (seller_id) REFERENCES users(id)
    ON DELETE RESTRICT -- Chặn tuyệt đối việc xóa tài khoản người dùng nếu họ đang hoặc đã từng có phiên đấu giá.
    ON UPDATE CASCADE, -- Nếu mã định danh (id) của người dùng thay đổi, hệ thống sẽ tự động cập nhật lại mã đó ở tất cả các phiên đấu giá liên quan.

    CONSTRAINT fk_auctions_highest_bidder
    FOREIGN KEY (current_highest_bidder_id) REFERENCES users(id)
    ON DELETE SET NULL -- Nếu người đang dẫn đầu bị xóa khỏi hệ thống, cột current_highest_bidder_id trong phiên đấu giá sẽ tự động chuyển thành NULL thay vì RESTRICT,giúp phiên đấu giá vẫn tồn tại và có thể tiếp tục với người khác hoặc để trống nếu chưa có ai bid lại.
    ON UPDATE CASCADE, -- Nếu id của người dùng thay đổi, mã người dẫn đầu trong phiên đấu giá sẽ tự động cập nhật theo.

    CONSTRAINT chk_auctions_starting_price_positive CHECK (starting_price > 0),
    CONSTRAINT chk_auctions_bid_increment_positive CHECK (bid_increment > 0),
    CONSTRAINT chk_auctions_current_price_valid CHECK (current_price >= starting_price),
    CONSTRAINT chk_auctions_time_valid CHECK (end_time > start_time)
    );

-- Tăng tốc độ tìm kiếm
CREATE INDEX idx_auctions_status_end_time ON auctions(status, end_time);
CREATE INDEX idx_auctions_item_id ON auctions(item_id);
CREATE INDEX idx_auctions_seller_id ON auctions(seller_id);

-- =========================================================
-- BID TRANSACTIONS
-- Lưu cả created_at và bid_time để khớp model hiện tại
-- =========================================================
CREATE TABLE IF NOT EXISTS bid_transactions (
                                                id VARCHAR(36) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bid_amount DECIMAL(19,2) NOT NULL,
    bid_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bid_transactions_auction
    FOREIGN KEY (auction_id) REFERENCES auctions(id)
    ON DELETE CASCADE -- Nếu một phiên đấu giá bị xóa khỏi hệ thống, toàn bộ lịch sử đặt giá liên quan đến phiên đó sẽ tự động bị xóa sạch theo.
    ON UPDATE CASCADE,-- Nếu mã id của phiên đấu giá thay đổi, mã auction_id trong lịch sử đặt giá sẽ tự động cập nhật theo để duy trì liên kết.

    CONSTRAINT fk_bid_transactions_bidder
    FOREIGN KEY (bidder_id) REFERENCES users(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT chk_bid_transactions_amount_positive CHECK (bid_amount > 0)
    );

CREATE INDEX idx_bid_transactions_auction_time ON bid_transactions(auction_id, bid_time);
CREATE INDEX idx_bid_transactions_bidder_time ON bid_transactions(bidder_id, bid_time);

-- =========================================================
-- AUTO BIDDINGS
-- =========================================================
CREATE TABLE IF NOT EXISTS auto_biddings (
                                             id VARCHAR(36) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    max_limit DECIMAL(19,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_auto_biddings_auction
    FOREIGN KEY (auction_id) REFERENCES auctions(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

    CONSTRAINT fk_auto_biddings_bidder
    FOREIGN KEY (bidder_id) REFERENCES users(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT uq_auto_biddings_auction_bidder UNIQUE (auction_id, bidder_id),
    CONSTRAINT chk_auto_biddings_max_limit_positive CHECK (max_limit > 0)
    );

CREATE INDEX idx_auto_biddings_active ON auto_biddings(is_active);
CREATE INDEX idx_auto_biddings_auction_id ON auto_biddings(auction_id);
CREATE INDEX idx_auto_biddings_bidder_id ON auto_biddings(bidder_id);
