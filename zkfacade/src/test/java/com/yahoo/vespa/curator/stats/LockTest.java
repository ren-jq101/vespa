// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.curator.stats;

import com.yahoo.vespa.curator.Lock;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author hakon
 */
public class LockTest {
    private final InterProcessLock mutex = mock(InterProcessLock.class);
    private final String lockPath = "/lock/path";
    private final Duration acquireTimeout = Duration.ofSeconds(10);
    private final Lock lock = new Lock(lockPath, mutex);

    @Before
    public void setUp() {
        LockStats.clearForTesting();
    }

    @Test
    public void acquireThrows() throws Exception {
        Exception exception = new Exception("example curator exception");
        when(mutex.acquire(anyLong(), any())).thenThrow(exception);

        try {
            lock.acquire(acquireTimeout);
            fail();
        } catch (Exception e) {
            assertSame(e.getCause(), exception);
        }

        var expectedMetrics = new LockMetrics();
        expectedMetrics.setAcquireCount(1);
        expectedMetrics.setCumulativeAcquireCount(1);
        expectedMetrics.setAcquireFailedCount(1);
        expectedMetrics.setCumulativeAcquireFailedCount(1);
        assertLockMetrics(expectedMetrics);

        List<LockAttempt> slowLockAttempts = LockStats.getGlobal().getLockAttemptSamples();
        assertEquals(1, slowLockAttempts.size());
        LockAttempt slowLockAttempt = slowLockAttempts.get(0);
        assertEquals(acquireTimeout, slowLockAttempt.getAcquireTimeout());
        Optional<String> stackTrace = slowLockAttempt.getStackTrace();
        assertTrue(stackTrace.isPresent());
        assertTrue("bad stacktrace: " + stackTrace.get(), stackTrace.get().contains(".Lock.acquire(Lock.java"));
        assertEquals(LockAttempt.LockState.ACQUIRE_FAILED, slowLockAttempt.getLockState());
        assertTrue(slowLockAttempt.getTimeTerminalStateWasReached().isPresent());

        List<ThreadLockStats> threadLockStatsList = LockStats.getGlobal().getThreadLockStats();
        assertEquals(1, threadLockStatsList.size());
        ThreadLockStats threadLockStats = threadLockStatsList.get(0);
        assertEquals(0, threadLockStats.getOngoingLockAttempts().size());
    }

    private void assertLockMetrics(LockMetrics expected) {
        LockMetrics actual = LockStats.getGlobal().getLockMetricsByPath().get(lockPath);
        assertNotNull(actual);

        assertEquals(expected.getCumulativeAcquireCount(), actual.getCumulativeAcquireCount());
        assertEquals(expected.getCumulativeAcquireFailedCount(), actual.getCumulativeAcquireFailedCount());
        assertEquals(expected.getCumulativeAcquireTimedOutCount(), actual.getCumulativeAcquireTimedOutCount());
        assertEquals(expected.getCumulativeAcquireSucceededCount(), actual.getCumulativeAcquireSucceededCount());
        assertEquals(expected.getCumulativeReleaseCount(), actual.getCumulativeReleaseCount());
        assertEquals(expected.getCumulativeReleaseFailedCount(), actual.getCumulativeReleaseFailedCount());

        assertEquals(expected.getAndResetAcquireCount(), actual.getAndResetAcquireCount());
        assertEquals(expected.getAndResetAcquireFailedCount(), actual.getAndResetAcquireFailedCount());
        assertEquals(expected.getAndResetAcquireTimedOutCount(), actual.getAndResetAcquireTimedOutCount());
        assertEquals(expected.getAndResetAcquireSucceededCount(), actual.getAndResetAcquireSucceededCount());
        assertEquals(expected.getAndResetReleaseCount(), actual.getAndResetReleaseCount());
        assertEquals(expected.getAndResetReleaseFailedCount(), actual.getAndResetReleaseFailedCount());

        assertEquals(expected.getAcquiringNow(), actual.getAcquiringNow());
        assertEquals(expected.getLockedNow(), actual.getLockedNow());
    }

    @Test
    public void acquireTimesOut() throws Exception {
        when(mutex.acquire(anyLong(), any())).thenReturn(false);

        try {
            lock.acquire(acquireTimeout);
            fail();
        } catch (Exception e) {
            assertTrue("unexpected exception: " + e.getMessage(), e.getMessage().contains("Timed out"));
        }

        var expectedMetrics = new LockMetrics();
        expectedMetrics.setAcquireCount(1);
        expectedMetrics.setCumulativeAcquireCount(1);
        expectedMetrics.setAcquireTimedOutCount(1);
        expectedMetrics.setCumulativeAcquireTimedOutCount(1);
        assertLockMetrics(expectedMetrics);
    }

    @Test
    public void acquired() throws Exception {
        when(mutex.acquire(anyLong(), any())).thenReturn(true);

        lock.acquire(acquireTimeout);

        var expectedMetrics = new LockMetrics();
        expectedMetrics.setAcquireCount(1);
        expectedMetrics.setCumulativeAcquireCount(1);
        expectedMetrics.setAcquireSucceededCount(1);
        expectedMetrics.setCumulativeAcquireSucceededCount(1);
        expectedMetrics.setLockedNow(1);
        assertLockMetrics(expectedMetrics);

        // reenter
        // NB: non-cumulative counters are reset on fetch
        lock.acquire(acquireTimeout);
        expectedMetrics.setAcquireCount(1);  // reset to 0 above, + 1
        expectedMetrics.setCumulativeAcquireCount(2);
        expectedMetrics.setAcquireSucceededCount(1); // reset to 0 above, +1
        expectedMetrics.setCumulativeAcquireSucceededCount(2);
        expectedMetrics.setLockedNow(2);
        assertLockMetrics(expectedMetrics);

        // inner-most closes
        lock.close();
        expectedMetrics.setAcquireCount(0);  // reset to 0 above
        expectedMetrics.setAcquireSucceededCount(0); // reset to 0 above
        expectedMetrics.setReleaseCount(1);
        expectedMetrics.setCumulativeReleaseCount(1);
        expectedMetrics.setLockedNow(1);
        assertLockMetrics(expectedMetrics);

        // outer-most closes
        lock.close();
        expectedMetrics.setReleaseCount(1);  // reset to 0 above, +1
        expectedMetrics.setCumulativeReleaseCount(2);
        expectedMetrics.setLockedNow(0);
        assertLockMetrics(expectedMetrics);
    }

    @Test
    public void nestedLocks() throws Exception {
        when(mutex.acquire(anyLong(), any())).thenReturn(true);

        String lockPath2 = "/lock/path/2";
        Lock lock2 = new Lock(lockPath2, mutex);

        lock.acquire(acquireTimeout);
        lock2.acquire(acquireTimeout);

        List<ThreadLockStats> threadLockStats = LockStats.getGlobal().getThreadLockStats();
        assertEquals(1, threadLockStats.size());
        List<LockAttempt> lockAttempts = threadLockStats.get(0).getOngoingLockAttempts();
        assertEquals(2, lockAttempts.size());
        assertEquals(lockPath, lockAttempts.get(0).getLockPath());
        assertEquals(LockAttempt.LockState.ACQUIRED, lockAttempts.get(0).getLockState());
        assertEquals(lockPath2, lockAttempts.get(1).getLockPath());
        assertEquals(LockAttempt.LockState.ACQUIRED, lockAttempts.get(1).getLockState());

        lock.close();
        lock.close();
    }
}
