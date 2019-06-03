package com.mparticle.internal.listeners;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@org.aspectj.lang.annotation.Aspect
public class ListenerHook {

    /**
     * This includes all methods in classes marked by @ApiClass (not their inner classes)
     */
    @Pointcut("@within(com.mparticle.internal.listeners.ApiClass)")
    public void apiClassAnnotated() {}

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}

    /**
     *is no-arg with non-void return type, or starts with "get".
     *
     * example methods we want this rule to catch and ultimately exclude from
     * instrumentation:
     *  String Cart.toString()
     *  KitIntegration MParticle.getKit(int id)
     *  MParticle.Internal MParticle.Internal()
     */
    @Pointcut("(execution(public * *()) && !execution(public void *()))" +
            "|| execution(public * get*(..))")
    public void getterMethod() {}

    /**
     * this pointcut is to be used as a catch-all for classes that we
     * want to be annotated, but don't exactly fit the rule.
     *
     * example methods we want this rule to catch and ultimately include from
     * instrumentation:
     *  Cart Cart.clear();
     *  MParticleTask IdentityApi.login();
     *  MParticleTask IdentityApi.identify()
     *
     * both pass the "getterMethod()" rule above, so they need to be manually included
     */
    @Pointcut("execution(public com.mparticle.commerce.Cart clear())" +
            "|| execution(public com.mparticle.MParticleTask *())")
    public void manuallyIncluded() {}

    @Pointcut("execution(public void checkout())")
    public void manuallyExcluded() {}

    @Before("(apiClassAnnotated() && publicMethod() && !getterMethod() && !manuallyExcluded())" +
            "|| manuallyIncluded()")
    public void onApiCall(JoinPoint joinPoint) {
        InternalListenerManager.getListener().onApiCalled(joinPoint.getArgs());
    }


}
