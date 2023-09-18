package com.mysql.jdbc;

import java.sql.SQLException;




























public class CommunicationsException
  extends SQLException
  implements StreamingNotifiable
{
  static final long serialVersionUID = 3193864990663398317L;
  private String exceptionMessage = null;
  
  public CommunicationsException(MySQLConnection conn, long lastPacketSentTimeMs, long lastPacketReceivedTimeMs, Exception underlyingException) {
    exceptionMessage = SQLError.createLinkFailureMessageBasedOnHeuristics(conn, lastPacketSentTimeMs, lastPacketReceivedTimeMs, underlyingException);
    
    if (underlyingException != null) {
      initCause(underlyingException);
    }
  }
  



  public String getMessage()
  {
    return exceptionMessage;
  }
  



  public String getSQLState()
  {
    return "08S01";
  }
  



  public void setWasStreamingResults()
  {
    exceptionMessage = Messages.getString("CommunicationsException.ClientWasStreaming");
  }
}
