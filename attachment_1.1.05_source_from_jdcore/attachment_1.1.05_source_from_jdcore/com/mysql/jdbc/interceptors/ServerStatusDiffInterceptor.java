package com.mysql.jdbc.interceptors;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ResultSetInternalMethods;
import com.mysql.jdbc.StatementInterceptor;
import com.mysql.jdbc.Util;
import com.mysql.jdbc.log.Log;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;























public class ServerStatusDiffInterceptor
  implements StatementInterceptor
{
  private Map<String, String> preExecuteValues = new HashMap();
  
  private Map<String, String> postExecuteValues = new HashMap();
  
  public ServerStatusDiffInterceptor() {}
  
  public void init(Connection conn, Properties props) throws SQLException
  {}
  
  public ResultSetInternalMethods postProcess(String sql, com.mysql.jdbc.Statement interceptedStatement, ResultSetInternalMethods originalResultSet, Connection connection) throws SQLException
  {
    if (connection.versionMeetsMinimum(5, 0, 2)) {
      populateMapWithSessionStatusValues(connection, postExecuteValues);
      
      connection.getLog().logInfo("Server status change for statement:\n" + Util.calculateDifferences(preExecuteValues, postExecuteValues));
    }
    
    return null;
  }
  
  private void populateMapWithSessionStatusValues(Connection connection, Map<String, String> toPopulate) throws SQLException
  {
    java.sql.Statement stmt = null;
    ResultSet rs = null;
    try
    {
      toPopulate.clear();
      
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SHOW SESSION STATUS");
      Util.resultSetToMap(toPopulate, rs);
    } finally {
      if (rs != null) {
        rs.close();
      }
      
      if (stmt != null) {
        stmt.close();
      }
    }
  }
  
  public ResultSetInternalMethods preProcess(String sql, com.mysql.jdbc.Statement interceptedStatement, Connection connection) throws SQLException
  {
    if (connection.versionMeetsMinimum(5, 0, 2)) {
      populateMapWithSessionStatusValues(connection, preExecuteValues);
    }
    
    return null;
  }
  
  public boolean executeTopLevelOnly() {
    return true;
  }
  
  public void destroy() {}
}
