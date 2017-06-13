// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus.routing;

import com.yahoo.messagebus.ErrorCode;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class RetryPolicyTestCase extends TestCase {

    public void testSimpleRetryPolicy() {
        RetryTransientErrorsPolicy policy = new RetryTransientErrorsPolicy();
        for (int i = 0; i < 5; ++i) {
            double delay = i / 3.0;
            policy.setBaseDelay(delay);
            for (int j = 0; j < 5; ++j) {
                assertEquals((int)(j * delay), (int)policy.getRetryDelay(j));
            }
            for (int j = ErrorCode.NONE; j < ErrorCode.ERROR_LIMIT; ++j) {
                policy.setEnabled(true);
                if (j < ErrorCode.FATAL_ERROR) {
                    assertTrue(policy.canRetry(j));
                } else {
                    assertFalse(policy.canRetry(j));
                }
                policy.setEnabled(false);
                assertFalse(policy.canRetry(j));
            }
        }
    }
}
