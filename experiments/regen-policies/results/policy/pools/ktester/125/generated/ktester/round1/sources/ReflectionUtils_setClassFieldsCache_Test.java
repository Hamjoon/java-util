package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setClassFieldsCache_Test {

    /**
     * Simple class used for reflection tests – it has exactly two declared fields.
     */
    static class TestClass {

        private int field1;

        protected String field2;
    }

    /**
     * Custom Map subclass used in one test case.
     */
    static class MyMap extends HashMap<Object, Collection<Field>> {
        // No extra behaviour – just a distinct concrete class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test – an empty HashMap works because
        // ReflectionUtils will lazily repopulate it when needed.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Ensure a clean state before each test.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache to a neutral state so that one test cannot affect another.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Reset to a neutral empty map after each test.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    /**
     * Helper to read the private static {@code FIELDS_CACHE} field via reflection.
     */
    private static Map<?, ?> getFieldsCache() throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        f.setAccessible(true);
        return (Map<?, ?>) f.get(null);
    }

    static Stream<Arguments> basicCacheProvider() {
        return Stream.of(Arguments.of("nonEmptyMap", new HashMap<Object, Collection<Field>>() {

            {
                put("key", Collections.emptyList());
            }
        }), Arguments.of("emptyMap", Collections.emptyMap()), Arguments.of("nullMap", null));
    }

    /**
     * Verify that {@code setClassFieldsCache} accepts an arbitrary map without
     * throwing and that subsequent reflective operations continue to work.
     */
    @Test
    void testSetClassFieldsCacheWithArbitraryMap() {
        // GIVEN – a map that contains an unrelated entry.
        Map<Object, Collection<Field>> bogusCache = new HashMap<>();
        bogusCache.put("bogusKey", Collections.emptyList());
        // WHEN – the cache is replaced with the bogus map.
        ReflectionUtils.setClassFieldsCache(bogusCache);
        // THEN – reflective utilities must still return the correct fields for a normal class.
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(TestClass.class);
        assertNotNull(fields, "Returned collection should never be null");
        assertEquals(2, fields.size(), "TestClass should expose exactly two declared fields");
        Set<String> names = new HashSet<>();
        fields.forEach(f -> names.add(f.getName()));
        assertTrue(names.containsAll(Arrays.asList("field1", "field2")), "Both field1 and field2 must be present");
    }

    /**
     * Verify that providing an empty map works and does not interfere with normal operation.
     */
    @Test
    void testSetClassFieldsCacheWithEmptyMap() {
        // GIVEN – an empty map.
        Map<Object, Collection<Field>> emptyCache = new HashMap<>();
        // WHEN – we replace the internal cache with the empty map.
        ReflectionUtils.setClassFieldsCache(emptyCache);
        // THEN – calling a reflective method should still populate and return the expected result.
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(TestClass.class);
        assertNotNull(fields);
        assertEquals(2, fields.size(), "Even with an empty cache we should retrieve the two fields");
    }

    /**
     * Ensure that after resetting the cache to a fresh map, the utility still behaves
     * identically – this indirectly proves that the setter does not leave the class
     * in a corrupted state.
     */
    @Test
    void testCacheResetBetweenTests() {
        // The @BeforeEach method already reset the cache to an empty HashMap.
        // Simply invoke the reflective method and verify the result.
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(TestClass.class);
        assertEquals(2, fields.size(), "Cache reset should not affect field discovery");
    }

    @ParameterizedTest(name = "{index}: setClassFieldsCache with {0}")
    @MethodSource("basicCacheProvider")
    void testSetClassFieldsCache_basicReferenceEquality(String description, Map<Object, Collection<Field>> cache) throws Exception {
        // WHEN
        ReflectionUtils.setClassFieldsCache(cache);
        // THEN – the internal cache reference must be exactly the same object (or null)
        Map<?, ?> internal = getFieldsCache();
        assertSame(cache, internal, "FIELDS_CACHE should reference the exact map instance supplied");
    }

    @Test
    void testSetClassFieldsCache_unmodifiableMap() throws Exception {
        Map<Object, Collection<Field>> unmod = Collections.unmodifiableMap(new HashMap<Object, Collection<Field>>() {

            {
                put("key", Collections.emptyList());
            }
        });
        ReflectionUtils.setClassFieldsCache(unmod);
        assertSame(unmod, getFieldsCache(), "Unmodifiable map should be stored unchanged");
    }

    @Test
    void testSetClassFieldsCache_mapWithNullKey() throws Exception {
        Map<Object, Collection<Field>> map = new HashMap<Object, Collection<Field>>() {

            {
                put(null, Collections.emptyList());
            }
        };
        ReflectionUtils.setClassFieldsCache(map);
        assertSame(map, getFieldsCache(), "Map with null key must be stored by reference");
    }

    @Test
    void testSetClassFieldsCache_mapWithNullValue() throws Exception {
        Map<Object, Collection<Field>> map = new HashMap<Object, Collection<Field>>() {

            {
                put("key", null);
            }
        };
        ReflectionUtils.setClassFieldsCache(map);
        assertSame(map, getFieldsCache(), "Map with null value must be stored by reference");
    }

    @Test
    void testSetClassFieldsCache_collectionWithNullElement() throws Exception {
        Map<Object, Collection<Field>> map = new HashMap<Object, Collection<Field>>() {

            {
                put("key", new ArrayList<Field>(Arrays.asList((Field) null)));
            }
        };
        ReflectionUtils.setClassFieldsCache(map);
        assertSame(map, getFieldsCache(), "Map whose collection contains null must be stored by reference");
    }

    @Test
    void testSetClassFieldsCache_concurrentMap() throws Exception {
        Map<Object, Collection<Field>> concurrent = new ConcurrentHashMap<Object, Collection<Field>>() {

            {
                put("key", Collections.emptyList());
            }
        };
        ReflectionUtils.setClassFieldsCache(concurrent);
        assertSame(concurrent, getFieldsCache(), "ConcurrentHashMap instance should be stored unchanged");
    }

    @Test
    void testSetClassFieldsCache_customMapSubclass() throws Exception {
        MyMap custom = new MyMap() {

            {
                put("key", Collections.emptyList());
            }
        };
        ReflectionUtils.setClassFieldsCache(custom);
        assertSame(custom, getFieldsCache(), "Custom MyMap instance should be stored unchanged");
    }

    @Test
    void testSetClassFieldsCache_mutabilityReflection() throws Exception {
        Map<Object, Collection<Field>> mutable = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(mutable);
        // Mutate after assignment
        mutable.put("newKey", Collections.emptyList());
        // The internal cache should see the new entry because it holds the same reference.
        Map<?, ?> internal = getFieldsCache();
        assertTrue(internal.containsKey("newKey"), "Internal cache must reflect mutations to the original map");
    }

    @Test
    void testSetClassFieldsCache_replacementOfPreviousCache() throws Exception {
        Map<Object, Collection<Field>> first = new HashMap<Object, Collection<Field>>() {

            {
                put("first", Collections.emptyList());
            }
        };
        ReflectionUtils.setClassFieldsCache(first);
        assertSame(first, getFieldsCache(), "First assignment should be stored");
        Map<Object, Collection<Field>> second = new HashMap<Object, Collection<Field>>() {

            {
                put("second", Collections.emptyList());
            }
        };
        ReflectionUtils.setClassFieldsCache(second);
        assertSame(second, getFieldsCache(), "Second assignment should replace the first");
        assertNotSame(first, getFieldsCache(), "Internal cache must no longer reference the first map");
    }
}
