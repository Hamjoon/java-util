package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
// import org.junit.*;
// import static org.junit.Assert.*;

class MapConversions_toLocalDateTime_Test {

    /**
     * Shared Converter used by all tests.
     */
    private static Converter converter;

    /**
     * Fresh map for each test.
     */
    private Map<String, Object> sourceMap;

//     @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor now requires a ConverterOptions instance.
        // A default instance can be created with its no‑arg constructor.
//         converter = new Converter(new ConverterOptions());
    }

//     @BeforeEach
    void setupBeforeEach() {
        sourceMap = new HashMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        sourceMap.clear();
    }

//     @AfterAll
    static void teardownAfterAll() {
        // No external resources to release.
        converter = null;
    }

    /**
     * Verify that a map containing a LocalDateTime under the "localDateTime" key is returned unchanged.
     */
//     @Test
//     void toLocalDateTime_FromMapWithLocalDateTimeKey() {
//         LocalDateTime now = LocalDateTime.now();
//         sourceMap.put(MapConversions.LOCAL_DATE_TIME, now);
//         LocalDateTime result = MapConversions.toLocalDateTime(sourceMap, converter);
//         assertEquals("The LocalDateTime should be taken directly from the 'localDateTime' key.", now, result);
//     }

    /**
     * Verify conversion when the map contains a string representation under the "value" key.
     */
//     @Test
//     void toLocalDateTime_FromMapWithValueString() {
//         String iso = "2023-08-15T12:34:56";
//         sourceMap.put(MapConversions.VALUE, iso);
//         LocalDateTime expected = LocalDateTime.parse(iso);
//         LocalDateTime result = MapConversions.toLocalDateTime(sourceMap, converter);
//         assertEquals("The string under 'value' should be parsed to a LocalDateTime.", expected, result);
//     }

    /**
     * Verify conversion when the map contains epoch milliseconds under the "_v" key.
     */
//     @Test
//     void toLocalDateTime_FromMapWithV_EpochMillis() {
//         long epochMillis = 1_688_123_456_789L;
//         sourceMap.put(MapConversions.V, epochMillis);
//         LocalDateTime expected = LocalDateTime.ofEpochSecond(epochMillis / 1000, (int) ((epochMillis % 1000) * 1_000_000), ZoneOffset.UTC);
//         LocalDateTime result = MapConversions.toLocalDateTime(sourceMap, converter);
//         assertEquals("The epoch millis under '_v' should be converted to LocalDateTime (UTC).", expected, result);
//     }

    /**
     * Verify conversion when the map contains epoch milliseconds under the "epochMillis" key.
     */
//     @Test
//     void toLocalDateTime_FromMapWithEpochMillisKey() {
//         long epochMillis = 1_600_000_000_000L;
//         sourceMap.put(MapConversions.EPOCH_MILLIS, epochMillis);
//         LocalDateTime expected = LocalDateTime.ofEpochSecond(epochMillis / 1000, (int) ((epochMillis % 1000) * 1_000_000), ZoneOffset.UTC);
//         LocalDateTime result = MapConversions.toLocalDateTime(sourceMap, converter);
//         assertEquals("The epoch millis under 'epochMillis' should be converted to LocalDateTime (UTC).", expected, result);
//     }

    /**
     * Verify that passing a null source returns null.
     */
//     @Test
//     void toLocalDateTime_NullSourceReturnsNull() {
//         LocalDateTime result = MapConversions.toLocalDateTime(null, converter);
//         assertNull("Converting a null source should return null.", result);
//     }

//     @Test
//     void toLocalDateTime_directLocalDateTimeInput() {
//         LocalDateTime expected = LocalDateTime.of(2023, 5, 17, 10, 30, 45);
//         Object result = MapConversions.toLocalDateTime(expected, converter);
//         assertTrue("Result should be a LocalDateTime instance.", result instanceof LocalDateTime);
//         assertEquals("When the source is already a LocalDateTime it must be returned unchanged.", expected, result);
//     }

//     @Test
//     void toLocalDateTime_nullConverter_throwsNullPointerException() {
//         LocalDateTime now = LocalDateTime.now();
//         MapConversions.toLocalDateTime(now, null);
//     }
}