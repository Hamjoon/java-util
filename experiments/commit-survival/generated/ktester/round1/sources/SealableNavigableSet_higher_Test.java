package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_higher_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /* ----------------------------------------------------------------------
     *  Simple POJO used for the custom‑object comparator tests.
     * ---------------------------------------------------------------------- */
    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // Equality based on both fields – required for assertEquals in tests
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
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordering (null is allowed by the underlying set)
        set.add(null);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC – not strictly required but keeps the pattern consistent
        set = null;
    }

    static Stream<Arguments> normalOrderingProvider() {
        return Stream.of(// immediate next
        Arguments.of(2, 3), // greatest element
        Arguments.of(4, null), // less than smallest
        Arguments.of(0, 1), // greater than any element
        Arguments.of(5, null));
    }

    static Stream<Arguments> reverseOrderProvider() {
        return Stream.of(// in reverse order “higher” means the next smaller natural value
        Arguments.of(3, 2), // greatest according to reverse comparator
        Arguments.of(5, null));
    }

    static Stream<Arguments> singletonProvider() {
        return Stream.of(Arguments.of(10, null), Arguments.of(5, 10));
    }

    static Stream<Arguments> negativeNumbersProvider() {
        return Stream.of(Arguments.of(-2, -1), Arguments.of(0, null));
    }

    static Stream<Arguments> caseInsensitiveStringProvider() {
        return Stream.of(Arguments.of("apple", "banana"), Arguments.of("Banana", "Cherry"), Arguments.of("cherry", null));
    }

    static Stream<Arguments> subSetProvider() {
        return Stream.of(Arguments.of(3, 4), Arguments.of(4, null), Arguments.of(2, 3));
    }

    static Stream<Arguments> nullsFirstProvider() {
        return Stream.of(Arguments.of(null, 1), Arguments.of(2, null));
    }

    static Stream<Arguments> customObjectProvider() {
        return Stream.of(Arguments.of(new Person("Bob", 30), new Person("Carol", 40)), Arguments.of(new Person("Dummy", 25), new Person("Bob", 30)), Arguments.of(new Person("Eve", 45), null));
    }

    @Test
    void testHigherReturnsCorrectElement() {
        // higher(20) should return the least element greater than 20, i.e., 30
        assertEquals(30, set.higher(20));
        // higher(25) also returns 30
        assertEquals(30, set.higher(25));
        // higher(30) has no greater element, should return null
        assertNull(set.higher(30));
    }

    @Test
    void testHigherWhenSealedDoesNotAffectResult() {
        // simulate sealed (read‑only) state
        sealedState = true;
        // The higher operation is a read‑only query, it must still work
        assertEquals(30, set.higher(20));
        assertEquals(30, set.higher(25));
        assertNull(set.higher(30));
    }

    @Test
    void testHigherWithNullElement() {
        // The set contains a null element; higher(null) should return the first
        // non‑null element according to the natural ordering (10)
        assertEquals(10, set.higher(null));
    }

    @Test
    void testHigherWithElementNotPresent() {
        // When the element is not in the set, higher should still return the least
        // element greater than the supplied value
        assertEquals(20, set.higher(15));
        assertEquals(10, set.higher(-5));
    }

    @Test
    void testHigherDoesNotModifySet() {
        int originalSize = set.size();
        set.higher(20);
        assertEquals(originalSize, set.size(), "higher() must not mutate the set");
    }

    /* --------------------------- normal ordering --------------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("normalOrderingProvider")
    void higher_normalOrdering(Integer e, Integer expected) {
        // Build a fresh set [1,2,3,4] (no nulls)
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(Arrays.asList(1, 2, 3, 4));
        assertEquals(expected, s.higher(e));
    }

    /* ----------------------------- empty set ----------------------------- */
    @Test
    void higher_emptySet_returnsNull() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        assertNull(empty.higher(1), "higher() on an empty set must return null");
    }

    /* -------------------------- null argument --------------------------- */
    @Test
    void higher_nullArgument_throwsNPE() {
        // TreeSet with natural ordering does not accept null – we must not add null first.
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> s.higher(null));
    }

    /* --------------------- reverse order comparator ---------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("reverseOrderProvider")
    void higher_reverseOrder(Integer e, Integer expected) throws Exception {
        SealableNavigableSet<Integer> rev = new SealableNavigableSet<>(sealedSupplier);
        // Replace the internal NavigableSet with a TreeSet using reverse order
        TreeSet<Integer> backing = new TreeSet<>(Comparator.reverseOrder());
        backing.addAll(Arrays.asList(5, 4, 3, 2, 1));
        setPrivateNavSet(rev, backing);
        assertEquals(expected, rev.higher(e));
    }

    /* ------------------------- singleton set ---------------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("singletonProvider")
    void higher_singleton(Integer e, Integer expected) {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.add(10);
        assertEquals(expected, s.higher(e));
    }

    /* -------------------------- negative numbers -------------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("negativeNumbersProvider")
    void higher_negativeNumbers(Integer e, Integer expected) {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(Arrays.asList(-3, -2, -1, 0));
        assertEquals(expected, s.higher(e));
    }

    /* ------------------- case‑insensitive string comparator ------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher(\"{0}\") = \"{1}\"")
    @MethodSource("caseInsensitiveStringProvider")
    void higher_caseInsensitiveString(String e, String expected) throws Exception {
        SealableNavigableSet<String> s = new SealableNavigableSet<>(sealedSupplier);
        TreeSet<String> backing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        backing.addAll(Arrays.asList("Apple", "banana", "Cherry"));
        setPrivateNavSet(s, backing);
        assertEquals(expected, s.higher(e));
    }

    /* --------------------------- subSet view --------------------------- */
    @ParameterizedTest(name = "{index} ⇒ view.higher({0}) = {1}")
    @MethodSource("subSetProvider")
    void higher_subSetView(Integer e, Integer expected) {
        SealableNavigableSet<Integer> base = new SealableNavigableSet<>(sealedSupplier);
        base.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // [2,3,4]
        NavigableSet<Integer> view = base.subSet(2, true, 5, false);
        assertEquals(expected, view.higher(e));
    }

    /* -------------------------- nullsFirst comparator -------------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("nullsFirstProvider")
    void higher_nullsFirst(Integer e, Integer expected) throws Exception {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        TreeSet<Integer> backing = new TreeSet<>(Comparator.nullsFirst(Integer::compareTo));
        backing.addAll(Arrays.asList(null, 1, 2));
        setPrivateNavSet(s, backing);
        assertEquals(expected, s.higher(e));
    }

    /* -------------------------- custom object comparator -------------------------- */
    @ParameterizedTest(name = "{index} ⇒ higher({0}) = {1}")
    @MethodSource("customObjectProvider")
    void higher_customObject(Person e, Person expected) throws Exception {
        SealableNavigableSet<Person> s = new SealableNavigableSet<>(sealedSupplier);
        TreeSet<Person> backing = new TreeSet<>(Comparator.comparingInt(p -> p.age));
        backing.addAll(Arrays.asList(new Person("Alice", 20), new Person("Bob", 30), new Person("Carol", 40)));
        setPrivateNavSet(s, backing);
        assertEquals(expected, s.higher(e));
    }

    /* -------------------------- type‑mismatch exceptions -------------------------- */
    @Test
    void higher_typeMismatch_integerSet_stringArgument_throwsClassCastException() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(Arrays.asList(1, 2, 3));
        assertThrows(ClassCastException.class, () -> s.higher((Integer) (Object) "abc"));
    }

    @Test
    void higher_typeMismatch_stringSet_integerArgument_throwsClassCastException() {
        SealableNavigableSet<String> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(ClassCastException.class, () -> s.higher((String) (Object) 10));
    }

    /* -------------------------- comparator incompatibility -------------------------- */
    @Test
    void higher_comparatorIncompatibility_throwsClassCastException() throws Exception {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        TreeSet<Integer> backing = new TreeSet<>(Comparator.reverseOrder());
        backing.addAll(Arrays.asList(1, 2, 3));
        setPrivateNavSet(s, backing);
        // The reverse comparator expects Integers; passing a String leads to ClassCastException
        assertThrows(ClassCastException.class, () -> s.higher((Integer) (Object) "xyz"));
    }

    /* ----------------------------------------------------------------------
     *  Helper to inject a custom backing NavigableSet via reflection.
     * ---------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <T> void setPrivateNavSet(SealableNavigableSet<T> wrapper, NavigableSet<T> backing) throws Exception {
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(wrapper, backing);
    }
}
