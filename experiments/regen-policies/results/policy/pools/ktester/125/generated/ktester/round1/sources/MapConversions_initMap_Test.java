package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class MapConversions_initMap_Test {

    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static final class DummyObject {

        private final String type;

        private final int id;

        DummyObject(String type, int id) {
            this.type = type;
            this.id = id;
        }

        // equals / hashCode are not required for these tests, but they help
        // when debugging failures.
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DummyObject))
                return false;
            DummyObject that = (DummyObject) o;
            return id == that.id && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public String toString() {
            return "DummyObject{type='" + type + "', id=" + id + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is not used by initMap, but we provide a concrete instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for this test)
        converter = null;
    }

    /**
     * Supplies all test cases described in the JSON specification.
     *
     * <p>Each {@link Arguments} consists of:
     * <ul>
     *   <li>The {@code from} value to be stored.</li>
     *   <li>The {@code Converter} to pass (real instance or mock).</li>
     * </ul>
     */
    private static Stream<Arguments> provideInitMapCases() {
        // A mock converter for the last test case (ensures the method ignores it)
        Converter mockConverter = Mockito.mock(Converter.class);
        // Simple scalar values
        Object nullValue = null;
        String stringValue = "sample text";
        Integer intValue = 12345;
        String isoDateString = "2023-01-01T12:34:56Z";
        Boolean boolValue = Boolean.TRUE;
        Double doubleValue = 3.1415926535;
        Double nanValue = Double.NaN;
        String numericString = "123.45";
        // Complex objects
        int[] intArray = new int[] { 1, 2, 3 };
        List<String> stringList = Arrays.asList("alpha", "beta", "gamma");
        Map<String, String> simpleMap = Collections.singletonMap("sampleKey", "sampleValue");
        DummyObject dummy = new DummyObject("DummyObject", 42);
        Date epochDate = new Date(0L);
        AtomicInteger atomicInt = new AtomicInteger(7);
        return Stream.of(// 1. null source
        Arguments.of(nullValue, converter), // 2. simple String
        Arguments.of(stringValue, converter), // 3. Integer
        Arguments.of(intValue, converter), // 4. date‑time String (kept as raw String)
        Arguments.of(isoDateString, converter), // 5. Boolean
        Arguments.of(boolValue, converter), // 6. int array
        Arguments.of(intArray, converter), // 7. List
        Arguments.of(stringList, converter), // 8. Map
        Arguments.of(simpleMap, converter), // 9. user‑defined object
        Arguments.of(dummy, converter), // 10. Double
        Arguments.of(doubleValue, converter), // 11. Double.NaN
        Arguments.of(nanValue, converter), // 12. numeric String (no conversion performed)
        Arguments.of(numericString, converter), // 13. java.util.Date (epoch)
        Arguments.of(epochDate, converter), // 14. AtomicInteger
        Arguments.of(atomicInt, converter), // 15. non‑null Converter (mock) – should be ignored
        Arguments.of("any value", mockConverter));
    }

    @Test
    void initMapShouldCreateMapWithValueKey() {
        // GIVEN
        String source = "testValue";
        // WHEN
        Map<String, ?> result = MapConversions.initMap(source, converter);
        // THEN
        Assertions.assertNotNull(result, "Resulting map must not be null");
        Assertions.assertTrue(result instanceof java.util.Map, "Result should be a Map");
        Assertions.assertEquals(1, result.size(), "Map should contain exactly one entry");
        Assertions.assertTrue(result.containsKey(MapConversions.V), "Map must contain the key \"_v\"");
        Assertions.assertEquals(source, result.get(MapConversions.V), "Map value for key \"_v\" must be the source object");
    }

    @Test
    void initMapShouldHandleNullSource() {
        // GIVEN
        Object source = null;
        // WHEN
        Map<String, ?> result = MapConversions.initMap(source, converter);
        // THEN
        Assertions.assertNotNull(result, "Resulting map must not be null even when source is null");
        Assertions.assertEquals(1, result.size(), "Map should still contain exactly one entry");
        Assertions.assertTrue(result.containsKey(MapConversions.V), "Map must contain the key \"_v\"");
        Assertions.assertNull(result.get(MapConversions.V), "Map value for key \"_v\" should be null when source is null");
    }

    @ParameterizedTest(name = "{index}: initMap with from={0}")
    @MethodSource("provideInitMapCases")
    void initMapShouldStoreProvidedObject(Object from, Converter conv) {
        // WHEN
        Map<String, ?> result = MapConversions.initMap(from, conv);
        // THEN
        Assertions.assertNotNull(result, "Resulting map must not be null");
        Assertions.assertEquals(1, result.size(), "Map must contain exactly one entry");
        Assertions.assertTrue(result.containsKey(MapConversions.V), "Map must contain the special key \"_v\"");
        if (from == null) {
            Assertions.assertNull(result.get(MapConversions.V), "Value for key \"_v\" should be null when source is null");
        } else {
            // Reference equality – initMap must not copy or transform the object.
            Assertions.assertSame(from, result.get(MapConversions.V), "Map should store the original object instance");
        }
    }
}
