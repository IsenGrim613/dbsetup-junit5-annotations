package com.iseninc.junit5;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

public class DbSetupExtension implements TestInstancePostProcessor, BeforeEachCallback {
    private static final Logger LOGGER = Logger.getLogger(DbSetupExtension.class.getName());

    private Field dataSourceDestinationField;
    private List<Field> operationFields;
    private DbSetupTracker dbSetupTracker = new DbSetupTracker();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        dataSourceDestinationField = findDataSourceField(context);
        operationFields = findOperationFields(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        DataSource dataSource = getFieldValue(dataSourceDestinationField, testInstance);
        List<Operation> operations = new ArrayList<>();
        for (Field field : operationFields) {
            operations.add(getFieldValue(field, testInstance));
        }

        DataSourceDestination dataSourceDestination = new DataSourceDestination(dataSource);
        DbSetup dbSetup = new DbSetup(dataSourceDestination, sequenceOf(operations));
        dbSetupTracker.launchIfNecessary(dbSetup);

        Method testMethod = context.getRequiredTestMethod();
        if (isAnnotated(testMethod, DbSetupSkipNext.class)) {
            LOGGER.log(Level.FINE, "Skipping next db setup for {0}", testMethod.getName());
            dbSetupTracker.skipNextLaunch();
        }
    }

    private static Field findDataSourceField(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        List<Field> dbSetupSources = findAnnotatedFieldsInHierarchy(testClass, DbSetupSource.class);

        if (dbSetupSources.size() <= 0) {
            throw new IllegalStateException("No @DbSetupSource found");
        }

        if (dbSetupSources.size() > 1) {
            throw new IllegalStateException("There can only be 1 @DbSetupSource");
        }

        Field field = dbSetupSources.get(0);
        makeAccessible(field);
        checkField(field, DataSource.class, "@DbSetupSource");

        return field;
    }

    private static List<Field> findOperationFields(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        List<Field> dbSetupOperationElements = findAnnotatedFieldsInHierarchy(testClass, DbSetupOperation.class);

        if (dbSetupOperationElements.size() <= 0) {
            throw new IllegalStateException("No @DbSetupOperation found");
        }

        for (Field field : dbSetupOperationElements) {
            makeAccessible(field);
            checkField(field, Operation.class, "@DbSetupOperation");
        }

        dbSetupOperationElements.sort(Comparator.comparingInt(DbSetupExtension::getOperationOrder));

        return dbSetupOperationElements;
    }

    private static void checkField(Field field, Class<?> returnType, String name) {
        if (!returnType.isAssignableFrom(field.getType())) {
            throw new IllegalStateException(name + " should return an instance or subclass of " + returnType);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Field field, Object instance) throws Exception {
        if (isStatic(field)) {
            return (T) field.get(null);
        }
        else {
            instance = matchElementDeclaringClass(field.getDeclaringClass(), instance);
            return (T) field.get(instance);
        }
    }

    private static List<Field> findAnnotatedFieldsInHierarchy(Class<?> clazz, Class<? extends Annotation> annotationType) {
        List<Field> fields = new ArrayList<>();

        if (clazz.getDeclaringClass() != null) {
            fields.addAll(findAnnotatedFieldsInHierarchy(clazz.getDeclaringClass(), annotationType));
        }

        fields.addAll(findAnnotatedFields(clazz, annotationType, f -> true, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN));
        return fields;
    }

    private static Object matchElementDeclaringClass(Class<?> elementDeclaringClass, Object instance) {
        //while (elementDeclaringClass != instance.getClass()) {
        while (!elementDeclaringClass.isAssignableFrom(instance.getClass())) {
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

    private static int getOperationOrder(Field field) {
        DbSetupOperation dbSetupOperation = field.getAnnotation(DbSetupOperation.class);
        int order = dbSetupOperation.order();

        if (order < 0) {
            String fieldName = field.getName();
            int lastInteger = getLastInt(fieldName);
            if (lastInteger < 0) {
                throw new IllegalArgumentException("No order specified and implicit order cannot be determined by " +
                        "inspecting the field name");
            }
            else {
                order = lastInteger;
            }
        }

        return order;
    }

    private static int getLastInt(String line) {
        int offset = line.length();
        for (int i = line.length() - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (Character.isDigit(c)) {
                offset--;
            }
            else {
                if (offset == line.length()) {
                    // No int at the end
                    return -1;
                }
                return Integer.parseInt(line.substring(offset));
            }
        }
        return Integer.parseInt(line.substring(offset));
    }
}
