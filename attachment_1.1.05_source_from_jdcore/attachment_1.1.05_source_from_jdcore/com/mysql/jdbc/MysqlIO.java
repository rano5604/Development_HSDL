package com.mysql.jdbc;

import com.mysql.jdbc.authentication.CachingSha2PasswordPlugin;
import com.mysql.jdbc.authentication.MysqlClearPasswordPlugin;
import com.mysql.jdbc.authentication.MysqlNativePasswordPlugin;
import com.mysql.jdbc.authentication.MysqlOldPasswordPlugin;
import com.mysql.jdbc.authentication.Sha256PasswordPlugin;
import com.mysql.jdbc.exceptions.MySQLStatementCancelledException;
import com.mysql.jdbc.exceptions.MySQLTimeoutException;
import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.LogUtils;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import com.mysql.jdbc.util.ReadAheadInputStream;
import com.mysql.jdbc.util.ResultSetUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.Deflater;


























public class MysqlIO
{
  private static final String CODE_PAGE_1252 = "Cp1252";
  protected static final int NULL_LENGTH = -1;
  protected static final int COMP_HEADER_LENGTH = 3;
  protected static final int MIN_COMPRESS_LEN = 50;
  protected static final int HEADER_LENGTH = 4;
  protected static final int AUTH_411_OVERHEAD = 33;
  public static final int SEED_LENGTH = 20;
  private static int maxBufferSize = 65535;
  
  private static final String NONE = "none";
  
  private static final int CLIENT_LONG_PASSWORD = 1;
  
  private static final int CLIENT_FOUND_ROWS = 2;
  
  private static final int CLIENT_LONG_FLAG = 4;
  
  protected static final int CLIENT_CONNECT_WITH_DB = 8;
  
  private static final int CLIENT_COMPRESS = 32;
  private static final int CLIENT_LOCAL_FILES = 128;
  private static final int CLIENT_PROTOCOL_41 = 512;
  private static final int CLIENT_INTERACTIVE = 1024;
  protected static final int CLIENT_SSL = 2048;
  private static final int CLIENT_TRANSACTIONS = 8192;
  protected static final int CLIENT_RESERVED = 16384;
  protected static final int CLIENT_SECURE_CONNECTION = 32768;
  private static final int CLIENT_MULTI_STATEMENTS = 65536;
  private static final int CLIENT_MULTI_RESULTS = 131072;
  private static final int CLIENT_PLUGIN_AUTH = 524288;
  private static final int CLIENT_CONNECT_ATTRS = 1048576;
  private static final int CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA = 2097152;
  private static final int CLIENT_CAN_HANDLE_EXPIRED_PASSWORD = 4194304;
  private static final int CLIENT_SESSION_TRACK = 8388608;
  private static final int CLIENT_DEPRECATE_EOF = 16777216;
  private static final int SERVER_STATUS_IN_TRANS = 1;
  private static final int SERVER_STATUS_AUTOCOMMIT = 2;
  static final int SERVER_MORE_RESULTS_EXISTS = 8;
  private static final int SERVER_QUERY_NO_GOOD_INDEX_USED = 16;
  private static final int SERVER_QUERY_NO_INDEX_USED = 32;
  private static final int SERVER_QUERY_WAS_SLOW = 2048;
  private static final int SERVER_STATUS_CURSOR_EXISTS = 64;
  private static final String FALSE_SCRAMBLE = "xxxxxxxx";
  protected static final int MAX_QUERY_SIZE_TO_LOG = 1024;
  protected static final int MAX_QUERY_SIZE_TO_EXPLAIN = 1048576;
  protected static final int INITIAL_PACKET_SIZE = 1024;
  private static String jvmPlatformCharset = null;
  

  protected static final String ZERO_DATE_VALUE_MARKER = "0000-00-00";
  
  protected static final String ZERO_DATETIME_VALUE_MARKER = "0000-00-00 00:00:00";
  
  private static final String EXPLAINABLE_STATEMENT = "SELECT";
  
  private static final String[] EXPLAINABLE_STATEMENT_EXTENSION = { "INSERT", "UPDATE", "REPLACE", "DELETE" };
  private static final int MAX_PACKET_DUMP_LENGTH = 1024;
  
  static { OutputStreamWriter outWriter = null;
    



    try
    {
      outWriter = new OutputStreamWriter(new ByteArrayOutputStream());
      jvmPlatformCharset = outWriter.getEncoding();
    } finally {
      try {
        if (outWriter != null) {
          outWriter.close();
        }
      }
      catch (IOException ioEx) {}
    }
  }
  



  private boolean packetSequenceReset = false;
  

  protected int serverCharsetIndex;
  

  private Buffer reusablePacket = null;
  private Buffer sendPacket = null;
  private Buffer sharedSendPacket = null;
  

  protected BufferedOutputStream mysqlOutput = null;
  protected MySQLConnection connection;
  private Deflater deflater = null;
  protected InputStream mysqlInput = null;
  private LinkedList<StringBuilder> packetDebugRingBuffer = null;
  private RowData streamingData = null;
  

  public Socket mysqlConnection = null;
  protected SocketFactory socketFactory = null;
  


  private SoftReference<Buffer> loadFileBufRef;
  


  private SoftReference<Buffer> splitBufRef;
  


  private SoftReference<Buffer> compressBufRef;
  

  protected String host = null;
  protected String seed;
  private String serverVersion = null;
  private String socketFactoryClassName = null;
  private byte[] packetHeaderBuf = new byte[4];
  private boolean colDecimalNeedsBump = false;
  private boolean hadWarnings = false;
  private boolean has41NewNewProt = false;
  

  private boolean hasLongColumnInfo = false;
  private boolean isInteractiveClient = false;
  private boolean logSlowQueries = false;
  




  private boolean platformDbCharsetMatches = true;
  private boolean profileSql = false;
  private boolean queryBadIndexUsed = false;
  private boolean queryNoIndexUsed = false;
  private boolean serverQueryWasSlow = false;
  

  private boolean use41Extensions = false;
  private boolean useCompression = false;
  private boolean useNewLargePackets = false;
  private boolean useNewUpdateCounts = false;
  private byte packetSequence = 0;
  private byte compressedPacketSequence = 0;
  private byte readPacketSequence = -1;
  private boolean checkPacketSequence = false;
  private byte protocolVersion = 0;
  private int maxAllowedPacket = 1048576;
  protected int maxThreeBytes = 16581375;
  protected int port = 3306;
  protected int serverCapabilities;
  private int serverMajorVersion = 0;
  private int serverMinorVersion = 0;
  private int oldServerStatus = 0;
  private int serverStatus = 0;
  private int serverSubMinorVersion = 0;
  private int warningCount = 0;
  protected long clientParam = 0L;
  protected long lastPacketSentTimeMs = 0L;
  protected long lastPacketReceivedTimeMs = 0L;
  private boolean traceProtocol = false;
  private boolean enablePacketDebug = false;
  private boolean useConnectWithDb;
  private boolean needToGrabQueryFromPacket;
  private boolean autoGenerateTestcaseScript;
  private long threadId;
  private boolean useNanosForElapsedTime;
  private long slowQueryThreshold;
  private String queryTimingUnits;
  private boolean useDirectRowUnpack = true;
  private int useBufferRowSizeThreshold;
  private int commandCount = 0;
  private List<StatementInterceptorV2> statementInterceptors;
  private ExceptionInterceptor exceptionInterceptor;
  private int authPluginDataLength = 0;
  





















  public MysqlIO(String host, int port, Properties props, String socketFactoryClassName, MySQLConnection conn, int socketTimeout, int useBufferRowSizeThreshold)
    throws IOException, SQLException
  {
    connection = conn;
    
    if (connection.getEnablePacketDebug()) {
      packetDebugRingBuffer = new LinkedList();
    }
    traceProtocol = connection.getTraceProtocol();
    
    useAutoSlowLog = connection.getAutoSlowLog();
    
    this.useBufferRowSizeThreshold = useBufferRowSizeThreshold;
    useDirectRowUnpack = connection.getUseDirectRowUnpack();
    
    logSlowQueries = connection.getLogSlowQueries();
    
    reusablePacket = new Buffer(1024);
    sendPacket = new Buffer(1024);
    
    this.port = port;
    this.host = host;
    
    this.socketFactoryClassName = socketFactoryClassName;
    socketFactory = createSocketFactory();
    exceptionInterceptor = connection.getExceptionInterceptor();
    try
    {
      mysqlConnection = socketFactory.connect(this.host, this.port, props);
      
      if (socketTimeout != 0) {
        try {
          mysqlConnection.setSoTimeout(socketTimeout);
        }
        catch (Exception ex) {}
      }
      

      mysqlConnection = socketFactory.beforeHandshake();
      
      if (connection.getUseReadAheadInput()) {
        mysqlInput = new ReadAheadInputStream(mysqlConnection.getInputStream(), 16384, connection.getTraceProtocol(), connection.getLog());
      }
      else if (connection.useUnbufferedInput()) {
        mysqlInput = mysqlConnection.getInputStream();
      } else {
        mysqlInput = new BufferedInputStream(mysqlConnection.getInputStream(), 16384);
      }
      
      mysqlOutput = new BufferedOutputStream(mysqlConnection.getOutputStream(), 16384);
      
      isInteractiveClient = connection.getInteractiveClient();
      profileSql = connection.getProfileSql();
      autoGenerateTestcaseScript = connection.getAutoGenerateTestcaseScript();
      
      needToGrabQueryFromPacket = ((profileSql) || (logSlowQueries) || (autoGenerateTestcaseScript));
      
      if ((connection.getUseNanosForElapsedTime()) && (TimeUtil.nanoTimeAvailable())) {
        useNanosForElapsedTime = true;
        
        queryTimingUnits = Messages.getString("Nanoseconds");
      } else {
        queryTimingUnits = Messages.getString("Milliseconds");
      }
      
      if (connection.getLogSlowQueries()) {
        calculateSlowQueryThreshold();
      }
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, 0L, 0L, ioEx, getExceptionInterceptor());
    }
  }
  




  public boolean hasLongColumnInfo()
  {
    return hasLongColumnInfo;
  }
  
  protected boolean isDataAvailable() throws SQLException {
    try {
      return mysqlInput.available() > 0;
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  



  protected long getLastPacketSentTimeMs()
  {
    return lastPacketSentTimeMs;
  }
  
  protected long getLastPacketReceivedTimeMs() {
    return lastPacketReceivedTimeMs;
  }
  






























  protected ResultSetImpl getResultSet(StatementImpl callingStatement, long columnCount, int maxRows, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, boolean isBinaryEncoded, Field[] metadataFromCache)
    throws SQLException
  {
    Field[] fields = null;
    


    if (metadataFromCache == null) {
      fields = new Field[(int)columnCount];
      
      for (int i = 0; i < columnCount; i++) {
        Buffer fieldPacket = null;
        
        fieldPacket = readPacket();
        fields[i] = unpackField(fieldPacket, false);
      }
    } else {
      for (int i = 0; i < columnCount; i++) {
        skipPacket();
      }
    }
    

    if ((!isEOFDeprecated()) || ((connection.versionMeetsMinimum(5, 0, 2)) && (callingStatement != null) && (isBinaryEncoded) && (callingStatement.isCursorRequired())))
    {


      Buffer packet = reuseAndReadPacket(reusablePacket);
      readServerStatusForResultSets(packet);
    }
    




    if ((connection.versionMeetsMinimum(5, 0, 2)) && (connection.getUseCursorFetch()) && (isBinaryEncoded) && (callingStatement != null) && (callingStatement.getFetchSize() != 0) && (callingStatement.getResultSetType() == 1003))
    {
      ServerPreparedStatement prepStmt = (ServerPreparedStatement)callingStatement;
      
      boolean usingCursor = true;
      





      if (connection.versionMeetsMinimum(5, 0, 5)) {
        usingCursor = (serverStatus & 0x40) != 0;
      }
      
      if (usingCursor) {
        RowData rows = new RowDataCursor(this, prepStmt, fields);
        
        ResultSetImpl rs = buildResultSetWithRows(callingStatement, catalog, fields, rows, resultSetType, resultSetConcurrency, isBinaryEncoded);
        
        if (usingCursor) {
          rs.setFetchSize(callingStatement.getFetchSize());
        }
        
        return rs;
      }
    }
    
    RowData rowData = null;
    
    if (!streamResults) {
      rowData = readSingleRowSet(columnCount, maxRows, resultSetConcurrency, isBinaryEncoded, metadataFromCache == null ? fields : metadataFromCache);
    } else {
      rowData = new RowDataDynamic(this, (int)columnCount, metadataFromCache == null ? fields : metadataFromCache, isBinaryEncoded);
      streamingData = rowData;
    }
    
    ResultSetImpl rs = buildResultSetWithRows(callingStatement, catalog, metadataFromCache == null ? fields : metadataFromCache, rowData, resultSetType, resultSetConcurrency, isBinaryEncoded);
    

    return rs;
  }
  

  protected NetworkResources getNetworkResources()
  {
    return new NetworkResources(mysqlConnection, mysqlInput, mysqlOutput);
  }
  

  protected final void forceClose()
  {
    try
    {
      getNetworkResources().forceClose();
    } finally {
      mysqlConnection = null;
      mysqlInput = null;
      mysqlOutput = null;
    }
  }
  





  protected final void skipPacket()
    throws SQLException
  {
    try
    {
      int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);
      
      if (lengthRead < 4) {
        forceClose();
        throw new IOException(Messages.getString("MysqlIO.1"));
      }
      
      int packetLength = (packetHeaderBuf[0] & 0xFF) + ((packetHeaderBuf[1] & 0xFF) << 8) + ((packetHeaderBuf[2] & 0xFF) << 16);
      
      if (traceProtocol) {
        StringBuilder traceMessageBuf = new StringBuilder();
        
        traceMessageBuf.append(Messages.getString("MysqlIO.2"));
        traceMessageBuf.append(packetLength);
        traceMessageBuf.append(Messages.getString("MysqlIO.3"));
        traceMessageBuf.append(StringUtils.dumpAsHex(packetHeaderBuf, 4));
        
        connection.getLog().logTrace(traceMessageBuf.toString());
      }
      
      byte multiPacketSeq = packetHeaderBuf[3];
      
      if (!packetSequenceReset) {
        if ((enablePacketDebug) && (checkPacketSequence)) {
          checkPacketSequencing(multiPacketSeq);
        }
      } else {
        packetSequenceReset = false;
      }
      
      readPacketSequence = multiPacketSeq;
      
      skipFully(mysqlInput, packetLength);
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
    catch (OutOfMemoryError oom) {
      try {
        connection.realClose(false, false, true, oom);
      }
      catch (Exception ex) {}
      throw oom;
    }
  }
  






  protected final Buffer readPacket()
    throws SQLException
  {
    try
    {
      int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);
      
      if (lengthRead < 4) {
        forceClose();
        throw new IOException(Messages.getString("MysqlIO.1"));
      }
      
      int packetLength = (packetHeaderBuf[0] & 0xFF) + ((packetHeaderBuf[1] & 0xFF) << 8) + ((packetHeaderBuf[2] & 0xFF) << 16);
      
      if (packetLength > maxAllowedPacket) {
        throw new PacketTooBigException(packetLength, maxAllowedPacket);
      }
      
      if (traceProtocol) {
        StringBuilder traceMessageBuf = new StringBuilder();
        
        traceMessageBuf.append(Messages.getString("MysqlIO.2"));
        traceMessageBuf.append(packetLength);
        traceMessageBuf.append(Messages.getString("MysqlIO.3"));
        traceMessageBuf.append(StringUtils.dumpAsHex(packetHeaderBuf, 4));
        
        connection.getLog().logTrace(traceMessageBuf.toString());
      }
      
      byte multiPacketSeq = packetHeaderBuf[3];
      
      if (!packetSequenceReset) {
        if ((enablePacketDebug) && (checkPacketSequence)) {
          checkPacketSequencing(multiPacketSeq);
        }
      } else {
        packetSequenceReset = false;
      }
      
      readPacketSequence = multiPacketSeq;
      

      byte[] buffer = new byte[packetLength];
      int numBytesRead = readFully(mysqlInput, buffer, 0, packetLength);
      
      if (numBytesRead != packetLength) {
        throw new IOException("Short read, expected " + packetLength + " bytes, only read " + numBytesRead);
      }
      
      Buffer packet = new Buffer(buffer);
      
      if (traceProtocol) {
        StringBuilder traceMessageBuf = new StringBuilder();
        
        traceMessageBuf.append(Messages.getString("MysqlIO.4"));
        traceMessageBuf.append(getPacketDumpToLog(packet, packetLength));
        
        connection.getLog().logTrace(traceMessageBuf.toString());
      }
      
      if (enablePacketDebug) {
        enqueuePacketForDebugging(false, false, 0, packetHeaderBuf, packet);
      }
      
      if (connection.getMaintainTimeStats()) {
        lastPacketReceivedTimeMs = System.currentTimeMillis();
      }
      
      return packet;
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
    catch (OutOfMemoryError oom) {
      try {
        connection.realClose(false, false, true, oom);
      }
      catch (Exception ex) {}
      throw oom;
    }
  }
  











  protected final Field unpackField(Buffer packet, boolean extractDefaultValues)
    throws SQLException
  {
    if (use41Extensions)
    {

      if (has41NewNewProt)
      {
        int catalogNameStart = packet.getPosition() + 1;
        int catalogNameLength = packet.fastSkipLenString();
        catalogNameStart = adjustStartForFieldLength(catalogNameStart, catalogNameLength);
      }
      
      int databaseNameStart = packet.getPosition() + 1;
      int databaseNameLength = packet.fastSkipLenString();
      databaseNameStart = adjustStartForFieldLength(databaseNameStart, databaseNameLength);
      
      int tableNameStart = packet.getPosition() + 1;
      int tableNameLength = packet.fastSkipLenString();
      tableNameStart = adjustStartForFieldLength(tableNameStart, tableNameLength);
      

      int originalTableNameStart = packet.getPosition() + 1;
      int originalTableNameLength = packet.fastSkipLenString();
      originalTableNameStart = adjustStartForFieldLength(originalTableNameStart, originalTableNameLength);
      

      int nameStart = packet.getPosition() + 1;
      int nameLength = packet.fastSkipLenString();
      
      nameStart = adjustStartForFieldLength(nameStart, nameLength);
      

      int originalColumnNameStart = packet.getPosition() + 1;
      int originalColumnNameLength = packet.fastSkipLenString();
      originalColumnNameStart = adjustStartForFieldLength(originalColumnNameStart, originalColumnNameLength);
      
      packet.readByte();
      
      short charSetNumber = (short)packet.readInt();
      
      long colLength = 0L;
      
      if (has41NewNewProt) {
        colLength = packet.readLong();
      } else {
        colLength = packet.readLongInt();
      }
      
      int colType = packet.readByte() & 0xFF;
      
      short colFlag = 0;
      
      if (hasLongColumnInfo) {
        colFlag = (short)packet.readInt();
      } else {
        colFlag = (short)(packet.readByte() & 0xFF);
      }
      
      int colDecimals = packet.readByte() & 0xFF;
      
      int defaultValueStart = -1;
      int defaultValueLength = -1;
      
      if (extractDefaultValues) {
        defaultValueStart = packet.getPosition() + 1;
        defaultValueLength = packet.fastSkipLenString();
      }
      
      Field field = new Field(connection, packet.getByteBuffer(), databaseNameStart, databaseNameLength, tableNameStart, tableNameLength, originalTableNameStart, originalTableNameLength, nameStart, nameLength, originalColumnNameStart, originalColumnNameLength, colLength, colType, colFlag, colDecimals, defaultValueStart, defaultValueLength, charSetNumber);
      


      return field;
    }
    
    int tableNameStart = packet.getPosition() + 1;
    int tableNameLength = packet.fastSkipLenString();
    tableNameStart = adjustStartForFieldLength(tableNameStart, tableNameLength);
    
    int nameStart = packet.getPosition() + 1;
    int nameLength = packet.fastSkipLenString();
    nameStart = adjustStartForFieldLength(nameStart, nameLength);
    
    int colLength = packet.readnBytes();
    int colType = packet.readnBytes();
    packet.readByte();
    
    short colFlag = 0;
    
    if (hasLongColumnInfo) {
      colFlag = (short)packet.readInt();
    } else {
      colFlag = (short)(packet.readByte() & 0xFF);
    }
    
    int colDecimals = packet.readByte() & 0xFF;
    
    if (colDecimalNeedsBump) {
      colDecimals++;
    }
    
    Field field = new Field(connection, packet.getByteBuffer(), nameStart, nameLength, tableNameStart, tableNameLength, colLength, colType, colFlag, colDecimals);
    

    return field;
  }
  
  private int adjustStartForFieldLength(int nameStart, int nameLength) {
    if (nameLength < 251) {
      return nameStart;
    }
    
    if ((nameLength >= 251) && (nameLength < 65536)) {
      return nameStart + 2;
    }
    
    if ((nameLength >= 65536) && (nameLength < 16777216)) {
      return nameStart + 3;
    }
    
    return nameStart + 8;
  }
  
  protected boolean isSetNeededForAutoCommitMode(boolean autoCommitFlag) {
    if ((use41Extensions) && (connection.getElideSetAutoCommits())) {
      boolean autoCommitModeOnServer = (serverStatus & 0x2) != 0;
      
      if ((!autoCommitFlag) && (versionMeetsMinimum(5, 0, 0)))
      {


        return !inTransactionOnServer();
      }
      
      return autoCommitModeOnServer != autoCommitFlag;
    }
    
    return true;
  }
  
  protected boolean inTransactionOnServer() {
    return (serverStatus & 0x1) != 0;
  }
  







  protected void changeUser(String userName, String password, String database)
    throws SQLException
  {
    packetSequence = -1;
    compressedPacketSequence = -1;
    
    int passwordLength = 16;
    int userLength = userName != null ? userName.length() : 0;
    int databaseLength = database != null ? database.length() : 0;
    
    int packLength = (userLength + passwordLength + databaseLength) * 3 + 7 + 4 + 33;
    
    if ((serverCapabilities & 0x80000) != 0)
    {
      proceedHandshakeWithPluggableAuthentication(userName, password, database, null);
    }
    else if ((serverCapabilities & 0x8000) != 0) {
      Buffer changeUserPacket = new Buffer(packLength + 1);
      changeUserPacket.writeByte((byte)17);
      
      if (versionMeetsMinimum(4, 1, 1)) {
        secureAuth411(changeUserPacket, packLength, userName, password, database, false, true);
      } else {
        secureAuth(changeUserPacket, packLength, userName, password, database, false);
      }
    }
    else {
      Buffer packet = new Buffer(packLength);
      packet.writeByte((byte)17);
      

      packet.writeString(userName);
      
      if (protocolVersion > 9) {
        packet.writeString(Util.newCrypt(password, seed, connection.getPasswordCharacterEncoding()));
      } else {
        packet.writeString(Util.oldCrypt(password, seed));
      }
      
      boolean localUseConnectWithDb = (useConnectWithDb) && (database != null) && (database.length() > 0);
      
      if (localUseConnectWithDb) {
        packet.writeString(database);
      }
      
      send(packet, packet.getPosition());
      checkErrorPacket();
      
      if (!localUseConnectWithDb) {
        changeDatabaseTo(database);
      }
    }
  }
  







  protected Buffer checkErrorPacket()
    throws SQLException
  {
    return checkErrorPacket(-1);
  }
  


  protected void checkForCharsetMismatch()
  {
    if ((connection.getUseUnicode()) && (connection.getEncoding() != null)) {
      String encodingToCheck = jvmPlatformCharset;
      
      if (encodingToCheck == null) {
        encodingToCheck = System.getProperty("file.encoding");
      }
      
      if (encodingToCheck == null) {
        platformDbCharsetMatches = false;
      } else {
        platformDbCharsetMatches = encodingToCheck.equals(connection.getEncoding());
      }
    }
  }
  
  protected void clearInputStream()
    throws SQLException
  {
    try
    {
      int len;
      while (((len = mysqlInput.available()) > 0) && (mysqlInput.skip(len) > 0L)) {}
    }
    catch (IOException ioEx)
    {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  
  protected void resetReadPacketSequence()
  {
    readPacketSequence = 0;
  }
  
  protected void dumpPacketRingBuffer() throws SQLException {
    if ((packetDebugRingBuffer != null) && (connection.getEnablePacketDebug())) {
      StringBuilder dumpBuffer = new StringBuilder();
      
      dumpBuffer.append("Last " + packetDebugRingBuffer.size() + " packets received from server, from oldest->newest:\n");
      dumpBuffer.append("\n");
      
      for (Iterator<StringBuilder> ringBufIter = packetDebugRingBuffer.iterator(); ringBufIter.hasNext();) {
        dumpBuffer.append((CharSequence)ringBufIter.next());
        dumpBuffer.append("\n");
      }
      
      connection.getLog().logTrace(dumpBuffer.toString());
    }
  }
  






  protected void explainSlowQuery(byte[] querySQL, String truncatedQuery)
    throws SQLException
  {
    if ((StringUtils.startsWithIgnoreCaseAndWs(truncatedQuery, "SELECT")) || ((versionMeetsMinimum(5, 6, 3)) && (StringUtils.startsWithIgnoreCaseAndWs(truncatedQuery, EXPLAINABLE_STATEMENT_EXTENSION) != -1)))
    {

      PreparedStatement stmt = null;
      ResultSet rs = null;
      try
      {
        stmt = (PreparedStatement)connection.clientPrepareStatement("EXPLAIN ?");
        stmt.setBytesNoEscapeNoQuotes(1, querySQL);
        rs = stmt.executeQuery();
        
        StringBuilder explainResults = new StringBuilder(Messages.getString("MysqlIO.8") + truncatedQuery + Messages.getString("MysqlIO.9"));
        
        ResultSetUtil.appendResultSetSlashGStyle(explainResults, rs);
        
        connection.getLog().logWarn(explainResults.toString());
      }
      catch (SQLException sqlEx) {}finally {
        if (rs != null) {
          rs.close();
        }
        
        if (stmt != null) {
          stmt.close();
        }
      }
    }
  }
  
  static int getMaxBuf() {
    return maxBufferSize;
  }
  


  final int getServerMajorVersion()
  {
    return serverMajorVersion;
  }
  


  final int getServerMinorVersion()
  {
    return serverMinorVersion;
  }
  


  final int getServerSubMinorVersion()
  {
    return serverSubMinorVersion;
  }
  


  String getServerVersion()
  {
    return serverVersion;
  }
  










  void doHandshake(String user, String password, String database)
    throws SQLException
  {
    checkPacketSequence = false;
    readPacketSequence = 0;
    
    Buffer buf = readPacket();
    

    protocolVersion = buf.readByte();
    
    if (protocolVersion == -1) {
      try {
        mysqlConnection.close();
      }
      catch (Exception e) {}
      

      int errno = 2000;
      
      errno = buf.readInt();
      
      String serverErrorMessage = buf.readString("ASCII", getExceptionInterceptor());
      
      StringBuilder errorBuf = new StringBuilder(Messages.getString("MysqlIO.10"));
      errorBuf.append(serverErrorMessage);
      errorBuf.append("\"");
      
      String xOpen = SQLError.mysqlToSqlState(errno, connection.getUseSqlStateCodes());
      
      throw SQLError.createSQLException(SQLError.get(xOpen) + ", " + errorBuf.toString(), xOpen, errno, getExceptionInterceptor());
    }
    
    serverVersion = buf.readString("ASCII", getExceptionInterceptor());
    

    int point = serverVersion.indexOf('.');
    
    if (point != -1) {
      try {
        int n = Integer.parseInt(serverVersion.substring(0, point));
        serverMajorVersion = n;
      }
      catch (NumberFormatException NFE1) {}
      

      String remaining = serverVersion.substring(point + 1, serverVersion.length());
      point = remaining.indexOf('.');
      
      if (point != -1) {
        try {
          int n = Integer.parseInt(remaining.substring(0, point));
          serverMinorVersion = n;
        }
        catch (NumberFormatException nfe) {}
        

        remaining = remaining.substring(point + 1, remaining.length());
        
        int pos = 0;
        
        while ((pos < remaining.length()) && 
          (remaining.charAt(pos) >= '0') && (remaining.charAt(pos) <= '9'))
        {


          pos++;
        }
        try
        {
          int n = Integer.parseInt(remaining.substring(0, pos));
          serverSubMinorVersion = n;
        }
        catch (NumberFormatException nfe) {}
      }
    }
    

    if (versionMeetsMinimum(4, 0, 8)) {
      maxThreeBytes = 16777215;
      useNewLargePackets = true;
    } else {
      maxThreeBytes = 16581375;
      useNewLargePackets = false;
    }
    
    colDecimalNeedsBump = versionMeetsMinimum(3, 23, 0);
    colDecimalNeedsBump = (!versionMeetsMinimum(3, 23, 15));
    useNewUpdateCounts = versionMeetsMinimum(3, 22, 5);
    

    threadId = buf.readLong();
    
    if (protocolVersion > 9)
    {
      seed = buf.readString("ASCII", getExceptionInterceptor(), 8);
      
      buf.readByte();
    }
    else {
      seed = buf.readString("ASCII", getExceptionInterceptor());
    }
    
    serverCapabilities = 0;
    

    if (buf.getPosition() < buf.getBufLength()) {
      serverCapabilities = buf.readInt();
    }
    
    if ((versionMeetsMinimum(4, 1, 1)) || ((protocolVersion > 9) && ((serverCapabilities & 0x200) != 0)))
    {


      serverCharsetIndex = (buf.readByte() & 0xFF);
      
      serverStatus = buf.readInt();
      checkTransactionState(0);
      

      serverCapabilities |= buf.readInt() << 16;
      
      if ((serverCapabilities & 0x80000) != 0)
      {
        authPluginDataLength = (buf.readByte() & 0xFF);
      }
      else {
        buf.readByte();
      }
      
      buf.setPosition(buf.getPosition() + 10);
      
      if ((serverCapabilities & 0x8000) != 0) {
        StringBuilder newSeed;
        String seedPart2;
        StringBuilder newSeed;
        if (authPluginDataLength > 0)
        {





          String seedPart2 = buf.readString("ASCII", getExceptionInterceptor(), authPluginDataLength - 8);
          newSeed = new StringBuilder(authPluginDataLength);
        } else {
          seedPart2 = buf.readString("ASCII", getExceptionInterceptor());
          newSeed = new StringBuilder(20);
        }
        newSeed.append(seed);
        newSeed.append(seedPart2);
        seed = newSeed.toString();
      }
    }
    
    if (((serverCapabilities & 0x20) != 0) && (connection.getUseCompression())) {
      clientParam |= 0x20;
    }
    
    useConnectWithDb = ((database != null) && (database.length() > 0) && (!connection.getCreateDatabaseIfNotExist()));
    
    if (useConnectWithDb) {
      clientParam |= 0x8;
    }
    

    if ((versionMeetsMinimum(5, 7, 0)) && (!connection.getUseSSL()) && (!connection.isUseSSLExplicit())) {
      connection.setUseSSL(true);
      connection.setVerifyServerCertificate(false);
      connection.getLog().logWarn(Messages.getString("MysqlIO.SSLWarning"));
    }
    

    if (((serverCapabilities & 0x800) == 0) && (connection.getUseSSL())) {
      if (connection.getRequireSSL()) {
        connection.close();
        forceClose();
        throw SQLError.createSQLException(Messages.getString("MysqlIO.15"), "08001", getExceptionInterceptor());
      }
      

      connection.setUseSSL(false);
    }
    
    if ((serverCapabilities & 0x4) != 0)
    {
      clientParam |= 0x4;
      hasLongColumnInfo = true;
    }
    

    if (!connection.getUseAffectedRows()) {
      clientParam |= 0x2;
    }
    
    if (connection.getAllowLoadLocalInfile()) {
      clientParam |= 0x80;
    }
    
    if (isInteractiveClient) {
      clientParam |= 0x400;
    }
    
    if (((serverCapabilities & 0x800000) == 0) || 
    



      ((serverCapabilities & 0x1000000) != 0)) {
      clientParam |= 0x1000000;
    }
    



    if ((serverCapabilities & 0x80000) != 0) {
      proceedHandshakeWithPluggableAuthentication(user, password, database, buf);
      return;
    }
    

    if (protocolVersion > 9) {
      clientParam |= 1L;
    } else {
      clientParam &= 0xFFFFFFFFFFFFFFFE;
    }
    



    if ((versionMeetsMinimum(4, 1, 0)) || ((protocolVersion > 9) && ((serverCapabilities & 0x4000) != 0))) {
      if ((versionMeetsMinimum(4, 1, 1)) || ((protocolVersion > 9) && ((serverCapabilities & 0x200) != 0))) {
        clientParam |= 0x200;
        has41NewNewProt = true;
        

        clientParam |= 0x2000;
        

        clientParam |= 0x20000;
        



        if (connection.getAllowMultiQueries()) {
          clientParam |= 0x10000;
        }
      } else {
        clientParam |= 0x4000;
        has41NewNewProt = false;
      }
      
      use41Extensions = true;
    }
    
    int passwordLength = 16;
    int userLength = user != null ? user.length() : 0;
    int databaseLength = database != null ? database.length() : 0;
    
    int packLength = (userLength + passwordLength + databaseLength) * 3 + 7 + 4 + 33;
    
    Buffer packet = null;
    
    if (!connection.getUseSSL()) {
      if ((serverCapabilities & 0x8000) != 0) {
        clientParam |= 0x8000;
        
        if ((versionMeetsMinimum(4, 1, 1)) || ((protocolVersion > 9) && ((serverCapabilities & 0x200) != 0))) {
          secureAuth411(null, packLength, user, password, database, true, false);
        } else {
          secureAuth(null, packLength, user, password, database, true);
        }
      }
      else {
        packet = new Buffer(packLength);
        
        if ((clientParam & 0x4000) != 0L) {
          if ((versionMeetsMinimum(4, 1, 1)) || ((protocolVersion > 9) && ((serverCapabilities & 0x200) != 0))) {
            packet.writeLong(clientParam);
            packet.writeLong(maxThreeBytes);
            

            packet.writeByte((byte)8);
            

            packet.writeBytesNoNull(new byte[23]);
          } else {
            packet.writeLong(clientParam);
            packet.writeLong(maxThreeBytes);
          }
        } else {
          packet.writeInt((int)clientParam);
          packet.writeLongInt(maxThreeBytes);
        }
        

        packet.writeString(user, "Cp1252", connection);
        
        if (protocolVersion > 9) {
          packet.writeString(Util.newCrypt(password, seed, connection.getPasswordCharacterEncoding()), "Cp1252", connection);
        } else {
          packet.writeString(Util.oldCrypt(password, seed), "Cp1252", connection);
        }
        
        if (useConnectWithDb) {
          packet.writeString(database, "Cp1252", connection);
        }
        
        send(packet, packet.getPosition());
      }
    } else {
      negotiateSSLConnection(user, password, database, packLength);
      
      if ((serverCapabilities & 0x8000) != 0) {
        if (versionMeetsMinimum(4, 1, 1)) {
          secureAuth411(null, packLength, user, password, database, true, false);
        } else {
          secureAuth411(null, packLength, user, password, database, true, false);
        }
      }
      else {
        packet = new Buffer(packLength);
        
        if (use41Extensions) {
          packet.writeLong(clientParam);
          packet.writeLong(maxThreeBytes);
        } else {
          packet.writeInt((int)clientParam);
          packet.writeLongInt(maxThreeBytes);
        }
        

        packet.writeString(user);
        
        if (protocolVersion > 9) {
          packet.writeString(Util.newCrypt(password, seed, connection.getPasswordCharacterEncoding()));
        } else {
          packet.writeString(Util.oldCrypt(password, seed));
        }
        
        if (((serverCapabilities & 0x8) != 0) && (database != null) && (database.length() > 0)) {
          packet.writeString(database);
        }
        
        send(packet, packet.getPosition());
      }
    }
    

    if ((!versionMeetsMinimum(4, 1, 1)) || (protocolVersion <= 9) || ((serverCapabilities & 0x200) == 0)) {
      checkErrorPacket();
    }
    



    if (((serverCapabilities & 0x20) != 0) && (connection.getUseCompression()) && (!(mysqlInput instanceof CompressedInputStream)))
    {
      deflater = new Deflater();
      useCompression = true;
      mysqlInput = new CompressedInputStream(connection, mysqlInput);
    }
    
    if (!useConnectWithDb) {
      changeDatabaseTo(database);
    }
    try
    {
      mysqlConnection = socketFactory.afterHandshake();
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  






  private Map<String, AuthenticationPlugin> authenticationPlugins = null;
  



  private List<String> disabledAuthenticationPlugins = null;
  


  private String clientDefaultAuthenticationPlugin = null;
  


  private String clientDefaultAuthenticationPluginName = null;
  


  private String serverDefaultAuthenticationPluginName = null;
  
















  private void loadAuthenticationPlugins()
    throws SQLException
  {
    clientDefaultAuthenticationPlugin = connection.getDefaultAuthenticationPlugin();
    if ((clientDefaultAuthenticationPlugin == null) || ("".equals(clientDefaultAuthenticationPlugin.trim()))) {
      throw SQLError.createSQLException(Messages.getString("Connection.BadDefaultAuthenticationPlugin", new Object[] { clientDefaultAuthenticationPlugin }), getExceptionInterceptor());
    }
    



    String disabledPlugins = connection.getDisabledAuthenticationPlugins();
    if ((disabledPlugins != null) && (!"".equals(disabledPlugins))) {
      disabledAuthenticationPlugins = new ArrayList();
      List<String> pluginsToDisable = StringUtils.split(disabledPlugins, ",", true);
      Iterator<String> iter = pluginsToDisable.iterator();
      while (iter.hasNext()) {
        disabledAuthenticationPlugins.add(iter.next());
      }
    }
    
    authenticationPlugins = new HashMap();
    

    AuthenticationPlugin plugin = new MysqlOldPasswordPlugin();
    plugin.init(connection, connection.getProperties());
    boolean defaultIsFound = addAuthenticationPlugin(plugin);
    
    plugin = new MysqlNativePasswordPlugin();
    plugin.init(connection, connection.getProperties());
    if (addAuthenticationPlugin(plugin)) {
      defaultIsFound = true;
    }
    
    plugin = new MysqlClearPasswordPlugin();
    plugin.init(connection, connection.getProperties());
    if (addAuthenticationPlugin(plugin)) {
      defaultIsFound = true;
    }
    
    plugin = new Sha256PasswordPlugin();
    plugin.init(connection, connection.getProperties());
    if (addAuthenticationPlugin(plugin)) {
      defaultIsFound = true;
    }
    
    plugin = new CachingSha2PasswordPlugin();
    plugin.init(connection, connection.getProperties());
    if (addAuthenticationPlugin(plugin)) {
      defaultIsFound = true;
    }
    

    String authenticationPluginClasses = connection.getAuthenticationPlugins();
    if ((authenticationPluginClasses != null) && (!"".equals(authenticationPluginClasses)))
    {
      List<Extension> plugins = Util.loadExtensions(connection, connection.getProperties(), authenticationPluginClasses, "Connection.BadAuthenticationPlugin", getExceptionInterceptor());
      

      for (Extension object : plugins) {
        plugin = (AuthenticationPlugin)object;
        if (addAuthenticationPlugin(plugin)) {
          defaultIsFound = true;
        }
      }
    }
    

    if (!defaultIsFound) {
      throw SQLError.createSQLException(Messages.getString("Connection.DefaultAuthenticationPluginIsNotListed", new Object[] { clientDefaultAuthenticationPlugin }), getExceptionInterceptor());
    }
  }
  











  private boolean addAuthenticationPlugin(AuthenticationPlugin plugin)
    throws SQLException
  {
    boolean isDefault = false;
    String pluginClassName = plugin.getClass().getName();
    String pluginProtocolName = plugin.getProtocolPluginName();
    boolean disabledByClassName = (disabledAuthenticationPlugins != null) && (disabledAuthenticationPlugins.contains(pluginClassName));
    boolean disabledByMechanism = (disabledAuthenticationPlugins != null) && (disabledAuthenticationPlugins.contains(pluginProtocolName));
    
    if ((disabledByClassName) || (disabledByMechanism))
    {
      if (clientDefaultAuthenticationPlugin.equals(pluginClassName)) {
        throw SQLError.createSQLException(Messages.getString("Connection.BadDisabledAuthenticationPlugin", new Object[] { disabledByClassName ? pluginClassName : pluginProtocolName }), getExceptionInterceptor());
      }
    }
    else {
      authenticationPlugins.put(pluginProtocolName, plugin);
      if (clientDefaultAuthenticationPlugin.equals(pluginClassName)) {
        clientDefaultAuthenticationPluginName = pluginProtocolName;
        isDefault = true;
      }
    }
    return isDefault;
  }
  















  private AuthenticationPlugin getAuthenticationPlugin(String pluginName)
    throws SQLException
  {
    AuthenticationPlugin plugin = (AuthenticationPlugin)authenticationPlugins.get(pluginName);
    
    if ((plugin != null) && (!plugin.isReusable())) {
      try {
        plugin = (AuthenticationPlugin)plugin.getClass().newInstance();
        plugin.init(connection, connection.getProperties());
      } catch (Throwable t) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("Connection.BadAuthenticationPlugin", new Object[] { plugin.getClass().getName() }), getExceptionInterceptor());
        
        sqlEx.initCause(t);
        throw sqlEx;
      }
    }
    
    return plugin;
  }
  




  private void checkConfidentiality(AuthenticationPlugin plugin)
    throws SQLException
  {
    if ((plugin.requiresConfidentiality()) && (!isSSLEstablished())) {
      throw SQLError.createSQLException(Messages.getString("Connection.AuthenticationPluginRequiresSSL", new Object[] { plugin.getProtocolPluginName() }), getExceptionInterceptor());
    }
  }
  























  private void proceedHandshakeWithPluggableAuthentication(String user, String password, String database, Buffer challenge)
    throws SQLException
  {
    if (authenticationPlugins == null) {
      loadAuthenticationPlugins();
    }
    
    boolean skipPassword = false;
    int passwordLength = 16;
    int userLength = user != null ? user.length() : 0;
    int databaseLength = database != null ? database.length() : 0;
    
    int packLength = (userLength + passwordLength + databaseLength) * 3 + 7 + 4 + 33;
    
    AuthenticationPlugin plugin = null;
    Buffer fromServer = null;
    ArrayList<Buffer> toServer = new ArrayList();
    boolean done = false;
    Buffer last_sent = null;
    
    boolean old_raw_challenge = false;
    
    int counter = 100;
    
    while (0 < counter--)
    {
      if (!done)
      {
        if (challenge != null)
        {
          if (challenge.isOKPacket()) {
            throw SQLError.createSQLException(Messages.getString("Connection.UnexpectedAuthenticationApproval", new Object[] { plugin.getProtocolPluginName() }), getExceptionInterceptor());
          }
          




          clientParam |= 0xAA201;
          



          if (connection.getAllowMultiQueries()) {
            clientParam |= 0x10000;
          }
          
          if (((serverCapabilities & 0x400000) != 0) && (!connection.getDisconnectOnExpiredPasswords())) {
            clientParam |= 0x400000;
          }
          if (((serverCapabilities & 0x100000) != 0) && (!"none".equals(connection.getConnectionAttributes()))) {
            clientParam |= 0x100000;
          }
          if ((serverCapabilities & 0x200000) != 0) {
            clientParam |= 0x200000;
          }
          
          has41NewNewProt = true;
          use41Extensions = true;
          
          if (connection.getUseSSL()) {
            negotiateSSLConnection(user, password, database, packLength);
          }
          
          String pluginName = null;
          
          if ((serverCapabilities & 0x80000) != 0) {
            if ((!versionMeetsMinimum(5, 5, 10)) || ((versionMeetsMinimum(5, 6, 0)) && (!versionMeetsMinimum(5, 6, 2)))) {
              pluginName = challenge.readString("ASCII", getExceptionInterceptor(), authPluginDataLength);
            } else {
              pluginName = challenge.readString("ASCII", getExceptionInterceptor());
            }
          }
          
          plugin = getAuthenticationPlugin(pluginName);
          if (plugin == null)
          {


            plugin = getAuthenticationPlugin(clientDefaultAuthenticationPluginName);
          } else if ((pluginName.equals(Sha256PasswordPlugin.PLUGIN_NAME)) && (!isSSLEstablished()) && (connection.getServerRSAPublicKeyFile() == null) && (!connection.getAllowPublicKeyRetrieval()))
          {






            plugin = getAuthenticationPlugin(clientDefaultAuthenticationPluginName);
            skipPassword = !clientDefaultAuthenticationPluginName.equals(pluginName);
          }
          
          serverDefaultAuthenticationPluginName = plugin.getProtocolPluginName();
          
          checkConfidentiality(plugin);
          fromServer = new Buffer(StringUtils.getBytes(seed));
        }
        else {
          plugin = getAuthenticationPlugin(serverDefaultAuthenticationPluginName == null ? clientDefaultAuthenticationPluginName : serverDefaultAuthenticationPluginName);
          

          checkConfidentiality(plugin);
          



          fromServer = new Buffer(StringUtils.getBytes(seed));
        }
        
      }
      else
      {
        challenge = checkErrorPacket();
        old_raw_challenge = false;
        packetSequence = ((byte)(packetSequence + 1));
        compressedPacketSequence = ((byte)(compressedPacketSequence + 1));
        
        if (plugin == null)
        {

          plugin = getAuthenticationPlugin(serverDefaultAuthenticationPluginName != null ? serverDefaultAuthenticationPluginName : clientDefaultAuthenticationPluginName);
        }
        

        if (challenge.isOKPacket())
        {
          challenge.newReadLength();
          challenge.newReadLength();
          
          oldServerStatus = serverStatus;
          serverStatus = challenge.readInt();
          

          plugin.destroy();
          break;
        }
        if (challenge.isAuthMethodSwitchRequestPacket()) {
          skipPassword = false;
          

          String pluginName = challenge.readString("ASCII", getExceptionInterceptor());
          

          if (!plugin.getProtocolPluginName().equals(pluginName)) {
            plugin.destroy();
            plugin = getAuthenticationPlugin(pluginName);
            
            if (plugin == null) {
              throw SQLError.createSQLException(Messages.getString("Connection.BadAuthenticationPlugin", new Object[] { pluginName }), getExceptionInterceptor());
            }
          }
          else {
            plugin.reset();
          }
          
          checkConfidentiality(plugin);
          fromServer = new Buffer(StringUtils.getBytes(challenge.readString("ASCII", getExceptionInterceptor())));


        }
        else if (versionMeetsMinimum(5, 5, 16)) {
          fromServer = new Buffer(challenge.getBytes(challenge.getPosition(), challenge.getBufLength() - challenge.getPosition()));
        } else {
          old_raw_challenge = true;
          fromServer = new Buffer(challenge.getBytes(challenge.getPosition() - 1, challenge.getBufLength() - challenge.getPosition() + 1));
        }
      }
      


      try
      {
        plugin.setAuthenticationParameters(user, skipPassword ? null : password);
        done = plugin.nextAuthenticationStep(fromServer, toServer);
      } catch (SQLException e) {
        throw SQLError.createSQLException(e.getMessage(), e.getSQLState(), e, getExceptionInterceptor());
      }
      

      if (toServer.size() > 0) {
        if (challenge == null) {
          String enc = getEncodingForHandshake();
          

          last_sent = new Buffer(packLength + 1);
          last_sent.writeByte((byte)17);
          

          last_sent.writeString(user, enc, connection);
          

          if (((Buffer)toServer.get(0)).getBufLength() < 256)
          {
            last_sent.writeByte((byte)((Buffer)toServer.get(0)).getBufLength());
            last_sent.writeBytesNoNull(((Buffer)toServer.get(0)).getByteBuffer(), 0, ((Buffer)toServer.get(0)).getBufLength());
          } else {
            last_sent.writeByte((byte)0);
          }
          
          if (useConnectWithDb) {
            last_sent.writeString(database, enc, connection);
          }
          else {
            last_sent.writeByte((byte)0);
          }
          
          appendCharsetByteForHandshake(last_sent, enc);
          
          last_sent.writeByte((byte)0);
          

          if ((serverCapabilities & 0x80000) != 0) {
            last_sent.writeString(plugin.getProtocolPluginName(), enc, connection);
          }
          

          if ((clientParam & 0x100000) != 0L) {
            sendConnectionAttributes(last_sent, enc, connection);
            last_sent.writeByte((byte)0);
          }
          
          send(last_sent, last_sent.getPosition());
        }
        else if (challenge.isAuthMethodSwitchRequestPacket())
        {
          last_sent = new Buffer(((Buffer)toServer.get(0)).getBufLength() + 4);
          last_sent.writeBytesNoNull(((Buffer)toServer.get(0)).getByteBuffer(), 0, ((Buffer)toServer.get(0)).getBufLength());
          send(last_sent, last_sent.getPosition());
        }
        else if ((challenge.isRawPacket()) || (old_raw_challenge))
        {
          for (Buffer buffer : toServer) {
            last_sent = new Buffer(buffer.getBufLength() + 4);
            last_sent.writeBytesNoNull(buffer.getByteBuffer(), 0, ((Buffer)toServer.get(0)).getBufLength());
            send(last_sent, last_sent.getPosition());
          }
        }
        else
        {
          String enc = getEncodingForHandshake();
          
          last_sent = new Buffer(packLength);
          last_sent.writeLong(clientParam);
          last_sent.writeLong(maxThreeBytes);
          
          appendCharsetByteForHandshake(last_sent, enc);
          
          last_sent.writeBytesNoNull(new byte[23]);
          

          last_sent.writeString(user, enc, connection);
          
          if ((serverCapabilities & 0x200000) != 0)
          {
            last_sent.writeLenBytes(((Buffer)toServer.get(0)).getBytes(((Buffer)toServer.get(0)).getBufLength()));
          }
          else {
            last_sent.writeByte((byte)((Buffer)toServer.get(0)).getBufLength());
            last_sent.writeBytesNoNull(((Buffer)toServer.get(0)).getByteBuffer(), 0, ((Buffer)toServer.get(0)).getBufLength());
          }
          
          if (useConnectWithDb) {
            last_sent.writeString(database, enc, connection);
          }
          
          if ((serverCapabilities & 0x80000) != 0) {
            last_sent.writeString(plugin.getProtocolPluginName(), enc, connection);
          }
          

          if ((clientParam & 0x100000) != 0L) {
            sendConnectionAttributes(last_sent, enc, connection);
          }
          
          send(last_sent, last_sent.getPosition());
        }
      }
    }
    


    if (counter == 0) {
      throw SQLError.createSQLException(Messages.getString("CommunicationsException.TooManyAuthenticationPluginNegotiations"), getExceptionInterceptor());
    }
    



    if (((serverCapabilities & 0x20) != 0) && (connection.getUseCompression()) && (!(mysqlInput instanceof CompressedInputStream)))
    {
      deflater = new Deflater();
      useCompression = true;
      mysqlInput = new CompressedInputStream(connection, mysqlInput);
    }
    
    if (!useConnectWithDb) {
      changeDatabaseTo(database);
    }
    try
    {
      mysqlConnection = socketFactory.afterHandshake();
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  
  private Properties getConnectionAttributesAsProperties(String atts)
    throws SQLException
  {
    Properties props = new Properties();
    
    if (atts != null) {
      String[] pairs = atts.split(",");
      for (String pair : pairs) {
        int keyEnd = pair.indexOf(":");
        if ((keyEnd > 0) && (keyEnd + 1 < pair.length())) {
          props.setProperty(pair.substring(0, keyEnd), pair.substring(keyEnd + 1));
        }
      }
    }
    



    props.setProperty("_client_name", "MySQL Connector Java");
    props.setProperty("_client_version", "5.1.46");
    props.setProperty("_runtime_vendor", NonRegisteringDriver.RUNTIME_VENDOR);
    props.setProperty("_runtime_version", NonRegisteringDriver.RUNTIME_VERSION);
    props.setProperty("_client_license", "GPL");
    
    return props;
  }
  
  private void sendConnectionAttributes(Buffer buf, String enc, MySQLConnection conn) throws SQLException {
    String atts = conn.getConnectionAttributes();
    
    Buffer lb = new Buffer(100);
    Properties props;
    try {
      props = getConnectionAttributesAsProperties(atts);
      
      for (Object key : props.keySet()) {
        lb.writeLenString((String)key, enc, conn.getServerCharset(), null, conn.parserKnowsUnicode(), conn);
        lb.writeLenString(props.getProperty((String)key), enc, conn.getServerCharset(), null, conn.parserKnowsUnicode(), conn);
      }
    }
    catch (UnsupportedEncodingException e) {}
    


    buf.writeByte((byte)(lb.getPosition() - 4));
    buf.writeBytesNoNull(lb.getByteBuffer(), 4, lb.getBufLength() - 4);
  }
  
  private void changeDatabaseTo(String database) throws SQLException
  {
    if ((database == null) || (database.length() == 0)) {
      return;
    }
    try
    {
      sendCommand(2, database, null, false, null, 0);
    } catch (Exception ex) {
      if (connection.getCreateDatabaseIfNotExist()) {
        sendCommand(3, "CREATE DATABASE IF NOT EXISTS " + database, null, false, null, 0);
        sendCommand(2, database, null, false, null, 0);
      } else {
        throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ex, getExceptionInterceptor());
      }
    }
  }
  














  final ResultSetRow nextRow(Field[] fields, int columnCount, boolean isBinaryEncoded, int resultSetConcurrency, boolean useBufferRowIfPossible, boolean useBufferRowExplicit, boolean canReuseRowPacketForBufferRow, Buffer existingRowPacket)
    throws SQLException
  {
    if ((useDirectRowUnpack) && (existingRowPacket == null) && (!isBinaryEncoded) && (!useBufferRowIfPossible) && (!useBufferRowExplicit)) {
      return nextRowFast(fields, columnCount, isBinaryEncoded, resultSetConcurrency, useBufferRowIfPossible, useBufferRowExplicit, canReuseRowPacketForBufferRow);
    }
    

    Buffer rowPacket = null;
    
    if (existingRowPacket == null) {
      rowPacket = checkErrorPacket();
      
      if ((!useBufferRowExplicit) && (useBufferRowIfPossible) && 
        (rowPacket.getBufLength() > useBufferRowSizeThreshold)) {
        useBufferRowExplicit = true;
      }
    }
    else
    {
      rowPacket = existingRowPacket;
      checkErrorPacket(existingRowPacket);
    }
    
    if (!isBinaryEncoded)
    {


      rowPacket.setPosition(rowPacket.getPosition() - 1);
      
      if (((isEOFDeprecated()) || (!rowPacket.isEOFPacket())) && ((!isEOFDeprecated()) || (!rowPacket.isResultSetOKPacket()))) {
        if ((resultSetConcurrency == 1008) || ((!useBufferRowIfPossible) && (!useBufferRowExplicit)))
        {
          byte[][] rowData = new byte[columnCount][];
          
          for (int i = 0; i < columnCount; i++) {
            rowData[i] = rowPacket.readLenByteArray(0);
          }
          
          return new ByteArrayRow(rowData, getExceptionInterceptor());
        }
        
        if (!canReuseRowPacketForBufferRow) {
          reusablePacket = new Buffer(rowPacket.getBufLength());
        }
        
        return new BufferRow(rowPacket, fields, false, getExceptionInterceptor());
      }
      

      readServerStatusForResultSets(rowPacket);
      
      return null;
    }
    



    if (((isEOFDeprecated()) || (!rowPacket.isEOFPacket())) && ((!isEOFDeprecated()) || (!rowPacket.isResultSetOKPacket()))) {
      if ((resultSetConcurrency == 1008) || ((!useBufferRowIfPossible) && (!useBufferRowExplicit))) {
        return unpackBinaryResultSetRow(fields, rowPacket, resultSetConcurrency);
      }
      
      if (!canReuseRowPacketForBufferRow) {
        reusablePacket = new Buffer(rowPacket.getBufLength());
      }
      
      return new BufferRow(rowPacket, fields, true, getExceptionInterceptor());
    }
    
    rowPacket.setPosition(rowPacket.getPosition() - 1);
    readServerStatusForResultSets(rowPacket);
    
    return null;
  }
  
  final ResultSetRow nextRowFast(Field[] fields, int columnCount, boolean isBinaryEncoded, int resultSetConcurrency, boolean useBufferRowIfPossible, boolean useBufferRowExplicit, boolean canReuseRowPacket) throws SQLException
  {
    try {
      int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);
      
      if (lengthRead < 4) {
        forceClose();
        throw new RuntimeException(Messages.getString("MysqlIO.43"));
      }
      
      int packetLength = (packetHeaderBuf[0] & 0xFF) + ((packetHeaderBuf[1] & 0xFF) << 8) + ((packetHeaderBuf[2] & 0xFF) << 16);
      

      if (packetLength == maxThreeBytes) {
        reuseAndReadPacket(reusablePacket, packetLength);
        

        return nextRow(fields, columnCount, isBinaryEncoded, resultSetConcurrency, useBufferRowIfPossible, useBufferRowExplicit, canReuseRowPacket, reusablePacket);
      }
      



      if (packetLength > useBufferRowSizeThreshold) {
        reuseAndReadPacket(reusablePacket, packetLength);
        

        return nextRow(fields, columnCount, isBinaryEncoded, resultSetConcurrency, true, true, false, reusablePacket);
      }
      
      int remaining = packetLength;
      
      boolean firstTime = true;
      
      byte[][] rowData = (byte[][])null;
      
      for (int i = 0; i < columnCount; i++)
      {
        int sw = mysqlInput.read() & 0xFF;
        remaining--;
        
        if (firstTime) {
          if (sw == 255)
          {

            Buffer errorPacket = new Buffer(packetLength + 4);
            errorPacket.setPosition(0);
            errorPacket.writeByte(packetHeaderBuf[0]);
            errorPacket.writeByte(packetHeaderBuf[1]);
            errorPacket.writeByte(packetHeaderBuf[2]);
            errorPacket.writeByte((byte)1);
            errorPacket.writeByte((byte)sw);
            readFully(mysqlInput, errorPacket.getByteBuffer(), 5, packetLength - 1);
            errorPacket.setPosition(4);
            checkErrorPacket(errorPacket);
          }
          
          if ((sw == 254) && (packetLength < 16777215))
          {





            if (use41Extensions) {
              if (isEOFDeprecated())
              {
                remaining -= skipLengthEncodedInteger(mysqlInput);
                remaining -= skipLengthEncodedInteger(mysqlInput);
                
                oldServerStatus = serverStatus;
                serverStatus = (mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8);
                checkTransactionState(oldServerStatus);
                remaining -= 2;
                
                warningCount = (mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8);
                remaining -= 2;
                
                if (warningCount > 0) {
                  hadWarnings = true;
                }
              }
              else
              {
                warningCount = (mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8);
                remaining -= 2;
                
                if (warningCount > 0) {
                  hadWarnings = true;
                }
                
                oldServerStatus = serverStatus;
                
                serverStatus = (mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8);
                checkTransactionState(oldServerStatus);
                
                remaining -= 2;
              }
              
              setServerSlowQueryFlags();
              
              if (remaining > 0) {
                skipFully(mysqlInput, remaining);
              }
            }
            
            return null;
          }
          
          rowData = new byte[columnCount][];
          
          firstTime = false;
        }
        
        int len = 0;
        
        switch (sw) {
        case 251: 
          len = -1;
          break;
        
        case 252: 
          len = mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8;
          remaining -= 2;
          break;
        
        case 253: 
          len = mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8 | (mysqlInput.read() & 0xFF) << 16;
          
          remaining -= 3;
          break;
        
        case 254: 
          len = (int)(mysqlInput.read() & 0xFF | (mysqlInput.read() & 0xFF) << 8 | (mysqlInput.read() & 0xFF) << 16 | (mysqlInput.read() & 0xFF) << 24 | (mysqlInput.read() & 0xFF) << 32 | (mysqlInput.read() & 0xFF) << 40 | (mysqlInput.read() & 0xFF) << 48 | (mysqlInput.read() & 0xFF) << 56);
          


          remaining -= 8;
          break;
        
        default: 
          len = sw;
        }
        
        if (len == -1) {
          rowData[i] = null;
        } else if (len == 0) {
          rowData[i] = Constants.EMPTY_BYTE_ARRAY;
        } else {
          rowData[i] = new byte[len];
          
          int bytesRead = readFully(mysqlInput, rowData[i], 0, len);
          
          if (bytesRead != len) {
            throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, new IOException(Messages.getString("MysqlIO.43")), getExceptionInterceptor());
          }
          

          remaining -= bytesRead;
        }
      }
      
      if (remaining > 0) {
        skipFully(mysqlInput, remaining);
      }
      
      return new ByteArrayRow(rowData, getExceptionInterceptor());
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  




  final void quit()
    throws SQLException
  {
    try
    {
      try
      {
        if (!mysqlConnection.isClosed()) {
          try {
            mysqlConnection.shutdownInput();
          }
          catch (UnsupportedOperationException ex) {}
        }
      }
      catch (IOException ioEx) {
        connection.getLog().logWarn("Caught while disconnecting...", ioEx);
      }
      
      Buffer packet = new Buffer(6);
      packetSequence = -1;
      compressedPacketSequence = -1;
      packet.writeByte((byte)1);
      send(packet, packet.getPosition());
    } finally {
      forceClose();
    }
  }
  





  Buffer getSharedSendPacket()
  {
    if (sharedSendPacket == null) {
      sharedSendPacket = new Buffer(1024);
    }
    
    return sharedSendPacket;
  }
  
  void closeStreamer(RowData streamer) throws SQLException {
    if (streamingData == null) {
      throw SQLError.createSQLException(Messages.getString("MysqlIO.17") + streamer + Messages.getString("MysqlIO.18"), getExceptionInterceptor());
    }
    
    if (streamer != streamingData) {
      throw SQLError.createSQLException(Messages.getString("MysqlIO.19") + streamer + Messages.getString("MysqlIO.20") + Messages.getString("MysqlIO.21") + Messages.getString("MysqlIO.22"), getExceptionInterceptor());
    }
    

    streamingData = null;
  }
  
  boolean tackOnMoreStreamingResults(ResultSetImpl addingTo) throws SQLException {
    if ((serverStatus & 0x8) != 0)
    {
      boolean moreRowSetsExist = true;
      ResultSetImpl currentResultSet = addingTo;
      boolean firstTime = true;
      
      while ((moreRowSetsExist) && (
        (firstTime) || (!currentResultSet.reallyResult())))
      {


        firstTime = false;
        
        Buffer fieldPacket = checkErrorPacket();
        fieldPacket.setPosition(0);
        
        java.sql.Statement owningStatement = addingTo.getStatement();
        
        int maxRows = owningStatement.getMaxRows();
        


        ResultSetImpl newResultSet = readResultsForQueryOrUpdate((StatementImpl)owningStatement, maxRows, owningStatement.getResultSetType(), owningStatement.getResultSetConcurrency(), true, owningStatement.getConnection().getCatalog(), fieldPacket, isBinaryEncoded, -1L, null);
        


        currentResultSet.setNextResultSet(newResultSet);
        
        currentResultSet = newResultSet;
        
        moreRowSetsExist = (serverStatus & 0x8) != 0;
        
        if ((!currentResultSet.reallyResult()) && (!moreRowSetsExist))
        {
          return false;
        }
      }
      
      return true;
    }
    
    return false;
  }
  
  ResultSetImpl readAllResults(StatementImpl callingStatement, int maxRows, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Buffer resultPacket, boolean isBinaryEncoded, long preSentColumnCount, Field[] metadataFromCache) throws SQLException
  {
    resultPacket.setPosition(resultPacket.getPosition() - 1);
    
    ResultSetImpl topLevelResultSet = readResultsForQueryOrUpdate(callingStatement, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, resultPacket, isBinaryEncoded, preSentColumnCount, metadataFromCache);
    

    ResultSetImpl currentResultSet = topLevelResultSet;
    
    boolean checkForMoreResults = (clientParam & 0x20000) != 0L;
    
    boolean serverHasMoreResults = (serverStatus & 0x8) != 0;
    



    if ((serverHasMoreResults) && (streamResults))
    {



      if (topLevelResultSet.getUpdateCount() != -1L) {
        tackOnMoreStreamingResults(topLevelResultSet);
      }
      
      reclaimLargeReusablePacket();
      
      return topLevelResultSet;
    }
    
    boolean moreRowSetsExist = checkForMoreResults & serverHasMoreResults;
    
    while (moreRowSetsExist) {
      Buffer fieldPacket = checkErrorPacket();
      fieldPacket.setPosition(0);
      
      ResultSetImpl newResultSet = readResultsForQueryOrUpdate(callingStatement, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, fieldPacket, isBinaryEncoded, preSentColumnCount, metadataFromCache);
      

      currentResultSet.setNextResultSet(newResultSet);
      
      currentResultSet = newResultSet;
      
      moreRowSetsExist = (serverStatus & 0x8) != 0;
    }
    
    if (!streamResults) {
      clearInputStream();
    }
    
    reclaimLargeReusablePacket();
    
    return topLevelResultSet;
  }
  


  void resetMaxBuf()
  {
    maxAllowedPacket = connection.getMaxAllowedPacket();
  }
  

























  final Buffer sendCommand(int command, String extraData, Buffer queryPacket, boolean skipCheck, String extraDataCharEncoding, int timeoutMillis)
    throws SQLException
  {
    commandCount += 1;
    




    enablePacketDebug = connection.getEnablePacketDebug();
    readPacketSequence = 0;
    
    int oldTimeout = 0;
    
    if (timeoutMillis != 0) {
      try {
        oldTimeout = mysqlConnection.getSoTimeout();
        mysqlConnection.setSoTimeout(timeoutMillis);
      } catch (SocketException e) {
        throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, e, getExceptionInterceptor());
      }
    }
    

    try
    {
      checkForOutstandingStreamingData();
      

      oldServerStatus = serverStatus;
      serverStatus = 0;
      hadWarnings = false;
      warningCount = 0;
      
      queryNoIndexUsed = false;
      queryBadIndexUsed = false;
      serverQueryWasSlow = false;
      



      if (useCompression) {
        int bytesLeft = mysqlInput.available();
        
        if (bytesLeft > 0) {
          mysqlInput.skip(bytesLeft);
        }
      }
      try
      {
        clearInputStream();
        





        if (queryPacket == null) {
          int packLength = 8 + (extraData != null ? extraData.length() : 0) + 2;
          
          if (sendPacket == null) {
            sendPacket = new Buffer(packLength);
          }
          
          packetSequence = -1;
          compressedPacketSequence = -1;
          readPacketSequence = 0;
          checkPacketSequence = true;
          sendPacket.clear();
          
          sendPacket.writeByte((byte)command);
          
          if ((command == 2) || (command == 3) || (command == 22)) {
            if (extraDataCharEncoding == null) {
              sendPacket.writeStringNoNull(extraData);
            } else {
              sendPacket.writeStringNoNull(extraData, extraDataCharEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), connection);
            }
          }
          

          send(sendPacket, sendPacket.getPosition());
        } else {
          packetSequence = -1;
          compressedPacketSequence = -1;
          send(queryPacket, queryPacket.getPosition());
        }
      }
      catch (SQLException sqlEx) {
        throw sqlEx;
      } catch (Exception ex) {
        throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ex, getExceptionInterceptor());
      }
      

      Buffer returnPacket = null;
      
      if (!skipCheck) {
        if ((command == 23) || (command == 26)) {
          readPacketSequence = 0;
          packetSequenceReset = true;
        }
        
        returnPacket = checkErrorPacket(command);
      }
      
      return returnPacket;
    } catch (IOException ioEx) {
      preserveOldTransactionState();
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
    catch (SQLException e)
    {
      preserveOldTransactionState();
      throw e;
    }
    finally {
      if (timeoutMillis != 0) {
        try {
          mysqlConnection.setSoTimeout(oldTimeout);
        } catch (SocketException e) {
          throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, e, getExceptionInterceptor());
        }
      }
    }
  }
  

  private int statementExecutionDepth = 0;
  private boolean useAutoSlowLog;
  
  protected boolean shouldIntercept() {
    return statementInterceptors != null;
  }
  

















  final ResultSetInternalMethods sqlQueryDirect(StatementImpl callingStatement, String query, String characterEncoding, Buffer queryPacket, int maxRows, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata)
    throws Exception
  {
    statementExecutionDepth += 1;
    try
    {
      if (statementInterceptors != null) {
        ResultSetInternalMethods interceptedResults = invokeStatementInterceptorsPre(query, callingStatement, false);
        
        if (interceptedResults != null) {
          return interceptedResults;
        }
      }
      
      long queryStartTime = 0L;
      long queryEndTime = 0L;
      
      String statementComment = connection.getStatementComment();
      
      if (connection.getIncludeThreadNamesAsStatementComment()) {
        statementComment = (statementComment != null ? statementComment + ", " : "") + "java thread: " + Thread.currentThread().getName();
      }
      
      if (query != null)
      {

        int packLength = 5 + query.length() * 3 + 2;
        
        byte[] commentAsBytes = null;
        
        if (statementComment != null) {
          commentAsBytes = StringUtils.getBytes(statementComment, null, characterEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
          

          packLength += commentAsBytes.length;
          packLength += 6;
        }
        
        if (sendPacket == null) {
          sendPacket = new Buffer(packLength);
        } else {
          sendPacket.clear();
        }
        
        sendPacket.writeByte((byte)3);
        
        if (commentAsBytes != null) {
          sendPacket.writeBytesNoNull(Constants.SLASH_STAR_SPACE_AS_BYTES);
          sendPacket.writeBytesNoNull(commentAsBytes);
          sendPacket.writeBytesNoNull(Constants.SPACE_STAR_SLASH_SPACE_AS_BYTES);
        }
        
        if (characterEncoding != null) {
          if (platformDbCharsetMatches) {
            sendPacket.writeStringNoNull(query, characterEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), connection);

          }
          else if (StringUtils.startsWithIgnoreCaseAndWs(query, "LOAD DATA")) {
            sendPacket.writeBytesNoNull(StringUtils.getBytes(query));
          } else {
            sendPacket.writeStringNoNull(query, characterEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), connection);
          }
          
        }
        else {
          sendPacket.writeStringNoNull(query);
        }
        
        queryPacket = sendPacket;
      }
      
      byte[] queryBuf = null;
      int oldPacketPosition = 0;
      
      if (needToGrabQueryFromPacket) {
        queryBuf = queryPacket.getByteBuffer();
        

        oldPacketPosition = queryPacket.getPosition();
        
        queryStartTime = getCurrentTimeNanosOrMillis();
      }
      
      if (autoGenerateTestcaseScript) {
        String testcaseQuery = null;
        
        if (query != null) {
          if (statementComment != null) {
            testcaseQuery = "/* " + statementComment + " */ " + query;
          } else {
            testcaseQuery = query;
          }
        } else {
          testcaseQuery = StringUtils.toString(queryBuf, 5, oldPacketPosition - 5);
        }
        
        StringBuilder debugBuf = new StringBuilder(testcaseQuery.length() + 32);
        connection.generateConnectionCommentBlock(debugBuf);
        debugBuf.append(testcaseQuery);
        debugBuf.append(';');
        connection.dumpTestcaseQuery(debugBuf.toString());
      }
      

      Buffer resultPacket = sendCommand(3, null, queryPacket, false, null, 0);
      
      long fetchBeginTime = 0L;
      long fetchEndTime = 0L;
      
      String profileQueryToLog = null;
      
      boolean queryWasSlow = false;
      
      if ((profileSql) || (logSlowQueries)) {
        queryEndTime = getCurrentTimeNanosOrMillis();
        
        boolean shouldExtractQuery = false;
        
        if (profileSql) {
          shouldExtractQuery = true;
        } else if (logSlowQueries) {
          long queryTime = queryEndTime - queryStartTime;
          
          boolean logSlow = false;
          
          if (!useAutoSlowLog) {
            logSlow = queryTime > connection.getSlowQueryThresholdMillis();
          } else {
            logSlow = connection.isAbonormallyLongQuery(queryTime);
            
            connection.reportQueryTime(queryTime);
          }
          
          if (logSlow) {
            shouldExtractQuery = true;
            queryWasSlow = true;
          }
        }
        
        if (shouldExtractQuery)
        {
          boolean truncated = false;
          
          int extractPosition = oldPacketPosition;
          
          if (oldPacketPosition > connection.getMaxQuerySizeToLog()) {
            extractPosition = connection.getMaxQuerySizeToLog() + 5;
            truncated = true;
          }
          
          profileQueryToLog = StringUtils.toString(queryBuf, 5, extractPosition - 5);
          
          if (truncated) {
            profileQueryToLog = profileQueryToLog + Messages.getString("MysqlIO.25");
          }
        }
        
        fetchBeginTime = queryEndTime;
      }
      
      ResultSetInternalMethods rs = readAllResults(callingStatement, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, resultPacket, false, -1L, cachedMetadata);
      

      if ((queryWasSlow) && (!serverQueryWasSlow)) {
        StringBuilder mesgBuf = new StringBuilder(48 + profileQueryToLog.length());
        
        mesgBuf.append(Messages.getString("MysqlIO.SlowQuery", new Object[] { String.valueOf(useAutoSlowLog ? " 95% of all queries " : Long.valueOf(slowQueryThreshold)), queryTimingUnits, Long.valueOf(queryEndTime - queryStartTime) }));
        

        mesgBuf.append(profileQueryToLog);
        
        ProfilerEventHandler eventSink = ProfilerEventHandlerFactory.getInstance(connection);
        
        eventSink.consumeEvent(new ProfilerEvent((byte)6, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), (int)(queryEndTime - queryStartTime), queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), mesgBuf.toString()));
        



        if (connection.getExplainSlowQueries()) {
          if (oldPacketPosition < 1048576) {
            explainSlowQuery(queryPacket.getBytes(5, oldPacketPosition - 5), profileQueryToLog);
          } else {
            connection.getLog().logWarn(Messages.getString("MysqlIO.28") + 1048576 + Messages.getString("MysqlIO.29"));
          }
        }
      }
      
      if (logSlowQueries)
      {
        ProfilerEventHandler eventSink = ProfilerEventHandlerFactory.getInstance(connection);
        
        if ((queryBadIndexUsed) && (profileSql)) {
          eventSink.consumeEvent(new ProfilerEvent((byte)6, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), queryEndTime - queryStartTime, queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), Messages.getString("MysqlIO.33") + profileQueryToLog));
        }
        



        if ((queryNoIndexUsed) && (profileSql)) {
          eventSink.consumeEvent(new ProfilerEvent((byte)6, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), queryEndTime - queryStartTime, queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), Messages.getString("MysqlIO.35") + profileQueryToLog));
        }
        



        if ((serverQueryWasSlow) && (profileSql)) {
          eventSink.consumeEvent(new ProfilerEvent((byte)6, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), queryEndTime - queryStartTime, queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), Messages.getString("MysqlIO.ServerSlowQuery") + profileQueryToLog));
        }
      }
      



      if (profileSql) {
        fetchEndTime = getCurrentTimeNanosOrMillis();
        
        ProfilerEventHandler eventSink = ProfilerEventHandlerFactory.getInstance(connection);
        
        eventSink.consumeEvent(new ProfilerEvent((byte)3, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), queryEndTime - queryStartTime, queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), profileQueryToLog));
        


        eventSink.consumeEvent(new ProfilerEvent((byte)5, "", catalog, connection.getId(), callingStatement != null ? callingStatement.getId() : 999, resultId, System.currentTimeMillis(), fetchEndTime - fetchBeginTime, queryTimingUnits, null, LogUtils.findCallingClassAndMethod(new Throwable()), null));
      }
      


      if (hadWarnings) {
        scanForAndThrowDataTruncation();
      }
      ResultSetInternalMethods interceptedResults;
      if (statementInterceptors != null) {
        interceptedResults = invokeStatementInterceptorsPost(query, callingStatement, rs, false, null);
        
        if (interceptedResults != null) {
          rs = interceptedResults;
        }
      }
      
      return rs;
    } catch (SQLException sqlEx) {
      if (statementInterceptors != null) {
        invokeStatementInterceptorsPost(query, callingStatement, null, false, sqlEx);
      }
      
      if (callingStatement != null) {
        synchronized (cancelTimeoutMutex) {
          if (wasCancelled) {
            SQLException cause = null;
            
            if (wasCancelledByTimeout) {
              cause = new MySQLTimeoutException();
            } else {
              cause = new MySQLStatementCancelledException();
            }
            
            callingStatement.resetCancelledState();
            
            throw cause;
          }
        }
      }
      
      throw sqlEx;
    } finally {
      statementExecutionDepth -= 1;
    }
  }
  
  ResultSetInternalMethods invokeStatementInterceptorsPre(String sql, Statement interceptedStatement, boolean forceExecute) throws SQLException {
    ResultSetInternalMethods previousResultSet = null;
    
    int i = 0; for (int s = statementInterceptors.size(); i < s; i++) {
      StatementInterceptorV2 interceptor = (StatementInterceptorV2)statementInterceptors.get(i);
      
      boolean executeTopLevelOnly = interceptor.executeTopLevelOnly();
      boolean shouldExecute = ((executeTopLevelOnly) && ((statementExecutionDepth == 1) || (forceExecute))) || (!executeTopLevelOnly);
      
      if (shouldExecute) {
        String sqlToInterceptor = sql;
        





        ResultSetInternalMethods interceptedResultSet = interceptor.preProcess(sqlToInterceptor, interceptedStatement, connection);
        
        if (interceptedResultSet != null) {
          previousResultSet = interceptedResultSet;
        }
      }
    }
    
    return previousResultSet;
  }
  
  ResultSetInternalMethods invokeStatementInterceptorsPost(String sql, Statement interceptedStatement, ResultSetInternalMethods originalResultSet, boolean forceExecute, SQLException statementException)
    throws SQLException
  {
    int i = 0; for (int s = statementInterceptors.size(); i < s; i++) {
      StatementInterceptorV2 interceptor = (StatementInterceptorV2)statementInterceptors.get(i);
      
      boolean executeTopLevelOnly = interceptor.executeTopLevelOnly();
      boolean shouldExecute = ((executeTopLevelOnly) && ((statementExecutionDepth == 1) || (forceExecute))) || (!executeTopLevelOnly);
      
      if (shouldExecute) {
        String sqlToInterceptor = sql;
        
        ResultSetInternalMethods interceptedResultSet = interceptor.postProcess(sqlToInterceptor, interceptedStatement, originalResultSet, connection, warningCount, queryNoIndexUsed, queryBadIndexUsed, statementException);
        

        if (interceptedResultSet != null) {
          originalResultSet = interceptedResultSet;
        }
      }
    }
    
    return originalResultSet;
  }
  
  private void calculateSlowQueryThreshold() {
    slowQueryThreshold = connection.getSlowQueryThresholdMillis();
    
    if (connection.getUseNanosForElapsedTime()) {
      long nanosThreshold = connection.getSlowQueryThresholdNanos();
      
      if (nanosThreshold != 0L) {
        slowQueryThreshold = nanosThreshold;
      } else {
        slowQueryThreshold *= 1000000L;
      }
    }
  }
  
  protected long getCurrentTimeNanosOrMillis() {
    if (useNanosForElapsedTime) {
      return TimeUtil.getCurrentTimeNanosOrMillis();
    }
    
    return System.currentTimeMillis();
  }
  


  String getHost()
  {
    return host;
  }
  













  boolean isVersion(int major, int minor, int subminor)
  {
    return (major == getServerMajorVersion()) && (minor == getServerMinorVersion()) && (subminor == getServerSubMinorVersion());
  }
  







  boolean versionMeetsMinimum(int major, int minor, int subminor)
  {
    if (getServerMajorVersion() >= major) {
      if (getServerMajorVersion() == major) {
        if (getServerMinorVersion() >= minor) {
          if (getServerMinorVersion() == minor) {
            return getServerSubMinorVersion() >= subminor;
          }
          

          return true;
        }
        

        return false;
      }
      

      return true;
    }
    
    return false;
  }
  










  private static final String getPacketDumpToLog(Buffer packetToDump, int packetLength)
  {
    if (packetLength < 1024) {
      return packetToDump.dump(packetLength);
    }
    
    StringBuilder packetDumpBuf = new StringBuilder(4096);
    packetDumpBuf.append(packetToDump.dump(1024));
    packetDumpBuf.append(Messages.getString("MysqlIO.36"));
    packetDumpBuf.append(1024);
    packetDumpBuf.append(Messages.getString("MysqlIO.37"));
    
    return packetDumpBuf.toString();
  }
  
  private final int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
    if (len < 0) {
      throw new IndexOutOfBoundsException();
    }
    
    int n = 0;
    
    while (n < len) {
      int count = in.read(b, off + n, len - n);
      
      if (count < 0) {
        throw new EOFException(Messages.getString("MysqlIO.EOF", new Object[] { Integer.valueOf(len), Integer.valueOf(n) }));
      }
      
      n += count;
    }
    
    return n;
  }
  
  private final long skipFully(InputStream in, long len) throws IOException {
    if (len < 0L) {
      throw new IOException("Negative skip length not allowed");
    }
    
    long n = 0L;
    
    while (n < len) {
      long count = in.skip(len - n);
      
      if (count < 0L) {
        throw new EOFException(Messages.getString("MysqlIO.EOF", new Object[] { Long.valueOf(len), Long.valueOf(n) }));
      }
      
      n += count;
    }
    
    return n;
  }
  
  private final int skipLengthEncodedInteger(InputStream in) throws IOException {
    int sw = in.read() & 0xFF;
    
    switch (sw) {
    case 252: 
      return (int)skipFully(in, 2L) + 1;
    
    case 253: 
      return (int)skipFully(in, 3L) + 1;
    
    case 254: 
      return (int)skipFully(in, 8L) + 1;
    }
    
    return 1;
  }
  































  protected final ResultSetImpl readResultsForQueryOrUpdate(StatementImpl callingStatement, int maxRows, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Buffer resultPacket, boolean isBinaryEncoded, long preSentColumnCount, Field[] metadataFromCache)
    throws SQLException
  {
    long columnCount = resultPacket.readFieldLength();
    
    if (columnCount == 0L)
      return buildResultSetWithUpdates(callingStatement, resultPacket);
    if (columnCount == -1L) {
      String charEncoding = null;
      
      if (connection.getUseUnicode()) {
        charEncoding = connection.getEncoding();
      }
      
      String fileName = null;
      
      if (platformDbCharsetMatches) {
        fileName = charEncoding != null ? resultPacket.readString(charEncoding, getExceptionInterceptor()) : resultPacket.readString();
      } else {
        fileName = resultPacket.readString();
      }
      
      return sendFileToServer(callingStatement, fileName);
    }
    ResultSetImpl results = getResultSet(callingStatement, columnCount, maxRows, resultSetType, resultSetConcurrency, streamResults, catalog, isBinaryEncoded, metadataFromCache);
    

    return results;
  }
  
  private int alignPacketSize(int a, int l)
  {
    return a + l - 1 & (l - 1 ^ 0xFFFFFFFF);
  }
  
  private ResultSetImpl buildResultSetWithRows(StatementImpl callingStatement, String catalog, Field[] fields, RowData rows, int resultSetType, int resultSetConcurrency, boolean isBinaryEncoded) throws SQLException
  {
    ResultSetImpl rs = null;
    
    switch (resultSetConcurrency) {
    case 1007: 
      rs = ResultSetImpl.getInstance(catalog, fields, rows, connection, callingStatement, false);
      
      if (isBinaryEncoded) {
        rs.setBinaryEncoded();
      }
      

      break;
    case 1008: 
      rs = ResultSetImpl.getInstance(catalog, fields, rows, connection, callingStatement, true);
      
      break;
    
    default: 
      return ResultSetImpl.getInstance(catalog, fields, rows, connection, callingStatement, false);
    }
    
    rs.setResultSetType(resultSetType);
    rs.setResultSetConcurrency(resultSetConcurrency);
    
    return rs;
  }
  
  private ResultSetImpl buildResultSetWithUpdates(StatementImpl callingStatement, Buffer resultPacket) throws SQLException {
    long updateCount = -1L;
    long updateID = -1L;
    String info = null;
    try
    {
      if (useNewUpdateCounts) {
        updateCount = resultPacket.newReadLength();
        updateID = resultPacket.newReadLength();
      } else {
        updateCount = resultPacket.readLength();
        updateID = resultPacket.readLength();
      }
      
      if (use41Extensions)
      {
        serverStatus = resultPacket.readInt();
        
        checkTransactionState(oldServerStatus);
        
        warningCount = resultPacket.readInt();
        
        if (warningCount > 0) {
          hadWarnings = true;
        }
        
        resultPacket.readByte();
        
        setServerSlowQueryFlags();
      }
      
      if (connection.isReadInfoMsgEnabled()) {
        info = resultPacket.readString(connection.getErrorMessageEncoding(), getExceptionInterceptor());
      }
    } catch (Exception ex) {
      SQLException sqlEx = SQLError.createSQLException(SQLError.get("S1000"), "S1000", -1, getExceptionInterceptor());
      
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
    
    ResultSetInternalMethods updateRs = ResultSetImpl.getInstance(updateCount, updateID, connection, callingStatement);
    
    if (info != null) {
      ((ResultSetImpl)updateRs).setServerInfo(info);
    }
    
    return (ResultSetImpl)updateRs;
  }
  
  private void setServerSlowQueryFlags() {
    queryBadIndexUsed = ((serverStatus & 0x10) != 0);
    queryNoIndexUsed = ((serverStatus & 0x20) != 0);
    serverQueryWasSlow = ((serverStatus & 0x800) != 0);
  }
  
  private void checkForOutstandingStreamingData() throws SQLException {
    if (streamingData != null) {
      boolean shouldClobber = connection.getClobberStreamingResults();
      
      if (!shouldClobber) {
        throw SQLError.createSQLException(Messages.getString("MysqlIO.39") + streamingData + Messages.getString("MysqlIO.40") + Messages.getString("MysqlIO.41") + Messages.getString("MysqlIO.42"), getExceptionInterceptor());
      }
      


      streamingData.getOwner().realClose(false);
      

      clearInputStream();
    }
  }
  










  private Buffer compressPacket(Buffer packet, int offset, int packetLen)
    throws SQLException
  {
    int compressedLength = packetLen;
    int uncompressedLength = 0;
    byte[] compressedBytes = null;
    int offsetWrite = offset;
    
    if (packetLen < 50) {
      compressedBytes = packet.getByteBuffer();
    }
    else {
      byte[] bytesToCompress = packet.getByteBuffer();
      compressedBytes = new byte[bytesToCompress.length * 2];
      
      if (deflater == null) {
        deflater = new Deflater();
      }
      deflater.reset();
      deflater.setInput(bytesToCompress, offset, packetLen);
      deflater.finish();
      
      compressedLength = deflater.deflate(compressedBytes);
      
      if (compressedLength > packetLen)
      {
        compressedBytes = packet.getByteBuffer();
        compressedLength = packetLen;
      } else {
        uncompressedLength = packetLen;
        offsetWrite = 0;
      }
    }
    
    Buffer compressedPacket = new Buffer(7 + compressedLength);
    
    compressedPacket.setPosition(0);
    compressedPacket.writeLongInt(compressedLength);
    compressedPacket.writeByte(compressedPacketSequence);
    compressedPacket.writeLongInt(uncompressedLength);
    compressedPacket.writeBytesNoNull(compressedBytes, offsetWrite, compressedLength);
    
    return compressedPacket;
  }
  
  private final void readServerStatusForResultSets(Buffer rowPacket) throws SQLException {
    if (use41Extensions) {
      rowPacket.readByte();
      
      if (isEOFDeprecated())
      {
        rowPacket.newReadLength();
        rowPacket.newReadLength();
        
        oldServerStatus = serverStatus;
        serverStatus = rowPacket.readInt();
        checkTransactionState(oldServerStatus);
        
        warningCount = rowPacket.readInt();
        if (warningCount > 0) {
          hadWarnings = true;
        }
        
        rowPacket.readByte();
        
        if (connection.isReadInfoMsgEnabled()) {
          rowPacket.readString(connection.getErrorMessageEncoding(), getExceptionInterceptor());
        }
      }
      else
      {
        warningCount = rowPacket.readInt();
        if (warningCount > 0) {
          hadWarnings = true;
        }
        
        oldServerStatus = serverStatus;
        serverStatus = rowPacket.readInt();
        checkTransactionState(oldServerStatus);
      }
      
      setServerSlowQueryFlags();
    }
  }
  
  private SocketFactory createSocketFactory() throws SQLException {
    try {
      if (socketFactoryClassName == null) {
        throw SQLError.createSQLException(Messages.getString("MysqlIO.75"), "08001", getExceptionInterceptor());
      }
      

      return (SocketFactory)Class.forName(socketFactoryClassName).newInstance();
    } catch (Exception ex) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("MysqlIO.76") + socketFactoryClassName + Messages.getString("MysqlIO.77"), "08001", getExceptionInterceptor());
      

      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  
  private void enqueuePacketForDebugging(boolean isPacketBeingSent, boolean isPacketReused, int sendLength, byte[] header, Buffer packet) throws SQLException
  {
    if (packetDebugRingBuffer.size() + 1 > connection.getPacketDebugBufferSize()) {
      packetDebugRingBuffer.removeFirst();
    }
    
    StringBuilder packetDump = null;
    
    if (!isPacketBeingSent) {
      int bytesToDump = Math.min(1024, packet.getBufLength());
      
      Buffer packetToDump = new Buffer(4 + bytesToDump);
      
      packetToDump.setPosition(0);
      packetToDump.writeBytesNoNull(header);
      packetToDump.writeBytesNoNull(packet.getBytes(0, bytesToDump));
      
      String packetPayload = packetToDump.dump(bytesToDump);
      
      packetDump = new StringBuilder(96 + packetPayload.length());
      
      packetDump.append("Server ");
      
      packetDump.append(isPacketReused ? "(re-used) " : "(new) ");
      
      packetDump.append(packet.toSuperString());
      packetDump.append(" --------------------> Client\n");
      packetDump.append("\nPacket payload:\n\n");
      packetDump.append(packetPayload);
      
      if (bytesToDump == 1024) {
        packetDump.append("\nNote: Packet of " + packet.getBufLength() + " bytes truncated to " + 1024 + " bytes.\n");
      }
    } else {
      int bytesToDump = Math.min(1024, sendLength);
      
      String packetPayload = packet.dump(bytesToDump);
      
      packetDump = new StringBuilder(68 + packetPayload.length());
      
      packetDump.append("Client ");
      packetDump.append(packet.toSuperString());
      packetDump.append("--------------------> Server\n");
      packetDump.append("\nPacket payload:\n\n");
      packetDump.append(packetPayload);
      
      if (bytesToDump == 1024) {
        packetDump.append("\nNote: Packet of " + sendLength + " bytes truncated to " + 1024 + " bytes.\n");
      }
    }
    
    packetDebugRingBuffer.addLast(packetDump);
  }
  
  private RowData readSingleRowSet(long columnCount, int maxRows, int resultSetConcurrency, boolean isBinaryEncoded, Field[] fields) throws SQLException
  {
    ArrayList<ResultSetRow> rows = new ArrayList();
    
    boolean useBufferRowExplicit = useBufferRowExplicit(fields);
    

    ResultSetRow row = nextRow(fields, (int)columnCount, isBinaryEncoded, resultSetConcurrency, false, useBufferRowExplicit, false, null);
    
    int rowCount = 0;
    
    if (row != null) {
      rows.add(row);
      rowCount = 1;
    }
    
    while (row != null) {
      row = nextRow(fields, (int)columnCount, isBinaryEncoded, resultSetConcurrency, false, useBufferRowExplicit, false, null);
      
      if ((row != null) && (
        (maxRows == -1) || (rowCount < maxRows))) {
        rows.add(row);
        rowCount++;
      }
    }
    

    RowData rowData = new RowDataStatic(rows);
    
    return rowData;
  }
  
  public static boolean useBufferRowExplicit(Field[] fields) {
    if (fields == null) {
      return false;
    }
    
    for (int i = 0; i < fields.length; i++) {
      switch (fields[i].getSQLType()) {
      case -4: 
      case -1: 
      case 2004: 
      case 2005: 
        return true;
      }
      
    }
    return false;
  }
  


  private void reclaimLargeReusablePacket()
  {
    if ((reusablePacket != null) && (reusablePacket.getCapacity() > 1048576)) {
      reusablePacket = new Buffer(1024);
    }
  }
  




  private final Buffer reuseAndReadPacket(Buffer reuse)
    throws SQLException
  {
    return reuseAndReadPacket(reuse, -1);
  }
  
  private final Buffer reuseAndReadPacket(Buffer reuse, int existingPacketLength) throws SQLException
  {
    try {
      reuse.setWasMultiPacket(false);
      int packetLength = 0;
      
      if (existingPacketLength == -1) {
        int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);
        
        if (lengthRead < 4) {
          forceClose();
          throw new IOException(Messages.getString("MysqlIO.43"));
        }
        
        packetLength = (packetHeaderBuf[0] & 0xFF) + ((packetHeaderBuf[1] & 0xFF) << 8) + ((packetHeaderBuf[2] & 0xFF) << 16);
      } else {
        packetLength = existingPacketLength;
      }
      
      if (traceProtocol) {
        StringBuilder traceMessageBuf = new StringBuilder();
        
        traceMessageBuf.append(Messages.getString("MysqlIO.44"));
        traceMessageBuf.append(packetLength);
        traceMessageBuf.append(Messages.getString("MysqlIO.45"));
        traceMessageBuf.append(StringUtils.dumpAsHex(packetHeaderBuf, 4));
        
        connection.getLog().logTrace(traceMessageBuf.toString());
      }
      
      byte multiPacketSeq = packetHeaderBuf[3];
      
      if (!packetSequenceReset) {
        if ((enablePacketDebug) && (checkPacketSequence)) {
          checkPacketSequencing(multiPacketSeq);
        }
      } else {
        packetSequenceReset = false;
      }
      
      readPacketSequence = multiPacketSeq;
      

      reuse.setPosition(0);
      




      if (reuse.getByteBuffer().length <= packetLength) {
        reuse.setByteBuffer(new byte[packetLength + 1]);
      }
      

      reuse.setBufLength(packetLength);
      

      int numBytesRead = readFully(mysqlInput, reuse.getByteBuffer(), 0, packetLength);
      
      if (numBytesRead != packetLength) {
        throw new IOException("Short read, expected " + packetLength + " bytes, only read " + numBytesRead);
      }
      
      if (traceProtocol) {
        StringBuilder traceMessageBuf = new StringBuilder();
        
        traceMessageBuf.append(Messages.getString("MysqlIO.46"));
        traceMessageBuf.append(getPacketDumpToLog(reuse, packetLength));
        
        connection.getLog().logTrace(traceMessageBuf.toString());
      }
      
      if (enablePacketDebug) {
        enqueuePacketForDebugging(false, true, 0, packetHeaderBuf, reuse);
      }
      
      boolean isMultiPacket = false;
      
      if (packetLength == maxThreeBytes) {
        reuse.setPosition(maxThreeBytes);
        

        isMultiPacket = true;
        
        packetLength = readRemainingMultiPackets(reuse, multiPacketSeq);
      }
      
      if (!isMultiPacket) {
        reuse.getByteBuffer()[packetLength] = 0;
      }
      
      if (connection.getMaintainTimeStats()) {
        lastPacketReceivedTimeMs = System.currentTimeMillis();
      }
      
      return reuse;
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
    catch (OutOfMemoryError oom)
    {
      try {
        clearInputStream();
      }
      catch (Exception ex) {}
      try {
        connection.realClose(false, false, true, oom);
      }
      catch (Exception ex) {}
      throw oom;
    }
  }
  
  private int readRemainingMultiPackets(Buffer reuse, byte multiPacketSeq) throws IOException, SQLException
  {
    int packetLength = -1;
    Buffer multiPacket = null;
    do
    {
      int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);
      if (lengthRead < 4) {
        forceClose();
        throw new IOException(Messages.getString("MysqlIO.47"));
      }
      
      packetLength = (packetHeaderBuf[0] & 0xFF) + ((packetHeaderBuf[1] & 0xFF) << 8) + ((packetHeaderBuf[2] & 0xFF) << 16);
      if (multiPacket == null) {
        multiPacket = new Buffer(packetLength);
      }
      
      if ((!useNewLargePackets) && (packetLength == 1)) {
        clearInputStream();
        break;
      }
      
      multiPacketSeq = (byte)(multiPacketSeq + 1);
      if (multiPacketSeq != packetHeaderBuf[3]) {
        throw new IOException(Messages.getString("MysqlIO.49"));
      }
      

      multiPacket.setPosition(0);
      

      multiPacket.setBufLength(packetLength);
      

      byte[] byteBuf = multiPacket.getByteBuffer();
      int lengthToWrite = packetLength;
      
      int bytesRead = readFully(mysqlInput, byteBuf, 0, packetLength);
      
      if (bytesRead != lengthToWrite) {
        throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, SQLError.createSQLException(Messages.getString("MysqlIO.50") + lengthToWrite + Messages.getString("MysqlIO.51") + bytesRead + ".", getExceptionInterceptor()), getExceptionInterceptor());
      }
      



      reuse.writeBytesNoNull(byteBuf, 0, lengthToWrite);
    } while (packetLength == maxThreeBytes);
    
    reuse.setPosition(0);
    reuse.setWasMultiPacket(true);
    return packetLength;
  }
  


  private void checkPacketSequencing(byte multiPacketSeq)
    throws SQLException
  {
    if ((multiPacketSeq == Byte.MIN_VALUE) && (readPacketSequence != Byte.MAX_VALUE)) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, new IOException("Packets out of order, expected packet # -128, but received packet # " + multiPacketSeq), getExceptionInterceptor());
    }
    

    if ((readPacketSequence == -1) && (multiPacketSeq != 0)) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, new IOException("Packets out of order, expected packet # -1, but received packet # " + multiPacketSeq), getExceptionInterceptor());
    }
    

    if ((multiPacketSeq != Byte.MIN_VALUE) && (readPacketSequence != -1) && (multiPacketSeq != readPacketSequence + 1)) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, new IOException("Packets out of order, expected packet # " + (readPacketSequence + 1) + ", but received packet # " + multiPacketSeq), getExceptionInterceptor());
    }
  }
  
  void enableMultiQueries()
    throws SQLException
  {
    Buffer buf = getSharedSendPacket();
    
    buf.clear();
    buf.writeByte((byte)27);
    buf.writeInt(0);
    sendCommand(27, null, buf, false, null, 0);
  }
  
  void disableMultiQueries() throws SQLException {
    Buffer buf = getSharedSendPacket();
    
    buf.clear();
    buf.writeByte((byte)27);
    buf.writeInt(1);
    sendCommand(27, null, buf, false, null, 0);
  }
  



  private final void send(Buffer packet, int packetLen)
    throws SQLException
  {
    try
    {
      if ((maxAllowedPacket > 0) && (packetLen > maxAllowedPacket)) {
        throw new PacketTooBigException(packetLen, maxAllowedPacket);
      }
      
      if ((serverMajorVersion >= 4) && ((packetLen - 4 >= maxThreeBytes) || ((useCompression) && (packetLen - 4 >= maxThreeBytes - 3))))
      {
        sendSplitPackets(packet, packetLen);
      }
      else {
        packetSequence = ((byte)(packetSequence + 1));
        
        Buffer packetToSend = packet;
        packetToSend.setPosition(0);
        packetToSend.writeLongInt(packetLen - 4);
        packetToSend.writeByte(packetSequence);
        
        if (useCompression) {
          compressedPacketSequence = ((byte)(compressedPacketSequence + 1));
          int originalPacketLen = packetLen;
          
          packetToSend = compressPacket(packetToSend, 0, packetLen);
          packetLen = packetToSend.getPosition();
          
          if (traceProtocol) {
            StringBuilder traceMessageBuf = new StringBuilder();
            
            traceMessageBuf.append(Messages.getString("MysqlIO.57"));
            traceMessageBuf.append(getPacketDumpToLog(packetToSend, packetLen));
            traceMessageBuf.append(Messages.getString("MysqlIO.58"));
            traceMessageBuf.append(getPacketDumpToLog(packet, originalPacketLen));
            
            connection.getLog().logTrace(traceMessageBuf.toString());
          }
          
        }
        else if (traceProtocol) {
          StringBuilder traceMessageBuf = new StringBuilder();
          
          traceMessageBuf.append(Messages.getString("MysqlIO.59"));
          traceMessageBuf.append("host: '");
          traceMessageBuf.append(host);
          traceMessageBuf.append("' threadId: '");
          traceMessageBuf.append(threadId);
          traceMessageBuf.append("'\n");
          traceMessageBuf.append(packetToSend.dump(packetLen));
          
          connection.getLog().logTrace(traceMessageBuf.toString());
        }
        

        mysqlOutput.write(packetToSend.getByteBuffer(), 0, packetLen);
        mysqlOutput.flush();
      }
      
      if (enablePacketDebug) {
        enqueuePacketForDebugging(true, false, packetLen + 5, packetHeaderBuf, packet);
      }
      



      if (packet == sharedSendPacket) {
        reclaimLargeSharedSendPacket();
      }
      
      if (connection.getMaintainTimeStats()) {
        lastPacketSentTimeMs = System.currentTimeMillis();
      }
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  









  private final ResultSetImpl sendFileToServer(StatementImpl callingStatement, String fileName)
    throws SQLException
  {
    if (useCompression) {
      compressedPacketSequence = ((byte)(compressedPacketSequence + 1));
    }
    
    Buffer filePacket = loadFileBufRef == null ? null : (Buffer)loadFileBufRef.get();
    
    int bigPacketLength = Math.min(connection.getMaxAllowedPacket() - 12, alignPacketSize(connection.getMaxAllowedPacket() - 16, 4096) - 12);
    

    int oneMeg = 1048576;
    
    int smallerPacketSizeAligned = Math.min(oneMeg - 12, alignPacketSize(oneMeg - 16, 4096) - 12);
    
    int packetLength = Math.min(smallerPacketSizeAligned, bigPacketLength);
    
    if (filePacket == null) {
      try {
        filePacket = new Buffer(packetLength + 4);
        loadFileBufRef = new SoftReference(filePacket);
      } catch (OutOfMemoryError oom) {
        throw SQLError.createSQLException("Could not allocate packet of " + packetLength + " bytes required for LOAD DATA LOCAL INFILE operation." + " Try increasing max heap allocation for JVM or decreasing server variable 'max_allowed_packet'", "S1001", getExceptionInterceptor());
      }
    }
    




    filePacket.clear();
    send(filePacket, 0);
    
    byte[] fileBuf = new byte[packetLength];
    
    BufferedInputStream fileIn = null;
    try
    {
      if (!connection.getAllowLoadLocalInfile()) {
        throw SQLError.createSQLException(Messages.getString("MysqlIO.LoadDataLocalNotAllowed"), "S1000", getExceptionInterceptor());
      }
      

      InputStream hookedStream = null;
      
      if (callingStatement != null) {
        hookedStream = callingStatement.getLocalInfileInputStream();
      }
      
      if (hookedStream != null) {
        fileIn = new BufferedInputStream(hookedStream);
      } else if (!connection.getAllowUrlInLocalInfile()) {
        fileIn = new BufferedInputStream(new FileInputStream(fileName));

      }
      else if (fileName.indexOf(':') != -1) {
        try {
          URL urlFromFileName = new URL(fileName);
          fileIn = new BufferedInputStream(urlFromFileName.openStream());
        }
        catch (MalformedURLException badUrlEx) {
          fileIn = new BufferedInputStream(new FileInputStream(fileName));
        }
      } else {
        fileIn = new BufferedInputStream(new FileInputStream(fileName));
      }
      

      int bytesRead = 0;
      
      while ((bytesRead = fileIn.read(fileBuf)) != -1) {
        filePacket.clear();
        filePacket.writeBytesNoNull(fileBuf, 0, bytesRead);
        send(filePacket, filePacket.getPosition());
      }
    } catch (IOException ioEx) {
      StringBuilder messageBuf = new StringBuilder(Messages.getString("MysqlIO.60"));
      
      if ((fileName != null) && (!connection.getParanoid())) {
        messageBuf.append("'");
        messageBuf.append(fileName);
        messageBuf.append("'");
      }
      
      messageBuf.append(Messages.getString("MysqlIO.63"));
      
      if (!connection.getParanoid()) {
        messageBuf.append(Messages.getString("MysqlIO.64"));
        messageBuf.append(Util.stackTraceToString(ioEx));
      }
      
      throw SQLError.createSQLException(messageBuf.toString(), "S1009", getExceptionInterceptor());
    } finally {
      if (fileIn != null) {
        try {
          fileIn.close();
        } catch (Exception ex) {
          SQLException sqlEx = SQLError.createSQLException(Messages.getString("MysqlIO.65"), "S1000", ex, getExceptionInterceptor());
          

          throw sqlEx;
        }
        
        fileIn = null;
      }
      else {
        filePacket.clear();
        send(filePacket, filePacket.getPosition());
        checkErrorPacket();
      }
    }
    

    filePacket.clear();
    send(filePacket, filePacket.getPosition());
    
    Buffer resultPacket = checkErrorPacket();
    
    return buildResultSetWithUpdates(callingStatement, resultPacket);
  }
  










  private Buffer checkErrorPacket(int command)
    throws SQLException
  {
    Buffer resultPacket = null;
    serverStatus = 0;
    

    try
    {
      resultPacket = reuseAndReadPacket(reusablePacket);
    }
    catch (SQLException sqlEx) {
      throw sqlEx;
    } catch (Exception fallThru) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, fallThru, getExceptionInterceptor());
    }
    

    checkErrorPacket(resultPacket);
    
    return resultPacket;
  }
  
  private void checkErrorPacket(Buffer resultPacket) throws SQLException
  {
    int statusCode = resultPacket.readByte();
    

    if (statusCode == -1)
    {
      int errno = 2000;
      
      if (protocolVersion > 9) {
        errno = resultPacket.readInt();
        
        String xOpen = null;
        
        String serverErrorMessage = resultPacket.readString(connection.getErrorMessageEncoding(), getExceptionInterceptor());
        
        if (serverErrorMessage.charAt(0) == '#')
        {

          if (serverErrorMessage.length() > 6) {
            xOpen = serverErrorMessage.substring(1, 6);
            serverErrorMessage = serverErrorMessage.substring(6);
            
            if (xOpen.equals("HY000")) {
              xOpen = SQLError.mysqlToSqlState(errno, connection.getUseSqlStateCodes());
            }
          } else {
            xOpen = SQLError.mysqlToSqlState(errno, connection.getUseSqlStateCodes());
          }
        } else {
          xOpen = SQLError.mysqlToSqlState(errno, connection.getUseSqlStateCodes());
        }
        
        clearInputStream();
        
        StringBuilder errorBuf = new StringBuilder();
        
        String xOpenErrorMessage = SQLError.get(xOpen);
        
        if ((!connection.getUseOnlyServerErrorMessages()) && 
          (xOpenErrorMessage != null)) {
          errorBuf.append(xOpenErrorMessage);
          errorBuf.append(Messages.getString("MysqlIO.68"));
        }
        

        errorBuf.append(serverErrorMessage);
        
        if ((!connection.getUseOnlyServerErrorMessages()) && 
          (xOpenErrorMessage != null)) {
          errorBuf.append("\"");
        }
        

        appendDeadlockStatusInformation(xOpen, errorBuf);
        
        if ((xOpen != null) && (xOpen.startsWith("22"))) {
          throw new MysqlDataTruncation(errorBuf.toString(), 0, true, false, 0, 0, errno);
        }
        throw SQLError.createSQLException(errorBuf.toString(), xOpen, errno, false, getExceptionInterceptor(), connection);
      }
      
      String serverErrorMessage = resultPacket.readString(connection.getErrorMessageEncoding(), getExceptionInterceptor());
      clearInputStream();
      
      if (serverErrorMessage.indexOf(Messages.getString("MysqlIO.70")) != -1) {
        throw SQLError.createSQLException(SQLError.get("S0022") + ", " + serverErrorMessage, "S0022", -1, false, getExceptionInterceptor(), connection);
      }
      

      StringBuilder errorBuf = new StringBuilder(Messages.getString("MysqlIO.72"));
      errorBuf.append(serverErrorMessage);
      errorBuf.append("\"");
      
      throw SQLError.createSQLException(SQLError.get("S1000") + ", " + errorBuf.toString(), "S1000", -1, false, getExceptionInterceptor(), connection);
    }
  }
  
  private void appendDeadlockStatusInformation(String xOpen, StringBuilder errorBuf) throws SQLException
  {
    if ((connection.getIncludeInnodbStatusInDeadlockExceptions()) && (xOpen != null) && ((xOpen.startsWith("40")) || (xOpen.startsWith("41"))) && (streamingData == null))
    {
      ResultSet rs = null;
      try
      {
        rs = sqlQueryDirect(null, "SHOW ENGINE INNODB STATUS", connection.getEncoding(), null, -1, 1003, 1007, false, connection.getCatalog(), null);
        

        if (rs.next()) {
          errorBuf.append("\n\n");
          errorBuf.append(rs.getString("Status"));
        } else {
          errorBuf.append("\n\n");
          errorBuf.append(Messages.getString("MysqlIO.NoInnoDBStatusFound"));
        }
      } catch (Exception ex) {
        errorBuf.append("\n\n");
        errorBuf.append(Messages.getString("MysqlIO.InnoDBStatusFailed"));
        errorBuf.append("\n\n");
        errorBuf.append(Util.stackTraceToString(ex));
      } finally {
        if (rs != null) {
          rs.close();
        }
      }
    }
    
    if (connection.getIncludeThreadDumpInDeadlockExceptions()) {
      errorBuf.append("\n\n*** Java threads running at time of deadlock ***\n\n");
      
      ThreadMXBean threadMBean = ManagementFactory.getThreadMXBean();
      long[] threadIds = threadMBean.getAllThreadIds();
      
      ThreadInfo[] threads = threadMBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
      Object activeThreads = new ArrayList();
      
      for (ThreadInfo info : threads) {
        if (info != null) {
          ((List)activeThreads).add(info);
        }
      }
      
      for (ThreadInfo threadInfo : (List)activeThreads)
      {

        errorBuf.append('"');
        errorBuf.append(threadInfo.getThreadName());
        errorBuf.append("\" tid=");
        errorBuf.append(threadInfo.getThreadId());
        errorBuf.append(" ");
        errorBuf.append(threadInfo.getThreadState());
        
        if (threadInfo.getLockName() != null) {
          errorBuf.append(" on lock=" + threadInfo.getLockName());
        }
        if (threadInfo.isSuspended()) {
          errorBuf.append(" (suspended)");
        }
        if (threadInfo.isInNative()) {
          errorBuf.append(" (running in native)");
        }
        
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        
        if (stackTrace.length > 0) {
          errorBuf.append(" in ");
          errorBuf.append(stackTrace[0].getClassName());
          errorBuf.append(".");
          errorBuf.append(stackTrace[0].getMethodName());
          errorBuf.append("()");
        }
        
        errorBuf.append("\n");
        
        if (threadInfo.getLockOwnerName() != null) {
          errorBuf.append("\t owned by " + threadInfo.getLockOwnerName() + " Id=" + threadInfo.getLockOwnerId());
          errorBuf.append("\n");
        }
        
        for (int j = 0; j < stackTrace.length; j++) {
          StackTraceElement ste = stackTrace[j];
          errorBuf.append("\tat " + ste.toString());
          errorBuf.append("\n");
        }
      }
    }
  }
  





  private final void sendSplitPackets(Buffer packet, int packetLen)
    throws SQLException
  {
    try
    {
      Buffer packetToSend = splitBufRef == null ? null : (Buffer)splitBufRef.get();
      Buffer toCompress = (!useCompression) || (compressBufRef == null) ? null : (Buffer)compressBufRef.get();
      




      if (packetToSend == null) {
        packetToSend = new Buffer(maxThreeBytes + 4);
        splitBufRef = new SoftReference(packetToSend);
      }
      if (useCompression) {
        int cbuflen = packetLen + (packetLen / maxThreeBytes + 1) * 4;
        if (toCompress == null) {
          toCompress = new Buffer(cbuflen);
          compressBufRef = new SoftReference(toCompress);
        } else if (toCompress.getBufLength() < cbuflen) {
          toCompress.setPosition(toCompress.getBufLength());
          toCompress.ensureCapacity(cbuflen - toCompress.getBufLength());
        }
      }
      
      int len = packetLen - 4;
      int splitSize = maxThreeBytes;
      int originalPacketPos = 4;
      byte[] origPacketBytes = packet.getByteBuffer();
      
      int toCompressPosition = 0;
      

      while (len >= 0) {
        packetSequence = ((byte)(packetSequence + 1));
        
        if (len < splitSize) {
          splitSize = len;
        }
        
        packetToSend.setPosition(0);
        packetToSend.writeLongInt(splitSize);
        packetToSend.writeByte(packetSequence);
        if (len > 0) {
          System.arraycopy(origPacketBytes, originalPacketPos, packetToSend.getByteBuffer(), 4, splitSize);
        }
        
        if (useCompression) {
          System.arraycopy(packetToSend.getByteBuffer(), 0, toCompress.getByteBuffer(), toCompressPosition, 4 + splitSize);
          toCompressPosition += 4 + splitSize;
        } else {
          mysqlOutput.write(packetToSend.getByteBuffer(), 0, 4 + splitSize);
          mysqlOutput.flush();
        }
        
        originalPacketPos += splitSize;
        len -= maxThreeBytes;
      }
      


      if (useCompression) {
        len = toCompressPosition;
        toCompressPosition = 0;
        splitSize = maxThreeBytes - 3;
        while (len >= 0) {
          compressedPacketSequence = ((byte)(compressedPacketSequence + 1));
          
          if (len < splitSize) {
            splitSize = len;
          }
          
          Buffer compressedPacketToSend = compressPacket(toCompress, toCompressPosition, splitSize);
          packetLen = compressedPacketToSend.getPosition();
          mysqlOutput.write(compressedPacketToSend.getByteBuffer(), 0, packetLen);
          mysqlOutput.flush();
          
          toCompressPosition += splitSize;
          len -= maxThreeBytes - 3;
        }
      }
    } catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, lastPacketSentTimeMs, lastPacketReceivedTimeMs, ioEx, getExceptionInterceptor());
    }
  }
  
  private void reclaimLargeSharedSendPacket()
  {
    if ((sharedSendPacket != null) && (sharedSendPacket.getCapacity() > 1048576)) {
      sharedSendPacket = new Buffer(1024);
    }
  }
  
  boolean hadWarnings() {
    return hadWarnings;
  }
  
  void scanForAndThrowDataTruncation() throws SQLException {
    if ((streamingData == null) && (versionMeetsMinimum(4, 1, 0)) && (connection.getJdbcCompliantTruncation()) && (warningCount > 0)) {
      int warningCountOld = warningCount;
      SQLError.convertShowWarningsToSQLWarnings(connection, warningCount, true);
      warningCount = warningCountOld;
    }
  }
  











  private void secureAuth(Buffer packet, int packLength, String user, String password, String database, boolean writeClientParams)
    throws SQLException
  {
    if (packet == null) {
      packet = new Buffer(packLength);
    }
    
    if (writeClientParams) {
      if (use41Extensions) {
        if (versionMeetsMinimum(4, 1, 1)) {
          packet.writeLong(clientParam);
          packet.writeLong(maxThreeBytes);
          

          packet.writeByte((byte)8);
          

          packet.writeBytesNoNull(new byte[23]);
        } else {
          packet.writeLong(clientParam);
          packet.writeLong(maxThreeBytes);
        }
      } else {
        packet.writeInt((int)clientParam);
        packet.writeLongInt(maxThreeBytes);
      }
    }
    

    packet.writeString(user, "Cp1252", connection);
    
    if (password.length() != 0)
    {
      packet.writeString("xxxxxxxx", "Cp1252", connection);
    }
    else {
      packet.writeString("", "Cp1252", connection);
    }
    
    if (useConnectWithDb) {
      packet.writeString(database, "Cp1252", connection);
    }
    
    send(packet, packet.getPosition());
    



    if (password.length() > 0) {
      Buffer b = readPacket();
      
      b.setPosition(0);
      
      byte[] replyAsBytes = b.getByteBuffer();
      
      if ((replyAsBytes.length == 24) && (replyAsBytes[0] != 0))
      {
        if (replyAsBytes[0] != 42) {
          try
          {
            byte[] buff = Security.passwordHashStage1(password);
            

            byte[] passwordHash = new byte[buff.length];
            System.arraycopy(buff, 0, passwordHash, 0, buff.length);
            

            passwordHash = Security.passwordHashStage2(passwordHash, replyAsBytes);
            
            byte[] packetDataAfterSalt = new byte[replyAsBytes.length - 4];
            
            System.arraycopy(replyAsBytes, 4, packetDataAfterSalt, 0, replyAsBytes.length - 4);
            
            byte[] mysqlScrambleBuff = new byte[20];
            

            Security.xorString(packetDataAfterSalt, mysqlScrambleBuff, passwordHash, 20);
            

            Security.xorString(mysqlScrambleBuff, buff, buff, 20);
            
            Buffer packet2 = new Buffer(25);
            packet2.writeBytesNoNull(buff);
            
            packetSequence = ((byte)(packetSequence + 1));
            
            send(packet2, 24);
          } catch (NoSuchAlgorithmException nse) {
            throw SQLError.createSQLException(Messages.getString("MysqlIO.91") + Messages.getString("MysqlIO.92"), "S1000", getExceptionInterceptor());
          }
          
        } else {
          try
          {
            byte[] passwordHash = Security.createKeyFromOldPassword(password);
            

            byte[] netReadPos4 = new byte[replyAsBytes.length - 4];
            
            System.arraycopy(replyAsBytes, 4, netReadPos4, 0, replyAsBytes.length - 4);
            
            byte[] mysqlScrambleBuff = new byte[20];
            

            Security.xorString(netReadPos4, mysqlScrambleBuff, passwordHash, 20);
            

            String scrambledPassword = Util.scramble(StringUtils.toString(mysqlScrambleBuff), password);
            
            Buffer packet2 = new Buffer(packLength);
            packet2.writeString(scrambledPassword, "Cp1252", connection);
            packetSequence = ((byte)(packetSequence + 1));
            
            send(packet2, 24);
          } catch (NoSuchAlgorithmException nse) {
            throw SQLError.createSQLException(Messages.getString("MysqlIO.91") + Messages.getString("MysqlIO.92"), "S1000", getExceptionInterceptor());
          }
        }
      }
    }
  }
  













  void secureAuth411(Buffer packet, int packLength, String user, String password, String database, boolean writeClientParams, boolean forChangeUser)
    throws SQLException
  {
    String enc = getEncodingForHandshake();
    

















    if (packet == null) {
      packet = new Buffer(packLength);
    }
    
    if (writeClientParams) {
      if (use41Extensions) {
        if (versionMeetsMinimum(4, 1, 1)) {
          packet.writeLong(clientParam);
          packet.writeLong(maxThreeBytes);
          
          appendCharsetByteForHandshake(packet, enc);
          

          packet.writeBytesNoNull(new byte[23]);
        } else {
          packet.writeLong(clientParam);
          packet.writeLong(maxThreeBytes);
        }
      } else {
        packet.writeInt((int)clientParam);
        packet.writeLongInt(maxThreeBytes);
      }
    }
    

    if (user != null) {
      packet.writeString(user, enc, connection);
    }
    
    if (password.length() != 0) {
      packet.writeByte((byte)20);
      try
      {
        packet.writeBytesNoNull(Security.scramble411(password, seed, connection.getPasswordCharacterEncoding()));
      } catch (NoSuchAlgorithmException nse) {
        throw SQLError.createSQLException(Messages.getString("MysqlIO.91") + Messages.getString("MysqlIO.92"), "S1000", getExceptionInterceptor());
      }
      catch (UnsupportedEncodingException e) {
        throw SQLError.createSQLException(Messages.getString("MysqlIO.91") + Messages.getString("MysqlIO.92"), "S1000", getExceptionInterceptor());
      }
    }
    else
    {
      packet.writeByte((byte)0);
    }
    
    if (useConnectWithDb) {
      packet.writeString(database, enc, connection);
    } else if (forChangeUser)
    {
      packet.writeByte((byte)0);
    }
    

    if ((serverCapabilities & 0x100000) != 0) {
      sendConnectionAttributes(packet, enc, connection);
    }
    
    send(packet, packet.getPosition());
    
    byte savePacketSequence = packetSequence++;
    
    Buffer reply = checkErrorPacket();
    
    if (reply.isAuthMethodSwitchRequestPacket())
    {


      savePacketSequence = (byte)(savePacketSequence + 1);packetSequence = savePacketSequence;
      packet.clear();
      
      String seed323 = seed.substring(0, 8);
      packet.writeString(Util.newCrypt(password, seed323, connection.getPasswordCharacterEncoding()));
      send(packet, packet.getPosition());
      

      checkErrorPacket();
    }
    
    if (!useConnectWithDb) {
      changeDatabaseTo(database);
    }
  }
  









  private final ResultSetRow unpackBinaryResultSetRow(Field[] fields, Buffer binaryData, int resultSetConcurrency)
    throws SQLException
  {
    int numFields = fields.length;
    
    byte[][] unpackedRowData = new byte[numFields][];
    




    int nullCount = (numFields + 9) / 8;
    int nullMaskPos = binaryData.getPosition();
    binaryData.setPosition(nullMaskPos + nullCount);
    int bit = 4;
    




    for (int i = 0; i < numFields; i++) {
      if ((binaryData.readByte(nullMaskPos) & bit) != 0) {
        unpackedRowData[i] = null;
      }
      else if (resultSetConcurrency != 1008) {
        extractNativeEncodedColumn(binaryData, fields, i, unpackedRowData);
      } else {
        unpackNativeEncodedColumn(binaryData, fields, i, unpackedRowData);
      }
      

      if ((bit <<= 1 & 0xFF) == 0) {
        bit = 1;
        
        nullMaskPos++;
      }
    }
    
    return new ByteArrayRow(unpackedRowData, getExceptionInterceptor());
  }
  
  private final void extractNativeEncodedColumn(Buffer binaryData, Field[] fields, int columnIndex, byte[][] unpackedRowData) throws SQLException {
    Field curField = fields[columnIndex];
    int length;
    switch (curField.getMysqlType())
    {
    case 6: 
      break;
    
    case 1: 
      unpackedRowData[columnIndex] = { binaryData.readByte() };
      break;
    

    case 2: 
    case 13: 
      unpackedRowData[columnIndex] = binaryData.getBytes(2);
      break;
    
    case 3: 
    case 9: 
      unpackedRowData[columnIndex] = binaryData.getBytes(4);
      break;
    
    case 8: 
      unpackedRowData[columnIndex] = binaryData.getBytes(8);
      break;
    
    case 4: 
      unpackedRowData[columnIndex] = binaryData.getBytes(4);
      break;
    
    case 5: 
      unpackedRowData[columnIndex] = binaryData.getBytes(8);
      break;
    
    case 11: 
      length = (int)binaryData.readFieldLength();
      
      unpackedRowData[columnIndex] = binaryData.getBytes(length);
      
      break;
    
    case 10: 
      length = (int)binaryData.readFieldLength();
      
      unpackedRowData[columnIndex] = binaryData.getBytes(length);
      
      break;
    case 7: 
    case 12: 
      length = (int)binaryData.readFieldLength();
      
      unpackedRowData[columnIndex] = binaryData.getBytes(length);
      break;
    case 0: 
    case 15: 
    case 16: 
    case 245: 
    case 246: 
    case 249: 
    case 250: 
    case 251: 
    case 252: 
    case 253: 
    case 254: 
    case 255: 
      unpackedRowData[columnIndex] = binaryData.readLenByteArray(0);
      
      break;
    default: 
      throw SQLError.createSQLException(Messages.getString("MysqlIO.97") + curField.getMysqlType() + Messages.getString("MysqlIO.98") + columnIndex + Messages.getString("MysqlIO.99") + fields.length + Messages.getString("MysqlIO.100"), "S1000", getExceptionInterceptor());
    }
    
  }
  
  private final void unpackNativeEncodedColumn(Buffer binaryData, Field[] fields, int columnIndex, byte[][] unpackedRowData)
    throws SQLException
  {
    Field curField = fields[columnIndex];
    int length;
    int hour; int minute; int seconds; int year; int month; int day; int after1000; int after100; switch (curField.getMysqlType())
    {
    case 6: 
      break;
    
    case 1: 
      byte tinyVal = binaryData.readByte();
      
      if (!curField.isUnsigned()) {
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(tinyVal));
      } else {
        short unsignedTinyVal = (short)(tinyVal & 0xFF);
        
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(unsignedTinyVal));
      }
      
      break;
    

    case 2: 
    case 13: 
      short shortVal = (short)binaryData.readInt();
      
      if (!curField.isUnsigned()) {
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(shortVal));
      } else {
        int unsignedShortVal = shortVal & 0xFFFF;
        
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(unsignedShortVal));
      }
      
      break;
    

    case 3: 
    case 9: 
      int intVal = (int)binaryData.readLong();
      
      if (!curField.isUnsigned()) {
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(intVal));
      } else {
        long longVal = intVal & 0xFFFFFFFF;
        
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(longVal));
      }
      
      break;
    

    case 8: 
      long longVal = binaryData.readLongLong();
      
      if (!curField.isUnsigned()) {
        unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(longVal));
      } else {
        BigInteger asBigInteger = ResultSetImpl.convertLongToUlong(longVal);
        
        unpackedRowData[columnIndex] = StringUtils.getBytes(asBigInteger.toString());
      }
      
      break;
    

    case 4: 
      float floatVal = Float.intBitsToFloat(binaryData.readIntAsLong());
      
      unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(floatVal));
      
      break;
    

    case 5: 
      double doubleVal = Double.longBitsToDouble(binaryData.readLongLong());
      
      unpackedRowData[columnIndex] = StringUtils.getBytes(String.valueOf(doubleVal));
      
      break;
    

    case 11: 
      length = (int)binaryData.readFieldLength();
      
      hour = 0;
      minute = 0;
      seconds = 0;
      
      if (length != 0) {
        binaryData.readByte();
        binaryData.readLong();
        hour = binaryData.readByte();
        minute = binaryData.readByte();
        seconds = binaryData.readByte();
        
        if (length > 8) {
          binaryData.readLong();
        }
      }
      
      byte[] timeAsBytes = new byte[8];
      
      timeAsBytes[0] = ((byte)Character.forDigit(hour / 10, 10));
      timeAsBytes[1] = ((byte)Character.forDigit(hour % 10, 10));
      
      timeAsBytes[2] = 58;
      
      timeAsBytes[3] = ((byte)Character.forDigit(minute / 10, 10));
      timeAsBytes[4] = ((byte)Character.forDigit(minute % 10, 10));
      
      timeAsBytes[5] = 58;
      
      timeAsBytes[6] = ((byte)Character.forDigit(seconds / 10, 10));
      timeAsBytes[7] = ((byte)Character.forDigit(seconds % 10, 10));
      
      unpackedRowData[columnIndex] = timeAsBytes;
      
      break;
    
    case 10: 
      length = (int)binaryData.readFieldLength();
      
      year = 0;
      month = 0;
      day = 0;
      
      hour = 0;
      minute = 0;
      seconds = 0;
      
      if (length != 0) {
        year = binaryData.readInt();
        month = binaryData.readByte();
        day = binaryData.readByte();
      }
      
      if ((year == 0) && (month == 0) && (day == 0)) {
        if ("convertToNull".equals(connection.getZeroDateTimeBehavior())) {
          unpackedRowData[columnIndex] = null;
        }
        else {
          if ("exception".equals(connection.getZeroDateTimeBehavior())) {
            throw SQLError.createSQLException("Value '0000-00-00' can not be represented as java.sql.Date", "S1009", getExceptionInterceptor());
          }
          

          year = 1;
          month = 1;
          day = 1;
        }
      } else {
        byte[] dateAsBytes = new byte[10];
        
        dateAsBytes[0] = ((byte)Character.forDigit(year / 1000, 10));
        
        after1000 = year % 1000;
        
        dateAsBytes[1] = ((byte)Character.forDigit(after1000 / 100, 10));
        
        after100 = after1000 % 100;
        
        dateAsBytes[2] = ((byte)Character.forDigit(after100 / 10, 10));
        dateAsBytes[3] = ((byte)Character.forDigit(after100 % 10, 10));
        
        dateAsBytes[4] = 45;
        
        dateAsBytes[5] = ((byte)Character.forDigit(month / 10, 10));
        dateAsBytes[6] = ((byte)Character.forDigit(month % 10, 10));
        
        dateAsBytes[7] = 45;
        
        dateAsBytes[8] = ((byte)Character.forDigit(day / 10, 10));
        dateAsBytes[9] = ((byte)Character.forDigit(day % 10, 10));
        
        unpackedRowData[columnIndex] = dateAsBytes;
      }
      break;
    
    case 7: 
    case 12: 
      length = (int)binaryData.readFieldLength();
      
      year = 0;
      month = 0;
      day = 0;
      
      hour = 0;
      minute = 0;
      seconds = 0;
      
      int nanos = 0;
      
      if (length != 0) {
        year = binaryData.readInt();
        month = binaryData.readByte();
        day = binaryData.readByte();
        
        if (length > 4) {
          hour = binaryData.readByte();
          minute = binaryData.readByte();
          seconds = binaryData.readByte();
        }
      }
      




      if ((year == 0) && (month == 0) && (day == 0)) {
        if ("convertToNull".equals(connection.getZeroDateTimeBehavior())) {
          unpackedRowData[columnIndex] = null;
        }
        else {
          if ("exception".equals(connection.getZeroDateTimeBehavior())) {
            throw SQLError.createSQLException("Value '0000-00-00' can not be represented as java.sql.Timestamp", "S1009", getExceptionInterceptor());
          }
          

          year = 1;
          month = 1;
          day = 1;
        }
      } else {
        int stringLength = 19;
        
        byte[] nanosAsBytes = StringUtils.getBytes(Integer.toString(nanos));
        
        stringLength += 1 + nanosAsBytes.length;
        
        byte[] datetimeAsBytes = new byte[stringLength];
        
        datetimeAsBytes[0] = ((byte)Character.forDigit(year / 1000, 10));
        
        after1000 = year % 1000;
        
        datetimeAsBytes[1] = ((byte)Character.forDigit(after1000 / 100, 10));
        
        after100 = after1000 % 100;
        
        datetimeAsBytes[2] = ((byte)Character.forDigit(after100 / 10, 10));
        datetimeAsBytes[3] = ((byte)Character.forDigit(after100 % 10, 10));
        
        datetimeAsBytes[4] = 45;
        
        datetimeAsBytes[5] = ((byte)Character.forDigit(month / 10, 10));
        datetimeAsBytes[6] = ((byte)Character.forDigit(month % 10, 10));
        
        datetimeAsBytes[7] = 45;
        
        datetimeAsBytes[8] = ((byte)Character.forDigit(day / 10, 10));
        datetimeAsBytes[9] = ((byte)Character.forDigit(day % 10, 10));
        
        datetimeAsBytes[10] = 32;
        
        datetimeAsBytes[11] = ((byte)Character.forDigit(hour / 10, 10));
        datetimeAsBytes[12] = ((byte)Character.forDigit(hour % 10, 10));
        
        datetimeAsBytes[13] = 58;
        
        datetimeAsBytes[14] = ((byte)Character.forDigit(minute / 10, 10));
        datetimeAsBytes[15] = ((byte)Character.forDigit(minute % 10, 10));
        
        datetimeAsBytes[16] = 58;
        
        datetimeAsBytes[17] = ((byte)Character.forDigit(seconds / 10, 10));
        datetimeAsBytes[18] = ((byte)Character.forDigit(seconds % 10, 10));
        
        datetimeAsBytes[19] = 46;
        
        int nanosOffset = 20;
        
        System.arraycopy(nanosAsBytes, 0, datetimeAsBytes, 20, nanosAsBytes.length);
        
        unpackedRowData[columnIndex] = datetimeAsBytes;
      }
      break;
    
    case 0: 
    case 15: 
    case 16: 
    case 245: 
    case 246: 
    case 249: 
    case 250: 
    case 251: 
    case 252: 
    case 253: 
    case 254: 
      unpackedRowData[columnIndex] = binaryData.readLenByteArray(0);
      
      break;
    
    default: 
      throw SQLError.createSQLException(Messages.getString("MysqlIO.97") + curField.getMysqlType() + Messages.getString("MysqlIO.98") + columnIndex + Messages.getString("MysqlIO.99") + fields.length + Messages.getString("MysqlIO.100"), "S1000", getExceptionInterceptor());
    }
    
  }
  











  private void negotiateSSLConnection(String user, String password, String database, int packLength)
    throws SQLException
  {
    if (!ExportControlled.enabled()) {
      throw new ConnectionFeatureNotAvailableException(connection, lastPacketSentTimeMs, null);
    }
    
    if ((serverCapabilities & 0x8000) != 0) {
      clientParam |= 0x8000;
    }
    
    clientParam |= 0x800;
    
    Buffer packet = new Buffer(packLength);
    
    if (use41Extensions) {
      packet.writeLong(clientParam);
      packet.writeLong(maxThreeBytes);
      appendCharsetByteForHandshake(packet, getEncodingForHandshake());
      packet.writeBytesNoNull(new byte[23]);
    } else {
      packet.writeInt((int)clientParam);
    }
    
    send(packet, packet.getPosition());
    
    ExportControlled.transformSocketToSSLSocket(this);
  }
  
  public boolean isSSLEstablished() {
    return (ExportControlled.enabled()) && (ExportControlled.isSSLEstablished(this));
  }
  
  protected int getServerStatus() {
    return serverStatus;
  }
  
  protected List<ResultSetRow> fetchRowsViaCursor(List<ResultSetRow> fetchedRows, long statementId, Field[] columnTypes, int fetchSize, boolean useBufferRowExplicit)
    throws SQLException
  {
    if (fetchedRows == null) {
      fetchedRows = new ArrayList(fetchSize);
    } else {
      fetchedRows.clear();
    }
    
    sharedSendPacket.clear();
    
    sharedSendPacket.writeByte((byte)28);
    sharedSendPacket.writeLong(statementId);
    sharedSendPacket.writeLong(fetchSize);
    
    sendCommand(28, null, sharedSendPacket, true, null, 0);
    
    ResultSetRow row = null;
    
    while ((row = nextRow(columnTypes, columnTypes.length, true, 1007, false, useBufferRowExplicit, false, null)) != null) {
      fetchedRows.add(row);
    }
    
    return fetchedRows;
  }
  
  protected long getThreadId() {
    return threadId;
  }
  
  protected boolean useNanosForElapsedTime() {
    return useNanosForElapsedTime;
  }
  
  protected long getSlowQueryThreshold() {
    return slowQueryThreshold;
  }
  
  protected String getQueryTimingUnits() {
    return queryTimingUnits;
  }
  
  protected int getCommandCount() {
    return commandCount;
  }
  
  private void checkTransactionState(int oldStatus) throws SQLException {
    boolean previouslyInTrans = (oldStatus & 0x1) != 0;
    boolean currentlyInTrans = inTransactionOnServer();
    
    if ((previouslyInTrans) && (!currentlyInTrans)) {
      connection.transactionCompleted();
    } else if ((!previouslyInTrans) && (currentlyInTrans)) {
      connection.transactionBegun();
    }
  }
  
  private void preserveOldTransactionState() {
    serverStatus |= oldServerStatus & 0x1;
  }
  
  protected void setStatementInterceptors(List<StatementInterceptorV2> statementInterceptors) {
    this.statementInterceptors = (statementInterceptors.isEmpty() ? null : statementInterceptors);
  }
  
  protected ExceptionInterceptor getExceptionInterceptor() {
    return exceptionInterceptor;
  }
  
  protected void setSocketTimeout(int milliseconds) throws SQLException {
    try {
      if (mysqlConnection != null) {
        mysqlConnection.setSoTimeout(milliseconds);
      }
    } catch (SocketException e) {
      SQLException sqlEx = SQLError.createSQLException("Invalid socket timeout value or state", "S1009", getExceptionInterceptor());
      
      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  
  protected void releaseResources() {
    if (deflater != null) {
      deflater.end();
      deflater = null;
    }
  }
  



  String getEncodingForHandshake()
  {
    String enc = connection.getEncoding();
    if (enc == null) {
      enc = "UTF-8";
    }
    return enc;
  }
  














  private void appendCharsetByteForHandshake(Buffer packet, String enc)
    throws SQLException
  {
    int charsetIndex = 0;
    if (enc != null) {
      charsetIndex = CharsetMapping.getCollationIndexForJavaEncoding(enc, connection);
    }
    if (charsetIndex == 0) {
      charsetIndex = 33;
    }
    if (charsetIndex > 255) {
      throw SQLError.createSQLException("Invalid character set index for encoding: " + enc, "S1009", getExceptionInterceptor());
    }
    
    packet.writeByte((byte)charsetIndex);
  }
  
  public boolean isEOFDeprecated() {
    return (clientParam & 0x1000000) != 0L;
  }
}
