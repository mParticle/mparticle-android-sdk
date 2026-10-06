package com.mparticle.integrationtests;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class CommerceTest extends BaselineTest {

    private static Product product(String sku) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("size", "M");
        return new Product.Builder("Product " + sku, sku, 19.99)
                .brand("Brand")
                .category("Category")
                .variant("Blue")
                .couponCode("SAVE10")
                .position(1)
                .quantity(2)
                .customAttributes(attributes)
                .build();
    }

    @Test
    public void purchase() throws Exception {
        MParticle mParticle = start();
        TransactionAttributes transaction = new TransactionAttributes("txn-1")
                .setRevenue(43.97)
                .setTax(3.99)
                .setShipping(0.0)
                .setAffiliation("integration")
                .setCouponCode("SAVE10");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("channel", "app");
        mParticle.logEvent(new CommerceEvent.Builder(Product.PURCHASE, product("sku-1"))
                .addProduct(product("sku-2"))
                .transactionAttributes(transaction)
                .currency("USD")
                .screen("Checkout")
                .customAttributes(attributes)
                .build());
        uploadAndVerify();
    }

    @Test
    public void everyProductAction() throws Exception {
        MParticle mParticle = start();
        String[] actions = {Product.ADD_TO_CART, Product.REMOVE_FROM_CART, Product.ADD_TO_WISHLIST,
                Product.REMOVE_FROM_WISHLIST, Product.CHECKOUT, Product.CLICK, Product.DETAIL,
                Product.REFUND, Product.CHECKOUT_OPTION};
        for (String action : actions) {
            CommerceEvent.Builder builder = new CommerceEvent.Builder(action, product("sku-" + action));
            if (action.equals(Product.REFUND)) {
                builder.transactionAttributes(new TransactionAttributes("txn-refund"));
            }
            if (action.equals(Product.CHECKOUT) || action.equals(Product.CHECKOUT_OPTION)) {
                builder.checkoutStep(2).checkoutOptions("express");
            }
            mParticle.logEvent(builder.build());
        }
        uploadAndVerify();
    }

    @Test
    public void promotionsAndImpressions() throws Exception {
        MParticle mParticle = start();
        Promotion promotion = new Promotion().setId("promo-1").setName("Summer").setCreative("banner").setPosition("top");
        mParticle.logEvent(new CommerceEvent.Builder(Promotion.VIEW, promotion).build());
        mParticle.logEvent(new CommerceEvent.Builder(Promotion.CLICK, promotion).build());
        mParticle.logEvent(new CommerceEvent.Builder(new Impression("Search Results", product("sku-imp")))
                .productListName("Search Results")
                .productListSource("search")
                .build());
        uploadAndVerify();
    }
}
