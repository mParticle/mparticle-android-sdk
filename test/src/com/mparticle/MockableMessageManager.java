package com.mparticle;

// This class only exists to allow Mockito and Dexmaker to make a
// mock of the package-private MessageManager class by providing
// a public packaged class
public class MockableMessageManager extends MessageManager {

    public MockableMessageManager() {
    }

}
