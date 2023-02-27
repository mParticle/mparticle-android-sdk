package com.mparticle.modernization

class Test {

    fun test(){
        MParticle.getInstance()?.EventLogging()?.leaveBreadcrumb("as")
    }
}