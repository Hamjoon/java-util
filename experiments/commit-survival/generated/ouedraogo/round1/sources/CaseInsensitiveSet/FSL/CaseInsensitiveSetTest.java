package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 * The tests exercise every public method, including the deprecated
 * plus/minus helpers, and verify the case‑insensitive behaviour for
 * {@link String} elements while preserving the original case in iteration.
 */
public class CaseInsensitiveSetTest {

    /* ---------------------------------------------------------------------- */
    /* Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /** Computes the expected hash code using the same algorithm as the class under test. */
    private static int expectedHashCode(Collection<?> items) {
        int hash = 0;
        for (Object item : items) {
            if (item != null) {
                if (item instanceof String) {
                    hash += StringUtilities.hashCodeIgnoreCase((String) item);
                } else {
                    hash += item.hashCode();
                }
            }
        }
        return hash;
    }

    /** Convenience method to create a Set from var‑args (preserves insertion order for deterministic tests). */
    @SafeVarargs
    private static <T> Collection<T> asCollection(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor tests                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorWithTreeSet() {
        TreeSet<String> source = new TreeSet<>();
        source.addAll(Arrays.asList("Alpha", "beta", "GAMMA"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // case‑insensitive contains checks
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("BETA"));
        assertTrue(set.contains("gamma"));
        // original case is retained when iterated
        assertTrue(set.iterator().next().equals("Alpha") ||
                   set.iterator().next().equals("beta") ||
                   set.iterator().next().equals("GAMMA"));
        assertEquals(3, set.size());
    }

    @Test
    public void testCollectionConstructorWithConcurrentSkipListSet() {
        ConcurrentSkipListSet<String> source = new ConcurrentSkipListSet<>();
        source.addAll(Arrays.asList("One", "Two"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertTrue(set.contains("one"));
        assertTrue(set.contains("TWO"));
        assertEquals(2, set.size());
    }

    @Test
    public void testConstructorWithBackingMap() {
        // Use a simple HashMap as a backing map – the constructor accepts any Map.
        java.util.Map<Object, Object> backingMap = new java.util.HashMap<>();
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(asCollection("X", "y"), backingMap);
        assertEquals(2, set.size());
        assertTrue(set.contains("x"));
        assertTrue(set.contains("Y"));
        // Verify that the backing map was indeed used (size matches)
        assertEquals(2, backingMap.size());
    }

    /* ---------------------------------------------------------------------- */
    /* Basic add / contains / remove                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertTrue(set.add("Foo"));
        assertEquals(1, set.size());

        // Different case should be considered the same element
        assertFalse(set.add("foo"));   // duplicate – no change
        assertEquals(1, set.size());
        assertTrue(set.contains("FOO"));
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("FoO"));

        // Original case is retained in iteration
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("Foo", it.next());
    }

    @Test
    public void testAddNullAndRemoveNull() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertTrue(set.add(null));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));

        // Adding another null does not increase size
        assertFalse(set.add(null));
        assertEquals(1, set.size());

        // Remove null
        assertTrue(set.remove(null));
        assertEquals(0, set.size());
        assertFalse(set.contains(null));
    }

    @Test
    public void testRemoveElement() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        assertTrue(set.remove("beta")); // case‑insensitive removal
        assertEquals(2, set.size());
        assertFalse(set.contains("BETA"));
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("Gamma"));
    }

    /* ---------------------------------------------------------------------- */
    /* Bulk operations (addAll, containsAll, retainAll, removeAll)            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAllAndContainsAll() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        Collection<String> toAdd = Arrays.asList("One", "Two", "Three");
        assertTrue(set.addAll(toAdd));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("one", "TWO")));
        assertFalse(set.containsAll(Arrays.asList("one", "four")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("Apple", "Banana", "Cherry", "Date"));
        // Retain only a subset, using different cases
        boolean changed = set.retainAll(Arrays.asList("banana", "CHERRY"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("Banana"));
        assertTrue(set.contains("Cherry"));
        assertFalse(set.contains("Apple"));
        assertFalse(set.contains("Date"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("red", "green", "blue"));
        boolean changed = set.removeAll(Arrays.asList("GREEN", "Yellow"));
        assertTrue(changed); // only "green" was present
        assertEquals(2, set.size());
        assertTrue(set.contains("red"));
        assertTrue(set.contains("blue"));
        assertFalse(set.contains("green"));
    }

    @Test
    public void testClearEmptiesSet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* ---------------------------------------------------------------------- */
    /* toArray variants                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArrayReturnsAllElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("x", "y", "z")));
    }

    @Test
    public void testToArrayTypedReturnsAllElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("a", "b"));
        String[] typed = set.toArray(new String[0]);
        assertEquals(2, typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("a", "b")));
    }

    /* ---------------------------------------------------------------------- */
    /* equals and hashCode                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsWithDifferentSetImplementations() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(Arrays.asList("One", "Two"));

        HashSet<String> hashSet = new HashSet<>();
        hashSet.addAll(Arrays.asList("ONE", "two"));

        assertTrue(ciSet.equals(hashSet));
        assertTrue(hashSet.equals(ciSet)); // HashSet's equals uses containsAll as well
    }

    @Test
    public void testEqualsReflexiveAndNegativeCases() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("alpha");
        assertTrue(set.equals(set)); // reflexive
        assertFalse(set.equals(null));
        assertFalse(set.equals("not a set"));
        // Different size
        CaseInsensitiveSet<String> other = new CaseInsensitiveSet<>();
        other.add("alpha");
        other.add("beta");
        assertFalse(set.equals(other));
    }

    @Test
    public void testHashCodeIsCaseInsensitiveForStrings() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add("Foo");
        set.add("Bar");
        set.add(42); // non‑String element

        int expected = expectedHashCode(Arrays.asList("Foo", "Bar", 42));
        assertEquals(expected, set.hashCode());

        // Adding the same strings with different case must NOT change the hash
        set.add("foo");
        set.add("BAR");
        assertEquals(expected, set.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /* Deprecated plus/minus helpers                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedPlusAndMinusMethods() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // plus(Object)
        set.plus("Alpha");
        assertTrue(set.contains("alpha"));
        // plus(Iterable)
        set.plus(Arrays.asList("Beta", "Gamma"));
        assertTrue(set.containsAll(Arrays.asList("beta", "gamma")));

        // minus(Object)
        set.minus("beta");
        assertFalse(set.contains("beta"));
        // minus(Iterable)
        set.minus(Arrays.asList("Alpha", "delta")); // "delta" not present
        assertFalse(set.contains("alpha"));
        assertTrue(set.contains("gamma"));
    }

    /* ---------------------------------------------------------------------- */
    /* toString representation                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringReflectsCurrentContents() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("first", "second"));
        String repr = set.toString();
        // The string must contain both elements (order not guaranteed)
        assertTrue(repr.contains("first"));
        assertTrue(repr.contains("second"));
        assertTrue(repr.startsWith("[") && repr.endsWith("]"));
    }
}
