package com.xceptance.loadtest.data.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.htmlunit.HttpMethod;
import org.htmlunit.util.NameValuePair;
import org.junit.Assert;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.xceptance.loadtest.controll.JMeterTestCase;
import com.xceptance.xlt.api.actions.AbstractAction;
import com.xceptance.xlt.engine.SessionImpl;
import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;

/**
 * A Simple Action for basic REST desires, which handles excactly one request. Is able to validate
 * the response and extract values from it.
 *
 * @author b.weigel@xceptance.com
 *
 */
public class SimpleRESTJSONAction extends AbstractAction
{

    /** The response, needs to be stored for validations and data extraction. */
    private HttpResponse response;

    /**
     * String containing a RegExp pattern which is checked on against the status code of the
     * response. Accepts all 20x responses by default.
     */
    private String statusPattern = "20.";

    /** List of all validations which should be performed on the response. */
    private final List<Validation> validations = new ArrayList<>();

    /** List of all data extractions which should be performed on the response. */
    private final List<StoragePrompt> storagePrompts = new ArrayList<>();

    /** The request to be fired. */
    private final HttpRequest httpRequest = new HttpRequest();

    public SimpleRESTJSONAction()
    {
        // Hence we do not need the page of the previous action, we don't need to provide it
        super(null, null);
        final String timerName = getClass().getSimpleName();
        handleTimerName(timerName);
    }

    public SimpleRESTJSONAction(final String timerName)
    {
        // Hence we do not need the page of the previous action, we don't need to provide it
        super(null, null);
        handleTimerName(timerName);
    }

    /**
     * Take care of the timer name and add the Site ID if desired.
     *
     * @param timerName
     */
    private void handleTimerName(final String timerName)
    {
        // Adjust action name if necessary, if we go with default and hence we don't rename with the
        // site id
        final String siteId = Context.get().getSite().id;
        final String newTimerName = JMeterTestCase.getSiteSpecificName(timerName, siteId);

        this.setTimerName(newTimerName);
        httpRequest.timerName(newTimerName);
    }

    @Override
    protected void execute() throws Exception
    {
        response = httpRequest.fire();
    }

    /**
     * Sets the base URL for this call.
     *
     * @param url
     * @return
     */
    public SimpleRESTJSONAction baseUrl(final String url)
    {
        httpRequest.baseUrl(url);
        return this;
    }

    /**
     * Sets the HTTP request header with the given name to the given value.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the value of the header
     * @return
     */
    public SimpleRESTJSONAction header(final String name, final String value)
    {
        httpRequest.header(name, value);
        return this;
    }

    /**
     * Adds a request parameter with the given name and value.
     *
     * @param name
     *            the name of the parameter to add
     * @param value
     *            the value of the parameter to add
     * @return
     */
    public SimpleRESTJSONAction param(final String name, final String value)
    {
        httpRequest.param(name, value);
        return this;
    }

    /**
     * Adds all of the given request parameters.
     *
     * @param params
     *            the parameters to add
     * @return
     */
    public SimpleRESTJSONAction params(final List<NameValuePair> params)
    {
        httpRequest.params(params);
        return this;
    }

    /**
     * Sets the given request headers.
     *
     * @param headers
     *            the request headers to set
     * @return
     */
    public SimpleRESTJSONAction headers(final List<NameValuePair> headers)
    {
        httpRequest.headers(headers);
        return this;
    }

    /**
     * Removes the request header for the given name.
     *
     * @param name
     *            he name of the header to remove
     * @return
     */
    public SimpleRESTJSONAction removeHeader(final String name)
    {
        httpRequest.removeHeader(name);
        return this;
    }

    /**
     * Removes the request parameter with the given name.
     *
     * @param name
     *            the name of the parameter to remove
     * @return
     */
    public SimpleRESTJSONAction removeParam(final String name)
    {
        httpRequest.removeParam(name);
        return this;
    }

    /**
     * Sets the relative URL to be used by this request.
     *
     * @param url
     *            the relative URL as string
     * @return
     */
    public SimpleRESTJSONAction relativeUrl(final String url)
    {
        httpRequest.relativeUrl(url);
        return this;
    }

    /**
     * Sets the body of this request.
     *
     *
     * @param body
     *            the request body as string
     * @return
     */
    public SimpleRESTJSONAction body(final String body)
    {
        httpRequest.body(body);
        return this;
    }

    /**
     * Sets the HTTP method of this request.
     *
     * @param method
     *            the HTTP method
     * @return
     */
    public SimpleRESTJSONAction method(final HttpMethod method)
    {
        httpRequest.method(method);
        return this;
    }

    @Override
    protected void postValidate() throws Exception
    {
        Assert.assertNotNull("Response not received", response);
        if (StringUtils.isNotBlank(statusPattern))
        {
            Assert.assertTrue("Response code does not match expected pattern " + statusPattern, String.valueOf(response.getStatusCode()).matches(statusPattern));
        }

        ReadContext ctx = null;
        if (!validations.isEmpty())
        {
            ctx = JsonPath.parse(response.getContentAsString());

            for (final Validation validation : validations)
            {
                handleValidation(validation, ctx);
            }
        }

        if (!storagePrompts.isEmpty())
        {
            if (ctx == null)
            {
                ctx = JsonPath.parse(response.getContentAsString());
            }
            for (final StoragePrompt storagePrompt : storagePrompts)
            {
                handleStore(storagePrompt, ctx);
            }

        }
    }

    /**
     * Handles a storage Promt. Extracts the data from the response and puts it into the data store
     * with the given name.
     *
     * @param storagePrompt
     * @param ctx
     */
    private void handleStore(final StoragePrompt storagePrompt, final ReadContext ctx)
    {
        Assert.assertTrue("Response " + storagePrompt.jsonPath + " does not exists.", ctx.read(storagePrompt.jsonPath) != null);
        Context.get().testData.store.put(storagePrompt.name, ctx.read(storagePrompt.jsonPath));
    }

    /**
     * Match the status of the response against a given pattern.
     *
     * @param pattern
     *            The RegExp pattern to check the status against. E.g. "20.", "404|401", ...
     * @return
     */
    public SimpleRESTJSONAction assertStatusPattern(final String pattern)
    {
        this.statusPattern = pattern;
        return this;
    }

    /**
     * Check the response status code against this code.
     *
     * @param statusCode
     *            the HTTP status code we expect
     * @return
     */
    public SimpleRESTJSONAction assertStatus(final int statusCode)
    {
        this.statusPattern = String.valueOf(statusCode);
        return this;
    }

    /**
     * Handles a stored validation. Extracts the value from the response and checks against the
     * expectation.
     *
     * @param validation
     * @param ctx
     */
    private void handleValidation(final Validation validation, final ReadContext ctx)
    {
        switch (validation.validationType) {
            case EXISTS:
                Assert.assertTrue(validation.message != null ? validation.message : "Response " + validation.jsonPath + " does not exists.", ctx.read(validation.jsonPath) != null);
                break;
            case NOT_EQUALS:
                Assert.assertNotEquals(
                                validation.message != null ? validation.message : "Response " + validation.jsonPath + " does equals " + validation.expectedValue + "but should not",
                                validation.expectedValue,
                                ctx.read(validation.jsonPath));
                break;
            case EQUALS:
                Assert.assertEquals(validation.message != null ? validation.message : "Response " + validation.jsonPath + " does not equals " + validation.expectedValue,
                                validation.expectedValue,
                                ctx.read(validation.jsonPath));
                break;

            default:
                break;
        }
    }

    /**
     * Store a value from the response, which can be found at the given JSON path. The data will be
     * stored in the context data store and can be retrieved via Context.get().getStored(name);
     *
     * @param jsonPath
     *            the path of the value in the response
     * @param variableName
     *            the name of the variable under which the value should be stored
     * @return
     */
    public SimpleRESTJSONAction storeResponseValue(final String jsonPath, final String variableName)
    {
        final StoragePrompt storagePrompt = new StoragePrompt();

        storagePrompt.jsonPath = jsonPath;
        storagePrompt.name = variableName;
        this.storagePrompts.add(storagePrompt);

        return this;
    }

    /**
     * Adds a validation to the list.
     *
     * @param message
     * @param jsonPath
     * @param expectedValue
     * @param validationType
     * @return
     */
    private SimpleRESTJSONAction addValidation(final String message, final String jsonPath, final Object expectedValue, final ValidationType validationType)
    {
        final Validation validation = new Validation();
        validation.message = message;
        validation.jsonPath = jsonPath;
        validation.expectedValue = expectedValue;
        validation.validationType = validationType;

        this.validations.add(validation);
        return this;
    }

    /**
     * Validate the response content against an expected value. Checks if the value at the given
     * JSON path equals the given value.
     *
     * @param message
     *            Additional error message if the validation fails
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @param expectedValue
     *            The expected value
     * @return
     */
    public SimpleRESTJSONAction validateEquals(final String message, final String jsonPath, final Object expectedValue)
    {
        addValidation(message, jsonPath, expectedValue, ValidationType.EQUALS);
        return this;
    }

    /**
     * Validate the response content against an expected value. Checks if the value at the given
     * JSON path equals the given value.
     *
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @param expectedValue
     *            The expected value
     * @return
     */
    public SimpleRESTJSONAction validateEquals(final String jsonPath, final Object expectedValue)
    {
        return validateEquals(null, jsonPath, expectedValue);
    }

    /**
     * Validate the response content against an expected value. Checks if the value at the given
     * JSON path does NOT equals the given value.
     *
     * @param message
     *            Additional error message if the validation fails
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @param expectedValue
     *            The expected value
     * @return
     */
    public SimpleRESTJSONAction validateNotEquals(final String message, final String jsonPath, final Object expectedValue)
    {
        addValidation(message, jsonPath, expectedValue, ValidationType.NOT_EQUALS);
        return this;
    }

    /**
     * Validate the response content against an expected value. Checks if the value at the given
     * JSON path does NOT equals the given value.
     *
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @param expectedValue
     *            The expected value
     * @return
     */
    public SimpleRESTJSONAction validateNotEquals(final String jsonPath, final Object expectedValue)
    {
        return validateNotEquals(null, jsonPath, expectedValue);
    }

    /**
     * Validate the response content. Checks if the value at the given JSON path does exist.
     *
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @return
     */
    public SimpleRESTJSONAction validateExists(final String jsonPath)
    {
        return validateExists(null, jsonPath);
    }

    /**
     * Validate the response content. Checks if the value at the given JSON path does exist.
     *
     * @param message
     *            Additional error message if the validation fails
     * @param jsonPath
     *            The path under which the response will contain the value to validate
     * @return
     */
    public SimpleRESTJSONAction validateExists(final String message, final String jsonPath)
    {
        addValidation(message, jsonPath, null, ValidationType.EXISTS);
        return this;

    }

    @Override
    public void preValidate() throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() throws Throwable
    {
        try
        {
            super.run();
        }
        finally
        {
            // add an empty "page" as the result of this action
            SessionImpl.getCurrent().getRequestHistory().add(getTimerName());
        }
    }

    /** Internal helper enum to differ types of validations. */
    private enum ValidationType
    {
        EXISTS, EQUALS, NOT_EQUALS;
    }

    /** Internal data object to store a validation promt. */
    private class Validation
    {
        ValidationType validationType;
        Object expectedValue;
        String jsonPath;
        String message;
    }

    /** Internal data object to store a storage promt. */
    private class StoragePrompt
    {
        String name;
        String jsonPath;
    }
}
