package com.mysql.fabric;

import com.mysql.fabric.proto.xmlrpc.ResultSetParser;
import java.util.List;
import java.util.Map;



























public class Response
{
  private int protocolVersion;
  private String fabricUuid;
  private int ttl;
  private String errorMessage;
  private List<Map<String, ?>> resultSet;
  
  public Response(List<?> responseData)
    throws FabricCommunicationException
  {
    protocolVersion = ((Integer)responseData.get(0)).intValue();
    if (protocolVersion != 1) {
      throw new FabricCommunicationException("Unknown protocol version: " + protocolVersion);
    }
    fabricUuid = ((String)responseData.get(1));
    ttl = ((Integer)responseData.get(2)).intValue();
    errorMessage = ((String)responseData.get(3));
    if ("".equals(errorMessage)) {
      errorMessage = null;
    }
    List<Map<String, ?>> resultSets = (List)responseData.get(4);
    if (resultSets.size() > 0) {
      Map<String, ?> resultData = (Map)resultSets.get(0);
      resultSet = new ResultSetParser().parse((Map)resultData.get("info"), (List)resultData.get("rows"));
    }
  }
  
  public int getProtocolVersion() {
    return protocolVersion;
  }
  
  public String getFabricUuid() {
    return fabricUuid;
  }
  
  public int getTtl() {
    return ttl;
  }
  
  public String getErrorMessage() {
    return errorMessage;
  }
  
  public List<Map<String, ?>> getResultSet() {
    return resultSet;
  }
}
