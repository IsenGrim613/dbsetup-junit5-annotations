package com.iseninc.junit5.datasource;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.platform.commons.util.AnnotationUtils.*;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

public class DbSetupOperationExtension implements BeforeEachCallback {
    private static final Logger LOGGER = Logger.getLogger(DbSetupOperationExtension.class.getName());

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        System.out.println("bar");
        Method testMethod = context.getRequiredTestMethod();
        if (isAnnotated(testMethod, DbSetupSkip.class)) {
            LOGGER.log(Level.FINE, "Skipping db setup for {0}", testMethod.getName());
            return;
        }

        Optional<AnnotatedElement> elementOption = context.getElement();
        if (!elementOption.isPresent()) {
            throw new IllegalStateException("@DbSetupOperation should not be annotated on a test method");
        }

        // get operation
        AnnotatedElement operationElement = elementOption.get();
        Operation operation;
        if (operationElement instanceof Method) {
            Method method = (Method) operationElement;
            checkNoArgsMethod(method, Operation.class, "@DbSetupOperation");

            makeAccessible(method);
            operation = invokeMethod(method, context);
        }
        else if (operationElement instanceof Field) {
            Field field = (Field) operationElement;
            checkField(field, Operation.class, "@DbSetupOperation");

            makeAccessible(field);
            operation = getField(field, context);
        }
        else {
            throw new IllegalStateException("@DbSetupOperation should not be annotated on a " + operationElement.getClass());
        }

        // get data source
        List<AnnotatedElement> dbSetupSourceFactories = new ArrayList<>();
        dbSetupSourceFactories.addAll(
                findAnnotatedMethods(context.getRequiredTestClass(), DbSetupSourceFactory.class, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN));
        dbSetupSourceFactories.addAll(
                findAnnotatedFields(context.getRequiredTestClass(), DbSetupSourceFactory.class, f -> true, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN));


        if (dbSetupSourceFactories.size() > 1) {
            throw new IllegalStateException("There can only be 1 @DbSetupSourceFactory");
        }

        if (dbSetupSourceFactories.size() <= 0) {
            throw new IllegalStateException("No @DbSetupSourceFactory found");
        }

        AnnotatedElement dbSetupSourceFactory = dbSetupSourceFactories.get(0);
        DataSource dataSource;
        if (dbSetupSourceFactory instanceof Method) {
            Method method = (Method) dbSetupSourceFactory;
            checkNoArgsMethod(method, DataSource.class, "@DbSetupSourceFactory");

            makeAccessible(method);
            dataSource = invokeMethod(method, context);
        }
        else if (dbSetupSourceFactory instanceof Field) {
            Field field = (Field) dbSetupSourceFactory;
            checkField(field, DataSource.class, "@DbSetupSourceFactory");

            makeAccessible(field);
            dataSource = getField(field, context);
        }
        else {
            throw new IllegalStateException("@DbSetupSourceFactory should not be annotated on a " + operationElement.getClass());
        }

        DataSourceDestination dataSourceDestination = new DataSourceDestination(dataSource);
        DbSetup dbSetup = new DbSetup(dataSourceDestination, operation);
        dbSetup.launch();
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

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Method method, ExtensionContext context) throws Exception {
        if (isStatic(method)) {
            return (T) method.invoke(null);
        }
        else {
            return (T) method.invoke(context.getRequiredTestInstance());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Field field, ExtensionContext context) throws Exception {
        if (isStatic(field)) {
            return (T) field.get(null);
        }
        else {
            return (T) field.get(context.getRequiredTestInstance());
        }
    }
}
