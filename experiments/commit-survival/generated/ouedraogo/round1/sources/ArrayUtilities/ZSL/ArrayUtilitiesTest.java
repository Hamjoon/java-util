package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link ArrayUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null‑safety and size checks for all array types.</li>
 *   <li>Shallow copying behaviour (null handling, new instance, element identity).</li>
 *   <li>Var‑args array creation, including empty and {@code null} element handling.</li>
 *   <li>All overloads of {@code addAll} (both null, one null, empty, with null elements).</li>
 *   <li>Item removal at start, middle and end positions.</li>
 *   <li>Sub‑set extraction, including edge cases and exception handling.</li>
 *   <li>Conversion from {@link Collection} to typed arrays.</li>
 *   <li>Verification of the immutable EMPTY_* constants.</li>
 * </ul>
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  isEmpty / size                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyAndSizeWithObjectArrays() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
        Assert.assertTrue(ArrayUtilities.isEmpty(new Object[0]));
        Assert.assertFalse(ArrayUtilities.isEmpty(new Object[]{"a"}));

        Assert.assertEquals(0, ArrayUtilities.size(null));
        Assert.assertEquals(0, ArrayUtilities.size(new Object[0]));
        Assert.assertEquals(3, ArrayUtilities.size(new Object[]{1, 2, 3}));
    }

    @Test
    public void testIsEmptyAndSizeWithPrimitiveArrays() {
        int[] ints = null;
        int[] emptyInts = new int[0];
        int[] someInts = new int[]{1, 2, 3};

        // isEmpty works for any array type via reflection
        Assert.assertTrue(ArrayUtilities.isEmpty(ints));
        Assert.assertTrue(ArrayUtilities.isEmpty(emptyInts));
        Assert.assertFalse(ArrayUtilities.isEmpty(someInts));

        // size works for any array type via reflection
        Assert.assertEquals(0, ArrayUtilities.size(ints));
        Assert.assertEquals(0, ArrayUtilities.size(emptyInts));
        Assert.assertEquals(3, ArrayUtilities.size(someInts));
    }

    /* ---------------------------------------------------------------------- */
    /*  shallowCopy                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopyNull() {
        String[] result = ArrayUtilities.shallowCopy(null);
        Assert.assertNull(result);
    }

    @Test
    public void testShallowCopyCreatesNewArray() {
        String[] original = {"one", "two", "three"};
        String[] copy = ArrayUtilities.shallowCopy(original);

        // Different object reference
        Assert.assertNotSame(original, copy);
        // Same contents
        Assert.assertArrayEquals(original, copy);
        // Elements themselves are the same (shallow copy, not deep)
        Assert.assertSame(original[0], copy[0]);
    }

    @Test
    public void testShallowCopyWithMutableElements() {
        // Use a mutable holder to prove that the copy shares the same element references
        class Box {
            int value;
            Box(int v) { this.value = v; }
        }
        Box b1 = new Box(10);
        Box b2 = new Box(20);
        Box[] original = {b1, b2};

        Box[] copy = ArrayUtilities.shallowCopy(original);
        // Mutate via original reference
        original[0].value = 99;
        Assert.assertEquals(99, copy[0].value);
    }

    /* ---------------------------------------------------------------------- */
    /*  createArray                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArrayWithElements() {
        String[] arr = ArrayUtilities.createArray("a", "b", "c");
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testCreateArrayEmpty() {
        Integer[] empty = ArrayUtilities.createArray();
        Assert.assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArrayWithNullElements() {
        String[] arr = ArrayUtilities.createArray("x", null, "y");
        Assert.assertEquals(3, arr.length);
        Assert.assertNull(arr[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArrayVarargsNullArray() {
        // Passing a single null argument that is interpreted as the whole var‑args array
        ArrayUtilities.createArray((String[]) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAllBothNull() {
        String[] result = ArrayUtilities.addAll(null, null);
        Assert.assertNull(result);
    }

    @Test
    public void testAddAllFirstNullSecondNonNull() {
        String[] second = {"x", "y"};
        String[] result = ArrayUtilities.addAll(null, second);
        Assert.assertArrayEquals(second, result);
        Assert.assertNotSame(second, result); // must be a cloned copy
    }

    @Test
    public void testAddAllSecondNullFirstNonNull() {
        String[] first = {"a", "b"};
        String[] result = ArrayUtilities.addAll(first, null);
        Assert.assertArrayEquals(first, result);
        Assert.assertNotSame(first, result);
    }

    @Test
    public void testAddAllBothEmpty() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        Assert.assertArrayEquals(new String[0], result);
    }

    @Test
    public void testAddAllWithNullElements() {
        String[] a = {null, "a"};
        String[] b = {"b", null};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{null, "a", "b", null}, result);
    }

    @Test
    public void testAddAllTypicalScenario() {
        String[] left = {"1", "2", "3"};
        String[] right = {"4", "5"};
        String[] combined = ArrayUtilities.addAll(left, right);
        Assert.assertArrayEquals(new String[]{"1", "2", "3", "4", "5"}, combined);
    }

    /* ---------------------------------------------------------------------- */
    /*  removeItem                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveItemMiddle() {
        String[] source = {"a", "b", "c", "d"};
        String[] result = ArrayUtilities.removeItem(source, 2); // remove "c"
        Assert.assertArrayEquals(new String[]{"a", "b", "d"}, result);
    }

    @Test
    public void testRemoveItemFirst() {
        Integer[] source = {1, 2, 3};
        Integer[] result = ArrayUtilities.removeItem(source, 0);
        Assert.assertArrayEquals(new Integer[]{2, 3}, result);
    }

    @Test
    public void testRemoveItemLast() {
        Character[] source = {'x', 'y', 'z'};
        Character[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(new Character[]{'x', 'y'}, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  getArraySubset                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubsetFullRange() {
        String[] source = {"a", "b", "c", "d"};
        String[] subset = ArrayUtilities.getArraySubset(source, 0, source.length);
        Assert.assertArrayEquals(source, subset);
        Assert.assertNotSame(source, subset); // copy, not original
    }

    @Test
    public void testGetArraySubsetPartial() {
        String[] source = {"a", "b", "c", "d"};
        String[] subset = ArrayUtilities.getArraySubset(source, 1, 3);
        Assert.assertArrayEquals(new String[]{"b", "c"}, subset);
    }

    @Test
    public void testGetArraySubsetEmpty() {
        String[] source = {"a", "b"};
        String[] subset = ArrayUtilities.getArraySubset(source, 1, 1);
        Assert.assertEquals(0, subset.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetInvalidRange() {
        String[] source = {"x", "y"};
        // end index beyond array length should trigger the exception from Arrays.copyOfRange
        ArrayUtilities.getArraySubset(source, 0, 5);
    }

    /* ---------------------------------------------------------------------- */
    /*  toArray                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArrayWithStringClass() {
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        String[] array = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"alpha", "beta"}, array);
    }

    @Test
    public void testToArrayWithCustomClass() {
        class Person {
            final String name;
            Person(String n) { this.name = n; }
            @Override
            public boolean equals(Object o) {
                return (o instanceof Person) && ((Person) o).name.equals(this.name);
            }
        }
        List<Person> people = new ArrayList<>();
        people.add(new Person("John"));
        people.add(new Person("Jane"));

        Person[] array = ArrayUtilities.toArray(Person.class, people);
        Assert.assertArrayEquals(people.toArray(new Person[0]), array);
    }

    @Test
    public void testToArrayEnsuresArrayElementsMatchCollectionOrder() {
        Collection<Integer> col = new ArrayList<>();
        col.add(10);
        col.add(20);
        col.add(30);
        Integer[] arr = ArrayUtilities.toArray(Integer.class, col);
        Assert.assertArrayEquals(new Integer[]{10, 20, 30}, arr);
    }

    /* ---------------------------------------------------------------------- */
    /*  EMPTY_* constants                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyObjectArrayIsImmutableAndZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        // Attempting to modify should throw ArrayIndexOutOfBoundsException
        try {
            ArrayUtilities.EMPTY_OBJECT_ARRAY[0] = new Object();
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) { }
    }

    @Test
    public void testEmptyPrimitiveArraysAreZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
    }

    @Test
    public void testEmptyCharacterArrayIsZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
    }

    @Test
    public void testEmptyClassArrayIsZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
