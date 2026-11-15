package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private TestableCompactMap map;

    /**
     * Simple subclass that makes the protected method public.
     */
    private static class TestableCompactMap extends CompactMap<Object, Object> {

        int computeValueHashCodePublic(Object value) {
            return super.computeValueHashCode(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestableCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean‑up reference for GC; not strictly required.
        map = null;
    }

    /**
     * Supplies a stream of arguments matching the JSON test cases.
     * The expected hash codes are calculated at runtime to avoid hard‑coded magic numbers.
     */
    private static Stream<Arguments> computeValueHashCodeProvider() {
        // 1️⃣ Integer 42 → hashCode() == 42
        Integer intValue = 42;
        // 2️⃣ String "test" → compute once
        String stringValue = "test";
        int stringHash = stringValue.hashCode();
        // 3️⃣ Custom object with overridden hashCode() → 12345
        Object customHashObject = new Object() {

            @Override
            public int hashCode() {
                return 12345;
            }
        };
        // 4️⃣ Different CompactMap instance (not the same reference)
        CompactMap<Object, Object> otherMap = new CompactMap<>() {
        };
        int otherMapHash = otherMap.hashCode();
        // 5️⃣ Boolean.TRUE → 1231
        Boolean boolValue = Boolean.TRUE;
        // 6️⃣ Double 3.14 → Double.hashCode()
        Double doubleValue = 3.14;
        int doubleHash = Double.hashCode(doubleValue);
        // 7️⃣ int[] array → identity based hash (same as Object.hashCode())
        int[] intArray = new int[] { 1, 2, 3 };
        // identity hash
        int arrayHash = intArray.hashCode();
        // 8️⃣ Custom object returning a negative hash code → -999
        Object negativeHashObject = new Object() {

            @Override
            public int hashCode() {
                return -999;
            }
        };
        return Stream.of(// Integer case
        Arguments.of(intValue, intValue.hashCode()), // String case
        Arguments.of(stringValue, stringHash), // Custom object case
        Arguments.of(customHashObject, 12345), // Other CompactMap instance case
        Arguments.of(otherMap, otherMapHash), // Boolean case
        Arguments.of(boolValue, boolValue.hashCode()), // Double case
        Arguments.of(doubleValue, doubleHash), // int[] array case
        Arguments.of(intArray, arrayHash), // Negative hash object case
        Arguments.of(negativeHashObject, -999));
    }

    /**
     * Helper to produce a readable description of a test input for assertion messages.
     */
    private static String describe(Object o) {
        if (o == null)
            return "null";
        if (o.getClass().isArray()) {
            // Show array type and length
            return o.getClass().getComponentType().getSimpleName() + "[] (len=" + java.lang.reflect.Array.getLength(o) + ")";
        }
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    @Test
    @DisplayName("computeValueHashCode returns 17 when value is the map itself")
    void returnsFixedHashWhenValueIsSelf() {
        // GIVEN a CompactMap instance
        // WHEN computeValueHashCode is called with the map itself as argument
        int result = map.computeValueHashCodePublic(map);
        // THEN the special constant 17 is returned
        assertEquals(17, result, "Hash code for self‑reference should be 17");
    }

    @Test
    @DisplayName("computeValueHashCode returns 0 for null value")
    void returnsZeroForNull() {
        // GIVEN a null value
        // WHEN computeValueHashCode is invoked
        int result = map.computeValueHashCodePublic(null);
        // THEN 0 is returned
        assertEquals(0, result, "Hash code for null should be 0");
    }

    @Test
    @DisplayName("computeValueHashCode delegates to value.hashCode() for non‑null, non‑self values")
    void delegatesToValueHashCode() {
        // GIVEN a regular non‑null object with a known hash code
        String value = "test-value";
        int expected = value.hashCode();
        // WHEN computeValueHashCode is called with that object
        int result = map.computeValueHashCodePublic(value);
        // THEN the result equals the object's hashCode()
        assertEquals(expected, result, "Hash code should match the delegated value.hashCode()");
    }

    @Test
    @DisplayName("computeValueHashCode works with objects that have overridden hashCode")
    void worksWithCustomHashCodeObject() {
        // GIVEN an object with a custom hashCode implementation
        Object custom = new Object() {

            @Override
            public int hashCode() {
                return 123456;
            }
        };
        // WHEN computeValueHashCode is invoked
        int result = map.computeValueHashCodePublic(custom);
        // THEN the custom hash code is returned
        assertEquals(123456, result, "Hash code should reflect the overridden hashCode() of the value");
    }

    @ParameterizedTest(name = "{index}: value={0} → expected={1}")
    @MethodSource("computeValueHashCodeProvider")
    @DisplayName("computeValueHashCode various inputs")
    void computeValueHashCodeVariousInputs(Object input, int expected) {
        // WHEN the protected method is invoked via the public wrapper
        int actual = map.computeValueHashCodePublic(input);
        // THEN the returned hash matches the expected value
        assertEquals(expected, actual, () -> "Unexpected hash for input [" + describe(input) + "]");
    }
}
