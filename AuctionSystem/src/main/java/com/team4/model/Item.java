package com.team4.model;

import java.io.Serializable;

public abstract class Item implements Serializable {
    protected String id;
    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String description;

    public Item(String id, String name, double startingPrice, String description) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.description = description;
    }

    public abstract void showInfo();

    public String getName() {
        return name;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
