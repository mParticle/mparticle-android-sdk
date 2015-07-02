package com.mparticle.commerce;

import android.content.Context;

import com.mparticle.MParticle;

import java.util.List;

public class CommerceApi {

    private CommerceApi(){}

    Context mContext;
    public CommerceApi(Context context) {
        mContext = context;
    }

    public Cart cart() {
        return Cart.getInstance(mContext);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param step the checkout progress/step for apps that have a multi-step checkout process
     * @param options a label to associate with the checkout event
     */
    public synchronized void checkout(int step, String options) {
        List<Product> productList = Cart.getInstance(mContext).products();
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, productList.toArray(new Product[productList.size()]))
                .checkoutStep(step)
                .checkoutOptions(options)
                .build();
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     */
    public synchronized void checkout() {
        List<Product> productList = Cart.getInstance(mContext).products();
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, productList.toArray(new Product[productList.size()]))
                .build();
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * By default, this method will *not* clear the cart. You must manually call {@link Cart#clear()}.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     */
    public void purchase(TransactionAttributes attributes) {
        purchase(attributes, false);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     * @param clearCart boolean determining if the cart should remove its contents after the purchase
     */
    public synchronized void purchase(TransactionAttributes attributes, boolean clearCart) {
        List<Product> productList = Cart.getInstance(mContext).products();
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.PURCHASE, productList.toArray(new Product[productList.size()]))
                .transactionAttributes(attributes)
                .build();
        if (clearCart) {
            Cart.getInstance(mContext).clear();
        }
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#REFUND} action for the Products that are
     * currently in the Cart.
     *
     * @param attributes the attributes to associate with this refund. Typically at least the transaction ID is required.
     */
    public void refund(TransactionAttributes attributes) {
        List<Product> productList = Cart.getInstance(mContext).products();
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REFUND, productList.toArray(new Product[productList.size()]))
                .transactionAttributes(attributes)
                .build();
        MParticle.getInstance().logEvent(event);
    }

}
