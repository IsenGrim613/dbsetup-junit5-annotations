package com.iseninc.junit5.datasource;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.iseninc.junit5.reflection.ReflectionUtils.*;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;

public class DbSetupExtension implements BeforeAllCallback, BeforeEachCallback {
    private static final Logger LOGGER = Logger.getLogger(DbSetupExtension.class.getName());

    private Callable<DataSource> dataSourceSupplier;
    private List<Callable<Operation>> operationSuppliers;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Optional<Class<?>> classHolder = context.getTestClass();
        if (!classHolder.isPresent()) {
            return;
        }

        Class<?> testClass = classHolder.get();
        dataSourceSupplier = getDataSourceSupplier(testClass);
        operationSuppliers = getOperationSuppliers(testClass);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (dataSourceSupplier == null || operationSuppliers == null) {
            return;
        }

        Optional<Method> methodHolder = context.getTestMethod();
        if (!methodHolder.isPresent()) {
            return;
        }

        Method testMethod = methodHolder.get();

        if (isMethodAnnotatedWith(testMethod, DbSetupSkip.class)) {
            LOGGER.log(Level.FINE, "Skipping db setup for {0}", testMethod.getName());
            return;
        }

        DataSourceDestination dataSourceDestination = new DataSourceDestination(dataSourceSupplier.call());
        List<Operation> operations = new ArrayList<>();
        for (Callable<Operation> operationSupplier : operationSuppliers) {
            operations.add(operationSupplier.call());
        }

        DbSetup dbSetup = new DbSetup(dataSourceDestination, sequenceOf(operations));
        dbSetup.launch();
    }

    private static Callable<DataSource> getDataSourceSupplier(Class<?> testClass) {
        List<Method> dbSetupSourceFactories = getMethodsWithAnnotated(testClass, DbSetupSourceFactory.class);
        if (dbSetupSourceFactories.size() > 1) {
            throw new IllegalStateException("There can only be 1 @DbSetupSourceFactory");
        }

        if (dbSetupSourceFactories.size() <= 0) {
            LOGGER.warning("No @DbSetupSourceFactory found. Skipping DbSetup");
            return null;
        }

        Method dbSetupSourceFactory = dbSetupSourceFactories.get(0);

        if (!isMethodStatic(dbSetupSourceFactory)) {
            throw new IllegalStateException("@DbSetupSourceFactory must be static");
        }

        if (dbSetupSourceFactory.getParameterCount() > 0) {
            throw new IllegalStateException("@DbSetupSourceFactory must have no parameters");
        }

        if (!DataSource.class.isAssignableFrom(dbSetupSourceFactory.getReturnType())) {
            throw new IllegalStateException("@DbSetupSourceFactory must return an instance or subclass of javax.sql.DataSource");
        }

        dbSetupSourceFactory.setAccessible(true);
        return () -> (DataSource) dbSetupSourceFactory.invoke(null);
    }

    private static List<Callable<Operation>> getOperationSuppliers(Class<?> testClass) {
        List<Method> dbSetupOperations = getMethodsWithAnnotated(testClass, DbSetupOperation.class);
        if (dbSetupOperations.size() <= 0) {
            LOGGER.warning("No @DbSetupOperation found. Skipping DbSetup");
            return null;
        }

        List<Callable<Operation>> operationSuppliers = new ArrayList<>();

        for (Method dbSetupOperation : dbSetupOperations) {
            if (!isMethodStatic(dbSetupOperation)) {
                throw new IllegalStateException(String.format("@DbSetupOperation[%s] must be static", dbSetupOperation.getName()));
            }

            if (dbSetupOperation.getParameterCount() > 0) {
                throw new IllegalStateException(String.format("@DbSetupOperation[%s] must have no parameters", dbSetupOperation.getName()));
            }

            if (!Operation.class.isAssignableFrom(dbSetupOperation.getReturnType())) {
                throw new IllegalStateException(String.format("@DbSetupOperation[%s] must return an instance or " +
                        "subclass of com.ninja_squad.dbsetup.operation.Operation", dbSetupOperation.getName()));
            }

            dbSetupOperation.setAccessible(true);
            operationSuppliers.add(() -> (Operation) dbSetupOperation.invoke(null));
        }

        return operationSuppliers;
    }
}
