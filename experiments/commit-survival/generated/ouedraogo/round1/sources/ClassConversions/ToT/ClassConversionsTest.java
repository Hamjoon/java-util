package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassConversions}.
 *
 * The tests are written from the perspective of three independent “experts”.
 * Each expert contributed one test case for the only public method
 * {@code ClassConversions.toString(Object, Converter)}.  After each contribution
 * the group reviewed the result and added additional edge‑case checks to obtain
 * full coverage.
 *
 * The final suite therefore contains:
 *   1. A typical use‑case – converting a real {@code Class} object.
 *   2. An edge case – the {@code from} argument is {@code null}.
 *   3. An edge case – the {@code from} argument is not a {@code Class}.
 *   4. A sanity check – the {@code converter} argument can be {@code null}
 *      because the implementation never touches it.
 *
 * No external libraries, mocking frameworks or subclasses are used; only the
 * public API of {@link Converter} (which is required only for construction) is
 * employed.  Passing {@code null} as the {@code ConverterOptions} is acceptable
 * because {@code ClassConversions.toString} does not reference the converter.
 */
public class ClassConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The implementation of toString does not use the Converter at all,
        // therefore we can safely pass null (or a real options object if
        // available in the classpath).  This satisfies the API contract without
        // pulling in additional library code.
        dummyConverter = new Converter(null);
    }

    /**
     * Expert 1 – Typical usage.
     * Verify that a valid {@code Class} argument returns its fully‑qualified name.
     */
    @Test
    public void testToStringWithValidClass() {
        // Arrange
        Class<?> input = java.util.ArrayList.class;

        // Act
        String result = ClassConversions.toString(input, dummyConverter);

        // Assert
        assertEquals("java.util.ArrayList", result);
    }

    /**
     * Expert 2 – Edge case: {@code from} is {@code null}.
     * The cast succeeds (producing {@code null}) but the subsequent call to
     * {@code getName()} must throw a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFrom() {
        // Act
        ClassConversions.toString(null, dummyConverter);
        // Exception is expected; no further assertions.
    }

    /**
     * Expert 3 – Edge case: {@code from} is not a {@code Class} instance.
     * The explicit cast to {@code Class<?>} should fail with a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassFrom() {
        // Arrange
        Object notAClass = new Object();

        // Act
        ClassConversions.toString(notAClass, dummyConverter);
        // Exception is expected.
    }

    /**
     * Additional sanity check: the {@code converter} argument may be {@code null}.
     * Since the method never accesses the converter, the call should succeed
     * and produce the same result as when a non‑null converter is supplied.
     */
    @Test
    public void testToStringWithNullConverter() {
        // Arrange
        Class<?> input = String.class;

        // Act
        String result = ClassConversions.toString(input, null);

        // Assert
        assertEquals("java.lang.String", result);
    }
}
