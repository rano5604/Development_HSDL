package com.mysql.fabric.proto.xmlrpc;

import com.mysql.fabric.FabricCommunicationException;
import java.util.List;

public abstract interface XmlRpcMethodCaller
{
  public abstract List<?> call(String paramString, Object[] paramArrayOfObject)
    throws FabricCommunicationException;
  
  public abstract void setHeader(String paramString1, String paramString2);
  
  public abstract void clearHeader(String paramString);
}
