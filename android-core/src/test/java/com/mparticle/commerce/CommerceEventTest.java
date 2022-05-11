package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;
import com.mparticle.mock.utils.RandomUtils;
import com.mparticle.testutils.AndroidUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class CommerceEventTest {

    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
    }
    @Test
    public void testScreen() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).screen("some screen name").build();
        assertEquals("some screen name", event.getScreen());
    }

    @Test
    public void testAddProduct() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        Product product2 = new Product.Builder("name 2", "sku 2", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).addProduct(product2).build();
        assertEquals("name 2", event.getProducts().get(1).getName());

        final AndroidUtils.Mutable<String> errorMessage = new AndroidUtils.Mutable<String>(null);
        Logger.setLogHandler(new Logger.DefaultLogHandler() {
            @Override
            public void log(MParticle.LogLevel priority, Throwable error, String messages) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages;
                }
            }
        });
        new CommerceEvent.Builder(Promotion.VIEW, new Promotion().setId("whatever")).addProduct(product2).build();
        assertNotNull("Should have logged Error", errorMessage.value);
        errorMessage.value = null;

        new CommerceEvent.Builder(new Impression("name", product)).addProduct(product2).build();
        assertNotNull("Should have loggedError", errorMessage.value);
        errorMessage.value = null;
    }

    @Test
    public void testTransactionAttributes() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).transactionAttributes(new TransactionAttributes().setId("the id")).build();
        assertEquals("the id", event.getTransactionAttributes().getId());

        final AndroidUtils.Mutable<String> errorMessage = new AndroidUtils.Mutable<String>(null);
        Logger.setLogHandler(new Logger.DefaultLogHandler() {
            @Override
            public void log(MParticle.LogLevel priority, Throwable error, String messages) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages;
                }
            }
        });
        new CommerceEvent.Builder(Product.PURCHASE, product).build();
        assertNotNull("Should have logged Error", errorMessage.value);

    }

    @Test
    public void testCurrency() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).currency("test").build();
        assertEquals("test", event.getCurrency());
    }

    @Test
    public void testNonInteraction() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(true).build();
        assertTrue(event.getNonInteraction());
         event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(false).build();
        assertFalse(event.getNonInteraction());
    }

    @Test
    public void testCustomAttributes() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("cool attribute key", "cool attribute value");
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).customAttributes(attributes).nonInteraction(true).build();
        assertEquals("cool attribute value", event.getCustomAttributeStrings().get("cool attribute key"));
    }

    @Test
    public void testAddPromotion() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder("promo", new Promotion().setId("promo id")).nonInteraction(true).build();
        assertEquals("promo id", event.getPromotions().get(0).getId());

        final AndroidUtils.Mutable<String> errorMessage = new AndroidUtils.Mutable<String>(null);
        Logger.setLogHandler(new Logger.DefaultLogHandler() {
            @Override
            public void log(MParticle.LogLevel priority, Throwable error, String messages) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages;
                }
            }
        });

        new CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(true).addPromotion(new Promotion().setId("promo id")).build();
        assertNotNull("Should have logged Error", errorMessage.value);
    }

    @Test
    public void testCheckoutStep() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder("promo", new Promotion().setId("promo id")).checkoutStep(100).nonInteraction(true).build();
        assertEquals(new Integer(100), event.getCheckoutStep());
    }

    @Test
    public void testAddImpression() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder(new Impression("name", product)).addImpression(new Impression("name 2", product)).nonInteraction(true).build();
        assertEquals("name 2", event.getImpressions().get(1).getListName());
    }

    @Test
    public void testCheckoutOptions() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder("promo", new Promotion().setId("promo id")).checkoutStep(100).checkoutOptions("some checkout options").nonInteraction(true).build();
        assertEquals("some checkout options", event.getCheckoutOptions());
    }

    @Test
    public void testProductListName() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder("promo", new Promotion().setId("promo id")).productListName("the list name").nonInteraction(true).build();
        assertEquals("the list name", event.getProductListName());

    }

    @Test
    public void testProductListSource() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent event = new CommerceEvent.Builder("promo", new Promotion().setId("promo id")).productListSource("the list source").nonInteraction(true).build();
        assertEquals("the list source", event.getProductListSource());
    }

    @Test
    public void testCustomFlags() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent.Builder builder = new CommerceEvent.Builder(Product.CLICK, product);

        CommerceEvent event = builder.build();
        assertNull(event.getCustomFlags());

        Map<String, List<String>> attributes = RandomUtils.getInstance().getRandomCustomFlags(RandomUtils.getInstance().randomInt(1, 10));
        for (Map.Entry<String, List<String>> attribute: attributes.entrySet()) {
            for (String value : attribute.getValue()) {
                builder.addCustomFlag(attribute.getKey(), value);
            }
        }
        event = builder.build();
        attributes.remove(null);
        assertEquals(attributes.size(), event.getCustomFlags().size());
        for (Map.Entry<String, List<String>> entry: attributes.entrySet()) {
            if (entry.getValue() != null) {
                Collections.sort(entry.getValue());
                List<String> customFlagValues = event.getCustomFlags().get(entry.getKey());
                Collections.sort(customFlagValues);
                assertEquals(entry.getValue(), customFlagValues);
            } else {
                assertEquals(entry.getValue(), event.getCustomFlags().get(entry.getKey()));
            }
        }
        assertEquals(attributes, event.getCustomFlags());
    }
}