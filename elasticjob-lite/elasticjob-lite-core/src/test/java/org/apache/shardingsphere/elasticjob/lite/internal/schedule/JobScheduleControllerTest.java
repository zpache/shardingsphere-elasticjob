/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.elasticjob.lite.internal.schedule;

import org.apache.shardingsphere.elasticjob.infra.exception.JobSystemException;
import org.apache.shardingsphere.elasticjob.lite.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public final class JobScheduleControllerTest {
    
    @Mock
    private Scheduler scheduler;
    
    @Mock
    private JobDetail jobDetail;
    
    private JobScheduleController jobScheduleController;
    
    @BeforeEach
    public void setUp() {
        jobScheduleController = new JobScheduleController(scheduler, jobDetail, "test_job_Trigger");
    }
    
    @Test
    public void assertIsPausedFailure() {
        assertThrows(JobSystemException.class, () -> {
            doThrow(SchedulerException.class).when(scheduler).getTriggerState(new TriggerKey("test_job_Trigger"));
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.isPaused();
            } finally {
                verify(scheduler).getTriggerState(new TriggerKey("test_job_Trigger"));
            }
        });
    }
    
    @Test
    public void assertIsPausedIfTriggerStateIsNormal() throws SchedulerException {
        when(scheduler.getTriggerState(new TriggerKey("test_job_Trigger"))).thenReturn(Trigger.TriggerState.NORMAL);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        assertFalse(jobScheduleController.isPaused());
    }
    
    @Test
    public void assertIsPausedIfTriggerStateIsPaused() throws SchedulerException {
        when(scheduler.getTriggerState(new TriggerKey("test_job_Trigger"))).thenReturn(Trigger.TriggerState.PAUSED);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        assertTrue(jobScheduleController.isPaused());
    }
    
    @Test
    public void assertIsPauseJobIfShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        assertFalse(jobScheduleController.isPaused());
    }
    
    @Test
    public void assertPauseJobIfShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.pauseJob();
        verify(scheduler, times(0)).pauseAll();
    }
    
    @Test
    public void assertPauseJobFailure() {
        assertThrows(JobSystemException.class, () -> {
            doThrow(SchedulerException.class).when(scheduler).pauseAll();
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.pauseJob();
            } finally {
                verify(scheduler).pauseAll();
            }
        });
    }
    
    @Test
    public void assertPauseJobSuccess() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.pauseJob();
        verify(scheduler).pauseAll();
    }
    
    @Test
    public void assertResumeJobIfShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.resumeJob();
        verify(scheduler, times(0)).resumeAll();
    }
    
    @Test
    public void assertResumeJobFailure() {
        assertThrows(JobSystemException.class, () -> {
            doThrow(SchedulerException.class).when(scheduler).resumeAll();
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.resumeJob();
            } finally {
                verify(scheduler).resumeAll();
            }
        });
    }
    
    @Test
    public void assertResumeJobSuccess() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.resumeJob();
        verify(scheduler).resumeAll();
    }
    
    @Test
    public void assertTriggerJobIfShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        ReflectionUtils.setFieldValue(jobScheduleController, "jobDetail", jobDetail);
        jobScheduleController.triggerJob();
        verify(jobDetail, times(0)).getKey();
        verify(scheduler, times(0)).triggerJob(any());
    }
    
    @Test
    public void assertTriggerJobFailure() {
        assertThrows(JobSystemException.class, () -> {
            JobKey jobKey = new JobKey("test_job");
            when(jobDetail.getKey()).thenReturn(jobKey);
            when(scheduler.checkExists(jobKey)).thenReturn(true);
            doThrow(SchedulerException.class).when(scheduler).triggerJob(jobKey);
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            ReflectionUtils.setFieldValue(jobScheduleController, "jobDetail", jobDetail);
            try {
                jobScheduleController.triggerJob();
            } finally {
                verify(jobDetail, times(2)).getKey();
                verify(scheduler).triggerJob(jobKey);
            }
        });
    }
    
    @Test
    public void assertTriggerJobSuccess() throws SchedulerException {
        JobKey jobKey = new JobKey("test_job");
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        ReflectionUtils.setFieldValue(jobScheduleController, "jobDetail", jobDetail);
        jobScheduleController.triggerJob();
        verify(jobDetail, times(2)).getKey();
        verify(scheduler).triggerJob(jobKey);
    }
    
    @Test
    public void assertTriggerOneOffJobSuccess() throws SchedulerException {
        JobKey jobKey = new JobKey("test_job");
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(scheduler.checkExists(jobDetail.getKey())).thenReturn(false);
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        ReflectionUtils.setFieldValue(jobScheduleController, "jobDetail", jobDetail);
        jobScheduleController.triggerJob();
        verify(jobDetail, times(2)).getKey();
        verify(scheduler).scheduleJob(eq(jobDetail), any(Trigger.class));
        verify(scheduler).start();
    }
    
    @Test
    public void assertShutdownJobIfShutdown() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        when(scheduler.isShutdown()).thenReturn(true);
        jobScheduleController.shutdown();
        verify(scheduler, times(0)).shutdown();
    }
    
    @Test
    public void assertShutdownFailure() {
        assertThrows(JobSystemException.class, () -> {
            doThrow(SchedulerException.class).when(scheduler).shutdown(false);
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.shutdown();
            } finally {
                verify(scheduler).shutdown(false);
            }
        });
    }
    
    @Test
    public void assertShutdownSuccess() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.shutdown();
        verify(scheduler).shutdown(false);
    }
    
    @Test
    public void assertRescheduleJobIfShutdown() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        when(scheduler.isShutdown()).thenReturn(true);
        jobScheduleController.rescheduleJob("0/1 * * * * ?", null);
        verify(scheduler, times(0)).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
    
    @Test
    public void assertRescheduleJobFailure() {
        assertThrows(JobSystemException.class, () -> {
            when(scheduler.getTrigger(TriggerKey.triggerKey("test_job_Trigger"))).thenReturn(new CronTriggerImpl());
            doThrow(SchedulerException.class).when(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.rescheduleJob("0/1 * * * * ?", null);
            } finally {
                verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
            }
        });
    }
    
    @Test
    public void assertRescheduleJobSuccess() throws SchedulerException {
        when(scheduler.getTrigger(TriggerKey.triggerKey("test_job_Trigger"))).thenReturn(new CronTriggerImpl());
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.rescheduleJob("0/1 * * * * ?", null);
        verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
    
    @Test
    public void assertRescheduleJobWhenTriggerIsNull() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.rescheduleJob("0/1 * * * * ?", null);
        verify(scheduler, times(0)).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
    
    @Test
    public void assertRescheduleJobIfShutdownForOneOffJob() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        when(scheduler.isShutdown()).thenReturn(true);
        jobScheduleController.rescheduleJob();
        verify(scheduler, times(0)).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
    
    @Test
    public void assertRescheduleJobFailureForOneOffJob() {
        assertThrows(JobSystemException.class, () -> {
            when(scheduler.getTrigger(TriggerKey.triggerKey("test_job_Trigger"))).thenReturn(new SimpleTriggerImpl());
            doThrow(SchedulerException.class).when(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
            ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
            try {
                jobScheduleController.rescheduleJob();
            } finally {
                verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
            }
        });
    }
    
    @Test
    public void assertRescheduleJobSuccessForOneOffJob() throws SchedulerException {
        when(scheduler.getTrigger(TriggerKey.triggerKey("test_job_Trigger"))).thenReturn(new SimpleTriggerImpl());
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.rescheduleJob();
        verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
    
    @Test
    public void assertRescheduleJobWhenTriggerIsNullForOneOffJob() throws SchedulerException {
        ReflectionUtils.setFieldValue(jobScheduleController, "scheduler", scheduler);
        jobScheduleController.rescheduleJob();
        verify(scheduler, times(0)).rescheduleJob(eq(TriggerKey.triggerKey("test_job_Trigger")), any());
    }
}
