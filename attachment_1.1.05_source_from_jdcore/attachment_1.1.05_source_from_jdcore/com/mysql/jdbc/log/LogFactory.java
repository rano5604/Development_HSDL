package com.mysql.jdbc.log;

import com.mysql.jdbc.ExceptionInterceptor;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.Util;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;





































public class LogFactory
{
  public LogFactory() {}
  
  public static Log getLogger(String className, String instanceName, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    if (className == null) {
      throw SQLError.createSQLException("Logger class can not be NULL", "S1009", exceptionInterceptor);
    }
    
    if (instanceName == null) {
      throw SQLError.createSQLException("Logger instance name can not be NULL", "S1009", exceptionInterceptor);
    }
    try
    {
      Class<?> loggerClass = null;
      try
      {
        loggerClass = Class.forName(className);
      } catch (ClassNotFoundException nfe) {
        loggerClass = Class.forName(Util.getPackageName(Log.class) + "." + className);
      }
      
      Constructor<?> constructor = loggerClass.getConstructor(new Class[] { String.class });
      
      return (Log)constructor.newInstance(new Object[] { instanceName });
    } catch (ClassNotFoundException cnfe) {
      SQLException sqlEx = SQLError.createSQLException("Unable to load class for logger '" + className + "'", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(cnfe);
      
      throw sqlEx;
    } catch (NoSuchMethodException nsme) {
      SQLException sqlEx = SQLError.createSQLException("Logger class does not have a single-arg constructor that takes an instance name", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(nsme);
      
      throw sqlEx;
    } catch (InstantiationException inse) {
      SQLException sqlEx = SQLError.createSQLException("Unable to instantiate logger class '" + className + "', exception in constructor?", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(inse);
      
      throw sqlEx;
    } catch (InvocationTargetException ite) {
      SQLException sqlEx = SQLError.createSQLException("Unable to instantiate logger class '" + className + "', exception in constructor?", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(ite);
      
      throw sqlEx;
    } catch (IllegalAccessException iae) {
      SQLException sqlEx = SQLError.createSQLException("Unable to instantiate logger class '" + className + "', constructor not public", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(iae);
      
      throw sqlEx;
    } catch (ClassCastException cce) {
      SQLException sqlEx = SQLError.createSQLException("Logger class '" + className + "' does not implement the '" + Log.class.getName() + "' interface", "S1009", exceptionInterceptor);
      
      sqlEx.initCause(cce);
      
      throw sqlEx;
    }
  }
}
