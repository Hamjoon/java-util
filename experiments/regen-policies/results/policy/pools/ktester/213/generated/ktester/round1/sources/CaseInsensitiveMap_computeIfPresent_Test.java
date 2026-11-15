package com.cedarsoftware.util;

import java.util.function.BiFunction;
import static org.mockito.Mockito.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.*;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.Assert;
// import static org.junit.Assert.assertNull;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_computeIfPresent_Test {

    private CaseInsensitiveMap<String, String> map;

//     @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test
        map = new CaseInsensitiveMap<>();
        map.put("One", "Value1");
        map.put("Two", "Value2");
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC in large test suites
        map = null;
    }

    private static Stream<Arguments> provideCaseInsensitiveScenarios() {
        return Stream.of(Arguments.of("Apple", 10, (BiFunction<String, Integer, Integer>) (k, v) -> v + 10, 20), Arguments.of("Orange", 15, (BiFunction<String, Integer, Integer>) (k, v) -> v * 2, 30), Arguments.of("  space  ", 4, (BiFunction<String, Integer, Integer>) (k, v) -> v * 2, 8));
    }

    /**
     * Passing a {@code null} key should behave like {@link java.util.Map#computeIfPresent},
     * i.e. return {@code null} and not invoke the remapping function.
     */
//     @Test
//     void computeIfPresent_NullKey_ReturnsNullAndDoesNotInvokeFunction() {
//         // Arrange
//         @SuppressWarnings("unchecked")
//         BiFunction<String, String, String> mockFunction = mock(BiFunction.class);
//         // Act & Assert
//         assertNull(map.computeIfPresent(null, mockFunction));
//         verifyNoInteractions(mockFunction);
//     }
}