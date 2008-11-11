// Copyright 2006 The Apache Software Foundation
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

package org.apache.tapestry.internal.util;

import org.apache.tapestry.services.ClassTransformation;

import static java.lang.String.format;

/**
 * Implementation of {@link org.apache.tapestry.internal.util.ParameterBuilder} that simply provides a static
 * string value for the parameter expression.
 */
public final class StringParameterBuilder implements ParameterBuilder
{
    private final String _expression;

    public StringParameterBuilder(String expression)
    {
        _expression = expression;
    }

    public String buildParameter(ClassTransformation transformation)
    {
        return _expression;
    }

    @Override
    public String toString()
    {
        return format("StringParameterBuilder[%s]", _expression);
    }
}