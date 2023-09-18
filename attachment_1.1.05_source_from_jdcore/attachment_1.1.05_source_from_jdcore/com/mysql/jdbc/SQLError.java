package com.mysql.jdbc;

import com.mysql.jdbc.exceptions.MySQLDataException;
import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import com.mysql.jdbc.exceptions.MySQLQueryInterruptedException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;
import com.mysql.jdbc.exceptions.MySQLTransactionRollbackException;
import com.mysql.jdbc.exceptions.MySQLTransientConnectionException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.BindException;
import java.sql.BatchUpdateException;
import java.sql.DataTruncation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;


































public class SQLError
{
  static final int ER_WARNING_NOT_COMPLETE_ROLLBACK = 1196;
  private static Map<Integer, String> mysqlToSql99State;
  private static Map<Integer, String> mysqlToSqlState;
  public static final String SQL_STATE_WARNING = "01000";
  public static final String SQL_STATE_DISCONNECT_ERROR = "01002";
  public static final String SQL_STATE_DATA_TRUNCATED = "01004";
  public static final String SQL_STATE_PRIVILEGE_NOT_REVOKED = "01006";
  public static final String SQL_STATE_NO_DATA = "02000";
  public static final String SQL_STATE_WRONG_NO_OF_PARAMETERS = "07001";
  public static final String SQL_STATE_UNABLE_TO_CONNECT_TO_DATASOURCE = "08001";
  public static final String SQL_STATE_CONNECTION_IN_USE = "08002";
  public static final String SQL_STATE_CONNECTION_NOT_OPEN = "08003";
  public static final String SQL_STATE_CONNECTION_REJECTED = "08004";
  public static final String SQL_STATE_CONNECTION_FAILURE = "08006";
  public static final String SQL_STATE_TRANSACTION_RESOLUTION_UNKNOWN = "08007";
  public static final String SQL_STATE_COMMUNICATION_LINK_FAILURE = "08S01";
  public static final String SQL_STATE_FEATURE_NOT_SUPPORTED = "0A000";
  public static final String SQL_STATE_CARDINALITY_VIOLATION = "21000";
  public static final String SQL_STATE_INSERT_VALUE_LIST_NO_MATCH_COL_LIST = "21S01";
  public static final String SQL_STATE_STRING_DATA_RIGHT_TRUNCATION = "22001";
  public static final String SQL_STATE_NUMERIC_VALUE_OUT_OF_RANGE = "22003";
  public static final String SQL_STATE_INVALID_DATETIME_FORMAT = "22007";
  public static final String SQL_STATE_DATETIME_FIELD_OVERFLOW = "22008";
  public static final String SQL_STATE_DIVISION_BY_ZERO = "22012";
  public static final String SQL_STATE_INVALID_CHARACTER_VALUE_FOR_CAST = "22018";
  public static final String SQL_STATE_INTEGRITY_CONSTRAINT_VIOLATION = "23000";
  public static final String SQL_STATE_INVALID_CURSOR_STATE = "24000";
  public static final String SQL_STATE_INVALID_TRANSACTION_STATE = "25000";
  public static final String SQL_STATE_INVALID_AUTH_SPEC = "28000";
  public static final String SQL_STATE_INVALID_TRANSACTION_TERMINATION = "2D000";
  public static final String SQL_STATE_INVALID_CONDITION_NUMBER = "35000";
  public static final String SQL_STATE_INVALID_CATALOG_NAME = "3D000";
  public static final String SQL_STATE_ROLLBACK_SERIALIZATION_FAILURE = "40001";
  public static final String SQL_STATE_SYNTAX_ERROR = "42000";
  public static final String SQL_STATE_ER_TABLE_EXISTS_ERROR = "42S01";
  public static final String SQL_STATE_BASE_TABLE_OR_VIEW_NOT_FOUND = "42S02";
  public static final String SQL_STATE_ER_NO_SUCH_INDEX = "42S12";
  public static final String SQL_STATE_ER_DUP_FIELDNAME = "42S21";
  public static final String SQL_STATE_ER_BAD_FIELD_ERROR = "42S22";
  public static final String SQL_STATE_INVALID_CONNECTION_ATTRIBUTE = "01S00";
  public static final String SQL_STATE_ERROR_IN_ROW = "01S01";
  public static final String SQL_STATE_NO_ROWS_UPDATED_OR_DELETED = "01S03";
  public static final String SQL_STATE_MORE_THAN_ONE_ROW_UPDATED_OR_DELETED = "01S04";
  public static final String SQL_STATE_RESIGNAL_WHEN_HANDLER_NOT_ACTIVE = "0K000";
  public static final String SQL_STATE_STACKED_DIAGNOSTICS_ACCESSED_WITHOUT_ACTIVE_HANDLER = "0Z002";
  public static final String SQL_STATE_CASE_NOT_FOUND_FOR_CASE_STATEMENT = "20000";
  public static final String SQL_STATE_NULL_VALUE_NOT_ALLOWED = "22004";
  public static final String SQL_STATE_INVALID_LOGARITHM_ARGUMENT = "2201E";
  public static final String SQL_STATE_ACTIVE_SQL_TRANSACTION = "25001";
  public static final String SQL_STATE_READ_ONLY_SQL_TRANSACTION = "25006";
  public static final String SQL_STATE_SRE_PROHIBITED_SQL_STATEMENT_ATTEMPTED = "2F003";
  public static final String SQL_STATE_SRE_FUNCTION_EXECUTED_NO_RETURN_STATEMENT = "2F005";
  public static final String SQL_STATE_ER_QUERY_INTERRUPTED = "70100";
  public static final String SQL_STATE_BASE_TABLE_OR_VIEW_ALREADY_EXISTS = "S0001";
  public static final String SQL_STATE_BASE_TABLE_NOT_FOUND = "S0002";
  public static final String SQL_STATE_INDEX_ALREADY_EXISTS = "S0011";
  public static final String SQL_STATE_INDEX_NOT_FOUND = "S0012";
  public static final String SQL_STATE_COLUMN_ALREADY_EXISTS = "S0021";
  public static final String SQL_STATE_COLUMN_NOT_FOUND = "S0022";
  public static final String SQL_STATE_NO_DEFAULT_FOR_COLUMN = "S0023";
  public static final String SQL_STATE_GENERAL_ERROR = "S1000";
  public static final String SQL_STATE_MEMORY_ALLOCATION_FAILURE = "S1001";
  public static final String SQL_STATE_INVALID_COLUMN_NUMBER = "S1002";
  public static final String SQL_STATE_ILLEGAL_ARGUMENT = "S1009";
  public static final String SQL_STATE_DRIVER_NOT_CAPABLE = "S1C00";
  public static final String SQL_STATE_TIMEOUT_EXPIRED = "S1T00";
  public static final String SQL_STATE_CLI_SPECIFIC_CONDITION = "HY000";
  public static final String SQL_STATE_MEMORY_ALLOCATION_ERROR = "HY001";
  public static final String SQL_STATE_XA_RBROLLBACK = "XA100";
  public static final String SQL_STATE_XA_RBDEADLOCK = "XA102";
  public static final String SQL_STATE_XA_RBTIMEOUT = "XA106";
  public static final String SQL_STATE_XA_RMERR = "XAE03";
  public static final String SQL_STATE_XAER_NOTA = "XAE04";
  public static final String SQL_STATE_XAER_INVAL = "XAE05";
  public static final String SQL_STATE_XAER_RMFAIL = "XAE07";
  public static final String SQL_STATE_XAER_DUPID = "XAE08";
  public static final String SQL_STATE_XAER_OUTSIDE = "XAE09";
  private static Map<String, String> sqlStateMessages;
  private static final long DEFAULT_WAIT_TIMEOUT_SECONDS = 28800L;
  private static final int DUE_TO_TIMEOUT_FALSE = 0;
  private static final int DUE_TO_TIMEOUT_MAYBE = 2;
  private static final int DUE_TO_TIMEOUT_TRUE = 1;
  private static final Constructor<?> JDBC_4_COMMUNICATIONS_EXCEPTION_CTOR;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_COMMUNICATIONS_EXCEPTION_CTOR = Class.forName("com.mysql.jdbc.exceptions.jdbc4.CommunicationsException").getConstructor(new Class[] { MySQLConnection.class, Long.TYPE, Long.TYPE, Exception.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_COMMUNICATIONS_EXCEPTION_CTOR = null;
    }
    
    sqlStateMessages = new HashMap();
    sqlStateMessages.put("01002", Messages.getString("SQLError.35"));
    sqlStateMessages.put("01004", Messages.getString("SQLError.36"));
    sqlStateMessages.put("01006", Messages.getString("SQLError.37"));
    sqlStateMessages.put("01S00", Messages.getString("SQLError.38"));
    sqlStateMessages.put("01S01", Messages.getString("SQLError.39"));
    sqlStateMessages.put("01S03", Messages.getString("SQLError.40"));
    sqlStateMessages.put("01S04", Messages.getString("SQLError.41"));
    sqlStateMessages.put("07001", Messages.getString("SQLError.42"));
    sqlStateMessages.put("08001", Messages.getString("SQLError.43"));
    sqlStateMessages.put("08002", Messages.getString("SQLError.44"));
    sqlStateMessages.put("08003", Messages.getString("SQLError.45"));
    sqlStateMessages.put("08004", Messages.getString("SQLError.46"));
    sqlStateMessages.put("08007", Messages.getString("SQLError.47"));
    sqlStateMessages.put("08S01", Messages.getString("SQLError.48"));
    sqlStateMessages.put("21S01", Messages.getString("SQLError.49"));
    sqlStateMessages.put("22003", Messages.getString("SQLError.50"));
    sqlStateMessages.put("22008", Messages.getString("SQLError.51"));
    sqlStateMessages.put("22012", Messages.getString("SQLError.52"));
    sqlStateMessages.put("40001", Messages.getString("SQLError.53"));
    sqlStateMessages.put("28000", Messages.getString("SQLError.54"));
    sqlStateMessages.put("42000", Messages.getString("SQLError.55"));
    sqlStateMessages.put("42S02", Messages.getString("SQLError.56"));
    sqlStateMessages.put("S0001", Messages.getString("SQLError.57"));
    sqlStateMessages.put("S0002", Messages.getString("SQLError.58"));
    sqlStateMessages.put("S0011", Messages.getString("SQLError.59"));
    sqlStateMessages.put("S0012", Messages.getString("SQLError.60"));
    sqlStateMessages.put("S0021", Messages.getString("SQLError.61"));
    sqlStateMessages.put("S0022", Messages.getString("SQLError.62"));
    sqlStateMessages.put("S0023", Messages.getString("SQLError.63"));
    sqlStateMessages.put("S1000", Messages.getString("SQLError.64"));
    sqlStateMessages.put("S1001", Messages.getString("SQLError.65"));
    sqlStateMessages.put("S1002", Messages.getString("SQLError.66"));
    sqlStateMessages.put("S1009", Messages.getString("SQLError.67"));
    sqlStateMessages.put("S1C00", Messages.getString("SQLError.68"));
    sqlStateMessages.put("S1T00", Messages.getString("SQLError.69"));
    
    mysqlToSqlState = new Hashtable();
    
    mysqlToSqlState.put(Integer.valueOf(1249), "01000");
    mysqlToSqlState.put(Integer.valueOf(1261), "01000");
    mysqlToSqlState.put(Integer.valueOf(1262), "01000");
    mysqlToSqlState.put(Integer.valueOf(1265), "01000");
    mysqlToSqlState.put(Integer.valueOf(1311), "01000");
    mysqlToSqlState.put(Integer.valueOf(1642), "01000");
    mysqlToSqlState.put(Integer.valueOf(1040), "08004");
    mysqlToSqlState.put(Integer.valueOf(1251), "08004");
    mysqlToSqlState.put(Integer.valueOf(1042), "08004");
    mysqlToSqlState.put(Integer.valueOf(1043), "08004");
    mysqlToSqlState.put(Integer.valueOf(1129), "08004");
    mysqlToSqlState.put(Integer.valueOf(1130), "08004");
    mysqlToSqlState.put(Integer.valueOf(1047), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1053), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1080), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1081), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1152), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1153), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1154), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1155), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1156), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1157), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1158), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1159), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1160), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1161), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1184), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1189), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1190), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1218), "08S01");
    mysqlToSqlState.put(Integer.valueOf(1312), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1314), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1335), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1336), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1415), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1845), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1846), "0A000");
    mysqlToSqlState.put(Integer.valueOf(1044), "42000");
    mysqlToSqlState.put(Integer.valueOf(1049), "42000");
    mysqlToSqlState.put(Integer.valueOf(1055), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1056), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1057), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1059), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1060), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1061), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1062), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1063), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1064), "42000");
    mysqlToSqlState.put(Integer.valueOf(1065), "42000");
    mysqlToSqlState.put(Integer.valueOf(1066), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1067), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1068), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1069), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1070), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1071), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1072), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1073), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1074), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1075), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1082), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1083), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1084), "S1009");
    mysqlToSqlState.put(Integer.valueOf(1090), "42000");
    mysqlToSqlState.put(Integer.valueOf(1091), "42000");
    mysqlToSqlState.put(Integer.valueOf(1101), "42000");
    mysqlToSqlState.put(Integer.valueOf(1102), "42000");
    mysqlToSqlState.put(Integer.valueOf(1103), "42000");
    mysqlToSqlState.put(Integer.valueOf(1104), "42000");
    mysqlToSqlState.put(Integer.valueOf(1106), "42000");
    mysqlToSqlState.put(Integer.valueOf(1107), "42000");
    mysqlToSqlState.put(Integer.valueOf(1110), "42000");
    mysqlToSqlState.put(Integer.valueOf(1112), "42000");
    mysqlToSqlState.put(Integer.valueOf(1113), "42000");
    mysqlToSqlState.put(Integer.valueOf(1115), "42000");
    mysqlToSqlState.put(Integer.valueOf(1118), "42000");
    mysqlToSqlState.put(Integer.valueOf(1120), "42000");
    mysqlToSqlState.put(Integer.valueOf(1121), "42000");
    mysqlToSqlState.put(Integer.valueOf(1131), "42000");
    mysqlToSqlState.put(Integer.valueOf(1132), "42000");
    mysqlToSqlState.put(Integer.valueOf(1133), "42000");
    mysqlToSqlState.put(Integer.valueOf(1139), "42000");
    mysqlToSqlState.put(Integer.valueOf(1140), "42000");
    mysqlToSqlState.put(Integer.valueOf(1141), "42000");
    mysqlToSqlState.put(Integer.valueOf(1142), "42000");
    mysqlToSqlState.put(Integer.valueOf(1143), "42000");
    mysqlToSqlState.put(Integer.valueOf(1144), "42000");
    mysqlToSqlState.put(Integer.valueOf(1145), "42000");
    mysqlToSqlState.put(Integer.valueOf(1147), "42000");
    mysqlToSqlState.put(Integer.valueOf(1148), "42000");
    mysqlToSqlState.put(Integer.valueOf(1149), "42000");
    mysqlToSqlState.put(Integer.valueOf(1162), "42000");
    mysqlToSqlState.put(Integer.valueOf(1163), "42000");
    mysqlToSqlState.put(Integer.valueOf(1164), "42000");
    mysqlToSqlState.put(Integer.valueOf(1166), "42000");
    mysqlToSqlState.put(Integer.valueOf(1167), "42000");
    mysqlToSqlState.put(Integer.valueOf(1170), "42000");
    mysqlToSqlState.put(Integer.valueOf(1171), "42000");
    mysqlToSqlState.put(Integer.valueOf(1172), "42000");
    mysqlToSqlState.put(Integer.valueOf(1173), "42000");
    mysqlToSqlState.put(Integer.valueOf(1176), "42000");
    mysqlToSqlState.put(Integer.valueOf(1177), "42000");
    mysqlToSqlState.put(Integer.valueOf(1178), "42000");
    mysqlToSqlState.put(Integer.valueOf(1203), "42000");
    mysqlToSqlState.put(Integer.valueOf(1211), "42000");
    mysqlToSqlState.put(Integer.valueOf(1226), "42000");
    mysqlToSqlState.put(Integer.valueOf(1227), "42000");
    mysqlToSqlState.put(Integer.valueOf(1230), "42000");
    mysqlToSqlState.put(Integer.valueOf(1231), "42000");
    mysqlToSqlState.put(Integer.valueOf(1232), "42000");
    mysqlToSqlState.put(Integer.valueOf(1234), "42000");
    mysqlToSqlState.put(Integer.valueOf(1235), "42000");
    mysqlToSqlState.put(Integer.valueOf(1239), "42000");
    mysqlToSqlState.put(Integer.valueOf(1248), "42000");
    mysqlToSqlState.put(Integer.valueOf(1250), "42000");
    mysqlToSqlState.put(Integer.valueOf(1252), "42000");
    mysqlToSqlState.put(Integer.valueOf(1253), "42000");
    mysqlToSqlState.put(Integer.valueOf(1280), "42000");
    mysqlToSqlState.put(Integer.valueOf(1281), "42000");
    mysqlToSqlState.put(Integer.valueOf(1286), "42000");
    mysqlToSqlState.put(Integer.valueOf(1304), "42000");
    mysqlToSqlState.put(Integer.valueOf(1305), "42000");
    mysqlToSqlState.put(Integer.valueOf(1308), "42000");
    mysqlToSqlState.put(Integer.valueOf(1309), "42000");
    mysqlToSqlState.put(Integer.valueOf(1310), "42000");
    mysqlToSqlState.put(Integer.valueOf(1313), "42000");
    mysqlToSqlState.put(Integer.valueOf(1315), "42000");
    mysqlToSqlState.put(Integer.valueOf(1316), "42000");
    mysqlToSqlState.put(Integer.valueOf(1318), "42000");
    mysqlToSqlState.put(Integer.valueOf(1319), "42000");
    mysqlToSqlState.put(Integer.valueOf(1320), "42000");
    mysqlToSqlState.put(Integer.valueOf(1322), "42000");
    mysqlToSqlState.put(Integer.valueOf(1323), "42000");
    mysqlToSqlState.put(Integer.valueOf(1324), "42000");
    mysqlToSqlState.put(Integer.valueOf(1327), "42000");
    mysqlToSqlState.put(Integer.valueOf(1330), "42000");
    mysqlToSqlState.put(Integer.valueOf(1331), "42000");
    mysqlToSqlState.put(Integer.valueOf(1332), "42000");
    mysqlToSqlState.put(Integer.valueOf(1333), "42000");
    mysqlToSqlState.put(Integer.valueOf(1337), "42000");
    mysqlToSqlState.put(Integer.valueOf(1338), "42000");
    mysqlToSqlState.put(Integer.valueOf(1370), "42000");
    mysqlToSqlState.put(Integer.valueOf(1403), "42000");
    mysqlToSqlState.put(Integer.valueOf(1407), "42000");
    mysqlToSqlState.put(Integer.valueOf(1410), "42000");
    mysqlToSqlState.put(Integer.valueOf(1413), "42000");
    mysqlToSqlState.put(Integer.valueOf(1414), "42000");
    mysqlToSqlState.put(Integer.valueOf(1425), "42000");
    mysqlToSqlState.put(Integer.valueOf(1426), "42000");
    mysqlToSqlState.put(Integer.valueOf(1427), "42000");
    mysqlToSqlState.put(Integer.valueOf(1437), "42000");
    mysqlToSqlState.put(Integer.valueOf(1439), "42000");
    mysqlToSqlState.put(Integer.valueOf(1453), "42000");
    mysqlToSqlState.put(Integer.valueOf(1458), "42000");
    mysqlToSqlState.put(Integer.valueOf(1460), "42000");
    mysqlToSqlState.put(Integer.valueOf(1461), "42000");
    mysqlToSqlState.put(Integer.valueOf(1463), "42000");
    mysqlToSqlState.put(Integer.valueOf(1582), "42000");
    mysqlToSqlState.put(Integer.valueOf(1583), "42000");
    mysqlToSqlState.put(Integer.valueOf(1584), "42000");
    mysqlToSqlState.put(Integer.valueOf(1630), "42000");
    mysqlToSqlState.put(Integer.valueOf(1641), "42000");
    mysqlToSqlState.put(Integer.valueOf(1687), "42000");
    mysqlToSqlState.put(Integer.valueOf(1701), "42000");
    mysqlToSqlState.put(Integer.valueOf(1222), "21000");
    mysqlToSqlState.put(Integer.valueOf(1241), "21000");
    mysqlToSqlState.put(Integer.valueOf(1242), "21000");
    mysqlToSqlState.put(Integer.valueOf(1022), "23000");
    mysqlToSqlState.put(Integer.valueOf(1048), "23000");
    mysqlToSqlState.put(Integer.valueOf(1052), "23000");
    mysqlToSqlState.put(Integer.valueOf(1169), "23000");
    mysqlToSqlState.put(Integer.valueOf(1216), "23000");
    mysqlToSqlState.put(Integer.valueOf(1217), "23000");
    mysqlToSqlState.put(Integer.valueOf(1451), "23000");
    mysqlToSqlState.put(Integer.valueOf(1452), "23000");
    mysqlToSqlState.put(Integer.valueOf(1557), "23000");
    mysqlToSqlState.put(Integer.valueOf(1586), "23000");
    mysqlToSqlState.put(Integer.valueOf(1761), "23000");
    mysqlToSqlState.put(Integer.valueOf(1762), "23000");
    mysqlToSqlState.put(Integer.valueOf(1859), "23000");
    mysqlToSqlState.put(Integer.valueOf(1406), "22001");
    mysqlToSqlState.put(Integer.valueOf(1264), "01000");
    mysqlToSqlState.put(Integer.valueOf(1416), "22003");
    mysqlToSqlState.put(Integer.valueOf(1690), "22003");
    mysqlToSqlState.put(Integer.valueOf(1292), "22007");
    mysqlToSqlState.put(Integer.valueOf(1367), "22007");
    mysqlToSqlState.put(Integer.valueOf(1441), "22008");
    mysqlToSqlState.put(Integer.valueOf(1365), "22012");
    mysqlToSqlState.put(Integer.valueOf(1325), "24000");
    mysqlToSqlState.put(Integer.valueOf(1326), "24000");
    mysqlToSqlState.put(Integer.valueOf(1179), "25000");
    mysqlToSqlState.put(Integer.valueOf(1207), "25000");
    mysqlToSqlState.put(Integer.valueOf(1045), "28000");
    mysqlToSqlState.put(Integer.valueOf(1698), "28000");
    mysqlToSqlState.put(Integer.valueOf(1873), "28000");
    mysqlToSqlState.put(Integer.valueOf(1758), "35000");
    mysqlToSqlState.put(Integer.valueOf(1046), "3D000");
    mysqlToSqlState.put(Integer.valueOf(1058), "21S01");
    mysqlToSqlState.put(Integer.valueOf(1136), "21S01");
    mysqlToSqlState.put(Integer.valueOf(1050), "42S01");
    mysqlToSqlState.put(Integer.valueOf(1051), "42S02");
    mysqlToSqlState.put(Integer.valueOf(1109), "42S02");
    mysqlToSqlState.put(Integer.valueOf(1146), "42S02");
    mysqlToSqlState.put(Integer.valueOf(1054), "S0022");
    mysqlToSqlState.put(Integer.valueOf(1247), "42S22");
    mysqlToSqlState.put(Integer.valueOf(1037), "S1001");
    mysqlToSqlState.put(Integer.valueOf(1038), "S1001");
    mysqlToSqlState.put(Integer.valueOf(1205), "40001");
    mysqlToSqlState.put(Integer.valueOf(1213), "40001");
    
    mysqlToSql99State = new HashMap();
    
    mysqlToSql99State.put(Integer.valueOf(1249), "01000");
    mysqlToSql99State.put(Integer.valueOf(1261), "01000");
    mysqlToSql99State.put(Integer.valueOf(1262), "01000");
    mysqlToSql99State.put(Integer.valueOf(1265), "01000");
    mysqlToSql99State.put(Integer.valueOf(1263), "01000");
    mysqlToSql99State.put(Integer.valueOf(1264), "01000");
    mysqlToSql99State.put(Integer.valueOf(1311), "01000");
    mysqlToSql99State.put(Integer.valueOf(1642), "01000");
    mysqlToSql99State.put(Integer.valueOf(1329), "02000");
    mysqlToSql99State.put(Integer.valueOf(1643), "02000");
    mysqlToSql99State.put(Integer.valueOf(1040), "08004");
    mysqlToSql99State.put(Integer.valueOf(1251), "08004");
    mysqlToSql99State.put(Integer.valueOf(1042), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1043), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1047), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1053), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1080), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1081), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1152), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1153), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1154), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1155), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1156), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1157), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1158), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1159), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1160), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1161), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1184), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1189), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1190), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1218), "08S01");
    mysqlToSql99State.put(Integer.valueOf(1312), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1314), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1335), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1336), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1415), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1845), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1846), "0A000");
    mysqlToSql99State.put(Integer.valueOf(1044), "42000");
    mysqlToSql99State.put(Integer.valueOf(1049), "42000");
    mysqlToSql99State.put(Integer.valueOf(1055), "42000");
    mysqlToSql99State.put(Integer.valueOf(1056), "42000");
    mysqlToSql99State.put(Integer.valueOf(1057), "42000");
    mysqlToSql99State.put(Integer.valueOf(1059), "42000");
    mysqlToSql99State.put(Integer.valueOf(1061), "42000");
    mysqlToSql99State.put(Integer.valueOf(1063), "42000");
    mysqlToSql99State.put(Integer.valueOf(1064), "42000");
    mysqlToSql99State.put(Integer.valueOf(1065), "42000");
    mysqlToSql99State.put(Integer.valueOf(1066), "42000");
    mysqlToSql99State.put(Integer.valueOf(1067), "42000");
    mysqlToSql99State.put(Integer.valueOf(1068), "42000");
    mysqlToSql99State.put(Integer.valueOf(1069), "42000");
    mysqlToSql99State.put(Integer.valueOf(1070), "42000");
    mysqlToSql99State.put(Integer.valueOf(1071), "42000");
    mysqlToSql99State.put(Integer.valueOf(1072), "42000");
    mysqlToSql99State.put(Integer.valueOf(1073), "42000");
    mysqlToSql99State.put(Integer.valueOf(1074), "42000");
    mysqlToSql99State.put(Integer.valueOf(1075), "42000");
    mysqlToSql99State.put(Integer.valueOf(1083), "42000");
    mysqlToSql99State.put(Integer.valueOf(1084), "42000");
    mysqlToSql99State.put(Integer.valueOf(1090), "42000");
    mysqlToSql99State.put(Integer.valueOf(1091), "42000");
    mysqlToSql99State.put(Integer.valueOf(1101), "42000");
    mysqlToSql99State.put(Integer.valueOf(1102), "42000");
    mysqlToSql99State.put(Integer.valueOf(1103), "42000");
    mysqlToSql99State.put(Integer.valueOf(1104), "42000");
    mysqlToSql99State.put(Integer.valueOf(1106), "42000");
    mysqlToSql99State.put(Integer.valueOf(1107), "42000");
    mysqlToSql99State.put(Integer.valueOf(1110), "42000");
    mysqlToSql99State.put(Integer.valueOf(1112), "42000");
    mysqlToSql99State.put(Integer.valueOf(1113), "42000");
    mysqlToSql99State.put(Integer.valueOf(1115), "42000");
    mysqlToSql99State.put(Integer.valueOf(1118), "42000");
    mysqlToSql99State.put(Integer.valueOf(1120), "42000");
    mysqlToSql99State.put(Integer.valueOf(1121), "42000");
    mysqlToSql99State.put(Integer.valueOf(1131), "42000");
    mysqlToSql99State.put(Integer.valueOf(1132), "42000");
    mysqlToSql99State.put(Integer.valueOf(1133), "42000");
    mysqlToSql99State.put(Integer.valueOf(1139), "42000");
    mysqlToSql99State.put(Integer.valueOf(1140), "42000");
    mysqlToSql99State.put(Integer.valueOf(1141), "42000");
    mysqlToSql99State.put(Integer.valueOf(1142), "42000");
    mysqlToSql99State.put(Integer.valueOf(1143), "42000");
    mysqlToSql99State.put(Integer.valueOf(1144), "42000");
    mysqlToSql99State.put(Integer.valueOf(1145), "42000");
    mysqlToSql99State.put(Integer.valueOf(1147), "42000");
    mysqlToSql99State.put(Integer.valueOf(1148), "42000");
    mysqlToSql99State.put(Integer.valueOf(1149), "42000");
    mysqlToSql99State.put(Integer.valueOf(1162), "42000");
    mysqlToSql99State.put(Integer.valueOf(1163), "42000");
    mysqlToSql99State.put(Integer.valueOf(1164), "42000");
    mysqlToSql99State.put(Integer.valueOf(1166), "42000");
    mysqlToSql99State.put(Integer.valueOf(1167), "42000");
    mysqlToSql99State.put(Integer.valueOf(1170), "42000");
    mysqlToSql99State.put(Integer.valueOf(1171), "42000");
    mysqlToSql99State.put(Integer.valueOf(1172), "42000");
    mysqlToSql99State.put(Integer.valueOf(1173), "42000");
    mysqlToSql99State.put(Integer.valueOf(1176), "42000");
    mysqlToSql99State.put(Integer.valueOf(1177), "42000");
    mysqlToSql99State.put(Integer.valueOf(1178), "42000");
    mysqlToSql99State.put(Integer.valueOf(1203), "42000");
    mysqlToSql99State.put(Integer.valueOf(1211), "42000");
    mysqlToSql99State.put(Integer.valueOf(1226), "42000");
    mysqlToSql99State.put(Integer.valueOf(1227), "42000");
    mysqlToSql99State.put(Integer.valueOf(1230), "42000");
    mysqlToSql99State.put(Integer.valueOf(1231), "42000");
    mysqlToSql99State.put(Integer.valueOf(1232), "42000");
    mysqlToSql99State.put(Integer.valueOf(1234), "42000");
    mysqlToSql99State.put(Integer.valueOf(1235), "42000");
    mysqlToSql99State.put(Integer.valueOf(1239), "42000");
    mysqlToSql99State.put(Integer.valueOf(1248), "42000");
    mysqlToSql99State.put(Integer.valueOf(1250), "42000");
    mysqlToSql99State.put(Integer.valueOf(1252), "42000");
    mysqlToSql99State.put(Integer.valueOf(1253), "42000");
    mysqlToSql99State.put(Integer.valueOf(1280), "42000");
    mysqlToSql99State.put(Integer.valueOf(1281), "42000");
    mysqlToSql99State.put(Integer.valueOf(1286), "42000");
    mysqlToSql99State.put(Integer.valueOf(1304), "42000");
    mysqlToSql99State.put(Integer.valueOf(1305), "42000");
    mysqlToSql99State.put(Integer.valueOf(1308), "42000");
    mysqlToSql99State.put(Integer.valueOf(1309), "42000");
    mysqlToSql99State.put(Integer.valueOf(1310), "42000");
    mysqlToSql99State.put(Integer.valueOf(1313), "42000");
    mysqlToSql99State.put(Integer.valueOf(1315), "42000");
    mysqlToSql99State.put(Integer.valueOf(1316), "42000");
    mysqlToSql99State.put(Integer.valueOf(1318), "42000");
    mysqlToSql99State.put(Integer.valueOf(1319), "42000");
    mysqlToSql99State.put(Integer.valueOf(1320), "42000");
    mysqlToSql99State.put(Integer.valueOf(1322), "42000");
    mysqlToSql99State.put(Integer.valueOf(1323), "42000");
    mysqlToSql99State.put(Integer.valueOf(1324), "42000");
    mysqlToSql99State.put(Integer.valueOf(1327), "42000");
    mysqlToSql99State.put(Integer.valueOf(1330), "42000");
    mysqlToSql99State.put(Integer.valueOf(1331), "42000");
    mysqlToSql99State.put(Integer.valueOf(1332), "42000");
    mysqlToSql99State.put(Integer.valueOf(1333), "42000");
    mysqlToSql99State.put(Integer.valueOf(1337), "42000");
    mysqlToSql99State.put(Integer.valueOf(1338), "42000");
    mysqlToSql99State.put(Integer.valueOf(1370), "42000");
    mysqlToSql99State.put(Integer.valueOf(1403), "42000");
    mysqlToSql99State.put(Integer.valueOf(1407), "42000");
    mysqlToSql99State.put(Integer.valueOf(1410), "42000");
    mysqlToSql99State.put(Integer.valueOf(1413), "42000");
    mysqlToSql99State.put(Integer.valueOf(1414), "42000");
    mysqlToSql99State.put(Integer.valueOf(1425), "42000");
    mysqlToSql99State.put(Integer.valueOf(1426), "42000");
    mysqlToSql99State.put(Integer.valueOf(1427), "42000");
    mysqlToSql99State.put(Integer.valueOf(1437), "42000");
    mysqlToSql99State.put(Integer.valueOf(1439), "42000");
    mysqlToSql99State.put(Integer.valueOf(1453), "42000");
    mysqlToSql99State.put(Integer.valueOf(1458), "42000");
    mysqlToSql99State.put(Integer.valueOf(1460), "42000");
    mysqlToSql99State.put(Integer.valueOf(1461), "42000");
    mysqlToSql99State.put(Integer.valueOf(1463), "42000");
    mysqlToSql99State.put(Integer.valueOf(1582), "42000");
    mysqlToSql99State.put(Integer.valueOf(1583), "42000");
    mysqlToSql99State.put(Integer.valueOf(1584), "42000");
    mysqlToSql99State.put(Integer.valueOf(1630), "42000");
    mysqlToSql99State.put(Integer.valueOf(1641), "42000");
    mysqlToSql99State.put(Integer.valueOf(1687), "42000");
    mysqlToSql99State.put(Integer.valueOf(1701), "42000");
    mysqlToSql99State.put(Integer.valueOf(1222), "21000");
    mysqlToSql99State.put(Integer.valueOf(1241), "21000");
    mysqlToSql99State.put(Integer.valueOf(1242), "21000");
    mysqlToSql99State.put(Integer.valueOf(1022), "23000");
    mysqlToSql99State.put(Integer.valueOf(1048), "23000");
    mysqlToSql99State.put(Integer.valueOf(1052), "23000");
    mysqlToSql99State.put(Integer.valueOf(1062), "23000");
    mysqlToSql99State.put(Integer.valueOf(1169), "23000");
    mysqlToSql99State.put(Integer.valueOf(1216), "23000");
    mysqlToSql99State.put(Integer.valueOf(1217), "23000");
    mysqlToSql99State.put(Integer.valueOf(1451), "23000");
    mysqlToSql99State.put(Integer.valueOf(1452), "23000");
    mysqlToSql99State.put(Integer.valueOf(1557), "23000");
    mysqlToSql99State.put(Integer.valueOf(1586), "23000");
    mysqlToSql99State.put(Integer.valueOf(1761), "23000");
    mysqlToSql99State.put(Integer.valueOf(1762), "23000");
    mysqlToSql99State.put(Integer.valueOf(1859), "23000");
    mysqlToSql99State.put(Integer.valueOf(1406), "22001");
    mysqlToSql99State.put(Integer.valueOf(1416), "22003");
    mysqlToSql99State.put(Integer.valueOf(1690), "22003");
    mysqlToSql99State.put(Integer.valueOf(1292), "22007");
    mysqlToSql99State.put(Integer.valueOf(1367), "22007");
    mysqlToSql99State.put(Integer.valueOf(1441), "22008");
    mysqlToSql99State.put(Integer.valueOf(1365), "22012");
    mysqlToSql99State.put(Integer.valueOf(1325), "24000");
    mysqlToSql99State.put(Integer.valueOf(1326), "24000");
    mysqlToSql99State.put(Integer.valueOf(1179), "25000");
    mysqlToSql99State.put(Integer.valueOf(1207), "25000");
    mysqlToSql99State.put(Integer.valueOf(1045), "28000");
    mysqlToSql99State.put(Integer.valueOf(1698), "28000");
    mysqlToSql99State.put(Integer.valueOf(1873), "28000");
    mysqlToSql99State.put(Integer.valueOf(1758), "35000");
    mysqlToSql99State.put(Integer.valueOf(1046), "3D000");
    mysqlToSql99State.put(Integer.valueOf(1645), "0K000");
    mysqlToSql99State.put(Integer.valueOf(1887), "0Z002");
    mysqlToSql99State.put(Integer.valueOf(1339), "20000");
    mysqlToSql99State.put(Integer.valueOf(1058), "21S01");
    mysqlToSql99State.put(Integer.valueOf(1136), "21S01");
    mysqlToSql99State.put(Integer.valueOf(1138), "42000");
    mysqlToSql99State.put(Integer.valueOf(1903), "2201E");
    mysqlToSql99State.put(Integer.valueOf(1568), "25001");
    mysqlToSql99State.put(Integer.valueOf(1792), "25006");
    mysqlToSql99State.put(Integer.valueOf(1303), "2F003");
    mysqlToSql99State.put(Integer.valueOf(1321), "2F005");
    mysqlToSql99State.put(Integer.valueOf(1050), "42S01");
    mysqlToSql99State.put(Integer.valueOf(1051), "42S02");
    mysqlToSql99State.put(Integer.valueOf(1109), "42S02");
    mysqlToSql99State.put(Integer.valueOf(1146), "42S02");
    mysqlToSql99State.put(Integer.valueOf(1082), "42S12");
    mysqlToSql99State.put(Integer.valueOf(1060), "42S21");
    mysqlToSql99State.put(Integer.valueOf(1054), "42S22");
    mysqlToSql99State.put(Integer.valueOf(1247), "42S22");
    mysqlToSql99State.put(Integer.valueOf(1317), "70100");
    mysqlToSql99State.put(Integer.valueOf(1037), "HY001");
    mysqlToSql99State.put(Integer.valueOf(1038), "HY001");
    mysqlToSql99State.put(Integer.valueOf(1402), "XA100");
    mysqlToSql99State.put(Integer.valueOf(1614), "XA102");
    mysqlToSql99State.put(Integer.valueOf(1613), "XA106");
    mysqlToSql99State.put(Integer.valueOf(1401), "XAE03");
    mysqlToSql99State.put(Integer.valueOf(1397), "XAE04");
    mysqlToSql99State.put(Integer.valueOf(1398), "XAE05");
    mysqlToSql99State.put(Integer.valueOf(1399), "XAE07");
    mysqlToSql99State.put(Integer.valueOf(1440), "XAE08");
    mysqlToSql99State.put(Integer.valueOf(1400), "XAE09");
    mysqlToSql99State.put(Integer.valueOf(1205), "40001");
    mysqlToSql99State.put(Integer.valueOf(1213), "40001");
  }
  












  static SQLWarning convertShowWarningsToSQLWarnings(Connection connection)
    throws SQLException
  {
    return convertShowWarningsToSQLWarnings(connection, 0, false);
  }
  

















  static SQLWarning convertShowWarningsToSQLWarnings(Connection connection, int warningCountIfKnown, boolean forTruncationOnly)
    throws SQLException
  {
    Statement stmt = null;
    ResultSet warnRs = null;
    
    SQLWarning currentWarning = null;
    try
    {
      if (warningCountIfKnown < 100) {
        stmt = connection.createStatement();
        stmt.setFetchSize(0);
        
        if (stmt.getMaxRows() != 0) {
          stmt.setMaxRows(0);
        }
      }
      else {
        stmt = connection.createStatement(1003, 1007);
        stmt.setFetchSize(Integer.MIN_VALUE);
      }
      







      warnRs = stmt.executeQuery("SHOW WARNINGS");
      int code;
      while (warnRs.next()) {
        code = warnRs.getInt("Code");
        
        if (forTruncationOnly) {
          if ((code == 1265) || (code == 1264)) {
            DataTruncation newTruncation = new MysqlDataTruncation(warnRs.getString("Message"), 0, false, false, 0, 0, code);
            
            if (currentWarning == null) {
              currentWarning = newTruncation;
            } else {
              currentWarning.setNextWarning(newTruncation);
            }
          }
        }
        else {
          String message = warnRs.getString("Message");
          
          SQLWarning newWarning = new SQLWarning(message, mysqlToSqlState(code, connection.getUseSqlStateCodes()), code);
          
          if (currentWarning == null) {
            currentWarning = newWarning;
          } else {
            currentWarning.setNextWarning(newWarning);
          }
        }
      }
      
      if ((forTruncationOnly) && (currentWarning != null)) {
        throw currentWarning;
      }
      
      return currentWarning;
    } finally {
      SQLException reThrow = null;
      
      if (warnRs != null) {
        try {
          warnRs.close();
        } catch (SQLException sqlEx) {
          reThrow = sqlEx;
        }
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (SQLException sqlEx) {
          reThrow = sqlEx;
        }
      }
      
      if (reThrow != null) {
        throw reThrow;
      }
    }
  }
  
  public static void dumpSqlStatesMappingsAsXml() throws Exception {
    TreeMap<Integer, Integer> allErrorNumbers = new TreeMap();
    Map<Object, String> mysqlErrorNumbersToNames = new HashMap();
    





    for (Integer errorNumber : mysqlToSql99State.keySet()) {
      allErrorNumbers.put(errorNumber, errorNumber);
    }
    
    for (Integer errorNumber : mysqlToSqlState.keySet()) {
      allErrorNumbers.put(errorNumber, errorNumber);
    }
    



    Field[] possibleFields = MysqlErrorNumbers.class.getDeclaredFields();
    
    for (int i = 0; i < possibleFields.length; i++) {
      String fieldName = possibleFields[i].getName();
      
      if (fieldName.startsWith("ER_")) {
        mysqlErrorNumbersToNames.put(possibleFields[i].get(null), fieldName);
      }
    }
    
    System.out.println("<ErrorMappings>");
    
    for (Integer errorNumber : allErrorNumbers.keySet()) {
      String sql92State = mysqlToSql99(errorNumber.intValue());
      String oldSqlState = mysqlToXOpen(errorNumber.intValue());
      
      System.out.println("   <ErrorMapping mysqlErrorNumber=\"" + errorNumber + "\" mysqlErrorName=\"" + (String)mysqlErrorNumbersToNames.get(errorNumber) + "\" legacySqlState=\"" + (oldSqlState == null ? "" : oldSqlState) + "\" sql92SqlState=\"" + (sql92State == null ? "" : sql92State) + "\"/>");
    }
    


    System.out.println("</ErrorMappings>");
  }
  
  static String get(String stateCode) {
    return (String)sqlStateMessages.get(stateCode);
  }
  
  private static String mysqlToSql99(int errno) {
    Integer err = Integer.valueOf(errno);
    
    if (mysqlToSql99State.containsKey(err)) {
      return (String)mysqlToSql99State.get(err);
    }
    
    return "HY000";
  }
  







  static String mysqlToSqlState(int errno, boolean useSql92States)
  {
    if (useSql92States) {
      return mysqlToSql99(errno);
    }
    
    return mysqlToXOpen(errno);
  }
  
  private static String mysqlToXOpen(int errno) {
    Integer err = Integer.valueOf(errno);
    
    if (mysqlToSqlState.containsKey(err)) {
      return (String)mysqlToSqlState.get(err);
    }
    
    return "S1000";
  }
  










  public static SQLException createSQLException(String message, String sqlState, ExceptionInterceptor interceptor)
  {
    return createSQLException(message, sqlState, 0, interceptor);
  }
  
  public static SQLException createSQLException(String message, ExceptionInterceptor interceptor) {
    return createSQLException(message, interceptor, null);
  }
  
  public static SQLException createSQLException(String message, ExceptionInterceptor interceptor, Connection conn) {
    SQLException sqlEx = new SQLException(message);
    return runThroughExceptionInterceptor(interceptor, sqlEx, conn);
  }
  
  public static SQLException createSQLException(String message, String sqlState, Throwable cause, ExceptionInterceptor interceptor) {
    return createSQLException(message, sqlState, cause, interceptor, null);
  }
  
  public static SQLException createSQLException(String message, String sqlState, Throwable cause, ExceptionInterceptor interceptor, Connection conn) {
    SQLException sqlEx = createSQLException(message, sqlState, null);
    if (sqlEx.getCause() == null) {
      sqlEx.initCause(cause);
    }
    
    return runThroughExceptionInterceptor(interceptor, sqlEx, conn);
  }
  
  public static SQLException createSQLException(String message, String sqlState, int vendorErrorCode, ExceptionInterceptor interceptor) {
    return createSQLException(message, sqlState, vendorErrorCode, false, interceptor);
  }
  






  public static SQLException createSQLException(String message, String sqlState, int vendorErrorCode, boolean isTransient, ExceptionInterceptor interceptor)
  {
    return createSQLException(message, sqlState, vendorErrorCode, isTransient, interceptor, null);
  }
  
  public static SQLException createSQLException(String message, String sqlState, int vendorErrorCode, boolean isTransient, ExceptionInterceptor interceptor, Connection conn)
  {
    try {
      SQLException sqlEx = null;
      
      if (sqlState != null) {
        if (sqlState.startsWith("08")) {
          if (isTransient) {
            if (!Util.isJdbc4()) {
              sqlEx = new MySQLTransientConnectionException(message, sqlState, vendorErrorCode);
            } else {
              sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLTransientConnectionException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
            }
            
          }
          else if (!Util.isJdbc4()) {
            sqlEx = new MySQLNonTransientConnectionException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else if (sqlState.startsWith("22")) {
          if (!Util.isJdbc4()) {
            sqlEx = new MySQLDataException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLDataException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else if (sqlState.startsWith("23"))
        {
          if (!Util.isJdbc4()) {
            sqlEx = new MySQLIntegrityConstraintViolationException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else if (sqlState.startsWith("42")) {
          if (!Util.isJdbc4()) {
            sqlEx = new MySQLSyntaxErrorException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else if (sqlState.startsWith("40")) {
          if (!Util.isJdbc4()) {
            sqlEx = new MySQLTransactionRollbackException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else if (sqlState.startsWith("70100")) {
          if (!Util.isJdbc4()) {
            sqlEx = new MySQLQueryInterruptedException(message, sqlState, vendorErrorCode);
          } else {
            sqlEx = (SQLException)Util.getInstance("com.mysql.jdbc.exceptions.jdbc4.MySQLQueryInterruptedException", new Class[] { String.class, String.class, Integer.TYPE }, new Object[] { message, sqlState, Integer.valueOf(vendorErrorCode) }, interceptor);
          }
          
        }
        else {
          sqlEx = new SQLException(message, sqlState, vendorErrorCode);
        }
      } else {
        sqlEx = new SQLException(message, sqlState, vendorErrorCode);
      }
      
      return runThroughExceptionInterceptor(interceptor, sqlEx, conn);
    } catch (SQLException sqlEx) {
      SQLException unexpectedEx = new SQLException("Unable to create correct SQLException class instance, error class/codes may be incorrect. Reason: " + Util.stackTraceToString(sqlEx), "S1000");
      


      return runThroughExceptionInterceptor(interceptor, unexpectedEx, conn);
    }
  }
  
  public static SQLException createCommunicationsException(MySQLConnection conn, long lastPacketSentTimeMs, long lastPacketReceivedTimeMs, Exception underlyingException, ExceptionInterceptor interceptor)
  {
    SQLException exToReturn = null;
    
    if (!Util.isJdbc4()) {
      exToReturn = new CommunicationsException(conn, lastPacketSentTimeMs, lastPacketReceivedTimeMs, underlyingException);
    } else {
      try
      {
        exToReturn = (SQLException)Util.handleNewInstance(JDBC_4_COMMUNICATIONS_EXCEPTION_CTOR, new Object[] { conn, Long.valueOf(lastPacketSentTimeMs), Long.valueOf(lastPacketReceivedTimeMs), underlyingException }, interceptor);

      }
      catch (SQLException sqlEx)
      {
        return sqlEx;
      }
    }
    
    return runThroughExceptionInterceptor(interceptor, exToReturn, conn);
  }
  









  public static String createLinkFailureMessageBasedOnHeuristics(MySQLConnection conn, long lastPacketSentTimeMs, long lastPacketReceivedTimeMs, Exception underlyingException)
  {
    long serverTimeoutSeconds = 0L;
    boolean isInteractiveClient = false;
    
    if (conn != null) {
      isInteractiveClient = conn.getInteractiveClient();
      
      String serverTimeoutSecondsStr = null;
      
      if (isInteractiveClient) {
        serverTimeoutSecondsStr = conn.getServerVariable("interactive_timeout");
      } else {
        serverTimeoutSecondsStr = conn.getServerVariable("wait_timeout");
      }
      
      if (serverTimeoutSecondsStr != null) {
        try {
          serverTimeoutSeconds = Long.parseLong(serverTimeoutSecondsStr);
        } catch (NumberFormatException nfe) {
          serverTimeoutSeconds = 0L;
        }
      }
    }
    
    StringBuilder exceptionMessageBuf = new StringBuilder();
    
    long nowMs = System.currentTimeMillis();
    
    if (lastPacketSentTimeMs == 0L) {
      lastPacketSentTimeMs = nowMs;
    }
    
    long timeSinceLastPacketSentMs = nowMs - lastPacketSentTimeMs;
    long timeSinceLastPacketSeconds = timeSinceLastPacketSentMs / 1000L;
    
    long timeSinceLastPacketReceivedMs = nowMs - lastPacketReceivedTimeMs;
    
    int dueToTimeout = 0;
    
    StringBuilder timeoutMessageBuf = null;
    
    if (serverTimeoutSeconds != 0L) {
      if (timeSinceLastPacketSeconds > serverTimeoutSeconds) {
        dueToTimeout = 1;
        
        timeoutMessageBuf = new StringBuilder();
        
        timeoutMessageBuf.append(Messages.getString("CommunicationsException.2"));
        
        if (!isInteractiveClient) {
          timeoutMessageBuf.append(Messages.getString("CommunicationsException.3"));
        } else {
          timeoutMessageBuf.append(Messages.getString("CommunicationsException.4"));
        }
      }
    }
    else if (timeSinceLastPacketSeconds > 28800L) {
      dueToTimeout = 2;
      
      timeoutMessageBuf = new StringBuilder();
      
      timeoutMessageBuf.append(Messages.getString("CommunicationsException.5"));
      timeoutMessageBuf.append(Messages.getString("CommunicationsException.6"));
      timeoutMessageBuf.append(Messages.getString("CommunicationsException.7"));
      timeoutMessageBuf.append(Messages.getString("CommunicationsException.8"));
    }
    
    if ((dueToTimeout == 1) || (dueToTimeout == 2))
    {
      if (lastPacketReceivedTimeMs != 0L) {
        Object[] timingInfo = { Long.valueOf(timeSinceLastPacketReceivedMs), Long.valueOf(timeSinceLastPacketSentMs) };
        exceptionMessageBuf.append(Messages.getString("CommunicationsException.ServerPacketTimingInfo", timingInfo));
      } else {
        exceptionMessageBuf.append(Messages.getString("CommunicationsException.ServerPacketTimingInfoNoRecv", new Object[] { Long.valueOf(timeSinceLastPacketSentMs) }));
      }
      

      if (timeoutMessageBuf != null) {
        exceptionMessageBuf.append(timeoutMessageBuf);
      }
      
      exceptionMessageBuf.append(Messages.getString("CommunicationsException.11"));
      exceptionMessageBuf.append(Messages.getString("CommunicationsException.12"));
      exceptionMessageBuf.append(Messages.getString("CommunicationsException.13"));





    }
    else if ((underlyingException instanceof BindException)) {
      if ((conn.getLocalSocketAddress() != null) && (!Util.interfaceExists(conn.getLocalSocketAddress()))) {
        exceptionMessageBuf.append(Messages.getString("CommunicationsException.LocalSocketAddressNotAvailable"));
      }
      else {
        exceptionMessageBuf.append(Messages.getString("CommunicationsException.TooManyClientConnections"));
      }
    }
    

    if (exceptionMessageBuf.length() == 0)
    {
      exceptionMessageBuf.append(Messages.getString("CommunicationsException.20"));
      
      if ((conn != null) && (conn.getMaintainTimeStats()) && (!conn.getParanoid())) {
        exceptionMessageBuf.append("\n\n");
        if (lastPacketReceivedTimeMs != 0L) {
          Object[] timingInfo = { Long.valueOf(timeSinceLastPacketReceivedMs), Long.valueOf(timeSinceLastPacketSentMs) };
          exceptionMessageBuf.append(Messages.getString("CommunicationsException.ServerPacketTimingInfo", timingInfo));
        } else {
          exceptionMessageBuf.append(Messages.getString("CommunicationsException.ServerPacketTimingInfoNoRecv", new Object[] { Long.valueOf(timeSinceLastPacketSentMs) }));
        }
      }
    }
    

    return exceptionMessageBuf.toString();
  }
  







  private static SQLException runThroughExceptionInterceptor(ExceptionInterceptor exInterceptor, SQLException sqlEx, Connection conn)
  {
    if (exInterceptor != null) {
      SQLException interceptedEx = exInterceptor.interceptException(sqlEx, conn);
      
      if (interceptedEx != null) {
        return interceptedEx;
      }
    }
    return sqlEx;
  }
  



  public static SQLException createBatchUpdateException(SQLException underlyingEx, long[] updateCounts, ExceptionInterceptor interceptor)
    throws SQLException
  {
    SQLException newEx;
    

    SQLException newEx;
    

    if (Util.isJdbc42()) {
      newEx = (SQLException)Util.getInstance("java.sql.BatchUpdateException", new Class[] { String.class, String.class, Integer.TYPE, [J.class, Throwable.class }, new Object[] { underlyingEx.getMessage(), underlyingEx.getSQLState(), Integer.valueOf(underlyingEx.getErrorCode()), updateCounts, underlyingEx }, interceptor);

    }
    else
    {
      newEx = new BatchUpdateException(underlyingEx.getMessage(), underlyingEx.getSQLState(), underlyingEx.getErrorCode(), Util.truncateAndConvertToInt(updateCounts));
      
      newEx.initCause(underlyingEx);
    }
    return runThroughExceptionInterceptor(interceptor, newEx, null);
  }
  

  public static SQLException createSQLFeatureNotSupportedException()
    throws SQLException
  {
    SQLException newEx;
    SQLException newEx;
    if (Util.isJdbc4()) {
      newEx = (SQLException)Util.getInstance("java.sql.SQLFeatureNotSupportedException", null, null, null);
    } else {
      newEx = new NotImplemented();
    }
    
    return newEx;
  }
  


  public static SQLException createSQLFeatureNotSupportedException(String message, String sqlState, ExceptionInterceptor interceptor)
    throws SQLException
  {
    SQLException newEx;
    

    SQLException newEx;
    
    if (Util.isJdbc4()) {
      newEx = (SQLException)Util.getInstance("java.sql.SQLFeatureNotSupportedException", new Class[] { String.class, String.class }, new Object[] { message, sqlState }, interceptor);
    }
    else {
      newEx = new NotImplemented();
    }
    
    return runThroughExceptionInterceptor(interceptor, newEx, null);
  }
  
  public SQLError() {}
}
