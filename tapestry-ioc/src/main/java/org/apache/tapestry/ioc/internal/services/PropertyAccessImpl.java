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

package org.apache.tapestry.ioc.internal.services;

import static org.apache.tapestry.ioc.internal.util.CollectionFactory.newLinkedList;
import static org.apache.tapestry.ioc.internal.util.CollectionFactory.newList;
import static org.apache.tapestry.ioc.internal.util.CollectionFactory.newThreadSafeMap;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.ioc.services.ClassPropertyAdapter;
import org.apache.tapestry.ioc.services.PropertyAccess;

public class PropertyAccessImpl implements PropertyAccess
{
    private final Map<Class, ClassPropertyAdapter> _adapters = newThreadSafeMap();

    public Object get(Object instance, String propertyName)
    {
        return getAdapter(instance).get(instance, propertyName);
    }

    public void set(Object instance, String propertyName, Object value)
    {
        getAdapter(instance).set(instance, propertyName, value);
    }

    /** Clears the cache of adapters and asks the Introspector to clear its cache. */
    public synchronized void clearCache()
    {
        _adapters.clear();

        Introspector.flushCaches();
    }

    public ClassPropertyAdapter getAdapter(Object instance)
    {
        return getAdapter(instance.getClass());
    }

    public ClassPropertyAdapter getAdapter(Class forClass)
    {
        ClassPropertyAdapter result = _adapters.get(forClass);

        if (result == null)
        {
            result = buildAdapter(forClass);
            _adapters.put(forClass, result);
        }

        return result;
    }

    /**
     * Builds a new adapter and updates the _adapters cache. This not only guards access to the
     * adapter cache, but also serializes access to the Java Beans Introspector, which is not thread
     * safe. In addition, handles the case where the class in question is an interface, accumulating
     * properties inherited from super-classes.
     */
    private synchronized ClassPropertyAdapter buildAdapter(Class forClass)
    {
        // In some race conditions, we may hit this method for the same class multiple times.
        // We just let it happen, replacing the old ClassPropertyAdapter with a new one.

        try
        {
            BeanInfo info = Introspector.getBeanInfo(forClass);

            List<PropertyDescriptor> descriptors = newList();

            addAll(descriptors, info.getPropertyDescriptors());

            if (forClass.isInterface()) addPropertiesFromExtendedInterfaces(forClass, descriptors);

            return new ClassPropertyAdapterImpl(forClass, descriptors);
        }
        catch (Throwable ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private <T> void addAll(List<T> list, T[] array)
    {
        for (T i : array)
            list.add(i);
    }

    private void addPropertiesFromExtendedInterfaces(Class forClass,
            List<PropertyDescriptor> descriptors) throws IntrospectionException
    {
        LinkedList<Class> queue = newLinkedList();

        // Seed the queue
        addAll(queue, forClass.getInterfaces());

        while (!queue.isEmpty())
        {
            Class c = queue.removeFirst();

            BeanInfo info = Introspector.getBeanInfo(c);

            addAll(descriptors, info.getPropertyDescriptors());
            addAll(queue, c.getInterfaces());
        }
    }

}