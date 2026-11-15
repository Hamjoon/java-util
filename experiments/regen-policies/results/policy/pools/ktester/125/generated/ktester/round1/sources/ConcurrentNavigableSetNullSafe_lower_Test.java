package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import java.util.NavigableSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_lower_Test {

    /**
     * Set used in the majority of tests – created fresh before each test.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default (natural) ordering for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid accidental cross‑test contamination.
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void lower_NaturalOrder_WithNullAndElements() {
        // Arrange
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Act & Assert
        // The internal sentinel for null is considered greater than any non‑null,
        // therefore the greatest element less than null is the maximum element.
        assertEquals("cherry", set.lower(null));
        // lower of "banana" should be "apple"
        assertEquals("apple", set.lower("banana"));
        // lower of the smallest element ("apple") should be null (no element less than it)
        assertNull(set.lower("apple"));
    }

    @Test
    void lower_NaturalOrder_EmptySetReturnsNull() {
        assertNull(set.lower("any"));
        assertNull(set.lower(null));
    }

    @Test
    void lower_NaturalOrder_SingleElementSet() {
        set.add("only");
        // lower of the only element should be null because there is nothing smaller
        assertNull(set.lower("only"));
        // lower of a value greater than the only element should return the element
        assertEquals("only", set.lower("zzz"));
    }

    @Test
    void lower_WithLengthComparator() {
        // Arrange – comparator orders by string length, null considered length 0
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 0
        set.add(null);
        // length 4
        set.add("kiwi");
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // Act & Assert
        // lower(null) → there is no element with length < 0, so null
        assertNull(set.lower(null));
        // lower("kiwi") → element with length < 4 is null (sentinel)
        assertNull(set.lower("kiwi"));
        // lower("apple") → "kiwi" (length 4)
        assertEquals("kiwi", set.lower("apple"));
        // lower("banana") → "apple" (length 5)
        assertEquals("apple", set.lower("banana"));
    }

    @Test
    void lower_WithLengthComparator_EdgeCases() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // Empty set – always null
        assertNull(set.lower("any"));
        assertNull(set.lower(null));
        // Single element ("pear") length 4
        set.add("pear");
        // nothing shorter
        assertNull(set.lower("pear"));
        // anything longer returns "pear"
        assertEquals("pear", set.lower("zebra"));
    }

    /**
     * Set contains only a null element – lower(null) must return null.
     */
    @Test
    void lower_OnlyNullElement_ReturnsNull() {
        set.add(null);
        assertNull(set.lower(null));
    }

    /**
     * Set contains null and a non‑null element "b". lower(null) must return null.
     */
    @Test
    void lower_NullAndNonNull_ElementB_LowerOfNull() {
        set.add(null);
        set.add("b");
        assertNull(set.lower(null));
    }

    /**
     * Set contains only "a". lower("a") must return null (no smaller element).
     */
    @Test
    void lower_OnlyA_ElementA_ReturnsNull() {
        set.add("a");
        assertNull(set.lower("a"));
    }

    /**
     * lower("d") when set contains "a","b","c" – should return "c".
     */
    @Test
    void lower_ElementGreaterThanGreatest_ReturnsGreatest() {
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.lower("d"));
    }

    /**
     * Set contains null and "b". lower("b") must return null (the stored null sentinel).
     */
    @Test
    void lower_NullAndB_LowerOfB_ReturnsNull() {
        set.add(null);
        set.add("b");
        assertNull(set.lower("b"));
    }

    /**
     * Set contains only null sentinel. lower("a") must return null.
     */
    @Test
    void lower_OnlyNullSentinel_NonNullArgument_ReturnsNull() {
        set.add(null);
        assertNull(set.lower("a"));
    }

    /**
     * lower("") when set contains "a","b","c" – empty string is less than all, expect null.
     */
    @Test
    void lower_EmptyStringSmallerThanAll_ReturnsNull() {
        set.add("a");
        set.add("b");
        set.add("c");
        assertNull(set.lower(""));
    }

    /**
     * lower("z") with null sentinel and "a","b","c" – should return "c".
     */
    @Test
    void lower_GreaterThanAll_WithNullSentinel_ReturnsGreatestNonNull() {
        set.add(null);
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.lower("z"));
    }

    /**
     * Set contains only "b". lower("b") must return null (smallest element).
     */
    @Test
    void lower_SingleElementB_LowerOfSame_ReturnsNull() {
        set.add("b");
        assertNull(set.lower("b"));
    }

    @Test
    void lower_ClassCastException_IncompatibleArgument() {
        // Raw type is used to bypass compile‑time generic checks.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe rawSet = new ConcurrentNavigableSetNullSafe<>();
        rawSet.add(1);
        rawSet.add(2);
        rawSet.add(3);
        // Passing a String should trigger ClassCastException inside internalSet.lower(...)
        assertThrows(ClassCastException.class, () -> rawSet.lower("a"));
    }

    @Test
    void lower_NullPointerException_InternalSetIsNull() throws Exception {
        // Create a normal set and then force the private field internalSet to null via reflection.
        ConcurrentNavigableSetNullSafe<String> vulnerableSet = new ConcurrentNavigableSetNullSafe<>();
        // Use reflection to set the private field 'internalSet' to null.
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(vulnerableSet, null);
        // Any call to lower should now throw NPE.
        assertThrows(NullPointerException.class, () -> vulnerableSet.lower("any"));
    }

    @ParameterizedTest(name = "[{index}] lower({0}) ⇒ {1}")
    @CsvSource({ // group: functional branches
    // only null element
    "null, null, SetOnlyNull", // null + "b"
    "null, null, SetNullAndB", // only "a"
    "a, null, SetOnlyA", // "b" -> "a"
    "b, a, SetABC", // "d" -> "c"
    "d, c, SetABC", // null + "b", lower(b) -> null
    "b, null, SetNullAndB2", // only null, lower(a) -> null
    "a, null, SetOnlyNull2", // group: additional edge cases
    // "c" -> "b"
    "c, b, SetABC", // empty string -> null
    "'', null, SetABC", // only null, lower(a) -> null
    "a, null, SetOnlyNull3", // empty set, lower(null) -> null
    "null, null, EmptySet", // "z" with null sentinel -> "c"
    "z, c, SetABCNull", // only null sentinel, lower(b) -> null
    "b, null, SetOnlyNull4", // only "b", lower(b) -> null
    "b, null, SetOnlyB" })
    void lower_ParameterizedFunctionalTests(String e, String expected, String _ignore) {
        // Setup the appropriate set based on the description encoded in _ignore.
        // This is a tiny helper to avoid duplicating large amounts of boilerplate.
        switch(_ignore) {
            case "SetOnlyNull":
            case "SetOnlyNull2":
            case "SetOnlyNull3":
            case "SetOnlyNull4":
                set.add(null);
                break;
            case "SetNullAndB":
            case "SetNullAndB2":
                set.add(null);
                set.add("b");
                break;
            case "SetOnlyA":
                set.add("a");
                break;
            case "SetABC":
                set.add("a");
                set.add("b");
                set.add("c");
                break;
            case "SetABCNull":
                set.add(null);
                set.add("a");
                set.add("b");
                set.add("c");
                break;
            case "EmptySet":
                // leave empty
                break;
            case "SetOnlyB":
                set.add("b");
                break;
            default:
                fail("Unexpected test case identifier: " + _ignore);
        }
        // Convert the CSV string "null" to actual null reference.
        String argument = "null".equals(e) ? null : e;
        // Expected result handling – "null" means we expect a null return.
        String expectedResult = "null".equals(expected) ? null : expected;
        assertEquals(expectedResult, set.lower(argument));
    }
}
