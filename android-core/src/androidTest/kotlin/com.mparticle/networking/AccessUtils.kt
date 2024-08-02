package com.mparticle.networking

object AccessUtils {
    val defaultNetworkOptions: NetworkOptions
        get() = NetworkOptionsManager.defaultNetworkOptions()

    fun equals(networkOptions1: NetworkOptions, networkOptions2: NetworkOptions): Boolean {
        if (networkOptions1 === networkOptions2) {
            return true
        }
        if (networkOptions1.pinningDisabledInDevelopment != networkOptions2.pinningDisabledInDevelopment) {
            return false
        }
        for ((key, value) in networkOptions1.domainMappings) {
            val other = networkOptions2.domainMappings[key]
            if (other == null || !equals(value, other)) {
                return false
            }
        }
        return true
    }

    fun equals(domainMapping1: DomainMapping, domainMapping2: DomainMapping): Boolean {
        if (domainMapping1 === domainMapping2) {
            return true
        }
        if (domainMapping1.url == domainMapping2.url && domainMapping1.type == domainMapping2.type) {
            for (i in domainMapping1.certificates.indices) {
                if (!equals(domainMapping1.certificates[i], domainMapping2.certificates[i])) {
                    return false
                }
            }
        }
        return true
    }

    fun equals(certificate1: Certificate, certificate2: Certificate): Boolean {
        if (certificate1 == certificate2) {
            return true
        }
        return (
            (certificate1.certificate === certificate2.certificate || certificate1.certificate == certificate2.certificate) &&
                (certificate1.alias === certificate2.alias || certificate1.alias == certificate2.alias)
            )
    }
}
