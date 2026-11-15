package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lastKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class Letter implements Comparable<Letter> {

        final String value;

        Letter(String v) {
            this.value = v;
        }

        @Override
        public int compareTo(Letter o) {
            return value.compareTo(o.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    class Letter implements Comparable<Letter> {

        final String value;

        Letter(String v) {
            this.value = v;
        }

        @Override
        public int compareTo(Letter o) {
            return value.compareTo(o.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test – default natural ordering
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC; not strictly necessary but keeps test isolation clear
        if (map != null) {
            map.clear();
        }
    }

    private static Stream<Arguments> provideBehaviourCases() {
        return Stream.of(// empty map → NoSuchElementException
        Arguments.of((Runnable) () -> {
            // nothing to put
        }, (Class<?>) java.util.NoSuchElementException.class, "empty map"), // single non‑null key
        Arguments.of((Runnable) () -> {
            map.put("alpha", 1);
        }, "alpha", "single non‑null key"), // several ordered keys – greatest is "zeta"
        Arguments.of((Runnable) () -> {
            map.put("beta", 1);
            map.put("gamma", 2);
            map.put("zeta", 3);
        }, "zeta", "multiple ordered keys"), // only a null key – should be unmasked back to null
        Arguments.of((Runnable) () -> {
            map.put(null, 99);
        }, null, "only null key"));
    }

    private static Stream<Arguments> provideAdditionalScenarios() {
        return Stream.of(// multiple string keys
        Arguments.of((Runnable) () -> {
            map.put("alpha", 1);
            map.put("beta", 2);
            map.put("delta", 3);
        }, "delta", "multiple strings, highest lexical value"), // integer keys (using a map with Integer type)
        Arguments.of((Runnable) () -> {
            ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
            intMap.put(1, "a");
            intMap.put(42, "b");
            intMap.put(12345, "c");
            // expose via a wrapper to reuse the same assertion logic
            Assertions.assertEquals(12345, intMap.lastKey());
        }, null, "integer keys"), // boolean keys (true > false)
        Arguments.of((Runnable) () -> {
            ConcurrentNavigableMapNullSafe<Boolean, String> boolMap = new ConcurrentNavigableMapNullSafe<>();
            boolMap.put(false, "no");
            boolMap.put(true, "yes");
            Assertions.assertEquals(true, boolMap.lastKey());
        }, null, "boolean keys"), // custom Comparable objects
        Arguments.of((Runnable) () -> {
            class Letter implements Comparable<Letter> {

                final String value;

                Letter(String v) {
                    this.value = v;
                }

                @Override
                public int compareTo(Letter o) {
                    return value.compareTo(o.value);
                }

                @Override
                public String toString() {
                    return value;
                }
            }
            ConcurrentNavigableMapNullSafe<Letter, Integer> compMap = new ConcurrentNavigableMapNullSafe<>();
            compMap.put(new Letter("a"), 1);
            compMap.put(new Letter("b"), 2);
            compMap.put(new Letter("c"), 3);
            assertEquals("c", compMap.lastKey().toString(), "Custom Comparable should order correctly");
        }, null, "custom Comparable objects"), // mixed‑case strings – natural ordering is case‑sensitive
        Arguments.of((Runnable) () -> {
            map.put("a", 1);
            map.put("Z", 2);
            map.put("m", 3);
        }, "Z", "mixed case strings, Unicode order"), // Unicode character Ω (code point 937) is greatest among given keys
        Arguments.of((Runnable) () -> {
            // Greek alpha
            map.put("α", 1);
            // Greek capital omega
            map.put("Ω", 2);
            map.put("β", 3);
        }, "Ω", "Unicode keys, highest code point"));
    }

    @Test
    void lastKeyWithoutNullKey_returnsGreatestKey() {
        // given
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        // when
        String last = map.lastKey();
        // then
        assertEquals("cherry", last, "lastKey should return the greatest key according to natural ordering");
    }

    @Test
    void lastKeyWithNullKey_considersNullAsLast() {
        // given
        map.put("alpha", 10);
        map.put("beta", 20);
        // null key should be treated as the greatest
        map.put(null, 999);
        // when
        String last = map.lastKey();
        // then
        assertNull(last, "When a null key is present, lastKey should return null (null is considered last)");
    }

    @Test
    void lastKeyOnEmptyMap_throwsNoSuchElementException() {
        // given an empty map
        // when / then
        assertThrows(java.util.NoSuchElementException.class, () -> map.lastKey(), "Calling lastKey on an empty map should throw NoSuchElementException");
    }

    @ParameterizedTest(name = "lastKey behaviour – {2}")
    @MethodSource("provideBehaviourCases")
    void lastKeyBehaviour(Runnable setup, Object expected, String description) {
        // given
        setup.run();
        // when / then
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> map.lastKey(), "Expected exception for case: " + description);
        } else {
            // Expected a concrete key (may be null)
            assertEquals(expected, map.lastKey(), "Unexpected lastKey for case: " + description);
        }
    }

    @Test
    void lastKey_WithNullKeyAndCustomComparator_TreatsNullAsLowest() {
        // Comparator where null is considered less than any non‑null value
        Comparator<String> nullLowComparator = (a, b) -> {
            if (a == null && b == null)
                return 0;
            if (a == null)
                return -1;
            if (b == null)
                return 1;
            return a.compareTo(b);
        };
        // Create a map that uses the above comparator
        ConcurrentNavigableMapNullSafe<String, Integer> customMap = new ConcurrentNavigableMapNullSafe<>(nullLowComparator);
        customMap.put(null, 0);
        customMap.put("delta", 1);
        customMap.put("omega", 2);
        // The highest *real* key is "omega" because null is treated as lowest
        assertEquals("omega", customMap.lastKey(), "With a comparator that treats null as lowest, lastKey should return the greatest real key");
    }

    @ParameterizedTest(name = "additional scenario – {2}")
    @MethodSource("provideAdditionalScenarios")
    void lastKeyAdditionalScenarios(Runnable setup, Object expected, String description) {
        // The setup may contain its own assertions (for non‑String maps);
        // when an explicit expected value is supplied we assert it.
        if (expected != null) {
            setup.run();
            assertEquals(expected, map.lastKey(), "Unexpected lastKey for scenario: " + description);
        } else {
            // scenario already performed its own assertions
            setup.run();
        }
    }

    @Test
    void lastKey_IncompatibleKeyTypes_throwsClassCastException() {
        // The default comparator expects homogeneous Comparable keys.
        map.put("alpha", 1);
        // raw put to bypass generic type safety – we insert an Integer key
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe rawMap = map;
        // mix String and Integer
        rawMap.put(1, 2);
        assertThrows(ClassCastException.class, () -> map.lastKey(), "Mixing incomparable key types should propagate ClassCastException");
    }

    @Test
    void lastKey_ComparatorThrowsException_propagatesIllegalArgumentException() {
        Comparator<String> explodingComparator = (a, b) -> {
            throw new IllegalArgumentException("Comparator exploded");
        };
        ConcurrentNavigableMapNullSafe<String, Integer> explodingMap = new ConcurrentNavigableMapNullSafe<>(explodingComparator);
        explodingMap.put("x", 1);
        explodingMap.put("y", 2);
        assertThrows(IllegalArgumentException.class, explodingMap::lastKey, "Exception from comparator should propagate");
    }

    @Test
    void lastKey_InternalMapIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the protected field 'internalMap' to null
        Field field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        assertThrows(NullPointerException.class, () -> map.lastKey(), "Calling lastKey when internalMap is null should throw NPE");
    }
}
