package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class CompactCIHashSet_getNewSet_Test {

    private TestableCompactCIHashSet<String> setUnderTest;

    /**
     * Simple concrete subclass that makes the protected method public for the test.
     */
    private static class TestableCompactCIHashSet<E> extends CompactCIHashSet<E> {

        public Set<E> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    /**
     * Subclass that forces an illegal negative capacity.
     */
    private static class NegativeCompactSizeSet<E> extends CompactCIHashSet<E> {

        @Override
        protected int compactSize() {
            // illegal negative size
            return -5;
        }

        public Set<E> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    /**
     * Subclass that forces an overflow to a negative capacity.
     */
    private static class OverflowCompactSizeSet<E> extends CompactCIHashSet<E> {

        @Override
        protected int compactSize() {
            // +1 overflows to Integer.MIN_VALUE
            return Integer.MAX_VALUE;
        }

        public Set<E> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        setUnderTest = new TestableCompactCIHashSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        setUnderTest = null;
    }

    @Test
    void getNewSet_returnsNonNullCaseInsensitiveSet() {
        // GIVEN
        // (setUnderTest already created in @BeforeEach)
        // WHEN
        Set<String> result = setUnderTest.exposeGetNewSet();
        // THEN
        assertNotNull(result, "The set returned by getNewSet() must not be null");
        assertTrue(result.isEmpty(), "The newly created set should be empty");
        // Verify that the set is case‑insensitive:
        result.add("Hello");
        assertTrue(result.contains("hello"), "CaseInsensitiveSet should treat \"Hello\" and \"hello\" as equal");
        assertTrue(result.contains("HELLO"), "CaseInsensitiveSet should treat \"Hello\" and \"HELLO\" as equal");
        // Adding a different case should not increase the size.
        result.add("HELLO");
        assertEquals(1, result.size(), "Adding a duplicate value with different case must not increase size");
    }

    @Test
    void getNewSet_isInstanceOfCaseInsensitiveSet() {
        // WHEN
        Set<String> result = setUnderTest.exposeGetNewSet();
        // THEN
        assertTrue(result instanceof CaseInsensitiveSet, "The returned set should be an instance of CaseInsensitiveSet");
    }

    @ParameterizedTest(name = "add \"{0}\" then check contains \"{1}\"")
    @CsvSource({ "'Hello', 'hello'", "'HELLO', 'hello'", "'HeLLo', 'HELLO'" })
    void getNewSet_caseInsensitiveContainment(String elementToAdd, String elementToCheck) {
        Set<String> result = setUnderTest.exposeGetNewSet();
        // add first element
        result.add(elementToAdd);
        // the set must contain the same value irrespective of case
        assertTrue(result.contains(elementToCheck), () -> "Set should contain \"" + elementToCheck + "\" after adding \"" + elementToAdd + "\"");
        // adding the same logical element again (different case) must not increase size
        result.add(elementToCheck);
        assertEquals(1, result.size(), "Adding a duplicate element with different case must not increase size");
    }

    @Test
    void getNewSet_containsDifferentCaseSingleElement() {
        Set<String> result = setUnderTest.exposeGetNewSet();
        result.add("TeSt");
        assertTrue(result.contains("test"), "After adding \"TeSt\", the set must contain \"test\" (case‑insensitive)");
    }

    @Test
    void getNewSet_internalMapHasExpectedInitialCapacity() throws Exception {
        // WHEN
        Set<String> result = setUnderTest.exposeGetNewSet();
        // -----------------------------------------------------------------
        // The returned set is a CaseInsensitiveSet.  Its internal delegate
        // map is a CaseInsensitiveMap that itself wraps a HashMap.  We need
        // to reach that HashMap via reflection and verify the capacity that
        // was supplied to its constructor (compactSize() + 1).
        // -----------------------------------------------------------------
        // 1. Access the private field "map" inside CaseInsensitiveSet
        Field setMapField = result.getClass().getDeclaredField("map");
        setMapField.setAccessible(true);
        Object caseInsensitiveMap = setMapField.get(result);
        assertNotNull(caseInsensitiveMap, "CaseInsensitiveSet should hold a non‑null map");
        // 2. Inside CaseInsensitiveMap, the backing HashMap is stored in a field
        //    named "map" (the same name).  Reach it via reflection.
        Field ciMapField = caseInsensitiveMap.getClass().getDeclaredField("map");
        ciMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> backingHashMap = (HashMap<String, Object>) ciMapField.get(caseInsensitiveMap);
        assertNotNull(backingHashMap, "CaseInsensitiveMap should wrap a non‑null HashMap");
        // 3. The HashMap does not expose its capacity directly, but the
        //    private field "threshold" is (capacity * loadFactor).  The default
        //    load factor is 0.75f, so we can recover the original capacity.
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(backingHashMap);
        Field loadFactorField = HashMap.class.getDeclaredField("loadFactor");
        loadFactorField.setAccessible(true);
        float loadFactor = (float) loadFactorField.get(backingHashMap);
        int derivedCapacity = (int) Math.ceil(threshold / loadFactor);
        // Expected capacity = compactSize() + 1
        int expectedCapacity = setUnderTest.compactSize() + 1;
        assertEquals(expectedCapacity, derivedCapacity, "Backing HashMap initial capacity must equal compactSize() + 1");
    }

    @Test
    void getNewSet_negativeCompactSize_throwsIllegalArgumentException() {
        NegativeCompactSizeSet<String> negativeSet = new NegativeCompactSizeSet<>();
        assertThrows(IllegalArgumentException.class, negativeSet::exposeGetNewSet, "When compactSize() returns a negative value, HashMap constructor must throw IllegalArgumentException");
    }

    @Test
    void getNewSet_overflowCompactSize_throwsIllegalArgumentException() {
        OverflowCompactSizeSet<String> overflowSet = new OverflowCompactSizeSet<>();
        assertThrows(IllegalArgumentException.class, overflowSet::exposeGetNewSet, "When compactSize() + 1 overflows to a negative value, HashMap constructor must throw IllegalArgumentException");
    }
}
