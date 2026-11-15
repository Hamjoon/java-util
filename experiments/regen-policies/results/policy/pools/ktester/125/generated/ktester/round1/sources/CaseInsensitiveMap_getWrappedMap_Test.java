package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_getWrappedMap_Test {

    /**
     * The map under test – created in each test method.
     */
    private CaseInsensitiveMap<String, String> ciMap;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        ciMap = null;
        // Reset the cache length to its default value in case another test altered it.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        ciMap = null;
        // Ensure default cache length to avoid side‑effects.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache length again to avoid side‑effects on other test classes.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /* -------------------------------------------------------------
     *  2. Custom backing map types – parameterized test.
     * ------------------------------------------------------------- */
    private static List<Map<String, String>> customBackingMaps() {
        return List.of(new TreeMap<>(), Collections.synchronizedMap(new HashMap<>()), new ConcurrentHashMap<>(), new IdentityHashMap<>(), Collections.unmodifiableMap(new TreeMap<>()), new AbstractMap<>() {

            private final Map<String, String> delegate = new HashMap<>();

            @Override
            public Set<Entry<String, String>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public String put(String key, String value) {
                return delegate.put(key, value);
            }
        });
    }

    /* -------------------------------------------------------------
     *  Helper to obtain the private `map` field via reflection.
     * ------------------------------------------------------------- */
    private static Map<?, ?> getInternalMap(CaseInsensitiveMap<?, ?> instance) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            return (Map<?, ?>) f.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void defaultConstructor_returnsLinkedHashMap() {
        // GIVEN
        ciMap = new CaseInsensitiveMap<>();
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped, "Wrapped map must not be null");
        assertTrue(wrapped instanceof LinkedHashMap, "Default constructor should use LinkedHashMap as backing map");
        // Mutate the wrapped map directly and verify the CaseInsensitiveMap reflects the change.
        wrapped.put("Key", "Value");
        assertEquals("Value", ciMap.get("key"), "Case‑insensitive lookup must see the entry added via the wrapped map");
    }

    @Test
    void constructorWithInitialCapacity_returnsLinkedHashMap() {
        // GIVEN
        ciMap = new CaseInsensitiveMap<>(32);
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof LinkedHashMap, "Constructor with initial capacity should also use LinkedHashMap");
        // Verify that the map behaves like a normal CaseInsensitiveMap.
        ciMap.put("Apple", "Red");
        assertEquals("Red", wrapped.get("Apple"));
    }

    @Test
    void constructorWithInitialCapacityAndLoadFactor_returnsLinkedHashMap() {
        // GIVEN
        ciMap = new CaseInsensitiveMap<>(16, 0.75f);
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof LinkedHashMap, "Constructor with capacity and load factor should use LinkedHashMap");
        // Direct put into wrapped map should be visible via the case‑insensitive view.
        wrapped.put("Banana", "Yellow");
        assertEquals("Yellow", ciMap.get("BANANA"));
    }

    @Test
    void constructorWithSourceMap_selectsAppropriateBackingMap() {
        // GIVEN – a TreeMap source (the API states TreeMap should be preserved)
        TreeMap<String, String> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.put("One", "1");
        source.put("Two", "2");
        // WHEN
        ciMap = new CaseInsensitiveMap<>(source);
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof TreeMap, "When source is a TreeMap, the backing map should also be a TreeMap");
        // The wrapped map must contain the same entries as the source.
        assertEquals(source.size(), wrapped.size());
        assertEquals("1", wrapped.get("ONE"));
        assertEquals("2", wrapped.get("two"));
        // Changes to the wrapped map must be reflected in the CaseInsensitiveMap.
        wrapped.put("Three", "3");
        assertEquals("3", ciMap.get("THREE"));
    }

    @Test
    void constructorWithSourceAndExplicitInstance_copiesIntoProvidedMap() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        source.put("Alpha", "A");
        source.put("Beta", "B");
        Map<String, String> explicitInstance = new LinkedHashMap<>();
        // WHEN
        ciMap = new CaseInsensitiveMap<>(source, explicitInstance);
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped);
        // The method copy returns the destination map, so we expect the same instance.
        assertSame(explicitInstance, wrapped, "When a destination map is supplied, getWrappedMap() should return that exact instance");
        // Verify that all entries from the source are present.
        assertEquals(2, wrapped.size());
        assertEquals("A", wrapped.get("Alpha"));
        assertEquals("B", wrapped.get("Beta"));
        // Mutating the wrapped map must affect the CaseInsensitiveMap view.
        wrapped.put("Gamma", "G");
        assertEquals("G", ciMap.get("GAMMA"));
    }

    @Test
    void getWrappedMap_isNeverNull_evenWhenMapIsEmpty() {
        // GIVEN
        ciMap = new CaseInsensitiveMap<>();
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped, "Wrapped map should never be null, even for an empty CaseInsensitiveMap");
        assertTrue(wrapped.isEmpty(), "Wrapped map should be empty when the CaseInsensitiveMap has no entries");
    }

    /* -------------------------------------------------------------
     *  1. Default backing map – verify the concrete type and identity.
     * ------------------------------------------------------------- */
    @Test
    void getWrappedMap_defaultBackingMap_isLinkedHashMap_andSameInstance() {
        // GIVEN
        ciMap = new CaseInsensitiveMap<>();
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertNotNull(wrapped, "Wrapped map must not be null");
        assertTrue(wrapped instanceof LinkedHashMap, "Default constructor should use LinkedHashMap as backing map");
        // The wrapped map should be the exact instance stored in the private field.
        assertSame(getInternalMap(ciMap), wrapped, "Returned map must be the same instance as the internal field");
    }

    @ParameterizedTest
    @MethodSource("customBackingMaps")
    void getWrappedMap_customBackingMap_isExactInstance(Map<String, String> backing) {
        // GIVEN a custom map instance
        ciMap = new CaseInsensitiveMap<>(backing);
        // WHEN
        Map<String, String> wrapped = ciMap.getWrappedMap();
        // THEN
        assertSame(backing, wrapped, "getWrappedMap() must return the exact map instance supplied to the constructor");
    }

    /* -------------------------------------------------------------
     *  3. Live reference mutation – ensure changes are visible.
     * ------------------------------------------------------------- */
    @Test
    void getWrappedMap_liveReferenceMutation_isReflectedInCiMap() {
        ciMap = new CaseInsensitiveMap<>();
        Map<String, String> wrapped = ciMap.getWrappedMap();
        wrapped.put("Hello", "World");
        assertEquals("World", ciMap.get("hello"), "Mutation via wrapped map must be visible through CaseInsensitiveMap");
    }

    /* -------------------------------------------------------------
     *  4. Null backing map – use reflection to inject null.
     * ------------------------------------------------------------- */
    @Test
    void getWrappedMap_nullBackingMap_returnsNull() throws Exception {
        // GIVEN a normal instance
        ciMap = new CaseInsensitiveMap<>();
        // Use reflection to set the private field `map` to null.
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(ciMap, null);
        // WHEN
        Map<String, String> result = ciMap.getWrappedMap();
        // THEN
        assertNull(result, "When internal map is null, getWrappedMap() should return null");
    }

    /* -------------------------------------------------------------
     *  5. Reference stability after mutating operations.
     * ------------------------------------------------------------- */
    @Test
    void getWrappedMap_referenceStability_afterMutations() {
        // Use a LinkedHashMap as the backing map for clarity.
        Map<String, String> backing = new LinkedHashMap<>();
        ciMap = new CaseInsensitiveMap<>(backing);
        Map<String, String> original = ciMap.getWrappedMap();
        // Perform a series of mutating operations on the CaseInsensitiveMap.
        ciMap.put("A", "1");
        assertSame(original, ciMap.getWrappedMap(), "Reference must stay the same after put");
        ciMap.putIfAbsent("B", "2");
        assertSame(original, ciMap.getWrappedMap(), "Reference must stay the same after putIfAbsent");
        ciMap.replace("A", "10");
        assertSame(original, ciMap.getWrappedMap(), "Reference must stay the same after replace");
        ciMap.remove("B");
        assertSame(original, ciMap.getWrappedMap(), "Reference must stay the same after remove");
        ciMap.clear();
        assertSame(original, ciMap.getWrappedMap(), "Reference must stay the same after clear");
    }

    /* -------------------------------------------------------------
     *  6. Anonymous Map subclass – ensure exact instance is returned.
     * ------------------------------------------------------------- */
    @Test
    void getWrappedMap_anonymousMapImplementation_isExactInstance() {
        // Anonymous subclass overriding size()
        Map<String, String> anonMap = new HashMap<String, String>() {

            @Override
            public int size() {
                // arbitrary constant
                return 42;
            }
        };
        anonMap.put("X", "Y");
        ciMap = new CaseInsensitiveMap<>(anonMap);
        Map<String, String> wrapped = ciMap.getWrappedMap();
        assertSame(anonMap, wrapped, "Anonymous map instance must be returned unchanged");
        // Verify overridden behavior is preserved.
        assertEquals(42, wrapped.size(), "Overridden size() should be observed");
    }
}
