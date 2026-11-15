package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// import org.junit.*;
// import static org.junit.Assert.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_equals_Test {

    private CaseInsensitiveMap<String, String> ciMap;

    private Map<String, String> regularMap;

    /**
     * Simple value object with a custom {@code equals} implementation.
     */
    private static final class CustomValue {

        private final int id;

        CustomValue(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomValue))
                return false;
            return id == ((CustomValue) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomValue{id=" + id + '}';
        }
    }

    /**
     * Custom key type that does not implement {@link CharSequence}.
     */
    private static final class CustomKey {

        private final String value;

        CustomKey(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomKey{value='" + value + "'}";
        }
    }

    /**
     * A map whose {@code entrySet()} iterator throws a {@link RuntimeException}
     * when {@code hasNext()} or {@code next()} is called. Used to verify that
     * {@code CaseInsensitiveMap.equals} does not swallow such exceptions.
     */
    private static final class FaultyMap extends AbstractMap<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<Entry<Object, Object>>() {

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<Entry<Object, Object>>() {

                        @Override
                        public boolean hasNext() {
                            throw new RuntimeException("Iterator failure (hasNext)");
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            throw new RuntimeException("Iterator failure (next)");
                        }
                    };
                }

                @Override
                public int size() {
                    // size is irrelevant; iterator always fails
                    return 1;
                }
            };
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        ciMap = new CaseInsensitiveMap<>();
        regularMap = new HashMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        ciMap.clear();
        regularMap.clear();
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Reset any static configuration that could affect other test suites.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @Test
//     void equalsSameInstanceReturnsTrue() {
//         assertTrue("A map must be equal to itself", ciMap.equals(ciMap));
//     }

//     @Test
//     void equalsAgainstNonMapReturnsFalse() {
//         assertFalse("Equality with a non‑Map object must be false", ciMap.equals("not a map"));
//     }

//     @Test
//     void equalsEmptyMapsAreTrue() {
//         assertTrue("Two empty maps should be equal", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsSameContentDifferentCaseIsTrue() {
//         ciMap.put("One", "Two");
//         ciMap.put("Three", "Four");
//         regularMap.put("one", "Two");
//         regularMap.put("THREE", "Four");
//         assertTrue("Case‑insensitive keys with identical values must be equal", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsDifferentSizeIsFalse() {
//         ciMap.put("One", "Two");
//         regularMap.put("One", "Two");
//         regularMap.put("Extra", "Value");
//         assertFalse("Maps of different sizes must not be equal", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsDifferentValueIsFalse() {
//         ciMap.put("Key", "Value1");
//         regularMap.put("key", "Value2");
//         assertFalse("Maps with same keys but different values must not be equal", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsMissingKeyIsFalse() {
//         ciMap.put("Present", "Yes");
//         regularMap.put("Present", "Yes");
//         regularMap.put("Missing", "No");
//         assertFalse("If the other map contains a key not present in this map, equality must be false", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsWithNullKeyAndValue() {
//         ciMap.put(null, "nullValue");
//         regularMap.put(null, "nullValue");
//         assertTrue("Maps containing a null key/value pair must be equal when the pairs match", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsWithNonStringKeys() {
//         CaseInsensitiveMap<Object, String> mixedCiMap = new CaseInsensitiveMap<>();
//         Map<Object, String> mixedRegularMap = new HashMap<>();
//         mixedCiMap.put(42, "Answer");
//         mixedCiMap.put("Key", "Value");
//         mixedRegularMap.put(42, "Answer");
//         mixedRegularMap.put("key", "Value");
//         assertTrue("Equality must work with a mixture of String and non‑String keys", mixedCiMap.equals(mixedRegularMap));
//     }

//     @Test
//     void equalsWhenOtherMapDoesNotContainKeyFromThisMapIsFalse() {
//         ciMap.put("One", "Two");
//         ciMap.put("Two", "Three");
//         regularMap.put("one", "Two");
//         assertFalse("If the other map lacks a key present in this map, equality must be false", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsNullArgumentReturnsFalse() {
//         assertFalse("Passing null should return false because the method checks instanceof Map after reference check", ciMap.equals(null));
//     }

//     @Test
//     void equalsNullKeyMissingMapReturnsFalse() {
//         regularMap.put(null, "value");
//         assertFalse("When the other map contains a null key that this map does not, equals must be false", ciMap.equals(regularMap));
//     }

//     @Test
//     void equalsCustomObjectEqualMapReturnsTrue() {
//         CustomValue v1 = new CustomValue(10);
//         CustomValue v2 = new CustomValue(10);
//         ciMap.put("key", v1.toString());
//         Map<String, CustomValue> other = new HashMap<>();
//         other.put("KEY", v2);
//         assertTrue("Maps with equal custom objects as values must be considered equal", ciMap.equals(other));
//     }

//     @Test
//     void equalsCustomObjectDifferentMapReturnsFalse() {
//         CustomValue v1 = new CustomValue(10);
//         CustomValue v2 = new CustomValue(20);
//         ciMap.put("key", v1.toString());
//         Map<String, CustomValue> other = new HashMap<>();
//         other.put("KEY", v2);
//         assertFalse("Maps with different custom objects as values must not be equal", ciMap.equals(other));
//     }

//     @Test
//     void equalsConcurrentHashMapWithSameEntriesReturnsTrue() {
//         ciMap.put("Alpha", "A");
//         ciMap.put("Beta", "B");
//         ConcurrentHashMap<String, String> concurrent = new ConcurrentHashMap<>();
//         concurrent.put("alpha", "A");
//         concurrent.put("BETA", "B");
//         assertTrue("A ConcurrentHashMap with the same entries (case‑insensitively) must be equal", ciMap.equals(concurrent));
//     }

//     @Test
//     void equalsMapWithIntegerKeyThrowsClassCastException() {
//         ciMap.put("one", "1");
//         Map<Integer, String> other = new HashMap<>();
//         other.put(1, "1");
//         ciMap.equals(other);
//     }

//     @Test
//     void equalsMapWithCustomKeyObjectThrowsClassCastException() {
//         ciMap.put("custom", "value");
//         Map<CustomKey, String> other = new HashMap<>();
//         other.put(new CustomKey("c"), "value");
//         ciMap.equals(other);
//     }

//     @Test
//     void equalsMapWithFaultyEntrySetPropagatesRuntimeException() {
//         ciMap.put("ok", "value");
//         Map<Object, Object> faulty = new FaultyMap();
//         faulty.put("any", "value");
//         ciMap.equals(faulty);
//     }

//     @Before
//     public void setUp() {
//         ciMap = new CaseInsensitiveMap<>();
//         regularMap = new HashMap<>();
//     }

//     @After
//     public void tearDown() {
//         ciMap.clear();
//         regularMap.clear();
//     }

//     @AfterClass
//     public static void afterAll() {
//         // Reset any static configuration that could affect other test suites.
//         CaseInsensitiveMap.setMaxCacheLengthString(100);
//         CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
//     }
}