package com.mysql.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;






















public class CallableStatement
  extends PreparedStatement
  implements java.sql.CallableStatement
{
  protected static final Constructor<?> JDBC_4_CSTMT_2_ARGS_CTOR;
  protected static final Constructor<?> JDBC_4_CSTMT_4_ARGS_CTOR;
  private static final int NOT_OUTPUT_PARAMETER_INDICATOR = Integer.MIN_VALUE;
  private static final String PARAMETER_NAMESPACE_PREFIX = "@com_mysql_jdbc_outparam_";
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        String jdbc4ClassName = Util.isJdbc42() ? "com.mysql.jdbc.JDBC42CallableStatement" : "com.mysql.jdbc.JDBC4CallableStatement";
        JDBC_4_CSTMT_2_ARGS_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, CallableStatementParamInfo.class });
        
        JDBC_4_CSTMT_4_ARGS_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, String.class, String.class, Boolean.TYPE });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_CSTMT_4_ARGS_CTOR = null;
      JDBC_4_CSTMT_2_ARGS_CTOR = null;
    }
  }
  

  protected static class CallableStatementParam
  {
    int desiredJdbcType;
    
    int index;
    
    int inOutModifier;
    
    boolean isIn;
    
    boolean isOut;
    
    int jdbcType;
    
    short nullability;
    
    String paramName;
    
    int precision;
    int scale;
    String typeName;
    
    CallableStatementParam(String name, int idx, boolean in, boolean out, int jdbcType, String typeName, int precision, int scale, short nullability, int inOutModifier)
    {
      paramName = name;
      isIn = in;
      isOut = out;
      index = idx;
      
      this.jdbcType = jdbcType;
      this.typeName = typeName;
      this.precision = precision;
      this.scale = scale;
      this.nullability = nullability;
      this.inOutModifier = inOutModifier;
    }
    




    protected Object clone()
      throws CloneNotSupportedException
    {
      return super.clone();
    }
  }
  

  protected class CallableStatementParamInfo
    implements ParameterMetaData
  {
    String catalogInUse;
    
    boolean isFunctionCall;
    
    String nativeSql;
    
    int numParameters;
    
    List<CallableStatement.CallableStatementParam> parameterList;
    
    Map<String, CallableStatement.CallableStatementParam> parameterMap;
    
    boolean isReadOnlySafeProcedure = false;
    



    boolean isReadOnlySafeChecked = false;
    







    CallableStatementParamInfo(CallableStatementParamInfo fullParamInfo)
    {
      nativeSql = originalSql;
      catalogInUse = currentCatalog;
      isFunctionCall = isFunctionCall;
      
      int[] localParameterMap = placeholderToParameterIndexMap;
      int parameterMapLength = localParameterMap.length;
      
      isReadOnlySafeProcedure = isReadOnlySafeProcedure;
      isReadOnlySafeChecked = isReadOnlySafeChecked;
      parameterList = new ArrayList(numParameters);
      parameterMap = new HashMap(numParameters);
      
      if (isFunctionCall)
      {
        parameterList.add(parameterList.get(0));
      }
      
      int offset = isFunctionCall ? 1 : 0;
      
      for (int i = 0; i < parameterMapLength; i++) {
        if (localParameterMap[i] != 0) {
          CallableStatement.CallableStatementParam param = (CallableStatement.CallableStatementParam)parameterList.get(localParameterMap[i] + offset);
          
          parameterList.add(param);
          parameterMap.put(paramName, param);
        }
      }
      
      numParameters = parameterList.size();
    }
    
    CallableStatementParamInfo(ResultSet paramTypesRs) throws SQLException
    {
      boolean hadRows = paramTypesRs.last();
      
      nativeSql = originalSql;
      catalogInUse = currentCatalog;
      isFunctionCall = callingStoredFunction;
      
      if (hadRows) {
        numParameters = paramTypesRs.getRow();
        
        parameterList = new ArrayList(numParameters);
        parameterMap = new HashMap(numParameters);
        
        paramTypesRs.beforeFirst();
        
        addParametersFromDBMD(paramTypesRs);
      } else {
        numParameters = 0;
      }
      
      if (isFunctionCall) {
        numParameters += 1;
      }
    }
    
    private void addParametersFromDBMD(ResultSet paramTypesRs) throws SQLException {
      int i = 0;
      
      while (paramTypesRs.next()) {
        String paramName = paramTypesRs.getString(4);
        int inOutModifier;
        switch (paramTypesRs.getInt(5)) {
        case 1: 
          inOutModifier = 1;
          break;
        case 2: 
          inOutModifier = 2;
          break;
        case 4: 
        case 5: 
          inOutModifier = 4;
          break;
        case 3: default: 
          inOutModifier = 0;
        }
        
        boolean isOutParameter = false;
        boolean isInParameter = false;
        
        if ((i == 0) && (isFunctionCall)) {
          isOutParameter = true;
          isInParameter = false;
        } else if (inOutModifier == 2) {
          isOutParameter = true;
          isInParameter = true;
        } else if (inOutModifier == 1) {
          isOutParameter = false;
          isInParameter = true;
        } else if (inOutModifier == 4) {
          isOutParameter = true;
          isInParameter = false;
        }
        
        int jdbcType = paramTypesRs.getInt(6);
        String typeName = paramTypesRs.getString(7);
        int precision = paramTypesRs.getInt(8);
        int scale = paramTypesRs.getInt(10);
        short nullability = paramTypesRs.getShort(12);
        
        CallableStatement.CallableStatementParam paramInfoToAdd = new CallableStatement.CallableStatementParam(paramName, i++, isInParameter, isOutParameter, jdbcType, typeName, precision, scale, nullability, inOutModifier);
        

        parameterList.add(paramInfoToAdd);
        parameterMap.put(paramName, paramInfoToAdd);
      }
    }
    
    protected void checkBounds(int paramIndex) throws SQLException {
      int localParamIndex = paramIndex - 1;
      
      if ((paramIndex < 0) || (localParamIndex >= numParameters)) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.11") + paramIndex + Messages.getString("CallableStatement.12") + numParameters + Messages.getString("CallableStatement.13"), "S1009", getExceptionInterceptor());
      }
    }
    





    protected Object clone()
      throws CloneNotSupportedException
    {
      return super.clone();
    }
    
    CallableStatement.CallableStatementParam getParameter(int index) {
      return (CallableStatement.CallableStatementParam)parameterList.get(index);
    }
    
    CallableStatement.CallableStatementParam getParameter(String name) {
      return (CallableStatement.CallableStatementParam)parameterMap.get(name);
    }
    
    public String getParameterClassName(int arg0) throws SQLException {
      String mysqlTypeName = getParameterTypeName(arg0);
      
      boolean isBinaryOrBlob = (StringUtils.indexOfIgnoreCase(mysqlTypeName, "BLOB") != -1) || (StringUtils.indexOfIgnoreCase(mysqlTypeName, "BINARY") != -1);
      
      boolean isUnsigned = StringUtils.indexOfIgnoreCase(mysqlTypeName, "UNSIGNED") != -1;
      
      int mysqlTypeIfKnown = 0;
      
      if (StringUtils.startsWithIgnoreCase(mysqlTypeName, "MEDIUMINT")) {
        mysqlTypeIfKnown = 9;
      }
      
      return ResultSetMetaData.getClassNameForJavaType(getParameterType(arg0), isUnsigned, mysqlTypeIfKnown, isBinaryOrBlob, false, connection.getYearIsDateType());
    }
    
    public int getParameterCount() throws SQLException
    {
      if (parameterList == null) {
        return 0;
      }
      
      return parameterList.size();
    }
    
    public int getParameterMode(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1inOutModifier;
    }
    
    public int getParameterType(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1jdbcType;
    }
    
    public String getParameterTypeName(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1typeName;
    }
    
    public int getPrecision(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1precision;
    }
    
    public int getScale(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1scale;
    }
    
    public int isNullable(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return getParameter1nullability;
    }
    
    public boolean isSigned(int arg0) throws SQLException {
      checkBounds(arg0);
      
      return false;
    }
    
    Iterator<CallableStatement.CallableStatementParam> iterator() {
      return parameterList.iterator();
    }
    
    int numberOfParameters() {
      return numParameters;
    }
    

    public boolean isWrapperFor(Class<?> iface)
      throws SQLException
    {
      checkClosed();
      

      return iface.isInstance(this);
    }
    

    public <T> T unwrap(Class<T> iface)
      throws SQLException
    {
      try
      {
        return iface.cast(this);
      } catch (ClassCastException cce) {
        throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", getExceptionInterceptor());
      }
    }
  }
  




  private static String mangleParameterName(String origParameterName)
  {
    if (origParameterName == null) {
      return null;
    }
    
    int offset = 0;
    
    if ((origParameterName.length() > 0) && (origParameterName.charAt(0) == '@')) {
      offset = 1;
    }
    
    StringBuilder paramNameBuf = new StringBuilder("@com_mysql_jdbc_outparam_".length() + origParameterName.length());
    paramNameBuf.append("@com_mysql_jdbc_outparam_");
    paramNameBuf.append(origParameterName.substring(offset));
    
    return paramNameBuf.toString();
  }
  
  private boolean callingStoredFunction = false;
  
  private ResultSetInternalMethods functionReturnValueResults;
  
  private boolean hasOutputParams = false;
  

  private ResultSetInternalMethods outputParameterResults;
  

  protected boolean outputParamWasNull = false;
  


  private int[] parameterIndexToRsIndex;
  

  protected CallableStatementParamInfo paramInfo;
  

  private CallableStatementParam returnValueParam;
  

  private int[] placeholderToParameterIndexMap;
  


  public CallableStatement(MySQLConnection conn, CallableStatementParamInfo paramInfo)
    throws SQLException
  {
    super(conn, nativeSql, catalogInUse);
    
    this.paramInfo = paramInfo;
    callingStoredFunction = paramInfoisFunctionCall;
    
    if (callingStoredFunction) {
      parameterCount += 1;
    }
    
    retrieveGeneratedKeys = true;
  }
  





  protected static CallableStatement getInstance(MySQLConnection conn, String sql, String catalog, boolean isFunctionCall)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new CallableStatement(conn, sql, catalog, isFunctionCall);
    }
    
    return (CallableStatement)Util.handleNewInstance(JDBC_4_CSTMT_4_ARGS_CTOR, new Object[] { conn, sql, catalog, Boolean.valueOf(isFunctionCall) }, conn.getExceptionInterceptor());
  }
  






  protected static CallableStatement getInstance(MySQLConnection conn, CallableStatementParamInfo paramInfo)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new CallableStatement(conn, paramInfo);
    }
    
    return (CallableStatement)Util.handleNewInstance(JDBC_4_CSTMT_2_ARGS_CTOR, new Object[] { conn, paramInfo }, conn.getExceptionInterceptor());
  }
  

  private void generateParameterMap()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (paramInfo == null) {
        return;
      }
      


      int parameterCountFromMetaData = paramInfo.getParameterCount();
      


      if (callingStoredFunction) {
        parameterCountFromMetaData--;
      }
      
      if ((paramInfo != null) && (parameterCount != parameterCountFromMetaData)) {
        placeholderToParameterIndexMap = new int[parameterCount];
        
        int startPos = callingStoredFunction ? StringUtils.indexOfIgnoreCase(originalSql, "SELECT") : StringUtils.indexOfIgnoreCase(originalSql, "CALL");
        

        if (startPos != -1) {
          int parenOpenPos = originalSql.indexOf('(', startPos + 4);
          
          if (parenOpenPos != -1) {
            int parenClosePos = StringUtils.indexOfIgnoreCase(parenOpenPos, originalSql, ")", "'", "'", StringUtils.SEARCH_MODE__ALL);
            
            if (parenClosePos != -1) {
              List<?> parsedParameters = StringUtils.split(originalSql.substring(parenOpenPos + 1, parenClosePos), ",", "'\"", "'\"", true);
              
              int numParsedParameters = parsedParameters.size();
              


              if (numParsedParameters != parameterCount) {}
              


              int placeholderCount = 0;
              
              for (int i = 0; i < numParsedParameters; i++) {
                if (((String)parsedParameters.get(i)).equals("?")) {
                  placeholderToParameterIndexMap[(placeholderCount++)] = i;
                }
              }
            }
          }
        }
      }
    }
  }
  











  public CallableStatement(MySQLConnection conn, String sql, String catalog, boolean isFunctionCall)
    throws SQLException
  {
    super(conn, sql, catalog);
    
    callingStoredFunction = isFunctionCall;
    
    if (!callingStoredFunction) {
      if (!StringUtils.startsWithIgnoreCaseAndWs(sql, "CALL"))
      {
        fakeParameterTypes(false);
      } else {
        determineParameterTypes();
      }
      
      generateParameterMap();
    } else {
      determineParameterTypes();
      generateParameterMap();
      
      parameterCount += 1;
    }
    
    retrieveGeneratedKeys = true;
  }
  




  public void addBatch()
    throws SQLException
  {
    setOutParams();
    
    super.addBatch();
  }
  
  private CallableStatementParam checkIsOutputParam(int paramIndex) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (callingStoredFunction) {
        if (paramIndex == 1)
        {
          if (returnValueParam == null) {
            returnValueParam = new CallableStatementParam("", 0, false, true, 12, "VARCHAR", 0, 0, (short)2, 5);
          }
          

          return returnValueParam;
        }
        

        paramIndex--;
      }
      
      checkParameterIndexBounds(paramIndex);
      
      int localParamIndex = paramIndex - 1;
      
      if (placeholderToParameterIndexMap != null) {
        localParamIndex = placeholderToParameterIndexMap[localParamIndex];
      }
      
      CallableStatementParam paramDescriptor = paramInfo.getParameter(localParamIndex);
      


      if (connection.getNoAccessToProcedureBodies()) {
        isOut = true;
        isIn = true;
        inOutModifier = 2;
      } else if (!isOut) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.9") + paramIndex + Messages.getString("CallableStatement.10"), "S1009", getExceptionInterceptor());
      }
      

      hasOutputParams = true;
      
      return paramDescriptor;
    }
  }
  



  private void checkParameterIndexBounds(int paramIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      paramInfo.checkBounds(paramIndex);
    }
  }
  





  private void checkStreamability()
    throws SQLException
  {
    if ((hasOutputParams) && (createStreamingResultSet())) {
      throw SQLError.createSQLException(Messages.getString("CallableStatement.14"), "S1C00", getExceptionInterceptor());
    }
  }
  
  public void clearParameters() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.clearParameters();
      try
      {
        if (outputParameterResults != null) {
          outputParameterResults.close();
        }
      } finally {
        outputParameterResults = null;
      }
    }
  }
  





  private void fakeParameterTypes(boolean isReallyProcedure)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      Field[] fields = new Field[13];
      
      fields[0] = new Field("", "PROCEDURE_CAT", 1, 0);
      fields[1] = new Field("", "PROCEDURE_SCHEM", 1, 0);
      fields[2] = new Field("", "PROCEDURE_NAME", 1, 0);
      fields[3] = new Field("", "COLUMN_NAME", 1, 0);
      fields[4] = new Field("", "COLUMN_TYPE", 1, 0);
      fields[5] = new Field("", "DATA_TYPE", 5, 0);
      fields[6] = new Field("", "TYPE_NAME", 1, 0);
      fields[7] = new Field("", "PRECISION", 4, 0);
      fields[8] = new Field("", "LENGTH", 4, 0);
      fields[9] = new Field("", "SCALE", 5, 0);
      fields[10] = new Field("", "RADIX", 5, 0);
      fields[11] = new Field("", "NULLABLE", 5, 0);
      fields[12] = new Field("", "REMARKS", 1, 0);
      
      String procName = isReallyProcedure ? extractProcedureName() : null;
      
      byte[] procNameAsBytes = null;
      try
      {
        procNameAsBytes = procName == null ? null : StringUtils.getBytes(procName, "UTF-8");
      } catch (UnsupportedEncodingException ueEx) {
        procNameAsBytes = StringUtils.s2b(procName, connection);
      }
      
      ArrayList<ResultSetRow> resultRows = new ArrayList();
      
      for (int i = 0; i < parameterCount; i++) {
        byte[][] row = new byte[13][];
        row[0] = null;
        row[1] = null;
        row[2] = procNameAsBytes;
        row[3] = StringUtils.s2b(String.valueOf(i), connection);
        
        row[4] = StringUtils.s2b(String.valueOf(1), connection);
        
        row[5] = StringUtils.s2b(String.valueOf(12), connection);
        row[6] = StringUtils.s2b("VARCHAR", connection);
        row[7] = StringUtils.s2b(Integer.toString(65535), connection);
        row[8] = StringUtils.s2b(Integer.toString(65535), connection);
        row[9] = StringUtils.s2b(Integer.toString(0), connection);
        row[10] = StringUtils.s2b(Integer.toString(10), connection);
        
        row[11] = StringUtils.s2b(Integer.toString(2), connection);
        
        row[12] = null;
        
        resultRows.add(new ByteArrayRow(row, getExceptionInterceptor()));
      }
      
      ResultSet paramTypesRs = DatabaseMetaData.buildResultSet(fields, resultRows, connection);
      
      convertGetProcedureColumnsToInternalDescriptors(paramTypesRs);
    }
  }
  
  private void determineParameterTypes() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSet paramTypesRs = null;
      
      try
      {
        String procName = extractProcedureName();
        String quotedId = "";
        try {
          quotedId = connection.supportsQuotedIdentifiers() ? connection.getMetaData().getIdentifierQuoteString() : "";
        }
        catch (SQLException sqlEx)
        {
          AssertionFailedException.shouldNotHappen(sqlEx);
        }
        
        List<?> parseList = StringUtils.splitDBdotName(procName, "", quotedId, connection.isNoBackslashEscapesSet());
        String tmpCatalog = "";
        
        if (parseList.size() == 2) {
          tmpCatalog = (String)parseList.get(0);
          procName = (String)parseList.get(1);
        }
        


        java.sql.DatabaseMetaData dbmd = connection.getMetaData();
        
        boolean useCatalog = false;
        
        if (tmpCatalog.length() <= 0) {
          useCatalog = true;
        }
        
        paramTypesRs = dbmd.getProcedureColumns((connection.versionMeetsMinimum(5, 0, 2)) && (useCatalog) ? currentCatalog : tmpCatalog, null, procName, "%");
        

        boolean hasResults = false;
        try {
          if (paramTypesRs.next()) {
            paramTypesRs.previous();
            hasResults = true;
          }
        }
        catch (Exception e) {}
        
        if (hasResults) {
          convertGetProcedureColumnsToInternalDescriptors(paramTypesRs);
        } else {
          fakeParameterTypes(true);
        }
      } finally {
        SQLException sqlExRethrow = null;
        
        if (paramTypesRs != null) {
          try {
            paramTypesRs.close();
          } catch (SQLException sqlEx) {
            sqlExRethrow = sqlEx;
          }
          
          paramTypesRs = null;
        }
        
        if (sqlExRethrow != null) {
          throw sqlExRethrow;
        }
      }
    }
  }
  
  private void convertGetProcedureColumnsToInternalDescriptors(ResultSet paramTypesRs) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      paramInfo = new CallableStatementParamInfo(paramTypesRs);
    }
  }
  




  public boolean execute()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      boolean returnVal = false;
      
      checkStreamability();
      
      setInOutParamsOnServer();
      setOutParams();
      
      returnVal = super.execute();
      
      if (callingStoredFunction) {
        functionReturnValueResults = results;
        functionReturnValueResults.next();
        results = null;
      }
      
      retrieveOutParams();
      
      if (!callingStoredFunction) {
        return returnVal;
      }
      

      return false;
    }
  }
  




  public ResultSet executeQuery()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      checkStreamability();
      
      ResultSet execResults = null;
      
      setInOutParamsOnServer();
      setOutParams();
      
      execResults = super.executeQuery();
      
      retrieveOutParams();
      
      return execResults;
    }
  }
  




  public int executeUpdate()
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate());
  }
  
  private String extractProcedureName() throws SQLException {
    String sanitizedSql = StringUtils.stripComments(originalSql, "`\"'", "`\"'", true, false, true, true);
    

    int endCallIndex = StringUtils.indexOfIgnoreCase(sanitizedSql, "CALL ");
    int offset = 5;
    
    if (endCallIndex == -1) {
      endCallIndex = StringUtils.indexOfIgnoreCase(sanitizedSql, "SELECT ");
      offset = 7;
    }
    
    if (endCallIndex != -1) {
      StringBuilder nameBuf = new StringBuilder();
      
      String trimmedStatement = sanitizedSql.substring(endCallIndex + offset).trim();
      
      int statementLength = trimmedStatement.length();
      
      for (int i = 0; i < statementLength; i++) {
        char c = trimmedStatement.charAt(i);
        
        if ((Character.isWhitespace(c)) || (c == '(') || (c == '?')) {
          break;
        }
        nameBuf.append(c);
      }
      

      return nameBuf.toString();
    }
    
    throw SQLError.createSQLException(Messages.getString("CallableStatement.1"), "S1000", getExceptionInterceptor());
  }
  









  protected String fixParameterName(String paramNameIn)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (((paramNameIn == null) || (paramNameIn.length() == 0)) && (!hasParametersView())) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.0") + paramNameIn == null ? Messages.getString("CallableStatement.15") : Messages.getString("CallableStatement.16"), "S1009", getExceptionInterceptor());
      }
      


      if ((paramNameIn == null) && (hasParametersView())) {
        paramNameIn = "nullpn";
      }
      
      if (connection.getNoAccessToProcedureBodies()) {
        throw SQLError.createSQLException("No access to parameters by name when connection has been configured not to access procedure bodies", "S1009", getExceptionInterceptor());
      }
      

      return mangleParameterName(paramNameIn);
    }
  }
  

  public Array getArray(int i)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(i);
      
      Array retValue = rs.getArray(mapOutputParameterIndexToRsIndex(i));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Array getArray(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Array retValue = rs.getArray(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public BigDecimal getBigDecimal(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      BigDecimal retValue = rs.getBigDecimal(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  







  @Deprecated
  public BigDecimal getBigDecimal(int parameterIndex, int scale)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      BigDecimal retValue = rs.getBigDecimal(mapOutputParameterIndexToRsIndex(parameterIndex), scale);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public BigDecimal getBigDecimal(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      BigDecimal retValue = rs.getBigDecimal(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Blob getBlob(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Blob retValue = rs.getBlob(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Blob getBlob(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Blob retValue = rs.getBlob(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public boolean getBoolean(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      boolean retValue = rs.getBoolean(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public boolean getBoolean(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      boolean retValue = rs.getBoolean(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public byte getByte(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      byte retValue = rs.getByte(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public byte getByte(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      byte retValue = rs.getByte(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public byte[] getBytes(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      byte[] retValue = rs.getBytes(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public byte[] getBytes(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      byte[] retValue = rs.getBytes(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Clob getClob(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Clob retValue = rs.getClob(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Clob getClob(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Clob retValue = rs.getClob(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Date getDate(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Date retValue = rs.getDate(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Date getDate(int parameterIndex, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Date retValue = rs.getDate(mapOutputParameterIndexToRsIndex(parameterIndex), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Date getDate(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Date retValue = rs.getDate(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Date getDate(String parameterName, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Date retValue = rs.getDate(fixParameterName(parameterName), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public double getDouble(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      double retValue = rs.getDouble(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public double getDouble(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      double retValue = rs.getDouble(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public float getFloat(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      float retValue = rs.getFloat(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public float getFloat(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      float retValue = rs.getFloat(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public int getInt(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      int retValue = rs.getInt(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public int getInt(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      int retValue = rs.getInt(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public long getLong(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      long retValue = rs.getLong(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public long getLong(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      long retValue = rs.getLong(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  
  protected int getNamedParamIndex(String paramName, boolean forOut) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (connection.getNoAccessToProcedureBodies()) {
        throw SQLError.createSQLException("No access to parameters by name when connection has been configured not to access procedure bodies", "S1009", getExceptionInterceptor());
      }
      


      if ((paramName == null) || (paramName.length() == 0)) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.2"), "S1009", getExceptionInterceptor());
      }
      
      CallableStatementParam namedParamInfo;
      if ((paramInfo == null) || ((namedParamInfo = paramInfo.getParameter(paramName)) == null)) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.3") + paramName + Messages.getString("CallableStatement.4"), "S1009", getExceptionInterceptor());
      }
      
      CallableStatementParam namedParamInfo;
      if ((forOut) && (!isOut)) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.5") + paramName + Messages.getString("CallableStatement.6"), "S1009", getExceptionInterceptor());
      }
      

      if (placeholderToParameterIndexMap == null) {
        return index + 1;
      }
      
      for (int i = 0; i < placeholderToParameterIndexMap.length; i++) {
        if (placeholderToParameterIndexMap[i] == index) {
          return i + 1;
        }
      }
      
      throw SQLError.createSQLException("Can't find local placeholder mapping for parameter named \"" + paramName + "\".", "S1009", getExceptionInterceptor());
    }
  }
  


  public Object getObject(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      CallableStatementParam paramDescriptor = checkIsOutputParam(parameterIndex);
      
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Object retVal = rs.getObjectStoredProc(mapOutputParameterIndexToRsIndex(parameterIndex), desiredJdbcType);
      
      outputParamWasNull = rs.wasNull();
      
      return retVal;
    }
  }
  

  public Object getObject(int parameterIndex, Map<String, Class<?>> map)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Object retVal = rs.getObject(mapOutputParameterIndexToRsIndex(parameterIndex), map);
      
      outputParamWasNull = rs.wasNull();
      
      return retVal;
    }
  }
  

  public Object getObject(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Object retValue = rs.getObject(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Object getObject(String parameterName, Map<String, Class<?>> map)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Object retValue = rs.getObject(fixParameterName(parameterName), map);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  
  public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      

      T retVal = ((ResultSetImpl)rs).getObject(mapOutputParameterIndexToRsIndex(parameterIndex), type);
      
      outputParamWasNull = rs.wasNull();
      
      return retVal;
    }
  }
  
  public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      T retValue = ((ResultSetImpl)rs).getObject(fixParameterName(parameterName), type);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  








  protected ResultSetInternalMethods getOutputParameters(int paramIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      outputParamWasNull = false;
      
      if ((paramIndex == 1) && (callingStoredFunction) && (returnValueParam != null)) {
        return functionReturnValueResults;
      }
      
      if (outputParameterResults == null) {
        if (paramInfo.numberOfParameters() == 0) {
          throw SQLError.createSQLException(Messages.getString("CallableStatement.7"), "S1009", getExceptionInterceptor());
        }
        
        throw SQLError.createSQLException(Messages.getString("CallableStatement.8"), "S1000", getExceptionInterceptor());
      }
      
      return outputParameterResults;
    }
  }
  
  public ParameterMetaData getParameterMetaData() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (placeholderToParameterIndexMap == null) {
        return paramInfo;
      }
      
      return new CallableStatementParamInfo(paramInfo);
    }
  }
  

  public Ref getRef(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Ref retValue = rs.getRef(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Ref getRef(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Ref retValue = rs.getRef(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public short getShort(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      short retValue = rs.getShort(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public short getShort(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      short retValue = rs.getShort(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public String getString(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      String retValue = rs.getString(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public String getString(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      String retValue = rs.getString(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Time getTime(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Time retValue = rs.getTime(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Time getTime(int parameterIndex, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Time retValue = rs.getTime(mapOutputParameterIndexToRsIndex(parameterIndex), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Time getTime(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Time retValue = rs.getTime(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Time getTime(String parameterName, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Time retValue = rs.getTime(fixParameterName(parameterName), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Timestamp getTimestamp(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Timestamp retValue = rs.getTimestamp(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Timestamp getTimestamp(int parameterIndex, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      Timestamp retValue = rs.getTimestamp(mapOutputParameterIndexToRsIndex(parameterIndex), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Timestamp getTimestamp(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Timestamp retValue = rs.getTimestamp(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public Timestamp getTimestamp(String parameterName, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      Timestamp retValue = rs.getTimestamp(fixParameterName(parameterName), cal);
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public URL getURL(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
      
      URL retValue = rs.getURL(mapOutputParameterIndexToRsIndex(parameterIndex));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  

  public URL getURL(String parameterName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetInternalMethods rs = getOutputParameters(0);
      
      URL retValue = rs.getURL(fixParameterName(parameterName));
      
      outputParamWasNull = rs.wasNull();
      
      return retValue;
    }
  }
  
  protected int mapOutputParameterIndexToRsIndex(int paramIndex) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((returnValueParam != null) && (paramIndex == 1)) {
        return 1;
      }
      
      checkParameterIndexBounds(paramIndex);
      
      int localParamIndex = paramIndex - 1;
      
      if (placeholderToParameterIndexMap != null) {
        localParamIndex = placeholderToParameterIndexMap[localParamIndex];
      }
      
      int rsIndex = parameterIndexToRsIndex[localParamIndex];
      
      if (rsIndex == Integer.MIN_VALUE) {
        throw SQLError.createSQLException(Messages.getString("CallableStatement.21") + paramIndex + Messages.getString("CallableStatement.22"), "S1009", getExceptionInterceptor());
      }
      

      return rsIndex + 1;
    }
  }
  

  public void registerOutParameter(int parameterIndex, int sqlType)
    throws SQLException
  {
    CallableStatementParam paramDescriptor = checkIsOutputParam(parameterIndex);
    desiredJdbcType = sqlType;
  }
  

  public void registerOutParameter(int parameterIndex, int sqlType, int scale)
    throws SQLException
  {
    registerOutParameter(parameterIndex, sqlType);
  }
  

  public void registerOutParameter(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    checkIsOutputParam(parameterIndex);
  }
  

  public void registerOutParameter(String parameterName, int sqlType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      registerOutParameter(getNamedParamIndex(parameterName, true), sqlType);
    }
  }
  

  public void registerOutParameter(String parameterName, int sqlType, int scale)
    throws SQLException
  {
    registerOutParameter(getNamedParamIndex(parameterName, true), sqlType);
  }
  

  public void registerOutParameter(String parameterName, int sqlType, String typeName)
    throws SQLException
  {
    registerOutParameter(getNamedParamIndex(parameterName, true), sqlType, typeName);
  }
  




  private void retrieveOutParams()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      int numParameters = paramInfo.numberOfParameters();
      
      parameterIndexToRsIndex = new int[numParameters];
      
      for (int i = 0; i < numParameters; i++) {
        parameterIndexToRsIndex[i] = Integer.MIN_VALUE;
      }
      
      int localParamIndex = 0;
      
      if (numParameters > 0) {
        StringBuilder outParameterQuery = new StringBuilder("SELECT ");
        
        boolean firstParam = true;
        boolean hadOutputParams = false;
        
        for (Iterator<CallableStatementParam> paramIter = paramInfo.iterator(); paramIter.hasNext();) {
          CallableStatementParam retrParamInfo = (CallableStatementParam)paramIter.next();
          
          if (isOut) {
            hadOutputParams = true;
            
            parameterIndexToRsIndex[index] = (localParamIndex++);
            
            if ((paramName == null) && (hasParametersView())) {
              paramName = ("nullnp" + index);
            }
            
            String outParameterName = mangleParameterName(paramName);
            
            if (!firstParam) {
              outParameterQuery.append(",");
            } else {
              firstParam = false;
            }
            
            if (!outParameterName.startsWith("@")) {
              outParameterQuery.append('@');
            }
            
            outParameterQuery.append(outParameterName);
          }
        }
        
        if (hadOutputParams)
        {
          Statement outParameterStmt = null;
          ResultSet outParamRs = null;
          try
          {
            outParameterStmt = connection.createStatement();
            outParamRs = outParameterStmt.executeQuery(outParameterQuery.toString());
            outputParameterResults = ((ResultSetInternalMethods)outParamRs).copy();
            
            if (!outputParameterResults.next()) {
              outputParameterResults.close();
              outputParameterResults = null;
            }
          } finally {
            if (outParameterStmt != null) {
              outParameterStmt.close();
            }
          }
        } else {
          outputParameterResults = null;
        }
      } else {
        outputParameterResults = null;
      }
    }
  }
  

  public void setAsciiStream(String parameterName, InputStream x, int length)
    throws SQLException
  {
    setAsciiStream(getNamedParamIndex(parameterName, false), x, length);
  }
  

  public void setBigDecimal(String parameterName, BigDecimal x)
    throws SQLException
  {
    setBigDecimal(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setBinaryStream(String parameterName, InputStream x, int length)
    throws SQLException
  {
    setBinaryStream(getNamedParamIndex(parameterName, false), x, length);
  }
  

  public void setBoolean(String parameterName, boolean x)
    throws SQLException
  {
    setBoolean(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setByte(String parameterName, byte x)
    throws SQLException
  {
    setByte(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setBytes(String parameterName, byte[] x)
    throws SQLException
  {
    setBytes(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setCharacterStream(String parameterName, Reader reader, int length)
    throws SQLException
  {
    setCharacterStream(getNamedParamIndex(parameterName, false), reader, length);
  }
  

  public void setDate(String parameterName, Date x)
    throws SQLException
  {
    setDate(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setDate(String parameterName, Date x, Calendar cal)
    throws SQLException
  {
    setDate(getNamedParamIndex(parameterName, false), x, cal);
  }
  

  public void setDouble(String parameterName, double x)
    throws SQLException
  {
    setDouble(getNamedParamIndex(parameterName, false), x);
  }
  



  public void setFloat(String parameterName, float x)
    throws SQLException { setFloat(getNamedParamIndex(parameterName, false), x); }
  
  private void setInOutParamsOnServer() throws SQLException {
    Iterator<CallableStatementParam> paramIter;
    synchronized (checkClosed().getConnectionMutex()) {
      if (paramInfo.numParameters > 0) {
        for (paramIter = paramInfo.iterator(); paramIter.hasNext();)
        {
          CallableStatementParam inParamInfo = (CallableStatementParam)paramIter.next();
          

          if ((isOut) && (isIn)) {
            if ((paramName == null) && (hasParametersView())) {
              paramName = ("nullnp" + index);
            }
            
            String inOutParameterName = mangleParameterName(paramName);
            StringBuilder queryBuf = new StringBuilder(4 + inOutParameterName.length() + 1 + 1);
            queryBuf.append("SET ");
            queryBuf.append(inOutParameterName);
            queryBuf.append("=?");
            
            PreparedStatement setPstmt = null;
            try
            {
              setPstmt = (PreparedStatement)((Wrapper)connection.clientPrepareStatement(queryBuf.toString())).unwrap(PreparedStatement.class);
              
              if (isNull[index] != 0) {
                setPstmt.setBytesNoEscapeNoQuotes(1, "NULL".getBytes());
              }
              else {
                byte[] parameterAsBytes = getBytesRepresentation(index);
                
                if (parameterAsBytes != null) {
                  if ((parameterAsBytes.length > 8) && (parameterAsBytes[0] == 95) && (parameterAsBytes[1] == 98) && (parameterAsBytes[2] == 105) && (parameterAsBytes[3] == 110) && (parameterAsBytes[4] == 97) && (parameterAsBytes[5] == 114) && (parameterAsBytes[6] == 121) && (parameterAsBytes[7] == 39))
                  {

                    setPstmt.setBytesNoEscapeNoQuotes(1, parameterAsBytes);
                  } else {
                    int sqlType = desiredJdbcType;
                    
                    switch (sqlType) {
                    case -7: 
                    case -4: 
                    case -3: 
                    case -2: 
                    case 2000: 
                    case 2004: 
                      setPstmt.setBytes(1, parameterAsBytes);
                      break;
                    
                    default: 
                      setPstmt.setBytesNoEscape(1, parameterAsBytes);
                    }
                  }
                } else {
                  setPstmt.setNull(1, 0);
                }
              }
              
              setPstmt.executeUpdate();
            } finally {
              if (setPstmt != null) {
                setPstmt.close();
              }
            }
          }
        }
      }
    }
  }
  

  public void setInt(String parameterName, int x)
    throws SQLException
  {
    setInt(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setLong(String parameterName, long x)
    throws SQLException
  {
    setLong(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setNull(String parameterName, int sqlType)
    throws SQLException
  {
    setNull(getNamedParamIndex(parameterName, false), sqlType);
  }
  

  public void setNull(String parameterName, int sqlType, String typeName)
    throws SQLException
  {
    setNull(getNamedParamIndex(parameterName, false), sqlType, typeName);
  }
  

  public void setObject(String parameterName, Object x)
    throws SQLException
  {
    setObject(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setObject(String parameterName, Object x, int targetSqlType)
    throws SQLException
  {
    setObject(getNamedParamIndex(parameterName, false), x, targetSqlType);
  }
  


  private void setOutParams()
    throws SQLException
  {
    Iterator<CallableStatementParam> paramIter;
    
    synchronized (checkClosed().getConnectionMutex()) {
      if (paramInfo.numParameters > 0) {
        for (paramIter = paramInfo.iterator(); paramIter.hasNext();) {
          CallableStatementParam outParamInfo = (CallableStatementParam)paramIter.next();
          
          if ((!callingStoredFunction) && (isOut))
          {
            if ((paramName == null) && (hasParametersView())) {
              paramName = ("nullnp" + index);
            }
            
            String outParameterName = mangleParameterName(paramName);
            
            int outParamIndex = 0;
            
            if (placeholderToParameterIndexMap == null) {
              outParamIndex = index + 1;
            }
            else {
              boolean found = false;
              
              for (int i = 0; i < placeholderToParameterIndexMap.length; i++) {
                if (placeholderToParameterIndexMap[i] == index) {
                  outParamIndex = i + 1;
                  found = true;
                  break;
                }
              }
              
              if (!found) {
                throw SQLError.createSQLException(Messages.getString("CallableStatement.21") + paramName + Messages.getString("CallableStatement.22"), "S1009", getExceptionInterceptor());
              }
            }
            


            setBytesNoEscapeNoQuotes(outParamIndex, StringUtils.getBytes(outParameterName, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor()));
          }
        }
      }
    }
  }
  


  public void setShort(String parameterName, short x)
    throws SQLException
  {
    setShort(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setString(String parameterName, String x)
    throws SQLException
  {
    setString(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setTime(String parameterName, Time x)
    throws SQLException
  {
    setTime(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setTime(String parameterName, Time x, Calendar cal)
    throws SQLException
  {
    setTime(getNamedParamIndex(parameterName, false), x, cal);
  }
  

  public void setTimestamp(String parameterName, Timestamp x)
    throws SQLException
  {
    setTimestamp(getNamedParamIndex(parameterName, false), x);
  }
  

  public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
    throws SQLException
  {
    setTimestamp(getNamedParamIndex(parameterName, false), x, cal);
  }
  

  public void setURL(String parameterName, URL val)
    throws SQLException
  {
    setURL(getNamedParamIndex(parameterName, false), val);
  }
  

  public boolean wasNull()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return outputParamWasNull;
    }
  }
  
  public int[] executeBatch() throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeBatch());
  }
  

  protected int getParameterIndexOffset()
  {
    if (callingStoredFunction) {
      return -1;
    }
    
    return super.getParameterIndexOffset();
  }
  
  public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
    setAsciiStream(getNamedParamIndex(parameterName, false), x);
  }
  
  public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException
  {
    setAsciiStream(getNamedParamIndex(parameterName, false), x, length);
  }
  
  public void setBinaryStream(String parameterName, InputStream x) throws SQLException
  {
    setBinaryStream(getNamedParamIndex(parameterName, false), x);
  }
  
  public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException
  {
    setBinaryStream(getNamedParamIndex(parameterName, false), x, length);
  }
  
  public void setBlob(String parameterName, Blob x) throws SQLException
  {
    setBlob(getNamedParamIndex(parameterName, false), x);
  }
  
  public void setBlob(String parameterName, InputStream inputStream) throws SQLException
  {
    setBlob(getNamedParamIndex(parameterName, false), inputStream);
  }
  
  public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException
  {
    setBlob(getNamedParamIndex(parameterName, false), inputStream, length);
  }
  
  public void setCharacterStream(String parameterName, Reader reader) throws SQLException
  {
    setCharacterStream(getNamedParamIndex(parameterName, false), reader);
  }
  
  public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException
  {
    setCharacterStream(getNamedParamIndex(parameterName, false), reader, length);
  }
  
  public void setClob(String parameterName, Clob x) throws SQLException
  {
    setClob(getNamedParamIndex(parameterName, false), x);
  }
  
  public void setClob(String parameterName, Reader reader) throws SQLException
  {
    setClob(getNamedParamIndex(parameterName, false), reader);
  }
  
  public void setClob(String parameterName, Reader reader, long length) throws SQLException
  {
    setClob(getNamedParamIndex(parameterName, false), reader, length);
  }
  
  public void setNCharacterStream(String parameterName, Reader value) throws SQLException
  {
    setNCharacterStream(getNamedParamIndex(parameterName, false), value);
  }
  
  public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException
  {
    setNCharacterStream(getNamedParamIndex(parameterName, false), value, length);
  }
  





  private boolean checkReadOnlyProcedure()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (connection.getNoAccessToProcedureBodies()) {
        return false;
      }
      
      if (paramInfo.isReadOnlySafeChecked) {
        return paramInfo.isReadOnlySafeProcedure;
      }
      
      ResultSet rs = null;
      java.sql.PreparedStatement ps = null;
      try
      {
        String procName = extractProcedureName();
        
        String catalog = currentCatalog;
        
        if (procName.indexOf(".") != -1) {
          catalog = procName.substring(0, procName.indexOf("."));
          
          if ((StringUtils.startsWithIgnoreCaseAndWs(catalog, "`")) && (catalog.trim().endsWith("`"))) {
            catalog = catalog.substring(1, catalog.length() - 1);
          }
          
          procName = procName.substring(procName.indexOf(".") + 1);
          procName = StringUtils.toString(StringUtils.stripEnclosure(StringUtils.getBytes(procName), "`", "`"));
        }
        ps = connection.prepareStatement("SELECT SQL_DATA_ACCESS FROM information_schema.routines WHERE routine_schema = ? AND routine_name = ?");
        ps.setMaxRows(0);
        ps.setFetchSize(0);
        
        ps.setString(1, catalog);
        ps.setString(2, procName);
        rs = ps.executeQuery();
        if (rs.next()) {
          String sqlDataAccess = rs.getString(1);
          if (("READS SQL DATA".equalsIgnoreCase(sqlDataAccess)) || ("NO SQL".equalsIgnoreCase(sqlDataAccess))) {
            synchronized (paramInfo) {
              paramInfo.isReadOnlySafeChecked = true;
              paramInfo.isReadOnlySafeProcedure = true;
            }
            ??? = 1;jsr 30;return ???;
          }
        }
      }
      catch (SQLException e) {}finally
      {
        jsr 6; } localObject3 = returnAddress; if (rs != null) {
        rs.close();
      }
      if (ps != null)
        ps.close(); ret;
      


      paramInfo.isReadOnlySafeChecked = false;
      paramInfo.isReadOnlySafeProcedure = false;
    }
    return false;
  }
  
  protected boolean checkReadOnlySafeStatement()
    throws SQLException
  {
    return (super.checkReadOnlySafeStatement()) || (checkReadOnlyProcedure());
  }
  
  private boolean hasParametersView() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        if (connection.versionMeetsMinimum(5, 5, 0)) {
          java.sql.DatabaseMetaData dbmd1 = new DatabaseMetaDataUsingInfoSchema(connection, connection.getCatalog());
          return ((DatabaseMetaDataUsingInfoSchema)dbmd1).gethasParametersView();
        }
        
        return false;
      } catch (SQLException e) {
        return false;
      }
    }
  }
  


  public long executeLargeUpdate()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      long returnVal = -1L;
      
      checkStreamability();
      
      if (callingStoredFunction) {
        execute();
        
        return -1L;
      }
      
      setInOutParamsOnServer();
      setOutParams();
      
      returnVal = super.executeLargeUpdate();
      
      retrieveOutParams();
      
      return returnVal;
    }
  }
  
  public long[] executeLargeBatch() throws SQLException
  {
    if (hasOutputParams) {
      throw SQLError.createSQLException("Can't call executeBatch() on CallableStatement with OUTPUT parameters", "S1009", getExceptionInterceptor());
    }
    

    return super.executeLargeBatch();
  }
  
  public void setObject(String parameterName, Object x, int targetSqlType, int scale)
    throws SQLException
  {}
}
