package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// kept for consistency with the template; not used here
@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_setMaxCacheLengthString_Test {

    /**
     * Reset the static cache length after each test so that other tests are not polluted.
     */
    @AfterEach
    void teardownAfterEach() {
        // default value as defined in the source code
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Helper that reads the private static field {@code maxCacheLengthString} via reflection.
     */
    private static int getMaxCacheLengthStringViaReflection() {
        try {
            Field field = CaseInsensitiveMap.class.getDeclaredField("maxCacheLengthString");
            field.setAccessible(true);
            // static field, so instance is null
            return (int) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access maxCacheLengthString field via reflection", e);
        }
    }

    private static Stream<Arguments> validLengthsProvider() {
        return Stream.of(// edge – minimum allowed
        Arguments.of(10), // typical value
        Arguments.of(50), // large but still reasonable
        Arguments.of(1000), // repeated call with same value
        Arguments.of(10), // just above minimum
        Arguments.of(11), // maximum int value
        Arguments.of(Integer.MAX_VALUE));
    }

    private static Stream<Arguments> invalidLengthsProvider() {
        return Stream.of(Arguments.of(9), Arguments.of(0), Arguments.of(-5), // -2147483648
        Arguments.of(Integer.MIN_VALUE));
    }

    @Test
    void validLengthsDoNotThrow() {
        assertDoesNotThrow(() -> CaseInsensitiveMap.setMaxCacheLengthString(10), "Length equal to the minimum (10) should be accepted");
        assertDoesNotThrow(() -> CaseInsensitiveMap.setMaxCacheLengthString(50), "Typical length values should be accepted");
        assertDoesNotThrow(() -> CaseInsensitiveMap.setMaxCacheLengthString(200), "Large length values should be accepted");
    }

    @Test
    void invalidLengthsThrowException() {
        assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(9), "Length lower than 10 must cause IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(0), "Zero length must cause IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(-5), "Negative length must cause IllegalArgumentException");
    }

    /**
     * The cache length influences only the internal caching of {@code CaseInsensitiveString}
     * instances.  Changing it must not break the observable behaviour of the map.
     *
     * This test verifies that after increasing the limit, a very long String key can still be
     * retrieved case‑insensitively.
     */
    @Test
    void longKeyRemainsCaseInsensitiveAfterIncreasingCacheLimit() {
        // increase the cache limit so that a 150‑character key will be cached
        CaseInsensitiveMap.setMaxCacheLengthString(200);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // build a long key (150 characters)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append('a');
        }
        String longKey = sb.toString();
        // same characters, different case
        String longKeyMixedCase = sb.toString().toUpperCase();
        map.put(longKey, "value");
        // retrieval with the original case
        assertEquals("value", map.get(longKey), "Value should be retrievable with original case");
        // retrieval with a different case must also succeed
        assertEquals("value", map.get(longKeyMixedCase), "Value should be retrievable case‑insensitively even for a long key");
    }

    /**
     * Verify that the default cache limit (100) still allows case‑insensitive access for
     * keys shorter than the limit.
     */
    @Test
    void shortKeyCaseInsensitiveWithDefaultLimit() {
        // ensure default limit is in place
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        String shortKey = "ShortKey";
        String shortKeyMixed = "sHoRtKeY";
        map.put(shortKey, "shortValue");
        assertEquals("shortValue", map.get(shortKeyMixed), "Short keys must be case‑insensitive with the default cache limit");
    }

    @ParameterizedTest(name = "valid length {0} should be accepted and stored")
    @MethodSource("validLengthsProvider")
    void validLengthsAreAccepted(int length) {
        assertDoesNotThrow(() -> CaseInsensitiveMap.setMaxCacheLengthString(length), () -> "Setting length " + length + " must not throw");
        // Verify that the static field was updated to the supplied value
        assertEquals(length, getMaxCacheLengthStringViaReflection(), "Static field should reflect the newly set length");
    }

    @ParameterizedTest(name = "invalid length {0} should throw IllegalArgumentException")
    @MethodSource("invalidLengthsProvider")
    void invalidLengthsThrow(int length) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(length), () -> "Setting length " + length + " must throw IllegalArgumentException");
        assertEquals("Max cache String length must be at least 10.", ex.getMessage(), "Exception message must match contract");
    }

    @Test
    void statePersistenceAfterFailedSet() {
        // 1. Set a known good baseline
        CaseInsensitiveMap.setMaxCacheLengthString(20);
        assertEquals(20, getMaxCacheLengthStringViaReflection(), "Baseline value should be 20");
        // 2. Attempt to set an invalid value; expect exception and unchanged state
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(5));
        assertEquals("Max cache String length must be at least 10.", ex.getMessage());
        // Verify that the static field is still 20
        assertEquals(20, getMaxCacheLengthStringViaReflection(), "Static field must remain unchanged after failed call");
        // 3. Set the same valid value again to confirm normal operation continues
        assertDoesNotThrow(() -> CaseInsensitiveMap.setMaxCacheLengthString(20));
        assertEquals(20, getMaxCacheLengthStringViaReflection(), "Static field should still be 20 after re‑setting the same valid value");
    }
}
