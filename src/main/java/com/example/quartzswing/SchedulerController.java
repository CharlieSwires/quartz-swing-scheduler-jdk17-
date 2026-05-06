package com.example.quartzswing;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.listeners.JobListenerSupport;

import javax.swing.SwingUtilities;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;

public class SchedulerController {
    private static final JobKey JOB_KEY = jobKey("userScheduledJob", "ui");
    private static final TriggerKey TRIGGER_KEY = triggerKey("userScheduledTrigger", "ui");

    private final Scheduler scheduler;
    private Consumer<String> logConsumer = message -> { };

    public SchedulerController() {
        try {
            this.scheduler = StdSchedulerFactory.getDefaultScheduler();
            addUiJobListener();
            this.scheduler.start();
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not start Quartz scheduler", e);
        }
    }

    public void setLogConsumer(Consumer<String> logConsumer) {
        this.logConsumer = Objects.requireNonNull(logConsumer);
        ExampleScheduledJob.setLogConsumer(this.logConsumer);
    }

    public boolean isScheduled() {
        try {
            return scheduler.checkExists(TRIGGER_KEY);
        } catch (SchedulerException e) {
            log("Could not check schedule: " + e.getMessage());
            return false;
        }
    }

    public void scheduleSimpleInterval(int seconds, String taskName, String scriptPath) {
        if (seconds < 1) {
            throw new IllegalArgumentException("Interval must be at least 1 second.");
        }

        JobDetail job = buildJob(taskName, scriptPath);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_KEY)
                .forJob(job)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(seconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();

        replaceSchedule(job, trigger);
        log("Scheduled simple interval: every " + seconds + " second(s).");
    }

    public void scheduleCron(String cronExpression, String taskName, String scriptPath) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("Cron expression cannot be blank.");
        }

        JobDetail job = buildJob(taskName, scriptPath);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_KEY)
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        replaceSchedule(job, trigger);
        log("Scheduled cron expression: " + cronExpression);
    }

    public void stopSchedule() {
        try {
            if (scheduler.checkExists(JOB_KEY)) {
                scheduler.deleteJob(JOB_KEY);
                log("Schedule stopped.");
            } else {
                log("No active schedule to stop.");
            }
        } catch (SchedulerException e) {
            log("Could not stop schedule: " + e.getMessage());
        }
    }

    public void runOnceNow(String taskName, String scriptPath) {
        JobDetail job = buildJob(taskName, scriptPath);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey("runOnceTrigger-" + System.nanoTime(), "ui"))
                .forJob(job)
                .startNow()
                .build();

        try {
            if (scheduler.checkExists(JOB_KEY)) {
                scheduler.addJob(job, true, true);
            } else {
                scheduler.scheduleJob(job, trigger);
                return;
            }
            scheduler.scheduleJob(trigger);
            log("Queued one immediate run.");
        } catch (SchedulerException e) {
            log("Could not run now: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown(true);
            }
        } catch (SchedulerException e) {
            log("Could not shut down scheduler cleanly: " + e.getMessage());
        }
    }

    private JobDetail buildJob(String taskName, String scriptPath) {
        JobDataMap map = new JobDataMap();
        map.put("taskName", taskName == null || taskName.isBlank() ? "Example scheduled process" : taskName.trim());
        map.put("scriptPath", scriptPath == null ? "" : scriptPath.trim());

        return JobBuilder.newJob(ExampleScheduledJob.class)
                .withIdentity(JOB_KEY)
                .usingJobData(map)
                .storeDurably(true)
                .build();
    }

    private void replaceSchedule(JobDetail job, Trigger trigger) {
        try {
            if (scheduler.checkExists(JOB_KEY)) {
                scheduler.deleteJob(JOB_KEY);
            }
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not schedule job", e);
        }
    }

    private void addUiJobListener() throws SchedulerException {
        JobListener listener = new JobListenerSupport() {
            @Override
            public String getName() {
                return "ui-job-listener";
            }

            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
                String taskName = context.getMergedJobDataMap().getString("taskName");
                if (jobException == null) {
                    log("Completed: " + taskName);
                } else {
                    log("Failed: " + taskName + " - " + jobException.getMessage());
                }
            }
        };

        scheduler.getListenerManager().addJobListener(listener, KeyMatcher.keyEquals(JOB_KEY));
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SwingUtilities.invokeLater(() -> logConsumer.accept("[" + timestamp + "] " + message));
    }
}
