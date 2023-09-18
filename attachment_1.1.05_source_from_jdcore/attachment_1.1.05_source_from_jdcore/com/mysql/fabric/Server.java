package com.mysql.fabric;




public class Server
  implements Comparable<Server>
{
  private String groupName;
  


  private String uuid;
  


  private String hostname;
  


  private int port;
  


  private ServerMode mode;
  


  private ServerRole role;
  


  private double weight;
  


  public Server(String groupName, String uuid, String hostname, int port, ServerMode mode, ServerRole role, double weight)
  {
    this.groupName = groupName;
    this.uuid = uuid;
    this.hostname = hostname;
    this.port = port;
    this.mode = mode;
    this.role = role;
    this.weight = weight;
    assert ((uuid != null) && (!"".equals(uuid)));
    assert ((hostname != null) && (!"".equals(hostname)));
    assert (port > 0);
    assert (mode != null);
    assert (role != null);
    assert (weight > 0.0D);
  }
  
  public String getGroupName() {
    return groupName;
  }
  
  public String getUuid() {
    return uuid;
  }
  
  public String getHostname() {
    return hostname;
  }
  
  public int getPort() {
    return port;
  }
  
  public ServerMode getMode() {
    return mode;
  }
  
  public ServerRole getRole() {
    return role;
  }
  
  public double getWeight() {
    return weight;
  }
  
  public String getHostPortString() {
    return hostname + ":" + port;
  }
  
  public boolean isMaster() {
    return role == ServerRole.PRIMARY;
  }
  
  public boolean isSlave() {
    return (role == ServerRole.SECONDARY) || (role == ServerRole.SPARE);
  }
  
  public String toString()
  {
    return String.format("Server[%s, %s:%d, %s, %s, weight=%s]", new Object[] { uuid, hostname, Integer.valueOf(port), mode, role, Double.valueOf(weight) });
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Server)) {
      return false;
    }
    Server s = (Server)o;
    return s.getUuid().equals(getUuid());
  }
  
  public int hashCode()
  {
    return getUuid().hashCode();
  }
  
  public int compareTo(Server other) {
    return getUuid().compareTo(other.getUuid());
  }
}
