package com.rasaser.free.navigasyon

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkeyRangeCheckTest {
    @Test fun validResumeOnlyAtExactOffsetAndSize() {
        assertTrue(TurkeyRangeCheck.valid("bytes 1234-9999/10000",1234L,10000L))
        assertFalse(TurkeyRangeCheck.valid("bytes 1233-9999/10000",1234L,10000L))
        assertFalse(TurkeyRangeCheck.valid("bytes 1234-9999/10001",1234L,10000L))
        assertFalse(TurkeyRangeCheck.valid("bytes 1234-10000/10000",1234L,10000L))
    }
    @Test fun rejectMalformedAndMissingByteRanges() {
        assertFalse(TurkeyRangeCheck.valid(null,0,100))
        assertFalse(TurkeyRangeCheck.valid("",0,100))
        assertFalse(TurkeyRangeCheck.valid("bytes */100",0,100))
        assertFalse(TurkeyRangeCheck.valid("bytes 20-10/100",20,100))
        assertFalse(TurkeyRangeCheck.valid("bytes abc-def/100",0,100))
    }
    @Test fun countryManifestHasSevenDistinctFixedSizeSources() {
        assertTrue(TurkeyAssets.all.map{it.name}.distinct().size==7)
        assertTrue(TurkeyAssets.totalBytes==1_821_111_961L)
        assertTrue(TurkeyAssets.all.all{it.size>0 && it.sha256.length==64})
    }
}
