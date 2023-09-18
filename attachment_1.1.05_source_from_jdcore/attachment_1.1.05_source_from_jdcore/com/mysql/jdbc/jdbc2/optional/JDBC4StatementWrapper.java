package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.SQLError;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;









































public class JDBC4StatementWrapper
  extends StatementWrapper
{
  public JDBC4StatementWrapper(ConnectionWrapper c, MysqlPooledConnection conn, Statement toWrap)
  {
    super(c, conn, toWrap);
  }
  
  public void close() throws SQLException {
    try {
      super.close();
      
      unwrappedInterfaces = null; } finally { unwrappedInterfaces = null;
    }
  }
  
  public boolean isClosed() throws SQLException {
    try {
      if (wrappedStmt != null) {
        return wrappedStmt.isClosed();
      }
      throw SQLError.createSQLException("Statement already closed", "S1009", exceptionInterceptor);
    }
    catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
    
    return false;
  }
  
  public void setPoolable(boolean poolable) throws SQLException {
    try {
      if (wrappedStmt != null) {
        wrappedStmt.setPoolable(poolable);
      } else {
        throw SQLError.createSQLException("Statement already closed", "S1009", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  
  public boolean isPoolable() throws SQLException {
    try {
      if (wrappedStmt != null) {
        return wrappedStmt.isPoolable();
      }
      throw SQLError.createSQLException("Statement already closed", "S1009", exceptionInterceptor);
    }
    catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
    
    return false;
  }
  



















  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    boolean isInstance = iface.isInstance(this);
    
    if (isInstance) {
      return true;
    }
    
    String interfaceClassName = iface.getName();
    
    return (interfaceClassName.equals("com.mysql.jdbc.Statement")) || (interfaceClassName.equals("java.sql.Statement")) || 
      (interfaceClassName.equals("java.sql.Wrapper"));
  }
  
















  public synchronized <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try
    {
      if (("java.sql.Statement".equals(iface.getName())) || ("java.sql.Wrapper.class".equals(iface.getName()))) {
        return iface.cast(this);
      }
      
      if (unwrappedInterfaces == null) {
        unwrappedInterfaces = new HashMap();
      }
      
      Object cachedUnwrapped = unwrappedInterfaces.get(iface);
      
      if (cachedUnwrapped == null) {
        cachedUnwrapped = Proxy.newProxyInstance(wrappedStmt.getClass().getClassLoader(), new Class[] { iface }, new WrapperBase.ConnectionErrorFiringInvocationHandler(this, wrappedStmt));
        
        unwrappedInterfaces.put(iface, cachedUnwrapped);
      }
      
      return iface.cast(cachedUnwrapped);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", exceptionInterceptor);
    }
  }
}
