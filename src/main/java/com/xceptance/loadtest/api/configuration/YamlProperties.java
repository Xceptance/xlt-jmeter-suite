package com.xceptance.loadtest.api.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

import com.xceptance.xlt.api.util.XltLogger;

/**
 * Container for YAML based properties.
 *
 * @author Rene Schwietzke
 *
 */
public class YamlProperties
{
    /**
     * Our properties that will hit the outside world later
     */
    private final Properties properties = new Properties();

    /**
     * Don't publish the constructor
     */
    private YamlProperties()
    {
    }

    /**
     * Build us a property key from the current state on the stack
     *
     * @param state
     *            the stack
     * @return a property key
     */
    private String buildKey(final Deque<String> state)
    {
        final Iterator<String> i = state.descendingIterator();

        final StringBuilder sb = new StringBuilder(64);
        while (i.hasNext())
        {
            if (sb.length() > 0)
            {
                sb.append('.');
            }
            sb.append(i.next());
        }

        return sb.toString();
    }

    /**
     * Handle all remaining, hopefully primitive types
     *
     * @param entry
     *            the entry
     * @param state
     *            the current staate of the stack
     */
    private void handleTypes(final Object entry, final Deque<String> state)
    {
        properties.setProperty(buildKey(state), entry == null ? "" : entry.toString());
    }

    /**
     * Handle lists
     *
     * @param list
     *            the list to deal with
     * @param state
     *            the current state
     */
    @SuppressWarnings("unchecked")
    private void list(final List<Object> list, final Deque<String> state)
    {
        for (int i = 0; i < list.size(); i++)
        {
            final Object entry = list.get(i);
            state.push(String.valueOf(i));

            if (entry instanceof Map)
            {
                map((Map<String, Object>) entry, state);
            }
            else if (entry instanceof List)
            {
                list((List<Object>) entry, state);
            }
            else
            {
                handleTypes(entry, state);
            }

            state.pop();
        }
    }

    /**
     * Deal with maps
     *
     * @param map
     *            the map of data
     * @param state
     *            the current state
     */
    @SuppressWarnings("unchecked")
    private void map(final Map<String, Object> map, final Deque<String> state)
    {
        // is there anything to map?
        if (map == null)
        {
            return;
        }

        for (final String key : map.keySet())
        {
            state.push(key);

            final Object value = map.get(key);

            XltLogger.runTimeLogger.info(MessageFormat.format("Processing: {1} = {0}", value, state));

            if (value instanceof Map)
            {
                map((Map<String, Object>) value, state);
            }
            else if (value instanceof List)
            {
                list((List<Object>) value, state);
            }
            else
            {
                handleTypes(value, state);
            }

            state.pop();
        }
    }

    /**
     * Just do the magic
     *
     * @param map
     *            the map to convert
     */
    private void yamlToJavaProperties(final Map<String, Object> map)
    {
        map(map, new ArrayDeque<String>());
    }

    /**
     * Parses yaml properties and turns them into regular Java properties. If the file does not
     * exists, it returns null.
     *
     * @param file
     *            a yaml file
     * @return a Java properties file reader
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static Properties build(final File file) throws FileNotFoundException, IOException
    {
        try (final Reader reader = Files.newBufferedReader(file.toPath()))
        {
            return build(reader);
        }
    }

    /**
     * Parses yaml properties and turns them into regular Java properties. If the file does not
     * exists, it returns null.
     *
     * @param reader
     *            a reader for a YAML file
     * @return a Java properties file reader
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static Properties build(final Reader reader) throws FileNotFoundException, IOException
    {
        final Yaml yaml = new Yaml();

        final Map<String, Object> map = yaml.load(reader);
        final YamlProperties yamlProperties = new YamlProperties();
        yamlProperties.yamlToJavaProperties(map);

        return yamlProperties.properties;
    }
}
