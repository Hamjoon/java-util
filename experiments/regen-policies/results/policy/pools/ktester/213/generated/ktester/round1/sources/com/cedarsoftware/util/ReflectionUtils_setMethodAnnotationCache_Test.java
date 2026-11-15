package com.cedarsoftware.util;

import java.util.concurrent.atomic.AtomicReference;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ReflectionUtils_setMethodAnnotationCache_Test {

    /* -----------------------------------------------------------
     *  Sample class with a method annotated with @TestAnno
     * ----------------------------------------------------------- */
    private static class Sample {

        // Uncomment and add a real annotation if you need it for additional tests.
        // @TestAnno
        // void annotatedMethod() { }
        void plainMethod() {
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh empty cache before each test.
//         ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test.
//         ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    /**
     * Retrieves the internal METHOD_ANNOTATION_CACHE map via reflection.
     */
//     @SuppressWarnings("unchecked")
//     private static Map<Object, Annotation> getInternalCache() throws Exception {
//         Field field = ReflectionUtils.class.getDeclaredField("METHOD_ANNOTATION_CACHE");
//         field.setAccessible(true);
//         AtomicReference<Map<? super Object, Annotation>> ref = (AtomicReference<Map<? super Object, Annotation>>) field.get(null);
//         return (Map<Object, Annotation>) ref.get();
//     }

    /* -----------------------------------------------------------
     *  3. Empty map handling
     * ----------------------------------------------------------- */
//     @Test
//     void setMethodAnnotationCache_acceptsEmptyMap() throws Exception {
//         // Act
//         ReflectionUtils.setMethodAnnotationCache(Collections.emptyMap());
//         // Assert
//         Map<Object, Annotation> internal = getInternalCache();
//         Assert.assertTrue(internal.isEmpty(), "Internal cache should be empty.");
//         // Ensure it is a thread‑safe implementation (not the unmodifiable emptyMap)
//         Assert.assertFalse(internal.getClass().equals(Collections.emptyMap().getClass()), "Empty map should be wrapped into a thread‑safe implementation.");
//     }

    /* -----------------------------------------------------------
     *  4. Null argument validation
     * ----------------------------------------------------------- */
//     @Test
//     void setMethodAnnotationCache_nullArgumentThrows() {
//         try {
//             ReflectionUtils.setMethodAnnotationCache(null);
//             Assert.fail("Passing null should result in NullPointerException.");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

    /* -----------------------------------------------------------
     *  8. Idempotent swap with the current cache instance
     * ----------------------------------------------------------- */
//     @Test
//     void setMethodAnnotationCache_idempotentSwapWithSameInstance() throws Exception {
//         // Arrange: obtain the current internal cache (should be empty after @Before)
//         Map<Object, Annotation> current = getInternalCache();
//         // Act
//         ReflectionUtils.setMethodAnnotationCache(current);
//         // Assert
//         Map<Object, Annotation> after = getInternalCache();
//         Assert.assertSame(current, after, "Swapping with the same instance must not change the reference.");
//     }
}