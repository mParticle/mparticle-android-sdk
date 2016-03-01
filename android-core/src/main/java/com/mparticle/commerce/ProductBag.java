package com.mparticle.commerce;

import com.mparticle.MParticle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Immutable representation of a Product Bag. Product Bags consist of a name and a list of {@link Product} objects.
 *
 * This class cannot and should not be directly instantiated.
 *
 * @see ProductBagApi
 * @see MParticle#ProductBags()
 *
 */
public class ProductBag {

    private ProductBag() {
        super();
    }

    ProductBag(String bagName) {
        this.bagName = bagName;
    }

    String bagName;
    List<Product> products = new LinkedList<Product>();

    /**
     * Get the name of the bag
     *
     * @return returns the name of a bag
     */
    public String getName() {
        return bagName;
    }

    /**
     * Get the products contained in this bag.
     *
     * @return returns an <i>unmodifiable list</i> of products. The list will throw an {@code UnsupportedOperationException} if an attempt is made to
     * modify it.
     */
    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }
}
