package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.mock.MockContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public class CommerceApiTest {


    private static Cart cart;
    private static CommerceApi commerceApi;
    private static MParticle mockMp;
    private static IdentityApi mockIdentity;
    private static MParticleUser mockCurrentUser;

    @BeforeClass
    public static void setupAll() {
        mockMp = Mockito.mock(MParticle.class);
        cart = new Cart(new MockContext(), 2);
        mockCurrentUser = Mockito.mock(MParticleUser.class);
        mockIdentity = Mockito.mock(IdentityApi.class);
        Mockito.when(mockCurrentUser.getCart()).thenReturn(cart);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockCurrentUser);
        Mockito.when(mockMp.Identity()).thenReturn(mockIdentity);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);

        commerceApi = new CommerceApi(new MockContext());
    }

    @Before
    public void setupEachTest() {
        cart.clear();
        Product.setEqualityComparator(null);
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
        Product product = new Product.Builder("name 1", "sku", 5).build();
        Product product2 = new Product.Builder("name 2", "sku", 2).build();
        cart.add(product).add(product2).add(nullProduct);
        assertEquals(2, cart.products().size());
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
    public void testClearingCartWithPurchase() throws Exception {

        Product product = new Product.Builder("name 1", "sku", 5).build();
        Product product2 = new Product.Builder("name 2", "sku", 2).build();
        cart.add(product, false).add(product2, false);
        TransactionAttributes attributes = new TransactionAttributes("some id");
        CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, cart.products().get(0))
                .products( cart.products())
                .transactionAttributes(attributes)
                .build();
        commerceApi.purchase(attributes, true);
        Mockito.verify(mockMp, Mockito.atLeastOnce()).logEvent(event);
        assertEquals(2, event.getProducts().size());

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