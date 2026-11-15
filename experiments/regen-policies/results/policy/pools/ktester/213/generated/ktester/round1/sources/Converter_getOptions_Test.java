import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Converter_getOptions_Test {

    /**
     * Mocked options that will be passed to the {@link Converter} constructor.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * The {@link Converter} instance under test.
     */
    private Converter converter;

    class MySubConverterOptions extends ConverterOptions {
    }

    class MySubConverterOptions extends ConverterOptions {
    }

    // No extra behaviour – just a distinct type.
    class MyConverterOptions extends ConverterOptions {
    }

    class MySubConverterOptions extends ConverterOptions {
    }

    @BeforeEach
    void setupBeforeEach() {
        // The mock is injected by MockitoExtension, we only need to create the
        // Converter instance that uses this mock.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and avoid accidental reuse.
        converter = null;
    }

    /**
     * Verifies that {@link Converter#getOptions()} returns the same
     * {@link ConverterOptions} instance that was supplied to the constructor.
     */
    @Test
    void getOptions_returnsProvidedOptionsInstance() {
        // WHEN
        ConverterOptions returned = converter.getOptions();
        // THEN
        assertNotNull(returned, "getOptions() should never return null");
        assertSame(mockOptions, returned, "Converter#getOptions() must return the exact instance supplied to the constructor");
    }

    /**
     * A secondary sanity check – creating a second Converter with a different
     * mock must yield a different options instance.
     */
    @Test
    void getOptions_isInstanceSpecific() {
        // GIVEN
        ConverterOptions anotherMock = Mockito.mock(ConverterOptions.class);
        Converter anotherConverter = new Converter(anotherMock);
        // WHEN
        ConverterOptions firstOptions = converter.getOptions();
        ConverterOptions secondOptions = anotherConverter.getOptions();
        // THEN
        assertNotSame(firstOptions, secondOptions, "Each Converter instance should keep its own options object");
        assertSame(mockOptions, firstOptions);
        assertSame(anotherMock, secondOptions);
    }

    /**
     * Ensures that repeated calls always return the very same reference.
     */
    @Test
    void getOptions_multipleCalls_returnSameInstance() {
        ConverterOptions first = converter.getOptions();
        ConverterOptions second = converter.getOptions();
        ConverterOptions third = converter.getOptions();
        assertSame(first, second, "Second call should return the same instance as the first");
        assertSame(first, third, "Third call should also return the same instance");
    }

    /**
     * Demonstrates that a mutable change on the returned {@link ConverterOptions}
     * instance is visible on subsequent calls – i.e. the accessor does not
     * return a defensive copy.
     *
     * The test uses a real {@code ConverterOptions} object because we do not
     * know its mutable API; we simply set a flag via a hypothetical {@code setFlag}
     * method (if it exists) using reflection.  If the method is absent the test
     * still proves that the same instance is returned.
     */
    @Test
    void getOptions_mutabilityReflection() throws Exception {
        // Use a real options object instead of the mock.
        ConverterOptions realOptions = new ConverterOptions();
        Converter realConverter = new Converter(realOptions);
        // Obtain the instance via the accessor.
        ConverterOptions viaAccessor = realConverter.getOptions();
        // Try to mutate a (presumed) boolean field called "someFlag".
        // If the field does not exist, the reflection call is ignored – the test
        // still verifies that the same object is returned.
        try {
            Field flagField = ConverterOptions.class.getDeclaredField("someFlag");
            flagField.setAccessible(true);
            flagField.setBoolean(viaAccessor, true);
        } catch (NoSuchFieldException ignored) {
            // No mutable field to flip – the test focuses on reference equality.
        }
        // Retrieve again and assert that the mutation (if any) is still present.
        ConverterOptions afterMutation = realConverter.getOptions();
        assertSame(viaAccessor, afterMutation, "getOptions() must return the exact same object after mutation");
    }

    /**
     * Verifies that a subclass of {@link ConverterOptions} is preserved
     * unchanged – the runtime type of the object returned by {@code getOptions()}
     * matches the concrete subclass supplied to the constructor.
     */
    @Test
    void getOptions_subclassHandling() {
        // No extra behaviour – just a distinct type.
        class MySubConverterOptions extends ConverterOptions {
        }
        MySubConverterOptions subOptions = new MySubConverterOptions();
        Converter subConverter = new Converter(subOptions);
        ConverterOptions returned = subConverter.getOptions();
        assertSame(subOptions, returned, "The returned options object must be the exact instance supplied");
        assertEquals(MySubConverterOptions.class, returned.getClass(), "Runtime type of the returned object should be the subclass used at construction");
    }

    /**
     * Confirms that concurrent access to {@code getOptions()} yields the same
     * reference for every thread, demonstrating thread‑safety of the accessor.
     */
    @Test
    void getOptions_concurrencyConsistency() throws Exception {
        final int THREAD_COUNT = 20;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ConverterOptions>> futures = new ArrayList<>();
        // All threads share the same Converter instance.
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> converter.getOptions()));
        }
        // Collect results.
        List<ConverterOptions> results = new ArrayList<>();
        for (Future<ConverterOptions> f : futures) {
            results.add(f.get(2, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        // Every returned reference must be identical to the first one.
        ConverterOptions first = results.get(0);
        for (int i = 1; i < results.size(); i++) {
            assertSame(first, results.get(i), "All threads must receive the identical ConverterOptions instance");
        }
    }

    /**
     * Checks the behaviour when the internal {@code options} field is forced
     * to {@code null} via reflection.  This simulates an unexpected internal
     * state and ensures that {@code getOptions()} simply returns {@code null}
     * without throwing.
     */
    @Test
    void getOptions_nullOptionsViaReflection() throws Exception {
        // Create a Converter with a non‑null options object first.
        ConverterOptions dummy = new ConverterOptions();
        Converter reflectiveConverter = new Converter(dummy);
        // Obtain the private final field and set it to null.
        Field optionsField = Converter.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        // Remove the 'final' modifier (necessary on some JVMs).
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(optionsField, optionsField.getModifiers() & (~Modifier.FINAL));
        optionsField.set(reflectiveConverter, null);
        // Now the accessor should return null.
        assertNull(reflectiveConverter.getOptions(), "When the internal options field is null, getOptions() must return null");
    }

    /**
     * Validates that constructing a {@link Converter} with a {@code null}
     * {@link ConverterOptions} reference (if the public constructor permits it)
     * results in {@code getOptions()} returning {@code null}.
     *
     * The test guards against a {@link NullPointerException} that could be
     * thrown by the constructor.
     */
    @Test
    void getOptions_nullWhenConstructedWithNull() {
        // Some versions of the library allow a null argument – the test simply
        // verifies the observable behaviour.
        Converter nullOptionsConverter = new Converter(((ConverterOptions) (null)));
        assertNull(nullOptionsConverter.getOptions(), "Converter constructed with a null options reference should return null from getOptions()");
    }
}
