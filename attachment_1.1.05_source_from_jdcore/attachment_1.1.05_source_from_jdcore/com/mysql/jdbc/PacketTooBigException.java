package com.mysql.jdbc;

import java.sql.SQLException;

































public class PacketTooBigException
  extends SQLException
{
  static final long serialVersionUID = 7248633977685452174L;
  
  public PacketTooBigException(long packetSize, long maximumPacketSize)
  {
    super(Messages.getString("PacketTooBigException.0") + packetSize + Messages.getString("PacketTooBigException.1") + maximumPacketSize + Messages.getString("PacketTooBigException.2") + Messages.getString("PacketTooBigException.3") + Messages.getString("PacketTooBigException.4"), "S1000");
  }
}
