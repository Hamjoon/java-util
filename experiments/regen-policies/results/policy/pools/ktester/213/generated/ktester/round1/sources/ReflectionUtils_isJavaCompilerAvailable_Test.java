package com.cedarsoftware.util;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
// import org.junit.*;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_isJavaCompilerAvailable_Test {

    /**
     * Holds the original value of the system property so we can restore it after each test.
     */
    private String originalForceJreProperty;

//     @BeforeEach
    void setupBeforeEach() {
        // Preserve the current value (may be null) of the property that influences the method.
        originalForceJreProperty = System.getProperty("java.util.force.jre");
        // Ensure the property is cleared unless a test explicitly sets it.
        System.clearProperty("java.util.force.jre");
    }

//     @AfterEach
    void teardownAfterEach() {
        // Restore the property to its original state to avoid side‑effects on other tests.
        if (originalForceJreProperty == null) {
            System.clearProperty("java.util.force.jre");
        } else {
            System.setProperty("java.util.force.jre", originalForceJreProperty);
        }
    }

    /**
     * When the special system property {@code java.util.force.jre} is **not** set,
     * {@link ReflectionUtils#isJavaCompilerAvailable()} should return {@code true}
     * on a typical JDK installation (the JDK ships with {@code javax.tools.ToolProvider}).
     */
//     @Test
//     void isJavaCompilerAvailable_whenForceJreNotSet_returnsTrue() {
//         // Guard: make sure the property is really absent.
//         Assert.assertNull(System.getProperty("java.util.force.jre"));
//         boolean available = ReflectionUtils.isJavaCompilerAvailable();
//         // In a standard JDK the compiler is present, so we expect {@code true}.
//         // If the test runs on a JRE without a compiler the method would return false;
//         // we explicitly assert true because the library is intended to be used with a JDK.
//         Assert.assertTrue(available, "Expected Java compiler to be reported as available when the force‑JRE flag is not set.");
//     }

    /**
     * When the system property {@code java.util.force.jre} is set to {@code true},
     * the method must behave as if it is running on a JRE and return {@code false},
     * regardless of the actual presence of a compiler.
     */
//     @Test
//     void isJavaCompilerAvailable_whenForceJreIsTrue_returnsFalse() {
//         System.setProperty("java.util.force.jre", "true");
//         boolean available = ReflectionUtils.isJavaCompilerAvailable();
//         Assert.assertFalse(available, "Expected Java compiler to be reported as unavailable when 'java.util.force.jre' is true.");
//     }

    /**
     * When the system property {@code java.util.force.jre} is set to {@code false},
     * the method should ignore the flag and perform the normal availability check.
     */
//     @Test
//     void isJavaCompilerAvailable_whenForceJreIsFalse_usesNormalCheck() {
//         System.setProperty("java.util.force.jre", "false");
//         // Normal behaviour – we do not mock anything here, letting the real JDK decide.
//         // On a JDK the compiler is present, therefore true is expected.
//         boolean available = ReflectionUtils.isJavaCompilerAvailable();
//         Assert.assertTrue(available, "When 'java.util.force.jre' is false the flag should be ignored and the normal check performed.");
//     }

//     @Test
//     void isJavaCompilerAvailable_whenToolProviderClassMissing_returnsFalse() {
//         try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
//             classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new ClassNotFoundException("ToolProvider not found"));
//             boolean available = ReflectionUtils.isJavaCompilerAvailable();
//             Assert.assertFalse(available, "When ToolProvider cannot be loaded the method must return false.");
//         }
//     }

//     @Test
//     void isJavaCompilerAvailable_whenLinkageError_returnsFalse() {
//         try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
//             classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new LinkageError("Incompatible class version"));
//             boolean available = ReflectionUtils.isJavaCompilerAvailable();
//             Assert.assertFalse(available, "LinkageError should be caught and result in false.");
//         }
//     }

//     @Test
//     void isJavaCompilerAvailable_whenNoClassDefFoundError_returnsFalse() {
//         try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
//             classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new NoClassDefFoundError("Dependent class missing"));
//             boolean available = ReflectionUtils.isJavaCompilerAvailable();
//             Assert.assertFalse(available, "NoClassDefFoundError should be caught and result in false.");
//         }
//     }

//     @Test
//     void isJavaCompilerAvailable_whenExceptionInInitializerError_returnsFalse() {
//         try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
//             classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new ExceptionInInitializerError("Static init failed"));
//             boolean available = ReflectionUtils.isJavaCompilerAvailable();
//             Assert.assertFalse(available, "ExceptionInInitializerError should be caught and result in false.");
//         }
//     }

//     @Test
//     void isJavaCompilerAvailable_whenSecurityExceptionOnClassLoading_returnsFalse() {
//         try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
//             classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new SecurityException("Access denied"));
//             boolean available = ReflectionUtils.isJavaCompilerAvailable();
//             Assert.assertFalse(available, "SecurityException during class loading should be caught and result in false.");
//         }
//     }
}