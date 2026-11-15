package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

class CompactSet_toString_Test {

    /**
     * Simple holder for a test scenario used by the parameterized test.
     */
    private static class TestScenario {

        final String description;

        final java.util.function.Consumer<CompactSet<Object>> actions;

        final String expected;

        TestScenario(String description, java.util.function.Consumer<CompactSet<Object>> actions, String expected) {
            this.description = description;
            this.actions = actions;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Custom object used in the "mixed types" test case.
     */
    private static class CustomObj {

        private final int value;

        CustomObj(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomObj{value=" + value + '}';
        }

        // equals & hashCode are not required for the toString test, but are provided
        // for completeness should the set need to enforce uniqueness.
        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomObj) && ((CustomObj) o).value == this.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    /**
     * Provides arguments for normal {@code toString()} behaviour.
     * Each {@link TestScenario} contains the actions to perform on a fresh
     * {@link CompactSet} and the expected string representation.
     */
    private static Stream<TestScenario> normalScenarios() {
        return Stream.of(// empty set
        new TestScenario("Empty set", set -> {
        }, "[]"), // single element "a"
        new TestScenario("Single element", set -> {
            set.add("a");
        }, "[a]"), // multiple elements preserving insertion order
        new TestScenario("Multiple elements insertion order", set -> {
            set.add(1);
            set.add(2);
            set.add(3);
        }, "[1, 2, 3]"), // null element
        new TestScenario("Null element", set -> {
            set.add(null);
        }, "[null]"), // duplicate element "a" should appear only once
        new TestScenario("Duplicate element", set -> {
            set.add("a");
            // duplicate
            set.add("a");
        }, "[a]"), // removal case: add three, remove middle
        new TestScenario("Removal of middle element", set -> {
            set.add("x");
            set.add("y");
            set.add("z");
            set.remove("y");
        }, "[x, z]"), // duplicate null (only one null)
        new TestScenario("Duplicate null", set -> {
            set.add(null);
            set.add(null);
        }, "[null]"), // mixed runtime types
        new TestScenario("Mixed types", set -> {
            set.add("a");
            set.add(1);
            set.add(new CustomObj(5));
        }, "[a, 1, CustomObj{value=5}]"), // strings containing commas and brackets
        new TestScenario("Special characters", set -> {
            set.add("a,b");
            set.add("[c]");
        }, "[a,b, [c]]"), // builder‑added elements (order should be insertion order)
        new TestScenario("Builder added elements", set -> {
            // using builder to create the set, then add via builder API
            // The builder API itself does not add elements, so we add directly
            set.add("builder1");
            set.add("builder2");
        }, "[builder1, builder2]"), // clear after adding elements
        new TestScenario("Clear after adds", set -> {
            set.add("temp1");
            set.add("temp2");
            set.clear();
        }, "[]"));
    }

    /**
     * Helper that injects a value into the private {@code map} field of a
     * {@link CompactSet} instance via reflection.
     */
    private static void injectMap(CompactSet<?> set, Object map) throws Exception {
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, map);
    }

    @Test
    void testToStringEmptySet() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.isEmpty(), "Set should be empty");
        assertEquals("[]", set.toString(), "Empty set should render as []");
    }

    @Test
    void testToStringSingleElement() {
        CompactSet<String> set = new CompactSet<>();
        set.add("foo");
        assertEquals("[foo]", set.toString(), "Single element set string representation is incorrect");
    }

    @Test
    void testToStringMultipleElementsInsertionOrder() {
        CompactSet<String> set = new CompactSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // CompactSet uses insertion order by default, so the string should reflect that order
        assertEquals("[apple, banana, cherry]", set.toString(), "Insertion order should be preserved in toString()");
    }

    @Test
    void testToStringSortedOrderViaBuilder() {
        // Builder creates a sorted CompactSet
        CompactSet<String> set = CompactSet.<String>builder().sortedOrder().build();
        // Add elements in unsorted order
        set.add("zebra");
        set.add("apple");
        set.add("monkey");
        // Sorted order should be alphabetical
        assertEquals("[apple, monkey, zebra]", set.toString(), "Sorted order set should produce a sorted string representation");
    }

    @Test
    void testToStringCaseInsensitiveBuilder() {
        // Builder creates a case‑insensitive set
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).build();
        set.add("A");
        set.add("b");
        set.add("C");
        // The underlying keySet keeps the original objects, so the string should contain them
        // (order is insertion order because we did not request sorting)
        assertEquals("[A, b, C]", set.toString(), "Case‑insensitive set should retain original element representations in toString()");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("normalScenarios")
    void testToStringNormalCases(TestScenario scenario) {
        CompactSet<Object> set = new CompactSet<>();
        scenario.actions.accept(set);
        assertEquals(scenario.expected, set.toString(), () -> "Unexpected toString() for scenario: " + scenario.description);
    }

    @Test
    void testToStringWhenInternalMapIsNull() throws Exception {
        CompactSet<String> set = new CompactSet<>();
        // Force the internal map to be null
        injectMap(set, null);
        // Expect NPE because toString() dereferences map
        assertThrows(NullPointerException.class, set::toString, "Calling toString() with a null internal map should throw NullPointerException");
    }

    @Test
    void testToStringPropagatesRuntimeExceptionFromKeySet() throws Exception {
        // Mock CompactMap to throw when keySet() is called
        @SuppressWarnings("unchecked")
        CompactMap<Object, Object> mockMap = mock(CompactMap.class);
        when(mockMap.keySet()).thenThrow(new RuntimeException("keySet failure"));
        CompactSet<String> set = new CompactSet<>();
        injectMap(set, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, set::toString, "RuntimeException from map.keySet() should propagate");
        assertEquals("keySet failure", ex.getMessage());
    }

    @Test
    void testToStringConcurrentModification() throws Exception {
        // Create a Set whose iterator throws CME after the first element
        Set<String> problematicKeySet = new AbstractSet<String>() {

            private final List<String> data = Arrays.asList("first", "second");

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < data.size();
                    }

                    @Override
                    public String next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        // After returning the first element, simulate concurrent modification
                        if (index == 1) {
                            throw new ConcurrentModificationException("simulated concurrent modification");
                        }
                        return data.get(index++);
                    }
                };
            }

            @Override
            public int size() {
                return data.size();
            }
        };
        @SuppressWarnings("unchecked")
        CompactMap<String, Object> mockMap = mock(CompactMap.class);
        when(mockMap.keySet()).thenReturn(problematicKeySet);
        CompactSet<String> set = new CompactSet<>();
        injectMap(set, mockMap);
        assertThrows(ConcurrentModificationException.class, set::toString, "Concurrent modification during toString() should result in CME");
    }
}
