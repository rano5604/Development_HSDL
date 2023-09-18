package com.mysql.jdbc.util;

import com.mysql.jdbc.NonRegisteringDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;





















public class VersionFSHierarchyMaker
{
  public VersionFSHierarchyMaker() {}
  
  public static void main(String[] args)
    throws Exception
  {
    if (args.length < 3) {
      usage();
      System.exit(1);
    }
    
    String jdbcUrl = null;
    
    String jvmVersion = removeWhitespaceChars(System.getProperty("java.version"));
    String jvmVendor = removeWhitespaceChars(System.getProperty("java.vendor"));
    String osName = removeWhitespaceChars(System.getProperty("os.name"));
    String osArch = removeWhitespaceChars(System.getProperty("os.arch"));
    String osVersion = removeWhitespaceChars(System.getProperty("os.version"));
    
    jdbcUrl = System.getProperty("com.mysql.jdbc.testsuite.url");
    
    String mysqlVersion = "MySQL" + args[2] + "_";
    try
    {
      Properties props = new Properties();
      props.setProperty("allowPublicKeyRetrieval", "true");
      Connection conn = new NonRegisteringDriver().connect(jdbcUrl, props);
      
      ResultSet rs = conn.createStatement().executeQuery("SELECT VERSION()");
      rs.next();
      mysqlVersion = mysqlVersion + removeWhitespaceChars(rs.getString(1));
    } catch (Throwable t) {
      mysqlVersion = mysqlVersion + "no-server-running-on-" + removeWhitespaceChars(jdbcUrl);
    }
    
    String jvmSubdirName = jvmVendor + "-" + jvmVersion;
    String osSubdirName = osName + "-" + osArch + "-" + osVersion;
    
    File baseDir = new File(args[0]);
    File mysqlVersionDir = new File(baseDir, mysqlVersion);
    File osVersionDir = new File(mysqlVersionDir, osSubdirName);
    File jvmVersionDir = new File(osVersionDir, jvmSubdirName);
    
    jvmVersionDir.mkdirs();
    
    FileOutputStream pathOut = null;
    try
    {
      String propsOutputPath = args[1];
      pathOut = new FileOutputStream(propsOutputPath);
      String baseDirStr = baseDir.getAbsolutePath();
      String jvmVersionDirStr = jvmVersionDir.getAbsolutePath();
      
      if (jvmVersionDirStr.startsWith(baseDirStr)) {
        jvmVersionDirStr = jvmVersionDirStr.substring(baseDirStr.length() + 1);
      }
      
      pathOut.write(jvmVersionDirStr.getBytes());
    } finally {
      if (pathOut != null) {
        pathOut.flush();
        pathOut.close();
      }
    }
  }
  
  public static String removeWhitespaceChars(String input) {
    if (input == null) {
      return input;
    }
    
    int strLen = input.length();
    
    StringBuilder output = new StringBuilder(strLen);
    
    for (int i = 0; i < strLen; i++) {
      char c = input.charAt(i);
      if ((!Character.isDigit(c)) && (!Character.isLetter(c))) {
        if (Character.isWhitespace(c)) {
          output.append("_");
        } else {
          output.append(".");
        }
      } else {
        output.append(c);
      }
    }
    
    return output.toString();
  }
  
  private static void usage() {
    System.err.println("Creates a fs hierarchy representing MySQL version, OS version and JVM version.");
    System.err.println("Stores the full path as 'outputDirectory' property in file 'directoryPropPath'");
    System.err.println();
    System.err.println("Usage: java VersionFSHierarchyMaker baseDirectory directoryPropPath jdbcUrlIter");
  }
}
