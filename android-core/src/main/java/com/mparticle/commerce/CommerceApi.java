package com.mparticle.commerce;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MParticle;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Helper class used to access the shopping Cart and to log CommerceEvents
 */
public class CommerceApi {

    private CommerceApi(){}

    Context mContext;
    public CommerceApi(Context context) {
        mContext = context;
    }

    private Cart cart() {
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        if (user != null) {
            return user.getCart();
        } else {
            return null;
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param step the checkout progress/step for apps that have a multi-step checkout process
     * @param options a label to associate with the checkout event
     */
    public synchronized void checkout(int step, String options) {
        Cart cart = cart();
        if (cart == null) {
            Logger.error("Unable to log checkout event - no mParticle user identified.");
            return;
        }
        List<Product> productList = cart.products();
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT, productList.get(0))
                    .checkoutStep(step)
                    .checkoutOptions(options)
                    .products(productList)
                    .build();
            MParticle.getInstance().logEvent(event);
        } else {
            Logger.error("checkout() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     */
    public synchronized void checkout() {
        Cart cart = cart();
        if (cart == null) {
            Logger.error("Unable to log checkout event - no mParticle user identified.");
            return;
        }
        List<Product> productList = cart.products();
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT, productList.get(0))
                    .products(productList)
                    .build();
            MParticle.getInstance().logEvent(event);
        }else {
            Logger.error("checkout() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#PURCHASE} action for the Products that are
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
     * Log a {@link CommerceEvent} with the {@link Product#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     * @param clearCart boolean determining if the cart should remove its contents after the purchase
     */
    public synchronized void purchase(TransactionAttributes attributes, boolean clearCart) {
        Cart cart = cart();
        if (cart == null) {
            Logger.error("Unable to log purchase event - no mParticle user identified.");
            return;
        }
        List<Product> productList = cart.products();
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, productList.get(0))
                    .products(productList)
                    .transactionAttributes(attributes)
                    .build();
            if (clearCart) {
                cart.clear();
            }
            MParticle.getInstance().logEvent(event);
        }else {
            Logger.error("purchase() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#REFUND} action for the Products that are
     * currently in the Cart.
     *
     * @param attributes the attributes to associate with this refund. Typically at least the transaction ID is required.
     */
    public void refund(TransactionAttributes attributes, boolean clearCart) {
        Cart cart = cart();
        if (cart == null) {
            Logger.error("Unable to log refund event - no mParticle user identified.");
            return;
        }
        List<Product> productList = cart.products();
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.REFUND, productList.get(0))
                    .products(productList)
                    .transactionAttributes(attributes)
                    .build();
            if (clearCart) {
                cart.clear();
            }
            MParticle.getInstance().logEvent(event);
        } else {
            Logger.error("refund() called but there are no Products in the Cart, no event was logged.");
        }
    }
}