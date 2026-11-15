package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_add_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for each test to guarantee isolation
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help the garbage collector; not strictly required
        if (set != null) {
            set.clear();
        }
    }

    // Example of a parameterised test for simple add/duplicate scenarios
    private static Stream<Arguments> integerAddScenarios() {
        return Stream.of(Arguments.of(42, true, "first insertion of 42"), Arguments.of(42, false, "duplicate insertion of 42"));
    }

    @Test
    void addNonNullElements() {
        // given
        assertTrue(set.isEmpty());
        // when
        boolean addedApple = set.add("apple");
        boolean addedBanana = set.add("banana");
        boolean addedCherry = set.add("cherry");
        // then
        assertTrue(addedApple, "Adding a new element should return true");
        assertTrue(addedBanana, "Adding a new element should return true");
        assertTrue(addedCherry, "Adding a new element should return true");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("apple", "banana", "cherry")));
    }

    @Test
    void addNullElement() {
        // when
        boolean addedNullFirst = set.add(null);
        // duplicate null
        boolean addedNullSecond = set.add(null);
        // then
        assertTrue(addedNullFirst, "First addition of null should return true");
        assertFalse(addedNullSecond, "Second addition of the same null should return false");
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    void addDuplicateElements() {
        // given
        set.add("apple");
        set.add("banana");
        // when
        boolean addedAppleAgain = set.add("apple");
        boolean addedBananaAgain = set.add("banana");
        // then
        assertFalse(addedAppleAgain, "Adding a duplicate element should return false");
        assertFalse(addedBananaAgain, "Adding a duplicate element should return false");
        assertEquals(2, set.size());
    }

    @Test
    void addMixedNullAndNonNull() {
        // when
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        // then
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList("apple", "banana", "cherry", null)));
        // iterator order should be natural ordering with null placed last (as per maskNull implementation)
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addUsingComparatorConstructor() {
        // given
        Comparator<String> reverse = Comparator.nullsFirst(Comparator.reverseOrder());
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // when
        assertTrue(customSet.add("apple"));
        assertTrue(customSet.add(null));
        assertTrue(customSet.add("banana"));
        assertTrue(customSet.add("cherry"));
        // then
        assertEquals(4, customSet.size());
        assertTrue(customSet.containsAll(Arrays.asList("apple", "banana", "cherry", null)));
        // iterator order should follow the comparator (null first, then reverse alphabetical)
        Iterator<String> it = customSet.iterator();
        // nullsFirst puts null first
        assertEquals(null, it.next());
        // reverse order: cherry > banana > apple
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    // --- Integer set basic add / duplicate --------------------------------
    @Test
    void addNonNullIntegerElements() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // first insertion should succeed
        assertTrue(intSet.add(42), "Adding a non‑null Integer to an empty set should return true");
        // duplicate insertion should be detected
        assertFalse(intSet.add(42), "Adding the same Integer again should return false");
        assertEquals(1, intSet.size());
        assertTrue(intSet.contains(42));
    }

    // --- Integer set null handling -----------------------------------------
    @Test
    void addNullIntegerElements() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(intSet.add(null), "First null insertion should return true");
        assertFalse(intSet.add(null), "Second null insertion should return false");
        assertEquals(1, intSet.size());
        assertTrue(intSet.contains(null));
    }

    // --- Distinct element to non‑empty Integer set -------------------------
    @Test
    void addDistinctToNonEmptyIntegerSet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // pre‑populate with 3
        assertTrue(intSet.add(3));
        // add a different element
        assertTrue(intSet.add(7), "Adding a different Integer should succeed");
        // duplicate of the original element
        assertFalse(intSet.add(3), "Adding 3 again must be recognised as a duplicate");
        assertEquals(2, intSet.size());
        assertTrue(intSet.containsAll(Arrays.asList(3, 7)));
    }

    // --- Add after clear ---------------------------------------------------
    @Test
    void addAfterClear() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // first insertion
        assertTrue(intSet.add(5), "First add should succeed");
        // clear the set
        intSet.clear();
        assertTrue(intSet.isEmpty(), "Set should be empty after clear");
        // add again after clear
        assertTrue(intSet.add(5), "Add after clear should succeed");
        assertEquals(1, intSet.size());
    }

    // --- Reverse‑order comparator (Integer) --------------------------------
    @Test
    void addWithReverseOrderComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // add elements
        assertTrue(revSet.add(10));
        assertTrue(revSet.add(5));
        // duplicate
        assertFalse(revSet.add(10));
        // verify order: descending (10 then 5)
        Iterator<Integer> it = revSet.iterator();
        assertEquals(10, it.next());
        assertEquals(5, it.next());
        assertFalse(it.hasNext());
    }

    // --- Length‑based comparator (String) ----------------------------------
    @Test
    void addWithLengthComparator() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComp);
        // first element of length 2
        assertTrue(lengthSet.add("aa"));
        // another of same length -> considered duplicate
        assertFalse(lengthSet.add("bb"));
        // different length
        assertTrue(lengthSet.add("c"));
        assertEquals(2, lengthSet.size());
        assertTrue(lengthSet.containsAll(Arrays.asList("aa", "c")));
    }

    // --- Integer.MAX_VALUE -------------------------------------------------
    @Test
    void addIntegerMaxValue() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(intSet.add(Integer.MAX_VALUE));
        assertFalse(intSet.add(Integer.MAX_VALUE));
        assertEquals(1, intSet.size());
    }

    // --- Empty string -------------------------------------------------------
    @Test
    void addEmptyString() {
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(strSet.add(""));
        assertFalse(strSet.add(""));
        assertEquals(1, strSet.size());
    }

    // --- Very large string --------------------------------------------------
    @Test
    void addVeryLargeString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append('X');
        String large = sb.toString();
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(strSet.add(large));
        assertFalse(strSet.add(large));
        assertEquals(1, strSet.size());
    }

    // --- Null when set already contains non‑null ---------------------------
    @Test
    void addNullWhenNonNullExists() {
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(strSet.add("foo"));
        assertTrue(strSet.add(null));
        assertFalse(strSet.add(null));
        assertEquals(2, strSet.size());
        assertTrue(strSet.containsAll(Arrays.asList("foo", null)));
    }

    // --- Non‑null when set already contains null sentinel ------------------
    @Test
    void addNonNullWhenNullExists() {
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(strSet.add(null));
        assertTrue(strSet.add("bar"));
        assertFalse(strSet.add("bar"));
        assertEquals(2, strSet.size());
        assertTrue(strSet.containsAll(Arrays.asList(null, "bar")));
    }

    // --- Heterogeneous objects in Object‑typed set -------------------------
    @Test
    void addHeterogeneousObjects() {
        NavigableSet<Object> objSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(objSet.add(1), "Integer 1 should be added");
        assertTrue(objSet.add("1"), "String \"1\" should be considered distinct");
        assertFalse(objSet.add(1), "Duplicate Integer 1 should be rejected");
        assertEquals(2, objSet.size());
        assertTrue(objSet.containsAll(Arrays.asList(1, "1")));
    }

    // --- Incompatible type to natural‑ordered Integer set ------------------
    @Test
    void addIncompatibleTypeToIntegerSet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertThrows(ClassCastException.class, () -> intSet.add((Integer) (Object) "incompatibleString"), "Adding a String to an Integer natural‑ordered set must throw ClassCastException");
    }

    // --- Non‑Comparable element to natural‑ordered Object set ---------------
    @Test
    void addNonComparableToObjectSet() {
        NavigableSet<Object> objSet = new ConcurrentNavigableSetNullSafe<>();
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> objSet.add(nonComparable), "Adding a non‑Comparable Object to a natural‑ordered set must throw ClassCastException");
    }

    // --- Element incompatible with custom comparator ------------------------
    @Test
    void addIncompatibleWithCustomComparator() {
        // Comparator that blindly casts to Integer – will fail for a String
        Comparator<Integer> badComparator = (a, b) -> ((Integer) a).compareTo((Integer) b);
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        // first, a valid integer to initialise the set
        assertTrue(intSet.add(1));
        // now attempt to add a String; the comparator will be invoked and throw
        assertThrows(ClassCastException.class, () -> intSet.add((Integer) (Object) "stringValue"), "Adding a String to a set with an Integer‑casting comparator must throw ClassCastException");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("integerAddScenarios")
    void parameterisedIntegerAdd(int value, boolean expectedResult, String description) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        boolean result = intSet.add(value);
        assertEquals(expectedResult, result, description);
    }
}
