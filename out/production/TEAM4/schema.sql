CREATE DATABASE IF NOT EXISTS auction_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE auction_system;

CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     full_name VARCHAR(100),
                                     email VARCHAR(100) UNIQUE,
                                     role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL,
                                     balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,

                                     access_level INT,
                                     admin_code VARCHAR(50),

                                     shipping_address VARCHAR(255),
                                     phone_number VARCHAR(20),

                                     store_name VARCHAR(100),
                                     rating DECIMAL(3,2),

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS items (
                                     id VARCHAR(36) PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
                                     description TEXT,
                                     starting_price DECIMAL(15,2) NOT NULL,
                                     category VARCHAR(30) NOT NULL,
                                     owner_id VARCHAR(36),

                                     brand VARCHAR(120),
                                     model VARCHAR(120),
                                     color VARCHAR(50),
                                     condition_status VARCHAR(50),
                                     item_condition VARCHAR(50),

                                     artist VARCHAR(120),
                                     creation_year INT,
                                     medium VARCHAR(120),
                                     dimensions VARCHAR(120),
                                     style VARCHAR(120),
                                     is_original BOOLEAN,
                                     exhibition_history TEXT,

                                     year_of_origin INT,
                                     rarity_level VARCHAR(50),
                                     condition_grade VARCHAR(50),
                                     category_specific VARCHAR(120),
                                     has_certificate BOOLEAN,
                                     origin VARCHAR(120),
                                     special_features TEXT,

                                     serial_number VARCHAR(120),
                                     warranty_months INT,
                                     is_fully_functional BOOLEAN,
                                     technical_spec TEXT,

                                     size VARCHAR(30),
                                     material VARCHAR(120),
                                     gender VARCHAR(30),
                                     is_authentic BOOLEAN,

                                     manufacturing_year INT,
                                     odo INT,
                                     engine_type VARCHAR(50),
                                     license_plate VARCHAR(50),
                                     has_legal_papers BOOLEAN,
                                     transmission VARCHAR(30),

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_items_owner FOREIGN KEY (owner_id)
                                         REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
                                        id VARCHAR(36) PRIMARY KEY,
                                        item_id VARCHAR(36) NOT NULL UNIQUE,
                                        seller_id VARCHAR(36) NOT NULL,
                                        current_highest_bidder_id VARCHAR(36),
                                        current_price DECIMAL(15,2) NOT NULL,
                                        start_time DATETIME NOT NULL,
                                        end_time DATETIME NOT NULL,
                                        status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') NOT NULL DEFAULT 'OPEN',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        version INT NOT NULL DEFAULT 0, -- CONCURRENT BIDDING

                                        CONSTRAINT fk_auctions_item FOREIGN KEY (item_id)
                                            REFERENCES items(id) ON DELETE CASCADE ON UPDATE CASCADE,
                                        CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id)
                                            REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                        CONSTRAINT fk_auctions_highest_bidder FOREIGN KEY (current_highest_bidder_id)
                                            REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS bids (
                                    id VARCHAR(36) PRIMARY KEY,
                                    auction_id VARCHAR(36) NOT NULL,
                                    bidder_id VARCHAR(36) NOT NULL,
                                    bid_amount DECIMAL(15,2) NOT NULL,
                                    bid_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id)
                                        REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE,
                                    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id)
                                        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS auto_biddings (
                                             id VARCHAR(36) PRIMARY KEY,
                                             auction_id VARCHAR(36) NOT NULL,
                                             bidder_id VARCHAR(36) NOT NULL,
                                             max_limit DECIMAL(15,2) NOT NULL,
                                             increment_amount DECIMAL(15,2) NOT NULL,
                                             is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_auto_biddings_auction FOREIGN KEY (auction_id)
                                                 REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE,
                                             CONSTRAINT fk_auto_biddings_bidder FOREIGN KEY (bidder_id)
                                                 REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
                                             CONSTRAINT uq_auto_biddings_auction_bidder UNIQUE (auction_id, bidder_id)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_auctions_status_end_time ON auctions(status, end_time);
CREATE INDEX idx_bids_auction_time ON bids(auction_id, bid_time);
CREATE INDEX idx_auto_biddings_active ON auto_biddings(is_active);