package com.github.isengrim613.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Fields annotated will be gathered up and launched in sequence against the {@link DbSetupSource} DataSource.
 *
 * <p>The field type must be {@link com.ninja_squad.dbsetup.operation.Operation} and there can be many operations.
 *
 * <p>Operations must be ordered implicitly or explicitly.
 *
 * <h3>Implicit ordering</h3>
 * <p>The field name must be post fixed by an integer. The integer can be of any length and the value will be used as
 * the operation's order. Only integers will be considered.
 *
 * <p>Consider,
 * <pre>
 *     &#064;DbSetupOperation
 *     Operation delete1 = deleteFrom("my_table");
 * </pre>
 *
 * In this case, this delete operation will be tagged as order 1.
 *
 * <h3>Explicit ordering</h3>
 * <p>The order of the field is explicitly defined in the {@link DbSetupOperation#order()} annotation value.
 *
 * Consider,
 * <pre>
 *     &#064;DbSetupOperation(order = 2)
 *     Operation deleteMyTable = deleteFrom("my_table");
 * </pre>
 *
 * <p>In this case, this delete operation will be tagged as order 2.
 *
 * <p>In the case where both implicit and explicit orders are defined, explicit order will take precedence. If neither
 * are defined, a runtime exception will be thrown when parsing for the operations.
 *
 * <p>Only fields are allowed because of the same reason why only fields are allowed for {@link DbSetupSource}.
 * Although this is less of an issue for operations as they are only used once, it is still good programming practice
 * to make sure the operation is only instantiated once.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface DbSetupOperation {
    /**
     * This defines the explicit order of the annotated operation.
     *
     * <p>When this is set to less than 0, @DbSetup will fallback to use implicit ordering. See
     * {@link DbSetupOperation} to get the details on implicit and explicit ordering.
     *
     * @return The order of the annotated operation
     */
    int order() default -1;
}