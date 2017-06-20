package com.mparticle;

import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Session;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MParticleTest {

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testAndroidIdDisabled() throws Exception {
        assertFalse(MParticle.isAndroidIdDisabled());
        MParticle.setAndroidIdDisabled(true);
        assertTrue(MParticle.isAndroidIdDisabled());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                assertTrue(MParticle.isAndroidIdDisabled());
                MParticle.setAndroidIdDisabled(false);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(MParticle.isAndroidIdDisabled());
                    }
                });
            }
        });
    }

    @Test
    public void testSetUserAttribute() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.mConfigManager = Mockito.mock(ConfigManager.class);
        mp.mAppStateManager = Mockito.mock(AppStateManager.class);
        Session mockSession = Mockito.mock(Session.class);
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
        Mockito.when(mp.mConfigManager.isEnabled()).thenReturn(true);
        assertFalse(mp.setUserAttribute(null, "test"));
        assertFalse(mp.setUserAttribute(null, null));
        assertFalse(mp.setUserAttribute(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        String legalString = new String(new char[256]);
        assertTrue(mp.setUserAttribute(legalString, null));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(legalString, null, 1);

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), 1);
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
        }
        assertTrue(mp.setUserAttribute("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(3)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), 1);
        assertTrue(stringCaptor.getValue().equals("test3"));
        capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.equals(longStringList));
        longStringList.add("too much!");
        assertFalse(mp.setUserAttribute("test", longStringList));

        List<String> stringList = new LinkedList<String>();
        stringList.add(new String(new char[512]));
        assertTrue(mp.setUserAttribute("test", stringList));
        stringList.add(new String(new char[513]));
        assertFalse(mp.setUserAttribute("test", stringList));


        assertTrue(mp.setUserAttribute("test", null));
        assertTrue(mp.setUserAttribute("test", new String(new char[4096])));
        assertFalse(mp.setUserAttribute("test", new String(new char[4097])));
        assertTrue(mp.setUserAttribute("test", 1212));

    }

    @Test
    public void testSetUserAttributeList() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.mConfigManager = Mockito.mock(ConfigManager.class);
        mp.mAppStateManager = Mockito.mock(AppStateManager.class);
        Session mockSession = Mockito.mock(Session.class);
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
        Mockito.when(mp.mConfigManager.isEnabled()).thenReturn(true);
        assertFalse(mp.setUserAttributeList(null, null));
        assertFalse(mp.setUserAttributeList(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        String legalString = new String(new char[256]);
        assertFalse(mp.setUserAttributeList(legalString, null));

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), 1);
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
        }
        assertTrue(mp.setUserAttributeList("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture(), 1);
        assertTrue(stringCaptor.getValue().equals("test3"));
        capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.equals(longStringList));
        longStringList.add("too much!");
        assertFalse(mp.setUserAttributeList("test", longStringList));

        List<String> stringList = new LinkedList<String>();
        stringList.add(new String(new char[512]));
        assertTrue(mp.setUserAttributeList("test", stringList));
        stringList.add(new String(new char[513]));
        assertFalse(mp.setUserAttributeList("test", stringList));
        assertFalse(mp.setUserAttributeList("test", null));
    }

    @Test
    public void testIncrementUserAttribute() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        assertFalse(mp.incrementUserAttribute(null, 3));

        assertTrue(mp.incrementUserAttribute("test", 3));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).incrementUserAttribute("test", 3, 1);
    }

    @Test
    public void testRemoveUserIdentityWhenNoneExist() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        JSONArray identities = new JSONArray();
        Mockito.when(mp.mMessageManager.getUserIdentityJson()).thenReturn(identities);
        mp.setUserIdentity(null, MParticle.IdentityType.Alias);
        Mockito.verify(mp.mMessageManager,
                Mockito.times(0)).
                logUserIdentityChangeMessage(
                        Mockito.any(JSONObject.class),
                        Mockito.any(JSONObject.class),
                        Mockito.any(JSONArray.class)
                );
        Mockito.verify(mp.mKitManager, Mockito.times(0)).removeUserIdentity(Mockito.any(MParticle.IdentityType.class));

    }

    @Test
    public void testRemoveUserIdentity() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.mMessageManager.getUserIdentityJson()).thenReturn(identities);
        mp.setUserIdentity(null, MParticle.IdentityType.Email);
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        Mockito.verify(mp.mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        Mockito.isNull(JSONObject.class),
                        argument2.capture(),
                        argument3.capture()
                );
        JSONObject oldIdentity = argument2.getValue();
        assertEquals(oldIdentity.get("i"), "email value 1");
        assertEquals(oldIdentity.get("n"), 7);
        assertEquals(oldIdentity.getDouble("dfs"), 1473869816521d, 100);
        assertEquals(oldIdentity.get("f"), true);
        Mockito.verify(mp.mKitManager, Mockito.times(1)).removeUserIdentity(MParticle.IdentityType.Email);
        JSONArray allIdentities = argument3.getValue();
        assertEquals(0, allIdentities.length());
    }

    @Test
    public void testAddUserIdentity() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.mMessageManager.getUserIdentityJson()).thenReturn(identities);
        mp.setUserIdentity("alias test", MParticle.IdentityType.Alias);
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        Mockito.verify(mp.mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        argument2.capture(),
                        Mockito.isNull(JSONObject.class),
                        argument3.capture()
                );
        JSONObject oldIdentity = argument2.getValue();
        assertEquals(oldIdentity.get("i"), "alias test");
        assertEquals(oldIdentity.get("n"), MParticle.IdentityType.Alias.getValue());
        assertEquals(oldIdentity.getDouble("dfs"), System.currentTimeMillis(), 1000);
        assertEquals(oldIdentity.get("f"), true);
        Mockito.verify(mp.mKitManager, Mockito.times(1)).setUserIdentity(Mockito.eq("alias test"), Mockito.eq(MParticle.IdentityType.Alias));

        JSONArray allIdentities = argument3.getValue();
        assertEquals(2, allIdentities.length());
    }

    /**
     * this tests that when you add a call setUserIdentity, with a user name and id that already exists,
     * we will not log it
     */
    @Test
    public void testAddExistingUserIdentity() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 8, \"i\": \"alias test\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.mMessageManager.getUserIdentityJson()).thenReturn(identities);
        Mockito.when(mp.mMessageManager.logUserIdentityChangeMessage(Mockito.any(JSONObject.class), Mockito.any(JSONObject.class), Mockito.any(JSONArray.class))).thenThrow(new AssertionError("Should not log redundent User Identity"));
        mp.setUserIdentity("alias test", MParticle.IdentityType.Alias);
    }

    @Test
    public void testChangeUserIdentity() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        Mockito.when(mp.mMessageManager.getUserIdentityJson()).thenReturn(identities);
        mp.setUserIdentity("email value 2", MParticle.IdentityType.Email);
        ArgumentCaptor<JSONObject> argument1 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONObject> argument2 = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<JSONArray> argument3 = ArgumentCaptor.forClass(JSONArray.class);
        Mockito.verify(mp.mMessageManager,
                Mockito.times(1)).
                logUserIdentityChangeMessage(
                        argument1.capture(),
                        argument2.capture(),
                        argument3.capture()
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
        Mockito.verify(mp.mKitManager, Mockito.times(1)).setUserIdentity(Mockito.eq("email value 2"), Mockito.eq(MParticle.IdentityType.Email));
        JSONArray allIdentities = argument3.getValue();
        assertEquals(1, allIdentities.length());
    }


    @Test
    public void testRemoveUserAttribute() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        assertFalse(mp.removeUserAttribute(null));
        assertFalse(mp.removeUserAttribute(""));
        assertTrue(mp.removeUserAttribute("test"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).removeUserAttribute("test", 1);
        Mockito.verify(mp.mKitManager, Mockito.times(1)).removeUserAttribute("test");
    }

    @Test
    public void testSetUserTag() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.mConfigManager = Mockito.mock(ConfigManager.class);
        mp.mAppStateManager = Mockito.mock(AppStateManager.class);
        Session mockSession = Mockito.mock(Session.class);
        Mockito.when(mockSession.checkEventLimit()).thenReturn(true);
        Mockito.when(mp.mAppStateManager.getSession()).thenReturn(mockSession);
        Mockito.when(mp.mConfigManager.isEnabled()).thenReturn(true);
        assertFalse(mp.setUserTag(null));
        assertFalse(mp.setUserTag(""));
        assertTrue(mp.setUserTag("blah"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute("blah", null, 1);
    }

    @Test
    public void testRemoveUserTag() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();

        assertFalse(mp.removeUserTag(null));
        assertFalse(mp.removeUserTag(""));
        assertTrue(mp.removeUserTag("test"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).removeUserAttribute("test", 1);
        Mockito.verify(mp.mKitManager, Mockito.times(1)).removeUserAttribute("test");
    }

    @Test
    public void testGetUserAttributes() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.getUserAttributes();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null);
    }

    @Test
    public void testGetUserAttributeLists() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.getUserAttributeLists();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributeLists();
    }

    @Test
    public void testGetAllUserAttributes() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        mp.getAllUserAttributes();
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getAllUserAttributes(null);
    }

    @Test
    public void testGetAllUserAttributes1() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        UserAttributeListener listener = Mockito.mock(UserAttributeListener.class);
        mp.getAllUserAttributes(listener);
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getAllUserAttributes(listener);
    }
}