package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setLenient_Test {

    private static final String FORMAT = "yyyy-MM-dd";

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private SimpleDateFormat mockDelegate;

    private MockedStatic<SafeSimpleDateFormat> staticMock;

    private static final String FORMAT_A = "yyyy-MM-dd";

    private static final String FORMAT_B = "dd/MM/yyyy";

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock SimpleDateFormat that will be returned by the static factory method
        mockDelegate = mock(SimpleDateFormat.class);
        // Mock the static method SafeSimpleDateFormat.getDateFormat(String)
        staticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockDelegate);
        // Instantiate the class under test – the constructor will call the mocked static method
        safeSimpleDateFormat = new SafeSimpleDateFormat(FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so it does not affect other tests.
        if (staticMock != null) {
            staticMock.close();
        }
    }

    @Test
    void setLenient_true_delegatesToUnderlyingDateFormat() {
        // given
        boolean lenient = true;
        // when
        safeSimpleDateFormat.setLenient(lenient);
        // then
        verify(mockDelegate, times(1)).setLenient(lenient);
    }

    @Test
    void setLenient_false_delegatesToUnderlyingDateFormat() {
        // given
        boolean lenient = false;
        // when
        safeSimpleDateFormat.setLenient(lenient);
        // then
        verify(mockDelegate, times(1)).setLenient(lenient);
    }

    /* --------------------------------------------------------------
     *  Toggle behaviour – two successive calls on the same instance
     * -------------------------------------------------------------- */
    @Test
    void setLenient_toggle_callsDelegateInOrder() {
        // first call with true
        safeSimpleDateFormat.setLenient(true);
        // second call with false
        safeSimpleDateFormat.setLenient(false);
        // Verify order of invocation on the same mock
        InOrder inOrder = inOrder(mockDelegate);
        inOrder.verify(mockDelegate).setLenient(true);
        inOrder.verify(mockDelegate).setLenient(false);
        inOrder.verifyNoMoreInteractions();
    }

    /* --------------------------------------------------------------
     *  Cross‑instance isolation – different format strings must not share state
     * -------------------------------------------------------------- */
    @Test
    void crossInstanceIsolation_differentFormats_notAffectEachOther() {
        // Create separate mocks for each format string
        SimpleDateFormat mockDelegateA = mock(SimpleDateFormat.class);
        SimpleDateFormat mockDelegateB = mock(SimpleDateFormat.class);
        // Adjust the static mock to return the appropriate delegate per format
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(FORMAT_A)).thenReturn(mockDelegateA);
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(FORMAT_B)).thenReturn(mockDelegateB);
        // Two independent SafeSimpleDateFormat instances
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat(FORMAT_A);
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat(FORMAT_B);
        // Change lenient only on the first instance
        sdfA.setLenient(true);
        // Verify that only the delegate belonging to FORMAT_A was affected
        verify(mockDelegateA, times(1)).setLenient(true);
        verifyNoInteractions(mockDelegateB);
    }

    /* --------------------------------------------------------------
     *  Invalid format handling – null format string should cause NPE
     * -------------------------------------------------------------- */
    @Test
    void setLenient_nullFormat_throwsNullPointerException() throws Exception {
        // Create an instance without invoking the public constructor
        Constructor<SafeSimpleDateFormat> ctor = SafeSimpleDateFormat.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SafeSimpleDateFormat instance = ctor.newInstance();
        // Force the internal _format field to null via reflection
        Field formatField = SafeSimpleDateFormat.class.getDeclaredField("_format");
        formatField.setAccessible(true);
        formatField.set(instance, null);
        // The static mock is still active; it matches any non‑null string, so a null argument
        // will fall through to the real implementation and trigger a NullPointerException.
        assertThrows(NullPointerException.class, () -> instance.setLenient(true), "Expected NullPointerException when _format is null");
    }

    /* --------------------------------------------------------------
     *  Invalid format handling – malformed pattern should propagate IAE
     * -------------------------------------------------------------- */
    @Test
    void setLenient_invalidPattern_throwsIllegalArgumentException() {
        // Close the generic anyString() stub so the real static method is used for the bad pattern
        staticMock.close();
        // Use a clearly malformed pattern
        String badPattern = "@@@";
        SafeSimpleDateFormat badSdf = new SafeSimpleDateFormat(badPattern);
        // The underlying SimpleDateFormat construction will fail and setLenient should surface the exception
        assertThrows(IllegalArgumentException.class, () -> badSdf.setLenient(false), "Expected IllegalArgumentException for malformed date pattern");
    }
}
