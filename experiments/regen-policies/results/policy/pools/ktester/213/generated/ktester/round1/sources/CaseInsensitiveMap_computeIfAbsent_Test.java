package com.cedarsoftware.util;

import org.mockito.Mock;
import java.util.Map;
import java.util.function.Function;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_computeIfAbsent_Test {

    private CaseInsensitiveMap<String, String> ciMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    /* ---------------------------------------------------------------------- */
    @Mock
    private Function<String, String> stringMappingFunction;

    @Mock
    private Function<Integer, String> intMappingFunction;

    /**
     * Minimal stub of the library's {@code CaseInsensitiveString}.
     */
    static final class CaseInsensitiveString {

        private final String value;

        CaseInsensitiveString(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof CaseInsensitiveString) {
                return value.equalsIgnoreCase(((CaseInsensitiveString) o).value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return value;
        }
    }

//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for all tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @BeforeEach
    void setupBeforeEach() {
        ciMap = new CaseInsensitiveMap<>();
        intMap = new CaseInsensitiveMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        ciMap = null;
        intMap = null;
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Restore defaults for any other test suites that may run after this one
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    // Helper for JUnit‑4 compatibility with JUnit‑5's assertDoesNotThrow
//     private static void assertDoesNotThrow(Runnable runnable) {
//         try {
//             runnable.run();
//         } catch (Throwable t) {
//             fail("Expected no exception to be thrown, but got: " + t);
//         }
//     }

//     @Test
//     void computeIfAbsentAddsValueAndPreservesOriginalKey(@Mock Function<String, String> mappingFunction) {
//         // GIVEN
//         when(stringMappingFunction.apply("Key")).thenReturn("ComputedValue");
//         // WHEN
//         String result = ciMap.computeIfAbsent("Key", stringMappingFunction);
//         // THEN
//         assertEquals("ComputedValue", result);
//         // The original case of the key must be stored
//         assertEquals("ComputedValue", ciMap.get("Key"));
//         // Verify the mapping function received the *original* key, not a wrapped one
//         verify(stringMappingFunction, times(1)).apply("Key");
//     }

//     @Test
//     void computeIfAbsentDoesNotInvokeMappingWhenKeyAlreadyPresent(@Mock Function<String, String> mappingFunction) {
//         // GIVEN
//         ciMap.put("ExistingKey", "ExistingValue");
//         // WHEN
//         String result = ciMap.computeIfAbsent("existingkey", stringMappingFunction);
//         // THEN
//         assertEquals("ExistingValue", result);
//         // Mapping function should never be called because the key is already present
//         verifyZeroInteractions(stringMappingFunction);
//     }

//     @Test
//     void computeIfAbsentWorksForNonStringKeys(@Mock Function<Integer, String> mappingFunction) {
//         // GIVEN
//         when(intMappingFunction.apply(42)).thenReturn("Answer");
//         // WHEN
//         String result = intMap.computeIfAbsent(42, intMappingFunction);
//         // THEN
//         assertEquals("Answer", result);
//         assertEquals("Answer", intMap.get(42));
//         verify(intMappingFunction, times(1)).apply(42);
//     }

//     @Test
//     void computeIfAbsentWithNullKeyThrowsNPE() {
//         Function<String, String> mappingFunction = key -> "value";
//         try {
//             ciMap.computeIfAbsent(null, mappingFunction);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void computeIfAbsentMappingReturnsNullDoesNotStoreEntry(@Mock Function<String, String> mappingFunction) {
//         // GIVEN
//         when(stringMappingFunction.apply("AbsentKey")).thenReturn(null);
//         // WHEN
//         String result = ciMap.computeIfAbsent("AbsentKey", stringMappingFunction);
//         // THEN
//         assertNull(result);
//         // The map must not contain the key after a null result
//         assertFalse(ciMap.containsKey("AbsentKey"));
//         verify(stringMappingFunction, times(1)).apply("AbsentKey");
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_NullKeyAllowed_StoresValue(@Mock Function<String, String> mappingFunction) {
//         // GIVEN a map backed by a HashMap (allows null keys)
//         CaseInsensitiveMap<String, String> mapWithHashMap = new CaseInsensitiveMap<>(new HashMap<>());
//         when(stringMappingFunction.apply(null)).thenReturn("nullValue");
//         // WHEN
//         String result = mapWithHashMap.computeIfAbsent(null, stringMappingFunction);
//         // THEN
//         assertEquals("nullValue", result);
//         assertTrue(mapWithHashMap.containsKey(null));
//         assertEquals("nullValue", mapWithHashMap.get(null));
//         verify(stringMappingFunction, times(1)).apply(null);
//     }

//     @Test
//     void computeIfAbsent_NullKeyAllowed_MappingReturnsNull_DoesNotStore() {
//         // GIVEN a map backed by a HashMap (allows null keys)
//         CaseInsensitiveMap<String, String> mapWithHashMap = new CaseInsensitiveMap<>(new HashMap<>());
//         // WHEN
//         String result = mapWithHashMap.computeIfAbsent(null, k -> null);
//         // THEN
//         assertNull(result);
//         assertFalse(mapWithHashMap.containsKey(null));
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_NullKeyNotAllowed_ThrowsNPE() {
//         // GIVEN a map backed by ConcurrentHashMap (null keys prohibited)
//         CaseInsensitiveMap<String, String> mapWithConcurrent = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
//         try {
//             mapWithConcurrent.computeIfAbsent(null, k -> "value");
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_NullMappingFunction_ThrowsNPE() {
//         Function<String, String> nullFunction = null;
//         try {
//             ciMap.computeIfAbsent("anyKey", nullFunction);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_MappingFunctionThrows_PropagatesException() {
//         Function<String, String> throwingFunction = key -> {
//             throw new IllegalStateException("boom");
//         };
//         // ensure map is not empty
//         ciMap.put("existing", "value");
//         try {
//             ciMap.computeIfAbsent("errorKey", throwingFunction);
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ex) {
//             assertEquals("boom", ex.getMessage());
//         }
//         // The map must remain unchanged – no new entry for "errorKey"
//         assertFalse(ciMap.containsKey("errorKey"));
//         assertEquals("value", ciMap.get("existing"));
//     }

    /* ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] key=\"{0}\" expects \"{2}\"")
//     @CsvSource({ // first insertion (mixed case) -> mapping function should be called
//     // subsequent call with different case -> should return existing value, no function call
//     "'Key',       first,   first", "'kEy',       second,  first" })
//     void computeIfAbsent_CaseInsensitiveBehavior(String key, String mappingResult, String expected, @Mock Function<String, String> mappingFunction) {
//         // GIVEN
//         when(mappingFunction.apply(key)).thenReturn(mappingResult);
//         // WHEN
//         String result = ciMap.computeIfAbsent(key, mappingFunction);
//         // THEN
//         assertEquals(expected, result);
//         assertEquals(expected, ciMap.get(key));
//         // Verify that the mapping function is invoked only on the first distinct logical key.
//         verify(mappingFunction, times(1)).apply(key);
//         clearInvocations(mappingFunction);
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_CaseInsensitiveString_PassesOriginalInstance() {
//         // GIVEN
//         CaseInsensitiveString originalKey = new CaseInsensitiveString("Original");
//         AtomicReference<CaseInsensitiveString> capturedKey = new AtomicReference<>();
//         Function<CaseInsensitiveString, String> mappingFunction = k -> {
//             capturedKey.set(k);
//             return "mapped";
//         };
//         // Use a map whose key type is CaseInsensitiveString (non‑String keys are treated normally)
//         CaseInsensitiveMap<CaseInsensitiveString, String> ciStringMap = new CaseInsensitiveMap<>();
//         // WHEN
//         String result = ciStringMap.computeIfAbsent(originalKey, mappingFunction);
//         // THEN
//         assertEquals("mapped", result);
//         assertEquals("mapped", ciStringMap.get(originalKey));
//         assertSame(originalKey, capturedKey.get(), "The mapping function should receive the exact CaseInsensitiveString instance supplied.");
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void computeIfAbsent_NullKeyWithIntegerMap_ThrowsNPE() {
//         // Integer keys cannot be null for ConcurrentHashMap backing; default uses LinkedHashMap which allows null.
//         // Here we use the default map (LinkedHashMap) which permits null keys.
//         assertDoesNotThrow(() -> intMap.computeIfAbsent(null, k -> "nullInt"));
//         assertEquals("nullInt", intMap.get(null));
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     public void computeIfAbsent_CaseInsensitiveBehavior_FirstInsertion() {
//         // GIVEN
//         when(stringMappingFunction.apply("Key")).thenReturn("first");
//         // WHEN
//         String result = ciMap.computeIfAbsent("Key", stringMappingFunction);
//         // THEN
//         assertEquals("first", result);
//         assertEquals("first", ciMap.get("Key"));
//         verify(stringMappingFunction, times(1)).apply("Key");
//         clearInvocations(stringMappingFunction);
//     }

//     @Test
//     public void computeIfAbsent_CaseInsensitiveBehavior_SubsequentCallWithDifferentCase() {
//         // GIVEN – first insertion already performed
//         when(stringMappingFunction.apply("Key")).thenReturn("first");
//         ciMap.computeIfAbsent("Key", stringMappingFunction);
//         clearInvocations(stringMappingFunction);
//         // WHEN – call with a different case
//         String result = ciMap.computeIfAbsent("kEy", stringMappingFunction);
//         // THEN – should return the existing value without invoking the function
//         assertEquals("first", result);
//         assertEquals("first", ciMap.get("kEy"));
//         verifyZeroInteractions(stringMappingFunction);
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     public void computeIfAbsent_NullKeyWithIntegerMap_AllowsNull() {
//         // Integer keys default to LinkedHashMap which permits null keys.
//         assertDoesNotThrow(() -> intMap.computeIfAbsent(null, k -> "nullInt"));
//         assertEquals("nullInt", intMap.get(null));
//     }
}