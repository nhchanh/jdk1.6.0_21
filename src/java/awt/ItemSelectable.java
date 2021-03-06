/*
 * @(#)ItemSelectable.java	1.18 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.awt;

import java.awt.event.*;

/**
 * The interface for objects which contain a set of items for
 * which zero or more can be selected.
 *
 * @version 1.18 03/23/10
 * @author Amy Fowler
 */

public interface ItemSelectable {

    /**
     * Returns the selected items or <code>null</code> if no
     * items are selected.
     */
    public Object[] getSelectedObjects();

    /**
     * Adds a listener to receive item events when the state of an item is
     * changed by the user. Item events are not sent when an item's
     * state is set programmatically.  If <code>l</code> is
     * <code>null</code>, no exception is thrown and no action is performed.
     *
     * @param    l the listener to receive events
     * @see ItemEvent
     */    
    public void addItemListener(ItemListener l);

    /**
     * Removes an item listener.
     * If <code>l</code> is <code>null</code>,
     * no exception is thrown and no action is performed.
     *
     * @param 	l the listener being removed
     * @see ItemEvent
     */ 
    public void removeItemListener(ItemListener l);
}
