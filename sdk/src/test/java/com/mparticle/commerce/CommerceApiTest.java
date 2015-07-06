package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class CommerceApiTest {


    private static Cart cart;
    private static CommerceApi commerceApi;

    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
        commerceApi = new CommerceApi(new MockContext());
        cart = Cart.getInstance(new MockContext());
    }

    @Before
    public void setupEachTest() {
        cart.clear();
        Cart.setProductEqualityComparator(null);
    }
    @Test
    public void testCheckout() throws Exception {
        commerceApi.checkout();
        commerceApi.checkout(-1, null);
        commerceApi.checkout(0, "");
    }


    @Test
    public void testPurchase() throws Exception {
        Product nullProduct = null;
        cart.add(nullProduct);
        Product product = new Product.Builder("name 1", "sku").build();
        Product product2 = new Product.Builder("name 2", "sku").build();
        cart.add(product).add(product2).add(nullProduct);
        assertEquals(2, cart.products().size());
        cart.add(product).add(product2);
        assertEquals(2, (int) cart.products().get(0).getQuantity());
        assertEquals(2, (int) cart.products().get(1).getQuantity());
        try {
            commerceApi.purchase(null);
        }catch (IllegalStateException stateException){

        }
        assertEquals(2, cart.products().size());
        try {
            commerceApi.purchase(new TransactionAttributes());
        }catch (IllegalStateException stateException){

        }
        assertEquals(2, cart.products().size());
        commerceApi.purchase(new TransactionAttributes("trans id"));
        assertEquals(2, cart.products().size());
        commerceApi.purchase(new TransactionAttributes("trans id"), true);
        assertEquals(0, cart.products().size());
    }

    @Test
    public void testRefund() throws Exception {
        try {
            commerceApi.refund(null, false);
        }catch (IllegalStateException stateexception){

        }
        try {
            commerceApi.refund(new TransactionAttributes(), false);
        }catch (IllegalStateException illegalstateexception){

        }
        commerceApi.refund(new TransactionAttributes("trans id"), false);
    }
}