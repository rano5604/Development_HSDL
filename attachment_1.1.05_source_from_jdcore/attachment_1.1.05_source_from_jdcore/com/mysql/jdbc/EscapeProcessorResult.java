package com.mysql.jdbc;



























class EscapeProcessorResult
{
  boolean callingStoredFunction = false;
  
  String escapedSql;
  
  byte usesVariables = 0;
  
  EscapeProcessorResult() {}
}
