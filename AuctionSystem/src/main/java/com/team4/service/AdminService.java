package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Admin;
import com.team4.dao.ItemDAO;
import com.team4.dao.AuctionDAO;
import com.team4.service.UserService;
import com.team4.service.ItemService;
import com.team4.service.AuctionService;

/**
 * Mục đích: Gom các use case quản trị cấp cao. Gọi AuctionService, UserService, ItemService thay vì tự viết lại logic.
 * Admin service điều phối, không copy nghiệp vụ của service khác.
 */
public class AdminService {
}
