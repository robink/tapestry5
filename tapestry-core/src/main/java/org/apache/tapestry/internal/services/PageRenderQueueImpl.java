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

package org.apache.tapestry.internal.services;

import org.apache.tapestry.MarkupWriter;
import org.apache.tapestry.internal.structure.Page;
import static org.apache.tapestry.ioc.IOCConstants.PERTHREAD_SCOPE;
import org.apache.tapestry.ioc.annotations.Scope;
import org.apache.tapestry.runtime.RenderCommand;

@Scope(PERTHREAD_SCOPE)
public class PageRenderQueueImpl implements PageRenderQueue
{
    private Page _page;

    private RenderCommand _rootCommand;

    public void initializeForCompletePage(Page page)
    {
        _page = page;
        _rootCommand = page.getRootElement();
    }

    public void initializeForPartialPageRender(RenderCommand rootCommand)
    {
        if (_page == null) throw new IllegalStateException("Page must be specified before root render command.");

        _rootCommand = rootCommand;
    }

    public void render(MarkupWriter writer)
    {
        RenderQueueImpl queue = new RenderQueueImpl(_page.getLogger());

        queue.push(_rootCommand);

        // Run the queue until empty.

        queue.run(writer);
    }
}