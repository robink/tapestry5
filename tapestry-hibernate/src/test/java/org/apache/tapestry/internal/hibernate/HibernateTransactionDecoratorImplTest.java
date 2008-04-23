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

package org.apache.tapestry.internal.hibernate;

import org.apache.tapestry.hibernate.HibernateSessionManager;
import org.apache.tapestry.hibernate.HibernateTransactionDecorator;
import org.apache.tapestry.hibernate.annotations.CommitAfter;
import org.apache.tapestry.ioc.IOCUtilities;
import org.apache.tapestry.ioc.Registry;
import org.apache.tapestry.ioc.services.AspectDecorator;
import org.apache.tapestry.test.TapestryTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
public class HibernateTransactionDecoratorImplTest extends TapestryTestCase
{
    private Registry _registry;

    private AspectDecorator _aspectDecorator;

    @BeforeClass
    public void setup()
    {
        _registry = IOCUtilities.buildDefaultRegistry();

        _aspectDecorator = _registry.getService(AspectDecorator.class);
    }


    @AfterClass
    public void shutdown()
    {
        _aspectDecorator = null;
        _registry.shutdown();
        _registry = null;
    }

    @Test
    public void undecorated()
    {
        VoidService delegate = newMock(VoidService.class);
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        VoidService interceptor = decorator.build(VoidService.class, delegate, "foo.Bar");

        delegate.undecorated();

        replay();
        interceptor.undecorated();
        verify();

        assertToString(interceptor);
    }

    @Test
    public void void_method()
    {
        VoidService delegate = newMock(VoidService.class);
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        VoidService interceptor = decorator.build(VoidService.class, delegate, "foo.Bar");

        delegate.voidMethod();
        manager.commit();

        replay();
        interceptor.voidMethod();
        verify();

        assertToString(interceptor);
    }

    @Test
    public void void_method_with_param()
    {
        VoidService delegate = newMock(VoidService.class);
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        VoidService interceptor = decorator.build(VoidService.class, delegate, "foo.Bar");

        delegate.voidMethodWithParam(777);
        manager.commit();

        replay();
        interceptor.voidMethodWithParam(777);
        verify();

        assertToString(interceptor);
    }

    @Test
    public void runtime_exception_will_abort_transaction() throws Exception
    {
        Performer delegate = newMock(Performer.class);
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        RuntimeException re = new RuntimeException("Unexpected.");

        delegate.perform();
        setThrowable(re);
        manager.abort();

        replay();

        Performer interceptor = decorator.build(Performer.class, delegate, "foo.Bar");

        try
        {
            interceptor.perform();
            unreachable();
        }
        catch (RuntimeException ex)
        {
            assertSame(ex, re);
        }

        verify();
    }

    @Test
    public void checked_exception_will_commit_transaction() throws Exception
    {
        Performer delegate = newMock(Performer.class);
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        SQLException se = new SQLException("Checked.");

        delegate.perform();
        setThrowable(se);
        manager.commit();

        replay();

        Performer interceptor = decorator.build(Performer.class, delegate, "foo.Bar");

        try
        {
            interceptor.perform();
            unreachable();
        }
        catch (SQLException ex)
        {
            assertSame(ex, se);
        }

        verify();
    }

    @Test
    public void return_type_method()
    {
        ReturnTypeService delegate = newTestService();
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        ReturnTypeService interceptor = decorator.build(ReturnTypeService.class, delegate, "foo.Bar");

        delegate.returnTypeMethod();

        manager.commit();

        replay();
        assertEquals(interceptor.returnTypeMethod(), "Foo");
        verify();

    }

    @Test
    public void return_type_method_with_param()
    {
        ReturnTypeService delegate = newTestService();
        HibernateSessionManager manager = newMock(HibernateSessionManager.class);
        HibernateTransactionDecorator decorator = newHibernateSessionManagerDecorator(manager);
        ReturnTypeService interceptor = decorator.build(ReturnTypeService.class, delegate, "foo.Bar");

        delegate.returnTypeMethodWithParam(5, 3);

        manager.commit();

        replay();
        assertEquals(interceptor.returnTypeMethodWithParam(5, 3), 8);
        verify();

        assertEquals(
                interceptor.toString(),
                "Baz");
    }

    private HibernateTransactionDecorator newHibernateSessionManagerDecorator(HibernateSessionManager manager)
    {
        return new HibernateTransactionDecoratorImpl(_aspectDecorator, manager);
    }

    private void assertToString(VoidService interceptor)
    {
        assertEquals(
                interceptor.toString(),
                "<Hibernate Transaction interceptor for foo.Bar(" + getClass().getName() + "$VoidService)>");
    }

    private ReturnTypeService newTestService()
    {
        return new ReturnTypeService()
        {

            public String returnTypeMethod()
            {
                return "Foo";
            }

            public int returnTypeMethodWithParam(int first, int second)
            {
                return first + second;
            }

            public String toString()
            {
                return "Baz";
            }

        };
    }

    public interface ReturnTypeService
    {
        @CommitAfter
        String returnTypeMethod();

        @CommitAfter
        int returnTypeMethodWithParam(int first, int second);

        String toString();
    }

    public interface VoidService
    {
        void undecorated();

        @CommitAfter
        void voidMethod();

        @CommitAfter
        void voidMethodWithParam(long id);
    }

    public interface Performer
    {
        @CommitAfter
        void perform() throws SQLException;
    }
}