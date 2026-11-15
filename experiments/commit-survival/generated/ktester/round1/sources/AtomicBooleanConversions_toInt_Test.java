package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toInt_Test {

    /**
     * Mocked {@link Converter} – not used by the method but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * The AtomicBoolean instance used in the current test.
     */
    private AtomicBoolean atomicBoolean;

    /**
     * Placeholder class used only to signal "use the @Mock field". The actual
     * instance is replaced inside the test method.
     */
    private static final class MockConverterPlaceholder extends Converter {
        // No implementation needed – the test method substitutes the real mock.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid unwanted interactions.
        Mockito.reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps GC in larger test suites.
        atomicBoolean = null;
    }

    /* --------------------------------------------------------------
     *  Parameterised test covering true/false with both null and
     *  non‑null converters (including the mock defined at the class level).
     *  This demonstrates the same behaviour across all combinations.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> trueFalseWithConverters() {
        return Stream.of(// true value, null converter
        Arguments.of(new AtomicBoolean(true), null, CommonValues.INTEGER_ONE), // false value, null converter
        Arguments.of(new AtomicBoolean(false), null, CommonValues.INTEGER_ZERO), // true value, mock converter (injected by JUnit)
        Arguments.of(new AtomicBoolean(true), new MockConverterPlaceholder(), CommonValues.INTEGER_ONE), // false value, mock converter
        Arguments.of(new AtomicBoolean(false), new MockConverterPlaceholder(), CommonValues.INTEGER_ZERO));
    }

    @Test
    void toInt_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        atomicBoolean = new AtomicBoolean(true);
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ONE, result, "toInt should return Integer 1 when the AtomicBoolean value is true");
    }

    @Test
    void toInt_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        atomicBoolean = new AtomicBoolean(false);
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ZERO, result, "toInt should return Integer 0 when the AtomicBoolean value is false");
    }

    @Test
    void toInt_throwsClassCastExceptionWhenFromIsNotAtomicBoolean() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toInt(notAtomic, mockConverter), "toInt should throw ClassCastException when the supplied object is not an AtomicBoolean");
    }

    /* --------------------------------------------------------------
     *  Null source handling – the cast succeeds (null) but b.get()
     *  triggers a NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toInt_throwsNullPointerExceptionWhenFromIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toInt(nullSource, mockConverter), "toInt should throw NullPointerException when the source object is null");
    }

    /* --------------------------------------------------------------
     *  Reference‑equality tests – the method must return the cached
     *  Integer objects from CommonValues.
     * -------------------------------------------------------------- */
    @Test
    void toInt_returnsCachedIntegerOneInstance() {
        // given
        atomicBoolean = new AtomicBoolean(true);
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, mockConverter);
        // then
        assertSame(CommonValues.INTEGER_ONE, result, "Result must be the exact cached Integer instance for true (CommonValues.INTEGER_ONE)");
    }

    @Test
    void toInt_returnsCachedIntegerZeroInstance() {
        // given
        atomicBoolean = new AtomicBoolean(false);
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, mockConverter);
        // then
        assertSame(CommonValues.INTEGER_ZERO, result, "Result must be the exact cached Integer instance for false (CommonValues.INTEGER_ZERO)");
    }

    /* --------------------------------------------------------------
     *  Converter argument is ignored – both a normal mock and a
     *  deliberately mis‑behaving implementation must not affect the
     *  outcome.
     * -------------------------------------------------------------- */
    @Test
    void toInt_ignoresNonNullConverterInstance() {
        // given
        atomicBoolean = new AtomicBoolean(true);
        // a real Converter instance that does nothing special
        Converter realConverter = new Converter() {

            // The actual Converter API is unknown; we provide a stub that does nothing.
            @Override
            public <T> T convert(Object src, Class<T> target) {
                // Should never be called by toInt.
                throw new AssertionError("Converter should not be invoked");
            }
        };
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, realConverter);
        // then
        assertEquals(CommonValues.INTEGER_ONE, result, "A non‑null Converter must not change the conversion result");
    }

    @Test
    void toInt_ignoresConverterThatThrowsIfInvoked() {
        // given
        atomicBoolean = new AtomicBoolean(false);
        // Mockito mock that throws on any method call – proves that toInt never touches it.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("converter should not be invoked");
        });
        // when
        Integer result = AtomicBooleanConversions.toInt(atomicBoolean, throwingConverter);
        // then
        assertEquals(CommonValues.INTEGER_ZERO, result, "Even a Converter that throws must be ignored by toInt");
        // Verify that no interaction happened – confirms the method truly ignored the argument.
        verifyNoInteractions(throwingConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("trueFalseWithConverters")
    void toInt_parameterised_trueFalse(AtomicBoolean from, Object converterPlaceholder, Integer expected) {
        // Resolve the converter to use:
        Converter converterToUse = (converterPlaceholder instanceof MockConverterPlaceholder) ? // the @Mock field injected by Mockito
        mockConverter : // could be null or a real instance
        (Converter) converterPlaceholder;
        // when
        Integer result = AtomicBooleanConversions.toInt(from, converterToUse);
        // then
        assertEquals(expected, result, () -> "Expected conversion result for AtomicBoolean(" + from.get() + ")");
        // Ensure reference equality with the cached constants
        assertSame(expected, result, "Result must be the cached CommonValues instance");
    }
}
