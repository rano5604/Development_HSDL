package com.mysql.jdbc.profiler;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.log.Log;
import java.sql.SQLException;
import java.util.Properties;


























public class LoggingProfilerEventHandler
  implements ProfilerEventHandler
{
  private Log log;
  
  public LoggingProfilerEventHandler() {}
  
  public void consumeEvent(ProfilerEvent evt)
  {
    if (eventType == 0) {
      log.logWarn(evt);
    } else {
      log.logInfo(evt);
    }
  }
  
  public void destroy() {
    log = null;
  }
  
  public void init(Connection conn, Properties props) throws SQLException {
    log = conn.getLog();
  }
}
