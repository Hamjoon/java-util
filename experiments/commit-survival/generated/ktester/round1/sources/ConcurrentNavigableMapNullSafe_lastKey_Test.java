package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lastKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Simple comparable wrapper used for custom‑object key tests.
     */
    private static class MyKey implements Comparable<MyKey> {

        private final int value;

        MyKey(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(MyKey o) {
            return Integer.compare(this.value, o.value);
        }

        @Override
        public String toString() {
            return "obj" + value;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof MyKey) && ((MyKey) o).value == this.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default map (natural ordering)
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        map = null;
    }

    @Test
    void lastKeyReturnsHighestKeyWhenNoNullPresent() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        // Natural ordering: "cherry" is the greatest key
        assertEquals("cherry", map.lastKey(), "lastKey() should return the highest key according to natural ordering");
    }

    @Test
    void lastKeyConsidersNullKeyAsLastInAscendingOrder() {
        map.put("alpha", 10);
        map.put("beta", 20);
        // null key should be considered the last element
        map.put(null, 30);
        assertNull(map.lastKey(), "When a null key is present, lastKey() should return null (null is treated as the last key)");
    }

    @Test
    void lastKeyWithCustomComparatorAndNullKey() {
        Comparator<String> reverse = Collections.reverseOrder();
        // Use the constructor that accepts a comparator
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("first", 1);
        map.put("second", 2);
        // null still considered last irrespective of comparator
        map.put(null, 3);
        assertNull(map.lastKey(), "Even with a custom comparator, a null key must be treated as the last key");
    }

    @Test
    void lastKeyWithCustomComparatorWithoutNullKey() {
        Comparator<String> reverse = Collections.reverseOrder();
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Reverse order: "a" is the greatest (because reverse comparator makes "a" > "b" > "c")
        assertEquals("a", map.lastKey(), "lastKey() should respect the provided comparator when no null key is present");
    }

    @Test
    void lastKeyThrowsExceptionWhenMapIsEmpty() {
        // The contract of ConcurrentNavigableMap.lastKey() is to throw NoSuchElementException
        // when the map contains no entries.
        assertThrows(NoSuchElementException.class, () -> map.lastKey(), "Calling lastKey() on an empty map should throw NoSuchElementException");
    }

    @Test
    void lastKeyReturnsNullWhenOnlyNullKeyIsPresent() {
        // Map contains only a null key (masked internally)
        map.put(null, 42);
        assertNull(map.lastKey(), "When the map contains only a null key, lastKey() must unmask and return null");
    }

    @Test
    void lastKeyReturnsSingleKeyWhenMapHasOneEntry() {
        map.put("maxKey", 99);
        assertEquals("maxKey", map.lastKey(), "With a single entry, lastKey() should return that key");
    }

    @Test
    void lastKeyReturnsHighestKeyAmongMultipleEntries() {
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        assertEquals("c", map.lastKey(), "Among multiple entries, lastKey() must return the highest key according to natural ordering");
    }

    @Test
    void lastKeyAfterRemovalOfCurrentHighestKey() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Remove the current highest key ("c")
        map.remove("c");
        assertEquals("b", map.lastKey(), "After removing the highest key, lastKey() should return the next highest key");
    }

    @Test
    void lastKeyAfterAddingHigherKey() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Insert a new key that is greater than existing keys
        map.put("d", 4);
        assertEquals("d", map.lastKey(), "After adding a key greater than all existing ones, lastKey() should return that new key");
    }

    @Test
    void lastKeyWithNumericKeys() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(5, "five");
        intMap.put(10, "ten");
        assertEquals(10, intMap.lastKey(), "Numeric keys should be ordered naturally; lastKey() must return the greatest number");
    }

    @Test
    void lastKeyWithCustomComparableObjects() {
        ConcurrentNavigableMapNullSafe<MyKey, String> objMap = new ConcurrentNavigableMapNullSafe<>();
        MyKey obj1 = new MyKey(1);
        MyKey obj2 = new MyKey(2);
        objMap.put(obj1, "low");
        objMap.put(obj2, "high");
        assertEquals(obj2, objMap.lastKey(), "Custom comparable objects should be ordered by their natural ordering; lastKey() must return the greatest");
    }

    @Test
    void lastKeyThrowsNoSuchElementExceptionWhenMapIsEmpty() {
        // Re‑asserting the contract explicitly for clarity
        assertThrows(NoSuchElementException.class, () -> map.lastKey(), "Empty map must cause lastKey() to throw NoSuchElementException");
    }

    @Test
    void lastKeyThrowsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // Use reflection to null‑out the protected internalMap field
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.lastKey(), "If internalMap is null, invoking lastKey() should result in NullPointerException");
    }

    @Test
    void lastKeyThrowsClassCastExceptionWhenInternalMapIsWrongType() throws Exception {
        // Replace the internal map with a plain HashMap (not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.lastKey(), "If internalMap is not a ConcurrentSkipListMap, the cast inside lastKey() should throw ClassCastException");
    }
}
