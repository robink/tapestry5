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

package org.apache.tapestry.integration.app1.pages;

import org.apache.tapestry.integration.app1.data.IntegerHolder;
import org.apache.tapestry.annotations.InjectPage;

public class NumberBeanEditorDemo
{
    private IntegerHolder _holder;

    @InjectPage
    private NumberBeanDisplayDemo _page;

    public IntegerHolder getHolder()
    {
        return _holder;
    }

    public void setHolder(IntegerHolder holder)
    {
        _holder = holder;
    }

    Object onSuccess()
    {
        return _page.initialize(_holder);
    }
}