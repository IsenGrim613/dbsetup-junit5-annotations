package com.iseninc.junit5.datasource;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.platform.commons.util.AnnotationUtils.*;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

public class DbSetupExtension implements TestInstancePostProcessor, BeforeEachCallback {
    private static final Logger LOGGER = Logger.getLogger(DbSetupExtension.class.getName());

    private AnnotatedElement dataSourceElement;
    private List<AnnotatedElement> operationElements;

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        dataSourceElement = findDataSourceElement(context);
        operationElements = findOperationElements(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Method testMethod = context.getRequiredTestMethod();
        if (isAnnotated(testMethod, DbSetupSkip.class)) {
            LOGGER.log(Level.FINE, "Skipping db setup for {0}", testMethod.getName());
            return;
        }

        Object testInstance = context.getRequiredTestInstance();
        DataSource dataSource = invoke(dataSourceElement, testInstance);
        List<Operation> operations = new ArrayList<>();
        for (AnnotatedElement element : operationElements) {
            operations.add(invoke(element, testInstance));
        }

        DataSourceDestination dataSourceDestination = new DataSourceDestination(dataSource);
        DbSetup dbSetup = new DbSetup(dataSourceDestination, sequenceOf(operations));
        dbSetup.launch();
    }

    private static AnnotatedElement findDataSourceElement(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        List<AnnotatedElement> dbSetupSourceFactories = findAnnotatedElements(testClass, DbSetupSourceFactory.class);

        if (dbSetupSourceFactories.size() <= 0) {
            throw new IllegalStateException("No @DbSetupSourceFactory found");
        }

        if (dbSetupSourceFactories.size() > 1) {
            throw new IllegalStateException("There can only be 1 @DbSetupSourceFactory");
        }

        AnnotatedElement element = dbSetupSourceFactories.get(0);
        if (element instanceof Method) {
            Method method = (Method) element;
            makeAccessible(method);
            checkNoArgsMethod(method, DataSource.class, "@DbSetupSourceFactory");
        }
        else if (element instanceof Field) {
            Field field = (Field) element;
            makeAccessible(field);
            checkField(field, DataSource.class, "@DbSetupSourceFactory");
        }
        else {
            throw new IllegalStateException("@DbSetupOperation should not be annotated on a " + element.getClass());
        }

        return element;
    }

    private static List<AnnotatedElement> findOperationElements(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        List<AnnotatedElement> dbSetupOperationElements = findAnnotatedElements(testClass, DbSetupOperation.class);

        if (dbSetupOperationElements.size() <= 0) {
            throw new IllegalStateException("No @DbSetupOperation found");
        }

        for (AnnotatedElement element : dbSetupOperationElements) {
            if (element instanceof Method) {
                Method method = (Method) element;
                makeAccessible(method);
                checkNoArgsMethod(method, Operation.class, "@DbSetupOperation");
            }
            else if (element instanceof Field) {
                Field field = (Field) element;
                makeAccessible(field);
                checkField(field, Operation.class, "@DbSetupOperation");
            }
            else {
                throw new IllegalStateException("@DbSetupOperation should not be annotated on a " + element.getClass());
            }
        }

        return dbSetupOperationElements;
    }

    private static void checkNoArgsMethod(Method method, Class<?> returnType, String name) {
        if (!returnType.isAssignableFrom(method.getReturnType())) {
            throw new IllegalStateException(name + " should return an instance or subclass of " + returnType);
        }

        if (method.getParameterCount() > 0) {
            throw new IllegalStateException(name + " should have 0 parameters");
        }
    }

    private static void checkField(Field field, Class<?> returnType, String name) {
        if (!returnType.isAssignableFrom(field.getType())) {
            throw new IllegalStateException(name + " should return an instance or subclass of " + returnType);
        }
    }

    private static <T> T invoke(AnnotatedElement element, Object instance) throws Exception {
        if (element instanceof Method) {
            return invokeMethod((Method) element, instance);
        }
        else if (element instanceof Field) {
            return getField((Field) element, instance);
        }
        else {
            throw new IllegalStateException("Element type not supported: " + element.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Method method, Object instance) throws Exception {
        if (isStatic(method)) {
            return (T) method.invoke(null);
        }
        else {
            instance = matchElementDeclaringClass(method.getDeclaringClass(), instance);
            return (T) method.invoke(instance);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Field field, Object instance) throws Exception {
        if (isStatic(field)) {
            return (T) field.get(null);
        }
        else {
            instance = matchElementDeclaringClass(field.getDeclaringClass(), instance);
            return (T) field.get(instance);
        }
    }

    private static List<AnnotatedElement> findAnnotatedElements(Class<?> clazz, Class<? extends Annotation> annotationType) {
        List<AnnotatedElement> elements = new ArrayList<>();

        if (clazz.getDeclaringClass() != null) {
            elements.addAll(findAnnotatedElements(clazz.getDeclaringClass(), annotationType));
        }

        elements.addAll(findAnnotatedMethods(clazz, annotationType, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN));
        elements.addAll(findAnnotatedFields(clazz, annotationType, f -> true, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN));

        return elements;
    }

    private static Object matchElementDeclaringClass(Class<?> elementDeclaringClass, Object instance) {
        while (elementDeclaringClass != instance.getClass()) {
            Optional<Object> outerOption = getOuterInstance(instance);
            if (!outerOption.isPresent()) {
                throw new IllegalStateException("Cannot map outer instance to outer methods found");
            }

            instance = outerOption.get();
        }

        return instance;
    }

    private static Optional<Object> getOuterInstance(Object inner) {
        // This is risky since it depends on the name of the field which is nowhere guaranteed
        // but has been stable so far in all JDKs

        return Arrays.stream(inner.getClass().getDeclaredFields())
                .filter(field -> field.getName().startsWith("this$"))
                .findFirst()
                .map(field -> {
                    try {
                        return makeAccessible(field).get(inner);
                    }
                    catch (Throwable t) {
                        throw ExceptionUtils.throwAsUncheckedException(t);
                    }
                });
    }
}
