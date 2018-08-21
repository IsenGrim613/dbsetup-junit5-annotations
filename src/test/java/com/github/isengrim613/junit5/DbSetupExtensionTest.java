package com.github.isengrim613.junit5;

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
import java.util.concurrent.atomic.AtomicReference;
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
            assertThat(t).isInstanceOf(IllegalArgumentException.class);
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
                    Arguments.of(StaticFieldFactory.class, StaticFieldFactory.INSTANCE),
                    Arguments.of(InstanceFieldFactory.class, InstanceFieldFactory.INSTANCE),
                    Arguments.of(StaticFieldOperation.class, StaticFieldOperation.INSTANCE),
                    Arguments.of(InstanceFieldOperation.class, InstanceFieldOperation.INSTANCE),
                    Arguments.of(MixedOperation.class, MixedOperation.INSTANCE),
                    Arguments.of(InnerOperations.class, InnerOperations.INSTANCE));
        }

        private Stream<Arguments> createInvalidCombinations() {
            return Stream.of(
                    Arguments.of(NoDataSource.class, NoDataSource.INSTANCE),
                    Arguments.of(WrongDataSource.class, WrongDataSource.INSTANCE),
                    Arguments.of(MultipleDataSources.class, MultipleDataSources.INSTANCE),
                    Arguments.of(NoOperations.class, NoOperations.INSTANCE),
                    Arguments.of(WrongOperation.class, WrongOperation.INSTANCE),
                    Arguments.of(NotOrderedOperations.class, NotOrderedOperations.INSTANCE));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class BeforeEachCallback {
        @Test
        void shouldNotRunNextSetupIfMethodHasSkipNextAnnotation() throws Exception {
            // arrange
            StaticFieldOperation.resetMocks();

            doReturn(StaticFieldOperation.class).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(StaticFieldOperation.INSTANCE, mockContext);

            Method method = Methods.class.getMethod("skipDbSetup");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(StaticFieldOperation.INSTANCE).when(mockContext).getRequiredTestInstance();

            extension.beforeEach(mockContext);
            StaticFieldOperation.verifyExecuted();

            // act
            StaticFieldOperation.resetMocks();

            method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();

            extension.beforeEach(mockContext);

            // assert
            verify(StaticFieldOperation.mockDataSource, never()).getConnection();
            verify(StaticFieldOperation.mockDataSource, never()).getConnection(any(), any());
            verify(StaticFieldOperation.mockOperation, never()).execute(any(), any());
        }

        @ParameterizedTest
        @MethodSource("createValidCombinations")
        void shouldRunSetupForValidCombinations(Class<?> clazz, Object instance, Runnable reset, Runnable verify) throws Exception {
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

        @Test
        void shouldPreserveOrderOfExplicitOrderedOperations() throws Exception {
            // arrange
            doReturn(ExplicitOrderedOperations.class).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(ExplicitOrderedOperations.INSTANCE, mockContext);

            Method method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(ExplicitOrderedOperations.INSTANCE).when(mockContext).getRequiredTestInstance();

            AtomicReference<String> order = new AtomicReference<>("");

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("1", String::concat);
                return null;
            }).when(ExplicitOrderedOperations.mockOperationForDelete).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("2", String::concat);
                return null;
            }).when(ExplicitOrderedOperations.mockOperationForInsert).execute(any(), any());

            // act
            extension.beforeEach(mockContext);

            // assert
            assertThat(order.get()).isEqualTo("12");
        }

        @Test
        void shouldPreserveOrderOfImplicitOrderedOperations() throws Exception {
            // arrange
            doReturn(ImplicitOrderedOperations.class).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(ImplicitOrderedOperations.INSTANCE, mockContext);

            Method method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(ImplicitOrderedOperations.INSTANCE).when(mockContext).getRequiredTestInstance();

            AtomicReference<String> order = new AtomicReference<>("");

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("1", String::concat);
                return null;
            }).when(ImplicitOrderedOperations.mockOperation0).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("2", String::concat);
                return null;
            }).when(ImplicitOrderedOperations.mockOperation1).execute(any(), any());

            // act
            extension.beforeEach(mockContext);

            // assert
            assertThat(order.get()).isEqualTo("12");
        }

        @Test
        void shouldPreserveOrderOfMultiLevelOrderedOperations() throws Exception {
            // arrange
            doReturn(MultiLevelOrderedOperations.InnerClassMultiLevel.class).when(mockContext).getRequiredTestClass();
            extension.postProcessTestInstance(MultiLevelOrderedOperations.INNER_INSTANCE, mockContext);

            Method method = Methods.class.getMethod("normalTest");
            doReturn(method).when(mockContext).getRequiredTestMethod();
            doReturn(MultiLevelOrderedOperations.INNER_INSTANCE).when(mockContext).getRequiredTestInstance();

            AtomicReference<String> order = new AtomicReference<>("");

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("0", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.mockOperation0).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("1", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.InnerClassMultiLevel.mockOperation1).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("2", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.INNER_INSTANCE.outer().mockOperation2).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("3", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.INNER_INSTANCE.outer().mockOperation3).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("4", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.mockOperation4).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("6", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.INNER_INSTANCE.mockOperation6).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("7", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.INNER_INSTANCE.mockOperation7).execute(any(), any());

            doAnswer(invocationOnMock -> {
                order.accumulateAndGet("8", String::concat);
                return null;
            }).when(MultiLevelOrderedOperations.INNER_INSTANCE.mockOperation8).execute(any(), any());

            // act
            extension.beforeEach(mockContext);

            // assert
            assertThat(order.get()).isEqualTo("01234678");
        }

        private Stream<Arguments> createValidCombinations() {
            return Stream.of(
                    createArguments(StaticFieldFactory.class),
                    createArguments(InstanceFieldFactory.class),
                    createArguments(StaticFieldOperation.class),
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
        @DbSetupSkipNext
        public void skipDbSetup() {

        }

        public void normalTest() {

        }
    }

    static class NoDataSource {
        static final NoDataSource INSTANCE = new NoDataSource();
    }

    static class WrongDataSource {
        static final WrongDataSource INSTANCE = new WrongDataSource();

        @DbSetupSource
        private static String mockDataSource = "abcd1234";
    }

    static class MultipleDataSources {
        static final MultipleDataSources INSTANCE = new MultipleDataSources();

        @DbSetupSource
        private static DataSource mockDataSource1 = mock(DataSource.class);

        @DbSetupSource
        private static DataSource mockDataSource2 = mock(DataSource.class);
    }

    static class NoOperations {
        static final NoOperations INSTANCE = new NoOperations();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class);
    }

    static class WrongOperation {
        static final WrongOperation INSTANCE = new WrongOperation();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static String mockOperation = "abcd1234";
    }

    static class StaticFieldFactory {
        static final StaticFieldFactory INSTANCE = new StaticFieldFactory();

        @DbSetupSource
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

    static class InstanceFieldFactory {
        static final InstanceFieldFactory INSTANCE = new InstanceFieldFactory();

        @DbSetupSource
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

    static class StaticFieldOperation {
        static final StaticFieldOperation INSTANCE = new StaticFieldOperation();

        @DbSetupSource
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

    static class InstanceFieldOperation {
        static final InstanceFieldOperation INSTANCE = new InstanceFieldOperation();

        @DbSetupSource
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

    static class MixedOperation {
        static final MixedOperation INSTANCE = new MixedOperation();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation1 = mock(Operation.class);

        @DbSetupOperation
        private Operation mockOperation2 = mock(Operation.class);

        static void resetMocks() {
            reset(mockDataSource, mockOperation1, INSTANCE.mockOperation2);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockDataSource, mockOperation1, INSTANCE.mockOperation2);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(mockOperation1).execute(eq(mockDataSource.getConnection()), any());
                verify(INSTANCE.mockOperation2).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class InnerOperations {
        static final InnerOperations INSTANCE = new InnerOperations();
        static final InnerOperations.InnerClass INNER_INSTANCE = INSTANCE.new InnerClass();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation1 = mock(Operation.class);

        class InnerClass {
            @DbSetupOperation
            private Operation mockOperation2 = mock(Operation.class);
        }

        static void resetMocks() {
            reset(mockDataSource, mockOperation1, INNER_INSTANCE.mockOperation2);
        }

        static void verifyNotExecuted() {
            verifyZeroInteractions(mockOperation1, INNER_INSTANCE.mockOperation2);
        }

        static void verifyExecuted() {
            try {
                verify(mockDataSource).getConnection();
                verify(mockOperation1).execute(eq(mockDataSource.getConnection()), any());
                verify(INNER_INSTANCE.mockOperation2).execute(eq(mockDataSource.getConnection()), any());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class NotOrderedOperations {
        static final NotOrderedOperations INSTANCE = new NotOrderedOperations();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation(order = 1)
        private static Operation mockOperationForInsert = mock(Operation.class);

        @DbSetupOperation
        private static Operation mockOperationForDelete = mock(Operation.class);
    }

    static class ExplicitOrderedOperations {
        static final ExplicitOrderedOperations INSTANCE = new ExplicitOrderedOperations();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation(order = 1)
        private static Operation mockOperationForInsert = mock(Operation.class);

        @DbSetupOperation(order = 0)
        private static Operation mockOperationForDelete = mock(Operation.class);
    }

    static class ImplicitOrderedOperations {
        static final ImplicitOrderedOperations INSTANCE = new ImplicitOrderedOperations();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        private static Operation mockOperation1 = mock(Operation.class);

        @DbSetupOperation
        private static Operation mockOperation0 = mock(Operation.class);
    }

    static abstract class MultiLevelOrderedOperationsParent {
        @DbSetupOperation(order = 0)
        protected static Operation mockOperation0 = mock(Operation.class);

        @DbSetupOperation(order = 2)
        protected Operation mockOperation2 = mock(Operation.class);
    }

    static abstract class MultiLevelOrderedOperationsInnerParent {
        @DbSetupOperation(order = 1)
        protected static Operation mockOperation1 = mock(Operation.class);

        @DbSetupOperation(order = 6)
        protected Operation mockOperation6 = mock(Operation.class);
    }

    static class MultiLevelOrderedOperations extends MultiLevelOrderedOperationsParent {
        static final MultiLevelOrderedOperations INSTANCE = new MultiLevelOrderedOperations();
        static final InnerClassMultiLevel INNER_INSTANCE = INSTANCE.new InnerClassMultiLevel();

        @DbSetupSource
        private static DataSource mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation(order = 4)
        private static Operation mockOperation4 = mock(Operation.class);

        @DbSetupOperation(order = 3)
        private Operation mockOperation3 = mock(Operation.class);

        class InnerClassMultiLevel extends MultiLevelOrderedOperationsInnerParent {
            @DbSetupOperation(order = 7)
            private Operation mockOperation7 = mock(Operation.class);

            @DbSetupOperation(order = 8)
            private Operation mockOperation8 = mock(Operation.class);

            private MultiLevelOrderedOperations outer() {
                return MultiLevelOrderedOperations.this;
            }
        }
    }
}