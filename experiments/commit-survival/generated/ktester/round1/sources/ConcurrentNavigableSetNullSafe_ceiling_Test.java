package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_ceiling_Test {

    /**
     * Set that uses natural ordering (the default constructor).
     */
    private NavigableSet<String> naturalSet;

    /**
     * Set that uses a length‑based comparator (null is treated as length 0).
     */
    private NavigableSet<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test so that tests are independent.
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid accidental cross‑test contamination.
        if (naturalSet != null) {
            naturalSet.clear();
        }
        if (lengthComparatorSet != null) {
            lengthComparatorSet.clear();
        }
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Set contains null element.
        Arguments.of("nullPresent", null, null, "Calling ceiling(null) when the underlying set contains a null element."), // Set does NOT contain null but has ordered elements.
        Arguments.of("nullAbsent", null, "apple", "Calling ceiling(null) on a set that does not contain null but has ordered elements."), // Exact match.
        Arguments.of("bananaPresent", "banana", "banana", "Calling ceiling(\"banana\") when \"banana\" is already present in the set."), // Higher element exists.
        Arguments.of("blueberryMissing", "blueberry", "cherry", "Calling ceiling(\"blueberry\") when the exact element is absent but a higher element (\"cherry\") exists."), // Greater than any element.
        Arguments.of("zucchiniBeyond", "zucchini", null, "Calling ceiling(\"zucchini\") when the element is greater than any element in the set."), // Empty set.
        Arguments.of("emptySet", "apple", null, "Calling ceiling(\"apple\") on an empty set."));
    }

    private static Stream<Arguments> additionalEdgeCasesProvider() {
        return Stream.of(// Single element set.
        Arguments.of(Collections.singleton("apple"), "apple", "apple", "Set contains only \"apple\". ceiling(\"apple\") should return the element itself."), // Smallest element scenario.
        Arguments.of(new String[] { "apple" }, "aardvark", "apple", "Set contains \"apple\" as the smallest element. ceiling(\"aardvark\") should return \"apple\"."), // Beyond greatest element.
        Arguments.of(new String[] { "apple", "banana", "cherry" }, "zzzz", null, "Set contains \"apple\", \"banana\", \"cherry\". ceiling(\"zzzz\") is beyond the greatest element."), // Null present together with other elements.
        Arguments.of(new Object[] { null, "apple" }, null, null, "Set contains a null element and also \"apple\". The ceiling of null should be the null element itself."), // Null absent, smallest element is "apple".
        Arguments.of(new String[] { "apple" }, null, "apple", "Set does NOT contain null but contains \"apple\" as the smallest element. ceiling(null) should return \"apple\"."), // Duplicate logical values (different String instances).
        Arguments.of(new String[] { "banana" }, new String("banana"), "banana", "Set contains duplicate logical values via different instances; ceiling should return the existing element."), // Case‑insensitive comparator.
        Arguments.of(new String[] { "banana" }, "Banana", "banana", "Set uses case‑insensitive comparator. ceiling(\"Banana\") should return \"banana\"."), // Integer set – exact match.
        Arguments.of(new Integer[] { 5, 10 }, 5, 5, "Set of Integers with natural ordering, contains 5. ceiling(5) returns 5."), // Integer set – lower than smallest.
        Arguments.of(new Integer[] { 5, 10 }, 4, 5, "Set of Integers contains [5,10]. ceiling(4) returns the smallest element >=4, which is 5."), // Integer set – greater than max.
        Arguments.of(new Integer[] { 5, 10 }, 11, null, "Set of Integers contains [5,10]. ceiling(11) exceeds max, returns null."));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(// Natural‑ordering String set, incompatible Integer argument.
        Arguments.of("StringNatural", 5, ClassCastException.class, "Set is created with natural ordering of Strings. Passing an Integer should cause ClassCastException."), // Case‑insensitive String comparator, generic Object argument.
        Arguments.of("StringCaseInsensitive", new Object(), ClassCastException.class, "Set uses a case‑insensitive Comparator<String>. Supplying a generic Object triggers ClassCastException."), // Stub internal set that throws UnsupportedOperationException.
        Arguments.of("StubUnsupported", "any", UnsupportedOperationException.class, "Underlying NavigableSet implementation throws UnsupportedOperationException from its ceiling method."), // Integer set, null argument (sentinel not comparable).
        Arguments.of("IntegerNatural", null, ClassCastException.class, "Underlying set uses natural ordering of non‑null Integers. maskNull creates a sentinel that is not comparable, causing ClassCastException."));
    }

    @Test
    void ceiling_NaturalOrder_BasicCases() {
        // Populate the set – note that null is allowed.
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        // null is treated as the smallest element
        naturalSet.add(null);
        // null is the smallest element → ceiling(null) should be null itself.
        assertNull(naturalSet.lower(null), "lower(null) should be null because there is no element < null");
        assertNull(naturalSet.floor(null), "floor(null) should be null because null is present");
        assertNull(naturalSet.ceiling(null), "ceiling(null) should be null because null is present");
        // Element smaller than any existing (e.g., "aardvark") → ceiling should be "apple".
        assertEquals("apple", naturalSet.ceiling("aardvark"));
        // Exact match → ceiling returns the element itself.
        assertEquals("banana", naturalSet.ceiling("banana"));
        // Element between "banana" and "cherry" → ceiling should be "cherry".
        assertEquals("cherry", naturalSet.ceiling("blueberry"));
        // Element greater than any existing → ceiling returns null.
        assertNull(naturalSet.ceiling("zebra"));
    }

    @Test
    void ceiling_NaturalOrder_EmptySet() {
        assertTrue(naturalSet.isEmpty(), "Set should be empty at start of test");
        assertNull(naturalSet.ceiling("anything"));
        assertNull(naturalSet.ceiling(null));
    }

    @Test
    void ceiling_LengthComparator_BasicCases() {
        // Comparator orders by length, null is considered length 0.
        // length 0
        lengthComparatorSet.add(null);
        // length 4
        lengthComparatorSet.add("kiwi");
        // length 5
        lengthComparatorSet.add("apple");
        // length 6
        lengthComparatorSet.add("banana");
        // Ceiling of null (length 0) is null itself.
        assertNull(lengthComparatorSet.ceiling(null));
        // Ceiling of a string shorter than the smallest non‑null element (e.g., "a").
        assertEquals("kiwi", lengthComparatorSet.ceiling("a"));
        // Ceiling of a string whose length equals an existing element returns that element.
        // both length 5
        assertEquals("apple", lengthComparatorSet.ceiling("pear"));
        // Ceiling of a longer string (length 7) should be null because no element is >= length 7.
        assertNull(lengthComparatorSet.ceiling("pumpkin"));
    }

    @Test
    void ceiling_LengthComparator_NullOnly() {
        lengthComparatorSet.add(null);
        // With only null present, any non‑null argument should return null (no element with length >= argument).
        assertNull(lengthComparatorSet.ceiling("a"));
        assertNull(lengthComparatorSet.ceiling("any longer string"));
    }

    @Test
    void ceiling_DoesNotAffectSetContents() {
        naturalSet.add("alpha");
        naturalSet.add("beta");
        naturalSet.add(null);
        // Capture size before calling ceiling.
        int sizeBefore = naturalSet.size();
        // Invoke ceiling with a value that is present and one that is not.
        assertEquals("beta", naturalSet.ceiling("beta"));
        assertEquals("beta", naturalSet.ceiling("aardvark"));
        // Verify that the set's size and contents are unchanged.
        assertEquals(sizeBefore, naturalSet.size());
        assertTrue(naturalSet.contains("alpha"));
        assertTrue(naturalSet.contains("beta"));
        assertTrue(naturalSet.contains(null));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void ceiling_BasicFunctionality(String scenario, Object input, Object expected, String description) {
        // Build the appropriate set for the scenario.
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        switch(scenario) {
            case "nullPresent":
                // null + three regular elements
                set.add(null);
                set.add("apple");
                set.add("banana");
                set.add("cherry");
                break;
            case "nullAbsent":
                // three regular elements, no null
                set.add("apple");
                set.add("banana");
                set.add("cherry");
                break;
            case "emptySet":
                // leave empty
                break;
            default:
                // all other scenarios use the normal three‑element set plus null
                set.add(null);
                set.add("apple");
                set.add("banana");
                set.add("cherry");
                break;
        }
        // Act & Assert
        @SuppressWarnings("unchecked")
        String result = (String) set.ceiling((String) input);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("additionalEdgeCasesProvider")
    void ceiling_AdditionalEdgeCases(Object elements, Object input, Object expected, String description) {
        // Determine element type (String or Integer) and create appropriate set.
        NavigableSet<?> set;
        if (elements instanceof String[]) {
            // String set – default natural ordering.
            @SuppressWarnings("unchecked")
            String[] arr = (String[]) elements;
            ConcurrentNavigableSetNullSafe<String> s = new ConcurrentNavigableSetNullSafe<>();
            for (String v : arr) {
                s.add(v);
            }
            // handle possible null element in the array
            if (Arrays.asList(arr).contains(null)) {
                s.add(null);
            }
            set = s;
        } else if (elements instanceof Integer[]) {
            @SuppressWarnings("unchecked")
            Integer[] arr = (Integer[]) elements;
            ConcurrentNavigableSetNullSafe<Integer> s = new ConcurrentNavigableSetNullSafe<>();
            for (Integer v : arr) {
                s.add(v);
            }
            set = s;
        } else if (elements instanceof Object[]) {
            // mixed (null + String) case
            @SuppressWarnings("unchecked")
            Object[] arr = (Object[]) elements;
            ConcurrentNavigableSetNullSafe<String> s = new ConcurrentNavigableSetNullSafe<>();
            for (Object v : arr) {
                //noinspection unchecked
                s.add((String) v);
            }
            set = s;
        } else {
            throw new IllegalArgumentException("Unsupported element type");
        }
        // Act
        @SuppressWarnings("unchecked")
        Object result = ((NavigableSet<Object>) set).ceiling(input);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("exceptionScenariosProvider")
    void ceiling_ExceptionScenarios(String setKind, Object input, Class<? extends Throwable> expectedException, String description) throws Exception {
        ConcurrentNavigableSetNullSafe<?> testSet;
        switch(setKind) {
            case "StringNatural":
                testSet = new ConcurrentNavigableSetNullSafe<String>();
                ((ConcurrentNavigableSetNullSafe<String>) testSet).add("apple");
                ((ConcurrentNavigableSetNullSafe<String>) testSet).add("banana");
                break;
            case "StringCaseInsensitive":
                Comparator<String> ci = String.CASE_INSENSITIVE_ORDER;
                testSet = new ConcurrentNavigableSetNullSafe<>(ci);
                ((ConcurrentNavigableSetNullSafe<String>) testSet).add("apple");
                ((ConcurrentNavigableSetNullSafe<String>) testSet).add("banana");
                break;
            case "StubUnsupported":
                // Create a mock NavigableSet that throws UnsupportedOperationException from ceiling().
                @SuppressWarnings("unchecked")
                NavigableSet<Object> mockSet = mock(NavigableSet.class);
                when(mockSet.ceiling(any())).thenThrow(new UnsupportedOperationException("stub"));
                testSet = createWithMockInternalSet(mockSet);
                break;
            case "IntegerNatural":
                testSet = new ConcurrentNavigableSetNullSafe<Integer>();
                ((ConcurrentNavigableSetNullSafe<Integer>) testSet).add(5);
                ((ConcurrentNavigableSetNullSafe<Integer>) testSet).add(10);
                break;
            default:
                throw new IllegalArgumentException("Unknown set kind: " + setKind);
        }
        // Act & Assert
        assertThrows(expectedException, () -> {
            // The ceiling method is generic, so we need an unchecked cast.
            @SuppressWarnings("unchecked")
            Object result = ((ConcurrentNavigableSetNullSafe<Object>) testSet).ceiling(input);
            // The result is irrelevant; the expectation is that an exception is thrown.
            return result;
        }, description);
    }

    /**
     * Helper that creates a {@link ConcurrentNavigableSetNullSafe} instance whose
     * private {@code internalSet} field is replaced by the supplied mock.
     */
    @SuppressWarnings("unchecked")
    private static <E> ConcurrentNavigableSetNullSafe<E> createWithMockInternalSet(NavigableSet<Object> mockInternal) throws Exception {
        ConcurrentNavigableSetNullSafe<E> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(wrapper, mockInternal);
        return wrapper;
    }
}
