package com.mysql.fabric.proto.xmlrpc;

import com.mysql.fabric.FabricCommunicationException;
import com.mysql.fabric.xmlrpc.Client;
import com.mysql.fabric.xmlrpc.base.Array;
import com.mysql.fabric.xmlrpc.base.Data;
import com.mysql.fabric.xmlrpc.base.Member;
import com.mysql.fabric.xmlrpc.base.MethodCall;
import com.mysql.fabric.xmlrpc.base.MethodResponse;
import com.mysql.fabric.xmlrpc.base.Param;
import com.mysql.fabric.xmlrpc.base.Params;
import com.mysql.fabric.xmlrpc.base.Struct;
import com.mysql.fabric.xmlrpc.base.Value;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;























public class InternalXmlRpcMethodCaller
  implements XmlRpcMethodCaller
{
  private Client xmlRpcClient;
  
  public InternalXmlRpcMethodCaller(String url)
    throws FabricCommunicationException
  {
    try
    {
      xmlRpcClient = new Client(url);
    } catch (MalformedURLException ex) {
      throw new FabricCommunicationException(ex);
    }
  }
  


  private Object unwrapValue(Value v)
  {
    if (v.getType() == 8)
      return methodResponseArrayToList((Array)v.getValue());
    if (v.getType() == 7) {
      Map<String, Object> s = new HashMap();
      for (Member m : ((Struct)v.getValue()).getMember()) {
        s.put(m.getName(), unwrapValue(m.getValue()));
      }
      return s;
    }
    return v.getValue();
  }
  
  private List<Object> methodResponseArrayToList(Array array) {
    List<Object> result = new ArrayList();
    for (Value v : array.getData().getValue()) {
      result.add(unwrapValue(v));
    }
    return result;
  }
  
  public void setHeader(String name, String value) {
    xmlRpcClient.setHeader(name, value);
  }
  
  public void clearHeader(String name) {
    xmlRpcClient.clearHeader(name);
  }
  
  public List<Object> call(String methodName, Object[] args) throws FabricCommunicationException {
    MethodCall methodCall = new MethodCall();
    Params p = new Params();
    if (args == null) {
      args = new Object[0];
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null)
        throw new NullPointerException("nil args unsupported");
      if (String.class.isAssignableFrom(args[i].getClass())) {
        p.addParam(new Param(new Value((String)args[i])));
      } else if (Double.class.isAssignableFrom(args[i].getClass())) {
        p.addParam(new Param(new Value(((Double)args[i]).doubleValue())));
      } else if (Integer.class.isAssignableFrom(args[i].getClass())) {
        p.addParam(new Param(new Value(((Integer)args[i]).intValue())));
      } else {
        throw new IllegalArgumentException("Unknown argument type: " + args[i].getClass());
      }
    }
    methodCall.setMethodName(methodName);
    methodCall.setParams(p);
    try {
      MethodResponse resp = xmlRpcClient.execute(methodCall);
      return methodResponseArrayToList((Array)((Param)resp.getParams().getParam().get(0)).getValue().getValue());
    } catch (Exception ex) {
      throw new FabricCommunicationException("Error during call to `" + methodName + "' (args=" + Arrays.toString(args) + ")", ex);
    }
  }
}
