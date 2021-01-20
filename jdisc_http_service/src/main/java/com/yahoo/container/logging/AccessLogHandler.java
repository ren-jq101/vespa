// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.logging;

import com.yahoo.container.core.AccessLogConfig;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Bjorn Borud
 */
class AccessLogHandler {

    private final LogFileHandler logFileHandler;

    AccessLogHandler(AccessLogConfig.FileHandler config) {
        LogFormatter lf = new LogFormatter();
        lf.messageOnly(true);
        logFileHandler = new LogFileHandler(toCompression(config), config.pattern(), config.rotation(), config.symlink(), lf);
    }

    public void log(String message) {
        logFileHandler.publish(new LogRecord(Level.INFO, message));
    }

    private LogFileHandler.Compression toCompression(AccessLogConfig.FileHandler config) {
        if (!config.compressOnRotation()) return LogFileHandler.Compression.NONE;
        switch (config.compressionFormat()) {
            case ZSTD: return LogFileHandler.Compression.ZSTD;
            case GZIP: return LogFileHandler.Compression.GZIP;
            default: throw new IllegalArgumentException(config.compressionFormat().toString());
        }
    }

    void shutdown() {
        logFileHandler.close();

        if (logFileHandler!=null)
            logFileHandler.shutdown();
    }

    void rotateNow() {
        logFileHandler.rotateNow();
    }
}
