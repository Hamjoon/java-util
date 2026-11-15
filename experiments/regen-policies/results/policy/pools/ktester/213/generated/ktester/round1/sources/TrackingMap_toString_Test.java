package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_toString_Test {

    // --------------------------------------------------------------------
    @Mock
    private Map<String, String> // a Mockito mock for delegation test
    mockMap;

    private TrackingMap<String, String> trackingMapWithMock;

    // for manual mock cleanup (optional)
    private AutoCloseable mocks;

    /**
     * Simple custom key object used in the non‑String key test.
     */
    private static final class FooObj {

        @Override
        public String toString() {
            return "FooObj";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required when we use @Mock without constructor injection)
        mocks = MockitoAnnotations.openMocks(this);
        // Configure the mock to return a deterministic string from toString()
        when(mockMap.toString()).thenReturn("MockedMapString");
        // Create a TrackingMap that wraps the mock.
        trackingMapWithMock = new TrackingMap<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        if (mocks != null) {
            mocks.close();
        }
        // Help GC – not strictly required but keeps the test tidy.
        trackingMapWithMock = null;
        mockMap = null;
    }

    // -------------------- Group: basic map representations --------------------
    private static Stream<Arguments> basicMapRepresentationsProvider() {
        return Stream.of(Arguments.of(new LinkedHashMap<String, String>(), "{}", "Empty internal map should produce the standard empty map string."), Arguments.of(mapOf(entry("key", "value")), "{key=value}", "Single entry map should return a string with that entry formatted as key=value."), Arguments.of(mapOf(entry("a", "1"), entry("b", "2")), "{a=1, b=2}", "Map with two entries (using LinkedHashMap to preserve insertion order) should return a string containing both entries in the order they were added."), Arguments.of(mapOf(entry(null, "val")), "{null=val}", "Map containing a null key and a non‑null value should render the null key as the literal string \"null\"."), Arguments.of(mapOf(entry("key", null)), "{key=null}", "Map containing a non‑null key and a null value should render the null value as the literal string \"null\"."), Arguments.of(mapOf(entry(null, null)), "{null=null}", "Map containing both a null key and a null value should render both as the literal string \"null\"."));
    }

    // -------------------- Group: after modifications --------------------
    private static Stream<Arguments> afterModificationsProvider() {
        // Case 1: put then remove
        Map<String, String> map1 = new HashMap<>();
        map1.put("temp", "value");
        map1.remove("temp");
        // Case 2: putAll with three entries preserving order via LinkedHashMap
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("c", 3);
        return Stream.of(Arguments.of(map1, "{}", "After putting an entry and then removing it, the internal map is empty; toString() should return the empty map representation."), Arguments.of(map2, "{a=1, b=2, c=3}", "After calling putAll() with a LinkedHashMap containing three entries, toString() should reflect all entries in insertion order."));
    }

    // -------------------- Group: non‑String keys and values --------------------
    private static Stream<Arguments> nonStringKeyValueProvider() {
        // Integer key, Boolean value
        Map<Integer, Boolean> map1 = new HashMap<>();
        map1.put(42, true);
        // Custom object key with List value
        FooObj foo = new FooObj();
        List<String> list = Arrays.asList("x", "y");
        Map<FooObj, List<String>> map2 = new HashMap<>();
        map2.put(foo, list);
        return Stream.of(Arguments.of(map1, "{42=true}", "When the map stores an Integer key and a Boolean value, toString() should use their default string representations."), Arguments.of(map2, "{FooObj=[x, y]}", "A custom object key whose toString() returns \"FooObj\" and a List value should be rendered using the objects' toString() results."));
    }

    // --------------------------------------------------------------------
    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    // --------------------------------------------------------------------
    @Test
    void toString_delegatesToInternalMap() {
        // When we call toString() on the TrackingMap, it should delegate to the wrapped map.
        String result = trackingMapWithMock.toString();
        // Verify that the underlying map's toString() was indeed invoked.
        verify(mockMap, times(1)).toString();
        // The result must be exactly what the mock map returned.
        assertEquals("MockedMapString", result, "TrackingMap.toString() should return the wrapped map's toString() result");
    }

    @Test
    void toString_reflectsContentsOfRealMap() {
        // Use a real LinkedHashMap to guarantee iteration order for predictable toString().
        Map<String, Integer> backing = new LinkedHashMap<>();
        backing.put("apple", 1);
        backing.put("banana", 2);
        backing.put("cherry", 3);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backing);
        // The toString of TrackingMap must be identical to the backing map's toString().
        assertEquals(backing.toString(), trackingMap.toString(), "TrackingMap.toString() should mirror the backing map's toString()");
    }

    @Test
    void toString_updatesAfterMutations() {
        Map<String, String> backing = new HashMap<>();
        TrackingMap<String, String> trackingMap = new TrackingMap<>(backing);
        // Initially empty
        assertEquals("{}", trackingMap.toString(), "Empty map should produce '{}' string representation");
        // Add entries
        backing.put("k1", "v1");
        backing.put("k2", "v2");
        // The TrackingMap should now reflect the new state.
        assertEquals(backing.toString(), trackingMap.toString(), "After mutation, TrackingMap.toString() must reflect the current state of the backing map");
    }

    @Test
    void toString_afterExpungeUnusedReflectsRemainingEntries() {
        // This test also shows that toString works after using TrackingMap specific behaviour.
        Map<String, String> backing = new LinkedHashMap<>();
        TrackingMap<String, String> trackingMap = new TrackingMap<>(backing);
        // Populate map
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "bravo");
        trackingMap.put("c", "charlie");
        // Access only "a" and "b"
        trackingMap.get("a");
        trackingMap.get("b");
        // Remove unused entries
        trackingMap.expungeUnused();
        // Expected map now contains only "a" and "b"
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "alpha");
        expected.put("b", "bravo");
        assertEquals(expected.toString(), trackingMap.toString(), "toString() should reflect the map after expungeUnused()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicMapRepresentationsProvider")
    void toString_basicRepresentations(Map<String, String> backing, String expected, String description) {
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertEquals(expected, tm.toString(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("afterModificationsProvider")
    void toString_afterModifications(Map<?, ?> backing, String expected, String description) {
        @SuppressWarnings("unchecked")
        TrackingMap<Object, Object> tm = new TrackingMap<>((Map<Object, Object>) backing);
        assertEquals(expected, tm.toString(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonStringKeyValueProvider")
    void toString_nonStringKeyValue(Map<?, ?> backing, String expected, String description) {
        @SuppressWarnings("unchecked")
        TrackingMap<Object, Object> tm = new TrackingMap<>((Map<Object, Object>) backing);
        assertEquals(expected, tm.toString(), description);
    }

    // -------------------- Group: different underlying map implementations --------------------
    @Test
    void toString_treeMapOrdering() {
        // TreeMap sorts keys naturally.
        Map<Integer, String> tree = new TreeMap<>();
        tree.put(3, "c");
        tree.put(1, "a");
        tree.put(2, "b");
        TrackingMap<Integer, String> tm = new TrackingMap<>(tree);
        assertEquals("{1=a, 2=b, 3=c}", tm.toString(), "When the wrapped map is a TreeMap, entries are ordered by the natural ordering of keys; toString() must reflect that sorted order.");
    }

    // -------------------- Group: larger map --------------------
    @Test
    void toString_largerMap() {
        Map<String, String> backing = new LinkedHashMap<>();
        backing.put("k1", "v1");
        backing.put("k2", "v2");
        backing.put("k3", "v3");
        backing.put("k4", "v4");
        backing.put("k5", "v5");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertEquals("{k1=v1, k2=v2, k3=v3, k4=v4, k5=v5}", tm.toString(), "A map with five entries verifies that toString() correctly concatenates multiple key‑value pairs with proper separators.");
    }

    // -------------------- Group: keys/values with special characters --------------------
    @Test
    void toString_specialCharacters() {
        Map<String, String> backing = new LinkedHashMap<>();
        backing.put("key, with, commas", "val{brace}");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertEquals("{key, with, commas=val{brace}}", tm.toString(), "Keys or values containing commas, spaces, or braces are rendered verbatim; no escaping is performed by Map.toString().");
    }

    // -------------------- Group: exception scenarios --------------------
    @Test
    void toString_nullUnderlyingMap_throwsNPE() {
        // Directly construct with a null map – the constructor should accept it,
        // but any operation (including toString) must throw NPE because internalMap is null.
        TrackingMap<String, String> tm = new TrackingMap<>(null);
        assertThrows(NullPointerException.class, tm::toString, "Constructing TrackingMap with a null underlying map should cause toString() to throw NullPointerException.");
    }

    @Test
    void toString_underlyingMapThrowsIllegalStateException_propagates() {
        // Custom map that deliberately throws from toString().
        Map<String, String> badMap = new AbstractMap<String, String>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public String toString() {
                throw new IllegalStateException("Bad map toString");
            }
        };
        TrackingMap<String, String> tm = new TrackingMap<>(badMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, tm::toString, "toString() should propagate IllegalStateException thrown by the wrapped map.");
        assertEquals("Bad map toString", ex.getMessage());
    }
}
