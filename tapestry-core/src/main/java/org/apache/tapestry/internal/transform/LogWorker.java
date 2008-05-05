// Copyright 2008 The Apache Software Foundation
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

package org.apache.tapestry.internal.transform;

import org.apache.tapestry.annotations.Log;
import org.apache.tapestry.ioc.MethodAdvice;
import org.apache.tapestry.ioc.internal.services.LoggingAdvice;
import org.apache.tapestry.ioc.services.ExceptionTracker;
import org.apache.tapestry.model.MutableComponentModel;
import org.apache.tapestry.services.*;

import java.util.List;

/**
 * Looks for the {@link org.apache.tapestry.annotations.Log} marker annotation and adds method advice to perform the
 * logging. This is similar to what the {@link org.apache.tapestry.ioc.services.LoggingDecorator} does for service
 * interface methods.
 */
public class LogWorker implements ComponentClassTransformWorker
{
    private final ExceptionTracker _exceptionTracker;

    public LogWorker(ExceptionTracker exceptionTracker)
    {
        _exceptionTracker = exceptionTracker;
    }

    public void transform(ClassTransformation transformation, MutableComponentModel model)
    {
        List<TransformMethodSignature> signatures = transformation.findMethodsWithAnnotation(Log.class);

        if (signatures.isEmpty()) return;

        // Re-use the logging advice from LoggingDecorator
        final MethodAdvice loggingAdvice = new LoggingAdvice(model.getLogger(), _exceptionTracker);

        // ... but wrap it for use at the component level.
        ComponentMethodAdvice advice = new ComponentMethodAdvice()
        {
            public void advise(ComponentMethodInvocation invocation)
            {
                loggingAdvice.advise(invocation);
            }
        };

        for (TransformMethodSignature signature : signatures)
            transformation.advise(signature, advice);
    }
}