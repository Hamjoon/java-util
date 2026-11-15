package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toString_Test {

    /**
     * Subclass that deliberately returns {@code null} from {@code iterator()}.
     * Invoking {@code toString()} on an instance of this class should throw
     * a {@link NullPointerException} because the implementation does not
     * guard against a {@code null} iterator.
     */
    private static class NullIteratorSet<T> extends ConcurrentSet<T> {

        @Override
        public Iterator<T> iterator() {
            // break the contract intentionally
            return null;
        }
    }

    /**
     * Helper that converts the string returned by {@code ConcurrentSet.toString()}
     *  into a {@code Set<String>} of the element representations (without the surrounding braces).
     */
    private static Set<String> elementsFromToString(String s) {
        assertTrue(s.startsWith("{") && s.endsWith("}"), "String representation must start with '{' and end with '}'");
        String inner = s.substring(1, s.length() - 1).trim();
        if (inner.isEmpty()) {
            return Collections.emptySet();
        }
        // Split on ", " – this is exactly how the implementation separates elements
        return Arrays.stream(inner.split(", ")).map(String::trim).collect(Collectors.toSet());
    }

    /**
     * Data for the “Single Element” group.
     */
    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(42, "{42}"));
    }

    /**
     * Data for the “Multiple Elements” group.
     */
    private static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of(Arrays.asList("a", "b", "c"), "{a, b, c}"), Arguments.of(Arrays.asList(1, 2, 3, 4), "{1, 2, 3, 4}"));
    }

    /**
     * Data for the “Self‑reference Element” group.
     */
    private static Stream<Arguments> selfReferenceProvider() {
        return Stream.of(// Only the set itself
        Arguments.of(Collections.emptyList(), "{(this Set)}"), // Set itself plus another element
        Arguments.of(Collections.singletonList(99), "{(this Set), 99}"));
    }

    @Test
    void testToStringEmpty() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertEquals("{}", set.toString(), "Empty set should be represented as {}");
    }

    @Test
    void testToStringMultipleElements() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        String result = set.toString();
        // Verify format
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
        // Verify that all elements appear, order is not important
        Set<String> elems = elementsFromToString(result);
        Set<String> expected = new HashSet<>(Arrays.asList("1", "2", "3"));
        assertEquals(expected, elems, "All added elements must appear in the string representation");
    }

    @Test
    void testToStringSelfReference() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        // The set contains itself – the implementation should render "(this Set)"
        assertTrue(set.add(set), "Adding the set to itself should succeed");
        String result = set.toString();
        assertEquals("{(this Set)}", result, "Self‑reference should be rendered as \"(this Set)\" inside the braces");
    }

    @Test
    void testToStringWithNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));
        String result = set.toString();
        // Parse elements
        Set<String> elems = elementsFromToString(result);
        Set<String> expected = new HashSet<>(Arrays.asList("a", "null", "b"));
        assertEquals(expected, elems, "Null element should be represented by the string \"null\" in the output");
    }

    @ParameterizedTest(name = "Single element {0} should produce {1}")
    @MethodSource("singleElementProvider")
    void testToStringSingleElement(Object element, String expectedString) {
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        assertTrue(set.add(element), "Adding the element should succeed");
        assertEquals(expectedString, set.toString(), "Single element representation must match exactly");
    }

    @ParameterizedTest(name = "Multiple elements {0} should produce a string containing all of them")
    @MethodSource("multipleElementsProvider")
    void testToStringMultipleElementsParameterized(List<?> elements, String expectedPattern) {
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(elements);
        String result = set.toString();
        // Basic format checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must be wrapped in braces");
        // Verify that each element appears (order is not important)
        Set<String> actualElems = elementsFromToString(result);
        Set<String> expectedElems = new HashSet<>();
        for (Object e : elements) {
            expectedElems.add(String.valueOf(e));
        }
        assertEquals(expectedElems, actualElems, "All supplied elements must be present exactly once");
    }

    @ParameterizedTest(name = "Self‑reference with extra elements {1}")
    @MethodSource("selfReferenceProvider")
    void testToStringSelfReferenceWithExtras(List<Integer> extraElements, String expectedString) {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        // Add the set itself first
        assertTrue(set.add(set), "Adding the set to itself should succeed");
        // Add any extra elements
        for (Integer i : extraElements) {
            assertTrue(set.add(i), "Adding extra element should succeed");
        }
        String result = set.toString();
        // The expected string is deterministic because we know the order:
        // the iterator will return the self‑reference first (it was added first),
        // then the subsequent elements in insertion order.
        assertEquals(expectedString, result, "String representation with self‑reference must match expected format");
    }

    @Test
    void testToStringIteratorReturnsNull() {
        ConcurrentSet<Integer> brokenSet = new NullIteratorSet<>();
        assertThrows(NullPointerException.class, brokenSet::toString, "When iterator() returns null, toString() should throw NPE");
    }
}
