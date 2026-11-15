package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// No Mockito usage currently, but kept for consistency with the template
@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_remove_Test {

    /**
     * The set used in each test.
     */
    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for every test to guarantee isolation.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set – not strictly necessary but makes intent clear.
        set.clear();
    }

    private static Stream<Arguments> sizeChangeProvider() {
        return Stream.of(// Removing an existing element ("test")
        Arguments.of("test", true, true, "Removing an existing element should decrease map size and return true."), // Removing a non‑existent element ("absent")
        Arguments.of("absent", false, false, "Removing a non‑existent element should leave size unchanged and return false."), // Removing null when it is present
        Arguments.of(null, true, true, "If the set contains a null element, removing null reduces size and returns true."), // Removing null when it is absent
        Arguments.of(null, false, false, "If the set does not contain null, removing null leaves size unchanged and returns false."));
    }

    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of("any", false, "Removing any element from an empty CaseInsensitiveSet should leave the size unchanged and return false."));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(Arguments.of("TeSt", "test", true, "When the set contains \"TeSt\" (different case), calling remove(\"test\") should succeed because the set is case‑insensitive, decreasing the size and returning true."));
    }

    private static Stream<Arguments> differentTypeProvider() {
        return Stream.of(Arguments.of("someString", 1, false, "Attempting to remove an Integer from a CaseInsensitiveSet<String> should not affect the set and must return false."));
    }

    private static Stream<Arguments> equalDistinctInstanceProvider() {
        return Stream.of(Arguments.of("example", new String("example"), true, "If the set contains a String instance with value \"example\", removing another distinct String instance with the same content should succeed and return true."));
    }

    private static Stream<Arguments> lastElementProvider() {
        return Stream.of(Arguments.of("solo", "solo", true, "Removing the only element in the set should return true and leave the set empty (size becomes 0)."));
    }

    @Test
    void removeExistingStringIsCaseInsensitive() {
        // given
        set.add("HelloWorld");
        assertEquals(1, set.size());
        // when
        // different case
        boolean removed = set.remove("helloworld");
        // then
        assertTrue(removed, "remove should return true for an existing element");
        assertEquals(0, set.size(), "set should be empty after removal");
        assertFalse(set.contains("HelloWorld"), "set should no longer contain the removed element");
    }

    @Test
    void removeNonExistingStringReturnsFalse() {
        // given
        set.add("Alpha");
        set.add("Beta");
        assertEquals(2, set.size());
        // when
        // not present
        boolean removed = set.remove("Gamma");
        // then
        assertFalse(removed, "remove should return false when the element is absent");
        assertEquals(2, set.size(), "size must remain unchanged when nothing is removed");
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("Beta"));
    }

    @Test
    void removeNonStringElement() {
        // given
        Integer number = 42;
        set.add(number);
        set.add("FortyTwo");
        assertEquals(2, set.size());
        // when
        boolean removed = set.remove(number);
        // then
        assertTrue(removed, "remove should succeed for a present non‑String element");
        assertEquals(1, set.size());
        assertFalse(set.contains(number));
        assertTrue(set.contains("FortyTwo"));
    }

    @Test
    void removeNullWhenNotPresentReturnsFalse() {
        // given – set does not contain null
        set.add("NotNull");
        assertEquals(1, set.size());
        // when
        boolean removed = set.remove(null);
        // then
        assertFalse(removed, "remove(null) should return false when null is not in the set");
        assertEquals(1, set.size());
    }

    @Test
    void removeNullWhenPresentReturnsTrue() {
        // given
        set.add(null);
        set.add("Something");
        assertEquals(2, set.size());
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove(null) should return true when null is present");
        assertEquals(1, set.size());
        assertFalse(set.contains(null));
        assertTrue(set.contains("Something"));
    }

    @Test
    void removeMultipleElementsSequentially() {
        // given
        set.add("One");
        set.add("Two");
        set.add("Three");
        set.add(100);
        assertEquals(4, set.size());
        // when / then – remove each element and verify the return value and size
        // case‑insensitive
        assertTrue(set.remove("one"));
        assertEquals(3, set.size());
        assertTrue(set.remove(100));
        assertEquals(2, set.size());
        assertTrue(set.remove("TWO"));
        assertEquals(1, set.size());
        // final element
        assertTrue(set.remove("three"));
        assertEquals(0, set.size());
        // subsequent removal should return false
        assertFalse(set.remove("one"));
        assertFalse(set.remove(100));
    }

    @Test
    void removeDoesNotAffectUnrelatedElements() {
        // given
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        assertEquals(3, set.size());
        // when
        // case‑insensitive removal
        boolean removed = set.remove("banana");
        // then
        assertTrue(removed);
        assertEquals(2, set.size());
        assertTrue(set.contains("Apple"));
        assertTrue(set.contains("Cherry"));
        assertFalse(set.contains("Banana"));
    }

    /**
     * Group: remove(Object) - size change detection
     *
     * Scenarios:
     *   1. Existing element ("test") → true
     *   2. Absent element ("absent") → false
     *   3. Null present → true
     *   4. Null absent → false
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("sizeChangeProvider")
    void removeSizeChangeDetection(Object toRemove, boolean initiallyPresent, boolean expectedResult, String description) {
        // Arrange – optionally add the element before removal
        if (initiallyPresent) {
            set.add(toRemove);
        }
        // Act
        boolean actual = set.remove(toRemove);
        // Assert
        assertEquals(expectedResult, actual, description);
        // Verify size change only when removal succeeded
        if (expectedResult) {
            assertEquals(initiallyPresent ? 0 : 1, set.size(), "Size should reflect removal of the element");
        } else {
            assertEquals(initiallyPresent ? 1 : 0, set.size(), "Size should remain unchanged when removal fails");
        }
    }

    /**
     * Group: remove(Object) - empty set
     *
     * Removing any element from an empty set must return false and keep the size at zero.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("emptySetProvider")
    void removeFromEmptySet(Object toRemove, boolean expectedResult, String description) {
        // No elements are added – the set is empty.
        assertEquals(0, set.size(), "Pre‑condition: set must be empty");
        boolean actual = set.remove(toRemove);
        assertEquals(expectedResult, actual, description);
        assertEquals(0, set.size(), "Size must remain zero after removal attempt on empty set");
    }

    /**
     * Group: remove(Object) - case‑insensitive removal
     *
     * The set contains "TeSt". Removing "test" (different case) must succeed.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("caseInsensitiveProvider")
    void caseInsensitiveRemoval(String stored, String toRemove, boolean expectedResult, String description) {
        set.add(stored);
        assertEquals(1, set.size());
        boolean actual = set.remove(toRemove);
        assertEquals(expectedResult, actual, description);
        assertEquals(expectedResult ? 0 : 1, set.size(), "Size should reflect whether the element was removed");
    }

    /**
     * Group: remove(Object) - different type removal
     *
     * Removing an Integer from a {@code CaseInsensitiveSet<String>} must return false.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("differentTypeProvider")
    void differentTypeRemoval(String stored, Object toRemove, boolean expectedResult, String description) {
        // Use a raw set that can hold heterogeneous types, but we only add a String.
        set.add(stored);
        assertEquals(1, set.size());
        boolean actual = set.remove(toRemove);
        assertEquals(expectedResult, actual, description);
        assertEquals(1, set.size(), "Size must stay unchanged when removal of a different type fails");
    }

    /**
     * Group: remove(Object) - equal but distinct instance
     *
     * Removing a new {@code String} instance with the same content must succeed.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("equalDistinctInstanceProvider")
    void equalButDistinctInstanceRemoval(String stored, String toRemove, boolean expectedResult, String description) {
        set.add(stored);
        assertEquals(1, set.size());
        // Ensure we have distinct instances (not the same reference)
        assertNotSame(stored, toRemove, "Test setup: instances must be distinct");
        boolean actual = set.remove(toRemove);
        assertEquals(expectedResult, actual, description);
        assertEquals(0, set.size(), "Set should be empty after successful removal");
    }

    /**
     * Group: remove(Object) - removal of last element
     *
     * Removing the sole element must return true and leave the set empty.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("lastElementProvider")
    void removalOfLastElement(String stored, String toRemove, boolean expectedResult, String description) {
        set.add(stored);
        assertEquals(1, set.size());
        boolean actual = set.remove(toRemove);
        assertEquals(expectedResult, actual, description);
        assertEquals(0, set.size(), "Set should be empty after removing the last element");
    }
}
