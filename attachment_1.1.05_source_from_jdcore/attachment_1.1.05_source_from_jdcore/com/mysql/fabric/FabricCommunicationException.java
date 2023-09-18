package com.mysql.fabric;













public class FabricCommunicationException
  extends Exception
{
  private static final long serialVersionUID = 1L;
  












  public FabricCommunicationException(Throwable cause)
  {
    super(cause);
  }
  
  public FabricCommunicationException(String message) {
    super(message);
  }
  
  public FabricCommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
