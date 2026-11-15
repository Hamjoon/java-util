package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class Converter_isSimpleTypeConversionSupported_2_Test {

    class Custom {
    }

    /**
     * Supplies a {@link Stream} of arguments for {@link #testIsSimpleTypeConversionSupported}.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>source class (may be {@code null})</li>
     *   <li>target class (may be {@code null})</li>
     *   <li>expected boolean result</li>
     *   <li>human‑readable description (used as the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> provideIsSimpleTypeConversionSupportedCases() {
        return Stream.of(/* Null handling */
        args(null, null, false, "Both source and target are null – conversion cannot be supported."), args(null, String.class, false, "Source is null while target is a concrete class – conversion not supported."), args(Integer.class, null, false, "Target is null while source is a concrete class – conversion not supported."), /* Same type conversion */
        args(String.class, String.class, true, "Source and target are the identical type – conversion is trivially supported."), args(int.class, int.class, true, "Both source and target are the same primitive type – conversion is supported."), /* Primitive ↔ Wrapper conversion */
        args(int.class, Integer.class, true, "Primitive int to its wrapper Integer – simple conversion supported."), args(Boolean.class, boolean.class, true, "Wrapper Boolean to primitive boolean – simple conversion supported."), args(double.class, Long.class, false, "Primitive double to unrelated wrapper Long – not a simple conversion."), /* Number ↔ Number conversion */
        args(Integer.class, Long.class, true, "Integer to Long – widening numeric conversion is supported."), args(BigDecimal.class, Double.class, true, "BigDecimal to Double – conversion via numeric value is supported."), args(Long.class, BigInteger.class, true, "Long to BigInteger – conversion is supported."), args(Float.class, BigInteger.class, false, "Floating‑point to integral BigInteger without explicit rounding – not a simple conversion."), /* String ↔ Primitive/Wrapper conversion */
        args(String.class, int.class, true, "String containing a parsable integer to primitive int – supported."), args(String.class, Boolean.class, true, "String \"true\"/\"false\" to Boolean wrapper – supported."), args(String.class, LocalDate.class, true, "ISO‑8601 date string to LocalDate – simple conversion supported."), args(String.class, UUID.class, true, "String representation of a UUID to java.util.UUID – supported."), args(String.class, Date.class, true, "String containing a timestamp to java.util.Date – supported."), args(String.class, File.class, false, "String to File is not a simple type conversion in this utility."), /* Date / Time conversion */
        args(Date.class, Instant.class, true, "java.util.Date to java.time.Instant – supported."), args(LocalDateTime.class, Date.class, true, "LocalDateTime to java.util.Date – supported via default zone."), args(Timestamp.class, ZonedDateTime.class, true, "SQL Timestamp to ZonedDateTime – supported."), args(LocalDate.class, LocalDateTime.class, true, "LocalDate to LocalDateTime – supported (time set to start of day)."), args(ZonedDateTime.class, Timestamp.class, true, "ZonedDateTime to SQL Timestamp – supported."), /* Atomic types conversion */
        args(Integer.class, AtomicInteger.class, true, "Integer to AtomicInteger – simple conversion supported."), args(AtomicLong.class, Long.class, true, "AtomicLong to Long – simple conversion supported."), args(AtomicBoolean.class, String.class, false, "AtomicBoolean to String is not a simple conversion."), /* Unsupported or edge conversions */
        args(List.class, Set.class, false, "Collection type conversion (List → Set) is not considered a simple type conversion."), args(Map.class, Properties.class, false, "Map to Properties is not a simple conversion."), args(Object.class, String.class, false, "Object to String without a specific conversion rule is not supported as a simple conversion."), args(Character.class, LocalDate.class, false, "Character to LocalDate has no logical simple conversion."), /* Primitive ↔ String conversion */
        args(int.class, String.class, true, "Primitive int can be converted to its textual representation (String)."), args(Integer.class, String.class, true, "Wrapper Integer to String via toString is a simple conversion."), args(boolean.class, Boolean.class, true, "Primitive boolean to its wrapper Boolean is a direct simple conversion."), args(Boolean.class, String.class, true, "Wrapper Boolean to String (\"true\"/\"false\") is a simple conversion."), args(char.class, Character.class, true, "Primitive char to its wrapper Character is directly supported."), args(Character.class, String.class, true, "Character to String (single‑character string) is a simple conversion."), /* Number ↔ String conversion */
        args(BigInteger.class, String.class, true, "BigInteger to its decimal String representation is a simple conversion."), args(BigDecimal.class, String.class, true, "BigDecimal to String via toPlainString is a simple conversion."), /* String ↔ BigNumber conversion */
        args(String.class, BigInteger.class, true, "String containing a decimal integer can be parsed into a BigInteger."), args(String.class, BigDecimal.class, true, "String containing a decimal number can be parsed into a BigDecimal."), /* Date / Time reverse conversions */
        args(Instant.class, Date.class, true, "Instant to java.util.Date (inverse of Date→Instant) is supported."), args(Instant.class, ZonedDateTime.class, true, "Instant can be converted to ZonedDateTime using the system default zone."), args(ZonedDateTime.class, Instant.class, true, "ZonedDateTime to Instant (UTC) is a simple conversion."), args(LocalDateTime.class, Instant.class, true, "LocalDateTime to Instant (using system default zone) is supported."), args(Instant.class, LocalDateTime.class, true, "Instant to LocalDateTime (system default zone) is a simple conversion."), args(Calendar.class, Date.class, true, "Calendar to Date conversion is directly supported."), args(Date.class, Calendar.class, true, "Date to Calendar conversion is directly supported."), /* Atomic ↔ Primitive/Wrapper conversions */
        args(int.class, AtomicInteger.class, true, "Primitive int can be boxed into an AtomicInteger."), args(AtomicInteger.class, int.class, true, "AtomicInteger can be unwrapped to a primitive int."), args(AtomicLong.class, Long.class, true, "AtomicLong to wrapper Long is a simple conversion (already covered but kept for symmetry)."), args(Long.class, AtomicLong.class, true, "Wrapper Long to AtomicLong is a simple conversion."), /* CharBuffer / ByteBuffer conversions */
        args(String.class, CharBuffer.class, true, "String can be wrapped into a CharBuffer (simple conversion)."), args(CharBuffer.class, String.class, true, "CharBuffer can be converted back to a String."), args(ByteBuffer.class, String.class, false, "ByteBuffer to String is not considered a simple type conversion by this utility."), /* Numeric widening primitive conversions */
        args(int.class, Long.class, true, "Primitive int can be widened to wrapper Long (simple numeric conversion)."), args(short.class, int.class, true, "Primitive short can be widened to primitive int."), args(float.class, Double.class, true, "Primitive float can be widened to wrapper Double."));
    }

    /**
     * Helper to create {@link Arguments} without verbose import of {@code arguments}.
     */
    private static Arguments args(Class<?> source, Class<?> target, boolean expected, String description) {
        return Arguments.of(source, target, expected, description);
    }

    @Test
    void simpleSameClassConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, String.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(int.class, int.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Boolean.class, Boolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(boolean.class, boolean.class));
    }

    @Test
    void primitiveToWrapperAndViceVersa() {
        assertTrue(Converter.isSimpleTypeConversionSupported(int.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, int.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(long.class, Long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Long.class, long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(boolean.class, Boolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Boolean.class, boolean.class));
    }

    @Test
    void stringToNumericConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Double.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, BigDecimal.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, BigInteger.class));
    }

    @Test
    void numericToStringConversion() {
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, String.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Double.class, String.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(BigDecimal.class, String.class));
    }

    @Test
    void dateAndTimeConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(Date.class, Long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Long.class, Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDate.class, Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDateTime.class, Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(ZonedDateTime.class, Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Date.class, LocalDate.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Date.class, LocalDateTime.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Date.class, ZonedDateTime.class));
    }

    @Test
    void atomicAndWrapperConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicBoolean.class, Boolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Boolean.class, AtomicBoolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicInteger.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, AtomicInteger.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicLong.class, Long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Long.class, AtomicLong.class));
    }

    @Test
    void collectionConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(List.class, Set.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Set.class, List.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Map.class, Map.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Collection.class, List.class));
    }

    @Test
    void bufferConversions() {
        assertTrue(Converter.isSimpleTypeConversionSupported(ByteBuffer.class, byte[].class));
        assertTrue(Converter.isSimpleTypeConversionSupported(CharBuffer.class, char[].class));
        assertTrue(Converter.isSimpleTypeConversionSupported(byte[].class, ByteBuffer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(char[].class, CharBuffer.class));
    }

    @Test
    void unsupportedCustomClassConversions() {
        class Custom {
        }
        assertFalse(Converter.isSimpleTypeConversionSupported(Custom.class, String.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(String.class, Custom.class));
    }

    @Test
    void unrelatedClassPairs() {
        assertFalse(Converter.isSimpleTypeConversionSupported(Object.class, String.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(String.class, Object.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(UUID.class, Integer.class));
    }

    @Test
    void primitiveToUnrelatedObject() {
        assertFalse(Converter.isSimpleTypeConversionSupported(int.class, UUID.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(boolean.class, Date.class));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideIsSimpleTypeConversionSupportedCases")
    void testIsSimpleTypeConversionSupported(Class<?> source, Class<?> target, boolean expected, String description) {
        // The method under test is a simple static delegator – we just assert the result.
        assertEquals(expected, Converter.isSimpleTypeConversionSupported(source, target), description);
    }
}
