package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toAtomicLong_Test {

    @Mock
    private Converter // mock of the conversion utility
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already creates the mock instance.
        // Ensure a clean state before each test.
        reset(converter);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // 1. Long value
        Arguments a1 = Arguments.of(12345L, 12345L);
        // 2. Numeric String
        Arguments a2 = Arguments.of("67890", 67890L);
        // 3. Null input → 0
        Arguments a3 = Arguments.of(null, 0L);
        // 4. java.util.Date (epoch 0)
        Arguments a4 = Arguments.of(new Date(0L), 0L);
        // 5. ZonedDateTime parsed string
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]");
        Arguments a5 = Arguments.of(zdt, 1672563296000L);
        // 6. BigInteger
        Arguments a6 = Arguments.of(new BigInteger("9876543210"), 9876543210L);
        // 7. BigDecimal (truncated)
        Arguments a7 = Arguments.of(new BigDecimal("12345.67"), 12345L);
        // 8. java.sql.Timestamp
        Arguments a8 = Arguments.of(new Timestamp(1609459200000L), 1609459200000L);
        // 9. Integer
        Arguments a9 = Arguments.of(Integer.valueOf(42), 42L);
        // 10. Short
        Arguments a10 = Arguments.of(Short.valueOf((short) 7), 7L);
        // 11. Double (truncated)
        Arguments a11 = Arguments.of(Double.valueOf(123.9), 123L);
        // 12. Float (truncated, negative)
        Arguments a12 = Arguments.of(Float.valueOf(-45.6f), -45L);
        // 13. Instant
        Arguments a13 = Arguments.of(Instant.ofEpochMilli(1609459200123L), 1609459200123L);
        // 14. Calendar (UTC, epoch millis)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200000L);
        Arguments a14 = Arguments.of(cal, 1609459200000L);
        // 15. Map with DATE/TIME/ZONE keys
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(MapConversions.DATE, "2023-01-01");
        map.put(MapConversions.TIME, "12:34:56");
        map.put(MapConversions.ZONE, "Europe/Paris");
        Arguments a15 = Arguments.of(map, 1672572896000L);
        // 16. String with surrounding whitespace
        Arguments a16 = Arguments.of("  987  ", 987L);
        // 17. AtomicLong input (treated as Number)
        Arguments a17 = Arguments.of(new AtomicLong(555L), 555L);
        return Stream.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);
    }

    private static Stream<Arguments> provideExceptionConversions() {
        // 1. Invalid numeric string
        Arguments e1 = Arguments.of("not a number", NumberFormatException.class);
        // 2. Boolean (unsupported)
        Arguments e2 = Arguments.of(Boolean.TRUE, IllegalArgumentException.class);
        // 3. Character (unsupported)
        Arguments e3 = Arguments.of('A', IllegalArgumentException.class);
        // 4. Collection (unsupported)
        Arguments e4 = Arguments.of(new ArrayList<>(), IllegalArgumentException.class);
        // 5. Map without required keys
        Map<String, Object> badMap = new HashMap<>();
        badMap.put("unexpected", "value");
        Arguments e5 = Arguments.of(badMap, IllegalArgumentException.class);
        // 6. Empty string
        Arguments e6 = Arguments.of("", NumberFormatException.class);
        // 7. Whitespace‑only string
        Arguments e7 = Arguments.of("   ", NumberFormatException.class);
        // 8. BigDecimal overflow (greater than Long.MAX_VALUE)
        Arguments e8 = Arguments.of(new BigDecimal("9223372036854775808"), NumberFormatException.class);
        // 9. BigInteger overflow (greater than Long.MAX_VALUE)
        Arguments e9 = Arguments.of(new BigInteger("9223372036854775808"), NumberFormatException.class);
        // 10. BigDecimal underflow (less than Long.MIN_VALUE)
        Arguments e10 = Arguments.of(new BigDecimal("-9223372036854775809"), NumberFormatException.class);
        // 11. BigInteger underflow (less than Long.MIN_VALUE)
        Arguments e11 = Arguments.of(new BigInteger("-9223372036854775809"), NumberFormatException.class);
        return Stream.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11);
    }

    @Test
    void toAtomicLong_givenLongValue_returnsAtomicLongWithSameValue() {
        // GIVEN
        Long source = 42L;
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(42L);
        // WHEN
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(42L, result.get(), "AtomicLong should contain the long value supplied by the converter");
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    @Test
    void toAtomicLong_givenStringValue_returnsAtomicLongWithParsedLong() {
        // GIVEN
        String source = "12345";
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(12345L);
        // WHEN
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(12345L, result.get());
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    @Test
    void toAtomicLong_givenNull_returnsAtomicLongWithZero() {
        // GIVEN
        when(converter.convert(isNull(), eq(Long.class))).thenReturn(0L);
        // WHEN
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(null, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.get());
        verify(converter, times(1)).convert(isNull(), eq(Long.class));
    }

    @ParameterizedTest(name = "[{index}] from={0} ⇒ expected={1}")
    @MethodSource("provideSuccessfulConversions")
    void toAtomicLong_successfulConversion(Object from, long expected) {
        // WHEN
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(from, null);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(expected, result.get(), () -> "AtomicLong value mismatch for input: " + (from == null ? "null" : from.getClass()));
    }

    @Test
    void toAtomicLong_withCustomConverter_usesConverterResult() {
        // GIVEN: a converter that always returns 777L regardless of input
        Converter custom = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return 777L;
            }
        };
        String source = "any string";
        // WHEN
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, custom);
        // THEN
        assertNotNull(result);
        assertEquals(777L, result.get(), "When a non‑null Converter is supplied, its result should be used.");
    }

    @ParameterizedTest(name = "[{index}] from={0} ⇒ throws {1}")
    @MethodSource("provideExceptionConversions")
    void toAtomicLong_expectedExceptions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toAtomicLong(from, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + (from == null ? "null" : from.getClass()));
    }

    @Test
    void toAtomicLong_converterThrowsRuntimeException_propagates() {
        // GIVEN: a converter that throws RuntimeException
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ZonedDateTimeConversions.toAtomicLong(null, badConverter));
        assertEquals("converter failure", ex.getMessage(), "Exception from the supplied Converter should be propagated unchanged.");
    }
}
