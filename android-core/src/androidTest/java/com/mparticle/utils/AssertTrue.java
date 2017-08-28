package com.mparticle.utils;

public abstract class AssertTrue<T> implements StreamAssert.Assert<T> {
    @Override
    public void assertObject(T object) {}
}
