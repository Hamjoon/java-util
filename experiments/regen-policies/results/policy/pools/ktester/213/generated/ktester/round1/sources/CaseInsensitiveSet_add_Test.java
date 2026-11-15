package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_add_Test {

    /**
     * Set used for the majority of the tests (String elements).
     */
    private CaseInsensitiveSet<String> stringSet;

    /**
     * Set used for mixed‑type tests (String + non‑String).
     */
    private CaseInsensitiveSet<Object> mixedSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation.
        stringSet = new CaseInsensitiveSet<>();
        mixedSet = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets – not strictly required because a new instance is created per test,
        // but it demonstrates the intended cleanup pattern.
        stringSet.clear();
        mixedSet.clear();
    }

    @Test
    void addNewElementReturnsTrue() {
        assertTrue(stringSet.add("Hello"));
        assertTrue(stringSet.contains("Hello"));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addDuplicateElementReturnsFalse() {
        assertTrue(stringSet.add("World"));
        // Adding the same element again (identical case) must return false.
        assertFalse(stringSet.add("World"));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addDuplicateElementDifferentCaseReturnsFalse() {
        assertTrue(stringSet.add("Case"));
        // Different case, but logically equal for the set.
        assertFalse(stringSet.add("cAsE"));
        assertEquals(1, stringSet.size());
        // Original case is preserved.
        Iterator<String> it = stringSet.iterator();
        assertTrue(it.hasNext());
        assertEquals("Case", it.next());
    }

    @Test
    void addMultipleDistinctElements() {
        assertTrue(stringSet.add("One"));
        assertTrue(stringSet.add("Two"));
        assertTrue(stringSet.add("Three"));
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("One", "Two", "Three")));
    }

    @Test
    void addMixedTypesRespectsCaseInsensitivityOnlyForStrings() {
        // Use a set that accepts Object to store both strings and numbers.
        assertTrue(mixedSet.add("Apple"));
        // non‑String element
        assertTrue(mixedSet.add(42));
        assertTrue(mixedSet.add("banana"));
        // Duplicate string with different case – should be rejected.
        assertFalse(mixedSet.add("APPLE"));
        // Different numeric type – allowed.
        assertTrue(mixedSet.add(42L));
        assertEquals(4, mixedSet.size());
        assertTrue(mixedSet.contains("Apple"));
        assertTrue(mixedSet.contains(42));
        assertTrue(mixedSet.contains("banana"));
        assertTrue(mixedSet.contains(42L));
    }

    @Test
    void addNullFirstTimeReturnsTrue() {
        assertTrue(stringSet.add(null));
        assertTrue(stringSet.contains(null));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addNullSecondTimeReturnsFalse() {
        assertTrue(stringSet.add(null));
        assertFalse(stringSet.add(null));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addWithExplicitBackingMapPreservesBehaviour() {
        // Explicitly create a CaseInsensitiveMap backed by a LinkedHashMap to guarantee order.
        Map<String, Object> backing = new CaseInsensitiveMap<>(new LinkedHashMap<>());
        // The public API does not expose a constructor taking a map directly, so we use reflection.
        CaseInsensitiveSet<String> orderedSet = createSetWithBackingMap(backing);
        assertTrue(orderedSet.add("First"));
        assertTrue(orderedSet.add("Second"));
        // Duplicate with different case – should not be added.
        assertFalse(orderedSet.add("first"));
        List<String> iterationOrder = new ArrayList<>();
        orderedSet.forEach(iterationOrder::add);
        assertEquals(Arrays.asList("First", "Second"), iterationOrder);
    }

    @Test
    void addAllUsesAddInternallyAndRespectsReturnValue() {
        // Spy on the set to verify that add(E) is invoked for each element.
        CaseInsensitiveSet<String> spySet = spy(new CaseInsensitiveSet<>());
        // "ALPHA" is duplicate
        List<String> toAdd = Arrays.asList("Alpha", "Beta", "ALPHA");
        // addAll should return true because at least one element changes the set.
        assertTrue(spySet.addAll(toAdd));
        // Verify internal calls.
        verify(spySet, times(3)).add(anyString());
        // Final state must contain only the distinct case‑insensitive values.
        assertEquals(2, spySet.size());
        assertTrue(spySet.contains("Alpha"));
        assertTrue(spySet.contains("Beta"));
    }

    @Test
    void addNullThrowsNullPointerException() {
        // The default backing map (ConcurrentSkipListMap) does not allow null keys.
        assertThrows(NullPointerException.class, () -> stringSet.add(null));
    }

    @Test
    void addStringWithWhitespaceIsDistinct() {
        assertTrue(stringSet.add("  Alpha  "));
        assertTrue(stringSet.contains("  Alpha  "));
        assertEquals(1, stringSet.size());
        // Adding the same word without spaces is a different element.
        assertTrue(stringSet.add("Alpha"));
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("  Alpha  ", "Alpha")));
    }

    @Test
    void addNonStringElementThrowsClassCastException() {
        // The underlying CaseInsensitiveMap uses a String comparator; inserting a non‑String triggers CCE.
        assertThrows(ClassCastException.class, () -> mixedSet.add(42));
    }

    @Test
    void addNewStringInstanceReturnsTrueThenFalseOnDuplicate() {
        String beta1 = new String("Beta");
        String beta2 = "Beta";
        assertTrue(stringSet.add(beta1));
        assertFalse(stringSet.add(beta2));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addInLargeSetContext() {
        // Populate set with a..z
        for (char c = 'a'; c <= 'z'; c++) {
            assertTrue(stringSet.add(String.valueOf(c)));
        }
        assertEquals(26, stringSet.size());
        // Add a distinct element
        assertTrue(stringSet.add("Omega"));
        assertEquals(27, stringSet.size());
        // Adding case‑insensitive duplicate should fail
        assertFalse(stringSet.add("omega"));
        assertEquals(27, stringSet.size());
    }

    @Test
    void addPrecomposedUnicodeCharacter() {
        // U+00E9 = é (precomposed)
        assertTrue(stringSet.add("\u00E9"));
        assertTrue(stringSet.contains("\u00E9"));
        assertEquals(1, stringSet.size());
    }

    @Test
    void addDecomposedUnicodeSequenceIsDistinct() {
        // 'e' + combining acute accent (U+0301) – different from precomposed é
        assertTrue(stringSet.add("e\u0301"));
        assertTrue(stringSet.contains("e\u0301"));
        assertEquals(1, stringSet.size());
        // Both forms can coexist because the comparator is case‑insensitive only, not Unicode‑normalizing.
        assertTrue(stringSet.add("\u00E9"));
        assertEquals(2, stringSet.size());
    }

    /**
     * Helper that uses reflection to invoke the package‑private constructor
     * {@code CaseInsensitiveSet(Collection<?>, Map<E, Object>)}.
     */
    @SuppressWarnings("unchecked")
    private <E> CaseInsensitiveSet<E> createSetWithBackingMap(Map<E, Object> backing) {
        try {
            java.lang.reflect.Constructor<CaseInsensitiveSet> ctor = CaseInsensitiveSet.class.getDeclaredConstructor(Collection.class, Map.class);
            ctor.setAccessible(true);
            return ctor.newInstance(Collections.emptySet(), backing);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create set with custom backing map", e);
        }
    }
}
