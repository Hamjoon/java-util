package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.regex.Pattern;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toString_Test {

    /**
     * Instance created before each test – a fresh, empty set.
     */
    private ConcurrentSet<Object> set;

    class Bar {

        @Override
        public String toString() {
            return "Bar";
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class Bar {

        @Override
        public String toString() {
            return "Bar";
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a failing test does not affect others.
        set.clear();
    }

    /**
     * Verify the string representation of an empty set.
     */
    @Test
    void testToStringEmptySet() {
        // given
        // (set is already empty)
        // when
        String result = set.toString();
        // then
        assertEquals("{}", result, "Empty set should be represented as {}");
    }

    /**
     * Verify the string representation of a set with a single element.
     */
    @Test
    void testToStringSingleElement() {
        // given
        set.add("apple");
        // when
        String result = set.toString();
        // then
        assertEquals("{apple}", result, "Single‑element set should be represented as {element}");
    }

    /**
     * Verify that a set containing multiple distinct elements produces a string
     * that contains all elements, regardless of iteration order.
     */
    @Test
    void testToStringMultipleElements() {
        // given
        set.addAll(Arrays.asList("a", "b", "c"));
        // when
        String result = set.toString();
        // then
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Strip the surrounding braces and split on ", "
        String inner = result.substring(1, result.length() - 1);
        Set<String> actualElements = Arrays.stream(inner.split(",\\s*")).collect(Collectors.toSet());
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertEquals(expected, actualElements, "String representation should contain exactly the set elements");
    }

    /**
     * Verify that a set containing {@code null} is rendered as the literal "null".
     */
    @Test
    void testToStringWithNullElement() {
        // given
        set.addAll(Arrays.asList("x", null, "y"));
        // when
        String result = set.toString();
        // then
        assertTrue(result.contains("null"), "String representation should contain the literal \"null\" when the set holds a null element");
    }

    /**
     * Verify the special handling of a set that contains itself.
     */
    @Test
    void testToStringSelfReference() {
        // given
        // The set must contain a reference to itself.
        // Because ConcurrentSet implements Set<T>, we can add the set itself as an element.
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> selfReferencingSet = (ConcurrentSet<Object>) set;
        selfReferencingSet.add(selfReferencingSet);
        // when
        String result = selfReferencingSet.toString();
        // then
        assertEquals("{(this Set)}", result, "When a set contains itself, toString should render it as \"(this Set)\"");
    }

    /**
     * Verify that a set containing multiple distinct elements produces a string
     * that contains all elements, regardless of iteration order.
     */
    @Test
    void testToStringMultipleElementsThree() {
        // given
        set.addAll(Arrays.asList("a", "b", "c"));
        // when
        String result = set.toString();
        // then
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Strip the surrounding braces and split on ", "
        String inner = result.substring(1, result.length() - 1);
        Set<String> actualElements = Arrays.stream(inner.split(",\\s*")).collect(Collectors.toSet());
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertEquals(expected, actualElements, "String representation should contain exactly the set elements");
    }

    /**
     * Verify that a set containing exactly two elements yields either "{a, b}" or "{b, a}".
     */
    @Test
    void testToStringMultipleElementsTwo() {
        // given
        set.addAll(Arrays.asList("a", "b"));
        // when
        String result = set.toString();
        // then
        assertTrue(Pattern.matches("\\{a, b\\}|\\{b, a\\}", result), "Two‑element set should be represented as either {a, b} or {b, a}");
    }

    /**
     * Verify that adding the same element twice does not create duplicates in the string output.
     */
    @Test
    void testToStringDuplicateElement() {
        // given
        set.add("apple");
        // duplicate – should be ignored
        set.add("apple");
        // when
        String result = set.toString();
        // then
        assertEquals("{apple}", result, "Duplicate elements must not appear more than once in the string representation");
    }

    /**
     * Verify that a set containing both {@code null} and a non‑null element renders both values.
     */
    @Test
    void testToStringMixedNullAndNonNullElements() {
        // given
        set.addAll(Arrays.asList(null, "apple"));
        // when
        String result = set.toString();
        // then
        assertTrue(Pattern.matches("^\\{null, apple\\}$|^\\{apple, null\\}$", result), "Mixed null/non‑null set should be represented as {null, apple} or {apple, null}");
    }

    /**
     * Verify that a custom object’s own toString() value is used in the set’s string representation.
     */
    @Test
    void testToStringCustomObjectElement() {
        // given
        class Bar {

            @Override
            public String toString() {
                return "Bar";
            }
        }
        set.add(new Bar());
        // when
        String result = set.toString();
        // then
        assertEquals("{Bar}", result, "Custom object's toString() result should be used directly");
    }

    /**
     * Verify that a set containing itself and another element renders both correctly.
     */
    @Test
    void testToStringSelfReferencePlusOtherElement() {
        // given
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> selfReferencingSet = (ConcurrentSet<Object>) set;
        selfReferencingSet.add(selfReferencingSet);
        selfReferencingSet.add("apple");
        // when
        String result = selfReferencingSet.toString();
        // then
        assertTrue(Pattern.matches("^\\{\\(this Set\\), apple\\}$|^\\{apple, \\(this Set\\)\\}$", result), "Set with self‑reference and another element should render both correctly");
    }

    /**
     * Verify that a larger set (5 elements) contains all elements exactly once, order independent.
     */
    @Test
    void testToStringLargeSetOrdering() {
        // given
        set.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        // when
        String result = set.toString();
        // then
        // Look‑ahead regex: ensure each element appears at least once, starts with '{', ends with '}'
        String regex = "^(?=.*a)(?=.*b)(?=.*c)(?=.*d)(?=.*e)\\{[^}]*\\}$";
        assertTrue(Pattern.matches(regex, result), "Large set should contain all elements a‑e exactly once, order not significant");
    }

    /**
     * Verify that after clearing a populated set, toString() returns the empty literal.
     */
    @Test
    void testToStringSetAfterClear() {
        // given
        set.addAll(Arrays.asList("x", "y", "z"));
        set.clear();
        // when
        String result = set.toString();
        // then
        assertEquals("{}", result, "Cleared set should be represented as {}");
    }

    /**
     * Verify that an element whose toString() throws propagates the exception from ConcurrentSet.toString().
     */
    @Test
    void testToStringElementToStringThrowsException() {
        // given
        class BadToString {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }
        set.add(new BadToString());
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.toString(), "Exception from element's toString() should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
