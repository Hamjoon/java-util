package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    /**
     * Simple subclass that makes the protected method public for verification.
     */
    private static class TestStringMap extends CompactMap<String, Integer> {

        public String exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Subclass with a mismatched generic type to verify the cast behaviour.
     */
    private static class TestIntegerMap extends CompactMap<Integer, String> {

        public Integer exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code String}.
     */
    private static class StringMap extends CompactMap<String, Integer> {

        public String exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code Object}.
     */
    private static class ObjectMap extends CompactMap<Object, Integer> {

        public Object exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code Integer}.  Used to provoke a ClassCastException.
     */
    private static class IntegerMap extends CompactMap<Integer, String> {

        public Integer exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code CharSequence}.
     */
    private static class CharSequenceMap extends CompactMap<CharSequence, String> {

        public CharSequence exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code Comparable<String>}.
     */
    private static class ComparableStringMap extends CompactMap<Comparable<String>, String> {

        public Comparable<String> exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Map with key type {@code Number}.  Expected to fail casting.
     */
    private static class NumberMap extends CompactMap<Number, String> {

        public Number exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    static Stream<SuccessCase<?>> successCases() {
        return Stream.of(new SuccessCase<>(new CharSequenceMap(), "key"), new SuccessCase<>(new ComparableStringMap(), "key"));
    }

    static Stream<FailureCase<?>> failureCases() {
        return Stream.of(new FailureCase<>(new NumberMap(), ClassCastException.class));
    }

    @Test
    @DisplayName("getSingleValueKey() returns the literal \"key\" for matching generic type")
    void testGetSingleValueKeyReturnsLiteralKey() {
        TestStringMap map = new TestStringMap();
        String result = map.exposeGetSingleValueKey();
        assertEquals("key", result, "The protected getSingleValueKey() should return the literal \"key\"");
    }

    @Test
    @DisplayName("getSingleValueKey() throws ClassCastException when generic type does not match literal")
    void testGetSingleValueKeyClassCastException() {
        TestIntegerMap map = new TestIntegerMap();
        assertThrows(ClassCastException.class, map::exposeGetSingleValueKey, "Calling getSingleValueKey() with a mismatched generic type should throw ClassCastException");
    }

    @Test
    @DisplayName("getSingleValueKey() returns the literal \"key\" for matching generic type (String)")
    void testGetSingleValueKeyReturnsLiteralKey_String() {
        StringMap map = new StringMap();
        String result = map.exposeGetSingleValueKey();
        assertEquals("key", result, "The method should return the literal string \"key\".");
    }

    @Test
    @DisplayName("getSingleValueKey() returns the literal \"key\" as an Object")
    void testGetSingleValueKeyReturnsLiteralKey_Object() {
        ObjectMap map = new ObjectMap();
        Object result = map.exposeGetSingleValueKey();
        assertTrue(result instanceof String, "Result should be a String instance.");
        assertEquals("key", result, "The returned String value must equal \"key\".");
    }

    @Test
    @DisplayName("getSingleValueKey() throws ClassCastException for mismatched generic type (Integer)")
    void testGetSingleValueKeyClassCastException_Integer() {
        IntegerMap map = new IntegerMap();
        assertThrows(ClassCastException.class, map::exposeGetSingleValueKey, "Casting the literal \"key\" to Integer should throw ClassCastException.");
    }

    @ParameterizedTest(name = "[{index}] {0} should return \"key\"")
    @MethodSource("successCases")
    @DisplayName("getSingleValueKey() returns \"key\" for compatible generic types")
    <T> void testGetSingleValueKeyCompatibleTypes(SuccessCase<T> tc) {
        // The wrapper method returns the correctly typed value.
        T result = ((CompactMap<T, ?>) tc.map).getSingleValueKey();
        assertEquals(tc.expected, result, "For compatible generic type the returned value must be \"key\".");
    }

    @ParameterizedTest(name = "[{index}] {0} should throw {1}")
    @MethodSource("failureCases")
    @DisplayName("getSingleValueKey() throws ClassCastException for incompatible generic types")
    <T> void testGetSingleValueKeyIncompatibleTypes(FailureCase<T> tc) {
        assertThrows(tc.exception, () -> ((CompactMap<T, ?>) tc.map).getSingleValueKey(), "Casting \"key\" to an incompatible type must throw the expected exception.");
    }
}
