package com.redhat.gss;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.logging.Logger;

@Singleton
@Startup
public class ResteasyDatasourceCleaner {
  
  private static Logger log = Logger.getLogger(ResteasyDatasourceCleaner.class);
  private static final long oneDay = 24 * 60 * 60 * 1000;
  private static final String FILE_PREFIX = "resteasy-provider-datasource";
  private File tempDir = null;

  @Resource
  private TimerService timerService;

  @Timeout
  public void cleanTempFiles() {
    log.debug("Cleaning temporary datasource files from temp dir");
    long now = System.currentTimeMillis();
    for(String tmpName : tempDir.list()) {
      if(tmpName.startsWith(FILE_PREFIX)) {
        File tmpFile = new File(tempDir + File.separator + tmpName);
        long lastModified = tmpFile.lastModified();
        long elapsedTime = now - lastModified;
        if(elapsedTime > oneDay) {
          tmpFile.delete();
        }
      }
    }
  }

  @PostConstruct
  public void schedule() {
    String tmpdir = System.getProperty("java.io.tmpdir");
    if(tmpdir == null || tmpdir.equals("")) {
      //Don't schedule if we can't get the temp dir
      return;
    }
    tempDir = new File(tmpdir);

    // Create a timer that is not persistent and runs every 5 minutes
    TimerConfig timerConfig = new TimerConfig();
    timerConfig.setPersistent(false);

    //Daily at midnight
    ScheduleExpression scheduleExpression = new ScheduleExpression();
    scheduleExpression.minute("0");
    scheduleExpression.hour("0");

    timerService.createCalendarTimer(scheduleExpression, timerConfig);
  }
}
