package com.sun.corba.se.spi.activation.LocatorPackage;


/**
* com/sun/corba/se/spi/activation/LocatorPackage/ServerLocation.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../../../../src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Tuesday, June 22, 2010 1:35:08 AM GMT-08:00
*/

public final class ServerLocation implements org.omg.CORBA.portable.IDLEntity
{
  public String hostname = null;
  public com.sun.corba.se.spi.activation.ORBPortInfo ports[] = null;

  public ServerLocation ()
  {
  } // ctor

  public ServerLocation (String _hostname, com.sun.corba.se.spi.activation.ORBPortInfo[] _ports)
  {
    hostname = _hostname;
    ports = _ports;
  } // ctor

} // class ServerLocation