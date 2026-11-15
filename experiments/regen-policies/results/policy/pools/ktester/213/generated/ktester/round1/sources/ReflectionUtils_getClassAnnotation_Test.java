package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassAnnotation_Test {

    static class Bar extends Foo {
    }

    //     @SimpleAnnotation
    static interface Baz {
    }

    static interface Qux extends Baz {
    }

    static class ImplementsQux implements Qux {
    }

    //     @MyInheritedAnnotation
    static class SuperClassWithInheritedAnno {
    }

    static class SubClassWithInheritedAnno extends SuperClassWithInheritedAnno {
    }

    //     @MyNonInheritedAnnotation
    static class SuperClassWithNonInheritedAnno {
    }

    static class SubClassWithoutInheritedAnno extends SuperClassWithNonInheritedAnno {
    }

    //     @ClassRetentionAnnotation
    static class ClassWithClassRetentionAnno {
    }

    //     @MyInheritedAnnotation
    static interface AnnotatedInterface {
    }

    static class ImplementsAnnotatedInterface implements AnnotatedInterface {
    }

    static class Foo {
        // No annotation – the original tests used a custom @ControllerClass
        // which is not present in this repository.
    }

    @Deprecated
    static class DirectAnnotatedClass {
    }

    static class NoAnnotation {
    }

    /* -------------------------------------------------------------
     *  Lifecycle handling – clear the static caches before/after tests
     * ------------------------------------------------------------- */
//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure a clean cache for the whole suite
//         ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test so that one test's cache does not affect another
//         ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Final clean‑up (not strictly required but keeps the static field tidy)
//         ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    private static void resetCache() {
        // Ensure a clean cache before each test – the original tests did this
        ReflectionUtils.setClassAnnotationCache(new java.util.HashMap<>());
    }

    /* -------------------------------------------------------------
     *  Main entry point – runs all the test methods.
     * ------------------------------------------------------------- */
//     public static void main(String[] args) {
//         // Enable assertions if they are not already enabled.
//         // Run the JVM with the -ea flag for production use.
//         boolean assertionsEnabled = false;
//         // Intentional side‑effect
//         assert assertionsEnabled = true;
//         if (!assertionsEnabled) {
//             System.err.println("Assertions are not enabled. Run the program with the -ea JVM option.");
//             System.exit(1);
//         }
//         System.out.println("Running ReflectionUtils_getClassAnnotation_Test...");
//         testNullAnnotationClassThrowsException();
//         System.out.println(" - testNullAnnotationClassThrowsException passed");
//         testDirectAnnotationWithJdkAnnotation();
//         System.out.println(" - testDirectAnnotationWithJdkAnnotation passed");
//         testCacheReuseForPresentAnnotation_JdkAnnotation();
//         System.out.println(" - testCacheReuseForPresentAnnotation_JdkAnnotation passed");
//         testCacheReuseForMissingAnnotation();
//         System.out.println(" - testCacheReuseForMissingAnnotation passed");
//         System.out.println("All tests passed.");
//     }

//     @Test
//     void testNullAnnotationClassThrowsException() {
//         resetCache();
//         try {
//             ReflectionUtils.getClassAnnotation(Foo.class, null);
//             throw new AssertionError("Expected IllegalArgumentException was not thrown");
//         } catch (IllegalArgumentException ex) {
//             assert ex.getMessage().contains("annotation class cannot be null") : "Exception message does not contain expected text";
//         }
//     }

//     @Test
//     void testDirectAnnotationWithJdkAnnotation() {
//         resetCache();
//         Deprecated ann = ReflectionUtils.getClassAnnotation(DirectAnnotatedClass.class, Deprecated.class);
//         assert ann != null : "The @Deprecated annotation should be discovered on the class.";
//     }

//     @Test
//     void testCacheReuseForPresentAnnotation_JdkAnnotation() {
//         resetCache();
//         // First lookup – cache miss, loads the @Deprecated instance
//         Deprecated first = ReflectionUtils.getClassAnnotation(DirectAnnotatedClass.class, Deprecated.class);
//         assert first != null : "First lookup should return a non‑null annotation";
//         // Second lookup – should hit the cache and return the exact same instance
//         Deprecated second = ReflectionUtils.getClassAnnotation(DirectAnnotatedClass.class, Deprecated.class);
//         assert first == second : "The cached annotation instance must be returned on subsequent calls";
//     }

//     @Test
//     void testCacheReuseForMissingAnnotation() {
//         resetCache();
//         // First call stores a null entry for a class that lacks @Deprecated
//         Deprecated first = ReflectionUtils.getClassAnnotation(NoAnnotation.class, Deprecated.class);
//         assert first == null : "First lookup should return null for missing annotation";
//         // Second call must also return null without recomputing
//         Deprecated second = ReflectionUtils.getClassAnnotation(NoAnnotation.class, Deprecated.class);
//         assert second == null : "Second lookup should also return null";
//     }
}