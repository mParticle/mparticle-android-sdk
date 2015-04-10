package com.mparticle;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.*;

/**
 * Created by sdozor on 4/7/15.
 */
public class MPProductTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBuilder(){
        MPProduct product = new MPProduct.Builder("test product name", "test-product-sku-0").build();
        assertNotNull(product);
        assertEquals(product.getProductName(), "test product name");
        assertEquals(product.get("ProductSKU"), "test-product-sku-0");
    }

    @Test
    public void testGetUnitPrice() throws Exception {

    }

    @Test
    public void testGetQuantity() throws Exception {

    }

    @Test
    public void testGetProductCategory() throws Exception {

    }

    @Test
    public void testGetTotalAmount() throws Exception {

    }

    @Test
    public void testGetTaxAmount() throws Exception {

    }

    @Test
    public void testGetShippingAmount() throws Exception {

    }

    @Test
    public void testGetCurrencyCode() throws Exception {

    }

    @Test
    public void testGetAffiliation() throws Exception {

    }

    @Test
    public void testGetTransactionId() throws Exception {

    }

    @Test
    public void testGetProductName() throws Exception {

    }

    @Test
    public void testGet() throws Exception {

    }
}