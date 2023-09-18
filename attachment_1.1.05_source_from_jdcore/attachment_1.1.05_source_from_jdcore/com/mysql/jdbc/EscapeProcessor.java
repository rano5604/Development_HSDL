package com.mysql.jdbc;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

























class EscapeProcessor
{
  private static Map<String, String> JDBC_CONVERT_TO_MYSQL_TYPE_MAP;
  private static Map<String, String> JDBC_NO_CONVERT_TO_MYSQL_EXPRESSION_MAP;
  
  static
  {
    Map<String, String> tempMap = new HashMap();
    
    tempMap.put("BIGINT", "0 + ?");
    tempMap.put("BINARY", "BINARY");
    tempMap.put("BIT", "0 + ?");
    tempMap.put("CHAR", "CHAR");
    tempMap.put("DATE", "DATE");
    tempMap.put("DECIMAL", "0.0 + ?");
    tempMap.put("DOUBLE", "0.0 + ?");
    tempMap.put("FLOAT", "0.0 + ?");
    tempMap.put("INTEGER", "0 + ?");
    tempMap.put("LONGVARBINARY", "BINARY");
    tempMap.put("LONGVARCHAR", "CONCAT(?)");
    tempMap.put("REAL", "0.0 + ?");
    tempMap.put("SMALLINT", "CONCAT(?)");
    tempMap.put("TIME", "TIME");
    tempMap.put("TIMESTAMP", "DATETIME");
    tempMap.put("TINYINT", "CONCAT(?)");
    tempMap.put("VARBINARY", "BINARY");
    tempMap.put("VARCHAR", "CONCAT(?)");
    
    JDBC_CONVERT_TO_MYSQL_TYPE_MAP = Collections.unmodifiableMap(tempMap);
    
    tempMap = new HashMap(JDBC_CONVERT_TO_MYSQL_TYPE_MAP);
    
    tempMap.put("BINARY", "CONCAT(?)");
    tempMap.put("CHAR", "CONCAT(?)");
    tempMap.remove("DATE");
    tempMap.put("LONGVARBINARY", "CONCAT(?)");
    tempMap.remove("TIME");
    tempMap.remove("TIMESTAMP");
    tempMap.put("VARBINARY", "CONCAT(?)");
    
    JDBC_NO_CONVERT_TO_MYSQL_EXPRESSION_MAP = Collections.unmodifiableMap(tempMap);
  }
  










  public static final Object escapeSQL(String sql, boolean serverSupportsConvertFn, MySQLConnection conn)
    throws SQLException
  {
    boolean replaceEscapeSequence = false;
    String escapeSequence = null;
    
    if (sql == null) {
      return null;
    }
    



    int beginBrace = sql.indexOf('{');
    int nextEndBrace = beginBrace == -1 ? -1 : sql.indexOf('}', beginBrace);
    
    if (nextEndBrace == -1) {
      return sql;
    }
    
    StringBuilder newSql = new StringBuilder();
    
    EscapeTokenizer escapeTokenizer = new EscapeTokenizer(sql);
    
    byte usesVariables = 0;
    boolean callingStoredFunction = false;
    
    while (escapeTokenizer.hasMoreTokens()) {
      String token = escapeTokenizer.nextToken();
      
      if (token.length() != 0) {
        if (token.charAt(0) == '{')
        {
          if (!token.endsWith("}")) {
            throw SQLError.createSQLException("Not a valid escape sequence: " + token, conn.getExceptionInterceptor());
          }
          
          if (token.length() > 2) {
            int nestedBrace = token.indexOf('{', 2);
            
            if (nestedBrace != -1) {
              StringBuilder buf = new StringBuilder(token.substring(0, 1));
              
              Object remainingResults = escapeSQL(token.substring(1, token.length() - 1), serverSupportsConvertFn, conn);
              
              String remaining = null;
              
              if ((remainingResults instanceof String)) {
                remaining = (String)remainingResults;
              } else {
                remaining = escapedSql;
                
                if (usesVariables != 1) {
                  usesVariables = usesVariables;
                }
              }
              
              buf.append(remaining);
              
              buf.append('}');
              
              token = buf.toString();
            }
          }
          


          String collapsedToken = removeWhitespace(token);
          



          if (StringUtils.startsWithIgnoreCase(collapsedToken, "{escape")) {
            try {
              StringTokenizer st = new StringTokenizer(token, " '");
              st.nextToken();
              escapeSequence = st.nextToken();
              
              if (escapeSequence.length() < 3) {
                newSql.append(token);
              }
              else {
                escapeSequence = escapeSequence.substring(1, escapeSequence.length() - 1);
                replaceEscapeSequence = true;
              }
            } catch (NoSuchElementException e) {
              newSql.append(token);
            }
          } else if (StringUtils.startsWithIgnoreCase(collapsedToken, "{fn")) {
            int startPos = token.toLowerCase().indexOf("fn ") + 3;
            int endPos = token.length() - 1;
            
            String fnToken = token.substring(startPos, endPos);
            


            if (StringUtils.startsWithIgnoreCaseAndWs(fnToken, "convert")) {
              newSql.append(processConvertToken(fnToken, serverSupportsConvertFn, conn));
            }
            else {
              newSql.append(fnToken);
            }
          } else if (StringUtils.startsWithIgnoreCase(collapsedToken, "{d")) {
            int startPos = token.indexOf('\'') + 1;
            int endPos = token.lastIndexOf('\'');
            
            if ((startPos == -1) || (endPos == -1)) {
              newSql.append(token);
            }
            else {
              String argument = token.substring(startPos, endPos);
              try
              {
                StringTokenizer st = new StringTokenizer(argument, " -");
                String year4 = st.nextToken();
                String month2 = st.nextToken();
                String day2 = st.nextToken();
                String dateString = "'" + year4 + "-" + month2 + "-" + day2 + "'";
                newSql.append(dateString);
              } catch (NoSuchElementException e) {
                throw SQLError.createSQLException("Syntax error for DATE escape sequence '" + argument + "'", "42000", conn.getExceptionInterceptor());
              }
            }
          }
          else if (StringUtils.startsWithIgnoreCase(collapsedToken, "{ts")) {
            processTimestampToken(conn, newSql, token);
          } else if (StringUtils.startsWithIgnoreCase(collapsedToken, "{t")) {
            processTimeToken(conn, newSql, token);
          } else if ((StringUtils.startsWithIgnoreCase(collapsedToken, "{call")) || (StringUtils.startsWithIgnoreCase(collapsedToken, "{?=call")))
          {
            int startPos = StringUtils.indexOfIgnoreCase(token, "CALL") + 5;
            int endPos = token.length() - 1;
            
            if (StringUtils.startsWithIgnoreCase(collapsedToken, "{?=call")) {
              callingStoredFunction = true;
              newSql.append("SELECT ");
              newSql.append(token.substring(startPos, endPos));
            } else {
              callingStoredFunction = false;
              newSql.append("CALL ");
              newSql.append(token.substring(startPos, endPos));
            }
            
            for (int i = endPos - 1; i >= startPos; i--) {
              char c = token.charAt(i);
              
              if (!Character.isWhitespace(c))
              {


                if (c == ')') break;
                newSql.append("()"); break;
              }
              
            }
          }
          else if (StringUtils.startsWithIgnoreCase(collapsedToken, "{oj"))
          {
            newSql.append(token);
          }
          else {
            newSql.append(token);
          }
        } else {
          newSql.append(token);
        }
      }
    }
    
    String escapedSql = newSql.toString();
    



    if (replaceEscapeSequence) {
      String currentSql = escapedSql;
      
      while (currentSql.indexOf(escapeSequence) != -1) {
        int escapePos = currentSql.indexOf(escapeSequence);
        String lhs = currentSql.substring(0, escapePos);
        String rhs = currentSql.substring(escapePos + 1, currentSql.length());
        currentSql = lhs + "\\" + rhs;
      }
      
      escapedSql = currentSql;
    }
    
    EscapeProcessorResult epr = new EscapeProcessorResult();
    escapedSql = escapedSql;
    callingStoredFunction = callingStoredFunction;
    
    if (usesVariables != 1) {
      if (escapeTokenizer.sawVariableUse()) {
        usesVariables = 1;
      } else {
        usesVariables = 0;
      }
    }
    
    return epr;
  }
  
  private static void processTimeToken(MySQLConnection conn, StringBuilder newSql, String token) throws SQLException {
    int startPos = token.indexOf('\'') + 1;
    int endPos = token.lastIndexOf('\'');
    
    if ((startPos == -1) || (endPos == -1)) {
      newSql.append(token);
    }
    else {
      String argument = token.substring(startPos, endPos);
      try
      {
        StringTokenizer st = new StringTokenizer(argument, " :.");
        String hour = st.nextToken();
        String minute = st.nextToken();
        String second = st.nextToken();
        
        boolean serverSupportsFractionalSecond = false;
        String fractionalSecond = "";
        
        if ((st.hasMoreTokens()) && 
          (conn.versionMeetsMinimum(5, 6, 4))) {
          serverSupportsFractionalSecond = true;
          fractionalSecond = "." + st.nextToken();
        }
        

        if ((!conn.getUseTimezone()) || (!conn.getUseLegacyDatetimeCode())) {
          newSql.append("'");
          newSql.append(hour);
          newSql.append(":");
          newSql.append(minute);
          newSql.append(":");
          newSql.append(second);
          newSql.append(fractionalSecond);
          newSql.append("'");
        } else {
          Calendar sessionCalendar = conn.getCalendarInstanceForSessionOrNew();
          try
          {
            int hourInt = Integer.parseInt(hour);
            int minuteInt = Integer.parseInt(minute);
            int secondInt = Integer.parseInt(second);
            
            Time toBeAdjusted = TimeUtil.fastTimeCreate(sessionCalendar, hourInt, minuteInt, secondInt, conn.getExceptionInterceptor());
            
            Time inServerTimezone = TimeUtil.changeTimezone(conn, sessionCalendar, null, toBeAdjusted, sessionCalendar.getTimeZone(), conn.getServerTimezoneTZ(), false);
            

            newSql.append("'");
            newSql.append(inServerTimezone.toString());
            
            if (serverSupportsFractionalSecond) {
              newSql.append(fractionalSecond);
            }
            
            newSql.append("'");
          }
          catch (NumberFormatException nfe) {
            throw SQLError.createSQLException("Syntax error in TIMESTAMP escape sequence '" + token + "'.", "S1009", conn.getExceptionInterceptor());
          }
        }
      }
      catch (NoSuchElementException e) {
        throw SQLError.createSQLException("Syntax error for escape sequence '" + argument + "'", "42000", conn.getExceptionInterceptor());
      }
    }
  }
  
  private static void processTimestampToken(MySQLConnection conn, StringBuilder newSql, String token) throws SQLException {
    int startPos = token.indexOf('\'') + 1;
    int endPos = token.lastIndexOf('\'');
    
    if ((startPos == -1) || (endPos == -1)) {
      newSql.append(token);
    }
    else {
      String argument = token.substring(startPos, endPos);
      try
      {
        if (!conn.getUseLegacyDatetimeCode()) {
          Timestamp ts = Timestamp.valueOf(argument);
          SimpleDateFormat tsdf = new SimpleDateFormat("''yyyy-MM-dd HH:mm:ss", Locale.US);
          
          tsdf.setTimeZone(conn.getServerTimezoneTZ());
          
          newSql.append(tsdf.format(ts));
          
          if ((ts.getNanos() > 0) && (conn.versionMeetsMinimum(5, 6, 4))) {
            newSql.append('.');
            newSql.append(TimeUtil.formatNanos(ts.getNanos(), true, true));
          }
          
          newSql.append('\'');
        }
        else {
          StringTokenizer st = new StringTokenizer(argument, " .-:");
          try {
            String year4 = st.nextToken();
            String month2 = st.nextToken();
            String day2 = st.nextToken();
            String hour = st.nextToken();
            String minute = st.nextToken();
            String second = st.nextToken();
            
            boolean serverSupportsFractionalSecond = false;
            String fractionalSecond = "";
            if ((st.hasMoreTokens()) && 
              (conn.versionMeetsMinimum(5, 6, 4))) {
              serverSupportsFractionalSecond = true;
              fractionalSecond = "." + st.nextToken();
            }
            













            if ((!conn.getUseTimezone()) && (!conn.getUseJDBCCompliantTimezoneShift())) {
              newSql.append("'").append(year4).append("-").append(month2).append("-").append(day2).append(" ").append(hour).append(":").append(minute).append(":").append(second).append(fractionalSecond).append("'");
            }
            else {
              Calendar sessionCalendar = conn.getCalendarInstanceForSessionOrNew();
              try
              {
                int year4Int = Integer.parseInt(year4);
                int month2Int = Integer.parseInt(month2);
                int day2Int = Integer.parseInt(day2);
                int hourInt = Integer.parseInt(hour);
                int minuteInt = Integer.parseInt(minute);
                int secondInt = Integer.parseInt(second);
                
                boolean useGmtMillis = conn.getUseGmtMillisForDatetimes();
                
                Timestamp toBeAdjusted = TimeUtil.fastTimestampCreate(useGmtMillis, useGmtMillis ? Calendar.getInstance(TimeZone.getTimeZone("GMT")) : null, sessionCalendar, year4Int, month2Int, day2Int, hourInt, minuteInt, secondInt, 0);
                


                Timestamp inServerTimezone = TimeUtil.changeTimezone(conn, sessionCalendar, null, toBeAdjusted, sessionCalendar.getTimeZone(), conn.getServerTimezoneTZ(), false);
                

                newSql.append("'");
                
                String timezoneLiteral = inServerTimezone.toString();
                
                int indexOfDot = timezoneLiteral.indexOf(".");
                
                if (indexOfDot != -1) {
                  timezoneLiteral = timezoneLiteral.substring(0, indexOfDot);
                }
                
                newSql.append(timezoneLiteral);
                
                if (serverSupportsFractionalSecond) {
                  newSql.append(fractionalSecond);
                }
                newSql.append("'");
              }
              catch (NumberFormatException nfe) {
                throw SQLError.createSQLException("Syntax error in TIMESTAMP escape sequence '" + token + "'.", "S1009", conn.getExceptionInterceptor());
              }
            }
          }
          catch (NoSuchElementException e) {
            throw SQLError.createSQLException("Syntax error for TIMESTAMP escape sequence '" + argument + "'", "42000", conn.getExceptionInterceptor());
          }
        }
      }
      catch (IllegalArgumentException illegalArgumentException) {
        SQLException sqlEx = SQLError.createSQLException("Syntax error for TIMESTAMP escape sequence '" + argument + "'", "42000", conn.getExceptionInterceptor());
        
        sqlEx.initCause(illegalArgumentException);
        
        throw sqlEx;
      }
    }
  }
  



































  private static String processConvertToken(String functionToken, boolean serverSupportsConvertFn, MySQLConnection conn)
    throws SQLException
  {
    int firstIndexOfParen = functionToken.indexOf("(");
    
    if (firstIndexOfParen == -1) {
      throw SQLError.createSQLException("Syntax error while processing {fn convert (... , ...)} token, missing opening parenthesis in token '" + functionToken + "'.", "42000", conn.getExceptionInterceptor());
    }
    


    int indexOfComma = functionToken.lastIndexOf(",");
    
    if (indexOfComma == -1) {
      throw SQLError.createSQLException("Syntax error while processing {fn convert (... , ...)} token, missing comma in token '" + functionToken + "'.", "42000", conn.getExceptionInterceptor());
    }
    

    int indexOfCloseParen = functionToken.indexOf(')', indexOfComma);
    
    if (indexOfCloseParen == -1) {
      throw SQLError.createSQLException("Syntax error while processing {fn convert (... , ...)} token, missing closing parenthesis in token '" + functionToken + "'.", "42000", conn.getExceptionInterceptor());
    }
    



    String expression = functionToken.substring(firstIndexOfParen + 1, indexOfComma);
    String type = functionToken.substring(indexOfComma + 1, indexOfCloseParen);
    
    String newType = null;
    
    String trimmedType = type.trim();
    
    if (StringUtils.startsWithIgnoreCase(trimmedType, "SQL_")) {
      trimmedType = trimmedType.substring(4, trimmedType.length());
    }
    
    if (serverSupportsConvertFn) {
      newType = (String)JDBC_CONVERT_TO_MYSQL_TYPE_MAP.get(trimmedType.toUpperCase(Locale.ENGLISH));
    } else {
      newType = (String)JDBC_NO_CONVERT_TO_MYSQL_EXPRESSION_MAP.get(trimmedType.toUpperCase(Locale.ENGLISH));
      



      if (newType == null) {
        throw SQLError.createSQLException("Can't find conversion re-write for type '" + type + "' that is applicable for this server version while processing escape tokens.", "S1000", conn.getExceptionInterceptor());
      }
    }
    


    if (newType == null) {
      throw SQLError.createSQLException("Unsupported conversion type '" + type.trim() + "' found while processing escape token.", "S1000", conn.getExceptionInterceptor());
    }
    

    int replaceIndex = newType.indexOf("?");
    
    if (replaceIndex != -1) {
      StringBuilder convertRewrite = new StringBuilder(newType.substring(0, replaceIndex));
      convertRewrite.append(expression);
      convertRewrite.append(newType.substring(replaceIndex + 1, newType.length()));
      
      return convertRewrite.toString();
    }
    
    StringBuilder castRewrite = new StringBuilder("CAST(");
    castRewrite.append(expression);
    castRewrite.append(" AS ");
    castRewrite.append(newType);
    castRewrite.append(")");
    
    return castRewrite.toString();
  }
  









  private static String removeWhitespace(String toCollapse)
  {
    if (toCollapse == null) {
      return null;
    }
    
    int length = toCollapse.length();
    
    StringBuilder collapsed = new StringBuilder(length);
    
    for (int i = 0; i < length; i++) {
      char c = toCollapse.charAt(i);
      
      if (!Character.isWhitespace(c)) {
        collapsed.append(c);
      }
    }
    
    return collapsed.toString();
  }
  
  EscapeProcessor() {}
}
