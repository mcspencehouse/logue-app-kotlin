package com.spencehouse.logue

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testLocationParsing() {
        val payload = """
            {"state":{"reported":{"cigServiceRequestId":"location2276458971781663039345","status":"SUCCESS","responseBody":{"ignition":"unknown","gpsData":{"coordinate":{"latitude":42.757122,"longitude":-73.94447,"datum":"na","format":""},"dtTime":"2026-06-17T02:24:09Z","velocity":{"value":0.0,"unit":"na"},"courseHeading":0.0,"accuracy":{"pdop":0,"hdop":0}}}}}}
        """.trimIndent()

        val data = JSONObject(payload)
        val reported = data.optJSONObject("state")?.optJSONObject("reported")
        assertNotNull("reported object should not be null", reported)
        
        val rb = reported!!.optJSONObject("responseBody")
        assertNotNull("responseBody object should not be null", rb)

        val gpsData = rb!!.optJSONObject("gpsData")
        assertNotNull("gpsData object should not be null", gpsData)
        
        val coordinate = gpsData!!.optJSONObject("coordinate")
        assertNotNull("coordinate object should not be null", coordinate)

        val latitude = coordinate!!.optDouble("latitude")
        val longitude = coordinate!!.optDouble("longitude")

        assertEquals(42.757122, latitude, 0.000001)
        assertEquals(-73.94447, longitude, 0.000001)
    }

    @Test
    fun testDateParsing() {
        val startTimeStr = "2026-04-22T13:23:11.969+0000"
        try {
            val sf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
            val date = sf.parse(startTimeStr)
            assertNotNull("Parsed date should not be null", date)
            println("Successfully parsed date: $date, millis: ${date?.time}")
        } catch (e: Exception) {
            fail("Failed to parse date: ${e.message}")
        }
    }
}