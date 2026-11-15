package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_Test {

    @Test
    void testToArrayWithoutNulls() {
        // GIVEN a set containing distinct non‑null elements
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        // WHEN converting to an Object[] array
        Object[] array = set.toArray();
        // THEN the array length matches the set size and contains the same elements
        assertEquals(3, array.length, "Array length should equal set size");
        Set<Object> result = new HashSet<>(Arrays.asList(array));
        assertTrue(result.containsAll(Arrays.asList("x", "y", "z")), "Array should contain all elements added to the set");
    }

    @Test
    void testToArrayWithNullSentinel() {
        // GIVEN a set that includes a null element (handled internally by a sentinel)
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));
        // WHEN converting to an Object[] array
        Object[] array = set.toArray();
        // THEN the array length reflects all three entries and the null is preserved
        assertEquals(3, array.length, "Array should have three elements");
        assertTrue(Arrays.asList(array).contains(null), "Array must contain the null element");
        assertTrue(Arrays.asList(array).containsAll(Arrays.asList("a", "b")), "Array should contain the non‑null elements as well");
        // AND WHEN converting to a typed array
        String[] typedArray = set.toArray(new String[0]);
        // THEN the typed array also preserves the null entry
        assertEquals(3, typedArray.length, "Typed array should have three elements");
        assertTrue(Arrays.asList(typedArray).contains(null), "Typed array must contain the null element");
        assertTrue(Arrays.asList(typedArray).containsAll(Arrays.asList("a", "b")), "Typed array should contain the non‑null elements as well");
    }

    /**
     * Group: toArray_emptySet
     * <p>
     * Verifies that a freshly created {@code ConcurrentSet} (no elements added)
     * returns an empty {@code Object[]} when {@code toArray()} is invoked.
     */
    @Test
    void testToArrayEmptySet() {
        // GIVEN a newly instantiated ConcurrentSet with no elements
        ConcurrentSet<String> emptySet = new ConcurrentSet<>();
        // WHEN converting to an Object[] array
        Object[] result = emptySet.toArray();
        // THEN the returned array is empty (length 0)
        assertNotNull(result, "Resulting array should not be null");
        assertEquals(0, result.length, "Array length should be 0 for an empty set");
    }

    /**
     * Group: toArray_nullInternalSet
     * <p>
     * Simulates a corrupted internal state where the backing {@code set} field is {@code null}.
     * The method is expected to throw a {@link NullPointerException} because it attempts to
     * delegate to {@code set.toArray()}.
     *
     * This test uses reflection to set the private {@code set} field to {@code null}.
     */
    @Test
    void testToArrayWhenInternalSetIsNull() throws Exception {
        // GIVEN a ConcurrentSet instance whose internal backing set is forced to null
        ConcurrentSet<String> corruptedSet = new ConcurrentSet<>();
        // Use reflection to access the private 'set' field and assign null
        Field internalSetField = ConcurrentSet.class.getDeclaredField("set");
        internalSetField.setAccessible(true);
        internalSetField.set(corruptedSet, null);
        // WHEN invoking toArray(), THEN a NullPointerException should be thrown
        assertThrows(NullPointerException.class, corruptedSet::toArray, "Calling toArray() when internal set is null should throw NullPointerException");
    }
}
