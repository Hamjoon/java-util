package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private TestCompactMap mapUnderTest;

    /**
     * Test‑specific subclass that makes {@code computeValueHashCode} public.
     */
    private static class TestCompactMap extends CompactMap<Object, Object> {

        /**
         * Expose the protected method for testing.
         */
        int exposeComputeValueHashCode(Object value) {
            return computeValueHashCode(value);
        }

        /**
         * CompactMap requires a compact size of at least 2.
         */
        @Override
        protected int compactSize() {
            // any value >= 2 is acceptable
            return 3;
        }

        /**
         * Provide a backing map – never used in these tests.
         */
        @Override
        protected java.util.Map<Object, Object> getNewMap() {
            return new java.util.HashMap<>();
        }
    }

    /**
     * Simple object that returns a fixed hash code (999).
     */
    private static class DummyObject {

        @Override
        public int hashCode() {
            return 999;
        }
    }

    /**
     * Object whose hashCode returns a negative value (-42).
     */
    private static class NegativeHashObject {

        @Override
        public int hashCode() {
            return -42;
        }
    }

    /**
     * Object whose hashCode deliberately throws an IllegalStateException.
     */
    private static class ExceptionThrowingObject {

        @Override
        public int hashCode() {
            throw new IllegalStateException("forced exception");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTest = new TestCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        mapUnderTest = null;
    }

    @Test
    void computeValueHashCode_returns17WhenValueIsSelf() {
        // The method should recognise the map instance itself and return the constant 17.
        int result = mapUnderTest.exposeComputeValueHashCode(mapUnderTest);
        assertEquals(17, result, "When the value argument is the map itself, computeValueHashCode should return 17");
    }

    @Test
    void computeValueHashCode_returnsZeroWhenValueIsNull() {
        int result = mapUnderTest.exposeComputeValueHashCode(null);
        assertEquals(0, result, "When the value argument is null, computeValueHashCode should return 0");
    }

    @Test
    void computeValueHashCode_returnsValueHashCodeForNonNullNonSelf() {
        // Use a few distinct objects to verify that their hash codes are propagated unchanged.
        String str = "testString";
        Integer number = 42;
        Object custom = new Object() {

            @Override
            public int hashCode() {
                return 123456;
            }
        };
        assertEquals(str.hashCode(), mapUnderTest.exposeComputeValueHashCode(str), "String hash code should be returned unchanged");
        assertEquals(number.hashCode(), mapUnderTest.exposeComputeValueHashCode(number), "Integer hash code should be returned unchanged");
        assertEquals(123456, mapUnderTest.exposeComputeValueHashCode(custom), "Custom object's hashCode should be returned unchanged");
    }

    @Test
    void computeValueHashCode_returnsValueHashCodeForVariousNonNullObjects() {
        // String
        String str = "hello";
        assertEquals(str.hashCode(), mapUnderTest.exposeComputeValueHashCode(str), "String hash code should be returned unchanged");
        // Integer
        Integer number = 42;
        assertEquals(number.hashCode(), mapUnderTest.exposeComputeValueHashCode(number), "Integer hash code should be returned unchanged");
        // Boolean.TRUE
        assertEquals(Boolean.TRUE.hashCode(), mapUnderTest.exposeComputeValueHashCode(Boolean.TRUE), "Boolean.TRUE hash code should be returned unchanged");
        // Custom object with overridden hashCode (999)
        DummyObject dummy = new DummyObject();
        assertEquals(999, mapUnderTest.exposeComputeValueHashCode(dummy), "DummyObject's hashCode (999) should be returned unchanged");
        // Custom object with negative hash code (-42)
        NegativeHashObject neg = new NegativeHashObject();
        assertEquals(-42, mapUnderTest.exposeComputeValueHashCode(neg), "NegativeHashObject's hashCode (-42) should be returned unchanged");
    }

    @Test
    void computeValueHashCode_returnsOtherCompactMapHashCodeWhenValueIsAnotherMap() {
        // Create a *different* CompactMap instance.
        TestCompactMap otherMap = new TestCompactMap();
        // The expected value is the other map's own hashCode (empty map → 0, but we assert equality).
        int expected = otherMap.hashCode();
        int result = mapUnderTest.exposeComputeValueHashCode(otherMap);
        assertEquals(expected, result, "When the argument is another CompactMap instance, its own hashCode should be returned (not the special constant 17)");
        assertNotEquals(17, result, "The returned hash code must not be the special constant 17");
    }

    @Test
    void computeValueHashCode_propagatesExceptionFromValueHashCode() {
        ExceptionThrowingObject obj = new ExceptionThrowingObject();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mapUnderTest.exposeComputeValueHashCode(obj), "Exceptions thrown by the value's hashCode() must propagate unchanged");
        assertEquals("forced exception", ex.getMessage(), "The propagated exception should retain the original message");
    }
}
