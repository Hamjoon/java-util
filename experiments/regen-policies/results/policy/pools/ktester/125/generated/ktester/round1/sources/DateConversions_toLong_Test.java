package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLong_Test {

    /**
     * Mocked Converter – the method under test must not call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Auto‑closeable for Mockito annotations (helps with JUnit 5 lifecycle).
     */
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that the mock was never used and release resources.
        verifyNoInteractions(mockConverter);
        mocks.close();
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // custom Converter that throws if its convert() method is called – should never be invoked.
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object o, Class<?> c) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // Date subclass that overrides getTime() – used in error tests, not here.
        return Stream.of(arguments("java.util.Date at epoch (0 ms)", new Date(0L), null, 0L), arguments("java.util.Date with specific timestamp", new Date(1_622_548_800_123L), // empty implementation, unused
        new Converter() {
        }, 1_622_548_800_123L), arguments("java.sql.Timestamp subclass of Date", Timestamp.valueOf("2023-01-01 12:34:56.789"), null, 1_672_578_896_789L), arguments("java.util.Date with negative timestamp (pre‑epoch)", new Date(-86_400_000L), null, -86_400_000L), arguments("java.sql.Date subclass (midnight 1970‑01‑02)", Date.valueOf("1970-01-02"), null, 86_400_000L), arguments("java.util.Date with Long.MAX_VALUE", new Date(Long.MAX_VALUE), null, Long.MAX_VALUE), arguments("custom Converter not used – should still return date millis", new Date(12_345L), explodingConverter, 12_345L));
    }

    private static Stream<Arguments> errorConversionProvider() {
        // Date subclass that throws IllegalStateException from getTime()
        Date illegalStateDate = new Date(0L) {

            @Override
            public long getTime() {
                throw new IllegalStateException("forced");
            }
        };
        // Date subclass that throws RuntimeException from getTime()
        Date runtimeExceptionDate = new Date(0L) {

            @Override
            public long getTime() {
                throw new RuntimeException("forced runtime");
            }
        };
        // Date subclass that throws AssertionError from getTime()
        Date assertionErrorDate = new Date(0L) {

            @Override
            public long getTime() {
                throw new AssertionError("forced error");
            }
        };
        return Stream.of(arguments("null source object", null, null, NullPointerException.class), arguments("incompatible type – String", "2023-01-01", null, ClassCastException.class), arguments("incompatible type – java.time.LocalDate", java.time.LocalDate.now(), null, ClassCastException.class), arguments("Date subclass that overrides getTime() with IllegalStateException", illegalStateDate, null, IllegalStateException.class), arguments("Date subclass that overrides getTime() with RuntimeException", runtimeExceptionDate, null, RuntimeException.class), arguments("Date subclass that overrides getTime() with AssertionError", assertionErrorDate, null, AssertionError.class));
    }

    @Test
    void toLong_returnsDateGetTime() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long expectedMillis = 1_640_995_200_000L;
        Date date = new Date(expectedMillis);
        // WHEN
        long actual = DateConversions.toLong(date, mockConverter);
        // THEN
        assertEquals(expectedMillis, actual, "toLong should return the same epoch millis as Date#getTime()");
    }

    @Test
    void toLong_nullArgument_throwsNullPointerException() {
        // GIVEN
        Date nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toLong(nullDate, mockConverter), "Casting null to Date should result in NullPointerException");
    }

    @Test
    void toLong_nonDateArgument_throwsClassCastException() {
        // GIVEN
        String notADate = "2022-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toLong(notADate, mockConverter), "Passing a non‑Date object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toLong_successfulConversion(String description, Object from, Converter converter, long expected) {
        long actual = DateConversions.toLong(from, converter);
        assertEquals(expected, actual, () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorConversionProvider")
    void toLong_errorConversion(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toLong(from, converter), () -> "Expected exception for case: " + description);
    }
}
