package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CompactMap_compactSize_Test {

    /**
     * Simple subclass that exposes the protected {@code compactSize()} method
     * via a public accessor for testing purposes.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        /**
         * Expose the protected {@code compactSize()} as a public method.
         *
         * @return the compact size configured for this map instance
         */
        public int getCompactSize() {
            return super.compactSize();
        }
    }

    /**
     * Verify that the default implementation returns the constant 70.
     */
    @Test
    void defaultCompactSize_isConstant() {
        TestCompactMap<String, Integer> map = new TestCompactMap<>();
        assertEquals(70, map.getCompactSize(), "The default compact size should match the documented constant DEFAULT_COMPACT_SIZE (70).");
    }

    /**
     * Verify that an overriding subclass can change the compact size.
     */
    @Test
    void overriddenCompactSize_isRespected() {
        TestCompactMap<String, Integer> map = new TestCompactMap<>() {

            @Override
            protected int compactSize() {
                // a custom compact size
                return 5;
            }
        };
        // The public wrapper should return the overridden value.
        assertEquals(5, map.getCompactSize(), "The overridden compactSize() should be returned by the subclass.");
    }

    /**
     * The constructor must reject a compact size smaller than 2.
     */
    @Test
    void constructorRejectsInvalidCompactSize() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            new CompactMap<String, Integer>() {

                @Override
                protected int compactSize() {
                    // invalid: less than the required minimum of 2
                    return 1;
                }

                // expose the overridden value for possible debugging (not overriding anything)
                public int getCompactSize() {
                    return super.compactSize();
                }
            };
        });
        assertTrue(ex.getMessage().contains("compactSize() must be >= 2"), "Exception message should indicate the compact size constraint.");
    }

    /**
     * Verify that after inserting several entries the compact size
     * remains the default constant (70). This matches the JSON test case
     * “compactSize after put operations”.
     */
    @Test
    void compactSizeAfterPutOperations_isConstant() {
        TestCompactMap<String, Integer> map = new TestCompactMap<>();
        // Insert a handful of key/value pairs (more than one, but far below 70)
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, i);
        }
        // The compactSize should still be the default value.
        assertEquals(70, map.getCompactSize(), "compactSize() must remain 70 after normal put operations.");
    }
}
