package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Read‑only behavior when the map is sealed (mutating methods must throw).</li>
 *   <li>Normal mutable behavior when the map is unsealed.</li>
 *   <li>Delegation of {@code equals}, {@code hashCode} and {@code toString} to the wrapped map.</li>
 *   <li>Correct operation of the view collections {@code keySet()}, {@code values()} and {@code entrySet()}
 *       with respect to the sealed state.</li>
 *   <li>Wrapping an existing {@code Map} instance.</li>
 * </ul>
 */
public class SealableMapTest {

    /** Simple mutable supplier that can be toggled between sealed / unsealed. */
    private static class ToggleSupplier implements Supplier<Boolean> {
        private final AtomicBoolean flag = new AtomicBoolean(false);
        void setSealed(boolean sealed) { flag.set(sealed); }
        @Override public Boolean get() { return flag.get(); }
    }

    private ToggleSupplier sealedSupplier;
    private SealableMap<String, Integer> sealableMap;

    @Before
    public void setUp() {
        sealedSupplier = new ToggleSupplier();
        // start unsealed for most tests; individual tests may change the state
        sealedSupplier.setSealed(false);
        sealableMap = new SealableMap<>(sealedSupplier);
    }

    /** Helper to populate the map with a few entries. */
    private void populateMap() {
        sealableMap.put("one",   1);
        sealableMap.put("two",   2);
        sealableMap.put("three", 3);
    }

    @Test
    public void testMutableOperationsWhenUnsealed() {
        // map is initially unsealed
        sealableMap.put("a", 10);
        sealableMap.put("b", 20);
        assertEquals(2, sealableMap.size());
        assertTrue(sealableMap.containsKey("a"));
        assertTrue(sealableMap.containsValue(20));
        assertEquals(Integer.valueOf(10), sealableMap.get("a"));

        // remove works
        Integer removed = sealableMap.remove("a");
        assertEquals(Integer.valueOf(10), removed);
        assertFalse(sealableMap.containsKey("a"));
        assertEquals(1, sealableMap.size());

        // putAll works
        Map<String, Integer> other = new HashMap<>();
        other.put("c", 30);
        other.put("d", 40);
        sealableMap.putAll(other);
        assertEquals(3, sealableMap.size());
        assertTrue(sealableMap.containsKey("c"));
        assertTrue(sealableMap.containsKey("d"));

        // clear works
        sealableMap.clear();
        assertTrue(sealableMap.isEmpty());
    }

    @Test
    public void testMutatingOperationsThrowWhenSealed() {
        populateMap();
        sealedSupplier.setSealed(true); // seal the map

        // put
        try {
            sealableMap.put("new", 99);
            fail("Expected UnsupportedOperationException on put when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // remove
        try {
            sealableMap.remove("one");
            fail("Expected UnsupportedOperationException on remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // putAll
        try {
            sealableMap.putAll(Collections.singletonMap("x", 1));
            fail("Expected UnsupportedOperationException on putAll when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // clear
        try {
            sealableMap.clear();
            fail("Expected UnsupportedOperationException on clear when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testKeySetViewRespectsSealedState() {
        populateMap();

        // keySet should allow removal when unsealed
        Set<String> keys = sealableMap.keySet();
        assertTrue(keys.remove("one"));
        assertFalse(sealableMap.containsKey("one"));
        assertEquals(2, sealableMap.size());

        // add a key back for the next part
        sealableMap.put("one", 1);
        assertTrue(sealableMap.containsKey("one"));

        // now seal and attempt a mutating operation on the view
        sealedSupplier.setSealed(true);
        try {
            keys.remove("two");
            fail("Expected UnsupportedOperationException from keySet.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testValuesViewRespectsSealedState() {
        populateMap();

        Collection<Integer> values = sealableMap.values();

        // remove a value when unsealed
        assertTrue(values.remove(2));
        assertFalse(sealableMap.containsValue(2));
        assertEquals(2, sealableMap.size());

        // put the value back
        sealableMap.put("two", 2);
        assertTrue(sealableMap.containsValue(2));

        // seal and attempt to modify via the view
        sealedSupplier.setSealed(true);
        try {
            values.remove(3);
            fail("Expected UnsupportedOperationException from values.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testEntrySetViewRespectsSealedState() {
        populateMap();

        Set<Map.Entry<String, Integer>> entries = sealableMap.entrySet();

        // remove an entry via iterator when unsealed
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("three".equals(e.getKey())) {
                it.remove();
                break;
            }
        }
        assertFalse(sealableMap.containsKey("three"));
        assertEquals(2, sealableMap.size());

        // add back for sealed test
        sealableMap.put("three", 3);
        assertTrue(sealableMap.containsKey("three"));

        // seal and attempt to remove via iterator
        sealedSupplier.setSealed(true);
        it = entries.iterator();
        try {
            while (it.hasNext()) {
                it.next();
                it.remove(); // should fail on first call
            }
            fail("Expected UnsupportedOperationException from entrySet iterator.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testEqualsAndHashCodeDelegateToWrappedMap() {
        populateMap();

        Map<String, Integer> plainMap = new HashMap<>();
        plainMap.put("one", 1);
        plainMap.put("two", 2);
        plainMap.put("three", 3);

        assertTrue(sealableMap.equals(plainMap));
        assertTrue(plainMap.equals(sealableMap));
        assertEquals(plainMap.hashCode(), sealableMap.hashCode());
    }

    @Test
    public void testToStringDelegatesToWrappedMap() {
        populateMap();
        String expected = "{one=1, two=2, three=3}";
        // The underlying ConcurrentHashMapNullSafe uses the same toString format as a normal map.
        // Since map iteration order is not guaranteed, we verify that the string contains the expected pairs.
        String actual = sealableMap.toString();
        for (String pair : new String[] {"one=1", "two=2", "three=3"}) {
            assertTrue("toString should contain '" + pair + "': " + actual, actual.contains(pair));
        }
        // Ensure the string starts with '{' and ends with '}'
        assertTrue(actual.startsWith("{"));
        assertTrue(actual.endsWith("}"));
    }

    @Test
    public void testWrappingExistingMapReflectsExternalChanges() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("alpha", 100);
        sealingMapWrapperTest(backing);
    }

    /** Helper that creates a SealableMap wrapping the supplied map and runs assertions. */
    private void sealingMapWrapperTest(Map<String, Integer> backing) {
        sealedSupplier.setSealed(false);
        SealableMap<String, Integer> wrapper = new SealableMap<>(backing, sealedSupplier);

        // wrapper sees the initial entry
        assertEquals(Integer.valueOf(100), wrapper.get("alpha"));
        assertEquals(1, wrapper.size());

        // mutate via wrapper
        wrapper.put("beta", 200);
        assertEquals(Integer.valueOf(200), backing.get("beta"));
        assertEquals(2, backing.size());

        // seal and verify wrapper cannot mutate, but external changes are still visible
        sealedSupplier.setSealed(true);
        try {
            wrapper.put("gamma", 300);
            fail("Expected UnsupportedOperationException on put when sealed (wrapped map)");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // external modification while sealed
        backing.put("delta", 400);
        assertEquals(Integer.valueOf(400), wrapper.get("delta"));
        assertEquals(3, wrapper.size());
    }

    @Test
    public void testSizeAndIsEmptyBehaveCorrectly() {
        assertTrue(sealableMap.isEmpty());
        assertEquals(0, sealableMap.size());

        sealableMap.put("k", 1);
        assertFalse(sealableMap.isEmpty());
        assertEquals(1, sealableMap.size());

        sealableMap.clear();
        assertTrue(sealableMap.isEmpty());
        assertEquals(0, sealableMap.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        sealableMap.put("x", 9);
        sealableMap.put("y", 10);
        assertTrue(sealableMap.containsKey("x"));
        assertFalse(sealableMap.containsKey("z"));
        assertTrue(sealableMap.containsValue(10));
        assertFalse(sealableMap.containsValue(99));
    }

    @Test
    public void testGetReturnsNullForMissingKey() {
        assertNull(sealableMap.get("nonexistent"));
        sealableMap.put("present", 5);
        assertNull(sealableMap.get("absent"));
    }
}
