package com.xceptance.loadtest.api.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;

import com.xceptance.common.util.PropertiesUtils;

/**
 * Properties container, capable of handling prefixes for property keys
 *
 * @author Rene Schwietzke
 *
 */
public class LTProperties
{
    /**
     * List of properties to run through, first hit will stop it, this is used backwards aka the
     * newest will checked first
     */
    public final Properties properties = new Properties();

    /**
     * A prefix to get nicer things
     */
    public final Optional<String> prefix;

    /**
     * The full classname
     */
    public final String fullTestClassName;

    /**
     * The short aka simple classname
     */
    public final String shortTestClassName;

    /**
     * The current user name
     */
    public final String userName;

    /**
     * The site to use for lookup
     */
    public final String siteId;

    /**
     * Constructor that has two contexts to lookup data
     *
     * @param fullTestClassName
     *            the original properties to extend by a prefix
     * @param prefix
     *            the prefix to incorporate for enum properties
     */
    public LTProperties(final String fullTestClassName, final String userName, final String siteId)
    {
        this.fullTestClassName = fullTestClassName;
        this.userName = userName;
        this.shortTestClassName = ClassUtils.getShortClassName(fullTestClassName);

        this.siteId = siteId;
        this.prefix = Optional.empty();
    }

    /**
     * Constructor that has two contexts to lookup data
     *
     * @param propertyLookUp
     *            the original properties to extend by a prefix
     * @param prefix
     *            the prefix to incorporate for enum properties
     */
    public LTProperties(final LTProperties propertyLookUp, final String prefix)
    {
        this.fullTestClassName = propertyLookUp.fullTestClassName;
        this.userName = propertyLookUp.userName;
        this.shortTestClassName = propertyLookUp.shortTestClassName;

        this.siteId = propertyLookUp.siteId;
        this.prefix = Optional.of(prefix);

        this.properties.putAll(propertyLookUp.properties);
    }

    /**
     * Add a set of properties for lookup to the beginning of the lookup
     *
     * @param properties
     *            the new list of properties to read from
     */
    public void addProperties(final Optional<Properties> properties)
    {
        if (properties.isPresent())
        {
            this.properties.putAll(properties.get());
        }
    }

    /**
     * Get a property form stack, first hit wins
     *
     * @param key
     *            the key to look up
     * @return the property value or null if it does not exist
     */
    private String lookUpProperty(final String key)
    {
        final String effectiveKey = getEffectiveKey(key);

        final String result = this.properties.getProperty(effectiveKey);
        if (result != null)
        {
            return PropertiesUtils.substituteVariables(result, this.properties);
        }

        return null;
    }

    /**
     * Returns the effective key to be used for property lookup via one of the getProperty(...)
     * methods.
     *
     * This method implements the fall-back logic, given example key foobar and site context US,
     * while the test case TBrowse has also a mapping called TBrowse_US (test case setup).
     *
     * <ol>
     * <li>TBrowse_US.site.US.foobar
     * <li>TBrowse_US.foobar
     * <li>TBrowse.site.US.foobar
     * <li>TBrowse.foobar
     * <li>site.US.foobar
     * <li>foobar
     *
     * @param key
     *            the property key, i.e. without any prefixes
     * @param siteId
     *            site specific property prefix (if any)
     * @return the first key that produces a result
     */
    private String getEffectiveKey(final String originalKey, final String siteId)
    {
        String key = originalKey;

        // 0. see if we have any prefix to use when we have enum properties such as a list
        key = prefix.orElse("") + originalKey;

        final String siteKey = "site." + siteId + "." + key;

        // 1. TBrowse_US.site.<site>.<property>
        final String userNameAndSiteKey = userName + "." + siteKey;
        if (this.properties.containsKey(userNameAndSiteKey))
        {
            return userNameAndSiteKey;
        }

        // 2. TBrowse_US.<property>
        final String userNameAndKey = userName + "." + key;
        if (this.properties.containsKey(userNameAndKey))
        {
            return userNameAndKey;
        }

        // 3. use the given site id and the bare key for a test case
        // e.g. TBrowse.site.<site>.<property> (matches all tests in context of <site>)
        final String classAndSiteKey = shortTestClassName + "." + siteKey;
        if (this.properties.containsKey(classAndSiteKey))
        {
            return classAndSiteKey;
        }

        // 4. TBrowse.property
        final String shortClassNameQualifiedKey = shortTestClassName + "." + key;
        if (this.properties.containsKey(shortClassNameQualifiedKey))
        {
            return shortClassNameQualifiedKey;
        }

        // 5. use the given site id and the bare key
        // e.g. site.<site>.<property> (matches all tests in context of <site>)
        if (this.properties.containsKey(siteKey))
        {
            return siteKey;
        }

        // 6. use bare key (most general property notation)
        // just <property>
        return key;
    }

    /**
     * Returns the effective key to be used for property lookup via one of the
     * getProperty(...) methods.
     * <p>
     * This method implements the fall-back logic:
     * <ol>
     * <li>user name plus simple key (e.g. <b>TMyRunningTest.</b>password)</li>
     * <li>site prefix plus user name plus simple key (e.g.
     * <b>de.TMyRunningTest.</b>password)</li>
     * <li>test class name plus simple key (e.g.
     * <b>com.xceptance.xlt.samples.testsuite.tests.TAuthor.</b>password)</li>
     * <li>site prefix plus plus simple key (e.g. <b>de.</b>password)</li>
     * <li>simple key (e.g. password)</li>
     * </ol>
     * As site the currently configured site is taken.
     *
     * @param bareKey
     *            the bare property key, i.e. without any prefixes
     * @return the first key that produces a result
     */
    public String getEffectiveKey(final String bareKey)
    {
        return getEffectiveKey(bareKey, siteId);
    }

    /**
     * Returns the value for the given key as configured in the test suite
     * configuration. See {@link #getProperty(String)} for a description of the
     * look-up logic. This method returns the passed default value if the
     * property value could not be found.
     *
     * @param key
     *            the property key
     * @param defaultValue
     *            the default value
     * @return the property value as an int
     */
    public int getProperty(final String key, final int defaultValue)
    {
        final String effectiveKey = getEffectiveKey(key);

        final String value = lookUpProperty(effectiveKey);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (final NumberFormatException e)
            {
            }
        }

        // property is not set so far or its integer value does not seem to be a
        // string representation of an integer value -> return defaultValue
        return defaultValue;
    }

    /**
     * Returns the value for the given key as configured in the test suite
     * configuration. See {@link #getProperty(String)} for a description of the
     * look-up logic. This method returns the passed default value if the
     * property value could not be found.
     *
     * @param key
     *            the property key
     * @param defaultValue
     *            the default value
     * @return the property value
     */
    public String getProperty(final String key, final String defaultValue)
    {
        final String effectiveKey = getEffectiveKey(key);

        final String value = lookUpProperty(effectiveKey);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the value for the given key as configured in the test suite
     * configuration. See {@link #getProperty(String)} for a description of the
     * look-up logic. This method returns the passed default value if the
     * property value could not be found.
     *
     * @param key
     *            the property key
     * @param defaultValue
     *            the default value
     * @return the property value as a boolean
     */
    public boolean getProperty(final String key, final boolean defaultValue)
    {
        final String effectiveKey = getEffectiveKey(key);

        final String value = lookUpProperty(effectiveKey);
        if (value != null)
        {
            return Boolean.valueOf(value);
        }

        return defaultValue;
    }

    /**
     * Returns the value for the given key as configured in the test suite
     * configuration. The process of looking up a property uses multiple
     * fall-backs. When resolving the value for the key "password", for example,
     * the following effective keys are tried, in this order:
     * <ol>
     * <li>the test user name plus simple key, e.g. "TAuthor.password"</li>
     * <li>the test class name plus simple key, e.g.
     * "com.xceptance.xlt.samples.tests.TAuthor.password"</li>
     * <li>the simple key, e.g. "password"</li>
     * </ol>
     * This multi-step hierarchy allows for test-user-specific or
     * test-case-specific overrides of certain settings, while falling back to
     * the globally defined values if such specific settings are absent.
     *
     * @param key
     *            the simple property key
     * @return the property value, or <code>null</code> if not found
     */
    public String getProperty(final String key)
    {
        return lookUpProperty(key);
    }

    /**
     * Returns all properties for this domain key, strips the key from the
     * property name, e.g. ClassName.Testproperty=ABC --> TestProperty=ABC
     * Attention: Properties without a domain (e.g. foobar=test) or domain only
     * properties are invalid and will be ignored. A property has to have at
     * least this form: domain.propertyname=value
     *
     * @param domain
     *            domain for the properties
     * @return map with all key value pairs of properties
     */
    public Map<String, String> getPropertiesForKey(final String domain)
    {
        final String effectiveKey = getEffectiveKey(domain);
        return PropertiesUtils.getPropertiesForKey(effectiveKey, properties);
    }

    /**
     * Just for debugging
     */
    @Override
    public String toString()
    {
        final List<String> all = properties.stringPropertyNames().stream().map(k -> k + " = " + properties.getProperty(k)).sorted().collect(Collectors.toList());

        final StringBuilder sb = new StringBuilder(1024);
        for (final String s : all)
        {
            sb.append(s);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
