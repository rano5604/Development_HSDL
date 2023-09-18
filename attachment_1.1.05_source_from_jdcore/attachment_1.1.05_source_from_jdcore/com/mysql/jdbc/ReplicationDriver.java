package com.mysql.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;































public class ReplicationDriver
  extends NonRegisteringReplicationDriver
  implements Driver
{
  public ReplicationDriver()
    throws SQLException
  {}
  
  static
  {
    try
    {
      DriverManager.registerDriver(new NonRegisteringReplicationDriver());
    } catch (SQLException E) {
      throw new RuntimeException("Can't register driver!");
    }
  }
}
