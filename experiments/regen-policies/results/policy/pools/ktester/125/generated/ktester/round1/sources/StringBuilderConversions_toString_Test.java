package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringBuilderConversions_toString_Test {

    @Mock
    private Converter // Not used by the method but required for the signature
    mockConverter;

    /**
     * Simple POJO whose {@code toString()} returns a constant value.
     */
    private static final class Dummy {

        @Override
        public String toString() {
            return "dummy";
        }
    }

    /**
     * Provides a mixture of inputs, converters and expected outcomes.
     * The {@code expected} element can be:
     * <ul>
     *   <li>a {@link String} – the exact value that should be returned,</li>
     *   <li>{@code null} – the method is expected to return {@code null},</li>
     *   <li>a {@link Class}&lt;? extends {@link Throwable}&gt; – the test expects that
     *       exception to be thrown,</li>
     *   <li>the special token {@code "nonNullString"} – any non‑null string is
     *       acceptable (used for array default {@code toString()}).</li>
     * </ul>
     */
    Stream<Arguments> provideToStringCases() {
        return Stream.of(// 1. plain String, converter null
        Arguments.of("test", null, "test"), // 2. StringBuilder instance, converter null
        Arguments.of(new StringBuilder("builder"), null, "builder"), // 3. Integer, converter null
        Arguments.of(Integer.valueOf(123), null, "123"), // 4. null source – should throw NPE
        Arguments.of(null, null, NullPointerException.class), // 5. Dummy object with a non‑null Converter (mocked)
        Arguments.of(new Dummy(), mockConverter, "dummy"), // 6. Object whose toString() returns null
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, null), // 7. Object whose toString() throws IllegalStateException
        Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }, null, IllegalStateException.class), // 8. Primitive array – default toString() yields a non‑null string
        Arguments.of(new int[] { 1, 2, 3 }, null, "nonNullString"), // 9. Boolean.TRUE – should return "true"
        Arguments.of(Boolean.TRUE, null, "true"), // 10. toString() throws NullPointerException (propagation)
        Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new NullPointerException("null inside");
            }
        }, null, NullPointerException.class), // 11. toString() throws AssertionError (propagation of Error)
        Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new AssertionError("boom");
            }
        }, null, AssertionError.class));
    }

    @Test
    void toString_returnsObjectToString() {
        // GIVEN
        Object source = new Object() {

            @Override
            public String toString() {
                return "custom-toString";
            }
        };
        // WHEN
        String result = StringBuilderConversions.toString(source, mockConverter);
        // THEN
        assertEquals("custom-toString", result, "The method should return the result of source.toString()");
    }

    @Test
    void toString_withNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBuilderConversions.toString(source, mockConverter), "Calling toString with a null source should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("provideToStringCases")
    void toString_variousScenarios(Object from, Converter converter, Object expected) {
        // If the expected value is an exception class, assert that it propagates.
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> StringBuilderConversions.toString(from, converter), () -> "Expected exception " + ((Class<?>) expected).getSimpleName() + " to be thrown");
            return;
        }
        // For the special marker indicating any non‑null string (used for arrays).
        if ("nonNullString".equals(expected)) {
            String result = StringBuilderConversions.toString(from, converter);
            assertNotNull(result, "Result should not be null for array default toString()");
            // The default array toString() format is something like "[I@1a2b3c"
            assertTrue(result.matches("^\\[.+@.+$"), "Result should look like an array default toString(): " + result);
            return;
        }
        // Normal return value (including the possibility of a null result).
        String result = StringBuilderConversions.toString(from, converter);
        assertEquals(expected, result, () -> "Unexpected result for source: " + (from == null ? "null" : from.getClass().getSimpleName()));
    }
}
