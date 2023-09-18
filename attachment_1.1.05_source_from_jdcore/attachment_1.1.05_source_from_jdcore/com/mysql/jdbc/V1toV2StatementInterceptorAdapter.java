package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;





















public class V1toV2StatementInterceptorAdapter
  implements StatementInterceptorV2
{
  private final StatementInterceptor toProxy;
  
  public V1toV2StatementInterceptorAdapter(StatementInterceptor toProxy)
  {
    this.toProxy = toProxy;
  }
  
  public ResultSetInternalMethods postProcess(String sql, Statement interceptedStatement, ResultSetInternalMethods originalResultSet, Connection connection, int warningCount, boolean noIndexUsed, boolean noGoodIndexUsed, SQLException statementException) throws SQLException
  {
    return toProxy.postProcess(sql, interceptedStatement, originalResultSet, connection);
  }
  
  public void destroy() {
    toProxy.destroy();
  }
  
  public boolean executeTopLevelOnly() {
    return toProxy.executeTopLevelOnly();
  }
  
  public void init(Connection conn, Properties props) throws SQLException {
    toProxy.init(conn, props);
  }
  
  public ResultSetInternalMethods preProcess(String sql, Statement interceptedStatement, Connection connection) throws SQLException {
    return toProxy.preProcess(sql, interceptedStatement, connection);
  }
}
