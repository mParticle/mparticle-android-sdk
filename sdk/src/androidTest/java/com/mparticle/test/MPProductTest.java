package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MPProduct;

public class MPProductTest extends AndroidTestCase {

    public void testBuilder(){
        MPProduct product = new MPProduct.Builder("test product name", "test-product-sku-0").build();
        assertNotNull(product);
        assertEquals(product.getProductName(), "test product name");
        assertEquals(product.get("ProductSKU"), "test-product-sku-0");
    }
}
