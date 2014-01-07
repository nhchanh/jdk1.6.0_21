/*
 * @(#)IdentifiableFactory.java	1.16 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.corba.se.spi.ior ;

import org.omg.CORBA_2_3.portable.InputStream ;

import com.sun.corba.se.spi.ior.Identifiable ;

/** Factory interface for creating Identifiables.
 */
public interface IdentifiableFactory {
    /** Return the id of this factory, which is the id of the result 
     * of any create call.
     */
    public int getId() ;

    /** Construct the appropriate Identifiable object with the 
     * given id from the InputStream is.  
     */
    public Identifiable create( InputStream in ) ;
}
