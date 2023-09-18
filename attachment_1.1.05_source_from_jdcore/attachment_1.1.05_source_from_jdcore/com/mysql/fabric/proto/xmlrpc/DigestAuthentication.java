package com.mysql.fabric.proto.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
























public class DigestAuthentication
{
  private static Random random = new Random();
  
  public DigestAuthentication() {}
  
  public static String getChallengeHeader(String url)
    throws IOException
  {
    HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
    conn.setDoOutput(true);
    conn.getOutputStream().close();
    try {
      conn.getInputStream().close();
    } catch (IOException ex) {
      if (401 == conn.getResponseCode())
      {


        String hdr = conn.getHeaderField("WWW-Authenticate");
        if ((hdr != null) && (!"".equals(hdr)))
          return hdr;
      } else {
        if (400 == conn.getResponseCode())
        {
          throw new IOException("Fabric returns status 400. If authentication is disabled on the Fabric node, omit the `fabricUsername' and `fabricPassword' properties from your connection.");
        }
        
        throw ex;
      }
    }
    return null;
  }
  



  public static String calculateMD5RequestDigest(String uri, String username, String password, String realm, String nonce, String nc, String cnonce, String qop)
  {
    String reqA1 = username + ":" + realm + ":" + password;
    
    String reqA2 = "POST:" + uri;
    
    String hashA1 = checksumMD5(reqA1);
    String hashA2 = checksumMD5(reqA2);
    String requestDigest = digestMD5(hashA1, nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + hashA2);
    
    return requestDigest;
  }
  


  private static String checksumMD5(String data)
  {
    MessageDigest md5 = null;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("Unable to create MD5 instance", ex);
    }
    
    return hexEncode(md5.digest(data.getBytes()));
  }
  


  private static String digestMD5(String secret, String data)
  {
    return checksumMD5(secret + ":" + data);
  }
  


  private static String hexEncode(byte[] data)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < data.length; i++) {
      sb.append(String.format("%02x", new Object[] { Byte.valueOf(data[i]) }));
    }
    return sb.toString();
  }
  




  public static String serializeDigestResponse(Map<String, String> paramMap)
  {
    StringBuilder sb = new StringBuilder("Digest ");
    
    boolean prefixComma = false;
    for (Map.Entry<String, String> entry : paramMap.entrySet()) {
      if (!prefixComma) {
        prefixComma = true;
      } else {
        sb.append(", ");
      }
      sb.append((String)entry.getKey());
      sb.append("=");
      sb.append((String)entry.getValue());
    }
    
    return sb.toString();
  }
  




  public static Map<String, String> parseDigestChallenge(String headerValue)
  {
    if (!headerValue.startsWith("Digest ")) {
      throw new IllegalArgumentException("Header is not a digest challenge");
    }
    
    String params = headerValue.substring(7);
    Map<String, String> paramMap = new HashMap();
    for (String param : params.split(",\\s*")) {
      String[] pieces = param.split("=");
      paramMap.put(pieces[0], pieces[1].replaceAll("^\"(.*)\"$", "$1"));
    }
    return paramMap;
  }
  






  public static String generateCnonce(String nonce, String nc)
  {
    byte[] buf = new byte[8];
    random.nextBytes(buf);
    for (int i = 0; i < 8; i++) {
      buf[i] = ((byte)(32 + buf[i] % 95));
    }
    
    String combo = String.format("%s:%s:%s:%s", new Object[] { nonce, nc, new Date().toGMTString(), new String(buf) });
    MessageDigest sha1 = null;
    try {
      sha1 = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("Unable to create SHA-1 instance", ex);
    }
    
    return hexEncode(sha1.digest(combo.getBytes()));
  }
  



  private static String quoteParam(String param)
  {
    if ((param.contains("\"")) || (param.contains("'"))) {
      throw new IllegalArgumentException("Invalid character in parameter");
    }
    return "\"" + param + "\"";
  }
  



  public static String generateAuthorizationHeader(Map<String, String> digestChallenge, String username, String password)
  {
    String nonce = (String)digestChallenge.get("nonce");
    String nc = "00000001";
    String cnonce = generateCnonce(nonce, nc);
    String qop = "auth";
    String uri = "/RPC2";
    String realm = (String)digestChallenge.get("realm");
    String opaque = (String)digestChallenge.get("opaque");
    
    String requestDigest = calculateMD5RequestDigest(uri, username, password, realm, nonce, nc, cnonce, qop);
    Map<String, String> digestResponseMap = new HashMap();
    digestResponseMap.put("algorithm", "MD5");
    digestResponseMap.put("username", quoteParam(username));
    digestResponseMap.put("realm", quoteParam(realm));
    digestResponseMap.put("nonce", quoteParam(nonce));
    digestResponseMap.put("uri", quoteParam(uri));
    digestResponseMap.put("qop", qop);
    digestResponseMap.put("nc", nc);
    digestResponseMap.put("cnonce", quoteParam(cnonce));
    digestResponseMap.put("response", quoteParam(requestDigest));
    digestResponseMap.put("opaque", quoteParam(opaque));
    
    return serializeDigestResponse(digestResponseMap);
  }
}
