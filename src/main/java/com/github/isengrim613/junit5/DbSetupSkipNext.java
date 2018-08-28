package com.github.isengrim613.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a test does not modify the DataSource in anyway, this annotation can be added to the test method to tell
 * {@link DbSetup} not to launch for the next test.
 *
 * <p>This will skip all launches for all data sources if there are multiple.
 *
 * <p>This annotation is not necessary and is only for improving performance.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DbSetupSkipNext {
}
