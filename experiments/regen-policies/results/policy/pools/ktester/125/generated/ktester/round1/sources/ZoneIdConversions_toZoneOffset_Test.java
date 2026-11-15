package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toZoneOffset_Test {

    // not used by the method but required by its signature
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Converter instance – the method under test does not use it,
        // but constructing a valid object satisfies the method contract.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make the test class tidy
        converter = null;
    }

    /**
     * Supplies arguments for {@link #toZoneOffset_Parameterized(String, Object, Converter, Class, ZoneOffset)}.
     *
     * @return a stream of test arguments
     */
    private static Stream<Arguments> provideToZoneOffsetCases() {
        // 1. Valid UTC ZoneId, converter = null, expect ZoneOffset.UTC
        Arguments utcCase = Arguments.of("Valid UTC ZoneId with null converter", ZoneId.of("UTC"), null, null, ZoneOffset.UTC);
        // 2. Null 'from' argument, any converter (null is fine), expect NullPointerException
        Arguments nullFromCase = Arguments.of("Null 'from' argument should throw NullPointerException", null, null, NullPointerException.class, null);
        // 3. Invalid type (String), converter = null, expect ClassCastException
        Arguments invalidTypeCase = Arguments.of("Invalid 'from' type (String) should throw ClassCastException", "some string", null, ClassCastException.class, null);
        return Stream.of(utcCase, nullFromCase, invalidTypeCase);
    }

    @Test
    void toZoneOffset_WithUtcZone_ReturnsZeroOffset() {
        // GIVEN
        ZoneId utc = ZoneId.of("UTC");
        // WHEN
        ZoneOffset result = ZoneIdConversions.toZoneOffset(utc, converter);
        // THEN
        // UTC is always offset zero regardless of the instant
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void toZoneOffset_WithSystemDefaultZone_ReturnsCurrentOffset() {
        // GIVEN
        ZoneId systemZone = ZoneId.systemDefault();
        Instant now = Instant.now();
        ZoneOffset expected = systemZone.getRules().getOffset(now);
        // WHEN
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(systemZone, converter);
        // THEN
        // The method uses Instant.now() internally; the two calls are close enough
        // that the offsets should be equal. In the unlikely case of a midnight rollover
        // between the two calls, the test will still pass because the offset does not
        // change at that moment for most zones.
        assertEquals(expected, actual);
    }

    @Test
    void toZoneOffset_WithNonZoneIdInput_ThrowsClassCastException() {
        // GIVEN
        String notAZoneId = "not-a-zone-id";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toZoneOffset(notAZoneId, converter));
    }

    /**
     * Parameterized test covering the three groups:
     * <ul>
     *   <li>Valid UTC ZoneId (converter may be null)</li>
     *   <li>Null {@code from} argument (expect NPE)</li>
     *   <li>Invalid {@code from} type (expect ClassCastException)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToZoneOffsetCases")
    void toZoneOffset_Parameterized(String description, Object from, Converter conv, Class<? extends Throwable> expectedException, ZoneOffset expectedResult) {
        if (expectedException != null) {
            // Expect an exception when invoking the method
            assertThrows(expectedException, () -> ZoneIdConversions.toZoneOffset(from, conv));
        } else {
            // No exception expected – verify the returned ZoneOffset
            ZoneOffset actual = ZoneIdConversions.toZoneOffset(from, conv);
            assertEquals(expectedResult, actual, description);
        }
    }
}
