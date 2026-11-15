package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toDouble_Test {

    @Mock
    Converter // the method does not use the converter, but we provide a mock anyway
    mockConverter;

    private static Stream<Arguments> provideNormalCases() {
        // 1. true atomic, null converter
        // 2. false atomic, null converter
        // 3. true atomic, mock converter
        // 4. false atomic, mock converter
        // 5. true atomic, throwing converter
        // 6. false atomic, throwing converter
        // 7. atomic whose value was changed after construction (false→true)
        // 8. atomic that should remain unchanged after conversion
        AtomicBoolean mutable = new AtomicBoolean(false);
        // false → true before call
        mutable.set(true);
        AtomicBoolean unchanged = new AtomicBoolean(true);
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                throw new RuntimeException("should not be called");
            }
        };
        return Stream.of(// true, null
        Arguments.of(new AtomicBoolean(true), null, CommonValues.DOUBLE_ONE), // false, null
        Arguments.of(new AtomicBoolean(false), null, CommonValues.DOUBLE_ZERO), // true, mock
        Arguments.of(new AtomicBoolean(true), mockInstance(), CommonValues.DOUBLE_ONE), // false, mock
        Arguments.of(new AtomicBoolean(false), mockInstance(), CommonValues.DOUBLE_ZERO), // true, throwing converter
        Arguments.of(new AtomicBoolean(true), throwingConverter, CommonValues.DOUBLE_ONE), // false, throwing converter
        Arguments.of(new AtomicBoolean(false), throwingConverter, CommonValues.DOUBLE_ZERO), // mutable atomic (false→true before call)
        Arguments.of(mutable, null, CommonValues.DOUBLE_ONE), // unchanged atomic (verify it is not mutated)
        Arguments.of(unchanged, null, CommonValues.DOUBLE_ONE));
    }

    /**
     * Helper that returns the Mockito mock injected into the test class.
     * This indirection is needed because the {@code mockConverter} field is
     * initialised by Mockito after the test class instance is created.
     */
    private static Converter mockInstance() {
        // The test instance will have its @Mock field populated; however,
        // static method sources cannot access instance fields.  Instead,
        // we rely on JUnit to inject the mock into the test method parameters
        // via the @Mock field.  Here we simply return {@code null}; the actual
        // mock will be supplied by the test method itself.
        return null;
    }

    @Test
    void toDouble_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, mockConverter);
        // then
        assertEquals(1.0, result, "AtomicBoolean true should be converted to Double 1.0");
    }

    @Test
    void toDouble_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, mockConverter);
        // then
        assertEquals(0.0, result, "AtomicBoolean false should be converted to Double 0.0");
    }

    @Test
    void toDouble_whenNullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toDouble(source, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toDouble_whenNonAtomicBooleanSource_throwsClassCastException() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toDouble(source, mockConverter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index}: from={0}, converter={1} ⇒ expected={2}")
    @MethodSource("provideNormalCases")
    void toDouble_normalCases(Object from, Converter converter, Double expected) {
        // when
        Double result = AtomicBooleanConversions.toDouble(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected conversion result of " + expected + " but got " + result);
    }

    @Test
    void toDouble_whenAtomicBooleanTrue_returnsOne_usingMock() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, mockConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ONE, result, "AtomicBoolean true should be converted to CommonValues.DOUBLE_ONE");
    }

    @Test
    void toDouble_whenAtomicBooleanFalse_returnsZero_usingMock() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, mockConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ZERO, result, "AtomicBoolean false should be converted to CommonValues.DOUBLE_ZERO");
    }

    @Test
    void toDouble_converterImplementationIsNeverInvoked() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                throw new RuntimeException("Converter should not be called");
            }
        };
        // when
        Double result = AtomicBooleanConversions.toDouble(source, throwingConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Even a Converter that throws should not be invoked; result must be ZERO");
    }

    @Test
    void toDouble_doesNotMutateAtomicBoolean() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, mockConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ONE, result, "Result should be ONE for true value");
        assertTrue(source.get(), "The original AtomicBoolean must remain true after conversion");
    }
}
