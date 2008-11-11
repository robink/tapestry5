// Copyright 2006, 2007 The Apache Software Foundation
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

import org.apache.tapestry.ComponentEventHandler;
import org.apache.tapestry.Link;
import org.apache.tapestry.TapestryConstants;
import org.apache.tapestry.internal.structure.ComponentPageElement;
import org.apache.tapestry.internal.structure.Page;
import org.apache.tapestry.internal.util.Holder;
import org.apache.tapestry.runtime.Component;
import org.apache.tapestry.services.ComponentActionRequestHandler;
import org.apache.tapestry.services.ComponentEventResultProcessor;
import org.apache.tapestry.services.Response;
import org.apache.tapestry.services.Traditional;

import java.io.IOException;

public class ComponentActionRequestHandlerImpl implements ComponentActionRequestHandler
{
    private final ComponentEventResultProcessor _resultProcessor;

    private final RequestPageCache _cache;

    private final LinkFactory _linkFactory;

    private final Response _response;

    public ComponentActionRequestHandlerImpl(@Traditional ComponentEventResultProcessor resultProcessor,
                                             RequestPageCache cache, LinkFactory linkFactory, Response response)
    {
        _resultProcessor = resultProcessor;
        _cache = cache;
        _linkFactory = linkFactory;
        _response = response;
    }

    public void handle(String logicalPageName, String nestedComponentId, String eventType, String[] context,
                       String[] activationContext) throws IOException
    {
        Page page = _cache.get(logicalPageName);

        // This is the active page, until we know better.

        ComponentPageElement element = page.getComponentElementByNestedId(nestedComponentId);

        final Holder<Boolean> holder = Holder.create();

        ComponentEventHandler handler = new ComponentEventHandler()
        {
            @SuppressWarnings("unchecked")
            public boolean handleResult(Object result, Component component, String methodDescription)
            {
                try
                {
                    _resultProcessor.processComponentEvent(result, component, methodDescription);
                }
                catch (IOException ex)
                {
                    throw new RuntimeException(ex);
                }

                holder.put(true);

                return true;
            }
        };

        // If activating the page returns a "navigational result", then don't trigger the action
        // on the component.

        page.getRootElement().triggerEvent(TapestryConstants.ACTIVATE_EVENT, activationContext, handler);

        if (holder.hasValue()) return;

        element.triggerEvent(eventType, context, handler);

        if (holder.hasValue()) return;

        if (!_response.isCommitted())
        {
            Link link = _linkFactory.createPageLink(page, false);

            _response.sendRedirect(link);
        }
    }
}