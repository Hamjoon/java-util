package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain Converter with default options – sufficient for all test cases.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        converter = null;
    }

    /**
     * Provides a stream of arguments for the parameterized test.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>description – a short string for readability in test reports</li>
     *   <li>source object – the {@code from} parameter for {@code toAtomicInteger}</li>
     *   <li>expected value – the integer value we expect inside the result (null if an exception is expected)</li>
     *   <li>expected exception – the exception class we expect (null for successful conversions)</li>
     *   <li>expectSameInstance – true if the spec says the original AtomicInteger instance must be returned</li>
     * </ul>
     */
    private static Stream<Arguments> atomicIntegerConversionProvider() {
        return Stream.of(// ----- Successful conversions -------------------------------------------------
        Arguments.of("AtomicInteger instance is returned unchanged", new AtomicInteger(42), 42, null, true), Arguments.of("Integer value is wrapped", Integer.valueOf(123), 123, null, false), Arguments.of("Long value is cast to int and wrapped", Long.valueOf(987L), 987, null, false), Arguments.of("String numeric value is parsed", "555", 555, null, false), Arguments.of("Map with key \"value\" containing Integer", Collections.singletonMap("value", 777), 777, null, false), Arguments.of("Map with alias key \"_v\" containing numeric String", Collections.singletonMap("_v", "888"), 888, null, false), Arguments.of("Short value is widened", Short.valueOf((short) 7), 7, null, false), Arguments.of("Byte value is widened (negative)", Byte.valueOf((byte) -3), -3, null, false), Arguments.of("BigInteger truncates to lower 32 bits", new BigInteger("12345678901234567890"), (int) new BigInteger("12345678901234567890").longValue(), null, false), Arguments.of("BigDecimal truncates toward zero", new BigDecimal("-42.99"), -42, null, false), Arguments.of("Map value String with surrounding whitespace is trimmed", Collections.singletonMap("value", "  321  "), 321, null, false), Arguments.of("Map alias \"_v\" with negative numeric string", Collections.singletonMap("_v", "-999"), -999, null, false), Arguments.of("Map value already an AtomicInteger – new wrapper with same int", Collections.singletonMap("value", new AtomicInteger(55)), 55, null, false), Arguments.of("Nested map one level deep containing Integer", Collections.singletonMap("value", Collections.singletonMap("value", 66)), 66, null, false), Arguments.of("Hexadecimal string is parsed as decimal 26", "0x1A", 26, null, false), Arguments.of("String with leading zero is parsed as decimal 75", "075", 75, null, false), Arguments.of("AtomicLong is cast to int and wrapped", new AtomicLong(12345L), 12345, null, false), // ----- Expected failures ----------------------------------------------------
        Arguments.of("Non‑numeric String throws ConversionException", "not-a-number", null, ConversionException.class, false), Arguments.of("Empty map throws ConversionException", Collections.emptyMap(), null, ConversionException.class, false), Arguments.of("Null source returns null (no exception)", null, null, null, false), Arguments.of("Boolean source throws ConversionException", Boolean.TRUE, null, ConversionException.class, false), Arguments.of("Map value null throws ConversionException", Collections.singletonMap("value", null), null, ConversionException.class, false), Arguments.of("Map value non‑numeric String throws ConversionException", Collections.singletonMap("value", "abc"), null, ConversionException.class, false), Arguments.of("Map value Boolean throws ConversionException", Collections.singletonMap("value", Boolean.FALSE), null, ConversionException.class, false), Arguments.of("Map value Date throws ConversionException", Collections.singletonMap("value", new Date()), null, ConversionException.class, false), Arguments.of("String with only whitespace throws ConversionException", "   ", null, ConversionException.class, false), Arguments.of("Scientific notation string throws ConversionException", "1e3", null, ConversionException.class, false), Arguments.of("Invalid hexadecimal string throws ConversionException", "0xG1", null, ConversionException.class, false), Arguments.of("Invalid octal string throws ConversionException", "08", null, ConversionException.class, false), Arguments.of("Double.NaN throws ConversionException", Double.NaN, null, ConversionException.class, false), Arguments.of("Double.POSITIVE_INFINITY throws ConversionException", Double.POSITIVE_INFINITY, null, ConversionException.class, false), Arguments.of("Float.NaN throws ConversionException", Float.NaN, null, ConversionException.class, false), Arguments.of("Long.MAX_VALUE exceeds int range – throws ConversionException", Long.MAX_VALUE, null, ConversionException.class, false), Arguments.of("Arbitrary Object throws ConversionException", new Object(), null, ConversionException.class, false), Arguments.of("LocalDate now throws ConversionException", java.time.LocalDate.now(), null, ConversionException.class, false), Arguments.of("LocalTime now throws ConversionException", java.time.LocalTime.now(), null, ConversionException.class, false), Arguments.of("EnumSet throws ConversionException", EnumSet.noneOf(java.time.DayOfWeek.class), null, ConversionException.class, false), Arguments.of("Deeply nested map without direct integer throws ConversionException", Collections.singletonMap("value", Collections.singletonMap("inner", 5)), null, ConversionException.class, false), Arguments.of("Null converter argument throws NullPointerException", new AtomicInteger(10), null, NullPointerException.class, false), Arguments.of("Array source throws ConversionException", new int[] { 1, 2, 3 }, null, ConversionException.class, false), Arguments.of("List source throws ConversionException", Arrays.asList(42), null, ConversionException.class, false), Arguments.of("AtomicBoolean source throws ConversionException", new AtomicBoolean(true), null, ConversionException.class, false), Arguments.of("Instant source throws ConversionException", java.time.Instant.now(), null, ConversionException.class, false));
    }

    @Test
    void toAtomicInteger_convertsMapWithValueKey() {
        // given
        Map<String, Object> source = new HashMap<>();
        // "_v" key expected by the conversion logic
        source.put(MapConversions.V, "42");
        // when
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the converted int value");
    }

    @Test
    void toAtomicInteger_convertsMapWithValueKeyAsNumber() {
        // given
        Map<String, Object> source = new HashMap<>();
        // direct Number
        source.put(MapConversions.V, 7);
        // when
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void toAtomicInteger_returnsNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // then
        assertNull(result, "Null source should produce a null AtomicInteger");
    }

    @Test
    void toAtomicInteger_returnsNullWhenMapDoesNotContainValueKey() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put("someOtherKey", "123");
        // when
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // then
        assertNull(result, "Map without the expected key should produce null");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("atomicIntegerConversionProvider")
    void toAtomicInteger_variousScenarios(String description, Object from, Integer expectedValue, Class<? extends Throwable> expectedException, boolean expectSameInstance) {
        // Arrange – if the test case expects a NullPointerException due to a null converter,
        // we deliberately set converter to null after the test data is prepared.
        Converter usedConverter = this.converter;
        if (expectedException == NullPointerException.class && from instanceof AtomicInteger) {
            // force NPE path
            usedConverter = null;
        }
        // Act & Assert
        if (expectedException != null) {
            // Expect an exception – the exact type is asserted.
            Throwable thrown = assertThrows(expectedException, () -> MapConversions.toAtomicInteger(from, usedConverter), description);
            // For ConversionException we could further assert a message, but the spec only requires the type.
            assertNotNull(thrown, "Exception should not be null");
        } else {
            // Successful conversion – result must be non‑null and contain the expected int.
            AtomicInteger result = MapConversions.toAtomicInteger(from, usedConverter);
            assertNotNull(result, description + " – result should not be null");
            assertEquals(expectedValue.intValue(), result.get(), description + " – unexpected integer value");
            if (expectSameInstance && from instanceof AtomicInteger) {
                // The spec says the same instance should be returned.
                assertSame(from, result, description + " – expected the original AtomicInteger instance to be returned");
            } else if (from instanceof AtomicInteger) {
                // For other AtomicInteger inputs we expect a *new* instance with the same value.
                assertNotSame(from, result, description + " – expected a new AtomicInteger instance");
            }
        }
    }
}
