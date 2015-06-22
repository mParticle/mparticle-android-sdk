package com.mparticle.commerce;


import java.util.List;

public class Cart {

    public Cart add(Product... product) {
        return this;
    }

    public boolean remove(Product product) {
        return true;
    }

    public void checkout(int step, String options) {

    }

    public void checkout() {

    }

    public void purchase(TransactionAttributes attributes) {

    }

    public void purchase(TransactionAttributes attributes, boolean clearCart) {

    }

    public void refund() {

    }

    public void refund(TransactionAttributes attributes) {

    }

    @Override
    public String toString() {
        return super.toString();
    }

    public Cart clear() {
        return this;
    }

    public int findProduct(String id) {
        return 0;
    }

    public List<Product> products() {
        return null;
    }

    public Cart setQuantity(int index, int i) {
        return this;
    }
}
