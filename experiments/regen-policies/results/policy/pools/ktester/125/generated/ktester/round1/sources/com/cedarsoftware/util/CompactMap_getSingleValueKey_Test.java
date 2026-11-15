import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import java.lang.reflect.Method;
import java.util.List;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    /* simple placeholder class used in a few tests */
    static class MyKey {
    }

    /**
     * Verify that the default implementation of {@code getSingleValueKey()}
     * returns the constant {@code "id"}.
     */
//     @Test
//     void defaultSingleValueKeyIsId() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object result = method.invoke(map);
//         Assertions.assertEquals("id", result, "Default single value key should be \"id\"");
//     }

    /**
     * Verify that a subclass overriding {@code getSingleValueKey()} returns
     * the overridden value.
     */
//     @Test
//     void overriddenSingleValueKeyIsReturned() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "customKey";
//             }
//         };
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object result = method.invoke(map);
//         Assertions.assertEquals("customKey", result, "Overridden single value key should be returned by the method");
//     }

    /**
     * Edge case: the generic key type is {@code Integer}.  The method still
     * returns a {@code String}, so casting the result to {@code Integer}
     * must throw {@link ClassCastException}.
     */
//     @Test
//     void getSingleValueKey_TypeSafety_IntegerKey() throws Exception {
//         CompactMap<Integer, String> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Assertions.assertThrows(ClassCastException.class, () -> {
//             // The cast to Integer is performed after reflection invocation.
//             Integer value = (Integer) method.invoke(map);
//         }, "Assigning the returned String to an Integer variable should throw ClassCastException");
//     }

    /**
     * Normal usage with a {@code String} key type – the method should return
     * the literal {@code "id"}.
     */
//     @Test
//     void getSingleValueKey_NormalStringKey() throws Exception {
//         CompactMap<String, Integer> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object result = method.invoke(map);
//         Assertions.assertEquals("id", result, "Expected the default key \"id\" for String key type");
//     }

    /**
     * Raw (non‑generic) map instance – the method still returns the constant
     * {@code "id"}.
     */
//     @Test
//     void getSingleValueKey_RawMap() throws Exception {
//         CompactMap map = new CompactMap();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object result = method.invoke(map);
//         Assertions.assertEquals("id", result, "Raw map should still return the default key \"id\"");
//     }

    /**
     * Custom non‑String key type.  The method returns a {@code String}, so
     * casting to the custom key type must fail with {@link ClassCastException}.
     */
//     @Test
//     void getSingleValueKey_CustomKeyType_MyKey() throws Exception {
//         CompactMap<MyKey, String> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Assertions.assertThrows(ClassCastException.class, () -> {
//             MyKey key = (MyKey) method.invoke(map);
//         }, "Assigning the returned String to a MyKey variable should throw ClassCastException");
//     }

    /**
     * Verify that two successive calls on the same instance return the exact
     * same {@code String} object (identity equality) and that it is the same
     * instance as the literal {@code "id"}.
     */
//     @Test
//     void getSingleValueKey_IdentityEqualityAcrossCalls() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object first = method.invoke(map);
//         Object second = method.invoke(map);
//         Assertions.assertSame(first, second, "Both calls should return the same instance");
//         Assertions.assertSame(first, "id", "Returned instance should be the same as the literal \"id\"");
//     }

    /**
     * Using {@code Object} as the key type – the method returns a {@code String}
     * which can be assigned to an {@code Object} without issue.
     */
//     @Test
//     void getSingleValueKey_KeyTypeObject() throws Exception {
//         CompactMap<Object, String> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Object result = method.invoke(map);
//         Assertions.assertEquals("id", result, "When key type is Object, method should still return \"id\"");
//     }

    /**
     * Interface type not implemented by {@code String}.  Attempting to assign
     * the returned value to a {@code List} variable must throw {@link ClassCastException}.
     */
//     @Test
//     void getSingleValueKey_InterfaceKeyType_List() throws Exception {
//         CompactMap<List, String> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Assertions.assertThrows(ClassCastException.class, () -> {
//             List<?> list = (List<?>) method.invoke(map);
//         }, "Assigning the returned String to a List variable should throw ClassCastException");
//     }

    /**
     * Array type key (e.g., {@code String[]}).  The method returns a {@code String},
     * so casting to {@code String[]} must fail with {@link ClassCastException}.
     */
//     @Test
//     void getSingleValueKey_ArrayKeyType_StringArray() throws Exception {
//         CompactMap<String[], Integer> map = new CompactMap<>();
//         Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
//         method.setAccessible(true);
//         Assertions.assertThrows(ClassCastException.class, () -> {
//             String[] array = (String[]) method.invoke(map);
//         }, "Assigning the returned String to a String[] variable should throw ClassCastException");
//     }
}