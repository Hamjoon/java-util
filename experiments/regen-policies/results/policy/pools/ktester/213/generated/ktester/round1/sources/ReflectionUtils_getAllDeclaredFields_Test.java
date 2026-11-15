package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ReflectionUtils_getAllDeclaredFields_Test {

    /* --------------------------------------------------------------------- */
    static class SuperClass {

        private int superField;

        public static int staticSuperField;
    }

    static class SubClass extends SuperClass {

        private String subField;
    }

    static class WithStatic {

        public static int staticField;

        private long instanceField;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test – the cache is internal,
        // but we can replace it with a fresh one via the public setter.
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getAllDeclaredFields_nullClass_throwsException() {
//         Predicate<Field> any = f -> true;
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(null, any));
//         assertTrue(ex.getMessage().contains("class cannot be null"));
//     }

//     @Test
//     void getAllDeclaredFields_nullPredicate_throwsException() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(SubClass.class, null));
//         assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getAllDeclaredFields_defaultFilter_excludesStaticFields() {
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(WithStatic.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         // only the instance field should survive the default filter
//         assertEquals(1, fields.size());
//         assertEquals("instanceField", fields.get(0).getName());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getAllDeclaredFields_resultIsUnmodifiable() {
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
//         assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getAllDeclaredFields_returnsSameCachedInstance() {
//         Predicate<Field> acceptAll = f -> true;
//         List<Field> firstCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, acceptAll);
//         List<Field> secondCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, acceptAll);
//         // The method caches the list; both references should point to the same object
//         assertSame(firstCall, secondCall);
//     }
}