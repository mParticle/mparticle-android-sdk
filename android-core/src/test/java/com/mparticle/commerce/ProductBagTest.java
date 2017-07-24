package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductBagTest {

    @BeforeClass
    public static void setup() {
        MParticle.setInstance(new MockMParticle());
    }

    @Test
    public void testGetName() throws Exception {
        ProductBag bag = new ProductBag("cool name");
        assertEquals("cool name", bag.getName());
    }

    @Test
    public void testGetProducts() throws Exception {
        ProductBag bag = new ProductBag("cool name");
        bag.products.add(new Product.Builder("product name", "sku", 1.0).build());
        assertEquals("product name", bag.products.get(0).getName());
        try {
            bag.getProducts().add(new Product.Builder("product name 2", "sku 2", 1.0).build());
        }catch (UnsupportedOperationException e) {
            return;
        }
        fail("Bag was able to be altered.");
    }
}