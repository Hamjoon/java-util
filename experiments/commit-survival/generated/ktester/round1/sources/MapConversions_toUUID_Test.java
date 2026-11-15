package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toUUID_Test {

    @Mock
    Converter // mocked conversion utility
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference
        Mockito.reset(converter);
    }

    /**
     * When the source map contains the key {@code "UUID"} the method must delegate to
     * {@code Converter.convert(uuidValue, UUID.class)} and return the conversion result.
     */
    @Test
    void toUUID_usesUuidKeyIfPresent() {
        // GIVEN
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        UUID expected = UUID.fromString(uuidString);
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", uuidString);
        when(converter.convert(uuidString, UUID.class)).thenReturn(expected);
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertEquals(expected, result, "Result should be the UUID returned by the converter");
        verify(converter, times(1)).convert(uuidString, UUID.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * When the source map contains {@code "mostSigBits"} and {@code "leastSigBits"} the
     * method must convert both to {@code long} and construct a new {@link UUID}.
     */
    @Test
    void toUUID_usesMostAndLeastSigBitsIfPresent() {
        // GIVEN
        long most = 0x1122334455667788L;
        long least = 0x99AABBCCDDEEFF00L;
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", most);
        map.put("leastSigBits", least);
        // The converter is asked to convert the raw objects to long primitives.
        when(converter.convert(most, long.class)).thenReturn(most);
        when(converter.convert(least, long.class)).thenReturn(least);
        UUID expected = new UUID(most, least);
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertEquals(expected, result, "Result should be a UUID built from the supplied bits");
        verify(converter, times(1)).convert(most, long.class);
        verify(converter, times(1)).convert(least, long.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * When neither {@code "UUID"} nor the bit‑pair keys are present, the method falls back
     * to {@code fromMap(...)}.  The fallback ultimately calls {@code Converter.convert(..., UUID.class)}
     * on the {@code "value"} entry (as defined by the conversion framework).  This test verifies
     * that behaviour.
     */
    @Test
    void toUUID_fallsBackToFromMapWhenNoSpecificKeys() {
        // GIVEN
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        UUID expected = UUID.fromString(uuidString);
        Map<String, Object> map = new HashMap<>();
        // the generic “value” key used by fromMap
        map.put("value", uuidString);
        when(converter.convert(uuidString, UUID.class)).thenReturn(expected);
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertEquals(expected, result, "Result should be the UUID produced by the fallback conversion");
        // The only conversion that should happen is the final UUID conversion.
        verify(converter, times(1)).convert(uuidString, UUID.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * If only {@code "leastSigBits"} is present the method should fall back to {@code fromMap}
     * which, for the supplied data, yields {@code null}.
     */
    @Test
    void toUUID_missingMostSigBitsFallsBackAndReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("leastSigBits", 456L);
        // No conversion expectations – fromMap will not find a usable value.
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertNull(result, "When mostSigBits is missing the method should return null");
        verifyNoInteractions(converter);
    }

    /**
     * If only {@code "mostSigBits"} is present the method should fall back to {@code fromMap}
     * which, for the supplied data, yields {@code null}.
     */
    @Test
    void toUUID_missingLeastSigBitsFallsBackAndReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 123L);
        // No conversion expectations – fromMap will not find a usable value.
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertNull(result, "When leastSigBits is missing the method should return null");
        verifyNoInteractions(converter);
    }

    /**
     * When the map contains no relevant keys at all, the fallback {@code fromMap}
     * returns {@code null}.
     */
    @Test
    void toUUID_fallbackWithNoRelevantKeysReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        // No conversion expectations.
        // WHEN
        Object result = MapConversions.toUUID(map, converter);
        // THEN
        assertNull(result, "Fallback should return null when map lacks UUID and bit keys");
        verifyNoInteractions(converter);
    }

    /**
     * Providing a non‑map object as {@code from} should cause a {@link ClassCastException}
     * because the method casts the argument to {@code Map<String,Object>}.
     */
    @Test
    void toUUID_invalidFromTypeThrowsClassCastException() {
        // GIVEN
        String notAMap = "not a map";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toUUID(notAMap, converter), "Casting a non‑map to Map should throw ClassCastException");
        verifyNoInteractions(converter);
    }

    /**
     * Passing {@code null} as the {@code from} argument leads to a {@link NullPointerException}
     * when the method attempts to call {@code map.get(...)}.
     */
    @Test
    void toUUID_nullFromThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(nullMap, converter), "Calling toUUID with a null map should throw NullPointerException");
        verifyNoInteractions(converter);
    }

    /**
     * Passing {@code null} as the {@code converter} argument leads to a {@link NullPointerException}
     * when the method attempts to invoke {@code converter.convert(...)}.
     */
    @Test
    void toUUID_nullConverterThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", "123e4567-e89b-12d3-a456-426655440000");
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(map, nullConverter), "Calling toUUID with a null converter should throw NullPointerException");
    }

    /**
     * If the converter throws an {@link IllegalArgumentException} while converting the
     * {@code "UUID"} entry, the exception must propagate unchanged.
     */
    @Test
    void toUUID_converterThrowsOnUuidConversion() {
        // GIVEN
        String badUuid = "bad-uuid-string";
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", badUuid);
        when(converter.convert(badUuid, UUID.class)).thenThrow(new IllegalArgumentException("Invalid UUID format"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toUUID(map, converter), "Exception from converter should propagate");
        assertEquals("Invalid UUID format", ex.getMessage());
        verify(converter, times(1)).convert(badUuid, UUID.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * If conversion of the most‑significant bits throws a {@link NumberFormatException},
     * the exception must propagate.
     */
    @Test
    void toUUID_converterThrowsOnMostSigBitsConversion() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", "not-a-long");
        map.put("leastSigBits", "456");
        when(converter.convert("not-a-long", long.class)).thenThrow(new NumberFormatException("Cannot parse mostSigBits"));
        // The leastSigBits conversion would not be reached, but we can stub it anyway.
        when(converter.convert("456", long.class)).thenReturn(456L);
        // WHEN / THEN
        NumberFormatException ex = assertThrows(NumberFormatException.class, () -> MapConversions.toUUID(map, converter), "Exception from mostSigBits conversion should propagate");
        assertEquals("Cannot parse mostSigBits", ex.getMessage());
        verify(converter, times(1)).convert("not-a-long", long.class);
        // No interaction for leastSigBits because the method aborts on the first exception.
        verifyNoMoreInteractions(converter);
    }

    /**
     * If conversion of the least‑significant bits throws a {@link NumberFormatException},
     * the exception must propagate.
     */
    @Test
    void toUUID_converterThrowsOnLeastSigBitsConversion() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", "123");
        map.put("leastSigBits", "not-a-long");
        when(converter.convert("123", long.class)).thenReturn(123L);
        when(converter.convert("not-a-long", long.class)).thenThrow(new NumberFormatException("Cannot parse leastSigBits"));
        // WHEN / THEN
        NumberFormatException ex = assertThrows(NumberFormatException.class, () -> MapConversions.toUUID(map, converter), "Exception from leastSigBits conversion should propagate");
        assertEquals("Cannot parse leastSigBits", ex.getMessage());
        verify(converter, times(1)).convert("123", long.class);
        verify(converter, times(1)).convert("not-a-long", long.class);
        verifyNoMoreInteractions(converter);
    }
}
