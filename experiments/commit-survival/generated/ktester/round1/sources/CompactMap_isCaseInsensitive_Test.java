package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactMap_isCaseInsensitive_Test {

    private DefaultCompactMap caseSensitiveMap;

    private CaseInsensitiveCompactMap caseInsensitiveMap;

    /**
     * A concrete subclass that forces case‑insensitive handling.
     */
    private static class CaseInsensitiveCompactMap extends DefaultCompactMap {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }
    }

    /**
     * A concrete subclass that does NOT override {@code isCaseInsensitive()} – uses the default (false).
     */
    private static class DefaultCompactMap extends CompactMap<String, String> {

        /**
         * Expose {@code computeKeyHashCode} for the existing tests.
         */
        public int computeKeyHashCodePublic(Object key) {
            return super.computeKeyHashCode(key);
        }

        /**
         * Expose the protected {@code isCaseInsensitive()} for direct invocation.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        caseSensitiveMap = new DefaultCompactMap();
        caseInsensitiveMap = new CaseInsensitiveCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        caseSensitiveMap = null;
        caseInsensitiveMap = null;
    }

    /**
     * Verify that the default (case‑sensitive) implementation produces different hash codes
     * for strings that differ only by case.
     */
    @Test
    void computeKeyHashCode_isCaseSensitive_returnsDifferentHashCodes() {
        // GIVEN
        String upper = "Key";
        String lower = "key";
        // WHEN
        int hashUpper = caseSensitiveMap.computeKeyHashCodePublic(upper);
        int hashLower = caseSensitiveMap.computeKeyHashCodePublic(lower);
        // THEN
        assertNotEquals(hashUpper, hashLower, "When isCaseInsensitive() is false, hash codes for \"Key\" and \"key\" must differ");
    }

    /**
     * Verify that a subclass that overrides {@code isCaseInsensitive()} to {@code true}
     * yields identical hash codes for case‑variant strings.
     */
    @Test
    void computeKeyHashCode_isCaseInsensitive_returnsSameHashCodes() {
        // GIVEN
        String upper = "Key";
        String lower = "key";
        // WHEN
        int hashUpper = caseInsensitiveMap.computeKeyHashCodePublic(upper);
        int hashLower = caseInsensitiveMap.computeKeyHashCodePublic(lower);
        // THEN
        assertEquals(hashUpper, hashLower, "When isCaseInsensitive() is true, hash codes for \"Key\" and \"key\" must be equal");
    }

    /**
     * Ensure that the overridden {@code isCaseInsensitive()} method is indeed called
     * by checking the behaviour of {@code computeKeyHashCode} for a non‑String key,
     * which should fall back to the standard {@code Objects.hashCode()} implementation.
     */
    @Test
    void computeKeyHashCode_nonStringKey_ignoresCaseInsensitivity() {
        // GIVEN
        // non‑String key
        Integer key = 42;
        // WHEN
        int hashSensitive = caseSensitiveMap.computeKeyHashCodePublic(key);
        int hashInsensitive = caseInsensitiveMap.computeKeyHashCodePublic(key);
        // THEN
        assertEquals(hashSensitive, hashInsensitive, "Non‑String keys must produce the same hash code regardless of case‑insensitivity setting");
    }

    /**
     * Direct invocation of the protected {@code isCaseInsensitive()} via a concrete subclass.
     * The default implementation returns {@code false}, so the expectation is {@code false}.
     */
    @Test
    void isCaseInsensitive_directInvocation_returnsFalse() {
        // WHEN
        boolean result = caseSensitiveMap.isCaseInsensitivePublic();
        // THEN
        assertFalse(result, "Default implementation of isCaseInsensitive() should return false");
    }

    /**
     * Reflective invocation of {@code isCaseInsensitive()} on a subclass instance.
     * Even when accessed via reflection the method must still return {@code false}.
     */
    @Test
    void isCaseInsensitive_reflectionInvocation_returnsFalse() throws Exception {
        // GIVEN
        Method method = DefaultCompactMap.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        // WHEN
        boolean result = (boolean) method.invoke(caseSensitiveMap);
        // THEN
        assertFalse(result, "Reflective call to isCaseInsensitive() should also return false");
    }
}
