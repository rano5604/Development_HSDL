package com.mysql.fabric.proto.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;






























public class ResultSetParser
{
  public ResultSetParser() {}
  
  public List<Map<String, ?>> parse(Map<String, ?> info, List<List<Object>> rows)
  {
    List<String> fieldNames = (List)info.get("names");
    Map<String, Integer> fieldNameIndexes = new HashMap();
    for (int i = 0; i < fieldNames.size(); i++) {
      fieldNameIndexes.put(fieldNames.get(i), Integer.valueOf(i));
    }
    
    List<Map<String, ?>> result = new ArrayList(rows.size());
    for (List<Object> r : rows) {
      Map<String, Object> resultRow = new HashMap();
      for (Map.Entry<String, Integer> f : fieldNameIndexes.entrySet()) {
        resultRow.put(f.getKey(), r.get(((Integer)f.getValue()).intValue()));
      }
      result.add(resultRow);
    }
    return result;
  }
}
