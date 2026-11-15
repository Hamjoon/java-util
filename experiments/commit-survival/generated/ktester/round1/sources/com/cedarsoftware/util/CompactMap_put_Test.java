package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;

// import org.junit.*;
// import org.junit.runner.RunWith;
// @ExtendWith(MockitoExtension.class)
class CompactMap_put_Test {

    private CompactMap<String, String> map;

    //     @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for most tests.
        map = new CompactMap<>();
    }

    /* ---------------------------------------------------------------------- */
    private static Object getInternalVal(CompactMap<?, ?> m) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(m);
    }
}
