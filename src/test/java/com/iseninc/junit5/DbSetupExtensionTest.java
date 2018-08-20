package com.iseninc.junit5;

import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

class DbSetupExtensionTest {
    private DbSetupExtension extension;
    private ExtensionContext mockContext;

    @BeforeEach
    void setup() {
        extension = new DbSetupExtension();
        mockContext = mock(ExtensionContext.class);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PostProcessTestInstance {
        @ParameterizedTest
        @MethodSource("createInvalidCombinations")
        void shouldThrowIfAnnotationsAreInvalid(Class<?> clazz, Object instance) throws Exception {
            // arrange
            doReturn(clazz).when(mockContext).getRequiredTestClass();

            // act
            Throwable t = catchThrowable(() -> extension.postProcessTestInstance(instance, mockContext));

            // assert
            assertThat(t).isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @MethodSource("createValidCombinations")
        void shouldPassWithValidAnnotations(Class<?> clazz, Object instance) {
            // arrange
            doReturn(clazz).when(mockContext).getRequiredTestClass();

            // act
            Throwable t = catchThrowable(() -> extension.postProcessTestInstance(instance, mockContext));

            // assert
            assertThat(t).isNull();
        }

        private Stream<Arguments> createValidCombinations() {
            return Stream.of(
                    Arguments.of(StaticMethodFactory.class, StaticMethodFactory.INSTANCE),
                    Arguments.of(StaticFieldFactory.class, StaticFieldFactory.INSTANCE),
                    Arguments.of(InstanceMethodFactory.class, InstanceMethodFactory.INSTANCE),
                    Arguments.of(InstanceFieldFactory.class, InstanceFieldFactory.INSTANCE),
                    Arguments.of(StaticMethodOperation.class, StaticMethodOperation.INSTANCE),
                    Arguments.of(StaticFieldOperation.class, StaticFieldOperation.INSTANCE),
                    Arguments.of(InstanceMethodOperation.class, InstanceMethodOperation.INSTANCE),
                    Arguments.of(InstanceFieldOperation.class, InstanceFieldOperation.INSTANCE),
                    Arguments.of(MixedOperation.class, MixedOperation.INSTANCE),
                    Arguments.of(InnerOperations.class, InnerOperations.INSTANCE));
        }

        private Stream<Arguments> createInvalidCombinations() {
            return Stream.of(
                    Arguments.of(NoFactory.class, NoFactory.INSTANCE),
                    Arguments.of(MultipleFactories.class, MultipleFactories.INSTANCE),
                    Arguments.of(NoOperations.class, NoOperations.INSTANCE));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class BeforeEachCallback {
        @ParameterizedTest
        @MethodSource("createValidCombinations")
        void shouldNotRunSetupIfMethodHasSkipAnnotation(Class<?> clazz, Object instance, Runnable reset, Runnable verify, Runnable verifyNoExecutions) throws Exception {
            // arrange
            reset.run();

            doReturn(clazz).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(instance, mockContext);

            Method method = Methods.class.getMethod("skipDbSetup");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(instance).when(mockContext).getRequiredTestInstance();

            // act
            extension.beforeEach(mockContext);

            // assert
            verifyNoExecutions.run();
        }

        @ParameterizedTest
        @MethodSource("createValidCombinations")
        void shouldRunSetupIfMethodDoesNotHaveSkipAnnotation(Class<?> clazz, Object instance, Runnable reset, Runnable verify) throws Exception {
            // arrange
            reset.run();

            doReturn(clazz).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(instance, mockContext);

            Method method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(instance).when(mockContext).getRequiredTestInstance();

            // act
            extension.beforeEach(mockContext);

            // assert
            verify.run();
        }

        private Stream<Arguments> createValidCombinations() {
            return Stream.of(
                    createArguments(StaticMethodFactory.class),
                    createArguments(StaticFieldFactory.class),
                    createArguments(InstanceMethodFactory.class),
                    createArguments(InstanceFieldFactory.class),
                    createArguments(StaticMethodOperation.class),
                    createArguments(StaticFieldOperation.class),
                    createArguments(InstanceMethodOperation.class),
                    createArguments(InstanceFieldOperation.class),
                    createArguments(MixedOperation.class),
                    Arguments.of(InnerOperations.InnerClass.class, InnerOperations.INNER_INSTANCE, (Runnable) InnerOperations::resetMocks, (Runnable) InnerOperations::verifyExecuted, (Runnable) InnerOperations::verifyNotExecuted));
        }

        private Arguments createArguments(Class<?> clazz) {
            try {
                Object instance = clazz.getDeclaredField("INSTANCE").get(null);
                Runnable reset = () -> {
                    try {
                        clazz.getDeclaredMethod("resetMocks").invoke(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                Runnable verify = () -> {
                    try {
                        clazz.getDeclaredMethod("verifyExecuted").invoke(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                Runnable verifyNoExecutions = () -> {
                    try {
                        clazz.getDeclaredMethod("verifyNotExecuted").invoke(null);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

                return Arguments.of(clazz, instance, reset, verify, verifyNoExecutions);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Methods {
        @DbSetupSkip
        public void skipDbSetup() {

        }

        public void normalTest() {

        }
    }

    static class NoFactory {
        static final NoFactory INSTANCE = new NoFactory();
    }

    static class MultipleFactories {
        static final MultipleFactories INSTANCE = new MultipleFactories();

        @DbSetupSourceFactory
        private static DataSource mockDataSource1 = mock(DataSource.class);

        @DbSetupSourceFactory
        private static DataSource mockDataSource2 = mock(DataSource.class);
    }

    static class NoOperations {
        static final NoOperations INSTANCE = new NoOperations();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);
    }

    static class StaticMethodFactory {
        static final StaticMethodFactory INSTANCE = new StaticMethodFactory();

        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupSourceFactory
        static DataSource getMockDataSource() {
            return mockDataSource;
        }

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(INSTANCE.mockOperation).execute(eq(mockDataSource.getConnection()), any());;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class StaticFieldFactory {
        static final StaticFieldFactory INSTANCE = new StaticFieldFactory();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(INSTANCE.mockOperation).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InstanceMethodFactory {
        static final InstanceMethodFactory INSTANCE = new InstanceMethodFactory();

        private DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupSourceFactory
        DataSource getMockDataSource() {
            return mockDataSource;
        }

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(INSTANCE.mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(INSTANCE.mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(INSTANCE.mockDataSource).getConnection();;
                verify(INSTANCE.mockOperation).execute(eq(INSTANCE.mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InstanceFieldFactory {
        static final InstanceFieldFactory INSTANCE = new InstanceFieldFactory();

        @DbSetupSourceFactory
        private DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(INSTANCE.mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(INSTANCE.mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(INSTANCE.mockDataSource).getConnection();
                verify(INSTANCE.mockOperation).execute(eq(INSTANCE.mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class StaticMethodOperation {
        static final StaticMethodOperation INSTANCE = new StaticMethodOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        private static Operation mockOperation = mock(Operation.class);

        @DbSetupOperation
        private static Operation getOperation() {
            return mockOperation;
        }

        static void resetMocks() {
            reset(mockDataSource, mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(mockOperation).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class StaticFieldOperation {
        static final StaticFieldOperation INSTANCE = new StaticFieldOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(mockDataSource, mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(mockOperation).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InstanceMethodOperation {
        static final InstanceMethodOperation INSTANCE = new InstanceMethodOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        private Operation mockOperation = mock(Operation.class);

        @DbSetupOperation
        private Operation getOperation() {
            return mockOperation;
        }

        static void resetMocks() {
            reset(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(INSTANCE.mockOperation).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InstanceFieldOperation {
        static final InstanceFieldOperation INSTANCE = new InstanceFieldOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        static void resetMocks() {
            reset(mockDataSource,
                    INSTANCE.mockOperation);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource,
                    INSTANCE.mockOperation);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(INSTANCE.mockOperation).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class MixedOperation {
        static final MixedOperation INSTANCE = new MixedOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation1 = mock(Operation.class);

        private static Operation mockOperation2 = mock(Operation.class);

        @DbSetupOperation
        private static Operation getOperation2() {
            return mockOperation2;
        }

        @DbSetupOperation
        private Operation mockOperation3 = mock(Operation.class);

        private Operation mockOperation4 = mock(Operation.class);

        @DbSetupOperation
        private Operation getOperation4() {
            return mockOperation4;
        }

        static void resetMocks() {
            reset(mockDataSource,
                    mockOperation1,
                    mockOperation2,
                    INSTANCE.mockOperation3,
                    INSTANCE.mockOperation4);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockOperation1,
                    mockOperation2,
                    INSTANCE.mockOperation3,
                    INSTANCE.mockOperation4);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();

                verify(mockOperation1).execute(eq(mockDataSource.getConnection()), any());
                verify(mockOperation2).execute(eq(mockDataSource.getConnection()), any());
                verify(INSTANCE.mockOperation3).execute(eq(mockDataSource.getConnection()), any());
                verify(INSTANCE.mockOperation4).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InnerOperations {
        static final InnerOperations INSTANCE = new InnerOperations();
        static final InnerOperations.InnerClass INNER_INSTANCE = INSTANCE.new InnerClass();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation1 = mock(Operation.class);

        private static Operation mockOperation2 = mock(Operation.class);

        @DbSetupOperation
        private static Operation getOperation2() {
            return mockOperation2;
        }

        class InnerClass {
            @DbSetupOperation
            private Operation mockOperation3 = mock(Operation.class);

            private Operation mockOperation4 = mock(Operation.class);

            @DbSetupOperation
            private Operation getOperation4() {
                return mockOperation4;
            }
        }

        static void resetMocks() {
            reset(mockDataSource,
                    mockOperation1,
                    mockOperation2,
                    INNER_INSTANCE.mockOperation3,
                    INNER_INSTANCE.mockOperation4);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockOperation1,
                    mockOperation2,
                    INNER_INSTANCE.mockOperation3,
                    INNER_INSTANCE.mockOperation4);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();

                verify(mockOperation1).execute(eq(mockDataSource.getConnection()), any());
                verify(mockOperation2).execute(eq(mockDataSource.getConnection()), any());
                verify(INNER_INSTANCE.mockOperation3).execute(eq(mockDataSource.getConnection()), any());
                verify(INNER_INSTANCE.mockOperation4).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}