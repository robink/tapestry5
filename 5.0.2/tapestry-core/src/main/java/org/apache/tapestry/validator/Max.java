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

package org.apache.tapestry.validator;

import org.apache.tapestry.Field;
import org.apache.tapestry.ValidationException;
import org.apache.tapestry.Validator;
import org.apache.tapestry.ioc.MessageFormatter;

/** Enforces a maximum integer value. */
public class Max implements Validator<Long, Number>
{
    public Class<Long> getConstraintType()
    {
        return Long.class;
    }

    public String getMessageKey()
    {
        return "max-integer";
    }

    public Class<Number> getValueType()
    {
        return Number.class;
    }

    public boolean invokeIfBlank()
    {
        return false;
    }

    public void validate(Field field, Long constraintValue, MessageFormatter formatter, Number value)
            throws ValidationException
    {
        if (value.longValue() > constraintValue)
            throw new ValidationException(formatter.format(constraintValue, field.getLabel()));
    }
}