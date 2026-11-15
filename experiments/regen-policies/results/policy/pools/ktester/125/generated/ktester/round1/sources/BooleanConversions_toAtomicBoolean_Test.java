package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicBoolean_Test {

    @Mock
    private Converter // the Converter is not used by the method, but we provide a mock
    mockConverter;

    // for manual mock cleanup (optional)
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled automatically by @ExtendWith, but kept for clarity)
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset interactions and close the mock context
        reset(mockConverter);
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Provides a stream of successful conversion scenarios.
     * The {@code converter} argument may be {@code null} or a mock – the method must ignore it.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// from                converter            expected boolean value
        Arguments.of(Boolean.TRUE, null, true), Arguments.of(Boolean.FALSE, null, false), Arguments.of(new Boolean(true), null, true), Arguments.of(new Boolean(false), null, false), Arguments.of(Boolean.TRUE, mock(Converter.class), true), Arguments.of(Boolean.FALSE, mock(Converter.class), false));
    }

    @Test
    @DisplayName("toAtomicBoolean converts Boolean.TRUE to AtomicBoolean true")
    void testTrueConversion() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "AtomicBoolean should hold true");
        // Verify that the converter was never used
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean converts Boolean.FALSE to AtomicBoolean false")
    void testFalseConversion() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.get(), "AtomicBoolean should hold false");
        // Verify that the converter was never used
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean throws NullPointerException when source is null")
    void testNullSourceThrowsException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicBoolean(source, mockConverter), "Passing null should result in NullPointerException");
        // Verify that the converter was never used
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean throws ClassCastException when source is a String")
    void testClassCastExceptionWithString() {
        // given
        Object source = "true";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicBoolean(source, mockConverter), "Casting a String to Boolean should throw ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean works with an explicit Boolean instance (new Boolean(true))")
    void testExplicitBooleanTrue() {
        // given
        // deprecated constructor, still valid for the test
        Object source = new Boolean(true);
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get());
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean works with an explicit Boolean instance (new Boolean(false))")
    void testExplicitBooleanFalse() {
        // given
        Object source = new Boolean(false);
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertFalse(result.get());
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean ignores a non‑null mock Converter (true case)")
    void testConversionWithNonNullMockConverterTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // mockConverter is already a Mockito mock (non‑null)
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get());
        // Ensure the method never touched the converter
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean ignores a non‑null mock Converter (false case)")
    void testConversionWithNonNullMockConverterFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertFalse(result.get());
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toAtomicBoolean works even when the Converter would throw if used")
    void testConversionWithThrowingConverterTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // Create a mock that throws on any interaction – the method under test must never call it.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, throwingConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get());
        // No interaction should have happened; verify that the mock was never called.
        verifyNoInteractions(throwingConverter);
    }

    @ParameterizedTest(name = "from={0}, converter={1} => expected={2}")
    @MethodSource("provideSuccessfulConversions")
    @DisplayName("Parameterized happy‑path tests for toAtomicBoolean")
    void testParameterizedSuccessfulConversions(Object from, Converter converter, boolean expected) {
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(from, converter);
        // then
        assertNotNull(result);
        assertEquals(expected, result.get());
        // The method never uses the converter, so there should be no interactions.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
