package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    private TestCompactMap mapCaseSensitive;

    private TestCompactMap mapCaseInsensitive;

    /**
     * Simple concrete subclass that makes {@code computeKeyHashCode} public.
     */
    private static class TestCompactMap extends CompactMap<Object, Object> {

        private final boolean caseInsensitive;

        TestCompactMap(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }

        /**
         * Exposes the protected method for the test suite.
         */
        int invokeComputeKeyHashCode(Object key) {
            return computeKeyHashCode(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // One map that treats strings case‑sensitively and another that treats them case‑insensitively.
        mapCaseSensitive = new TestCompactMap(false);
        mapCaseInsensitive = new TestCompactMap(true);
    }

    private static Stream<Arguments> stringKeyProvider() {
        String key = "HelloWorld";
        return Stream.of(// case‑sensitive: delegate to String.hashCode()
        Arguments.of(false, key, key.hashCode()), // case‑insensitive: delegate to StringUtilities.hashCodeIgnoreCase()
        Arguments.of(true, key, StringUtilities.hashCodeIgnoreCase(key)));
    }

    private static Stream<Arguments> nonStringKeyProvider() {
        Integer intKey = Integer.valueOf(123);
        Object objKey = new Object() {

            @Override
            public int hashCode() {
                return 987654;
            }
        };
        return Stream.of(Arguments.of(intKey, intKey.hashCode()), Arguments.of(objKey, objKey.hashCode()));
    }

    @Test
    void computeKeyHashCode_stringCaseSensitive_returnsStringHashCode() {
        String key = "HelloWorld";
        // normal hashCode for a String
        int expected = key.hashCode();
        int actual = mapCaseSensitive.invokeComputeKeyHashCode(key);
        assertEquals(expected, actual, "When case‑sensitive, a String key should use its own hashCode()");
    }

    @Test
    void computeKeyHashCode_stringCaseInsensitive_usesStringUtilities() {
        String key = "HelloWorld";
        // The utility method performs a case‑insensitive hash; we delegate to it for the expected value.
        int expected = StringUtilities.hashCodeIgnoreCase(key);
        int actual = mapCaseInsensitive.invokeComputeKeyHashCode(key);
        assertEquals(expected, actual, "When case‑insensitive, a String key should use StringUtilities.hashCodeIgnoreCase()");
    }

    @Test
    void computeKeyHashCode_nullKey_returnsZero() {
        int actualSensitive = mapCaseSensitive.invokeComputeKeyHashCode(null);
        int actualInsensitive = mapCaseInsensitive.invokeComputeKeyHashCode(null);
        assertEquals(0, actualSensitive, "Null key must yield 0 (case‑sensitive map)");
        assertEquals(0, actualInsensitive, "Null key must yield 0 (case‑insensitive map)");
    }

    @Test
    void computeKeyHashCode_keyIsMapInstance_returnsConstant37() {
        // The map should recognise its own instance and return the constant 37.
        int actual = mapCaseSensitive.invokeComputeKeyHashCode(mapCaseSensitive);
        assertEquals(37, actual, "When the key equals the map itself, hash code must be 37");
    }

    @Test
    void computeKeyHashCode_arbitraryObject_returnsItsHashCode() {
        Object key = new Object() {

            @Override
            public int hashCode() {
                return 123456;
            }
        };
        int actual = mapCaseSensitive.invokeComputeKeyHashCode(key);
        assertEquals(123456, actual, "For a non‑String, non‑null, non‑self key the method should return key.hashCode()");
    }

    /* ------------------------------------------------------------
     *  String key handling
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: caseInsensitive={0}, key=\"{1}\"")
    @MethodSource("stringKeyProvider")
    void computeKeyHashCode_stringHandling(boolean caseInsensitive, String key, int expected) {
        TestCompactMap map = caseInsensitive ? mapCaseInsensitive : mapCaseSensitive;
        int actual = map.invokeComputeKeyHashCode(key);
        assertEquals(expected, actual, () -> String.format("String key '%s' with caseInsensitive=%b should produce %d", key, caseInsensitive, expected));
    }

    /* ------------------------------------------------------------
     *  Non‑String key handling (including Integer and generic Object)
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: key={0}")
    @MethodSource("nonStringKeyProvider")
    void computeKeyHashCode_nonStringHandling(Object key, int expected) {
        // The result must be identical for case‑sensitive and case‑insensitive maps.
        int actualSensitive = mapCaseSensitive.invokeComputeKeyHashCode(key);
        int actualInsensitive = mapCaseInsensitive.invokeComputeKeyHashCode(key);
        assertEquals(expected, actualSensitive, () -> "Non‑String key should return its own hashCode (case‑sensitive map)");
        assertEquals(expected, actualInsensitive, () -> "Non‑String key should return its own hashCode (case‑insensitive map)");
    }

    /* ------------------------------------------------------------
     *  Self‑reference key handling
     * ------------------------------------------------------------ */
    @Test
    void computeKeyHashCode_selfReference_returnsConstant37() {
        // The map should recognise its own instance and return the constant 37.
        int actual = mapCaseSensitive.invokeComputeKeyHashCode(mapCaseSensitive);
        assertEquals(37, actual, "When the key equals the map itself, hash code must be 37");
    }

    /* ------------------------------------------------------------
     *  Case‑insensitivity ignored for non‑String keys
     * ------------------------------------------------------------ */
    @Test
    void computeKeyHashCode_caseInsensitiveIgnoresNonString() {
        Integer key = Integer.valueOf(456);
        int expected = key.hashCode();
        int actual = mapCaseInsensitive.invokeComputeKeyHashCode(key);
        assertEquals(expected, actual, "Even when map is case‑insensitive, non‑String keys must use their own hashCode()");
    }
}
