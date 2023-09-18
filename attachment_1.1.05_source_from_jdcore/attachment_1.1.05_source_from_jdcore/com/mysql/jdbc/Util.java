package com.mysql.jdbc;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

































public class Util
{
  private static Util enclosingInstance = new Util();
  
  private static boolean isJdbc4;
  
  private static boolean isJdbc42;
  
  private static int jvmVersion = -1;
  
  private static int jvmUpdateNumber = -1;
  
  private static boolean isColdFusion = false;
  

















  private static final ConcurrentMap<Class<?>, Boolean> isJdbcInterfaceCache;
  

















  private static final String MYSQL_JDBC_PACKAGE_ROOT;
  


















  public static boolean isJdbc4()
  {
    return isJdbc4;
  }
  
  public static boolean isJdbc42() {
    return isJdbc42;
  }
  
  public static int getJVMVersion() {
    return jvmVersion;
  }
  
  public static boolean jvmMeetsMinimum(int version, int updateNumber) {
    return (getJVMVersion() > version) || ((getJVMVersion() == version) && (getJVMUpdateNumber() >= updateNumber));
  }
  
  public static int getJVMUpdateNumber() {
    return jvmUpdateNumber;
  }
  
  public static boolean isColdFusion() {
    return isColdFusion;
  }
  


  public static boolean isCommunityEdition(String serverVersion)
  {
    return !isEnterpriseEdition(serverVersion);
  }
  


  public static boolean isEnterpriseEdition(String serverVersion)
  {
    return (serverVersion.contains("enterprise")) || (serverVersion.contains("commercial")) || (serverVersion.contains("advanced"));
  }
  



  public static String newCrypt(String password, String seed, String encoding)
  {
    if ((password == null) || (password.length() == 0)) {
      return password;
    }
    
    long[] pw = newHash(seed.getBytes());
    long[] msg = hashPre41Password(password, encoding);
    long max = 1073741823L;
    long seed1 = (pw[0] ^ msg[0]) % max;
    long seed2 = (pw[1] ^ msg[1]) % max;
    char[] chars = new char[seed.length()];
    
    for (int i = 0; i < seed.length(); i++) {
      seed1 = (seed1 * 3L + seed2) % max;
      seed2 = (seed1 + seed2 + 33L) % max;
      double d = seed1 / max;
      byte b = (byte)(int)Math.floor(d * 31.0D + 64.0D);
      chars[i] = ((char)b);
    }
    
    seed1 = (seed1 * 3L + seed2) % max;
    seed2 = (seed1 + seed2 + 33L) % max;
    double d = seed1 / max;
    byte b = (byte)(int)Math.floor(d * 31.0D);
    
    for (int i = 0; i < seed.length(); tmp213_211++) {
      int tmp213_211 = i; char[] tmp213_209 = chars;tmp213_209[tmp213_211] = ((char)(tmp213_209[tmp213_211] ^ (char)b));
    }
    
    return new String(chars);
  }
  
  public static long[] hashPre41Password(String password, String encoding)
  {
    try {
      return newHash(password.replaceAll("\\s", "").getBytes(encoding));
    } catch (UnsupportedEncodingException e) {}
    return new long[0];
  }
  
  public static long[] hashPre41Password(String password)
  {
    return hashPre41Password(password, Charset.defaultCharset().name());
  }
  
  static long[] newHash(byte[] password) {
    long nr = 1345345333L;
    long add = 7L;
    long nr2 = 305419889L;
    

    for (byte b : password) {
      long tmp = 0xFF & b;
      nr ^= ((nr & 0x3F) + add) * tmp + (nr << 8);
      nr2 += (nr2 << 8 ^ nr);
      add += tmp;
    }
    
    long[] result = new long[2];
    result[0] = (nr & 0x7FFFFFFF);
    result[1] = (nr2 & 0x7FFFFFFF);
    
    return result;
  }
  



  public static String oldCrypt(String password, String seed)
  {
    long max = 33554431L;
    


    if ((password == null) || (password.length() == 0)) {
      return password;
    }
    
    long hp = oldHash(seed);
    long hm = oldHash(password);
    
    long nr = hp ^ hm;
    nr %= max;
    long s1 = nr;
    long s2 = nr / 2L;
    
    char[] chars = new char[seed.length()];
    
    for (int i = 0; i < seed.length(); i++) {
      s1 = (s1 * 3L + s2) % max;
      s2 = (s1 + s2 + 33L) % max;
      double d = s1 / max;
      byte b = (byte)(int)Math.floor(d * 31.0D + 64.0D);
      chars[i] = ((char)b);
    }
    
    return new String(chars);
  }
  
  static long oldHash(String password) {
    long nr = 1345345333L;
    long nr2 = 7L;
    

    for (int i = 0; i < password.length(); i++) {
      if ((password.charAt(i) != ' ') && (password.charAt(i) != '\t'))
      {


        long tmp = password.charAt(i);
        nr ^= ((nr & 0x3F) + nr2) * tmp + (nr << 8);
        nr2 += tmp;
      }
    }
    return nr & 0x7FFFFFFF;
  }
  
  private static RandStructcture randomInit(long seed1, long seed2) {
    Util tmp7_4 = enclosingInstance;tmp7_4.getClass();RandStructcture randStruct = new RandStructcture(tmp7_4);
    
    maxValue = 1073741823L;
    maxValueDbl = maxValue;
    seed1 = (seed1 % maxValue);
    seed2 = (seed2 % maxValue);
    
    return randStruct;
  }
  











  public static Object readObject(ResultSet resultSet, int index)
    throws Exception
  {
    ObjectInputStream objIn = new ObjectInputStream(resultSet.getBinaryStream(index));
    Object obj = objIn.readObject();
    objIn.close();
    
    return obj;
  }
  
  private static double rnd(RandStructcture randStruct) {
    seed1 = ((seed1 * 3L + seed2) % maxValue);
    seed2 = ((seed1 + seed2 + 33L) % maxValue);
    
    return seed1 / maxValueDbl;
  }
  





  public static String scramble(String message, String password)
  {
    byte[] to = new byte[8];
    String val = "";
    
    message = message.substring(0, 8);
    
    if ((password != null) && (password.length() > 0)) {
      long[] hashPass = hashPre41Password(password);
      long[] hashMessage = newHash(message.getBytes());
      
      RandStructcture randStruct = randomInit(hashPass[0] ^ hashMessage[0], hashPass[1] ^ hashMessage[1]);
      
      int msgPos = 0;
      int msgLength = message.length();
      int toPos = 0;
      
      while (msgPos++ < msgLength) {
        to[(toPos++)] = ((byte)(int)(Math.floor(rnd(randStruct) * 31.0D) + 64.0D));
      }
      

      byte extra = (byte)(int)Math.floor(rnd(randStruct) * 31.0D);
      
      for (int i = 0; i < to.length; i++) {
        int tmp143_141 = i; byte[] tmp143_139 = to;tmp143_139[tmp143_141] = ((byte)(tmp143_139[tmp143_141] ^ extra));
      }
      
      val = StringUtils.toString(to);
    }
    
    return val;
  }
  








  public static String stackTraceToString(Throwable ex)
  {
    StringBuilder traceBuf = new StringBuilder();
    traceBuf.append(Messages.getString("Util.1"));
    
    if (ex != null) {
      traceBuf.append(ex.getClass().getName());
      
      String message = ex.getMessage();
      
      if (message != null) {
        traceBuf.append(Messages.getString("Util.2"));
        traceBuf.append(message);
      }
      
      StringWriter out = new StringWriter();
      
      PrintWriter printOut = new PrintWriter(out);
      
      ex.printStackTrace(printOut);
      
      traceBuf.append(Messages.getString("Util.3"));
      traceBuf.append(out.toString());
    }
    
    traceBuf.append(Messages.getString("Util.4"));
    
    return traceBuf.toString();
  }
  
  public static Object getInstance(String className, Class<?>[] argTypes, Object[] args, ExceptionInterceptor exceptionInterceptor) throws SQLException
  {
    try {
      return handleNewInstance(Class.forName(className).getConstructor(argTypes), args, exceptionInterceptor);
    } catch (SecurityException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    } catch (NoSuchMethodException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    } catch (ClassNotFoundException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    }
  }
  


  public static final Object handleNewInstance(Constructor<?> ctor, Object[] args, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    try
    {
      return ctor.newInstance(args);
    } catch (IllegalArgumentException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    } catch (InstantiationException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    } catch (IllegalAccessException e) {
      throw SQLError.createSQLException("Can't instantiate required class", "S1000", e, exceptionInterceptor);
    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();
      
      if ((target instanceof SQLException)) {
        throw ((SQLException)target);
      }
      
      if ((target instanceof ExceptionInInitializerError)) {
        target = ((ExceptionInInitializerError)target).getException();
      }
      
      throw SQLError.createSQLException(target.toString(), "S1000", target, exceptionInterceptor);
    }
  }
  






  public static boolean interfaceExists(String hostname)
  {
    try
    {
      Class<?> networkInterfaceClass = Class.forName("java.net.NetworkInterface");
      return networkInterfaceClass.getMethod("getByName", (Class[])null).invoke(networkInterfaceClass, new Object[] { hostname }) != null;
    } catch (Throwable t) {}
    return false;
  }
  
  public static void resultSetToMap(Map mappedValues, ResultSet rs)
    throws SQLException
  {
    while (rs.next()) {
      mappedValues.put(rs.getObject(1), rs.getObject(2));
    }
  }
  
  public static void resultSetToMap(Map mappedValues, ResultSet rs, int key, int value) throws SQLException
  {
    while (rs.next()) {
      mappedValues.put(rs.getObject(key), rs.getObject(value));
    }
  }
  
  public static void resultSetToMap(Map mappedValues, ResultSet rs, String key, String value) throws SQLException
  {
    while (rs.next()) {
      mappedValues.put(rs.getObject(key), rs.getObject(value));
    }
  }
  
  public static Map<Object, Object> calculateDifferences(Map<?, ?> map1, Map<?, ?> map2) {
    Map<Object, Object> diffMap = new HashMap();
    
    for (Map.Entry<?, ?> entry : map1.entrySet()) {
      Object key = entry.getKey();
      
      Number value1 = null;
      Number value2 = null;
      
      if ((entry.getValue() instanceof Number))
      {
        value1 = (Number)entry.getValue();
        value2 = (Number)map2.get(key);
      } else {
        try {
          value1 = new Double(entry.getValue().toString());
          value2 = new Double(map2.get(key).toString());
        } catch (NumberFormatException nfe) {}
        continue;
      }
      

      if (!value1.equals(value2))
      {


        if ((value1 instanceof Byte)) {
          diffMap.put(key, Byte.valueOf((byte)(((Byte)value2).byteValue() - ((Byte)value1).byteValue())));
        } else if ((value1 instanceof Short)) {
          diffMap.put(key, Short.valueOf((short)(((Short)value2).shortValue() - ((Short)value1).shortValue())));
        } else if ((value1 instanceof Integer)) {
          diffMap.put(key, Integer.valueOf(((Integer)value2).intValue() - ((Integer)value1).intValue()));
        } else if ((value1 instanceof Long)) {
          diffMap.put(key, Long.valueOf(((Long)value2).longValue() - ((Long)value1).longValue()));
        } else if ((value1 instanceof Float)) {
          diffMap.put(key, Float.valueOf(((Float)value2).floatValue() - ((Float)value1).floatValue()));
        } else if ((value1 instanceof Double)) {
          diffMap.put(key, Double.valueOf(((Double)value2).shortValue() - ((Double)value1).shortValue()));
        } else if ((value1 instanceof BigDecimal)) {
          diffMap.put(key, ((BigDecimal)value2).subtract((BigDecimal)value1));
        } else if ((value1 instanceof BigInteger)) {
          diffMap.put(key, ((BigInteger)value2).subtract((BigInteger)value1));
        }
      }
    }
    return diffMap;
  }
  










  public static List<Extension> loadExtensions(Connection conn, Properties props, String extensionClassNames, String errorMessageKey, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    List<Extension> extensionList = new LinkedList();
    
    List<String> interceptorsToCreate = StringUtils.split(extensionClassNames, ",", true);
    
    String className = null;
    try
    {
      int i = 0; for (int s = interceptorsToCreate.size(); i < s; i++) {
        className = (String)interceptorsToCreate.get(i);
        Extension extensionInstance = (Extension)Class.forName(className).newInstance();
        extensionInstance.init(conn, props);
        
        extensionList.add(extensionInstance);
      }
    } catch (Throwable t) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString(errorMessageKey, new Object[] { className }), exceptionInterceptor);
      sqlEx.initCause(t);
      
      throw sqlEx;
    }
    
    return extensionList;
  }
  








  public static boolean isJdbcInterface(Class<?> clazz)
  {
    if (isJdbcInterfaceCache.containsKey(clazz)) {
      return ((Boolean)isJdbcInterfaceCache.get(clazz)).booleanValue();
    }
    
    if (clazz.isInterface()) {
      try {
        if (isJdbcPackage(getPackageName(clazz))) {
          isJdbcInterfaceCache.putIfAbsent(clazz, Boolean.valueOf(true));
          return true;
        }
      }
      catch (Exception ex) {}
    }
    




    for (Class<?> iface : clazz.getInterfaces()) {
      if (isJdbcInterface(iface)) {
        isJdbcInterfaceCache.putIfAbsent(clazz, Boolean.valueOf(true));
        return true;
      }
    }
    
    if ((clazz.getSuperclass() != null) && (isJdbcInterface(clazz.getSuperclass()))) {
      isJdbcInterfaceCache.putIfAbsent(clazz, Boolean.valueOf(true));
      return true;
    }
    
    isJdbcInterfaceCache.putIfAbsent(clazz, Boolean.valueOf(false));
    return false;
  }
  
  static
  {
    try
    {
      Class.forName("java.sql.NClob");
      isJdbc4 = true;
    } catch (ClassNotFoundException e) {
      isJdbc4 = false;
    }
    try
    {
      Class.forName("java.sql.JDBCType");
      isJdbc42 = true;
    } catch (Throwable t) {
      isJdbc42 = false;
    }
    
    String jvmVersionString = System.getProperty("java.version");
    int startPos = jvmVersionString.indexOf('.');
    int endPos = startPos + 1;
    if (startPos != -1) {
      do { if (!Character.isDigit(jvmVersionString.charAt(endPos))) break; endPos++; } while (endPos < jvmVersionString.length());
    }
    

    startPos++;
    if (endPos > startPos) {
      jvmVersion = Integer.parseInt(jvmVersionString.substring(startPos, endPos));
    }
    else {
      jvmVersion = isJdbc4 ? 6 : isJdbc42 ? 8 : 5;
    }
    startPos = jvmVersionString.indexOf("_");
    endPos = startPos + 1;
    if (startPos != -1) {
      do { if (!Character.isDigit(jvmVersionString.charAt(endPos))) break; endPos++; } while (endPos < jvmVersionString.length());
    }
    

    startPos++;
    if (endPos > startPos) {
      jvmUpdateNumber = Integer.parseInt(jvmVersionString.substring(startPos, endPos));
    }
    






    String loadedFrom = stackTraceToString(new Throwable());
    
    if (loadedFrom != null) {
      isColdFusion = loadedFrom.indexOf("coldfusion") != -1;
    } else {
      isColdFusion = false;
    }
    


























































































































































































































































































































































































































































    isJdbcInterfaceCache = new ConcurrentHashMap();
    













































    String packageName = getPackageName(MultiHostConnectionProxy.class);
    
    MYSQL_JDBC_PACKAGE_ROOT = packageName.substring(0, packageName.indexOf("jdbc") + 4);
  }
  





  public static boolean isJdbcPackage(String packageName)
  {
    return (packageName != null) && ((packageName.startsWith("java.sql")) || (packageName.startsWith("javax.sql")) || (packageName.startsWith(MYSQL_JDBC_PACKAGE_ROOT)));
  }
  


  private static final ConcurrentMap<Class<?>, Class<?>[]> implementedInterfacesCache = new ConcurrentHashMap();
  








  public static Class<?>[] getImplementedInterfaces(Class<?> clazz)
  {
    Class<?>[] implementedInterfaces = (Class[])implementedInterfacesCache.get(clazz);
    if (implementedInterfaces != null) {
      return implementedInterfaces;
    }
    
    Set<Class<?>> interfaces = new LinkedHashSet();
    Class<?> superClass = clazz;
    do {
      Collections.addAll(interfaces, (Class[])superClass.getInterfaces());
    } while ((superClass = superClass.getSuperclass()) != null);
    
    implementedInterfaces = (Class[])interfaces.toArray(new Class[interfaces.size()]);
    Class<?>[] oldValue = (Class[])implementedInterfacesCache.putIfAbsent(clazz, implementedInterfaces);
    if (oldValue != null) {
      implementedInterfaces = oldValue;
    }
    return implementedInterfaces;
  }
  







  public static long secondsSinceMillis(long timeInMillis)
  {
    return (System.currentTimeMillis() - timeInMillis) / 1000L;
  }
  





  public static int truncateAndConvertToInt(long longValue)
  {
    return longValue < -2147483648L ? Integer.MIN_VALUE : longValue > 2147483647L ? Integer.MAX_VALUE : (int)longValue;
  }
  





  public static int[] truncateAndConvertToInt(long[] longArray)
  {
    int[] intArray = new int[longArray.length];
    
    for (int i = 0; i < longArray.length; i++) {
      intArray[i] = (longArray[i] < -2147483648L ? Integer.MIN_VALUE : longArray[i] > 2147483647L ? Integer.MAX_VALUE : (int)longArray[i]);
    }
    return intArray;
  }
  







  public static String getPackageName(Class<?> clazz)
  {
    String fqcn = clazz.getName();
    int classNameStartsAt = fqcn.lastIndexOf('.');
    if (classNameStartsAt > 0) {
      return fqcn.substring(0, classNameStartsAt);
    }
    return "";
  }
  
  public Util() {}
  
  class RandStructcture
  {
    long maxValue;
    double maxValueDbl;
    long seed1;
    long seed2;
    
    RandStructcture() {}
  }
}
