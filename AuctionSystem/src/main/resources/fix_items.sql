-- Chạy file này trong MySQL Workbench hoặc console để fix dữ liệu item thiếu trường
-- Database: auction_system

USE auction_system;

-- item-001: ART — thiếu creation_year, dimensions
UPDATE items 
SET creation_year = 1980, dimensions = '60x80cm'
WHERE id = 'item-001';

-- item-002: ELECTRONICS — thiếu condition_grade, model, warranty_months, fully_functional
UPDATE items 
SET condition_grade = 'MINT',
    model           = 'MacBook Pro M3 Max',
    warranty_months = 12,
    fully_functional = 1
WHERE id = 'item-002';

-- item-003: VEHICLE — thiếu model, odo, engine_type, color, has_legal_papers, transmission
UPDATE items 
SET model            = 'SH 150i ABS',
    odo              = 5000,
    engine_type      = 'GASOLINE',
    color            = 'Đen bóng',
    has_legal_papers = 1,
    transmission     = 'AUTOMATIC'
WHERE id = 'item-003';

-- item-004: FASHION — thiếu size, material, color, gender, condition_grade, authentic
UPDATE items 
SET size           = 'OTHER',
    material       = 'Da bò thật',
    color          = 'Nâu',
    gender         = 'UNISEX',
    condition_grade = 'VERY_GOOD',
    authentic      = 1
WHERE id = 'item-004';

-- item-005: COLLECTIBLE — thiếu year_of_origin, rarity_level, condition_grade, has_certificate, origin
UPDATE items 
SET year_of_origin  = 1945,
    rarity_level    = 'RARE',
    condition_grade = 'GOOD',
    has_certificate = 1,
    origin          = 'Việt Nam'
WHERE id = 'item-005';

-- Kiểm tra kết quả
SELECT id, name, category, condition_grade, model, engine_type, size, rarity_level, creation_year
FROM items
ORDER BY id;
