package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;






















public class LoadBalancedAutoCommitInterceptor
  implements StatementInterceptorV2
{
  private int matchingAfterStatementCount = 0;
  private int matchingAfterStatementThreshold = 0;
  private String matchingAfterStatementRegex;
  private ConnectionImpl conn;
  private LoadBalancedConnectionProxy proxy = null;
  
  private boolean countStatements = false;
  


  public LoadBalancedAutoCommitInterceptor() {}
  


  public void destroy() {}
  

  public boolean executeTopLevelOnly()
  {
    return false;
  }
  
  public void init(Connection connection, Properties props) throws SQLException {
    conn = ((ConnectionImpl)connection);
    
    String autoCommitSwapThresholdAsString = props.getProperty("loadBalanceAutoCommitStatementThreshold", "0");
    try {
      matchingAfterStatementThreshold = Integer.parseInt(autoCommitSwapThresholdAsString);
    }
    catch (NumberFormatException nfe) {}
    
    String autoCommitSwapRegex = props.getProperty("loadBalanceAutoCommitStatementRegex", "");
    if ("".equals(autoCommitSwapRegex)) {
      return;
    }
    matchingAfterStatementRegex = autoCommitSwapRegex;
  }
  








  public ResultSetInternalMethods postProcess(String sql, Statement interceptedStatement, ResultSetInternalMethods originalResultSet, Connection connection, int warningCount, boolean noIndexUsed, boolean noGoodIndexUsed, SQLException statementException)
    throws SQLException
  {
    if ((!countStatements) || (StringUtils.startsWithIgnoreCase(sql, "SET")) || (StringUtils.startsWithIgnoreCase(sql, "SHOW"))) {
      return originalResultSet;
    }
    

    if (!conn.getAutoCommit()) {
      matchingAfterStatementCount = 0;
      return originalResultSet;
    }
    
    if ((proxy == null) && (conn.isProxySet())) {
      MySQLConnection lcl_proxy = conn.getMultiHostSafeProxy();
      while ((lcl_proxy != null) && (!(lcl_proxy instanceof LoadBalancedMySQLConnection))) {
        lcl_proxy = lcl_proxy.getMultiHostSafeProxy();
      }
      if (lcl_proxy != null) {
        proxy = ((LoadBalancedMySQLConnection)lcl_proxy).getThisAsProxy();
      }
    }
    


    if (proxy == null) {
      return originalResultSet;
    }
    

    if ((matchingAfterStatementRegex == null) || (sql.matches(matchingAfterStatementRegex))) {
      matchingAfterStatementCount += 1;
    }
    

    if (matchingAfterStatementCount >= matchingAfterStatementThreshold) {
      matchingAfterStatementCount = 0;
      try {
        proxy.pickNewConnection();
      }
      catch (SQLException e) {}
    }
    


    return originalResultSet;
  }
  
  public ResultSetInternalMethods preProcess(String sql, Statement interceptedStatement, Connection connection) throws SQLException
  {
    return null;
  }
  
  void pauseCounters() {
    countStatements = false;
  }
  
  void resumeCounters() {
    countStatements = true;
  }
}
