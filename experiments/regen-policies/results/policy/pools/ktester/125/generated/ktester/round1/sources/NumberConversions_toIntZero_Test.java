package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toIntZero_Test {

    @Mock
    private Converter // mock created by MockitoExtension
    converter;

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    /* --------------------------------------------------------------------- */
    private static final class DummyObject {

        private final boolean customObject;

        DummyObject(boolean customObject) {
            this.customObject = customObject;
        }

        @Override
        public String toString() {
            return "DummyObject{" + "customObject=" + customObject + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields, but we keep this method
        // for symmetry and future extensions.
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction leakage between tests
        reset(converter);
    }

    /**
     * Supplies a stream of arguments built from the JSON test cases.
     * Each argument consists of:
     *   - the {@code from} object (or {@code null})
     *   - a {@link Converter} instance (may be {@code null})
     *   - a description string (used as the display name)
     */
    static Stream<Arguments> toIntZeroCases() {
        // Helper lambda to create a Converter that throws on any interaction.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter must not be invoked");
        });
        return Stream.of(// 1. both arguments null
        Arguments.of(null, null, "Both arguments null"), // 2. Integer non‑zero, converter null
        Arguments.of(123, null, "Integer 123, converter null"), // 3. String representation of a number, converter null
        Arguments.of("456", null, "String \"456\", converter null"), // 4. Double value, converter null
        Arguments.of(3.14d, null, "Double 3.14, converter null"), // 5. Custom object (represented as a simple map here)
        Arguments.of(new DummyObject(true), null, "Custom object, converter null"), // 6. String with mock converter
        Arguments.of("any", mock(Converter.class), "String \"any\", mock converter"), // 7. null source with mock converter
        Arguments.of(null, mock(Converter.class), "null source, mock converter"), // 8. Boolean true, converter null
        Arguments.of(Boolean.TRUE, null, "Boolean true, converter null"), // 9. Boolean false with mock converter
        Arguments.of(Boolean.FALSE, mock(Converter.class), "Boolean false, mock converter"), // 10. BigDecimal instance
        Arguments.of(new BigDecimal("123.45"), null, "BigDecimal instance, converter null"), // 11. BigInteger instance with mock converter
        Arguments.of(new BigInteger("9876543210"), mock(Converter.class), "BigInteger instance, mock converter"), // 12. AtomicInteger instance
        Arguments.of(new AtomicInteger(5), null, "AtomicInteger(5), converter null"), // 13. AtomicLong instance with mock converter
        Arguments.of(new AtomicLong(10L), mock(Converter.class), "AtomicLong(10), mock converter"), // 14. java.util.Date instance
        Arguments.of(new Date(), null, "java.util.Date instance, converter null"), // 15. int array
        Arguments.of(new int[] { 1, 2, 3 }, null, "int[] {1,2,3}, converter null"), // 16. DayOfWeek enum constant
        Arguments.of(DayOfWeek.MONDAY, null, "DayOfWeek.MONDAY, converter null"), // 17. String \"0\" with mock converter
        Arguments.of("0", mock(Converter.class), "String \"0\", mock converter"), // 18. String \"any\" with throwing converter
        Arguments.of("any", throwingConverter, "String \"any\", throwing converter"));
    }

    @Test
    void toIntZero_returnsCommonZero_forNullInput() {
        Integer result = NumberConversions.toIntZero(null, converter);
        assertEquals(Integer.valueOf(0), result, "Expected zero for null source");
        // The method should not use the converter at all
        verifyNoInteractions(converter);
    }

    @Test
    void toIntZero_returnsCommonZero_forNonNullInput() {
        // Any arbitrary object can be supplied; the implementation always returns zero.
        Object anyObject = "some string";
        Integer result = NumberConversions.toIntZero(anyObject, converter);
        assertEquals(Integer.valueOf(0), result, "Expected zero for any non‑null source");
        // Ensure the converter is not touched
        verifyNoInteractions(converter);
    }

    @Test
    void toIntZero_returnsSameInstanceAsCommonValues() {
        Integer result = NumberConversions.toIntZero(12345, converter);
        // CommonValues.INTEGER_ZERO is the canonical zero instance used by the library.
        assertSame(CommonValues.INTEGER_ZERO, result, "Result should be the exact instance defined in CommonValues");
    }

    /**
     * Executes the parameterised test matrix.  For each supplied case we verify that:
     * <ul>
     *   <li>The method returns {@code Integer.ZERO} (the constant from {@link CommonValues}).</li>
     *   <li>The returned instance is exactly {@link CommonValues#INTEGER_ZERO}.</li>
     *   <li>No interaction with the supplied {@link Converter} occurs, even when the mock is
     *       configured to throw on any call.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("toIntZeroCases")
    void toIntZero_alwaysReturnsZero(Object from, Converter converter, String description) {
        // Act
        Integer result = NumberConversions.toIntZero(from, converter);
        // Assert – value and canonical instance
        assertEquals(Integer.valueOf(0), result, "Method must always return zero");
        assertSame(CommonValues.INTEGER_ZERO, result, "Returned instance must be the shared constant from CommonValues");
        // Assert – converter must remain untouched (if present)
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
