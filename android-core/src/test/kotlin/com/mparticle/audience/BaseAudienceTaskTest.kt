package com.mparticle.audience

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.lang.reflect.Field

class BaseAudienceTaskTest {
    private lateinit var classInstance: BaseAudienceTask
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun before() {
        Dispatchers.setMain(mainThreadSurrogate)
        classInstance = BaseAudienceTask()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher after the test
        mainThreadSurrogate.close()
    }

    @Test
    fun setFailedTest() = runBlocking {
        val listenerMock = mock(AudienceTaskFailureListener::class.java)
        classInstance.addFailureListener(listenerMock)
        classInstance.setFailed(AudienceResponse(401, "Unauthorized user"))
        val failureListenersField: Field =
            BaseAudienceTask::class.java.getDeclaredField("failureListeners")
        failureListenersField.isAccessible = true
        Assert.assertEquals(classInstance.isComplete(), true)
        Assert.assertEquals(classInstance.isSuccessful(), false)
        val failureListener = failureListenersField.get(classInstance) as MutableSet<*>
        Assert.assertEquals(1, failureListener.size)
    }

    @Test
    fun setFailedTest_when_Listener_not_register() = runBlocking {
        classInstance.setFailed(AudienceResponse(401, "Unauthorized user"))
        val failureListenersField: Field =
            BaseAudienceTask::class.java.getDeclaredField("failureListeners")
        failureListenersField.isAccessible = true
        Assert.assertEquals(classInstance.isComplete(), true)
        Assert.assertEquals(classInstance.isSuccessful(), false)
        val failureListener = failureListenersField.get(classInstance) as MutableSet<*>
        Assert.assertEquals(0, failureListener.size)
    }

    @Test
    fun setSuccessfulTest() = runBlocking {
        val listenerMock = mock(AudienceTaskSuccessListener::class.java)
        classInstance.addSuccessListener(listenerMock)
        val sampleJson =
            "{'dt':'ar','id':'54335128-0b2c-4089-a36d-8b456890dfe8','ct':1713390288601}"
        classInstance.setSuccessful(AudienceResponse(200, sampleJson))
        val isCompletedField: Field = BaseAudienceTask::class.java.getDeclaredField("isCompleted")
        isCompletedField.isAccessible = true
        val isSuccessfulField: Field = BaseAudienceTask::class.java.getDeclaredField("isSuccessful")
        isSuccessfulField.isAccessible = true
        val successListenersField: Field =
            BaseAudienceTask::class.java.getDeclaredField("successListeners")
        successListenersField.isAccessible = true
        Assert.assertEquals(classInstance.isComplete(), true)
        Assert.assertEquals(classInstance.isSuccessful(), true)
        val successListener = successListenersField.get(classInstance) as MutableSet<*>
        Assert.assertEquals(1, successListener.size)
    }

    @Test
    fun setSuccessfulTest_when_Listener_not_register() = runBlocking {
        val sampleJson =
            "{'dt':'ar','id':'54335128-0b2c-4089-a36d-8b456890dfe8','ct':1713390288601}"
        classInstance.setSuccessful(AudienceResponse(200, sampleJson))
        val isCompletedField: Field = BaseAudienceTask::class.java.getDeclaredField("isCompleted")
        isCompletedField.isAccessible = true
        val isSuccessfulField: Field = BaseAudienceTask::class.java.getDeclaredField("isSuccessful")
        isSuccessfulField.isAccessible = true
        val successListenersField: Field =
            BaseAudienceTask::class.java.getDeclaredField("successListeners")
        successListenersField.isAccessible = true
        Assert.assertEquals(classInstance.isComplete(), true)
        Assert.assertEquals(classInstance.isSuccessful(), true)
        val successListener = successListenersField.get(classInstance) as MutableSet<*>
        Assert.assertEquals(0, successListener.size)
    }
}
