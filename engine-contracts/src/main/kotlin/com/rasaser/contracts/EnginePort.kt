package com.rasaser.contracts

/** Yalnız public arayüzler. PRIVATE motor uygulaması veya gizli anahtar içermez. */
data class EngineHealth(val ready: Boolean, val detail: String)

interface EnginePort {
    val engineName: String
    val engineVersion: String
    fun health(): EngineHealth
}

/** Test alternatifi: gerçek motor değildir. */
class PublicFallbackEngine : EnginePort {
    override val engineName = "public-fallback"
    override val engineVersion = "0.1.0"
    override fun health() = EngineHealth(false, "Özel RaSaSeR motoru bu PUBLIC test yapısına dahil değildir.")
}
