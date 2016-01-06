package com.mparticle.commerce;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


public class ProductTest {
    @Test
    public void testEqualityComparator() {
        Product.setEqualityComparator(new Product.EqualityComparator() {
            @Override
            public boolean equals(Product product1, Product product2) {
                return product1.getName().equals(product2.getBrand());
            }
        });
        Product product1 = new Product.Builder("name", "sku",2).brand("cool brand!").build();
        Product product2 = new Product.Builder("cool brand!", "sku",2).brand("cool brand!adsflkjh").build();
        assertEquals(product2, product1);
    }

    @Test
    public void testSerializationDeserialization() {
        Product.setEqualityComparator(new Product.EqualityComparator() {
            @Override
            public boolean equals(Product product1, Product product2) {
                return product1.toString().equals(product2.toString());
            }
        });
        Product product = new Product.Builder("product name", "product sku", 301.45)
                .brand("product brand")
                .category("product category")
                .couponCode("product coupon code")
                .name("product name")
                .position(4)
                .variant("product variant")
                .quantity(12.1)
                .build();

        JSONObject productJson = product.toJson();
        Product product2 = Product.fromJson(productJson);
        assertEquals(product, product2);
        product2.setQuantity(10000);
        assertNotEquals(product, product2);
    }
}