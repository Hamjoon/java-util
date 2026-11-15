package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

// import static org.junit.Assert.*;
class CompactMap_remove_Test {

    private CompactMap<Integer, String> map;

    private SmallCompactMap<Integer, String> bigMap;

    /**
     * A CompactMap that uses a very small {@code compactSize} so we can force the
     * backing‑Map representation without relying on the default (80).
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // Switch to a backing map after 2 entries (or any value we need in a test)
            return 2;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
        bigMap = new SmallCompactMap<>();
    }

    /* -------------------------------------------------
     * Helper method to access the private 'val' field via reflection.
     * ------------------------------------------------- */
    private static Object getInternalVal(CompactMap<?, ?> cm) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            return f.get(cm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
