package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toBigDecimal_Test {

    /**
     * The constant used by {@link DateConversions#toBigDecimal(Object, Converter)} to scale the epoch.
     */
    private static BigDecimal GRAND;

    /**
     * Mocked {@link Converter} – the implementation does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Helper class to pass arguments (allows optional custom Converter)
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        // may be null → use mock
        final Converter converter;

        ArgumentsHolder(Object from) {
            this(from, null);
        }

        ArgumentsHolder(Object from, Converter converter) {
            this.from = from;
            this.converter = converter;
        }

        @Override
        public String toString() {
            return from.getClass().getSimpleName() + "@" + System.identityHashCode(from);
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Retrieve the package‑private static constant from BigDecimalConversions via reflection.
        Field grandField = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions").getDeclaredField("GRAND");
        grandField.setAccessible(true);
        GRAND = (BigDecimal) grandField.get(null);
        assertNotNull(GRAND, "BigDecimalConversions.GRAND must be available for the tests");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Ensure no unexpected interactions have occurred before each test.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not used (the method under test does not call any Converter API).
        verifyNoInteractions(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to clean up; just clear the static reference.
        GRAND = null;
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterized)
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// epoch start
        new ArgumentsHolder(new Date(0L)), // positive timestamp (123 456 789 ms)
        new ArgumentsHolder(new Date(123_456_789L)), // negative timestamp
        new ArgumentsHolder(new Date(-987_654_321L)), // java.sql.Timestamp (subclass of Date)
        new ArgumentsHolder(new Timestamp(1_500L)), // java.sql.Date (subclass of Date) – exactly one day
        new ArgumentsHolder(new java.sql.Date(86_400_000L)), // Long.MAX_VALUE
        new ArgumentsHolder(new Date(Long.MAX_VALUE)), // Long.MIN_VALUE
        new ArgumentsHolder(new Date(Long.MIN_VALUE)), // larger than one second
        new ArgumentsHolder(new Date(123_456_789_123L)), // anonymous subclass overriding getTime()
        new ArgumentsHolder(new Date() {

            @Override
            public long getTime() {
                return 5_000L;
            }
        }), // non‑null Converter (the method should ignore it)
        new ArgumentsHolder(new Date(0L), new Converter() {
        }));
    }

    @Test
    void toBigDecimal_givenDate_returnsScaledBigDecimal() {
        // GIVEN
        // a deterministic timestamp
        long epochMillis = 1_632_456_789_012L;
        Date date = new Date(epochMillis);
        // Expected value calculated with the same algorithm the production code uses.
        BigDecimal expected = new BigDecimal(epochMillis).divide(GRAND, 9, RoundingMode.DOWN);
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(date, mockConverter);
        // THEN
        assertEquals(expected, actual, "The BigDecimal representation of the date should be correctly scaled");
    }

    @Test
    void toBigDecimal_givenNonDate_throwsClassCastException() {
        // GIVEN
        Object notADate = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toBigDecimal(notADate, mockConverter), "Passing a non‑Date object must result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] toBigDecimal conversion for {0}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_givenValidDate_returnsScaledBigDecimal(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter converter = holder.converter != null ? holder.converter : mockConverter;
        // Expected value calculated with the same algorithm the production code uses.
        long epochMillis = ((Date) from).getTime();
        BigDecimal expected = new BigDecimal(epochMillis).divide(GRAND, 9, RoundingMode.DOWN);
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(from, converter);
        // THEN
        assertEquals(expected, actual, () -> "Scaled BigDecimal does not match for input: " + from);
    }

    /* --------------------------------------------------------------
     *  Exception test cases
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_givenNull_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigDecimal(from, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toBigDecimal_givenDateSubclassThrowingException_propagatesException() {
        // GIVEN: anonymous Date subclass whose getTime() throws
        Date badDate = new Date() {

            @Override
            public long getTime() {
                throw new IllegalStateException("bad time");
            }
        };
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DateConversions.toBigDecimal(badDate, mockConverter), "Exception from getTime() should be propagated");
        assertEquals("bad time", ex.getMessage());
    }
}
