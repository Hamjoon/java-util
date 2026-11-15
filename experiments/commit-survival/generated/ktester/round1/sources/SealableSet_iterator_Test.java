package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;

@ExtendWith(MockitoExtension.class)
class SealableSet_iterator_Test {

    /**
     * The set used for the primitive‑type iterator tests.
     */
    private SealableSet<Integer> intSet;

    /**
     * The set used for the Map.Entry iterator test.
     */
    private SealableSet<Map.Entry<String, String>> entrySet;

    /**
     * Controls the sealed state supplied to the SealableSet instances.
     */
    private volatile boolean sealed;

    /**
     * Supplier that forwards the current value of {@code sealed}.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with an un‑sealed set
        sealed = false;
        // ----- integer set -------------------------------------------------
        intSet = new SealableSet<>(sealedSupplier);
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        // ----- entry set ---------------------------------------------------
        Set<Map.Entry<String, String>> backing = new HashSet<>();
        backing.add(new AbstractMap.SimpleEntry<>("key1", "value1"));
        entrySet = new SealableSet<>(backing, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the pattern tidy.
        intSet = null;
        entrySet = null;
    }

    @Test
    void iteratorTraversesAllElements() {
        Iterator<Integer> it = intSet.iterator();
        // The set contains exactly three elements (10, 20 and null) – order is
        // unspecified, so we just verify that we see three distinct calls to
        // next() and then the iterator reports no more elements.
        assertTrue(it.hasNext());
        int seen = 0;
        while (it.hasNext()) {
            Integer v = it.next();
            assertTrue(v == null || v == 10 || v == 20, "Unexpected value from iterator: " + v);
            seen++;
        }
        assertEquals(3, seen, "Iterator should have visited three elements");
        // After the last element has been consumed, hasNext() must be false and
        // next() must throw NoSuchElementException.
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorRemoveIsForbiddenWhenSealed() {
        Iterator<Integer> it = intSet.iterator();
        // Move to the first element – any element is fine.
        assertTrue(it.hasNext());
        it.next();
        // Seal the set and attempt to remove via the iterator.
        sealed = true;
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() must be forbidden when the set is sealed");
    }

    @Test
    void iteratorRemoveWorksWhenUnsealed() {
        Iterator<Integer> it = intSet.iterator();
        // Remove the first element while the set is still unsealed.
        assertTrue(it.hasNext());
        Integer first = it.next();
        it.remove();
        // The removed element must no longer be present.
        assertFalse(intSet.contains(first));
        assertEquals(2, intSet.size());
    }

    @Test
    void iteratorWrapsMapEntryInSealAwareEntry() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> wrapped = it.next();
        // The iterator must return a different object than the original entry
        // because SealableSet wraps Map.Entry values in a SealAwareEntry.
        // The wrapper class is package‑private, so we verify its name via reflection.
        Class<?> wrapperClass = wrapped.getClass();
        assertTrue(wrapperClass.getSimpleName().contains("SealAwareEntry"), "Returned object should be a SealAwareEntry, but was: " + wrapperClass.getName());
        // The wrapper still implements Map.Entry and must expose the original key/value.
        assertEquals("key1", wrapped.getKey());
        assertEquals("value1", wrapped.getValue());
        // Changing the value is allowed while the set is unsealed.
        wrapped.setValue("newValue");
        assertEquals("newValue", wrapped.getValue());
        // When the set becomes sealed, attempting to modify the entry must fail.
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> wrapped.setValue("anotherValue"), "Modifying a sealed entry must throw");
    }

    @Test
    void iteratorNextThrowsWhenNoElements() {
        // Exhaust the iterator first.
        Iterator<Integer> it = intSet.iterator();
        while (it.hasNext()) {
            it.next();
        }
        // Now the iterator is empty – next() must throw.
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorHasNextBehaviorOnEmptySet() {
        // Create a SealableSet backed by an empty HashSet.
        SealableSet<String> emptySet = new SealableSet<>(new HashSet<>(), sealedSupplier);
        Iterator<String> it = emptySet.iterator();
        // hasNext must be false immediately and next() must throw.
        assertFalse(it.hasNext(), "hasNext() should be false for an empty set");
        assertThrows(NoSuchElementException.class, it::next, "next() must throw on empty iterator");
    }

    @Test
    void iteratorNextReturnsNonMapEntryUnchanged() {
        // Back the SealableSet with a Set<String> containing a single value.
        Set<String> backing = new HashSet<>();
        backing.add("alpha");
        SealableSet<String> stringSet = new SealableSet<>(backing, sealedSupplier);
        Iterator<String> it = stringSet.iterator();
        assertTrue(it.hasNext());
        String value = it.next();
        assertEquals("alpha", value, "Iterator should return the original element unchanged");
        // Since the element is a plain String, it must be the same instance stored in the backing set.
        // (HashSet does not guarantee identity, but for a single element the returned reference is the stored one.)
        assertTrue(backing.contains(value));
    }

    @Test
    void iteratorMixedElementTypesPreservesOrderAndWrapping() throws Exception {
        // LinkedHashSet preserves insertion order.
        LinkedHashSet<Object> mixedBacking = new LinkedHashSet<>();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("k", "v");
        mixedBacking.add(entry);
        mixedBacking.add("foo");
        // SealableSet must be constructed with the same generic type (Object) to hold both.
        @SuppressWarnings("unchecked")
        SealableSet<Object> mixedSet = new SealableSet<>((Set<Object>) mixedBacking, sealedSupplier);
        Iterator<Object> it = mixedSet.iterator();
        // First element should be a wrapped SealAwareEntry.
        assertTrue(it.hasNext());
        Object first = it.next();
        assertTrue(first instanceof Map.Entry, "First element must implement Map.Entry");
        Class<?> firstClass = first.getClass();
        assertTrue(firstClass.getSimpleName().contains("SealAwareEntry"), "First element should be a SealAwareEntry wrapper, but was: " + firstClass.getName());
        // Verify the wrapped entry still returns the original key/value.
        @SuppressWarnings("unchecked")
        Map.Entry<String, String> wrappedEntry = (Map.Entry<String, String>) first;
        assertEquals("k", wrappedEntry.getKey());
        assertEquals("v", wrappedEntry.getValue());
        // Second element should be the raw String "foo".
        assertTrue(it.hasNext());
        Object second = it.next();
        assertTrue(second instanceof String, "Second element should be the original String");
        assertEquals("foo", second);
        // No more elements.
        assertFalse(it.hasNext());
    }
}
