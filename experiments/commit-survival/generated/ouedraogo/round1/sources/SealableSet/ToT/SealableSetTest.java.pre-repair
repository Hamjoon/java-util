package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableSet}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All constructors</li>
 *   <li>Immutable operations (delegated to the backing set)</li>
 *   <li>Mutable operations when the set is sealed vs. unsealed</li>
 *   <li>Iterator behaviour, including {@code remove()} and {@code SealAwareEntry}</li>
 *   <li>Edge cases such as empty collections, duplicate adds, and {@code toArray(...)} contracts</li>
 * </ul>
 */
public class SealableSetTest {

    /** Helper that can be toggled to simulate sealing / unsealing. */
    private static class ToggleSupplier implements Supplier<Boolean> {
        private final AtomicBoolean sealed = new AtomicBoolean(false);
        void setSealed(boolean value) { sealed.set(value); }
        @Override public Boolean get() { return sealed.get(); }
    }

    private ToggleSupplier sealedSupplier;

    @Before
    public void setUp() {
        sealedSupplier = new ToggleSupplier(); // starts unsealed
    }

    /** --------------------------------------------------------------------- *
     *  Constructor tests
     *  --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesMutableSet() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.isEmpty());
        assertTrue(set.add("one"));
        assertEquals(1, set.size());
        assertTrue(set.contains("one"));
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<Integer> source = Arrays.asList(1, 2, 3, 2);
        SealableSet<Integer> set = new SealableSet<>(source, sealedSupplier);
        // Duplicates are removed because underlying Set is a Set
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testWrappingConstructorOperatesOnOriginalSet() {
        Set<String> backing = new HashSet<>(Arrays.asList("a", "b"));
        SealableSet<String> wrapper = new SealableSet<>(backing, sealedSupplier);
        assertEquals(2, wrapper.size());
        // Mutate through wrapper – should affect the original backing set
        wrapper.add("c");
        assertTrue(backing.contains("c"));
        // Mutate backing directly – should be visible through wrapper
        backing.remove("a");
        assertFalse(wrapper.contains("a"));
    }

    /** --------------------------------------------------------------------- *
     *  Immutable API delegation tests
     *  --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeDelegateToBackingSet() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(5, 6));
        SealableSet<Integer> sealable = new SealableSet<>(backing, sealedSupplier);

        // Equality with a plain Set that has the same elements
        Set<Integer> plain = new HashSet<>(Arrays.asList(5, 6));
        assertTrue(sealable.equals(plain));
        assertEquals(plain.hashCode(), sealable.hashCode());

        // Equality with another SealableSet wrapping the *same* backing set
        SealableSet<Integer> other = new SealableSet<>(backing, sealedSupplier);
        // The underlying set equals the other SealableSet? No – because the other is not a Set.
        // Therefore equals should be false (as per Set.equals contract).
        assertFalse(sealable.equals(other));
    }

    @Test
    public void testToStringReflectsBackingSet() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("x", "y"));
        String s = set.toString();
        // The exact order is unspecified; just verify it contains both elements and brackets.
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("x"));
        assertTrue(s.contains("y"));
    }

    @Test
    public void testContainsAllAndIsEmpty() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.isEmpty());

        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty());

        assertTrue(set.containsAll(Arrays.asList("a", "c")));
        assertFalse(set.containsAll(Arrays.asList("a", "d")));
    }

    @Test
    public void testToArrayAndToArrayGeneric() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList(10, 20, 30));

        Object[] objArray = set.toArray();
        assertEquals(3, objArray.length);
        assertTrue(Arrays.asList(objArray).containsAll(Arrays.asList(10, 20, 30)));

        Integer[] intArray = set.toArray(new Integer[0]);
        assertEquals(3, intArray.length);
        List<Integer> asList = Arrays.asList(intArray);
        assertTrue(asList.containsAll(Arrays.asList(10, 20, 30)));
    }

    /** --------------------------------------------------------------------- *
     *  Mutable operations – unsealed (should succeed)
     *  --------------------------------------------------------------------- */

    @Test
    public void testAddRemoveWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.add("alpha"));
        assertTrue(set.contains("alpha"));
        assertTrue(set.remove("alpha"));
        assertFalse(set.contains("alpha"));
    }

    @Test
    public void testAddAllRemoveAllRetainAllWhenUnsealed() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList(1, 2, 3, 4));

        // addAll with duplicates – should ignore existing ones
        assertFalse(set.addAll(Arrays.asList(2, 3)));

        // removeAll
        assertTrue(set.removeAll(Arrays.asList(1, 4)));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(2, 3)));

        // retainAll – keep only 2
        assertTrue(set.retainAll(Collections.singleton(2)));
        assertEquals(1, set.size());
        assertTrue(set.contains(2));
    }

    @Test
    public void testClearWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("a", "b"));
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testIteratorRemoveWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("one", "two"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("one".equals(v)) {
                it.remove();
            }
        }
        assertFalse(set.contains("one"));
        assertTrue(set.contains("two"));
    }

    /** --------------------------------------------------------------------- *
     *  Mutable operations – sealed (should throw)
     *  --------------------------------------------------------------------- */

    private void seal() { sealedSupplier.setSealed(true); }
    private void unseal() { sealedSupplier.setSealed(false); }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.add("first");
        seal();
        set.add("second"); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.add("elem");
        seal();
        set.remove("elem");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.add(1);
        seal();
        set.addAll(Arrays.asList(2, 3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList(1, 2, 3));
        seal();
        set.removeAll(Arrays.asList(1, 2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList(1, 2, 3));
        seal();
        set.retainAll(Collections.singleton(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.add("x");
        seal();
        set.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("a", "b"));
        seal();
        Iterator<String> it = set.iterator();
        it.next(); // advance
        it.remove(); // should throw
    }

    /** --------------------------------------------------------------------- *
     *  Iterator – SealAwareEntry handling
     *  --------------------------------------------------------------------- */

    @Test
    public void testIteratorWrapsMapEntryInSealAwareEntry() {
        // Build a Set of Map.Entry objects
        Set<Map.Entry<String, Integer>> entrySet = new HashSet<>();
        entrySet.add(new AbstractMap.SimpleEntry<>("k1", 100));
        entrySet.add(new AbstractMap.SimpleEntry<>("k2", 200));

        SealableSet<Map.Entry<String, Integer>> sealable = new SealableSet<>(entrySet, sealedSupplier);
        List<Map.Entry<String, Integer>> iterated = new ArrayList<>();

        for (Map.Entry<String, Integer> e : sealable) {
            // The iterator should have wrapped the entry in SealAwareEntry
            assertTrue(e instanceof SealableSet.SealAwareEntry);
            iterated.add(e);
        }

        // Verify that the wrapped entries expose the original key/value
        Map<String, Integer> asMap = new HashMap<>();
        for (Map.Entry<String, Integer> e : iterated) {
            asMap.put(e.getKey(), e.getValue());
        }
        assertEquals(Integer.valueOf(100), asMap.get("k1"));
        assertEquals(Integer.valueOf(200), asMap.get("k2"));
    }

    @Test
    public void testSealAwareEntrySetValueRespectsSealState() {
        Map.Entry<String, String> raw = new AbstractMap.SimpleEntry<>("alpha", "first");
        Set<Map.Entry<String, String>> backing = new HashSet<>();
        backing.add(raw);

        SealableSet<Map.Entry<String, String>> set = new SealableSet<>(backing, sealedSupplier);
        Iterator<Map.Entry<String, String>> it = set.iterator();
        Map.Entry<String, String> wrapped = it.next(); // should be SealAwareEntry

        // Unsealed – setValue should succeed
        unseal();
        String old = wrapped.setValue("second");
        assertEquals("first", old);
        assertEquals("second", raw.getValue());

        // Seal and try again – should throw
        seal();
        try {
            wrapped.setValue("third");
            fail("Expected UnsupportedOperationException when sealed");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    /** --------------------------------------------------------------------- *
     *  Dynamic sealing – ensure state can be toggled at any time
     *  --------------------------------------------------------------------- */

    @Test
    public void testDynamicSealingAllowsStateChangeBetweenOperations() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        // start unsealed
        assertTrue(set.add(10));

        // seal – further mutation forbidden
        seal();
        try {
            set.add(20);
            fail("add should have thrown while sealed");
        } catch (UnsupportedOperationException ignored) {}

        // unseal again – mutation works again
        unseal();
        assertTrue(set.add(20));
        assertTrue(set.containsAll(Arrays.asList(10, 20)));
    }

    /** --------------------------------------------------------------------- *
     *  Misc edge cases
     *  --------------------------------------------------------------------- */

    @Test
    public void testAddNullWhenBackingSetAllowsIt() {
        // ConcurrentSet (backed by ConcurrentHashMap) does NOT allow null, but this test
        // documents the observed behaviour. If the implementation changes, the test will
        // surface the difference.
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        try {
            set.add(null);
            // Some Set implementations permit null – just verify the set now contains null.
            assertTrue(set.contains(null));
        } catch (NullPointerException | IllegalArgumentException e) {
            // Acceptable alternative – the Set rejected null.
        }
    }

    @Test
    public void testIteratorHasNextDoesNotConsumeElements() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("x", "y"));
        Iterator<String> it = set.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.hasNext()); // second call should still be true
        String first = it.next();
        assertNotNull(first);
    }
}
