package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDouble_Test {

    /**
     * Shared Converter instance used by the tests.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the real Converter does not need any special configuration for this test.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        sharedConverter = null;
    }

    private static Stream<Arguments> validZonedDateTimeProvider() {
        Converter identity = mock(Converter.class);
        // The mock itself is irrelevant because we stub the static method.
        return Stream.of(arguments("Epoch start – IdentityInstantConverter", ZonedDateTime.parse("2023-01-01T00:00:00Z"), identity, 0.0), arguments("Non‑UTC offset – IdentityInstantConverter", ZonedDateTime.parse("2023-01-01T12:34:56.789+02:00[Europe/Paris]"), identity, 1672566896.789));
    }

    private static Stream<Arguments> extremeInstantProvider() {
        Converter identity = mock(Converter.class);
        return Stream.of(arguments("Instant.MAX → Infinity", ZonedDateTime.ofInstant(Instant.MAX, ZoneOffset.UTC), identity, Double.POSITIVE_INFINITY), arguments("Instant.MIN → large negative double", ZonedDateTime.ofInstant(Instant.MIN, ZoneOffset.UTC), identity, -31557014167219200.0));
    }

    private static Stream<Arguments> customConverterProvider() {
        // We use simple mock Converter instances; the behaviour is simulated by the stubbed static method.
        Converter multiplyByTwo = mock(Converter.class);
        Converter addThousand = mock(Converter.class);
        Converter nanConverter = mock(Converter.class);
        return Stream.of(arguments("MultiplyByTwoInstantConverter – epoch start", ZonedDateTime.parse("1970-01-01T00:00:00Z"), multiplyByTwo, 0.0), arguments("AddThousandInstantConverter – adds 1000", ZonedDateTime.parse("2023-01-01T12:34:56.789Z"), addThousand, 1672588496.789), arguments("NaNInstantConverter – returns NaN", ZonedDateTime.parse("2023-01-01T00:00:00Z"), nanConverter, Double.NaN));
    }

    private static Stream<Arguments> sameInstantDifferentZonesProvider() {
        Converter identity = mock(Converter.class);
        double expected = 1672531200.0;
        return Stream.of(arguments("Paris +02:00 representation", ZonedDateTime.parse("2023-01-01T12:00:00+02:00[Europe/Paris]"), identity, expected), arguments("UTC representation", ZonedDateTime.parse("2023-01-01T10:00:00Z"), identity, expected));
    }

    @Test
    void toDouble_delegatesToInstantConversions_andReturnsResult() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 15, 12, 30, 45, 123_456_789, ZoneOffset.UTC);
        Instant instant = zdt.toInstant();
        double expected = 12345.6789;
        // Mock the static method InstantConversions.toDouble(...)
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(instant, sharedConverter)).thenReturn(expected);
            // WHEN
            double actual = ZonedDateTimeConversions.toDouble(zdt, sharedConverter);
            // THEN
            assertEquals(expected, actual, "The double value should be the one returned by InstantConversions");
            mocked.verify(() -> InstantConversions.toDouble(instant, sharedConverter), times(1));
        }
    }

    @Test
    void toDouble_nullZonedDateTime_throwsNullPointerException() {
        // GIVEN
        Object nullZdt = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toDouble(nullZdt, sharedConverter));
    }

    @Test
    void toDouble_wrongSourceType_throwsClassCastException() {
        // GIVEN
        Object notAZonedDateTime = "I am not a ZonedDateTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toDouble(notAZonedDateTime, sharedConverter));
    }

    // ----------  Valid ZonedDateTime conversion ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validZonedDateTimeProvider")
    void toDouble_validZonedDateTime(String description, ZonedDateTime zdt, Converter converter, double expected) {
        Instant instant = zdt.toInstant();
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(instant, converter)).thenReturn(expected);
            double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
            assertEquals(expected, actual, "Unexpected conversion result");
            mocked.verify(() -> InstantConversions.toDouble(instant, converter), times(1));
        }
    }

    // ----------  Null handling (null converter) ----------
    @Test
    void toDouble_nullConverter_throwsNullPointerException() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-01T00:00:00Z");
        Converter nullConverter = null;
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(zdt.toInstant(), null)).thenThrow(NullPointerException.class);
            assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toDouble(zdt, nullConverter));
        }
    }

    // ----------  Extreme instant values ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeInstantProvider")
    void toDouble_extremeInstantValues(String description, ZonedDateTime zdt, Converter converter, double expected) {
        Instant instant = zdt.toInstant();
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(instant, converter)).thenReturn(expected);
            double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
            assertEquals(expected, actual, "Unexpected extreme instant conversion");
            mocked.verify(() -> InstantConversions.toDouble(instant, converter), times(1));
        }
    }

    // ----------  Custom converter behavior ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("customConverterProvider")
    void toDouble_customConverterBehavior(String description, ZonedDateTime zdt, Converter converter, double expected) {
        Instant instant = zdt.toInstant();
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(instant, converter)).thenReturn(expected);
            double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
            assertEquals(expected, actual, "Result does not match custom converter logic");
            mocked.verify(() -> InstantConversions.toDouble(instant, converter), times(1));
        }
    }

    // ----------  Different time zones but same instant ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("sameInstantDifferentZonesProvider")
    void toDouble_sameInstantDifferentZones(String description, ZonedDateTime zdt, Converter converter, double expected) {
        Instant instant = zdt.toInstant();
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(instant, converter)).thenReturn(expected);
            double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
            assertEquals(expected, actual, "Different zones representing same instant should yield same double");
            mocked.verify(() -> InstantConversions.toDouble(instant, converter), times(1));
        }
    }

    // ----------  Fractional nanos handling ----------
    @Test
    void toDouble_fractionalNanos_preservesPrecision() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-01T00:00:00.123456789Z");
        Converter identity = mock(Converter.class);
        // rounded to double precision
        double expected = 1672531200.1234568;
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(zdt.toInstant(), identity)).thenReturn(expected);
            double actual = ZonedDateTimeConversions.toDouble(zdt, identity);
            assertEquals(expected, actual, 1e-12, "Fractional nanoseconds should be represented correctly");
            mocked.verify(() -> InstantConversions.toDouble(zdt.toInstant(), identity), times(1));
        }
    }

    // ----------  Converter exception propagation ----------
    @Test
    void toDouble_converterThrowsException_propagates() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-01T00:00:00Z");
        Converter throwing = mock(Converter.class);
        IllegalStateException ex = new IllegalStateException("converter failure");
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toDouble(zdt.toInstant(), throwing)).thenThrow(ex);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ZonedDateTimeConversions.toDouble(zdt, throwing));
            assertSame(ex, thrown, "Exception from converter should propagate unchanged");
            mocked.verify(() -> InstantConversions.toDouble(zdt.toInstant(), throwing), times(1));
        }
    }
}
