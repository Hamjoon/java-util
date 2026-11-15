package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_listOf_Test {

    class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }

    /* --------------------------------------------------------------
     *  Helper class used for custom object tests
     * -------------------------------------------------------------- */
    static class TestObject {

        final int id;

        TestObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestObject))
                return false;
            TestObject that = (TestObject) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "TestObject{id=" + id + '}';
        }
    }

    static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// null array
        Arguments.of((Object) null), // zero‑length array
        Arguments.of(new Object[0]));
    }

    static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of(new Integer[] { 1, 2, 3, 4 }, List.of(1, 2, 3, 4)), Arguments.of(new String[] { "a", "b", "c" }, List.of("a", "b", "c")));
    }

    static Stream<Arguments> nullElementsProvider() {
        return Stream.of(Arguments.of(new Object[] { null }, List.of((Object) null)), Arguments.of(new Object[] { "x", null, "y" }, List.of("x", null, "y")));
    }

    @Test
    void listOf_WithNullArray_ReturnsEmptyUnmodifiableList() {
        // given
        String[] nullArray = null;
        // when
        List<String> result = CollectionUtilities.listOf(nullArray);
        // then
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Resulting list must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    void listOf_WithNoArguments_ReturnsEmptyUnmodifiableList() {
        // when
        List<Object> result = CollectionUtilities.listOf();
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "List should be empty when no arguments are supplied");
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()));
    }

    @Test
    void listOf_WithMultipleElements_ReturnsImmutableListInOrder() {
        // given
        String a = "alpha";
        String b = "beta";
        String c = "gamma";
        // when
        List<String> result = CollectionUtilities.listOf(a, b, c);
        // then
        assertEquals(3, result.size(), "List size should match number of supplied elements");
        assertEquals(a, result.get(0));
        assertEquals(b, result.get(1));
        assertEquals(c, result.get(2));
        // Verify immutability
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> result.add("delta")), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove(0)), () -> assertThrows(UnsupportedOperationException.class, () -> result.clear()));
    }

    @Test
    void listOf_WithDifferentElementTypes_ReturnsCorrectImmutableList() {
        // Integer test
        List<Integer> intList = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(List.of(1, 2, 3), intList);
        assertThrows(UnsupportedOperationException.class, () -> intList.set(0, 99));
        // Custom object test
        class Point {

            final int x, y;

            Point(int x, int y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Point))
                    return false;
                Point p = (Point) o;
                return x == p.x && y == p.y;
            }

            @Override
            public int hashCode() {
                return x * 31 + y;
            }
        }
        Point p1 = new Point(1, 2);
        Point p2 = new Point(3, 4);
        List<Point> pointList = CollectionUtilities.listOf(p1, p2);
        assertEquals(2, pointList.size());
        assertEquals(p1, pointList.get(0));
        assertEquals(p2, pointList.get(1));
        assertThrows(UnsupportedOperationException.class, () -> pointList.add(new Point(5, 6)));
    }

    @Test
    void listOf_ReturnsSameEmptyInstanceForRepeatedEmptyCalls() {
        // While the internal empty list is private, we can still assert that two empty calls
        // return equal (and, by contract of Collections.unmodifiableList, the same instance).
        List<String> first = CollectionUtilities.listOf();
        List<String> second = CollectionUtilities.listOf(new String[0]);
        assertEquals(first, second, "Both empty calls should produce equal lists");
        assertTrue(first.isEmpty() && second.isEmpty());
        // The safest way to verify same instance without accessing the private field is to
        // check reference equality; this is allowed because the method returns the same object.
        assertSame(first, second, "Empty list calls should return the same unmodifiable instance");
    }

    /* --------------------------------------------------------------
     *  Null and Empty Input
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: items={0}")
    @MethodSource("nullAndEmptyProvider")
    void listOf_NullOrEmptyInput_ReturnsEmptyUnmodifiableList(Object[] items) {
        // when
        List<Object> result = CollectionUtilities.listOf(items);
        // then
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Resulting list must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    /* --------------------------------------------------------------
     *  Single Element
     * -------------------------------------------------------------- */
    @Test
    void listOf_SingleElement_ReturnsImmutableList() {
        // given
        String element = "alpha";
        // when
        List<String> result = CollectionUtilities.listOf(element);
        // then
        assertEquals(1, result.size());
        assertEquals(element, result.get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.add("beta"));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
    }

    /* --------------------------------------------------------------
     *  Multiple Elements – primitive wrappers and strings
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: items={0}")
    @MethodSource("multipleElementsProvider")
    void listOf_MultipleElements_ReturnsImmutableListInOrder(Object[] items, List<?> expected) {
        // when
        List<?> result = CollectionUtilities.listOf(items);
        // then
        assertEquals(expected.size(), result.size(), "List size must match expected");
        assertIterableEquals(expected, result, "Elements must preserve order");
        // immutability checks
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> result.add("extra")), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove(0)), () -> assertThrows(UnsupportedOperationException.class, result::clear));
    }

    /* --------------------------------------------------------------
     *  Null Elements Inside Array
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: items={0}")
    @MethodSource("nullElementsProvider")
    void listOf_NullElementsInsideArray_PreservedInResult(Object[] items, List<?> expected) {
        // when
        List<?> result = CollectionUtilities.listOf(items);
        // then
        assertEquals(expected.size(), result.size());
        assertIterableEquals(expected, result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("new"));
    }

    /* --------------------------------------------------------------
     *  Modification Immutability (explicit test)
     * -------------------------------------------------------------- */
    @Test
    void listOf_ModificationImmutability_VerifyUnsupportedOperation() {
        List<String> list = CollectionUtilities.listOf("immutable");
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> list.add("new")), () -> assertThrows(UnsupportedOperationException.class, () -> list.remove(0)), () -> assertThrows(UnsupportedOperationException.class, () -> list.set(0, "changed")));
    }

    /* --------------------------------------------------------------
     *  Independence From Source Array
     * -------------------------------------------------------------- */
    @Test
    void listOf_DefensiveCopy_OriginalArrayModificationDoesNotAffectResult() {
        // given
        String[] source = new String[] { "original" };
        List<String> result = CollectionUtilities.listOf(source);
        // mutate source array after the call
        source[0] = "changed";
        // then
        assertEquals(1, result.size());
        assertEquals("original", result.get(0), "Result must not reflect changes to the source array");
    }

    /* --------------------------------------------------------------
     *  Custom Object Types
     * -------------------------------------------------------------- */
    @Test
    void listOf_CustomObjectTypes_ReturnsImmutableListPreservingReferences() {
        // given
        TestObject o1 = new TestObject(1);
        TestObject o2 = new TestObject(2);
        // when
        List<TestObject> result = CollectionUtilities.listOf(o1, o2);
        // then
        assertEquals(2, result.size());
        assertSame(o1, result.get(0));
        assertSame(o2, result.get(1));
        assertThrows(UnsupportedOperationException.class, () -> result.add(new TestObject(3)));
    }

    /* --------------------------------------------------------------
     *  Empty List Modification (re‑assertion for both null & empty array)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: items={0}")
    @MethodSource("nullAndEmptyProvider")
    void listOf_EmptyListModification_ThrowsUnsupportedOperation(Object[] items) {
        List<Object> result = CollectionUtilities.listOf(items);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
    }
}
