package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProductBagApiTest {
    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
    }

    @Test
    public void testGetProductBags() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        bags.addProduct("some bag name", new Product.Builder("product name", "product sku", 1).build());
        assertEquals("product name", bags.getBags().get(0).getProducts().get(0).getName());
        try {
            bags.getBags().clear();
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail("Bag was able to be altered.");
    }

    @Test
    public void testAddProduct() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        String bagString = bags.toString();
        //make sure error conditions don't mutate the bags
        boolean result = bags.addProduct(null, new Product.Builder("product name", "product sku", 1).build());
        assertFalse(result);
        assertEquals(bagString, bags.toString());
        assertEquals(0, bags.getBags().size());
        result = bags.addProduct("bag name", null);
        //add an empty bag
        assertTrue(result);
        assertNotEquals(bagString, bags.toString());
        assertEquals(1, bags.getBags().size());
        //now test a simple add of a bag that doesn't exist
        result = bags.addProduct("some bag name", new Product.Builder("product name", "product sku", 1).build());
        assertTrue(result);
        assertEquals(2, bags.getBags().size());
        assertEquals("some bag name", bags.findBag("some bag name").getName());
        assertNotEquals(bagString, bags.toString());
        bagString = bags.toString();
        //adding with an existing bag shouldn't create a whole new bag
        bags.addProduct("some bag name", new Product.Builder("product name 2", "product sku 2", 1).build());
        assertEquals(2, bags.getBags().size());
        assertEquals("product name 2", bags.getBags().get(1).getProducts().get(1).getName());
        assertNotEquals(bagString, bags.toString());
    }

    @Test
    public void testRemoveProduct() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        //error scenarios
        assertFalse(
                bags.removeProduct(null, new Product.Builder("name", "sku", 1.0).build())
        );
        assertFalse(
                bags.removeProduct("whatever", new Product.Builder("name", "sku", 1.0).build())
        );
        assertFalse(
                bags.removeProduct("whatever", null)
        );

        //remove a product that doesn't exist
        Product product = new Product.Builder("product name", "product sku", 1).build();
        Product product2 = new Product.Builder("product name 2", "product sku 2", 1).build();
        bags.addProduct("some bag name", product);
        ProductBag bag = bags.findBag("some bag name");
        assertEquals("some bag name", bag.getName());
        assertEquals(1, bag.getProducts().size());
        String bagString = bags.toString();
        assertFalse(
                bags.removeProduct("some bag name 2", product)
        );
        assertFalse(
                bags.removeProduct("some bag name", product2)
        );
        assertEquals(bagString, bags.toString());
        assertTrue(
                bags.removeProduct("some bag name", product)
        );
        assertEquals(1, bags.getBags().size());
        assertEquals(0, bag.getProducts().size());
        assertNotEquals(bagString, bags.toString());
    }

    @Test
    public void testClearProductBag() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        assertFalse(bags.clearProductBag(""));
        assertFalse(bags.clearProductBag(null));
        assertFalse(bags.clearProductBag("whatever"));
        bags.addProduct("some bag name", new Product.Builder("product name", "product sku", 1).build());
        assertEquals(1, bags.getBags().size());
        assertEquals(1, bags.getBags().get(0).getProducts().size());
        assertFalse(bags.clearProductBag("whatever"));
        assertTrue(bags.clearProductBag("sOme bag name"));
        assertEquals(1, bags.getBags().size());
        assertEquals(0, bags.getBags().get(0).getProducts().size());
    }

    @Test
    public void testRemoveProductBag() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        assertFalse(bags.removeProductBag(""));
        assertFalse(bags.removeProductBag(null));
        assertFalse(bags.removeProductBag("whatever"));
        bags.addProduct("some bag name", new Product.Builder("product name", "product sku", 1).build());
        assertEquals(1, bags.getBags().size());
        assertEquals(1, bags.getBags().get(0).getProducts().size());
        assertFalse(bags.removeProductBag("whatever"));
        String bagString = bags.toString();
        assertTrue(bags.removeProductBag("sOme bag name"));
        assertEquals(0, bags.getBags().size());
        assertNotEquals(bagString, bags.toString());
    }

    @Test
    public void testToString() throws Exception {
        ProductBagApi bags = new ProductBagApi(new MockContext());
        bags.addProduct("some bag name", new Product.Builder("product name", "product sku", 1).build());
        bags.addProduct("some bag name", new Product.Builder("product name 2", "product sku 2", 1).build());
        bags.addProduct("some bag name 2", new Product.Builder("product name 3", "product sku 3", 1).build());
        bags.addProduct("some bag name 3", null);
        JSONObject json = new JSONObject(bags.toString());
        assertTrue(json.has("some bag name"));
        JSONObject bag1 = json.getJSONObject("some bag name");
        assertTrue(bag1.has("pl"));
        JSONArray products1 = bag1.getJSONArray("pl");
        assertEquals(2, products1.length());
        JSONObject bag2 = json.getJSONObject("some bag name 2");
        assertTrue(bag2.has("pl"));
        JSONArray products2 = bag2.getJSONArray("pl");
        assertEquals(1, products2.length());

        JSONObject bag3 = json.getJSONObject("some bag name 3");
        assertTrue(bag3.has("pl"));
        JSONArray products3 = bag3.getJSONArray("pl");
        assertEquals(0, products3.length());
    }
}