package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.StandardLogger;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;































public class ConnectionPropertiesImpl
  implements Serializable, ConnectionProperties
{
  private static final long serialVersionUID = 4257801713007640580L;
  
  static class BooleanConnectionProperty
    extends ConnectionPropertiesImpl.ConnectionProperty
    implements Serializable
  {
    private static final long serialVersionUID = 2540132501709159404L;
    
    BooleanConnectionProperty(String propertyNameToSet, boolean defaultValueToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(Boolean.valueOf(defaultValueToSet), null, 0, 0, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    



    String[] getAllowableValues()
    {
      return new String[] { "true", "false", "yes", "no" };
    }
    
    boolean getValueAsBoolean() {
      return ((Boolean)valueAsObject).booleanValue();
    }
    



    boolean hasValueConstraints()
    {
      return true;
    }
    


    void initializeFrom(String extractedValue, ExceptionInterceptor exceptionInterceptor)
      throws SQLException
    {
      if (extractedValue != null) {
        validateStringValues(extractedValue, exceptionInterceptor);
        
        valueAsObject = Boolean.valueOf((extractedValue.equalsIgnoreCase("TRUE")) || (extractedValue.equalsIgnoreCase("YES")));
        wasExplicitlySet = true;
      } else {
        valueAsObject = defaultValue;
      }
      updateCount += 1;
    }
    



    boolean isRangeBased()
    {
      return false;
    }
    
    void setValue(boolean valueFlag) {
      valueAsObject = Boolean.valueOf(valueFlag);
      wasExplicitlySet = true;
      updateCount += 1;
    }
  }
  

  static abstract class ConnectionProperty
    implements Serializable
  {
    static final long serialVersionUID = -6644853639584478367L;
    
    String[] allowableValues;
    
    String categoryName;
    
    Object defaultValue;
    
    int lowerBound;
    
    int order;
    
    String propertyName;
    
    String sinceVersion;
    
    int upperBound;
    
    Object valueAsObject;
    
    boolean required;
    String description;
    int updateCount = 0;
    
    boolean wasExplicitlySet = false;
    

    public ConnectionProperty() {}
    

    ConnectionProperty(String propertyNameToSet, Object defaultValueToSet, String[] allowableValuesToSet, int lowerBoundToSet, int upperBoundToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      description = descriptionToSet;
      propertyName = propertyNameToSet;
      defaultValue = defaultValueToSet;
      valueAsObject = defaultValueToSet;
      allowableValues = allowableValuesToSet;
      lowerBound = lowerBoundToSet;
      upperBound = upperBoundToSet;
      required = false;
      sinceVersion = sinceVersionToSet;
      categoryName = category;
      order = orderInCategory;
    }
    
    String[] getAllowableValues() {
      return allowableValues;
    }
    


    String getCategoryName()
    {
      return categoryName;
    }
    
    Object getDefaultValue() {
      return defaultValue;
    }
    
    int getLowerBound() {
      return lowerBound;
    }
    


    int getOrder()
    {
      return order;
    }
    
    String getPropertyName() {
      return propertyName;
    }
    
    int getUpperBound() {
      return upperBound;
    }
    
    Object getValueAsObject() {
      return valueAsObject;
    }
    
    int getUpdateCount() {
      return updateCount;
    }
    
    boolean isExplicitlySet() {
      return wasExplicitlySet;
    }
    
    abstract boolean hasValueConstraints();
    
    void initializeFrom(Properties extractFrom, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      String extractedValue = extractFrom.getProperty(getPropertyName());
      extractFrom.remove(getPropertyName());
      initializeFrom(extractedValue, exceptionInterceptor);
    }
    
    void initializeFrom(Reference ref, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      RefAddr refAddr = ref.get(getPropertyName());
      
      if (refAddr != null) {
        String refContentAsString = (String)refAddr.getContent();
        
        initializeFrom(refContentAsString, exceptionInterceptor);
      }
    }
    

    abstract void initializeFrom(String paramString, ExceptionInterceptor paramExceptionInterceptor)
      throws SQLException;
    

    abstract boolean isRangeBased();
    
    void setCategoryName(String categoryName)
    {
      this.categoryName = categoryName;
    }
    



    void setOrder(int order)
    {
      this.order = order;
    }
    
    void setValueAsObject(Object obj) {
      valueAsObject = obj;
      updateCount += 1;
    }
    
    void storeTo(Reference ref) {
      if (getValueAsObject() != null) {
        ref.add(new StringRefAddr(getPropertyName(), getValueAsObject().toString()));
      }
    }
    
    DriverPropertyInfo getAsDriverPropertyInfo() {
      DriverPropertyInfo dpi = new DriverPropertyInfo(propertyName, null);
      choices = getAllowableValues();
      value = (valueAsObject != null ? valueAsObject.toString() : null);
      required = required;
      description = description;
      
      return dpi;
    }
    
    void validateStringValues(String valueToValidate, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      String[] validateAgainst = getAllowableValues();
      
      if (valueToValidate == null) {
        return;
      }
      
      if ((validateAgainst == null) || (validateAgainst.length == 0)) {
        return;
      }
      
      for (int i = 0; i < validateAgainst.length; i++) {
        if ((validateAgainst[i] != null) && (validateAgainst[i].equalsIgnoreCase(valueToValidate))) {
          return;
        }
      }
      
      StringBuilder errorMessageBuf = new StringBuilder();
      
      errorMessageBuf.append("The connection property '");
      errorMessageBuf.append(getPropertyName());
      errorMessageBuf.append("' only accepts values of the form: ");
      
      if (validateAgainst.length != 0) {
        errorMessageBuf.append("'");
        errorMessageBuf.append(validateAgainst[0]);
        errorMessageBuf.append("'");
        
        for (int i = 1; i < validateAgainst.length - 1; i++) {
          errorMessageBuf.append(", ");
          errorMessageBuf.append("'");
          errorMessageBuf.append(validateAgainst[i]);
          errorMessageBuf.append("'");
        }
        
        errorMessageBuf.append(" or '");
        errorMessageBuf.append(validateAgainst[(validateAgainst.length - 1)]);
        errorMessageBuf.append("'");
      }
      
      errorMessageBuf.append(". The value '");
      errorMessageBuf.append(valueToValidate);
      errorMessageBuf.append("' is not in this set.");
      
      throw SQLError.createSQLException(errorMessageBuf.toString(), "S1009", exceptionInterceptor);
    }
  }
  
  static class IntegerConnectionProperty
    extends ConnectionPropertiesImpl.ConnectionProperty implements Serializable
  {
    private static final long serialVersionUID = -3004305481796850832L;
    int multiplier = 1;
    
    public IntegerConnectionProperty(String propertyNameToSet, Object defaultValueToSet, String[] allowableValuesToSet, int lowerBoundToSet, int upperBoundToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(defaultValueToSet, allowableValuesToSet, lowerBoundToSet, upperBoundToSet, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    

    IntegerConnectionProperty(String propertyNameToSet, int defaultValueToSet, int lowerBoundToSet, int upperBoundToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(Integer.valueOf(defaultValueToSet), null, lowerBoundToSet, upperBoundToSet, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    








    IntegerConnectionProperty(String propertyNameToSet, int defaultValueToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      this(propertyNameToSet, defaultValueToSet, 0, 0, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    



    String[] getAllowableValues()
    {
      return null;
    }
    



    int getLowerBound()
    {
      return lowerBound;
    }
    



    int getUpperBound()
    {
      return upperBound;
    }
    
    int getValueAsInt() {
      return ((Integer)valueAsObject).intValue();
    }
    



    boolean hasValueConstraints()
    {
      return false;
    }
    


    void initializeFrom(String extractedValue, ExceptionInterceptor exceptionInterceptor)
      throws SQLException
    {
      if (extractedValue != null) {
        try
        {
          int intValue = (int)(Double.valueOf(extractedValue).doubleValue() * multiplier);
          
          setValue(intValue, extractedValue, exceptionInterceptor);
        } catch (NumberFormatException nfe) {
          throw SQLError.createSQLException("The connection property '" + getPropertyName() + "' only accepts integer values. The value '" + extractedValue + "' can not be converted to an integer.", "S1009", exceptionInterceptor);
        }
        
      } else {
        valueAsObject = defaultValue;
      }
      updateCount += 1;
    }
    



    boolean isRangeBased()
    {
      return getUpperBound() != getLowerBound();
    }
    
    void setValue(int intValue, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      setValue(intValue, null, exceptionInterceptor);
    }
    
    void setValue(int intValue, String valueAsString, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      if ((isRangeBased()) && (
        (intValue < getLowerBound()) || (intValue > getUpperBound()))) {
        throw SQLError.createSQLException("The connection property '" + getPropertyName() + "' only accepts integer values in the range of " + getLowerBound() + " - " + getUpperBound() + ", the value '" + (valueAsString == null ? Integer.valueOf(intValue) : valueAsString) + "' exceeds this range.", "S1009", exceptionInterceptor);
      }
      




      valueAsObject = Integer.valueOf(intValue);
      wasExplicitlySet = true;
      updateCount += 1;
    }
  }
  
  public static class LongConnectionProperty extends ConnectionPropertiesImpl.IntegerConnectionProperty
  {
    private static final long serialVersionUID = 6068572984340480895L;
    
    LongConnectionProperty(String propertyNameToSet, long defaultValueToSet, long lowerBoundToSet, long upperBoundToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(Long.valueOf(defaultValueToSet), null, (int)lowerBoundToSet, (int)upperBoundToSet, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    

    LongConnectionProperty(String propertyNameToSet, long defaultValueToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      this(propertyNameToSet, defaultValueToSet, 0L, 0L, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    
    void setValue(long longValue, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      setValue(longValue, null, exceptionInterceptor);
    }
    
    void setValue(long longValue, String valueAsString, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      if ((isRangeBased()) && (
        (longValue < getLowerBound()) || (longValue > getUpperBound()))) {
        throw SQLError.createSQLException("The connection property '" + getPropertyName() + "' only accepts long integer values in the range of " + getLowerBound() + " - " + getUpperBound() + ", the value '" + (valueAsString == null ? Long.valueOf(longValue) : valueAsString) + "' exceeds this range.", "S1009", exceptionInterceptor);
      }
      



      valueAsObject = Long.valueOf(longValue);
      wasExplicitlySet = true;
      updateCount += 1;
    }
    
    long getValueAsLong() {
      return ((Long)valueAsObject).longValue();
    }
    
    void initializeFrom(String extractedValue, ExceptionInterceptor exceptionInterceptor) throws SQLException
    {
      if (extractedValue != null) {
        try
        {
          long longValue = Double.valueOf(extractedValue).longValue();
          
          setValue(longValue, extractedValue, exceptionInterceptor);
        } catch (NumberFormatException nfe) {
          throw SQLError.createSQLException("The connection property '" + getPropertyName() + "' only accepts long integer values. The value '" + extractedValue + "' can not be converted to a long integer.", "S1009", exceptionInterceptor);
        }
        
      } else {
        valueAsObject = defaultValue;
      }
      updateCount += 1;
    }
  }
  
  static class MemorySizeConnectionProperty
    extends ConnectionPropertiesImpl.IntegerConnectionProperty implements Serializable
  {
    private static final long serialVersionUID = 7351065128998572656L;
    private String valueAsString;
    
    MemorySizeConnectionProperty(String propertyNameToSet, int defaultValueToSet, int lowerBoundToSet, int upperBoundToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(defaultValueToSet, lowerBoundToSet, upperBoundToSet, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    
    void initializeFrom(String extractedValue, ExceptionInterceptor exceptionInterceptor) throws SQLException
    {
      valueAsString = extractedValue;
      multiplier = 1;
      
      if (extractedValue != null) {
        if ((extractedValue.endsWith("k")) || (extractedValue.endsWith("K")) || (extractedValue.endsWith("kb")) || (extractedValue.endsWith("Kb")) || (extractedValue.endsWith("kB")) || (extractedValue.endsWith("KB")))
        {
          multiplier = 1024;
          int indexOfK = StringUtils.indexOfIgnoreCase(extractedValue, "k");
          extractedValue = extractedValue.substring(0, indexOfK);
        } else if ((extractedValue.endsWith("m")) || (extractedValue.endsWith("M")) || (extractedValue.endsWith("mb")) || (extractedValue.endsWith("Mb")) || (extractedValue.endsWith("mB")) || (extractedValue.endsWith("MB")))
        {
          multiplier = 1048576;
          int indexOfM = StringUtils.indexOfIgnoreCase(extractedValue, "m");
          extractedValue = extractedValue.substring(0, indexOfM);
        } else if ((extractedValue.endsWith("g")) || (extractedValue.endsWith("G")) || (extractedValue.endsWith("gb")) || (extractedValue.endsWith("Gb")) || (extractedValue.endsWith("gB")) || (extractedValue.endsWith("GB")))
        {
          multiplier = 1073741824;
          int indexOfG = StringUtils.indexOfIgnoreCase(extractedValue, "g");
          extractedValue = extractedValue.substring(0, indexOfG);
        }
      }
      
      super.initializeFrom(extractedValue, exceptionInterceptor);
    }
    
    void setValue(String value, ExceptionInterceptor exceptionInterceptor) throws SQLException {
      initializeFrom(value, exceptionInterceptor);
    }
    
    String getValueAsString() {
      return valueAsString;
    }
  }
  
  static class StringConnectionProperty extends ConnectionPropertiesImpl.ConnectionProperty implements Serializable
  {
    private static final long serialVersionUID = 5432127962785948272L;
    
    StringConnectionProperty(String propertyNameToSet, String defaultValueToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      this(propertyNameToSet, defaultValueToSet, null, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    







    StringConnectionProperty(String propertyNameToSet, String defaultValueToSet, String[] allowableValuesToSet, String descriptionToSet, String sinceVersionToSet, String category, int orderInCategory)
    {
      super(defaultValueToSet, allowableValuesToSet, 0, 0, descriptionToSet, sinceVersionToSet, category, orderInCategory);
    }
    
    String getValueAsString() {
      return (String)valueAsObject;
    }
    



    boolean hasValueConstraints()
    {
      return (allowableValues != null) && (allowableValues.length > 0);
    }
    


    void initializeFrom(String extractedValue, ExceptionInterceptor exceptionInterceptor)
      throws SQLException
    {
      if (extractedValue != null) {
        validateStringValues(extractedValue, exceptionInterceptor);
        
        valueAsObject = extractedValue;
        wasExplicitlySet = true;
      } else {
        valueAsObject = defaultValue;
      }
      updateCount += 1;
    }
    



    boolean isRangeBased()
    {
      return false;
    }
    
    void setValue(String valueFlag) {
      valueAsObject = valueFlag;
      wasExplicitlySet = true;
      updateCount += 1;
    }
  }
  
  private static final String CONNECTION_AND_AUTH_CATEGORY = Messages.getString("ConnectionProperties.categoryConnectionAuthentication");
  
  private static final String NETWORK_CATEGORY = Messages.getString("ConnectionProperties.categoryNetworking");
  
  private static final String DEBUGING_PROFILING_CATEGORY = Messages.getString("ConnectionProperties.categoryDebuggingProfiling");
  
  private static final String HA_CATEGORY = Messages.getString("ConnectionProperties.categorryHA");
  
  private static final String MISC_CATEGORY = Messages.getString("ConnectionProperties.categoryMisc");
  
  private static final String PERFORMANCE_CATEGORY = Messages.getString("ConnectionProperties.categoryPerformance");
  
  private static final String SECURITY_CATEGORY = Messages.getString("ConnectionProperties.categorySecurity");
  
  private static final String[] PROPERTY_CATEGORIES = { CONNECTION_AND_AUTH_CATEGORY, NETWORK_CATEGORY, HA_CATEGORY, SECURITY_CATEGORY, PERFORMANCE_CATEGORY, DEBUGING_PROFILING_CATEGORY, MISC_CATEGORY };
  

  private static final ArrayList<Field> PROPERTY_LIST = new ArrayList();
  



  private static final String STANDARD_LOGGER_NAME = StandardLogger.class.getName();
  protected static final String ZERO_DATETIME_BEHAVIOR_CONVERT_TO_NULL = "convertToNull";
  protected static final String ZERO_DATETIME_BEHAVIOR_EXCEPTION = "exception";
  protected static final String ZERO_DATETIME_BEHAVIOR_ROUND = "round";
  private BooleanConnectionProperty allowLoadLocalInfile;
  private BooleanConnectionProperty allowMultiQueries;
  private BooleanConnectionProperty allowNanAndInf;
  private BooleanConnectionProperty allowUrlInLocalInfile;
  private BooleanConnectionProperty alwaysSendSetIsolation;
  private BooleanConnectionProperty autoClosePStmtStreams;
  private StringConnectionProperty replicationConnectionGroup;
  private BooleanConnectionProperty allowMasterDownConnections;
  private BooleanConnectionProperty allowSlaveDownConnections;
  private BooleanConnectionProperty readFromMasterWhenNoSlaves;
  private BooleanConnectionProperty autoDeserialize;
  private BooleanConnectionProperty autoGenerateTestcaseScript;
  private boolean autoGenerateTestcaseScriptAsBoolean;
  private BooleanConnectionProperty autoReconnect;
  private BooleanConnectionProperty autoReconnectForPools;
  private boolean autoReconnectForPoolsAsBoolean;
  private MemorySizeConnectionProperty blobSendChunkSize;
  
  static
  {
    try
    {
      Field[] declaredFields = ConnectionPropertiesImpl.class.getDeclaredFields();
      
      for (int i = 0; i < declaredFields.length; i++) {
        if (ConnectionProperty.class.isAssignableFrom(declaredFields[i].getType())) {
          PROPERTY_LIST.add(declaredFields[i]);
        }
      }
    }
    catch (Exception ex)
    {
      RuntimeException rtEx = new RuntimeException();
      rtEx.initCause(ex);
      
      throw rtEx;
    }
  }
  
  private BooleanConnectionProperty autoSlowLog;
  private BooleanConnectionProperty blobsAreStrings;
  private BooleanConnectionProperty functionsNeverReturnBlobs;
  private BooleanConnectionProperty cacheCallableStatements;
  private BooleanConnectionProperty cachePreparedStatements;
  private BooleanConnectionProperty cacheResultSetMetadata;
  private boolean cacheResultSetMetaDataAsBoolean;
  
  public ExceptionInterceptor getExceptionInterceptor()
  {
    return null;
  }
  
  private StringConnectionProperty serverConfigCacheFactory;
  private BooleanConnectionProperty cacheServerConfiguration;
  private IntegerConnectionProperty callableStatementCacheSize;
  private BooleanConnectionProperty capitalizeTypeNames;
  private StringConnectionProperty characterEncoding;
  private String characterEncodingAsString;
  protected boolean characterEncodingIsAliasForSjis;
  private StringConnectionProperty characterSetResults;
  private StringConnectionProperty connectionAttributes;
  private StringConnectionProperty clientInfoProvider;
  private BooleanConnectionProperty clobberStreamingResults;
  private StringConnectionProperty clobCharacterEncoding;
  private BooleanConnectionProperty compensateOnDuplicateKeyUpdateCounts;
  private StringConnectionProperty connectionCollation;
  private StringConnectionProperty connectionLifecycleInterceptors;
  private IntegerConnectionProperty connectTimeout;
  private BooleanConnectionProperty continueBatchOnError;
  private BooleanConnectionProperty createDatabaseIfNotExist;
  private IntegerConnectionProperty defaultFetchSize;
  
  protected static DriverPropertyInfo[] exposeAsDriverPropertyInfo(Properties info, int slotsToReserve)
    throws SQLException
  {
    new ConnectionPropertiesImpl() { private static final long serialVersionUID = 4257801713007640581L; }.exposeAsDriverPropertyInfoInternal(info, slotsToReserve);
  }
  
  public ConnectionPropertiesImpl()
  {
    allowLoadLocalInfile = new BooleanConnectionProperty("allowLoadLocalInfile", true, Messages.getString("ConnectionProperties.loadDataLocal"), "3.0.3", SECURITY_CATEGORY, Integer.MAX_VALUE);
    

    allowMultiQueries = new BooleanConnectionProperty("allowMultiQueries", false, Messages.getString("ConnectionProperties.allowMultiQueries"), "3.1.1", SECURITY_CATEGORY, 1);
    

    allowNanAndInf = new BooleanConnectionProperty("allowNanAndInf", false, Messages.getString("ConnectionProperties.allowNANandINF"), "3.1.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    allowUrlInLocalInfile = new BooleanConnectionProperty("allowUrlInLocalInfile", false, Messages.getString("ConnectionProperties.allowUrlInLoadLocal"), "3.1.4", SECURITY_CATEGORY, Integer.MAX_VALUE);
    

    alwaysSendSetIsolation = new BooleanConnectionProperty("alwaysSendSetIsolation", true, Messages.getString("ConnectionProperties.alwaysSendSetIsolation"), "3.1.7", PERFORMANCE_CATEGORY, Integer.MAX_VALUE);
    

    autoClosePStmtStreams = new BooleanConnectionProperty("autoClosePStmtStreams", false, Messages.getString("ConnectionProperties.autoClosePstmtStreams"), "3.1.12", MISC_CATEGORY, Integer.MIN_VALUE);
    

    replicationConnectionGroup = new StringConnectionProperty("replicationConnectionGroup", null, Messages.getString("ConnectionProperties.replicationConnectionGroup"), "5.1.27", HA_CATEGORY, Integer.MIN_VALUE);
    

    allowMasterDownConnections = new BooleanConnectionProperty("allowMasterDownConnections", false, Messages.getString("ConnectionProperties.allowMasterDownConnections"), "5.1.27", HA_CATEGORY, Integer.MAX_VALUE);
    

    allowSlaveDownConnections = new BooleanConnectionProperty("allowSlaveDownConnections", false, Messages.getString("ConnectionProperties.allowSlaveDownConnections"), "5.1.38", HA_CATEGORY, Integer.MAX_VALUE);
    

    readFromMasterWhenNoSlaves = new BooleanConnectionProperty("readFromMasterWhenNoSlaves", false, Messages.getString("ConnectionProperties.readFromMasterWhenNoSlaves"), "5.1.38", HA_CATEGORY, Integer.MAX_VALUE);
    

    autoDeserialize = new BooleanConnectionProperty("autoDeserialize", false, Messages.getString("ConnectionProperties.autoDeserialize"), "3.1.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    autoGenerateTestcaseScript = new BooleanConnectionProperty("autoGenerateTestcaseScript", false, Messages.getString("ConnectionProperties.autoGenerateTestcaseScript"), "3.1.9", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    autoGenerateTestcaseScriptAsBoolean = false;
    
    autoReconnect = new BooleanConnectionProperty("autoReconnect", false, Messages.getString("ConnectionProperties.autoReconnect"), "1.1", HA_CATEGORY, 0);
    

    autoReconnectForPools = new BooleanConnectionProperty("autoReconnectForPools", false, Messages.getString("ConnectionProperties.autoReconnectForPools"), "3.1.3", HA_CATEGORY, 1);
    

    autoReconnectForPoolsAsBoolean = false;
    
    blobSendChunkSize = new MemorySizeConnectionProperty("blobSendChunkSize", 1048576, 0, 0, Messages.getString("ConnectionProperties.blobSendChunkSize"), "3.1.9", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    autoSlowLog = new BooleanConnectionProperty("autoSlowLog", true, Messages.getString("ConnectionProperties.autoSlowLog"), "5.1.4", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    blobsAreStrings = new BooleanConnectionProperty("blobsAreStrings", false, "Should the driver always treat BLOBs as Strings - specifically to work around dubious metadata returned by the server for GROUP BY clauses?", "5.0.8", MISC_CATEGORY, Integer.MIN_VALUE);
    


    functionsNeverReturnBlobs = new BooleanConnectionProperty("functionsNeverReturnBlobs", false, "Should the driver always treat data from functions returning BLOBs as Strings - specifically to work around dubious metadata returned by the server for GROUP BY clauses?", "5.0.8", MISC_CATEGORY, Integer.MIN_VALUE);
    



    cacheCallableStatements = new BooleanConnectionProperty("cacheCallableStmts", false, Messages.getString("ConnectionProperties.cacheCallableStatements"), "3.1.2", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    cachePreparedStatements = new BooleanConnectionProperty("cachePrepStmts", false, Messages.getString("ConnectionProperties.cachePrepStmts"), "3.0.10", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    cacheResultSetMetadata = new BooleanConnectionProperty("cacheResultSetMetadata", false, Messages.getString("ConnectionProperties.cacheRSMetadata"), "3.1.1", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    



    serverConfigCacheFactory = new StringConnectionProperty("serverConfigCacheFactory", PerVmServerConfigCacheFactory.class.getName(), Messages.getString("ConnectionProperties.serverConfigCacheFactory"), "5.1.1", PERFORMANCE_CATEGORY, 12);
    


    cacheServerConfiguration = new BooleanConnectionProperty("cacheServerConfiguration", false, Messages.getString("ConnectionProperties.cacheServerConfiguration"), "3.1.5", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    callableStatementCacheSize = new IntegerConnectionProperty("callableStmtCacheSize", 100, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.callableStmtCacheSize"), "3.1.2", PERFORMANCE_CATEGORY, 5);
    

    capitalizeTypeNames = new BooleanConnectionProperty("capitalizeTypeNames", true, Messages.getString("ConnectionProperties.capitalizeTypeNames"), "2.0.7", MISC_CATEGORY, Integer.MIN_VALUE);
    

    characterEncoding = new StringConnectionProperty("characterEncoding", null, Messages.getString("ConnectionProperties.characterEncoding"), "1.1g", MISC_CATEGORY, 5);
    

    characterEncodingAsString = null;
    
    characterEncodingIsAliasForSjis = false;
    
    characterSetResults = new StringConnectionProperty("characterSetResults", null, Messages.getString("ConnectionProperties.characterSetResults"), "3.0.13", MISC_CATEGORY, 6);
    

    connectionAttributes = new StringConnectionProperty("connectionAttributes", null, Messages.getString("ConnectionProperties.connectionAttributes"), "5.1.25", MISC_CATEGORY, 7);
    

    clientInfoProvider = new StringConnectionProperty("clientInfoProvider", "com.mysql.jdbc.JDBC4CommentClientInfoProvider", Messages.getString("ConnectionProperties.clientInfoProvider"), "5.1.0", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    clobberStreamingResults = new BooleanConnectionProperty("clobberStreamingResults", false, Messages.getString("ConnectionProperties.clobberStreamingResults"), "3.0.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    clobCharacterEncoding = new StringConnectionProperty("clobCharacterEncoding", null, Messages.getString("ConnectionProperties.clobCharacterEncoding"), "5.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    compensateOnDuplicateKeyUpdateCounts = new BooleanConnectionProperty("compensateOnDuplicateKeyUpdateCounts", false, Messages.getString("ConnectionProperties.compensateOnDuplicateKeyUpdateCounts"), "5.1.7", MISC_CATEGORY, Integer.MIN_VALUE);
    
    connectionCollation = new StringConnectionProperty("connectionCollation", null, Messages.getString("ConnectionProperties.connectionCollation"), "3.0.13", MISC_CATEGORY, 7);
    

    connectionLifecycleInterceptors = new StringConnectionProperty("connectionLifecycleInterceptors", null, Messages.getString("ConnectionProperties.connectionLifecycleInterceptors"), "5.1.4", CONNECTION_AND_AUTH_CATEGORY, Integer.MAX_VALUE);
    

    connectTimeout = new IntegerConnectionProperty("connectTimeout", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.connectTimeout"), "3.0.1", CONNECTION_AND_AUTH_CATEGORY, 9);
    

    continueBatchOnError = new BooleanConnectionProperty("continueBatchOnError", true, Messages.getString("ConnectionProperties.continueBatchOnError"), "3.0.3", MISC_CATEGORY, Integer.MIN_VALUE);
    

    createDatabaseIfNotExist = new BooleanConnectionProperty("createDatabaseIfNotExist", false, Messages.getString("ConnectionProperties.createDatabaseIfNotExist"), "3.1.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    defaultFetchSize = new IntegerConnectionProperty("defaultFetchSize", 0, Messages.getString("ConnectionProperties.defaultFetchSize"), "3.1.9", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    



    detectServerPreparedStmts = new BooleanConnectionProperty("useServerPrepStmts", false, Messages.getString("ConnectionProperties.useServerPrepStmts"), "3.1.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    dontTrackOpenResources = new BooleanConnectionProperty("dontTrackOpenResources", false, Messages.getString("ConnectionProperties.dontTrackOpenResources"), "3.1.7", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    dumpQueriesOnException = new BooleanConnectionProperty("dumpQueriesOnException", false, Messages.getString("ConnectionProperties.dumpQueriesOnException"), "3.1.3", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    dynamicCalendars = new BooleanConnectionProperty("dynamicCalendars", false, Messages.getString("ConnectionProperties.dynamicCalendars"), "3.1.5", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    elideSetAutoCommits = new BooleanConnectionProperty("elideSetAutoCommits", false, Messages.getString("ConnectionProperties.eliseSetAutoCommit"), "3.1.3", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    emptyStringsConvertToZero = new BooleanConnectionProperty("emptyStringsConvertToZero", true, Messages.getString("ConnectionProperties.emptyStringsConvertToZero"), "3.1.8", MISC_CATEGORY, Integer.MIN_VALUE);
    

    emulateLocators = new BooleanConnectionProperty("emulateLocators", false, Messages.getString("ConnectionProperties.emulateLocators"), "3.1.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    emulateUnsupportedPstmts = new BooleanConnectionProperty("emulateUnsupportedPstmts", true, Messages.getString("ConnectionProperties.emulateUnsupportedPstmts"), "3.1.7", MISC_CATEGORY, Integer.MIN_VALUE);
    

    enablePacketDebug = new BooleanConnectionProperty("enablePacketDebug", false, Messages.getString("ConnectionProperties.enablePacketDebug"), "3.1.3", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    enableQueryTimeouts = new BooleanConnectionProperty("enableQueryTimeouts", true, Messages.getString("ConnectionProperties.enableQueryTimeouts"), "5.0.6", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    explainSlowQueries = new BooleanConnectionProperty("explainSlowQueries", false, Messages.getString("ConnectionProperties.explainSlowQueries"), "3.1.2", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    exceptionInterceptors = new StringConnectionProperty("exceptionInterceptors", null, Messages.getString("ConnectionProperties.exceptionInterceptors"), "5.1.8", MISC_CATEGORY, Integer.MIN_VALUE);
    


    failOverReadOnly = new BooleanConnectionProperty("failOverReadOnly", true, Messages.getString("ConnectionProperties.failoverReadOnly"), "3.0.12", HA_CATEGORY, 2);
    

    gatherPerformanceMetrics = new BooleanConnectionProperty("gatherPerfMetrics", false, Messages.getString("ConnectionProperties.gatherPerfMetrics"), "3.1.2", DEBUGING_PROFILING_CATEGORY, 1);
    

    generateSimpleParameterMetadata = new BooleanConnectionProperty("generateSimpleParameterMetadata", false, Messages.getString("ConnectionProperties.generateSimpleParameterMetadata"), "5.0.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    highAvailabilityAsBoolean = false;
    
    holdResultsOpenOverStatementClose = new BooleanConnectionProperty("holdResultsOpenOverStatementClose", false, Messages.getString("ConnectionProperties.holdRSOpenOverStmtClose"), "3.1.7", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    includeInnodbStatusInDeadlockExceptions = new BooleanConnectionProperty("includeInnodbStatusInDeadlockExceptions", false, Messages.getString("ConnectionProperties.includeInnodbStatusInDeadlockExceptions"), "5.0.7", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    includeThreadDumpInDeadlockExceptions = new BooleanConnectionProperty("includeThreadDumpInDeadlockExceptions", false, Messages.getString("ConnectionProperties.includeThreadDumpInDeadlockExceptions"), "5.1.15", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    includeThreadNamesAsStatementComment = new BooleanConnectionProperty("includeThreadNamesAsStatementComment", false, Messages.getString("ConnectionProperties.includeThreadNamesAsStatementComment"), "5.1.15", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    ignoreNonTxTables = new BooleanConnectionProperty("ignoreNonTxTables", false, Messages.getString("ConnectionProperties.ignoreNonTxTables"), "3.0.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    initialTimeout = new IntegerConnectionProperty("initialTimeout", 2, 1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.initialTimeout"), "1.1", HA_CATEGORY, 5);
    

    isInteractiveClient = new BooleanConnectionProperty("interactiveClient", false, Messages.getString("ConnectionProperties.interactiveClient"), "3.1.0", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    jdbcCompliantTruncation = new BooleanConnectionProperty("jdbcCompliantTruncation", true, Messages.getString("ConnectionProperties.jdbcCompliantTruncation"), "3.1.2", MISC_CATEGORY, Integer.MIN_VALUE);
    

    jdbcCompliantTruncationForReads = jdbcCompliantTruncation.getValueAsBoolean();
    
    largeRowSizeThreshold = new MemorySizeConnectionProperty("largeRowSizeThreshold", 2048, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.largeRowSizeThreshold"), "5.1.1", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceStrategy = new StringConnectionProperty("loadBalanceStrategy", "random", null, Messages.getString("ConnectionProperties.loadBalanceStrategy"), "5.0.6", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    serverAffinityOrder = new StringConnectionProperty("serverAffinityOrder", "", null, Messages.getString("ConnectionProperties.serverAffinityOrder"), "5.1.43", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceBlacklistTimeout = new IntegerConnectionProperty("loadBalanceBlacklistTimeout", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.loadBalanceBlacklistTimeout"), "5.1.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalancePingTimeout = new IntegerConnectionProperty("loadBalancePingTimeout", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.loadBalancePingTimeout"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceValidateConnectionOnSwapServer = new BooleanConnectionProperty("loadBalanceValidateConnectionOnSwapServer", false, Messages.getString("ConnectionProperties.loadBalanceValidateConnectionOnSwapServer"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceConnectionGroup = new StringConnectionProperty("loadBalanceConnectionGroup", null, Messages.getString("ConnectionProperties.loadBalanceConnectionGroup"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceExceptionChecker = new StringConnectionProperty("loadBalanceExceptionChecker", "com.mysql.jdbc.StandardLoadBalanceExceptionChecker", null, Messages.getString("ConnectionProperties.loadBalanceExceptionChecker"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    


    loadBalanceSQLStateFailover = new StringConnectionProperty("loadBalanceSQLStateFailover", null, Messages.getString("ConnectionProperties.loadBalanceSQLStateFailover"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceSQLExceptionSubclassFailover = new StringConnectionProperty("loadBalanceSQLExceptionSubclassFailover", null, Messages.getString("ConnectionProperties.loadBalanceSQLExceptionSubclassFailover"), "5.1.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceEnableJMX = new BooleanConnectionProperty("loadBalanceEnableJMX", false, Messages.getString("ConnectionProperties.loadBalanceEnableJMX"), "5.1.13", MISC_CATEGORY, Integer.MAX_VALUE);
    

    loadBalanceHostRemovalGracePeriod = new IntegerConnectionProperty("loadBalanceHostRemovalGracePeriod", 15000, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.loadBalanceHostRemovalGracePeriod"), "5.1.39", MISC_CATEGORY, Integer.MAX_VALUE);
    

    loadBalanceAutoCommitStatementRegex = new StringConnectionProperty("loadBalanceAutoCommitStatementRegex", null, Messages.getString("ConnectionProperties.loadBalanceAutoCommitStatementRegex"), "5.1.15", MISC_CATEGORY, Integer.MIN_VALUE);
    

    loadBalanceAutoCommitStatementThreshold = new IntegerConnectionProperty("loadBalanceAutoCommitStatementThreshold", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.loadBalanceAutoCommitStatementThreshold"), "5.1.15", MISC_CATEGORY, Integer.MIN_VALUE);
    

    localSocketAddress = new StringConnectionProperty("localSocketAddress", null, Messages.getString("ConnectionProperties.localSocketAddress"), "5.0.5", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    locatorFetchBufferSize = new MemorySizeConnectionProperty("locatorFetchBufferSize", 1048576, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.locatorFetchBufferSize"), "3.2.1", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    loggerClassName = new StringConnectionProperty("logger", STANDARD_LOGGER_NAME, Messages.getString("ConnectionProperties.logger", new Object[] { Log.class.getName(), STANDARD_LOGGER_NAME }), "3.1.1", DEBUGING_PROFILING_CATEGORY, 0);
    


    logSlowQueries = new BooleanConnectionProperty("logSlowQueries", false, Messages.getString("ConnectionProperties.logSlowQueries"), "3.1.2", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    logXaCommands = new BooleanConnectionProperty("logXaCommands", false, Messages.getString("ConnectionProperties.logXaCommands"), "5.0.5", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    maintainTimeStats = new BooleanConnectionProperty("maintainTimeStats", true, Messages.getString("ConnectionProperties.maintainTimeStats"), "3.1.9", PERFORMANCE_CATEGORY, Integer.MAX_VALUE);
    

    maintainTimeStatsAsBoolean = true;
    
    maxQuerySizeToLog = new IntegerConnectionProperty("maxQuerySizeToLog", 2048, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.maxQuerySizeToLog"), "3.1.3", DEBUGING_PROFILING_CATEGORY, 4);
    

    maxReconnects = new IntegerConnectionProperty("maxReconnects", 3, 1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.maxReconnects"), "1.1", HA_CATEGORY, 4);
    

    retriesAllDown = new IntegerConnectionProperty("retriesAllDown", 120, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.retriesAllDown"), "5.1.6", HA_CATEGORY, 4);
    

    maxRows = new IntegerConnectionProperty("maxRows", -1, -1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.maxRows"), Messages.getString("ConnectionProperties.allVersions"), MISC_CATEGORY, Integer.MIN_VALUE);
    

    maxRowsAsInt = -1;
    
    metadataCacheSize = new IntegerConnectionProperty("metadataCacheSize", 50, 1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.metadataCacheSize"), "3.1.1", PERFORMANCE_CATEGORY, 5);
    

    netTimeoutForStreamingResults = new IntegerConnectionProperty("netTimeoutForStreamingResults", 600, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.netTimeoutForStreamingResults"), "5.1.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    noAccessToProcedureBodies = new BooleanConnectionProperty("noAccessToProcedureBodies", false, "When determining procedure parameter types for CallableStatements, and the connected user  can't access procedure bodies through \"SHOW CREATE PROCEDURE\" or select on mysql.proc  should the driver instead create basic metadata (all parameters reported as IN VARCHARs, but allowing registerOutParameter() to be called on them anyway) instead of throwing an exception?", "5.0.3", MISC_CATEGORY, Integer.MIN_VALUE);
    





    noDatetimeStringSync = new BooleanConnectionProperty("noDatetimeStringSync", false, Messages.getString("ConnectionProperties.noDatetimeStringSync"), "3.1.7", MISC_CATEGORY, Integer.MIN_VALUE);
    

    noTimezoneConversionForTimeType = new BooleanConnectionProperty("noTimezoneConversionForTimeType", false, Messages.getString("ConnectionProperties.noTzConversionForTimeType"), "5.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    noTimezoneConversionForDateType = new BooleanConnectionProperty("noTimezoneConversionForDateType", true, Messages.getString("ConnectionProperties.noTzConversionForDateType"), "5.1.35", MISC_CATEGORY, Integer.MIN_VALUE);
    

    cacheDefaultTimezone = new BooleanConnectionProperty("cacheDefaultTimezone", true, Messages.getString("ConnectionProperties.cacheDefaultTimezone"), "5.1.35", MISC_CATEGORY, Integer.MIN_VALUE);
    

    nullCatalogMeansCurrent = new BooleanConnectionProperty("nullCatalogMeansCurrent", true, Messages.getString("ConnectionProperties.nullCatalogMeansCurrent"), "3.1.8", MISC_CATEGORY, Integer.MIN_VALUE);
    

    nullNamePatternMatchesAll = new BooleanConnectionProperty("nullNamePatternMatchesAll", true, Messages.getString("ConnectionProperties.nullNamePatternMatchesAll"), "3.1.8", MISC_CATEGORY, Integer.MIN_VALUE);
    

    packetDebugBufferSize = new IntegerConnectionProperty("packetDebugBufferSize", 20, 1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.packetDebugBufferSize"), "3.1.3", DEBUGING_PROFILING_CATEGORY, 7);
    

    padCharsWithSpace = new BooleanConnectionProperty("padCharsWithSpace", false, Messages.getString("ConnectionProperties.padCharsWithSpace"), "5.0.6", MISC_CATEGORY, Integer.MIN_VALUE);
    

    paranoid = new BooleanConnectionProperty("paranoid", false, Messages.getString("ConnectionProperties.paranoid"), "3.0.1", SECURITY_CATEGORY, Integer.MIN_VALUE);
    

    pedantic = new BooleanConnectionProperty("pedantic", false, Messages.getString("ConnectionProperties.pedantic"), "3.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    pinGlobalTxToPhysicalConnection = new BooleanConnectionProperty("pinGlobalTxToPhysicalConnection", false, Messages.getString("ConnectionProperties.pinGlobalTxToPhysicalConnection"), "5.0.1", MISC_CATEGORY, Integer.MIN_VALUE);
    

    populateInsertRowWithDefaultValues = new BooleanConnectionProperty("populateInsertRowWithDefaultValues", false, Messages.getString("ConnectionProperties.populateInsertRowWithDefaultValues"), "5.0.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    preparedStatementCacheSize = new IntegerConnectionProperty("prepStmtCacheSize", 25, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.prepStmtCacheSize"), "3.0.10", PERFORMANCE_CATEGORY, 10);
    

    preparedStatementCacheSqlLimit = new IntegerConnectionProperty("prepStmtCacheSqlLimit", 256, 1, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.prepStmtCacheSqlLimit"), "3.0.10", PERFORMANCE_CATEGORY, 11);
    

    parseInfoCacheFactory = new StringConnectionProperty("parseInfoCacheFactory", PerConnectionLRUFactory.class.getName(), Messages.getString("ConnectionProperties.parseInfoCacheFactory"), "5.1.1", PERFORMANCE_CATEGORY, 12);
    

    processEscapeCodesForPrepStmts = new BooleanConnectionProperty("processEscapeCodesForPrepStmts", true, Messages.getString("ConnectionProperties.processEscapeCodesForPrepStmts"), "3.1.12", MISC_CATEGORY, Integer.MIN_VALUE);
    

    profilerEventHandler = new StringConnectionProperty("profilerEventHandler", "com.mysql.jdbc.profiler.LoggingProfilerEventHandler", Messages.getString("ConnectionProperties.profilerEventHandler"), "5.1.6", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    


    profileSql = new StringConnectionProperty("profileSql", null, Messages.getString("ConnectionProperties.profileSqlDeprecated"), "2.0.14", DEBUGING_PROFILING_CATEGORY, 3);
    

    profileSQL = new BooleanConnectionProperty("profileSQL", false, Messages.getString("ConnectionProperties.profileSQL"), "3.1.0", DEBUGING_PROFILING_CATEGORY, 1);
    

    profileSQLAsBoolean = false;
    
    propertiesTransform = new StringConnectionProperty("propertiesTransform", null, Messages.getString("ConnectionProperties.connectionPropertiesTransform"), "3.1.4", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    queriesBeforeRetryMaster = new IntegerConnectionProperty("queriesBeforeRetryMaster", 50, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.queriesBeforeRetryMaster"), "3.0.2", HA_CATEGORY, 7);
    

    queryTimeoutKillsConnection = new BooleanConnectionProperty("queryTimeoutKillsConnection", false, Messages.getString("ConnectionProperties.queryTimeoutKillsConnection"), "5.1.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    reconnectAtTxEnd = new BooleanConnectionProperty("reconnectAtTxEnd", false, Messages.getString("ConnectionProperties.reconnectAtTxEnd"), "3.0.10", HA_CATEGORY, 4);
    

    reconnectTxAtEndAsBoolean = false;
    
    relaxAutoCommit = new BooleanConnectionProperty("relaxAutoCommit", false, Messages.getString("ConnectionProperties.relaxAutoCommit"), "2.0.13", MISC_CATEGORY, Integer.MIN_VALUE);
    

    reportMetricsIntervalMillis = new IntegerConnectionProperty("reportMetricsIntervalMillis", 30000, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.reportMetricsIntervalMillis"), "3.1.2", DEBUGING_PROFILING_CATEGORY, 3);
    

    requireSSL = new BooleanConnectionProperty("requireSSL", false, Messages.getString("ConnectionProperties.requireSSL"), "3.1.0", SECURITY_CATEGORY, 3);
    

    resourceId = new StringConnectionProperty("resourceId", null, Messages.getString("ConnectionProperties.resourceId"), "5.0.1", HA_CATEGORY, Integer.MIN_VALUE);
    

    resultSetSizeThreshold = new IntegerConnectionProperty("resultSetSizeThreshold", 100, Messages.getString("ConnectionProperties.resultSetSizeThreshold"), "5.0.5", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    retainStatementAfterResultSetClose = new BooleanConnectionProperty("retainStatementAfterResultSetClose", false, Messages.getString("ConnectionProperties.retainStatementAfterResultSetClose"), "3.1.11", MISC_CATEGORY, Integer.MIN_VALUE);
    

    rewriteBatchedStatements = new BooleanConnectionProperty("rewriteBatchedStatements", false, Messages.getString("ConnectionProperties.rewriteBatchedStatements"), "3.1.13", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    rollbackOnPooledClose = new BooleanConnectionProperty("rollbackOnPooledClose", true, Messages.getString("ConnectionProperties.rollbackOnPooledClose"), "3.0.15", MISC_CATEGORY, Integer.MIN_VALUE);
    

    roundRobinLoadBalance = new BooleanConnectionProperty("roundRobinLoadBalance", false, Messages.getString("ConnectionProperties.roundRobinLoadBalance"), "3.1.2", HA_CATEGORY, 5);
    

    runningCTS13 = new BooleanConnectionProperty("runningCTS13", false, Messages.getString("ConnectionProperties.runningCTS13"), "3.1.7", MISC_CATEGORY, Integer.MIN_VALUE);
    

    secondsBeforeRetryMaster = new IntegerConnectionProperty("secondsBeforeRetryMaster", 30, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.secondsBeforeRetryMaster"), "3.0.2", HA_CATEGORY, 8);
    

    selfDestructOnPingSecondsLifetime = new IntegerConnectionProperty("selfDestructOnPingSecondsLifetime", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.selfDestructOnPingSecondsLifetime"), "5.1.6", HA_CATEGORY, Integer.MAX_VALUE);
    

    selfDestructOnPingMaxOperations = new IntegerConnectionProperty("selfDestructOnPingMaxOperations", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.selfDestructOnPingMaxOperations"), "5.1.6", HA_CATEGORY, Integer.MAX_VALUE);
    

    replicationEnableJMX = new BooleanConnectionProperty("replicationEnableJMX", false, Messages.getString("ConnectionProperties.loadBalanceEnableJMX"), "5.1.27", HA_CATEGORY, Integer.MAX_VALUE);
    

    serverTimezone = new StringConnectionProperty("serverTimezone", null, Messages.getString("ConnectionProperties.serverTimezone"), "3.0.2", MISC_CATEGORY, Integer.MIN_VALUE);
    

    sessionVariables = new StringConnectionProperty("sessionVariables", null, Messages.getString("ConnectionProperties.sessionVariables"), "3.1.8", MISC_CATEGORY, Integer.MAX_VALUE);
    

    slowQueryThresholdMillis = new IntegerConnectionProperty("slowQueryThresholdMillis", 2000, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.slowQueryThresholdMillis"), "3.1.2", DEBUGING_PROFILING_CATEGORY, 9);
    

    slowQueryThresholdNanos = new LongConnectionProperty("slowQueryThresholdNanos", 0L, Messages.getString("ConnectionProperties.slowQueryThresholdNanos"), "5.0.7", DEBUGING_PROFILING_CATEGORY, 10);
    

    socketFactoryClassName = new StringConnectionProperty("socketFactory", StandardSocketFactory.class.getName(), Messages.getString("ConnectionProperties.socketFactory"), "3.0.3", CONNECTION_AND_AUTH_CATEGORY, 4);
    

    socksProxyHost = new StringConnectionProperty("socksProxyHost", null, Messages.getString("ConnectionProperties.socksProxyHost"), "5.1.34", NETWORK_CATEGORY, 1);
    

    socksProxyPort = new IntegerConnectionProperty("socksProxyPort", SocksProxySocketFactory.SOCKS_DEFAULT_PORT, 0, 65535, Messages.getString("ConnectionProperties.socksProxyPort"), "5.1.34", NETWORK_CATEGORY, 2);
    

    socketTimeout = new IntegerConnectionProperty("socketTimeout", 0, 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.socketTimeout"), "3.0.1", CONNECTION_AND_AUTH_CATEGORY, 10);
    

    statementInterceptors = new StringConnectionProperty("statementInterceptors", null, Messages.getString("ConnectionProperties.statementInterceptors"), "5.1.1", MISC_CATEGORY, Integer.MIN_VALUE);
    

    strictFloatingPoint = new BooleanConnectionProperty("strictFloatingPoint", false, Messages.getString("ConnectionProperties.strictFloatingPoint"), "3.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    strictUpdates = new BooleanConnectionProperty("strictUpdates", true, Messages.getString("ConnectionProperties.strictUpdates"), "3.0.4", MISC_CATEGORY, Integer.MIN_VALUE);
    

    overrideSupportsIntegrityEnhancementFacility = new BooleanConnectionProperty("overrideSupportsIntegrityEnhancementFacility", false, Messages.getString("ConnectionProperties.overrideSupportsIEF"), "3.1.12", MISC_CATEGORY, Integer.MIN_VALUE);
    


    tcpNoDelay = new BooleanConnectionProperty("tcpNoDelay", Boolean.valueOf("true").booleanValue(), Messages.getString("ConnectionProperties.tcpNoDelay"), "5.0.7", NETWORK_CATEGORY, Integer.MIN_VALUE);
    


    tcpKeepAlive = new BooleanConnectionProperty("tcpKeepAlive", Boolean.valueOf("true").booleanValue(), Messages.getString("ConnectionProperties.tcpKeepAlive"), "5.0.7", NETWORK_CATEGORY, Integer.MIN_VALUE);
    


    tcpRcvBuf = new IntegerConnectionProperty("tcpRcvBuf", Integer.parseInt("0"), 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.tcpSoRcvBuf"), "5.0.7", NETWORK_CATEGORY, Integer.MIN_VALUE);
    


    tcpSndBuf = new IntegerConnectionProperty("tcpSndBuf", Integer.parseInt("0"), 0, Integer.MAX_VALUE, Messages.getString("ConnectionProperties.tcpSoSndBuf"), "5.0.7", NETWORK_CATEGORY, Integer.MIN_VALUE);
    


    tcpTrafficClass = new IntegerConnectionProperty("tcpTrafficClass", Integer.parseInt("0"), 0, 255, Messages.getString("ConnectionProperties.tcpTrafficClass"), "5.0.7", NETWORK_CATEGORY, Integer.MIN_VALUE);
    


    tinyInt1isBit = new BooleanConnectionProperty("tinyInt1isBit", true, Messages.getString("ConnectionProperties.tinyInt1isBit"), "3.0.16", MISC_CATEGORY, Integer.MIN_VALUE);
    

    traceProtocol = new BooleanConnectionProperty("traceProtocol", false, Messages.getString("ConnectionProperties.traceProtocol"), "3.1.2", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    treatUtilDateAsTimestamp = new BooleanConnectionProperty("treatUtilDateAsTimestamp", true, Messages.getString("ConnectionProperties.treatUtilDateAsTimestamp"), "5.0.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    transformedBitIsBoolean = new BooleanConnectionProperty("transformedBitIsBoolean", false, Messages.getString("ConnectionProperties.transformedBitIsBoolean"), "3.1.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useBlobToStoreUTF8OutsideBMP = new BooleanConnectionProperty("useBlobToStoreUTF8OutsideBMP", false, Messages.getString("ConnectionProperties.useBlobToStoreUTF8OutsideBMP"), "5.1.3", MISC_CATEGORY, 128);
    

    utf8OutsideBmpExcludedColumnNamePattern = new StringConnectionProperty("utf8OutsideBmpExcludedColumnNamePattern", null, Messages.getString("ConnectionProperties.utf8OutsideBmpExcludedColumnNamePattern"), "5.1.3", MISC_CATEGORY, 129);
    

    utf8OutsideBmpIncludedColumnNamePattern = new StringConnectionProperty("utf8OutsideBmpIncludedColumnNamePattern", null, Messages.getString("ConnectionProperties.utf8OutsideBmpIncludedColumnNamePattern"), "5.1.3", MISC_CATEGORY, 129);
    

    useCompression = new BooleanConnectionProperty("useCompression", false, Messages.getString("ConnectionProperties.useCompression"), "3.0.17", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    useColumnNamesInFindColumn = new BooleanConnectionProperty("useColumnNamesInFindColumn", false, Messages.getString("ConnectionProperties.useColumnNamesInFindColumn"), "5.1.7", MISC_CATEGORY, Integer.MAX_VALUE);
    

    useConfigs = new StringConnectionProperty("useConfigs", null, Messages.getString("ConnectionProperties.useConfigs"), "3.1.5", CONNECTION_AND_AUTH_CATEGORY, Integer.MAX_VALUE);
    

    useCursorFetch = new BooleanConnectionProperty("useCursorFetch", false, Messages.getString("ConnectionProperties.useCursorFetch"), "5.0.0", PERFORMANCE_CATEGORY, Integer.MAX_VALUE);
    

    useDynamicCharsetInfo = new BooleanConnectionProperty("useDynamicCharsetInfo", true, Messages.getString("ConnectionProperties.useDynamicCharsetInfo"), "5.0.6", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    useDirectRowUnpack = new BooleanConnectionProperty("useDirectRowUnpack", true, "Use newer result set row unpacking code that skips a copy from network buffers  to a MySQL packet instance and instead reads directly into the result set row data buffers.", "5.1.1", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    



    useFastIntParsing = new BooleanConnectionProperty("useFastIntParsing", true, Messages.getString("ConnectionProperties.useFastIntParsing"), "3.1.4", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    useFastDateParsing = new BooleanConnectionProperty("useFastDateParsing", true, Messages.getString("ConnectionProperties.useFastDateParsing"), "5.0.5", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    useHostsInPrivileges = new BooleanConnectionProperty("useHostsInPrivileges", true, Messages.getString("ConnectionProperties.useHostsInPrivileges"), "3.0.2", MISC_CATEGORY, Integer.MIN_VALUE);
    
    useInformationSchema = new BooleanConnectionProperty("useInformationSchema", false, Messages.getString("ConnectionProperties.useInformationSchema"), "5.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    
    useJDBCCompliantTimezoneShift = new BooleanConnectionProperty("useJDBCCompliantTimezoneShift", false, Messages.getString("ConnectionProperties.useJDBCCompliantTimezoneShift"), "5.0.0", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useLocalSessionState = new BooleanConnectionProperty("useLocalSessionState", false, Messages.getString("ConnectionProperties.useLocalSessionState"), "3.1.7", PERFORMANCE_CATEGORY, 5);
    

    useLocalTransactionState = new BooleanConnectionProperty("useLocalTransactionState", false, Messages.getString("ConnectionProperties.useLocalTransactionState"), "5.1.7", PERFORMANCE_CATEGORY, 6);
    

    useLegacyDatetimeCode = new BooleanConnectionProperty("useLegacyDatetimeCode", true, Messages.getString("ConnectionProperties.useLegacyDatetimeCode"), "5.1.6", MISC_CATEGORY, Integer.MIN_VALUE);
    

    sendFractionalSeconds = new BooleanConnectionProperty("sendFractionalSeconds", true, Messages.getString("ConnectionProperties.sendFractionalSeconds"), "5.1.37", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useNanosForElapsedTime = new BooleanConnectionProperty("useNanosForElapsedTime", false, Messages.getString("ConnectionProperties.useNanosForElapsedTime"), "5.0.7", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    

    useOldAliasMetadataBehavior = new BooleanConnectionProperty("useOldAliasMetadataBehavior", false, Messages.getString("ConnectionProperties.useOldAliasMetadataBehavior"), "5.0.4", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useOldUTF8Behavior = new BooleanConnectionProperty("useOldUTF8Behavior", false, Messages.getString("ConnectionProperties.useOldUtf8Behavior"), "3.1.6", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useOldUTF8BehaviorAsBoolean = false;
    
    useOnlyServerErrorMessages = new BooleanConnectionProperty("useOnlyServerErrorMessages", true, Messages.getString("ConnectionProperties.useOnlyServerErrorMessages"), "3.0.15", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useReadAheadInput = new BooleanConnectionProperty("useReadAheadInput", true, Messages.getString("ConnectionProperties.useReadAheadInput"), "3.1.5", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    useSqlStateCodes = new BooleanConnectionProperty("useSqlStateCodes", true, Messages.getString("ConnectionProperties.useSqlStateCodes"), "3.1.3", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useSSL = new BooleanConnectionProperty("useSSL", false, Messages.getString("ConnectionProperties.useSSL"), "3.0.2", SECURITY_CATEGORY, 2);
    

    useSSPSCompatibleTimezoneShift = new BooleanConnectionProperty("useSSPSCompatibleTimezoneShift", false, Messages.getString("ConnectionProperties.useSSPSCompatibleTimezoneShift"), "5.0.5", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useStreamLengthsInPrepStmts = new BooleanConnectionProperty("useStreamLengthsInPrepStmts", true, Messages.getString("ConnectionProperties.useStreamLengthsInPrepStmts"), "3.0.2", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useTimezone = new BooleanConnectionProperty("useTimezone", false, Messages.getString("ConnectionProperties.useTimezone"), "3.0.2", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useUltraDevWorkAround = new BooleanConnectionProperty("ultraDevHack", false, Messages.getString("ConnectionProperties.ultraDevHack"), "2.0.3", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useUnbufferedInput = new BooleanConnectionProperty("useUnbufferedInput", true, Messages.getString("ConnectionProperties.useUnbufferedInput"), "3.0.11", MISC_CATEGORY, Integer.MIN_VALUE);
    

    useUnicode = new BooleanConnectionProperty("useUnicode", true, Messages.getString("ConnectionProperties.useUnicode"), "1.1g", MISC_CATEGORY, 0);
    


    useUnicodeAsBoolean = true;
    
    useUsageAdvisor = new BooleanConnectionProperty("useUsageAdvisor", false, Messages.getString("ConnectionProperties.useUsageAdvisor"), "3.1.1", DEBUGING_PROFILING_CATEGORY, 10);
    

    useUsageAdvisorAsBoolean = false;
    
    yearIsDateType = new BooleanConnectionProperty("yearIsDateType", true, Messages.getString("ConnectionProperties.yearIsDateType"), "3.1.9", MISC_CATEGORY, Integer.MIN_VALUE);
    

    zeroDateTimeBehavior = new StringConnectionProperty("zeroDateTimeBehavior", "exception", new String[] { "exception", "round", "convertToNull" }, Messages.getString("ConnectionProperties.zeroDateTimeBehavior", new Object[] { "exception", "round", "convertToNull" }), "3.1.4", MISC_CATEGORY, Integer.MIN_VALUE);
    




    useJvmCharsetConverters = new BooleanConnectionProperty("useJvmCharsetConverters", false, Messages.getString("ConnectionProperties.useJvmCharsetConverters"), "5.0.1", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    useGmtMillisForDatetimes = new BooleanConnectionProperty("useGmtMillisForDatetimes", false, Messages.getString("ConnectionProperties.useGmtMillisForDatetimes"), "3.1.12", MISC_CATEGORY, Integer.MIN_VALUE);
    

    dumpMetadataOnColumnNotFound = new BooleanConnectionProperty("dumpMetadataOnColumnNotFound", false, Messages.getString("ConnectionProperties.dumpMetadataOnColumnNotFound"), "3.1.13", DEBUGING_PROFILING_CATEGORY, Integer.MIN_VALUE);
    



    clientCertificateKeyStoreUrl = new StringConnectionProperty("clientCertificateKeyStoreUrl", null, Messages.getString("ConnectionProperties.clientCertificateKeyStoreUrl"), "5.1.0", SECURITY_CATEGORY, 5);
    

    trustCertificateKeyStoreUrl = new StringConnectionProperty("trustCertificateKeyStoreUrl", null, Messages.getString("ConnectionProperties.trustCertificateKeyStoreUrl"), "5.1.0", SECURITY_CATEGORY, 8);
    

    clientCertificateKeyStoreType = new StringConnectionProperty("clientCertificateKeyStoreType", "JKS", Messages.getString("ConnectionProperties.clientCertificateKeyStoreType"), "5.1.0", SECURITY_CATEGORY, 6);
    

    clientCertificateKeyStorePassword = new StringConnectionProperty("clientCertificateKeyStorePassword", null, Messages.getString("ConnectionProperties.clientCertificateKeyStorePassword"), "5.1.0", SECURITY_CATEGORY, 7);
    

    trustCertificateKeyStoreType = new StringConnectionProperty("trustCertificateKeyStoreType", "JKS", Messages.getString("ConnectionProperties.trustCertificateKeyStoreType"), "5.1.0", SECURITY_CATEGORY, 9);
    

    trustCertificateKeyStorePassword = new StringConnectionProperty("trustCertificateKeyStorePassword", null, Messages.getString("ConnectionProperties.trustCertificateKeyStorePassword"), "5.1.0", SECURITY_CATEGORY, 10);
    

    verifyServerCertificate = new BooleanConnectionProperty("verifyServerCertificate", true, Messages.getString("ConnectionProperties.verifyServerCertificate"), "5.1.6", SECURITY_CATEGORY, 4);
    

    useAffectedRows = new BooleanConnectionProperty("useAffectedRows", false, Messages.getString("ConnectionProperties.useAffectedRows"), "5.1.7", MISC_CATEGORY, Integer.MIN_VALUE);
    

    passwordCharacterEncoding = new StringConnectionProperty("passwordCharacterEncoding", null, Messages.getString("ConnectionProperties.passwordCharacterEncoding"), "5.1.7", SECURITY_CATEGORY, Integer.MIN_VALUE);
    

    maxAllowedPacket = new IntegerConnectionProperty("maxAllowedPacket", -1, Messages.getString("ConnectionProperties.maxAllowedPacket"), "5.1.8", NETWORK_CATEGORY, Integer.MIN_VALUE);
    

    authenticationPlugins = new StringConnectionProperty("authenticationPlugins", null, Messages.getString("ConnectionProperties.authenticationPlugins"), "5.1.19", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    disabledAuthenticationPlugins = new StringConnectionProperty("disabledAuthenticationPlugins", null, Messages.getString("ConnectionProperties.disabledAuthenticationPlugins"), "5.1.19", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    defaultAuthenticationPlugin = new StringConnectionProperty("defaultAuthenticationPlugin", "com.mysql.jdbc.authentication.MysqlNativePasswordPlugin", Messages.getString("ConnectionProperties.defaultAuthenticationPlugin"), "5.1.19", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    


    disconnectOnExpiredPasswords = new BooleanConnectionProperty("disconnectOnExpiredPasswords", true, Messages.getString("ConnectionProperties.disconnectOnExpiredPasswords"), "5.1.23", CONNECTION_AND_AUTH_CATEGORY, Integer.MIN_VALUE);
    

    getProceduresReturnsFunctions = new BooleanConnectionProperty("getProceduresReturnsFunctions", true, Messages.getString("ConnectionProperties.getProceduresReturnsFunctions"), "5.1.26", MISC_CATEGORY, Integer.MIN_VALUE);
    

    detectCustomCollations = new BooleanConnectionProperty("detectCustomCollations", false, Messages.getString("ConnectionProperties.detectCustomCollations"), "5.1.29", MISC_CATEGORY, Integer.MIN_VALUE);
    

    serverRSAPublicKeyFile = new StringConnectionProperty("serverRSAPublicKeyFile", null, Messages.getString("ConnectionProperties.serverRSAPublicKeyFile"), "5.1.31", SECURITY_CATEGORY, Integer.MIN_VALUE);
    

    allowPublicKeyRetrieval = new BooleanConnectionProperty("allowPublicKeyRetrieval", false, Messages.getString("ConnectionProperties.allowPublicKeyRetrieval"), "5.1.31", SECURITY_CATEGORY, Integer.MIN_VALUE);
    

    dontCheckOnDuplicateKeyUpdateInSQL = new BooleanConnectionProperty("dontCheckOnDuplicateKeyUpdateInSQL", false, Messages.getString("ConnectionProperties.dontCheckOnDuplicateKeyUpdateInSQL"), "5.1.32", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    readOnlyPropagatesToServer = new BooleanConnectionProperty("readOnlyPropagatesToServer", true, Messages.getString("ConnectionProperties.readOnlyPropagatesToServer"), "5.1.35", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
    

    enabledSSLCipherSuites = new StringConnectionProperty("enabledSSLCipherSuites", null, Messages.getString("ConnectionProperties.enabledSSLCipherSuites"), "5.1.35", SECURITY_CATEGORY, 11);
    

    enabledTLSProtocols = new StringConnectionProperty("enabledTLSProtocols", null, Messages.getString("ConnectionProperties.enabledTLSProtocols"), "5.1.44", SECURITY_CATEGORY, 12);
    

    enableEscapeProcessing = new BooleanConnectionProperty("enableEscapeProcessing", true, Messages.getString("ConnectionProperties.enableEscapeProcessing"), "5.1.37", PERFORMANCE_CATEGORY, Integer.MIN_VALUE);
  }
  
  protected DriverPropertyInfo[] exposeAsDriverPropertyInfoInternal(Properties info, int slotsToReserve)
    throws SQLException
  {
    initializeProperties(info);
    
    int numProperties = PROPERTY_LIST.size();
    
    int listSize = numProperties + slotsToReserve;
    
    DriverPropertyInfo[] driverProperties = new DriverPropertyInfo[listSize];
    
    for (int i = slotsToReserve; i < listSize; i++) {
      Field propertyField = (Field)PROPERTY_LIST.get(i - slotsToReserve);
      try
      {
        ConnectionProperty propToExpose = (ConnectionProperty)propertyField.get(this);
        
        if (info != null) {
          propToExpose.initializeFrom(info, getExceptionInterceptor());
        }
        
        driverProperties[i] = propToExpose.getAsDriverPropertyInfo();
      } catch (IllegalAccessException iae) {
        throw SQLError.createSQLException(Messages.getString("ConnectionProperties.InternalPropertiesFailure"), "S1000", getExceptionInterceptor());
      }
    }
    

    return driverProperties;
  }
  
  protected Properties exposeAsProperties(Properties info)
    throws SQLException
  {
    if (info == null) {
      info = new Properties();
    }
    
    int numPropertiesToSet = PROPERTY_LIST.size();
    
    for (int i = 0; i < numPropertiesToSet; i++) {
      Field propertyField = (Field)PROPERTY_LIST.get(i);
      try
      {
        ConnectionProperty propToGet = (ConnectionProperty)propertyField.get(this);
        
        Object propValue = propToGet.getValueAsObject();
        
        if (propValue != null) {
          info.setProperty(propToGet.getPropertyName(), propValue.toString());
        }
      } catch (IllegalAccessException iae) {
        throw SQLError.createSQLException("Internal properties failure", "S1000", getExceptionInterceptor());
      }
    }
    
    return info;
  }
  
  public Properties exposeAsProperties(Properties props, boolean explicitOnly)
    throws SQLException
  {
    if (props == null) {
      props = new Properties();
    }
    
    int numPropertiesToSet = PROPERTY_LIST.size();
    
    for (int i = 0; i < numPropertiesToSet; i++) {
      Field propertyField = (Field)PROPERTY_LIST.get(i);
      try
      {
        ConnectionProperty propToGet = (ConnectionProperty)propertyField.get(this);
        
        Object propValue = propToGet.getValueAsObject();
        
        if ((propValue != null) && ((!explicitOnly) || (propToGet.isExplicitlySet()))) {
          props.setProperty(propToGet.getPropertyName(), propValue.toString());
        }
      } catch (IllegalAccessException iae) {
        throw SQLError.createSQLException("Internal properties failure", "S1000", iae, getExceptionInterceptor());
      }
    }
    
    return props;
  }
  
  class XmlMap
  {
    protected Map<Integer, Map<String, ConnectionPropertiesImpl.ConnectionProperty>> ordered = new TreeMap();
    protected Map<String, ConnectionPropertiesImpl.ConnectionProperty> alpha = new TreeMap();
    
    XmlMap() {}
  }
  
  public String exposeAsXml()
    throws SQLException
  {
    StringBuilder xmlBuf = new StringBuilder();
    xmlBuf.append("<ConnectionProperties>");
    
    int numPropertiesToSet = PROPERTY_LIST.size();
    
    int numCategories = PROPERTY_CATEGORIES.length;
    
    Map<String, XmlMap> propertyListByCategory = new HashMap();
    
    for (int i = 0; i < numCategories; i++) {
      propertyListByCategory.put(PROPERTY_CATEGORIES[i], new XmlMap());
    }
    




    StringConnectionProperty userProp = new StringConnectionProperty("user", null, Messages.getString("ConnectionProperties.Username"), Messages.getString("ConnectionProperties.allVersions"), CONNECTION_AND_AUTH_CATEGORY, -2147483647);
    

    StringConnectionProperty passwordProp = new StringConnectionProperty("password", null, Messages.getString("ConnectionProperties.Password"), Messages.getString("ConnectionProperties.allVersions"), CONNECTION_AND_AUTH_CATEGORY, -2147483646);
    


    XmlMap connectionSortMaps = (XmlMap)propertyListByCategory.get(CONNECTION_AND_AUTH_CATEGORY);
    TreeMap<String, ConnectionProperty> userMap = new TreeMap();
    userMap.put(userProp.getPropertyName(), userProp);
    
    ordered.put(Integer.valueOf(userProp.getOrder()), userMap);
    
    TreeMap<String, ConnectionProperty> passwordMap = new TreeMap();
    passwordMap.put(passwordProp.getPropertyName(), passwordProp);
    
    ordered.put(new Integer(passwordProp.getOrder()), passwordMap);
    try
    {
      for (int i = 0; i < numPropertiesToSet; i++) {
        Field propertyField = (Field)PROPERTY_LIST.get(i);
        ConnectionProperty propToGet = (ConnectionProperty)propertyField.get(this);
        XmlMap sortMaps = (XmlMap)propertyListByCategory.get(propToGet.getCategoryName());
        int orderInCategory = propToGet.getOrder();
        
        if (orderInCategory == Integer.MIN_VALUE) {
          alpha.put(propToGet.getPropertyName(), propToGet);
        } else {
          Integer order = Integer.valueOf(orderInCategory);
          Map<String, ConnectionProperty> orderMap = (Map)ordered.get(order);
          
          if (orderMap == null) {
            orderMap = new TreeMap();
            ordered.put(order, orderMap);
          }
          
          orderMap.put(propToGet.getPropertyName(), propToGet);
        }
      }
      
      for (int j = 0; j < numCategories; j++) {
        XmlMap sortMaps = (XmlMap)propertyListByCategory.get(PROPERTY_CATEGORIES[j]);
        
        xmlBuf.append("\n <PropertyCategory name=\"");
        xmlBuf.append(PROPERTY_CATEGORIES[j]);
        xmlBuf.append("\">");
        
        for (Map<String, ConnectionProperty> orderedEl : ordered.values()) {
          for (ConnectionProperty propToGet : orderedEl.values()) {
            xmlBuf.append("\n  <Property name=\"");
            xmlBuf.append(propToGet.getPropertyName());
            xmlBuf.append("\" required=\"");
            xmlBuf.append(required ? "Yes" : "No");
            
            xmlBuf.append("\" default=\"");
            
            if (propToGet.getDefaultValue() != null) {
              xmlBuf.append(propToGet.getDefaultValue());
            }
            
            xmlBuf.append("\" sortOrder=\"");
            xmlBuf.append(propToGet.getOrder());
            xmlBuf.append("\" since=\"");
            xmlBuf.append(sinceVersion);
            xmlBuf.append("\">\n");
            xmlBuf.append("    ");
            String escapedDescription = description;
            escapedDescription = escapedDescription.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            
            xmlBuf.append(escapedDescription);
            xmlBuf.append("\n  </Property>");
          }
        }
        
        for (ConnectionProperty propToGet : alpha.values()) {
          xmlBuf.append("\n  <Property name=\"");
          xmlBuf.append(propToGet.getPropertyName());
          xmlBuf.append("\" required=\"");
          xmlBuf.append(required ? "Yes" : "No");
          
          xmlBuf.append("\" default=\"");
          
          if (propToGet.getDefaultValue() != null) {
            xmlBuf.append(propToGet.getDefaultValue());
          }
          
          xmlBuf.append("\" sortOrder=\"alpha\" since=\"");
          xmlBuf.append(sinceVersion);
          xmlBuf.append("\">\n");
          xmlBuf.append("    ");
          xmlBuf.append(description);
          xmlBuf.append("\n  </Property>");
        }
        
        xmlBuf.append("\n </PropertyCategory>");
      }
    } catch (IllegalAccessException iae) {
      throw SQLError.createSQLException("Internal properties failure", "S1000", getExceptionInterceptor());
    }
    
    xmlBuf.append("\n</ConnectionProperties>");
    
    return xmlBuf.toString();
  }
  
  public boolean getAllowLoadLocalInfile()
  {
    return allowLoadLocalInfile.getValueAsBoolean();
  }
  
  public boolean getAllowMultiQueries()
  {
    return allowMultiQueries.getValueAsBoolean();
  }
  
  public boolean getAllowNanAndInf()
  {
    return allowNanAndInf.getValueAsBoolean();
  }
  
  public boolean getAllowUrlInLocalInfile()
  {
    return allowUrlInLocalInfile.getValueAsBoolean();
  }
  
  public boolean getAlwaysSendSetIsolation()
  {
    return alwaysSendSetIsolation.getValueAsBoolean();
  }
  
  public boolean getAutoDeserialize()
  {
    return autoDeserialize.getValueAsBoolean();
  }
  
  public boolean getAutoGenerateTestcaseScript()
  {
    return autoGenerateTestcaseScriptAsBoolean;
  }
  
  public boolean getAutoReconnectForPools()
  {
    return autoReconnectForPoolsAsBoolean;
  }
  
  public int getBlobSendChunkSize()
  {
    return blobSendChunkSize.getValueAsInt();
  }
  
  public boolean getCacheCallableStatements()
  {
    return cacheCallableStatements.getValueAsBoolean();
  }
  
  public boolean getCachePreparedStatements()
  {
    return ((Boolean)cachePreparedStatements.getValueAsObject()).booleanValue();
  }
  
  public boolean getCacheResultSetMetadata()
  {
    return cacheResultSetMetaDataAsBoolean;
  }
  
  public boolean getCacheServerConfiguration()
  {
    return cacheServerConfiguration.getValueAsBoolean();
  }
  
  public int getCallableStatementCacheSize()
  {
    return callableStatementCacheSize.getValueAsInt();
  }
  
  public boolean getCapitalizeTypeNames()
  {
    return capitalizeTypeNames.getValueAsBoolean();
  }
  
  public String getCharacterSetResults()
  {
    return characterSetResults.getValueAsString();
  }
  
  public String getConnectionAttributes()
  {
    return connectionAttributes.getValueAsString();
  }
  
  public void setConnectionAttributes(String val)
  {
    connectionAttributes.setValue(val);
  }
  
  public boolean getClobberStreamingResults()
  {
    return clobberStreamingResults.getValueAsBoolean();
  }
  
  public String getClobCharacterEncoding()
  {
    return clobCharacterEncoding.getValueAsString();
  }
  
  public String getConnectionCollation()
  {
    return connectionCollation.getValueAsString();
  }
  
  public int getConnectTimeout()
  {
    return connectTimeout.getValueAsInt();
  }
  
  public boolean getContinueBatchOnError()
  {
    return continueBatchOnError.getValueAsBoolean();
  }
  
  public boolean getCreateDatabaseIfNotExist()
  {
    return createDatabaseIfNotExist.getValueAsBoolean();
  }
  
  public int getDefaultFetchSize()
  {
    return defaultFetchSize.getValueAsInt();
  }
  
  public boolean getDontTrackOpenResources()
  {
    return dontTrackOpenResources.getValueAsBoolean();
  }
  private BooleanConnectionProperty detectServerPreparedStmts;
  private BooleanConnectionProperty dontTrackOpenResources;
  private BooleanConnectionProperty dumpQueriesOnException;
  private BooleanConnectionProperty dynamicCalendars;
  private BooleanConnectionProperty elideSetAutoCommits;
  private BooleanConnectionProperty emptyStringsConvertToZero;
  private BooleanConnectionProperty emulateLocators;
  private BooleanConnectionProperty emulateUnsupportedPstmts;
  private BooleanConnectionProperty enablePacketDebug;
  private BooleanConnectionProperty enableQueryTimeouts;
  
  public boolean getDumpQueriesOnException()
  {
    return dumpQueriesOnException.getValueAsBoolean();
  }
  
  private BooleanConnectionProperty explainSlowQueries;
  private StringConnectionProperty exceptionInterceptors;
  private BooleanConnectionProperty failOverReadOnly;
  private BooleanConnectionProperty gatherPerformanceMetrics;
  private BooleanConnectionProperty generateSimpleParameterMetadata;
  private boolean highAvailabilityAsBoolean;
  private BooleanConnectionProperty holdResultsOpenOverStatementClose;
  private BooleanConnectionProperty includeInnodbStatusInDeadlockExceptions;
  private BooleanConnectionProperty includeThreadDumpInDeadlockExceptions;
  public boolean getDynamicCalendars()
  {
    return dynamicCalendars.getValueAsBoolean();
  }
  
  private BooleanConnectionProperty includeThreadNamesAsStatementComment;
  private BooleanConnectionProperty ignoreNonTxTables;
  private IntegerConnectionProperty initialTimeout;
  private BooleanConnectionProperty isInteractiveClient;
  private BooleanConnectionProperty jdbcCompliantTruncation;
  private boolean jdbcCompliantTruncationForReads;
  protected MemorySizeConnectionProperty largeRowSizeThreshold;
  private StringConnectionProperty loadBalanceStrategy;
  private StringConnectionProperty serverAffinityOrder;
  private IntegerConnectionProperty loadBalanceBlacklistTimeout;
  private IntegerConnectionProperty loadBalancePingTimeout;
  private BooleanConnectionProperty loadBalanceValidateConnectionOnSwapServer;
  
  public boolean getElideSetAutoCommits()
  {
    return false;
  }
  
  private StringConnectionProperty loadBalanceConnectionGroup;
  private StringConnectionProperty loadBalanceExceptionChecker;
  private StringConnectionProperty loadBalanceSQLStateFailover;
  private StringConnectionProperty loadBalanceSQLExceptionSubclassFailover;
  private BooleanConnectionProperty loadBalanceEnableJMX;
  private IntegerConnectionProperty loadBalanceHostRemovalGracePeriod;
  private StringConnectionProperty loadBalanceAutoCommitStatementRegex;
  private IntegerConnectionProperty loadBalanceAutoCommitStatementThreshold;
  private StringConnectionProperty localSocketAddress;
  private MemorySizeConnectionProperty locatorFetchBufferSize;
  private StringConnectionProperty loggerClassName;
  private BooleanConnectionProperty logSlowQueries;
  private BooleanConnectionProperty logXaCommands;
  private BooleanConnectionProperty maintainTimeStats;
  private boolean maintainTimeStatsAsBoolean;
  
  public boolean getEmptyStringsConvertToZero()
  {
    return emptyStringsConvertToZero.getValueAsBoolean();
  }
  
  public boolean getEmulateLocators()
  {
    return emulateLocators.getValueAsBoolean();
  }
  
  public boolean getEmulateUnsupportedPstmts()
  {
    return emulateUnsupportedPstmts.getValueAsBoolean();
  }
  
  public boolean getEnablePacketDebug()
  {
    return enablePacketDebug.getValueAsBoolean();
  }
  
  public String getEncoding()
  {
    return characterEncodingAsString;
  }
  
  public boolean getExplainSlowQueries()
  {
    return explainSlowQueries.getValueAsBoolean();
  }
  
  public boolean getFailOverReadOnly()
  {
    return failOverReadOnly.getValueAsBoolean();
  }
  
  public boolean getGatherPerformanceMetrics()
  {
    return gatherPerformanceMetrics.getValueAsBoolean();
  }
  
  protected boolean getHighAvailability()
  {
    return highAvailabilityAsBoolean;
  }
  
  public boolean getHoldResultsOpenOverStatementClose()
  {
    return holdResultsOpenOverStatementClose.getValueAsBoolean();
  }
  
  public boolean getIgnoreNonTxTables()
  {
    return ignoreNonTxTables.getValueAsBoolean();
  }
  
  public int getInitialTimeout()
  {
    return initialTimeout.getValueAsInt();
  }
  
  public boolean getInteractiveClient()
  {
    return isInteractiveClient.getValueAsBoolean();
  }
  
  public boolean getIsInteractiveClient()
  {
    return isInteractiveClient.getValueAsBoolean();
  }
  
  public boolean getJdbcCompliantTruncation()
  {
    return jdbcCompliantTruncation.getValueAsBoolean();
  }
  
  public int getLocatorFetchBufferSize()
  {
    return locatorFetchBufferSize.getValueAsInt();
  }
  
  public String getLogger()
  {
    return loggerClassName.getValueAsString();
  }
  
  public String getLoggerClassName()
  {
    return loggerClassName.getValueAsString();
  }
  
  public boolean getLogSlowQueries()
  {
    return logSlowQueries.getValueAsBoolean();
  }
  
  public boolean getMaintainTimeStats()
  {
    return maintainTimeStatsAsBoolean;
  }
  
  public int getMaxQuerySizeToLog()
  {
    return maxQuerySizeToLog.getValueAsInt();
  }
  
  public int getMaxReconnects()
  {
    return maxReconnects.getValueAsInt();
  }
  
  public int getMaxRows()
  {
    return maxRowsAsInt;
  }
  
  public int getMetadataCacheSize()
  {
    return metadataCacheSize.getValueAsInt();
  }
  
  public boolean getNoDatetimeStringSync()
  {
    return noDatetimeStringSync.getValueAsBoolean();
  }
  
  public boolean getNullCatalogMeansCurrent()
  {
    return nullCatalogMeansCurrent.getValueAsBoolean();
  }
  
  public boolean getNullNamePatternMatchesAll()
  {
    return nullNamePatternMatchesAll.getValueAsBoolean();
  }
  
  public int getPacketDebugBufferSize()
  {
    return packetDebugBufferSize.getValueAsInt();
  }
  
  public boolean getParanoid()
  {
    return paranoid.getValueAsBoolean();
  }
  
  public boolean getPedantic()
  {
    return pedantic.getValueAsBoolean();
  }
  
  public int getPreparedStatementCacheSize()
  {
    return ((Integer)preparedStatementCacheSize.getValueAsObject()).intValue();
  }
  
  public int getPreparedStatementCacheSqlLimit()
  {
    return ((Integer)preparedStatementCacheSqlLimit.getValueAsObject()).intValue();
  }
  
  public boolean getProfileSql()
  {
    return profileSQLAsBoolean;
  }
  
  public boolean getProfileSQL()
  {
    return profileSQL.getValueAsBoolean();
  }
  
  public String getPropertiesTransform()
  {
    return propertiesTransform.getValueAsString();
  }
  
  public int getQueriesBeforeRetryMaster()
  {
    return queriesBeforeRetryMaster.getValueAsInt();
  }
  
  public boolean getReconnectAtTxEnd()
  {
    return reconnectTxAtEndAsBoolean;
  }
  
  public boolean getRelaxAutoCommit()
  {
    return relaxAutoCommit.getValueAsBoolean();
  }
  
  public int getReportMetricsIntervalMillis()
  {
    return reportMetricsIntervalMillis.getValueAsInt();
  }
  
  public boolean getRequireSSL()
  {
    return requireSSL.getValueAsBoolean();
  }
  
  public boolean getRetainStatementAfterResultSetClose()
  {
    return retainStatementAfterResultSetClose.getValueAsBoolean();
  }
  
  public boolean getRollbackOnPooledClose()
  {
    return rollbackOnPooledClose.getValueAsBoolean();
  }
  
  public boolean getRoundRobinLoadBalance()
  {
    return roundRobinLoadBalance.getValueAsBoolean();
  }
  
  public boolean getRunningCTS13()
  {
    return runningCTS13.getValueAsBoolean();
  }
  
  public int getSecondsBeforeRetryMaster()
  {
    return secondsBeforeRetryMaster.getValueAsInt();
  }
  
  public String getServerTimezone()
  {
    return serverTimezone.getValueAsString();
  }
  
  public String getSessionVariables()
  {
    return sessionVariables.getValueAsString();
  }
  
  public int getSlowQueryThresholdMillis()
  {
    return slowQueryThresholdMillis.getValueAsInt();
  }
  
  public String getSocketFactoryClassName()
  {
    return socketFactoryClassName.getValueAsString();
  }
  
  public int getSocketTimeout()
  {
    return socketTimeout.getValueAsInt();
  }
  
  public boolean getStrictFloatingPoint()
  {
    return strictFloatingPoint.getValueAsBoolean();
  }
  
  public boolean getStrictUpdates()
  {
    return strictUpdates.getValueAsBoolean();
  }
  
  public boolean getTinyInt1isBit()
  {
    return tinyInt1isBit.getValueAsBoolean();
  }
  
  public boolean getTraceProtocol()
  {
    return traceProtocol.getValueAsBoolean();
  }
  
  public boolean getTransformedBitIsBoolean()
  {
    return transformedBitIsBoolean.getValueAsBoolean();
  }
  
  public boolean getUseCompression()
  {
    return useCompression.getValueAsBoolean();
  }
  
  public boolean getUseFastIntParsing()
  {
    return useFastIntParsing.getValueAsBoolean();
  }
  
  public boolean getUseHostsInPrivileges()
  {
    return useHostsInPrivileges.getValueAsBoolean();
  }
  
  public boolean getUseInformationSchema()
  {
    return useInformationSchema.getValueAsBoolean();
  }
  
  public boolean getUseLocalSessionState()
  {
    return useLocalSessionState.getValueAsBoolean();
  }
  
  public boolean getUseOldUTF8Behavior()
  {
    return useOldUTF8BehaviorAsBoolean;
  }
  
  public boolean getUseOnlyServerErrorMessages()
  {
    return useOnlyServerErrorMessages.getValueAsBoolean();
  }
  
  public boolean getUseReadAheadInput()
  {
    return useReadAheadInput.getValueAsBoolean();
  }
  
  public boolean getUseServerPreparedStmts()
  {
    return detectServerPreparedStmts.getValueAsBoolean();
  }
  
  public boolean getUseSqlStateCodes()
  {
    return useSqlStateCodes.getValueAsBoolean();
  }
  
  public boolean getUseSSL()
  {
    return useSSL.getValueAsBoolean();
  }
  
  public boolean isUseSSLExplicit()
  {
    return useSSL.wasExplicitlySet;
  }
  
  public boolean getUseStreamLengthsInPrepStmts()
  {
    return useStreamLengthsInPrepStmts.getValueAsBoolean();
  }
  
  public boolean getUseTimezone()
  {
    return useTimezone.getValueAsBoolean();
  }
  
  public boolean getUseUltraDevWorkAround()
  {
    return useUltraDevWorkAround.getValueAsBoolean();
  }
  
  public boolean getUseUnbufferedInput()
  {
    return useUnbufferedInput.getValueAsBoolean();
  }
  private IntegerConnectionProperty maxQuerySizeToLog;
  private IntegerConnectionProperty maxReconnects;
  private IntegerConnectionProperty retriesAllDown;
  private IntegerConnectionProperty maxRows;
  private int maxRowsAsInt;
  private IntegerConnectionProperty metadataCacheSize;
  private IntegerConnectionProperty netTimeoutForStreamingResults;
  private BooleanConnectionProperty noAccessToProcedureBodies;
  private BooleanConnectionProperty noDatetimeStringSync;
  private BooleanConnectionProperty noTimezoneConversionForTimeType;
  private BooleanConnectionProperty noTimezoneConversionForDateType;
  private BooleanConnectionProperty cacheDefaultTimezone;
  
  public boolean getUseUnicode()
  {
    return useUnicodeAsBoolean;
  }
  
  private BooleanConnectionProperty nullCatalogMeansCurrent;
  private BooleanConnectionProperty nullNamePatternMatchesAll;
  private IntegerConnectionProperty packetDebugBufferSize;
  private BooleanConnectionProperty padCharsWithSpace;
  private BooleanConnectionProperty paranoid;
  private BooleanConnectionProperty pedantic;
  private BooleanConnectionProperty pinGlobalTxToPhysicalConnection;
  private BooleanConnectionProperty populateInsertRowWithDefaultValues;
  private IntegerConnectionProperty preparedStatementCacheSize;
  private IntegerConnectionProperty preparedStatementCacheSqlLimit;
  private StringConnectionProperty parseInfoCacheFactory;
  
  public boolean getUseUsageAdvisor()
  {
    return useUsageAdvisorAsBoolean;
  }
  
  private BooleanConnectionProperty processEscapeCodesForPrepStmts;
  private StringConnectionProperty profilerEventHandler;
  private StringConnectionProperty profileSql;
  private BooleanConnectionProperty profileSQL;
  private boolean profileSQLAsBoolean;
  private StringConnectionProperty propertiesTransform;
  private IntegerConnectionProperty queriesBeforeRetryMaster;
  private BooleanConnectionProperty queryTimeoutKillsConnection;
  private BooleanConnectionProperty reconnectAtTxEnd;
  private boolean reconnectTxAtEndAsBoolean;
  private BooleanConnectionProperty relaxAutoCommit;
  
  public boolean getYearIsDateType()
  {
    return yearIsDateType.getValueAsBoolean();
  }
  
  private IntegerConnectionProperty reportMetricsIntervalMillis;
  private BooleanConnectionProperty requireSSL;
  private StringConnectionProperty resourceId;
  private IntegerConnectionProperty resultSetSizeThreshold;
  private BooleanConnectionProperty retainStatementAfterResultSetClose;
  private BooleanConnectionProperty rewriteBatchedStatements;
  private BooleanConnectionProperty rollbackOnPooledClose;
  private BooleanConnectionProperty roundRobinLoadBalance;
  private BooleanConnectionProperty runningCTS13;
  private IntegerConnectionProperty secondsBeforeRetryMaster;
  
  public String getZeroDateTimeBehavior()
  {
    return zeroDateTimeBehavior.getValueAsString();
  }
  
  private IntegerConnectionProperty selfDestructOnPingSecondsLifetime;
  private IntegerConnectionProperty selfDestructOnPingMaxOperations;
  private BooleanConnectionProperty replicationEnableJMX;
  private StringConnectionProperty serverTimezone;
  private StringConnectionProperty sessionVariables;
  private IntegerConnectionProperty slowQueryThresholdMillis;
  private LongConnectionProperty slowQueryThresholdNanos;
  private StringConnectionProperty socketFactoryClassName;
  private StringConnectionProperty socksProxyHost;
  private IntegerConnectionProperty socksProxyPort;
  
  protected void initializeFromRef(Reference ref)
    throws SQLException
  {
    int numPropertiesToSet = PROPERTY_LIST.size();
    
    for (int i = 0; i < numPropertiesToSet; i++) {
      Field propertyField = (Field)PROPERTY_LIST.get(i);
      try
      {
        ConnectionProperty propToSet = (ConnectionProperty)propertyField.get(this);
        
        if (ref != null) {
          propToSet.initializeFrom(ref, getExceptionInterceptor());
        }
      } catch (IllegalAccessException iae) {
        throw SQLError.createSQLException("Internal properties failure", "S1000", getExceptionInterceptor());
      }
    }
    
    postInitialization();
  }
  
  protected void initializeProperties(Properties info)
    throws SQLException
  {
    if (info != null)
    {
      String profileSqlLc = info.getProperty("profileSql");
      
      if (profileSqlLc != null) {
        info.put("profileSQL", profileSqlLc);
      }
      
      Properties infoCopy = (Properties)info.clone();
      
      infoCopy.remove("HOST");
      infoCopy.remove("user");
      infoCopy.remove("password");
      infoCopy.remove("DBNAME");
      infoCopy.remove("PORT");
      infoCopy.remove("profileSql");
      
      int numPropertiesToSet = PROPERTY_LIST.size();
      
      for (int i = 0; i < numPropertiesToSet; i++) {
        Field propertyField = (Field)PROPERTY_LIST.get(i);
        try
        {
          ConnectionProperty propToSet = (ConnectionProperty)propertyField.get(this);
          
          propToSet.initializeFrom(infoCopy, getExceptionInterceptor());
        } catch (IllegalAccessException iae) {
          throw SQLError.createSQLException(Messages.getString("ConnectionProperties.unableToInitDriverProperties") + iae.toString(), "S1000", getExceptionInterceptor());
        }
      }
      

      postInitialization();
    }
  }
  
  protected void postInitialization()
    throws SQLException
  {
    if (profileSql.getValueAsObject() != null) {
      profileSQL.initializeFrom(profileSql.getValueAsObject().toString(), getExceptionInterceptor());
    }
    
    reconnectTxAtEndAsBoolean = ((Boolean)reconnectAtTxEnd.getValueAsObject()).booleanValue();
    

    if (getMaxRows() == 0)
    {
      maxRows.setValueAsObject(Integer.valueOf(-1));
    }
    



    String testEncoding = (String)characterEncoding.getValueAsObject();
    
    if (testEncoding != null) {
      try
      {
        String testString = "abc";
        StringUtils.getBytes(testString, testEncoding);
      } catch (UnsupportedEncodingException UE) {
        throw SQLError.createSQLException(Messages.getString("ConnectionProperties.unsupportedCharacterEncoding", new Object[] { testEncoding }), "0S100", getExceptionInterceptor());
      }
    }
    



    if (((Boolean)cacheResultSetMetadata.getValueAsObject()).booleanValue()) {
      try {
        Class.forName("java.util.LinkedHashMap");
      } catch (ClassNotFoundException cnfe) {
        cacheResultSetMetadata.setValue(false);
      }
    }
    
    cacheResultSetMetaDataAsBoolean = cacheResultSetMetadata.getValueAsBoolean();
    useUnicodeAsBoolean = useUnicode.getValueAsBoolean();
    characterEncodingAsString = ((String)characterEncoding.getValueAsObject());
    highAvailabilityAsBoolean = autoReconnect.getValueAsBoolean();
    autoReconnectForPoolsAsBoolean = autoReconnectForPools.getValueAsBoolean();
    maxRowsAsInt = ((Integer)maxRows.getValueAsObject()).intValue();
    profileSQLAsBoolean = profileSQL.getValueAsBoolean();
    useUsageAdvisorAsBoolean = useUsageAdvisor.getValueAsBoolean();
    useOldUTF8BehaviorAsBoolean = useOldUTF8Behavior.getValueAsBoolean();
    autoGenerateTestcaseScriptAsBoolean = autoGenerateTestcaseScript.getValueAsBoolean();
    maintainTimeStatsAsBoolean = maintainTimeStats.getValueAsBoolean();
    jdbcCompliantTruncationForReads = getJdbcCompliantTruncation();
    
    if (getUseCursorFetch()) {
      setDetectServerPreparedStmts(true);
    }
  }
  
  public void setAllowLoadLocalInfile(boolean property)
  {
    allowLoadLocalInfile.setValue(property);
  }
  
  public void setAllowMultiQueries(boolean property)
  {
    allowMultiQueries.setValue(property);
  }
  
  public void setAllowNanAndInf(boolean flag)
  {
    allowNanAndInf.setValue(flag);
  }
  
  public void setAllowUrlInLocalInfile(boolean flag)
  {
    allowUrlInLocalInfile.setValue(flag);
  }
  
  public void setAlwaysSendSetIsolation(boolean flag)
  {
    alwaysSendSetIsolation.setValue(flag);
  }
  
  public void setAutoDeserialize(boolean flag)
  {
    autoDeserialize.setValue(flag);
  }
  
  public void setAutoGenerateTestcaseScript(boolean flag)
  {
    autoGenerateTestcaseScript.setValue(flag);
    autoGenerateTestcaseScriptAsBoolean = autoGenerateTestcaseScript.getValueAsBoolean();
  }
  
  public void setAutoReconnect(boolean flag)
  {
    autoReconnect.setValue(flag);
  }
  
  public void setAutoReconnectForConnectionPools(boolean property)
  {
    autoReconnectForPools.setValue(property);
    autoReconnectForPoolsAsBoolean = autoReconnectForPools.getValueAsBoolean();
  }
  
  public void setAutoReconnectForPools(boolean flag)
  {
    autoReconnectForPools.setValue(flag);
  }
  
  public void setBlobSendChunkSize(String value)
    throws SQLException
  {
    blobSendChunkSize.setValue(value, getExceptionInterceptor());
  }
  
  public void setCacheCallableStatements(boolean flag)
  {
    cacheCallableStatements.setValue(flag);
  }
  
  public void setCachePreparedStatements(boolean flag)
  {
    cachePreparedStatements.setValue(flag);
  }
  
  public void setCacheResultSetMetadata(boolean property)
  {
    cacheResultSetMetadata.setValue(property);
    cacheResultSetMetaDataAsBoolean = cacheResultSetMetadata.getValueAsBoolean();
  }
  
  public void setCacheServerConfiguration(boolean flag)
  {
    cacheServerConfiguration.setValue(flag);
  }
  
  public void setCallableStatementCacheSize(int size)
    throws SQLException
  {
    callableStatementCacheSize.setValue(size, getExceptionInterceptor());
  }
  
  public void setCapitalizeDBMDTypes(boolean property)
  {
    capitalizeTypeNames.setValue(property);
  }
  
  public void setCapitalizeTypeNames(boolean flag)
  {
    capitalizeTypeNames.setValue(flag);
  }
  
  public void setCharacterEncoding(String encoding)
  {
    characterEncoding.setValue(encoding);
  }
  
  public void setCharacterSetResults(String characterSet)
  {
    characterSetResults.setValue(characterSet);
  }
  
  public void setClobberStreamingResults(boolean flag)
  {
    clobberStreamingResults.setValue(flag);
  }
  
  public void setClobCharacterEncoding(String encoding)
  {
    clobCharacterEncoding.setValue(encoding);
  }
  
  public void setConnectionCollation(String collation)
  {
    connectionCollation.setValue(collation);
  }
  
  public void setConnectTimeout(int timeoutMs)
    throws SQLException
  {
    connectTimeout.setValue(timeoutMs, getExceptionInterceptor());
  }
  
  public void setContinueBatchOnError(boolean property)
  {
    continueBatchOnError.setValue(property);
  }
  
  public void setCreateDatabaseIfNotExist(boolean flag)
  {
    createDatabaseIfNotExist.setValue(flag);
  }
  
  public void setDefaultFetchSize(int n)
    throws SQLException
  {
    defaultFetchSize.setValue(n, getExceptionInterceptor());
  }
  
  public void setDetectServerPreparedStmts(boolean property)
  {
    detectServerPreparedStmts.setValue(property);
  }
  
  public void setDontTrackOpenResources(boolean flag)
  {
    dontTrackOpenResources.setValue(flag);
  }
  
  public void setDumpQueriesOnException(boolean flag)
  {
    dumpQueriesOnException.setValue(flag);
  }
  
  public void setDynamicCalendars(boolean flag)
  {
    dynamicCalendars.setValue(flag);
  }
  
  public void setElideSetAutoCommits(boolean flag)
  {
    elideSetAutoCommits.setValue(flag);
  }
  
  public void setEmptyStringsConvertToZero(boolean flag)
  {
    emptyStringsConvertToZero.setValue(flag);
  }
  
  public void setEmulateLocators(boolean property)
  {
    emulateLocators.setValue(property);
  }
  
  public void setEmulateUnsupportedPstmts(boolean flag)
  {
    emulateUnsupportedPstmts.setValue(flag);
  }
  
  public void setEnablePacketDebug(boolean flag)
  {
    enablePacketDebug.setValue(flag);
  }
  
  public void setEncoding(String property)
  {
    characterEncoding.setValue(property);
    characterEncodingAsString = characterEncoding.getValueAsString();
  }
  
  public void setExplainSlowQueries(boolean flag)
  {
    explainSlowQueries.setValue(flag);
  }
  
  public void setFailOverReadOnly(boolean flag)
  {
    failOverReadOnly.setValue(flag);
  }
  
  public void setGatherPerformanceMetrics(boolean flag)
  {
    gatherPerformanceMetrics.setValue(flag);
  }
  
  protected void setHighAvailability(boolean property)
  {
    autoReconnect.setValue(property);
    highAvailabilityAsBoolean = autoReconnect.getValueAsBoolean();
  }
  
  public void setHoldResultsOpenOverStatementClose(boolean flag)
  {
    holdResultsOpenOverStatementClose.setValue(flag);
  }
  
  public void setIgnoreNonTxTables(boolean property)
  {
    ignoreNonTxTables.setValue(property);
  }
  
  public void setInitialTimeout(int property)
    throws SQLException
  {
    initialTimeout.setValue(property, getExceptionInterceptor());
  }
  
  public void setIsInteractiveClient(boolean property)
  {
    isInteractiveClient.setValue(property);
  }
  
  public void setJdbcCompliantTruncation(boolean flag)
  {
    jdbcCompliantTruncation.setValue(flag);
  }
  
  public void setLocatorFetchBufferSize(String value)
    throws SQLException
  {
    locatorFetchBufferSize.setValue(value, getExceptionInterceptor());
  }
  
  public void setLogger(String property)
  {
    loggerClassName.setValueAsObject(property);
  }
  
  public void setLoggerClassName(String className)
  {
    loggerClassName.setValue(className);
  }
  
  public void setLogSlowQueries(boolean flag)
  {
    logSlowQueries.setValue(flag);
  }
  
  public void setMaintainTimeStats(boolean flag)
  {
    maintainTimeStats.setValue(flag);
    maintainTimeStatsAsBoolean = maintainTimeStats.getValueAsBoolean();
  }
  
  public void setMaxQuerySizeToLog(int sizeInBytes)
    throws SQLException
  {
    maxQuerySizeToLog.setValue(sizeInBytes, getExceptionInterceptor());
  }
  
  public void setMaxReconnects(int property)
    throws SQLException
  {
    maxReconnects.setValue(property, getExceptionInterceptor());
  }
  
  public void setMaxRows(int property)
    throws SQLException
  {
    maxRows.setValue(property, getExceptionInterceptor());
    maxRowsAsInt = maxRows.getValueAsInt();
  }
  
  public void setMetadataCacheSize(int value)
    throws SQLException
  {
    metadataCacheSize.setValue(value, getExceptionInterceptor());
  }
  
  public void setNoDatetimeStringSync(boolean flag)
  {
    noDatetimeStringSync.setValue(flag);
  }
  
  public void setNullCatalogMeansCurrent(boolean value)
  {
    nullCatalogMeansCurrent.setValue(value);
  }
  
  public void setNullNamePatternMatchesAll(boolean value)
  {
    nullNamePatternMatchesAll.setValue(value);
  }
  
  public void setPacketDebugBufferSize(int size)
    throws SQLException
  {
    packetDebugBufferSize.setValue(size, getExceptionInterceptor());
  }
  
  public void setParanoid(boolean property)
  {
    paranoid.setValue(property);
  }
  
  public void setPedantic(boolean property)
  {
    pedantic.setValue(property);
  }
  
  public void setPreparedStatementCacheSize(int cacheSize)
    throws SQLException
  {
    preparedStatementCacheSize.setValue(cacheSize, getExceptionInterceptor());
  }
  
  public void setPreparedStatementCacheSqlLimit(int cacheSqlLimit)
    throws SQLException
  {
    preparedStatementCacheSqlLimit.setValue(cacheSqlLimit, getExceptionInterceptor());
  }
  
  public void setProfileSql(boolean property)
  {
    profileSQL.setValue(property);
    profileSQLAsBoolean = profileSQL.getValueAsBoolean();
  }
  
  public void setProfileSQL(boolean flag)
  {
    profileSQL.setValue(flag);
  }
  
  public void setPropertiesTransform(String value)
  {
    propertiesTransform.setValue(value);
  }
  
  public void setQueriesBeforeRetryMaster(int property)
    throws SQLException
  {
    queriesBeforeRetryMaster.setValue(property, getExceptionInterceptor());
  }
  
  public void setReconnectAtTxEnd(boolean property)
  {
    reconnectAtTxEnd.setValue(property);
    reconnectTxAtEndAsBoolean = reconnectAtTxEnd.getValueAsBoolean();
  }
  
  public void setRelaxAutoCommit(boolean property)
  {
    relaxAutoCommit.setValue(property);
  }
  
  public void setReportMetricsIntervalMillis(int millis)
    throws SQLException
  {
    reportMetricsIntervalMillis.setValue(millis, getExceptionInterceptor());
  }
  
  public void setRequireSSL(boolean property)
  {
    requireSSL.setValue(property);
  }
  
  public void setRetainStatementAfterResultSetClose(boolean flag)
  {
    retainStatementAfterResultSetClose.setValue(flag);
  }
  
  public void setRollbackOnPooledClose(boolean flag)
  {
    rollbackOnPooledClose.setValue(flag);
  }
  
  public void setRoundRobinLoadBalance(boolean flag)
  {
    roundRobinLoadBalance.setValue(flag);
  }
  
  public void setRunningCTS13(boolean flag)
  {
    runningCTS13.setValue(flag);
  }
  
  public void setSecondsBeforeRetryMaster(int property)
    throws SQLException
  {
    secondsBeforeRetryMaster.setValue(property, getExceptionInterceptor());
  }
  
  public void setServerTimezone(String property)
  {
    serverTimezone.setValue(property);
  }
  
  public void setSessionVariables(String variables)
  {
    sessionVariables.setValue(variables);
  }
  
  public void setSlowQueryThresholdMillis(int millis)
    throws SQLException
  {
    slowQueryThresholdMillis.setValue(millis, getExceptionInterceptor());
  }
  
  public void setSocketFactoryClassName(String property)
  {
    socketFactoryClassName.setValue(property);
  }
  
  public void setSocketTimeout(int property)
    throws SQLException
  {
    socketTimeout.setValue(property, getExceptionInterceptor());
  }
  
  public void setStrictFloatingPoint(boolean property)
  {
    strictFloatingPoint.setValue(property);
  }
  
  public void setStrictUpdates(boolean property)
  {
    strictUpdates.setValue(property);
  }
  
  public void setTinyInt1isBit(boolean flag)
  {
    tinyInt1isBit.setValue(flag);
  }
  
  public void setTraceProtocol(boolean flag)
  {
    traceProtocol.setValue(flag);
  }
  
  public void setTransformedBitIsBoolean(boolean flag)
  {
    transformedBitIsBoolean.setValue(flag);
  }
  
  public void setUseCompression(boolean property)
  {
    useCompression.setValue(property);
  }
  
  public void setUseFastIntParsing(boolean flag)
  {
    useFastIntParsing.setValue(flag);
  }
  
  public void setUseHostsInPrivileges(boolean property)
  {
    useHostsInPrivileges.setValue(property);
  }
  
  public void setUseInformationSchema(boolean flag)
  {
    useInformationSchema.setValue(flag);
  }
  
  public void setUseLocalSessionState(boolean flag)
  {
    useLocalSessionState.setValue(flag);
  }
  
  public void setUseOldUTF8Behavior(boolean flag)
  {
    useOldUTF8Behavior.setValue(flag);
    useOldUTF8BehaviorAsBoolean = useOldUTF8Behavior.getValueAsBoolean();
  }
  
  public void setUseOnlyServerErrorMessages(boolean flag)
  {
    useOnlyServerErrorMessages.setValue(flag);
  }
  
  public void setUseReadAheadInput(boolean flag)
  {
    useReadAheadInput.setValue(flag);
  }
  
  public void setUseServerPreparedStmts(boolean flag)
  {
    detectServerPreparedStmts.setValue(flag);
  }
  
  public void setUseSqlStateCodes(boolean flag)
  {
    useSqlStateCodes.setValue(flag);
  }
  
  public void setUseSSL(boolean property)
  {
    useSSL.setValue(property);
  }
  
  public void setUseStreamLengthsInPrepStmts(boolean property)
  {
    useStreamLengthsInPrepStmts.setValue(property);
  }
  
  public void setUseTimezone(boolean property)
  {
    useTimezone.setValue(property);
  }
  
  public void setUseUltraDevWorkAround(boolean property)
  {
    useUltraDevWorkAround.setValue(property);
  }
  
  public void setUseUnbufferedInput(boolean flag)
  {
    useUnbufferedInput.setValue(flag);
  }
  
  public void setUseUnicode(boolean flag)
  {
    useUnicode.setValue(flag);
    useUnicodeAsBoolean = useUnicode.getValueAsBoolean();
  }
  
  public void setUseUsageAdvisor(boolean useUsageAdvisorFlag)
  {
    useUsageAdvisor.setValue(useUsageAdvisorFlag);
    useUsageAdvisorAsBoolean = useUsageAdvisor.getValueAsBoolean();
  }
  
  public void setYearIsDateType(boolean flag)
  {
    yearIsDateType.setValue(flag);
  }
  
  public void setZeroDateTimeBehavior(String behavior)
  {
    zeroDateTimeBehavior.setValue(behavior);
  }
  
  protected void storeToRef(Reference ref)
    throws SQLException
  {
    int numPropertiesToSet = PROPERTY_LIST.size();
    
    for (int i = 0; i < numPropertiesToSet; i++)
    {
      Field propertyField = (Field)PROPERTY_LIST.get(i);
      try
      {
        ConnectionProperty propToStore = (ConnectionProperty)propertyField.get(this);
        
        if (ref != null) {
          propToStore.storeTo(ref);
        }
      }
      catch (IllegalAccessException iae)
      {
        throw SQLError.createSQLException(Messages.getString("ConnectionProperties.errorNotExpected"), getExceptionInterceptor());
      }
    }
  }
  
  public boolean useUnbufferedInput()
  {
    return useUnbufferedInput.getValueAsBoolean();
  }
  
  public boolean getUseCursorFetch()
  {
    return useCursorFetch.getValueAsBoolean();
  }
  
  public void setUseCursorFetch(boolean flag)
  {
    useCursorFetch.setValue(flag);
  }
  
  public boolean getOverrideSupportsIntegrityEnhancementFacility()
  {
    return overrideSupportsIntegrityEnhancementFacility.getValueAsBoolean();
  }
  
  public void setOverrideSupportsIntegrityEnhancementFacility(boolean flag)
  {
    overrideSupportsIntegrityEnhancementFacility.setValue(flag);
  }
  
  public boolean getNoTimezoneConversionForTimeType()
  {
    return noTimezoneConversionForTimeType.getValueAsBoolean();
  }
  
  public void setNoTimezoneConversionForTimeType(boolean flag)
  {
    noTimezoneConversionForTimeType.setValue(flag);
  }
  
  public boolean getNoTimezoneConversionForDateType()
  {
    return noTimezoneConversionForDateType.getValueAsBoolean();
  }
  
  public void setNoTimezoneConversionForDateType(boolean flag)
  {
    noTimezoneConversionForDateType.setValue(flag);
  }
  
  public boolean getCacheDefaultTimezone()
  {
    return cacheDefaultTimezone.getValueAsBoolean();
  }
  
  public void setCacheDefaultTimezone(boolean flag)
  {
    cacheDefaultTimezone.setValue(flag);
  }
  
  public boolean getUseJDBCCompliantTimezoneShift()
  {
    return useJDBCCompliantTimezoneShift.getValueAsBoolean();
  }
  
  public void setUseJDBCCompliantTimezoneShift(boolean flag)
  {
    useJDBCCompliantTimezoneShift.setValue(flag);
  }
  
  public boolean getAutoClosePStmtStreams()
  {
    return autoClosePStmtStreams.getValueAsBoolean();
  }
  
  public void setAutoClosePStmtStreams(boolean flag)
  {
    autoClosePStmtStreams.setValue(flag);
  }
  
  public boolean getProcessEscapeCodesForPrepStmts()
  {
    return processEscapeCodesForPrepStmts.getValueAsBoolean();
  }
  
  public void setProcessEscapeCodesForPrepStmts(boolean flag)
  {
    processEscapeCodesForPrepStmts.setValue(flag);
  }
  
  public boolean getUseGmtMillisForDatetimes()
  {
    return useGmtMillisForDatetimes.getValueAsBoolean();
  }
  
  public void setUseGmtMillisForDatetimes(boolean flag)
  {
    useGmtMillisForDatetimes.setValue(flag);
  }
  
  public boolean getDumpMetadataOnColumnNotFound()
  {
    return dumpMetadataOnColumnNotFound.getValueAsBoolean();
  }
  
  public void setDumpMetadataOnColumnNotFound(boolean flag)
  {
    dumpMetadataOnColumnNotFound.setValue(flag);
  }
  
  public String getResourceId()
  {
    return resourceId.getValueAsString();
  }
  
  public void setResourceId(String resourceId)
  {
    this.resourceId.setValue(resourceId);
  }
  
  public boolean getRewriteBatchedStatements()
  {
    return rewriteBatchedStatements.getValueAsBoolean();
  }
  
  public void setRewriteBatchedStatements(boolean flag)
  {
    rewriteBatchedStatements.setValue(flag);
  }
  
  public boolean getJdbcCompliantTruncationForReads()
  {
    return jdbcCompliantTruncationForReads;
  }
  
  public void setJdbcCompliantTruncationForReads(boolean jdbcCompliantTruncationForReads)
  {
    this.jdbcCompliantTruncationForReads = jdbcCompliantTruncationForReads;
  }
  private IntegerConnectionProperty socketTimeout;
  private StringConnectionProperty statementInterceptors;
  private BooleanConnectionProperty strictFloatingPoint;
  private BooleanConnectionProperty strictUpdates;
  private BooleanConnectionProperty overrideSupportsIntegrityEnhancementFacility;
  private BooleanConnectionProperty tcpNoDelay;
  private BooleanConnectionProperty tcpKeepAlive;
  private IntegerConnectionProperty tcpRcvBuf;
  private IntegerConnectionProperty tcpSndBuf;
  private IntegerConnectionProperty tcpTrafficClass;
  private BooleanConnectionProperty tinyInt1isBit;
  protected BooleanConnectionProperty traceProtocol;
  private BooleanConnectionProperty treatUtilDateAsTimestamp;
  private BooleanConnectionProperty transformedBitIsBoolean;
  private BooleanConnectionProperty useBlobToStoreUTF8OutsideBMP;
  private StringConnectionProperty utf8OutsideBmpExcludedColumnNamePattern;
  private StringConnectionProperty utf8OutsideBmpIncludedColumnNamePattern;
  private BooleanConnectionProperty useCompression;
  private BooleanConnectionProperty useColumnNamesInFindColumn;
  
  public boolean getUseJvmCharsetConverters()
  {
    return useJvmCharsetConverters.getValueAsBoolean();
  }
  
  private StringConnectionProperty useConfigs;
  private BooleanConnectionProperty useCursorFetch;
  private BooleanConnectionProperty useDynamicCharsetInfo;
  private BooleanConnectionProperty useDirectRowUnpack;
  private BooleanConnectionProperty useFastIntParsing;
  private BooleanConnectionProperty useFastDateParsing;
  private BooleanConnectionProperty useHostsInPrivileges;
  private BooleanConnectionProperty useInformationSchema;
  private BooleanConnectionProperty useJDBCCompliantTimezoneShift;
  
  public void setUseJvmCharsetConverters(boolean flag)
  {
    useJvmCharsetConverters.setValue(flag); }
  
  private BooleanConnectionProperty useLocalSessionState;
  private BooleanConnectionProperty useLocalTransactionState;
  private BooleanConnectionProperty useLegacyDatetimeCode;
  private BooleanConnectionProperty sendFractionalSeconds;
  private BooleanConnectionProperty useNanosForElapsedTime;
  private BooleanConnectionProperty useOldAliasMetadataBehavior;
  private BooleanConnectionProperty useOldUTF8Behavior; private boolean useOldUTF8BehaviorAsBoolean; private BooleanConnectionProperty useOnlyServerErrorMessages;
  public boolean getPinGlobalTxToPhysicalConnection() { return pinGlobalTxToPhysicalConnection.getValueAsBoolean(); }
  
  private BooleanConnectionProperty useReadAheadInput;
  private BooleanConnectionProperty useSqlStateCodes;
  private BooleanConnectionProperty useSSL;
  private BooleanConnectionProperty useSSPSCompatibleTimezoneShift;
  private BooleanConnectionProperty useStreamLengthsInPrepStmts;
  private BooleanConnectionProperty useTimezone;
  private BooleanConnectionProperty useUltraDevWorkAround; private BooleanConnectionProperty useUnbufferedInput; private BooleanConnectionProperty useUnicode;
  public void setPinGlobalTxToPhysicalConnection(boolean flag) { pinGlobalTxToPhysicalConnection.setValue(flag); }
  
  private boolean useUnicodeAsBoolean;
  private BooleanConnectionProperty useUsageAdvisor;
  private boolean useUsageAdvisorAsBoolean;
  private BooleanConnectionProperty yearIsDateType;
  private StringConnectionProperty zeroDateTimeBehavior;
  private BooleanConnectionProperty useJvmCharsetConverters;
  private BooleanConnectionProperty useGmtMillisForDatetimes;
  private BooleanConnectionProperty dumpMetadataOnColumnNotFound;
  private StringConnectionProperty clientCertificateKeyStoreUrl;
  private StringConnectionProperty trustCertificateKeyStoreUrl;
  
  public void setGatherPerfMetrics(boolean flag) {
    setGatherPerformanceMetrics(flag); }
  
  private StringConnectionProperty clientCertificateKeyStoreType;
  private StringConnectionProperty clientCertificateKeyStorePassword;
  private StringConnectionProperty trustCertificateKeyStoreType;
  private StringConnectionProperty trustCertificateKeyStorePassword;
  private BooleanConnectionProperty verifyServerCertificate;
  private BooleanConnectionProperty useAffectedRows;
  private StringConnectionProperty passwordCharacterEncoding;
  public boolean getGatherPerfMetrics() { return getGatherPerformanceMetrics(); }
  
  private IntegerConnectionProperty maxAllowedPacket;
  private StringConnectionProperty authenticationPlugins;
  private StringConnectionProperty disabledAuthenticationPlugins;
  private StringConnectionProperty defaultAuthenticationPlugin;
  private BooleanConnectionProperty disconnectOnExpiredPasswords;
  private BooleanConnectionProperty getProceduresReturnsFunctions;
  private BooleanConnectionProperty detectCustomCollations;
  private StringConnectionProperty serverRSAPublicKeyFile; private BooleanConnectionProperty allowPublicKeyRetrieval; private BooleanConnectionProperty dontCheckOnDuplicateKeyUpdateInSQL; private BooleanConnectionProperty readOnlyPropagatesToServer; private StringConnectionProperty enabledSSLCipherSuites; private StringConnectionProperty enabledTLSProtocols; private BooleanConnectionProperty enableEscapeProcessing; public void setUltraDevHack(boolean flag) { setUseUltraDevWorkAround(flag); }
  





  public boolean getUltraDevHack()
  {
    return getUseUltraDevWorkAround();
  }
  




  public void setInteractiveClient(boolean property)
  {
    setIsInteractiveClient(property);
  }
  




  public void setSocketFactory(String name)
  {
    setSocketFactoryClassName(name);
  }
  




  public String getSocketFactory()
  {
    return getSocketFactoryClassName();
  }
  




  public void setUseServerPrepStmts(boolean flag)
  {
    setUseServerPreparedStmts(flag);
  }
  




  public boolean getUseServerPrepStmts()
  {
    return getUseServerPreparedStmts();
  }
  




  public void setCacheCallableStmts(boolean flag)
  {
    setCacheCallableStatements(flag);
  }
  




  public boolean getCacheCallableStmts()
  {
    return getCacheCallableStatements();
  }
  




  public void setCachePrepStmts(boolean flag)
  {
    setCachePreparedStatements(flag);
  }
  




  public boolean getCachePrepStmts()
  {
    return getCachePreparedStatements();
  }
  



  public void setCallableStmtCacheSize(int cacheSize)
    throws SQLException
  {
    setCallableStatementCacheSize(cacheSize);
  }
  




  public int getCallableStmtCacheSize()
  {
    return getCallableStatementCacheSize();
  }
  



  public void setPrepStmtCacheSize(int cacheSize)
    throws SQLException
  {
    setPreparedStatementCacheSize(cacheSize);
  }
  




  public int getPrepStmtCacheSize()
  {
    return getPreparedStatementCacheSize();
  }
  



  public void setPrepStmtCacheSqlLimit(int sqlLimit)
    throws SQLException
  {
    setPreparedStatementCacheSqlLimit(sqlLimit);
  }
  




  public int getPrepStmtCacheSqlLimit()
  {
    return getPreparedStatementCacheSqlLimit();
  }
  




  public boolean getNoAccessToProcedureBodies()
  {
    return noAccessToProcedureBodies.getValueAsBoolean();
  }
  




  public void setNoAccessToProcedureBodies(boolean flag)
  {
    noAccessToProcedureBodies.setValue(flag);
  }
  




  public boolean getUseOldAliasMetadataBehavior()
  {
    return useOldAliasMetadataBehavior.getValueAsBoolean();
  }
  




  public void setUseOldAliasMetadataBehavior(boolean flag)
  {
    useOldAliasMetadataBehavior.setValue(flag);
  }
  




  public String getClientCertificateKeyStorePassword()
  {
    return clientCertificateKeyStorePassword.getValueAsString();
  }
  




  public void setClientCertificateKeyStorePassword(String value)
  {
    clientCertificateKeyStorePassword.setValue(value);
  }
  




  public String getClientCertificateKeyStoreType()
  {
    return clientCertificateKeyStoreType.getValueAsString();
  }
  




  public void setClientCertificateKeyStoreType(String value)
  {
    clientCertificateKeyStoreType.setValue(value);
  }
  




  public String getClientCertificateKeyStoreUrl()
  {
    return clientCertificateKeyStoreUrl.getValueAsString();
  }
  




  public void setClientCertificateKeyStoreUrl(String value)
  {
    clientCertificateKeyStoreUrl.setValue(value);
  }
  




  public String getTrustCertificateKeyStorePassword()
  {
    return trustCertificateKeyStorePassword.getValueAsString();
  }
  




  public void setTrustCertificateKeyStorePassword(String value)
  {
    trustCertificateKeyStorePassword.setValue(value);
  }
  




  public String getTrustCertificateKeyStoreType()
  {
    return trustCertificateKeyStoreType.getValueAsString();
  }
  




  public void setTrustCertificateKeyStoreType(String value)
  {
    trustCertificateKeyStoreType.setValue(value);
  }
  




  public String getTrustCertificateKeyStoreUrl()
  {
    return trustCertificateKeyStoreUrl.getValueAsString();
  }
  




  public void setTrustCertificateKeyStoreUrl(String value)
  {
    trustCertificateKeyStoreUrl.setValue(value);
  }
  




  public boolean getUseSSPSCompatibleTimezoneShift()
  {
    return useSSPSCompatibleTimezoneShift.getValueAsBoolean();
  }
  




  public void setUseSSPSCompatibleTimezoneShift(boolean flag)
  {
    useSSPSCompatibleTimezoneShift.setValue(flag);
  }
  




  public boolean getTreatUtilDateAsTimestamp()
  {
    return treatUtilDateAsTimestamp.getValueAsBoolean();
  }
  




  public void setTreatUtilDateAsTimestamp(boolean flag)
  {
    treatUtilDateAsTimestamp.setValue(flag);
  }
  




  public boolean getUseFastDateParsing()
  {
    return useFastDateParsing.getValueAsBoolean();
  }
  




  public void setUseFastDateParsing(boolean flag)
  {
    useFastDateParsing.setValue(flag);
  }
  




  public String getLocalSocketAddress()
  {
    return localSocketAddress.getValueAsString();
  }
  




  public void setLocalSocketAddress(String address)
  {
    localSocketAddress.setValue(address);
  }
  




  public void setUseConfigs(String configs)
  {
    useConfigs.setValue(configs);
  }
  




  public String getUseConfigs()
  {
    return useConfigs.getValueAsString();
  }
  




  public boolean getGenerateSimpleParameterMetadata()
  {
    return generateSimpleParameterMetadata.getValueAsBoolean();
  }
  




  public void setGenerateSimpleParameterMetadata(boolean flag)
  {
    generateSimpleParameterMetadata.setValue(flag);
  }
  




  public boolean getLogXaCommands()
  {
    return logXaCommands.getValueAsBoolean();
  }
  




  public void setLogXaCommands(boolean flag)
  {
    logXaCommands.setValue(flag);
  }
  




  public int getResultSetSizeThreshold()
  {
    return resultSetSizeThreshold.getValueAsInt();
  }
  



  public void setResultSetSizeThreshold(int threshold)
    throws SQLException
  {
    resultSetSizeThreshold.setValue(threshold, getExceptionInterceptor());
  }
  




  public int getNetTimeoutForStreamingResults()
  {
    return netTimeoutForStreamingResults.getValueAsInt();
  }
  



  public void setNetTimeoutForStreamingResults(int value)
    throws SQLException
  {
    netTimeoutForStreamingResults.setValue(value, getExceptionInterceptor());
  }
  




  public boolean getEnableQueryTimeouts()
  {
    return enableQueryTimeouts.getValueAsBoolean();
  }
  




  public void setEnableQueryTimeouts(boolean flag)
  {
    enableQueryTimeouts.setValue(flag);
  }
  




  public boolean getPadCharsWithSpace()
  {
    return padCharsWithSpace.getValueAsBoolean();
  }
  




  public void setPadCharsWithSpace(boolean flag)
  {
    padCharsWithSpace.setValue(flag);
  }
  




  public boolean getUseDynamicCharsetInfo()
  {
    return useDynamicCharsetInfo.getValueAsBoolean();
  }
  




  public void setUseDynamicCharsetInfo(boolean flag)
  {
    useDynamicCharsetInfo.setValue(flag);
  }
  




  public String getClientInfoProvider()
  {
    return clientInfoProvider.getValueAsString();
  }
  




  public void setClientInfoProvider(String classname)
  {
    clientInfoProvider.setValue(classname);
  }
  
  public boolean getPopulateInsertRowWithDefaultValues() {
    return populateInsertRowWithDefaultValues.getValueAsBoolean();
  }
  
  public void setPopulateInsertRowWithDefaultValues(boolean flag) {
    populateInsertRowWithDefaultValues.setValue(flag);
  }
  
  public String getLoadBalanceStrategy() {
    return loadBalanceStrategy.getValueAsString();
  }
  
  public void setLoadBalanceStrategy(String strategy) {
    loadBalanceStrategy.setValue(strategy);
  }
  
  public String getServerAffinityOrder() {
    return serverAffinityOrder.getValueAsString();
  }
  
  public void setServerAffinityOrder(String hostsList) {
    serverAffinityOrder.setValue(hostsList);
  }
  
  public boolean getTcpNoDelay() {
    return tcpNoDelay.getValueAsBoolean();
  }
  
  public void setTcpNoDelay(boolean flag) {
    tcpNoDelay.setValue(flag);
  }
  
  public boolean getTcpKeepAlive() {
    return tcpKeepAlive.getValueAsBoolean();
  }
  
  public void setTcpKeepAlive(boolean flag) {
    tcpKeepAlive.setValue(flag);
  }
  
  public int getTcpRcvBuf() {
    return tcpRcvBuf.getValueAsInt();
  }
  
  public void setTcpRcvBuf(int bufSize) throws SQLException {
    tcpRcvBuf.setValue(bufSize, getExceptionInterceptor());
  }
  
  public int getTcpSndBuf() {
    return tcpSndBuf.getValueAsInt();
  }
  
  public void setTcpSndBuf(int bufSize) throws SQLException {
    tcpSndBuf.setValue(bufSize, getExceptionInterceptor());
  }
  
  public int getTcpTrafficClass() {
    return tcpTrafficClass.getValueAsInt();
  }
  
  public void setTcpTrafficClass(int classFlags) throws SQLException {
    tcpTrafficClass.setValue(classFlags, getExceptionInterceptor());
  }
  
  public boolean getUseNanosForElapsedTime() {
    return useNanosForElapsedTime.getValueAsBoolean();
  }
  
  public void setUseNanosForElapsedTime(boolean flag) {
    useNanosForElapsedTime.setValue(flag);
  }
  
  public long getSlowQueryThresholdNanos() {
    return slowQueryThresholdNanos.getValueAsLong();
  }
  
  public void setSlowQueryThresholdNanos(long nanos) throws SQLException {
    slowQueryThresholdNanos.setValue(nanos, getExceptionInterceptor());
  }
  
  public String getStatementInterceptors() {
    return statementInterceptors.getValueAsString();
  }
  
  public void setStatementInterceptors(String value) {
    statementInterceptors.setValue(value);
  }
  
  public boolean getUseDirectRowUnpack() {
    return useDirectRowUnpack.getValueAsBoolean();
  }
  
  public void setUseDirectRowUnpack(boolean flag) {
    useDirectRowUnpack.setValue(flag);
  }
  
  public String getLargeRowSizeThreshold() {
    return largeRowSizeThreshold.getValueAsString();
  }
  
  public void setLargeRowSizeThreshold(String value) throws SQLException {
    largeRowSizeThreshold.setValue(value, getExceptionInterceptor());
  }
  
  public boolean getUseBlobToStoreUTF8OutsideBMP() {
    return useBlobToStoreUTF8OutsideBMP.getValueAsBoolean();
  }
  
  public void setUseBlobToStoreUTF8OutsideBMP(boolean flag) {
    useBlobToStoreUTF8OutsideBMP.setValue(flag);
  }
  
  public String getUtf8OutsideBmpExcludedColumnNamePattern() {
    return utf8OutsideBmpExcludedColumnNamePattern.getValueAsString();
  }
  
  public void setUtf8OutsideBmpExcludedColumnNamePattern(String regexPattern) {
    utf8OutsideBmpExcludedColumnNamePattern.setValue(regexPattern);
  }
  
  public String getUtf8OutsideBmpIncludedColumnNamePattern() {
    return utf8OutsideBmpIncludedColumnNamePattern.getValueAsString();
  }
  
  public void setUtf8OutsideBmpIncludedColumnNamePattern(String regexPattern) {
    utf8OutsideBmpIncludedColumnNamePattern.setValue(regexPattern);
  }
  
  public boolean getIncludeInnodbStatusInDeadlockExceptions() {
    return includeInnodbStatusInDeadlockExceptions.getValueAsBoolean();
  }
  
  public void setIncludeInnodbStatusInDeadlockExceptions(boolean flag) {
    includeInnodbStatusInDeadlockExceptions.setValue(flag);
  }
  
  public boolean getBlobsAreStrings() {
    return blobsAreStrings.getValueAsBoolean();
  }
  
  public void setBlobsAreStrings(boolean flag) {
    blobsAreStrings.setValue(flag);
  }
  
  public boolean getFunctionsNeverReturnBlobs() {
    return functionsNeverReturnBlobs.getValueAsBoolean();
  }
  
  public void setFunctionsNeverReturnBlobs(boolean flag) {
    functionsNeverReturnBlobs.setValue(flag);
  }
  
  public boolean getAutoSlowLog() {
    return autoSlowLog.getValueAsBoolean();
  }
  
  public void setAutoSlowLog(boolean flag) {
    autoSlowLog.setValue(flag);
  }
  
  public String getConnectionLifecycleInterceptors() {
    return connectionLifecycleInterceptors.getValueAsString();
  }
  
  public void setConnectionLifecycleInterceptors(String interceptors) {
    connectionLifecycleInterceptors.setValue(interceptors);
  }
  
  public String getProfilerEventHandler() {
    return profilerEventHandler.getValueAsString();
  }
  
  public void setProfilerEventHandler(String handler) {
    profilerEventHandler.setValue(handler);
  }
  
  public boolean getVerifyServerCertificate() {
    return verifyServerCertificate.getValueAsBoolean();
  }
  
  public void setVerifyServerCertificate(boolean flag) {
    verifyServerCertificate.setValue(flag);
  }
  
  public boolean getUseLegacyDatetimeCode() {
    return useLegacyDatetimeCode.getValueAsBoolean();
  }
  
  public void setUseLegacyDatetimeCode(boolean flag) {
    useLegacyDatetimeCode.setValue(flag);
  }
  
  public boolean getSendFractionalSeconds() {
    return sendFractionalSeconds.getValueAsBoolean();
  }
  
  public void setSendFractionalSeconds(boolean flag) {
    sendFractionalSeconds.setValue(flag);
  }
  
  public int getSelfDestructOnPingSecondsLifetime() {
    return selfDestructOnPingSecondsLifetime.getValueAsInt();
  }
  
  public void setSelfDestructOnPingSecondsLifetime(int seconds) throws SQLException {
    selfDestructOnPingSecondsLifetime.setValue(seconds, getExceptionInterceptor());
  }
  
  public int getSelfDestructOnPingMaxOperations() {
    return selfDestructOnPingMaxOperations.getValueAsInt();
  }
  
  public void setSelfDestructOnPingMaxOperations(int maxOperations) throws SQLException {
    selfDestructOnPingMaxOperations.setValue(maxOperations, getExceptionInterceptor());
  }
  
  public boolean getUseColumnNamesInFindColumn() {
    return useColumnNamesInFindColumn.getValueAsBoolean();
  }
  
  public void setUseColumnNamesInFindColumn(boolean flag) {
    useColumnNamesInFindColumn.setValue(flag);
  }
  
  public boolean getUseLocalTransactionState() {
    return useLocalTransactionState.getValueAsBoolean();
  }
  
  public void setUseLocalTransactionState(boolean flag) {
    useLocalTransactionState.setValue(flag);
  }
  
  public boolean getCompensateOnDuplicateKeyUpdateCounts() {
    return compensateOnDuplicateKeyUpdateCounts.getValueAsBoolean();
  }
  
  public void setCompensateOnDuplicateKeyUpdateCounts(boolean flag) {
    compensateOnDuplicateKeyUpdateCounts.setValue(flag);
  }
  
  public int getLoadBalanceBlacklistTimeout() {
    return loadBalanceBlacklistTimeout.getValueAsInt();
  }
  
  public void setLoadBalanceBlacklistTimeout(int loadBalanceBlacklistTimeout) throws SQLException {
    this.loadBalanceBlacklistTimeout.setValue(loadBalanceBlacklistTimeout, getExceptionInterceptor());
  }
  
  public int getLoadBalancePingTimeout() {
    return loadBalancePingTimeout.getValueAsInt();
  }
  
  public void setLoadBalancePingTimeout(int loadBalancePingTimeout) throws SQLException {
    this.loadBalancePingTimeout.setValue(loadBalancePingTimeout, getExceptionInterceptor());
  }
  
  public void setRetriesAllDown(int retriesAllDown) throws SQLException {
    this.retriesAllDown.setValue(retriesAllDown, getExceptionInterceptor());
  }
  
  public int getRetriesAllDown() {
    return retriesAllDown.getValueAsInt();
  }
  
  public void setUseAffectedRows(boolean flag) {
    useAffectedRows.setValue(flag);
  }
  
  public boolean getUseAffectedRows() {
    return useAffectedRows.getValueAsBoolean();
  }
  
  public void setPasswordCharacterEncoding(String characterSet) {
    passwordCharacterEncoding.setValue(characterSet);
  }
  
  public String getPasswordCharacterEncoding() {
    String encoding;
    if ((encoding = passwordCharacterEncoding.getValueAsString()) != null) {
      return encoding;
    }
    if ((getUseUnicode()) && ((encoding = getEncoding()) != null)) {
      return encoding;
    }
    return "UTF-8";
  }
  
  public void setExceptionInterceptors(String exceptionInterceptors) {
    this.exceptionInterceptors.setValue(exceptionInterceptors);
  }
  
  public String getExceptionInterceptors() {
    return exceptionInterceptors.getValueAsString();
  }
  
  public void setMaxAllowedPacket(int max) throws SQLException {
    maxAllowedPacket.setValue(max, getExceptionInterceptor());
  }
  
  public int getMaxAllowedPacket() {
    return maxAllowedPacket.getValueAsInt();
  }
  
  public boolean getQueryTimeoutKillsConnection() {
    return queryTimeoutKillsConnection.getValueAsBoolean();
  }
  
  public void setQueryTimeoutKillsConnection(boolean queryTimeoutKillsConnection) {
    this.queryTimeoutKillsConnection.setValue(queryTimeoutKillsConnection);
  }
  
  public boolean getLoadBalanceValidateConnectionOnSwapServer() {
    return loadBalanceValidateConnectionOnSwapServer.getValueAsBoolean();
  }
  
  public void setLoadBalanceValidateConnectionOnSwapServer(boolean loadBalanceValidateConnectionOnSwapServer) {
    this.loadBalanceValidateConnectionOnSwapServer.setValue(loadBalanceValidateConnectionOnSwapServer);
  }
  
  public String getLoadBalanceConnectionGroup() {
    return loadBalanceConnectionGroup.getValueAsString();
  }
  
  public void setLoadBalanceConnectionGroup(String loadBalanceConnectionGroup) {
    this.loadBalanceConnectionGroup.setValue(loadBalanceConnectionGroup);
  }
  
  public String getLoadBalanceExceptionChecker() {
    return loadBalanceExceptionChecker.getValueAsString();
  }
  
  public void setLoadBalanceExceptionChecker(String loadBalanceExceptionChecker) {
    this.loadBalanceExceptionChecker.setValue(loadBalanceExceptionChecker);
  }
  
  public String getLoadBalanceSQLStateFailover() {
    return loadBalanceSQLStateFailover.getValueAsString();
  }
  
  public void setLoadBalanceSQLStateFailover(String loadBalanceSQLStateFailover) {
    this.loadBalanceSQLStateFailover.setValue(loadBalanceSQLStateFailover);
  }
  
  public String getLoadBalanceSQLExceptionSubclassFailover() {
    return loadBalanceSQLExceptionSubclassFailover.getValueAsString();
  }
  
  public void setLoadBalanceSQLExceptionSubclassFailover(String loadBalanceSQLExceptionSubclassFailover) {
    this.loadBalanceSQLExceptionSubclassFailover.setValue(loadBalanceSQLExceptionSubclassFailover);
  }
  
  public boolean getLoadBalanceEnableJMX() {
    return loadBalanceEnableJMX.getValueAsBoolean();
  }
  
  public void setLoadBalanceEnableJMX(boolean loadBalanceEnableJMX) {
    this.loadBalanceEnableJMX.setValue(loadBalanceEnableJMX);
  }
  
  public void setLoadBalanceHostRemovalGracePeriod(int loadBalanceHostRemovalGracePeriod) throws SQLException {
    this.loadBalanceHostRemovalGracePeriod.setValue(loadBalanceHostRemovalGracePeriod, getExceptionInterceptor());
  }
  
  public int getLoadBalanceHostRemovalGracePeriod() {
    return loadBalanceHostRemovalGracePeriod.getValueAsInt();
  }
  
  public void setLoadBalanceAutoCommitStatementThreshold(int loadBalanceAutoCommitStatementThreshold) throws SQLException {
    this.loadBalanceAutoCommitStatementThreshold.setValue(loadBalanceAutoCommitStatementThreshold, getExceptionInterceptor());
  }
  
  public int getLoadBalanceAutoCommitStatementThreshold() {
    return loadBalanceAutoCommitStatementThreshold.getValueAsInt();
  }
  
  public void setLoadBalanceAutoCommitStatementRegex(String loadBalanceAutoCommitStatementRegex) {
    this.loadBalanceAutoCommitStatementRegex.setValue(loadBalanceAutoCommitStatementRegex);
  }
  
  public String getLoadBalanceAutoCommitStatementRegex() {
    return loadBalanceAutoCommitStatementRegex.getValueAsString();
  }
  
  public void setIncludeThreadDumpInDeadlockExceptions(boolean flag) {
    includeThreadDumpInDeadlockExceptions.setValue(flag);
  }
  
  public boolean getIncludeThreadDumpInDeadlockExceptions() {
    return includeThreadDumpInDeadlockExceptions.getValueAsBoolean();
  }
  
  public void setIncludeThreadNamesAsStatementComment(boolean flag) {
    includeThreadNamesAsStatementComment.setValue(flag);
  }
  
  public boolean getIncludeThreadNamesAsStatementComment() {
    return includeThreadNamesAsStatementComment.getValueAsBoolean();
  }
  
  public void setAuthenticationPlugins(String authenticationPlugins) {
    this.authenticationPlugins.setValue(authenticationPlugins);
  }
  
  public String getAuthenticationPlugins() {
    return authenticationPlugins.getValueAsString();
  }
  
  public void setDisabledAuthenticationPlugins(String disabledAuthenticationPlugins) {
    this.disabledAuthenticationPlugins.setValue(disabledAuthenticationPlugins);
  }
  
  public String getDisabledAuthenticationPlugins() {
    return disabledAuthenticationPlugins.getValueAsString();
  }
  
  public void setDefaultAuthenticationPlugin(String defaultAuthenticationPlugin) {
    this.defaultAuthenticationPlugin.setValue(defaultAuthenticationPlugin);
  }
  
  public String getDefaultAuthenticationPlugin() {
    return defaultAuthenticationPlugin.getValueAsString();
  }
  
  public void setParseInfoCacheFactory(String factoryClassname) {
    parseInfoCacheFactory.setValue(factoryClassname);
  }
  
  public String getParseInfoCacheFactory() {
    return parseInfoCacheFactory.getValueAsString();
  }
  
  public void setServerConfigCacheFactory(String factoryClassname) {
    serverConfigCacheFactory.setValue(factoryClassname);
  }
  
  public String getServerConfigCacheFactory() {
    return serverConfigCacheFactory.getValueAsString();
  }
  
  public void setDisconnectOnExpiredPasswords(boolean disconnectOnExpiredPasswords) {
    this.disconnectOnExpiredPasswords.setValue(disconnectOnExpiredPasswords);
  }
  
  public boolean getDisconnectOnExpiredPasswords() {
    return disconnectOnExpiredPasswords.getValueAsBoolean();
  }
  
  public String getReplicationConnectionGroup() {
    return replicationConnectionGroup.getValueAsString();
  }
  
  public void setReplicationConnectionGroup(String replicationConnectionGroup) {
    this.replicationConnectionGroup.setValue(replicationConnectionGroup);
  }
  
  public boolean getAllowMasterDownConnections() {
    return allowMasterDownConnections.getValueAsBoolean();
  }
  
  public void setAllowMasterDownConnections(boolean connectIfMasterDown) {
    allowMasterDownConnections.setValue(connectIfMasterDown);
  }
  
  public boolean getAllowSlaveDownConnections() {
    return allowSlaveDownConnections.getValueAsBoolean();
  }
  
  public void setAllowSlaveDownConnections(boolean connectIfSlaveDown) {
    allowSlaveDownConnections.setValue(connectIfSlaveDown);
  }
  
  public boolean getReadFromMasterWhenNoSlaves() {
    return readFromMasterWhenNoSlaves.getValueAsBoolean();
  }
  
  public void setReadFromMasterWhenNoSlaves(boolean useMasterIfSlavesDown) {
    readFromMasterWhenNoSlaves.setValue(useMasterIfSlavesDown);
  }
  
  public boolean getReplicationEnableJMX() {
    return replicationEnableJMX.getValueAsBoolean();
  }
  
  public void setReplicationEnableJMX(boolean replicationEnableJMX) {
    this.replicationEnableJMX.setValue(replicationEnableJMX);
  }
  
  public void setGetProceduresReturnsFunctions(boolean getProcedureReturnsFunctions) {
    getProceduresReturnsFunctions.setValue(getProcedureReturnsFunctions);
  }
  
  public boolean getGetProceduresReturnsFunctions() {
    return getProceduresReturnsFunctions.getValueAsBoolean();
  }
  
  public void setDetectCustomCollations(boolean detectCustomCollations) {
    this.detectCustomCollations.setValue(detectCustomCollations);
  }
  
  public boolean getDetectCustomCollations() {
    return detectCustomCollations.getValueAsBoolean();
  }
  
  public String getServerRSAPublicKeyFile() {
    return serverRSAPublicKeyFile.getValueAsString();
  }
  
  public void setServerRSAPublicKeyFile(String serverRSAPublicKeyFile) throws SQLException {
    if (this.serverRSAPublicKeyFile.getUpdateCount() > 0) {
      throw SQLError.createSQLException(Messages.getString("ConnectionProperties.dynamicChangeIsNotAllowed", new Object[] { "'serverRSAPublicKeyFile'" }), "S1009", null);
    }
    
    this.serverRSAPublicKeyFile.setValue(serverRSAPublicKeyFile);
  }
  
  public boolean getAllowPublicKeyRetrieval() {
    return allowPublicKeyRetrieval.getValueAsBoolean();
  }
  
  public void setAllowPublicKeyRetrieval(boolean allowPublicKeyRetrieval) throws SQLException {
    if (this.allowPublicKeyRetrieval.getUpdateCount() > 0) {
      throw SQLError.createSQLException(Messages.getString("ConnectionProperties.dynamicChangeIsNotAllowed", new Object[] { "'allowPublicKeyRetrieval'" }), "S1009", null);
    }
    

    this.allowPublicKeyRetrieval.setValue(allowPublicKeyRetrieval);
  }
  
  public void setDontCheckOnDuplicateKeyUpdateInSQL(boolean dontCheckOnDuplicateKeyUpdateInSQL) {
    this.dontCheckOnDuplicateKeyUpdateInSQL.setValue(dontCheckOnDuplicateKeyUpdateInSQL);
  }
  
  public boolean getDontCheckOnDuplicateKeyUpdateInSQL() {
    return dontCheckOnDuplicateKeyUpdateInSQL.getValueAsBoolean();
  }
  
  public void setSocksProxyHost(String socksProxyHost) {
    this.socksProxyHost.setValue(socksProxyHost);
  }
  
  public String getSocksProxyHost() {
    return socksProxyHost.getValueAsString();
  }
  
  public void setSocksProxyPort(int socksProxyPort) throws SQLException {
    this.socksProxyPort.setValue(socksProxyPort, null);
  }
  
  public int getSocksProxyPort() {
    return socksProxyPort.getValueAsInt();
  }
  
  public boolean getReadOnlyPropagatesToServer() {
    return readOnlyPropagatesToServer.getValueAsBoolean();
  }
  
  public void setReadOnlyPropagatesToServer(boolean flag) {
    readOnlyPropagatesToServer.setValue(flag);
  }
  
  public String getEnabledSSLCipherSuites() {
    return enabledSSLCipherSuites.getValueAsString();
  }
  
  public void setEnabledSSLCipherSuites(String cipherSuites) {
    enabledSSLCipherSuites.setValue(cipherSuites);
  }
  
  public String getEnabledTLSProtocols() {
    return enabledTLSProtocols.getValueAsString();
  }
  
  public void setEnabledTLSProtocols(String protocols) {
    enabledTLSProtocols.setValue(protocols);
  }
  
  public boolean getEnableEscapeProcessing() {
    return enableEscapeProcessing.getValueAsBoolean();
  }
  
  public void setEnableEscapeProcessing(boolean flag) {
    enableEscapeProcessing.setValue(flag);
  }
}
