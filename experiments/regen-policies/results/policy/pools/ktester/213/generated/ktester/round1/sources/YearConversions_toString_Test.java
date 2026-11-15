package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.chrono.IsoEra;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toString_Test {

    // to satisfy the method signature.
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Valid Year instance, null converter allowed
        Arguments.of(Year.of(2023), null, "2023"), // Null source should cause NPE
        Arguments.of(null, null, NullPointerException.class), // Wrong type (String) should cause ClassCastException
        Arguments.of("2023", null, ClassCastException.class));
    }

    private static Stream<Arguments> boundaryYearProvider() {
        return Stream.of(Arguments.of(Year.of(999_999_999), "999999999"), Arguments.of(Year.of(-999_999_999), "-999999999"), Arguments.of(Year.of(0), "0"), Arguments.of(Year.of(-1), "-1"));
    }

    @Test
    void toString_ReturnsYearString() {
        // given
        Year year = Year.of(2023);
        // when
        String result = YearConversions.toString(year, mockConverter);
        // then
        assertEquals("2023", result, "Year.toString() should be delegated correctly");
    }

    @Test
    void toString_NullSource_ThrowsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> YearConversions.toString(nullSource, mockConverter), "Calling toString with a null source should throw NPE");
    }

    @Test
    void toString_InvalidType_ThrowsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toString(notAYear, mockConverter), "Calling toString with a non‑Year object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expected={2}")
    @MethodSource("basicFunctionalityProvider")
    void toString_BasicFunctionality(Object from, Converter converter, Object expected) {
        if (expected instanceof String) {
            // Normal successful conversion
            String result = YearConversions.toString(from, converter);
            assertEquals(expected, result);
        } else if (expected instanceof Class) {
            // Expected exception
            assertThrows((Class<? extends Throwable>) expected, () -> YearConversions.toString(from, converter));
        } else {
            fail("Invalid test data configuration");
        }
    }

    @ParameterizedTest(name = "[{index}] Year.of({0}) → \"{0}\"")
    @MethodSource("boundaryYearProvider")
    void toString_BoundaryYears(Year year, String expected) {
        String result = YearConversions.toString(year, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    void toString_WithRealConverterInstance_DoesNotUseIt() {
        // A real Converter implementation that would throw if called.
        Converter realConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> toClass) {
                throw new UnsupportedOperationException("Should not be called");
            }
        };
        Year year = Year.of(2025);
        // No exception should be thrown; the converter is ignored.
        String result = YearConversions.toString(year, realConverter);
        assertEquals("2025", result);
    }

    @Test
    void toString_WithMockitoMockConverter_NoInteraction() {
        Year year = Year.of(1999);
        // The mock is injected by MockitoExtension.
        String result = YearConversions.toString(year, mockConverter);
        assertEquals("1999", result);
        // Verify that no methods on the mock were invoked.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_UncastableTemporal_ThrowsClassCastException() {
        // IsoEra implements Temporal but is not a Year.
        IsoEra era = IsoEra.CE;
        assertThrows(ClassCastException.class, () -> YearConversions.toString(era, mockConverter));
    }
}
