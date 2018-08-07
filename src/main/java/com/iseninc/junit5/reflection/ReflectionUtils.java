package com.iseninc.junit5.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {
    private ReflectionUtils() {
        // singleton
    }

    public static List<Method> getMethodsWithAnnotated(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();

        while (type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }

            // move to the upper class in the hierarchy in search for more methods
            type = type.getSuperclass();
        }

        return methods;
    }

    public static boolean isMethodAnnotatedWith(Method method, Class<? extends Annotation> annotation) {
        return method.getAnnotation(annotation) != null;
    }

    public static boolean isMethodStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }
}
