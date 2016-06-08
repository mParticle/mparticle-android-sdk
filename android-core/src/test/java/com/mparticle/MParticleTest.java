package com.mparticle;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        assertFalse(mp.setUserAttribute(null, "test"));
        assertFalse(mp.setUserAttribute(null, null));
        assertFalse(mp.setUserAttribute(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        String legalString = new String(new char[256]);
        assertTrue(mp.setUserAttribute(legalString, null));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(legalString, null);

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture());
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
        }
        assertTrue(mp.setUserAttribute("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(3)).setUserAttribute(stringCaptor.capture(), listCaptor.capture());
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
        assertFalse(mp.setUserAttributeList(null, null));
        assertFalse(mp.setUserAttributeList(new String(new char[257]), null));

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        String legalString = new String(new char[256]);
        assertFalse(mp.setUserAttributeList(legalString, null));

        List<Integer> integerList = new LinkedList<Integer>();
        integerList.add(203948);
        assertTrue(mp.setUserAttribute("test2", integerList));

        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(stringCaptor.capture(), listCaptor.capture());
        assertTrue(stringCaptor.getValue().equals("test2"));
        List capturedStringList = listCaptor.getValue();
        assertTrue(capturedStringList.size() == 1);
        assertTrue(capturedStringList.get(0).equals("203948"));
        List<String> longStringList = new ArrayList<String>();
        for (int i = 0; i < 1000; i++){
            longStringList.add("whatever");
        }
        assertTrue(mp.setUserAttributeList("test3", longStringList));

        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(stringCaptor.capture(), listCaptor.capture());
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
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).incrementUserAttribute("test", 3);
    }

    @Test
    public void testRemoveUserAttribute() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        assertFalse(mp.removeUserAttribute(null));
        assertFalse(mp.removeUserAttribute(""));
        assertTrue(mp.removeUserAttribute("test"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).removeUserAttribute("test");
        Mockito.verify(mp.mKitManager, Mockito.times(1)).removeUserAttribute("test");
    }

    @Test
    public void testSetUserTag() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        assertFalse(mp.setUserTag(null));
        assertFalse(mp.setUserTag(""));
        assertTrue(mp.setUserTag("blah"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute("blah", null);
    }

    @Test
    public void testRemoveUserTag() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MParticle.start(new com.mparticle.mock.MockContext());
        MParticle mp = MParticle.getInstance();
        assertFalse(mp.removeUserTag(null));
        assertFalse(mp.removeUserTag(""));
        assertTrue(mp.removeUserTag("test"));
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).removeUserAttribute("test");
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