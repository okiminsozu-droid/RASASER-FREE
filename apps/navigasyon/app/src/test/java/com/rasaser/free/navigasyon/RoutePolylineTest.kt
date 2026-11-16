package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test

class RoutePolylineTest {
    // 1e6 precision: (41.000000, 29.000000), then (41.000100,29.000050)
    private fun encode(points:List<NavPoint>):String {
        val out=StringBuilder();var lat=0L;var lon=0L
        fun add(diff:Long) {
            var v=if(diff<0L) ((-diff) shl 1)-1 else diff shl 1
            while(v>=32) {
                out.append(((32 or (v and 31).toInt())+63).toChar())
                v=v shr 5
            }
            out.append((v.toInt()+63).toChar())
        }
        for(p in points){
            val a=(p.lat*1_000_000).toLong();val b=(p.lon*1_000_000).toLong()
            add(a-lat);add(b-lon);lat=a;lon=b
        }
        return out.toString()
    }
    @Test fun decodeTwoLocations() {
        val input=listOf(NavPoint(41.0,29.0),NavPoint(41.000100,29.000050))
        val output=decodePolyline6(encode(input))
        assertEquals(2,output.size)
        for(i in input.indices) {
            assertEquals(input[i].lat,output[i].lat,0.000001)
            assertEquals(input[i].lon,output[i].lon,0.000001)
        }
    }
    @Test fun damagedPolylineIsNotRendered(){
        assertTrue(decodePolyline6("~~~~").isEmpty())
        assertTrue(decodePolyline6("a").isEmpty())
    }
    @Test fun invalidCoordinateRejected(){
        assertTrue(decodePolyline6(encode(listOf(NavPoint(95.0,35.0)))).isEmpty())
    }
}
