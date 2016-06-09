/*
 *
 * Copyright (c) 2016 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 * @author Kevin Lee
 */
package com.serena.rlc.provider.schedule;

import com.serena.rlc.provider.BaseExecutionProvider;
import com.serena.rlc.provider.annotations.*;
import com.serena.rlc.provider.data.model.IActionInfo;
import com.serena.rlc.provider.domain.*;
import com.serena.rlc.provider.exceptions.ProviderException;
import com.serena.rlc.provider.schedule.client.ScheduleWaiter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class ScheduleExecutionProvider extends BaseExecutionProvider {

    final static Logger logger = LoggerFactory.getLogger(ScheduleExecutionProvider.class);
    final static String SCHEDULED_TIME = "scheduledTime";
    final static String DELAY_TIME = "delayTime";
    final static String WAIT_UNTIL = "waitUntil";
    final static String WAIT_FOR = "waitFor";

    //================================================================================
    // Configuration Properties
    // -------------------------------------------------------------------------------
    // The configuration properties are marked with the @ConfigProperty annotation
    // and will be displayed in the provider administration page when creating a 
    // configuration of this plugin for use.
    //================================================================================

    @ConfigProperty(
            name = "execution_provider_name",
            displayName = "Execution Provider Name",
            description = "provider name",
            defaultValue = "Schedule Provider",
            dataType = DataType.TEXT
    )
    private String providerName;
    @ConfigProperty(
            name = "execution_provider_description",
            displayName = "Execution Provider Description",
            description = "provider description",
            defaultValue = "",
            dataType = DataType.TEXT
    )
    private String providerDescription;
    @ConfigProperty(
            name = "schedule_dateFormat",
            displayName = "Date Format",
            description = "The format of the date string to be input",
            defaultValue = "yyyy-MM-dd HH:mm:ss",
            dataType = DataType.TEXT
    )
    private String dateFormat;
    @ConfigProperty(
            name = "schedule_callbackUrl",
            displayName = "RLC Callback URL",
            description = "The callback URL for Serena Release Control REST services",
            defaultValue = "http://localhost:8085/rlc/rest/taskexecutions/",
            dataType = DataType.TEXT
    )
    private String callbackUrl;
    @ConfigProperty(
            name = "schedule_callbackUsername",
            displayName = "RLC Callback Username",
            description = "The callback username for Serena Release Control REST services",
            defaultValue = "admin",
            dataType = DataType.TEXT
    )
    private String callbackUsername;
    @ConfigProperty(
            name = "schedule_callbackPassword",
            displayName = "RLC Callback Password",
            description = "The callback password for Serena Release Control REST services",
            defaultValue = "",
            dataType = DataType.PASSWORD
    )
    private String callbackPassword;


    @Override
    public String getProviderName() {
        return this.providerName;
    }

    @Autowired(required = false)
    @Override
    public void setProviderName(String providerName) {
        if (StringUtils.isNotEmpty(providerName)) {
            providerName = providerName.trim();
        }

        this.providerName = providerName;
    }

    @Override
    public String getProviderDescription() {
        return this.providerDescription;
    }

    @Autowired(required = false)
    @Override
    public void setProviderDescription(String providerDescription) {
        if (StringUtils.isNotEmpty(providerDescription)) {
            providerDescription = providerDescription.trim();
        }

        this.providerDescription = providerDescription;
    }

    @Autowired(required = false)
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    @Autowired(required = false)
    public void setCallbackUsername(String callbackUsername) {
        this.callbackUsername = callbackUsername;
    }

    public String getCallbackUsername() {
        return callbackUsername;
    }

    @Autowired(required = false)
    public void setCallbackPassword(String callbackPassword) {
        this.callbackPassword = callbackPassword;
    }

    public String getCallbackPassword() {
        return callbackPassword;
    }

    @Autowired(required = false)
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    protected String scheduledTime;
    protected String delayTime;

    //================================================================================
    // IExecutionProvider Overrides
    //================================================================================

    @Service(name = EXECUTE, displayName = "Schedule", description = "Schedule Task.")
    @Params(params = {@Param(fieldName = ACTION, displayName = "Action", description = "Schedule action to execute", required = true, dataType = DataType.SELECT),
            @Param(fieldName = PROPERTIES, description = "Schedule action properties", required = true)
    })
    public ExecutionInfo execute(String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        if (action.equalsIgnoreCase(WAIT_UNTIL))
            return waitUntil(properties, false);
        else if (action.equalsIgnoreCase(WAIT_FOR))
            return waitFor(properties, false);

        throw new ProviderException("Unsupported execution action: " + action);
    }

    @Service(name = VALIDATE, displayName = "Validate", description = "Validate Schedule action.")
    @Params(params = {@Param(fieldName = ACTION, description = "Schedule action to validate", required = true, dataType = DataType.SELECT),
            @Param(fieldName = PROPERTIES, description = "Schedule action properties", required = true)
    })
    @Override
    public ExecutionInfo validate(String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        if (action.equalsIgnoreCase(WAIT_UNTIL))
            return waitUntil(properties, true);
        else if (action.equalsIgnoreCase(WAIT_FOR))
            return waitFor(properties, true);

        throw new ProviderException("Unsupported execution action: " + action);
    }

    @Override
    public FieldInfo getFieldValues(String fieldName, List<Field> properties)
            throws ProviderException {
        if(!fieldName.equalsIgnoreCase(ACTION)) {
            /*if(fieldName.equalsIgnoreCase("product")) {
                return this.getProductFieldValues(fieldName);
            } else {
                throw new ProviderException("Unsupported get values for field name: " + fieldName);
            }*/
            return null;
        } else {
            ActionInfoResult results = this.getActions();
            if(results != null && results.getTotal() >= 1L) {
                FieldInfo fieldInfo = new FieldInfo(fieldName);
                ArrayList values = new ArrayList();
                IActionInfo[] arr$ = results.getResults();
                int len$ = arr$.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    IActionInfo actionInfo = arr$[i$];
                    values.add(new FieldValueInfo("name", actionInfo.getAction()));
                }

                fieldInfo.setValues(values);
                return fieldInfo;
            } else {
                return null;
            }
        }
    }

    //

    public Boolean validateWaitUntil(List<Field> properties) throws ProviderException {
        if (properties == null || properties.size() < 1)
            throw new ProviderException("Missing required field properties!");

        Field field = Field.getFieldByName(properties, SCHEDULED_TIME);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("A scheduled time needs to be supplied");
        } else {
            Date scheduledDate;
            try {
                scheduledDate = new SimpleDateFormat(dateFormat).parse(field.getValue());
            } catch (ParseException ex) {
                throw new ProviderException("Invalid scheduled time, format is " + dateFormat);
            }
            if (!new Date().before(scheduledDate)) throw new ProviderException("Scheduled time is not in the future");
            scheduledTime = field.getValue();
            logger.debug("Valid scheduled time of: " + scheduledTime);
        }

        return true;
    }

    @Action(name = WAIT_UNTIL, displayName = "Wait until (time)", description = "Wait until a specific time.")
    @Params(params = {@Param(fieldName = SCHEDULED_TIME, displayName = "Scheduled Time", description = "Scheduled time", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.TEXT)})
    public ExecutionInfo waitUntil(List<Field> properties, Boolean validateOnly) throws ProviderException {
        ExecutionInfo execInfo = new ExecutionInfo();
        try {
            Boolean bValid = validateWaitUntil(properties);
            if (validateOnly) {
                logger.debug("Validating waitUntil with: " + properties.toString());
                execInfo.setSuccess(bValid);
                execInfo.setMessage("Valid schedule action: " + WAIT_UNTIL);
                return execInfo;
            } else {
                UUID executionId = UUID.randomUUID();
                Date scheduledDate;
                try {
                    scheduledDate = new SimpleDateFormat(dateFormat).parse(scheduledTime);
                    logger.debug("Scheduled date is: " + scheduledDate.toString());
                } catch (ParseException ex) {
                    throw new ProviderException("Invalid scheduled time, format is " + dateFormat);
                }
                long sleepTime = scheduledDate.getTime() - (new Date()).getTime();
                try {
                    ScheduleWaiter scheduleWaiter = new ScheduleWaiter(callbackUrl, callbackUsername, callbackPassword, executionId, sleepTime);
                    Thread waiterThread = new Thread(scheduleWaiter, "ScheduleWaiter-"+executionId.toString());
                    waiterThread.start();
                } catch (Exception ex) {
                    throw new ProviderException(ex.getLocalizedMessage());
                }

                execInfo.setStatus(ExecutionStatus.IN_PROGRESS);
                execInfo.setExecutionId(executionId.toString());
                return execInfo;
            }
        } catch (ProviderException ex) {
            if (validateOnly) {
                execInfo.setSuccess(false);
                execInfo.setMessage(ex.getLocalizedMessage());
                return execInfo;
            }

            throw ex;
        }
    }

    public Boolean validateWaitFor(List<Field> properties) throws ProviderException {
        if (properties == null || properties.size() < 1)
            throw new ProviderException("Missing required field properties!");

        Field field = Field.getFieldByName(properties, DELAY_TIME);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("A delay time needs to be supplied");
        } else {
            if (!isNumeric(field.getValue())) throw new ProviderException("Delay time is not a valid number of minutes");
            delayTime = field.getValue();
            logger.debug("Valid delay time of: " + delayTime);
        }

        return true;
    }

    @Action(name = WAIT_FOR, displayName = "Wait for (minutes)", description = "Wait for a number of minutes.")
    @Params(params = {@Param(fieldName = DELAY_TIME, displayName = "Delay Time (mins)", description = "Delay time in minutes", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.NUMERIC)})
    public ExecutionInfo waitFor(List<Field> properties, Boolean validateOnly) throws ProviderException {
        ExecutionInfo execInfo = new ExecutionInfo();
        try {
            Boolean bValid = validateWaitFor(properties);
            if (validateOnly) {
                logger.debug("Validating waitFor with: " + properties.toString());
                execInfo.setSuccess(bValid);
                execInfo.setMessage("Valid schedule action: " + WAIT_FOR);
                return execInfo;
            } else {
                UUID executionId = UUID.randomUUID();
                long sleepTime = TimeUnit.MINUTES.toMillis(Long.parseLong(delayTime));
                try {
                    ScheduleWaiter scheduleWaiter = new ScheduleWaiter(callbackUrl, callbackUsername, callbackPassword, executionId, sleepTime);
                    Thread waiterThread = new Thread(scheduleWaiter, "ScheduleWaiter-"+executionId.toString());
                    waiterThread.start();
                } catch (Exception ex) {
                    throw new ProviderException(ex.getLocalizedMessage());
                }

                execInfo.setStatus(ExecutionStatus.IN_PROGRESS);
                execInfo.setExecutionId(executionId.toString());
                return execInfo;
            }
        } catch (ProviderException ex) {
            if (validateOnly) {
                execInfo.setSuccess(false);
                execInfo.setMessage(ex.getLocalizedMessage());
                return execInfo;
            }

            throw ex;
        }
    }

    private boolean isNumeric(String s) {
        return s.matches("\\d+");
    }

    private boolean isValidDate(String d) throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(d);
        return new Date().before(date);
    }

}
