/*
 *
 * Copyright (c) 2016 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 */

package com.serena.rlc.provider.schedule.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author klee
 */

public class ReleaseControlClientException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ReleaseControlClientException.class);

    public ReleaseControlClientException() {
    }

    public ReleaseControlClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReleaseControlClientException(String message) {
        super(message);
    }

    public ReleaseControlClientException(Throwable cause) {
        super(cause);
    }
}
