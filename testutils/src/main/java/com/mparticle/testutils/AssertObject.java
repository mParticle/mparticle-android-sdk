package com.mparticle.testutils;

public abstract class AssertObject<T> implements StreamAssert.Assert<T> {
    @Override
    public boolean assertTrueI(T object) {
        return true;
    }
}