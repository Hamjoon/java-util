package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_computeIfAbsent_Test {

    private CaseInsensitiveMap<String, String> map;

    @Mock
    private Function<String, String> // mock for the mapping function
    mappingFunction;

    @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
        // Mockito will inject the mock into the field annotated with @Mock
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared between tests
        map.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// key absent
        Arguments.of("Hello", false), // same logical key already present
        Arguments.of("HeLLo", true));
    }

    @Test
    void computeIfAbsentAddsNewEntryAndReturnsValue() {
        // GIVEN
        when(mappingFunction.apply("Key")).thenReturn("Value");
        // WHEN
        String result = map.computeIfAbsent("Key", mappingFunction);
        // THEN
        assertEquals("Value", result, "Returned value must be the one supplied by the mapping function");
        assertEquals("Value", map.get("key"), "Lookup must be case‑insensitive");
        verify(mappingFunction, times(1)).apply("Key");
    }

    @Test
    void computeIfAbsentIsCaseInsensitiveAndDoesNotInvokeMappingWhenKeyExists() {
        // GIVEN
        map.put("ExistingKey", "ExistingValue");
        // mappingFunction should not be called at all
        when(mappingFunction.apply(any())).thenReturn("NewValue");
        // WHEN
        String result = map.computeIfAbsent("eXiStInGkEy", mappingFunction);
        // THEN
        assertEquals("ExistingValue", result, "Existing value must be returned");
        verify(mappingFunction, never()).apply(any());
    }

    @Test
    void computeIfAbsentDoesNotStoreNullResult() {
        // GIVEN
        when(mappingFunction.apply("AbsentKey")).thenReturn(null);
        // WHEN
        String result = map.computeIfAbsent("AbsentKey", mappingFunction);
        // THEN
        assertNull(result, "Result should be null when mapping function returns null");
        assertFalse(map.containsKey("AbsentKey"), "Map must not contain a mapping for a null result");
        verify(mappingFunction, times(1)).apply("AbsentKey");
    }

    @Test
    void computeIfAbsentWithNullKeyIsHandledByBackingMap() {
        // GIVEN
        when(mappingFunction.apply(null)).thenReturn("NullValue");
        // WHEN
        String result = map.computeIfAbsent(null, mappingFunction);
        // THEN
        assertEquals("NullValue", result);
        assertEquals("NullValue", map.get(null));
        verify(mappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsentPassesOriginalKeyToMappingFunction() {
        // GIVEN
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(mappingFunction.apply(any())).thenReturn("Captured");
        // WHEN
        map.computeIfAbsent("MiXeDCase", mappingFunction);
        // THEN
        verify(mappingFunction).apply(captor.capture());
        assertEquals("MiXeDCase", captor.getValue(), "Mapping function must receive the original key (preserving case)");
    }

    /**
     * Parameterised test that covers the two basic scenarios described in the
     * JSON spec: a missing key (function invoked) and an already‑present key
     * (function not invoked).
     */
    @ParameterizedTest(name = "{index} => key={0}, alreadyPresent={1}")
    @MethodSource("basicFunctionalityProvider")
    void computeIfAbsentBasicFunctionality(String key, boolean alreadyPresent) {
        // Arrange
        // the map normalises strings to lower‑case
        String normalized = key.toLowerCase();
        if (alreadyPresent) {
            map.put(key, "existing");
        }
        // clear previous interactions
        reset(mappingFunction);
        when(mappingFunction.apply(key)).thenReturn("computed");
        // Act
        String result = map.computeIfAbsent(key, mappingFunction);
        // Assert
        if (alreadyPresent) {
            assertEquals("existing", result, "Existing value must be returned when key is already present");
            verify(mappingFunction, never()).apply(any());
        } else {
            assertEquals("computed", result, "Result must be the value returned by the mapping function");
            assertEquals("computed", map.get(normalized), "Map must store the value under the normalized key");
            verify(mappingFunction, times(1)).apply(key);
        }
    }

    @Test
    void computeIfAbsentNullMappingFunctionThrowsNPE() {
        // The Map contract requires a NullPointerException when the mapping function is null.
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent("any", null), "computeIfAbsent must throw NPE when mappingFunction is null");
    }

    @Test
    void computeIfAbsentUnsupportedKeyTypeThrowsClassCastException() {
        // The map only normalises String (or CaseInsensitiveString) keys.
        assertThrows(ClassCastException.class, () -> map.computeIfAbsent((String) (Object) 123, k -> "value"), "Supplying an Integer key should result in ClassCastException");
    }

    @Test
    void computeIfAbsentNullKeyThrowsNPE() {
        // According to the Map contract a null key is not allowed for this implementation.
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, k -> "value"), "Passing a null key must result in NullPointerException");
    }

    @Test
    void computeIfAbsentMappingFunctionThrowsIllegalStateExceptionIsPropagated() {
        // GIVEN
        when(mappingFunction.apply("error")).thenThrow(new IllegalStateException("boom"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.computeIfAbsent("error", mappingFunction), "Exception from mappingFunction must be propagated");
        assertEquals("boom", ex.getMessage());
        assertFalse(map.containsKey("error"), "Map must remain unchanged after exception");
    }

    @Test
    void computeIfAbsentMappingFunctionThrowsRuntimeExceptionIsPropagated() {
        // GIVEN
        when(mappingFunction.apply("any")).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.computeIfAbsent("any", mappingFunction), "Unchecked exception from mappingFunction must be propagated");
        assertEquals("boom", ex.getMessage());
        assertFalse(map.containsKey("any"), "Map must remain unchanged after exception");
    }

    @Test
    void computeIfAbsentWithCaseInsensitiveStringKeyPassesOriginalKeyToFunction() {
        // CASE: key is a CaseInsensitiveString – the mapping function should receive the original instance.
        CaseInsensitiveString originalKey = new CaseInsensitiveString("TeSt");
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        when(mappingFunction.apply(any())).thenReturn("result");
        // WHEN
        String result = map.computeIfAbsent((String) originalKey, mappingFunction);
        // THEN
        assertEquals("result", result);
        // stored under normalized form
        assertEquals("result", map.get("test"));
        verify(mappingFunction).apply(captor.capture());
        assertSame(originalKey, captor.getValue(), "Mapping function must receive the original CaseInsensitiveString instance");
    }

    @Test
    void computeIfAbsentNullValueFromMappingFunctionWithHashtableBackedMapThrowsNPE() throws Exception {
        // Create a map that uses a Hashtable as its backing store (which disallows null values).
        CaseInsensitiveMap<String, String> hashtableMap = new CaseInsensitiveMap<>(new Hashtable<>());
        // Verify that the backing map is indeed a Hashtable (using reflection on the private field).
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<?, ?> backing = (Map<?, ?>) mapField.get(hashtableMap);
        assertTrue(backing instanceof Hashtable, "Backing map should be a Hashtable");
        // Mock mapping function to return null.
        Function<String, String> nullReturningFunction = mock(Function.class);
        when(nullReturningFunction.apply("test")).thenReturn(null);
        // The contract of Hashtable (no null values) means computeIfAbsent must throw NPE.
        assertThrows(NullPointerException.class, () -> hashtableMap.computeIfAbsent("test", nullReturningFunction), "computeIfAbsent must throw NPE when underlying map disallows null values");
    }

    /* --------------------------------------------------------------------- *
     *  Helper method – not part of the test suite, used only for the Hashtable *
     *  specific test above.                                                   *
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private <K, V> void setBackingMap(CaseInsensitiveMap<K, V> target, Map<K, V> backing) throws Exception {
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(target, backing);
    }
}
