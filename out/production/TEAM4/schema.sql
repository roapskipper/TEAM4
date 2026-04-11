CREATE DATABASE IF NOT EXISTS auction_system;
USE auction_system;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    role VARCHAR(20) DEFAULT 'USER',
    balance DECIMAL(15, 2) DEFAULT 0.0,
    store_name VARCHAR(100),
    rating DECIMAL(3, 2) DEFAULT 5.0,
    shipping_address VARCHAR(255),
    phone_number VARCHAR(20),
    access_level INT,
    admin_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD COLUMN full_name VARCHAR(100);
ALTER TABLE users ADD COLUMN email VARCHAR(100);
ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER';
ALTER TABLE users ADD COLUMN balance DECIMAL(15, 2) DEFAULT 0.0;
ALTER TABLE users ADD COLUMN store_name VARCHAR(100);
ALTER TABLE users ADD COLUMN rating DECIMAL(3, 2) DEFAULT 5.0;
ALTER TABLE users ADD COLUMN shipping_address VARCHAR(255);
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
ALTER TABLE users ADD COLUMN access_level INT;
ALTER TABLE users ADD COLUMN admin_code VARCHAR(50);

CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    starting_price DECIMAL(15, 2) NOT NULL,
    current_price DECIMAL(15, 2),
    category VARCHAR(50),
    owner_id VARCHAR(36),
    brand VARCHAR(100),
    model VARCHAR(100),
    manufacturing_year INT,
    odo INT,
    artist VARCHAR(100),
    creation_year INT,
    size VARCHAR(30),
    is_authentic BOOLEAN,
    condition_status VARCHAR(50),
    serial_number VARCHAR(100),
    warranty_months INT,
    tech_spec TEXT,
    rarity_level VARCHAR(50),
    has_certificate BOOLEAN,
    origin VARCHAR(100),
    transmission VARCHAR(50),
    CONSTRAINT fk_item_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE items ADD COLUMN current_price DECIMAL(15, 2);
ALTER TABLE items ADD COLUMN owner_id VARCHAR(36);
ALTER TABLE items ADD COLUMN brand VARCHAR(100);
ALTER TABLE items ADD COLUMN model VARCHAR(100);
ALTER TABLE items ADD COLUMN manufacturing_year INT;
ALTER TABLE items ADD COLUMN odo INT;
ALTER TABLE items ADD COLUMN artist VARCHAR(100);
ALTER TABLE items ADD COLUMN creation_year INT;
ALTER TABLE items ADD COLUMN size VARCHAR(30);
ALTER TABLE items ADD COLUMN is_authentic BOOLEAN;
ALTER TABLE items ADD COLUMN condition_status VARCHAR(50);
ALTER TABLE items ADD COLUMN serial_number VARCHAR(100);
ALTER TABLE items ADD COLUMN warranty_months INT;
ALTER TABLE items ADD COLUMN tech_spec TEXT;
ALTER TABLE items ADD COLUMN rarity_level VARCHAR(50);
ALTER TABLE items ADD COLUMN has_certificate BOOLEAN;
ALTER TABLE items ADD COLUMN origin VARCHAR(100);
ALTER TABLE items ADD COLUMN transmission VARCHAR(50);

CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) UNIQUE NOT NULL,
    seller_id VARCHAR(36),
    current_highest_bidder_id VARCHAR(36),
    current_price DECIMAL(15, 2),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT fk_auction_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

ALTER TABLE auctions ADD COLUMN seller_id VARCHAR(36);
ALTER TABLE auctions ADD COLUMN current_highest_bidder_id VARCHAR(36);
ALTER TABLE auctions ADD COLUMN current_price DECIMAL(15, 2);
ALTER TABLE auctions ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

CREATE TABLE IF NOT EXISTS bids (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_user FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);
