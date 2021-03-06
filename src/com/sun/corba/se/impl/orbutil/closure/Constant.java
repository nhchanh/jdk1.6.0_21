/*
 * @(#)Constant.java	1.9 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.corba.se.impl.orbutil.closure ;

import com.sun.corba.se.spi.orbutil.closure.Closure ;

public class Constant implements Closure {
    private Object value ;

    public Constant( Object value ) 
    {
	this.value = value ;
    }

    public Object evaluate() 
    {
	return value ;
    }
}

