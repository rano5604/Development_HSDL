package com.mysql.jdbc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

























public class StringUtils
{
  public static enum SearchMode
  {
    ALLOW_BACKSLASH_ESCAPE,  SKIP_BETWEEN_MARKERS,  SKIP_BLOCK_COMMENTS,  SKIP_LINE_COMMENTS,  SKIP_WHITE_SPACE;
    



    private SearchMode() {}
  }
  


  public static final Set<SearchMode> SEARCH_MODE__ALL = Collections.unmodifiableSet(EnumSet.allOf(SearchMode.class));
  



  public static final Set<SearchMode> SEARCH_MODE__MRK_COM_WS = Collections.unmodifiableSet(EnumSet.of(SearchMode.SKIP_BETWEEN_MARKERS, SearchMode.SKIP_BLOCK_COMMENTS, SearchMode.SKIP_LINE_COMMENTS, SearchMode.SKIP_WHITE_SPACE));
  




  public static final Set<SearchMode> SEARCH_MODE__BSESC_COM_WS = Collections.unmodifiableSet(EnumSet.of(SearchMode.ALLOW_BACKSLASH_ESCAPE, SearchMode.SKIP_BLOCK_COMMENTS, SearchMode.SKIP_LINE_COMMENTS, SearchMode.SKIP_WHITE_SPACE));
  




  public static final Set<SearchMode> SEARCH_MODE__BSESC_MRK_WS = Collections.unmodifiableSet(EnumSet.of(SearchMode.ALLOW_BACKSLASH_ESCAPE, SearchMode.SKIP_BETWEEN_MARKERS, SearchMode.SKIP_WHITE_SPACE));
  




  public static final Set<SearchMode> SEARCH_MODE__COM_WS = Collections.unmodifiableSet(EnumSet.of(SearchMode.SKIP_BLOCK_COMMENTS, SearchMode.SKIP_LINE_COMMENTS, SearchMode.SKIP_WHITE_SPACE));
  




  public static final Set<SearchMode> SEARCH_MODE__MRK_WS = Collections.unmodifiableSet(EnumSet.of(SearchMode.SKIP_BETWEEN_MARKERS, SearchMode.SKIP_WHITE_SPACE));
  




  public static final Set<SearchMode> SEARCH_MODE__NONE = Collections.unmodifiableSet(EnumSet.noneOf(SearchMode.class));
  

  private static final int NON_COMMENTS_MYSQL_VERSION_REF_LENGTH = 5;
  
  private static final int BYTE_RANGE = 256;
  
  private static byte[] allBytes = new byte['Ā'];
  
  private static char[] byteToChars = new char['Ā'];
  
  private static Method toPlainStringMethod;
  
  private static final int WILD_COMPARE_MATCH = 0;
  
  private static final int WILD_COMPARE_CONTINUE_WITH_WILD = 1;
  
  private static final int WILD_COMPARE_NO_MATCH = -1;
  static final char WILDCARD_MANY = '%';
  static final char WILDCARD_ONE = '_';
  static final char WILDCARD_ESCAPE = '\\';
  private static final ConcurrentHashMap<String, Charset> charsetsByAlias = new ConcurrentHashMap();
  
  private static final String platformEncoding = System.getProperty("file.encoding");
  private static final String VALID_ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ0123456789$_#@";
  
  static Charset findCharset(String alias) throws UnsupportedEncodingException
  {
    try {
      Charset cs = (Charset)charsetsByAlias.get(alias);
      Charset oldCs;
      if (cs == null) {
        cs = Charset.forName(alias);
        oldCs = (Charset)charsetsByAlias.putIfAbsent(alias, cs);
        if (oldCs == null) {}
      }
      return oldCs;


    }
    catch (UnsupportedCharsetException uce)
    {


      throw new UnsupportedEncodingException(alias);
    } catch (IllegalCharsetNameException icne) {
      throw new UnsupportedEncodingException(alias);
    } catch (IllegalArgumentException iae) {
      throw new UnsupportedEncodingException(alias);
    }
  }
  
  static {
    for (int i = -128; i <= 127; i++) {
      allBytes[(i - -128)] = ((byte)i);
    }
    
    String allBytesString = new String(allBytes, 0, 255);
    
    int allBytesStringLen = allBytesString.length();
    
    for (int i = 0; (i < 255) && (i < allBytesStringLen); i++) {
      byteToChars[i] = allBytesString.charAt(i);
    }
    try
    {
      toPlainStringMethod = BigDecimal.class.getMethod("toPlainString", new Class[0]);
    }
    catch (NoSuchMethodException nsme) {}
  }
  









  public static String consistentToString(BigDecimal decimal)
  {
    if (decimal == null) {
      return null;
    }
    
    if (toPlainStringMethod != null) {
      try {
        return (String)toPlainStringMethod.invoke(decimal, (Object[])null);
      }
      catch (InvocationTargetException invokeEx) {}catch (IllegalAccessException accessEx) {}
    }
    



    return decimal.toString();
  }
  









  public static String dumpAsHex(byte[] byteBuffer, int length)
  {
    StringBuilder outputBuilder = new StringBuilder(length * 4);
    
    int p = 0;
    int rows = length / 8;
    
    for (int i = 0; (i < rows) && (p < length); i++) {
      int ptemp = p;
      
      for (int j = 0; j < 8; j++) {
        String hexVal = Integer.toHexString(byteBuffer[ptemp] & 0xFF);
        
        if (hexVal.length() == 1) {
          hexVal = "0" + hexVal;
        }
        
        outputBuilder.append(hexVal + " ");
        ptemp++;
      }
      
      outputBuilder.append("    ");
      
      for (int j = 0; j < 8; j++) {
        int b = 0xFF & byteBuffer[p];
        
        if ((b > 32) && (b < 127)) {
          outputBuilder.append((char)b + " ");
        } else {
          outputBuilder.append(". ");
        }
        
        p++;
      }
      
      outputBuilder.append("\n");
    }
    
    int n = 0;
    
    for (int i = p; i < length; i++) {
      String hexVal = Integer.toHexString(byteBuffer[i] & 0xFF);
      
      if (hexVal.length() == 1) {
        hexVal = "0" + hexVal;
      }
      
      outputBuilder.append(hexVal + " ");
      n++;
    }
    
    for (int i = n; i < 8; i++) {
      outputBuilder.append("   ");
    }
    
    outputBuilder.append("    ");
    
    for (int i = p; i < length; i++) {
      int b = 0xFF & byteBuffer[i];
      
      if ((b > 32) && (b < 127)) {
        outputBuilder.append((char)b + " ");
      } else {
        outputBuilder.append(". ");
      }
    }
    
    outputBuilder.append("\n");
    
    return outputBuilder.toString();
  }
  
  private static boolean endsWith(byte[] dataFrom, String suffix) {
    for (int i = 1; i <= suffix.length(); i++) {
      int dfOffset = dataFrom.length - i;
      int suffixOffset = suffix.length() - i;
      if (dataFrom[dfOffset] != suffix.charAt(suffixOffset)) {
        return false;
      }
    }
    return true;
  }
  










  public static byte[] escapeEasternUnicodeByteStream(byte[] origBytes, String origString)
  {
    if (origBytes == null) {
      return null;
    }
    if (origBytes.length == 0) {
      return new byte[0];
    }
    
    int bytesLen = origBytes.length;
    int bufIndex = 0;
    int strIndex = 0;
    
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(bytesLen);
    for (;;)
    {
      if (origString.charAt(strIndex) == '\\')
      {
        bytesOut.write(origBytes[(bufIndex++)]);

      }
      else
      {
        int loByte = origBytes[bufIndex];
        
        if (loByte < 0) {
          loByte += 256;
        }
        

        bytesOut.write(loByte);
        
















        if (loByte >= 128) {
          if (bufIndex < bytesLen - 1) {
            int hiByte = origBytes[(bufIndex + 1)];
            
            if (hiByte < 0) {
              hiByte += 256;
            }
            

            bytesOut.write(hiByte);
            bufIndex++;
            

            if (hiByte == 92) {
              bytesOut.write(hiByte);
            }
          }
        } else if ((loByte == 92) && 
          (bufIndex < bytesLen - 1)) {
          int hiByte = origBytes[(bufIndex + 1)];
          
          if (hiByte < 0) {
            hiByte += 256;
          }
          
          if (hiByte == 98)
          {
            bytesOut.write(92);
            bytesOut.write(98);
            bufIndex++;
          }
        }
        

        bufIndex++;
      }
      
      if (bufIndex >= bytesLen) {
        break;
      }
      

      strIndex++;
    }
    
    return bytesOut.toByteArray();
  }
  







  public static char firstNonWsCharUc(String searchIn)
  {
    return firstNonWsCharUc(searchIn, 0);
  }
  
  public static char firstNonWsCharUc(String searchIn, int startAt) {
    if (searchIn == null) {
      return '\000';
    }
    
    int length = searchIn.length();
    
    for (int i = startAt; i < length; i++) {
      char c = searchIn.charAt(i);
      
      if (!Character.isWhitespace(c)) {
        return Character.toUpperCase(c);
      }
    }
    
    return '\000';
  }
  
  public static char firstAlphaCharUc(String searchIn, int startAt) {
    if (searchIn == null) {
      return '\000';
    }
    
    int length = searchIn.length();
    
    for (int i = startAt; i < length; i++) {
      char c = searchIn.charAt(i);
      
      if (Character.isLetter(c)) {
        return Character.toUpperCase(c);
      }
    }
    
    return '\000';
  }
  








  public static String fixDecimalExponent(String dString)
  {
    int ePos = dString.indexOf('E');
    
    if (ePos == -1) {
      ePos = dString.indexOf('e');
    }
    
    if ((ePos != -1) && 
      (dString.length() > ePos + 1)) {
      char maybeMinusChar = dString.charAt(ePos + 1);
      
      if ((maybeMinusChar != '-') && (maybeMinusChar != '+')) {
        StringBuilder strBuilder = new StringBuilder(dString.length() + 1);
        strBuilder.append(dString.substring(0, ePos + 1));
        strBuilder.append('+');
        strBuilder.append(dString.substring(ePos + 1, dString.length()));
        dString = strBuilder.toString();
      }
    }
    

    return dString;
  }
  

  public static byte[] getBytes(char[] c, SingleByteCharsetConverter converter, String encoding, String serverEncoding, boolean parserKnowsUnicode, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      byte[] b;
      
      byte[] b;
      if (converter != null) {
        b = converter.toBytes(c); } else { byte[] b;
        if (encoding == null) {
          b = getBytes(c);
        } else {
          b = getBytes(c, encoding);
          
          if ((!parserKnowsUnicode) && (CharsetMapping.requiresEscapeEasternUnicode(encoding)))
          {
            if (encoding.equalsIgnoreCase(serverEncoding)) {} } } }
      return escapeEasternUnicodeByteStream(b, new String(c));


    }
    catch (UnsupportedEncodingException uee)
    {

      throw SQLError.createSQLException(Messages.getString("StringUtils.0") + encoding + Messages.getString("StringUtils.1"), "S1009", exceptionInterceptor);
    }
  }
  

  public static byte[] getBytes(char[] c, SingleByteCharsetConverter converter, String encoding, String serverEncoding, int offset, int length, boolean parserKnowsUnicode, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      byte[] b;
      
      byte[] b;
      
      if (converter != null) {
        b = converter.toBytes(c, offset, length); } else { byte[] b;
        if (encoding == null) {
          b = getBytes(c, offset, length);
        } else {
          b = getBytes(c, offset, length, encoding);
          
          if ((!parserKnowsUnicode) && (CharsetMapping.requiresEscapeEasternUnicode(encoding)))
          {
            if (encoding.equalsIgnoreCase(serverEncoding)) {} } } }
      return escapeEasternUnicodeByteStream(b, new String(c, offset, length));


    }
    catch (UnsupportedEncodingException uee)
    {

      throw SQLError.createSQLException(Messages.getString("StringUtils.0") + encoding + Messages.getString("StringUtils.1"), "S1009", exceptionInterceptor);
    }
  }
  



  public static byte[] getBytes(char[] c, String encoding, String serverEncoding, boolean parserKnowsUnicode, MySQLConnection conn, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      SingleByteCharsetConverter converter = conn != null ? conn.getCharsetConverter(encoding) : SingleByteCharsetConverter.getInstance(encoding, null);
      
      return getBytes(c, converter, encoding, serverEncoding, parserKnowsUnicode, exceptionInterceptor);
    } catch (UnsupportedEncodingException uee) {
      throw SQLError.createSQLException(Messages.getString("StringUtils.0") + encoding + Messages.getString("StringUtils.1"), "S1009", exceptionInterceptor);
    }
  }
  

  public static byte[] getBytes(String s, SingleByteCharsetConverter converter, String encoding, String serverEncoding, boolean parserKnowsUnicode, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      byte[] b;
      
      byte[] b;
      
      if (converter != null) {
        b = converter.toBytes(s); } else { byte[] b;
        if (encoding == null) {
          b = getBytes(s);
        } else {
          b = getBytes(s, encoding);
          
          if ((!parserKnowsUnicode) && (CharsetMapping.requiresEscapeEasternUnicode(encoding)))
          {
            if (encoding.equalsIgnoreCase(serverEncoding)) {} } } }
      return escapeEasternUnicodeByteStream(b, s);


    }
    catch (UnsupportedEncodingException uee)
    {

      throw SQLError.createSQLException(Messages.getString("StringUtils.5") + encoding + Messages.getString("StringUtils.6"), "S1009", exceptionInterceptor);
    }
  }
  

  public static byte[] getBytes(String s, SingleByteCharsetConverter converter, String encoding, String serverEncoding, int offset, int length, boolean parserKnowsUnicode, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      byte[] b;
      
      byte[] b;
      
      if (converter != null) {
        b = converter.toBytes(s, offset, length); } else { byte[] b;
        if (encoding == null) {
          b = getBytes(s, offset, length);
        } else {
          s = s.substring(offset, offset + length);
          b = getBytes(s, encoding);
          
          if ((!parserKnowsUnicode) && (CharsetMapping.requiresEscapeEasternUnicode(encoding)))
          {
            if (encoding.equalsIgnoreCase(serverEncoding)) {} } } }
      return escapeEasternUnicodeByteStream(b, s);


    }
    catch (UnsupportedEncodingException uee)
    {

      throw SQLError.createSQLException(Messages.getString("StringUtils.5") + encoding + Messages.getString("StringUtils.6"), "S1009", exceptionInterceptor);
    }
  }
  



  public static byte[] getBytes(String s, String encoding, String serverEncoding, boolean parserKnowsUnicode, MySQLConnection conn, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      SingleByteCharsetConverter converter = conn != null ? conn.getCharsetConverter(encoding) : SingleByteCharsetConverter.getInstance(encoding, null);
      
      return getBytes(s, converter, encoding, serverEncoding, parserKnowsUnicode, exceptionInterceptor);
    } catch (UnsupportedEncodingException uee) {
      throw SQLError.createSQLException(Messages.getString("StringUtils.5") + encoding + Messages.getString("StringUtils.6"), "S1009", exceptionInterceptor);
    }
  }
  



  public static final byte[] getBytes(String s, String encoding, String serverEncoding, int offset, int length, boolean parserKnowsUnicode, MySQLConnection conn, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      SingleByteCharsetConverter converter = conn != null ? conn.getCharsetConverter(encoding) : SingleByteCharsetConverter.getInstance(encoding, null);
      
      return getBytes(s, converter, encoding, serverEncoding, offset, length, parserKnowsUnicode, exceptionInterceptor);
    } catch (UnsupportedEncodingException uee) {
      throw SQLError.createSQLException(Messages.getString("StringUtils.5") + encoding + Messages.getString("StringUtils.6"), "S1009", exceptionInterceptor);
    }
  }
  

  public static byte[] getBytesWrapped(String s, char beginWrap, char endWrap, SingleByteCharsetConverter converter, String encoding, String serverEncoding, boolean parserKnowsUnicode, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      byte[] b;
      
      byte[] b;
      
      if (converter != null) {
        b = converter.toBytesWrapped(s, beginWrap, endWrap); } else { byte[] b;
        if (encoding == null) {
          StringBuilder strBuilder = new StringBuilder(s.length() + 2);
          strBuilder.append(beginWrap);
          strBuilder.append(s);
          strBuilder.append(endWrap);
          
          b = getBytes(strBuilder.toString());
        } else {
          StringBuilder strBuilder = new StringBuilder(s.length() + 2);
          strBuilder.append(beginWrap);
          strBuilder.append(s);
          strBuilder.append(endWrap);
          
          s = strBuilder.toString();
          b = getBytes(s, encoding);
          
          if ((!parserKnowsUnicode) && (CharsetMapping.requiresEscapeEasternUnicode(encoding)))
          {
            if (encoding.equalsIgnoreCase(serverEncoding)) {} } } }
      return escapeEasternUnicodeByteStream(b, s);


    }
    catch (UnsupportedEncodingException uee)
    {

      throw SQLError.createSQLException(Messages.getString("StringUtils.10") + encoding + Messages.getString("StringUtils.11"), "S1009", exceptionInterceptor);
    }
  }
  
  public static int getInt(byte[] buf) throws NumberFormatException
  {
    return getInt(buf, 0, buf.length);
  }
  
  public static int getInt(byte[] buf, int offset, int endPos) throws NumberFormatException {
    int base = 10;
    
    int s = offset;
    

    while ((s < endPos) && (Character.isWhitespace((char)buf[s]))) {
      s++;
    }
    
    if (s == endPos) {
      throw new NumberFormatException(toString(buf));
    }
    

    boolean negative = false;
    
    if ((char)buf[s] == '-') {
      negative = true;
      s++;
    } else if ((char)buf[s] == '+') {
      s++;
    }
    

    int save = s;
    
    int cutoff = Integer.MAX_VALUE / base;
    int cutlim = Integer.MAX_VALUE % base;
    
    if (negative) {
      cutlim++;
    }
    
    boolean overflow = false;
    
    int i = 0;
    for (; 
        s < endPos; s++) {
      char c = (char)buf[s];
      
      if (Character.isDigit(c)) {
        c = (char)(c - '0');
      } else { if (!Character.isLetter(c)) break;
        c = (char)(Character.toUpperCase(c) - 'A' + 10);
      }
      


      if (c >= base) {
        break;
      }
      

      if ((i > cutoff) || ((i == cutoff) && (c > cutlim))) {
        overflow = true;
      } else {
        i *= base;
        i += c;
      }
    }
    
    if (s == save) {
      throw new NumberFormatException(toString(buf));
    }
    
    if (overflow) {
      throw new NumberFormatException(toString(buf));
    }
    

    return negative ? -i : i;
  }
  
  public static long getLong(byte[] buf) throws NumberFormatException {
    return getLong(buf, 0, buf.length);
  }
  
  public static long getLong(byte[] buf, int offset, int endpos) throws NumberFormatException {
    int base = 10;
    
    int s = offset;
    

    while ((s < endpos) && (Character.isWhitespace((char)buf[s]))) {
      s++;
    }
    
    if (s == endpos) {
      throw new NumberFormatException(toString(buf));
    }
    

    boolean negative = false;
    
    if ((char)buf[s] == '-') {
      negative = true;
      s++;
    } else if ((char)buf[s] == '+') {
      s++;
    }
    

    int save = s;
    
    long cutoff = Long.MAX_VALUE / base;
    long cutlim = (int)(Long.MAX_VALUE % base);
    
    if (negative) {
      cutlim += 1L;
    }
    
    boolean overflow = false;
    long i = 0L;
    for (; 
        s < endpos; s++) {
      char c = (char)buf[s];
      
      if (Character.isDigit(c)) {
        c = (char)(c - '0');
      } else { if (!Character.isLetter(c)) break;
        c = (char)(Character.toUpperCase(c) - 'A' + 10);
      }
      


      if (c >= base) {
        break;
      }
      

      if ((i > cutoff) || ((i == cutoff) && (c > cutlim))) {
        overflow = true;
      } else {
        i *= base;
        i += c;
      }
    }
    
    if (s == save) {
      throw new NumberFormatException(toString(buf));
    }
    
    if (overflow) {
      throw new NumberFormatException(toString(buf));
    }
    

    return negative ? -i : i;
  }
  
  public static short getShort(byte[] buf) throws NumberFormatException {
    return getShort(buf, 0, buf.length);
  }
  
  public static short getShort(byte[] buf, int offset, int endpos) throws NumberFormatException {
    short base = 10;
    
    int s = offset;
    

    while ((s < endpos) && (Character.isWhitespace((char)buf[s]))) {
      s++;
    }
    
    if (s == endpos) {
      throw new NumberFormatException(toString(buf));
    }
    

    boolean negative = false;
    
    if ((char)buf[s] == '-') {
      negative = true;
      s++;
    } else if ((char)buf[s] == '+') {
      s++;
    }
    

    int save = s;
    
    short cutoff = (short)(Short.MAX_VALUE / base);
    short cutlim = (short)(Short.MAX_VALUE % base);
    
    if (negative) {
      cutlim = (short)(cutlim + 1);
    }
    
    boolean overflow = false;
    short i = 0;
    for (; 
        s < endpos; s++) {
      char c = (char)buf[s];
      
      if (Character.isDigit(c)) {
        c = (char)(c - '0');
      } else { if (!Character.isLetter(c)) break;
        c = (char)(Character.toUpperCase(c) - 'A' + 10);
      }
      


      if (c >= base) {
        break;
      }
      

      if ((i > cutoff) || ((i == cutoff) && (c > cutlim))) {
        overflow = true;
      } else {
        i = (short)(i * base);
        i = (short)(i + c);
      }
    }
    
    if (s == save) {
      throw new NumberFormatException(toString(buf));
    }
    
    if (overflow) {
      throw new NumberFormatException(toString(buf));
    }
    

    return negative ? (short)-i : i;
  }
  








  public static int indexOfIgnoreCase(String searchIn, String searchFor)
  {
    return indexOfIgnoreCase(0, searchIn, searchFor);
  }
  










  public static int indexOfIgnoreCase(int startingPosition, String searchIn, String searchFor)
  {
    if ((searchIn == null) || (searchFor == null)) {
      return -1;
    }
    
    int searchInLength = searchIn.length();
    int searchForLength = searchFor.length();
    int stopSearchingAt = searchInLength - searchForLength;
    
    if ((startingPosition > stopSearchingAt) || (searchForLength == 0)) {
      return -1;
    }
    

    char firstCharOfSearchForUc = Character.toUpperCase(searchFor.charAt(0));
    char firstCharOfSearchForLc = Character.toLowerCase(searchFor.charAt(0));
    
    for (int i = startingPosition; i <= stopSearchingAt; i++) {
      if (isCharAtPosNotEqualIgnoreCase(searchIn, i, firstCharOfSearchForUc, firstCharOfSearchForLc)) {
        do {
          i++; } while ((i <= stopSearchingAt) && (isCharAtPosNotEqualIgnoreCase(searchIn, i, firstCharOfSearchForUc, firstCharOfSearchForLc)));
      }
      

      if ((i <= stopSearchingAt) && (startsWithIgnoreCase(searchIn, i, searchFor))) {
        return i;
      }
    }
    
    return -1;
  }
  























  public static int indexOfIgnoreCase(int startingPosition, String searchIn, String[] searchForSequence, String openingMarkers, String closingMarkers, Set<SearchMode> searchMode)
  {
    if ((searchIn == null) || (searchForSequence == null)) {
      return -1;
    }
    
    int searchInLength = searchIn.length();
    int searchForLength = 0;
    for (String searchForPart : searchForSequence) {
      searchForLength += searchForPart.length();
    }
    
    if (searchForLength == 0) {
      return -1;
    }
    
    int searchForWordsCount = searchForSequence.length;
    searchForLength += (searchForWordsCount > 0 ? searchForWordsCount - 1 : 0);
    int stopSearchingAt = searchInLength - searchForLength;
    
    if (startingPosition > stopSearchingAt) {
      return -1;
    }
    
    if ((searchMode.contains(SearchMode.SKIP_BETWEEN_MARKERS)) && ((openingMarkers == null) || (closingMarkers == null) || (openingMarkers.length() != closingMarkers.length())))
    {
      throw new IllegalArgumentException(Messages.getString("StringUtils.15", new String[] { openingMarkers, closingMarkers }));
    }
    
    if ((Character.isWhitespace(searchForSequence[0].charAt(0))) && (searchMode.contains(SearchMode.SKIP_WHITE_SPACE)))
    {
      searchMode = EnumSet.copyOf(searchMode);
      searchMode.remove(SearchMode.SKIP_WHITE_SPACE);
    }
    


    Set<SearchMode> searchMode2 = EnumSet.of(SearchMode.SKIP_WHITE_SPACE);
    searchMode2.addAll(searchMode);
    searchMode2.remove(SearchMode.SKIP_BETWEEN_MARKERS);
    
    for (int positionOfFirstWord = startingPosition; positionOfFirstWord <= stopSearchingAt; positionOfFirstWord++) {
      positionOfFirstWord = indexOfIgnoreCase(positionOfFirstWord, searchIn, searchForSequence[0], openingMarkers, closingMarkers, searchMode);
      
      if ((positionOfFirstWord == -1) || (positionOfFirstWord > stopSearchingAt)) {
        return -1;
      }
      
      int startingPositionForNextWord = positionOfFirstWord + searchForSequence[0].length();
      int wc = 0;
      boolean match = true;
      for (;;) { wc++; if ((wc >= searchForWordsCount) || (!match)) break;
        int positionOfNextWord = indexOfNextChar(startingPositionForNextWord, searchInLength - 1, searchIn, null, null, null, searchMode2);
        if ((startingPositionForNextWord == positionOfNextWord) || (!startsWithIgnoreCase(searchIn, positionOfNextWord, searchForSequence[wc])))
        {
          match = false;
        } else {
          startingPositionForNextWord = positionOfNextWord + searchForSequence[wc].length();
        }
      }
      
      if (match) {
        return positionOfFirstWord;
      }
    }
    
    return -1;
  }
  


















  public static int indexOfIgnoreCase(int startingPosition, String searchIn, String searchFor, String openingMarkers, String closingMarkers, Set<SearchMode> searchMode)
  {
    return indexOfIgnoreCase(startingPosition, searchIn, searchFor, openingMarkers, closingMarkers, "", searchMode);
  }
  






















  public static int indexOfIgnoreCase(int startingPosition, String searchIn, String searchFor, String openingMarkers, String closingMarkers, String overridingMarkers, Set<SearchMode> searchMode)
  {
    if ((searchIn == null) || (searchFor == null)) {
      return -1;
    }
    
    int searchInLength = searchIn.length();
    int searchForLength = searchFor.length();
    int stopSearchingAt = searchInLength - searchForLength;
    
    if ((startingPosition > stopSearchingAt) || (searchForLength == 0)) {
      return -1;
    }
    
    if (searchMode.contains(SearchMode.SKIP_BETWEEN_MARKERS)) {
      if ((openingMarkers == null) || (closingMarkers == null) || (openingMarkers.length() != closingMarkers.length())) {
        throw new IllegalArgumentException(Messages.getString("StringUtils.15", new String[] { openingMarkers, closingMarkers }));
      }
      if (overridingMarkers == null) {
        throw new IllegalArgumentException(Messages.getString("StringUtils.16", new String[] { overridingMarkers, openingMarkers }));
      }
      for (char c : overridingMarkers.toCharArray()) {
        if (openingMarkers.indexOf(c) == -1) {
          throw new IllegalArgumentException(Messages.getString("StringUtils.16", new String[] { overridingMarkers, openingMarkers }));
        }
      }
    }
    

    char firstCharOfSearchForUc = Character.toUpperCase(searchFor.charAt(0));
    char firstCharOfSearchForLc = Character.toLowerCase(searchFor.charAt(0));
    
    if ((Character.isWhitespace(firstCharOfSearchForLc)) && (searchMode.contains(SearchMode.SKIP_WHITE_SPACE)))
    {
      searchMode = EnumSet.copyOf(searchMode);
      searchMode.remove(SearchMode.SKIP_WHITE_SPACE);
    }
    
    for (int i = startingPosition; i <= stopSearchingAt; i++) {
      i = indexOfNextChar(i, stopSearchingAt, searchIn, openingMarkers, closingMarkers, overridingMarkers, searchMode);
      
      if (i == -1) {
        return -1;
      }
      
      char c = searchIn.charAt(i);
      
      if ((isCharEqualIgnoreCase(c, firstCharOfSearchForUc, firstCharOfSearchForLc)) && (startsWithIgnoreCase(searchIn, i, searchFor))) {
        return i;
      }
    }
    
    return -1;
  }
  


















  private static int indexOfNextChar(int startingPosition, int stopPosition, String searchIn, String openingMarkers, String closingMarkers, String overridingMarkers, Set<SearchMode> searchMode)
  {
    if (searchIn == null) {
      return -1;
    }
    
    int searchInLength = searchIn.length();
    
    if (startingPosition >= searchInLength) {
      return -1;
    }
    
    char c0 = '\000';
    char c1 = searchIn.charAt(startingPosition);
    char c2 = startingPosition + 1 < searchInLength ? searchIn.charAt(startingPosition + 1) : '\000';
    
    for (int i = startingPosition; i <= stopPosition; i++) {
      c0 = c1;
      c1 = c2;
      c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000';
      
      boolean dashDashCommentImmediateEnd = false;
      int markerIndex = -1;
      
      if ((searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE)) && (c0 == '\\')) {
        i++;
        
        c1 = c2;
        c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000';
      }
      else if ((searchMode.contains(SearchMode.SKIP_BETWEEN_MARKERS)) && ((markerIndex = openingMarkers.indexOf(c0)) != -1))
      {
        int nestedMarkersCount = 0;
        char openingMarker = c0;
        char closingMarker = closingMarkers.charAt(markerIndex);
        boolean outerIsAnOverridingMarker = overridingMarkers.indexOf(openingMarker) != -1;
        for (;;) { i++; if ((i > stopPosition) || (((c0 = searchIn.charAt(i)) == closingMarker) && (nestedMarkersCount == 0))) break;
          if ((!outerIsAnOverridingMarker) && (overridingMarkers.indexOf(c0) != -1))
          {
            int overridingMarkerIndex = openingMarkers.indexOf(c0);
            int overridingNestedMarkersCount = 0;
            char overridingOpeningMarker = c0;
            char overridingClosingMarker = closingMarkers.charAt(overridingMarkerIndex);
            for (;;) { i++; if ((i > stopPosition) || (((c0 = searchIn.charAt(i)) == overridingClosingMarker) && (overridingNestedMarkersCount == 0)))
                break;
              if (c0 == overridingOpeningMarker) {
                overridingNestedMarkersCount++;
              } else if (c0 == overridingClosingMarker) {
                overridingNestedMarkersCount--;
              } else if ((searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE)) && (c0 == '\\')) {
                i++;
              }
            }
          } else if (c0 == openingMarker) {
            nestedMarkersCount++;
          } else if (c0 == closingMarker) {
            nestedMarkersCount--;
          } else if ((searchMode.contains(SearchMode.ALLOW_BACKSLASH_ESCAPE)) && (c0 == '\\')) {
            i++;
          }
        }
        
        c1 = i + 1 < searchInLength ? searchIn.charAt(i + 1) : '\000';
        c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000';
      }
      else if ((searchMode.contains(SearchMode.SKIP_BLOCK_COMMENTS)) && (c0 == '/') && (c1 == '*')) {
        if (c2 != '!')
        {
          i++;
          do {
            do { i++; if (i > stopPosition) break; } while (searchIn.charAt(i) != '*'); } while ((i + 1 < searchInLength ? searchIn.charAt(i + 1) : 0) != 47);
          

          i++;
        }
        else
        {
          i++;
          i++;
          
          for (int j = 1; 
              j <= 5; j++) {
            if ((i + j >= searchInLength) || (!Character.isDigit(searchIn.charAt(i + j)))) {
              break;
            }
          }
          if (j == 5) {
            i += 5;
          }
        }
        
        c1 = i + 1 < searchInLength ? searchIn.charAt(i + 1) : '\000';
        c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000';
      }
      else if ((searchMode.contains(SearchMode.SKIP_BLOCK_COMMENTS)) && (c0 == '*') && (c1 == '/'))
      {

        i++;
        
        c1 = c2;
        c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000';
      } else {
        if (searchMode.contains(SearchMode.SKIP_LINE_COMMENTS)) { if ((c0 == '-') && (c1 == '-')) { if (!Character.isWhitespace(c2)) if (((dashDashCommentImmediateEnd = c2 == ';' ? 1 : 0) != 0) || (c2 == 0)) {} } else if (c0 != '#') {
              break label968;
            }
          if (dashDashCommentImmediateEnd)
          {
            i++;
            i++;
            
            c1 = i + 1 < searchInLength ? searchIn.charAt(i + 1) : '\000';
            c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000'; continue;
          }
          do {
            i++; } while ((i <= stopPosition) && ((c0 = searchIn.charAt(i)) != '\n') && (c0 != '\r'));
          


          c1 = i + 1 < searchInLength ? searchIn.charAt(i + 1) : '\000';
          if ((c0 == '\r') && (c1 == '\n'))
          {
            i++;
            c1 = i + 1 < searchInLength ? searchIn.charAt(i + 1) : '\000';
          }
          c2 = i + 2 < searchInLength ? searchIn.charAt(i + 2) : '\000'; continue;
        }
        label968:
        if ((!searchMode.contains(SearchMode.SKIP_WHITE_SPACE)) || (!Character.isWhitespace(c0))) {
          return i;
        }
      }
    }
    return -1;
  }
  
  private static boolean isCharAtPosNotEqualIgnoreCase(String searchIn, int pos, char firstCharOfSearchForUc, char firstCharOfSearchForLc) {
    return (Character.toLowerCase(searchIn.charAt(pos)) != firstCharOfSearchForLc) && (Character.toUpperCase(searchIn.charAt(pos)) != firstCharOfSearchForUc);
  }
  
  private static boolean isCharEqualIgnoreCase(char charToCompare, char compareToCharUC, char compareToCharLC) {
    return (Character.toLowerCase(charToCompare) == compareToCharLC) || (Character.toUpperCase(charToCompare) == compareToCharUC);
  }
  













  public static List<String> split(String stringToSplit, String delimiter, boolean trim)
  {
    if (stringToSplit == null) {
      return new ArrayList();
    }
    
    if (delimiter == null) {
      throw new IllegalArgumentException();
    }
    
    StringTokenizer tokenizer = new StringTokenizer(stringToSplit, delimiter, false);
    
    List<String> splitTokens = new ArrayList(tokenizer.countTokens());
    
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      
      if (trim) {
        token = token.trim();
      }
      
      splitTokens.add(token);
    }
    
    return splitTokens;
  }
  

















  public static List<String> split(String stringToSplit, String delimiter, String openingMarkers, String closingMarkers, boolean trim)
  {
    return split(stringToSplit, delimiter, openingMarkers, closingMarkers, "", trim);
  }
  






















  public static List<String> split(String stringToSplit, String delimiter, String openingMarkers, String closingMarkers, String overridingMarkers, boolean trim)
  {
    if (stringToSplit == null) {
      return new ArrayList();
    }
    
    if (delimiter == null) {
      throw new IllegalArgumentException();
    }
    
    int delimPos = 0;
    int currentPos = 0;
    
    List<String> splitTokens = new ArrayList();
    

    while ((delimPos = indexOfIgnoreCase(currentPos, stringToSplit, delimiter, openingMarkers, closingMarkers, overridingMarkers, SEARCH_MODE__MRK_COM_WS)) != -1) {
      String token = stringToSplit.substring(currentPos, delimPos);
      
      if (trim) {
        token = token.trim();
      }
      
      splitTokens.add(token);
      currentPos = delimPos + 1;
    }
    
    if (currentPos < stringToSplit.length()) {
      String token = stringToSplit.substring(currentPos);
      
      if (trim) {
        token = token.trim();
      }
      
      splitTokens.add(token);
    }
    
    return splitTokens;
  }
  
  private static boolean startsWith(byte[] dataFrom, String chars) {
    int charsLength = chars.length();
    
    if (dataFrom.length < charsLength) {
      return false;
    }
    for (int i = 0; i < charsLength; i++) {
      if (dataFrom[i] != chars.charAt(i)) {
        return false;
      }
    }
    return true;
  }
  













  public static boolean startsWithIgnoreCase(String searchIn, int startAt, String searchFor)
  {
    return searchIn.regionMatches(true, startAt, searchFor, 0, searchFor.length());
  }
  










  public static boolean startsWithIgnoreCase(String searchIn, String searchFor)
  {
    return startsWithIgnoreCase(searchIn, 0, searchFor);
  }
  











  public static boolean startsWithIgnoreCaseAndNonAlphaNumeric(String searchIn, String searchFor)
  {
    if (searchIn == null) {
      return searchFor == null;
    }
    
    int beginPos = 0;
    int inLength = searchIn.length();
    for (; 
        beginPos < inLength; beginPos++) {
      char c = searchIn.charAt(beginPos);
      if (Character.isLetterOrDigit(c)) {
        break;
      }
    }
    
    return startsWithIgnoreCase(searchIn, beginPos, searchFor);
  }
  










  public static boolean startsWithIgnoreCaseAndWs(String searchIn, String searchFor)
  {
    return startsWithIgnoreCaseAndWs(searchIn, searchFor, 0);
  }
  













  public static boolean startsWithIgnoreCaseAndWs(String searchIn, String searchFor, int beginPos)
  {
    if (searchIn == null) {
      return searchFor == null;
    }
    
    int inLength = searchIn.length();
    for (; 
        beginPos < inLength; beginPos++) {
      if (!Character.isWhitespace(searchIn.charAt(beginPos))) {
        break;
      }
    }
    
    return startsWithIgnoreCase(searchIn, beginPos, searchFor);
  }
  










  public static int startsWithIgnoreCaseAndWs(String searchIn, String[] searchFor)
  {
    for (int i = 0; i < searchFor.length; i++) {
      if (startsWithIgnoreCaseAndWs(searchIn, searchFor[i], 0)) {
        return i;
      }
    }
    return -1;
  }
  




  public static byte[] stripEnclosure(byte[] source, String prefix, String suffix)
  {
    if ((source.length >= prefix.length() + suffix.length()) && (startsWith(source, prefix)) && (endsWith(source, suffix)))
    {
      int totalToStrip = prefix.length() + suffix.length();
      int enclosedLength = source.length - totalToStrip;
      byte[] enclosed = new byte[enclosedLength];
      
      int startPos = prefix.length();
      int numToCopy = enclosed.length;
      System.arraycopy(source, startPos, enclosed, 0, numToCopy);
      
      return enclosed;
    }
    return source;
  }
  







  public static String toAsciiString(byte[] buffer)
  {
    return toAsciiString(buffer, 0, buffer.length);
  }
  











  public static String toAsciiString(byte[] buffer, int startPos, int length)
  {
    char[] charArray = new char[length];
    int readpoint = startPos;
    
    for (int i = 0; i < length; i++) {
      charArray[i] = ((char)buffer[readpoint]);
      readpoint++;
    }
    
    return new String(charArray);
  }
  







  public static boolean wildCompareIgnoreCase(String searchIn, String searchFor)
  {
    return wildCompareInternal(searchIn, searchFor) == 0;
  }
  













  private static int wildCompareInternal(String searchIn, String searchFor)
  {
    if ((searchIn == null) || (searchFor == null)) {
      return -1;
    }
    
    if (searchFor.equals("%")) {
      return 0;
    }
    
    int searchForPos = 0;
    int searchForEnd = searchFor.length();
    
    int searchInPos = 0;
    int searchInEnd = searchIn.length();
    
    int result = -1;
    
    while (searchForPos != searchForEnd) {
      while ((searchFor.charAt(searchForPos) != '%') && (searchFor.charAt(searchForPos) != '_')) {
        if ((searchFor.charAt(searchForPos) == '\\') && (searchForPos + 1 != searchForEnd)) {
          searchForPos++;
        }
        
        if ((searchInPos == searchInEnd) || (Character.toUpperCase(searchFor.charAt(searchForPos++)) != Character.toUpperCase(searchIn.charAt(searchInPos++))))
        {
          return 1;
        }
        
        if (searchForPos == searchForEnd) {
          return searchInPos != searchInEnd ? 1 : 0;
        }
        
        result = 1;
      }
      
      if (searchFor.charAt(searchForPos) == '_') {
        do {
          if (searchInPos == searchInEnd) {
            return result;
          }
          searchInPos++;
          searchForPos++; } while ((searchForPos < searchForEnd) && (searchFor.charAt(searchForPos) == '_'));
        
        if (searchForPos == searchForEnd) {
          break;
        }
        
      }
      else if (searchFor.charAt(searchForPos) == '%') {
        searchForPos++;
        for (; 
            
            searchForPos != searchForEnd; searchForPos++) {
          if (searchFor.charAt(searchForPos) != '%')
          {


            if (searchFor.charAt(searchForPos) != '_') break;
            if (searchInPos == searchInEnd) {
              return -1;
            }
            searchInPos++;
          }
        }
        



        if (searchForPos == searchForEnd) {
          return 0;
        }
        
        if (searchInPos == searchInEnd) {
          return -1;
        }
        
        char cmp;
        if (((cmp = searchFor.charAt(searchForPos)) == '\\') && (searchForPos + 1 != searchForEnd)) {
          cmp = searchFor.charAt(++searchForPos);
        }
        
        searchForPos++;
        do
        {
          while ((searchInPos != searchInEnd) && (Character.toUpperCase(searchIn.charAt(searchInPos)) != Character.toUpperCase(cmp))) {
            searchInPos++;
          }
          
          if (searchInPos++ == searchInEnd) {
            return -1;
          }
          
          int tmp = wildCompareInternal(searchIn.substring(searchInPos), searchFor.substring(searchForPos));
          if (tmp <= 0) {
            return tmp;
          }
          
        } while (searchInPos != searchInEnd);
        
        return -1;
      }
    }
    
    return searchInPos != searchInEnd ? 1 : 0;
  }
  
  static byte[] s2b(String s, MySQLConnection conn) throws SQLException {
    if (s == null) {
      return null;
    }
    
    if ((conn != null) && (conn.getUseUnicode())) {
      try {
        String encoding = conn.getEncoding();
        
        if (encoding == null) {
          return s.getBytes();
        }
        
        SingleByteCharsetConverter converter = conn.getCharsetConverter(encoding);
        
        if (converter != null) {
          return converter.toBytes(s);
        }
        
        return s.getBytes(encoding);
      } catch (UnsupportedEncodingException E) {
        return s.getBytes();
      }
    }
    
    return s.getBytes();
  }
  
  public static int lastIndexOf(byte[] s, char c) {
    if (s == null) {
      return -1;
    }
    
    for (int i = s.length - 1; i >= 0; i--) {
      if (s[i] == c) {
        return i;
      }
    }
    
    return -1;
  }
  
  public static int indexOf(byte[] s, char c) {
    if (s == null) {
      return -1;
    }
    
    int length = s.length;
    
    for (int i = 0; i < length; i++) {
      if (s[i] == c) {
        return i;
      }
    }
    
    return -1;
  }
  
  public static boolean isNullOrEmpty(String toTest) {
    return (toTest == null) || (toTest.length() == 0);
  }
  




















  public static String stripComments(String src, String stringOpens, String stringCloses, boolean slashStarComments, boolean slashSlashComments, boolean hashComments, boolean dashDashComments)
  {
    if (src == null) {
      return null;
    }
    
    StringBuilder strBuilder = new StringBuilder(src.length());
    



    StringReader sourceReader = new StringReader(src);
    
    int contextMarker = 0;
    boolean escaped = false;
    int markerTypeFound = -1;
    
    int ind = 0;
    
    int currentChar = 0;
    try
    {
      while ((currentChar = sourceReader.read()) != -1)
      {
        if ((markerTypeFound != -1) && (currentChar == stringCloses.charAt(markerTypeFound)) && (!escaped)) {
          contextMarker = 0;
          markerTypeFound = -1;
        } else if (((ind = stringOpens.indexOf(currentChar)) != -1) && (!escaped) && (contextMarker == 0)) {
          markerTypeFound = ind;
          contextMarker = currentChar;
        }
        
        if ((contextMarker == 0) && (currentChar == 47) && ((slashSlashComments) || (slashStarComments))) {
          currentChar = sourceReader.read();
          if ((currentChar == 42) && (slashStarComments)) {
            int prevChar = 0;
            while (((currentChar = sourceReader.read()) != 47) || (prevChar != 42)) {
              if (currentChar == 13)
              {
                currentChar = sourceReader.read();
                if (currentChar == 10) {
                  currentChar = sourceReader.read();
                }
              }
              else if (currentChar == 10)
              {
                currentChar = sourceReader.read();
              }
              
              if (currentChar < 0) {
                break;
              }
              prevChar = currentChar;
            }
          }
          if ((currentChar != 47) || (!slashSlashComments)) {}
        } else { while (((currentChar = sourceReader.read()) != 10) && (currentChar != 13) && (currentChar >= 0))
          {
            continue;
            if ((contextMarker == 0) && (currentChar == 35) && (hashComments)) {}
            for (;;) {
              if (((currentChar = sourceReader.read()) != 10) && (currentChar != 13) && (currentChar >= 0)) {
                continue;
                if ((contextMarker == 0) && (currentChar == 45) && (dashDashComments)) {
                  currentChar = sourceReader.read();
                  
                  if ((currentChar == -1) || (currentChar != 45)) {
                    strBuilder.append('-');
                    
                    if (currentChar == -1) break;
                    strBuilder.append((char)currentChar); break;
                  }
                  





                  while (((currentChar = sourceReader.read()) != 10) && (currentChar != 13) && (currentChar >= 0)) {}
                }
              }
            } } }
        if (currentChar != -1) {
          strBuilder.append((char)currentChar);
        }
      }
    }
    catch (IOException ioEx) {}
    

    return strBuilder.toString();
  }
  











  public static String sanitizeProcOrFuncName(String src)
  {
    if ((src == null) || (src.equals("%"))) {
      return null;
    }
    
    return src;
  }
  














  public static List<String> splitDBdotName(String source, String catalog, String quoteId, boolean isNoBslashEscSet)
  {
    if ((source == null) || (source.equals("%"))) {
      return Collections.emptyList();
    }
    
    int dotIndex = -1;
    if (" ".equals(quoteId)) {
      dotIndex = source.indexOf(".");
    } else {
      dotIndex = indexOfIgnoreCase(0, source, ".", quoteId, quoteId, isNoBslashEscSet ? SEARCH_MODE__MRK_WS : SEARCH_MODE__BSESC_MRK_WS);
    }
    
    String database = catalog;
    String entityName;
    String entityName; if (dotIndex != -1) {
      database = unQuoteIdentifier(source.substring(0, dotIndex), quoteId);
      entityName = unQuoteIdentifier(source.substring(dotIndex + 1), quoteId);
    } else {
      entityName = unQuoteIdentifier(source, quoteId);
    }
    
    return Arrays.asList(new String[] { database, entityName });
  }
  
  public static boolean isEmptyOrWhitespaceOnly(String str) {
    if ((str == null) || (str.length() == 0)) {
      return true;
    }
    
    int length = str.length();
    
    for (int i = 0; i < length; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return false;
      }
    }
    
    return true;
  }
  
  public static String escapeQuote(String src, String quotChar) {
    if (src == null) {
      return null;
    }
    
    src = toString(stripEnclosure(src.getBytes(), quotChar, quotChar));
    
    int lastNdx = src.indexOf(quotChar);
    


    String tmpSrc = src.substring(0, lastNdx);
    tmpSrc = tmpSrc + quotChar + quotChar;
    
    String tmpRest = src.substring(lastNdx + 1, src.length());
    
    lastNdx = tmpRest.indexOf(quotChar);
    while (lastNdx > -1)
    {
      tmpSrc = tmpSrc + tmpRest.substring(0, lastNdx);
      tmpSrc = tmpSrc + quotChar + quotChar;
      tmpRest = tmpRest.substring(lastNdx + 1, tmpRest.length());
      
      lastNdx = tmpRest.indexOf(quotChar);
    }
    
    tmpSrc = tmpSrc + tmpRest;
    src = tmpSrc;
    
    return src;
  }
  





























  public static String quoteIdentifier(String identifier, String quoteChar, boolean isPedantic)
  {
    if (identifier == null) {
      return null;
    }
    
    identifier = identifier.trim();
    
    int quoteCharLength = quoteChar.length();
    if ((quoteCharLength == 0) || (" ".equals(quoteChar))) {
      return identifier;
    }
    

    if ((!isPedantic) && (identifier.startsWith(quoteChar)) && (identifier.endsWith(quoteChar)))
    {
      String identifierQuoteTrimmed = identifier.substring(quoteCharLength, identifier.length() - quoteCharLength);
      

      int quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar);
      while (quoteCharPos >= 0) {
        int quoteCharNextExpectedPos = quoteCharPos + quoteCharLength;
        int quoteCharNextPosition = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextExpectedPos);
        
        if (quoteCharNextPosition != quoteCharNextExpectedPos) break;
        quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextPosition + quoteCharLength);
      }
      




      if (quoteCharPos < 0) {
        return identifier;
      }
    }
    
    return quoteChar + identifier.replaceAll(quoteChar, new StringBuilder().append(quoteChar).append(quoteChar).toString()) + quoteChar;
  }
  


















  public static String quoteIdentifier(String identifier, boolean isPedantic)
  {
    return quoteIdentifier(identifier, "`", isPedantic);
  }
  




















  public static String unQuoteIdentifier(String identifier, String quoteChar)
  {
    if (identifier == null) {
      return null;
    }
    
    identifier = identifier.trim();
    
    int quoteCharLength = quoteChar.length();
    if ((quoteCharLength == 0) || (" ".equals(quoteChar))) {
      return identifier;
    }
    

    if ((identifier.startsWith(quoteChar)) && (identifier.endsWith(quoteChar)))
    {
      String identifierQuoteTrimmed = identifier.substring(quoteCharLength, identifier.length() - quoteCharLength);
      

      int quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar);
      while (quoteCharPos >= 0) {
        int quoteCharNextExpectedPos = quoteCharPos + quoteCharLength;
        int quoteCharNextPosition = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextExpectedPos);
        
        if (quoteCharNextPosition == quoteCharNextExpectedPos) {
          quoteCharPos = identifierQuoteTrimmed.indexOf(quoteChar, quoteCharNextPosition + quoteCharLength);
        }
        else {
          return identifier;
        }
      }
      
      return identifier.substring(quoteCharLength, identifier.length() - quoteCharLength).replaceAll(quoteChar + quoteChar, quoteChar);
    }
    
    return identifier;
  }
  
  public static int indexOfQuoteDoubleAware(String searchIn, String quoteChar, int startFrom) {
    if ((searchIn == null) || (quoteChar == null) || (quoteChar.length() == 0) || (startFrom > searchIn.length())) {
      return -1;
    }
    
    int lastIndex = searchIn.length() - 1;
    
    int beginPos = startFrom;
    int pos = -1;
    
    boolean next = true;
    while (next) {
      pos = searchIn.indexOf(quoteChar, beginPos);
      if ((pos == -1) || (pos == lastIndex) || (!searchIn.startsWith(quoteChar, pos + 1))) {
        next = false;
      } else {
        beginPos = pos + 2;
      }
    }
    
    return pos;
  }
  






  public static String toString(byte[] value, int offset, int length, String encoding)
    throws UnsupportedEncodingException
  {
    Charset cs = findCharset(encoding);
    
    return cs.decode(ByteBuffer.wrap(value, offset, length)).toString();
  }
  
  public static String toString(byte[] value, String encoding) throws UnsupportedEncodingException {
    Charset cs = findCharset(encoding);
    
    return cs.decode(ByteBuffer.wrap(value)).toString();
  }
  
  public static String toString(byte[] value, int offset, int length) {
    try {
      Charset cs = findCharset(platformEncoding);
      
      return cs.decode(ByteBuffer.wrap(value, offset, length)).toString();
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static String toString(byte[] value) {
    try {
      Charset cs = findCharset(platformEncoding);
      
      return cs.decode(ByteBuffer.wrap(value)).toString();
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static byte[] getBytes(char[] value) {
    try {
      return getBytes(value, 0, value.length, platformEncoding);
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static byte[] getBytes(char[] value, int offset, int length) {
    try {
      return getBytes(value, offset, length, platformEncoding);
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static byte[] getBytes(char[] value, String encoding) throws UnsupportedEncodingException {
    return getBytes(value, 0, value.length, encoding);
  }
  
  public static byte[] getBytes(char[] value, int offset, int length, String encoding) throws UnsupportedEncodingException {
    Charset cs = findCharset(encoding);
    
    ByteBuffer buf = cs.encode(CharBuffer.wrap(value, offset, length));
    

    int encodedLen = buf.limit();
    byte[] asBytes = new byte[encodedLen];
    buf.get(asBytes, 0, encodedLen);
    
    return asBytes;
  }
  
  public static byte[] getBytes(String value) {
    try {
      return getBytes(value, 0, value.length(), platformEncoding);
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static byte[] getBytes(String value, int offset, int length) {
    try {
      return getBytes(value, offset, length, platformEncoding);
    }
    catch (UnsupportedEncodingException e) {}
    

    return null;
  }
  
  public static byte[] getBytes(String value, String encoding) throws UnsupportedEncodingException {
    return getBytes(value, 0, value.length(), encoding);
  }
  



  public static byte[] getBytes(String value, int offset, int length, String encoding)
    throws UnsupportedEncodingException
  {
    if (!Util.isJdbc4()) {
      if ((offset != 0) || (length != value.length())) {
        return value.substring(offset, offset + length).getBytes(encoding);
      }
      return value.getBytes(encoding);
    }
    
    Charset cs = findCharset(encoding);
    
    ByteBuffer buf = cs.encode(CharBuffer.wrap(value.toCharArray(), offset, length));
    

    int encodedLen = buf.limit();
    byte[] asBytes = new byte[encodedLen];
    buf.get(asBytes, 0, encodedLen);
    
    return asBytes;
  }
  
  public static final boolean isValidIdChar(char c) {
    return "abcdefghijklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ0123456789$_#@".indexOf(c) != -1;
  }
  
  private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  
  public static void appendAsHex(StringBuilder builder, byte[] bytes) {
    builder.append("0x");
    for (byte b : bytes) {
      builder.append(HEX_DIGITS[(b >>> 4 & 0xF)]).append(HEX_DIGITS[(b & 0xF)]);
    }
  }
  
  public static void appendAsHex(StringBuilder builder, int value) {
    if (value == 0) {
      builder.append("0x0");
      return;
    }
    
    int shift = 32;
    
    boolean nonZeroFound = false;
    
    builder.append("0x");
    do {
      shift -= 4;
      byte nibble = (byte)(value >>> shift & 0xF);
      if (nonZeroFound) {
        builder.append(HEX_DIGITS[nibble]);
      } else if (nibble != 0) {
        builder.append(HEX_DIGITS[nibble]);
        nonZeroFound = true;
      }
    } while (shift != 0);
  }
  
  public static byte[] getBytesNullTerminated(String value, String encoding) throws UnsupportedEncodingException {
    Charset cs = findCharset(encoding);
    
    ByteBuffer buf = cs.encode(value);
    
    int encodedLen = buf.limit();
    byte[] asBytes = new byte[encodedLen + 1];
    buf.get(asBytes, 0, encodedLen);
    asBytes[encodedLen] = 0;
    
    return asBytes;
  }
  







  public static boolean isStrictlyNumeric(CharSequence cs)
  {
    if ((cs == null) || (cs.length() == 0)) {
      return false;
    }
    for (int i = 0; i < cs.length(); i++) {
      if (!Character.isDigit(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }
  
  public StringUtils() {}
}
