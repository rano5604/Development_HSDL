package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.LogFactory;
import com.mysql.jdbc.log.LogUtils;
import com.mysql.jdbc.log.NullLogger;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import com.mysql.jdbc.util.LRUCache;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLPermission;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;






























public class ConnectionImpl
  extends ConnectionPropertiesImpl
  implements MySQLConnection
{
  private static final long serialVersionUID = 2877471301981509474L;
  private static final SQLPermission SET_NETWORK_TIMEOUT_PERM = new SQLPermission("setNetworkTimeout");
  
  private static final SQLPermission ABORT_PERM = new SQLPermission("abort");
  public static final String JDBC_LOCAL_CHARACTER_SET_RESULTS = "jdbc.local.character_set_results";
  
  public String getHost()
  {
    return host;
  }
  
  public String getHostPortPair() {
    return host + ":" + port;
  }
  
  private MySQLConnection proxy = null;
  private InvocationHandler realProxy = null;
  
  public boolean isProxySet() {
    return proxy != null;
  }
  
  public void setProxy(MySQLConnection proxy) {
    this.proxy = proxy;
    realProxy = ((this.proxy instanceof MultiHostMySQLConnection) ? ((MultiHostMySQLConnection)proxy).getThisAsProxy() : null);
  }
  

  private MySQLConnection getProxy()
  {
    return proxy != null ? proxy : this;
  }
  


  @Deprecated
  public MySQLConnection getLoadBalanceSafeProxy()
  {
    return getMultiHostSafeProxy();
  }
  
  public MySQLConnection getMultiHostSafeProxy() {
    return getProxy();
  }
  
  public MySQLConnection getActiveMySQLConnection() {
    return this;
  }
  
  public Object getConnectionMutex() {
    return realProxy != null ? realProxy : getProxy();
  }
  
  public class ExceptionInterceptorChain implements ExceptionInterceptor {
    private List<Extension> interceptors;
    
    ExceptionInterceptorChain(String interceptorClasses) throws SQLException {
      interceptors = Util.loadExtensions(ConnectionImpl.this, props, interceptorClasses, "Connection.BadExceptionInterceptor", this);
    }
    
    void addRingZero(ExceptionInterceptor interceptor) throws SQLException
    {
      interceptors.add(0, interceptor);
    }
    
    public SQLException interceptException(SQLException sqlEx, Connection conn) {
      if (interceptors != null) {
        Iterator<Extension> iter = interceptors.iterator();
        
        while (iter.hasNext()) {
          sqlEx = ((ExceptionInterceptor)iter.next()).interceptException(sqlEx, ConnectionImpl.this);
        }
      }
      
      return sqlEx;
    }
    
    public void destroy() {
      if (interceptors != null) {
        Iterator<Extension> iter = interceptors.iterator();
        
        while (iter.hasNext()) {
          ((ExceptionInterceptor)iter.next()).destroy();
        }
      }
    }
    
    public void init(Connection conn, Properties properties) throws SQLException
    {
      if (interceptors != null) {
        Iterator<Extension> iter = interceptors.iterator();
        
        while (iter.hasNext()) {
          ((ExceptionInterceptor)iter.next()).init(conn, properties);
        }
      }
    }
    
    public List<Extension> getInterceptors() {
      return interceptors;
    }
  }
  


  static class CompoundCacheKey
  {
    final String componentOne;
    

    final String componentTwo;
    
    final int hashCode;
    

    CompoundCacheKey(String partOne, String partTwo)
    {
      componentOne = partOne;
      componentTwo = partTwo;
      
      int hc = 17;
      hc = 31 * hc + (componentOne != null ? componentOne.hashCode() : 0);
      hc = 31 * hc + (componentTwo != null ? componentTwo.hashCode() : 0);
      hashCode = hc;
    }
    





    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      if ((obj != null) && (CompoundCacheKey.class.isAssignableFrom(obj.getClass()))) {
        CompoundCacheKey another = (CompoundCacheKey)obj;
        if (componentOne == null ? componentOne == null : componentOne.equals(componentOne)) {
          return componentTwo == null ? false : componentTwo == null ? true : componentTwo.equals(componentTwo);
        }
      }
      return false;
    }
    





    public int hashCode()
    {
      return hashCode;
    }
  }
  




  private static final Object CHARSET_CONVERTER_NOT_AVAILABLE_MARKER = new Object();
  


  public static Map<?, ?> charsetMap;
  


  protected static final String DEFAULT_LOGGER_CLASS = "com.mysql.jdbc.log.StandardLogger";
  


  private static final int HISTOGRAM_BUCKETS = 20;
  


  private static final String LOGGER_INSTANCE_NAME = "MySQL";
  


  private static Map<String, Integer> mapTransIsolationNameToValue = null;
  

  private static final Log NULL_LOGGER = new NullLogger("MySQL");
  


  protected static Map<?, ?> roundRobinStatsMap;
  

  private static final Map<String, Map<Integer, String>> customIndexToCharsetMapByUrl = new HashMap();
  



  private static final Map<String, Map<String, Integer>> customCharsetToMblenMapByUrl = new HashMap();
  
  private CacheAdapter<String, Map<String, String>> serverConfigCache;
  
  private long queryTimeCount;
  
  private double queryTimeSum;
  
  private double queryTimeSumSquares;
  
  private double queryTimeMean;
  
  private transient Timer cancelTimer;
  private List<Extension> connectionLifecycleInterceptors;
  private static final Constructor<?> JDBC_4_CONNECTION_CTOR;
  private static final int DEFAULT_RESULT_SET_TYPE = 1003;
  private static final int DEFAULT_RESULT_SET_CONCURRENCY = 1007;
  
  static
  {
    mapTransIsolationNameToValue = new HashMap(8);
    mapTransIsolationNameToValue.put("READ-UNCOMMITED", Integer.valueOf(1));
    mapTransIsolationNameToValue.put("READ-UNCOMMITTED", Integer.valueOf(1));
    mapTransIsolationNameToValue.put("READ-COMMITTED", Integer.valueOf(2));
    mapTransIsolationNameToValue.put("REPEATABLE-READ", Integer.valueOf(4));
    mapTransIsolationNameToValue.put("SERIALIZABLE", Integer.valueOf(8));
    
    if (Util.isJdbc4()) {
      try {
        JDBC_4_CONNECTION_CTOR = Class.forName("com.mysql.jdbc.JDBC4Connection").getConstructor(new Class[] { String.class, Integer.TYPE, Properties.class, String.class, String.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_CONNECTION_CTOR = null;
    }
  }
  
  protected static SQLException appendMessageToException(SQLException sqlEx, String messageToAppend, ExceptionInterceptor interceptor) {
    String origMessage = sqlEx.getMessage();
    String sqlState = sqlEx.getSQLState();
    int vendorErrorCode = sqlEx.getErrorCode();
    
    StringBuilder messageBuf = new StringBuilder(origMessage.length() + messageToAppend.length());
    messageBuf.append(origMessage);
    messageBuf.append(messageToAppend);
    
    SQLException sqlExceptionWithNewMessage = SQLError.createSQLException(messageBuf.toString(), sqlState, vendorErrorCode, interceptor);
    




    try
    {
      Method getStackTraceMethod = null;
      Method setStackTraceMethod = null;
      Object theStackTraceAsObject = null;
      
      Class<?> stackTraceElementClass = Class.forName("java.lang.StackTraceElement");
      Class<?> stackTraceElementArrayClass = Array.newInstance(stackTraceElementClass, new int[] { 0 }).getClass();
      
      getStackTraceMethod = Throwable.class.getMethod("getStackTrace", new Class[0]);
      
      setStackTraceMethod = Throwable.class.getMethod("setStackTrace", new Class[] { stackTraceElementArrayClass });
      
      if ((getStackTraceMethod != null) && (setStackTraceMethod != null)) {
        theStackTraceAsObject = getStackTraceMethod.invoke(sqlEx, new Object[0]);
        setStackTraceMethod.invoke(sqlExceptionWithNewMessage, new Object[] { theStackTraceAsObject });
      }
    }
    catch (NoClassDefFoundError noClassDefFound) {}catch (NoSuchMethodException noSuchMethodEx) {}catch (Throwable catchAll) {}
    





    return sqlExceptionWithNewMessage;
  }
  
  public Timer getCancelTimer() {
    synchronized (getConnectionMutex()) {
      if (cancelTimer == null) {
        cancelTimer = new Timer("MySQL Statement Cancellation Timer", true);
      }
      return cancelTimer;
    }
  }
  






  protected static Connection getInstance(String hostToConnectTo, int portToConnectTo, Properties info, String databaseToConnectTo, String url)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new ConnectionImpl(hostToConnectTo, portToConnectTo, info, databaseToConnectTo, url);
    }
    
    return (Connection)Util.handleNewInstance(JDBC_4_CONNECTION_CTOR, new Object[] { hostToConnectTo, Integer.valueOf(portToConnectTo), info, databaseToConnectTo, url }, null);
  }
  

  private static final Random random = new Random();
  





  protected static synchronized int getNextRoundRobinHostIndex(String url, List<?> hostList)
  {
    int indexRange = hostList.size();
    
    int index = random.nextInt(indexRange);
    
    return index;
  }
  
  private static boolean nullSafeCompare(String s1, String s2) {
    if ((s1 == null) && (s2 == null)) {
      return true;
    }
    
    if ((s1 == null) && (s2 != null)) {
      return false;
    }
    
    return (s1 != null) && (s1.equals(s2));
  }
  

  private boolean autoCommit = true;
  


  private CacheAdapter<String, PreparedStatement.ParseInfo> cachedPreparedStatementParams;
  


  private String characterSetMetadata = null;
  




  private String characterSetResultsOnServer = null;
  





  private final Map<String, Object> charsetConverterMap = new HashMap(CharsetMapping.getNumberOfCharsetsConfigured());
  

  private long connectionCreationTimeMillis = 0L;
  

  private long connectionId;
  

  private String database = null;
  

  private java.sql.DatabaseMetaData dbmd = null;
  

  private TimeZone defaultTimeZone;
  

  private ProfilerEventHandler eventSink;
  

  private Throwable forceClosedReason;
  
  private boolean hasIsolationLevels = false;
  

  private boolean hasQuotedIdentifiers = false;
  

  private String host = null;
  
  public Map<Integer, String> indexToCustomMysqlCharset = null;
  
  private Map<String, Integer> mysqlCharsetToCustomMblen = null;
  

  private transient MysqlIO io = null;
  
  private boolean isClientTzUTC = false;
  

  private boolean isClosed = true;
  

  private boolean isInGlobalTx = false;
  

  private boolean isRunningOnJDK13 = false;
  

  private int isolationLevel = 2;
  
  private boolean isServerTzUTC = false;
  

  private long lastQueryFinishedTime = 0L;
  

  private transient Log log = NULL_LOGGER;
  




  private long longestQueryTimeMs = 0L;
  

  private boolean lowerCaseTableNames = false;
  



  private long maximumNumberTablesAccessed = 0L;
  

  private int sessionMaxRows = -1;
  

  private long metricsLastReportedMs;
  
  private long minimumNumberTablesAccessed = Long.MAX_VALUE;
  

  private String myURL = null;
  

  private boolean needsPing = false;
  
  private int netBufferLength = 16384;
  
  private boolean noBackslashEscapes = false;
  
  private long numberOfPreparedExecutes = 0L;
  
  private long numberOfPrepares = 0L;
  
  private long numberOfQueriesIssued = 0L;
  
  private long numberOfResultSetsCreated = 0L;
  
  private long[] numTablesMetricsHistBreakpoints;
  
  private int[] numTablesMetricsHistCounts;
  
  private long[] oldHistBreakpoints = null;
  
  private int[] oldHistCounts = null;
  




  private final CopyOnWriteArrayList<Statement> openStatements = new CopyOnWriteArrayList();
  
  private LRUCache<CompoundCacheKey, CallableStatement.CallableStatementParamInfo> parsedCallableStatementCache;
  
  private boolean parserKnowsUnicode = false;
  

  private String password = null;
  

  private long[] perfMetricsHistBreakpoints;
  

  private int[] perfMetricsHistCounts;
  
  private String pointOfOrigin;
  
  private int port = 3306;
  

  protected Properties props = null;
  

  private boolean readInfoMsg = false;
  

  private boolean readOnly = false;
  

  protected LRUCache<String, CachedResultSetMetaData> resultSetMetadataCache;
  

  private TimeZone serverTimezoneTZ = null;
  

  private Map<String, String> serverVariables = null;
  
  private long shortestQueryTimeMs = Long.MAX_VALUE;
  
  private double totalQueryTimeMs = 0.0D;
  

  private boolean transactionsSupported = false;
  



  private Map<String, Class<?>> typeMap;
  


  private boolean useAnsiQuotes = false;
  

  private String user = null;
  




  private boolean useServerPreparedStmts = false;
  
  private LRUCache<String, Boolean> serverSideStatementCheckCache;
  
  private LRUCache<CompoundCacheKey, ServerPreparedStatement> serverSideStatementCache;
  
  private Calendar sessionCalendar;
  
  private Calendar utcCalendar;
  
  private String origHostToConnectTo;
  
  private int origPortToConnectTo;
  
  private String origDatabaseToConnectTo;
  
  private String errorMessageEncoding = "Cp1252";
  


  private boolean usePlatformCharsetConverters;
  

  private boolean hasTriedMasterFlag = false;
  




  private String statementComment = null;
  





  private boolean storesLowerCaseTableName;
  




  private List<StatementInterceptorV2> statementInterceptors;
  




  private boolean requiresEscapingEncoder;
  




  private String hostPortPair;
  




  private static final String SERVER_VERSION_STRING_VAR_NAME = "server_version_string";
  





  public ConnectionImpl(String hostToConnectTo, int portToConnectTo, Properties info, String databaseToConnectTo, String url)
    throws SQLException
  {
    connectionCreationTimeMillis = System.currentTimeMillis();
    
    if (databaseToConnectTo == null) {
      databaseToConnectTo = "";
    }
    



    origHostToConnectTo = hostToConnectTo;
    origPortToConnectTo = portToConnectTo;
    origDatabaseToConnectTo = databaseToConnectTo;
    try
    {
      Blob.class.getMethod("truncate", new Class[] { Long.TYPE });
      
      isRunningOnJDK13 = false;
    } catch (NoSuchMethodException nsme) {
      isRunningOnJDK13 = true;
    }
    
    sessionCalendar = new GregorianCalendar();
    utcCalendar = new GregorianCalendar();
    utcCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    







    log = LogFactory.getLogger(getLogger(), "MySQL", getExceptionInterceptor());
    
    if (NonRegisteringDriver.isHostPropertiesList(hostToConnectTo)) {
      Properties hostSpecificProps = NonRegisteringDriver.expandHostKeyValues(hostToConnectTo);
      
      Enumeration<?> propertyNames = hostSpecificProps.propertyNames();
      
      while (propertyNames.hasMoreElements()) {
        String propertyName = propertyNames.nextElement().toString();
        String propertyValue = hostSpecificProps.getProperty(propertyName);
        
        info.setProperty(propertyName, propertyValue);
      }
      
    }
    else if (hostToConnectTo == null) {
      host = "localhost";
      hostPortPair = (host + ":" + portToConnectTo);
    } else {
      host = hostToConnectTo;
      
      if (hostToConnectTo.indexOf(":") == -1) {
        hostPortPair = (host + ":" + portToConnectTo);
      } else {
        hostPortPair = host;
      }
    }
    

    port = portToConnectTo;
    
    database = databaseToConnectTo;
    myURL = url;
    user = info.getProperty("user");
    password = info.getProperty("password");
    
    if ((user == null) || (user.equals(""))) {
      user = "";
    }
    
    if (password == null) {
      password = "";
    }
    
    props = info;
    
    initializeDriverProperties(info);
    

    defaultTimeZone = TimeUtil.getDefaultTimeZone(getCacheDefaultTimezone());
    
    isClientTzUTC = ((!defaultTimeZone.useDaylightTime()) && (defaultTimeZone.getRawOffset() == 0));
    
    if (getUseUsageAdvisor()) {
      pointOfOrigin = LogUtils.findCallingClassAndMethod(new Throwable());
    } else {
      pointOfOrigin = "";
    }
    try
    {
      dbmd = getMetaData(false, false);
      initializeSafeStatementInterceptors();
      createNewIO(false);
      unSafeStatementInterceptors();
    } catch (SQLException ex) {
      cleanup(ex);
      

      throw ex;
    } catch (Exception ex) {
      cleanup(ex);
      
      StringBuilder mesg = new StringBuilder(128);
      
      if (!getParanoid()) {
        mesg.append("Cannot connect to MySQL server on ");
        mesg.append(host);
        mesg.append(":");
        mesg.append(port);
        mesg.append(".\n\n");
        mesg.append("Make sure that there is a MySQL server ");
        mesg.append("running on the machine/port you are trying ");
        mesg.append("to connect to and that the machine this software is running on ");
        mesg.append("is able to connect to this host/port (i.e. not firewalled). ");
        mesg.append("Also make sure that the server has not been started with the --skip-networking ");
        mesg.append("flag.\n\n");
      } else {
        mesg.append("Unable to connect to database.");
      }
      
      SQLException sqlEx = SQLError.createSQLException(mesg.toString(), "08S01", getExceptionInterceptor());
      
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
    
    NonRegisteringDriver.trackConnection(this);
  }
  
  public void unSafeStatementInterceptors() throws SQLException
  {
    ArrayList<StatementInterceptorV2> unSafedStatementInterceptors = new ArrayList(statementInterceptors.size());
    
    for (int i = 0; i < statementInterceptors.size(); i++) {
      NoSubInterceptorWrapper wrappedInterceptor = (NoSubInterceptorWrapper)statementInterceptors.get(i);
      
      unSafedStatementInterceptors.add(wrappedInterceptor.getUnderlyingInterceptor());
    }
    
    statementInterceptors = unSafedStatementInterceptors;
    
    if (io != null) {
      io.setStatementInterceptors(statementInterceptors);
    }
  }
  
  public void initializeSafeStatementInterceptors() throws SQLException {
    isClosed = false;
    
    List<Extension> unwrappedInterceptors = Util.loadExtensions(this, props, getStatementInterceptors(), "MysqlIo.BadStatementInterceptor", getExceptionInterceptor());
    

    statementInterceptors = new ArrayList(unwrappedInterceptors.size());
    
    for (int i = 0; i < unwrappedInterceptors.size(); i++) {
      Extension interceptor = (Extension)unwrappedInterceptors.get(i);
      

      if ((interceptor instanceof StatementInterceptor)) {
        if (ReflectiveStatementInterceptorAdapter.getV2PostProcessMethod(interceptor.getClass()) != null) {
          statementInterceptors.add(new NoSubInterceptorWrapper(new ReflectiveStatementInterceptorAdapter((StatementInterceptor)interceptor)));
        } else {
          statementInterceptors.add(new NoSubInterceptorWrapper(new V1toV2StatementInterceptorAdapter((StatementInterceptor)interceptor)));
        }
      } else {
        statementInterceptors.add(new NoSubInterceptorWrapper((StatementInterceptorV2)interceptor));
      }
    }
  }
  
  public List<StatementInterceptorV2> getStatementInterceptorsInstances()
  {
    return statementInterceptors;
  }
  
  private void addToHistogram(int[] histogramCounts, long[] histogramBreakpoints, long value, int numberOfTimes, long currentLowerBound, long currentUpperBound)
  {
    if (histogramCounts == null) {
      createInitialHistogram(histogramBreakpoints, currentLowerBound, currentUpperBound);
    } else {
      for (int i = 0; i < 20; i++) {
        if (histogramBreakpoints[i] >= value) {
          histogramCounts[i] += numberOfTimes;
          
          break;
        }
      }
    }
  }
  
  private void addToPerformanceHistogram(long value, int numberOfTimes) {
    checkAndCreatePerformanceHistogram();
    
    addToHistogram(perfMetricsHistCounts, perfMetricsHistBreakpoints, value, numberOfTimes, shortestQueryTimeMs == Long.MAX_VALUE ? 0L : shortestQueryTimeMs, longestQueryTimeMs);
  }
  
  private void addToTablesAccessedHistogram(long value, int numberOfTimes)
  {
    checkAndCreateTablesAccessedHistogram();
    
    addToHistogram(numTablesMetricsHistCounts, numTablesMetricsHistBreakpoints, value, numberOfTimes, minimumNumberTablesAccessed == Long.MAX_VALUE ? 0L : minimumNumberTablesAccessed, maximumNumberTablesAccessed);
  }
  






  private void buildCollationMapping()
    throws SQLException
  {
    Map<Integer, String> customCharset = null;
    Map<String, Integer> customMblen = null;
    
    if (getCacheServerConfiguration()) {
      synchronized (customIndexToCharsetMapByUrl) {
        customCharset = (Map)customIndexToCharsetMapByUrl.get(getURL());
        customMblen = (Map)customCharsetToMblenMapByUrl.get(getURL());
      }
    }
    
    if ((customCharset == null) && (getDetectCustomCollations()) && (versionMeetsMinimum(4, 1, 0)))
    {
      java.sql.Statement stmt = null;
      ResultSet results = null;
      try
      {
        customCharset = new HashMap();
        customMblen = new HashMap();
        
        stmt = getMetadataSafeStatement();
        try
        {
          results = stmt.executeQuery("SHOW COLLATION");
          while (results.next()) {
            int collationIndex = ((Number)results.getObject(3)).intValue();
            String charsetName = results.getString(2);
            

            if ((collationIndex >= 2048) || (!charsetName.equals(CharsetMapping.getMysqlCharsetNameForCollationIndex(Integer.valueOf(collationIndex)))))
            {
              customCharset.put(Integer.valueOf(collationIndex), charsetName);
            }
            

            if (!CharsetMapping.CHARSET_NAME_TO_CHARSET.containsKey(charsetName)) {
              customMblen.put(charsetName, null);
            }
          }
        } catch (SQLException ex) {
          if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
            throw ex;
          }
        }
        

        if (customMblen.size() > 0) {
          try {
            results = stmt.executeQuery("SHOW CHARACTER SET");
            while (results.next()) {
              String charsetName = results.getString("Charset");
              if (customMblen.containsKey(charsetName)) {
                customMblen.put(charsetName, Integer.valueOf(results.getInt("Maxlen")));
              }
            }
          } catch (SQLException ex) {
            if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
              throw ex;
            }
          }
        }
        
        if (getCacheServerConfiguration()) {
          synchronized (customIndexToCharsetMapByUrl) {
            customIndexToCharsetMapByUrl.put(getURL(), customCharset);
            customCharsetToMblenMapByUrl.put(getURL(), customMblen);
          }
        }
      }
      catch (SQLException ex) {
        throw ex;
      } catch (RuntimeException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
        sqlEx.initCause(ex);
        throw sqlEx;
      } finally {
        if (results != null) {
          try {
            results.close();
          }
          catch (SQLException sqlE) {}
        }
        

        if (stmt != null) {
          try {
            stmt.close();
          }
          catch (SQLException sqlE) {}
        }
      }
    }
    

    if (customCharset != null) {
      indexToCustomMysqlCharset = Collections.unmodifiableMap(customCharset);
    }
    if (customMblen != null) {
      mysqlCharsetToCustomMblen = Collections.unmodifiableMap(customMblen);
    }
  }
  
  private boolean canHandleAsServerPreparedStatement(String sql) throws SQLException {
    if ((sql == null) || (sql.length() == 0)) {
      return true;
    }
    
    if (!useServerPreparedStmts) {
      return false;
    }
    
    if (getCachePreparedStatements()) {
      synchronized (serverSideStatementCheckCache) {
        Boolean flag = (Boolean)serverSideStatementCheckCache.get(sql);
        
        if (flag != null) {
          return flag.booleanValue();
        }
        
        boolean canHandle = canHandleAsServerPreparedStatementNoCache(sql);
        
        if (sql.length() < getPreparedStatementCacheSqlLimit()) {
          serverSideStatementCheckCache.put(sql, canHandle ? Boolean.TRUE : Boolean.FALSE);
        }
        
        return canHandle;
      }
    }
    
    return canHandleAsServerPreparedStatementNoCache(sql);
  }
  
  private boolean canHandleAsServerPreparedStatementNoCache(String sql)
    throws SQLException
  {
    if (StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "CALL")) {
      return false;
    }
    
    boolean canHandleAsStatement = true;
    
    if ((!versionMeetsMinimum(5, 0, 7)) && ((StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "SELECT")) || (StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "DELETE")) || (StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "INSERT")) || (StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "UPDATE")) || (StringUtils.startsWithIgnoreCaseAndNonAlphaNumeric(sql, "REPLACE"))))
    {








      int currentPos = 0;
      int statementLength = sql.length();
      int lastPosToLook = statementLength - 7;
      boolean allowBackslashEscapes = !noBackslashEscapes;
      String quoteChar = useAnsiQuotes ? "\"" : "'";
      boolean foundLimitWithPlaceholder = false;
      
      while (currentPos < lastPosToLook) {
        int limitStart = StringUtils.indexOfIgnoreCase(currentPos, sql, "LIMIT ", quoteChar, quoteChar, allowBackslashEscapes ? StringUtils.SEARCH_MODE__ALL : StringUtils.SEARCH_MODE__MRK_COM_WS);
        

        if (limitStart == -1) {
          break;
        }
        
        currentPos = limitStart + 7;
        
        while (currentPos < statementLength) {
          char c = sql.charAt(currentPos);
          




          if ((!Character.isDigit(c)) && (!Character.isWhitespace(c)) && (c != ',') && (c != '?')) {
            break;
          }
          
          if (c == '?') {
            foundLimitWithPlaceholder = true;
            break;
          }
          
          currentPos++;
        }
      }
      
      canHandleAsStatement = !foundLimitWithPlaceholder;
    } else if (StringUtils.startsWithIgnoreCaseAndWs(sql, "XA ")) {
      canHandleAsStatement = false;
    } else if (StringUtils.startsWithIgnoreCaseAndWs(sql, "CREATE TABLE")) {
      canHandleAsStatement = false;
    } else if (StringUtils.startsWithIgnoreCaseAndWs(sql, "DO")) {
      canHandleAsStatement = false;
    } else if (StringUtils.startsWithIgnoreCaseAndWs(sql, "SET")) {
      canHandleAsStatement = false;
    } else if ((StringUtils.startsWithIgnoreCaseAndWs(sql, "SHOW WARNINGS")) && (versionMeetsMinimum(5, 7, 2))) {
      canHandleAsStatement = false;
    } else if (sql.startsWith("/* ping */")) {
      canHandleAsStatement = false;
    }
    
    return canHandleAsStatement;
  }
  











  public void changeUser(String userName, String newPassword)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      
      if ((userName == null) || (userName.equals(""))) {
        userName = "";
      }
      
      if (newPassword == null) {
        newPassword = "";
      }
      

      sessionMaxRows = -1;
      try
      {
        io.changeUser(userName, newPassword, database);
      } catch (SQLException ex) {
        if ((versionMeetsMinimum(5, 6, 13)) && ("28000".equals(ex.getSQLState()))) {
          cleanup(ex);
        }
        throw ex;
      }
      user = userName;
      password = newPassword;
      
      if (versionMeetsMinimum(4, 1, 0)) {
        configureClientCharacterSet(true);
      }
      
      setSessionVariables();
      
      setupServerForTruncationChecks();
    }
  }
  
  private boolean characterSetNamesMatches(String mysqlEncodingName)
  {
    return (mysqlEncodingName != null) && (mysqlEncodingName.equalsIgnoreCase((String)serverVariables.get("character_set_client"))) && (mysqlEncodingName.equalsIgnoreCase((String)serverVariables.get("character_set_connection")));
  }
  
  private void checkAndCreatePerformanceHistogram()
  {
    if (perfMetricsHistCounts == null) {
      perfMetricsHistCounts = new int[20];
    }
    
    if (perfMetricsHistBreakpoints == null) {
      perfMetricsHistBreakpoints = new long[20];
    }
  }
  
  private void checkAndCreateTablesAccessedHistogram() {
    if (numTablesMetricsHistCounts == null) {
      numTablesMetricsHistCounts = new int[20];
    }
    
    if (numTablesMetricsHistBreakpoints == null) {
      numTablesMetricsHistBreakpoints = new long[20];
    }
  }
  
  public void checkClosed() throws SQLException {
    if (isClosed) {
      throwConnectionClosedException();
    }
  }
  
  public void throwConnectionClosedException() throws SQLException {
    SQLException ex = SQLError.createSQLException("No operations allowed after connection closed.", "08003", getExceptionInterceptor());
    

    if (forceClosedReason != null) {
      ex.initCause(forceClosedReason);
    }
    
    throw ex;
  }
  




  private void checkServerEncoding()
    throws SQLException
  {
    if ((getUseUnicode()) && (getEncoding() != null))
    {
      return;
    }
    
    String serverCharset = (String)serverVariables.get("character_set");
    
    if (serverCharset == null)
    {
      serverCharset = (String)serverVariables.get("character_set_server");
    }
    
    String mappedServerEncoding = null;
    
    if (serverCharset != null) {
      try {
        mappedServerEncoding = CharsetMapping.getJavaEncodingForMysqlCharset(serverCharset);
      } catch (RuntimeException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
        sqlEx.initCause(ex);
        throw sqlEx;
      }
    }
    



    if ((!getUseUnicode()) && (mappedServerEncoding != null)) {
      SingleByteCharsetConverter converter = getCharsetConverter(mappedServerEncoding);
      
      if (converter != null) {
        setUseUnicode(true);
        setEncoding(mappedServerEncoding);
        
        return;
      }
    }
    



    if (serverCharset != null) {
      if (mappedServerEncoding == null)
      {
        if (Character.isLowerCase(serverCharset.charAt(0))) {
          char[] ach = serverCharset.toCharArray();
          ach[0] = Character.toUpperCase(serverCharset.charAt(0));
          setEncoding(new String(ach));
        }
      }
      
      if (mappedServerEncoding == null) {
        throw SQLError.createSQLException("Unknown character encoding on server '" + serverCharset + "', use 'characterEncoding=' property " + " to provide correct mapping", "01S00", getExceptionInterceptor());
      }
      




      try
      {
        StringUtils.getBytes("abc", mappedServerEncoding);
        setEncoding(mappedServerEncoding);
        setUseUnicode(true);
      } catch (UnsupportedEncodingException UE) {
        throw SQLError.createSQLException("The driver can not map the character encoding '" + getEncoding() + "' that your server is using " + "to a character encoding your JVM understands. You can specify this mapping manually by adding \"useUnicode=true\" " + "as well as \"characterEncoding=[an_encoding_your_jvm_understands]\" to your JDBC URL.", "0S100", getExceptionInterceptor());
      }
    }
  }
  






  private void checkTransactionIsolationLevel()
    throws SQLException
  {
    String s = (String)serverVariables.get("transaction_isolation");
    if (s == null) {
      s = (String)serverVariables.get("tx_isolation");
    }
    
    if (s != null) {
      Integer intTI = (Integer)mapTransIsolationNameToValue.get(s);
      
      if (intTI != null) {
        isolationLevel = intTI.intValue();
      }
    }
  }
  




  public void abortInternal()
    throws SQLException
  {
    if (io != null)
    {

      try
      {

        io.forceClose();
        io.releaseResources();
      }
      catch (Throwable t) {}
      
      io = null;
    }
    
    isClosed = true;
  }
  




  private void cleanup(Throwable whyCleanedUp)
  {
    try
    {
      if (io != null) {
        if (isClosed()) {
          io.forceClose();
        } else {
          realClose(false, false, false, whyCleanedUp);
        }
      }
    }
    catch (SQLException sqlEx) {}
    

    isClosed = true;
  }
  
  @Deprecated
  public void clearHasTriedMaster() {
    hasTriedMasterFlag = false;
  }
  













  public java.sql.PreparedStatement clientPrepareStatement(String sql)
    throws SQLException
  {
    return clientPrepareStatement(sql, 1003, 1007);
  }
  

  public java.sql.PreparedStatement clientPrepareStatement(String sql, int autoGenKeyIndex)
    throws SQLException
  {
    java.sql.PreparedStatement pStmt = clientPrepareStatement(sql);
    
    ((PreparedStatement)pStmt).setRetrieveGeneratedKeys(autoGenKeyIndex == 1);
    
    return pStmt;
  }
  




  public java.sql.PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    return clientPrepareStatement(sql, resultSetType, resultSetConcurrency, true);
  }
  
  public java.sql.PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, boolean processEscapeCodesIfNeeded) throws SQLException
  {
    checkClosed();
    
    String nativeSql = (processEscapeCodesIfNeeded) && (getProcessEscapeCodesForPrepStmts()) ? nativeSQL(sql) : sql;
    
    PreparedStatement pStmt = null;
    
    if (getCachePreparedStatements()) {
      PreparedStatement.ParseInfo pStmtInfo = (PreparedStatement.ParseInfo)cachedPreparedStatementParams.get(nativeSql);
      
      if (pStmtInfo == null) {
        pStmt = PreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, database);
        
        cachedPreparedStatementParams.put(nativeSql, pStmt.getParseInfo());
      } else {
        pStmt = PreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, database, pStmtInfo);
      }
    } else {
      pStmt = PreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, database);
    }
    
    pStmt.setResultSetType(resultSetType);
    pStmt.setResultSetConcurrency(resultSetConcurrency);
    
    return pStmt;
  }
  


  public java.sql.PreparedStatement clientPrepareStatement(String sql, int[] autoGenKeyIndexes)
    throws SQLException
  {
    PreparedStatement pStmt = (PreparedStatement)clientPrepareStatement(sql);
    
    pStmt.setRetrieveGeneratedKeys((autoGenKeyIndexes != null) && (autoGenKeyIndexes.length > 0));
    
    return pStmt;
  }
  

  public java.sql.PreparedStatement clientPrepareStatement(String sql, String[] autoGenKeyColNames)
    throws SQLException
  {
    PreparedStatement pStmt = (PreparedStatement)clientPrepareStatement(sql);
    
    pStmt.setRetrieveGeneratedKeys((autoGenKeyColNames != null) && (autoGenKeyColNames.length > 0));
    
    return pStmt;
  }
  
  public java.sql.PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    return clientPrepareStatement(sql, resultSetType, resultSetConcurrency, true);
  }
  










  public void close()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if (connectionLifecycleInterceptors != null) {
        new IterateBlock(connectionLifecycleInterceptors.iterator())
        {
          void forEach(Extension each) throws SQLException {
            ((ConnectionLifecycleInterceptor)each).close();
          }
        }.doForAll();
      }
      
      realClose(true, true, false, null);
    }
  }
  



  private void closeAllOpenStatements()
    throws SQLException
  {
    SQLException postponedException = null;
    
    for (Statement stmt : openStatements) {
      try {
        ((StatementImpl)stmt).realClose(false, true);
      } catch (SQLException sqlEx) {
        postponedException = sqlEx;
      }
    }
    
    if (postponedException != null) {
      throw postponedException;
    }
  }
  
  private void closeStatement(java.sql.Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      }
      catch (SQLException sqlEx) {}
      

      stmt = null;
    }
  }
  











  public void commit()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      try
      {
        if (connectionLifecycleInterceptors != null) {
          IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
          {
            void forEach(Extension each) throws SQLException
            {
              if (!((ConnectionLifecycleInterceptor)each).commit()) {
                stopIterating = true;
              }
              
            }
          };
          iter.doForAll();
          
          if (!iter.fullIteration()) {
            jsr 136;return;
          }
        }
        

        if ((autoCommit) && (!getRelaxAutoCommit()))
          throw SQLError.createSQLException("Can't call commit when autocommit=true", getExceptionInterceptor());
        if (transactionsSupported) {
          if ((getUseLocalTransactionState()) && (versionMeetsMinimum(5, 0, 0)) && 
            (!io.inTransactionOnServer())) {
            jsr 71;return;
          }
          

          execSQL(null, "commit", -1, null, 1003, 1007, false, database, null, false);
        }
      } catch (SQLException sqlException) {
        if ("08S01".equals(sqlException.getSQLState())) {
          throw SQLError.createSQLException("Communications link failure during commit(). Transaction resolution unknown.", "08007", getExceptionInterceptor());
        }
        

        throw sqlException;
      } finally {
        jsr 5; } localObject2 = returnAddress;needsPing = getReconnectAtTxEnd();ret;
    }
  }
  






  private void configureCharsetProperties()
    throws SQLException
  {
    if (getEncoding() != null) {
      try
      {
        String testString = "abc";
        StringUtils.getBytes(testString, getEncoding());
      }
      catch (UnsupportedEncodingException UE) {
        String oldEncoding = getEncoding();
        try
        {
          setEncoding(CharsetMapping.getJavaEncodingForMysqlCharset(oldEncoding));
        } catch (RuntimeException ex) {
          SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
          sqlEx.initCause(ex);
          throw sqlEx;
        }
        
        if (getEncoding() == null) {
          throw SQLError.createSQLException("Java does not support the MySQL character encoding '" + oldEncoding + "'.", "01S00", getExceptionInterceptor());
        }
        
        try
        {
          String testString = "abc";
          StringUtils.getBytes(testString, getEncoding());
        } catch (UnsupportedEncodingException encodingEx) {
          throw SQLError.createSQLException("Unsupported character encoding '" + getEncoding() + "'.", "01S00", getExceptionInterceptor());
        }
      }
    }
  }
  












  private boolean configureClientCharacterSet(boolean dontCheckServerMatch)
    throws SQLException
  {
    String realJavaEncoding = getEncoding();
    boolean characterSetAlreadyConfigured = false;
    try
    {
      if (versionMeetsMinimum(4, 1, 0)) {
        characterSetAlreadyConfigured = true;
        
        setUseUnicode(true);
        
        configureCharsetProperties();
        realJavaEncoding = getEncoding();
        


        try
        {
          if ((props != null) && (props.getProperty("com.mysql.jdbc.faultInjection.serverCharsetIndex") != null)) {
            io.serverCharsetIndex = Integer.parseInt(props.getProperty("com.mysql.jdbc.faultInjection.serverCharsetIndex"));
          }
          
          String serverEncodingToSet = CharsetMapping.getJavaEncodingForCollationIndex(Integer.valueOf(io.serverCharsetIndex));
          
          if ((serverEncodingToSet == null) || (serverEncodingToSet.length() == 0)) {
            if (realJavaEncoding != null)
            {
              setEncoding(realJavaEncoding);
            } else {
              throw SQLError.createSQLException("Unknown initial character set index '" + io.serverCharsetIndex + "' received from server. Initial client character set can be forced via the 'characterEncoding' property.", "S1000", getExceptionInterceptor());
            }
          }
          




          if ((versionMeetsMinimum(4, 1, 0)) && ("ISO8859_1".equalsIgnoreCase(serverEncodingToSet))) {
            serverEncodingToSet = "Cp1252";
          }
          if (("UnicodeBig".equalsIgnoreCase(serverEncodingToSet)) || ("UTF-16".equalsIgnoreCase(serverEncodingToSet)) || ("UTF-16LE".equalsIgnoreCase(serverEncodingToSet)) || ("UTF-32".equalsIgnoreCase(serverEncodingToSet)))
          {
            serverEncodingToSet = "UTF-8";
          }
          
          setEncoding(serverEncodingToSet);
        }
        catch (ArrayIndexOutOfBoundsException outOfBoundsEx) {
          if (realJavaEncoding != null)
          {
            setEncoding(realJavaEncoding);
          } else {
            throw SQLError.createSQLException("Unknown initial character set index '" + io.serverCharsetIndex + "' received from server. Initial client character set can be forced via the 'characterEncoding' property.", "S1000", getExceptionInterceptor());
          }
          
        }
        catch (SQLException ex)
        {
          throw ex;
        } catch (RuntimeException ex) {
          SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
          sqlEx.initCause(ex);
          throw sqlEx;
        }
        
        if (getEncoding() == null)
        {
          setEncoding("ISO8859_1");
        }
        



        if (getUseUnicode()) {
          if (realJavaEncoding != null)
          {



            if ((realJavaEncoding.equalsIgnoreCase("UTF-8")) || (realJavaEncoding.equalsIgnoreCase("UTF8")))
            {

              boolean utf8mb4Supported = versionMeetsMinimum(5, 5, 2);
              boolean useutf8mb4 = (utf8mb4Supported) && (CharsetMapping.UTF8MB4_INDEXES.contains(Integer.valueOf(io.serverCharsetIndex)));
              
              if (!getUseOldUTF8Behavior()) {
                if ((dontCheckServerMatch) || (!characterSetNamesMatches("utf8")) || ((utf8mb4Supported) && (!characterSetNamesMatches("utf8mb4")))) {
                  execSQL(null, "SET NAMES " + (useutf8mb4 ? "utf8mb4" : "utf8"), -1, null, 1003, 1007, false, database, null, false);
                  
                  serverVariables.put("character_set_client", useutf8mb4 ? "utf8mb4" : "utf8");
                  serverVariables.put("character_set_connection", useutf8mb4 ? "utf8mb4" : "utf8");
                }
              } else {
                execSQL(null, "SET NAMES latin1", -1, null, 1003, 1007, false, database, null, false);
                
                serverVariables.put("character_set_client", "latin1");
                serverVariables.put("character_set_connection", "latin1");
              }
              
              setEncoding(realJavaEncoding);
            } else {
              String mysqlCharsetName = CharsetMapping.getMysqlCharsetForJavaEncoding(realJavaEncoding.toUpperCase(Locale.ENGLISH), this);
              









              if (mysqlCharsetName != null)
              {
                if ((dontCheckServerMatch) || (!characterSetNamesMatches(mysqlCharsetName))) {
                  execSQL(null, "SET NAMES " + mysqlCharsetName, -1, null, 1003, 1007, false, database, null, false);
                  
                  serverVariables.put("character_set_client", mysqlCharsetName);
                  serverVariables.put("character_set_connection", mysqlCharsetName);
                }
              }
              


              setEncoding(realJavaEncoding);
            }
          } else if (getEncoding() != null)
          {
            String mysqlCharsetName = getServerCharset();
            
            if (getUseOldUTF8Behavior()) {
              mysqlCharsetName = "latin1";
            }
            
            boolean ucs2 = false;
            if (("ucs2".equalsIgnoreCase(mysqlCharsetName)) || ("utf16".equalsIgnoreCase(mysqlCharsetName)) || ("utf16le".equalsIgnoreCase(mysqlCharsetName)) || ("utf32".equalsIgnoreCase(mysqlCharsetName)))
            {
              mysqlCharsetName = "utf8";
              ucs2 = true;
              if (getCharacterSetResults() == null) {
                setCharacterSetResults("UTF-8");
              }
            }
            
            if ((dontCheckServerMatch) || (!characterSetNamesMatches(mysqlCharsetName)) || (ucs2)) {
              try {
                execSQL(null, "SET NAMES " + mysqlCharsetName, -1, null, 1003, 1007, false, database, null, false);
                
                serverVariables.put("character_set_client", mysqlCharsetName);
                serverVariables.put("character_set_connection", mysqlCharsetName);
              } catch (SQLException ex) {
                if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
                  throw ex;
                }
              }
            }
            
            realJavaEncoding = getEncoding();
          }
        }
        






        String onServer = null;
        boolean isNullOnServer = false;
        
        if (serverVariables != null) {
          onServer = (String)serverVariables.get("character_set_results");
          
          isNullOnServer = (onServer == null) || ("NULL".equalsIgnoreCase(onServer)) || (onServer.length() == 0);
        }
        
        if (getCharacterSetResults() == null)
        {



          if (!isNullOnServer) {
            try {
              execSQL(null, "SET character_set_results = NULL", -1, null, 1003, 1007, false, database, null, false);
            }
            catch (SQLException ex) {
              if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
                throw ex;
              }
            }
            serverVariables.put("jdbc.local.character_set_results", null);
          } else {
            serverVariables.put("jdbc.local.character_set_results", onServer);
          }
        }
        else {
          if (getUseOldUTF8Behavior()) {
            try {
              execSQL(null, "SET NAMES latin1", -1, null, 1003, 1007, false, database, null, false);
              
              serverVariables.put("character_set_client", "latin1");
              serverVariables.put("character_set_connection", "latin1");
            } catch (SQLException ex) {
              if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
                throw ex;
              }
            }
          }
          String charsetResults = getCharacterSetResults();
          String mysqlEncodingName = null;
          
          if (("UTF-8".equalsIgnoreCase(charsetResults)) || ("UTF8".equalsIgnoreCase(charsetResults))) {
            mysqlEncodingName = "utf8";
          } else if ("null".equalsIgnoreCase(charsetResults)) {
            mysqlEncodingName = "NULL";
          } else {
            mysqlEncodingName = CharsetMapping.getMysqlCharsetForJavaEncoding(charsetResults.toUpperCase(Locale.ENGLISH), this);
          }
          




          if (mysqlEncodingName == null) {
            throw SQLError.createSQLException("Can't map " + charsetResults + " given for characterSetResults to a supported MySQL encoding.", "S1009", getExceptionInterceptor());
          }
          

          if (!mysqlEncodingName.equalsIgnoreCase((String)serverVariables.get("character_set_results"))) {
            StringBuilder setBuf = new StringBuilder("SET character_set_results = ".length() + mysqlEncodingName.length());
            setBuf.append("SET character_set_results = ").append(mysqlEncodingName);
            try
            {
              execSQL(null, setBuf.toString(), -1, null, 1003, 1007, false, database, null, false);
            }
            catch (SQLException ex) {
              if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
                throw ex;
              }
            }
            
            serverVariables.put("jdbc.local.character_set_results", mysqlEncodingName);
            

            if (versionMeetsMinimum(5, 5, 0)) {
              errorMessageEncoding = charsetResults;
            }
          }
          else {
            serverVariables.put("jdbc.local.character_set_results", onServer);
          }
        }
        
        if (getConnectionCollation() != null) {
          StringBuilder setBuf = new StringBuilder("SET collation_connection = ".length() + getConnectionCollation().length());
          setBuf.append("SET collation_connection = ").append(getConnectionCollation());
          try
          {
            execSQL(null, setBuf.toString(), -1, null, 1003, 1007, false, database, null, false);
          } catch (SQLException ex) {
            if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
              throw ex;
            }
          }
        }
      }
      else {
        realJavaEncoding = getEncoding();
      }
      
    }
    finally
    {
      setEncoding(realJavaEncoding);
    }
    



    try
    {
      CharsetEncoder enc = Charset.forName(getEncoding()).newEncoder();
      CharBuffer cbuf = CharBuffer.allocate(1);
      ByteBuffer bbuf = ByteBuffer.allocate(1);
      
      cbuf.put("¥");
      cbuf.position(0);
      enc.encode(cbuf, bbuf, true);
      if (bbuf.get(0) == 92) {
        requiresEscapingEncoder = true;
      } else {
        cbuf.clear();
        bbuf.clear();
        
        cbuf.put("₩");
        cbuf.position(0);
        enc.encode(cbuf, bbuf, true);
        if (bbuf.get(0) == 92) {
          requiresEscapingEncoder = true;
        }
      }
    }
    catch (UnsupportedCharsetException ucex) {
      try {
        byte[] bbuf = StringUtils.getBytes("¥", getEncoding());
        if (bbuf[0] == 92) {
          requiresEscapingEncoder = true;
        } else {
          bbuf = StringUtils.getBytes("₩", getEncoding());
          if (bbuf[0] == 92) {
            requiresEscapingEncoder = true;
          }
        }
      } catch (UnsupportedEncodingException ueex) {
        throw SQLError.createSQLException("Unable to use encoding: " + getEncoding(), "S1000", ueex, getExceptionInterceptor());
      }
    }
    

    return characterSetAlreadyConfigured;
  }
  





  private void configureTimezone()
    throws SQLException
  {
    String configuredTimeZoneOnServer = (String)serverVariables.get("timezone");
    
    if (configuredTimeZoneOnServer == null) {
      configuredTimeZoneOnServer = (String)serverVariables.get("time_zone");
      
      if ("SYSTEM".equalsIgnoreCase(configuredTimeZoneOnServer)) {
        configuredTimeZoneOnServer = (String)serverVariables.get("system_time_zone");
      }
    }
    
    String canonicalTimezone = getServerTimezone();
    
    if (((getUseTimezone()) || (!getUseLegacyDatetimeCode())) && (configuredTimeZoneOnServer != null))
    {
      if ((canonicalTimezone == null) || (StringUtils.isEmptyOrWhitespaceOnly(canonicalTimezone))) {
        try {
          canonicalTimezone = TimeUtil.getCanonicalTimezone(configuredTimeZoneOnServer, getExceptionInterceptor());
        } catch (IllegalArgumentException iae) {
          throw SQLError.createSQLException(iae.getMessage(), "S1000", getExceptionInterceptor());
        }
      }
    }
    
    if ((canonicalTimezone != null) && (canonicalTimezone.length() > 0)) {
      serverTimezoneTZ = TimeZone.getTimeZone(canonicalTimezone);
      



      if ((!canonicalTimezone.equalsIgnoreCase("GMT")) && (serverTimezoneTZ.getID().equals("GMT"))) {
        throw SQLError.createSQLException("No timezone mapping entry for '" + canonicalTimezone + "'", "S1009", getExceptionInterceptor());
      }
      

      isServerTzUTC = ((!serverTimezoneTZ.useDaylightTime()) && (serverTimezoneTZ.getRawOffset() == 0));
    }
  }
  
  private void createInitialHistogram(long[] breakpoints, long lowerBound, long upperBound)
  {
    double bucketSize = (upperBound - lowerBound) / 20.0D * 1.25D;
    
    if (bucketSize < 1.0D) {
      bucketSize = 1.0D;
    }
    
    for (int i = 0; i < 20; i++) {
      breakpoints[i] = lowerBound;
      lowerBound = (lowerBound + bucketSize);
    }
  }
  








  public void createNewIO(boolean isForReconnect)
    throws SQLException
  {
    synchronized (getConnectionMutex())
    {



      Properties mergedProps = exposeAsProperties(props);
      
      if (!getHighAvailability()) {
        connectOneTryOnly(isForReconnect, mergedProps);
        
        return;
      }
      
      connectWithRetries(isForReconnect, mergedProps);
    }
  }
  
  private void connectWithRetries(boolean isForReconnect, Properties mergedProps) throws SQLException {
    double timeout = getInitialTimeout();
    boolean connectionGood = false;
    
    Exception connectionException = null;
    
    for (int attemptCount = 0; (attemptCount < getMaxReconnects()) && (!connectionGood); attemptCount++) {
      try {
        if (io != null) {
          io.forceClose();
        }
        
        coreConnect(mergedProps);
        pingInternal(false, 0);
        
        boolean oldAutoCommit;
        
        int oldIsolationLevel;
        boolean oldReadOnly;
        String oldCatalog;
        synchronized (getConnectionMutex()) {
          connectionId = io.getThreadId();
          isClosed = false;
          

          oldAutoCommit = getAutoCommit();
          oldIsolationLevel = isolationLevel;
          oldReadOnly = isReadOnly(false);
          oldCatalog = getCatalog();
          
          io.setStatementInterceptors(statementInterceptors);
        }
        

        initializePropsFromServer();
        
        if (isForReconnect)
        {
          setAutoCommit(oldAutoCommit);
          
          if (hasIsolationLevels) {
            setTransactionIsolation(oldIsolationLevel);
          }
          
          setCatalog(oldCatalog);
          setReadOnly(oldReadOnly);
        }
        
        connectionGood = true;
      }
      catch (Exception EEE)
      {
        connectionException = EEE;
        connectionGood = false;
        

        if (!connectionGood) break label190; }
      break;
      
      label190:
      if (attemptCount > 0) {
        try {
          Thread.sleep(timeout * 1000L);
        }
        catch (InterruptedException IE) {}
      }
    }
    

    if (!connectionGood)
    {
      SQLException chainedEx = SQLError.createSQLException(Messages.getString("Connection.UnableToConnectWithRetries", new Object[] { Integer.valueOf(getMaxReconnects()) }), "08001", getExceptionInterceptor());
      

      chainedEx.initCause(connectionException);
      
      throw chainedEx;
    }
    
    if ((getParanoid()) && (!getHighAvailability())) {
      password = null;
      user = null;
    }
    
    if (isForReconnect)
    {


      Iterator<Statement> statementIter = openStatements.iterator();
      




      Stack<Statement> serverPreparedStatements = null;
      
      while (statementIter.hasNext()) {
        Statement statementObj = (Statement)statementIter.next();
        
        if ((statementObj instanceof ServerPreparedStatement)) {
          if (serverPreparedStatements == null) {
            serverPreparedStatements = new Stack();
          }
          
          serverPreparedStatements.add(statementObj);
        }
      }
      
      if (serverPreparedStatements != null) {
        while (!serverPreparedStatements.isEmpty()) {
          ((ServerPreparedStatement)serverPreparedStatements.pop()).rePrepare();
        }
      }
    }
  }
  
  private void coreConnect(Properties mergedProps) throws SQLException, IOException {
    int newPort = 3306;
    String newHost = "localhost";
    
    String protocol = mergedProps.getProperty("PROTOCOL");
    
    if (protocol != null)
    {

      if ("tcp".equalsIgnoreCase(protocol)) {
        newHost = normalizeHost(mergedProps.getProperty("HOST"));
        newPort = parsePortNumber(mergedProps.getProperty("PORT", "3306"));
      } else if ("pipe".equalsIgnoreCase(protocol)) {
        setSocketFactoryClassName(NamedPipeSocketFactory.class.getName());
        
        String path = mergedProps.getProperty("PATH");
        
        if (path != null) {
          mergedProps.setProperty("namedPipePath", path);
        }
      }
      else {
        newHost = normalizeHost(mergedProps.getProperty("HOST"));
        newPort = parsePortNumber(mergedProps.getProperty("PORT", "3306"));
      }
    }
    else {
      String[] parsedHostPortPair = NonRegisteringDriver.parseHostPortPair(hostPortPair);
      newHost = parsedHostPortPair[0];
      
      newHost = normalizeHost(newHost);
      
      if (parsedHostPortPair[1] != null) {
        newPort = parsePortNumber(parsedHostPortPair[1]);
      }
    }
    
    port = newPort;
    host = newHost;
    

    sessionMaxRows = -1;
    

    serverVariables = new HashMap();
    serverVariables.put("character_set_server", "utf8");
    
    io = new MysqlIO(newHost, newPort, mergedProps, getSocketFactoryClassName(), getProxy(), getSocketTimeout(), largeRowSizeThreshold.getValueAsInt());
    
    io.doHandshake(user, password, database);
    if (versionMeetsMinimum(5, 5, 0))
    {
      errorMessageEncoding = io.getEncodingForHandshake();
    }
  }
  
  private String normalizeHost(String hostname) {
    if ((hostname == null) || (StringUtils.isEmptyOrWhitespaceOnly(hostname))) {
      return "localhost";
    }
    
    return hostname;
  }
  
  private int parsePortNumber(String portAsString) throws SQLException {
    int portNumber = 3306;
    try {
      portNumber = Integer.parseInt(portAsString);
    } catch (NumberFormatException nfe) {
      throw SQLError.createSQLException("Illegal connection port value '" + portAsString + "'", "01S00", getExceptionInterceptor());
    }
    
    return portNumber;
  }
  
  private void connectOneTryOnly(boolean isForReconnect, Properties mergedProps) throws SQLException {
    Exception connectionNotEstablishedBecause = null;
    
    try
    {
      coreConnect(mergedProps);
      connectionId = io.getThreadId();
      isClosed = false;
      

      boolean oldAutoCommit = getAutoCommit();
      int oldIsolationLevel = isolationLevel;
      boolean oldReadOnly = isReadOnly(false);
      String oldCatalog = getCatalog();
      
      io.setStatementInterceptors(statementInterceptors);
      

      initializePropsFromServer();
      
      if (isForReconnect)
      {
        setAutoCommit(oldAutoCommit);
        
        if (hasIsolationLevels) {
          setTransactionIsolation(oldIsolationLevel);
        }
        
        setCatalog(oldCatalog);
        
        setReadOnly(oldReadOnly);
      }
      return;
    }
    catch (Exception EEE)
    {
      if (((EEE instanceof SQLException)) && (((SQLException)EEE).getErrorCode() == 1820) && (!getDisconnectOnExpiredPasswords()))
      {
        return;
      }
      
      if (io != null) {
        io.forceClose();
      }
      
      connectionNotEstablishedBecause = EEE;
      
      if ((EEE instanceof SQLException)) {
        throw ((SQLException)EEE);
      }
      
      SQLException chainedEx = SQLError.createSQLException(Messages.getString("Connection.UnableToConnect"), "08001", getExceptionInterceptor());
      
      chainedEx.initCause(connectionNotEstablishedBecause);
      
      throw chainedEx;
    }
  }
  
  private void createPreparedStatementCaches() throws SQLException {
    synchronized (getConnectionMutex()) {
      int cacheSize = getPreparedStatementCacheSize();
      

      try
      {
        Class<?> factoryClass = Class.forName(getParseInfoCacheFactory());
        

        CacheAdapterFactory<String, PreparedStatement.ParseInfo> cacheFactory = (CacheAdapterFactory)factoryClass.newInstance();
        
        cachedPreparedStatementParams = cacheFactory.getInstance(this, myURL, getPreparedStatementCacheSize(), getPreparedStatementCacheSqlLimit(), props);
      }
      catch (ClassNotFoundException e)
      {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantFindCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      } catch (InstantiationException e) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantLoadCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      } catch (IllegalAccessException e) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantLoadCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      }
      
      if (getUseServerPreparedStmts()) {
        serverSideStatementCheckCache = new LRUCache(cacheSize);
        
        serverSideStatementCache = new LRUCache(cacheSize)
        {
          private static final long serialVersionUID = 7692318650375988114L;
          
          protected boolean removeEldestEntry(Map.Entry<ConnectionImpl.CompoundCacheKey, ServerPreparedStatement> eldest)
          {
            if (maxElements <= 1) {
              return false;
            }
            
            boolean removeIt = super.removeEldestEntry(eldest);
            
            if (removeIt) {
              ServerPreparedStatement ps = (ServerPreparedStatement)eldest.getValue();
              isCached = false;
              ps.setClosed(false);
              try
              {
                ps.close();
              }
              catch (SQLException sqlEx) {}
            }
            

            return removeIt;
          }
        };
      }
    }
  }
  







  public java.sql.Statement createStatement()
    throws SQLException
  {
    return createStatement(1003, 1007);
  }
  










  public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    checkClosed();
    
    StatementImpl stmt = new StatementImpl(getMultiHostSafeProxy(), database);
    stmt.setResultSetType(resultSetType);
    stmt.setResultSetConcurrency(resultSetConcurrency);
    
    return stmt;
  }
  

  public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
    throws SQLException
  {
    if ((getPedantic()) && 
      (resultSetHoldability != 1)) {
      throw SQLError.createSQLException("HOLD_CUSRORS_OVER_COMMIT is only supported holdability level", "S1009", getExceptionInterceptor());
    }
    


    return createStatement(resultSetType, resultSetConcurrency);
  }
  
  public void dumpTestcaseQuery(String query) {
    System.err.println(query);
  }
  
  public Connection duplicate() throws SQLException {
    return new ConnectionImpl(origHostToConnectTo, origPortToConnectTo, props, origDatabaseToConnectTo, myURL);
  }
  

































  public ResultSetInternalMethods execSQL(StatementImpl callingStatement, String sql, int maxRows, Buffer packet, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata)
    throws SQLException
  {
    return execSQL(callingStatement, sql, maxRows, packet, resultSetType, resultSetConcurrency, streamResults, catalog, cachedMetadata, false);
  }
  
  public ResultSetInternalMethods execSQL(StatementImpl callingStatement, String sql, int maxRows, Buffer packet, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata, boolean isBatch) throws SQLException
  {
    synchronized (getConnectionMutex())
    {



      long queryStartTime = 0L;
      
      int endOfQueryPacketPosition = 0;
      
      if (packet != null) {
        endOfQueryPacketPosition = packet.getPosition();
      }
      
      if (getGatherPerformanceMetrics()) {
        queryStartTime = System.currentTimeMillis();
      }
      
      lastQueryFinishedTime = 0L;
      
      if ((getHighAvailability()) && ((autoCommit) || (getAutoReconnectForPools())) && (needsPing) && (!isBatch)) {
        try {
          pingInternal(false, 0);
          
          needsPing = false;
        } catch (Exception Ex) {
          createNewIO(true);
        }
      }
      try
      {
        if (packet == null) {
          encoding = null;
          
          if (getUseUnicode()) {
            encoding = getEncoding();
          }
          
          ResultSetInternalMethods localResultSetInternalMethods = io.sqlQueryDirect(callingStatement, sql, encoding, null, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, cachedMetadata);jsr 262;return localResultSetInternalMethods;
        }
        

        String encoding = io.sqlQueryDirect(callingStatement, null, null, packet, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, cachedMetadata);jsr 228;return encoding;

      }
      catch (SQLException sqlE)
      {
        if (getDumpQueriesOnException()) {
          String extractedSql = extractSqlFromPacket(sql, packet, endOfQueryPacketPosition);
          StringBuilder messageBuf = new StringBuilder(extractedSql.length() + 32);
          messageBuf.append("\n\nQuery being executed when exception was thrown:\n");
          messageBuf.append(extractedSql);
          messageBuf.append("\n\n");
          
          sqlE = appendMessageToException(sqlE, messageBuf.toString(), getExceptionInterceptor());
        }
        
        if (getHighAvailability()) {
          if ("08S01".equals(sqlE.getSQLState()))
          {
            io.forceClose();
          }
          needsPing = true;
        } else if ("08S01".equals(sqlE.getSQLState())) {
          cleanup(sqlE);
        }
        
        throw sqlE;
      } catch (Exception ex) {
        if (getHighAvailability()) {
          if ((ex instanceof IOException))
          {
            io.forceClose();
          }
          needsPing = true;
        } else if ((ex instanceof IOException)) {
          cleanup(ex);
        }
        
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.UnexpectedException"), "S1000", getExceptionInterceptor());
        
        sqlEx.initCause(ex);
        
        throw sqlEx;
      } finally {
        jsr 6; } localObject2 = returnAddress; if (getMaintainTimeStats()) {
        lastQueryFinishedTime = System.currentTimeMillis();
      }
      
      if (getGatherPerformanceMetrics()) {
        long queryTime = System.currentTimeMillis() - queryStartTime;
        
        registerQueryExecutionTime(queryTime); }
      ret;
    }
  }
  
  public String extractSqlFromPacket(String possibleSqlQuery, Buffer queryPacket, int endOfQueryPacketPosition)
    throws SQLException
  {
    String extractedSql = null;
    
    if (possibleSqlQuery != null) {
      if (possibleSqlQuery.length() > getMaxQuerySizeToLog()) {
        StringBuilder truncatedQueryBuf = new StringBuilder(possibleSqlQuery.substring(0, getMaxQuerySizeToLog()));
        truncatedQueryBuf.append(Messages.getString("MysqlIO.25"));
        extractedSql = truncatedQueryBuf.toString();
      } else {
        extractedSql = possibleSqlQuery;
      }
    }
    
    if (extractedSql == null)
    {

      int extractPosition = endOfQueryPacketPosition;
      
      boolean truncated = false;
      
      if (endOfQueryPacketPosition > getMaxQuerySizeToLog()) {
        extractPosition = getMaxQuerySizeToLog();
        truncated = true;
      }
      
      extractedSql = StringUtils.toString(queryPacket.getByteBuffer(), 5, extractPosition - 5);
      
      if (truncated) {
        extractedSql = extractedSql + Messages.getString("MysqlIO.25");
      }
    }
    
    return extractedSql;
  }
  
  public StringBuilder generateConnectionCommentBlock(StringBuilder buf)
  {
    buf.append("/* conn id ");
    buf.append(getId());
    buf.append(" clock: ");
    buf.append(System.currentTimeMillis());
    buf.append(" */ ");
    
    return buf;
  }
  
  public int getActiveStatementCount() {
    return openStatements.size();
  }
  






  public boolean getAutoCommit()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      return autoCommit;
    }
  }
  



  public Calendar getCalendarInstanceForSessionOrNew()
  {
    if (getDynamicCalendars()) {
      return Calendar.getInstance();
    }
    
    return getSessionLockedCalendar();
  }
  









  public String getCatalog()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      return database;
    }
  }
  


  public String getCharacterSetMetadata()
  {
    synchronized (getConnectionMutex()) {
      return characterSetMetadata;
    }
  }
  






  public SingleByteCharsetConverter getCharsetConverter(String javaEncodingName)
    throws SQLException
  {
    if (javaEncodingName == null) {
      return null;
    }
    
    if (usePlatformCharsetConverters) {
      return null;
    }
    
    SingleByteCharsetConverter converter = null;
    
    synchronized (charsetConverterMap) {
      Object asObject = charsetConverterMap.get(javaEncodingName);
      
      if (asObject == CHARSET_CONVERTER_NOT_AVAILABLE_MARKER) {
        return null;
      }
      
      converter = (SingleByteCharsetConverter)asObject;
      
      if (converter == null) {
        try {
          converter = SingleByteCharsetConverter.getInstance(javaEncodingName, this);
          
          if (converter == null) {
            charsetConverterMap.put(javaEncodingName, CHARSET_CONVERTER_NOT_AVAILABLE_MARKER);
          } else {
            charsetConverterMap.put(javaEncodingName, converter);
          }
        } catch (UnsupportedEncodingException unsupEncEx) {
          charsetConverterMap.put(javaEncodingName, CHARSET_CONVERTER_NOT_AVAILABLE_MARKER);
          
          converter = null;
        }
      }
    }
    
    return converter;
  }
  

  @Deprecated
  public String getCharsetNameForIndex(int charsetIndex)
    throws SQLException
  {
    return getEncodingForIndex(charsetIndex);
  }
  








  public String getEncodingForIndex(int charsetIndex)
    throws SQLException
  {
    String javaEncoding = null;
    
    if (getUseOldUTF8Behavior()) {
      return getEncoding();
    }
    
    if (charsetIndex != -1)
    {
      try
      {
        if (indexToCustomMysqlCharset != null) {
          String cs = (String)indexToCustomMysqlCharset.get(Integer.valueOf(charsetIndex));
          if (cs != null) {
            javaEncoding = CharsetMapping.getJavaEncodingForMysqlCharset(cs, getEncoding());
          }
        }
        
        if (javaEncoding == null) {
          javaEncoding = CharsetMapping.getJavaEncodingForCollationIndex(Integer.valueOf(charsetIndex), getEncoding());
        }
      }
      catch (ArrayIndexOutOfBoundsException outOfBoundsEx) {
        throw SQLError.createSQLException("Unknown character set index for field '" + charsetIndex + "' received from server.", "S1000", getExceptionInterceptor());
      }
      catch (RuntimeException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
        sqlEx.initCause(ex);
        throw sqlEx;
      }
      

      if (javaEncoding == null) {
        javaEncoding = getEncoding();
      }
    } else {
      javaEncoding = getEncoding();
    }
    
    return javaEncoding;
  }
  



  public TimeZone getDefaultTimeZone()
  {
    return getCacheDefaultTimezone() ? defaultTimeZone : TimeUtil.getDefaultTimeZone(false);
  }
  
  public String getErrorMessageEncoding() {
    return errorMessageEncoding;
  }
  

  public int getHoldability()
    throws SQLException
  {
    return 2;
  }
  
  public long getId() {
    return connectionId;
  }
  







  public long getIdleFor()
  {
    synchronized (getConnectionMutex()) {
      if (lastQueryFinishedTime == 0L) {
        return 0L;
      }
      
      long now = System.currentTimeMillis();
      long idleTime = now - lastQueryFinishedTime;
      
      return idleTime;
    }
  }
  





  public MysqlIO getIO()
    throws SQLException
  {
    if ((io == null) || (isClosed)) {
      throw SQLError.createSQLException("Operation not allowed on closed connection", "08003", getExceptionInterceptor());
    }
    
    return io;
  }
  






  public Log getLog()
    throws SQLException
  {
    return log;
  }
  
  public int getMaxBytesPerChar(String javaCharsetName) throws SQLException {
    return getMaxBytesPerChar(null, javaCharsetName);
  }
  
  public int getMaxBytesPerChar(Integer charsetIndex, String javaCharsetName) throws SQLException
  {
    String charset = null;
    int res = 1;
    



    try
    {
      if (indexToCustomMysqlCharset != null) {
        charset = (String)indexToCustomMysqlCharset.get(charsetIndex);
      }
      
      if (charset == null) {
        charset = CharsetMapping.getMysqlCharsetNameForCollationIndex(charsetIndex);
      }
      

      if (charset == null) {
        charset = CharsetMapping.getMysqlCharsetForJavaEncoding(javaCharsetName, this);
      }
      

      Integer mblen = null;
      if (mysqlCharsetToCustomMblen != null) {
        mblen = (Integer)mysqlCharsetToCustomMblen.get(charset);
      }
      

      if (mblen == null) {
        mblen = Integer.valueOf(CharsetMapping.getMblen(charset));
      }
      
      if (mblen != null) {
        res = mblen.intValue();
      }
    } catch (SQLException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
      sqlEx.initCause(ex);
      throw sqlEx;
    }
    
    return res;
  }
  








  public java.sql.DatabaseMetaData getMetaData()
    throws SQLException
  {
    return getMetaData(true, true);
  }
  
  private java.sql.DatabaseMetaData getMetaData(boolean checkClosed, boolean checkForInfoSchema) throws SQLException {
    if (checkClosed) {
      checkClosed();
    }
    
    return DatabaseMetaData.getInstance(getMultiHostSafeProxy(), database, checkForInfoSchema);
  }
  
  public java.sql.Statement getMetadataSafeStatement() throws SQLException {
    return getMetadataSafeStatement(0);
  }
  
  public java.sql.Statement getMetadataSafeStatement(int maxRows) throws SQLException {
    java.sql.Statement stmt = createStatement();
    
    stmt.setMaxRows(maxRows == -1 ? 0 : maxRows);
    
    stmt.setEscapeProcessing(false);
    
    if (stmt.getFetchSize() != 0) {
      stmt.setFetchSize(0);
    }
    
    return stmt;
  }
  


  public int getNetBufferLength()
  {
    return netBufferLength;
  }
  


  @Deprecated
  public String getServerCharacterEncoding()
  {
    return getServerCharset();
  }
  




  public String getServerCharset()
  {
    if (io.versionMeetsMinimum(4, 1, 0)) {
      String charset = null;
      if (indexToCustomMysqlCharset != null) {
        charset = (String)indexToCustomMysqlCharset.get(Integer.valueOf(io.serverCharsetIndex));
      }
      if (charset == null) {
        charset = CharsetMapping.getMysqlCharsetNameForCollationIndex(Integer.valueOf(io.serverCharsetIndex));
      }
      return charset != null ? charset : (String)serverVariables.get("character_set_server");
    }
    return (String)serverVariables.get("character_set");
  }
  
  public int getServerMajorVersion() {
    return io.getServerMajorVersion();
  }
  
  public int getServerMinorVersion() {
    return io.getServerMinorVersion();
  }
  
  public int getServerSubMinorVersion() {
    return io.getServerSubMinorVersion();
  }
  
  public TimeZone getServerTimezoneTZ() {
    return serverTimezoneTZ;
  }
  
  public String getServerVariable(String variableName) {
    if (serverVariables != null) {
      return (String)serverVariables.get(variableName);
    }
    
    return null;
  }
  
  public String getServerVersion() {
    return io.getServerVersion();
  }
  
  public Calendar getSessionLockedCalendar()
  {
    return sessionCalendar;
  }
  






  public int getTransactionIsolation()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if ((hasIsolationLevels) && (!getUseLocalSessionState())) {
        java.sql.Statement stmt = null;
        ResultSet rs = null;
        try
        {
          stmt = getMetadataSafeStatement(sessionMaxRows);
          String query = (versionMeetsMinimum(8, 0, 3)) || ((versionMeetsMinimum(5, 7, 20)) && (!versionMeetsMinimum(8, 0, 0))) ? "SELECT @@session.transaction_isolation" : "SELECT @@session.tx_isolation";
          
          rs = stmt.executeQuery(query);
          
          if (rs.next()) {
            String s = rs.getString(1);
            
            if (s != null) {
              Integer intTI = (Integer)mapTransIsolationNameToValue.get(s);
              
              if (intTI != null) {
                isolationLevel = intTI.intValue();
                int i = isolationLevel;jsr 68;return i;
              }
            }
            
            throw SQLError.createSQLException("Could not map transaction isolation '" + s + " to a valid JDBC level.", "S1000", getExceptionInterceptor());
          }
          

          throw SQLError.createSQLException("Could not retrieve transaction isolation level from server", "S1000", getExceptionInterceptor());
        }
        finally
        {
          jsr 6; } localObject2 = returnAddress; if (rs != null) {
          try {
            rs.close();
          }
          catch (Exception ex) {}
          

          rs = null;
        }
        
        if (stmt != null) {
          try {
            stmt.close();
          }
          catch (Exception ex) {}
          

          stmt = null; } ret;
      }
      


      return isolationLevel;
    }
  }
  






  public Map<String, Class<?>> getTypeMap()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if (typeMap == null) {
        typeMap = new HashMap();
      }
      
      return typeMap;
    }
  }
  
  public String getURL() {
    return myURL;
  }
  
  public String getUser() {
    return user;
  }
  
  public Calendar getUtcCalendar() {
    return utcCalendar;
  }
  







  public SQLWarning getWarnings()
    throws SQLException
  {
    return null;
  }
  
  public boolean hasSameProperties(Connection c) {
    return props.equals(c.getProperties());
  }
  
  public Properties getProperties() {
    return props;
  }
  
  @Deprecated
  public boolean hasTriedMaster() {
    return hasTriedMasterFlag;
  }
  
  public void incrementNumberOfPreparedExecutes() {
    if (getGatherPerformanceMetrics()) {
      numberOfPreparedExecutes += 1L;
      

      numberOfQueriesIssued += 1L;
    }
  }
  
  public void incrementNumberOfPrepares() {
    if (getGatherPerformanceMetrics()) {
      numberOfPrepares += 1L;
    }
  }
  
  public void incrementNumberOfResultSetsCreated() {
    if (getGatherPerformanceMetrics()) {
      numberOfResultSetsCreated += 1L;
    }
  }
  





  private void initializeDriverProperties(Properties info)
    throws SQLException
  {
    initializeProperties(info);
    
    String exceptionInterceptorClasses = getExceptionInterceptors();
    
    if ((exceptionInterceptorClasses != null) && (!"".equals(exceptionInterceptorClasses))) {
      exceptionInterceptor = new ExceptionInterceptorChain(exceptionInterceptorClasses);
    }
    
    usePlatformCharsetConverters = getUseJvmCharsetConverters();
    
    log = LogFactory.getLogger(getLogger(), "MySQL", getExceptionInterceptor());
    
    if ((getProfileSql()) || (getUseUsageAdvisor())) {
      eventSink = ProfilerEventHandlerFactory.getInstance(getMultiHostSafeProxy());
    }
    
    if (getCachePreparedStatements()) {
      createPreparedStatementCaches();
    }
    
    if ((getNoDatetimeStringSync()) && (getUseTimezone())) {
      throw SQLError.createSQLException("Can't enable noDatetimeStringSync and useTimezone configuration properties at the same time", "01S00", getExceptionInterceptor());
    }
    

    if (getCacheCallableStatements()) {
      parsedCallableStatementCache = new LRUCache(getCallableStatementCacheSize());
    }
    
    if (getAllowMultiQueries()) {
      setCacheResultSetMetadata(false);
    }
    
    if (getCacheResultSetMetadata()) {
      resultSetMetadataCache = new LRUCache(getMetadataCacheSize());
    }
    
    if (getSocksProxyHost() != null) {
      setSocketFactoryClassName("com.mysql.jdbc.SocksProxySocketFactory");
    }
  }
  





  private void initializePropsFromServer()
    throws SQLException
  {
    String connectionInterceptorClasses = getConnectionLifecycleInterceptors();
    
    connectionLifecycleInterceptors = null;
    
    if (connectionInterceptorClasses != null) {
      connectionLifecycleInterceptors = Util.loadExtensions(this, props, connectionInterceptorClasses, "Connection.badLifecycleInterceptor", getExceptionInterceptor());
    }
    

    setSessionVariables();
    




    if (!versionMeetsMinimum(4, 1, 0)) {
      setTransformedBitIsBoolean(false);
    }
    
    parserKnowsUnicode = versionMeetsMinimum(4, 1, 0);
    



    if ((getUseServerPreparedStmts()) && (versionMeetsMinimum(4, 1, 0))) {
      useServerPreparedStmts = true;
      
      if ((versionMeetsMinimum(5, 0, 0)) && (!versionMeetsMinimum(5, 0, 3))) {
        useServerPreparedStmts = false;
      }
    }
    




    if (versionMeetsMinimum(3, 21, 22)) {
      loadServerVariables();
      
      if (versionMeetsMinimum(5, 0, 2)) {
        autoIncrementIncrement = getServerVariableAsInt("auto_increment_increment", 1);
      } else {
        autoIncrementIncrement = 1;
      }
      
      buildCollationMapping();
      


      if (io.serverCharsetIndex == 0) {
        String collationServer = (String)serverVariables.get("collation_server");
        if (collationServer != null) {
          for (int i = 1; i < CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME.length; i++) {
            if (CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME[i].equals(collationServer)) {
              io.serverCharsetIndex = i;
              break;
            }
            
          }
        } else {
          io.serverCharsetIndex = 45;
        }
      }
      
      LicenseConfiguration.checkLicenseType(serverVariables);
      
      String lowerCaseTables = (String)serverVariables.get("lower_case_table_names");
      
      lowerCaseTableNames = (("on".equalsIgnoreCase(lowerCaseTables)) || ("1".equalsIgnoreCase(lowerCaseTables)) || ("2".equalsIgnoreCase(lowerCaseTables)));
      
      storesLowerCaseTableName = (("1".equalsIgnoreCase(lowerCaseTables)) || ("on".equalsIgnoreCase(lowerCaseTables)));
      
      configureTimezone();
      
      if (serverVariables.containsKey("max_allowed_packet")) {
        int serverMaxAllowedPacket = getServerVariableAsInt("max_allowed_packet", -1);
        
        if ((serverMaxAllowedPacket != -1) && ((serverMaxAllowedPacket < getMaxAllowedPacket()) || (getMaxAllowedPacket() <= 0))) {
          setMaxAllowedPacket(serverMaxAllowedPacket);
        } else if ((serverMaxAllowedPacket == -1) && (getMaxAllowedPacket() == -1)) {
          setMaxAllowedPacket(65535);
        }
        
        if (getUseServerPrepStmts()) {
          int preferredBlobSendChunkSize = getBlobSendChunkSize();
          

          int packetHeaderSize = 8203;
          int allowedBlobSendChunkSize = Math.min(preferredBlobSendChunkSize, getMaxAllowedPacket()) - packetHeaderSize;
          
          if (allowedBlobSendChunkSize <= 0) {
            throw SQLError.createSQLException("Connection setting too low for 'maxAllowedPacket'. When 'useServerPrepStmts=true', 'maxAllowedPacket' must be higher than " + packetHeaderSize + ". Check also 'max_allowed_packet' in MySQL configuration files.", "01S00", getExceptionInterceptor());
          }
          




          setBlobSendChunkSize(String.valueOf(allowedBlobSendChunkSize));
        }
      }
      
      if (serverVariables.containsKey("net_buffer_length")) {
        netBufferLength = getServerVariableAsInt("net_buffer_length", 16384);
      }
      
      checkTransactionIsolationLevel();
      
      if (!versionMeetsMinimum(4, 1, 0)) {
        checkServerEncoding();
      }
      
      io.checkForCharsetMismatch();
      
      if (serverVariables.containsKey("sql_mode")) {
        String sqlModeAsString = (String)serverVariables.get("sql_mode");
        if (StringUtils.isStrictlyNumeric(sqlModeAsString))
        {
          useAnsiQuotes = ((Integer.parseInt(sqlModeAsString) & 0x4) > 0);
        } else if (sqlModeAsString != null) {
          useAnsiQuotes = (sqlModeAsString.indexOf("ANSI_QUOTES") != -1);
          noBackslashEscapes = (sqlModeAsString.indexOf("NO_BACKSLASH_ESCAPES") != -1);
        }
      }
    }
    
    configureClientCharacterSet(false);
    try
    {
      errorMessageEncoding = CharsetMapping.getCharacterEncodingForErrorMessages(this);
    } catch (SQLException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
      sqlEx.initCause(ex);
      throw sqlEx;
    }
    
    if (versionMeetsMinimum(3, 23, 15)) {
      transactionsSupported = true;
      handleAutoCommitDefaults();
    } else {
      transactionsSupported = false;
    }
    
    if (versionMeetsMinimum(3, 23, 36)) {
      hasIsolationLevels = true;
    } else {
      hasIsolationLevels = false;
    }
    
    hasQuotedIdentifiers = versionMeetsMinimum(3, 23, 6);
    
    io.resetMaxBuf();
    




    if (io.versionMeetsMinimum(4, 1, 0)) {
      String characterSetResultsOnServerMysql = (String)serverVariables.get("jdbc.local.character_set_results");
      
      if ((characterSetResultsOnServerMysql == null) || (StringUtils.startsWithIgnoreCaseAndWs(characterSetResultsOnServerMysql, "NULL")) || (characterSetResultsOnServerMysql.length() == 0))
      {
        String defaultMetadataCharsetMysql = (String)serverVariables.get("character_set_system");
        String defaultMetadataCharset = null;
        
        if (defaultMetadataCharsetMysql != null) {
          defaultMetadataCharset = CharsetMapping.getJavaEncodingForMysqlCharset(defaultMetadataCharsetMysql);
        } else {
          defaultMetadataCharset = "UTF-8";
        }
        
        characterSetMetadata = defaultMetadataCharset;
      } else {
        characterSetResultsOnServer = CharsetMapping.getJavaEncodingForMysqlCharset(characterSetResultsOnServerMysql);
        characterSetMetadata = characterSetResultsOnServer;
      }
    } else {
      characterSetMetadata = getEncoding();
    }
    




    if ((versionMeetsMinimum(4, 1, 0)) && (!versionMeetsMinimum(4, 1, 10)) && (getAllowMultiQueries()) && 
      (isQueryCacheEnabled())) {
      setAllowMultiQueries(false);
    }
    

    if ((versionMeetsMinimum(5, 0, 0)) && ((getUseLocalTransactionState()) || (getElideSetAutoCommits())) && (isQueryCacheEnabled()) && (!versionMeetsMinimum(5, 1, 32)))
    {

      setUseLocalTransactionState(false);
      setElideSetAutoCommits(false);
    }
    




    setupServerForTruncationChecks();
  }
  
  public boolean isQueryCacheEnabled() {
    return ("ON".equalsIgnoreCase((String)serverVariables.get("query_cache_type"))) && (!"0".equalsIgnoreCase((String)serverVariables.get("query_cache_size")));
  }
  
  private int getServerVariableAsInt(String variableName, int fallbackValue) throws SQLException {
    try {
      return Integer.parseInt((String)serverVariables.get(variableName));
    } catch (NumberFormatException nfe) {
      getLog().logWarn(Messages.getString("Connection.BadValueInServerVariables", new Object[] { variableName, serverVariables.get(variableName), Integer.valueOf(fallbackValue) }));
    }
    
    return fallbackValue;
  }
  



  private void handleAutoCommitDefaults()
    throws SQLException
  {
    boolean resetAutoCommitDefault = false;
    
    if (!getElideSetAutoCommits()) {
      String initConnectValue = (String)serverVariables.get("init_connect");
      if ((versionMeetsMinimum(4, 1, 2)) && (initConnectValue != null) && (initConnectValue.length() > 0))
      {
        ResultSet rs = null;
        java.sql.Statement stmt = null;
        try
        {
          stmt = getMetadataSafeStatement();
          rs = stmt.executeQuery("SELECT @@session.autocommit");
          if (rs.next()) {
            autoCommit = rs.getBoolean(1);
            resetAutoCommitDefault = !autoCommit;
          }
        } finally {
          if (rs != null) {
            try {
              rs.close();
            }
            catch (SQLException sqlEx) {}
          }
          
          if (stmt != null) {
            try {
              stmt.close();
            }
            catch (SQLException sqlEx) {}
          }
        }
      }
      else
      {
        resetAutoCommitDefault = true;
      }
    } else if (getIO().isSetNeededForAutoCommitMode(true))
    {
      autoCommit = false;
      resetAutoCommitDefault = true;
    }
    
    if (resetAutoCommitDefault) {
      try {
        setAutoCommit(true);
      } catch (SQLException ex) {
        if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
          throw ex;
        }
      }
    }
  }
  
  public boolean isClientTzUTC() {
    return isClientTzUTC;
  }
  
  public boolean isClosed() {
    return isClosed;
  }
  
  public boolean isCursorFetchEnabled() throws SQLException {
    return (versionMeetsMinimum(5, 0, 2)) && (getUseCursorFetch());
  }
  
  public boolean isInGlobalTx() {
    return isInGlobalTx;
  }
  






  public boolean isMasterConnection()
  {
    return false;
  }
  





  public boolean isNoBackslashEscapesSet()
  {
    return noBackslashEscapes;
  }
  
  public boolean isReadInfoMsgEnabled() {
    return readInfoMsg;
  }
  







  public boolean isReadOnly()
    throws SQLException
  {
    return isReadOnly(true);
  }
  











  public boolean isReadOnly(boolean useSessionStatus)
    throws SQLException
  {
    if ((useSessionStatus) && (!isClosed) && (versionMeetsMinimum(5, 6, 5)) && (!getUseLocalSessionState()) && (getReadOnlyPropagatesToServer())) {
      java.sql.Statement stmt = null;
      ResultSet rs = null;
      
      try
      {
        stmt = getMetadataSafeStatement(sessionMaxRows);
        
        rs = stmt.executeQuery((versionMeetsMinimum(8, 0, 3)) || ((versionMeetsMinimum(5, 7, 20)) && (!versionMeetsMinimum(8, 0, 0))) ? "select @@session.transaction_read_only" : "select @@session.tx_read_only");
        
        if (rs.next()) {
          boolean bool = rs.getInt(1) != 0;return bool;
        }
      } catch (SQLException ex1) {
        if ((ex1.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
          throw SQLError.createSQLException("Could not retrieve transaction read-only status from server", "S1000", ex1, getExceptionInterceptor());
        }
        
      }
      finally
      {
        jsr 6; } if (rs != null) {
        try {
          rs.close();
        }
        catch (Exception ex) {}
        

        rs = null;
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (Exception ex) {}
        

        stmt = null; } ret;
    }
    


    return readOnly;
  }
  
  public boolean isRunningOnJDK13() {
    return isRunningOnJDK13;
  }
  
  public boolean isSameResource(Connection otherConnection) {
    synchronized (getConnectionMutex()) {
      if (otherConnection == null) {
        return false;
      }
      
      boolean directCompare = true;
      
      String otherHost = origHostToConnectTo;
      String otherOrigDatabase = origDatabaseToConnectTo;
      String otherCurrentCatalog = database;
      
      if (!nullSafeCompare(otherHost, origHostToConnectTo)) {
        directCompare = false;
      } else if ((otherHost != null) && (otherHost.indexOf(',') == -1) && (otherHost.indexOf(':') == -1))
      {
        directCompare = origPortToConnectTo == origPortToConnectTo;
      }
      
      if ((directCompare) && (
        (!nullSafeCompare(otherOrigDatabase, origDatabaseToConnectTo)) || (!nullSafeCompare(otherCurrentCatalog, database)))) {
        directCompare = false;
      }
      

      if (directCompare) {
        return true;
      }
      

      String otherResourceId = ((ConnectionImpl)otherConnection).getResourceId();
      String myResourceId = getResourceId();
      
      if ((otherResourceId != null) || (myResourceId != null)) {
        directCompare = nullSafeCompare(otherResourceId, myResourceId);
        
        if (directCompare) {
          return true;
        }
      }
      
      return false;
    }
  }
  
  public boolean isServerTzUTC() {
    return isServerTzUTC;
  }
  
  private void createConfigCacheIfNeeded() throws SQLException {
    synchronized (getConnectionMutex()) {
      if (serverConfigCache != null) {
        return;
      }
      

      try
      {
        Class<?> factoryClass = Class.forName(getServerConfigCacheFactory());
        

        CacheAdapterFactory<String, Map<String, String>> cacheFactory = (CacheAdapterFactory)factoryClass.newInstance();
        
        serverConfigCache = cacheFactory.getInstance(this, myURL, Integer.MAX_VALUE, Integer.MAX_VALUE, props);
        
        ExceptionInterceptor evictOnCommsError = new ExceptionInterceptor()
        {
          public void init(Connection conn, Properties config)
            throws SQLException
          {}
          
          public void destroy() {}
          
          public SQLException interceptException(SQLException sqlEx, Connection conn)
          {
            if ((sqlEx.getSQLState() != null) && (sqlEx.getSQLState().startsWith("08"))) {
              serverConfigCache.invalidate(getURL());
            }
            return null;
          }
        };
        
        if (exceptionInterceptor == null) {
          exceptionInterceptor = evictOnCommsError;
        } else {
          ((ExceptionInterceptorChain)exceptionInterceptor).addRingZero(evictOnCommsError);
        }
      } catch (ClassNotFoundException e) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantFindCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      } catch (InstantiationException e) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantLoadCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      } catch (IllegalAccessException e) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.CantLoadCacheFactory", new Object[] { getParseInfoCacheFactory(), "parseInfoCacheFactory" }), getExceptionInterceptor());
        

        sqlEx.initCause(e);
        
        throw sqlEx;
      }
    }
  }
  








  private void loadServerVariables()
    throws SQLException
  {
    if (getCacheServerConfiguration()) {
      createConfigCacheIfNeeded();
      
      Map<String, String> cachedVariableMap = (Map)serverConfigCache.get(getURL());
      
      if (cachedVariableMap != null) {
        String cachedServerVersion = (String)cachedVariableMap.get("server_version_string");
        
        if ((cachedServerVersion != null) && (io.getServerVersion() != null) && (cachedServerVersion.equals(io.getServerVersion()))) {
          serverVariables = cachedVariableMap;
          
          return;
        }
        
        serverConfigCache.invalidate(getURL());
      }
    }
    
    java.sql.Statement stmt = null;
    ResultSet results = null;
    try
    {
      stmt = getMetadataSafeStatement();
      
      String version = dbmd.getDriverVersion();
      
      if ((version != null) && (version.indexOf('*') != -1)) {
        StringBuilder buf = new StringBuilder(version.length() + 10);
        
        for (int i = 0; i < version.length(); i++) {
          char c = version.charAt(i);
          
          if (c == '*') {
            buf.append("[star]");
          } else {
            buf.append(c);
          }
        }
        
        version = buf.toString();
      }
      
      String versionComment = "/* " + version + " */";
      
      serverVariables = new HashMap();
      
      boolean currentJdbcComplTrunc = getJdbcCompliantTruncation();
      setJdbcCompliantTruncation(false);
      try
      {
        if (versionMeetsMinimum(5, 1, 0)) {
          StringBuilder queryBuf = new StringBuilder(versionComment).append("SELECT");
          queryBuf.append("  @@session.auto_increment_increment AS auto_increment_increment");
          queryBuf.append(", @@character_set_client AS character_set_client");
          queryBuf.append(", @@character_set_connection AS character_set_connection");
          queryBuf.append(", @@character_set_results AS character_set_results");
          queryBuf.append(", @@character_set_server AS character_set_server");
          queryBuf.append(", @@collation_server AS collation_server");
          queryBuf.append(", @@init_connect AS init_connect");
          queryBuf.append(", @@interactive_timeout AS interactive_timeout");
          if (!versionMeetsMinimum(5, 5, 0)) {
            queryBuf.append(", @@language AS language");
          }
          queryBuf.append(", @@license AS license");
          queryBuf.append(", @@lower_case_table_names AS lower_case_table_names");
          queryBuf.append(", @@max_allowed_packet AS max_allowed_packet");
          queryBuf.append(", @@net_buffer_length AS net_buffer_length");
          queryBuf.append(", @@net_write_timeout AS net_write_timeout");
          if (!versionMeetsMinimum(8, 0, 3)) {
            queryBuf.append(", @@query_cache_size AS query_cache_size");
            queryBuf.append(", @@query_cache_type AS query_cache_type");
          }
          queryBuf.append(", @@sql_mode AS sql_mode");
          queryBuf.append(", @@system_time_zone AS system_time_zone");
          queryBuf.append(", @@time_zone AS time_zone");
          if ((versionMeetsMinimum(8, 0, 3)) || ((versionMeetsMinimum(5, 7, 20)) && (!versionMeetsMinimum(8, 0, 0)))) {
            queryBuf.append(", @@transaction_isolation AS transaction_isolation");
          } else {
            queryBuf.append(", @@tx_isolation AS transaction_isolation");
          }
          queryBuf.append(", @@wait_timeout AS wait_timeout");
          
          results = stmt.executeQuery(queryBuf.toString());
          if (results.next()) {
            ResultSetMetaData rsmd = results.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
              serverVariables.put(rsmd.getColumnLabel(i), results.getString(i));
            }
          }
        } else {
          results = stmt.executeQuery(versionComment + "SHOW VARIABLES");
          while (results.next()) {
            serverVariables.put(results.getString(1), results.getString(2));
          }
        }
        
        results.close();
        results = null;
      } catch (SQLException ex) {
        if ((ex.getErrorCode() != 1820) || (getDisconnectOnExpiredPasswords())) {
          throw ex;
        }
      } finally {
        setJdbcCompliantTruncation(currentJdbcComplTrunc);
      }
      
      if (getCacheServerConfiguration()) {
        serverVariables.put("server_version_string", io.getServerVersion());
        
        serverConfigCache.put(getURL(), serverVariables);
      }
    }
    catch (SQLException e) {
      throw e;
    } finally {
      if (results != null) {
        try {
          results.close();
        }
        catch (SQLException sqlE) {}
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (SQLException sqlE) {}
      }
    }
  }
  
  private int autoIncrementIncrement = 0;
  private ExceptionInterceptor exceptionInterceptor;
  
  public int getAutoIncrementIncrement() { return autoIncrementIncrement; }
  





  public boolean lowerCaseTableNames()
  {
    return lowerCaseTableNames;
  }
  










  public String nativeSQL(String sql)
    throws SQLException
  {
    if (sql == null) {
      return null;
    }
    
    Object escapedSqlResult = EscapeProcessor.escapeSQL(sql, serverSupportsConvertFn(), getMultiHostSafeProxy());
    
    if ((escapedSqlResult instanceof String)) {
      return (String)escapedSqlResult;
    }
    
    return escapedSql;
  }
  
  private CallableStatement parseCallableStatement(String sql) throws SQLException {
    Object escapedSqlResult = EscapeProcessor.escapeSQL(sql, serverSupportsConvertFn(), getMultiHostSafeProxy());
    
    boolean isFunctionCall = false;
    String parsedSql = null;
    
    if ((escapedSqlResult instanceof EscapeProcessorResult)) {
      parsedSql = escapedSql;
      isFunctionCall = callingStoredFunction;
    } else {
      parsedSql = (String)escapedSqlResult;
      isFunctionCall = false;
    }
    
    return CallableStatement.getInstance(getMultiHostSafeProxy(), parsedSql, database, isFunctionCall);
  }
  
  public boolean parserKnowsUnicode() {
    return parserKnowsUnicode;
  }
  




  public void ping()
    throws SQLException
  {
    pingInternal(true, 0);
  }
  
  public void pingInternal(boolean checkForClosedConnection, int timeoutMillis) throws SQLException {
    if (checkForClosedConnection) {
      checkClosed();
    }
    
    long pingMillisLifetime = getSelfDestructOnPingSecondsLifetime();
    int pingMaxOperations = getSelfDestructOnPingMaxOperations();
    
    if (((pingMillisLifetime > 0L) && (System.currentTimeMillis() - connectionCreationTimeMillis > pingMillisLifetime)) || ((pingMaxOperations > 0) && (pingMaxOperations <= io.getCommandCount())))
    {

      close();
      
      throw SQLError.createSQLException(Messages.getString("Connection.exceededConnectionLifetime"), "08S01", getExceptionInterceptor());
    }
    

    io.sendCommand(14, null, null, false, null, timeoutMillis);
  }
  



  public java.sql.CallableStatement prepareCall(String sql)
    throws SQLException
  {
    return prepareCall(sql, 1003, 1007);
  }
  













  public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    if (versionMeetsMinimum(5, 0, 0)) {
      CallableStatement cStmt = null;
      
      if (!getCacheCallableStatements())
      {
        cStmt = parseCallableStatement(sql);
      } else {
        synchronized (parsedCallableStatementCache) {
          CompoundCacheKey key = new CompoundCacheKey(getCatalog(), sql);
          
          CallableStatement.CallableStatementParamInfo cachedParamInfo = (CallableStatement.CallableStatementParamInfo)parsedCallableStatementCache.get(key);
          
          if (cachedParamInfo != null) {
            cStmt = CallableStatement.getInstance(getMultiHostSafeProxy(), cachedParamInfo);
          } else {
            cStmt = parseCallableStatement(sql);
            
            synchronized (cStmt) {
              cachedParamInfo = paramInfo;
            }
            
            parsedCallableStatementCache.put(key, cachedParamInfo);
          }
        }
      }
      
      cStmt.setResultSetType(resultSetType);
      cStmt.setResultSetConcurrency(resultSetConcurrency);
      
      return cStmt;
    }
    
    throw SQLError.createSQLException("Callable statements not supported.", "S1C00", getExceptionInterceptor());
  }
  

  public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
    throws SQLException
  {
    if ((getPedantic()) && 
      (resultSetHoldability != 1)) {
      throw SQLError.createSQLException("HOLD_CUSRORS_OVER_COMMIT is only supported holdability level", "S1009", getExceptionInterceptor());
    }
    


    CallableStatement cStmt = (CallableStatement)prepareCall(sql, resultSetType, resultSetConcurrency);
    
    return cStmt;
  }
  



















  public java.sql.PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    return prepareStatement(sql, 1003, 1007);
  }
  

  public java.sql.PreparedStatement prepareStatement(String sql, int autoGenKeyIndex)
    throws SQLException
  {
    java.sql.PreparedStatement pStmt = prepareStatement(sql);
    
    ((PreparedStatement)pStmt).setRetrieveGeneratedKeys(autoGenKeyIndex == 1);
    
    return pStmt;
  }
  













  public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      



      PreparedStatement pStmt = null;
      
      boolean canServerPrepare = true;
      
      String nativeSql = getProcessEscapeCodesForPrepStmts() ? nativeSQL(sql) : sql;
      
      if ((useServerPreparedStmts) && (getEmulateUnsupportedPstmts())) {
        canServerPrepare = canHandleAsServerPreparedStatement(nativeSql);
      }
      
      if ((useServerPreparedStmts) && (canServerPrepare)) {
        if (getCachePreparedStatements()) {
          synchronized (serverSideStatementCache) {
            pStmt = (PreparedStatement)serverSideStatementCache.remove(new CompoundCacheKey(database, sql));
            
            if (pStmt != null) {
              ((ServerPreparedStatement)pStmt).setClosed(false);
              pStmt.clearParameters();
            }
            
            if (pStmt == null) {
              try {
                pStmt = ServerPreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, database, resultSetType, resultSetConcurrency);
                
                if (sql.length() < getPreparedStatementCacheSqlLimit()) {
                  isCached = true;
                }
                
                pStmt.setResultSetType(resultSetType);
                pStmt.setResultSetConcurrency(resultSetConcurrency);
              }
              catch (SQLException sqlEx) {
                if (getEmulateUnsupportedPstmts()) {
                  pStmt = (PreparedStatement)clientPrepareStatement(nativeSql, resultSetType, resultSetConcurrency, false);
                  
                  if (sql.length() < getPreparedStatementCacheSqlLimit()) {
                    serverSideStatementCheckCache.put(sql, Boolean.FALSE);
                  }
                } else {
                  throw sqlEx;
                }
              }
            }
          }
        } else {
          try {
            pStmt = ServerPreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, database, resultSetType, resultSetConcurrency);
            
            pStmt.setResultSetType(resultSetType);
            pStmt.setResultSetConcurrency(resultSetConcurrency);
          }
          catch (SQLException sqlEx) {
            if (getEmulateUnsupportedPstmts()) {
              pStmt = (PreparedStatement)clientPrepareStatement(nativeSql, resultSetType, resultSetConcurrency, false);
            } else {
              throw sqlEx;
            }
          }
        }
      } else {
        pStmt = (PreparedStatement)clientPrepareStatement(nativeSql, resultSetType, resultSetConcurrency, false);
      }
      
      return pStmt;
    }
  }
  

  public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
    throws SQLException
  {
    if ((getPedantic()) && 
      (resultSetHoldability != 1)) {
      throw SQLError.createSQLException("HOLD_CUSRORS_OVER_COMMIT is only supported holdability level", "S1009", getExceptionInterceptor());
    }
    


    return prepareStatement(sql, resultSetType, resultSetConcurrency);
  }
  

  public java.sql.PreparedStatement prepareStatement(String sql, int[] autoGenKeyIndexes)
    throws SQLException
  {
    java.sql.PreparedStatement pStmt = prepareStatement(sql);
    
    ((PreparedStatement)pStmt).setRetrieveGeneratedKeys((autoGenKeyIndexes != null) && (autoGenKeyIndexes.length > 0));
    
    return pStmt;
  }
  

  public java.sql.PreparedStatement prepareStatement(String sql, String[] autoGenKeyColNames)
    throws SQLException
  {
    java.sql.PreparedStatement pStmt = prepareStatement(sql);
    
    ((PreparedStatement)pStmt).setRetrieveGeneratedKeys((autoGenKeyColNames != null) && (autoGenKeyColNames.length > 0));
    
    return pStmt;
  }
  








  public void realClose(boolean calledExplicitly, boolean issueRollback, boolean skipLocalTeardown, Throwable reason)
    throws SQLException
  {
    SQLException sqlEx = null;
    
    if (isClosed()) {
      return;
    }
    
    forceClosedReason = reason;
    try
    {
      if (!skipLocalTeardown) {
        if ((!getAutoCommit()) && (issueRollback)) {
          try {
            rollback();
          } catch (SQLException ex) {
            sqlEx = ex;
          }
        }
        
        reportMetrics();
        
        if (getUseUsageAdvisor()) {
          if (!calledExplicitly) {
            String message = "Connection implicitly closed by Driver. You should call Connection.close() from your code to free resources more efficiently and avoid resource leaks.";
            
            eventSink.consumeEvent(new ProfilerEvent((byte)0, "", getCatalog(), getId(), -1, -1, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
          }
          

          long connectionLifeTime = System.currentTimeMillis() - connectionCreationTimeMillis;
          
          if (connectionLifeTime < 500L) {
            String message = "Connection lifetime of < .5 seconds. You might be un-necessarily creating short-lived connections and should investigate connection pooling to be more efficient.";
            
            eventSink.consumeEvent(new ProfilerEvent((byte)0, "", getCatalog(), getId(), -1, -1, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
          }
        }
        
        try
        {
          closeAllOpenStatements();
        } catch (SQLException ex) {
          sqlEx = ex;
        }
        
        if (io != null) {
          try {
            io.quit();
          }
          catch (Exception e) {}
        }
      }
      else {
        io.forceClose();
      }
      
      if (statementInterceptors != null) {
        for (int i = 0; i < statementInterceptors.size(); i++) {
          ((StatementInterceptorV2)statementInterceptors.get(i)).destroy();
        }
      }
      
      if (exceptionInterceptor != null) {
        exceptionInterceptor.destroy();
      }
    } finally {
      openStatements.clear();
      if (io != null) {
        io.releaseResources();
        io = null;
      }
      statementInterceptors = null;
      exceptionInterceptor = null;
      ProfilerEventHandlerFactory.removeInstance(this);
      
      synchronized (getConnectionMutex()) {
        if (cancelTimer != null) {
          cancelTimer.cancel();
        }
      }
      
      isClosed = true;
    }
    
    if (sqlEx != null) {
      throw sqlEx;
    }
  }
  
  public void recachePreparedStatement(ServerPreparedStatement pstmt) throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if ((getCachePreparedStatements()) && (pstmt.isPoolable())) {
        synchronized (serverSideStatementCache) {
          Object oldServerPrepStmt = serverSideStatementCache.put(new CompoundCacheKey(currentCatalog, originalSql), pstmt);
          if ((oldServerPrepStmt != null) && (oldServerPrepStmt != pstmt)) {
            isCached = false;
            ((ServerPreparedStatement)oldServerPrepStmt).setClosed(false);
            ((ServerPreparedStatement)oldServerPrepStmt).realClose(true, true);
          }
        }
      }
    }
  }
  
  public void decachePreparedStatement(ServerPreparedStatement pstmt) throws SQLException {
    synchronized (getConnectionMutex()) {
      if ((getCachePreparedStatements()) && (pstmt.isPoolable())) {
        synchronized (serverSideStatementCache) {
          serverSideStatementCache.remove(new CompoundCacheKey(currentCatalog, originalSql));
        }
      }
    }
  }
  


  public void registerQueryExecutionTime(long queryTimeMs)
  {
    if (queryTimeMs > longestQueryTimeMs) {
      longestQueryTimeMs = queryTimeMs;
      
      repartitionPerformanceHistogram();
    }
    
    addToPerformanceHistogram(queryTimeMs, 1);
    
    if (queryTimeMs < shortestQueryTimeMs) {
      shortestQueryTimeMs = (queryTimeMs == 0L ? 1L : queryTimeMs);
    }
    
    numberOfQueriesIssued += 1L;
    
    totalQueryTimeMs += queryTimeMs;
  }
  





  public void registerStatement(Statement stmt)
  {
    openStatements.addIfAbsent(stmt);
  }
  







  private void repartitionHistogram(int[] histCounts, long[] histBreakpoints, long currentLowerBound, long currentUpperBound)
  {
    if (oldHistCounts == null) {
      oldHistCounts = new int[histCounts.length];
      oldHistBreakpoints = new long[histBreakpoints.length];
    }
    
    System.arraycopy(histCounts, 0, oldHistCounts, 0, histCounts.length);
    
    System.arraycopy(histBreakpoints, 0, oldHistBreakpoints, 0, histBreakpoints.length);
    
    createInitialHistogram(histBreakpoints, currentLowerBound, currentUpperBound);
    
    for (int i = 0; i < 20; i++) {
      addToHistogram(histCounts, histBreakpoints, oldHistBreakpoints[i], oldHistCounts[i], currentLowerBound, currentUpperBound);
    }
  }
  
  private void repartitionPerformanceHistogram() {
    checkAndCreatePerformanceHistogram();
    
    repartitionHistogram(perfMetricsHistCounts, perfMetricsHistBreakpoints, shortestQueryTimeMs == Long.MAX_VALUE ? 0L : shortestQueryTimeMs, longestQueryTimeMs);
  }
  
  private void repartitionTablesAccessedHistogram()
  {
    checkAndCreateTablesAccessedHistogram();
    
    repartitionHistogram(numTablesMetricsHistCounts, numTablesMetricsHistBreakpoints, minimumNumberTablesAccessed == Long.MAX_VALUE ? 0L : minimumNumberTablesAccessed, maximumNumberTablesAccessed);
  }
  
  private void reportMetrics()
  {
    if (getGatherPerformanceMetrics()) {
      StringBuilder logMessage = new StringBuilder(256);
      
      logMessage.append("** Performance Metrics Report **\n");
      logMessage.append("\nLongest reported query: " + longestQueryTimeMs + " ms");
      logMessage.append("\nShortest reported query: " + shortestQueryTimeMs + " ms");
      logMessage.append("\nAverage query execution time: " + totalQueryTimeMs / numberOfQueriesIssued + " ms");
      logMessage.append("\nNumber of statements executed: " + numberOfQueriesIssued);
      logMessage.append("\nNumber of result sets created: " + numberOfResultSetsCreated);
      logMessage.append("\nNumber of statements prepared: " + numberOfPrepares);
      logMessage.append("\nNumber of prepared statement executions: " + numberOfPreparedExecutes);
      
      if (perfMetricsHistBreakpoints != null) {
        logMessage.append("\n\n\tTiming Histogram:\n");
        int maxNumPoints = 20;
        int highestCount = Integer.MIN_VALUE;
        
        for (int i = 0; i < 20; i++) {
          if (perfMetricsHistCounts[i] > highestCount) {
            highestCount = perfMetricsHistCounts[i];
          }
        }
        
        if (highestCount == 0) {
          highestCount = 1;
        }
        
        for (int i = 0; i < 19; i++)
        {
          if (i == 0) {
            logMessage.append("\n\tless than " + perfMetricsHistBreakpoints[(i + 1)] + " ms: \t" + perfMetricsHistCounts[i]);
          } else {
            logMessage.append("\n\tbetween " + perfMetricsHistBreakpoints[i] + " and " + perfMetricsHistBreakpoints[(i + 1)] + " ms: \t" + perfMetricsHistCounts[i]);
          }
          

          logMessage.append("\t");
          
          int numPointsToGraph = (int)(maxNumPoints * (perfMetricsHistCounts[i] / highestCount));
          
          for (int j = 0; j < numPointsToGraph; j++) {
            logMessage.append("*");
          }
          
          if (longestQueryTimeMs < perfMetricsHistCounts[(i + 1)]) {
            break;
          }
        }
        
        if (perfMetricsHistBreakpoints[18] < longestQueryTimeMs) {
          logMessage.append("\n\tbetween ");
          logMessage.append(perfMetricsHistBreakpoints[18]);
          logMessage.append(" and ");
          logMessage.append(perfMetricsHistBreakpoints[19]);
          logMessage.append(" ms: \t");
          logMessage.append(perfMetricsHistCounts[19]);
        }
      }
      
      if (numTablesMetricsHistBreakpoints != null) {
        logMessage.append("\n\n\tTable Join Histogram:\n");
        int maxNumPoints = 20;
        int highestCount = Integer.MIN_VALUE;
        
        for (int i = 0; i < 20; i++) {
          if (numTablesMetricsHistCounts[i] > highestCount) {
            highestCount = numTablesMetricsHistCounts[i];
          }
        }
        
        if (highestCount == 0) {
          highestCount = 1;
        }
        
        for (int i = 0; i < 19; i++)
        {
          if (i == 0) {
            logMessage.append("\n\t" + numTablesMetricsHistBreakpoints[(i + 1)] + " tables or less: \t\t" + numTablesMetricsHistCounts[i]);
          } else {
            logMessage.append("\n\tbetween " + numTablesMetricsHistBreakpoints[i] + " and " + numTablesMetricsHistBreakpoints[(i + 1)] + " tables: \t" + numTablesMetricsHistCounts[i]);
          }
          

          logMessage.append("\t");
          
          int numPointsToGraph = (int)(maxNumPoints * (numTablesMetricsHistCounts[i] / highestCount));
          
          for (int j = 0; j < numPointsToGraph; j++) {
            logMessage.append("*");
          }
          
          if (maximumNumberTablesAccessed < numTablesMetricsHistBreakpoints[(i + 1)]) {
            break;
          }
        }
        
        if (numTablesMetricsHistBreakpoints[18] < maximumNumberTablesAccessed) {
          logMessage.append("\n\tbetween ");
          logMessage.append(numTablesMetricsHistBreakpoints[18]);
          logMessage.append(" and ");
          logMessage.append(numTablesMetricsHistBreakpoints[19]);
          logMessage.append(" tables: ");
          logMessage.append(numTablesMetricsHistCounts[19]);
        }
      }
      
      log.logInfo(logMessage);
      
      metricsLastReportedMs = System.currentTimeMillis();
    }
  }
  



  protected void reportMetricsIfNeeded()
  {
    if ((getGatherPerformanceMetrics()) && 
      (System.currentTimeMillis() - metricsLastReportedMs > getReportMetricsIntervalMillis())) {
      reportMetrics();
    }
  }
  
  public void reportNumberOfTablesAccessed(int numTablesAccessed)
  {
    if (numTablesAccessed < minimumNumberTablesAccessed) {
      minimumNumberTablesAccessed = numTablesAccessed;
    }
    
    if (numTablesAccessed > maximumNumberTablesAccessed) {
      maximumNumberTablesAccessed = numTablesAccessed;
      
      repartitionTablesAccessedHistogram();
    }
    
    addToTablesAccessedHistogram(numTablesAccessed, 1);
  }
  






  public void resetServerState()
    throws SQLException
  {
    if ((!getParanoid()) && (io != null) && (versionMeetsMinimum(4, 0, 6))) {
      changeUser(user, password);
    }
  }
  







  public void rollback()
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      try
      {
        if (connectionLifecycleInterceptors != null) {
          IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
          {
            void forEach(Extension each) throws SQLException
            {
              if (!((ConnectionLifecycleInterceptor)each).rollback()) {
                stopIterating = true;
              }
              
            }
          };
          iter.doForAll();
          
          if (!iter.fullIteration()) {
            jsr 115;return;
          }
        }
        
        if ((autoCommit) && (!getRelaxAutoCommit())) {
          throw SQLError.createSQLException("Can't call rollback when autocommit=true", "08003", getExceptionInterceptor());
        }
        if (transactionsSupported) {
          try {
            rollbackNoChecks();
          }
          catch (SQLException sqlEx) {
            if ((getIgnoreNonTxTables()) && (sqlEx.getErrorCode() == 1196)) {
              return;
            }
            throw sqlEx;
          }
        }
      }
      catch (SQLException sqlException) {
        if ("08S01".equals(sqlException.getSQLState())) {
          throw SQLError.createSQLException("Communications link failure during rollback(). Transaction resolution unknown.", "08007", getExceptionInterceptor());
        }
        

        throw sqlException;
      } finally {
        jsr 5; } localObject2 = returnAddress;needsPing = getReconnectAtTxEnd();ret;
    }
  }
  



  public void rollback(final Savepoint savepoint)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if ((versionMeetsMinimum(4, 0, 14)) || (versionMeetsMinimum(4, 1, 1))) {
        checkClosed();
        try
        {
          if (connectionLifecycleInterceptors != null) {
            IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
            {
              void forEach(Extension each) throws SQLException
              {
                if (!((ConnectionLifecycleInterceptor)each).rollback(savepoint)) {
                  stopIterating = true;
                }
                
              }
            };
            iter.doForAll();
            
            if (!iter.fullIteration()) {
              jsr 240;return;
            }
          }
          
          StringBuilder rollbackQuery = new StringBuilder("ROLLBACK TO SAVEPOINT ");
          rollbackQuery.append('`');
          rollbackQuery.append(savepoint.getSavepointName());
          rollbackQuery.append('`');
          
          java.sql.Statement stmt = null;
          try
          {
            stmt = getMetadataSafeStatement();
            
            stmt.executeUpdate(rollbackQuery.toString());
          } catch (SQLException sqlEx) {
            int errno = sqlEx.getErrorCode();
            
            if (errno == 1181) {
              String msg = sqlEx.getMessage();
              
              if (msg != null) {
                int indexOfError153 = msg.indexOf("153");
                
                if (indexOfError153 != -1) {
                  throw SQLError.createSQLException("Savepoint '" + savepoint.getSavepointName() + "' does not exist", "S1009", errno, getExceptionInterceptor());
                }
              }
            }
            


            if ((getIgnoreNonTxTables()) && (sqlEx.getErrorCode() != 1196)) {
              throw sqlEx;
            }
            
            if ("08S01".equals(sqlEx.getSQLState())) {
              throw SQLError.createSQLException("Communications link failure during rollback(). Transaction resolution unknown.", "08007", getExceptionInterceptor());
            }
            

            throw sqlEx;
          } finally {
            closeStatement(stmt);
          }
        } finally {
          jsr 6; } localObject4 = returnAddress;needsPing = getReconnectAtTxEnd();ret;
      }
      else {
        throw SQLError.createSQLFeatureNotSupportedException();
      }
    }
  }
  
  private void rollbackNoChecks() throws SQLException {
    if ((getUseLocalTransactionState()) && (versionMeetsMinimum(5, 0, 0)) && 
      (!io.inTransactionOnServer())) {
      return;
    }
    

    execSQL(null, "rollback", -1, null, 1003, 1007, false, database, null, false);
  }
  

  public java.sql.PreparedStatement serverPrepareStatement(String sql)
    throws SQLException
  {
    String nativeSql = getProcessEscapeCodesForPrepStmts() ? nativeSQL(sql) : sql;
    
    return ServerPreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, getCatalog(), 1003, 1007);
  }
  


  public java.sql.PreparedStatement serverPrepareStatement(String sql, int autoGenKeyIndex)
    throws SQLException
  {
    String nativeSql = getProcessEscapeCodesForPrepStmts() ? nativeSQL(sql) : sql;
    
    PreparedStatement pStmt = ServerPreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, getCatalog(), 1003, 1007);
    

    pStmt.setRetrieveGeneratedKeys(autoGenKeyIndex == 1);
    
    return pStmt;
  }
  

  public java.sql.PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    String nativeSql = getProcessEscapeCodesForPrepStmts() ? nativeSQL(sql) : sql;
    
    return ServerPreparedStatement.getInstance(getMultiHostSafeProxy(), nativeSql, getCatalog(), resultSetType, resultSetConcurrency);
  }
  


  public java.sql.PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
    throws SQLException
  {
    if ((getPedantic()) && 
      (resultSetHoldability != 1)) {
      throw SQLError.createSQLException("HOLD_CUSRORS_OVER_COMMIT is only supported holdability level", "S1009", getExceptionInterceptor());
    }
    


    return serverPrepareStatement(sql, resultSetType, resultSetConcurrency);
  }
  


  public java.sql.PreparedStatement serverPrepareStatement(String sql, int[] autoGenKeyIndexes)
    throws SQLException
  {
    PreparedStatement pStmt = (PreparedStatement)serverPrepareStatement(sql);
    
    pStmt.setRetrieveGeneratedKeys((autoGenKeyIndexes != null) && (autoGenKeyIndexes.length > 0));
    
    return pStmt;
  }
  

  public java.sql.PreparedStatement serverPrepareStatement(String sql, String[] autoGenKeyColNames)
    throws SQLException
  {
    PreparedStatement pStmt = (PreparedStatement)serverPrepareStatement(sql);
    
    pStmt.setRetrieveGeneratedKeys((autoGenKeyColNames != null) && (autoGenKeyColNames.length > 0));
    
    return pStmt;
  }
  
  public boolean serverSupportsConvertFn() throws SQLException {
    return versionMeetsMinimum(4, 0, 2);
  }
  
















  public void setAutoCommit(final boolean autoCommitFlag)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      
      if (connectionLifecycleInterceptors != null) {
        IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
        {
          void forEach(Extension each) throws SQLException
          {
            if (!((ConnectionLifecycleInterceptor)each).setAutoCommit(autoCommitFlag)) {
              stopIterating = true;
            }
            
          }
        };
        iter.doForAll();
        
        if (!iter.fullIteration()) {
          return;
        }
      }
      
      if (getAutoReconnectForPools()) {
        setHighAvailability(true);
      }
      try
      {
        if (transactionsSupported)
        {
          boolean needsSetOnServer = true;
          
          if ((getUseLocalSessionState()) && (autoCommit == autoCommitFlag)) {
            needsSetOnServer = false;
          } else if (!getHighAvailability()) {
            needsSetOnServer = getIO().isSetNeededForAutoCommitMode(autoCommitFlag);
          }
          



          autoCommit = autoCommitFlag;
          
          if (needsSetOnServer) {
            execSQL(null, autoCommitFlag ? "SET autocommit=1" : "SET autocommit=0", -1, null, 1003, 1007, false, database, null, false);
          }
        }
        else
        {
          if ((!autoCommitFlag) && (!getRelaxAutoCommit())) {
            throw SQLError.createSQLException("MySQL Versions Older than 3.23.15 do not support transactions", "08003", getExceptionInterceptor());
          }
          

          autoCommit = autoCommitFlag;
        }
      } finally {
        if (getAutoReconnectForPools()) {
          setHighAvailability(false);
        }
      }
      
      return;
    }
  }
  











  public void setCatalog(final String catalog)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      
      if (catalog == null) {
        throw SQLError.createSQLException("Catalog can not be null", "S1009", getExceptionInterceptor());
      }
      
      if (connectionLifecycleInterceptors != null) {
        IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
        {
          void forEach(Extension each) throws SQLException
          {
            if (!((ConnectionLifecycleInterceptor)each).setCatalog(catalog)) {
              stopIterating = true;
            }
            
          }
        };
        iter.doForAll();
        
        if (!iter.fullIteration()) {
          return;
        }
      }
      
      if (getUseLocalSessionState()) {
        if (lowerCaseTableNames) {
          if (!database.equalsIgnoreCase(catalog)) {}


        }
        else if (database.equals(catalog)) {
          return;
        }
      }
      

      String quotedId = dbmd.getIdentifierQuoteString();
      
      if ((quotedId == null) || (quotedId.equals(" "))) {
        quotedId = "";
      }
      
      StringBuilder query = new StringBuilder("USE ");
      query.append(StringUtils.quoteIdentifier(catalog, quotedId, getPedantic()));
      
      execSQL(null, query.toString(), -1, null, 1003, 1007, false, database, null, false);
      
      database = catalog;
    }
  }
  














  public void setInGlobalTx(boolean flag)
  {
    isInGlobalTx = flag;
  }
  









  public void setReadInfoMsgEnabled(boolean flag)
  {
    readInfoMsg = flag;
  }
  









  public void setReadOnly(boolean readOnlyFlag)
    throws SQLException
  {
    checkClosed();
    
    setReadOnlyInternal(readOnlyFlag);
  }
  
  public void setReadOnlyInternal(boolean readOnlyFlag) throws SQLException
  {
    if ((getReadOnlyPropagatesToServer()) && (versionMeetsMinimum(5, 6, 5)) && (
      (!getUseLocalSessionState()) || (readOnlyFlag != readOnly))) {
      execSQL(null, "set session transaction " + (readOnlyFlag ? "read only" : "read write"), -1, null, 1003, 1007, false, database, null, false);
    }
    


    readOnly = readOnlyFlag;
  }
  

  public Savepoint setSavepoint()
    throws SQLException
  {
    MysqlSavepoint savepoint = new MysqlSavepoint(getExceptionInterceptor());
    
    setSavepoint(savepoint);
    
    return savepoint;
  }
  
  private void setSavepoint(MysqlSavepoint savepoint) throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if ((versionMeetsMinimum(4, 0, 14)) || (versionMeetsMinimum(4, 1, 1))) {
        checkClosed();
        
        StringBuilder savePointQuery = new StringBuilder("SAVEPOINT ");
        savePointQuery.append('`');
        savePointQuery.append(savepoint.getSavepointName());
        savePointQuery.append('`');
        
        java.sql.Statement stmt = null;
        try
        {
          stmt = getMetadataSafeStatement();
          
          stmt.executeUpdate(savePointQuery.toString());
        } finally {
          closeStatement(stmt);
        }
      } else {
        throw SQLError.createSQLFeatureNotSupportedException();
      }
    }
  }
  

  public Savepoint setSavepoint(String name)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      MysqlSavepoint savepoint = new MysqlSavepoint(name, getExceptionInterceptor());
      
      setSavepoint(savepoint);
      
      return savepoint;
    }
  }
  
  private void setSessionVariables() throws SQLException {
    if ((versionMeetsMinimum(4, 0, 0)) && (getSessionVariables() != null)) {
      List<String> variablesToSet = new ArrayList();
      for (String part : StringUtils.split(getSessionVariables(), ",", "\"'(", "\"')", "\"'", true)) {
        variablesToSet.addAll(StringUtils.split(part, ";", "\"'(", "\"')", "\"'", true));
      }
      
      if (!variablesToSet.isEmpty()) {
        java.sql.Statement stmt = null;
        try {
          stmt = getMetadataSafeStatement();
          StringBuilder query = new StringBuilder("SET ");
          String separator = "";
          for (String variableToSet : variablesToSet) {
            if (variableToSet.length() > 0) {
              query.append(separator);
              if (!variableToSet.startsWith("@")) {
                query.append("SESSION ");
              }
              query.append(variableToSet);
              separator = ",";
            }
          }
          stmt.executeUpdate(query.toString());
        } finally {
          if (stmt != null) {
            stmt.close();
          }
        }
      }
    }
  }
  


  public void setTransactionIsolation(int level)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      
      if (hasIsolationLevels) {
        String sql = null;
        
        boolean shouldSendSet = false;
        
        if (getAlwaysSendSetIsolation()) {
          shouldSendSet = true;
        }
        else if (level != isolationLevel) {
          shouldSendSet = true;
        }
        

        if (getUseLocalSessionState()) {
          shouldSendSet = isolationLevel != level;
        }
        
        if (shouldSendSet) {
          switch (level) {
          case 0: 
            throw SQLError.createSQLException("Transaction isolation level NONE not supported by MySQL", getExceptionInterceptor());
          
          case 2: 
            sql = "SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED";
            
            break;
          
          case 1: 
            sql = "SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED";
            
            break;
          
          case 4: 
            sql = "SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ";
            
            break;
          
          case 8: 
            sql = "SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE";
            
            break;
          case 3: case 5: case 6: 
          case 7: default: 
            throw SQLError.createSQLException("Unsupported transaction isolation level '" + level + "'", "S1C00", getExceptionInterceptor());
          }
          
          
          execSQL(null, sql, -1, null, 1003, 1007, false, database, null, false);
          
          isolationLevel = level;
        }
      } else {
        throw SQLError.createSQLException("Transaction Isolation Levels are not supported on MySQL versions older than 3.23.36.", "S1C00", getExceptionInterceptor());
      }
    }
  }
  








  public void setTypeMap(Map<String, Class<?>> map)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      typeMap = map;
    }
  }
  
  private void setupServerForTruncationChecks() throws SQLException {
    if ((getJdbcCompliantTruncation()) && 
      (versionMeetsMinimum(5, 0, 2))) {
      String currentSqlMode = (String)serverVariables.get("sql_mode");
      
      boolean strictTransTablesIsSet = StringUtils.indexOfIgnoreCase(currentSqlMode, "STRICT_TRANS_TABLES") != -1;
      
      if ((currentSqlMode == null) || (currentSqlMode.length() == 0) || (!strictTransTablesIsSet)) {
        StringBuilder commandBuf = new StringBuilder("SET sql_mode='");
        
        if ((currentSqlMode != null) && (currentSqlMode.length() > 0)) {
          commandBuf.append(currentSqlMode);
          commandBuf.append(",");
        }
        
        commandBuf.append("STRICT_TRANS_TABLES'");
        
        execSQL(null, commandBuf.toString(), -1, null, 1003, 1007, false, database, null, false);
        
        setJdbcCompliantTruncation(false);
      } else if (strictTransTablesIsSet)
      {
        setJdbcCompliantTruncation(false);
      }
    }
  }
  





  public void shutdownServer()
    throws SQLException
  {
    try
    {
      if (versionMeetsMinimum(5, 7, 9)) {
        execSQL(null, "SHUTDOWN", -1, null, 1003, 1007, false, database, null, false);
      } else {
        io.sendCommand(8, null, null, false, null, 0);
      }
    } catch (Exception ex) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.UnhandledExceptionDuringShutdown"), "S1000", getExceptionInterceptor());
      

      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  
  public boolean supportsIsolationLevel() {
    return hasIsolationLevels;
  }
  
  public boolean supportsQuotedIdentifiers() {
    return hasQuotedIdentifiers;
  }
  
  public boolean supportsTransactions() {
    return transactionsSupported;
  }
  





  public void unregisterStatement(Statement stmt)
  {
    openStatements.remove(stmt);
  }
  
  public boolean useAnsiQuotedIdentifiers() {
    synchronized (getConnectionMutex()) {
      return useAnsiQuotes;
    }
  }
  
  public boolean versionMeetsMinimum(int major, int minor, int subminor) throws SQLException {
    checkClosed();
    
    return io.versionMeetsMinimum(major, minor, subminor);
  }
  













  public CachedResultSetMetaData getCachedMetaData(String sql)
  {
    if (resultSetMetadataCache != null) {
      synchronized (resultSetMetadataCache) {
        return (CachedResultSetMetaData)resultSetMetadataCache.get(sql);
      }
    }
    
    return null;
  }
  
















  public void initializeResultsMetadataFromCache(String sql, CachedResultSetMetaData cachedMetaData, ResultSetInternalMethods resultSet)
    throws SQLException
  {
    if (cachedMetaData == null)
    {

      cachedMetaData = new CachedResultSetMetaData();
      

      resultSet.buildIndexMapping();
      resultSet.initializeWithMetadata();
      
      if ((resultSet instanceof UpdatableResultSet)) {
        ((UpdatableResultSet)resultSet).checkUpdatability();
      }
      
      resultSet.populateCachedMetaData(cachedMetaData);
      
      resultSetMetadataCache.put(sql, cachedMetaData);
    } else {
      resultSet.initializeFromCachedMetaData(cachedMetaData);
      resultSet.initializeWithMetadata();
      
      if ((resultSet instanceof UpdatableResultSet)) {
        ((UpdatableResultSet)resultSet).checkUpdatability();
      }
    }
  }
  






  public String getStatementComment()
  {
    return statementComment;
  }
  








  public void setStatementComment(String comment)
  {
    statementComment = comment;
  }
  
  public void reportQueryTime(long millisOrNanos) {
    synchronized (getConnectionMutex()) {
      queryTimeCount += 1L;
      queryTimeSum += millisOrNanos;
      queryTimeSumSquares += millisOrNanos * millisOrNanos;
      queryTimeMean = ((queryTimeMean * (queryTimeCount - 1L) + millisOrNanos) / queryTimeCount);
    }
  }
  
  public boolean isAbonormallyLongQuery(long millisOrNanos) {
    synchronized (getConnectionMutex()) {
      if (queryTimeCount < 15L) {
        return false;
      }
      
      double stddev = Math.sqrt((queryTimeSumSquares - queryTimeSum * queryTimeSum / queryTimeCount) / (queryTimeCount - 1L));
      
      return millisOrNanos > queryTimeMean + 5.0D * stddev;
    }
  }
  
  public void initializeExtension(Extension ex) throws SQLException {
    ex.init(this, props);
  }
  
  public void transactionBegun() throws SQLException {
    synchronized (getConnectionMutex()) {
      if (connectionLifecycleInterceptors != null) {
        IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
        {
          void forEach(Extension each) throws SQLException
          {
            ((ConnectionLifecycleInterceptor)each).transactionBegun();
          }
          
        };
        iter.doForAll();
      }
    }
  }
  
  public void transactionCompleted() throws SQLException {
    synchronized (getConnectionMutex()) {
      if (connectionLifecycleInterceptors != null) {
        IterateBlock<Extension> iter = new IterateBlock(connectionLifecycleInterceptors.iterator())
        {
          void forEach(Extension each) throws SQLException
          {
            ((ConnectionLifecycleInterceptor)each).transactionCompleted();
          }
          
        };
        iter.doForAll();
      }
    }
  }
  
  public boolean storesLowerCaseTableName() {
    return storesLowerCaseTableName;
  }
  


  public ExceptionInterceptor getExceptionInterceptor()
  {
    return exceptionInterceptor;
  }
  
  public boolean getRequiresEscapingEncoder() {
    return requiresEscapingEncoder;
  }
  
  public boolean isServerLocal() throws SQLException {
    synchronized (getConnectionMutex()) {
      SocketFactory factory = getIOsocketFactory;
      
      if ((factory instanceof SocketMetadata)) {
        return ((SocketMetadata)factory).isLocallyConnected(this);
      }
      getLog().logWarn(Messages.getString("Connection.NoMetadataOnSocketFactory"));
      return false;
    }
  }
  


  public int getSessionMaxRows()
  {
    synchronized (getConnectionMutex()) {
      return sessionMaxRows;
    }
  }
  






  public void setSessionMaxRows(int max)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if (sessionMaxRows != max) {
        sessionMaxRows = max;
        execSQL(null, "SET SQL_SELECT_LIMIT=" + (sessionMaxRows == -1 ? "DEFAULT" : Integer.valueOf(sessionMaxRows)), -1, null, 1003, 1007, false, database, null, false);
      }
    }
  }
  

  public void setSchema(String schema)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
    }
  }
  
  public String getSchema() throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      
      return null;
    }
  }
  




























  public void abort(Executor executor)
    throws SQLException
  {
    SecurityManager sec = System.getSecurityManager();
    
    if (sec != null) {
      sec.checkPermission(ABORT_PERM);
    }
    
    if (executor == null) {
      throw SQLError.createSQLException("Executor can not be null", "S1009", getExceptionInterceptor());
    }
    
    executor.execute(new Runnable()
    {
      public void run() {
        try {
          abortInternal();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
  
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
  {
    synchronized (getConnectionMutex()) {
      SecurityManager sec = System.getSecurityManager();
      
      if (sec != null) {
        sec.checkPermission(SET_NETWORK_TIMEOUT_PERM);
      }
      
      if (executor == null) {
        throw SQLError.createSQLException("Executor can not be null", "S1009", getExceptionInterceptor());
      }
      
      checkClosed();
      executor.execute(new NetworkTimeoutSetter(this, io, milliseconds));
    }
  }
  
  public int getNetworkTimeout() throws SQLException
  {
    synchronized (getConnectionMutex()) {
      checkClosed();
      return getSocketTimeout();
    }
  }
  
  public ProfilerEventHandler getProfilerEventHandlerInstance() {
    return eventSink;
  }
  
  public void setProfilerEventHandlerInstance(ProfilerEventHandler h) {
    eventSink = h;
  }
  
  protected ConnectionImpl() {}
  
  private static class NetworkTimeoutSetter implements Runnable
  {
    public NetworkTimeoutSetter(ConnectionImpl conn, MysqlIO io, int milliseconds)
    {
      connImplRef = new WeakReference(conn);
      mysqlIoRef = new WeakReference(io);
      this.milliseconds = milliseconds; }
    
    private final WeakReference<ConnectionImpl> connImplRef;
    private final WeakReference<MysqlIO> mysqlIoRef;
    private final int milliseconds;
    public void run() { try { ConnectionImpl conn = (ConnectionImpl)connImplRef.get();
        if (conn != null) {
          synchronized (conn.getConnectionMutex()) {
            conn.setSocketTimeout(milliseconds);
            MysqlIO io = (MysqlIO)mysqlIoRef.get();
            if (io != null) {
              io.setSocketTimeout(milliseconds);
            }
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  public void clearWarnings()
    throws SQLException
  {}
  
  public void releaseSavepoint(Savepoint arg0)
    throws SQLException
  {}
  
  public void setFailedOver(boolean flag) {}
  
  public void setHoldability(int arg0)
    throws SQLException
  {}
  
  @Deprecated
  public void setPreferSlaveDuringFailover(boolean flag) {}
}
