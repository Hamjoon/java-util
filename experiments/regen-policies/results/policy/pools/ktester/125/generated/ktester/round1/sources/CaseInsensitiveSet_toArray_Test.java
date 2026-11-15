package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toArray_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor which employs a CaseInsensitiveMap with insertion order
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to ensure isolation between tests
        set.clear();
    }

    private static Stream<Arguments> provideBasicToArrayCases() {
        return Stream.of(// 1. empty set → empty array
        Arguments.of("Empty set should return an empty Object[]", (Consumer<CaseInsensitiveSet<String>>) s -> {
        }, /* no elements */
        new Object[0]), // 2. single element
        Arguments.of("Single element set should return an array with that element", (Consumer<CaseInsensitiveSet<String>>) s -> s.add("hello"), new Object[] { "hello" }), // 3. case‑insensitive duplicate
        Arguments.of("Adding \"test\" then \"TEST\" must keep only the first entry", (Consumer<CaseInsensitiveSet<String>>) s -> {
            s.add("test");
            s.add("TEST");
        }, new Object[] { "test" }));
    }

    @Test
    void toArrayReturnsAllElementsInInsertionOrder() {
        // given
        set.add("Apple");
        set.add("banana");
        set.add("Cherry");
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(3, array.length);
        assertEquals("Apple", array[0]);
        assertEquals("banana", array[1]);
        assertEquals("Cherry", array[2]);
    }

    @Test
    void toArrayIsCaseInsensitiveForContainmentButPreservesOriginalCaseInResult() {
        // given
        set.add("Hello");
        // Adding same word with different case should not create a new entry
        set.add("HELLO");
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(1, array.length);
        // The original case that was first added must be preserved
        assertEquals("Hello", array[0]);
    }

    @Test
    void toArrayWorksWithMixedTypes() {
        // given
        CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
        mixedSet.add("One");
        mixedSet.add(42);
        mixedSet.add("TWO");
        mixedSet.add(3.14);
        // when
        Object[] array = mixedSet.toArray();
        // then
        assertEquals(4, array.length);
        // Verify that string elements keep their original case
        assertTrue(Arrays.asList(array).contains("One"));
        assertTrue(Arrays.asList(array).contains("TWO"));
        // Verify non‑string elements are present unchanged
        assertTrue(Arrays.asList(array).contains(42));
        assertTrue(Arrays.asList(array).contains(3.14));
    }

    @Test
    void toArrayWithTypedArrayParameterReturnsCorrectRuntimeType() {
        // given
        set.add("alpha");
        set.add("BETA");
        set.add("Gamma");
        // when
        String[] typedArray = set.toArray(new String[0]);
        // then
        assertEquals(3, typedArray.length);
        assertArrayEquals(new String[] { "alpha", "BETA", "Gamma" }, typedArray);
        // Ensure the runtime type of the returned array is exactly String[]
        assertEquals(String[].class, typedArray.getClass());
    }

    @Test
    void toArrayWithLargerTypedArrayUsesSuppliedArray() {
        // given
        set.add("x");
        set.add("y");
        String[] supplied = new String[5];
        supplied[2] = "shouldBeOverwritten";
        // when
        String[] result = set.toArray(supplied);
        // then
        // The returned array should be the same instance that was supplied
        assertSame(supplied, result);
        // First two positions must contain the set elements in insertion order
        assertEquals("x", result[0]);
        assertEquals("y", result[1]);
        // The element after the last set entry must be null per the contract
        assertNull(result[2]);
    }

    /**
     * Parameterised test that covers the first three JSON cases:
     * <ul>
     *   <li>empty set</li>
     *   <li>single element</li>
     *   <li>case‑insensitive duplicate handling</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideBasicToArrayCases")
    void toArrayBasicScenarios(String description, Consumer<CaseInsensitiveSet<String>> setup, Object[] expected) {
        // given
        setup.accept(set);
        // when
        Object[] result = set.toArray();
        // then
        assertArrayEquals(expected, result, description);
    }

    /**
     * Verifies that when the backing map is a case‑insensitive {@link TreeMap},
     * {@code toArray()} returns elements sorted ignoring case.
     *
     * <p>This test uses reflection to replace the private {@code map} field with a
     * {@code TreeMap<String, Object>} that uses {@link String#CASE_INSENSITIVE_ORDER}
     * as its comparator.</p>
     */
    @Test
    void toArrayReturnsElementsSortedWhenUsingCaseInsensitiveTreeMap() throws Exception {
        // Replace the internal map with a TreeMap that sorts case‑insensitively
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> sortedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        mapField.set(set, sortedMap);
        // Add elements in a non‑sorted order
        set.add("banana");
        set.add("Apple");
        set.add("Cherry");
        // when
        Object[] result = set.toArray();
        // then – expected order is case‑insensitive natural order: Apple, banana, Cherry
        assertArrayEquals(new Object[] { "Apple", "banana", "Cherry" }, result);
    }

    /**
     * After calling {@link CaseInsensitiveSet#clear()}, the set must behave as an empty set.
     */
    @Test
    void toArrayAfterClearReturnsEmptyArray() {
        // given – populate the set with a few values
        set.add("first");
        set.add("second");
        set.add("third");
        // when – clear the set
        set.clear();
        // then – toArray should return an empty array
        Object[] result = set.toArray();
        assertNotNull(result);
        assertEquals(0, result.length, "Array should be empty after clear()");
    }
}
