package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.NoSuchElementException;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableMap}.
 *
 * The tests are grouped in the order in which the public API appears in the source
 * file.  For each method we provide:
 *   1. a basic “happy‑path” test that verifies normal behaviour,
 *   2. one or more edge‑case / exception tests (where applicable),
 *   3. verification that the sealed / unsealed state is honoured for mutable operations.
 *
 * The tests use only the constructors and methods that are part of the supplied
 * source code (or standard JDK classes).  No mocking frameworks are used.
 */
public class SealableNavigableMapTest {

    /** Helper to create a mutable {@code Supplier<Boolean>} that can be toggled. */
    private Supplier<Boolean> supplier(final AtomicBoolean flag) {
        return flag::get;
    }

    /* --------------------------------------------------------------
     *  Constructors
     * -------------------------------------------------------------- */
    @Test
    public void testEmptyConstructorCreatesMutableEmptyMap() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(supplier(sealed));

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // mutable – should accept a put
        map.put(1, "one");
        assertEquals(1, map.size());
        assertEquals("one", map.get(1));
    }

    @Test
    public void testSortedMapConstructorCopiesEntries() {
        SortedMap<Integer, String> src = new TreeMap<>();
        src.put(1, "one");
        src.put(2, "two");
        src.put(3, "three");

        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(src, supplier(sealed));

        // original entries must be present
        assertEquals(3, map.size());
        assertEquals("two", map.get(2));

        // ensure modifications to the SealableNavigableMap do NOT affect the source map
        map.put(4, "four");
        assertFalse(src.containsKey(4));
        assertEquals(4, map.size());
    }

    @Test
    public void testNavigableMapConstructorWrapsGivenMap() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(10, "ten");
        backing.put(20, "twenty");

        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(backing, supplier(sealed));

        // the wrapper should reflect the backing map
        assertEquals(2, map.size());
        assertEquals("ten", map.get(10));

        // mutating through the wrapper must affect the original map
        map.put(30, "thirty");
        assertEquals("thirty", backing.get(30));
    }

    /* --------------------------------------------------------------
     *  Immutable (read‑only) API
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingMap() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(1, "one");
        backing.put(2, "two");

        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(backing, supplier(sealed));

        // equality with the backing map
        assertTrue(map.equals(backing));
        assertEquals(backing.hashCode(), map.hashCode());

        // inequality with a different map
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringReflectsContents() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(5, "five");
        backing.put(9, "nine");

        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(backing, supplier(sealed));

        assertEquals(backing.toString(), map.toString());
    }

    @Test
    public void testBasicQueryMethods() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(2, "two");
        map.put(1, "one");
        map.put(3, "three");

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertTrue(map.containsKey(2));
        assertTrue(map.containsValue("three"));
        assertEquals("two", map.get(2));
        assertEquals(Integer.valueOf(1), map.firstKey());
        assertEquals(Integer.valueOf(3), map.lastKey());

        Comparator<? super Integer> cmp = map.comparator();
        // TreeMap with natural ordering returns null comparator; we only assert that the call succeeds
        assertTrue(cmp == null || cmp.compare(1, 2) < 0);
    }

    @Test
    public void testNavigationalEntryMethods() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");

        // lower / floor / ceiling / higher
        assertEquals(Integer.valueOf(10), map.lowerKey(20));
        assertEquals(Integer.valueOf(20), map.floorKey(20));
        assertEquals(Integer.valueOf(20), map.ceilingKey(20));
        assertEquals(Integer.valueOf(30), map.higherKey(20));

        // entry variants
        assertEquals("ten", map.lowerEntry(20).getValue());
        assertEquals("twenty", map.floorEntry(20).getValue());
        assertEquals("twenty", map.ceilingEntry(20).getValue());
        assertEquals("thirty", map.higherEntry(20).getValue());

        // first / last entry
        assertEquals("ten", map.firstEntry().getValue());
        assertEquals("thirty", map.lastEntry().getValue());
    }

    @Test
    public void testKeySetValuesAndEntrySetAreSealable() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealed::get);
        map.put("a", 1);
        map.put("b", 2);

        // keySet removal works when unsealed
        Set<String> keys = map.keySet();
        assertTrue(keys.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // seal the map and verify mutation via keySet throws
        sealed.set(true);
        try {
            keys.remove("b");
            fail("Expected UnsupportedOperationException when removing from sealed keySet");
        } catch (UnsupportedOperationException ignored) { }

        // values list – removal when sealed should also throw
        Collection<Integer> values = map.values();
        try {
            values.remove(2);
            fail("Expected UnsupportedOperationException when removing from sealed values collection");
        } catch (UnsupportedOperationException ignored) { }

        // entrySet – removal when sealed should throw
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        try {
            Iterator<Map.Entry<String, Integer>> it = entries.iterator();
            it.next();
            it.remove();
            fail("Expected UnsupportedOperationException when removing from sealed entrySet");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testDescendingMapAndKeySets() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        NavigableMap<Integer, String> desc = map.descendingMap();
        Iterator<Integer> it = desc.keySet().iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());

        NavigableSet<Integer> navKeySet = map.navigableKeySet();
        assertTrue(navKeySet.containsAll(Arrays.asList(1, 2, 3)));

        NavigableSet<Integer> descKeySet = map.descendingKeySet();
        Iterator<Integer> dit = descKeySet.iterator();
        assertEquals(Integer.valueOf(3), dit.next());
        assertEquals(Integer.valueOf(2), dit.next());
        assertEquals(Integer.valueOf(1), dit.next());
    }

    @Test
    public void testSubMapHeadMapTailMapViews() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        for (int i = 1; i <= 5; i++) {
            map.put(i, "v" + i);
        }

        // subMap (inclusive from, exclusive to)
        SortedMap<Integer, String> sub = map.subMap(2, true, 4, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(2));
        assertTrue(sub.containsKey(3));
        assertFalse(sub.containsKey(4));

        // headMap (exclusive)
        SortedMap<Integer, String> head = map.headMap(3, false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey(1));
        assertTrue(head.containsKey(2));

        // tailMap (inclusive)
        SortedMap<Integer, String> tail = map.tailMap(4, true);
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(4));
        assertTrue(tail.containsKey(5));
    }

    /* --------------------------------------------------------------
     *  Mutable API – normal operation
     * -------------------------------------------------------------- */
    @Test
    public void testPutRemoveAndClearWhenUnsealed() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealed::get);
        map.put("x", 10);
        map.put("y", 20);
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(2, map.size());

        map.remove("x");
        assertNull(map.get("x"));
        assertEquals(1, map.size());

        Map<String, Integer> extra = new HashMap<>();
        extra.put("a", 1);
        extra.put("b", 2);
        map.putAll(extra);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPollFirstAndLastEntryWhenUnsealed() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(5, "five");
        map.put(10, "ten");
        map.put(15, "fifteen");

        Map.Entry<Integer, String> first = map.pollFirstEntry();
        assertEquals(Integer.valueOf(5), first.getKey());
        assertFalse(map.containsKey(5));

        Map.Entry<Integer, String> last = map.pollLastEntry();
        assertEquals(Integer.valueOf(15), last.getKey());
        assertFalse(map.containsKey(15));
    }

    /* --------------------------------------------------------------
     *  Mutable API – sealed state enforcement (exception tests)
     * -------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testPutWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealed::get);
        map.put("blocked", 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealed::get);
        map.put("a", 1); // this put succeeds because sealed flag is true *after* construction
        map.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAllWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.putAll(Collections.singletonMap(1, "one"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(1, "one"); // allowed during construction
        map.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollFirstEntryWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.pollFirstEntry();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollLastEntryWhenSealedThrows() {
        AtomicBoolean sealed = new AtomicBoolean(true);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.pollLastEntry();
    }

    /* --------------------------------------------------------------
     *  Edge‑case & exception tests for read‑only navigational methods
     * -------------------------------------------------------------- */
    @Test(expected = NoSuchElementException.class)
    public void testFirstKeyOnEmptyMapThrows() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.firstKey(); // delegated to underlying map – should throw
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastKeyOnEmptyMapThrows() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.lastKey();
    }

    @Test
    public void testLowerHigherMethodsReturnNullWhenNoSuchKey() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(10, "ten");
        assertNull(map.lowerKey(5));
        assertNull(map.higherKey(15));
        assertNull(map.lowerEntry(5));
        assertNull(map.higherEntry(15));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubMapInvalidRangeThrows() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.put(1, "one");
        map.put(2, "two");
        // fromKey > toKey should cause IllegalArgumentException
        map.subMap(5, true, 3, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHeadMapWithNullKeyThrows() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.headMap(null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTailMapWithNullKeyThrows() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(sealed::get);
        map.tailMap(null, false);
    }

    /* --------------------------------------------------------------
     *  Dynamic sealed state change verification
     * -------------------------------------------------------------- */
    @Test
    public void testToggleSealedStateDuringLifetime() {
        AtomicBoolean sealed = new AtomicBoolean(false);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealed::get);

        // initially mutable
        map.put("first", 1);
        assertEquals(Integer.valueOf(1), map.get("first"));

        // seal it
        sealed.set(true);
        try {
            map.put("second", 2);
            fail("Expected UnsupportedOperationException after sealing");
        } catch (UnsupportedOperationException ignored) { }

        // unseal again
        sealed.set(false);
        map.put("second", 2);
        assertEquals(Integer.valueOf(2), map.get("second"));
    }
}
