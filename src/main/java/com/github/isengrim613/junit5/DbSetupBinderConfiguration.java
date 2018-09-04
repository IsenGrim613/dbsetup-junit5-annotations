package com.github.isengrim613.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation tells @DbSetup the binder configuration to apply.
 *
 * <p>This annotation is optional. If none is supplied {@link com.ninja_squad.dbsetup.bind.DefaultBinderConfiguration}
 * will be used.
 *
 * <p>The field type must be an implementation of {@link com.ninja_squad.dbsetup.bind.BinderConfiguration} and there
 * can only be 1 configuration per {@link DbSetupSource}.
 *
 * <p>If there are multiple data sources, the {@link #sources()} field can be used to define which data source this
 * operation will be launched on.
 *
 * <p>Only fields are allowed because of the same reason why only fields are allowed for {@link DbSetupSource}.
 * Although this is less of an issue for operations as they are only used once, it is still good programming practice
 * to make sure the operation is only instantiated once.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface DbSetupBinderConfiguration {
    /**
     * This parameter defines the {@link DbSetupSource}s that this configuration will be applied to.
     *
     * <p>The data sources will be referenced by {@link DbSetupSource#name()}. If a unknown data source name is
     * referenced, an exception will be thrown during parsing. Declaring duplicated data source names will be resolved
     * into 1 reference.
     *
     * <p>If there is only 1 data source, this parameter can be left blank. It's default value already includes the
     * default data source.
     *
     * @return DataSource names that this configuration will be applied to
     */
    String[] sources() default { "DEFAULT" };
}
