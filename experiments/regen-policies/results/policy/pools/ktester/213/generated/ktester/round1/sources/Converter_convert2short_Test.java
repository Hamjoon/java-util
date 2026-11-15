package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2short_Test {

    /**
     * Supplies all test cases (both normal and exceptional) for {@link #convert2short_various(Object, Object)}.
     *
     * @return a stream of {@link Arguments}
     */
    private static Stream<Arguments> provideConvert2shortCases() {
        return Stream.of(// -----------------------------------------------------------------
        // Group: convert2short (basic cases)
        // -----------------------------------------------------------------
        Arguments.of(null, (short) 0), Arguments.of(123, (short) 123), Arguments.of(32767, (short) 32767), Arguments.of(-32768, (short) -32768), // Double truncation
        Arguments.of(12345.67, (short) 12345), Arguments.of("4567", (short) 4567), Arguments.of("notANumber", NumberFormatException.class), Arguments.of(Boolean.TRUE, (short) 1), Arguments.of(Boolean.FALSE, (short) 0), // overflow string
        Arguments.of("123456", IllegalArgumentException.class), // overflow int
        Arguments.of(40000, IllegalArgumentException.class), Arguments.of(new BigDecimal("123.99"), (short) 123), Arguments.of(new BigInteger("32760"), (short) 32760), Arguments.of(new BigInteger("40000"), IllegalArgumentException.class), // -----------------------------------------------------------------
        // Group: convert2short_additional (wide range of source types)
        // -----------------------------------------------------------------
        Arguments.of(Short.valueOf((short) 12345), (short) 12345), Arguments.of(Byte.valueOf((byte) 100), (short) 100), Arguments.of(Character.valueOf('A'), (short) 65), Arguments.of(Long.valueOf(12345L), (short) 12345), Arguments.of(Float.valueOf(123.99F), (short) 123), Arguments.of(new AtomicInteger(200), (short) 200), Arguments.of(new AtomicLong(30000L), (short) 30000), Arguments.of(new AtomicBoolean(true), (short) 1), Arguments.of(new AtomicBoolean(false), (short) 0), Arguments.of("+321", (short) 321), Arguments.of("  123  ", (short) 123), Arguments.of("00123", (short) 123), Arguments.of("-123", (short) -123), Arguments.of("-40000", IllegalArgumentException.class), Arguments.of("", NumberFormatException.class), Arguments.of(CharBuffer.wrap("4567"), (short) 4567), Arguments.of(new BigDecimal("40000"), IllegalArgumentException.class), Arguments.of(new BigInteger("-200"), (short) -200), Arguments.of(new BigInteger("-40000"), IllegalArgumentException.class), Arguments.of(new ArrayList<>(Arrays.asList(1, 2, 3)), IllegalArgumentException.class), Arguments.of(new HashMap<String, Integer>() {

            {
                put("key", 1);
            }
        }, IllegalArgumentException.class), Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), IllegalArgumentException.class), // -----------------------------------------------------------------
        // Group: convert2short_exceptions_additional (unsupported types)
        // -----------------------------------------------------------------
        Arguments.of(Timestamp.valueOf("2023-01-01 00:00:00"), IllegalArgumentException.class), Arguments.of(LocalDate.parse("2023-01-01"), IllegalArgumentException.class), Arguments.of(LocalDateTime.parse("2023-01-01T12:34:56"), IllegalArgumentException.class), Arguments.of(ZonedDateTime.parse("2023-01-01T12:34:56+01:00[Europe/Paris]"), IllegalArgumentException.class), Arguments.of(Date.from(Instant.parse("2023-01-01T00:00:00Z")), IllegalArgumentException.class), Arguments.of(ByteBuffer.wrap("0000".getBytes()), IllegalArgumentException.class), Arguments.of("1e3", NumberFormatException.class), Arguments.of("12a34", NumberFormatException.class), Arguments.of("true", NumberFormatException.class), Arguments.of(DayOfWeek.MONDAY, IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class));
    }

    @Test
    void convertNullReturnsZero() {
        assertEquals((short) 0, Converter.convert2short(null), "A null source must be converted to the primitive default 0");
    }

    @Test
    void convertFromInteger() {
        Integer source = 123;
        assertEquals((short) 123, Converter.convert2short(source));
    }

    @Test
    void convertFromString() {
        String source = "-45";
        assertEquals((short) -45, Converter.convert2short(source));
    }

    @Test
    void convertFromBooleanTrue() {
        assertEquals((short) 1, Converter.convert2short(Boolean.TRUE));
    }

    @Test
    void convertFromBooleanFalse() {
        assertEquals((short) 0, Converter.convert2short(Boolean.FALSE));
    }

    @Test
    void convertFromDouble() {
        // conversion should truncate / round toward zero as per short cast
        Double source = 12.9;
        assertEquals((short) 12, Converter.convert2short(source));
    }

    @Test
    void convertFromBigDecimal() {
        BigDecimal source = new BigDecimal("7");
        assertEquals((short) 7, Converter.convert2short(source));
    }

    @Test
    void convertFromLargeNumberTruncatesToShort() {
        // 40 000 exceeds the range of short (-32768..32767) – the underlying conversion
        // uses a cast, so the high‑order bits are discarded.
        int source = 40_000;
        short expected = (short) source;
        assertEquals(expected, Converter.convert2short(source));
    }

    /**
     * Executes both successful and exceptional conversion cases.
     *
     * @param input          the value passed to {@code Converter.convert2short}
     * @param expectedResult either a {@link Short} (or any {@link Number}) for a successful conversion,
     *                       or a {@link Class} of the expected exception.
     */
    @ParameterizedTest(name = "{index}: convert2short({0}) => {1}")
    @MethodSource("provideConvert2shortCases")
    void convert2short_various(Object input, Object expectedResult) {
        if (expectedResult instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedResult;
            assertThrows(exClass, () -> Converter.convert2short(input), () -> "Expected exception " + exClass.getSimpleName() + " for input: " + input);
        } else {
            // Expected a numeric result
            short expected = ((Number) expectedResult).shortValue();
            short actual = Converter.convert2short(input);
            assertEquals(expected, actual, () -> "Conversion result mismatch for input: " + input);
        }
    }
}
