package com.mysql.fabric.xmlrpc.base;

import java.util.Arrays;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


























public class Value
{
  public static final byte TYPE_i4 = 0;
  public static final byte TYPE_int = 1;
  public static final byte TYPE_boolean = 2;
  public static final byte TYPE_string = 3;
  public static final byte TYPE_double = 4;
  public static final byte TYPE_dateTime_iso8601 = 5;
  public static final byte TYPE_base64 = 6;
  public static final byte TYPE_struct = 7;
  public static final byte TYPE_array = 8;
  protected Object objValue = "";
  protected byte objType = 3;
  private DatatypeFactory dtf = null;
  
  public Value() {}
  
  public Value(int value)
  {
    setInt(value);
  }
  
  public Value(String value) {
    setString(value);
  }
  
  public Value(boolean value) {
    setBoolean(value);
  }
  
  public Value(double value) {
    setDouble(value);
  }
  
  public Value(GregorianCalendar value) throws DatatypeConfigurationException {
    setDateTime(value);
  }
  
  public Value(byte[] value) {
    setBase64(value);
  }
  
  public Value(Struct value) {
    setStruct(value);
  }
  
  public Value(Array value) {
    setArray(value);
  }
  
  public Object getValue() {
    return objValue;
  }
  
  public byte getType() {
    return objType;
  }
  
  public void setInt(int value) {
    objValue = Integer.valueOf(value);
    objType = 1;
  }
  
  public void setInt(String value) {
    objValue = Integer.valueOf(value);
    objType = 1;
  }
  
  public void setString(String value) {
    objValue = value;
    objType = 3;
  }
  
  public void appendString(String value) {
    objValue = (objValue != null ? objValue + value : value);
    objType = 3;
  }
  
  public void setBoolean(boolean value) {
    objValue = Boolean.valueOf(value);
    objType = 2;
  }
  
  public void setBoolean(String value) {
    if ((value.trim().equals("1")) || (value.trim().equalsIgnoreCase("true"))) {
      objValue = Boolean.valueOf(true);
    } else {
      objValue = Boolean.valueOf(false);
    }
    objType = 2;
  }
  
  public void setDouble(double value) {
    objValue = Double.valueOf(value);
    objType = 4;
  }
  
  public void setDouble(String value) {
    objValue = Double.valueOf(value);
    objType = 4;
  }
  
  public void setDateTime(GregorianCalendar value) throws DatatypeConfigurationException {
    if (dtf == null) {
      dtf = DatatypeFactory.newInstance();
    }
    objValue = dtf.newXMLGregorianCalendar(value);
    objType = 5;
  }
  
  public void setDateTime(String value) throws DatatypeConfigurationException {
    if (dtf == null) {
      dtf = DatatypeFactory.newInstance();
    }
    objValue = dtf.newXMLGregorianCalendar(value);
    objType = 5;
  }
  
  public void setBase64(byte[] value) {
    objValue = value;
    objType = 6;
  }
  
  public void setStruct(Struct value) {
    objValue = value;
    objType = 7;
  }
  
  public void setArray(Array value) {
    objValue = value;
    objType = 8;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("<value>");
    switch (objType) {
    case 0: 
      sb.append("<i4>" + ((Integer)objValue).toString() + "</i4>");
      break;
    case 1: 
      sb.append("<int>" + ((Integer)objValue).toString() + "</int>");
      break;
    
    case 2: 
      sb.append("<boolean>" + (((Boolean)objValue).booleanValue() ? 1 : 0) + "</boolean>");
      break;
    
    case 4: 
      sb.append("<double>" + ((Double)objValue).toString() + "</double>");
      break;
    
    case 5: 
      sb.append("<dateTime.iso8601>" + ((XMLGregorianCalendar)objValue).toString() + "</dateTime.iso8601>");
      break;
    
    case 6: 
      sb.append("<base64>" + Arrays.toString((byte[])objValue) + "</base64>");
      break;
    
    case 7: 
      sb.append(((Struct)objValue).toString());
      break;
    
    case 8: 
      sb.append(((Array)objValue).toString());
      break;
    case 3: 
    default: 
      sb.append("<string>" + escapeXMLChars(objValue.toString()) + "</string>");
    }
    sb.append("</value>");
    return sb.toString();
  }
  
  private String escapeXMLChars(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '&': 
        sb.append("&amp;");
        break;
      case '<': 
        sb.append("&lt;");
        break;
      case '>': 
        sb.append("&gt;");
        break;
      default: 
        sb.append(c);
      }
      
    }
    return sb.toString();
  }
}
