// Copyright 2007 The Apache Software Foundation
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

package org.apache.tapestry.ioc.services;

import org.apache.tapestry.Binding;
import org.apache.tapestry.ComponentResources;
import org.apache.tapestry.ComponentResourcesCommon;
import org.apache.tapestry.Field;

/**
 * A service that can be injected into a component to provide common defaults for various
 * parameters.
 */
public interface ComponentDefaultProvider
{
    /**
     * Computes the default label for the component (which will generally be a {@link Field}).
     * 
     * @param resources
     * @return the label, either extracted from the component's container's message catalog, or
     *         derived from the component's {@link ComponentResourcesCommon#getId()}.
     */
    String defaultLabel(ComponentResources resources);

    /**
     * Checks to see if the container of the component (identified by its resources) contains a
     * property matching the component's id. If so, a binding for that property is returned. This is
     * usually the default for a {@link Field}'s value parameter (or equivalent).
     * 
     * @param parameterName
     *            the name of the parameter
     * @param resources
     *            the resources of the component for which a binding is needed
     * @return the binding, or null if the container does not have a matching property
     */
    Binding defaultBinding(String parameterName, ComponentResources resources);
}