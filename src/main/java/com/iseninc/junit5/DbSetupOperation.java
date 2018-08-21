package com.iseninc.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Todo: explain why only field (coz object meant to be reused, methods might create a new object every time)
 * also ordering between methods and fields cannot be done without introducing a "order" value (increases complexity)
 *
 * todo: add custom annotation processor to read implicit ordering
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface DbSetupOperation {
    int order() default -1;
}