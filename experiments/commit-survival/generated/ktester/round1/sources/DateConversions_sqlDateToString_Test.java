package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_sqlDateToString_Test {

    // shared for all test methods
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter has a public constructor that takes a ConverterOptions instance.
        // Using the default options is sufficient for this test.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides valid {@code java.sql.Date} instances together with the {@code Converter}
     * to be used for the parameterized test above.
     */
    private static Stream<Arguments> provideValidSqlDates() {
        // Normal case – a specific calendar date
        Date normal = Date.valueOf("2023-01-15");
        // Epoch start (1970‑01‑01)
        Date epoch = new Date(0L);
        // Very large timestamp (Long.MAX_VALUE)
        Date max = new Date(Long.MAX_VALUE);
        // Mix of real Converter and null Converter to verify both pathways.
        return Stream.of(// null Converter
        Arguments.of(normal, null), Arguments.of(normal, new Converter(new ConverterOptions())), Arguments.of(epoch, null), Arguments.of(epoch, new Converter(new ConverterOptions())), Arguments.of(max, null), Arguments.of(max, new Converter(new ConverterOptions())));
    }

    @Test
    void sqlDateToString_delegatesToToString() {
        // GIVEN
        long now = System.currentTimeMillis();
        // java.sql.Date (subclass of java.util.Date)
        Date sqlDate = new Date(now);
        // WHEN
        String actual = DateConversions.sqlDateToString(sqlDate, converter);
        // THEN
        // The method should produce the same string as a direct call to toString()
        // with a java.util.Date that has the same epoch millisecond value.
        String expected = DateConversions.toString(new java.util.Date(now), converter);
        assertEquals(expected, actual, "sqlDateToString should delegate to toString with an equivalent java.util.Date");
    }

    /**
     * Happy‑path cases: a {@code java.sql.Date} is converted to a {@code java.util.Date}
     * and then to a {@code String} via {@code DateConversions.toString(...)}.
     *
     * The test is parameterized to cover several representative timestamps,
     * including a typical date, the epoch start, and the maximum {@code long}
     * value. The {@code Converter} argument is also varied (real instance and {@code null}).
     */
    @ParameterizedTest(name = "{index}: sqlDate={0}, converter={1}")
    @MethodSource("provideValidSqlDates")
    void sqlDateToString_delegatesCorrectly(Date sqlDate, Converter conv) {
        // WHEN
        String actual = DateConversions.sqlDateToString(sqlDate, conv);
        // THEN
        // Expected value is produced by directly calling the delegated method.
        String expected = DateConversions.toString(new java.util.Date(sqlDate.getTime()), conv);
        assertEquals(expected, actual, "sqlDateToString should delegate to toString with an equivalent java.util.Date");
    }

    /**
     * Passing {@code null} as the {@code from} argument must result in a
     * {@link NullPointerException} because the implementation immediately calls
     * {@code sqlDate.getTime()}.
     */
    @Test
    void sqlDateToString_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DateConversions.sqlDateToString(null, null), "Calling sqlDateToString with a null 'from' should throw NullPointerException");
    }

    /**
     * Supplying an object that is not a {@code java.sql.Date} (e.g., {@code Integer})
     * should cause a {@link ClassCastException} during the cast to {@code Date}.
     */
    @Test
    void sqlDateToString_invalidType_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> DateConversions.sqlDateToString(Integer.valueOf(42), null), "Calling sqlDateToString with an invalid type should throw ClassCastException");
    }
}
