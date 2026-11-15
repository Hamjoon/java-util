package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toShort_Test {

    @Mock
    private Converter // injected mock, no behavior needed for this method
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already creates the mock; reset it to a clean state
        reset(converter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method never called any method on the mock converter
        verifyNoMoreInteractions(converter);
    }

    private static Stream<Arguments> provideTrueFalseCases() {
        // true cases
        AtomicBoolean trueAtomic = new AtomicBoolean(true);
        // false cases
        AtomicBoolean falseAtomic = new AtomicBoolean(false);
        // a Converter implementation that would throw if used – proving the method never calls it
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        return Stream.of(// (from, converter, expected)
        Arguments.of(trueAtomic, null, CommonValues.SHORT_ONE), Arguments.of(falseAtomic, null, CommonValues.SHORT_ZERO), // using the Mockito mock (no behaviour needed)
        Arguments.of(trueAtomic, mock(Converter.class), CommonValues.SHORT_ONE), Arguments.of(falseAtomic, mock(Converter.class), CommonValues.SHORT_ZERO), // using a real converter that would fail if called
        Arguments.of(trueAtomic, throwingConverter, CommonValues.SHORT_ONE), Arguments.of(falseAtomic, throwingConverter, CommonValues.SHORT_ZERO));
    }

    @Test
    void toShort_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean atomicTrue = new AtomicBoolean(true);
        // when
        Short result = AtomicBooleanConversions.toShort(atomicTrue, converter);
        // then
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    void toShort_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean atomicFalse = new AtomicBoolean(false);
        // when
        Short result = AtomicBooleanConversions.toShort(atomicFalse, converter);
        // then
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    void toShort_whenObjectIsNotAtomicBoolean_throwsClassCastException() {
        // given
        // any non‑AtomicBoolean object
        Object notAtomic = Boolean.TRUE;
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toShort(notAtomic, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideTrueFalseCases")
    void toShort_variousConverters_returnsExpected(AtomicBoolean from, Converter suppliedConverter, Short expected) {
        // when
        Short result = AtomicBooleanConversions.toShort(from, suppliedConverter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toShort_subclassOverridesGet_returnsOverriddenValueTrue() {
        // given – subclass that forces get() to return false even though the
        // underlying value is true
        AtomicBoolean atomic = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                return false;
            }
        };
        // when
        Short result = AtomicBooleanConversions.toShort(atomic, converter);
        // then
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    void toShort_subclassOverridesGet_returnsOverriddenValueFalse() {
        // given – subclass that forces get() to return true even though the
        // underlying value is false
        AtomicBoolean atomic = new AtomicBoolean(false) {

            @Override
            public boolean get() {
                return true;
            }
        };
        // when
        Short result = AtomicBooleanConversions.toShort(atomic, converter);
        // then
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    void toShort_subclassGetThrowsIllegalStateException_propagates() {
        // given – get() throws a checked exception wrapped in RuntimeException
        AtomicBoolean atomic = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new IllegalStateException("forced");
            }
        };
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toShort(atomic, converter));
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void toShort_subclassGetThrowsRuntimeException_propagates() {
        // given – get() throws a generic RuntimeException
        AtomicBoolean atomic = new AtomicBoolean(false) {

            @Override
            public boolean get() {
                throw new RuntimeException("forced");
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> AtomicBooleanConversions.toShort(atomic, converter));
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void toShort_whenFromIsNull_throwsNullPointerException() {
        // given
        Object nullFrom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toShort(nullFrom, converter));
    }

    @Test
    void toShort_whenFromIsNotAtomicBoolean_throwsClassCastException() {
        // given – a plain Boolean, not an AtomicBoolean
        Object notAtomic = Boolean.TRUE;
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toShort(notAtomic, converter));
    }
}
