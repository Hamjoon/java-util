package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_hashCode_Test {

    private static Stream<Arguments> nonEmptyHashCodeProvider() {
        return Stream.of(Arguments.of("CompactSet containing \"a\" and \"b\" should have hashCode 97+98=195", new HashSet<>(Arrays.asList("a", "b")), 195), Arguments.of("CompactSet containing integers 1, 2, 3 should have hashCode 1+2+3=6", new HashSet<>(Arrays.asList(1, 2, 3)), 6));
    }

    private static Stream<Arguments> orderIndependenceProvider() {
        return Stream.of(Arguments.of(Arrays.asList("x", "y", "z"), 363), Arguments.of(Arrays.asList("z", "x", "y"), 363), Arguments.of(Arrays.asList("y", "z", "x"), 363));
    }

    @Test
    void hashCodeOfEmptySetMatchesStandardHashSet() {
        Set<String> compact = new CompactSet<>();
        Set<String> baseline = new HashSet<>();
        assertTrue(compact.isEmpty(), "CompactSet should be empty");
        assertEquals(baseline.hashCode(), compact.hashCode(), "hashCode of an empty CompactSet must match hashCode of an empty HashSet");
    }

    @Test
    void hashCodeMatchesStandardSetAfterAddingElements() {
        CompactSet<String> compact = new CompactSet<>();
        compact.add("apple");
        compact.add("banana");
        compact.add("cherry");
        Set<String> baseline = new HashSet<>();
        baseline.add("apple");
        baseline.add("banana");
        baseline.add("cherry");
        assertEquals(baseline.hashCode(), compact.hashCode(), "hashCode should be based on the set of elements, independent of internal representation");
    }

    @Test
    void hashCodeIsConsistentAcrossMultipleInvocations() {
        CompactSet<Integer> compact = new CompactSet<>();
        compact.add(1);
        compact.add(2);
        compact.add(3);
        int first = compact.hashCode();
        int second = compact.hashCode();
        int third = compact.hashCode();
        assertEquals(first, second, "hashCode must be deterministic");
        assertEquals(second, third, "hashCode must be deterministic across calls");
    }

    @Test
    void hashCodeChangesWhenSetIsModified() {
        CompactSet<String> compact = new CompactSet<>();
        compact.add("alpha");
        int hashBefore = compact.hashCode();
        compact.add("beta");
        int hashAfterAdd = compact.hashCode();
        assertNotEquals(hashBefore, hashAfterAdd, "hashCode must change after adding a new element");
        compact.remove("alpha");
        int hashAfterRemove = compact.hashCode();
        assertNotEquals(hashAfterAdd, hashAfterRemove, "hashCode must change after removing an element");
    }

    @Test
    void hashCodeRespectsBuilderConfiguration() {
        // Builder with case‑insensitive, sorted order – the hashCode contract is still based on element values
        CompactSet<String> compact = CompactSet.<String>builder().caseSensitive(false).sortedOrder().compactSize(50).build();
        compact.add("Zebra");
        compact.add("apple");
        compact.add("Monkey");
        // The baseline set uses the same logical elements (case‑insensitive view means the stored keys are lower‑cased)
        Set<String> baseline = new HashSet<>();
        baseline.add("zebra");
        baseline.add("apple");
        baseline.add("monkey");
        assertEquals(baseline.hashCode(), compact.hashCode(), "hashCode must reflect the logical content of the set, regardless of builder options");
    }

    /**
     * Empty CompactSet hashCode – expected to be 0 (same as empty HashSet).
     */
    @Test
    void emptyCompactSetHashCodeIsZero() {
        CompactSet<String> compact = new CompactSet<>();
        assertEquals(0, compact.hashCode(), "A newly created empty CompactSet should have hashCode 0, matching an empty HashSet");
    }

    /**
     * Non‑empty sets with deterministic hash codes.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonEmptyHashCodeProvider")
    void nonEmptyCompactSetHashCode(String description, Set<?> elements, int expectedHash) {
        // Build a CompactSet with the same elements
        CompactSet<Object> compact = new CompactSet<>();
        elements.forEach(compact::add);
        // Compute expected hash using a standard HashSet (the contract of Set.hashCode())
        Set<Object> baseline = new HashSet<>(elements);
        assertEquals(expectedHash, baseline.hashCode(), "Sanity check – expected hash calculated from baseline HashSet");
        assertEquals(expectedHash, compact.hashCode(), description);
    }

    /**
     * Order‑independence test – the hash code must be identical regardless of insertion order.
     */
    @ParameterizedTest(name = "{index}: order {0}")
    @MethodSource("orderIndependenceProvider")
    void hashCodeOrderIndependence(List<String> insertionOrder, int expectedHash) {
        CompactSet<String> compact = new CompactSet<>();
        insertionOrder.forEach(compact::add);
        // Baseline HashSet (order‑independent)
        Set<String> baseline = new HashSet<>(Arrays.asList("x", "y", "z"));
        assertEquals(expectedHash, baseline.hashCode(), "Sanity check – expected hash from baseline HashSet");
        assertEquals(expectedHash, compact.hashCode(), "hashCode must be independent of the order elements were added");
    }

    /**
     * CompactSet containing a {@code null} element.
     */
    @Test
    void hashCodeWithNullElement() {
        CompactSet<String> compact = new CompactSet<>();
        compact.add(null);
        compact.add("a");
        Set<String> baseline = new HashSet<>();
        baseline.add(null);
        baseline.add("a");
        // HashSet treats null as 0 in its hashCode calculation
        // 0 + 97 = 97
        int expected = 0 + "a".hashCode();
        assertEquals(expected, baseline.hashCode(), "Sanity check – baseline hash matches expected 97");
        assertEquals(expected, compact.hashCode(), "CompactSet hashCode with null element should equal baseline hash (97)");
    }

    /**
     * Hash code after removal of an element.
     */
    @Test
    void hashCodeAfterRemoval() {
        CompactSet<String> compact = new CompactSet<>();
        compact.add("a");
        compact.add("b");
        compact.add("c");
        // Verify initial hash (97+98+99 = 294)
        int initial = compact.hashCode();
        assertEquals(294, initial, "Initial hash should be 294");
        compact.remove("b");
        int afterRemoval = compact.hashCode();
        assertEquals(196, afterRemoval, "After removing \"b\", hashCode should be 97+99 = 196");
    }

    /**
     * Hash code after calling {@code clear()}.
     */
    @Test
    void hashCodeAfterClear() {
        CompactSet<Integer> compact = new CompactSet<>();
        compact.add(10);
        compact.add(20);
        assertNotEquals(0, compact.hashCode(), "Non‑empty set should not have hashCode 0");
        compact.clear();
        assertEquals(0, compact.hashCode(), "After clear(), hashCode must revert to 0 (empty set)");
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the internal
     * {@code map} field is forcibly set to {@code null}.
     *
     * <p>This test uses reflection because the field is private and final.
     * The implementation of {@code hashCode()} dereferences {@code map}
     * without a null‑check, so a NPE is expected.</p>
     */
    @Test
    void nullMapThrowsNPE() throws Exception {
        CompactSet<String> compact = new CompactSet<>();
        // Access the private final field 'map'
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // Remove the final modifier (required for Java 12+)
        // The following hack works for test purposes.
        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(mapField, mapField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        // Set the field to null
        mapField.set(compact, null);
        // Expect NPE when hashCode() is invoked
        assertThrows(NullPointerException.class, compact::hashCode, "hashCode() should throw NullPointerException when internal map is null");
    }
}
