package com.mysql.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Properties;






















public class NamedPipeSocketFactory
  implements SocketFactory, SocketMetadata
{
  public static final String NAMED_PIPE_PROP_NAME = "namedPipePath";
  private Socket namedPipeSocket;
  public NamedPipeSocketFactory() {}
  
  class NamedPipeSocket
    extends Socket
  {
    private boolean isClosed = false;
    private RandomAccessFile namedPipeFile;
    
    NamedPipeSocket(String filePath) throws IOException
    {
      if ((filePath == null) || (filePath.length() == 0)) {
        throw new IOException(Messages.getString("NamedPipeSocketFactory.4"));
      }
      
      namedPipeFile = new RandomAccessFile(filePath, "rw");
    }
    


    public synchronized void close()
      throws IOException
    {
      namedPipeFile.close();
      isClosed = true;
    }
    


    public InputStream getInputStream()
      throws IOException
    {
      return new NamedPipeSocketFactory.RandomAccessFileInputStream(NamedPipeSocketFactory.this, namedPipeFile);
    }
    


    public OutputStream getOutputStream()
      throws IOException
    {
      return new NamedPipeSocketFactory.RandomAccessFileOutputStream(NamedPipeSocketFactory.this, namedPipeFile);
    }
    



    public boolean isClosed()
    {
      return isClosed;
    }
  }
  
  class RandomAccessFileInputStream
    extends InputStream
  {
    RandomAccessFile raFile;
    
    RandomAccessFileInputStream(RandomAccessFile file)
    {
      raFile = file;
    }
    


    public int available()
      throws IOException
    {
      return -1;
    }
    


    public void close()
      throws IOException
    {
      raFile.close();
    }
    


    public int read()
      throws IOException
    {
      return raFile.read();
    }
    


    public int read(byte[] b)
      throws IOException
    {
      return raFile.read(b);
    }
    


    public int read(byte[] b, int off, int len)
      throws IOException
    {
      return raFile.read(b, off, len);
    }
  }
  
  class RandomAccessFileOutputStream
    extends OutputStream
  {
    RandomAccessFile raFile;
    
    RandomAccessFileOutputStream(RandomAccessFile file)
    {
      raFile = file;
    }
    


    public void close()
      throws IOException
    {
      raFile.close();
    }
    


    public void write(byte[] b)
      throws IOException
    {
      raFile.write(b);
    }
    


    public void write(byte[] b, int off, int len)
      throws IOException
    {
      raFile.write(b, off, len);
    }
    








    public void write(int b)
      throws IOException
    {}
  }
  







  public Socket afterHandshake()
    throws SocketException, IOException
  {
    return namedPipeSocket;
  }
  

  public Socket beforeHandshake()
    throws SocketException, IOException
  {
    return namedPipeSocket;
  }
  

  public Socket connect(String host, int portNumber, Properties props)
    throws SocketException, IOException
  {
    String namedPipePath = props.getProperty("namedPipePath");
    
    if (namedPipePath == null) {
      namedPipePath = "\\\\.\\pipe\\MySQL";
    } else if (namedPipePath.length() == 0) {
      throw new SocketException(Messages.getString("NamedPipeSocketFactory.2") + "namedPipePath" + Messages.getString("NamedPipeSocketFactory.3"));
    }
    
    namedPipeSocket = new NamedPipeSocket(namedPipePath);
    
    return namedPipeSocket;
  }
  
  public boolean isLocallyConnected(ConnectionImpl conn) throws SQLException
  {
    return true;
  }
}
