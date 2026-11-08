package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    private TestCompactMap caseSensitiveMap;

    private TestCompactMap caseInsensitiveMap;

    private MockedStatic<StringUtilities> stringUtilitiesMock;

    /**
     * Simple concrete subclass that makes the protected method public for testing.
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
         * Exposes the protected method under test.
         */
        int computeKeyHashCodePublic(Object key) {
            return computeKeyHashCode(key);
        }
    }

    /**
     * Subclass that throws from {@code isCaseInsensitive()} to test exception propagation.
     */
    private static class ExceptionMap extends TestCompactMap {

        ExceptionMap() {
            super(false);
        }

        @Override
        protected boolean isCaseInsensitive() {
            throw new IllegalStateException("isCaseInsensitive failure");
        }
    }

    /**
     * Custom key whose {@code hashCode()} returns a known constant.
     */
    private static final class CustomKey {

        private final int value;

        CustomKey(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * Key whose {@code hashCode()} throws an {@code IllegalStateException}.
     */
    private static final class ThrowingHashCode {

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Map that treats strings case‑sensitively.
        caseSensitiveMap = new TestCompactMap(false);
        // Map that treats strings case‑insensitively.
        caseInsensitiveMap = new TestCompactMap(true);
        // Mock the static utility used when case‑insensitivity is on.
        stringUtilitiesMock = mockStatic(StringUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock after each test to avoid cross‑test contamination.
        if (stringUtilitiesMock != null) {
            stringUtilitiesMock.close();
        }
    }

    @Test
    void computeKeyHashCode_StringCaseSensitive() {
        String key = "HelloWorld";
        // normal hashCode
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "When case‑sensitive, a String key should use its own hashCode()");
    }

    @Test
    void computeKeyHashCode_StringCaseInsensitive() {
        String key = "HelloWorld";
        // arbitrary value returned by the static helper
        int mockedHash = 42_424;
        stringUtilitiesMock.when(() -> StringUtilities.hashCodeIgnoreCase(key)).thenReturn(mockedHash);
        int actual = caseInsensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(mockedHash, actual, "When case‑insensitive, a String key should delegate to StringUtilities.hashCodeIgnoreCase");
        // Verify that the static method was indeed called.
        stringUtilitiesMock.verify(() -> StringUtilities.hashCodeIgnoreCase(key), times(1));
    }

    @Test
    void computeKeyHashCode_NullKey() {
        int actual = caseSensitiveMap.computeKeyHashCodePublic(null);
        assertEquals(0, actual, "Null key must produce hash code 0");
    }

    @Test
    void computeKeyHashCode_KeyIsMapItself() {
        // The map instance itself is used as the key.
        int actual = caseSensitiveMap.computeKeyHashCodePublic(caseSensitiveMap);
        assertEquals(37, actual, "When the key equals the map instance, the method must return the constant 37");
    }

    @Test
    void computeKeyHashCode_OtherKey() {
        Integer key = 12345;
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "For non‑String, non‑null keys that are not the map itself, the key's hashCode() should be returned");
    }

    @Test
    void computeKeyHashCode_EmptyStringCaseSensitive() {
        String key = "";
        // 0 for empty string
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Empty string with case‑sensitivity enabled should return its standard hashCode (0)");
    }

    @Test
    void computeKeyHashCode_EmptyStringCaseInsensitive() {
        String key = "";
        // empty string still yields 0
        int mockedHash = 0;
        stringUtilitiesMock.when(() -> StringUtilities.hashCodeIgnoreCase(key)).thenReturn(mockedHash);
        int actual = caseInsensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(mockedHash, actual, "Empty string with case‑insensitivity should delegate to StringUtilities.hashCodeIgnoreCase");
        stringUtilitiesMock.verify(() -> StringUtilities.hashCodeIgnoreCase(key), times(1));
    }

    @Test
    void computeKeyHashCode_NullKey_caseSensitive() {
        int actual = caseSensitiveMap.computeKeyHashCodePublic(null);
        assertEquals(0, actual, "Null key must produce hash code 0 (case‑sensitive map)");
    }

    @Test
    void computeKeyHashCode_NullKey_caseInsensitive() {
        int actual = caseInsensitiveMap.computeKeyHashCodePublic(null);
        assertEquals(0, actual, "Null key must produce hash code 0 even when map is case‑insensitive");
    }

    @Test
    void computeKeyHashCode_DifferentCompactMapInstanceAsKey() {
        // Another map instance used as a key (not the same as the map under test)
        TestCompactMap otherMap = new TestCompactMap(false);
        // default Object.hashCode()
        int expected = otherMap.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(otherMap);
        assertEquals(expected, actual, "A different CompactMap instance used as a key should return its own hashCode, not 37");
    }

    @Test
    void computeKeyHashCode_IntegerKey() {
        Integer key = 12345;
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Integer key should return its own hashCode");
    }

    @Test
    void computeKeyHashCode_BooleanKey() {
        Boolean key = Boolean.TRUE;
        // 1231
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Boolean.TRUE key should return its hashCode (1231)");
    }

    @Test
    void computeKeyHashCode_StringBuilderKey() {
        StringBuilder key = new StringBuilder("builder");
        // identity hash from Object
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "StringBuilder (non‑String CharSequence) should return its own hashCode");
    }

    @Test
    void computeKeyHashCode_ArrayKey() {
        int[] key = new int[] { 1, 2, 3 };
        // identity hash, not Arrays.hashCode
        int expected = key.hashCode();
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Array key should return its identity hashCode");
    }

    @Test
    void computeKeyHashCode_CustomObjectKey() {
        CustomKey key = new CustomKey(99);
        int expected = 99;
        int actual = caseSensitiveMap.computeKeyHashCodePublic(key);
        assertEquals(expected, actual, "Custom object with overridden hashCode should return that value");
    }

    @Test
    void computeKeyHashCode_KeyHashCodeThrowsException() {
        ThrowingHashCode key = new ThrowingHashCode();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> caseSensitiveMap.computeKeyHashCodePublic(key), "Exception from key.hashCode() must be propagated");
        assertEquals("hashCode failure", ex.getMessage());
    }

    @Test
    void computeKeyHashCode_isCaseInsensitiveThrowsException() {
        ExceptionMap map = new ExceptionMap();
        String key = "anyString";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.computeKeyHashCodePublic(key), "Exception from isCaseInsensitive() must be propagated");
        assertEquals("isCaseInsensitive failure", ex.getMessage());
    }
}
