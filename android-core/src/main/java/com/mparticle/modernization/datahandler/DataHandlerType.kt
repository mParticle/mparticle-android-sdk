package com.mparticle.modernization.datahandler

enum class DataHandlerType {
    MP_EVENT, COMMERCE_EVENT, BREADCRUMB;

    companion object {
        fun getType(clazz: Class<*>): DataHandlerType? {
            return when (clazz.javaClass.simpleName) {
                "com.mparticle.MpEvent" -> MP_EVENT
                "com.mparticle.commerce.CommerceEvent" -> COMMERCE_EVENT
                else -> null
            }
        }
    }

}