/*
 * Copyright 2011, Big Switch Networks, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class SingletonTaskTest {

    private int ran;
    private int finished;
    private long time;

    @Before
    public void init() {
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
        ran = 0;

        SingletonTask st1 = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
                ran++;
                time = System.nanoTime();
            }
        });
        long start = System.nanoTime();
        st1.reschedule(10, TimeUnit.MILLISECONDS);
        assertFalse("Task has run already", ran > 0);

        ses.shutdown();
        ses.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals("Check that task ran only once failed", 1, ran);
        assertTrue("Check that time passed appropriately",
                   (time - start) >= TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testReschedule() throws InterruptedException {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ran = 0;
        time = 0;

        final long EPSILON_NS = 1500000; // 1.5 ms
        final int DELAY_MS = 20;
        final int NUM_OF_ITERATIONS = 8;
        final int SLEEP_MS = 5;
        final int AWAIT_SEC = 5;
        final int EXPECTED_PASS_TIME =
                NUM_OF_ITERATIONS * SLEEP_MS + (DELAY_MS - SLEEP_MS);

        final Object tc = this;
        SingletonTask st1 = new SingletonTask(ses, new Runnable() {

            @Override
            public void run() {
                synchronized (tc) {
                    ran++;
                }
                time = System.nanoTime();
            }
        });

        long start = System.nanoTime();
        for (int i = 0; i < NUM_OF_ITERATIONS; i++) {
            st1.reschedule(DELAY_MS, TimeUnit.MILLISECONDS);
            Thread.sleep(SLEEP_MS);
            assertFalse("Task has run already", ran > 0);
        }

        ses.shutdown();
        ses.awaitTermination(AWAIT_SEC, TimeUnit.SECONDS);

        assertEquals("Check that task ran only once failed", 1, ran);
        assertTrue("Check that time passed appropriately: " + (time - start),
                 (time - start) >= 1000000 * EXPECTED_PASS_TIME - EPSILON_NS);
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
