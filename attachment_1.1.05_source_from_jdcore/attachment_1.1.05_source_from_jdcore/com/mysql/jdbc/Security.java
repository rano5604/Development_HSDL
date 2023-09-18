package com.mysql.jdbc;

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;




























public class Security
{
  private static final char PVERSION41_CHAR = '*';
  private static final int SHA1_HASH_SIZE = 20;
  private static int CACHING_SHA2_DIGEST_LENGTH = 32;
  


  private static int charVal(char c)
  {
    return (c >= 'A') && (c <= 'Z') ? c - 'A' + 10 : (c >= '0') && (c <= '9') ? c - '0' : c - 'a' + 10;
  }
  























  static byte[] createKeyFromOldPassword(String passwd)
    throws NoSuchAlgorithmException
  {
    passwd = makeScrambledPassword(passwd);
    

    int[] salt = getSaltFromPassword(passwd);
    

    return getBinaryPassword(salt, false);
  }
  





  static byte[] getBinaryPassword(int[] salt, boolean usingNewPasswords)
    throws NoSuchAlgorithmException
  {
    int val = 0;
    
    byte[] binaryPassword = new byte[20];
    
    if (usingNewPasswords) {
      int pos = 0;
      
      for (int i = 0; i < 4; i++) {
        val = salt[i];
        
        for (int t = 3; t >= 0; t--) {
          binaryPassword[(pos++)] = ((byte)(val & 0xFF));
          val >>= 8;
        }
      }
      
      return binaryPassword;
    }
    
    int offset = 0;
    
    for (int i = 0; i < 2; i++) {
      val = salt[i];
      
      for (int t = 3; t >= 0; t--) {
        binaryPassword[(t + offset)] = ((byte)(val % 256));
        val >>= 8;
      }
      
      offset += 4;
    }
    
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    
    md.update(binaryPassword, 0, 8);
    
    return md.digest();
  }
  
  private static int[] getSaltFromPassword(String password) {
    int[] result = new int[6];
    
    if ((password == null) || (password.length() == 0)) {
      return result;
    }
    
    if (password.charAt(0) == '*')
    {
      String saltInHex = password.substring(1, 5);
      
      int val = 0;
      
      for (int i = 0; i < 4; i++) {
        val = (val << 4) + charVal(saltInHex.charAt(i));
      }
      
      return result;
    }
    
    int resultPos = 0;
    int pos = 0;
    int length = password.length();
    
    while (pos < length) {
      int val = 0;
      
      for (int i = 0; i < 8; i++) {
        val = (val << 4) + charVal(password.charAt(pos++));
      }
      
      result[(resultPos++)] = val;
    }
    
    return result;
  }
  
  private static String longToHex(long val) {
    String longHex = Long.toHexString(val);
    
    int length = longHex.length();
    
    if (length < 8) {
      int padding = 8 - length;
      StringBuilder buf = new StringBuilder();
      
      for (int i = 0; i < padding; i++) {
        buf.append("0");
      }
      
      buf.append(longHex);
      
      return buf.toString();
    }
    
    return longHex.substring(0, 8);
  }
  











  static String makeScrambledPassword(String password)
    throws NoSuchAlgorithmException
  {
    long[] passwordHash = Util.hashPre41Password(password);
    StringBuilder scramble = new StringBuilder();
    
    scramble.append(longToHex(passwordHash[0]));
    scramble.append(longToHex(passwordHash[1]));
    
    return scramble.toString();
  }
  













  public static void xorString(byte[] from, byte[] to, byte[] scramble, int length)
  {
    int pos = 0;
    int scrambleLength = scramble.length;
    
    while (pos < length) {
      to[pos] = ((byte)(from[pos] ^ scramble[(pos % scrambleLength)]));
      pos++;
    }
  }
  









  static byte[] passwordHashStage1(String password)
    throws NoSuchAlgorithmException
  {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    StringBuilder cleansedPassword = new StringBuilder();
    
    int passwordLength = password.length();
    
    for (int i = 0; i < passwordLength; i++) {
      char c = password.charAt(i);
      
      if ((c != ' ') && (c != '\t'))
      {


        cleansedPassword.append(c);
      }
    }
    return md.digest(StringUtils.getBytes(cleansedPassword.toString()));
  }
  











  static byte[] passwordHashStage2(byte[] hashedPassword, byte[] salt)
    throws NoSuchAlgorithmException
  {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    

    md.update(salt, 0, 4);
    
    md.update(hashedPassword, 0, 20);
    
    return md.digest();
  }
  















  public static byte[] scramble411(String password, String seed, String passwordEncoding)
    throws NoSuchAlgorithmException, UnsupportedEncodingException
  {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    
    byte[] passwordHashStage1 = md.digest((passwordEncoding == null) || (passwordEncoding.length() == 0) ? StringUtils.getBytes(password) : StringUtils.getBytes(password, passwordEncoding));
    
    md.reset();
    
    byte[] passwordHashStage2 = md.digest(passwordHashStage1);
    md.reset();
    
    byte[] seedAsBytes = StringUtils.getBytes(seed, "ASCII");
    md.update(seedAsBytes);
    md.update(passwordHashStage2);
    
    byte[] toBeXord = md.digest();
    
    int numToXor = toBeXord.length;
    
    for (int i = 0; i < numToXor; i++) {
      toBeXord[i] = ((byte)(toBeXord[i] ^ passwordHashStage1[i]));
    }
    
    return toBeXord;
  }
  







  public static byte[] scrambleCachingSha2(byte[] password, byte[] seed)
    throws DigestException
  {
    MessageDigest md;
    






    try
    {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionFailedException(ex);
    }
    
    byte[] dig1 = new byte[CACHING_SHA2_DIGEST_LENGTH];
    byte[] dig2 = new byte[CACHING_SHA2_DIGEST_LENGTH];
    byte[] scramble1 = new byte[CACHING_SHA2_DIGEST_LENGTH];
    

    md.update(password, 0, password.length);
    md.digest(dig1, 0, CACHING_SHA2_DIGEST_LENGTH);
    md.reset();
    

    md.update(dig1, 0, dig1.length);
    md.digest(dig2, 0, CACHING_SHA2_DIGEST_LENGTH);
    md.reset();
    

    md.update(dig2, 0, dig1.length);
    md.update(seed, 0, seed.length);
    md.digest(scramble1, 0, CACHING_SHA2_DIGEST_LENGTH);
    

    byte[] mysqlScrambleBuff = new byte[CACHING_SHA2_DIGEST_LENGTH];
    xorString(dig1, mysqlScrambleBuff, scramble1, CACHING_SHA2_DIGEST_LENGTH);
    
    return mysqlScrambleBuff;
  }
  
  private Security() {}
}
