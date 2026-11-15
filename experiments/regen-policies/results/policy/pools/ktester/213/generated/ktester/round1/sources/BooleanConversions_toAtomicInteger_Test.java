package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicInteger_Test {

    /**
     * Mocked Converter – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Initializes Mockito annotations before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The @ExtendWith(MockitoExtension.class) already opens mocks, but we keep this
        // call for explicitness and to match the original framework.
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Provides normal Boolean inputs together with the expected integer value
     * stored inside the resulting {@link AtomicInteger}.
     */
    private static Stream<Arguments> normalBooleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1, "AtomicInteger should contain 1 for true"), Arguments.of(Boolean.FALSE, 0, "AtomicInteger should contain 0 for false"));
    }

    /**
     * Supplies inputs that should cause a {@link NullPointerException}
     * because the method attempts to cast/unbox a null {@link Boolean}.
     */
    private static Stream<Arguments> nullSourceProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null should result in NullPointerException due to unboxing"));
    }

    /**
     * Supplies inputs that are not {@link Boolean} instances, which should
     * cause a {@link ClassCastException} when the method attempts the cast.
     */
    private static Stream<Arguments> nonBooleanProvider() {
        return Stream.of(Arguments.of("true", ClassCastException.class, "String cannot be cast to Boolean – expect ClassCastException"), Arguments.of(1, ClassCastException.class, "Integer cannot be cast to Boolean – expect ClassCastException"), Arguments.of(new Object(), ClassCastException.class, "Arbitrary Object cannot be cast to Boolean – expect ClassCastException"));
    }

    @Test
    void toAtomicInteger_whenTrue_returnsAtomicIntegerWithOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.get(), "AtomicInteger should contain 1 for true");
    }

    @Test
    void toAtomicInteger_whenFalse_returnsAtomicIntegerWithZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.get(), "AtomicInteger should contain 0 for false");
    }

    @Test
    void toAtomicInteger_withNullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicInteger(source, converter), "Passing null should result in NullPointerException due to unboxing");
    }

    @Test
    void toAtomicInteger_withNonBooleanSource_throwsClassCastException() {
        // given
        Object source = "not a Boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicInteger(source, converter), "Passing a non‑Boolean should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("normalBooleanProvider")
    void toAtomicInteger_normalValues_returnsExpectedAtomicInteger(Boolean from, int expected, String message) {
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), message);
    }

    @ParameterizedTest(name = "[{index}] from=null → throws {1}")
    @MethodSource("nullSourceProvider")
    void toAtomicInteger_nullSource_throwsException(Object from, Class<? extends Throwable> exceptionClass, String message) {
        // when / then
        assertThrows(exceptionClass, () -> BooleanConversions.toAtomicInteger(from, converter), message);
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("nonBooleanProvider")
    void toAtomicInteger_nonBooleanSource_throwsException(Object from, Class<? extends Throwable> exceptionClass, String message) {
        // when / then
        assertThrows(exceptionClass, () -> BooleanConversions.toAtomicInteger(from, converter), message);
    }
}
