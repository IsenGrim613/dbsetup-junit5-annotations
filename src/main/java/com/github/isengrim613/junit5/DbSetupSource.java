package com.github.isengrim613.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation tells @DbSetup which variable is the {@link javax.sql.DataSource} to perform operations on.
 *
 * <p>The field type must be {@link javax.sql.DataSource}.
 *
 * <p>There can be multiple DataSources but they must all be uniquely named using {@link #name()}
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
    /**
     * Name of the data source for operations to attach to.
     *
     * <p>When there is a need for multiple data sources, a name can be attached to each data source and {@link DbSetupOperation}s
     * can be referenced to launch on these data sources.
     *
     * <p>Everyone of these data sources must be uniquely named. Duplicate naming will result in exception being thrown
     * during parsing. Names will be case-sensitive.
     *
     * <p>When there is only 1 data source, this parameter can be omitted as it will be automatically named "DEFAULT".
     * This means that other data sources cannot be named "DEFAULT" if there is already an unnamed data source.
     *
     * @return The name of the data source
     */
    String name() default "DEFAULT";
}
