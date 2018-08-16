package com.iseninc.junit5;

import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    class BeforeEachCallback {
        @BeforeEach
        void setup() throws Exception {
            reset(MixedOperation.mockOperation1,
                    MixedOperation.mockOperation2,
                    MixedOperation.INSTANCE.mockOperation3,
                    MixedOperation.INSTANCE.mockOperation4);

            doReturn(MixedOperation.class).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(MixedOperation.INSTANCE, mockContext);
        }

        @Test
        void shouldNotRunSetupIfMethodHasSkipAnnotation() throws Exception {
            // arrange
            Method method = Methods.class.getMethod("skipDbSetup");
            doReturn(method).when(mockContext).getRequiredTestMethod();

            // act
            extension.beforeEach(mockContext);

            // assert
            verifyZeroInteractions(MixedOperation.mockOperation1,
                    MixedOperation.mockOperation2,
                    MixedOperation.INSTANCE.mockOperation3,
                    MixedOperation.INSTANCE.mockOperation4);
        }

        @Test
        void shouldRunSetupIfMethodDoesNotHaveSkipAnnotation() throws Exception {
            // arrange
            Method method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(MixedOperation.INSTANCE).when(mockContext).getRequiredTestInstance();

            // act
            extension.beforeEach(mockContext);

            // assert
            verify(MixedOperation.mockDataSource).getConnection();

            verify(MixedOperation.mockOperation1).execute(eq(MixedOperation.mockDataSource.getConnection()), any());
            verify(MixedOperation.mockOperation2).execute(eq(MixedOperation.mockDataSource.getConnection()), any());
            verify(MixedOperation.INSTANCE.mockOperation3).execute(eq(MixedOperation.mockDataSource.getConnection()), any());
            verify(MixedOperation.INSTANCE.mockOperation4).execute(eq(MixedOperation.mockDataSource.getConnection()), any());
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

        private static DataSource mockDataSource = mock(DataSource.class);

        @DbSetupSourceFactory
        static DataSource getMockDataSource() {
            return mockDataSource;
        }

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);
    }

    static class StaticFieldFactory {
        static final StaticFieldFactory INSTANCE = new StaticFieldFactory();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);
    }

    static class InstanceMethodFactory {
        static final InstanceMethodFactory INSTANCE = new InstanceMethodFactory();

        private DataSource mockDataSource = mock(DataSource.class);

        @DbSetupSourceFactory
        DataSource getMockDataSource() {
            return mockDataSource;
        }

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);
    }

    static class InstanceFieldFactory {
        static final InstanceFieldFactory INSTANCE = new InstanceFieldFactory();

        @DbSetupSourceFactory
        private DataSource mockDataSource = mock(DataSource.class);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);
    }

    static class StaticMethodOperation {
        static final StaticMethodOperation INSTANCE = new StaticMethodOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        private static Operation mockOperation = mock(Operation.class);

        @DbSetupOperation
        private static Operation getOperation() {
            return mockOperation;
        }
    }

    static class StaticFieldOperation {
        static final StaticFieldOperation INSTANCE = new StaticFieldOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        @DbSetupOperation
        private static Operation mockOperation = mock(Operation.class);
    }

    static class InstanceMethodOperation {
        static final InstanceMethodOperation INSTANCE = new InstanceMethodOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        private Operation mockOperation = mock(Operation.class);

        @DbSetupOperation
        private Operation getOperation() {
            return mockOperation;
        }
    }

    static class InstanceFieldOperation {
        static final InstanceFieldOperation INSTANCE = new InstanceFieldOperation();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);
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
    }

    static class InnerOperations {
        static final InnerOperations INSTANCE = new InnerOperations();

        @DbSetupSourceFactory
        private static DataSource mockDataSource = mock(DataSource.class);

        @DbSetupOperation
        private Operation mockOperation = mock(Operation.class);

        class InnerClass {
            @DbSetupOperation
            private Operation mockOperation = mock(Operation.class);
        }
    }
}