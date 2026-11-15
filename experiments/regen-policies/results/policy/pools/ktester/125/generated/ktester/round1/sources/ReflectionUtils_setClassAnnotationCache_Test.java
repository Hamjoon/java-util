package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setClassAnnotationCache_Test {

    private Map<Object, Annotation> cache;

    // a reusable dummy annotation instance created via a dynamic proxy
    private static final Annotation DUMMY_ANNOTATION = (Annotation) Proxy.newProxyInstance(DummyAnnotation.class.getClassLoader(), new Class[] { DummyAnnotation.class }, (proxy, method, args) -> {
        if ("annotationType".equals(method.getName())) {
            return DummyAnnotation.class;
        }
        return null;
    });

    // reflective access to the private static field
    private static Field cacheField;

    @ControllerClass
    static class Foo {
    }

    /**
     * Simple user‑defined map subclass used in one of the test scenarios.
     */
    private static class CustomMap extends HashMap<Object, Annotation> {
        // No additional behaviour – existence of a custom subclass is what we test.
    }

    @BeforeAll
    static void initReflection() throws NoSuchFieldException {
        cacheField = ReflectionUtils.class.getDeclaredField("CLASS_ANNOTATION_CACHE");
        cacheField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh, empty cache that we can inspect.
        cache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(cache);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – the next test will create a new map.
        cache.clear();
        // to avoid cross‑test contamination.
        cacheField.set(null, new LRUCache<>(ReflectionUtils.CACHE_SIZE));
    }

    private static Stream<Arguments> cacheProvider() {
        // 1. null reference
        Map<Object, Annotation> nullMap = null;
        // 2. empty mutable map
        Map<Object, Annotation> emptyHashMap = new HashMap<>();
        // 3. immutable map with a single entry
        Map<Object, Annotation> singleEntryMap = Map.of("key1", DUMMY_ANNOTATION);
        // 4. unmodifiable map with two entries
        Map<Object, Annotation> unmodifiableTwo = Collections.unmodifiableMap(Map.of("keyA", DUMMY_ANNOTATION, "keyB", DUMMY_ANNOTATION));
        // 5. raw‑cast map with incompatible generic types (String → String)
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> rawStringMap = (Map) Map.of("stringKey", "stringValue");
        // 6. map with null key → DummyAnnotation
        Map<Object, Annotation> nullKeyMap = new HashMap<Object, Annotation>() {

            {
                put(null, DUMMY_ANNOTATION);
            }
        };
        // 7. map with key → null value
        Map<Object, Annotation> nullValueMap = new HashMap<Object, Annotation>() {

            {
                put("key", null);
            }
        };
        // 8. map with both key and value null
        Map<Object, Annotation> bothNullMap = new HashMap<Object, Annotation>() {

            {
                put(null, null);
            }
        };
        // 9. LinkedHashMap with 100 entries
        Map<Object, Annotation> hundredMap = new LinkedHashMap<Object, Annotation>() {

            {
                for (int i = 0; i < 100; i++) {
                    put("k" + i, DUMMY_ANNOTATION);
                }
            }
        };
        // 10. ConcurrentHashMap with one entry
        Map<Object, Annotation> concurrentMap = new ConcurrentHashMap<Object, Annotation>() {

            {
                put("cKey", DUMMY_ANNOTATION);
            }
        };
        // 11. Custom user‑defined map subclass
        Map<Object, Annotation> customMap = new CustomMap() {

            {
                put("custom", DUMMY_ANNOTATION);
            }
        };
        // 12. Re‑assign the same map instance (use emptyHashMap again)
        Map<Object, Annotation> sameInstanceMap = emptyHashMap;
        // 13. Immutable empty map from Map.of()
        Map<Object, Annotation> immutableEmpty = Map.of();
        // 14. Synchronized map wrapper
        Map<Object, Annotation> synchronizedMap = Collections.synchronizedMap(new HashMap<>());
        // 15. Raw‑cast mixed map (valid annotation + null value)
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> mixedRawMap = (Map) Map.of("mixedKey", DUMMY_ANNOTATION, "nullValue", null);
        return Stream.of(Arguments.of(nullMap, "null reference"), Arguments.of(emptyHashMap, "empty mutable HashMap"), Arguments.of(singleEntryMap, "single entry immutable map"), Arguments.of(unmodifiableTwo, "unmodifiable map with two entries"), Arguments.of(rawStringMap, "raw‑cast map with String→String"), Arguments.of(nullKeyMap, "null key → DummyAnnotation"), Arguments.of(nullValueMap, "key → null value"), Arguments.of(bothNullMap, "null key and null value"), Arguments.of(hundredMap, "LinkedHashMap with 100 entries"), Arguments.of(concurrentMap, "ConcurrentHashMap with one entry"), Arguments.of(customMap, "CustomMap subclass"), Arguments.of(sameInstanceMap, "re‑assign same map instance"), Arguments.of(immutableEmpty, "immutable empty map (Map.of())"), Arguments.of(synchronizedMap, "synchronized map wrapper"), Arguments.of(mixedRawMap, "raw‑cast mixed map (valid + null)"));
    }

    @Test
    void testCacheIsPopulatedAfterAnnotationLookup() {
        // When we ask for the annotation, ReflectionUtils should store it in the cache we supplied.
        Annotation ann = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertNotNull(ann, "Annotation should be found on Foo");
        assertEquals(1, cache.size(), "Cache should contain exactly one entry after the lookup");
    }

    @Test
    void testCacheDoesNotDuplicateEntriesOnRepeatedLookup() {
        // First lookup populates the cache.
        ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        int sizeAfterFirst = cache.size();
        assertEquals(1, sizeAfterFirst, "Cache should have one entry after first lookup");
        // Second lookup for the same class should hit the cache and not add a new entry.
        ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertEquals(sizeAfterFirst, cache.size(), "Cache size should remain unchanged after a repeated lookup");
    }

    @Test
    void testReplacingCacheClearsPreviousEntries() {
        // Populate the original cache.
        ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertEquals(1, cache.size(), "Original cache should contain one entry");
        // Replace the cache with a brand‑new map.
        Map<Object, Annotation> newCache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(newCache);
        // The new cache should be empty at this point.
        assertEquals(0, newCache.size(), "New cache must start empty");
        // A subsequent lookup should populate the new cache, not the old one.
        ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertEquals(1, newCache.size(), "New cache should contain one entry after lookup");
        assertEquals(1, cache.size(), "Original cache should remain unchanged after we switched to a new cache");
    }

    @Test
    void testSetClassAnnotationCacheAcceptsEmptyMap() {
        // An empty map should be accepted without throwing and should be used for future lookups.
        Map<Object, Annotation> emptyMap = new HashMap<>();
        assertDoesNotThrow(() -> ReflectionUtils.setClassAnnotationCache(emptyMap));
        // Verify that lookups still work and populate the provided empty map.
        ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertEquals(1, emptyMap.size(), "Provided empty map should be populated after an annotation lookup");
    }

    /**
     * Verifies that {@link ReflectionUtils#setClassAnnotationCache(Map)} stores the supplied
     * map reference unchanged (or {@code null}) and that the content of the map is untouched.
     *
     * The test is parameterised to cover a wide variety of map implementations and edge‑cases.
     */
    @ParameterizedTest(name = "[{index}] cache = {1}")
    @MethodSource("cacheProvider")
    void testSetClassAnnotationCacheVariousScenarios(Map<Object, Annotation> suppliedCache, String description) throws IllegalAccessException {
        // Act – set the cache (the method must not throw for any of the supplied maps)
        assertDoesNotThrow(() -> ReflectionUtils.setClassAnnotationCache(suppliedCache), () -> "setClassAnnotationCache threw for scenario: " + description);
        // Retrieve the internal static field via reflection
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> internalCache = (Map<Object, Annotation>) cacheField.get(null);
        // Assertion logic differs for the null case
        if (suppliedCache == null) {
            assertNull(internalCache, "Internal cache should be null when a null map is supplied");
            return;
        }
        // For non‑null maps we expect reference equality
        assertSame(suppliedCache, internalCache, () -> "Internal cache reference should be the same as the supplied map for scenario: " + description);
        // Additional sanity checks for a few interesting cases
        switch(description) {
            case "single entry immutable map":
                assertEquals(1, internalCache.size(), "Cache should contain exactly one entry");
                assertEquals(DUMMY_ANNOTATION, internalCache.get("key1"), "Entry value should be the dummy annotation");
                break;
            case "unmodifiable map with two entries":
                assertEquals(2, internalCache.size(), "Cache should contain two entries");
                assertTrue(internalCache.containsKey("keyA"));
                assertTrue(internalCache.containsKey("keyB"));
                break;
            case "raw‑cast map with String→String":
                // The map was forced via a raw cast; we verify that the raw contents are present.
                assertEquals(1, internalCache.size(), "Raw map should still contain one entry");
                assertEquals("stringValue", internalCache.get("stringKey"), "Value should be the original String, demonstrating type erasure");
                break;
            case "null key → DummyAnnotation":
                assertTrue(internalCache.containsKey(null), "Cache must contain a null key");
                assertEquals(DUMMY_ANNOTATION, internalCache.get(null), "Null key should map to the dummy annotation");
                break;
            case "key → null value":
                assertTrue(internalCache.containsKey("key"), "Cache must contain the key \"key\"");
                assertNull(internalCache.get("key"), "Value for \"key\" should be null");
                break;
            case "null key and null value":
                assertTrue(internalCache.containsKey(null), "Cache must contain a null key");
                assertNull(internalCache.get(null), "Value for null key should be null");
                break;
            case "LinkedHashMap with 100 entries":
                assertEquals(100, internalCache.size(), "Cache should contain 100 entries");
                // Spot‑check a few entries
                assertEquals(DUMMY_ANNOTATION, internalCache.get("k0"));
                assertEquals(DUMMY_ANNOTATION, internalCache.get("k99"));
                break;
            case "ConcurrentHashMap with one entry":
                assertEquals(1, internalCache.size(), "Cache should contain one entry");
                assertEquals(DUMMY_ANNOTATION, internalCache.get("cKey"));
                break;
            case "CustomMap subclass":
                assertTrue(internalCache instanceof CustomMap, "Internal cache should be an instance of CustomMap");
                assertEquals(DUMMY_ANNOTATION, internalCache.get("custom"));
                break;
            case "re‑assign same map instance":
                // No additional checks – reference equality already verified.
                break;
            case "immutable empty map (Map.of())":
                assertTrue(internalCache.isEmpty(), "Cache should be empty");
                break;
            case "synchronized map wrapper":
                assertTrue(internalCache.isEmpty(), "Synchronized wrapper should be empty");
                break;
            case "raw‑cast mixed map (valid + null)":
                assertEquals(2, internalCache.size(), "Mixed map should contain two entries");
                assertEquals(DUMMY_ANNOTATION, internalCache.get("mixedKey"));
                assertNull(internalCache.get("nullValue"));
                break;
            default:
                // For scenarios not explicitly checked we simply verify that the map is stored unchanged.
                break;
        }
    }
}
