package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toInstant_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction leakage between tests.
        reset(mockConverter);
    }

    /**
     * Supplies the test data for {@link #toInstant_variousScenarios(Object, Converter, Object)}.
     */
    private static Stream<Arguments> provideToInstantTestCases() {
        // Helper to avoid repetition of the mock (null is also acceptable)
        // the actual mock will be injected by the test instance; null is fine here.
        Converter mock = null;
        return Stream.of(// Standard conversion with +02:00 offset
        Arguments.of(OffsetDateTime.parse("2023-01-15T10:15:30+02:00"), mock, Instant.parse("2023-01-15T08:15:30Z")), // Epoch instant (zero offset)
        Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00Z"), mock, Instant.EPOCH), // Maximum supported year with a large positive offset
        Arguments.of(OffsetDateTime.parse("9999-12-31T23:59:59+18:00"), mock, Instant.parse("9999-12-31T05:59:59Z")), // Minimum supported year with a large negative offset
        Arguments.of(OffsetDateTime.parse("0000-01-01T00:00:00-18:00"), mock, Instant.parse("0000-01-01T18:00:00Z")), // Null source object – should throw NPE
        Arguments.of(null, mock, NullPointerException.class), // Invalid type (String) – should throw ClassCastException
        Arguments.of("2023-01-01T00:00:00Z", mock, ClassCastException.class), // Overflow scenario – instant beyond Instant.MAX
        Arguments.of(OffsetDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.ofHours(18)), mock, DateTimeException.class), // Underflow scenario – instant before Instant.MIN
        Arguments.of(OffsetDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), mock, DateTimeException.class));
    }

    /**
     * Verify that a valid {@link OffsetDateTime} is correctly converted to an {@link Instant}.
     */
    @Test
    void toInstant_givenValidOffsetDateTime_returnsCorrespondingInstant() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 12, 34, 56, 123_000_000, ZoneOffset.ofHoursMinutes(2, 30));
        Instant expected = odt.toInstant();
        // WHEN
        Instant actual = OffsetDateTimeConversions.toInstant(odt, mockConverter);
        // THEN
        assertEquals(expected, actual, "The Instant returned by toInstant should match the OffsetDateTime's instant.");
        // The converter is not used – verify no interactions.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException},
     * because the cast yields {@code null} and the subsequent call to {@code toInstant()} dereferences it.
     */
    @Test
    void toInstant_givenNullObject_throwsNullPointerException() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toInstant(nullObject, mockConverter), "Calling toInstant with a null source should throw NPE.");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that an object of an incompatible type causes a {@link ClassCastException}.
     */
    @Test
    void toInstant_givenIncompatibleType_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-05-17T12:34:56+02:30";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toInstant(notAnOffsetDateTime, mockConverter), "Calling toInstant with a non‑OffsetDateTime should throw ClassCastException.");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterised test that covers the exhaustive list of scenarios described in the
     * external JSON test definition. Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} argument (may be {@code null}, an {@link OffsetDateTime},
     *       or an invalid type)</li>
     *   <li>the {@code converter} argument (either {@code null} or the mocked instance)</li>
     *   <li>the expected outcome – either an {@link Instant} or the {@link Class} of an
     *       exception that should be thrown.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideToInstantTestCases")
    void toInstant_variousScenarios(Object from, Converter converter, Object expectedOutcome) {
        if (expectedOutcome instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expectedOutcome;
            assertThrows(expectedException, () -> OffsetDateTimeConversions.toInstant(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            // Expected a successful conversion to Instant
            Instant expectedInstant = (Instant) expectedOutcome;
            Instant actual = OffsetDateTimeConversions.toInstant(from, converter);
            assertEquals(expectedInstant, actual, () -> "Instant conversion mismatch for input: " + from);
        }
        // The method never uses the converter – ensure no interactions.
        verifyNoInteractions(mockConverter);
    }
}
