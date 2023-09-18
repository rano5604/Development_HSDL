package com.mysql.fabric.xmlrpc.base;







public class MethodCall
{
  protected String methodName;
  





  protected Params params;
  






  public MethodCall() {}
  





  public String getMethodName()
  {
    return methodName;
  }
  


  public void setMethodName(String value)
  {
    methodName = value;
  }
  


  public Params getParams()
  {
    return params;
  }
  


  public void setParams(Params value)
  {
    params = value;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.append("<methodCall>");
    sb.append("\t<methodName>" + methodName + "</methodName>");
    if (params != null) {
      sb.append(params.toString());
    }
    sb.append("</methodCall>");
    return sb.toString();
  }
}
