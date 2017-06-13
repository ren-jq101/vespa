// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus.jdisc;

import com.yahoo.jdisc.Response;
import com.yahoo.messagebus.Reply;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class MbusResponse extends Response {

    private final Reply reply;

    public MbusResponse(int status, Reply reply) {
        super(status);
        if (reply == null) {
            throw new NullPointerException();
        }
        this.reply = reply;
    }

    public Reply getReply() {
        return reply;
    }
}
