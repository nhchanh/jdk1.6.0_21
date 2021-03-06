/*
 * @(#)ObjectInputStreamWithLoader.java	4.22 10/03/23
 * 
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.jmx.mbeanserver;

// Java import
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.ObjectStreamClass;

import javax.management.* ; 
 



/**
 * This class deserializes an object in the context of a specific class loader.
 *
 * @since 1.5
 */
class ObjectInputStreamWithLoader extends ObjectInputStream {

    
    private ClassLoader loader;


    /**
     * @exception IOException Signals that an I/O exception of some
     * sort has occurred.
     * @exception StreamCorruptedException The object stream is corrupt.
     */
    public ObjectInputStreamWithLoader(InputStream in, ClassLoader theLoader) 
	    throws IOException {
	super(in);
	this.loader = theLoader;
    }
    
    protected Class resolveClass(ObjectStreamClass aClass) 
	    throws IOException, ClassNotFoundException {      
	if (loader == null) {
	    return super.resolveClass(aClass);
	} else {
	    String name = aClass.getName();
	    // Query the class loader ...    	
	    return Class.forName(name, false, loader);
	}
    }
}
