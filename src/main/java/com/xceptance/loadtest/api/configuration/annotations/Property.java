/**
 *
 */
package com.xceptance.loadtest.api.configuration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.xceptance.loadtest.api.configuration.ConfigConstants;

/**
 * This annotation defines what the source of a property is.
 *
 * @author Rene Schwietzke
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property
{
    /**
     * Disables the not defined complain and therefore permits empty and
     * initialized properties. Use with caution!
     *
     * @return true when no assertion on missing key-value is desired, false by
     *         default
     */
    boolean required() default true;

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
     * Property keys are extended by default to allow easier and more consistent access and
     * definition. Can be turned off optionally.
     *
     * @return false if auto complete is not desired, true by default
     */
    boolean autocomplete() default true;

    /**
     * The key to the property. Is is mandatory to be specified. Keep in mind it
     * will be (if not turned off) autocompleted to match the type of the field.
     *
     * @return the key name of the property.
     */
    String key();

    /**
     * A list of valid delimiters for list type properties. Will do nothing if
     * specified for non list attributes. Single character delimiters only.
     *
     * @return the delimiters desired, space is default
     */
    String delimiters() default " ";

    /**
     * The fallback value if the property is not specified. By default, we
     * complain if no property is set and not property is specified, unless you
     * turned it off with {@code required = false}.
     *
     * @return the fallback value or an empty string by default
     */
    String fallback() default ConfigConstants.EMPTY;
}
