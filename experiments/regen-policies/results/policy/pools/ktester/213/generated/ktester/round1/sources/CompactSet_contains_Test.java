package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class CompactSet_contains_Test {

    /**
     * Set used in most tests – a fresh instance for each test method.
     */
    private CompactSet<String> set;

    private static final class Arguments extends ArrayList<Object> {

        private Arguments(Object... values) {
            super(Arrays.asList(values));
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }
        //         Object get(int index) {
        //             return super.get(index);
        //         }
    }

    static final class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + '\'' + '}';
        }
    }

    @BeforeEach
    void setup() {
        set = new CompactSet<>();
    }

    @AfterEach
    void teardown() {
        if (set != null) {
            set.clear();
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        set = new CompactSet<>();
    }

    //     @AfterEach
    void teardownAfterEach() {
        if (set != null) {
            set.clear();
        }
    }

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("apple", true, "The set is populated with the element \"apple\"; contains should return true for an existing element."), Arguments.of("banana", false, "The set does not contain \"banana\"; contains should return false for a missing element."), Arguments.of(null, false, "Null is not added to the set (CompactMap does not allow null keys); contains(null) should safely return false without throwing."), Arguments.of("OrAnGe", true, "A case‑insensitive CompactSet is built (caseSensitive = false) and contains \"orange\" (lower‑case) added earlier; contains should return true regardless of case."), Arguments.of("pear", false, "After adding \"pear\" and then removing it, contains should return false because the element is no longer present."), Arguments.of("grape", false, "After calling clear() on a set that previously contained \"grape\", contains should return false because the set is empty."), Arguments.of(123, false, "The set contains only String elements; querying with an Integer (different type) should return false."));
    }

    static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(Arguments.of("apple", false, "A case‑sensitive CompactSet is built (caseSensitive = true) and \"Apple\" (capital A) is added; contains(\"apple\") should return false because the set is case‑sensitive."), Arguments.of("APPLE", false, "A case‑sensitive CompactSet is built and \"apple\" (lower‑case) is added; contains(\"APPLE\") should return false because the case does not match."), Arguments.of("Orange", true, "A case‑insensitive CompactSet (caseSensitive = false) is built and \"orange\" (lower‑case) is added; contains(\"Orange\") should return true regardless of case."));
    }

    @Test
    void containsEmptySet() {
        Assertions.assertTrue(set.isEmpty());
        Assertions.assertFalse(set.contains(null));
        Assertions.assertFalse(set.contains("any"));
    }

    @Test
    void containsAfterAdd() {
        Assertions.assertTrue(set.add("alpha"));
        Assertions.assertTrue(set.add("beta"));
        Assertions.assertTrue(set.contains("alpha"));
        Assertions.assertTrue(set.contains("beta"));
        Assertions.assertFalse(set.contains("gamma"));
    }

    @Test
    void containsAfterRemove() {
        Assertions.assertTrue(set.add("delta"));
        Assertions.assertTrue(set.contains("delta"));
        Assertions.assertTrue(set.remove("delta"));
        Assertions.assertFalse(set.contains("delta"));
    }

    @Test
    void containsCaseInsensitive() {
        CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
        try {
            Assertions.assertTrue(ciSet.add("Apple"));
            Assertions.assertTrue(ciSet.contains("apple"));
            Assertions.assertTrue(ciSet.contains("APPLE"));
            Assertions.assertTrue(ciSet.contains("ApPlE"));
            Assertions.assertFalse(ciSet.contains("Banana"));
        } finally {
            ciSet.clear();
        }
    }

    @Test
    void containsWithOrderedSet() {
        CompactSet<String> ordered = CompactSet.<String>builder().insertionOrder().build();
        try {
            Assertions.assertTrue(ordered.add("first"));
            Assertions.assertTrue(ordered.add("second"));
            Assertions.assertTrue(ordered.contains("first"));
            Assertions.assertTrue(ordered.contains("second"));
            Assertions.assertFalse(ordered.contains("third"));
        } finally {
            ordered.clear();
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void testBasicFunctionality(Object query, boolean expected, String description) {
        CompactSet<String> workingSet = new CompactSet<>();
        switch(description) {
            case "The set is populated with the element \"apple\"; contains should return true for an existing element.":
                workingSet.add("apple");
                break;
            case "The set does not contain \"banana\"; contains should return false for a missing element.":
                // leave empty
                break;
            case "Null is not added to the set (CompactMap does not allow null keys); contains(null) should safely return false without throwing.":
                // leave empty
                break;
            case "A case‑insensitive CompactSet is built (caseSensitive = false) and contains \"orange\" (lower‑case) added earlier; contains should return true regardless of case.":
                workingSet = CompactSet.<String>builder().caseSensitive(false).build();
                workingSet.add("orange");
                break;
            case "After adding \"pear\" and then removing it, contains should return false because the element is no longer present.":
                workingSet.add("pear");
                workingSet.remove("pear");
                break;
            case "After calling clear() on a set that previously contained \"grape\", contains should return false because the set is empty.":
                workingSet.add("grape");
                workingSet.clear();
                break;
            case "The set contains only String elements; querying with an Integer (different type) should return false.":
                workingSet.add("one");
                break;
            default:
        }
        Assertions.assertEquals(expected, workingSet.contains(query), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("caseSensitivityProvider")
    void testCaseSensitivity(Object query, boolean expected, String description) {
        CompactSet<String> caseSensitiveSet = CompactSet.<String>builder().caseSensitive(true).build();
        CompactSet<String> caseInsensitiveSet = CompactSet.<String>builder().caseSensitive(false).build();
        switch(description) {
            case "A case‑sensitive CompactSet is built (caseSensitive = true) and \"Apple\" (capital A) is added; contains(\"apple\") should return false because the set is case‑sensitive.":
                caseSensitiveSet.add("Apple");
                Assertions.assertEquals(expected, caseSensitiveSet.contains(query), description);
                break;
            case "A case‑sensitive CompactSet is built and \"apple\" (lower‑case) is added; contains(\"APPLE\") should return false because the case does not match.":
                caseSensitiveSet.add("apple");
                Assertions.assertEquals(expected, caseSensitiveSet.contains(query), description);
                break;
            case "A case‑insensitive CompactSet (caseSensitive = false) is built and \"orange\" (lower‑case) is added; contains(\"Orange\") should return true regardless of case.":
                caseInsensitiveSet.add("orange");
                Assertions.assertEquals(expected, caseInsensitiveSet.contains(query), description);
                break;
            default:
                Assertions.fail("Unexpected description: " + description);
        }
    }

    @Test
    void customObjectHandling() {
        CompactSet<Person> personSet = new CompactSet<>();
        Person alice1 = new Person("Alice");
        Assertions.assertTrue(personSet.add(alice1));
        Person alice2 = new Person("Alice");
        Assertions.assertTrue(personSet.contains(alice2));
        Person bob = new Person("Bob");
        Assertions.assertFalse(personSet.contains(bob));
    }

    @Test
    void bulkAddAll() {
        CompactSet<String> bulkSet = new CompactSet<>();
        Assertions.assertTrue(bulkSet.addAll(Arrays.asList("x", "y", "z")));
        Assertions.assertTrue(bulkSet.contains("y"));
        Assertions.assertFalse(bulkSet.contains("a"));
    }

    @Test
    void retainAllEffect() {
        CompactSet<String> s = new CompactSet<>();
        s.addAll(Arrays.asList("a", "b", "c"));
        Assertions.assertTrue(s.retainAll(Arrays.asList("b", "c")));
        Assertions.assertFalse(s.contains("a"));
        Assertions.assertTrue(s.contains("b"));
        Assertions.assertTrue(s.contains("c"));
    }

    @Test
    void removeAllNonExistentElements() {
        CompactSet<String> s = new CompactSet<>();
        s.addAll(Arrays.asList("m", "n"));
        Assertions.assertTrue(s.removeAll(Collections.singletonList("z")));
        Assertions.assertTrue(s.contains("m"));
    }

    @Test
    void iteratorRemove() {
        CompactSet<String> s = new CompactSet<>();
        s.addAll(Arrays.asList("p", "q"));
        Iterator<String> it = s.iterator();
        Assertions.assertTrue(it.hasNext());
        String first = it.next();
        Assertions.assertEquals("p", first);
        it.remove();
        Assertions.assertFalse(s.contains("p"));
        Assertions.assertTrue(s.contains("q"));
    }

    @Test
    void emptySetBaseline() {
        CompactSet<String> empty = new CompactSet<>();
        Assertions.assertFalse(empty.contains("any"));
    }

    @Test
    void afterClearNonEmptySet() {
        CompactSet<String> s = new CompactSet<>();
        s.add("kiwi");
        Assertions.assertTrue(s.contains("kiwi"));
        s.clear();
        Assertions.assertFalse(s.contains("kiwi"));
    }

    @Test
    void containsThrowsClassCastExceptionWithIncompatibleKey_NaturalOrdering() {
        CompactSet<String> sortedSet = CompactSet.<String>builder().sortedOrder().build();
        sortedSet.add("alpha");
        Assertions.assertThrows(ClassCastException.class, () -> sortedSet.contains(123));
    }

    @Test
    void containsThrowsClassCastExceptionWithIncompatibleKey_CaseInsensitiveComparator() {
        CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
        ciSet.add("test");
        Assertions.assertThrows(ClassCastException.class, () -> ciSet.contains(new Object()));
    }
}
