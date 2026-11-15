package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import java.util.concurrent.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_copy_Test {

    private TestableCaseInsensitiveMap<String, String> testableMap;

    /**
     * Helper subclass that exposes the protected {@code copy} method for testing.
     * It works with generic types so the test suite can use any key/value types.
     */
    private static class TestableCaseInsensitiveMap<K, V> extends CaseInsensitiveMap<K, V> {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<K, V> copyMap(Map<K, V> source, Map<K, V> dest) {
            return super.copy(source, dest);
        }
    }

    /**
     * A minimal map implementation whose {@code entrySet()} iterator throws
     * {@link ConcurrentModificationException} when {@code next()} is called.
     */
    private static class ThrowingMap<K, V> extends AbstractMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        @Override
                        public boolean hasNext() {
                            // pretend there is something
                            return true;
                        }

                        @Override
                        public Entry<K, V> next() {
                            throw new ConcurrentModificationException("Intentional for test");
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }

//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @BeforeEach
    void setupBeforeEach() {
        testableMap = new TestableCaseInsensitiveMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        testableMap = null;
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Restore defaults for any other test suites that might run after this one.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Group: Empty source handling
     */
//     static Stream<Arguments> emptySourceProvider() {
//         return Stream.of(Arguments.of(new HashMap<>(), new HashMap<>(), Collections.emptyMap(), "When the source map is empty, copy should return the destination map unchanged (still empty)."), Arguments.of(new HashMap<>(), mapOf("a", 1), mapOf("a", 1), "When the source map is empty but the destination already contains entries, copy should leave the destination unchanged."));
//     }

    /**
     * Group: Copy from regular map with mixed‑case string keys
     */
//     static Stream<Arguments> regularMapMixedCaseProvider() {
//         return Stream.of(Arguments.of(mapOf("HeLLo", "world", "JAVA", 8), new HashMap<>(), mapOf("hello", "world", "java", 8), "Source is a plain HashMap containing mixed‑case String keys. copy should normalize the keys (via convertKey) to lower‑case and insert them into an empty destination map."));
//     }

    /**
     * Group: Copy from regular map overwriting existing destination entries
     */
//     static Stream<Arguments> overwritingProvider() {
//         return Stream.of(Arguments.of(mapOf("Key", 100, "another", 200), mapOf("key", 1, "extra", 99), mapOf("key", 100, "another", 200, "extra", 99), "Destination already contains an entry with a key that will be normalized to the same value as a source entry. copy must replace the existing value (1 → 100) while preserving unrelated entries."));
//     }

    /**
     * Group: Copy from CaseInsensitiveMap source (optimized path)
     */
//     static Stream<Arguments> sourceCIMProvider() {
//         return Stream.of(Arguments.of(new CaseInsensitiveMap<String, Integer>() {
// 
//             {
//                 put("One", 1);
//                 put("TWO", 2);
//             }
//         }, new HashMap<>(), mapOf("One", 1, "TWO", 2), "When source is an instance of CaseInsensitiveMap, copy should bypass key conversion and copy the internal wrapped map directly. Keys retain their original case in the destination because they are already normalized internally."), Arguments.of(new CaseInsensitiveMap<String, Integer>() {
// 
//             {
//                 put("Alpha", 10);
//             }
//         }, mapOf("alpha", 5), mapOf("alpha", 10), "Source CaseInsensitiveMap already stores a normalized key. The destination entry with the same logical key (different case) must be overwritten by the source value."));
//     }

//     @Test
//     void copy_WithEmptySource_ReturnsDestinationUnchanged() {
//         Map<String, String> source = new HashMap<>();
//         Map<String, String> dest = new HashMap<>();
//         dest.put("existingKey", "existingValue");
//         Map<String, String> result = testableMap.copyMap(source, dest);
//         Assert.assertSame(dest, result);
//         Assert.assertEquals(1, dest.size());
//         Assert.assertEquals("existingValue", dest.get("existingKey"));
//     }

//     @Test
//     void copy_FromCaseInsensitiveMap_CopiesUnderlyingMapDirectly() {
//         CaseInsensitiveMap<String, String> sourceCIM = new CaseInsensitiveMap<>();
//         sourceCIM.put("MiXeD", "value1");
//         Map<String, String> dest = new HashMap<>();
//         Map<String, String> result = testableMap.copyMap(sourceCIM, dest);
//         Assert.assertSame(dest, result);
//         Assert.assertEquals(1, dest.size());
//         // The copy should preserve case‑insensitive behaviour
//         Assert.assertEquals("value1", dest.get("mixed"));
//         Assert.assertEquals("value1", dest.get("MIXED"));
//         Assert.assertEquals("value1", dest.get("MiXeD"));
//     }

//     @Test
//     void copy_FromRegularMap_ConvertsKeysCaseInsensitively() {
//         Map<String, String> source = new HashMap<>();
//         source.put("TeSt", "value2");
//         Map<String, String> dest = new HashMap<>();
//         Map<String, String> result = testableMap.copyMap(source, dest);
//         Assert.assertSame(dest, result);
//         Assert.assertEquals(1, dest.size());
//         // Keys must have been passed through convertKey (case‑insensitive)
//         Assert.assertEquals("value2", dest.get("test"));
//         Assert.assertEquals("value2", dest.get("TEST"));
//         Assert.assertEquals("value2", dest.get("TeSt"));
//     }

    /**
     * Creates a mutable {@link HashMap} from an even number of arguments.
     * The arguments are interpreted as key, value, key, value, ….
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... entries) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) entries[i];
            @SuppressWarnings("unchecked")
            V value = (V) entries[i + 1];
            map.put(key, value);
        }
        return map;
    }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("sourceCIMProvider")
//     void copy_SourceCaseInsensitiveMap(Map<String, Integer> source, Map<String, Integer> dest, Map<String, Integer> expected, String description) {
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> result = (Map<String, Integer>) testableMap.copyMap((Map) source, (Map) dest);
//         assertSame(dest, result);
//         assertEquals(expected, result);
//     }

    /**
     * Group: Copy with non‑String key types
     */
//     @Test
//     void copy_NonStringKeysAreCopiedAsIs() {
//         Map<Object, String> source = mapOf(10, "ten", 20L, "twenty");
//         Map<Object, String> dest = new HashMap<>();
//         @SuppressWarnings("unchecked")
//         Map<Object, String> result = (Map<Object, String>) testableMap.copyMap((Map) source, (Map) dest);
//         Assert.assertSame(dest, result);
//         Assert.assertEquals(mapOf(10, "ten", 20L, "twenty"), result);
//     }

    /**
     * Group: Null source handling
     */
//     @Test
//     void copy_NullSourceThrowsNPE() {
//         Map<String, String> dest = new HashMap<>();
//         try {
//             testableMap.copyMap(null, dest);
//             Assert.fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /**
     * Group: Null destination handling
     */
//     @Test
//     void copy_NullDestinationThrowsNPE() {
//         Map<String, String> source = mapOf("key", 1);
//         try {
//             testableMap.copyMap(source, null);
//             Assert.fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /**
     * Group: Destination does not allow null keys
     */
//     @Test
//     void copy_HashtableRejectsNullKey() {
//         Map<String, String> source = mapOf(null, "nullValue");
//         Map<String, String> dest = new Hashtable<>();
//         try {
//             testableMap.copyMap(source, dest);
//             Assert.fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /**
     * Group: Destination does not allow null values
     */
//     @Test
//     void copy_HashtableRejectsNullValue() {
//         Map<String, String> source = mapOf("key", null);
//         Map<String, String> dest = new Hashtable<>();
//         try {
//             testableMap.copyMap(source, dest);
//             Assert.fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

    /**
     * Group: Source map throws during iteration
     */
//     @Test
//     void copy_SourceThrowsDuringIteration() {
//         Map<String, String> source = new ThrowingMap<>();
//         Map<String, String> dest = new HashMap<>();
//         try {
//             testableMap.copyMap(source, dest);
//             Assert.fail("Expected ConcurrentModificationException");
//         } catch (ConcurrentModificationException ignored) {
//         }
//     }

//     @Test
//     public void copy_SourceCaseInsensitiveMap_OptimisedPath() {
//         CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
//         source.put("One", 1);
//         source.put("TWO", 2);
//         Map<String, Integer> dest = new HashMap<>();
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> result = (Map<String, Integer>) testableMap.copyMap((Map) source, (Map) dest);
//         Assert.assertSame(dest, result);
//         Assert.assertEquals(2, result.size());
//         Assert.assertEquals(Integer.valueOf(1), result.get("One"));
//         Assert.assertEquals(Integer.valueOf(2), result.get("TWO"));
//     }
}