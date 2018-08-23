package com.github.isengrim613.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation tells @DbSetup which variable is the {@link javax.sql.DataSource} to perform operations on.
 *
 * <p>There can only be 1 DataSource and the field type must be {@link javax.sql.DataSource}.
 *
 * <p>This annotation is disallowed on a method because it allows erroneous code to be written. Consider,
 * <pre>
 *     &#064;DbSetupSource
 *     DataSource dataSource() {
 *         return new DataSource() {
 *             ...
 *         };
 *     }
 * </pre>
 *
 * <p>A new DataSource will be instantiated on every call to this method, and operations running on these
 * ephemeral instances will simply be lost. This is not possible for field,
 * <pre>
 *     &#064;DbSetupSource
 *     DataSource dataSource = new DataSource() { ... };
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface DbSetupSource {
}
