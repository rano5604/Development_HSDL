package com.mysql.jdbc.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





















public class Slf4JLogger
  implements Log
{
  private Logger log;
  
  public Slf4JLogger(String name)
  {
    log = LoggerFactory.getLogger(name);
  }
  
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }
  
  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }
  
  public boolean isFatalEnabled() {
    return log.isErrorEnabled();
  }
  
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }
  
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }
  
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }
  
  public void logDebug(Object msg) {
    log.debug(msg.toString());
  }
  
  public void logDebug(Object msg, Throwable thrown) {
    log.debug(msg.toString(), thrown);
  }
  
  public void logError(Object msg) {
    log.error(msg.toString());
  }
  
  public void logError(Object msg, Throwable thrown) {
    log.error(msg.toString(), thrown);
  }
  
  public void logFatal(Object msg) {
    log.error(msg.toString());
  }
  
  public void logFatal(Object msg, Throwable thrown) {
    log.error(msg.toString(), thrown);
  }
  
  public void logInfo(Object msg) {
    log.info(msg.toString());
  }
  
  public void logInfo(Object msg, Throwable thrown) {
    log.info(msg.toString(), thrown);
  }
  
  public void logTrace(Object msg) {
    log.trace(msg.toString());
  }
  
  public void logTrace(Object msg, Throwable thrown) {
    log.trace(msg.toString(), thrown);
  }
  
  public void logWarn(Object msg) {
    log.warn(msg.toString());
  }
  
  public void logWarn(Object msg, Throwable thrown) {
    log.warn(msg.toString(), thrown);
  }
}
