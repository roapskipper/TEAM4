package com.team4;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.BidTransactionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.factory.ItemFactory;
import com.team4.factory.VehicleFactory;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.Bidder;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.service.AuctionManager;
import com.team4.util.DatabaseSetup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        DatabaseSetup.initDatabase();

        System.out.println("==========================================================");
        System.out.println("                TEAM 4 AUCTION SYSTEM                     ");
        System.out.println("==========================================================");

        UserDAOImpl userDAO = new UserDAOImpl();
        ItemDAOImpl itemDAO = new ItemDAOImpl();
        AuctionDAOImpl auctionDAO = new AuctionDAOImpl();
        BidTransactionDAOImpl bidDAO = new BidTransactionDAOImpl();
        AuctionManager auctionManager = AuctionManager.getInstance();

        System.out.println("[SCENARIO] Step 1: create users...");
        Seller mercedesHanoi = new Seller("mercedes_hn", "pass123", "Mercedes Vietnam Center");
        Bidder cuongDollar = new Bidder("cuong_dollar", "pass456", 2000000.0, "TP.HCM", "09001");
        Bidder tungMtp = new Bidder("tung_mtp", "pass789", 1500000.0, "Thai Binh", "09002");

        userDAO.save(mercedesHanoi);
        userDAO.save(cuongDollar);
        userDAO.save(tungMtp);

        System.out.println("[SCENARIO] Step 2: create item...");
        ItemFactory factory = new VehicleFactory(
                "Mercedes Maybach S680", 250000.0, "Flagship model year 2025", mercedesHanoi.getId(),
                "Mercedes", "Maybach S680", 2025, 0, "V12", "Obsidian White", "UN-9999", true, "Auto"
        );
        Item sieuXe = factory.createItem();
        itemDAO.save(sieuXe);

        System.out.println("[SCENARIO] Step 3: open auction...");
        Auction auction = new Auction(
                sieuXe.getId(),
                mercedesHanoi.getId(),
                sieuXe.getStartingPrice(),
                LocalDateTime.now().plusHours(1)
        );
        auctionManager.createAuction(auction);

        System.out.println("--- START BIDDING ---");
        if (auctionManager.placeBid(tungMtp, auction, 260000.0)) {
            auctionDAO.update(auction);
            bidDAO.save(new BidTransaction(auction.getId(), tungMtp.getId(), 260000.0));
        }

        if (auctionManager.placeBid(cuongDollar, auction, 350000.0)) {
            auctionDAO.update(auction);
            bidDAO.save(new BidTransaction(auction.getId(), cuongDollar.getId(), 350000.0));
        }

        Optional<Auction> resultFromDb = auctionDAO.findById(auction.getId());
        resultFromDb.ifPresent(dbAuc -> {
            System.out.println("--- DATABASE RESULT ---");
            System.out.println("Item ID: " + dbAuc.getItemId());
            System.out.println("Current Price: $" + dbAuc.getCurrentPrice());
            System.out.println("Current Highest Bidder ID: " + dbAuc.getCurrentHighestBidderId());
        });

        System.out.println("[LOGS] Bidding history from database:");
        List<BidTransaction> logs = bidDAO.findByAuctionId(auction.getId());
        for (BidTransaction log : logs) {
            System.out.println(" -> " + log.getBidTime().toLocalTime() + " | bidder " + log.getBidderId() + " | $" + log.getBidAmount());
        }

        System.out.println("==========================================================");
        System.out.println("                    RUN COMPLETED                         ");
        System.out.println("==========================================================");
    }
}
