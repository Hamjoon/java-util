package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_contains_Test {

    /**
     * Instance used by the individual test methods.
     */
    private CaseInsensitiveSet<Object> set;

    /**
     * Runs before each test – creates a fresh set for isolation.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new CaseInsensitiveSet<>();
        // Populate with a known mixture of values (used by the original tests).
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element
        set.add(42);
        // another non‑String
        set.add(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    }

    /**
     * Runs after each test – clears the reference for GC friendliness.
     */
    @AfterEach
    void teardownAfterEach() {
        set.clear();
        set = null;
    }

    private static Stream<Arguments> simpleContainsProvider() {
        // Helper to create a set pre‑populated as described by each case.
        // Cases that need a non‑empty set are built explicitly.
        return Stream.of(// 1. Empty set → false
        Arguments.of("apple", "false"), // 2. After adding "Apple" → true (case‑sensitive check)
        Arguments.of("Apple", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("Apple")), // 3. After adding "Apple" → true (case‑insensitive)
        Arguments.of("apple", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("Apple")), // 4. Element never added → false
        Arguments.of("Banana", "false"), // 5. Null argument → NPE (ConcurrentSkipListMap does not allow null keys)
        Arguments.of(null, "java.lang.NullPointerException"), // 6. Different type not present → false
        Arguments.of(new Object(), "false"), // 7. After adding "Orange" → true (mixed case)
        Arguments.of("Orange", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("Orange")), // 8. After adding "Orange" → true (lower‑case query)
        Arguments.of("orange", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("Orange")), // 9. Add "Apple", remove via lower‑case, then query any case → false
        Arguments.of("APPLE", "false", (Consumer<CaseInsensitiveSet<Object>>) s -> {
            s.add("Apple");
            s.remove("apple");
        }), // 10. Add "Apple", clear set, then query → false
        Arguments.of("Apple", "false", (Consumer<CaseInsensitiveSet<Object>>) s -> {
            s.add("Apple");
            s.clear();
        }), // 11. Empty string handling
        Arguments.of("", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("")), // 12. Whitespace string handling
        Arguments.of(" ", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add(" ")), // 13. Numeric string handling
        Arguments.of("123", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("123")), // 14. CharSequence (StringBuilder) not equal to stored String
        Arguments.of(new StringBuilder("Apple"), "false", (Consumer<CaseInsensitiveSet<Object>>) s -> s.add("Apple")), // 15. Duplicate addition (Apple then apple) → true
        Arguments.of("Apple", "true", (Consumer<CaseInsensitiveSet<Object>>) s -> {
            s.add("Apple");
            s.add("apple");
        })).map(arg -> {
            // Normalise arguments: if a Consumer is supplied, apply it to a fresh set before testing.
            if (arg.get()[2] instanceof Consumer) {
                @SuppressWarnings("unchecked")
                Consumer<CaseInsensitiveSet<Object>> mutator = (Consumer<CaseInsensitiveSet<Object>>) arg.get()[2];
                CaseInsensitiveSet<Object> preparedSet = new CaseInsensitiveSet<>();
                mutator.accept(preparedSet);
                // Replace the third argument with the prepared set for the test method.
                return Arguments.of(arg.get()[0], arg.get()[1], preparedSet);
            } else {
                // No mutator – use an empty set.
                return Arguments.of(arg.get()[0], arg.get()[1], new CaseInsensitiveSet<>());
            }
        });
    }

    /**
     * Verify case‑insensitive lookup for String elements.
     */
    @Test
    void containsString_IgnoresCase() {
        assertTrue(set.contains("One"));
        assertTrue(set.contains("one"));
        assertTrue(set.contains("ONe"));
        assertTrue(set.contains("tWo"));
        assertTrue(set.contains("THREE"));
        assertTrue(set.contains("three"));
        assertTrue(set.contains("ThReE"));
    }

    /**
     * Verify that a non‑present String returns {@code false}.
     */
    @Test
    void containsString_NotPresentReturnsFalse() {
        assertFalse(set.contains("Four"));
        assertFalse(set.contains("fOuR"));
    }

    /**
     * Verify that non‑String elements are compared using normal equality.
     */
    @Test
    void containsNonString_UsesStandardEquality() {
        Integer intValue = 42;
        assertTrue(set.contains(intValue));
        // different instance, same value
        assertTrue(set.contains(new Integer(42)));
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(set.contains(uuid));
        assertTrue(set.contains(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")));
        // Different object, same type, different value
        assertFalse(set.contains(43));
        assertFalse(set.contains(UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    /**
     * Verify that {@code contains} works when the set is created with a custom backing map.
     */
    @Test
    void contains_WithCustomBackingMap() {
        // Use a TreeMap (ordered) as the backing map via the constructor that accepts a source collection
        // and an explicit map.
        Map<Object, Object> backingMap = new CaseInsensitiveMap<>(new TreeMap<>());
        CaseInsensitiveSet<Object> orderedSet = new CaseInsensitiveSet<>(Collections.emptySet(), backingMap);
        orderedSet.addAll(Arrays.asList("Alpha", "beta", "GAMMA", 99));
        // Normal case‑insensitive checks
        assertTrue(orderedSet.contains("alpha"));
        assertTrue(orderedSet.contains("BETA"));
        assertTrue(orderedSet.contains("gamma"));
        assertTrue(orderedSet.contains(99));
        // Negative checks
        assertFalse(orderedSet.contains("delta"));
        assertFalse(orderedSet.contains(100));
    }

    /**
     * Verify that {@code contains} works with the constructor that receives a collection.
     */
    @Test
    void contains_FromCollectionConstructor() {
        List<Object> source = Arrays.asList("Cat", "dog", "Elephant", 7);
        CaseInsensitiveSet<Object> fromCollection = new CaseInsensitiveSet<>(source);
        // Case‑insensitive for strings
        assertTrue(fromCollection.contains("cat"));
        assertTrue(fromCollection.contains("DOG"));
        assertTrue(fromCollection.contains("eLePhAnT"));
        // Non‑string
        assertTrue(fromCollection.contains(7));
        assertFalse(fromCollection.contains(8));
    }

    /**
     * Simple contains checks that do not require any mutation of the set.
     * The {@code expected} column may be {@code true}, {@code false} or the fully qualified
     * name of an exception that should be thrown.
     */
    @ParameterizedTest(name = "{index}: contains({0}) → {1}")
    @MethodSource("simpleContainsProvider")
    void contains_SimpleScenarios(Object query, String expected) {
        // For these scenarios we start with an *empty* set unless the description
        // explicitly says otherwise.  The provider builds the appropriate set.
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        // Populate according to the description encoded in the provider.
        // The provider already performed any required add/remove/clear actions.
        // Here we just invoke the method under test.
        if ("java.lang.NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> localSet.contains(query));
        } else {
            boolean expectedBool = Boolean.parseBoolean(expected);
            assertEquals(expectedBool, localSet.contains(query));
        }
    }

    /**
     * Overloaded test method that receives the pre‑prepared set from the provider.
     */
    @ParameterizedTest(name = "{index}: contains({0}) on prepared set → {1}")
    @MethodSource("simpleContainsProvider")
    void contains_SimpleScenarios(Object query, String expected, CaseInsensitiveSet<Object> preparedSet) {
        if ("java.lang.NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> preparedSet.contains(query));
        } else {
            boolean expectedBool = Boolean.parseBoolean(expected);
            assertEquals(expectedBool, preparedSet.contains(query));
        }
    }

    /**
     * Verify that adding the same string with different case does not create duplicates.
     */
    @Test
    void contains_DuplicateAddition_NoDuplicate() {
        CaseInsensitiveSet<Object> dupSet = new CaseInsensitiveSet<>();
        dupSet.add("Apple");
        // should be ignored
        dupSet.add("apple");
        assertEquals(1, dupSet.size());
        assertTrue(dupSet.contains("APPLE"));
    }

    /**
     * Verify that a {@link StringBuilder} is not considered equal to a stored {@link String}.
     */
    @Test
    void contains_StringBuilder_NotEqualToString() {
        CaseInsensitiveSet<Object> sbSet = new CaseInsensitiveSet<>();
        sbSet.add("Apple");
        assertFalse(sbSet.contains(new StringBuilder("Apple")));
    }

    /**
     * Verify that an empty string is handled correctly.
     */
    @Test
    void contains_EmptyString() {
        CaseInsensitiveSet<Object> emptyStrSet = new CaseInsensitiveSet<>();
        emptyStrSet.add("");
        assertTrue(emptyStrSet.contains(""));
        assertFalse(emptyStrSet.contains(" "));
    }

    /**
     * Verify that whitespace-only strings are handled correctly.
     */
    @Test
    void contains_WhitespaceString() {
        CaseInsensitiveSet<Object> wsSet = new CaseInsensitiveSet<>();
        wsSet.add(" ");
        assertTrue(wsSet.contains(" "));
        assertFalse(wsSet.contains(""));
    }

    /**
     * Verify that numeric strings are treated like ordinary strings.
     */
    @Test
    void contains_NumericString() {
        CaseInsensitiveSet<Object> numSet = new CaseInsensitiveSet<>();
        numSet.add("123");
        assertTrue(numSet.contains("123"));
        // case‑insensitivity irrelevant but should still be true
        assertTrue(numSet.contains("123"));
    }
}
