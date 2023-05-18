package org.kabieror.elwasys.raspiclient.ui.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests für den InactivityScheduler
 *
 * @author Oliver Kabierschke
 */
public class InactivitySchedulerTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private AtomicInteger executionCounter = new AtomicInteger(0);

    @Test
    public void testTimeUnits() throws InterruptedException {
        InactivityScheduler sched = new InactivityScheduler();

        logger.info("Testing time unit nanoseconds");
        testTimeUnit(sched, 55, TimeUnit.NANOSECONDS, 2);
        this.executionCounter.set(0);

        logger.info("Testing time unit microseconds");
        testTimeUnit(sched, 10, TimeUnit.MICROSECONDS, 2);
        this.executionCounter.set(0);

        logger.info("Testing time unit milliseconds");
        testTimeUnit(sched, 5, TimeUnit.MILLISECONDS, 6);
        this.executionCounter.set(0);

        logger.info("Testing time unit seconds");
        testTimeUnit(sched, 1, TimeUnit.SECONDS, 1002);
        this.executionCounter.set(0);

        sched.shutdown();
    }

    private void testTimeUnit(InactivityScheduler sched, int rate, TimeUnit timeUnit, int waitMs)
            throws InterruptedException {
        InactivityFuture future = sched.scheduleJob(() -> executionCounter.incrementAndGet(), rate, timeUnit, 1);
        Thread.sleep(waitMs);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Assert.assertEquals(executionCounter.get(), 1);

        sched.shutdown();
    }

    @Test
    public void testMultipleExecutions() throws InterruptedException {
        this.executionCounter.set(0);

        InactivityScheduler sched = new InactivityScheduler();
        InactivityFuture future =
                sched.scheduleJob(() -> this.executionCounter.incrementAndGet(), 50, TimeUnit.MILLISECONDS, 5);
        Thread.sleep(52);
        Assert.assertEquals(this.executionCounter.get(), 1);
        Assert.assertFalse(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Thread.sleep(50);
        Assert.assertEquals(this.executionCounter.get(), 2);
        Assert.assertFalse(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Thread.sleep(50);
        Assert.assertEquals(this.executionCounter.get(), 3);
        Assert.assertFalse(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Thread.sleep(50);
        Assert.assertEquals(this.executionCounter.get(), 4);
        Assert.assertFalse(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Thread.sleep(50);
        Assert.assertEquals(this.executionCounter.get(), 5);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());

        sched.shutdown();
    }

    @Test
    public void testActivity() throws InterruptedException {
        this.executionCounter.set(0);
        InactivityScheduler sched = new InactivityScheduler();
        InactivityFuture future =
                sched.scheduleJob(() -> this.executionCounter.incrementAndGet(), 50, TimeUnit.MILLISECONDS, 1);
        Thread.sleep(40);
        sched.onActivityDetected();
        Thread.sleep(20);
        Assert.assertFalse(future.isDone());
        Assert.assertEquals(this.executionCounter.get(), 0);
        sched.onActivityDetected();
        Thread.sleep(45);
        Assert.assertFalse(future.isDone());
        Assert.assertEquals(this.executionCounter.get(), 0);
        Thread.sleep(10);
        Assert.assertTrue(future.isDone());
        Assert.assertEquals(this.executionCounter.get(), 1);

        sched.shutdown();
    }

    @Test
    public void testCancel() throws InterruptedException {
        this.executionCounter.set(0);
        InactivityScheduler sched = new InactivityScheduler();
        InactivityFuture future =
                sched.scheduleJob(() -> this.executionCounter.incrementAndGet(), 50, TimeUnit.MILLISECONDS, 1);
        Thread.sleep(40);
        future.cancel();
        Thread.sleep(20);
        Assert.assertEquals(this.executionCounter.get(), 0);
        Assert.assertTrue(future.isDone());
        Assert.assertTrue(future.isCancelled());

        sched.shutdown();
    }
}
