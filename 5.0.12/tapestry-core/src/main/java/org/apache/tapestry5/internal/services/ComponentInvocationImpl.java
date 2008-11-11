// Copyright 2006, 2007, 2008 The Apache Software Foundation
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

package org.apache.tapestry5.internal.services;

import org.apache.tapestry5.internal.TapestryInternalUtils;
import static org.apache.tapestry5.ioc.internal.util.CollectionFactory.newMap;
import static org.apache.tapestry5.ioc.internal.util.Defense.notBlank;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.test.PageTester;

import java.util.List;
import java.util.Map;

/**
 * Represents an invocation for a page or a component in the current application. This information is extracted from
 * incoming URLs for a running application (or created by the {@link PageTester}. Each invocation may provide a context
 * (Object[]) and parameters to the invocation target.
 */
public class ComponentInvocationImpl implements ComponentInvocation
{
    private final String[] context;

    private final InvocationTarget target;

    private final String[] activationContext;

    private Map<String, String> parameters;

    /**
     * @param target            identifies the target of the event: a component with a page
     * @param context           context information supplied by the component to be provided back when the event on the
     *                          component is triggered, or contains the activation context when the invocation is for a
     *                          page render request
     * @param activationContext page activation context for the page containing the component, supplied via a passivate
     *                          event to the page's root component (used when an action component invocation is for a
     *                          page with an activation context)
     */
    public ComponentInvocationImpl(InvocationTarget target, String[] context, String[] activationContext)
    {
        this.target = target;
        this.context = context;
        this.activationContext = activationContext;
    }


    public String buildURI(boolean isForm)
    {
        String path = getPath();
        if (isForm || parameters == null) return path;

        StringBuilder builder = new StringBuilder();

        builder.append(path);

        String sep = "?";

        for (String name : getParameterNames())
        {
            String value = parameters.get(name);

            builder.append(sep);

            // We assume that the name is URL safe and that the value will already have been URL
            // encoded if it is not known to be URL safe.

            builder.append(name);
            builder.append("=");
            builder.append(value);

            sep = "&";
        }

        return builder.toString();
    }

    /**
     * @return Just like the return value of {@link #buildURI(boolean)} except that parameters are not included.
     */
    private String getPath()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(target.getPath());

        for (String id : context)
        {
            if (builder.length() > 0) builder.append("/");

            builder.append(TapestryInternalUtils.encodeContext(id));
        }

        return builder.toString();
    }

    public String[] getContext()
    {
        return context;
    }

    public String[] getActivationContext()
    {
        return activationContext;
    }

    public void addParameter(String parameterName, String value)
    {
        notBlank(parameterName, "parameterName");
        notBlank(value, "value");

        if (parameters == null) parameters = newMap();

        if (parameters.containsKey(parameterName)) throw new IllegalArgumentException(
                ServicesMessages.parameterNameMustBeUnique(parameterName, parameters.get(parameterName)));

        parameters.put(parameterName, value);
    }

    public List<String> getParameterNames()
    {
        return InternalUtils.sortedKeys(parameters);
    }

    public String getParameterValue(String name)
    {
        return InternalUtils.get(parameters, name);
    }

    public InvocationTarget getTarget()
    {
        return target;
    }
}