package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Date;
import java.sql.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLong_Test {

    /**
     * Shared mock of {@link Converter}. The mock is created once for the whole test suite.
     */
    private static Converter mockConverter;

    /**
     * A {@link Date} instance used as the “given” value for the happy‑path tests.
     */
    private Date sampleDate;

    /**
     * Milliseconds since the epoch for {@link #sampleDate}.
     */
    private long expectedMillis;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by DateConversions.toLong, but the method signature requires it.
        // A Mockito mock is sufficient and avoids pulling in heavy ConverterOptions.
        mockConverter = mock(Converter.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a deterministic Date so the test is repeatable.
        // 2022‑01‑01T00:00:00Z
        expectedMillis = 1_640_995_200_000L;
        sampleDate = new Date(expectedMillis);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the mock reference – not strictly required but keeps the pattern symmetrical.
        mockConverter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Unix epoch (0 ms)
        Arguments.of(new Date(0L), 0L), // Typical non‑zero date (2020‑09‑13T12:26:40Z)
        Arguments.of(new Date(1_600_000_000_000L), 1_600_000_000_000L), // java.sql.Timestamp is a subclass of java.util.Date
        Arguments.of(new Timestamp(123_456_789L), 123_456_789L), // Extreme positive value
        Arguments.of(new Date(Long.MAX_VALUE), Long.MAX_VALUE), // Extreme negative value
        Arguments.of(new Date(Long.MIN_VALUE), Long.MIN_VALUE));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// A plain String is not a Date
        Arguments.of("2021-01-01"), // Completely unrelated type (ArrayList)
        Arguments.of(new ArrayList<>()));
    }

    @Test
    void toLong_returnsDateMillis_forUtilDate() {
        // given
        Object from = sampleDate;
        // when
        long actual = DateConversions.toLong(from, mockConverter);
        // then
        assertEquals(expectedMillis, actual, "toLong should return the same epoch milliseconds as Date#getTime()");
    }

    @Test
    void toLong_returnsDateMillis_forSqlDate() {
        // given
        java.sql.Date sqlDate = new java.sql.Date(expectedMillis);
        // java.sql.Date is a subclass of java.util.Date
        Object from = sqlDate;
        // when
        long actual = DateConversions.toLong(from, mockConverter);
        // then
        assertEquals(expectedMillis, actual, "toLong must correctly handle java.sql.Date (subclass of java.util.Date)");
    }

    @Test
    void toLong_throwsNullPointerException_whenFromIsNull() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DateConversions.toLong(from, mockConverter), "Passing a null source should result in NPE because the method casts to Date");
    }

    @Test
    void toLong_ignoresConverterParameter() {
        // given
        Object from = sampleDate;
        // Create a mock that throws if any method is called – we want to ensure DateConversions does NOT touch it.
        Converter strictMock = mock(Converter.class, invocation -> {
            fail("Converter should not be used by toLong");
            return null;
        });
        // when
        long actual = DateConversions.toLong(from, strictMock);
        // then
        assertEquals(expectedMillis, actual, "Converter argument must be ignored; result should still be correct");
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) → {1}")
    @MethodSource("validConversionProvider")
    void toLong_validConversions(Object from, long expected) {
        // The converter argument is deliberately null for these cases, matching the JSON spec.
        long actual = DateConversions.toLong(from, null);
        assertEquals(expected, actual, () -> "Expected " + expected + " but got " + actual + " for input: " + from);
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) throws ClassCastException")
    @MethodSource("invalidConversionProvider")
    void toLong_invalidConversions(Object from) {
        // when / then
        assertThrows(ClassCastException.class, () -> DateConversions.toLong(from, null), "Non‑Date inputs must cause a ClassCastException");
    }
}
