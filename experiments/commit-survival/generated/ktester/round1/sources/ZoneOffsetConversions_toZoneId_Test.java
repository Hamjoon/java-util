package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toZoneId_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (the @Mock field above).
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Supplies all test cases extracted from the provided JSON.
     *
     * @return a stream of arguments for {@link #testToZoneId(Object, Converter, ZoneId, Class)}
     */
    static Stream<Arguments> provideToZoneIdCases() {
        // ----- Helper converters -------------------------------------------------
        // Converter that would throw if its {@code convert} method were invoked.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // A benign dummy converter (does nothing).
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        // ----- Valid ZoneId / ZoneOffset instances -------------------------------
        ZoneId utc = ZoneId.of("UTC");
        ZoneId americaNY = ZoneId.of("America/New_York");
        ZoneId europeParis = ZoneId.of("Europe/Paris");
        ZoneOffset offset2 = ZoneOffset.ofHours(2);
        ZoneOffset offset5_30 = ZoneOffset.ofHoursMinutes(5, 30);
        ZoneOffset offset5_30_0 = ZoneOffset.ofHoursMinutesSeconds(5, 30, 0);
        // ----- Anonymous subclass of ZoneId --------------------------------------
        ZoneId customZoneId = new ZoneId() {

            @Override
            public String getId() {
                return "Custom/Zone";
            }
        };
        // ----- Build the argument list -------------------------------------------
        return Stream.of(// group: toZoneId
        arguments(utc, null, utc, null), arguments(offset2, null, offset2, null), arguments(null, null, null, null), arguments("UTC", null, null, ClassCastException.class), arguments(americaNY, dummyConverter, americaNY, null), // group: toZoneId_additional
        arguments(europeParis, null, europeParis, null), arguments(offset5_30, null, offset5_30, null), arguments(customZoneId, null, customZoneId, null), arguments(utc, throwingConverter, utc, null), arguments(Integer.valueOf(5), null, null, ClassCastException.class), // group: toZoneId_exception_cases
        arguments(Double.valueOf(3.14), null, null, ClassCastException.class), arguments(Boolean.TRUE, null, null, ClassCastException.class), arguments(List.of("UTC"), null, null, ClassCastException.class), arguments(Map.of("key", "value"), null, null, ClassCastException.class), arguments(new Object(), null, null, ClassCastException.class), arguments(LocalDateTime.now(), null, null, ClassCastException.class), arguments(Instant.now(), dummyConverter, null, ClassCastException.class));
    }

    @Test
    void toZoneId_WithValidZoneId_ReturnsSameInstance() {
        // given
        // any ZoneId implementation
        ZoneId expected = ZoneOffset.ofHoursMinutesSeconds(5, 30, 0);
        // when
        ZoneId actual = ZoneOffsetConversions.toZoneId(expected, mockConverter);
        // then
        assertSame(expected, actual, "The method should return the same ZoneId instance it received.");
    }

    @Test
    void toZoneId_WithInvalidObject_ThrowsClassCastException() {
        // given
        Object notAZoneId = "not-a-zone-id";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toZoneId(notAZoneId, mockConverter), "Casting a non‑ZoneId object should result in ClassCastException.");
    }

    /**
     * Parameterised test that covers all JSON‑defined cases.
     *
     * @param from               the source object passed to {@code toZoneId}
     * @param converter          the {@link Converter} argument (may be {@code null})
     * @param expectedResult     the expected {@link ZoneId} result (or {@code null})
     * @param expectedException  the exception that should be thrown, or {@code null}
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("provideToZoneIdCases")
    void testToZoneId(Object from, Converter converter, ZoneId expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // An exception is expected – verify the exact type.
            assertThrows(expectedException, () -> ZoneOffsetConversions.toZoneId(from, converter), "Expected a " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            // No exception – the method should return the same instance (or null).
            ZoneId actual = ZoneOffsetConversions.toZoneId(from, converter);
            if (expectedResult == null) {
                assertNull(actual, "Expected null result for input: " + from);
            } else {
                assertSame(expectedResult, actual, "Method should return the exact same ZoneId instance it received.");
            }
        }
    }
}
