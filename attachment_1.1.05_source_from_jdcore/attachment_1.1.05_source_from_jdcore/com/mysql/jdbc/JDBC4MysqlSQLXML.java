package com.mysql.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;






































public class JDBC4MysqlSQLXML
  implements SQLXML
{
  private XMLInputFactory inputFactory;
  private XMLOutputFactory outputFactory;
  private String stringRep;
  private ResultSetInternalMethods owningResultSet;
  private int columnIndexOfXml;
  private boolean fromResultSet;
  private boolean isClosed = false;
  
  private boolean workingWithResult;
  
  private DOMResult asDOMResult;
  
  private SAXResult asSAXResult;
  
  private SimpleSaxToReader saxToReaderConverter;
  
  private StringWriter asStringWriter;
  
  private ByteArrayOutputStream asByteArrayOutputStream;
  private ExceptionInterceptor exceptionInterceptor;
  
  protected JDBC4MysqlSQLXML(ResultSetInternalMethods owner, int index, ExceptionInterceptor exceptionInterceptor)
  {
    owningResultSet = owner;
    columnIndexOfXml = index;
    fromResultSet = true;
    this.exceptionInterceptor = exceptionInterceptor;
  }
  
  protected JDBC4MysqlSQLXML(ExceptionInterceptor exceptionInterceptor) {
    fromResultSet = false;
    this.exceptionInterceptor = exceptionInterceptor;
  }
  
  public synchronized void free() throws SQLException {
    stringRep = null;
    asDOMResult = null;
    asSAXResult = null;
    inputFactory = null;
    outputFactory = null;
    owningResultSet = null;
    workingWithResult = false;
    isClosed = true;
  }
  
  public synchronized String getString() throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    if (fromResultSet) {
      return owningResultSet.getString(columnIndexOfXml);
    }
    
    return stringRep;
  }
  
  private synchronized void checkClosed() throws SQLException {
    if (isClosed) {
      throw SQLError.createSQLException("SQLXMLInstance has been free()d", exceptionInterceptor);
    }
  }
  
  private synchronized void checkWorkingWithResult() throws SQLException {
    if (workingWithResult) {
      throw SQLError.createSQLException("Can't perform requested operation after getResult() has been called to write XML data", "S1009", exceptionInterceptor);
    }
  }
  
























  public synchronized void setString(String str)
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    stringRep = str;
    fromResultSet = false;
  }
  
  public synchronized boolean isEmpty() throws SQLException {
    checkClosed();
    checkWorkingWithResult();
    
    if (!fromResultSet) {
      return (stringRep == null) || (stringRep.length() == 0);
    }
    
    return false;
  }
  
  public synchronized InputStream getBinaryStream() throws SQLException {
    checkClosed();
    checkWorkingWithResult();
    
    return owningResultSet.getBinaryStream(columnIndexOfXml);
  }
  






















  public synchronized Reader getCharacterStream()
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    return owningResultSet.getCharacterStream(columnIndexOfXml);
  }
  











































  public synchronized <T extends Source> T getSource(Class<T> clazz)
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    



    if ((clazz == null) || (clazz.equals(SAXSource.class)))
    {
      InputSource inputSource = null;
      
      if (fromResultSet) {
        inputSource = new InputSource(owningResultSet.getCharacterStream(columnIndexOfXml));
      } else {
        inputSource = new InputSource(new StringReader(stringRep));
      }
      
      return new SAXSource(inputSource); }
    if (clazz.equals(DOMSource.class)) {
      try {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        InputSource inputSource = null;
        
        if (fromResultSet) {
          inputSource = new InputSource(owningResultSet.getCharacterStream(columnIndexOfXml));
        } else {
          inputSource = new InputSource(new StringReader(stringRep));
        }
        
        return new DOMSource(builder.parse(inputSource));
      } catch (Throwable t) {
        SQLException sqlEx = SQLError.createSQLException(t.getMessage(), "S1009", exceptionInterceptor);
        sqlEx.initCause(t);
        
        throw sqlEx;
      }
    }
    if (clazz.equals(StreamSource.class)) {
      Reader reader = null;
      
      if (fromResultSet) {
        reader = owningResultSet.getCharacterStream(columnIndexOfXml);
      } else {
        reader = new StringReader(stringRep);
      }
      
      return new StreamSource(reader); }
    if (clazz.equals(StAXSource.class)) {
      try {
        Reader reader = null;
        
        if (fromResultSet) {
          reader = owningResultSet.getCharacterStream(columnIndexOfXml);
        } else {
          reader = new StringReader(stringRep);
        }
        
        return new StAXSource(inputFactory.createXMLStreamReader(reader));
      } catch (XMLStreamException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.getMessage(), "S1009", exceptionInterceptor);
        sqlEx.initCause(ex);
        
        throw sqlEx;
      }
    }
    throw SQLError.createSQLException("XML Source of type \"" + clazz.toString() + "\" Not supported.", "S1009", exceptionInterceptor);
  }
  


















  public synchronized OutputStream setBinaryStream()
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    workingWithResult = true;
    
    return setBinaryStreamInternal();
  }
  
  private synchronized OutputStream setBinaryStreamInternal() throws SQLException {
    asByteArrayOutputStream = new ByteArrayOutputStream();
    
    return asByteArrayOutputStream;
  }
  






















  public synchronized Writer setCharacterStream()
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    workingWithResult = true;
    
    return setCharacterStreamInternal();
  }
  
  private synchronized Writer setCharacterStreamInternal() throws SQLException {
    asStringWriter = new StringWriter();
    
    return asStringWriter;
  }
  









































  public synchronized <T extends Result> T setResult(Class<T> clazz)
    throws SQLException
  {
    checkClosed();
    checkWorkingWithResult();
    
    workingWithResult = true;
    asDOMResult = null;
    asSAXResult = null;
    saxToReaderConverter = null;
    stringRep = null;
    asStringWriter = null;
    asByteArrayOutputStream = null;
    
    if ((clazz == null) || (clazz.equals(SAXResult.class))) {
      saxToReaderConverter = new SimpleSaxToReader();
      
      asSAXResult = new SAXResult(saxToReaderConverter);
      
      return asSAXResult; }
    if (clazz.equals(DOMResult.class))
    {
      asDOMResult = new DOMResult();
      return asDOMResult;
    }
    if (clazz.equals(StreamResult.class))
      return new StreamResult(setCharacterStreamInternal());
    if (clazz.equals(StAXResult.class)) {
      try {
        if (outputFactory == null) {
          outputFactory = XMLOutputFactory.newInstance();
        }
        
        return new StAXResult(outputFactory.createXMLEventWriter(setCharacterStreamInternal()));
      } catch (XMLStreamException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.getMessage(), "S1009", exceptionInterceptor);
        sqlEx.initCause(ex);
        
        throw sqlEx;
      }
    }
    throw SQLError.createSQLException("XML Result of type \"" + clazz.toString() + "\" Not supported.", "S1009", exceptionInterceptor);
  }
  




  private Reader binaryInputStreamStreamToReader(ByteArrayOutputStream out)
  {
    try
    {
      String encoding = "UTF-8";
      try
      {
        ByteArrayInputStream bIn = new ByteArrayInputStream(out.toByteArray());
        XMLStreamReader reader = inputFactory.createXMLStreamReader(bIn);
        
        int eventType = 0;
        
        while ((eventType = reader.next()) != 8) {
          if (eventType == 7) {
            String possibleEncoding = reader.getEncoding();
            
            if (possibleEncoding != null) {
              encoding = possibleEncoding;
            }
          }
        }
      }
      catch (Throwable localThrowable) {}
      



      return new StringReader(new String(out.toByteArray(), encoding));
    } catch (UnsupportedEncodingException badEnc) {
      throw new RuntimeException(badEnc);
    }
  }
  
  protected String readerToString(Reader reader) throws SQLException {
    StringBuilder buf = new StringBuilder();
    
    int charsRead = 0;
    
    char[] charBuf = new char['Ȁ'];
    try
    {
      while ((charsRead = reader.read(charBuf)) != -1) {
        buf.append(charBuf, 0, charsRead);
      }
    } catch (IOException ioEx) {
      SQLException sqlEx = SQLError.createSQLException(ioEx.getMessage(), "S1009", exceptionInterceptor);
      sqlEx.initCause(ioEx);
      
      throw sqlEx;
    }
    
    return buf.toString();
  }
  
  protected synchronized Reader serializeAsCharacterStream() throws SQLException {
    checkClosed();
    if (workingWithResult)
    {
      if (stringRep != null) {
        return new StringReader(stringRep);
      }
      
      if (asDOMResult != null) {
        return new StringReader(domSourceToString());
      }
      
      if (asStringWriter != null) {
        return new StringReader(asStringWriter.toString());
      }
      
      if (asSAXResult != null) {
        return saxToReaderConverter.toReader();
      }
      
      if (asByteArrayOutputStream != null) {
        return binaryInputStreamStreamToReader(asByteArrayOutputStream);
      }
    }
    
    return owningResultSet.getCharacterStream(columnIndexOfXml);
  }
  
  protected String domSourceToString() throws SQLException {
    try {
      DOMSource source = new DOMSource(asDOMResult.getNode());
      Transformer identity = TransformerFactory.newInstance().newTransformer();
      StringWriter stringOut = new StringWriter();
      Result result = new StreamResult(stringOut);
      identity.transform(source, result);
      
      return stringOut.toString();
    } catch (Throwable t) {
      SQLException sqlEx = SQLError.createSQLException(t.getMessage(), "S1009", exceptionInterceptor);
      sqlEx.initCause(t);
      
      throw sqlEx;
    }
  }
  
  protected synchronized String serializeAsString() throws SQLException {
    checkClosed();
    if (workingWithResult)
    {
      if (stringRep != null) {
        return stringRep;
      }
      
      if (asDOMResult != null) {
        return domSourceToString();
      }
      
      if (asStringWriter != null) {
        return asStringWriter.toString();
      }
      
      if (asSAXResult != null) {
        return readerToString(saxToReaderConverter.toReader());
      }
      
      if (asByteArrayOutputStream != null) {
        return readerToString(binaryInputStreamStreamToReader(asByteArrayOutputStream));
      }
    }
    
    return owningResultSet.getString(columnIndexOfXml);
  }
  









  class SimpleSaxToReader
    extends DefaultHandler
  {
    SimpleSaxToReader() {}
    








    StringBuilder buf = new StringBuilder();
    
    public void startDocument() throws SAXException {
      buf.append("<?xml version='1.0' encoding='UTF-8'?>"); }
    
    public void endDocument()
      throws SAXException
    {}
    
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
      throws SAXException
    {
      buf.append("<");
      buf.append(qName);
      
      if (attrs != null) {
        for (int i = 0; i < attrs.getLength(); i++) {
          buf.append(" ");
          buf.append(attrs.getQName(i)).append("=\"");
          escapeCharsForXml(attrs.getValue(i), true);
          buf.append("\"");
        }
      }
      
      buf.append(">");
    }
    
    public void characters(char[] buf, int offset, int len) throws SAXException {
      if (!inCDATA) {
        escapeCharsForXml(buf, offset, len, false);
      } else {
        this.buf.append(buf, offset, len);
      }
    }
    
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      characters(ch, start, length);
    }
    
    private boolean inCDATA = false;
    
    public void startCDATA() throws SAXException {
      buf.append("<![CDATA[");
      inCDATA = true;
    }
    
    public void endCDATA() throws SAXException {
      inCDATA = false;
      buf.append("]]>");
    }
    
    public void comment(char[] ch, int start, int length) throws SAXException
    {
      buf.append("<!--");
      for (int i = 0; i < length; i++) {
        buf.append(ch[(start + i)]);
      }
      buf.append("-->");
    }
    
    Reader toReader()
    {
      return new StringReader(buf.toString());
    }
    
    private void escapeCharsForXml(String str, boolean isAttributeData) {
      if (str == null) {
        return;
      }
      
      int strLen = str.length();
      
      for (int i = 0; i < strLen; i++) {
        escapeCharsForXml(str.charAt(i), isAttributeData);
      }
    }
    
    private void escapeCharsForXml(char[] buf, int offset, int len, boolean isAttributeData)
    {
      if (buf == null) {
        return;
      }
      
      for (int i = 0; i < len; i++) {
        escapeCharsForXml(buf[(offset + i)], isAttributeData);
      }
    }
    
    private void escapeCharsForXml(char c, boolean isAttributeData) {
      switch (c) {
      case '<': 
        buf.append("&lt;");
        break;
      
      case '>': 
        buf.append("&gt;");
        break;
      
      case '&': 
        buf.append("&amp;");
        break;
      

      case '"': 
        if (!isAttributeData) {
          buf.append("\"");
        } else {
          buf.append("&quot;");
        }
        
        break;
      
      case '\r': 
        buf.append("&#xD;");
        break;
      

      default: 
        if (((c >= '\001') && (c <= '\037') && (c != '\t') && (c != '\n')) || ((c >= '') && (c <= '')) || (c == ' ') || ((isAttributeData) && ((c == '\t') || (c == '\n'))))
        {
          buf.append("&#x");
          buf.append(Integer.toHexString(c).toUpperCase());
          buf.append(";");
        } else {
          buf.append(c);
        }
        break;
      }
    }
  }
}
