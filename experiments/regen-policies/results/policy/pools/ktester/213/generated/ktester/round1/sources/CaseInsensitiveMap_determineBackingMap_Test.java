package com.cedarsoftware.util;

import java.util.function.Function;
import java.util.AbstractMap.SimpleEntry;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_determineBackingMap_Test {

    /**
     * Keep a copy of the original registry so that we can restore it after the suite runs.
     */
    private static List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> originalRegistry;

    /**
     * Simple subclass that exposes the protected method for testing.
     */
    private static class TestableCaseInsensitiveMap<K, V> extends CaseInsensitiveMap<K, V> {

        Map<K, V> callDetermineBackingMap(Map<K, V> source) {
            return determineBackingMap(source);
        }
    }

    /**
     * Custom subclass used in one of the test scenarios.
     */
    private static class MyHashMap<K, V> extends HashMap<K, V> {

        private static final long serialVersionUID = 1L;
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Capture the original registry (it may be null the first time the class is loaded)
        originalRegistry = Collections.emptyList();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean registry before each test.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the registry to a clean state after each test.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore whatever was present before the test suite started.
        CaseInsensitiveMap.replaceRegistry(originalRegistry);
    }

    @Test
    public void determineBackingMap_throwsOnIdentityHashMap() {
        Map<String, String> source = new IdentityHashMap<>();
        source.put("Key", "Value");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("IdentityHashMap should not be accepted as a source map");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void determineBackingMap_usesRegisteredBackingMap() {
        // Register TreeMap as the backing map for any TreeMap source.
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(TreeMap.class, (Function<Integer, Map<?, ?>>) size -> new TreeMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        // Source is a TreeMap with a couple of entries.
        Map<String, String> source = new TreeMap<>();
        source.put("Alpha", "A");
        source.put("Beta", "B");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        // The returned map must be a TreeMap (the one we registered).
        assertTrue(backing instanceof TreeMap);
        // It must contain the same entries as the source (a shallow copy).
        assertEquals(source, backing);
        // Ensure it is not the same instance.
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_defaultsToLinkedHashMapWhenNoMatch() {
        // Ensure the registry is empty – no type will match.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        source.put("Three", 3);
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        // Default backing map is LinkedHashMap.
        assertTrue(backing instanceof LinkedHashMap);
        // Verify that the contents are identical to the source.
        assertEquals(source, backing);
        // Verify a new instance was created.
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_registryWithHashMapFactory_returnsConcurrentHashMap() {
        // Registry entry: HashMap -> ConcurrentHashMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, Integer> source = new HashMap<String, Integer>() {

            {
                put("x", 10);
                put("y", 20);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentHashMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_registryWithSortedMapFactory_returnsConcurrentSkipListMap() {
        // Registry entry: SortedMap -> ConcurrentSkipListMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(SortedMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentSkipListMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, String> source = new TreeMap<String, String>() {

            {
                put("a", "alpha");
                put("b", "beta");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentSkipListMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_emptySource_returnsEmptyLinkedHashMap() {
        Map<String, Integer> source = new HashMap<>();
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof LinkedHashMap);
        assertEquals(0, backing.size());
    }

    @Test
    public void determineBackingMap_nullSource_throwsNullPointerException() {
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(null);
            fail("Passing null as source should throw NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void determineBackingMap_customSubclassRecognisedAsHashMap() {
        // Registry entry: HashMap -> ConcurrentHashMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        MyHashMap<String, Integer> source = new MyHashMap<String, Integer>() {

            {
                put("x", 1);
                put("y", 2);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentHashMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_orderHashMapFirst_selectsHashMapEntry() {
        // Registry order: HashMap first, then Map
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentHashMap<>()));
        registry.add(new SimpleEntry<>(Map.class, (Function<Integer, Map<?, ?>>) size -> new LinkedHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, String> source = new HashMap<String, String>() {

            {
                put("a", "A");
                put("b", "B");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentHashMap);
        assertEquals(source, backing);
    }

    @Test
    public void determineBackingMap_orderSortedMapFirst_selectsSortedMapEntry() {
        // Registry order: SortedMap first, then Map
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(SortedMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentSkipListMap<>()));
        registry.add(new SimpleEntry<>(Map.class, (Function<Integer, Map<?, ?>>) size -> new LinkedHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, String> source = new TreeMap<String, String>() {

            {
                put("c", "C");
                put("d", "D");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentSkipListMap);
        assertEquals(source, backing);
    }

    @Test
    public void determineBackingMap_concurrentSkipListMapSource_matchesConcurrentNavigableMap() {
        // Registry entry: ConcurrentNavigableMap -> ConcurrentSkipListMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(ConcurrentNavigableMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentSkipListMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        ConcurrentSkipListMap<String, Integer> source = new ConcurrentSkipListMap<String, Integer>() {

            {
                put("k1", 100);
                put("k2", 200);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof ConcurrentSkipListMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_linkedHashMapSource_copiesIntoNewLinkedHashMap() {
        // Registry entry: LinkedHashMap -> LinkedHashMap (explicit)
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(LinkedHashMap.class, (Function<Integer, Map<?, ?>>) size -> new LinkedHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        LinkedHashMap<String, String> source = new LinkedHashMap<String, String>() {

            {
                put("first", "1");
                put("second", "2");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof LinkedHashMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_weakHashMapWithSpecificEntry_returnsWeakHashMap() {
        // Registry: WeakHashMap first, then Map
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(WeakHashMap.class, (Function<Integer, Map<?, ?>>) size -> new WeakHashMap<>()));
        registry.add(new SimpleEntry<>(Map.class, (Function<Integer, Map<?, ?>>) size -> new LinkedHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        WeakHashMap<String, Integer> source = new WeakHashMap<String, Integer>() {

            {
                put("wk1", 10);
                put("wk2", 20);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.callDetermineBackingMap(source);
        assertTrue(backing instanceof WeakHashMap);
        assertEquals(source, backing);
        assertNotSame(source, backing);
    }

    @Test
    public void determineBackingMap_nullRegistry_throwsNullPointerException() throws Exception {
        // Use reflection to set the private static AtomicReference to null.
        Field registryField = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        registryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>> ref = (AtomicReference<List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>>) registryField.get(null);
        ref.set(null);
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> source = new HashMap<String, Integer>() {

            {
                put("a", 1);
                put("b", 2);
            }
        };
        try {
            map.callDetermineBackingMap(source);
            fail("When mapRegistry is null, a NullPointerException should be thrown");
        } catch (NullPointerException ignored) {
        }
        // Restore a non‑null registry for subsequent tests
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
    }

    @Test
    public void determineBackingMap_registryWithNullEntry_throwsNullPointerException() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        // explicit null entry
        registry.add(null);
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, Integer> source = new HashMap<String, Integer>() {

            {
                put("x", 10);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("Null entry in registry should cause NullPointerException during iteration");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void determineBackingMap_registryNullKey_throwsNullPointerException() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(null, (Function<Integer, Map<?, ?>>) size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, Integer> source = new HashMap<String, Integer>() {

            {
                put("k", 1);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("Null key (Class) in registry entry should cause NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void determineBackingMap_registryNullFactory_throwsNullPointerException() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(Map.class, null));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, Integer> source = new Hashtable<String, Integer>() {

            {
                put("t", 5);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("Null factory in registry entry should cause NullPointerException when invoked");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void determineBackingMap_factoryThrowsException_propagatesException() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(SortedMap.class, (Function<Integer, Map<?, ?>>) size -> {
            throw new IllegalStateException("factory failure");
        }));
        CaseInsensitiveMap.replaceRegistry(registry);
        TreeMap<String, String> source = new TreeMap<String, String>() {

            {
                put("a", "alpha");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("Exception thrown by factory should propagate");
        } catch (IllegalStateException ex) {
            assertEquals("factory failure", ex.getMessage());
        }
    }

    @Test
    public void determineBackingMap_factoryReturnsNull_throwsNullPointerException() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(LinkedHashMap.class, (Function<Integer, Map<?, ?>>) size -> null));
        CaseInsensitiveMap.replaceRegistry(registry);
        LinkedHashMap<String, Integer> source = new LinkedHashMap<String, Integer>() {

            {
                put("one", 1);
                put("two", 2);
            }
        };
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        try {
            map.callDetermineBackingMap(source);
            fail("Factory returning null should cause NullPointerException inside copy()");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void determineBackingMap_registryWithMapFirst_thenHashMap_selectsMapEntry() {
        // Registry: Map first (LinkedHashMap), then HashMap (ConcurrentHashMap)
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(new SimpleEntry<>(Map.class, (Function<Integer, Map<?, ?>>) size -> new LinkedHashMap<>()));
        registry.add(new SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(registry);
        Map<String, String> source = new HashMap<String, String>() {

            {
                put("p", "1");
                put("q", "2");
            }
        };
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.callDetermineBackingMap(source);
        // Even though HashMap also matches, the first entry (Map) should be used.
        assertTrue(backing instanceof LinkedHashMap);
        assertEquals(source, backing);
    }
}
