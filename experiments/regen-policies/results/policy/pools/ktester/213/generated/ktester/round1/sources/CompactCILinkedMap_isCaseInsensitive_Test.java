package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedMap_isCaseInsensitive_Test {

    private CompactCILinkedMap<String, String> map;

    @BeforeEach
    void setupBeforeEach() {
        // GIVEN: a fresh instance of CompactCILinkedMap with default constructor.
        map = new CompactCILinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Cleanup instance reference.
        map = null;
    }

    @Test
    void isCaseInsensitive_returnsTrue() {
        // WHEN: invoking the protected method isCaseInsensitive().
        // (Accessible because the test resides in the same package.)
        boolean result = map.isCaseInsensitive();
        // THEN: the method should return true.
        assertTrue(result, "CompactCILinkedMap should report case‑insensitive behavior");
    }
}
