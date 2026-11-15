package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class CaseInsensitiveMap_getWrappedMap_Test {

    /**
     * Supplies the map implementations required for the parameterized test.
     */
    static Stream<Class<? extends Map>> underlyingMapProvider() {
        return Stream.of(HashMap.class, ConcurrentHashMap.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_DefaultConstructor_ReturnsLinkedHashMap() {
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>();
        // The wrapped map should be a LinkedHashMap and initially empty.
        Map<String, Integer> wrapped = ciMap.getWrappedMap();
        assertTrue(wrapped instanceof LinkedHashMap, "Wrapped map should be a LinkedHashMap");
        assertTrue(wrapped.isEmpty(), "Wrapped map should be empty initially");
        // Mutate through the public API and verify the wrapped map reflects the change.
        ciMap.put("One", 1);
        assertEquals(1, wrapped.size());
        assertTrue(wrapped.containsKey("One"));
        assertEquals(1, wrapped.get("One"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_InitialCapacityConstructor_ReturnsLinkedHashMap() {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(10);
        Map<String, String> wrapped = ciMap.getWrappedMap();
        assertTrue(wrapped instanceof LinkedHashMap, "Wrapped map should be a LinkedHashMap");
        assertTrue(wrapped.isEmpty());
        ciMap.put("Key", "Value");
        assertEquals("Value", wrapped.get("Key"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_SourceAndMapInstanceConstructor_CopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("A", 10);
        source.put("B", 20);
        // Use a LinkedHashMap as the concrete map instance for the copy.
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>(source, new LinkedHashMap<>());
        Map<String, Integer> wrapped = ciMap.getWrappedMap();
        assertTrue(wrapped instanceof LinkedHashMap, "Wrapped map should be a LinkedHashMap");
        assertEquals(2, wrapped.size());
        assertEquals(10, wrapped.get("A"));
        assertEquals(20, wrapped.get("B"));
        // Ensure modifications via the outer map are reflected inside the wrapped map.
        ciMap.put("C", 30);
        assertEquals(30, wrapped.get("C"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_MapConstructor_SelectsCorrectImplementation() {
        // Prepare a TreeMap source – the constructor should create a TreeMap internally.
        TreeMap<String, String> treeSource = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        treeSource.put("alpha", "first");
        treeSource.put("Beta", "second");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(treeSource);
        Map<String, String> wrapped = ciMap.getWrappedMap();
        assertTrue(wrapped instanceof TreeMap, "Wrapped map should be a TreeMap");
        assertEquals(2, wrapped.size());
        // TreeMap maintains natural ordering; verify ordering via iterator.
        Iterator<String> it = wrapped.keySet().iterator();
        assertEquals("alpha", it.next());
        assertEquals("Beta", it.next());
        // Verify case‑insensitive behavior through the public API.
        assertEquals("first", ciMap.get("ALPHA"));
        assertEquals("second", ciMap.get("beta"));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "Underlying map type: {0}")
    @MethodSource("underlyingMapProvider")
    void getWrappedMap_UnderlyingMapProvided_ReturnsSameInstance(Class<? extends Map> mapClass) throws Exception {
        // Create an instance of the requested map type.
        Map<String, Integer> underlying = mapClass.getDeclaredConstructor().newInstance();
        // Construct the CaseInsensitiveMap with the supplied underlying map.
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>(underlying);
        // The wrapped map must be the exact same instance we passed in.
        Map<String, Integer> wrapped = ciMap.getWrappedMap();
        assertSame(underlying, wrapped, "getWrappedMap() should return the same instance that was supplied to the constructor");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_PutReflectsInWrappedMap() {
        Map<String, Integer> underlying = new LinkedHashMap<>();
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>(underlying);
        ciMap.put("KeyOne", 123);
        Map<String, Integer> wrapped = ciMap.getWrappedMap();
        assertEquals(1, wrapped.size());
        assertEquals(123, wrapped.get("KeyOne"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_DirectModificationReflectsInCaseInsensitiveMap() {
        Map<String, String> underlying = new LinkedHashMap<>();
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(underlying);
        // Add entry directly to the wrapped map.
        Map<String, String> wrapped = ciMap.getWrappedMap();
        wrapped.put("external", "value");
        // The CaseInsensitiveMap view must see the entry.
        assertTrue(ciMap.containsKey("external"));
        assertEquals("value", ciMap.get("external"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_NullUnderlyingMap_ThrowsNullPointerException() {
        // The constructor that accepts a Map should reject null.
        assertThrows(NullPointerException.class, () -> {
            new CaseInsensitiveMap<>(null);
        });
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getWrappedMap_NonMapUnderlying_ThrowsClassCastException() throws Exception {
        // Create a normal instance using the default constructor.
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        // Use reflection to replace the private 'map' field with a plain Object.
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // deliberately wrong type
        mapField.set(ciMap, new Object());
        // Accessing getWrappedMap() now attempts to return the field as a Map,
        // which should trigger a ClassCastException.
        assertThrows(ClassCastException.class, ciMap::getWrappedMap);
    }
}
