package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_clear_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set before each test.
        set = new CaseInsensitiveSet<>();
        // Populate with a mixture of String (different cases) and non‑String values.
        set.addAll(Arrays.asList("One", "two", "THREE", 42, "FoO"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no accidental reuse.
        set = null;
    }

    /**
     * Uses reflection to replace the private {@code map} field of a {@link CaseInsensitiveSet}
     * instance. This is required for tests that need to inject a custom or null map.
     *
     * @param target the set whose internal map should be replaced
     * @param map    the new map (may be {@code null})
     */
    private static void setInternalMap(CaseInsensitiveSet<?> target, Map<?, ?> map) {
        try {
            Field f = CaseInsensitiveSet.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(target, map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set internal map via reflection", e);
        }
    }

    static Stream<Arguments> nonEmptySetProvider() {
        return Stream.of(// Case 1: two distinct strings, then clear
        Arguments.of(List.of("alpha", "BETA"), 0, true, Map.of("alpha", false, "BETA", false), // iteratorHasNext after clear
        false), // Case 2: single element, clear twice
        Arguments.of(List.of("single"), 0, true, Collections.emptyMap(), false));
    }

    @Test
    void clearRemovesAllElements() {
        // Pre‑condition: set is populated.
        assertEquals(5, set.size(), "Set should contain 5 elements before clear");
        // Invoke the method under test.
        set.clear();
        // Verify that the set is empty and size is zero.
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        // Verify that contains returns false for previously present elements.
        assertFalse(set.contains("One"));
        assertFalse(set.contains("ONE"));
        assertFalse(set.contains(42));
    }

    @Test
    void clearOnAlreadyEmptySet() {
        // Start with an empty set.
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        assertTrue(emptySet.isEmpty(), "Pre‑condition: set must be empty");
        // Clear should not throw and the set remains empty.
        emptySet.clear();
        assertTrue(emptySet.isEmpty(), "Set should still be empty after clear");
        assertEquals(0, emptySet.size(), "Size should remain 0 after clear");
    }

    @Test
    void setBehavesCorrectlyAfterClear() {
        // Clear the populated set.
        set.clear();
        // After clearing, the set should accept new elements and keep case‑insensitive semantics.
        set.add("Hello");
        // duplicate in different case – should not increase size
        set.add("hello");
        set.add(99);
        assertEquals(2, set.size(), "Set should contain 2 distinct elements after re‑adding");
        assertTrue(set.contains("HELLO"), "Case‑insensitive containment must work after clear");
        assertTrue(set.contains(99));
    }

    @Test
    void clearDoesNotAffectOtherInstances() {
        // Create a second independent set with its own elements.
        Set<Object> other = new CaseInsensitiveSet<>(Collections.singleton("OnlyMe"));
        // Clear the first set.
        set.clear();
        // Verify that the second set is untouched.
        assertEquals(1, other.size(), "Other set size must remain unchanged");
        assertTrue(other.contains("OnlyMe"));
    }

    @ParameterizedTest(name = "[{index}] clear on non‑empty set with elements {0}")
    @MethodSource("nonEmptySetProvider")
    void clearOnNonEmptySet(List<String> initialElements, int expectedSizeAfterClear, boolean expectedIsEmpty, Map<String, Boolean> containsExpectations, boolean iteratorHasNextAfterClear) {
        // Re‑initialise the set for this specific scenario.
        set = new CaseInsensitiveSet<>();
        set.addAll(initialElements);
        // Verify pre‑condition size.
        assertEquals(initialElements.size(), set.size(), "Pre‑condition: set should contain the initial elements");
        // First clear.
        set.clear();
        // Verify size and emptiness.
        assertEquals(expectedSizeAfterClear, set.size(), "Size after first clear");
        assertEquals(expectedIsEmpty, set.isEmpty(), "isEmpty after first clear");
        // Verify contains expectations.
        containsExpectations.forEach((elem, expected) -> assertEquals(expected, set.contains(elem), "contains('" + elem + "') after clear"));
        // For the second case we call clear again to ensure idempotence.
        if (initialElements.size() == 1) {
            // second clear – should not throw
            set.clear();
            assertEquals(expectedSizeAfterClear, set.size(), "Size after second clear (idempotence)");
        }
        // Iterator behavior after clear.
        Iterator<Object> it = set.iterator();
        assertEquals(iteratorHasNextAfterClear, it.hasNext(), "Iterator.hasNext() after clear");
    }

    @Test
    void clearAfterBulkAddition() {
        // Populate with 100 distinct strings.
        set = new CaseInsensitiveSet<>();
        List<String> hundred = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            hundred.add("elem" + i);
        }
        set.addAll(hundred);
        assertEquals(100, set.size(), "Pre‑condition: 100 elements added");
        // Clear and verify.
        set.clear();
        assertEquals(0, set.size(), "Size after clear should be 0");
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements after clear");
    }

    @Test
    void clearAfterAddingNullElement() {
        set = new CaseInsensitiveSet<>();
        assertTrue(set.add(null), "Adding null should succeed");
        assertTrue(set.contains(null), "Set must contain null after addition");
        set.clear();
        assertEquals(0, set.size(), "Size after clear should be 0");
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertFalse(set.contains(null), "Set must not contain null after clear");
    }

    @Test
    void clearAfterCaseInsensitiveDuplicateAddition() {
        set = new CaseInsensitiveSet<>();
        set.add("Alpha");
        // duplicate, case‑insensitive
        set.add("ALPHA");
        assertEquals(1, set.size(), "Duplicate addition should result in size 1");
        set.clear();
        assertEquals(0, set.size(), "Size after clear should be 0");
        assertTrue(set.isEmpty(), "Set should be empty after clear");
    }

    @Test
    void clearAfterRetainAllOperation() {
        set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size(), "Pre‑condition: three elements added");
        // Retain only "a"
        set.retainAll(Collections.singleton("a"));
        assertEquals(1, set.size(), "Size after retainAll should be 1");
        assertTrue(set.contains("a"), "Set should contain 'a' after retainAll");
        // Now clear
        set.clear();
        assertEquals(0, set.size(), "Size after clear should be 0");
        assertTrue(set.isEmpty(), "Set should be empty after clear");
    }

    @Test
    void clearAllowsReuseOfSet() {
        set = new CaseInsensitiveSet<>();
        set.add("first");
        assertEquals(1, set.size(), "Pre‑condition: one element added");
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        set.add("second");
        assertEquals(1, set.size(), "Size after re‑adding should be 1");
        assertTrue(set.contains("second"), "Set must contain the newly added element");
    }

    @Test
    void iteratorBehaviorAfterClear() {
        // Obtain iterator before clear.
        Iterator<Object> it = set.iterator();
        // Clear the set.
        set.clear();
        // The iterator should now report no elements.
        assertFalse(it.hasNext(), "Iterator.hasNext() must be false after clear");
        // next() should throw NoSuchElementException.
        assertThrows(NoSuchElementException.class, it::next, "Iterator.next() must throw NoSuchElementException after clear");
    }

    @Test
    void clearWithNullInternalMapThrowsNPE() {
        // Replace internal map with null via reflection.
        set = new CaseInsensitiveSet<>();
        setInternalMap(set, null);
        // Expect NullPointerException when clear() is invoked.
        assertThrows(NullPointerException.class, set::clear, "clear() should throw NPE when internal map is null");
    }

    @Test
    void clearWithUnmodifiableMapThrowsUnsupportedOperationException() {
        // Create an unmodifiable map and inject it.
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(new TreeMap<>());
        set = new CaseInsensitiveSet<>();
        setInternalMap(set, unmodifiable);
        // clear() should propagate UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, set::clear, "clear() should throw UnsupportedOperationException for unmodifiable map");
    }

    @Test
    void clearWhenUnderlyingMapThrowsRuntimeException() {
        // Mock a map that throws IllegalStateException on clear().
        @SuppressWarnings("unchecked")
        Map<Object, Object> mockMap = mock(Map.class);
        doThrow(new IllegalStateException("map clear failure")).when(mockMap).clear();
        set = new CaseInsensitiveSet<>();
        setInternalMap(set, mockMap);
        // The exception from the delegate map must be propagated unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::clear, "clear() should propagate IllegalStateException from underlying map");
        assertEquals("map clear failure", ex.getMessage());
    }
}
