package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.auction.CreateFashionRequestDTO;
import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.dto.item.*;
import com.team4.factory.*;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team4.mapper.ItemMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mục đích: Quản lý mặt hàng trước khi đưa vào đấu giá. Dùng ItemDAO, có thể dùng UserDAO để kiểm tra owner.
 */
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final AuctionDAO auctionDAO;

    private static final java.math.BigDecimal BD_5000 = new java.math.BigDecimal("5000");
    private static final java.math.BigDecimal BD_50000 = new java.math.BigDecimal("50000");
    private static final java.math.BigDecimal BD_1000 = new java.math.BigDecimal("1000");
    private static final java.math.BigDecimal BD_100000 = new java.math.BigDecimal("100000");
    private static final java.math.BigDecimal BD_500000 = new java.math.BigDecimal("500000");
    private static final java.math.BigDecimal BD_1_MILLION = new java.math.BigDecimal("1000000");
    private static final java.math.BigDecimal BD_2_MILLION = new java.math.BigDecimal("2000000");
    private static final java.math.BigDecimal BD_10_MILLION = new java.math.BigDecimal("10000000");
    private static final java.math.BigDecimal BD_50_MILLION = new java.math.BigDecimal("50000000");
    private static final java.math.BigDecimal BD_100_MILLION = new java.math.BigDecimal("100000000");
    private static final java.math.BigDecimal BD_500_MILLION = new java.math.BigDecimal("500000000");
    private static final java.math.BigDecimal BD_2_BILLION = new java.math.BigDecimal("2000000000");
    private static final java.math.BigDecimal BD_5_BILLION = new java.math.BigDecimal("5000000000");
    private static final java.math.BigDecimal BD_10_BILLION = new java.math.BigDecimal("10000000000");

    @Deprecated
    public ItemService(ItemDAO itemDAO, UserDAO userDAO) {
        this(itemDAO, userDAO, null);
    }

    public ItemService(ItemDAO itemDAO, UserDAO userDAO, AuctionDAO auctionDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.auctionDAO = auctionDAO;
    }

    /**
     * Tạo mặt hàng mới.
     * <p>
     * Validation Flow:
     * 1. Check if the user is a valid Seller.
     * 2. Common validation via Item.validateCommonFields().
     * 3. Apply defaults via ItemRequestDefaults.apply().
     * 4. Category-specific validation occurs in the Model constructors during ItemFactory.createItem().
     * <p>
     * Any validation failure (IllegalArgumentException) is caught and wrapped in a consistent BusinessException.
     * 
     * @param sellerId ID người bán
     * @param itemRequest Dữ liệu yêu cầu tạo item
     * @return Item đã được tạo và lưu vào DB
     */
    public ItemResponseDTO createItem(String sellerId, CreateItemRequestDTO requestDTO) {
        logger.info("Creating a new item for seller: sellerId={}, itemName={}, category={}",
                sellerId, requestDTO.getName(), requestDTO.getCategory());

        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
            logger.warn("Item creation failed: seller does not exist or is invalid. sellerId={}", sellerId);
            throw new BusinessException("Seller does not exist.");
        }

        java.sql.Connection conn = null;
        boolean isTx = false;
        try {
            // Map DTO to Factory Request
            ItemRequest itemRequest = mapToItemRequest(sellerId, requestDTO);
            validateCommonItemRequest(itemRequest);
            ItemRequestDefaults.apply(itemRequest);

            // Chọn Factory
            ItemFactory factory = getFactory(itemRequest.getCategory());
            Item item = factory.createItem(itemRequest);

            // Validate price at backend
            validatePrice(item.getCategory(), item.getStartingPrice());

            try {
                conn = com.team4.db.DatabaseManager.getConnection();
                com.team4.db.DatabaseManager.beginTransaction(conn);
                isTx = true;
            } catch (IllegalStateException | java.sql.SQLException e) {
                logger.info("Proceeding without transaction (fallback): {}", e.getMessage());
            }

            if (isTx) {
                if (!itemDAO.insert(conn, item)) {
                    logger.error("System error: unable to save item to database. sellerId={}", sellerId);
                    throw new BusinessException("Unable to create item.");
                }
            } else {
                if (!itemDAO.insert(item)) {
                    logger.error("System error: unable to save item to database. sellerId={}", sellerId);
                    throw new BusinessException("Unable to create item.");
                }
            }

            if (!item.getOwnerId().equals(sellerId)) {
                logger.error("Security/data error: creator does not match item owner. sellerId={}, ownerId={}", sellerId, item.getOwnerId());
                throw new BusinessException("Seller does not own this item.");
            }

            if (auctionDAO != null) {
                java.math.BigDecimal startingPrice = item.getStartingPrice();
                java.math.BigDecimal bidIncrement = calculateDefaultBidIncrement(startingPrice);
                java.time.LocalDateTime endTime = java.time.LocalDateTime.now().plusDays(7);
                Auction auction = new Auction(item.getId(), sellerId, startingPrice, bidIncrement, endTime);
                auction.approve(); // Tự động kích hoạt trạng thái RUNNING (Live)
                
                if (isTx) {
                    if (!auctionDAO.insert(conn, auction)) {
                        logger.error("System error: unable to auto-create auction for item. itemId={}", item.getId());
                        throw new BusinessException("Unable to auto-create auction for this item.");
                    }
                } else {
                    if (!auctionDAO.insert(auction)) {
                        logger.error("System error: unable to auto-create auction for item. itemId={}", item.getId());
                        throw new BusinessException("Unable to auto-create auction for this item.");
                    }
                }
                logger.info("Auto-created auction for item (RUNNING): itemId={}, auctionId={}", item.getId(), auction.getId());
                item.setStatus("RUNNING");
            } else if (isTx) {
                throw new BusinessException("AuctionDAO is required to create items in production.");
            }

            if (isTx) {
                com.team4.db.DatabaseManager.commitTransaction(conn);
            }

            logger.info("Item created successfully: itemId={}, name={}", item.getId(), item.getName());
            return ItemMapper.toItemResponseDTO(item);
        } catch (IllegalArgumentException e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            throw new BusinessException(e.getMessage());
        } catch (BusinessException e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            throw e;
        } catch (Exception e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            logger.error("System error during item creation", e);
            throw new BusinessException("System error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    public Item createItem(String sellerId, ItemRequest itemRequest) {
        logger.info("Creating a new item for seller from ItemRequest: sellerId={}, itemName={}, category={}",
                sellerId, itemRequest.getName(), itemRequest.getCategory());

        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
            logger.warn("Item creation failed: seller does not exist or is invalid. sellerId={}", sellerId);
            throw new BusinessException("Seller does not exist.");
        }

        java.sql.Connection conn = null;
        boolean isTx = false;
        try {
            // Common validation
            validateCommonItemRequest(itemRequest);
            ItemRequestDefaults.apply(itemRequest);

            // Chọn Factory
            ItemFactory factory = getFactory(itemRequest.getCategory());
            Item item = factory.createItem(itemRequest);

            // Validate price at backend
            validatePrice(item.getCategory(), item.getStartingPrice());

            try {
                conn = com.team4.db.DatabaseManager.getConnection();
                com.team4.db.DatabaseManager.beginTransaction(conn);
                isTx = true;
            } catch (IllegalStateException | java.sql.SQLException e) {
                logger.info("Proceeding without transaction (fallback): {}", e.getMessage());
            }

            if (isTx) {
                if (!itemDAO.insert(conn, item)) {
                    logger.error("System error: unable to save item to database. sellerId={}", sellerId);
                    throw new BusinessException("Unable to create item.");
                }
            } else {
                if (!itemDAO.insert(item)) {
                    logger.error("System error: unable to save item to database. sellerId={}", sellerId);
                    throw new BusinessException("Unable to create item.");
                }
            }

            if (!item.getOwnerId().equals(sellerId)) {
                logger.error("Security/data error: creator does not match item owner. sellerId={}, ownerId={}", sellerId, item.getOwnerId());
                throw new BusinessException("Seller does not own this item.");
            }

            if (auctionDAO != null) {
                java.math.BigDecimal startingPrice = item.getStartingPrice();
                java.math.BigDecimal bidIncrement = calculateDefaultBidIncrement(startingPrice);
                java.time.LocalDateTime endTime = java.time.LocalDateTime.now().plusDays(7);
                Auction auction = new Auction(item.getId(), sellerId, startingPrice, bidIncrement, endTime);
                auction.approve(); // Tự động kích hoạt trạng thái RUNNING (Live)
                
                if (isTx) {
                    if (!auctionDAO.insert(conn, auction)) {
                        logger.error("System error: unable to auto-create auction for item. itemId={}", item.getId());
                        throw new BusinessException("Unable to auto-create auction for this item.");
                    }
                } else {
                    if (!auctionDAO.insert(auction)) {
                        logger.error("System error: unable to auto-create auction for item. itemId={}", item.getId());
                        throw new BusinessException("Unable to auto-create auction for this item.");
                    }
                }
                logger.info("Auto-created auction for item (RUNNING): itemId={}, auctionId={}", item.getId(), auction.getId());
                item.setStatus("RUNNING");
            } else if (isTx) {
                throw new BusinessException("AuctionDAO is required to create items in production.");
            }

            if (isTx) {
                com.team4.db.DatabaseManager.commitTransaction(conn);
            }

            logger.info("Item created successfully: itemId={}, name={}", item.getId(), item.getName());
            return item;
        } catch (IllegalArgumentException e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            throw new BusinessException(e.getMessage());
        } catch (BusinessException e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            throw e;
        } catch (Exception e) {
            if (isTx) com.team4.db.DatabaseManager.rollbackTransaction(conn);
            logger.error("System error during item creation", e);
            throw new BusinessException("System error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private ItemFactory getFactory(Item.ItemCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Invalid item category.");
        }
        switch (category) {
            case ART:
                return new ArtFactory();
            case COLLECTIBLE:
                return new CollectibleFactory();
            case ELECTRONICS:
                return new ElectronicsFactory();
            case FASHION:
                return new FashionFactory();
            case VEHICLE:
                return new VehicleFactory();
            default:
                throw new IllegalArgumentException("Invalid item category.");
        }
    }

    /**
     * Rejects invalid common item payloads before factory construction.
     */
    private void validateCommonItemRequest(ItemRequest itemRequest) {
        Item.validateCommonFields(
                itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getCategory(),
                itemRequest.getOwnerId());
    }

    /**
     * Cập nhật thông tin mặt hàng: kiểm tra item thuộc về seller này không, cập nhật DB
     */
    public ItemResponseDTO updateItem(String sellerId, String itemId, String newName, String newDescription) {
        logger.info("Updating item: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Update failed: item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Update failed: seller does not own this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Seller does not own this item.");
        }

        existingItem.setName(newName);
        existingItem.setDescription(newDescription);
        boolean updated = itemDAO.update(existingItem);
        if (updated) {
            logger.info("Item updated successfully: itemId={}", itemId);
        } else {
            logger.error("System error: unable to update item in database. itemId={}", itemId);
            throw new BusinessException("Unable to update item.");
        }
        return ItemMapper.toItemResponseDTO(existingItem);
    }

    /**
     * Xóa mặt hàng
     */
    public void deleteItem(String itemId, String sellerId) {
        logger.info("Deleting item: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Delete failed: item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Delete failed: seller does not own this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Seller does not own this item.");
        }

        if (auctionDAO != null && auctionDAO.findByItemId(itemId) != null) {
            logger.warn("Delete failed: item already has an auction. itemId={}", itemId);
            throw new BusinessException("Cannot delete an item that already has an auction.");
        }

        boolean deleted = itemDAO.delete(itemId);
        if (deleted) {
            logger.info("Item deleted successfully: itemId={}", itemId);
        } else {
            logger.error("System error: unable to delete item from database. itemId={}", itemId);
            throw new BusinessException("Unable to delete item.");
        }
    }

    /**
     * Lấy danh sách mặt hàng theo danh mục, dùng cho trang lọc sản phẩm
     */
    public List<ItemResponseDTO> getItemsByCategory(String category) {
        logger.debug("Loading items by category: category={}", category);
        return itemDAO.findByCategory(category).stream()
                .map(ItemMapper::toItemResponseDTO)
                .collect(Collectors.toList());

    }

    /**
     * Lấy danh sách mặt hàng của 1 seller, dùng cho màn hình quản lý của seller
     */
    public List<ItemResponseDTO> findByOwnerId(String sellerId) {
        logger.debug("Loading items by owner: sellerId={}", sellerId);
        return itemDAO.findByOwnerId(sellerId).stream()
                .map(ItemMapper::toItemResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy mặt hàng theo ID
     */
    public ItemResponseDTO getItemById(String itemId) {
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new BusinessException("Item does not exist.");
        }
        return ItemMapper
                .toItemResponseDTO(item);
    }

    /**
     * Lấy đối tượng Item gốc (model) theo ID để sử dụng nội bộ trong server (nếu cần)
     */
    public Item getRawItemById(String itemId) {
        return itemDAO.findById(itemId);
    }

    /**
     * Helper mapping DTO sang Request
     */
    private ItemRequest mapToItemRequest(String sellerId, CreateItemRequestDTO dto) {
        ItemRequest req = new ItemRequest();
        req.setOwnerId(sellerId);
        req.setName(dto.getName());
        req.setDescription(dto.getDescription());
        req.setStartingPrice(dto.getStartingPrice());
        req.setCategory(dto.getCategory());

        if (dto instanceof CreateArtRequestDTO) {
            CreateArtRequestDTO artDto = (CreateArtRequestDTO) dto;
            req.setArtist(artDto.getArtist());
            req.setCreationYear(artDto.getCreationYear());
            req.setMedium(artDto.getMedium());
            req.setDimensions(artDto.getDimensions());
        } else if (dto instanceof CreateCollectibleRequestDTO) {
            CreateCollectibleRequestDTO colDto = (CreateCollectibleRequestDTO) dto;
            req.setYearOfOrigin(colDto.getYearOfOrigin());
            req.setRarityLevel(colDto.getRarityLevel());
            req.setConditionGrade(colDto.getConditionGrade());
            req.setHasCertificate(colDto.isHasCertificate());
            req.setOrigin(colDto.getOrigin());
        } else if (dto instanceof CreateElectronicsRequestDTO) {
            CreateElectronicsRequestDTO elecDto = (CreateElectronicsRequestDTO) dto;
            req.setBrand(elecDto.getBrand());
            req.setModel(elecDto.getModel());
            req.setItemCondition(elecDto.getItemCondition());
            req.setWarrantyMonths(elecDto.getWarrantyMonths());
            req.setFullyFunctional(elecDto.isFullyFunctional());
        } else if (dto instanceof CreateFashionRequestDTO) {
            CreateFashionRequestDTO fashDto = (CreateFashionRequestDTO) dto;
            req.setBrand(fashDto.getBrand());
            req.setSize(fashDto.getSize());
            req.setMaterial(fashDto.getMaterial());
            req.setColor(fashDto.getColor());
            req.setGender(fashDto.getGender());
            req.setCondition(fashDto.getCondition());
            req.setAuthentic(fashDto.isAuthentic());
        } else if (dto instanceof CreateVehicleRequestDTO) {
            CreateVehicleRequestDTO vehDto = (CreateVehicleRequestDTO) dto;
            req.setBrand(vehDto.getBrand());
            req.setModel(vehDto.getModel());
            req.setManufacturingYear(vehDto.getManufacturingYear());
            req.setOdo(vehDto.getOdo());
            req.setEngineType(vehDto.getEngineType());
            req.setColor(vehDto.getColor()); // No color field in ItemRequest? Wait! Let me double check if ItemRequest has color!
            req.setHasLegalPapers(vehDto.isHasLegalPapers());
            req.setTransmission(vehDto.getTransmission());
        }

        return req;
    }

    /**
     * Validate product pricing rules based on category
     */
    public static void validatePrice(Item.ItemCategory category, java.math.BigDecimal price) {
        if (price == null) {
            throw new BusinessException("Starting price cannot be null.");
        }
        if (price.compareTo(BD_50000) < 0) {
            throw new BusinessException("Minimum starting price is 50,000 VND.");
        }
        if (price.remainder(BD_1000).compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new BusinessException("Price must be a multiple of 1,000 VND (no decimals).");
        }

        java.math.BigDecimal maxPrice;
        switch (category) {
            case ELECTRONICS:
                maxPrice = BD_5_BILLION;
                break;
            case VEHICLE:
                maxPrice = BD_10_BILLION;
                break;
            case ART:
            case COLLECTIBLE:
                maxPrice = BD_2_BILLION;
                break;
            case FASHION:
                maxPrice = BD_500_MILLION;
                break;
            default:
                maxPrice = BD_10_BILLION;
        }
        if (price.compareTo(maxPrice) > 0) {
            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);
            throw new BusinessException("Maximum starting price for " + category + " is " + formatter.format(maxPrice.longValue()) + " VND.");
        }
    }

    private java.math.BigDecimal calculateDefaultBidIncrement(java.math.BigDecimal startingPrice) {
        if (startingPrice.compareTo(BD_100000) < 0) {
            return BD_5000;
        } else if (startingPrice.compareTo(BD_1_MILLION) < 0) {
            return BD_50000;
        } else if (startingPrice.compareTo(BD_10_MILLION) < 0) {
            return BD_100000;
        } else if (startingPrice.compareTo(BD_50_MILLION) < 0) {
            return BD_500000;
        } else if (startingPrice.compareTo(BD_100_MILLION) < 0) {
            return BD_1_MILLION;
        } else {
            return BD_2_MILLION;
        }
    }
}
