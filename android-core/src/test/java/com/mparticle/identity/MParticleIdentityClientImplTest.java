package com.mparticle.identity;

import com.mparticle.MParticle;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.*;


public class MParticleIdentityClientImplTest {

    @Test
    public void testConvertIdentityTypeToString() throws Exception {
        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            Assert.assertEquals(type,
                    MParticleIdentityClientImpl.getIdentityType(
                            MParticleIdentityClientImpl.getStringValue(type)));
        }
    }


}