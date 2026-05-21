package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.auction.*;
import com.team4.dto.item.*;
import com.team4.factory.*;
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

    public ItemService(ItemDAO itemDAO, UserDAO userDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /**
     * Tạo mặt hàng mới: kiểm tra seller có tồn tại và đúng role không, tạo object Item qua Factory, lưu xuống DB
     */
    public ItemResponseDTO createItem(String sellerId, CreateItemRequestDTO requestDTO) {
        logger.info("Creating a new item for seller: sellerId={}, itemName={}, category={}",
                sellerId, requestDTO.getName(), requestDTO.getCategory());

        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
            logger.warn("Item creation failed: Seller does not exist or is invalid. sellerId={}", sellerId);
            throw new BusinessException("Seller does not exist.");
        }

        // Map DTO to Factory Request
        ItemRequest itemRequest = mapToItemRequest(sellerId, requestDTO);

        // Chọn Factory
        ItemFactory factory;
        switch (itemRequest.getCategory()) {
            case ART:
                factory = new ArtFactory();
                break;
            case COLLECTIBLE:
                factory = new CollectibleFactory();
                break;
            case ELECTRONICS:
                factory = new ElectronicsFactory();
                break;
            case FASHION:
                factory = new FashionFactory();
                break;
            case VEHICLE:
                factory = new VehicleFactory();
                break;
            default:
                logger.warn("Item creation failed: Invalid item category. category={}", itemRequest.getCategory());
                throw new BusinessException("Invalid item category.");
        }
        // Tạo và lưu
        Item item = factory.createItem(itemRequest);
        if (!itemDAO.insert(item)) {
            logger.error("System error: Unable to save item to database. sellerId={}", sellerId);
            throw new BusinessException("Unable to create item.");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.error("Security/Data error: Creator does not match item owner. sellerId={}, ownerId={}", sellerId, item.getOwnerId());
            throw new BusinessException("ERROR: Seller is not the owner of the item.");
        }
        logger.info("Successfully created item: itemId={}, name={}", item.getId(), item.getName());
        return ItemMapper.toItemResponseDTO(item);
    }

    /**
     * Cập nhật thông tin mặt hàng: kiểm tra item thuộc về seller này không, cập nhật DB
     */
    public ItemResponseDTO updateItem(String sellerId, String itemId, String newName, String newDescription) {
        logger.info("Updating item: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Update failed: Item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Update failed: Seller does not have ownership of this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Ownership error.");
        }

        existingItem.setName(newName);
        existingItem.setDescription(newDescription);
        boolean updated = itemDAO.update(existingItem);
        if (updated) {
            logger.info("Successfully updated item: itemId={}", itemId);
        } else {
            logger.error("System error: Unable to update item in database. itemId={}", itemId);
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
            logger.warn("Deletion failed: Item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Deletion failed: Seller does not have ownership of this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Ownership error.");
        }

        boolean deleted = itemDAO.delete(itemId);
        if (deleted) {
            logger.info("Successfully deleted item: itemId={}", itemId);
        } else {
            logger.error("System error: Unable to delete item from database. itemId={}", itemId);
        }
    }

    /**
     * Lấy danh sách mặt hàng theo danh mục, dùng cho trang lọc sản phẩm
     */
    public List<ItemResponseDTO> getItemsByCategory(String category) {
        logger.debug("Fetching items by category: category={}", category);
        return itemDAO.findByCategory(category).stream()
                .map(ItemMapper::toItemResponseDTO)
                .collect(Collectors.toList());

    }

    /**
     * Lấy danh sách mặt hàng của 1 seller, dùng cho màn hình quản lý của seller
     */
    public List<ItemResponseDTO> findByOwnerId(String sellerId) {
        logger.debug("Fetching items by owner: sellerId={}", sellerId);
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
    public void validatePrice(Item.ItemCategory category, java.math.BigDecimal price) {
        if (price == null) {
            throw new BusinessException("Starting price cannot be null.");
        }
        if (price.compareTo(new java.math.BigDecimal("50000")) < 0) {
            throw new BusinessException("Minimum starting price is 50,000 VND.");
        }
        if (price.remainder(new java.math.BigDecimal("1000")).compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new BusinessException("Price must be a multiple of 1,000 VND (no decimals).");
        }

        java.math.BigDecimal maxPrice;
        switch (category) {
            case ELECTRONICS:
                maxPrice = new java.math.BigDecimal("5000000000");
                break;
            case VEHICLE:
                maxPrice = new java.math.BigDecimal("10000000000");
                break;
            case ART:
            case COLLECTIBLE:
                maxPrice = new java.math.BigDecimal("2000000000");
                break;
            case FASHION:
                maxPrice = new java.math.BigDecimal("500000000");
                break;
            default:
                maxPrice = new java.math.BigDecimal("10000000000");
        }
        if (price.compareTo(maxPrice) > 0) {
            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);
            throw new BusinessException("Maximum starting price for " + category + " is " + formatter.format(maxPrice.longValue()) + " VND.");
        }
    }
}
