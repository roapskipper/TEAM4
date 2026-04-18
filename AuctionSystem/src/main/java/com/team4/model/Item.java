package com.team4.model;

public abstract class Item extends Entity {
    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String description;

    public Item(String id, String name,double startingPrice, String desc) {
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.description = desc;
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double newBid) {
        if (newBid > this.currentPrice) {
            this.currentPrice = newBid;
        } else {
            System.out.println("Lỗi: Giá thầu " + newBid + " phải cao hơn giá hiện tại " + currentPrice);
        }
    }

    public abstract void showInfo();
}
