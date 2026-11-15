package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2boolean_Test {

    /**
     * A class whose {@code toString()} throws a RuntimeException.
     */
    private static final class ThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("boom from toString()");
        }
    }

    /**
     * A CharSequence implementation that throws from {@code length()}.
     */
    private static final class ThrowingCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new RuntimeException("boom from length()");
        }

        @Override
        public char charAt(int index) {
            return 'x';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this;
        }

        @Override
        public String toString() {
            return "throwing";
        }
    }

    static Stream<Arguments> stringValuesProvider() {
        return Stream.of(Arguments.of("true", true), Arguments.of("TRUE", true), Arguments.of("TrUe", true), Arguments.of("false", false), Arguments.of("any other text", false), Arguments.of("", false), Arguments.of("   ", false));
    }

    static Stream<Arguments> numericValuesProvider() {
        return Stream.of(Arguments.of(0, false), Arguments.of(1, true), Arguments.of(-42, true), Arguments.of(0.0, false), Arguments.of(3.14, true), // scientific zero – same as plain zero
        Arguments.of(0.0, false), Arguments.of(1e-10, true), // large zero – same as plain zero
        Arguments.of(0.0, false));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("0.0"), false), Arguments.of(new BigDecimal("12345"), true), Arguments.of(BigInteger.ZERO, false), Arguments.of(new BigInteger("987654321"), true));
    }

    static Stream<Arguments> charProvider() {
        return Stream.of(Arguments.of('t', true), Arguments.of('f', false), Arguments.of('Y', true), Arguments.of('N', false), Arguments.of('x', false));
    }

    static Stream<Arguments> atomicBooleanProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    static Stream<Arguments> collectionProvider() {
        return Stream.of(Arguments.of(List.of(), false), Arguments.of(List.of(1, 2, 3), true), Arguments.of(Set.of(), false), Arguments.of(Set.of("a"), true), Arguments.of(Map.of(), false), Arguments.of(Map.of("key", "value"), true), Arguments.of(new Object[] {}, false), Arguments.of(new Object[] { 0 }, true));
    }

    static Stream<Arguments> temporalProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), true), Arguments.of(LocalDate.of(2023, 1, 1), true), Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 0, 0), ZoneId.of("UTC")), true), // Instant epoch (0) – according to the JSON spec this should be false
        Arguments.of(Instant.ofEpochSecond(0), false), // Non‑zero instant
        Arguments.of(Instant.ofEpochSecond(1622548800L), true));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(new Object(), false), // explicit null – already covered but kept for completeness
        Arguments.of(null, false));
    }

    /**
     * The contract of {@code Converter.convert2boolean(Object)} states that a {@code null}
     * argument must result in {@code false}. This test verifies that behaviour.
     */
    @Test
    void convert2boolean_null_returnsFalse() {
        assertFalse(Converter.convert2boolean(null), "convert2boolean(null) should return false");
    }

    /**
     * Verify that a Boolean instance is returned unchanged.
     */
    @Test
    void convert2boolean_booleanTrue_returnsTrue() {
        assertTrue(Converter.convert2boolean(Boolean.TRUE), "convert2boolean(Boolean.TRUE) should return true");
    }

    @Test
    void convert2boolean_booleanFalse_returnsFalse() {
        assertFalse(Converter.convert2boolean(Boolean.FALSE), "convert2boolean(Boolean.FALSE) should return false");
    }

    /**
     * Strings that represent truthy values should be converted to {@code true}.
     * The underlying Converter treats the strings "true", "yes", "1" (case‑insensitive)
     * as true. We test a representative subset.
     */
    @Test
    void convert2boolean_stringTrueVariants_returnsTrue() {
        assertAll(() -> assertTrue(Converter.convert2boolean("true")), () -> assertTrue(Converter.convert2boolean("TRUE")), () -> assertTrue(Converter.convert2boolean("yes")), () -> assertTrue(Converter.convert2boolean("1")));
    }

    @Test
    void convert2boolean_stringFalseVariants_returnsFalse() {
        assertAll(() -> assertFalse(Converter.convert2boolean("false")), () -> assertFalse(Converter.convert2boolean("FALSE")), () -> assertFalse(Converter.convert2boolean("no")), () -> assertFalse(Converter.convert2boolean("0")), () -> assertFalse(Converter.convert2boolean("random")));
    }

    /**
     * Numeric values: zero should be {@code false}, any non‑zero should be {@code true}.
     */
    @Test
    void convert2boolean_numericZero_returnsFalse() {
        assertAll(() -> assertFalse(Converter.convert2boolean(0)), () -> assertFalse(Converter.convert2boolean(0L)), () -> assertFalse(Converter.convert2boolean(0.0)), () -> assertFalse(Converter.convert2boolean(0.0f)), () -> assertFalse(Converter.convert2boolean((short) 0)), () -> assertFalse(Converter.convert2boolean((byte) 0)));
    }

    @Test
    void convert2boolean_numericNonZero_returnsTrue() {
        assertAll(() -> assertTrue(Converter.convert2boolean(42)), () -> assertTrue(Converter.convert2boolean(-7L)), () -> assertTrue(Converter.convert2boolean(3.14)), () -> assertTrue(Converter.convert2boolean(2.71f)), () -> assertTrue(Converter.convert2boolean((short) 5)), () -> assertTrue(Converter.convert2boolean((byte) 9)));
    }

    /**
     * Collections and arrays: the Converter extracts a "value" key or first element.
     * For the purpose of this test we rely on the default conversion rules:
     * a non‑empty collection is considered {@code true}, an empty one {@code false}.
     */
    @Test
    void convert2boolean_collectionNonEmpty_returnsTrue() {
        assertTrue(Converter.convert2boolean(java.util.List.of("anything")), "Non‑empty collection should be true");
    }

    @Test
    void convert2boolean_collectionEmpty_returnsFalse() {
        assertFalse(Converter.convert2boolean(java.util.Collections.emptyList()), "Empty collection should be false");
    }

    /**
     * Date‑related objects are considered {@code true} because they are non‑null.
     */
    @Test
    void convert2boolean_dateObjects_returnTrue() {
        assertAll(() -> assertTrue(Converter.convert2boolean(new java.util.Date())), () -> assertTrue(Converter.convert2boolean(java.time.LocalDate.now())), () -> assertTrue(Converter.convert2boolean(java.time.LocalDateTime.now())), () -> assertTrue(Converter.convert2boolean(java.time.ZonedDateTime.now())), () -> assertTrue(Converter.convert2boolean(java.sql.Timestamp.from(java.time.Instant.now()))));
    }

    /**
     * Verify that an arbitrary object that is not specially handled is treated as {@code true}
     * because it is non‑null.
     */
    @Test
    void convert2boolean_arbitraryObject_returnsTrue() {
        Object dummy = new Object();
        assertTrue(Converter.convert2boolean(dummy), "A non‑null arbitrary object should be converted to true");
    }

    /**
     *  String values – case‑sensitive and whitespace handling **
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringValuesProvider")
    void convert2boolean_stringValues(String input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "String \"" + input + "\" should convert to " + expected);
    }

    /**
     *  Numeric primitive types **
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericValuesProvider")
    void convert2boolean_numericValues(Number input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Number " + input + " should convert to " + expected);
    }

    /**
     *  BigDecimal and BigInteger **
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("bigNumberProvider")
    void convert2boolean_bigNumbers(Object input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Big number " + input + " should convert to " + expected);
    }

    /**
     *  Character / Char values **
     */
    @ParameterizedTest(name = "[{index}] '{0}' → {1}")
    @MethodSource("charProvider")
    void convert2boolean_charValues(Character input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Character '" + input + "' should convert to " + expected);
    }

    /**
     *  AtomicBoolean **
     */
    @ParameterizedTest(name = "[{index}] AtomicBoolean({0}) → {1}")
    @MethodSource("atomicBooleanProvider")
    void convert2boolean_atomicBoolean(AtomicBoolean input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "AtomicBoolean " + input + " should convert to " + expected);
    }

    /**
     *  Collections & Arrays **
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("collectionProvider")
    void convert2boolean_collections(Object input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Collection/Array " + input + " should convert to " + expected);
    }

    /**
     *  Date / Temporal types **
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("temporalProvider")
    void convert2boolean_temporalObjects(Object input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Temporal object " + input + " should convert to " + expected);
    }

    /**
     *  Unsupported / Miscellaneous types – expecting false **
     */
    @ParameterizedTest(name = "[{index}] Object → {1}")
    @MethodSource("unsupportedProvider")
    void convert2boolean_unsupportedTypes(Object input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "Unsupported type " + input + " should convert to " + expected);
    }

    @Test
    void convert2boolean_unsupportedType_throwsIllegalArgumentException() {
        Thread thread = new Thread();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2boolean(thread), "Converting an unsupported type (Thread) should throw IllegalArgumentException");
    }

    @Test
    void convert2boolean_toStringThrowsRuntimeException_propagates() {
        ThrowingToString obj = new ThrowingToString();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Converter.convert2boolean(obj), "Exception from toString() should propagate");
        assertEquals("boom from toString()", ex.getMessage());
    }

    @Test
    void convert2boolean_readOnlyByteBuffer_throwsReadOnlyBufferException() {
        ByteBuffer ro = ByteBuffer.wrap("true".getBytes()).asReadOnlyBuffer();
        assertThrows(java.nio.ReadOnlyBufferException.class, () -> Converter.convert2boolean(ro), "Read‑only ByteBuffer should cause ReadOnlyBufferException");
    }

    @Test
    void convert2boolean_charSequenceLengthThrowsRuntimeException_propagates() {
        ThrowingCharSequence seq = new ThrowingCharSequence();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Converter.convert2boolean(seq), "Exception from CharSequence.length() should propagate");
        assertEquals("boom from length()", ex.getMessage());
    }
}
