package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static com.cedarsoftware.util.StringUtilities.hashCodeIgnoreCase;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_hashCode_Test {

    private CaseInsensitiveSet<Object> set;

    /**
     * Simple object whose hashCode() returns a predetermined value.
     */
    private static class CustomHashObject {

        private final int value;

        CustomHashObject(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * Object whose hashCode() deliberately throws a RuntimeException.
     */
    private static class RuntimeExceptionThrowingObject {

        @Override
        public int hashCode() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * Object whose hashCode() deliberately throws an ArithmeticException.
     */
    private static class ArithmeticExceptionThrowingObject {

        @Override
        public int hashCode() {
            // division by zero triggers ArithmeticException
            return 1 / 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test case
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC; not strictly necessary
        set.clear();
    }

    @Test
    void hashCode_EmptySet_ReturnsZero() {
        // GIVEN an empty CaseInsensitiveSet
        // WHEN hashCode is invoked
        int hash = set.hashCode();
        // THEN the result should be 0
        assertEquals(0, hash, "Hash code of an empty set must be 0");
    }

    @Test
    void hashCode_NonStringElements_SumsTheirHashCodes() {
        // GIVEN a set containing non‑String objects
        Integer intVal = 42;
        Boolean boolVal = Boolean.TRUE;
        set.add(intVal);
        set.add(boolVal);
        // WHEN hashCode is calculated
        int expected = intVal.hashCode() + boolVal.hashCode();
        int actual = set.hashCode();
        // THEN the hash code equals the sum of the individual hash codes
        assertEquals(expected, actual, "Hash code should be the sum of hash codes of non‑String elements");
    }

    @Test
    void hashCode_StringElements_UsesCaseInsensitiveHash() {
        // GIVEN a set containing strings with varying cases
        String s1 = "HelloWorld";
        // same letters, different case
        String s2 = "helloworld";
        set.add(s1);
        // Adding s2 should not create a new entry because the backing map is case‑insensitive
        set.add(s2);
        // WHEN hashCode is calculated
        // only one entry present
        int expected = hashCodeIgnoreCase(s1);
        int actual = set.hashCode();
        // THEN the hash code uses the case‑insensitive hash implementation
        assertEquals(expected, actual, "Hash code for string elements must be computed with case‑insensitive hash");
    }

    @Test
    void hashCode_MixedElements_CombinesBothStrategies() {
        // GIVEN a set with a mix of strings, non‑strings and a null value
        String s = "MixedCase";
        Integer i = 7;
        Object n = null;
        set.add(s);
        set.add(i);
        // null is ignored in hash calculation
        set.add(n);
        // WHEN hashCode is calculated
        // null contributes 0
        int expected = hashCodeIgnoreCase(s) + i.hashCode();
        int actual = set.hashCode();
        // THEN the result matches the combined expected value
        assertEquals(expected, actual, "Hash code must combine case‑insensitive string hash with normal hash codes of other elements, ignoring nulls");
    }

    @Test
    void hashCode_SetContainingOnlyNull_ReturnsZero() {
        // GIVEN a set that contains a single null element
        set.add(null);
        // WHEN hashCode is invoked
        int hash = set.hashCode();
        // THEN null is ignored and the result is 0
        assertEquals(0, hash, "Hash code should be 0 when the only element is null");
    }

    @Test
    void hashCode_SingleStringElement_UsesCaseInsensitiveHash() {
        // GIVEN a set with a single non‑null String
        String s = "Test";
        set.add(s);
        // WHEN hashCode is calculated
        int expected = hashCodeIgnoreCase(s);
        int actual = set.hashCode();
        // THEN the hash equals the case‑insensitive hash of the string
        assertEquals(expected, actual, "Hash code of a single string element must be computed with hashCodeIgnoreCase");
    }

    @Test
    void hashCode_SingleNonStringElement_UsesItsHashCode() {
        // GIVEN a set with a single non‑String element (Integer 42)
        Integer value = 42;
        set.add(value);
        // WHEN hashCode is calculated
        int expected = value.hashCode();
        int actual = set.hashCode();
        // THEN the hash equals the element's own hashCode()
        assertEquals(expected, actual, "Hash code of a single non‑String element must be its own hashCode");
    }

    @Test
    void hashCode_MixedElements_WithDuplicateStringsAndNull() {
        // GIVEN a set containing "abc", "ABC", Integer 5 and null
        set.add("abc");
        // duplicate in case‑insensitive sense
        set.add("ABC");
        set.add(5);
        set.add(null);
        // WHEN hashCode is calculated
        // two strings (same hash) + integer
        int expected = 2 * hashCodeIgnoreCase("abc") + 5;
        int actual = set.hashCode();
        // THEN the result matches the expected sum
        assertEquals(expected, actual, "Hash code must sum case‑insensitive hashes for duplicate strings and integer, ignoring null");
    }

    @Test
    void hashCode_CustomObjectOverridingHashCode() {
        // GIVEN a custom object whose hashCode() returns 12345
        CustomHashObject obj = new CustomHashObject(12345);
        set.add(obj);
        // WHEN hashCode is calculated
        int actual = set.hashCode();
        // THEN the set's hash equals the custom object's hash
        assertEquals(12345, actual, "Hash code should reflect the custom object's overridden hashCode");
    }

    @Test
    void hashCode_MultipleDistinctStrings_SumOfCaseInsensitiveHashes() {
        // GIVEN a set with two different strings
        set.add("abc");
        set.add("def");
        // WHEN hashCode is calculated
        int expected = hashCodeIgnoreCase("abc") + hashCodeIgnoreCase("def");
        int actual = set.hashCode();
        // THEN the hash equals the sum of the two case‑insensitive hashes
        assertEquals(expected, actual, "Hash code must be the sum of case‑insensitive hashes of distinct strings");
    }

    @Test
    void hashCode_DuplicateElementAddedTwice_OnlyCountsOnce() {
        // GIVEN the same string added twice
        set.add("dup");
        set.add("dup");
        // WHEN hashCode is calculated
        int expected = hashCodeIgnoreCase("dup");
        int actual = set.hashCode();
        // THEN the duplicate is ignored, hash is calculated once
        assertEquals(expected, actual, "Duplicate elements must not affect the hash calculation");
    }

    @Test
    void hashCode_EmptyStringElement() {
        // GIVEN a set containing an empty string
        set.add("");
        // WHEN hashCode is calculated
        int expected = hashCodeIgnoreCase("");
        int actual = set.hashCode();
        // THEN the hash equals the case‑insensitive hash of the empty string
        assertEquals(expected, actual, "Empty string should contribute its case‑insensitive hash");
    }

    @Test
    void hashCode_CustomObjectReturningNegativeHash() {
        // GIVEN a custom object whose hashCode() returns -7
        CustomHashObject obj = new CustomHashObject(-7);
        set.add(obj);
        // WHEN hashCode is calculated
        int actual = set.hashCode();
        // THEN the negative hash is propagated
        assertEquals(-7, actual, "Negative hash codes from elements must be added to the set's hash");
    }

    @Test
    void hashCode_DuplicateNonStringElementAndNull() {
        // GIVEN Integer 5 added twice and a null
        set.add(5);
        set.add(5);
        set.add(null);
        // WHEN hashCode is calculated
        // only one non‑null entry contributes
        int expected = 5;
        int actual = set.hashCode();
        // THEN the result equals the integer's hash code
        assertEquals(expected, actual, "Duplicate non‑String elements should be counted once; null is ignored");
    }

    @Test
    void hashCode_OrderIndependence_DifferentInsertionOrders() {
        // GIVEN three strings added in different orders
        List<String> order1 = Arrays.asList("x", "y", "z");
        List<String> order2 = Arrays.asList("z", "x", "y");
        // First order
        order1.forEach(set::add);
        int hash1 = set.hashCode();
        set.clear();
        // Second order
        order2.forEach(set::add);
        int hash2 = set.hashCode();
        // Expected sum (order‑independent)
        int expected = hashCodeIgnoreCase("x") + hashCodeIgnoreCase("y") + hashCodeIgnoreCase("z");
        // THEN both hashes are equal to the expected sum and to each other
        assertEquals(expected, hash1, "Hash should be order‑independent (first order)");
        assertEquals(expected, hash2, "Hash should be order‑independent (second order)");
        assertEquals(hash1, hash2, "Hashes from different insertion orders must match");
    }

    @Test
    void hashCode_InternalMapNull_ThrowsNullPointerException() throws Exception {
        // GIVEN a CaseInsensitiveSet whose internal map is forced to null via reflection
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        // WHEN hashCode is invoked
        // THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> set.hashCode(), "Calling hashCode on a set with a null internal map should throw NPE");
    }

    @Test
    void hashCode_ElementHashCodeThrowsRuntimeException() {
        // GIVEN an element whose hashCode() throws a RuntimeException
        set.add(new RuntimeExceptionThrowingObject());
        // WHEN hashCode is invoked
        // THEN the RuntimeException propagates
        assertThrows(RuntimeException.class, () -> set.hashCode(), "hashCode should propagate RuntimeException thrown by element's hashCode");
    }

    @Test
    void hashCode_ElementHashCodeThrowsArithmeticException() {
        // GIVEN an element whose hashCode() throws an ArithmeticException
        set.add(new ArithmeticExceptionThrowingObject());
        // WHEN hashCode is invoked
        // THEN the ArithmeticException propagates
        assertThrows(ArithmeticException.class, () -> set.hashCode(), "hashCode should propagate unchecked exception thrown by element's hashCode");
    }

    @Test
    void hashCode_ConcurrentModificationDuringIteration_ThrowsConcurrentModificationException() throws Exception {
        // GIVEN a set backed by a non‑concurrent HashMap (injected via reflection)
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<Object, Object> hashMap = new HashMap<>();
        mapField.set(set, hashMap);
        // Populate the map with many entries to increase the chance of iteration overlap
        for (int i = 0; i < 10_000; i++) {
            set.add("key" + i);
        }
        // A latch to make sure the modifier thread runs while hashCode is iterating
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // Wait until the main thread has started hashCode()
                startLatch.await();
                // Remove a key (this will cause the fail‑fast iterator to detect modification)
                hashMap.remove("key0");
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        modifier.start();
        // WHEN hashCode is executed, we signal the modifier thread just before iteration begins
        assertThrows(ConcurrentModificationException.class, () -> {
            // The lambda runs in the same thread as the test; we trigger the latch right before the loop starts.
            // To guarantee the latch fires early, we invoke a helper that accesses the map's keySet iterator.
            // The helper is placed inside the lambda to keep the stack trace clean.
            // allow the modifier to run
            startLatch.countDown();
            // this should throw CME because the map is being modified concurrently
            set.hashCode();
        }, "Concurrent modification should cause ConcurrentModificationException");
        // Ensure the modifier thread has finished before the test exits
        doneLatch.await();
    }
}
