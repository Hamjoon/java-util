package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toLong_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    // Dummy converter that does nothing; used to verify that a non‑null converter is ignored.
    private static class DummyConverter implements Converter {
        // No implementation needed for this test scenario.
    }

    // Converter that throws on any method call; the method under test must not invoke it.
    private static class ExplodingConverter implements Converter {

        @Override
        public String toString() {
            throw new RuntimeException("ExplodingConverter should never be called");
        }
        // If Converter has other methods, they would also throw, but they are never used.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (the @Mock fields)
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void toLong_whenTrue_returnsLongOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        Long result = BooleanConversions.toLong(input, mockConverter);
        // then
        assertEquals(CommonValues.LONG_ONE, result, "Converting Boolean.TRUE should yield CommonValues.LONG_ONE");
    }

    @Test
    void toLong_whenFalse_returnsLongZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        Long result = BooleanConversions.toLong(input, mockConverter);
        // then
        assertEquals(CommonValues.LONG_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.LONG_ZERO");
    }

    @Test
    void toLong_whenNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toLong(input, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toLong_whenNonBoolean_throwsClassCastException() {
        // given
        Object input = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toLong(input, mockConverter), "Passing a non‑Boolean should result in ClassCastException");
    }

    @Test
    void toLong_trueBoolean_withNonNullConverter_returnsLongOne() {
        // given
        Boolean input = Boolean.TRUE;
        Converter dummy = new DummyConverter();
        // when
        Long result = BooleanConversions.toLong(input, dummy);
        // then
        assertEquals(CommonValues.LONG_ONE, result, "Even with a non‑null converter, Boolean.TRUE should map to LONG_ONE");
    }

    @Test
    void toLong_falseBoolean_withNonNullConverter_returnsLongZero() {
        // given
        Boolean input = Boolean.FALSE;
        Converter dummy = new DummyConverter();
        // when
        Long result = BooleanConversions.toLong(input, dummy);
        // then
        assertEquals(CommonValues.LONG_ZERO, result, "Even with a non‑null converter, Boolean.FALSE should map to LONG_ZERO");
    }

    @Test
    void toLong_trueBoolean_withExplodingConverter_ignoresConverter() {
        // given
        Boolean input = Boolean.TRUE;
        Converter exploding = new ExplodingConverter();
        // when
        Long result = BooleanConversions.toLong(input, exploding);
        // then
        assertEquals(CommonValues.LONG_ONE, result, "The converter should be ignored, so Boolean.TRUE still yields LONG_ONE");
    }
}
