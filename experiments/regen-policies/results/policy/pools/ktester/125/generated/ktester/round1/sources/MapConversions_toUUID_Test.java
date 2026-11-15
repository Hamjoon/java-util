package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toUUID_Test {

    @Mock
    Converter // injected by Mockito
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference
        Mockito.reset(mockConverter);
    }

    /**
     * When the map contains a {@code "UUID"} entry, {@code MapConversions.toUUID}
     * should delegate to {@link Converter#convert(Object, Class)} and return
     * the conversion result.
     */
    @Test
    void toUUID_usesUuidKey_whenPresent() {
        // GIVEN
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        UUID expected = UUID.fromString(uuidString);
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", uuidString);
        // mock the conversion of the string to a UUID
        when(mockConverter.convert(uuidString, UUID.class)).thenReturn(expected);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter, times(1)).convert(uuidString, UUID.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When the map contains {@code "mostSigBits"} and {@code "leastSigBits"} entries,
     * {@code MapConversions.toUUID} should convert both to {@code long} values and
     * construct a new {@link UUID} from them.
     */
    @Test
    void toUUID_usesMostAndLeastSigBits_whenUuidKeyAbsent() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", "1");
        map.put("leastSigBits", "2");
        // mock conversion of the string representations to long primitives
        when(mockConverter.convert("1", long.class)).thenReturn(1L);
        when(mockConverter.convert("2", long.class)).thenReturn(2L);
        UUID expected = new UUID(1L, 2L);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter, times(1)).convert("1", long.class);
        verify(mockConverter, times(1)).convert("2", long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_prefersUuidKey_overBitsWhenBothPresent() {
        // GIVEN
        String uuidString = "123e4567-e89b-12d3-a456-426614174001";
        UUID expected = UUID.fromString(uuidString);
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", uuidString);
        map.put("mostSigBits", 999L);
        map.put("leastSigBits", 888L);
        when(mockConverter.convert(uuidString, UUID.class)).thenReturn(expected);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        // verify that conversion of the bits never happened
        verify(mockConverter, never()).convert(eq(999L), eq(long.class));
        verify(mockConverter, never()).convert(eq(888L), eq(long.class));
        verify(mockConverter, times(1)).convert(uuidString, UUID.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_convertsNonStringUuidValue_usingConverter() {
        // GIVEN
        byte[] bytes = new byte[] { 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        UUID expected = UUID.nameUUIDFromBytes(bytes);
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", bytes);
        // Converter creates a name‑based UUID from the byte array
        when(mockConverter.convert(eq(bytes), eq(UUID.class))).thenReturn(expected);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).convert(eq(bytes), eq(UUID.class));
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_propagatesNullWhenConverterReturnsNullForUuidKey() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", "any-value");
        when(mockConverter.convert("any-value", UUID.class)).thenReturn(null);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertNull(result);
        verify(mockConverter).convert("any-value", UUID.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_fallbackToFromMap_returnsNullWhenInsufficientData() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "value");
        // No special mocking – fromMap is private and returns null for unknown keys
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertNull(result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_fallbackToFromMap_returnsNullWhenOnlyOneBitPresent() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 111);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertNull(result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_caseSensitiveKey_mismatchLeadsToNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // wrong case
        map.put("Uuid", "123e4567-e89b-12d3-a456-426614174000");
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertNull(result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_nullMap_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(nullMap, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_invalidFromType_throwsClassCastException() {
        // GIVEN
        String notAMap = "I am just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toUUID(notAMap, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_nullConverter_withUuidKey_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", "123e4567-e89b-12d3-a456-426614174000");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(map, null));
    }

    @Test
    void toUUID_nullConverter_withBits_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 123);
        map.put("leastSigBits", 456);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(map, null));
    }

    @Test
    void toUUID_converterExceptionPropagates() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 1);
        map.put("leastSigBits", 2);
        when(mockConverter.convert(1, long.class)).thenThrow(new IllegalArgumentException("bad"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toUUID(map, mockConverter));
        assertEquals("bad", ex.getMessage());
        verify(mockConverter).convert(1, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_converterReturnsNullForMostSigBits_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 111);
        map.put("leastSigBits", 222);
        when(mockConverter.convert(111, long.class)).thenReturn(null);
        when(mockConverter.convert(222, long.class)).thenReturn(222L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(map, mockConverter));
        verify(mockConverter).convert(111, long.class);
        // leastSigBits conversion may not be reached because of NPE on mostSigBits,
        // but we allow it to be called – no strict ordering required.
    }

    @Test
    void toUUID_converterReturnsNullForLeastSigBits_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 111);
        map.put("leastSigBits", 222);
        when(mockConverter.convert(111, long.class)).thenReturn(111L);
        when(mockConverter.convert(222, long.class)).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toUUID(map, mockConverter));
        verify(mockConverter).convert(111, long.class);
        verify(mockConverter).convert(222, long.class);
    }

    @Test
    void toUUID_converterReturnsNonLongForMostSigBits_throwsClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", "not-a-number");
        map.put("leastSigBits", 555L);
        when(mockConverter.convert("not-a-number", long.class)).thenReturn("not-a-number");
        when(mockConverter.convert(555L, long.class)).thenReturn(555L);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toUUID(map, mockConverter));
        verify(mockConverter).convert("not-a-number", long.class);
        // leastSigBits conversion may not be reached; verification is optional.
    }

    @Test
    void toUUID_converterReturnsNonLongForLeastSigBits_throwsClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 777L);
        map.put("leastSigBits", "also-not-a-number");
        when(mockConverter.convert(777L, long.class)).thenReturn(777L);
        when(mockConverter.convert("also-not-a-number", long.class)).thenReturn("also-not-a-number");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toUUID(map, mockConverter));
        verify(mockConverter).convert(777L, long.class);
        verify(mockConverter).convert("also-not-a-number", long.class);
    }

    @Test
    void toUUID_negativeBitValues_areHandledCorrectly() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", -1);
        // Long.MIN_VALUE
        map.put("leastSigBits", -9223372036854775808L);
        when(mockConverter.convert(-1, long.class)).thenReturn(-1L);
        when(mockConverter.convert(-9223372036854775808L, long.class)).thenReturn(-9223372036854775808L);
        UUID expected = new UUID(-1L, -9223372036854775808L);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).convert(-1, long.class);
        verify(mockConverter).convert(-9223372036854775808L, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_integerObjectsAreConvertedToLong() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", Integer.valueOf(42));
        map.put("leastSigBits", Integer.valueOf(84));
        when(mockConverter.convert(Integer.valueOf(42), long.class)).thenReturn(42L);
        when(mockConverter.convert(Integer.valueOf(84), long.class)).thenReturn(84L);
        UUID expected = new UUID(42L, 84L);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).convert(Integer.valueOf(42), long.class);
        verify(mockConverter).convert(Integer.valueOf(84), long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toUUID_stringRepresentationsAreConvertedToLong() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", "12345");
        map.put("leastSigBits", "67890");
        when(mockConverter.convert("12345", long.class)).thenReturn(12345L);
        when(mockConverter.convert("67890", long.class)).thenReturn(67890L);
        UUID expected = new UUID(12345L, 67890L);
        // WHEN
        Object result = MapConversions.toUUID(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).convert("12345", long.class);
        verify(mockConverter).convert("67890", long.class);
        verifyNoMoreInteractions(mockConverter);
    }
}
