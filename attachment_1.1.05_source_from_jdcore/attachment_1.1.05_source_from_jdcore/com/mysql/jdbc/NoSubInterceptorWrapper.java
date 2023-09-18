package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;

























public class NoSubInterceptorWrapper
  implements StatementInterceptorV2
{
  private final StatementInterceptorV2 underlyingInterceptor;
  
  public NoSubInterceptorWrapper(StatementInterceptorV2 underlyingInterceptor)
  {
    if (underlyingInterceptor == null) {
      throw new RuntimeException("Interceptor to be wrapped can not be NULL");
    }
    
    this.underlyingInterceptor = underlyingInterceptor;
  }
  
  public void destroy() {
    underlyingInterceptor.destroy();
  }
  
  public boolean executeTopLevelOnly() {
    return underlyingInterceptor.executeTopLevelOnly();
  }
  
  public void init(Connection conn, Properties props) throws SQLException {
    underlyingInterceptor.init(conn, props);
  }
  
  public ResultSetInternalMethods postProcess(String sql, Statement interceptedStatement, ResultSetInternalMethods originalResultSet, Connection connection, int warningCount, boolean noIndexUsed, boolean noGoodIndexUsed, SQLException statementException) throws SQLException
  {
    underlyingInterceptor.postProcess(sql, interceptedStatement, originalResultSet, connection, warningCount, noIndexUsed, noGoodIndexUsed, statementException);
    

    return null;
  }
  
  public ResultSetInternalMethods preProcess(String sql, Statement interceptedStatement, Connection connection) throws SQLException {
    underlyingInterceptor.preProcess(sql, interceptedStatement, connection);
    
    return null;
  }
  
  public StatementInterceptorV2 getUnderlyingInterceptor() {
    return underlyingInterceptor;
  }
}
