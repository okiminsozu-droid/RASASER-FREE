package com.rasaser.free.navigasyon

/**
 * The last stage must not reserve space for a ZIP already verified and stored.
 * Keep the same 260 MB on-device SQLite staging requirement as the installer.
 */
internal object TurkeyInstallSpace {
    fun requiredBytes(assetSize:Long,partialBytes:Long,cachedVerified:Boolean,
                      installReserve:Long,lastSearch:Boolean):Long {
        val missing=if(cachedVerified)0L
            else (assetSize-partialBytes.coerceIn(0L,assetSize)).coerceAtLeast(0L)
        val reserve=if(lastSearch)260_000_000L else installReserve
        val overhead=if(lastSearch)0L else 80_000_000L
        return missing+reserve+overhead
    }
}
