package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicLongConversions_toAtomicLong_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private AutoCloseable mocks;

    /**
     * Simple stub implementation of {@link Converter}.  The real converter is
     * irrelevant for the method under test – it is never called.
     */
    private static class DummyConverter implements Converter {
        // No implementation required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that the method under test never touches the converter.
        verifyNoInteractions(mockConverter);
        // Close the Mockito session.
        mocks.close();
    }

    /**
     * Provides a comprehensive set of arguments covering normal, boundary,
     * error and “converter‑ignored” scenarios.
     */
    static Stream<Arguments> toAtomicLongProvider() {
        return Stream.of(// Normal case – a copy with the same numeric value.
        Arguments.of("Normal case", new AtomicLong(42L), null, 42L, null), // Invalid source type – cast should fail.
        Arguments.of("Invalid type", "not an atomic long", null, null, ClassCastException.class), // Null source – cast succeeds (null) but {@code get()} triggers NPE.
        Arguments.of("Null input", null, null, null, NullPointerException.class), // Boundary values.
        Arguments.of("Maximum long value", new AtomicLong(Long.MAX_VALUE), null, Long.MAX_VALUE, null), Arguments.of("Minimum long value", new AtomicLong(Long.MIN_VALUE), null, Long.MIN_VALUE, null), // Zero value.
        Arguments.of("Zero value", new AtomicLong(0L), null, 0L, null), // Non‑null converter – should be ignored.
        Arguments.of("Non‑null converter", new AtomicLong(123L), new DummyConverter(), 123L, null), // Invalid type with a non‑null converter – still a ClassCastException.
        Arguments.of("Invalid type with non‑null converter", "not an atomic long", new DummyConverter(), null, ClassCastException.class));
    }

    @Test
    void toAtomicLong_returnsCopyWithSameValue() {
        // GIVEN
        AtomicLong original = new AtomicLong(12345L);
        // WHEN
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(original.get(), result.get(), "The value of the returned AtomicLong must match the original");
        assertNotSame(original, result, "The returned AtomicLong must be a different instance (a copy)");
    }

    @Test
    void toAtomicLong_nullSource_throwsClassCastException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toAtomicLong(nullSource, mockConverter), "Passing null should result in a ClassCastException because of the cast inside the method");
    }

    @Test
    void toAtomicLong_invalidSourceType_throwsClassCastException() {
        // GIVEN
        Object notAnAtomicLong = "I am not an AtomicLong";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toAtomicLong(notAnAtomicLong, mockConverter), "Passing an object that is not an AtomicLong should cause a ClassCastException");
    }

    /**
     * Parameterized test that executes each scenario defined in {@link #toAtomicLongProvider()}.
     *
     * @param description       Human‑readable description for the test case.
     * @param from              The {@code from} argument passed to the method.
     * @param suppliedConverter The converter to pass (may be {@code null}).
     * @param expectedValue     Expected long value of the resulting {@link AtomicLong}
     *                          (ignored when an exception is expected).
     * @param expectedException Expected exception class, or {@code null} if no exception.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("toAtomicLongProvider")
    void toAtomicLong_various(String description, Object from, Converter suppliedConverter, Long expectedValue, Class<? extends Throwable> expectedException) {
        // The method under test requires a non‑null Converter argument; when the test case
        // specifies null we supply the Mockito mock (which must stay untouched).
        Converter converterToUse = (suppliedConverter == null) ? mockConverter : suppliedConverter;
        if (expectedException != null) {
            // The call is expected to throw.
            assertThrows(expectedException, () -> AtomicLongConversions.toAtomicLong(from, converterToUse), description);
        } else {
            // Normal successful conversion.
            AtomicLong result = AtomicLongConversions.toAtomicLong(from, converterToUse);
            assertNotNull(result, description + " – result must not be null");
            assertEquals(expectedValue.longValue(), result.get(), description + " – returned value must match expected");
            // When the source is an AtomicLong we must ensure a *copy* was created.
            if (from instanceof AtomicLong) {
                assertNotSame(from, result, description + " – result must be a different instance from the source");
            }
        }
    }
}
