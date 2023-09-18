package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.Messages;
import com.mysql.jdbc.StringUtils;
import com.mysql.jdbc.Util;
import com.mysql.jdbc.log.Log;
import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;









































public class MysqlXAConnection
  extends MysqlPooledConnection
  implements XAConnection, XAResource
{
  private static final int MAX_COMMAND_LENGTH = 300;
  private com.mysql.jdbc.Connection underlyingConnection;
  private static final Map<Integer, Integer> MYSQL_ERROR_CODES_TO_XA_ERROR_CODES;
  private Log log;
  protected boolean logXaCommands;
  private static final Constructor<?> JDBC_4_XA_CONNECTION_WRAPPER_CTOR;
  
  static
  {
    HashMap<Integer, Integer> temp = new HashMap();
    
    temp.put(Integer.valueOf(1397), Integer.valueOf(-4));
    temp.put(Integer.valueOf(1398), Integer.valueOf(-5));
    temp.put(Integer.valueOf(1399), Integer.valueOf(-7));
    temp.put(Integer.valueOf(1400), Integer.valueOf(-9));
    temp.put(Integer.valueOf(1401), Integer.valueOf(-3));
    temp.put(Integer.valueOf(1402), Integer.valueOf(100));
    temp.put(Integer.valueOf(1440), Integer.valueOf(-8));
    temp.put(Integer.valueOf(1613), Integer.valueOf(106));
    temp.put(Integer.valueOf(1614), Integer.valueOf(102));
    
    MYSQL_ERROR_CODES_TO_XA_ERROR_CODES = Collections.unmodifiableMap(temp);
    




    if (Util.isJdbc4()) {
      try {
        JDBC_4_XA_CONNECTION_WRAPPER_CTOR = Class.forName("com.mysql.jdbc.jdbc2.optional.JDBC4MysqlXAConnection").getConstructor(new Class[] { com.mysql.jdbc.Connection.class, Boolean.TYPE });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_XA_CONNECTION_WRAPPER_CTOR = null;
    }
  }
  
  protected static MysqlXAConnection getInstance(com.mysql.jdbc.Connection mysqlConnection, boolean logXaCommands) throws SQLException {
    if (!Util.isJdbc4()) {
      return new MysqlXAConnection(mysqlConnection, logXaCommands);
    }
    
    return (MysqlXAConnection)Util.handleNewInstance(JDBC_4_XA_CONNECTION_WRAPPER_CTOR, new Object[] { mysqlConnection, Boolean.valueOf(logXaCommands) }, mysqlConnection.getExceptionInterceptor());
  }
  


  public MysqlXAConnection(com.mysql.jdbc.Connection connection, boolean logXaCommands)
    throws SQLException
  {
    super(connection);
    underlyingConnection = connection;
    log = connection.getLog();
    this.logXaCommands = logXaCommands;
  }
  







  public XAResource getXAResource()
    throws SQLException
  {
    return this;
  }
  











  public int getTransactionTimeout()
    throws XAException
  {
    return 0;
  }
  



















  public boolean setTransactionTimeout(int arg0)
    throws XAException
  {
    return false;
  }
  














  public boolean isSameRM(XAResource xares)
    throws XAException
  {
    if ((xares instanceof MysqlXAConnection)) {
      return underlyingConnection.isSameResource(underlyingConnection);
    }
    
    return false;
  }
  



































  public Xid[] recover(int flag)
    throws XAException
  {
    return recover(underlyingConnection, flag);
  }
  



















  protected static Xid[] recover(java.sql.Connection c, int flag)
    throws XAException
  {
    boolean startRscan = (flag & 0x1000000) > 0;
    boolean endRscan = (flag & 0x800000) > 0;
    
    if ((!startRscan) && (!endRscan) && (flag != 0)) {
      throw new MysqlXAException(-5, Messages.getString("MysqlXAConnection.001"), null);
    }
    






    if (!startRscan) {
      return new Xid[0];
    }
    
    ResultSet rs = null;
    Statement stmt = null;
    
    List<MysqlXid> recoveredXidList = new ArrayList();
    
    try
    {
      stmt = c.createStatement();
      
      rs = stmt.executeQuery("XA RECOVER");
      
      while (rs.next()) {
        int formatId = rs.getInt(1);
        int gtridLength = rs.getInt(2);
        int bqualLength = rs.getInt(3);
        byte[] gtridAndBqual = rs.getBytes(4);
        
        byte[] gtrid = new byte[gtridLength];
        byte[] bqual = new byte[bqualLength];
        
        if (gtridAndBqual.length != gtridLength + bqualLength) {
          throw new MysqlXAException(105, Messages.getString("MysqlXAConnection.002"), null);
        }
        
        System.arraycopy(gtridAndBqual, 0, gtrid, 0, gtridLength);
        System.arraycopy(gtridAndBqual, gtridLength, bqual, 0, bqualLength);
        
        recoveredXidList.add(new MysqlXid(gtrid, bqual, formatId));
      }
    } catch (SQLException sqlEx) {
      throw mapXAExceptionFromSQLException(sqlEx);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException sqlEx) {
          throw mapXAExceptionFromSQLException(sqlEx);
        }
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException sqlEx) {
          throw mapXAExceptionFromSQLException(sqlEx);
        }
      }
    }
    
    int numXids = recoveredXidList.size();
    
    Xid[] asXids = new Xid[numXids];
    Object[] asObjects = recoveredXidList.toArray();
    
    for (int i = 0; i < numXids; i++) {
      asXids[i] = ((Xid)asObjects[i]);
    }
    
    return asXids;
  }
  
















  public int prepare(Xid xid)
    throws XAException
  {
    StringBuilder commandBuf = new StringBuilder(300);
    commandBuf.append("XA PREPARE ");
    appendXid(commandBuf, xid);
    
    dispatchCommand(commandBuf.toString());
    
    return 0;
  }
  






























  public void rollback(Xid xid)
    throws XAException
  {
    StringBuilder commandBuf = new StringBuilder(300);
    commandBuf.append("XA ROLLBACK ");
    appendXid(commandBuf, xid);
    try
    {
      dispatchCommand(commandBuf.toString());
    } finally {
      underlyingConnection.setInGlobalTx(false);
    }
  }
  


























  public void end(Xid xid, int flags)
    throws XAException
  {
    StringBuilder commandBuf = new StringBuilder(300);
    commandBuf.append("XA END ");
    appendXid(commandBuf, xid);
    
    switch (flags) {
    case 67108864: 
      break;
    case 33554432: 
      commandBuf.append(" SUSPEND");
      break;
    case 536870912: 
      break;
    default: 
      throw new XAException(-5);
    }
    
    dispatchCommand(commandBuf.toString());
  }
  





















  public void start(Xid xid, int flags)
    throws XAException
  {
    StringBuilder commandBuf = new StringBuilder(300);
    commandBuf.append("XA START ");
    appendXid(commandBuf, xid);
    
    switch (flags) {
    case 2097152: 
      commandBuf.append(" JOIN");
      break;
    case 134217728: 
      commandBuf.append(" RESUME");
      break;
    case 0: 
      break;
    
    default: 
      throw new XAException(-5);
    }
    
    dispatchCommand(commandBuf.toString());
    
    underlyingConnection.setInGlobalTx(true);
  }
  



















  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    StringBuilder commandBuf = new StringBuilder(300);
    commandBuf.append("XA COMMIT ");
    appendXid(commandBuf, xid);
    
    if (onePhase) {
      commandBuf.append(" ONE PHASE");
    }
    try
    {
      dispatchCommand(commandBuf.toString());
    } finally {
      underlyingConnection.setInGlobalTx(false);
    }
  }
  
  private ResultSet dispatchCommand(String command) throws XAException {
    Statement stmt = null;
    try
    {
      if (logXaCommands) {
        log.logDebug("Executing XA statement: " + command);
      }
      

      stmt = underlyingConnection.createStatement();
      
      stmt.execute(command);
      
      ResultSet rs = stmt.getResultSet();
      
      return rs;
    } catch (SQLException sqlEx) {
      throw mapXAExceptionFromSQLException(sqlEx);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (SQLException sqlEx) {}
      }
    }
  }
  
  protected static XAException mapXAExceptionFromSQLException(SQLException sqlEx) {
    Integer xaCode = (Integer)MYSQL_ERROR_CODES_TO_XA_ERROR_CODES.get(Integer.valueOf(sqlEx.getErrorCode()));
    
    if (xaCode != null) {
      return (XAException)new MysqlXAException(xaCode.intValue(), sqlEx.getMessage(), null).initCause(sqlEx);
    }
    
    return (XAException)new MysqlXAException(-7, Messages.getString("MysqlXAConnection.003"), null).initCause(sqlEx);
  }
  
  private static void appendXid(StringBuilder builder, Xid xid) {
    byte[] gtrid = xid.getGlobalTransactionId();
    byte[] btrid = xid.getBranchQualifier();
    
    if (gtrid != null) {
      StringUtils.appendAsHex(builder, gtrid);
    }
    
    builder.append(',');
    if (btrid != null) {
      StringUtils.appendAsHex(builder, btrid);
    }
    
    builder.append(',');
    StringUtils.appendAsHex(builder, xid.getFormatId());
  }
  




  public synchronized java.sql.Connection getConnection()
    throws SQLException
  {
    java.sql.Connection connToWrap = getConnection(false, true);
    
    return connToWrap;
  }
  
  public void forget(Xid xid)
    throws XAException
  {}
}
