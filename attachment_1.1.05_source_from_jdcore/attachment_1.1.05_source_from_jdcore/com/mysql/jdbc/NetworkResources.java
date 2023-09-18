package com.mysql.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;






















class NetworkResources
{
  private final Socket mysqlConnection;
  private final InputStream mysqlInput;
  private final OutputStream mysqlOutput;
  
  protected NetworkResources(Socket mysqlConnection, InputStream mysqlInput, OutputStream mysqlOutput)
  {
    this.mysqlConnection = mysqlConnection;
    this.mysqlInput = mysqlInput;
    this.mysqlOutput = mysqlOutput;
  }
  
  protected final void forceClose()
  {
    try
    {
      try
      {
        if (mysqlInput != null) {
          mysqlInput.close();
        }
      } finally {
        if ((mysqlConnection != null) && (!mysqlConnection.isClosed()) && (!mysqlConnection.isInputShutdown())) {
          try {
            mysqlConnection.shutdownInput();
          }
          catch (UnsupportedOperationException ex) {}
        }
      }
    }
    catch (IOException ioEx) {}
    
    try
    {
      try
      {
        if (mysqlOutput != null) {
          mysqlOutput.close();
        }
      } finally {
        if ((mysqlConnection != null) && (!mysqlConnection.isClosed()) && (!mysqlConnection.isOutputShutdown())) {
          try {
            mysqlConnection.shutdownOutput();
          }
          catch (UnsupportedOperationException ex) {}
        }
      }
    }
    catch (IOException ioEx) {}
    

    try
    {
      if (mysqlConnection != null) {
        mysqlConnection.close();
      }
    }
    catch (IOException ioEx) {}
  }
}
