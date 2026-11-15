package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter.ConversionPair;
import com.cedarsoftware.util.convert.Convert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class DefaultConverterOptionsTest {

    /**
     * Utility method that injects a custom option into the private {@code customOptions} map
     * of a {@link DefaultConverterOptions} instance using reflection.
     */
    private void putCustomOption(DefaultConverterOptions options, String name, Object value) throws Exception {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) field.get(options);
        map.put(name, value);
    }

    @Test
    public void testGetCustomOptionReturnsNullWhenAbsent() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        assertNull("Custom option should be null when not present", options.getCustomOption("nonexistent"));
    }

    @Test
    public void testGetCustomOptionReturnsValueWhenPresent() throws Exception {
        DefaultConverterOptions options = new DefaultConverterOptions();
        String key = "myOption";
        Integer expected = 42;

        // inject the value directly into the private map
        putCustomOption(options, key, expected);

        Integer actual = options.getCustomOption(key);
        assertEquals("Custom option should return the value that was put via reflection", expected, actual);
    }

    @Test
    public void testGetConverterOverridesReturnsSameMapInstance() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map1 = options.getConverterOverrides();
        Map<ConversionPair, Convert<?>> map2 = options.getConverterOverrides();

        // The returned map should be the exact same instance each time
        assertSame("Multiple calls to getConverterOverrides should return the same map instance", map1, map2);
    }

    @Test
    public void testConverterOverridesMapAllowsPutAndRetrieve() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();

        ConversionPair pair = new ConversionPair(String.class, Integer.class);
        Convert<Integer> convert = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return Integer.valueOf(from.toString());
            }
        };

        // Put the custom converter into the map
        overrides.put(pair, convert);

        // Retrieve and verify
        @SuppressWarnings("unchecked")
        Convert<Integer> retrieved = (Convert<Integer>) overrides.get(pair);
        assertNotNull("Retrieved Convert should not be null", retrieved);
        assertEquals("Convert should correctly transform a string to integer", Integer.valueOf(123),
                retrieved.convert("123", null));
    }

    @Test
    public void testConverterOverridesMapIsConcurrentHashMap() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();
        assertTrue("The internal map should be a ConcurrentHashMap",
                overrides instanceof ConcurrentHashMap);
    }

    @Test
    public void testConcurrentAccessToCustomOptionsMap() throws Exception {
        final DefaultConverterOptions options = new DefaultConverterOptions();
        final int threadCount = 10;
        final int entriesPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Each thread will insert entries into the private customOptions map
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // wait for the go signal
                    for (int j = 0; j < entriesPerThread; j++) {
                        String key = "t" + threadId + "_e" + j;
                        putCustomOption(options, key, threadId * 1000 + j);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Release all threads simultaneously
        startLatch.countDown();
        // Wait for all threads to finish
        doneLatch.await();

        // Verify that all expected entries are present
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < entriesPerThread; j++) {
                String key = "t" + i + "_e" + j;
                Integer expected = i * 1000 + j;
                Integer actual = options.getCustomOption(key);
                assertEquals("Value for key '" + key + "' should match expected", expected, actual);
            }
        }
    }

    @Test
    public void testConcurrentAccessToConverterOverridesMap() throws Exception {
        final DefaultConverterOptions options = new DefaultConverterOptions();
        final Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();

        final int threadCount = 5;
        final int pairsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < pairsPerThread; j++) {
                        Class<?> src = (threadId % 2 == 0) ? String.class : Integer.class;
                        Class<?> tgt = (j % 2 == 0) ? Double.class : Long.class;
                        ConversionPair pair = new ConversionPair(src, tgt);
                        Convert<?> convert = (from, converter) -> null; // dummy implementation
                        overrides.put(pair, convert);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // The map size should be at most threadCount * pairsPerThread, but because many pairs may be equal,
        // we only assert that the map is not empty and that no ConcurrentModificationException occurred.
        assertFalse("Overrides map should contain entries after concurrent puts", overrides.isEmpty());
    }
}
