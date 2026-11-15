package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLongZero_Test {

    /**
     * Converter instance required by the method signature.
     */
    private Converter converter;

    /**
     * Simple holder for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String description) {
            this.from = from;
            this.converter = converter;
            this.description = description;
        }
    }

    /**
     * Minimal mock implementation of {@link Converter} used in JSON‑driven cases.
     * It does not need any behaviour because {@code toLongZero} never calls it.
     */
    private static class MockConverter extends Converter {

        MockConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Converter with default options – the method under test does not
        // actually consult the converter, but we need a valid instance to pass.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    /**
     * Parameter provider for the JSON‑driven test cases.
     * Each element supplies a {@code from} object and a {@code Converter} (which may be null).
     */
    private static Stream<ArgumentsHolder> toLongZeroProvider() {
        return Stream.of(// case 1 – both arguments null
        new ArgumentsHolder(null, null, "Both arguments null"), // case 2 – from = \"123\", converter = mock
        new ArgumentsHolder("123", new MockConverter(), "String source with mock converter"), // case 3 – from = new Object(), converter = null
        new ArgumentsHolder(new Object(), null, "Object source with null converter"));
    }

    @Test
    void toLongZero_returnsZeroWhenSourceIsNull() {
        Long result = NumberConversions.toLongZero(null, converter);
        assertEquals(Long.valueOf(0L), result, "toLongZero should return 0L when the source object is null");
        assertSame(CommonValues.LONG_ZERO, result, "The returned instance should be the shared LONG_ZERO constant");
    }

    @Test
    void toLongZero_returnsZeroForNonNullObject() {
        // Try a few different source types – the implementation ignores them.
        Object[] sources = { 123, "456", 78.9, new java.util.Date() };
        for (Object src : sources) {
            Long result = NumberConversions.toLongZero(src, converter);
            assertEquals(Long.valueOf(0L), result, "toLongZero should return 0L for source: " + src);
            assertSame(CommonValues.LONG_ZERO, result, "The returned instance should be the shared LONG_ZERO constant");
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("toLongZeroProvider")
    void toLongZero_returnsConstantLongZero(ArgumentsHolder holder) {
        // Act
        Long result = NumberConversions.toLongZero(holder.from, holder.converter);
        // Assert – the method must return the shared constant instance
        assertEquals(Long.valueOf(0L), result, "toLongZero should return a Long equal to 0L for case: " + holder.description);
        assertSame(CommonValues.LONG_ZERO, result, "toLongZero must return the exact CommonValues.LONG_ZERO instance for case: " + holder.description);
    }

    /**
     * Additional explicit test verifying behavior with a variety of non‑null source objects.
     * This mirrors the original test but is kept for completeness.
     */
    @Test
    void toLongZero_returnsZeroForVariousNonNullSources() {
        Object[] sources = { 123, "456", 78.9, new java.util.Date(), new java.math.BigInteger("999") };
        for (Object src : sources) {
            Long result = NumberConversions.toLongZero(src, converter);
            assertEquals(Long.valueOf(0L), result, "toLongZero should return 0L for source: " + src);
            assertSame(CommonValues.LONG_ZERO, result, "The returned instance should be the shared LONG_ZERO constant for source: " + src);
        }
    }
}
