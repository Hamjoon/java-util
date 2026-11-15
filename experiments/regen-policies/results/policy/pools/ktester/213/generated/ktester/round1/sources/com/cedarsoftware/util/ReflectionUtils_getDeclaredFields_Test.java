import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    class LocalSecureTestClass {

        private int secret = 123;
    }

    /**
     * Simple class with a static and an instance field.
     */
    static class SimpleClass {

        static int staticField = 10;

        int instanceField = 20;
    }

    /**
     * Class containing a Groovy‑style metaClass field (simulated).
     */
    static class GroovyLike {

        // The default filter removes a field named "metaClass" whose type is
        // groovy.lang.MetaClass.  We cannot depend on Groovy at test time, so we
        // create a dummy type with the same fully‑qualified name.
        @SuppressWarnings("unused")
        private final Object metaClass = null;
    }

    /**
     * Simple class with a static and two instance fields.
     */
    static class SampleClass {

        static int staticField = 10;

        int instanceField1 = 1;

        int instanceField2 = 2;
    }

    /**
     * Class with no declared fields.
     */
    static class EmptyClass {
    }

    /**
     * Class used to test a predicate that filters by name prefix.
     */
    static class PrefixClass {

        int alpha;

        int beta;

        int albatross;

        int gamma;
    }

    /**
     * Class used to verify declaration order is preserved.
     */
    static class OrderedClass {

        int first;

        int second;

        int third;
    }

    /**
     * Class used for the SecurityManager tests (private field).
     */
    static class InaccessibleFieldClass {

        private int hidden = 42;
    }

    /**
     * Class used for the SecurityManager test that denies getDeclaredFields.
     */
    static class SecureClass {

        private int secret = 99;
    }

    /**
     * Helper class used in a few tests.
     */
    class SecureTestClass {

        private int secret = 123;
    }

//     class LocalSecureTestClass {

        private int secret = 123;
    }

//     @Test
//     void shouldThrowWhenClassIsNull() {
//         Predicate<Field> any = f -> true;
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(null, any));
//         assertTrue(ex.getMessage().contains("class cannot be null"));
//     }

//     @Test
//     void shouldThrowWhenPredicateIsNull() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(SimpleClass.class, null));
//         assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
//     }

//     @Test
//     void defaultFilterExcludesStaticFields() {
//         List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         // only the two instance fields should survive
//         assertEquals(2, fields.size());
//         assertEquals("instanceField1", fields.get(0).getName());
//         assertEquals("instanceField2", fields.get(1).getName());
//     }

//     @Test
//     void defaultFilterExcludesEnumInternalFields() {
//         List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         // Enum constants are not fields; the only fields are the synthetic ones that
//         // the default filter removes. Therefore the result should be empty.
//         assertTrue(fields.isEmpty(), "Default filter should remove enum internal fields such as $VALUES and ENUM$VALUES");
//     }

//     @Test
//     void defaultFilterExcludesGroovyMetaClassField() throws NoSuchFieldException, IllegalAccessException {
//         // The dummy field's type is java.lang.Object, not groovy.lang.MetaClass,
//         // so it should be kept.
//         List<Field> result = ReflectionUtils.getDeclaredFields(GroovyLike.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         assertEquals(1, result.size());
//         assertEquals("metaClass", result.get(0).getName());
//     }

//     @Test
//     void customPredicateCanIncludeStaticFields() {
//         // accept every field
//         Predicate<Field> includeAll = f -> true;
//         List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, includeAll);
//         assertEquals(3, fields.size());
//         assertTrue(fields.stream().anyMatch(f -> f.getName().equals("staticField")));
//         assertTrue(fields.stream().anyMatch(f -> f.getName().equals("instanceField1")));
//         assertTrue(fields.stream().anyMatch(f -> f.getName().equals("instanceField2")));
//     }

//     @Test
//     void repeatedCallsReturnSameUnmodifiableListInstance() {
//         Predicate<Field> includeAll = f -> true;
//         List<Field> firstCall = ReflectionUtils.getDeclaredFields(SimpleClass.class, includeAll);
//         List<Field> secondCall = ReflectionUtils.getDeclaredFields(SimpleClass.class, includeAll);
//         // The cache stores an *unmodifiable* list, so both references must be identical.
//         assertSame(firstCall, secondCall, "Subsequent calls should return the cached list instance");
//         // Verify that the list is indeed unmodifiable.
//         assertThrows(UnsupportedOperationException.class, () -> firstCall.add(null));
//     }

//     @Test
//     void predicateFiltersByNamePrefix() {
//         Predicate<Field> startsWithA = f -> f.getName().startsWith("a");
//         List<Field> result = ReflectionUtils.getDeclaredFields(PrefixClass.class, startsWithA);
//         assertEquals(2, result.size());
//         assertEquals("alpha", result.get(0).getName());
//         assertEquals("albatross", result.get(1).getName());
//     }

//     @Test
//     void predicateAlwaysFalseYieldsEmptyList() {
//         Predicate<Field> none = f -> false;
//         List<Field> result = ReflectionUtils.getDeclaredFields(SampleClass.class, none);
//         assertTrue(result.isEmpty());
//     }

//     @Test
//     void predicateThatThrowsExceptionPropagates() {
//         Predicate<Field> exploding = f -> {
//             throw new IllegalStateException("boom");
//         };
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, exploding));
//         assertEquals("boom", ex.getMessage());
//     }

    // -----------------------------------------------------------------
//     @Test
//     void fieldsOrderIsPreservedAfterFiltering() {
//         List<Field> fields = ReflectionUtils.getDeclaredFields(OrderedClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         assertEquals(3, fields.size());
//         assertEquals("first", fields.get(0).getName());
//         assertEquals("second", fields.get(1).getName());
//         assertEquals("third", fields.get(2).getName());
//     }

    // -----------------------------------------------------------------
//     @Test
//     void emptyClassReturnsEmptyList() {
//         List<Field> result = ReflectionUtils.getDeclaredFields(EmptyClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         assertTrue(result.isEmpty());
//     }

//     @Test
//     void classCastExceptionWhenCacheCorrupted() throws Exception {
//         // Preserve the original cache so we can restore it later.
//         Map<Object, Collection<Field>> originalCache = getFieldsCache();
//         // Build a corrupted cache that returns a non‑List value for the expected key.
//         Map<Object, Collection<Field>> corrupted = new HashMap<>();
//         // The key class is a private static inner class; create an instance via reflection.
//         Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$FieldsCacheKey");
//         Constructor<?> ctor = keyClass.getDeclaredConstructor(Class.class, Predicate.class, boolean.class);
//         ctor.setAccessible(true);
//         Object key = ctor.newInstance(SampleClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER, false);
//         // Store a String (not a List) as the cached value.
//         corrupted.put(key, Collections.singleton("not a List"));
//         ReflectionUtils.setClassFieldsCache(corrupted);
//         try {
//             assertThrows(ClassCastException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER));
//         } finally {
//             // Restore the original cache to avoid side‑effects on other tests.
//             ReflectionUtils.setClassFieldsCache(originalCache);
//         }
//     }

    // -----------------------------------------------------------------
//     @Test
//     void securityExceptionFromGetDeclaredFieldsPropagates() {
//         class LocalSecureTestClass {
// 
//             private int secret = 123;
//         }
//         SecurityManager original = System.getSecurityManager();
//         System.setSecurityManager(new SecurityManager() {
// 
//             @Override
//             public void checkMemberAccess(Class<?> clazz, int which) {
//                 if (clazz == LocalSecureTestClass.class) {
//                     throw new SecurityException("access denied");
//                 }
//             }
// 
//             @Override
//             public void checkPermission(Permission perm) {
//                 // allow everything else
//             }
//         });
//         try {
//             assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(LocalSecureTestClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER));
//         } finally {
//             System.setSecurityManager(original);
//         }
//     }

//     @Test
//     void securityExceptionFromTrySetAccessiblePropagates() throws NoSuchFieldException {
//         // The default filter will accept the private field; the call to
//         // ClassUtilities.trySetAccessible should trigger a ReflectPermission check.
//         SecurityManager original = System.getSecurityManager();
//         System.setSecurityManager(new SecurityManager() {
// 
//             @Override
//             public void checkPermission(Permission perm) {
//                 if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
//                     throw new SecurityException("ReflectPermission denied");
//                 }
//             }
//         });
//         try {
//             assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(InaccessibleFieldClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER));
//         } finally {
//             System.setSecurityManager(original);
//         }
//     }

    /**
     * Retrieves the current (private) {@code FIELDS_CACHE} from {@link ReflectionUtils}
     * via reflection. Used to restore the cache after the {@code classCastExceptionWhenCacheCorrupted}
     * test.
     */
//     @SuppressWarnings("unchecked")
//     private static Map<Object, Collection<Field>> getFieldsCache() throws Exception {
//         Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
//         cacheField.setAccessible(true);
//         AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) cacheField.get(null);
//         return (Map<Object, Collection<Field>>) ref.get();
//     }
}