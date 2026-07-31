package com.mparticle.kits

import com.braze.models.outgoing.BrazeProperties
import java.math.BigDecimal

data class BrazePurchase(
    val sku: String,
    val currency: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val purchaseProperties: BrazeProperties,
)
