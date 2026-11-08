package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    /**
     * Minimal compact size required by the constructor (must be >= 2).
     */
    private static final int MIN_COMPACT_SIZE = 3;

    /**
     * Map with default (case‑sensitive) configuration.
     */
    private CompactMap<Object, Object> caseSensitiveMap;

    /**
     * Map configured to be case‑insensitive.
     */
    private CompactMap<Object, Object> caseInsensitiveMap;

    /* --------------------------------------------------------------------- */
    private interface CompactMapWrapper {

        int computeKeyHashCodePublic(Object key);
    }

    /**
     * Key whose {@code hashCode()} deliberately throws an exception.
     */
    private static class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad hash");
        }
    }

    /**
     * Simple custom key that returns a fixed hash code.
     */
    private static class CustomKey {

        private final int value;

        CustomKey(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Case‑sensitive map – default behaviour
        caseSensitiveMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return MIN_COMPACT_SIZE;
            }

            @Override
            protected Map<Object, Object> getNewMap() {
                return new java.util.HashMap<>();
            }

            /**
             * expose the protected method for testing
             */
            public int computeKeyHashCodePublic(Object key) {
                return computeKeyHashCode(key);
            }
        };
        // Case‑insensitive map – override isCaseInsensitive()
        caseInsensitiveMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return MIN_COMPACT_SIZE;
            }

            @Override
            protected Map<Object, Object> getNewMap() {
                return new java.util.HashMap<>();
            }

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            /**
             * expose the protected method for testing
             */
            public int computeKeyHashCodePublic(Object key) {
                return computeKeyHashCode(key);
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        caseSensitiveMap = null;
        caseInsensitiveMap = null;
    }

    private int computeKeyHashCode(CompactMap<Object, Object> map, Object key) {
        return ((CompactMapWrapper) map).computeKeyHashCodePublic(key);
    }

    @Test
    void computeKeyHashCode_string_caseSensitive() {
        String key = "AbCde";
        // normal hashCode
        int expected = key.hashCode();
        int actual = ((CompactMap_computeKeyHashCode_Test.CompactMapWrapper) caseSensitiveMap).computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Case‑sensitive map should use String.hashCode()");
    }

    @Test
    void computeKeyHashCode_string_caseInsensitive() {
        String key = "AbCde";
        int expected = StringUtilities.hashCodeIgnoreCase(key);
        int actual = ((CompactMap_computeKeyHashCode_Test.CompactMapWrapper) caseInsensitiveMap).computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Case‑insensitive map should use StringUtilities.hashCodeIgnoreCase()");
    }

    @Test
    void computeKeyHashCode_nullKey_returnsZero() {
        int actual = ((CompactMap_computeKeyHashCode_Test.CompactMapWrapper) caseSensitiveMap).computeKeyHashCodePublic(null);
        assertEquals(0, actual, "Null key must produce hash code 0");
    }

    @Test
    void computeKeyHashCode_selfKey_returnsConstant37() {
        // The map instance itself is used as the key
        int actual = ((CompactMap_computeKeyHashCode_Test.CompactMapWrapper) caseSensitiveMap).computeKeyHashCodePublic(caseSensitiveMap);
        assertEquals(37, actual, "Key equal to the map instance must return 37");
    }

    @Test
    void computeKeyHashCode_otherObject_usesObjectHashCode() {
        Integer key = 12345;
        int expected = key.hashCode();
        int actual = ((CompactMap_computeKeyHashCode_Test.CompactMapWrapper) caseSensitiveMap).computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Non‑String, non‑null key should use its own hashCode()");
    }

    @Test
    void computeKeyHashCode_booleanKey_ignoresCaseSensitivity() {
        Boolean key = Boolean.TRUE;
        int expected = key.hashCode();
        int actual = computeKeyHashCode(caseInsensitiveMap, key);
        assertEquals(expected, actual, "Boolean key should use its own hashCode regardless of case‑sensitivity");
    }

    @Test
    void computeKeyHashCode_stringBuilderKey_fallsIntoGenericBranch() {
        StringBuilder key = new StringBuilder("abc");
        // StringBuilder does not override hashCode()
        int expected = key.hashCode();
        int actual = computeKeyHashCode(caseInsensitiveMap, key);
        assertEquals(expected, actual, "StringBuilder key should use its own hashCode (identity based)");
    }

    @Test
    void computeKeyHashCode_otherCompactMapKey_usesThatMapHashCode() {
        // another CompactMap instance (different reference)
        CompactMap<Object, Object> otherMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return MIN_COMPACT_SIZE;
            }

            @Override
            protected Map<Object, Object> getNewMap() {
                return new HashMap<>();
            }

            public int computeKeyHashCodePublic(Object key) {
                return computeKeyHashCode(key);
            }
        };
        int expected = otherMap.hashCode();
        int actual = computeKeyHashCode(caseSensitiveMap, otherMap);
        assertEquals(expected, actual, "A different CompactMap instance should use its own hashCode");
    }

    @Test
    void computeKeyHashCode_customKey_returnsCustomHash() {
        CustomKey key = new CustomKey(9999);
        int expected = 9999;
        int actual = computeKeyHashCode(caseSensitiveMap, key);
        assertEquals(expected, actual, "Custom key overriding hashCode should return the overridden value");
    }

    @Test
    void computeKeyHashCode_badHashKey_propagatesIllegalStateException() {
        BadHashKey key = new BadHashKey();
        assertThrows(IllegalStateException.class, () -> computeKeyHashCode(caseSensitiveMap, key), "Exception from key.hashCode() must be propagated");
    }

    @Test
    void computeKeyHashCode_isCaseInsensitiveThrowsNPE_propagates() {
        // Map whose isCaseInsensitive() throws NPE
        CompactMap<Object, Object> exceptionMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return MIN_COMPACT_SIZE;
            }

            @Override
            protected Map<Object, Object> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected boolean isCaseInsensitive() {
                throw new NullPointerException("forced NPE");
            }

            public int computeKeyHashCodePublic(Object key) {
                return computeKeyHashCode(key);
            }
        };
        assertThrows(NullPointerException.class, () -> computeKeyHashCode(exceptionMap, "sample"), "Exception from isCaseInsensitive() must be propagated");
    }

    @Test
    void computeKeyHashCode_badHashKey_withNonThrowingMap_propagatesIllegalStateException() {
        BadHashKey key = new BadHashKey();
        // Use a regular map (case‑sensitive) – the key's hashCode throws
        assertThrows(IllegalStateException.class, () -> computeKeyHashCode(caseSensitiveMap, key), "Even when map works, a bad key hashCode should propagate the exception");
    }
}
