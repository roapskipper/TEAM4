CREATE DATABASE IF NOT EXISTS auction_system;
USE auction_system;

-- 1. Bảng Users (Người dùng)
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) PRIMARY KEY, -- Sử dụng UUID String
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Sẽ lưu hash trong tương lai
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    role VARCHAR(20) DEFAULT 'USER', -- ADMIN, USER
    balance DECIMAL(15, 2) DEFAULT 0.0, -- Tiền trong ví để đấu giá
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 2. Bảng Items (Vật phẩm)
-- Phân loại vật phẩm sẽ do ItemFactory xử lý ở tầng Java
CREATE TABLE IF NOT EXISTS items (
                                     id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    starting_price DECIMAL(15, 2) NOT NULL,
    category VARCHAR(50),
    owner_id VARCHAR(36), -- Người tạo ra/sở hữu vật phẩm
    CONSTRAINT fk_item_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
    );

-- 3. Bảng Auctions (Các phiên đấu giá)
CREATE TABLE IF NOT EXISTS auctions (
                                        id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) UNIQUE NOT NULL, -- 1 vật phẩm tại 1 thời điểm chỉ nằm trong 1 phiên
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    current_price DECIMAL(15, 2),
    highest_bidder_id VARCHAR(36), -- ID người đang dẫn đầu
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACTIVE, CLOSED, CANCELLED
    CONSTRAINT fk_auction_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_winner FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
    );

-- 4. Bảng Bids (Lịch sử đặt giá)
-- Giúp truy vết quá trình đấu giá
CREATE TABLE IF NOT EXISTS bids (
                                    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_user FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
    );