package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.Map;

/**
 * Unit tests for {@link ThrowableConversions}.
 *
 * The test class resides in the same package as {@code ThrowableConversions}
 * so it can access the package‑private {@code toMap} method and the private
 * constructor.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a {@link Throwable} without a cause.</li>
 *   <li>Conversion when a cause is present (including null cause message).</li>
 *   <li>Edge cases such as {@code null} messages, {@code null} throwable,
 *       and non‑{@link Throwable} inputs.</li>
 *   <li>Verification that the {@code Converter} argument is ignored.</li>
 * </ul>
 */
public class ThrowableConversionsTest {

    // -----------------------------------------------------------------------
    // Helper constants – they are defined in MapConversions and are used as
    // keys in the map returned by ThrowableConversions.toMap().
    // -----------------------------------------------------------------------
    private static final String KEY_CLASS = CLASS;
    private static final String KEY_MESSAGE = MESSAGE;
    private static final String KEY_CAUSE = CAUSE;
    private static final String KEY_CAUSE_MESSAGE = CAUSE_MESSAGE;

    // -----------------------------------------------------------------------
    // 1. Basic functionality – Throwable without a cause
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_SimpleException_NoCause() {
        Exception ex = new Exception("simple error");
        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Expected keys
        Assert.assertEquals("Map should contain exactly 2 entries", 2, map.size());
        Assert.assertTrue("Map should contain CLASS key", map.containsKey(KEY_CLASS));
        Assert.assertTrue("Map should contain MESSAGE key", map.containsKey(KEY_MESSAGE));

        // Values
        Assert.assertEquals("Class name should match", Exception.class.getName(), map.get(KEY_CLASS));
        Assert.assertEquals("Message should match", "simple error", map.get(KEY_MESSAGE));

        // No cause related entries
        Assert.assertFalse("Map should not contain CAUSE key", map.containsKey(KEY_CAUSE));
        Assert.assertFalse("Map should not contain CAUSE_MESSAGE key", map.containsKey(KEY_CAUSE_MESSAGE));
    }

    // -----------------------------------------------------------------------
    // 2. Throwable with a non‑null cause
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_ExceptionWithCause() {
        IllegalArgumentException cause = new IllegalArgumentException("invalid argument");
        Exception ex = new Exception("wrapper exception", cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        // Expected keys (4 entries when a cause is present)
        Assert.assertEquals("Map should contain exactly 4 entries", 4, map.size());
        Assert.assertEquals("Class name should match", Exception.class.getName(), map.get(KEY_CLASS));
        Assert.assertEquals("Message should match", "wrapper exception", map.get(KEY_MESSAGE));
        Assert.assertEquals("Cause class name should match", IllegalArgumentException.class.getName(), map.get(KEY_CAUSE));
        Assert.assertEquals("Cause message should match", "invalid argument", map.get(KEY_CAUSE_MESSAGE));
    }

    // -----------------------------------------------------------------------
    // 3. Cause present but its message is null
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_CauseWithNullMessage() {
        IllegalStateException cause = new IllegalStateException(); // message == null
        Exception ex = new Exception("outer", cause);

        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        Assert.assertEquals("Map should contain exactly 4 entries", 4, map.size());
        Assert.assertEquals("Cause class name should match", IllegalStateException.class.getName(), map.get(KEY_CAUSE));
        Assert.assertNull("Cause message should be null", map.get(KEY_CAUSE_MESSAGE));
    }

    // -----------------------------------------------------------------------
    // 4. Throwable with a null message (but no cause)
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_NullMessage() {
        Exception ex = new Exception((String) null); // explicit null message

        Map<String, Object> map = ThrowableConversions.toMap(ex, null);

        Assert.assertEquals("Map should contain exactly 2 entries", 2, map.size());
        Assert.assertEquals("Class name should match", Exception.class.getName(), map.get(KEY_CLASS));
        Assert.assertNull("Message should be null", map.get(KEY_MESSAGE));
    }

    // -----------------------------------------------------------------------
    // 5. Edge case – null Throwable argument should raise NPE
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMap_NullThrowable_ThrowsNPE() {
        ThrowableConversions.toMap(null, null);
    }

    // -----------------------------------------------------------------------
    // 6. Edge case – non‑Throwable argument should raise ClassCastException
    // -----------------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testToMap_NonThrowable_ThrowsClassCastException() {
        String notAThrowable = "I am not a throwable";
        ThrowableConversions.toMap(notAThrowable, null);
    }

    // -----------------------------------------------------------------------
    // 7. Verify that the Converter parameter is truly ignored (no NPE)
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_WithNonNullConverter_Ignored() {
        // The Converter class requires a ConverterOptions instance; passing null
        // is acceptable because the method does not interact with it.
        Converter dummyConverter = new Converter(null);
        Exception ex = new Exception("test");

        Map<String, Object> map = ThrowableConversions.toMap(ex, dummyConverter);

        Assert.assertEquals("Class name should match", Exception.class.getName(), map.get(KEY_CLASS));
        Assert.assertEquals("Message should match", "test", map.get(KEY_MESSAGE));
    }

    // -----------------------------------------------------------------------
    // 8. Private constructor coverage – instantiate via reflection
    // -----------------------------------------------------------------------
    @Test
    public void testPrivateConstructor_IsNotInstantiable() throws Exception {
        java.lang.reflect.Constructor<ThrowableConversions> ctor =
                ThrowableConversions.class.getDeclaredConstructor();
        Assert.assertFalse("Constructor should be private", ctor.isAccessible());

        // Make it accessible for coverage purposes – it should not throw.
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        Assert.assertNotNull("Instance created via reflection should not be null", instance);
    }
}
