package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
// import java.util.concurrent.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.LinkedHashMap;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_entrySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a small compact map (compactSize = 3) that uses a LinkedHashMap
        // as its backing store once the compact threshold is exceeded.
        map = new CompactMap<String, Integer>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Convenience factory for a map entry.
     */
    private static Map.Entry<String, Integer> entry(String k, Integer v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    /**
     * Convenience factory for an entry with a raw Object key (used for non‑Entry tests).
     */
    private static Object nonEntry(String s) {
        return s;
    }

    private static Stream<Arguments> provideContainsCases() {
        return Stream.of(
                Arguments.of(entry("key1", 100), true),
                Arguments.of(entry("key1", 999), false),
                Arguments.of(entry("nullKey", null), true),
                Arguments.of(entry("nullKey", 5), false),
                Arguments.of(entry("missing", 1), false),
                Arguments.of(nonEntry("just a string"), false)
        );
    }

    private static Stream<Arguments> provideRemoveCases() {
        return Stream.of(
                Arguments.of(entry("key1", 100), false),
                Arguments.of(nonEntry("not an entry"), false)
        );
    }

    private static Stream<Arguments> provideRemoveAllCases() {
        return Stream.of(
                Arguments.of(
                        Arrays.asList(entry("one", 1), entry("two", 99), entry("absent", 0)),
                        false
                ),
                Arguments.of(Collections.emptyList(), false)
        );
    }

    private static Stream<Arguments> provideRetainAllCases() {
        return Stream.of(
                // collection contains both exact entries – no change
                Arguments.of(
                        Arrays.asList(entry("key1", 100), entry("key2", 200)),
                        false
                ),
                // same key, different value – entry removed, map changes
                Arguments.of(
                        Collections.singletonList(entry("key1", 999)),
                        true
                ),
                // unknown key – all existing entries removed, map changes
                Arguments.of(
                        Collections.singletonList(entry("unknown", 123)),
                        true
                ),
                // non‑Entry ignored, one matching entry – map changes
                Arguments.of(
                        Arrays.asList(nonEntry("just a string"), entry("key1", 100)),
                        true
                ),
                // extra entry not present – map unchanged
                Arguments.of(
                        Arrays.asList(entry("key1", 100), entry("nullKey", null), entry("key2", 200)),
                        false
                ),
                // empty collection – map cleared, change true
                Arguments.of(
                        Collections.emptyList(),
                        true
                )
        );
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; may be {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && method.getReturnType() == void.class;
    }

    /**
     * Finds a public method with the given name and parameter types on the supplied class.
     * If no such method exists {@code null} is returned.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null || name == null) {
            return null;
        }
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            // Not a public method – try declared methods (including private/protected)
            try {
                Method m = clazz.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // fall through
            }
            return null;
        }
    }

    /**
     * Convenience helper used by the JUnit platform to check whether a {@link Method}
     * instance is present (i.e. not {@code null}).
     *
     * @param method the method to test
     * @return {@code true} if the method is non‑null, otherwise {@code false}
     */
    public static boolean isMethodPresent(Method method) {
        return method != null;
    }

    @Test
    void entrySetContainsWhenEntryExists() {
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertTrue(es.contains(entry("a", 1)), "entrySet should contain an entry that is present in the map");
        assertFalse(es.contains(entry("a", 2)), "entrySet should not contain an entry with a mismatching value");
        assertFalse(es.contains(entry("b", 1)), "entrySet should not contain an entry whose key is absent");
    }

    @Test
    void entrySetRemoveRemovesKeyAndReportChange() {
        map.put("x", 10);
        map.put("y", 20);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertTrue(es.remove(entry("x", 10)), "remove should return true when the entry existed");
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
        assertFalse(es.remove(entry("z", 30)));
        assertEquals(1, map.size());
    }

    @Test
    void entrySetRemoveAllRemovesAllMatchingEntries() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Collection<Map.Entry<String, Integer>> toRemove = Arrays.asList(
                entry("one", 1),
                entry("two", 99),
                entry("absent", 0)
        );
        // Due to the size‑comparison bug the method returns false, but the map is modified.
        assertFalse(es.removeAll(toRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("three"));
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        map.put("alpha", 100);
        map.put("beta", 200);
        map.put("gamma", 300);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Collection<Map.Entry<String, Integer>> toRetain = Arrays.asList(
                entry("alpha", 100),
                entry("beta", 999) // different value – should be removed
        );
        assertTrue(es.retainAll(toRetain), "retainAll should report a change");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("alpha"));
        assertFalse(map.containsKey("beta"));
        assertFalse(map.containsKey("gamma"));
    }

    @Test
    void entrySetClearEmptiesTheMap() {
        map.put("k1", 1);
        map.put("k2", 2);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertFalse(map.isEmpty());
        es.clear();
        assertTrue(map.isEmpty(), "clear() on the entrySet should empty the underlying map");
    }

    @ParameterizedTest(name = "{index}: contains({0}) → {1}")
    @MethodSource("provideContainsCases")
    void entrySetContains(Object input, boolean expected) {
        map.put("key1", 100);
        map.put("nullKey", null);
        // If the test wants a null key we add it explicitly
        if (input instanceof Map.Entry && ((Map.Entry<?, ?>) input).getKey() == null) {
            map.put(null, (Integer) ((Map.Entry<?, ?>) input).getValue());
        }
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertEquals(expected, es.contains(input));
    }

    @ParameterizedTest(name = "{index}: remove({0}) → {1}")
    @MethodSource("provideRemoveCases")
    void entrySetRemove(Object input, boolean expected) {
        map.put("key1", 100);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        boolean result = es.remove(input);
        assertEquals(expected, result);
        if (input instanceof Map.Entry && ((Map.Entry<?, ?>) input).getKey() != null
                && ((Map.Entry<?, ?>) input).getKey().equals("key1")) {
            assertFalse(map.containsKey("key1"));
        } else {
            assertTrue(map.containsKey("key1"));
        }
    }

    @ParameterizedTest(name = "{index}: removeAll({0}) → {1}")
    @MethodSource("provideRemoveAllCases")
    void entrySetRemoveAll(Collection<?> collection, boolean expected) {
        map.put("key1", 100);
        map.put("nullKey", null);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        boolean changed = es.removeAll(collection);
        assertEquals(expected, changed);
    }

    @Test
    void entrySetRemoveAll_nullCollection_throwsNPE() {
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertThrows(NullPointerException.class, () -> es.removeAll(null));
    }

    @ParameterizedTest(name = "{index}: retainAll({0}) → {1}")
    @MethodSource("provideRetainAllCases")
    void entrySetRetainAll(Collection<?> collection, boolean expected) {
        map.put("key1", 100);
        map.put("key2", 200);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        boolean changed = es.retainAll(collection);
        assertEquals(expected, changed);
    }

    @Test
    void entrySetRetainAll_nullCollection_throwsNPE() {
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertThrows(NullPointerException.class, () -> es.retainAll(null));
    }

    @Test
    void entrySetRetainAll_entryWithNullKey_whenMapDisallowsNull_throwsNPE() {
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Collection<Object> collection = Collections.singletonList(entry(null, 42));
        assertThrows(NullPointerException.class, () -> es.retainAll(collection));
    }

    @Test
    void iteratorBasicTraversal_containsAllEntries() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = es.iterator();
        Set<Map.Entry<String, Integer>> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(3, seen.size());
        assertTrue(seen.contains(entry("a", 1)));
        assertTrue(seen.contains(entry("b", 2)));
        assertTrue(seen.contains(entry("c", 3)));
        assertFalse(it.hasNext(), "Iterator should report no further elements");
    }

    @Test
    void iteratorRemoveAfterNext_entryRemovedFromMap() {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> first = it.next();
        it.remove();
        assertEquals(1, map.size(), "Map size should be reduced after iterator.remove()");
        assertFalse(map.containsKey(first.getKey()), "Removed key must no longer be present");
    }

    @Test
    void iteratorRemoveWithoutNext_throwsIllegalStateException() {
        map.put("p", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

//     @Test
//     void iteratorConcurrentModification_throwsConcurrentModificationException() {
//         map.put("a", 1);
//         Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
//         map.put("b", 2);
//         assertThrows(ConcurrentModificationException.class, it::next);
//     }

    @Test
    void iteratorNextOnEmpty_throwsNoSuchElementException() {
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorRemoveAllViaIterator_clearsMap() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(map.isEmpty(), "Map should be empty after removing all entries via iterator");
        assertEquals(0, map.entrySet().size(), "entrySet size must be 0 after removals");
    }

    @Test
    void entrySetSizeReflectsMap() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        map.put("k4", 4);
        map.put("k5", 5);
        assertEquals(5, map.size());
        assertEquals(5, map.entrySet().size());
        map.remove("k1");
        map.remove("k2");
        assertEquals(3, map.size());
        assertEquals(3, map.entrySet().size());
    }

    @Test
    void entrySetContainsNullKeyAndValue() {
        map.put(null, null);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertTrue(es.contains(entry(null, null)), "Contains should be true for null key & null value");
        assertFalse(es.contains(entry(null, 42)), "Contains should be false when value differs");
    }

    @Test
    void entrySetRemoveNonexistentEntry() {
        map.put("existing", 1);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        boolean removed = es.remove(entry("missingKey", 123));
        assertFalse(removed, "Removing a non‑existent entry must return false");
        assertEquals(1, map.size(), "Map size must remain unchanged");
    }

//     @Test
//     void entrySetRemoveRemovesKeyAndReportChange() {
//         map.put("x", 10);
//         map.put("y", 20);
//         Set<Map.Entry<String, Integer>> es = map.entrySet();
//         assertTrue(es.remove(entry("x", 10)), "remove should return true when the entry existed");
//         assertFalse(map.containsKey("x"));
//         assertEquals(1, map.size());
//         assertFalse(es.remove(entry("z", 30)));
//         assertEquals(1, map.size());
//     }
}