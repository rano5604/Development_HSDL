package com.mysql.jdbc;

import java.sql.NClob;
























public class JDBC4NClob
  extends Clob
  implements NClob
{
  JDBC4NClob(ExceptionInterceptor exceptionInterceptor)
  {
    super(exceptionInterceptor);
  }
  
  JDBC4NClob(String charDataInit, ExceptionInterceptor exceptionInterceptor) {
    super(charDataInit, exceptionInterceptor);
  }
}
