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
class CaseInsensitiveSet_containsAll_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test
        set.clear();
    }

    /**
     * Parameter source for basic functionality tests.
     *
     * Each {@code Arguments} contains:
     * <ul>
     *   <li>the elements to pre‑populate the set with</li>
     *   <li>the collection passed to {@code containsAll}</li>
     *   <li>the expected boolean result</li>
     *   <li>a short description for the assertion message</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicFunctionalityProvider() {
        return Stream.of(// empty collection → true
        org.junit.jupiter.params.provider.Arguments.of(List.of("a", "b", "c"), Collections.emptyList(), true, "Empty collection should be considered contained"), // all elements present → true
        org.junit.jupiter.params.provider.Arguments.of(List.of("a", "b", "c"), List.of("a", "b"), true, "All elements are present"), // missing element → false
        org.junit.jupiter.params.provider.Arguments.of(List.of("a", "b", "c"), List.of("a", "c", "d"), false, "Element 'd' is missing"));
    }

    @Test
    void containsAll_AllElementsPresent_IrrespectiveOfCase() {
        // given
        set.add("One");
        set.add("Two");
        set.add("Three");
        Collection<String> query = Arrays.asList("one", "TWO", "ThReE");
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "Set should contain all elements regardless of case");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        // given
        set.add("Apple");
        set.add("Banana");
        Collection<String> query = Arrays.asList("apple", "banana", "cherry");
        // when
        boolean result = set.containsAll(query);
        // then
        assertFalse(result, "Set does not contain 'cherry', so containsAll must be false");
    }

    @Test
    void containsAll_MixedTypes_AllPresent() {
        // given
        set.add("Hello");
        set.add(42);
        set.add(true);
        Collection<Object> query = Arrays.asList("HELLO", 42, true);
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "Set should correctly handle mixed String and non‑String types");
    }

    @Test
    void containsAll_EmptyCollection_AlwaysTrue() {
        // given
        // set may be empty or not; does not matter
        set.add("Anything");
        Collection<Object> empty = Collections.emptyList();
        // when
        boolean result = set.containsAll(empty);
        // then
        assertTrue(result, "containsAll on an empty collection must return true");
    }

    @Test
    void containsAll_WithNullElement_WhenSetContainsNull() {
        // given
        set.add(null);
        set.add("test");
        Collection<Object> query = Arrays.asList(null, "TEST");
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "Set containing null should report true when collection also contains null");
    }

    @Test
    void containsAll_WithNullElement_WhenSetDoesNotContainNull() {
        // given
        set.add("test");
        Collection<Object> query = Collections.singletonList(null);
        // when
        boolean result = set.containsAll(query);
        // then
        assertFalse(result, "Set without null should report false when collection contains null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicFunctionalityProvider")
    void containsAll_BasicFunctionality(List<String> initialElements, Collection<String> query, boolean expected, String description) {
        // populate the set
        initialElements.forEach(set::add);
        // invoke
        boolean result = set.containsAll(query);
        // verify
        assertEquals(expected, result, description);
    }

    @Test
    void containsAll_NullCollection_ThrowsNullPointerException() {
        // given
        set.add("anything");
        // when / then
        assertThrows(NullPointerException.class, () -> set.containsAll(null), "Passing a null collection must throw NullPointerException");
    }

    @Test
    void containsAll_CollectionWithNullElement_ThrowsNullPointerException() {
        // given
        set.add("alpha");
        set.add("beta");
        Collection<Object> query = Arrays.asList("alpha", null);
        // when / then
        assertThrows(NullPointerException.class, () -> set.containsAll(query), "A collection containing null should cause NullPointerException because the backing map does not allow null keys");
    }

    @Test
    void containsAll_CaseInsensitiveBehaviour() {
        // given
        // stored as "A"
        set.add("A");
        // stored as "b"
        set.add("b");
        Collection<String> query = Arrays.asList("a", "B");
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "Case‑insensitive matching should treat 'a' and 'A' as equal, likewise for 'B' and 'b'");
    }

    @Test
    void containsAll_MixedCaseCollection_AllLogicalElementsPresent() {
        // given
        set.add("x");
        set.add("y");
        Collection<String> query = Arrays.asList("X", "x", "Y");
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "Mixed‑case collection where all logical elements exist should return true");
    }

    @Test
    void containsAll_MixedCaseCollection_WithMissingElement() {
        // given
        set.add("x");
        set.add("y");
        Collection<String> query = Arrays.asList("X", "y", "z");
        // when
        boolean result = set.containsAll(query);
        // then
        assertFalse(result, "Presence of a non‑existent element ('z') should make the result false");
    }

    @Test
    void containsAll_TypeMismatch_ThrowsClassCastException() {
        // The set is populated with a String element; the underlying CaseInsensitiveMap
        // expects String keys and will attempt a cast when checking containsKey.
        set.add("string");
        // Integer – incompatible with the expected String key type
        Collection<Object> query = List.of(123);
        assertThrows(ClassCastException.class, () -> set.containsAll(query), "Providing an element of an incompatible type should trigger ClassCastException");
    }

    @Test
    void containsAll_NonStringElements_NoClassCastException() {
        // given a set that contains only non‑String elements, containsAll should work
        set.add(42);
        set.add(true);
        Collection<Object> query = List.of(42, true);
        // when
        boolean result = set.containsAll(query);
        // then
        assertTrue(result, "When the set holds only non‑String elements, containsAll should behave like a normal Set");
    }
}
