/**
 *
 */
package com.xceptance.loadtest.api.configuration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is for a list of similar properties defined as enumeration.
 *
 * @author Rene Schwietzke
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumProperty
{
    /**
     * The class to build the property for
     */
    Class<?> clazz();

    /**
     * Disables the not defined complain and therefore permits empty and
     * initialized properties. Use with caution!
     *
     * @return true when no assertion on missing key-value is desired, false by
     *         default
     */
    boolean required() default true;

    /**
     * We permit holes in the list, so we do not complain in case an element is
     * missing. If you set stopOnGap, this parameter does not matter.
     *
     * @return true if we accept missing list elements
     */
    boolean permitMissing() default true;

    /**
     * Signals that the property should not change when queried. This disables
     * the randomness on access. Default is true to avoid strange site effects
     * when using.
     *
     * @return false when the property should be re-evaluated on access and
     *         create a new random value, true as default
     */
    boolean immutable() default true;

    /**
     * Enables the lookup in the read list by id. The id is determined by the implemented interface
     * ID, so you can basically make everything an id, it does not have to be named id
     */
    boolean byId() default false;

    /**
     * Defines the start value of the range, 0 as default
     *
     * @return the value of the range start, 0 if not defined
     */
    int from() default 0;

    /**
     * Defines the end value (inclusive) of the range, 1000 as default
     *
     * @return the value of the to of the range inclusive, 1000 if not defined
     */
    int to() default 1000;

    /**
     * The key to the property. Is is mandatory to be specified. Keep in mind it
     * will be (if not turned off) autocompleted to match the type of the field.
     *
     * @return the key name of the property.
     */
    String key();

    /**
     * If all values of the range should be probed or we shall stop if we do not
     * find a value
     *
     * @return true if we stop on a gap, false otherwise
     */
    boolean stopOnGap() default true;

    /**
     * If the list should be compacted in case we read all and accepted gaps.
     * Defaults to true
     *
     * @return true if gaps are ok in the final list, false otherwise
     */
    boolean compact() default true;
}
