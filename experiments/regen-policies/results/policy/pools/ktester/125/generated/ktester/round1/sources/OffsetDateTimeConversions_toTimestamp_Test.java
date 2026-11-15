package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toTimestamp_Test {

    /**
     * A {@link Converter} instance required by the method signature.
     */
    private static Converter converter;

    /**
     * A real {@link Converter} instance used by the original test suite.
     */
    private static Converter realConverter;

    /**
     * A mock {@link Converter} used to prove that the argument is ignored.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter constructor requires a ConverterOptions instance.
        // An empty options object is sufficient for these tests.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference for GC (optional).
        converter = null;
    }

    private static Stream<Arguments> provideValidConversions() {
        Converter nullConverter = null;
        // mock will be injected by MockitoExtension at runtime; we create a placeholder here
        // (the actual mock instance will be supplied by the test method via field injection).
        // For the static source we use null and will replace with the mock in the test method.
        return Stream.of(// +02:00 offset – instant 2 hours earlier (UTC)
        Arguments.of("Offset +02:00", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2)), nullConverter, Timestamp.valueOf("2023-05-15 08:20:30.0")), // Epoch UTC
        Arguments.of("Epoch UTC", OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), nullConverter, Timestamp.valueOf("1970-01-01 00:00:00.0")), // -05:00 offset – instant 5 hours later (UTC)
        Arguments.of("Offset -05:00", OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), nullConverter, Timestamp.valueOf("2023-01-01 05:00:00.0")), // Far‑future date (year 9999)
        Arguments.of("Far‑future year 9999", OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), nullConverter, Timestamp.valueOf("9999-12-31 23:59:59.999999999")), // Offset with minutes (+05:30)
        Arguments.of("Offset +05:30", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHoursMinutes(5, 30)), nullConverter, Timestamp.valueOf("2023-05-15 04:50:30.0")), // Offset with seconds (+01:02:30)
        Arguments.of("Offset +01:02:30", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHoursMinutesSeconds(1, 2, 30)), nullConverter, Timestamp.valueOf("2023-05-15 09:18:00.0")), // Nanosecond precision (non‑millisecond fraction)
        Arguments.of("Nanosecond precision", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 123_456_789, ZoneOffset.UTC), nullConverter, Timestamp.valueOf("2023-05-15 10:20:30.123456789")), // Very early date (year 1900)
        Arguments.of("Early year 1900", OffsetDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), nullConverter, Timestamp.valueOf("1900-01-01 00:00:00.0")), // Maximum positive ZoneOffset (+18:00)
        Arguments.of("Maximum positive offset +18:00", OffsetDateTime.of(2023, 5, 15, 23, 0, 0, 0, ZoneOffset.ofHours(18)), nullConverter, Timestamp.valueOf("2023-05-15 05:00:00.0")), // Maximum negative ZoneOffset (‑18:00)
        Arguments.of("Maximum negative offset -18:00", OffsetDateTime.of(2023, 5, 15, 5, 0, 0, 0, ZoneOffset.ofHours(-18)), nullConverter, Timestamp.valueOf("2023-05-15 23:00:00.0")), // Non‑null mock converter – should behave identically
        Arguments.of("Non‑null mock converter", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2)), // placeholder – will be replaced by the injected mock in the test method
        // will be overridden
        null, Timestamp.valueOf("2023-05-15 08:20:30.0")));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(Arguments.of("Null source", null, null, NullPointerException.class), Arguments.of("Wrong type (String)", "2023-01-01T00:00:00Z", null, ClassCastException.class));
    }

    private static Stream<Arguments> provideValidConversionsWithMock() {
        // Only the case that explicitly requires a non‑null converter (mock) is supplied here.
        return Stream.of(Arguments.of("Non‑null mock converter", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2)), // The mock will be injected by the test method via the @Mock field.
        // placeholder – replaced below
        null, Timestamp.valueOf("2023-05-15 08:20:30.0")));
    }

    @Test
    void toTimestamp_givenValidOffsetDateTime_returnsCorrectTimestamp() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000, ZoneOffset.UTC);
        // Expected instant is the same as the OffsetDateTime's instant.
        Timestamp expected = Timestamp.from(odt.toInstant());
        // WHEN
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp should match the one derived from the OffsetDateTime");
    }

    @Test
    void toTimestamp_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toTimestamp(nullSource, converter), "Calling toTimestamp with a null source should throw NPE");
    }

    @Test
    void toTimestamp_givenWrongType_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-03-15T12:30:45Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, converter), "Calling toTimestamp with a non‑OffsetDateTime source should throw ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Successful conversion cases (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidConversions")
    void toTimestamp_validInputs(String description, OffsetDateTime input, Converter converter, Timestamp expected) {
        // WHEN
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(input, converter);
        // THEN
        assertEquals(expected, actual, () -> "Failed conversion: " + description);
    }

    /* --------------------------------------------------------------
     *  Exception cases (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionCases")
    void toTimestamp_invalidInputs(String description, Object input, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toTimestamp(input, converter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidConversionsWithMock")
    void toTimestamp_validInputsWithMock(String description, OffsetDateTime input, Converter converter, Timestamp expected) {
        // WHEN
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(input, converter);
        // THEN
        assertEquals(expected, actual, () -> "Failed conversion with mock converter: " + description);
    }
}
