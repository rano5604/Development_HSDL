package com.mysql.jdbc.util;

import com.mysql.jdbc.TimeUtil;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;



































public class TimezoneDump
{
  private static final String DEFAULT_URL = "jdbc:mysql:///test";
  
  public TimezoneDump() {}
  
  public static void main(String[] args)
    throws Exception
  {
    String jdbcUrl = "jdbc:mysql:///test";
    
    if ((args.length == 1) && (args[0] != null)) {
      jdbcUrl = args[0];
    }
    
    Class.forName("com.mysql.jdbc.Driver").newInstance();
    
    ResultSet rs = null;
    try
    {
      rs = DriverManager.getConnection(jdbcUrl).createStatement().executeQuery("SHOW VARIABLES LIKE 'timezone'");
      
      while (rs.next()) {
        String timezoneFromServer = rs.getString(2);
        System.out.println("MySQL timezone name: " + timezoneFromServer);
        
        String canonicalTimezone = TimeUtil.getCanonicalTimezone(timezoneFromServer, null);
        System.out.println("Java timezone name: " + canonicalTimezone);
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
    }
  }
}
