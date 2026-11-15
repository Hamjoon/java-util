package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toTimestamp_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Provides normal conversion test data (epoch start, day after epoch,
     * leap‑year, and a recent date with a non‑null converter).
     */
    private static Stream<Arguments> normalConversionProvider() {
        // a non‑null converter that does nothing (the method must ignore it)
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // never called
                return null;
            }
        };
        return Stream.of(// epoch start – already covered but kept for completeness
        Arguments.of(LocalDate.of(1970, 1, 1), null, new Timestamp(0L), "Convert the epoch start date (1970‑01‑01)"), // day after epoch
        Arguments.of(LocalDate.of(1970, 1, 2), null, new Timestamp(86_400_000L), "Convert the day after the epoch (1970‑01‑02)"), // leap‑year date
        Arguments.of(LocalDate.of(2000, 2, 29), null, new Timestamp(951_782_400_000L), "Convert a leap‑year date (2000‑02‑29)"), // recent date with a non‑null converter
        Arguments.of(LocalDate.of(2023, 12, 31), dummyConverter, new Timestamp(((LocalDate.of(2023, 12, 31).toEpochDay()) * 86_400L) * 1_000L), "Convert a recent date using a non‑null Converter"));
    }

    /**
     * Provides boundary conversion test data:
     *   - one day before epoch (negative milliseconds)
     *   - maximum LocalDate that still fits in a signed 64‑bit millisecond value
     *   - a date that overflows the long range (the method does not guard against overflow)
     */
    private static Stream<Arguments> boundaryConversionProvider() {
        return Stream.of(Arguments.of(LocalDate.of(1969, 12, 31), new Timestamp(-86_400_000L), "Convert a date one day before the epoch (1969‑12‑31)"), Arguments.of(LocalDate.of(292_278_994, 8, 17), new Timestamp(((LocalDate.of(292_278_994, 8, 17).toEpochDay()) * 86_400L) * 1_000L), "Convert the maximum LocalDate that still fits in a signed 64‑bit millisecond value"), Arguments.of(LocalDate.of(292_278_994, 8, 18), // overflow is intentional – compute exactly as the method does
        new Timestamp(((LocalDate.of(292_278_994, 8, 18).toEpochDay()) * 86_400L) * 1_000L), "Convert a LocalDate whose epoch‑day multiplication overflows the long range"));
    }

    @Test
    @DisplayName("toTimestamp returns epoch 0 for 1970‑01‑01")
    void toTimestamp_EpochStart_ReturnsZero() {
        // given
        LocalDate date = LocalDate.of(1970, 1, 1);
        // when
        Timestamp ts = LocalDateConversions.toTimestamp(date, mockConverter);
        // then
        assertEquals(0L, ts.getTime(), "Timestamp for 1970‑01‑01 should be zero milliseconds since the epoch");
    }

    @Test
    @DisplayName("toTimestamp returns correct milliseconds for a typical date")
    void toTimestamp_TypicalDate_ReturnsCorrectTimestamp() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        // days → seconds → milliseconds
        long expected = date.toEpochDay() * 86_400L * 1_000L;
        // when
        Timestamp ts = LocalDateConversions.toTimestamp(date, mockConverter);
        // then
        assertEquals(expected, ts.getTime(), "Timestamp does not match expected epoch milliseconds for the given LocalDate");
    }

    @Test
    @DisplayName("toTimestamp throws NullPointerException when source is null")
    void toTimestamp_NullSource_ThrowsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toTimestamp(nullSource, mockConverter), "Method should throw NPE when the source object is null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversionProvider")
    @DisplayName("Normal conversions")
    void toTimestamp_NormalConversions(LocalDate from, Converter converter, Timestamp expected, String description) {
        // when
        Timestamp actual = LocalDateConversions.toTimestamp(from, converter);
        // then
        assertEquals(expected.getTime(), actual.getTime(), () -> "Unexpected timestamp for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryConversionProvider")
    @DisplayName("Boundary conversions")
    void toTimestamp_BoundaryConversions(LocalDate from, Timestamp expected, String description) {
        // when
        Timestamp actual = LocalDateConversions.toTimestamp(from, mockConverter);
        // then
        assertEquals(expected.getTime(), actual.getTime(), () -> "Boundary test failed: " + description);
    }

    @Test
    @DisplayName("ClassCastException when source is not a LocalDate")
    void toTimestamp_IncompatibleType_ThrowsClassCastException() {
        // given
        Object notALocalDate = "2023-01-01";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toTimestamp(notALocalDate, mockConverter), "Passing a non‑LocalDate should result in ClassCastException");
    }

    @Test
    @DisplayName("Method ignores a Converter that throws when called")
    void toTimestamp_ConverterThrows_IsIgnored() {
        // given: a Converter that throws on any convert() call
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        LocalDate date = LocalDate.of(2022, 1, 1);
        long expected = ((date.toEpochDay()) * 86_400L) * 1_000L;
        // when
        Timestamp ts = LocalDateConversions.toTimestamp(date, explodingConverter);
        // then
        assertEquals(expected, ts.getTime(), "The method must ignore the provided Converter and still return the correct Timestamp");
    }
}
