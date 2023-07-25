package com.mparticle

import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import java.lang.reflect.Modifier
import kotlin.random.Random

object Utils {

    fun randomPromotionAction(): String {
        return randomConstString(Promotion::class.java)
    }

    fun randomConstString(clazz: Class<*>): String {
        return clazz.fields
            .filter { Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) }
            .filter { it.name.all { it.isUpperCase() } }
            .filter { it.type == String::class.java }
            .let {
                it[Random.Default.nextInt(0, it.size - 1)].get(null) as String
            }
    }

    val chars: List<Char> = ('a'..'z') + ('A'..'Z')

    fun randomAttributes(): MutableMap<String, String> {
        return (0..Random.Default.nextInt(0, 5)).map {
            randomString(4) to randomString(8)
        }.toMap().toMutableMap()
    }

    fun randomIdentities(): MutableMap<MParticle.IdentityType, String> {
        val identities = MParticle.IdentityType.values()
        return (0..Random.Default.nextInt(3, 8)).map {
            identities[Random.Default.nextInt(0, identities.size - 1)] to randomString(8)
        }.toMap().toMutableMap()
    }

    fun randomString(length: Int): String {
        return (0..length - 1).map {
            chars[Random.Default.nextInt(0, chars.size - 1)]
        }.joinToString("")
    }

    fun randomEventType(): MParticle.EventType {
        return MParticle.EventType.values()[
            Random.Default.nextInt(
                0,
                MParticle.EventType.values().size - 1
            )
        ]
    }

    fun randomProductAction(): String {
        return randomConstString(Product::class.java)
    }
}
