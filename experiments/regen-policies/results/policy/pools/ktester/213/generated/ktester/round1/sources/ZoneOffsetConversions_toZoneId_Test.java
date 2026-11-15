package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toZoneId_Test {

    /**
     * Converter instance required by the method signature – not used internally.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with default options; the conversion logic inside
        // ZoneOffsetConversions.toZoneId does not depend on the Converter.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference – helps GC and mirrors typical teardown pattern.
        converter = null;
    }

    /**
     * Supplies the test data for {@link #toZoneId_variousInputs(Object, Object, boolean)}.
     *
     * @return a stream of arguments: (from, expectedResult, expectException)
     */
    static Stream<Arguments> toZoneIdTestCases() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset2 = ZoneOffset.ofHours(2);
        String notAZoneId = "America/New_York";
        ZoneId paris = ZoneId.of("Europe/Paris");
        ZoneOffset offset530 = ZoneOffset.ofHoursMinutes(5, 30);
        Date date = new Date();
        ZoneId systemDefault = ZoneId.systemDefault();
        return Stream.of(// Valid ZoneId instance
        arguments(utc, utc, false), // ZoneOffset is a subclass of ZoneId – should be cast successfully
        arguments(offset2, offset2, false), // String is not a ZoneId – expect ClassCastException
        arguments(notAZoneId, ClassCastException.class, true), // Null input – should return null
        arguments(null, null, false), // Another valid ZoneId instance (different reference)
        arguments(paris, paris, false), // ZoneOffset with non‑zero minutes
        arguments(offset530, offset530, false), // Unrelated type (Date) – expect ClassCastException
        arguments(date, ClassCastException.class, true), // System default ZoneId
        arguments(systemDefault, systemDefault, false));
    }

    @Test
    void toZoneId_givenZoneId_returnsSameInstance() {
        // GIVEN
        // a concrete ZoneId implementation
        ZoneId expected = ZoneOffset.ofHoursMinutes(5, 30);
        // WHEN
        ZoneId actual = ZoneOffsetConversions.toZoneId(expected, converter);
        // THEN
        assertSame(expected, actual, "The method should return the same ZoneId instance that was supplied.");
    }

    @Test
    void toZoneId_givenNonZoneId_throwsClassCastException() {
        // GIVEN
        Object notAZoneId = "America/New_York";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toZoneId(notAZoneId, converter), "Casting a non‑ZoneId object should result in ClassCastException.");
    }

    /**
     * Parameterized test that covers all scenarios from the supplied JSON matrix.
     *
     * @param from            the input value passed to {@code toZoneId}
     * @param expected        the expected result (a {@link ZoneId} instance or {@code null})
     * @param expectException {@code true} if a {@link ClassCastException} is expected
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("toZoneIdTestCases")
    void toZoneId_variousInputs(Object from, Object expected, boolean expectException) {
        if (expectException) {
            // When the input is not a ZoneId, the method should throw ClassCastException.
            assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toZoneId(from, converter), "Casting a non‑ZoneId object should result in ClassCastException.");
        } else {
            // Normal successful cast (including null handling).
            ZoneId actual = ZoneOffsetConversions.toZoneId(from, converter);
            if (expected == null) {
                assertNull(actual, "Casting null should return null without throwing.");
            } else {
                // The method returns the exact same instance that was supplied.
                assertSame(expected, actual, "The method should return the same ZoneId instance that was supplied.");
            }
        }
    }
}
