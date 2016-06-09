/*
 *
 * Copyright (c) 2016 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 */

package com.serena.rlc.provider.schedule.client;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CancellationException;

public class ScheduleWaiter implements Runnable {

    final static Logger logger = LoggerFactory.getLogger(ScheduleWaiter.class);

    String callbackUrl;
    String callbackUsername;
    String callbackPassword;
    UUID executionId;
    long waitTime;
    Date endDate;

    public ScheduleWaiter(String callbackUrl, String callbackUsername, String callbackPassword, UUID executionId, long waitTime) {
        if (callbackUrl.endsWith("/")) {
            this.callbackUrl = callbackUrl;
        } else {
            this.callbackUrl = callbackUrl + "/";
        }
        this.callbackUsername = callbackUsername;
        this.callbackPassword = callbackPassword;
        this.executionId = executionId;
        this.waitTime = waitTime;
        this.endDate = new Date(System.currentTimeMillis() + waitTime);
    }

    @Override
    public void run() {
        logger.debug("ScheduleWaiter is sleeping for " + waitTime + " milliseconds, until " + endDate);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ex) {
            logger.error("ScheduleWaiter was interrupted: " + ex.getLocalizedMessage());
        } catch (CancellationException ex) {
            logger.error("ScheduleWaiter thread has been cancelled: ", ex.getLocalizedMessage());
        }
        synchronized (executionId) {
            logger.debug("ScheduleWaiter has finished sleeping at " + new Date());

            try {
                String uri = callbackUrl + executionId + "/COMPLETED";
                logger.info("Start executing RLC PUT request to url=\"{}\"", uri);
                DefaultHttpClient rlcParams = new DefaultHttpClient();
                HttpPut put = new HttpPut(uri);
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(callbackUsername, callbackPassword);
                put.addHeader(BasicScheme.authenticate(credentials, "US-ASCII", false));
                //put.addHeader("Content-Type", "application/x-www-form-urlencoded");
                //put.addHeader("Accept", "application/json");
                logger.info(credentials.toString());
                HttpResponse response = rlcParams.execute(put);
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.error("HTTP Status Code: " + response.getStatusLine().getStatusCode());
                }
            } catch (IOException ex) {
                logger.error(ex.getLocalizedMessage());
            }

            executionId.notify();
        }

    }

}
