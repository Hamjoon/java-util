package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toOffsetTime_Test {

    /**
     * Mocked Converter that will be passed to the method under test.
     */
    @Mock
    Converter mockConverter;

    /**
     * Mocked options returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    ConverterOptions mockOptions;

    /**
     * Holds a static mock for {@link BigDecimalConversions} (used in one test).
     */
    MockedStatic<BigDecimalConversions> bigDecimalMock;

    /**
     * Holds a static mock for {@link BigIntegerConversions} (used in one test).
     */
    MockedStatic<BigIntegerConversions> bigIntegerMock;

    @BeforeEach
    void setupBeforeEach() {
        // The method under test uses: converter.getOptions().getZoneId()
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Use UTC for deterministic results.
        when(mockOptions.getZoneId()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close any static mocks that might have been opened by a test.
        if (bigDecimalMock != null) {
            bigDecimalMock.close();
            bigDecimalMock = null;
        }
        if (bigIntegerMock != null) {
            bigIntegerMock.close();
            bigIntegerMock = null;
        }
    }

    /* --------------------------------------------------------------
     *  Tests for the numeric‑type branches (Integer, Long, AtomicLong,
     *  AtomicInteger)
     * -------------------------------------------------------------- */
    @Test
    void toOffsetTime_fromLong_returnsCorrectOffsetTime() {
        // 2020‑09‑13T12:26:40Z
        long epochMillis = 1_600_000_000_000L;
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(epochMillis, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_fromInteger_returnsCorrectOffsetTime() {
        // 1970‑01‑19T15:46:40Z
        int epochMillis = 1_600_000_000;
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(epochMillis, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_fromAtomicLong_returnsCorrectOffsetTime() {
        AtomicLong atomic = new AtomicLong(2_000L);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(2_000L), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(atomic, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_fromAtomicInteger_returnsCorrectOffsetTime() {
        AtomicInteger atomic = new AtomicInteger(3_000);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(3_000L), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(atomic, mockConverter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------
     *  Tests for the BigDecimal and BigInteger branches.
     * -------------------------------------------------------------- */
    @Test
    void toOffsetTime_fromBigDecimal_delegatesToBigDecimalConversions() {
        BigDecimal bd = new BigDecimal("12345");
        OffsetTime expected = OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC);
        // Mock the static call.
        bigDecimalMock = mockStatic(BigDecimalConversions.class);
        bigDecimalMock.when(() -> BigDecimalConversions.toOffsetTime(bd, mockConverter)).thenReturn(expected);
        OffsetTime actual = NumberConversions.toOffsetTime(bd, mockConverter);
        assertEquals(expected, actual);
        bigDecimalMock.verify(() -> BigDecimalConversions.toOffsetTime(bd, mockConverter));
    }

    @Test
    void toOffsetTime_fromBigInteger_delegatesToBigIntegerConversions() {
        BigInteger bi = new BigInteger("9876543210");
        OffsetTime expected = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);
        // Mock the static call.
        bigIntegerMock = mockStatic(BigIntegerConversions.class);
        bigIntegerMock.when(() -> BigIntegerConversions.toOffsetTime(bi, mockConverter)).thenReturn(expected);
        OffsetTime actual = NumberConversions.toOffsetTime(bi, mockConverter);
        assertEquals(expected, actual);
        bigIntegerMock.verify(() -> BigIntegerConversions.toOffsetTime(bi, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Test for unsupported source type.
     * -------------------------------------------------------------- */
    @Test
    void toOffsetTime_unsupportedType_throwsIllegalArgumentException() {
        String unsupported = "not a number";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetTime(unsupported, mockConverter));
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    void toOffsetTime_negativeInteger_returnsCorrectOffsetTime() {
        // -1 second
        Integer from = -1000;
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(-1000L), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_negativeLongWithCustomOffset_returnsCorrectOffsetTime() {
        Long from = -123_456_789L;
        ZoneId custom = ZoneOffset.ofHours(2);
        when(mockOptions.getZoneId()).thenReturn(custom);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(-123_456_789L), custom);
        OffsetTime actual = NumberConversions.toOffsetTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_zeroAtomicInteger_returnsCorrectOffsetTime() {
        AtomicInteger atomic = new AtomicInteger(0);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
        OffsetTime actual = NumberConversions.toOffsetTime(atomic, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_zeroAtomicLongWithNegativeOffset_returnsCorrectOffsetTime() {
        AtomicLong atomic = new AtomicLong(0L);
        ZoneId custom = ZoneOffset.ofHours(-3);
        when(mockOptions.getZoneId()).thenReturn(custom);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(0L), custom);
        OffsetTime actual = NumberConversions.toOffsetTime(atomic, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_longWithNonStandardOffset_returnsCorrectOffsetTime() {
        Long from = 0L;
        ZoneId offset = ZoneOffset.ofHoursMinutes(5, 30);
        when(mockOptions.getZoneId()).thenReturn(offset);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(0L), offset);
        OffsetTime actual = NumberConversions.toOffsetTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_longMaxValue_throwsDateTimeException() {
        Long from = Long.MAX_VALUE;
        // The default zone (UTC) is fine; the exception originates from Instant.ofEpochMilli.
        assertThrows(DateTimeException.class, () -> NumberConversions.toOffsetTime(from, mockConverter));
    }

    @Test
    void toOffsetTime_longMinValue_throwsDateTimeException() {
        Long from = Long.MIN_VALUE;
        assertThrows(DateTimeException.class, () -> NumberConversions.toOffsetTime(from, mockConverter));
    }

    @Test
    void toOffsetTime_unsupportedString_throwsIllegalArgumentException() {
        String unsupported = "unsupported";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetTime(unsupported, mockConverter));
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    void toOffsetTime_nullSource_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetTime(null, mockConverter));
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    void toOffsetTime_shortNotSupported_throwsIllegalArgumentException() {
        Short unsupported = 5;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetTime(unsupported, mockConverter));
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    void toOffsetTime_doubleNotSupported_throwsIllegalArgumentException() {
        Double unsupported = 3.14;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetTime(unsupported, mockConverter));
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    void toOffsetTime_nullConverter_throwsNullPointerException() {
        Integer from = 0;
        assertThrows(NullPointerException.class, () -> NumberConversions.toOffsetTime(from, null));
    }

    @Test
    void toOffsetTime_converterOptionsNull_throwsNullPointerException() {
        // Create a separate mock for the converter that returns null options.
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        Integer from = 123;
        assertThrows(NullPointerException.class, () -> NumberConversions.toOffsetTime(from, converterWithNullOptions));
    }

    @Test
    void toOffsetTime_converterZoneIdNull_throwsNullPointerException() {
        // Mock options that return null ZoneId.
        ConverterOptions optionsWithNullZone = mock(ConverterOptions.class);
        when(optionsWithNullZone.getZoneId()).thenReturn(null);
        when(mockConverter.getOptions()).thenReturn(optionsWithNullZone);
        Integer from = 42;
        // OffsetTime.ofInstant will receive a null ZoneId and throw NPE.
        assertThrows(NullPointerException.class, () -> NumberConversions.toOffsetTime(from, mockConverter));
    }
}
