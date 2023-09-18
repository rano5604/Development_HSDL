package com.mysql.jdbc;




public class EscapeTokenizer
{
  private static final char CHR_ESCAPE = '\\';
  


  private static final char CHR_SGL_QUOTE = '\'';
  


  private static final char CHR_DBL_QUOTE = '"';
  


  private static final char CHR_LF = '\n';
  


  private static final char CHR_CR = '\r';
  


  private static final char CHR_COMMENT = '-';
  

  private static final char CHR_BEGIN_TOKEN = '{';
  

  private static final char CHR_END_TOKEN = '}';
  

  private static final char CHR_VARIABLE = '@';
  

  private String source = null;
  private int sourceLength = 0;
  private int pos = 0;
  
  private boolean emittingEscapeCode = false;
  private boolean sawVariableUse = false;
  private int bracesLevel = 0;
  private boolean inQuotes = false;
  private char quoteChar = '\000';
  





  public EscapeTokenizer(String source)
  {
    this.source = source;
    sourceLength = source.length();
    pos = 0;
  }
  




  public synchronized boolean hasMoreTokens()
  {
    return pos < sourceLength;
  }
  




  public synchronized String nextToken()
  {
    StringBuilder tokenBuf = new StringBuilder();
    boolean backslashEscape = false;
    
    if (emittingEscapeCode)
    {
      tokenBuf.append("{");
      emittingEscapeCode = false;
    }
    for (; 
        pos < sourceLength; pos += 1) {
      char c = source.charAt(pos);
      

      if (c == '\\') {
        tokenBuf.append(c);
        backslashEscape = !backslashEscape;



      }
      else if (((c == '\'') || (c == '"')) && (!backslashEscape)) {
        tokenBuf.append(c);
        if (inQuotes) {
          if (c == quoteChar)
          {
            if ((pos + 1 < sourceLength) && (source.charAt(pos + 1) == quoteChar)) {
              tokenBuf.append(c);
              pos += 1;
            } else {
              inQuotes = false;
            }
          }
        } else {
          inQuotes = true;
          quoteChar = c;

        }
        

      }
      else if ((c == '\n') || (c == '\r')) {
        tokenBuf.append(c);
        backslashEscape = false;
      }
      else
      {
        if ((!inQuotes) && (!backslashEscape))
        {
          if (c == '-') {
            tokenBuf.append(c);
            
            if ((pos + 1 >= sourceLength) || (source.charAt(pos + 1) != '-'))
              continue;
            while ((++pos < sourceLength) && (c != '\n') && (c != '\r')) {
              c = source.charAt(pos);
              tokenBuf.append(c);
            }
            pos -= 1; continue;
          }
          



          if (c == '{') {
            bracesLevel += 1;
            if (bracesLevel == 1) {
              emittingEscapeCode = true;
              pos += 1;
              return tokenBuf.toString();
            }
            tokenBuf.append(c);
            continue;
          }
          

          if (c == '}') {
            tokenBuf.append(c);
            bracesLevel -= 1;
            if (bracesLevel != 0) continue;
            pos += 1;
            return tokenBuf.toString();
          }
          



          if (c == '@') {
            sawVariableUse = true;
          }
        }
        
        tokenBuf.append(c);
        backslashEscape = false;
      }
    }
    return tokenBuf.toString();
  }
  





  boolean sawVariableUse()
  {
    return sawVariableUse;
  }
}
