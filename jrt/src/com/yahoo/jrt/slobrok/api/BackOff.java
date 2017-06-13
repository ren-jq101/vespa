// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.jrt.slobrok.api;


class BackOff implements BackOffPolicy
{
    private double time = 0.50;

    public void reset() {
        time = 0.50;
    }

    public double get() {
        double ret = time;
        if (time < 5.0) {
            time += 0.5;
        } else if (time < 10.0) {
            time += 1.0;
        } else if (time < 30.0) {
            time += 5;
        } else {
            // max retry time is 30 seconds
        }
        return ret;
    }

    public boolean shouldWarn(double t) {
        return ((t > 8.1 && t < 9.9) || (t > 29.9));
    }
}
