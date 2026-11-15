package com.cedarsoftware.util;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_toArray_2_Test {

    /**
     * Set used by most test cases – populated in {@link #setupBeforeEach()}
     */
    private CompactSet<String> set;

    /**
     * The backing map – the value is a dummy placeholder.
     */
//     private final CompactMap<E, Object> map;

    /**
     * Dummy value used for map entries – the actual value is irrelevant.
     */
    private static final Object DUMMY = new Object();

    /**
     * Builder class – only a subset is needed for the tests.
     */
    public static final class Builder<E> {

        public Builder<E> caseSensitive(boolean flag) {
            return this;
        }

        public Builder<E> sortedOrder() {
            return this;
        }

        public Builder<E> insertionOrder() {
            return this;
        }

        public Builder<E> compactSize(int size) {
            return this;
        }

        public CompactSet<E> build() {
            return new CompactSet<>();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet for each test to guarantee isolation
        set = new CompactSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set so that any lingering state does not affect the next test
        set.clear();
    }

    /**
     * Builder entry point – mirrors the real library’s builder pattern.
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    /**
     * The supplied array has exactly the same length as the set – the same
     *  array instance must be returned and filled with the set elements.
     */
    @Test
    void testToArrayExactSize() {
        String[] supplied = new String[3];
        String[] result = set.toArray(supplied);
        assertSame(supplied, result, "When the supplied array fits, the same instance should be returned");
        assertArrayEquals(new String[] { "apple", "banana", "cherry" }, result);
    }

    /**
     * The supplied array is larger than the set – the method must return the
     *  supplied array, copy the elements, and set the first unused slot to null.
     */
    @Test
    void testToArrayLargerArray() {
        String[] supplied = new String[5];
        String[] result = set.toArray(supplied);
        assertSame(supplied, result, "When the supplied array is larger, it should be returned unchanged");
        assertEquals(5, result.length);
        assertArrayEquals(new String[] { "apple", "banana", "cherry", null, null }, result);
    }

    /**
     * The supplied array is smaller than the set – a new array of the same
     *  runtime type must be allocated and returned.
     */
    @Test
    void testToArraySmallerArray() {
        String[] supplied = new String[2];
        String[] result = set.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated when the supplied one is too small");
        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "apple", "banana", "cherry" }, result);
    }

    /**
     * An empty CompactSet should return the supplied empty array unchanged.
     */
    @Test
    void testToArrayEmptySet() {
        CompactSet<Integer> empty = new CompactSet<>();
        Integer[] supplied = new Integer[0];
        Integer[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty set with empty array should return the same array instance");
        assertEquals(0, result.length);
    }

    /**
     * Passing a null array reference must result in a NullPointerException,
     *  matching the contract of {@link java.util.Collection#toArray(Object[])}.
     */
    @Test
    void testToArrayNullArrayThrowsNPE() {
        // The cast disambiguates the overloaded toArray methods (array vs IntFunction)
        assertThrows(NullPointerException.class, () -> set.toArray((String[]) null));
    }

//     @Override
//     public boolean add(E e) {
//         Objects.requireNonNull(e);
//         return map.put(e, DUMMY) == null;
//     }

//     @Override
//     public boolean contains(Object o) {
//         return map.containsKey(o);
//     }

//     @Override
//     public boolean remove(Object o) {
//         return map.remove(o) != null;
//     }

//     @Override
//     public void clear() {
//         map.clear();
//     }

//     @Override
//     public int size() {
//         return map.size();
//     }

//     @Override
//     public Iterator<E> iterator() {
//         return map.keySet().iterator();
//     }

//     @Override
//     public boolean containsAll(Collection<?> c) {
//         for (Object o : c) {
//             if (!contains(o))
//                 return false;
//         }
//         return true;
//     }

    /**
     * Returns an array containing all of the elements in this set; the
     * runtime type of the returned array is that of the supplied array.
     *
     * @param a the array into which the elements of the set are to be stored,
     *          if it is big enough; otherwise, a new array of the same runtime
     *          component type is allocated for this purpose.
     * @return an array containing the elements of the set
     * @throws NullPointerException if the supplied array is {@code null}
     */
//     @SuppressWarnings("unchecked")
//     @Override
//     public <T> T[] toArray(T[] a) {
//         Objects.requireNonNull(a, "Supplied array must not be null");
//         int size = size();
//         // If the supplied array is too small, allocate a new one of the same
//         // component type.
//         T[] result = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
//         int i = 0;
//         for (E e : this) {
//             result[i++] = (T) e;
//         }
//         // If the supplied array was larger than the set, set the first
//         // unused element to null as required by the Collection contract.
//         if (result.length > size) {
//             result[size] = null;
//         }
//         return result;
//     }

    /**
     * Returns an {@code Object[]} containing all of the elements in this set.
     * This method is required by {@link java.util.Collection}.
     *
     * @return an array containing all of the elements in this set
     */
//     @Override
//     public Object[] toArray() {
//         Object[] result = new Object[size()];
//         int i = 0;
//         for (E e : this) {
//             result[i++] = e;
//         }
//         return result;
//     }
}