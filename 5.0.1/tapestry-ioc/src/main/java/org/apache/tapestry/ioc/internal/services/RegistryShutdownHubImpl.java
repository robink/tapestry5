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

package org.apache.tapestry.ioc.internal.services;

import static org.apache.tapestry.ioc.internal.util.CollectionFactory.newThreadSafeList;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.tapestry.ioc.internal.util.OneShotLock;
import org.apache.tapestry.ioc.services.RegistryShutdownHub;
import org.apache.tapestry.ioc.services.RegistryShutdownListener;

public class RegistryShutdownHubImpl implements RegistryShutdownHub
{
    private final OneShotLock _lock = new OneShotLock();

    private final Log _log;

    private final List<RegistryShutdownListener> _listeners = newThreadSafeList();

    public RegistryShutdownHubImpl(Log log)
    {
        _log = log;
    }

    public void addRegistryShutdownListener(RegistryShutdownListener listener)
    {
        _lock.check();

        _listeners.add(listener);
    }

    /**
     * Fires the {@link RegistryShutdownListener#registryDidShutdown()} method on each listener. At
     * the end, all the listeners are discarded.
     * 
     * @param log
     *            used if any listener throws an exception
     */
    public void fireRegistryDidShutdown()
    {
        _lock.lock();

        for (RegistryShutdownListener l : _listeners)
        {
            try
            {
                l.registryDidShutdown();
            }
            catch (Exception ex)
            {
                _log.error(ServiceMessages.shutdownListenerError(l, ex), ex);
            }
        }

        _listeners.clear();
    }

}