package com.cedarsoftware.util;

// import org.junit.*;
import java.util.stream.Stream;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CompactMap_get_Test {

    private static Object[] emptyMapKeys() {
        return new Object[] { "anyKey" };
    }

    private static Object[][] singleEntryData() {
        return new Object[][] { { "alpha", "A" }, { "beta", null } };
    }

    private static Object[][] arrayLinearSearchData() {
        return new Object[][] { { "c", "C-value" }, { "nonexistent", null } };
    }

    private static Object[][] arrayBinarySearchSortedData() {
        return new Object[][] { { "b", "B-value" }, { "d", null } };
    }

    private static Object[][] arrayBinarySearchReverseData() {
        return new Object[][] { { "c", "C-value" }, { "a", null } };
    }

    private static Object[][] mapBackingData() {
        return new Object[][] { { "k3", "V3" }, { "missing", null } };
    }

    private static Object[][] caseInsensitiveData() {
        return new Object[][] { { "TeSt", "value" }, { "OTHER", null } };
    }

    private static Object[][] nullKeyData() {
        return new Object[][] { { null, "nullValue" }, { "something", null } };
    }

    private static Object[][] malformedArrayData() {
        return new Object[][] { { "anyKey", ArrayIndexOutOfBoundsException.class }, { "k1", ArrayIndexOutOfBoundsException.class } };
    }

    static Stream<Arguments> emptyMapProvider() {
        return Stream.of(arguments("anyKey"));
    }

    static Stream<Arguments> singleEntryProvider() {
        return Stream.of(arguments("alpha", "A"), arguments("beta", null));
    }

    static Stream<Arguments> arrayLinearSearchProvider() {
        return Stream.of(arguments("c", "C-value"), arguments("nonexistent", null));
    }

    static Stream<Arguments> arrayBinarySearchSortedProvider() {
        return Stream.of(arguments("b", "B-value"), arguments("d", null));
    }

    static Stream<Arguments> arrayBinarySearchReverseProvider() {
        return Stream.of(arguments("c", "C-value"), arguments("a", null));
    }

    static Stream<Arguments> mapBackingProvider() {
        return Stream.of(arguments("k3", "V3"), arguments("missing", null));
    }

    static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(arguments("TeSt", "value"), arguments("OTHER", null));
    }

    static Stream<Arguments> nullKeyProvider() {
        return Stream.of(arguments(null, "nullValue"), arguments("something", null));
    }

    static Stream<Arguments> malformedArrayProvider() {
        return Stream.of(arguments("anyKey", ArrayIndexOutOfBoundsException.class), arguments("k1", ArrayIndexOutOfBoundsException.class));
    }

    @Test
    void getFromEmptyMapReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertNull(map.get("any"));
    }

    @Test
    void getFromSingleEntryMap() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("onlyKey", "onlyValue");
        assertEquals("onlyValue", map.get("onlyKey"));
        assertNull(map.get("missing"));
    }

    @Test
    void getFromCompactArray() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertNull(map.get("d"));
    }

    @Test
    void getFromSortedArrayUsesBinarySearch() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(// case‑insensitive
        false).sortedOrder().compactSize(5).build();
        map.put("Apple", "red");
        map.put("banana", "yellow");
        map.put("Cherry", "red");
        assertEquals("red", map.get("apple"));
        assertEquals("yellow", map.get("BANANA"));
        assertEquals("red", map.get("CHERRY"));
        assertNull(map.get("date"));
    }

    @Test
    void getFromReverseArrayUsesBinarySearch() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(true).reverseOrder().compactSize(5).build();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertEquals(Integer.valueOf(3), map.get("three"));
        assertNull(map.get("four"));
    }

//     @Test
//     void getFromBackingMapAfterExceedingCompactSize() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(3).mapType(LinkedHashMap.class).build();
//         map.put("k1", "v1");
//         map.put("k2", "v2");
//         map.put("k3", "v3");
//         // still in compact array (size == compactSize)
//         assertEquals("v2", map.get("k2"));
//         // fourth entry forces transition to backing map
//         map.put("k4", "v4");
//         assertEquals("v1", map.get("k1"));
//         assertEquals("v2", map.get("k2"));
//         assertEquals("v3", map.get("k3"));
//         assertEquals("v4", map.get("k4"));
//         assertNull(map.get("missing"));
//     }

    @Test
    void getWithNullKey() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put(null, "nullValue");
        map.put("present", "value");
        assertEquals("nullValue", map.get(null));
        assertEquals("value", map.get("present"));
        assertNull(map.get("absent"));
    }

    @ParameterizedTest(name = "[{index}] EmptyMap – key={0}")
    @MethodSource("emptyMapProvider")
    void emptyMap_getReturnsNull(Object key) {
        CompactMap<String, String> map = new CompactMap<>();
        for (Object k : emptyMapKeys()) {
            assertNull("Key: " + k, map.get(k));
        }
    }

    @ParameterizedTest(name = "[{index}] SingleEntry – key={0}")
    @MethodSource("singleEntryProvider")
    void singleEntry_getScenarios(String key, String expected) {
        CompactMap<String, String> map = new CompactMap<>();
        // the only entry
        map.put("alpha", "A");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

    @ParameterizedTest(name = "[{index}] ArrayLinearSearch – key={0}")
    @MethodSource("arrayLinearSearchProvider")
    void arrayLinearSearch_getScenarios(String key, String expected) {
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(5).build();
        map.put("a", "A-value");
        map.put("b", "B-value");
        map.put("c", "C-value");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

    @ParameterizedTest(name = "[{index}] ArrayBinarySearchSorted – key={0}")
    @MethodSource("arrayBinarySearchSortedProvider")
    void arrayBinarySearchSorted_getScenarios(String key, String expected) {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(true).sortedOrder().compactSize(5).build();
        map.put("a", "A-value");
        map.put("b", "B-value");
        map.put("c", "C-value");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

    @ParameterizedTest(name = "[{index}] ArrayBinarySearchReverse – key={0}")
    @MethodSource("arrayBinarySearchReverseProvider")
    void arrayBinarySearchReverse_getScenarios(String key, String expected) {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(true).reverseOrder().compactSize(5).build();
        map.put("a", "A-value");
        map.put("b", "B-value");
        map.put("c", "C-value");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

//     @ParameterizedTest(name = "[{index}] MapBacking – key={0}")
//     @MethodSource("mapBackingProvider")
//     void mapBacking_getScenarios(String key, String expected) {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(2).mapType(LinkedHashMap.class).build();
//         map.put("k1", "V1");
//         map.put("k2", "V2");
//         // forces transition to backing map
//         map.put("k3", "V3");
//         assertEquals(expected, map.get(key), "Key: " + key);
//     }

    @ParameterizedTest(name = "[{index}] CaseInsensitive – key={0}")
    @MethodSource("caseInsensitiveProvider")
    void caseInsensitive_getScenarios(String key, String expected) {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).compactSize(5).build();
        map.put("test", "value");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

    @ParameterizedTest(name = "[{index}] NullKey – key={0}")
    @MethodSource("nullKeyProvider")
    void nullKey_getScenarios(Object key, String expected) {
        CompactMap<String, String> map = new CompactMap<>();
        map.put(null, "nullValue");
        assertEquals(expected, map.get(key), "Key: " + key);
    }

    @ParameterizedTest(name = "[{index}] MalformedArrayOddLength – key={0}")
    @MethodSource("malformedArrayProvider")
    void malformedArray_oddLength_throwsException(Object key, Class<? extends Exception> expectedException) throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        // Force the internal `val` field to an odd‑length array via reflection.
        java.lang.reflect.Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        // length 3 (odd)
        Object[] malformed = new Object[] { "k1", "v1", "k2" };
        valField.set(map, malformed);
        Executable exec = () -> map.get(key);
        Exception thrown = assertThrows(expectedException, exec, "Expected exception of type " + expectedException.getName());
        assertTrue(expectedException.isInstance(thrown), "Expected " + expectedException.getName() + " but got " + thrown.getClass().getName());
    }
}