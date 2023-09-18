package com.mysql.fabric.proto.xmlrpc;

import com.mysql.fabric.FabricCommunicationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


























public class AuthenticatedXmlRpcMethodCaller
  implements XmlRpcMethodCaller
{
  private XmlRpcMethodCaller underlyingCaller;
  private String url;
  private String username;
  private String password;
  
  public AuthenticatedXmlRpcMethodCaller(XmlRpcMethodCaller underlyingCaller, String url, String username, String password)
  {
    this.underlyingCaller = underlyingCaller;
    this.url = url;
    this.username = username;
    this.password = password;
  }
  
  public void setHeader(String name, String value) {
    underlyingCaller.setHeader(name, value);
  }
  
  public void clearHeader(String name) {
    underlyingCaller.clearHeader(name);
  }
  
  public List<?> call(String methodName, Object[] args) throws FabricCommunicationException
  {
    String authenticateHeader;
    try {
      authenticateHeader = DigestAuthentication.getChallengeHeader(url);
    } catch (IOException ex) {
      throw new FabricCommunicationException("Unable to obtain challenge header for authentication", ex);
    }
    
    Map<String, String> digestChallenge = DigestAuthentication.parseDigestChallenge(authenticateHeader);
    
    String authorizationHeader = DigestAuthentication.generateAuthorizationHeader(digestChallenge, username, password);
    
    underlyingCaller.setHeader("Authorization", authorizationHeader);
    
    return underlyingCaller.call(methodName, args);
  }
}
