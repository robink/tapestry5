// Copyright 2007, 2008 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.internal.services;

import org.apache.tapestry.*;
import org.apache.tapestry.corelib.internal.InternalMessages;
import org.apache.tapestry.internal.util.Holder;
import org.apache.tapestry.ioc.Messages;
import org.apache.tapestry.ioc.internal.util.Defense;
import org.apache.tapestry.ioc.internal.util.InternalUtils;
import org.apache.tapestry.ioc.services.TypeCoercer;
import org.apache.tapestry.ioc.util.ExceptionUtils;
import org.apache.tapestry.services.ValidationMessagesSource;

public class FieldValidationSupportImpl implements FieldValidationSupport
{
    static final String PARSE_CLIENT_EVENT = "parseClient";
    static final String TO_CLIENT_EVENT = "toClient";
    static final String VALIDATE_EVENT = "validate";

    private final ValidationMessagesSource _messagesSource;

    private final TypeCoercer _typeCoercer;

    public FieldValidationSupportImpl(ValidationMessagesSource messagesSource, TypeCoercer typeCoercer)
    {
        _messagesSource = messagesSource;
        _typeCoercer = typeCoercer;
    }

    @SuppressWarnings({ "unchecked" })
    public String toClient(Object value, ComponentResources componentResources, Translator translator,
                           NullFieldStrategy nullFieldStrategy)
    {
        Defense.notNull(componentResources, "componentResources");
        Defense.notNull(translator, "translator");
        Defense.notNull(nullFieldStrategy, "nullFieldStrategy");

        final Holder<String> resultHolder = Holder.create();

        ComponentEventCallback callback = new ComponentEventCallback()
        {
            public boolean handleResult(Object result)
            {
                // What's nice is that the ComponentEventException will automatically identify
                // the method description.

                if (!(result instanceof String))
                    throw new RuntimeException(InternalMessages.toClientShouldReturnString());

                resultHolder.put((String) result);

                return true;
            }
        };

        componentResources.triggerEvent(TO_CLIENT_EVENT, new Object[] { value }, callback);

        if (resultHolder.hasValue()) return resultHolder.get();

        Object effectiveValue = value;

        if (effectiveValue == null)
        {
            effectiveValue = nullFieldStrategy.replaceToClient();

            // Don't try to coerce or translate null.

            if (effectiveValue == null) return null;
        }

        // And now, whether its a value from a bound property, or from the null field strategy,
        // get it into the right format for the translator and let it translate.

        Object coerced = _typeCoercer.coerce(effectiveValue, translator.getType());

        return translator.toClient(coerced);

    }

    public Object parseClient(String clientValue, ComponentResources componentResources, Translator translator,
                              NullFieldStrategy nullFieldStrategy)
            throws ValidationException
    {
        Defense.notNull(componentResources, "componentResources");
        Defense.notNull(translator, "translator");
        Defense.notNull(nullFieldStrategy, "nullFieldStrategy");

        String effectiveValue = clientValue;

        if (InternalUtils.isBlank(effectiveValue))
        {
            effectiveValue = nullFieldStrategy.replaceFromClient();

            if (effectiveValue == null) return null;
        }

        final Holder<Object> resultHolder = Holder.create();

        ComponentEventCallback callback = new ComponentEventCallback()
        {
            public boolean handleResult(Object result)
            {
                resultHolder.put(result);
                return true;
            }
        };

        try
        {
            componentResources.triggerEvent(PARSE_CLIENT_EVENT, new Object[] { effectiveValue }, callback);
        }
        catch (RuntimeException ex)
        {
            rethrowValidationException(ex);
        }

        if (resultHolder.hasValue()) return resultHolder.get();

        // Otherwise, let the normal translator do the job.

        Messages messages = _messagesSource.getValidationMessages(componentResources.getLocale());

        return translator.parseClient(effectiveValue, messages);
    }

    /**
     * Checks for a {@link org.apache.tapestry.ValidationException} inside the outer exception and throws that,
     * otherwise rethrows the runtime exception.
     *
     * @param outerException initially caught exception
     * @throws ValidationException if found
     */
    private void rethrowValidationException(RuntimeException outerException) throws ValidationException
    {
        ValidationException ve = ExceptionUtils.findCause(outerException, ValidationException.class);

        if (ve != null) throw ve;

        throw outerException;
    }

    @SuppressWarnings({ "unchecked" })
    public void validate(Object value, ComponentResources componentResources, FieldValidator validator)
            throws ValidationException
    {
        Defense.notNull(componentResources, "componentResources");
        Defense.notNull(validator, "validator");

        validator.validate(value);

        try
        {
            componentResources.triggerEvent(VALIDATE_EVENT, new Object[] { value }, null);
        }
        catch (RuntimeException ex)
        {
            rethrowValidationException(ex);
        }
    }
}