package com.mparticle.internal;


import com.mparticle.MParticle;
import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.ProductBag;
import com.mparticle.commerce.ProductBagApi;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.mock.MockContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.Double.NaN;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;


public class MParticleJSInterfaceTest extends MParticleJSInterface {

    private String mProduct1Json = "{\"Name\":\"iPhone\",\"Sku\":\"12345\",\"Price\":400,\"Quantity\":1,\"TotalAmount\":400}";
    private String mProduct2Json = "{\"Name\":\"Android\",\"Sku\":\"98765\",\"Price\":\"600\",\"Quantity\":4,\"Brand\":\"Samsung\",\"Variant\":\"SuperDuper\",\"Category\":\"CellPhones\",\"Position\":2,\"CouponCode\":\"my-coupon-code-2\",\"TotalAmount\":2400,\"Attributes\":{\"customkey\":\"customvalue\"}}";
    private String mProduct3Json = "{\"Name\":\"iPhone\",\"Sku\":\"123456\",\"Price\":\"400\",\"Quantity\":2,\"Brand\":\"Apple\",\"Variant\":\"Plus\",\"Category\":\"Phones\",\"Position\":1,\"CouponCode\":\"my-coupon-code\",\"TotalAmount\":800,\"Attributes\":{\"customkey\":\"customvalue\"}}";
    private String mProduct4Json = "{\"Name\":\"iPhone\",\"Sku\":\"1234567\",\"Price\":null,\"Quantity\":\"2-foo\",\"Brand\":\"Apple\",\"Variant\":\"Plus\",\"Category\":\"Phones\",\"Position\":\"1-foo\",\"CouponCode\":\"my-coupon-code\",\"TotalAmount\":null,\"Attributes\":{\"customkey\":\"customvalue\"}}";
    private String mProduct5Json = "{\"Name\":\"iPhone\",\"Sku\":\"SKU1\",\"Price\":1,\"Quantity\":1,\"TotalAmount\":1},{\"Name\":\"Android\",\"Sku\":\"SKU2\",\"Price\":1,\"Quantity\":1,\"TotalAmount\":1}";
    private String mPromotion1String = "{\"Id\":\"12345\",\"Creative\":\"my-creative\",\"Name\":\"creative-name\",\"Position\":1}";

    private Product mProduct1, mProduct2;
    private ProductBagApi productBagApi;
    private Cart cart;

    private MParticleJSInterface jsInterfaceInstance;

    private String sampleCommerceEvent1 = "{\"EventName\":\"eCommerce - ViewDetail\",\"EventCategory\":15,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"a37943f1-c9b9-452f-8cc4-0d2eca2ef002\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029994680,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":6,\"ProductList\":[" + mProduct1Json +"]}}";
    private String sampleCommerceEvent2 = "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"64563b6b-0ece-41e1-a68c-675025be2a57\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492026476362,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"8b658328-f6bf-4bf6-a9bc-6dcc7b9a9e44\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[" + mProduct2Json + "],\"TransactionId\":\"5050505\",\"Affiliation\":\"test-affiliation2\",\"CouponCode\":\"coupon-cod2e\",\"TotalAmount\":43234,\"ShippingAmount\":100,\"TaxAmount\":400}}";
    private String sampleCommerceEvent3 = "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"67480717-6e06-4d7f-9326-75ee7bf7e6a8\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029690858,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[" + mProduct3Json + "],\"TransactionId\":\"12345\",\"Affiliation\":\"test-affiliation\",\"CouponCode\":\"coupon-code\",\"TotalAmount\":44334,\"ShippingAmount\":600,\"TaxAmount\":200}}";
    private String sampleCommerceEvent4 = "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"80db6a3b-4e4c-4ce1-8930-ca7eebdcbb06\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029758226,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[" + mProduct4Json + "],\"TransactionId\":\"12345\",\"Affiliation\":\"test-affiliation\",\"CouponCode\":\"coupon-code\",\"TotalAmount\":\"44334-foo\",\"ShippingAmount\":\"600-foo\",\"TaxAmount\":\"200-foo\"}}";
    private String sampleCommerceEvent5 = "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"cba9e5fe-31f3-431a-ba15-cdd887a298b7\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029826952,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[" + mProduct5Json + "],\"TransactionId\":\"12345\"}}";
    private String sampleCommerceEvent6 = "{\"EventName\":\"eCommerce - Refund\",\"EventCategory\":17,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"7c24e0b1-6686-4e08-9d8a-94235cb7e34d\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029858097,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":8,\"ProductList\":[" + mProduct5Json + "],\"TransactionId\":\"12345\"}}";
    private String sampleCommerceEvent7 = "{\"EventName\":\"eCommerce - PromotionClick\",\"EventCategory\":19,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"bb81adfd-cd23-492d-a333-c453fbf1b255\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029882614,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"PromotionAction\":{\"PromotionActionType\":2,\"PromotionList\":[" + mPromotion1String + "]}}";
    private String sampleCommerceEvent8 = "{\"EventName\":\"eCommerce - Impression\",\"EventCategory\":22,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"7cb945f2-8281-4483-ab41-cd10e9faac53\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029916774,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductImpressions\":[{\"ProductImpressionList\":\"impression-name\",\"ProductList\":[" + mProduct1Json + "]}]}";
    private String sampleCommerceEvent9 = "{\"EventName\":\"eCommerce - Refund\",\"EventCategory\":17,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"257aaa99-e1c6-4b50-bb6d-dcdcdcfc0abc\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029941497,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":8,\"ProductList\":[],\"TransactionId\":\"12345\"}}";
    private String sampleCommerceEvent10 = "{\"EventName\":\"eCommerce - Checkout\",\"EventCategory\":12,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"a2401cd1-1f2b-446a-a37c-5ed3ffc2b2d1\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029969607,\"OptOut\":null,\"ProductBags\":{},\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":3,\"CheckoutStep\":1,\"CheckoutOptions\":\"Visa\",\"ProductList\":[]}}";

    @Before
    public void setup() throws Exception {
        MParticle.setInstance(Mockito.mock(MParticle.class));
        productBagApi = new ProductBagApi(new MockContext());
        cart = Cart.getInstance(new MockContext());
        jsInterfaceInstance = new MParticleJSInterface();

        Mockito.when(MParticle.getInstance().getEnvironment()).thenReturn(MParticle.Environment.Development);
        Mockito.when(MParticle.getInstance().ProductBags()).thenReturn(productBagApi);
        Mockito.when(MParticle.getInstance().Commerce()).thenReturn(Mockito.mock(CommerceApi.class));
        Mockito.when(MParticle.getInstance().Commerce().cart()).thenReturn(cart);
        mProduct1 = new Product.Builder("iPhone", "12345", 400)
                .quantity(1)
                .build();
        Map<String, String> customAttributes = new TreeMap<>();
        customAttributes.put("customkey", "customvalue");
        mProduct2 = new Product.Builder("Android", "98765", 600)
                .quantity(4)
                .couponCode("my-coupon-code-2")
                .variant("SuperDuper")
                .brand("Samsung")
                .position(2)
                .category("CellPhones")
                .customAttributes(customAttributes)
                .build();
    }

    @Test
    public void testAddToProductBags() throws Exception {
        String productBag1Name = "productBag1";
        String productBag2Name = "productBag2";

        jsInterfaceInstance.addToProductBag(productBag1Name, mProduct1Json);
        jsInterfaceInstance.addToProductBag(productBag1Name, mProduct2Json);
        ProductBag bag = productBagApi.findBag(productBag1Name);
        assertNotNull(bag);
        assertEquals(bag.getName(), productBag1Name);
        assertEquals(bag.getProducts().size(), 2);
        assertEquals(bag.getProducts().get(0),mProduct1);
        assertEquals(bag.getProducts().get(1),mProduct2);
        jsInterfaceInstance.addToProductBag(productBag2Name, mProduct2Json);
        assertEquals(bag.getProducts().size(), 2);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 1);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().get(0), mProduct2);
    }

    @Test
    public void testRemoveFromProductBags() throws Exception {
        String productBagName = "productBag1";
        String productBag2Name = "productBag2";

        jsInterfaceInstance.addToProductBag(productBagName, mProduct1Json);
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 1);
        assertFalse(jsInterfaceInstance.removeFromProductBag(productBagName, mProduct2Json));
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 1);
        jsInterfaceInstance.addToProductBag(productBagName, mProduct2Json);
        jsInterfaceInstance.addToProductBag(productBag2Name, mProduct3Json);
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 2);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 1);
        assertTrue(jsInterfaceInstance.removeFromProductBag(productBagName, mProduct2Json));
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(),1);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 1);
        assertTrue(jsInterfaceInstance.removeFromProductBag(productBagName, mProduct1Json));
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 0);
        assertTrue(jsInterfaceInstance.removeFromProductBag(productBag2Name, mProduct3Json));
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 0);
    }

    @Test
    public void clearProductBag() throws Exception {
        String productBagName = "productBag1";
        String productBag2Name = "productBag2";

        jsInterfaceInstance.addToProductBag(productBagName, mProduct1Json);
        jsInterfaceInstance.addToProductBag(productBagName, mProduct2Json);
        jsInterfaceInstance.addToProductBag(productBag2Name, mProduct3Json);
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 2);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 1);
        jsInterfaceInstance.clearProductBag(productBagName);
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 0);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 1);
        jsInterfaceInstance.clearProductBag(productBag2Name);
        assertEquals(productBagApi.findBag(productBagName).getProducts().size(), 0);
        assertEquals(productBagApi.findBag(productBag2Name).getProducts().size(), 0);
    }

    @Test
    public void testAddToCart() throws Exception {
        jsInterfaceInstance.addToCart(mProduct1Json);
        assertNotNull(cart.getProduct(mProduct1.getName()));
        assertEquals(cart.getProduct(mProduct1.getName()), mProduct1);
        jsInterfaceInstance.addToCart(mProduct2Json);
        assertNotNull(cart.getProduct(mProduct2.getName()));
        assertNotNull(cart.getProduct(mProduct1.getName()));
        assertEquals(cart.getProduct(mProduct1.getName()), mProduct1);
        assertEquals(cart.getProduct(mProduct2.getName()), mProduct2);
    }

    @Test
    public void testRemoveFromCart() throws Exception {
        jsInterfaceInstance.addToCart(mProduct1Json);
        jsInterfaceInstance.addToCart(mProduct2Json);
        jsInterfaceInstance.removeFromCart(mProduct3Json);
        assertNotNull(cart.getProduct(mProduct2.getName()));
        assertNotNull(cart.getProduct(mProduct1.getName()));
        jsInterfaceInstance.removeFromCart(mProduct1Json);
        assertNull(cart.getProduct(mProduct1.getName()));
        assertNotNull(cart.getProduct(mProduct2.getName()));
        jsInterfaceInstance.removeFromCart(mProduct2Json);
        assertNull(cart.getProduct(mProduct2.getName()));
        assertNull(cart.getProduct(mProduct1.getName()));
    }

    @Test
    public void testClearCart() throws Exception {
        jsInterfaceInstance.addToCart(mProduct1Json);
        jsInterfaceInstance.addToCart(mProduct2Json);
        assertNotNull(cart.getProduct(mProduct1.getName()));
        assertNotNull(cart.getProduct(mProduct2.getName()));
        jsInterfaceInstance.clearCart();
        assertNull(cart.getProduct(mProduct1.getName()));
        assertNull(cart.getProduct(mProduct2.getName()));
    }

    @Test
    public void unpackSimpleCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent1);
        CommerceEvent commerceEvent = toCommerceEvent(object);
        assertEquals(commerceEvent.getProductAction(), "6");
        assertEquals(commerceEvent.getProducts().size(), 1);
        Product product = commerceEvent.getProducts().get(0);
        assertEquals(commerceEvent.getEventName(), ("eCommerce - ViewDetail"));
        assertEquals(product.getName(), mProduct1.getName());
        assertEquals(product.getUnitPrice(), mProduct1.getUnitPrice(), .01);
        assertEquals(product.getQuantity(), mProduct1.getQuantity(), .01);
        assertEquals(product.getTotalAmount(), mProduct1.getTotalAmount(), .01);
        assertEquals(product.getSku(), mProduct1.getSku());
    }

    @Test
    public void unpackCommerceEventWithTransactions() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent2);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getEventName(), ("eCommerce - Purchase"));
        assertEquals(commerceEvent.getProducts().size(), 1);
        Product product = commerceEvent.getProducts().get(0);

        assertEquals(product.getName(), mProduct2.getName());
        assertEquals(product.getSku(), mProduct2.getSku());
        assertEquals(product.getCustomAttributes().size(), 1);
        String key = (String) product.getCustomAttributes().keySet().toArray()[0];
        assertEquals(key, "customkey");
        assertEquals(product.getCustomAttributes().get(key), "customvalue");
        assertEquals(product.getCouponCode(), mProduct2.getCouponCode());
        assertEquals(product.getQuantity(), mProduct2.getQuantity(), .01);
        assertEquals(product.getPosition().intValue(), 2);
        assertEquals(product.getVariant(), mProduct2.getVariant());
        TransactionAttributes transactionAttributes = commerceEvent.getTransactionAttributes();
        assertEquals(transactionAttributes.getAffiliation(), "test-affiliation2");
        assertEquals(transactionAttributes.getRevenue(), 43234.0, .01);
        assertEquals(transactionAttributes.getShipping(), 100, .01);
        assertEquals(transactionAttributes.getCouponCode(), "coupon-cod2e");
        assertEquals(transactionAttributes.getId(), "5050505");
    }

    /**
     * tests how we respond to String values where we should have Numbers
     * @throws Exception
     */
    @Test
    public void unpackMalformedCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent4);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getTransactionAttributes().getShipping(), NaN);
        assertEquals(commerceEvent.getTransactionAttributes().getTax(), NaN);
        assertEquals(commerceEvent.getTransactionAttributes().getRevenue(), NaN);
        assertEquals(commerceEvent.getProducts().size(), 1);
        Product product2 = commerceEvent.getProducts().get(0);
        assertEquals(product2.getUnitPrice(), 0.0, .001);
        assertEquals(product2.getPosition().intValue(), 0);
    }

    @Test
    public void unpackMultipleProductsCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent5);
        CommerceEvent commerceEvent = toCommerceEvent(object);


        assertEquals(commerceEvent.getProducts().size(), 2);
        Product product3 = commerceEvent.getProducts().get(0);
        Product product4 = commerceEvent.getProducts().get(1);
        assertEquals(product3.getName(), "iPhone");
        assertEquals(product4.getName(), "Android");
        assertEquals(commerceEvent.getEventName(), "eCommerce - Purchase");
        assertEquals(commerceEvent.getProductAction(), "7");
        assertEquals(commerceEvent.getTransactionAttributes().getId(), "12345");
    }

    @Test
    public void unpackRefundMultipleProductsCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent6);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getEventName(), "eCommerce - Refund");
        assertEquals(commerceEvent.getProductAction(), "8");
        assertEquals(commerceEvent.getProducts().size(), 2);
    }

    @Test
    public void unpackPromotionCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent7);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getPromotionAction(), "2");
        assertEquals(commerceEvent.getPromotions().size(), 1);
        Promotion promotion = commerceEvent.getPromotions().get(0);
        assertEquals(promotion.getCreative(), "my-creative");
        assertEquals(promotion.getId(), "12345");
        assertEquals(promotion.getName(), "creative-name");
        assertEquals(promotion.getPosition(), "1");
    }

    @Test
    public void unpackImpressionCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent8);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getEventName(), "eCommerce - Impression");
        assertEquals(commerceEvent.getImpressions().size(), 1);
        Impression impression = commerceEvent.getImpressions().get(0);
        assertEquals(impression.getListName(), "impression-name");
        assertEquals(impression.getProducts().size(), 1);
        Product product5 = impression.getProducts().get(0);
        assertEquals(product5.getName(), "iPhone");
        assertEquals(product5.getSku(), "12345");
        assertEquals(product5.getUnitPrice(), 400, .01);
        assertEquals(product5.getQuantity(), 1, .01);
    }

    /**
     * tests how we respond to an empty product array, with a product action
     * @throws Exception
     */
    @Test
    public void unpackRefundEmptyProductArrayCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent9);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getEventName(), "eCommerce - Refund");
        assertEquals(commerceEvent.getProductAction(), "8");
        assertEquals(commerceEvent.getTransactionAttributes().getId(), "12345");
        assertEquals(commerceEvent.getProducts().size(), 0);
    }

    @Test
    public void unpackCheckoutCommerceEvent() throws Exception {
        JSONObject object = new JSONObject(sampleCommerceEvent10);
        CommerceEvent commerceEvent = toCommerceEvent(object);

        assertEquals(commerceEvent.getEventName(), "eCommerce - Checkout");
        assertEquals(commerceEvent.getProductAction(), "3");
        assertEquals(commerceEvent.getCheckoutStep().intValue(), 1);
        assertEquals(commerceEvent.getCheckoutOptions(), "Visa");
    }

    @Test
    public void testProductEqualityComparator() throws Exception {
        JSONObject product2Json = new JSONObject(mProduct2Json);
        Product product2 = toProduct(product2Json);
        assertTrue(isEqual(product2, mProduct2));
        product2Json.put("Sku", "00000");
        product2 = toProduct(product2Json);
        assertFalse(isEqual(product2, mProduct2));
    }

    public boolean isEqual(Product product1, Product product2) {
        try {
            JSONObject object1 = new JSONObject(product1.toString());
            JSONObject object2 = new JSONObject(product2.toString());
            object1.remove("act");
            object2.remove("act");
            return object1.toString().equals(object2.toString());
        }
        catch (JSONException ignore) {
            return false;
        }
    }
}
