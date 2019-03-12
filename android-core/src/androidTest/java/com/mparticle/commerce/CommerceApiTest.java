package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CommerceApiTest extends BaseCleanStartedEachTest {


    @Test
    public void testAddToCart() {
        Product product = new Product.Builder("product1", "1234", 1.0).build();
        MParticle.getInstance().Identity().getCurrentUser().getCart().add(product);
        assertEquals(1, MParticle.getInstance().Identity().getCurrentUser().getCart().products().size());
        MParticle.getInstance().Identity().getCurrentUser().getCart().remove(product);
        assertEquals(0, MParticle.getInstance().Identity().getCurrentUser().getCart().products().size());
    }

}
