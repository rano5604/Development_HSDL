package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;





























public class JDBC4MysqlPooledConnection
  extends MysqlPooledConnection
{
  private final Map<StatementEventListener, StatementEventListener> statementEventListeners = new HashMap();
  
  public JDBC4MysqlPooledConnection(Connection connection) {
    super(connection);
  }
  
  public synchronized void close() throws SQLException {
    super.close();
    
    statementEventListeners.clear();
  }
  











  public void addStatementEventListener(StatementEventListener listener)
  {
    synchronized (statementEventListeners) {
      statementEventListeners.put(listener, listener);
    }
  }
  









  public void removeStatementEventListener(StatementEventListener listener)
  {
    synchronized (statementEventListeners) {
      statementEventListeners.remove(listener);
    }
  }
  
  void fireStatementEvent(StatementEvent event) throws SQLException {
    synchronized (statementEventListeners) {
      for (StatementEventListener listener : statementEventListeners.keySet()) {
        listener.statementClosed(event);
      }
    }
  }
}
