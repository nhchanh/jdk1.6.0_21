/*
 * @(#)WindowsDesktopPaneUI.java	1.25 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.java.swing.plaf.windows;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.ComponentUI;
import java.awt.event.*;

/**
 * Windows desktop pane.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases.  The current serialization support is appropriate
 * for short term storage or RMI between applications running the same
 * version of Swing.  A future release of Swing will provide support for
 * long term persistence.
 *
 * @version %i% 03/23/10
 * @author David Kloba
 */
public class WindowsDesktopPaneUI extends BasicDesktopPaneUI
{
    public static ComponentUI createUI(JComponent c) {
        return new WindowsDesktopPaneUI();
    }

    protected void installDesktopManager() {
	desktopManager = desktop.getDesktopManager();
	if(desktopManager == null) {
	    desktopManager = new WindowsDesktopManager();
	    desktop.setDesktopManager(desktopManager);
	}
    }

    protected void installDefaults() {
        super.installDefaults();
    }

    protected void installKeyboardActions() {
	super.installKeyboardActions();

        // Request focus if it isn't set.
        if(!desktop.requestDefaultFocus()) {
            desktop.requestFocus();
        }
    }
}
