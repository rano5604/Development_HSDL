package com.mysql.fabric.xmlrpc.base;







public class MethodResponse
{
  protected Params params;
  





  protected Fault fault;
  






  public MethodResponse() {}
  





  public Params getParams()
  {
    return params;
  }
  


  public void setParams(Params value)
  {
    params = value;
  }
  


  public Fault getFault()
  {
    return fault;
  }
  


  public void setFault(Fault value)
  {
    fault = value;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.append("<methodResponse>");
    if (params != null) {
      sb.append(params.toString());
    }
    if (fault != null) {
      sb.append(fault.toString());
    }
    sb.append("</methodResponse>");
    return sb.toString();
  }
}
