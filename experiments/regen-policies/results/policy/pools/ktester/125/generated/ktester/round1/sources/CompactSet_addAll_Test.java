package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CompactSet_addAll_Test {

    private CompactSet<String> set;

    /**
     * Data holder for each test scenario.
     */
    private static class Scenario {

        final String description;

        // Elements already present in the set before addAll
        final Collection<?> initialElements;

        // Either a Collection<?> or the special token "self"
        final Object collectionToAdd;

        // true / false when no exception is expected
        final Boolean expectedResult;

        // Non‑null when an exception is expected
        final Class<? extends Throwable> expectedException;

        Scenario(String description, Collection<?> initialElements, Object collectionToAdd, Boolean expectedResult, Class<? extends Throwable> expectedException) {
            this.description = description;
            this.initialElements = initialElements;
            this.collectionToAdd = collectionToAdd;
            this.expectedResult = expectedResult;
            this.expectedException = expectedException;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to guarantee isolation
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set – not strictly required but demonstrates cleanup
        set.clear();
    }

    /**
     * Supplies all scenarios defined in the JSON document.
     */
    private static Stream<Arguments> addAllScenarios() {
        return Stream.of(// ---------- basic functionality ----------
        new Scenario("Adding an empty collection should not modify the set and must return false.", Collections.emptyList(), Collections.emptyList(), false, null), new Scenario("All elements are new; each add(e) returns true, so the method returns true and the set contains a, b, c.", Collections.emptyList(), Arrays.asList("a", "b", "c"), true, null), new Scenario("Set already contains \"x\"; adding collection [\"x\",\"y\"] results in one successful add (y) → modified = true.", Collections.singletonList("x"), Arrays.asList("x", "y"), true, null), new Scenario("First \"p\" is added (add returns true), subsequent duplicates are ignored. Method still returns true because at least one element changed the set.", Collections.emptyList(), Arrays.asList("p", "p", "p"), true, null), new Scenario("Set already contains both \"m\" and \"n\"; every add(e) returns false, so modified stays false and the method returns false.", Arrays.asList("m", "n"), Arrays.asList("m", "n"), false, null), new Scenario("Passing a null collection should cause a NullPointerException when the method attempts to iterate over it.", Collections.emptyList(), null, null, NullPointerException.class), // ---------- additional edge cases ----------
        new Scenario("The set already contains a null element. Adding a collection that only contains null does not modify the set, so addAll should return false.", Collections.singletonList(null), Collections.singletonList(null), false, null), new Scenario("The set is initially empty. Adding a collection with a null and a new non‑null element should add both, modify the set, and cause addAll to return true.", Collections.emptyList(), Arrays.asList(null, "alpha"), true, null), new Scenario("Calling addAll with the set itself as the argument (e.g., set.addAll(set)) must not modify the set because all elements are already present; the method should return false.", Arrays.asList("a", "b"), // special token handled in the test
        "self", false, null), new Scenario("Set initially contains [\"a\", \"b\"]. Adding a collection with a mix of existing (\"a\", \"b\") and new elements (\"c\", \"d\", \"e\") should modify the set and return true.", Arrays.asList("a", "b"), Arrays.asList("b", "c", "d", "a", "e"), true, null), new Scenario("Adding an unmodifiable collection that contains new elements should still succeed, modify the set, and return true.", Collections.emptyList(), Collections.unmodifiableList(Arrays.asList("x", "y")), true, null)).map(s -> Arguments.of(s));
    }

    @Test
    void addAllAddsAllNewElementsAndReturnsTrue() {
        List<String> toAdd = Arrays.asList("alpha", "beta", "gamma");
        boolean modified = set.addAll(toAdd);
        assertTrue(modified, "addAll should report modification when new elements are added");
        assertEquals(3, set.size(), "Set should contain exactly the added elements");
        assertTrue(set.containsAll(toAdd), "All supplied elements must be present");
    }

    @Test
    void addAllWithDuplicatesReturnsTrueOnlyIfAtLeastOneNewElementIsAdded() {
        // First add a subset of the elements
        set.add("alpha");
        set.add("beta");
        // Collection contains one new element ("gamma") and two duplicates
        List<String> toAdd = Arrays.asList("alpha", "beta", "gamma");
        boolean modified = set.addAll(toAdd);
        assertTrue(modified, "addAll should return true because at least one new element was added");
        assertEquals(3, set.size(), "Set size should reflect only unique elements");
        assertTrue(set.contains("gamma"));
    }

    @Test
    void addAllWithOnlyExistingElementsReturnsFalseAndLeavesSetUnchanged() {
        set.add("alpha");
        set.add("beta");
        List<String> duplicates = Arrays.asList("alpha", "beta");
        boolean modified = set.addAll(duplicates);
        assertFalse(modified, "addAll should return false when no new elements are added");
        assertEquals(2, set.size(), "Set size must remain unchanged");
        assertTrue(set.containsAll(duplicates));
    }

    @Test
    void addAllWithEmptyCollectionReturnsFalseAndDoesNotModifySet() {
        set.add("alpha");
        boolean modified = set.addAll(Collections.emptyList());
        assertFalse(modified, "addAll on an empty collection should return false");
        assertEquals(1, set.size(), "Set size must stay the same");
        assertTrue(set.contains("alpha"));
    }

    @Test
    void addAllHandlesNullElementsGracefully() {
        // CompactSet permits null as a regular element (add returns true on first null)
        List<String> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add("foo");
        boolean modified = set.addAll(withNull);
        assertTrue(modified, "addAll should report modification when null is added");
        assertEquals(2, set.size(), "Set should contain null and the non‑null element");
        assertTrue(set.contains(null));
        assertTrue(set.contains("foo"));
        // Adding another collection that only contains null should not modify the set
        List<String> onlyNull = Collections.singletonList(null);
        boolean secondModified = set.addAll(onlyNull);
        assertFalse(secondModified, "Adding an already‑present null should not modify the set");
        assertEquals(2, set.size(), "Set size must remain unchanged after duplicate null");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("addAllScenarios")
    void addAllParameterizedTests(Scenario scenario) {
        // -------------------------------------------------
        // 1. Prepare the set with the required initial state
        // -------------------------------------------------
        set.clear();
        if (scenario.initialElements != null) {
            for (Object e : scenario.initialElements) {
                //noinspection unchecked
                set.add((String) e);
            }
        }
        // -------------------------------------------------
        // 2. Determine the argument to pass to addAll()
        // -------------------------------------------------
        Object arg = scenario.collectionToAdd;
        if ("self".equals(arg)) {
            // The test wants to call set.addAll(set)
            arg = set;
        }
        // -------------------------------------------------
        // 3. Execute and verify
        // -------------------------------------------------
        if (scenario.expectedException != null) {
            // Expecting an exception (e.g., NullPointerException)
            assertThrows(scenario.expectedException, () -> {
                //noinspection unchecked
                set.addAll((Collection<?>) arg);
            }, scenario.description);
        } else {
            // Normal execution – capture the boolean result
            //noinspection unchecked
            boolean result = set.addAll((Collection<?>) arg);
            assertEquals(scenario.expectedResult, result, scenario.description + " – unexpected return value");
            // Additional sanity checks: the set must contain all elements that were added
            // (including any pre‑existing ones).  This also validates that duplicates are ignored.
            Set<String> expected = new LinkedHashSet<>();
            // start with initial elements
            if (scenario.initialElements != null) {
                for (Object e : scenario.initialElements) {
                    expected.add((String) e);
                }
            }
            // add elements from the collection argument (if not null and not the self‑reference)
            if (arg instanceof Collection) {
                for (Object e : (Collection<?>) arg) {
                    expected.add((String) e);
                }
            }
            // The actual set should match the expected composition
            assertEquals(expected, new HashSet<>(set), scenario.description + " – set contents differ from expectation");
        }
    }
}
