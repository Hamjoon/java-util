package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toDouble_Test {

    @Mock
    Converter // Converter is not used by the method, a mock is sufficient
    converter;

    @Test
    void toDouble_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        // then
        assertEquals(CommonValues.DOUBLE_ONE, result, "Conversion of AtomicBoolean(true) should yield Double ONE");
    }

    @Test
    void toDouble_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        // then
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Conversion of AtomicBoolean(false) should yield Double ZERO");
    }

    @Test
    void toDouble_whenNonAtomicBoolean_throwsClassCastException() {
        // given a non‑AtomicBoolean object (String)
        Object notAtomic = "not an AtomicBoolean";
        // then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toDouble(notAtomic, converter), "Providing a non‑AtomicBoolean should result in ClassCastException");
    }

    @Test
    void toDouble_whenSourceNull_throwsNullPointerException() {
        // null source leads to a NPE when the method tries to call get()
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toDouble(null, converter), "Passing null as the source object should throw NullPointerException");
    }

    @Test
    void toDouble_whenAtomicInteger_throwsClassCastException() {
        // given an AtomicInteger – still not an AtomicBoolean
        Object notAtomic = new AtomicInteger(5);
        // then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toDouble(notAtomic, converter), "Providing an AtomicInteger should result in ClassCastException");
    }

    @Test
    void toDouble_withNonNullConverter_true_returnsOne() {
        // a concrete (but otherwise unused) Converter instance
        Converter nonNullConverter = new Converter() {
        };
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, nonNullConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result, "Conversion should ignore a non‑null Converter and still return ONE");
    }

    @Test
    void toDouble_withNonNullConverter_false_returnsZero() {
        Converter nonNullConverter = new Converter() {
        };
        AtomicBoolean source = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(source, nonNullConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Conversion should ignore a non‑null Converter and still return ZERO");
    }

    @Test
    void toDouble_withNonNullConverter_nullSource_throwsNullPointerException() {
        Converter nonNullConverter = new Converter() {
        };
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toDouble(null, nonNullConverter), "Even with a non‑null Converter, a null source should throw NPE");
    }

    @Test
    void toDouble_converterThrowsIfCalled_isNeverInvoked() {
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be called");
            }
        };
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, explodingConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result, "Method does not use the Converter, so conversion succeeds despite the converter throwing");
    }

    @Test
    void toDouble_subclassOverridesGet_returnsOppositeValue() {
        // Anonymous subclass flips the boolean value
        AtomicBoolean source = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                return false;
            }
        };
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Overridden get() returning false should produce ZERO");
    }

    @Test
    void toDouble_subclassOverridesGet_returnsTrueValue() {
        // Anonymous subclass forces true regardless of initial state
        AtomicBoolean source = new AtomicBoolean(false) {

            @Override
            public boolean get() {
                return true;
            }
        };
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(CommonValues.DOUBLE_ONE, result, "Overridden get() returning true should produce ONE");
    }

    @Test
    void toDouble_subclassGetThrows_propagatesException() {
        AtomicBoolean source = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new IllegalStateException("forced");
            }
        };
        assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toDouble(source, converter), "Exception thrown by overridden get() should propagate unchanged");
    }
}
