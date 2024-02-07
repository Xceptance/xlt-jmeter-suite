package com.xceptance.loadtest.api.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;

import com.xceptance.common.util.RegExUtils;
import com.xceptance.loadtest.api.configuration.annotations.EnumProperty;
import com.xceptance.loadtest.api.configuration.annotations.Property;
import com.xceptance.loadtest.api.configuration.interfaces.Initable;
import com.xceptance.xlt.api.util.XltProperties;

/**
 * Process a class with configuration annotation and return a fresh instance of it.
 *
 * @author Rene Schwietzke
 */
public class ConfigurationBuilder
{
    /**
     * The property source
     */
    private final LTProperties propertyLookup;

    /**
     * Create a new configuration builder that uses certain properties
     *
     * @param properties
     *            the properties to read from via a lookup
     */
    public ConfigurationBuilder(final LTProperties properties)
    {
        this.propertyLookup = properties;
    }

    /**
     * Create a new builder and use the default property lookup This is meant for testing.
     *
     * @param clazz
     *            the class to build the config on
     * @return the fully setup class
     */
    public static <T> T buildDefault(final Class<T> clazz)
    {
        final LTProperties properties = new LTProperties("", "", "");
        properties.addProperties(Optional.of(XltProperties.getInstance().getProperties()));

        return new ConfigurationBuilder(properties).build(clazz);
    }

    /**
     * Returns a new configuration based with all annotated values set
     *
     * @param prefix
     *            a property prefix in case we nest things
     * @param clazz
     *            the class to build the config on
     * @return the fully setup class
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     */
    public <T> T build(final Class<T> clazz)
    {
        try
        {
            @SuppressWarnings("unchecked")
            final Constructor<T>[] ctors = (Constructor<T>[]) clazz.getDeclaredConstructors();

            Constructor<T> ctor = null;
            for (int i = 0; i < ctors.length; i++)
            {
                ctor = ctors[i];
                if (ctor.getGenericParameterTypes().length == 0)
                {
                    break;
                }
            }

            ctor.setAccessible(true);
            final T instance = ctor.newInstance();

            // ok, get me all fields
            final Field[] fields = instance.getClass().getDeclaredFields();

            // ok, check all fields for our annotation
            for (final Field field : fields)
            {
                final Property annotation = field.getAnnotation(Property.class);
                if (annotation != null)
                {
                    // get access
                    field.setAccessible(true);

                    // cool, we got something
                    final Class<?> type = field.getType();

                    // if string
                    if (type == String.class)
                    {
                        field.set(instance, initializeString(annotation));
                    }
                    else if (type == int.class)
                    {
                        field.setInt(instance, initializeInt(annotation));
                    }
                    else if (type == boolean.class)
                    {
                        field.setBoolean(instance, initializeBoolean(annotation));
                    }
                    else if (type == ConfigList.class)
                    {
                        field.set(instance, initializeConfigList(annotation));
                    }
                    else if (type == ConfigRange.class)
                    {
                        field.set(instance, initializeConfigRange(annotation));
                    }
                    else if (type == ConfigTimeRange.class)
                    {
                        field.set(instance, initializeConfigTimeRange(annotation));
                    }
                    else if (type == ConfigProbability.class)
                    {
                        field.set(instance, initializeConfigProbability(annotation));
                    }
                    else if (type == ConfigDistribution.class)
                    {
                        field.set(instance, initializeConfigDistribution(annotation));
                    }
                    else if (type == Pattern.class)
                    {
                        field.set(instance, initializePattern(annotation));
                    }
                    else
                    {
                        // ok, deal with the rest aka all kind of custom classes we might
                        // property-up
                        field.set(instance, handleNestedPropertyClazz(annotation, type));
                    }
                }
                else
                {
                    // do we have our other type=?
                    final EnumProperty enumAnnotation = field.getAnnotation(EnumProperty.class);
                    if (enumAnnotation != null)
                    {
                        // yeah, we got an enum property, see if the type fits
                        // get access
                        field.setAccessible(true);

                        // cool, we got something
                        final Class<?> type = field.getType();

                        // if string
                        if (type == EnumConfigList.class)
                        {
                            field.set(instance, initializeEnumConfigList(enumAnnotation));
                        }
                        else
                        {
                            // well, we do not know it, so complain
                            throw new IllegalArgumentException(MessageFormat.format("Annotation type for '{1}' not supported: {0}", enumAnnotation.toString(), field.getName()));
                        }
                    }
                }
            }

            return instance;
        }
        catch (final IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e)
        {
            throw new RuntimeException(
                            MessageFormat.format(
                                            "Could not initialize {0} due to {1} - {2}",
                                            clazz.toString(),
                                            e.getClass().getSimpleName(),
                                            e.getMessage()),
                            e);
        }

    }

    /**
     * Processes enum config property
     *
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private EnumConfigList<?> initializeEnumConfigList(final EnumProperty annotation)
    {
        // Weighted values, while the Object is the value and the Integer it's weight
        final ArrayList<Pair<Object, Integer>> list = new ArrayList<>();

        // ok, loop
        AssertionError firstError = null;

        for (int i = annotation.from(); i <= annotation.to(); i++ )
        {
            Pair<Object, Integer> o = null;

            if (!annotation.stopOnGap() && !annotation.permitMissing())
            {
                // run very strict because neither we want to stop on hole
                // neither accept any holes
                o = handleEnumPropertyClazz(annotation, i);
            }
            else
            {
                // run relaxed and complain only about empty list later
                try
                {
                    o = handleEnumPropertyClazz(annotation, i);
                }
                catch (final AssertionError error)
                {
                    firstError = firstError == null ? error : firstError;
                }
            }

            // if we got something, add it
            if (o != null && o.getLeft() != null)
            {
                // shall we init it?
                if (o.getLeft() instanceof Initable)
                {
                    // up to you what to do here... if you fail, we fail completely
                    ((Initable) o.getLeft()).init();
                }

                // ok, keep it
                if (StringUtils.isNotBlank(o.getLeft().toString()))
                {
                    list.add(o);
                }

                // ok, continue happily
                continue;
            }

            // we have nothing ...
            if (o == null || o.getLeft() == null)
            {
                // ... and want stop searching in that case
                if (annotation.stopOnGap())
                {
                    break;
                }
                // ... and permit holes in the list aka null
                else if (!annotation.compact())
                {
                    list.add(Pair.of(null, 1));
                }
            }
        }

        // required, but nothing read
        if (annotation.required() && list.isEmpty())
        {
            if (firstError == null)
            {
                Assert.fail(
                                MessageFormat.format(
                                                "No value provided for ''{0}''",
                                                propertyLookup.getEffectiveKey(annotation.key())));
            }
            else
            {
                Assert.fail(
                                MessageFormat.format(
                                                "No value provided for ''{0}'', that might be because of {1}",
                                                propertyLookup.getEffectiveKey(annotation.key()),
                                                firstError.getMessage()));
            }
        }

        if (annotation.immutable())
        {
            return EnumConfigList.buildImmutable(list, annotation.byId());
        }
        else
        {
            return EnumConfigList.build(list, annotation.byId());
        }
    }

    /**
     * Handle an Enum subtype, because this can be a plain flat Java object as
     * well
     *
     * @param annotation
     *            the annotation to process
     * @param index
     *            the current index position
     *
     * @return the looked up object value as left part of the pair and its
     *         weight (Integer value) as right part
     */
    private Pair<Object, Integer> handleEnumPropertyClazz(final EnumProperty annotation, final int index)
    {
        String fullKey;
        final boolean isJDKClass = isJDKClass(annotation.clazz());

        if (isJDKClass)
        {
            // simple types do not have any subkeys
            fullKey = annotation.key() + "." + index;
        }
        else
        {
            fullKey = annotation.key() + "." + index + ".";
        }

        final Integer weight = getPropertyWeight(fullKey, isJDKClass);

        if (!isJDKClass)
        {
            // build the complex type
            final ConfigurationBuilder cb = new ConfigurationBuilder(new LTProperties(propertyLookup, fullKey));

            return Pair.of(cb.build(annotation.clazz()), weight);
        }
        else
        {
            // deal with the simple types
            if (annotation.clazz().equals(String.class))
            {
                return Pair.of(propertyLookup.getProperty(fullKey), weight);
            }
            else if (annotation.clazz().equals(Integer.class))
            {
                final String value = propertyLookup.getProperty(fullKey);

                // turn it into an int, when it fails, complain
                try
                {
                    return Pair.of(Integer.valueOf(value), weight);
                }
                catch (final NumberFormatException e)
                {
                    Assert.fail(MessageFormat.format("''{0}'' is not an integer", propertyLookup.getEffectiveKey(fullKey)));
                }
            }
            else if (annotation.clazz().equals(Boolean.class))
            {
                final String value = propertyLookup.getProperty(fullKey);

                // turn it into an int, when it fails, complain
                try
                {
                    return Pair.of(Boolean.valueOf(value), weight);
                }
                catch (final NumberFormatException e)
                {
                    Assert.fail(MessageFormat.format("''{0}'' is not a boolean", propertyLookup.getEffectiveKey(fullKey)));
                }
            }
            else
            {
                // ok, we do not support any other simple type at the moment
                Assert.fail(MessageFormat.format("Class {1} of ''{0}'' is not supported",
                                propertyLookup.getEffectiveKey(fullKey),
                                annotation.clazz()));
            }
        }

        // should never get here
        return null;
    }

    /**
     * Get the values weight. If no weight is configured explicitly, the
     * implicit value is <code>1</code>.
     *
     * @param fullKey
     *            the enum's basic property key
     * @param isJDKClass
     * @return the values's weight. This value will e <code>1</code> or greater.
     *         <code>1</code> is also the default value in case no weight is
     *         specified explicitly.
     */
    private Integer getPropertyWeight(final String fullKey, final boolean isJDKClass)
    {
        // in case of simple classes (JDKClass) we need to add a period
        final String weightKey = fullKey + (isJDKClass ? "." : "") + "weight";
        final String weightString = propertyLookup.getProperty(weightKey);

        // if we have nothing, use the fallback
        if (weightString == null)
        {
            return 1;
        }

        Integer weight = 0;

        try
        {
            weight = Integer.valueOf(weightString);
        }
        catch (final NumberFormatException e)
        {
            Assert.fail(MessageFormat.format("''{0}'' is not an integer", propertyLookup.getEffectiveKey(weightKey)));
        }

        if (weight < 1)
        {
            Assert.fail(MessageFormat.format("''{0}'' is not an integer greater than 0", propertyLookup.getEffectiveKey(weightKey)));
        }

        return weight;
    }

    /**
     * Identify supported Java class types
     *
     * @param clazz the clazz to identify
     * @return true if a JDK class, false it is a custom class
     */
    private boolean isJDKClass(final Class<?> clazz)
    {
        if (clazz.equals(Boolean.class) ||
                        clazz.equals(String.class) ||
                        clazz.equals(Integer.class))
                        {
            return true;
                        }
        else
        {
            return false;
        }
    }

    /**
     * Processes string information
     *
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private String initializeString(final Property annotation)
    {
        // see if we got a fallback
        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key());
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key(), annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }

        return value;
    }

    /**
     * Processes int information
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private int initializeInt(final Property annotation)
    {
        // see if we got a fallback
        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key());
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key(), annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }
        else if (value == null)
        {
            // some kind of working default is what we need otherwise we fail
            // next
            return 0;
        }

        // turn it into an int, when it fails, complain
        try
        {
            return Integer.valueOf(value);
        }
        catch (final NumberFormatException e)
        {
            Assert.fail(MessageFormat.format("''{0}'' is not an integer", propertyLookup.getEffectiveKey(annotation.key())));
        }

        // we should never reach this
        return 0;
    }

    /**
     * Processes boolean information
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private boolean initializeBoolean(final Property annotation)
    {
        // see if we got a fallback
        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key());
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key(), annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }
        else if (value == null)
        {
            // some kind of working default is what we need otherwise we fail
            // next
            return false;
        }

        // turn it into an int, when it fails, complain
        try
        {
            return Boolean.valueOf(value);
        }
        catch (final NumberFormatException e)
        {
            Assert.fail(MessageFormat.format("''{0}'' is not a boolean", propertyLookup.getEffectiveKey(annotation.key())));
        }

        // we should never reach this
        return false;
    }

    /**
     * Create a list for strings
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private ConfigList initializeConfigList(final Property annotation)
    {
        // see if we got a fallback
        String suffix = ".list";
        if (!annotation.autocomplete())
        {
            suffix = "";
        }

        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key() + suffix);
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key() + suffix, annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key()) + suffix));
        }

        // turn it into an int, when it fails, complain
        return annotation.immutable() ? ConfigList.buildImmutable(value, annotation.delimiters()) : ConfigList.build(value, annotation.delimiters());
    }

    /**
     * Creates an integer range based on either min and max or a natural range
     * definition
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private ConfigRange initializeConfigRange(final Property annotation)
    {
        // see if we got a fallback
        String suffix = ".range";

        if (!annotation.autocomplete())
        {
            suffix = "";
        }

        String valueRange = null;
        final String key = annotation.key() + suffix;

        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            valueRange = propertyLookup.getProperty(key);
        }
        else
        {
            valueRange = propertyLookup.getProperty(key, annotation.fallback());

        }

        // see if we have to complain
        if (annotation.required() && valueRange == null)
        {
            Assert.fail(MessageFormat.format("No value provided for range definition ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }

        // build the ranges, complain if needed
        if (annotation.immutable())
        {
            return ConfigRange.buildImmutable(key, valueRange);
        }
        else
        {
            return ConfigRange.build(key, valueRange);
        }
    }

    /**
     * Creates an time range based on either min and max or a natural range
     * definition
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private ConfigTimeRange initializeConfigTimeRange(final Property annotation)
    {
        // see if we got a fallback
        String suffix = ".range";

        if (!annotation.autocomplete())
        {
            suffix = "";
        }

        String valueRange = null;
        final String key = annotation.key() + suffix;

        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            valueRange = propertyLookup.getProperty(key);
        }
        else
        {
            valueRange = propertyLookup.getProperty(annotation.key() + suffix, annotation.fallback());

        }

        // see if we have to complain
        if (annotation.required() && valueRange == null)
        {
            Assert.fail(MessageFormat.format("No value provided for time range definition ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }

        // build the ranges, complain if needed
        if (annotation.immutable())
        {
            return ConfigTimeRange.buildImmutable(key, valueRange);
        }
        else
        {
            return ConfigTimeRange.build(key, valueRange);
        }
    }

    /**
     * Deal with probability configuration and determines the values
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private ConfigProbability initializeConfigProbability(final Property annotation)
    {
        // see if we got a fallback
        String suffix = ".probability";

        if (!annotation.autocomplete())
        {
            suffix = "";
        }

        String value = null;

        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key() + suffix);
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key() + suffix, annotation.fallback());

        }

        // see if we have to complain
        if (annotation.required() && value == null)
        {
            Assert.fail(MessageFormat.format("No value provided for probability definition ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }

        // build the ranges, complain if needed
        if (annotation.immutable())
        {
            return ConfigProbability.buildImmutable(value);
        }
        else
        {
            return ConfigProbability.build(value);
        }
    }

    /**
     * Creates the distribution setup and data
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private ConfigDistribution initializeConfigDistribution(final Property annotation)
    {
        // see if we got a fallback
        String suffix = ".distribution";
        if (!annotation.autocomplete())
        {
            suffix = "";
        }

        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key() + suffix);
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key() + suffix, annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key()) + suffix));
        }

        // turn it into an int, when it fails, complain
        return annotation.immutable() ? ConfigDistribution.buildImmutable(value, annotation.delimiters()) : ConfigDistribution.build(value, annotation.delimiters());
    }

    /**
     * Creates the distribution setup and data
     *
     * @param field
     *            the field to care about
     * @param annotation
     *            the annotation to process
     * @return the value to attach to the field
     */
    private Pattern initializePattern(final Property annotation)
    {
        // see if we got a fallback
        String value;
        if (ConfigConstants.EMPTY.equals(annotation.fallback()))
        {
            value = propertyLookup.getProperty(annotation.key());
        }
        else
        {
            value = propertyLookup.getProperty(annotation.key(), annotation.fallback());
        }

        // see if we have to complain
        if (value == null && annotation.required())
        {
            Assert.fail(MessageFormat.format("No pattern value provided for ''{0}''", propertyLookup.getEffectiveKey(annotation.key())));
        }

        return RegExUtils.getPattern(value);
    }

    /**
     * Try to deal with nested classes, so that we can also configure custom classes the way the
     * enum type already does it.
     *
     * @param annotation
     *            the annotation to process
     * @param index
     *            the current index position
     *
     * @return the looked up object value as left part of the pair and its weight (Integer value) as
     *         right part
     */
    private <T> T handleNestedPropertyClazz(final Property annotation, final Class<T> clazz)
    {
        // get us the path till here and go into th enext level
        final String fullKey = propertyLookup.prefix.orElse("") + annotation.key() + ".";

        // build the complex type
        final ConfigurationBuilder cb = new ConfigurationBuilder(new LTProperties(propertyLookup, fullKey));
        final T result = cb.build(clazz);

        // if we have something that is not an enum but has an Initable, call it
        // the enums had their init call already
        if (result instanceof Initable && !(result instanceof EnumConfigList))
        {
            ((Initable) result).init();
        }

        return result;
    }
}
