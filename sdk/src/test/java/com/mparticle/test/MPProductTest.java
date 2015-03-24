package com.mparticle.test;

import com.mparticle.MPProduct;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class MPProductTest  {

    @Test
    public void testBuilder(){
        MPProduct product = new MPProduct.Builder("test product name", "test-product-sku-0").build();
        assertNotNull(product);
        assertEquals(product.getProductName(), "test product name");
        assertEquals(product.get("ProductSKU"), "test-product-sku-0");
    }
}
