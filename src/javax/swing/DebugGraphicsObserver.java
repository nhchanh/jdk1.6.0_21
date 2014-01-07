/*
 * @(#)DebugGraphicsObserver.java	1.13 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.swing;

import java.awt.*;
import java.awt.image.*;

/** ImageObserver for DebugGraphics, used for images only.
  * 
  * @version 1.13 03/23/10
  * @author Dave Karlton
  */
class DebugGraphicsObserver implements ImageObserver {
    int lastInfo;

    synchronized boolean allBitsPresent() {
        return (lastInfo & ImageObserver.ALLBITS) != 0;
    }

    synchronized boolean imageHasProblem() {
        return ((lastInfo & ImageObserver.ERROR) != 0 ||
                (lastInfo & ImageObserver.ABORT) != 0);
    }

    public synchronized boolean imageUpdate(Image img, int infoflags,
                                            int x, int y,
                                            int width, int height) {
        lastInfo = infoflags;
        return true;
    }
}
