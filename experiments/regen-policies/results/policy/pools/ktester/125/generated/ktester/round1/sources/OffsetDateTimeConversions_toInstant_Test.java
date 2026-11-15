package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toInstant_Test {

    @Mock
    private Converter // Converter is not used by the method but we provide a mock
    mockConverter;

    /* -------------------------------------------------------------
     *  Helper class used for the “converter ignored” test case.
     * ------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No implementation needed – the method under test does not call any methods.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via @Mock annotation
        // Ensure no unexpected interactions have happened before each test
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never interacts with the Converter
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Positive hour offset (+01:00)
        Arguments.of(OffsetDateTime.parse("2023-03-25T10:15:30+01:00"), null, Instant.parse("2023-03-25T09:15:30Z")), // Epoch (zero offset)
        Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00Z"), null, Instant.EPOCH), // Leap‑day with nanosecond precision and negative offset (-05:00)
        Arguments.of(OffsetDateTime.parse("2000-02-29T23:59:59.999999999-05:00"), null, Instant.parse("2000-03-01T04:59:59.999999999Z")), // Maximum OffsetDateTime value
        Arguments.of(OffsetDateTime.MAX, null, OffsetDateTime.MAX.toInstant()), // Minimum OffsetDateTime value
        Arguments.of(OffsetDateTime.MIN, null, OffsetDateTime.MIN.toInstant()), // Converter parameter ignored – non‑null converter supplied
        Arguments.of(OffsetDateTime.parse("2021-12-31T23:00:00+02:00"), new DummyConverter(), Instant.parse("2021-12-31T21:00:00Z")));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null 'from' argument – NPE due to cast then dereference
        Arguments.of(null, null, NullPointerException.class), // Incompatible type: String
        Arguments.of("2023-01-01T00:00Z", null, ClassCastException.class), // Incompatible type: Integer
        Arguments.of(12345, null, ClassCastException.class), // Incompatible type: generic Object
        Arguments.of(new Object(), null, ClassCastException.class));
    }

    @Test
    void toInstant_returnsCorrectInstant_forUtcOffset() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000, ZoneOffset.UTC);
        Instant expected = odt.toInstant();
        // WHEN
        Instant actual = OffsetDateTimeConversions.toInstant(odt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Instant produced from UTC OffsetDateTime should match");
    }

    @Test
    void toInstant_returnsCorrectInstant_forPositiveOffset() {
        // GIVEN
        // +05:00
        ZoneOffset offset = ZoneOffset.ofHours(5);
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 12, 30, 45, 0, offset);
        Instant expected = odt.toInstant();
        // WHEN
        Instant actual = OffsetDateTimeConversions.toInstant(odt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Instant should reflect the correct UTC moment for a +05:00 offset");
    }

    @Test
    void toInstant_returnsCorrectInstant_forNegativeOffset() {
        // GIVEN
        // -03:30
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-3, -30);
        OffsetDateTime odt = OffsetDateTime.of(2023, 12, 31, 23, 59, 59, 999_000_000, offset);
        Instant expected = odt.toInstant();
        // WHEN
        Instant actual = OffsetDateTimeConversions.toInstant(odt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Instant should reflect the correct UTC moment for a -03:30 offset");
    }

    @Test
    void toInstant_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toInstant(nullSource, mockConverter), "Calling toInstant with a null source should throw NPE because the method casts and dereferences");
    }

    /* -------------------------------------------------------------
     *  SUCCESS CASES – conversion returns the expected Instant.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("validConversionProvider")
    void toInstant_returnsExpectedInstant(Object from, Converter converter, Instant expected) {
        // WHEN
        Instant actual = OffsetDateTimeConversions.toInstant(from, converter);
        // THEN
        assertEquals(expected, actual, () -> "Instant conversion mismatch for input: " + from);
    }

    /* -------------------------------------------------------------
     *  EXCEPTION CASES – method should throw the expected exception.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expects {2}")
    @MethodSource("exceptionProvider")
    void toInstant_throwsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toInstant(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
