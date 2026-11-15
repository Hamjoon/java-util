package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_initMap_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real Converter instance – used for the majority of test cases.
     */
    private static Converter realConverter;

    /**
     * Mock Converter that we can pass to the method (it should never be invoked).
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the library provides a default ctor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Supplies the test data for {@link #initMap_VariousInputs_ShouldStoreOriginalObject}.
     *
     * @return a stream of arguments: source object, converter, expected stored value
     */
    private static Stream<Arguments> provideInitMapCases() {
        // Helper objects used in several cases
        Map<String, Object> customPojo = new HashMap<>();
        customPojo.put("customField", "value");
        Map<String, Object> emptyMap = new HashMap<>();
        List<Integer> list = Arrays.asList(1, 2, 3);
        int[] intArray = new int[] { 1, 2, 3 };
        Date epochDate = new Date(0);
        // Real converter (does nothing in initMap)
        Converter real = realConverter;
        // Mock converter that will never be called – we create it lazily in the test method.
        // Here we pass null for the "converter throws" case; the method never uses it.
        return Stream.of(// --- basic functionality -------------------------------------------------
        Arguments.of(null, null, null), Arguments.of(42, null, 42), Arguments.of("sample string", null, "sample string"), Arguments.of(customPojo, null, customPojo), Arguments.of(true, mock(Converter.class), true), Arguments.of(3.14, null, 3.14), // --- additional value types ---------------------------------------------
        Arguments.of(false, null, false), Arguments.of(1234567890123L, null, 1234567890123L), // Character stored as Character (autoboxed)
        Arguments.of('c', null, 'c'), Arguments.of("", null, ""), Arguments.of(emptyMap, null, emptyMap), Arguments.of(list, null, list), Arguments.of(intArray, null, intArray), Arguments.of(epochDate, null, epochDate), // --- non‑null converter that would throw if used ------------------------
        // We pass a mock that throws on any method call; initMap must ignore it.
        Arguments.of("test", throwingMockConverter(), "test"));
    }

    /**
     * Creates a mock {@link Converter} that throws a {@link RuntimeException}
     * for any invoked method. {@code initMap} never calls any method on the
     * converter, so the exception should never be triggered.
     *
     * @return a throwing mock Converter
     */
    private static Converter throwingMockConverter() {
        Converter mock = mock(Converter.class);
        // Use a generic stub that throws for any method invocation.
        // Mockito's default answer throws an exception for unstubbed methods,
        // but to be explicit we set an Answer that always throws.
        doAnswer(invocation -> {
            throw new RuntimeException("Converter method should not be called");
        }).when(mock).convert(// assuming a 'convert' method exists
        any());
        return mock;
    }

    @Test
    void initMap_ShouldContainVKeyWithOriginalObject() {
        // GIVEN
        String original = "testValue";
        // WHEN
        Map<String, ?> result = MapConversions.initMap(original, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result instanceof CompactLinkedMap, "Implementation should be CompactLinkedMap");
        assertTrue(result.containsKey(MapConversions.V), "Map must contain the key defined by V constant");
        assertEquals(original, result.get(MapConversions.V), "Value stored under V key must match the original object");
    }

    @Test
    void initMap_ShouldHandleNullObject() {
        // GIVEN
        Object original = null;
        // WHEN
        Map<String, ?> result = MapConversions.initMap(original, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null even when input is null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertNull(result.get(MapConversions.V), "Value stored under V key should be null when input is null");
    }

    /**
     * Parameterized test that covers all scenarios described in the JSON test matrix.
     *
     * @param from          the source object to be stored in the map
     * @param converter     the Converter instance supplied to {@code initMap}
     * @param expectedValue the expected value stored under the {@code "_v"} key
     */
    @ParameterizedTest(name = "{index} ⇒ from={0}, converter={1}")
    @MethodSource("provideInitMapCases")
    void initMap_VariousInputs_ShouldStoreOriginalObject(Object from, Converter converter, Object expectedValue) {
        // WHEN
        Map<String, ?> result = MapConversions.initMap(from, converter);
        // THEN
        assertNotNull(result, "Resulting map must never be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result instanceof CompactLinkedMap, "Implementation should be CompactLinkedMap");
        assertTrue(result.containsKey(MapConversions.V), "Map must contain the key defined by V constant");
        assertEquals(expectedValue, result.get(MapConversions.V), "Value stored under V key must match the original object");
    }
}
