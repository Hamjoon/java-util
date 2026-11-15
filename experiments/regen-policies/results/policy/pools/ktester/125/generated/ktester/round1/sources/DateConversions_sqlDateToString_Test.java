package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.sql.Date;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_sqlDateToString_Test {

    private Converter converterMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid interaction between tests
        converterMock = mock(Converter.class);
        converterMock = mock(Converter.class);
    }

    /**
     * Cases that expect a normal string result (SimpleConverter behaviour).
     */
    static Stream<Arguments> normalStringProvider() {
        return Stream.of(// java.sql.Date → expected string from java.util.Date
        Arguments.of(new Date(1633036800000L), "new java.util.Date(1633036800000L).toString()"), // java.util.Date (passed as Object) → same handling
        Arguments.of(new java.util.Date(1633036800000L), "new java.util.Date(1633036800000L).toString()"), // negative epoch (pre‑1970)
        Arguments.of(new Date(-86400000L), "new java.util.Date(-86400000L).toString()"));
    }

    /**
     * Cases that use a custom converter which always returns a fixed string.
     */
    static Stream<Arguments> fixedStringProvider() {
        return Stream.of(Arguments.of(new Date(0L), "fixed"), Arguments.of(new Timestamp(1633036800000L), "fixed"));
    }

    /**
     * Cases that verify exception propagation from the delegated {@code toString}.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Converter throws IllegalStateException
        Arguments.of(new Date(1633036800000L), new IllegalStateException("converter failure")), // Converter throws NullPointerException
        Arguments.of(new Date(1633036800000L), new NullPointerException("converter NPE")));
    }

    /**
     * Verify that {@code sqlDateToString} delegates to {@code DateConversions.toString}
     * with a {@link java.util.Date} that has the same epoch milliseconds as the supplied
     * {@link java.sql.Date}, and that the returned value from the static method is
     * propagated unchanged.
     */
    @Test
    void sqlDateToString_delegatesToToString_andReturnsResult() {
        // GIVEN
        // arbitrary timestamp
        long epochMillis = 1_617_283_200_000L;
        // java.sql.Date instance
        Date sqlDate = new Date(epochMillis);
        // Mock the static method DateConversions.toString(...)
        try (MockedStatic<DateConversions> mocked = mockStatic(DateConversions.class)) {
            // Stub the static method to return a known value
            mocked.when(() -> DateConversions.toString(any(), any())).thenReturn("mocked-string");
            // WHEN
            String result = DateConversions.sqlDateToString(sqlDate, converterMock);
            // THEN
            assertEquals("mocked-string", result, "The method should return the value from DateConversions.toString");
            // Verify that the static method was called exactly once with the expected arguments
            mocked.verify(() -> DateConversions.toString(argThat(arg -> {
                // The argument must be a java.util.Date with the same time as the sqlDate
                return arg instanceof java.util.Date && ((java.util.Date) arg).getTime() == sqlDate.getTime();
            }), same(converterMock)), times(1));
        }
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException},
     * because the implementation attempts to call {@code getTime()} on the casted {@code Date}.
     */
    @Test
    void sqlDateToString_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.sqlDateToString(nullSource, converterMock), "Calling sqlDateToString with a null source should throw NullPointerException");
    }

    /**
     * Verify that passing an object that is not a {@link java.util.Date} (or subclass) results
     * in a {@link ClassCastException}. The method casts the argument to {@code Date} without checks.
     */
    @Test
    void sqlDateToString_invalidType_throwsClassCastException() {
        // GIVEN
        Object notADate = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.sqlDateToString(notADate, converterMock), "Calling sqlDateToString with a non‑Date object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] sqlDateToString with normal source {0}")
    @MethodSource("normalStringProvider")
    void sqlDateToString_returnsDateString(Object source, String expectedDescription) {
        // Compute the exact string that a plain java.util.Date would produce.
        java.util.Date expectedDate = new java.util.Date(((java.util.Date) source).getTime());
        String expectedString = expectedDate.toString();
        try (MockedStatic<DateConversions> mocked = mockStatic(DateConversions.class)) {
            // Stub the static delegation to return the expected string.
            mocked.when(() -> DateConversions.toString(any(), any())).thenReturn(expectedString);
            String result = DateConversions.sqlDateToString(source, converterMock);
            assertEquals(expectedString, result, "sqlDateToString must return the value produced by DateConversions.toString");
            // Verify that the delegated call received a java.util.Date with the same epoch.
            mocked.verify(() -> DateConversions.toString(argThat(arg -> arg instanceof java.util.Date && ((java.util.Date) arg).getTime() == ((java.util.Date) source).getTime()), same(converterMock)), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] sqlDateToString with FixedStringConverter – source {0}")
    @MethodSource("fixedStringProvider")
    void sqlDateToString_customConverter_returnsFixedString(Object source, String fixed) {
        try (MockedStatic<DateConversions> mocked = mockStatic(DateConversions.class)) {
            // The static method should simply return the fixed value supplied by the converter.
            mocked.when(() -> DateConversions.toString(any(), any())).thenReturn(fixed);
            String result = DateConversions.sqlDateToString(source, converterMock);
            assertEquals(fixed, result, "The method must return the string supplied by the custom converter");
        }
    }

    @ParameterizedTest(name = "[{index}] sqlDateToString propagates exception {1}")
    @MethodSource("exceptionProvider")
    void sqlDateToString_converterExceptionPropagates(Object source, RuntimeException toThrow) {
        try (MockedStatic<DateConversions> mocked = mockStatic(DateConversions.class)) {
            mocked.when(() -> DateConversions.toString(any(), any())).thenThrow(toThrow);
            RuntimeException ex = assertThrows(toThrow.getClass(), () -> DateConversions.sqlDateToString(source, converterMock), "Exception from DateConversions.toString must propagate unchanged");
            assertSame(toThrow, ex, "The same exception instance should be propagated");
        }
    }

    @Test
    void sqlDateToString_invalidSource_throwsClassCastException() {
        Object notADate = "2023-01-01";
        assertThrows(ClassCastException.class, () -> DateConversions.sqlDateToString(notADate, converterMock), "Passing a non‑Date object must cause a ClassCastException");
    }

    @Test
    void sqlDateToString_nullConverter_throwsNullPointerException() {
        Date sqlDate = new Date(1633036800000L);
        try (MockedStatic<DateConversions> mocked = mockStatic(DateConversions.class)) {
            // Simulate the real static method throwing NPE when converter is null
            mocked.when(() -> DateConversions.toString(any(), isNull())).thenThrow(new NullPointerException("converter is null"));
            assertThrows(NullPointerException.class, () -> DateConversions.sqlDateToString(sqlDate, null), "A null converter should cause a NullPointerException");
        }
    }

    @Test
    void sqlDateToString_getTimeThrowsException_propagates() {
        // Anonymous subclass that throws from getTime()
        Date badDate = new Date(0L) {

            @Override
            public long getTime() {
                throw new UnsupportedOperationException("getTime unsupported");
            }
        };
        // No need to mock the static method – the exception occurs before delegation.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> DateConversions.sqlDateToString(badDate, converterMock), "Exception from getTime() must propagate");
        assertEquals("getTime unsupported", ex.getMessage());
    }
}
