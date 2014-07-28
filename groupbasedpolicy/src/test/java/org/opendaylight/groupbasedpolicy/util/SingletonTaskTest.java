/*
 * Copyright 2011, Big Switch Networks, Inc. 
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.util.SingletonTask;

public class SingletonTaskTest {

    public int ran = 0;
    public int finished = 0;
    public long time = 0;

    @Before
    public void setUp() throws Exception {
        ran = 0;
        finished = 0;
        time = 0;
    }

    @Test
    public void testBasic() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                ran += 1;
            }
        });
        st1.reschedule(0, null);
        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals("Check that task ran", 1, ran);
    }

    @Test
    public void testDelay() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                ran += 1;
                time = System.nanoTime();
            }
        });
        long start = System.nanoTime();
        st1.reschedule(10, TimeUnit.MILLISECONDS);
        assertFalse("Check that task hasn't run yet", ran > 0);

        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals("Check that task ran", 1, ran);
        assertTrue("Check that time passed appropriately",
                   (time - start) >= TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testReschedule() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        final Object tc = this;
        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                synchronized (tc) {
                    ran += 1;
                }
                time = System.nanoTime();
            }
        });
        long start = System.nanoTime();
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);
        st1.reschedule(20, TimeUnit.MILLISECONDS);
        Thread.sleep(5);
        assertFalse("Check that task hasn't run yet", ran > 0);

        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals("Check that task ran only once", 1, ran);
        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) >= TimeUnit.NANOSECONDS.convert(55, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConcurrentAddDelay() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        final Object tc = this;
        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                synchronized (tc) {
                    ran += 1;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (tc) {
                    finished += 1;
                    time = System.nanoTime();
                }
            }
        });

        long start = System.nanoTime();
        st1.reschedule(5, TimeUnit.MILLISECONDS);
        Thread.sleep(20);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);
        st1.reschedule(75, TimeUnit.MILLISECONDS);
        assertTrue("Check task running state true", st1.context.taskRunning);
        assertTrue("Check task should run state true", st1.context.taskShouldRun);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);

        Thread.sleep(150);

        assertTrue("Check task running state false", !st1.context.taskRunning);
        assertTrue("Check task should run state false", !st1.context.taskShouldRun);
        assertEquals("Check that task ran exactly twice", 2, ran);
        assertEquals("Check that task finished exactly twice", 2, finished);

        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) >= TimeUnit.NANOSECONDS.convert(130, TimeUnit.MILLISECONDS));
        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) <= TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));

        ses.shutdown();
        ses.awaitTermination(15, TimeUnit.SECONDS);
    }

    @Test
    public void testConcurrentAddDelay2() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        final Object tc = this;
        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                synchronized (tc) {
                    ran += 1;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                synchronized (tc) {
                    finished += 1;
                    time = System.nanoTime();
                }
            }
        });

        long start = System.nanoTime();
        st1.reschedule(5, TimeUnit.MILLISECONDS);
        Thread.sleep(20);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);
        st1.reschedule(25, TimeUnit.MILLISECONDS);
        assertTrue("Check task running state true", st1.context.taskRunning);
        assertTrue("Check task should run state true", st1.context.taskShouldRun);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);

        Thread.sleep(150);

        assertTrue("Check task running state false", !st1.context.taskRunning);
        assertTrue("Check task should run state false", !st1.context.taskShouldRun);
        assertEquals("Check that task ran exactly twice", 2, ran);
        assertEquals("Check that task finished exactly twice", 2, finished);

        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) >= TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS));
        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) <= TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));

        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);
    }


    @Test
    public void testConcurrentAddNoDelay() throws InterruptedException {
        ScheduledExecutorService ses =
            Executors.newSingleThreadScheduledExecutor();

        final Object tc = this;
        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                synchronized (tc) {
                    ran += 1;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                synchronized (tc) {
                    finished += 1;
                    time = System.nanoTime();
                }
            }
        });

        long start = System.nanoTime();
        st1.reschedule(0, null);
        Thread.sleep(20);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);
        st1.reschedule(0, null);
        assertTrue("Check task running state true", st1.context.taskRunning);
        assertTrue("Check task should run state true", st1.context.taskShouldRun);
        assertEquals("Check that task started", 1, ran);
        assertEquals("Check that task not finished", 0, finished);

        Thread.sleep(150);

        assertTrue("Check task running state false", !st1.context.taskRunning);
        assertTrue("Check task should run state false", !st1.context.taskShouldRun);
        assertEquals("Check that task ran exactly twice", 2, ran);
        assertEquals("Check that task finished exactly twice", 2, finished);

        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) >= TimeUnit.NANOSECONDS.convert(90, TimeUnit.MILLISECONDS));
        assertTrue("Check that time passed appropriately: " + (time - start),
                (time - start) <= TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));

        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);
    }
}
