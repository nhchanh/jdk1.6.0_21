/*
 * @(#)TreeWillExpandListener.java	1.11 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.swing.event;

import java.util.EventListener;
import javax.swing.tree.ExpandVetoException;

/**
  * The listener that's notified when a tree expands or collapses
  * a node.
  * For further information and examples see
  * <a href="http://java.sun.com/docs/books/tutorial/uiswing/events/treewillexpandlistener.html">How to Write a Tree-Will-Expand Listener</a>,
  * a section in <em>The Java Tutorial.</em>
  *
  * @version 1.11 03/23/10
  * @author Scott Violet
  */

public interface TreeWillExpandListener extends EventListener {
    /**
     * Invoked whenever a node in the tree is about to be expanded.
     */
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException;

    /**
     * Invoked whenever a node in the tree is about to be collapsed.
     */
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException;
}
