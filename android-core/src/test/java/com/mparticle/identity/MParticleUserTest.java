package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.consent.ConsentState;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

public class MParticleUserTest {
    private MParticle mp;
    private IdentityApi id;
    private Long defaultMpId = 1L;

    @Before
    public void setup() {
        MParticle.setInstance(new MockMParticle());
        mp = MParticle.getInstance();
        id = mp.Identity();
        Mockito.when(mp.Identity().mConfigManager.getMpid()).thenReturn(defaultMpId);

    }

    /**
     * this tests that when you add a call setUserIdentity, with a user name and id that already exists,
     * we will not log it
     */
    @Test
    public void testAddExistingUserIdentity() throws Exception {
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 8, \"i\": \"alias test\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(id.mMessageManager.getUserIdentityJson(defaultMpId)).thenReturn(identities);
        Mockito.when(id.mMessageManager.logUserIdentityChangeMessage(Mockito.any(JSONObject.class), Mockito.any(JSONObject.class), Mockito.any(JSONArray.class), eq(defaultMpId))).thenThrow(new AssertionError("Should not log redundent User Identity"));
        Map<MParticle.IdentityType, String> ids = new HashMap<MParticle.IdentityType, String >();
        ids.put(MParticle.IdentityType.Alias, "alias test");
        ((MParticleUserImpl)id.getCurrentUser()).setUserIdentities(ids);
    }

    @Test
    public void testChangeUserIdentity() throws Exception {
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(id.mMessageManager.getUserIdentityJson(1)).thenReturn(identities);
        ((MParticleUserImpl)id.getCurrentUser()).setUserIdentity(MParticle.IdentityType.Email, "email value 2");
        ArgumentCaptor<JSONObject> argument1 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        Mockito.verify(id.mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        argument1.capture(),
                        argument2.capture(),
                        argument3.capture(),
                        eq(defaultMpId)
                );
        JSONObject oldIdentity = argument2.getValue();
        assertEquals(oldIdentity.get("i"), "email value 1");
        assertEquals(oldIdentity.get("n"), 7);
        assertEquals(oldIdentity.getDouble("dfs"), 1473869816521d, 100);
        assertEquals(oldIdentity.get("f"), true);
        JSONObject newIdentity = argument1.getValue();
        assertEquals(newIdentity.get("i"), "email value 2");
        assertEquals(newIdentity.get("n"), 7);
        assertEquals(newIdentity.getDouble("dfs"), 1473869816521d, 100);
        assertEquals(newIdentity.get("f"), false);
        Mockito.verify(MParticle.getInstance().Internal().getKitManager(), Mockito.times(1)).setUserIdentity(eq("email value 2"), eq(MParticle.IdentityType.Email));
        JSONArray allIdentities = argument3.getValue();
        assertEquals(1, allIdentities.length());
    }


    @Test
    public void testRemoveUserAttribute() throws Exception {
        IdentityApi mp = MParticle.getInstance().Identity();
        assertFalse(mp.getCurrentUser().removeUserAttribute(null));
        assertFalse(mp.getCurrentUser().removeUserAttribute(""));
        assertTrue(mp.getCurrentUser().removeUserAttribute("test"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).removeUserAttribute("test", 1);
        Mockito.verify(MParticle.getInstance().Internal().getKitManager(), Mockito.times(1)).removeUserAttribute("test", 1);
    }

    @Test
    public void testRemoveUserTag() throws Exception {
        assertFalse(id.getCurrentUser().removeUserAttribute(null));
        assertFalse(id.getCurrentUser().removeUserAttribute(""));
        assertTrue(id.getCurrentUser().removeUserAttribute("test"));
        Mockito.verify(id.mMessageManager, Mockito.times(1)).removeUserAttribute("test", 1);
        Mockito.verify(mp.Internal().getKitManager(), Mockito.times(1)).removeUserAttribute("test", 1);
    }


    @Test
    public void testGetAllUserAttributes1() throws Exception {
        UserAttributeListener listener = Mockito.mock(UserAttributeListener.class);
        id.getCurrentUser().getUserAttributes(listener);
        Mockito.verify(mp.Identity().mMessageManager, Mockito.times(1)).getUserAttributes(listener, defaultMpId);
    }


    @Test
    public void testRemoveUserIdentityWhenNoneExist() throws Exception {
        JSONArray identities = new JSONArray();
        Mockito.when(mp.Identity().mMessageManager.getUserIdentityJson(defaultMpId)).thenReturn(identities);
        ((MParticleUserImpl)id.getCurrentUser()).setUserIdentity(MParticle.IdentityType.Alias, null);
        Mockito.verify(mp.Identity().mMessageManager,
                Mockito.times(0)).
                logUserIdentityChangeMessage(
                        Mockito.any(JSONObject.class),
                        Mockito.any(JSONObject.class),
                        Mockito.any(JSONArray.class),
                        eq(defaultMpId)
                );
        Mockito.verify(mp.Internal().getKitManager(), Mockito.times(0)).removeUserIdentity(Mockito.any(MParticle.IdentityType.class));
    }

    @Test
    public void testRemoveUserIdentity() throws Exception {
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.Identity().mMessageManager.getUserIdentityJson(defaultMpId)).thenReturn(identities);
        ((MParticleUserImpl)id.getCurrentUser()).setUserIdentity(MParticle.IdentityType.Email, null);
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        ArgumentCaptor<Long> argument4 = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(mp.Identity().mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        Mockito.isNull(JSONObject.class),
                        argument2.capture(),
                        argument3.capture(),
                        argument4.capture()
                );
        JSONObject oldIdentity = argument2.getValue();
        assertEquals(oldIdentity.get("i"), "email value 1");
        assertEquals(oldIdentity.get("n"), 7);
        assertEquals(oldIdentity.getDouble("dfs"), 1473869816521d, 100);
        assertEquals(oldIdentity.get("f"), true);
        assertTrue(argument4.getValue().equals(defaultMpId));
        Mockito.verify(mp.Internal().getKitManager(), Mockito.times(1)).removeUserIdentity(MParticle.IdentityType.Email);
        JSONArray allIdentities = argument3.getValue();
        assertEquals(0, allIdentities.length());
    }

    @Test
    public void testAddUserIdentity() throws Exception {
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.Identity().mMessageManager.getUserIdentityJson(Mockito.anyInt())).thenReturn(identities);
        ((MParticleUserImpl)id.getCurrentUser()).setUserIdentity(MParticle.IdentityType.Alias, "alias test");
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        ArgumentCaptor<Long> argument4 = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(mp.Identity().mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        argument2.capture(),
                        Mockito.isNull(JSONObject.class),
                        argument3.capture(),
                        argument4.capture()
                );
        JSONObject oldIdentity = argument2.getValue();
        assertEquals(oldIdentity.get("i"), "alias test");
        assertEquals(oldIdentity.get("n"), MParticle.IdentityType.Alias.getValue());
        assertEquals(oldIdentity.getDouble("dfs"), System.currentTimeMillis(), 1000);
        assertEquals(oldIdentity.get("f"), true);
        assertTrue(argument4.getValue().equals(defaultMpId));
        Mockito.verify(mp.Internal().getKitManager(), Mockito.times(1)).setUserIdentity(eq("alias test"), eq(MParticle.IdentityType.Alias));

        JSONArray allIdentities = argument3.getValue();
        assertEquals(2, allIdentities.length());
    }
}
