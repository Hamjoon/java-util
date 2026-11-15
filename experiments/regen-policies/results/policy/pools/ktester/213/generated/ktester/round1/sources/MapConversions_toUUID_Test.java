package com.cedarsoftware.util.convert;

import org.mockito.Mock;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
// import org.junit.*;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
// @ExtendWith(MockitoExtension.class)
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapConversions_toUUID_Test {

    @Mock
    private Converter // mocked Converter used by the tests
    converter;

    /* --------------------------------------------------------------------- */
//     private static Stream<Map<String, Object>> fallbackMaps() {
//         return Stream.of(// mostSigBits missing
//         new HashMap<>() {
// 
//             {
//                 put("leastSigBits", 42L);
//             }
//         }, // leastSigBits missing
//         new HashMap<>() {
// 
//             {
//                 put("mostSigBits", 42L);
//             }
//         }, // both keys present but null
//         new HashMap<>() {
// 
//             {
//                 put("mostSigBits", null);
//                 put("leastSigBits", null);
//             }
//         }, // mostSigBits present, leastSigBits null
//         new HashMap<>() {
// 
//             {
//                 put("mostSigBits", 123L);
//                 put("leastSigBits", null);
//             }
//         }, // leastSigBits present, mostSigBits null
//         new HashMap<>() {
// 
//             {
//                 put("mostSigBits", null);
//                 put("leastSigBits", 456L);
//             }
//         });
//     }

    /**
     * Utility to invoke the private static {@code dispatch} method via reflection.
     * This method is *not* used by the current test suite because the fallback
     * behaviour is verified through observable side‑effects (null result and no
     * converter interaction). It is kept here for completeness and future
     * extensions.
     */
    @SuppressWarnings("unchecked")
    private static <T> T invokeDispatch(Object from, Converter converter, Class<T> clazz, String[] keys) throws Exception {
        java.lang.reflect.Method method = MapConversions.class.getDeclaredMethod("dispatch", Object.class, Converter.class, Class.class, String[].class);
        method.setAccessible(true);
        return (T) method.invoke(null, from, converter, clazz, keys);
    }

    /**
     * Fast‑path test – both required keys are present and conversion succeeds.
     */
//     @Test
//     void toUUID_WithBothBits_ReturnsExpectedUuid() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("mostSigBits", "123");
//         map.put("leastSigBits", "456");
//         when(converter.convert("123", long.class)).thenReturn(123L);
//         when(converter.convert("456", long.class)).thenReturn(456L);
//         Object result = MapConversions.toUUID(map, converter);
//         assertTrue(result instanceof UUID, "Result should be a UUID instance");
//         UUID expected = new UUID(123L, 456L);
//         assertEquals(expected, result);
//         verify(converter).convert("123", long.class);
//         verify(converter).convert("456", long.class);
//         verifyNoMoreInteractions(converter);
//     }

    /**
     * Fast‑path test – string values that represent long numbers.
     */
//     @Test
//     void toUUID_WithBothBits_StringValues_ReturnsExpectedUuid() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("mostSigBits", "1234567890123456789");
//         map.put("leastSigBits", "987654321098765432");
//         when(converter.convert("1234567890123456789", long.class)).thenReturn(1234567890123456789L);
//         when(converter.convert("987654321098765432", long.class)).thenReturn(987654321098765432L);
//         Object result = MapConversions.toUUID(map, converter);
//         assertTrue(result instanceof UUID, "Result should be a UUID instance");
//         UUID expected = new UUID(1234567890123456789L, 987654321098765432L);
//         assertEquals(expected, result);
//         verify(converter).convert("1234567890123456789", long.class);
//         verify(converter).convert("987654321098765432", long.class);
//         verifyNoMoreInteractions(converter);
//     }

    /**
     * Fast‑path test – numeric values (Long and Integer).
     */
//     @Test
//     void toUUID_WithBothBits_NumericValues_ReturnsExpectedUuid() {
//         Map<String, Object> map = Map.of("mostSigBits", 1L, "leastSigBits", 2);
//         when(converter.convert(1L, long.class)).thenReturn(1L);
//         when(converter.convert(2, long.class)).thenReturn(2L);
//         Object result = MapConversions.toUUID(map, converter);
//         assertEquals(new UUID(1L, 2L), result);
//         verify(converter).convert(1L, long.class);
//         verify(converter).convert(2, long.class);
//         verifyNoMoreInteractions(converter);
//     }

    /**
     * Fast‑path test – extra unrelated keys are ignored.
     */
//     @Test
//     void toUUID_WithBothBits_ExtraUnrelatedKeys_IgnoresExtras() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("mostSigBits", 10L);
//         map.put("leastSigBits", 20L);
//         map.put("extra", "ignore");
//         when(converter.convert(10L, long.class)).thenReturn(10L);
//         when(converter.convert(20L, long.class)).thenReturn(20L);
//         Object result = MapConversions.toUUID(map, converter);
//         assertEquals(new UUID(10L, 20L), result);
//         verify(converter).convert(10L, long.class);
//         verify(converter).convert(20L, long.class);
//         verifyNoMoreInteractions(converter);
//     }

    /**
     * Fast‑path test – converter throws on mostSigBits conversion.
     */
//     @Test
//     void toUUID_ConverterThrowsOnMostSigBits_PropagatesException() {
//         Map<String, Object> map = Map.of("mostSigBits", "bad", "leastSigBits", "123");
//         when(converter.convert("bad", long.class)).thenThrow(new IllegalArgumentException("cannot convert mostSigBits"));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toUUID(map, converter));
//         assertEquals("cannot convert mostSigBits", ex.getMessage());
//         verify(converter, never()).convert("123", long.class);
//     }

    /**
     * Fast‑path test – converter throws on leastSigBits conversion.
     */
//     @Test
//     void toUUID_ConverterThrowsOnLeastSigBits_PropagatesException() {
//         Map<String, Object> map = Map.of("mostSigBits", "111", "leastSigBits", "bad");
//         when(converter.convert("111", long.class)).thenReturn(111L);
//         when(converter.convert("bad", long.class)).thenThrow(new IllegalArgumentException("cannot convert leastSigBits"));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toUUID(map, converter));
//         assertEquals("cannot convert leastSigBits", ex.getMessage());
//         verify(converter).convert("111", long.class);
//         verify(converter).convert("bad", long.class);
//     }

    /**
     * Verify that the source map is not mutated by the conversion.
     */
//     @Test
//     void toUUID_DoesNotMutateSourceMap() {
//         Map<String, Object> original = new HashMap<>();
//         original.put("mostSigBits", 7L);
//         original.put("leastSigBits", 8L);
//         Map<String, Object> copy = new HashMap<>(original);
//         when(converter.convert(7L, long.class)).thenReturn(7L);
//         when(converter.convert(8L, long.class)).thenReturn(8L);
//         MapConversions.toUUID(original, converter);
//         assertEquals(copy, original);
//     }

    /**
     * Parameterized test for all fallback scenarios – method should return null and not touch the converter.
     */
//     @ParameterizedTest(name = "{index}: fallback map {0}")
//     @MethodSource("fallbackMaps")
//     void toUUID_FallbackScenarios_ReturnNullAndNoConversion(Map<String, Object> map) {
//         Object result = MapConversions.toUUID(map, converter);
//         assertNull(result, "Expected null for fallback map: " + map);
//         verifyNoInteractions(converter);
//     }

    /**
     * Non‑map input should result in a {@link ClassCastException}.
     */
    @Test
    void toUUID_NonMapInput_ThrowsClassCastException() {
        String notAMap = "I am not a map";
        assertThrows(ClassCastException.class, () -> MapConversions.toUUID(notAMap, converter));
        verifyNoInteractions(converter);
    }
}