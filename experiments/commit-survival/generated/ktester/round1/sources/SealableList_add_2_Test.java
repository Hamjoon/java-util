package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_add_2_Test {

    /**
     * The list under test
     */
    private SealableList<Integer> list;

    /**
     * Backing mutable flag used by the sealed‑supplier
     */
    private volatile boolean sealedState;

    /**
     * Supplier that reflects the current sealed state
     */
    private Supplier<Boolean> sealedSupplier;

    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return age == p.age && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + '\'' + ", age=" + age + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sealed flag before each test.
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Initialise the list with a known state (10, 20, 30).
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests.
        list = null;
    }

    /**
     * Helper to build a SealableList with a supplied initial collection.
     */
    private <T> SealableList<T> createList(List<T> backing, Supplier<Boolean> supplier) {
        return new SealableList<>(backing, supplier);
    }

    static Stream<Arguments> normalAddProvider() {
        return Stream.of(// empty list, add at index 0
        Arguments.of(Collections.emptyList(), 0, "apple", 1, List.of("apple")), // list with one element, add at end (index == size)
        Arguments.of(List.of("apple"), 1, "banana", 2, List.of("apple", "banana")), // list with one element, add null at index 0
        Arguments.of(List.of("apple"), 0, null, 1, List.of(null, "apple")));
    }

    static Stream<Arguments> sealedAddProvider() {
        return Stream.of(Arguments.of(0, "elderberry"), Arguments.of(2, "fig"));
    }

    @Test
    void testAddAtMiddleIndex() {
        // Insert 15 between 10 and 20 (index 1)
        list.add(1, 15);
        assertEquals(4, list.size(), "Size should increase after insertion");
        assertEquals(Integer.valueOf(10), list.get(0));
        assertEquals(Integer.valueOf(15), list.get(1));
        assertEquals(Integer.valueOf(20), list.get(2));
        assertEquals(Integer.valueOf(30), list.get(3));
    }

    @Test
    void testAddAtBeginningIndex() {
        // Insert 5 at the very start (index 0)
        list.add(0, 5);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(5), list.get(0));
        assertEquals(Integer.valueOf(10), list.get(1));
        assertEquals(Integer.valueOf(20), list.get(2));
        assertEquals(Integer.valueOf(30), list.get(3));
    }

    @Test
    void testAddAtEndIndex() {
        // Insert 40 at the end using size() as the index
        list.add(list.size(), 40);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(10), list.get(0));
        assertEquals(Integer.valueOf(20), list.get(1));
        assertEquals(Integer.valueOf(30), list.get(2));
        assertEquals(Integer.valueOf(40), list.get(3));
    }

    @Test
    void testAddWhenSealedThrows() {
        // make the list immutable
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add(1, 99), "Adding while sealed must throw UnsupportedOperationException");
    }

    @Test
    void testAddWithNegativeIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 42), "Negative index should result in IndexOutOfBoundsException");
    }

    @Test
    void testAddWithIndexGreaterThanSizeThrows() {
        assertThrows(IndexOutOfBoundsException.class, // current size is 3
        () -> list.add(5, 42), "Index greater than size should result in IndexOutOfBoundsException");
    }

    @Test
    void testAddShiftsSubsequentElements() {
        // Insert 25 at index 2 – element originally at index 2 (30) should shift right.
        list.add(2, 25);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(10), list.get(0));
        assertEquals(Integer.valueOf(20), list.get(1));
        assertEquals(Integer.valueOf(25), list.get(2));
        assertEquals(Integer.valueOf(30), list.get(3));
    }

    @ParameterizedTest(name = "[{index}] Normal add – initial={0}, index={1}, element={2}")
    @MethodSource("normalAddProvider")
    void testAddWhenNotSealed_NormalOperation(List<String> initial, int index, String element, int expectedSize, List<String> expectedOrder) {
        // Arrange
        SealableList<String> sl = createList(new ArrayList<>(initial), () -> false);
        // Act
        sl.add(index, element);
        // Assert
        assertEquals(expectedSize, sl.size(), "Size should match expected after insertion");
        assertIterableEquals(expectedOrder, sl, "List order after insertion does not match expected");
    }

    @ParameterizedTest(name = "[{index}] Add while sealed – index={0}, element={1}")
    @MethodSource("sealedAddProvider")
    void testAddWhenSealed_BlocksModification(int index, String element) {
        // Arrange: list is sealed via supplier
        sealedState = true;
        SealableList<String> sl = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // populate a little to make index meaningful
        sl.add("a");
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> sl.add(index, element), "Adding while sealed must throw UnsupportedOperationException");
    }

    @Test
    void testAddAtMiddle_ShiftsElements() {
        // Arrange
        SealableList<String> sl = createList(new ArrayList<>(List.of("a", "b", "c")), () -> false);
        // Act
        sl.add(1, "x");
        // Assert
        assertEquals(4, sl.size());
        assertEquals(List.of("a", "x", "b", "c"), sl);
    }

    @Test
    void testAddAtEnd_IndexEqualsSize_Appends() {
        SealableList<String> sl = createList(new ArrayList<>(List.of("p", "q")), () -> false);
        sl.add(2, "r");
        assertEquals(3, sl.size());
        assertEquals(List.of("p", "q", "r"), sl);
    }

    @Test
    void testAddDuplicateElement_AllowsDuplicates() {
        SealableList<String> sl = createList(new ArrayList<>(List.of("a", "b", "c")), () -> false);
        sl.add(2, "a");
        assertEquals(4, sl.size());
        assertEquals(List.of("a", "b", "a", "c"), sl);
    }

    @Test
    void testAddCustomObject_StoredAndRetrievable() {
        SealableList<Person> sl = createList(new ArrayList<>(), () -> false);
        Person john = new Person("John", 30);
        sl.add(0, john);
        assertEquals(1, sl.size());
        assertEquals(john, sl.get(0));
    }

    @Test
    void testAddNullToNonEmptyList() {
        SealableList<String> sl = createList(new ArrayList<>(List.of("alpha")), () -> false);
        sl.add(1, null);
        assertEquals(2, sl.size());
        assertNull(sl.get(1));
        assertEquals("alpha", sl.get(0));
    }

    @Test
    void testAddAfterSealing_OperationBlocked() {
        // Arrange: add one element while mutable
        SealableList<String> sl = createList(new ArrayList<>(), () -> false);
        sl.add("first");
        // Seal the list
        Supplier<Boolean> sealingSupplier = () -> true;
        // recreate wrapper with sealed supplier (simulates state change)
        sl = new SealableList<>(sl, sealingSupplier);
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> sl.add(1, "second"), "Add after sealing must be blocked");
        // Ensure list unchanged
        assertEquals(1, sl.size());
        assertEquals("first", sl.get(0));
    }

    @Test
    void testAddOnUnmodifiableUnderlyingList_PropagatesUnsupportedOperation() {
        List<String> unmod = Collections.unmodifiableList(new ArrayList<>(List.of("x")));
        SealableList<String> sl = new SealableList<>(unmod, () -> false);
        assertThrows(UnsupportedOperationException.class, () -> sl.add(0, "test"), "Underlying unmodifiable list should cause UnsupportedOperationException");
    }

    @Test
    void testAddNullToNullDisallowingList_ThrowsNullPointerException() {
        // Simulate a list that rejects nulls by overriding add(int,T)
        List<String> nullRejecting = new ArrayList<String>() {

            @Override
            public void add(int index, String element) {
                if (element == null) {
                    throw new NullPointerException("null not allowed");
                }
                super.add(index, element);
            }
        };
        SealableList<String> sl = new SealableList<>(nullRejecting, () -> false);
        assertThrows(NullPointerException.class, () -> sl.add(0, null), "Underlying list rejecting null should propagate NullPointerException");
    }

    @Test
    void testAddIncompatibleType_PropagatesClassCastException() {
        // Underlying list is typed to String; we use raw type to force a bad cast
        List rawList = new ArrayList<String>();
        SealableList rawSealable = new SealableList(rawList, () -> false);
        assertThrows(ClassCastException.class, () -> rawSealable.add(0, 123), "Adding an Integer to a List<String> should cause ClassCastException");
    }
}
