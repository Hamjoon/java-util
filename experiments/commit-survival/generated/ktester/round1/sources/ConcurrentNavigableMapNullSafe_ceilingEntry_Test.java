package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_ceilingEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        if (map != null) {
            map.clear();
        }
        map = null;
    }

    /* Helper to manipulate the protected internalMap field via reflection. */
    private void setInternalMap(Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, newMap);
    }

    private static Stream<Arguments> basicNumericCases() {
        return Stream.of(// exact match
        Arguments.of(1, 1), // next higher key
        Arguments.of(2, 3), // null key is treated as greatest (maps to key 5)
        Arguments.of(null, 5), // greater than any existing key
        Arguments.of(100, null));
    }

    private static Stream<Arguments> stringKeyCases() {
        return Stream.of(Arguments.of("aardvark", "apple"), Arguments.of("balloon", "banana"), Arguments.of("zoo", null));
    }

    @Test
    void ceilingEntry_returnsEntryWithLeastKeyGreaterOrEqual_toGivenKey() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // when
        Entry<String, Integer> entry = map.ceilingEntry("b");
        // then
        assertNotNull(entry, "Ceiling entry for 'b' should not be null");
        assertEquals("c", entry.getKey(), "Ceiling key should be the smallest key >= 'b'");
        assertEquals(3, entry.getValue());
    }

    @Test
    void ceilingEntry_returnsExactMatch_whenKeyExists() {
        // given
        map.put("alpha", 10);
        map.put("beta", 20);
        // when
        Entry<String, Integer> entry = map.ceilingEntry("beta");
        // then
        assertNotNull(entry);
        assertEquals("beta", entry.getKey());
        assertEquals(20, entry.getValue());
    }

    @Test
    void ceilingEntry_returnsNull_whenKeyIsGreaterThanAllExistingKeys() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        // when
        Entry<String, Integer> entry = map.ceilingEntry("z");
        // then
        assertNull(entry, "Ceiling entry for a key beyond the highest should be null");
    }

    @Test
    void ceilingEntry_considersNullKeyAsGreatestKey() {
        // given
        map.put("x", 100);
        // null is treated as the last (greatest) key
        map.put(null, 999);
        // when: request ceiling for a normal key
        Entry<String, Integer> entryForNormalKey = map.ceilingEntry("a");
        // when: request ceiling for null key itself
        Entry<String, Integer> entryForNullKey = map.ceilingEntry(null);
        // then
        assertNotNull(entryForNormalKey, "Ceiling entry for a normal key should be the null entry");
        assertNull(entryForNormalKey.getKey(), "Key of the ceiling entry should be null");
        assertEquals(999, entryForNormalKey.getValue());
        assertNotNull(entryForNullKey, "Ceiling entry for null should be the null entry itself");
        assertNull(entryForNullKey.getKey());
        assertEquals(999, entryForNullKey.getValue());
    }

    @Test
    void ceilingEntry_whenOnlyNullKeyExists_returnsNullForSmallerKeyAndNullForExactKey() {
        // given
        map.put(null, 42);
        // when: ask for a key that is smaller than null (any non‑null key)
        Entry<String, Integer> entryForNonNull = map.ceilingEntry("any");
        // then
        assertNotNull(entryForNonNull, "With only a null key, any non‑null key should ceiling to the null entry");
        assertNull(entryForNonNull.getKey());
        assertEquals(42, entryForNonNull.getValue());
        // when: ask for null key itself
        Entry<String, Integer> entryForNull = map.ceilingEntry(null);
        // then
        assertNotNull(entryForNull);
        assertNull(entryForNull.getKey());
        assertEquals(42, entryForNull.getValue());
    }

    /* Basic functionality – numeric keys (natural ordering). */
    @ParameterizedTest(name = "{index} => key={0}, expectedKey={1}")
    @MethodSource("basicNumericCases")
    void ceilingEntry_basicNumeric(Object key, Object expectedKey) {
        // given
        map.put(1, "one");
        map.put(3, "three");
        map.put(5, "five");
        // when
        Map.Entry<Object, Object> entry = map.ceilingEntry(key);
        // then
        if (expectedKey == null) {
            assertNull(entry, "Expected null for key " + key);
        } else {
            assertNotNull(entry, "Expected non‑null entry for key " + key);
            assertEquals(expectedKey, entry.getKey(), "Ceiling key mismatch for input " + key);
        }
    }

    /* Empty map – any key should return null. */
    @Test
    void ceilingEntry_emptyMap_returnsNull() {
        // when
        Map.Entry<Object, Object> entry = map.ceilingEntry(5);
        // then
        assertNull(entry, "Empty map must return null for any ceilingEntry call");
    }

    /* Key smaller than the smallest existing key. */
    @Test
    void ceilingEntry_keySmallerThanSmallest_returnsFirstEntry() {
        map.put(10, "ten");
        map.put(20, "twenty");
        Map.Entry<Object, Object> entry = map.ceilingEntry(5);
        assertNotNull(entry);
        assertEquals(10, entry.getKey());
    }

    /* Key equal to the highest existing key. */
    @Test
    void ceilingEntry_keyEqualsHighest_returnsExactEntry() {
        map.put(10, "ten");
        map.put(20, "twenty");
        Map.Entry<Object, Object> entry = map.ceilingEntry(20);
        assertNotNull(entry);
        assertEquals(20, entry.getKey());
    }

    /* Key just below the highest existing key. */
    @Test
    void ceilingEntry_keyJustBelowHighest_returnsNextHigher() {
        map.put(10, "ten");
        map.put(20, "twenty");
        Map.Entry<Object, Object> entry = map.ceilingEntry(19);
        assertNotNull(entry);
        assertEquals(20, entry.getKey());
    }

    /* Map containing only a null key. */
    @Test
    void ceilingEntry_onlyNullKey_behaviour() {
        map.put(null, 42);
        // any non‑null key should ceiling to the null entry
        Map.Entry<Object, Object> entryForNonNull = map.ceilingEntry("any");
        assertNotNull(entryForNonNull);
        assertNull(entryForNonNull.getKey());
        assertEquals(42, entryForNonNull.getValue());
        // null key itself should also return the null entry
        Map.Entry<Object, Object> entryForNull = map.ceilingEntry(null);
        assertNotNull(entryForNull);
        assertNull(entryForNull.getKey());
        assertEquals(42, entryForNull.getValue());
    }

    /* String keys – natural ordering. */
    @ParameterizedTest(name = "{index} => key={0}, expectedKey={1}")
    @MethodSource("stringKeyCases")
    void ceilingEntry_stringKeys(String key, String expectedKey) {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        Map.Entry<Object, Object> entry = map.ceilingEntry(key);
        if (expectedKey == null) {
            assertNull(entry, "Expected null for key " + key);
        } else {
            assertNotNull(entry);
            assertEquals(expectedKey, entry.getKey());
        }
    }

    /* Custom comparator – reverse (descending) order. */
    @Test
    void ceilingEntry_customComparator_reverseOrder_exactMatch() {
        // Build a map with reverse order comparator
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        ConcurrentSkipListMap<Integer, String> internal = new ConcurrentSkipListMap<>(reverse);
        internal.put(3, "three");
        internal.put(2, "two");
        internal.put(1, "one");
        // inject the custom internal map
        assertDoesNotThrow(() -> setInternalMap(internal));
        Map.Entry<Object, Object> entry = map.ceilingEntry(2);
        assertNotNull(entry);
        // In reverse order, "ceiling" means the smallest key >= 2 according to reverse ordering,
        // which is 2 itself.
        assertEquals(2, entry.getKey());
    }

    @Test
    void ceilingEntry_customComparator_reverseOrder_missingKey() {
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        ConcurrentSkipListMap<Integer, String> internal = new ConcurrentSkipListMap<>(reverse);
        internal.put(3, "three");
        internal.put(1, "one");
        assertDoesNotThrow(() -> setInternalMap(internal));
        // With keys 3 and 1, the ordering is 3 > 1.
        // For key 2, the ceiling (>=2) in reverse order is 1.
        Map.Entry<Object, Object> entry = map.ceilingEntry(2);
        assertNotNull(entry);
        assertEquals(1, entry.getKey());
    }

    /* internalMap is null – should throw NullPointerException. */
    @Test
    void ceilingEntry_internalMapIsNull_throwsNPE() throws Exception {
        // Force internalMap to null via reflection
        setInternalMap(null);
        assertThrows(NullPointerException.class, () -> map.ceilingEntry(1), "When internalMap is null, ceilingEntry must throw NPE");
    }

    /* internalMap of wrong type – ClassCastException expected. */
    @Test
    void ceilingEntry_internalMapWrongType_throwsClassCastException() throws Exception {
        // Use a plain HashMap (not a ConcurrentSkipListMap)
        Map<Object, Object> wrongMap = new HashMap<>();
        wrongMap.put(1, "one");
        setInternalMap(wrongMap);
        assertThrows(ClassCastException.class, () -> map.ceilingEntry(1), "Casting to ConcurrentSkipListMap should fail");
    }

    /* Key not comparable to existing keys – ClassCastException. */
    @Test
    void ceilingEntry_keyNotComparable_throwsClassCastException() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        assertThrows(ClassCastException.class, () -> map.ceilingEntry("stringKey"), "Mixing incomparable key types must cause ClassCastException");
    }

    /* Key incompatible with custom comparator – ClassCastException. */
    @Test
    void ceilingEntry_keyIncompatibleWithComparator_throwsClassCastException() throws Exception {
        Comparator<Integer> intComparator = Comparator.naturalOrder();
        ConcurrentSkipListMap<Integer, String> internal = new ConcurrentSkipListMap<>(intComparator);
        internal.put(1, "one");
        internal.put(2, "two");
        setInternalMap(internal);
        assertThrows(ClassCastException.class, () -> map.ceilingEntry(2.5), "Comparator expecting Integer should reject Double key");
    }

    /* Null key with natural ordering comparator that rejects null – ClassCastException. */
    @Test
    void ceilingEntry_nullKeyWithNaturalComparator_throwsClassCastException() throws Exception {
        // Natural order comparator does not allow null; the maskNullKey creates a sentinel
        // that is not comparable with Integer keys, leading to ClassCastException.
        map.put(1, "one");
        map.put(2, "two");
        // The map already uses natural ordering, we just invoke with null.
        assertThrows(ClassCastException.class, () -> map.ceilingEntry(null), "Null key with natural ordering should cause ClassCastException");
    }
}
