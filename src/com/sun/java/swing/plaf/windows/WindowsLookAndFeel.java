/*
 * @(#)WindowsLookAndFeel.java	1.236 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * <p>These classes are designed to be used while the
 * corresponding <code>LookAndFeel</code> class has been installed
 * (<code>UIManager.setLookAndFeel(new <i>XXX</i>LookAndFeel())</code>).
 * Using them while a different <code>LookAndFeel</code> is installed
 * may produce unexpected results, including exceptions.
 * Additionally, changing the <code>LookAndFeel</code>
 * maintained by the <code>UIManager</code> without updating the
 * corresponding <code>ComponentUI</code> of any
 * <code>JComponent</code>s may also produce unexpected results,
 * such as the wrong colors showing up, and is generally not
 * encouraged.
 * 
 */

package com.sun.java.swing.plaf.windows;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;

import javax.swing.plaf.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultEditorKit;

import java.awt.Font;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.net.URL;
import java.io.Serializable;
import java.security.AccessController;
import java.util.*;

import sun.awt.SunToolkit;
import sun.awt.shell.ShellFolder;
import sun.font.FontManager;
import sun.security.action.GetPropertyAction;

import sun.swing.DefaultLayoutStyle;
import sun.swing.ImageIconUIResource;
import sun.swing.SwingLazyValue;
import sun.swing.SwingUtilities2;

import static com.sun.java.swing.plaf.windows.TMSchema.*;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

import com.sun.java.swing.plaf.windows.WindowsIconFactory
    .VistaMenuItemCheckIconFactory;

/**
 * Implements the Windows95/98/NT/2000 Look and Feel.
 * UI classes not implemented specifically for Windows will
 * default to those implemented in Basic.  
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases.  The current serialization support is appropriate
 * for short term storage or RMI between applications running the same
 * version of Swing.  A future release of Swing will provide support for
 * long term persistence.
 *
 * @version 1.236 03/23/10
 * @author unattributed
 */
public class WindowsLookAndFeel extends BasicLookAndFeel
{
    /**
     * A client property that can be used with any JComponent that will end up
     * calling the LookAndFeel.getDisabledIcon method. This client property,
     * when set to Boolean.TRUE, will cause getDisabledIcon to use an
     * alternate algorithm for creating disabled icons to produce icons
     * that appear similar to the native Windows file chooser
     */
    static final String HI_RES_DISABLED_ICON_CLIENT_KEY = 
        new StringBuilder("WindowsLookAndFeel.generateHiResDisabledIcon").toString();
    
    private Toolkit toolkit;
    private boolean updatePending = false;

    private boolean useSystemFontSettings = true;
    private boolean useSystemFontSizeSettings;

    // These properties are not used directly, but are kept as
    // private members to avoid being GC'd.
    private DesktopProperty themeActive, dllName, colorName, sizeName;
    private DesktopProperty aaSettings;

    private transient LayoutStyle style;

    /**
     * Base dialog units along the horizontal axis.
     */
    private int baseUnitX;

    /**
     * Base dialog units along the vertical axis.
     */
    private int baseUnitY;

    public String getName() {
        return "Windows";
    }

    public String getDescription() {
        return "The Microsoft Windows Look and Feel";
    }

    public String getID() {
        return "Windows";
    }
    
    public boolean isNativeLookAndFeel() {
        String osName = System.getProperty("os.name");
        return (osName != null) && (osName.indexOf("Windows") != -1);
    }

    public boolean isSupportedLookAndFeel() {
        return isNativeLookAndFeel();
    }

    public void initialize() {
        super.initialize();
	toolkit = Toolkit.getDefaultToolkit();

	// Set the flag which determines which version of Windows should
	// be rendered. This flag only need to be set once.
	// if version <= 4.0 then the classic LAF should be loaded.
	String osVersion = System.getProperty("os.version");
	if (osVersion != null) {
	    Float version = Float.valueOf(osVersion);
	    if (version.floatValue() <= 4.0) {
		isClassicWindows = true;
	    } else {
		isClassicWindows = false;
		XPStyle.invalidateStyle();
	    }
	}

	// Using the fonts set by the user can potentially cause
	// performance and compatibility issues, so allow this feature
	// to be switched off either at runtime or programmatically
	//
	String systemFonts = (String) java.security.AccessController.doPrivileged(
               new GetPropertyAction("swing.useSystemFontSettings"));
	useSystemFontSettings = (systemFonts == null ||
                                 Boolean.valueOf(systemFonts).booleanValue());

        if (useSystemFontSettings) {
            Object value = UIManager.get("Application.useSystemFontSettings");

            useSystemFontSettings = (value == null ||
                                     Boolean.TRUE.equals(value));
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
            addKeyEventPostProcessor(WindowsRootPaneUI.altProcessor);

    }
    
    /** 
     * Initialize the uiClassID to BasicComponentUI mapping.
     * The JComponent classes define their own uiClassID constants
     * (see AbstractComponent.getUIClassID).  This table must
     * map those constants to a BasicComponentUI class of the
     * appropriate type.
     * 
     * @see BasicLookAndFeel#getDefaults
     */
    protected void initClassDefaults(UIDefaults table)
    {
        super.initClassDefaults(table);

        final String windowsPackageName = "com.sun.java.swing.plaf.windows.";

        Object[] uiDefaults = {
              "ButtonUI", windowsPackageName + "WindowsButtonUI",
            "CheckBoxUI", windowsPackageName + "WindowsCheckBoxUI",
    "CheckBoxMenuItemUI", windowsPackageName + "WindowsCheckBoxMenuItemUI",
	       "LabelUI", windowsPackageName + "WindowsLabelUI",
         "RadioButtonUI", windowsPackageName + "WindowsRadioButtonUI",
 "RadioButtonMenuItemUI", windowsPackageName + "WindowsRadioButtonMenuItemUI",
        "ToggleButtonUI", windowsPackageName + "WindowsToggleButtonUI",
         "ProgressBarUI", windowsPackageName + "WindowsProgressBarUI",
	      "SliderUI", windowsPackageName + "WindowsSliderUI",
	   "SeparatorUI", windowsPackageName + "WindowsSeparatorUI",
           "SplitPaneUI", windowsPackageName + "WindowsSplitPaneUI",
	     "SpinnerUI", windowsPackageName + "WindowsSpinnerUI",
	  "TabbedPaneUI", windowsPackageName + "WindowsTabbedPaneUI",
            "TextAreaUI", windowsPackageName + "WindowsTextAreaUI",
           "TextFieldUI", windowsPackageName + "WindowsTextFieldUI",
       "PasswordFieldUI", windowsPackageName + "WindowsPasswordFieldUI",
            "TextPaneUI", windowsPackageName + "WindowsTextPaneUI",
          "EditorPaneUI", windowsPackageName + "WindowsEditorPaneUI",
                "TreeUI", windowsPackageName + "WindowsTreeUI",
	     "ToolBarUI", windowsPackageName + "WindowsToolBarUI",	      
    "ToolBarSeparatorUI", windowsPackageName + "WindowsToolBarSeparatorUI",
            "ComboBoxUI", windowsPackageName + "WindowsComboBoxUI",
	 "TableHeaderUI", windowsPackageName + "WindowsTableHeaderUI",
       "InternalFrameUI", windowsPackageName + "WindowsInternalFrameUI",
         "DesktopPaneUI", windowsPackageName + "WindowsDesktopPaneUI",
         "DesktopIconUI", windowsPackageName + "WindowsDesktopIconUI",
         "FileChooserUI", windowsPackageName + "WindowsFileChooserUI",
	        "MenuUI", windowsPackageName + "WindowsMenuUI",
	    "MenuItemUI", windowsPackageName + "WindowsMenuItemUI",
	     "MenuBarUI", windowsPackageName + "WindowsMenuBarUI",
	   "PopupMenuUI", windowsPackageName + "WindowsPopupMenuUI",
  "PopupMenuSeparatorUI", windowsPackageName + "WindowsPopupMenuSeparatorUI",
	   "ScrollBarUI", windowsPackageName + "WindowsScrollBarUI",
	    "RootPaneUI", windowsPackageName + "WindowsRootPaneUI"
        };

        table.putDefaults(uiDefaults);
    }

    /**
     * Load the SystemColors into the defaults table.  The keys
     * for SystemColor defaults are the same as the names of
     * the public fields in SystemColor.  If the table is being
     * created on a native Windows platform we use the SystemColor
     * values, otherwise we create color objects whose values match
     * the defaults Windows95 colors.
     */
    protected void initSystemColorDefaults(UIDefaults table)
    {
        String[] defaultSystemColors = {
                "desktop", "#005C5C", /* Color of the desktop background */
          "activeCaption", "#000080", /* Color for captions (title bars) when they are active. */
      "activeCaptionText", "#FFFFFF", /* Text color for text in captions (title bars). */
    "activeCaptionBorder", "#C0C0C0", /* Border color for caption (title bar) window borders. */
        "inactiveCaption", "#808080", /* Color for captions (title bars) when not active. */
    "inactiveCaptionText", "#C0C0C0", /* Text color for text in inactive captions (title bars). */
  "inactiveCaptionBorder", "#C0C0C0", /* Border color for inactive caption (title bar) window borders. */
                 "window", "#FFFFFF", /* Default color for the interior of windows */
           "windowBorder", "#000000", /* ??? */
             "windowText", "#000000", /* ??? */
                   "menu", "#C0C0C0", /* Background color for menus */
       "menuPressedItemB", "#000080", /* LightShadow of menubutton highlight */ 
       "menuPressedItemF", "#FFFFFF", /* Default color for foreground "text" in menu item */
               "menuText", "#000000", /* Text color for menus  */
                   "text", "#C0C0C0", /* Text background color */
               "textText", "#000000", /* Text foreground color */
          "textHighlight", "#000080", /* Text background color when selected */
      "textHighlightText", "#FFFFFF", /* Text color when selected */
       "textInactiveText", "#808080", /* Text color when disabled */
                "control", "#C0C0C0", /* Default color for controls (buttons, sliders, etc) */
            "controlText", "#000000", /* Default color for text in controls */
       "controlHighlight", "#C0C0C0",

  /*"controlHighlight", "#E0E0E0",*/ /* Specular highlight (opposite of the shadow) */
     "controlLtHighlight", "#FFFFFF", /* Highlight color for controls */
          "controlShadow", "#808080", /* Shadow color for controls */
        "controlDkShadow", "#000000", /* Dark shadow color for controls */
              "scrollbar", "#E0E0E0", /* Scrollbar background (usually the "track") */
                   "info", "#FFFFE1", /* ??? */
               "infoText", "#000000"  /* ??? */
        };

        loadSystemColors(table, defaultSystemColors, isNativeLookAndFeel());
    }

   /**
     * Initialize the defaults table with the name of the ResourceBundle
     * used for getting localized defaults.
     */
    private void initResourceBundle(UIDefaults table) {
        table.addResourceBundle( "com.sun.java.swing.plaf.windows.resources.windows" );
    }

    // XXX - there are probably a lot of redundant values that could be removed. 
    // ie. Take a look at RadioButtonBorder, etc...
    protected void initComponentDefaults(UIDefaults table) 
    {
        super.initComponentDefaults( table );

        initResourceBundle(table);

        // *** Shared Fonts
	Integer twelve = new Integer(12);
	Integer eight = new Integer(8);
	Integer ten = new Integer(10);
	Integer fontPlain = new Integer(Font.PLAIN);
	Integer fontBold = new Integer(Font.BOLD);

	Object dialogPlain12 = new SwingLazyValue(
			       "javax.swing.plaf.FontUIResource",
			       null,
			       new Object[] {Font.DIALOG, fontPlain, twelve});

	Object sansSerifPlain12 =  new SwingLazyValue(
			  "javax.swing.plaf.FontUIResource",
			  null,
			  new Object[] {Font.SANS_SERIF, fontPlain, twelve});
	Object monospacedPlain12 = new SwingLazyValue(
			  "javax.swing.plaf.FontUIResource",
			  null,
			  new Object[] {Font.MONOSPACED, fontPlain, twelve});
	Object dialogBold12 = new SwingLazyValue(
			  "javax.swing.plaf.FontUIResource",
			  null,
			  new Object[] {Font.DIALOG, fontBold, twelve});

        // *** Colors
	// XXX - some of these doens't seem to be used
        ColorUIResource red = new ColorUIResource(Color.red);
        ColorUIResource black = new ColorUIResource(Color.black);
        ColorUIResource white = new ColorUIResource(Color.white);
        ColorUIResource yellow = new ColorUIResource(Color.yellow);
        ColorUIResource gray = new ColorUIResource(Color.gray);
        ColorUIResource lightGray = new ColorUIResource(Color.lightGray);
        ColorUIResource darkGray = new ColorUIResource(Color.darkGray);
        ColorUIResource scrollBarTrack = lightGray;
        ColorUIResource scrollBarTrackHighlight = darkGray;

	// Set the flag which determines which version of Windows should
	// be rendered. This flag only need to be set once.
	// if version <= 4.0 then the classic LAF should be loaded.
	String osVersion = System.getProperty("os.version");
	if (osVersion != null) {
	    try {
		Float version = Float.valueOf(osVersion);
		if (version.floatValue() <= 4.0) {
		    isClassicWindows = true;
		} else {
		    isClassicWindows = false;
		}
	    } catch (NumberFormatException ex) {
		isClassicWindows = false;
	    }
	}

        // *** Tree 
        ColorUIResource treeSelection = new ColorUIResource(0, 0, 128);
        Object treeExpandedIcon = WindowsTreeUI.ExpandedIcon.createExpandedIcon();

        Object treeCollapsedIcon = WindowsTreeUI.CollapsedIcon.createCollapsedIcon();


	// *** Text
	Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
	              "control C", DefaultEditorKit.copyAction,
	              "control V", DefaultEditorKit.pasteAction,
                      "control X", DefaultEditorKit.cutAction,
			   "COPY", DefaultEditorKit.copyAction,
			  "PASTE", DefaultEditorKit.pasteAction,
			    "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,	    
	              "control A", DefaultEditorKit.selectAllAction,
	     "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
	             "shift LEFT", DefaultEditorKit.selectionBackwardAction,
	            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
	           "control LEFT", DefaultEditorKit.previousWordAction,
	          "control RIGHT", DefaultEditorKit.nextWordAction,
	     "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
	                   "HOME", DefaultEditorKit.beginLineAction,
	                    "END", DefaultEditorKit.endLineAction,
	             "shift HOME", DefaultEditorKit.selectionBeginLineAction,
	              "shift END", DefaultEditorKit.selectionEndLineAction,
                     "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
               "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                         "ctrl H", DefaultEditorKit.deletePrevCharAction,
                         "DELETE", DefaultEditorKit.deleteNextCharAction,
                    "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                          "RIGHT", DefaultEditorKit.forwardAction,
                           "LEFT", DefaultEditorKit.backwardAction,
                       "KP_RIGHT", DefaultEditorKit.forwardAction,
                        "KP_LEFT", DefaultEditorKit.backwardAction,
	                  "ENTER", JTextField.notifyAction,
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
	});

        Object passwordInputMap = new UIDefaults.LazyInputMap(new Object[] {
                      "control C", DefaultEditorKit.copyAction,
                      "control V", DefaultEditorKit.pasteAction,
                      "control X", DefaultEditorKit.cutAction,
                           "COPY", DefaultEditorKit.copyAction,
                          "PASTE", DefaultEditorKit.pasteAction,
                            "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,
                      "control A", DefaultEditorKit.selectAllAction,
             "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                     "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "control LEFT", DefaultEditorKit.beginLineAction,
                  "control RIGHT", DefaultEditorKit.endLineAction,
             "control shift LEFT", DefaultEditorKit.selectionBeginLineAction,
            "control shift RIGHT", DefaultEditorKit.selectionEndLineAction,
                           "HOME", DefaultEditorKit.beginLineAction,
                            "END", DefaultEditorKit.endLineAction,
                     "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                      "shift END", DefaultEditorKit.selectionEndLineAction,
                     "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
               "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                         "ctrl H", DefaultEditorKit.deletePrevCharAction,
                         "DELETE", DefaultEditorKit.deleteNextCharAction,
                          "RIGHT", DefaultEditorKit.forwardAction,
                           "LEFT", DefaultEditorKit.backwardAction,
                       "KP_RIGHT", DefaultEditorKit.forwardAction,
                        "KP_LEFT", DefaultEditorKit.backwardAction,
                          "ENTER", JTextField.notifyAction,
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });

	Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] {
		      "control C", DefaultEditorKit.copyAction,
		      "control V", DefaultEditorKit.pasteAction,
		      "control X", DefaultEditorKit.cutAction,
			   "COPY", DefaultEditorKit.copyAction,
			  "PASTE", DefaultEditorKit.pasteAction,
			    "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,	    
		     "shift LEFT", DefaultEditorKit.selectionBackwardAction,
		    "shift RIGHT", DefaultEditorKit.selectionForwardAction,
		   "control LEFT", DefaultEditorKit.previousWordAction,
		  "control RIGHT", DefaultEditorKit.nextWordAction,
	     "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
	    "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
		      "control A", DefaultEditorKit.selectAllAction,
	     "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
			   "HOME", DefaultEditorKit.beginLineAction,
			    "END", DefaultEditorKit.endLineAction,
		     "shift HOME", DefaultEditorKit.selectionBeginLineAction,
		      "shift END", DefaultEditorKit.selectionEndLineAction,
		   "control HOME", DefaultEditorKit.beginAction,
		    "control END", DefaultEditorKit.endAction,
	     "control shift HOME", DefaultEditorKit.selectionBeginAction,
	      "control shift END", DefaultEditorKit.selectionEndAction,
			     "UP", DefaultEditorKit.upAction,
			   "DOWN", DefaultEditorKit.downAction,
                     "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
               "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                         "ctrl H", DefaultEditorKit.deletePrevCharAction,
                         "DELETE", DefaultEditorKit.deleteNextCharAction,
                    "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                          "RIGHT", DefaultEditorKit.forwardAction,
                           "LEFT", DefaultEditorKit.backwardAction,
                       "KP_RIGHT", DefaultEditorKit.forwardAction,
                        "KP_LEFT", DefaultEditorKit.backwardAction,
			"PAGE_UP", DefaultEditorKit.pageUpAction,
		      "PAGE_DOWN", DefaultEditorKit.pageDownAction,
		  "shift PAGE_UP", "selection-page-up",
 	        "shift PAGE_DOWN", "selection-page-down",
	     "ctrl shift PAGE_UP", "selection-page-left",
 	   "ctrl shift PAGE_DOWN", "selection-page-right",
		       "shift UP", DefaultEditorKit.selectionUpAction,
		     "shift DOWN", DefaultEditorKit.selectionDownAction,
			  "ENTER", DefaultEditorKit.insertBreakAction,
			    "TAB", DefaultEditorKit.insertTabAction,
                      "control T", "next-link-action",
                "control shift T", "previous-link-action",
                  "control SPACE", "activate-link-action",
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
	});

	Object menuItemAcceleratorDelimiter = new String("+");

	Object ControlBackgroundColor = new DesktopProperty(
                                                       "win.3d.backgroundColor", 
						        table.get("control"),
                                                       toolkit);
	Object ControlLightColor      = new DesktopProperty(
                                                       "win.3d.lightColor", 
							table.get("controlHighlight"),
                                                       toolkit);
	Object ControlHighlightColor  = new DesktopProperty(
                                                       "win.3d.highlightColor", 
							table.get("controlLtHighlight"),
                                                       toolkit);
	Object ControlShadowColor     = new DesktopProperty(
                                                       "win.3d.shadowColor", 
							table.get("controlShadow"),
                                                       toolkit);
	Object ControlDarkShadowColor = new DesktopProperty(
                                                       "win.3d.darkShadowColor", 
							table.get("controlDkShadow"),
                                                       toolkit);
	Object ControlTextColor       = new DesktopProperty(
                                                       "win.button.textColor", 
							table.get("controlText"),
                                                       toolkit);
	Object MenuBackgroundColor    = new DesktopProperty(
                                                       "win.menu.backgroundColor", 
							table.get("menu"),
                                                       toolkit);
	Object MenuBarBackgroundColor = new DesktopProperty(
                                                       "win.menubar.backgroundColor", 
							table.get("menu"),
                                                       toolkit);
	Object MenuTextColor          = new DesktopProperty(
                                                       "win.menu.textColor", 
							table.get("menuText"),
                                                       toolkit);
	Object SelectionBackgroundColor = new DesktopProperty(
                                                       "win.item.highlightColor", 
							table.get("textHighlight"),
                                                       toolkit);
	Object SelectionTextColor     = new DesktopProperty(
                                                       "win.item.highlightTextColor", 
							table.get("textHighlightText"),
                                                       toolkit);
	Object WindowBackgroundColor  = new DesktopProperty(
                                                       "win.frame.backgroundColor", 
							table.get("window"),
                                                       toolkit);
	Object WindowTextColor        = new DesktopProperty(
                                                       "win.frame.textColor", 
							table.get("windowText"),
                                                       toolkit);
        Object WindowBorderWidth      = new DesktopProperty(
                                                       "win.frame.sizingBorderWidth",
                                                       new Integer(1),
                                                       toolkit);
        Object TitlePaneHeight        = new DesktopProperty(
                                                       "win.frame.captionHeight",
                                                       new Integer(18),
                                                       toolkit);
        Object TitleButtonWidth       = new DesktopProperty(
                                                       "win.frame.captionButtonWidth",
                                                       new Integer(16),
                                                       toolkit);
        Object TitleButtonHeight      = new DesktopProperty(
                                                       "win.frame.captionButtonHeight",
                                                       new Integer(16),
                                                       toolkit);
	Object InactiveTextColor      = new DesktopProperty(
                                                       "win.text.grayedTextColor", 
							table.get("textInactiveText"),
                                                       toolkit);
	Object ScrollbarBackgroundColor = new DesktopProperty(
                                                       "win.scrollbar.backgroundColor", 
							table.get("scrollbar"),
                                                       toolkit);

	Object TextBackground         = new XPColorValue(Part.EP_EDIT, null, Prop.FILLCOLOR,
							 WindowBackgroundColor);
        //The following four lines were commented out as part of bug 4991597
        //This code *is* correct, however it differs from WindowsXP and is, apparently
        //a Windows XP bug. Until Windows fixes this bug, we shall also exhibit the same
        //behavior
        //Object ReadOnlyTextBackground = new XPColorValue(Part.EP_EDITTEXT, State.READONLY, Prop.FILLCOLOR,
        //                                                 ControlBackgroundColor);
        //Object DisabledTextBackground = new XPColorValue(Part.EP_EDITTEXT, State.DISABLED, Prop.FILLCOLOR,
        //                                                 ControlBackgroundColor);
        Object ReadOnlyTextBackground = ControlBackgroundColor;
        Object DisabledTextBackground = ControlBackgroundColor;

        Object MenuFont = dialogPlain12;
        Object FixedControlFont = monospacedPlain12;
        Object ControlFont = dialogPlain12;
        Object MessageFont = dialogPlain12;
        Object WindowFont = dialogBold12;
        Object ToolTipFont = sansSerifPlain12;
	Object IconFont = ControlFont;

	Object scrollBarWidth = new DesktopProperty("win.scrollbar.width",
						    new Integer(16), toolkit);

	Object menuBarHeight = new DesktopProperty("win.menu.height", 
						   null, toolkit);

	Object hotTrackingOn = new DesktopProperty("win.item.hotTrackingOn", 
						   true, toolkit);

	Object showMnemonics = new DesktopProperty("win.menu.keyboardCuesOn",
						     Boolean.TRUE, toolkit);

        if (useSystemFontSettings) {
            MenuFont = getDesktopFontValue("win.menu.font", MenuFont, toolkit);
            FixedControlFont = getDesktopFontValue("win.ansiFixed.font",
                                                   FixedControlFont, toolkit);
            ControlFont = getDesktopFontValue("win.defaultGUI.font",
                                              ControlFont, toolkit);
            MessageFont = getDesktopFontValue("win.messagebox.font",
                                              MessageFont, toolkit);
            WindowFont = getDesktopFontValue("win.frame.captionFont",
                                             WindowFont, toolkit);
	    IconFont    = getDesktopFontValue("win.icon.font",
					      IconFont, toolkit);
            ToolTipFont = getDesktopFontValue("win.tooltip.font", ToolTipFont,
                                              toolkit);

	    /* Put the desktop AA settings in the defaults.
	     * JComponent.setUI() retrieves this and makes it available
	     * as a client property on the JComponent. Use the same key name
	     * for both client property and UIDefaults.
	     * Also need to set up listeners for changes in these settings.
	     */
            Object aaTextInfo = SwingUtilities2.AATextInfo.getAATextInfo(true);
            table.put(SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);
            this.aaSettings =
                new FontDesktopProperty(SunToolkit.DESKTOPFONTHINTS);
        }
        if (useSystemFontSizeSettings) {
            MenuFont = new WindowsFontSizeProperty("win.menu.font.height",
                                  toolkit, Font.DIALOG, Font.PLAIN, 12);
            FixedControlFont = new WindowsFontSizeProperty(
                       "win.ansiFixed.font.height", toolkit, Font.MONOSPACED,
                       Font.PLAIN, 12);
            ControlFont = new WindowsFontSizeProperty(
                        "win.defaultGUI.font.height", toolkit, Font.DIALOG,
                        Font.PLAIN, 12);
            MessageFont = new WindowsFontSizeProperty(
                              "win.messagebox.font.height",
                              toolkit, Font.DIALOG, Font.PLAIN, 12);
            WindowFont = new WindowsFontSizeProperty(
                             "win.frame.captionFont.height", toolkit,
                             Font.DIALOG, Font.BOLD, 12);
            ToolTipFont = new WindowsFontSizeProperty(
                              "win.tooltip.font.height", toolkit, Font.SANS_SERIF,
                              Font.PLAIN, 12);
	    IconFont    = new WindowsFontSizeProperty(
			      "win.icon.font.height", toolkit, Font.DIALOG,
			      Font.PLAIN, 12);
        }


	if (!(this instanceof WindowsClassicLookAndFeel) &&
	    (System.getProperty("os.name").startsWith("Windows ") &&
	     System.getProperty("os.version").compareTo("5.1") >= 0) &&
	    AccessController.doPrivileged(new GetPropertyAction("swing.noxp")) == null) {

	    // These desktop properties are not used directly, but are needed to
	    // trigger realoading of UI's.
	    this.themeActive = new TriggerDesktopProperty("win.xpstyle.themeActive");
	    this.dllName     = new TriggerDesktopProperty("win.xpstyle.dllName");
	    this.colorName   = new TriggerDesktopProperty("win.xpstyle.colorName");
	    this.sizeName    = new TriggerDesktopProperty("win.xpstyle.sizeName");
	}


        Object[] defaults = {
	    // *** Auditory Feedback
	    // this key defines which of the various cues to render 
	    // Overridden from BasicL&F. This L&F should play all sounds
	    // all the time. The infrastructure decides what to play.
            // This is disabled until sound bugs can be resolved.
	    "AuditoryCues.playList", null, // table.get("AuditoryCues.cueList"),

	    "Application.useSystemFontSettings", Boolean.valueOf(useSystemFontSettings),

	    "TextField.focusInputMap", fieldInputMap,
	    "PasswordField.focusInputMap", passwordInputMap,
	    "TextArea.focusInputMap", multilineInputMap,
	    "TextPane.focusInputMap", multilineInputMap,
	    "EditorPane.focusInputMap", multilineInputMap,

	    // Buttons
	    "Button.font", ControlFont,
	    "Button.background", ControlBackgroundColor,
            // Button.foreground, Button.shadow, Button.darkShadow,
            // Button.disabledForground, and Button.disabledShadow are only
            // used for Windows Classic. Windows XP will use colors
            // from the current visual style.
	    "Button.foreground", ControlTextColor,
	    "Button.shadow", ControlShadowColor,
            "Button.darkShadow", ControlDarkShadowColor,
            "Button.light", ControlLightColor,
            "Button.highlight", ControlHighlightColor,
	    "Button.disabledForeground", InactiveTextColor,
	    "Button.disabledShadow", ControlHighlightColor,
            "Button.focus", black,
            "Button.dashedRectGapX", new XPValue(new Integer(3), new Integer(5)),
            "Button.dashedRectGapY", new XPValue(new Integer(3), new Integer(4)),
            "Button.dashedRectGapWidth", new XPValue(new Integer(6), new Integer(10)),
            "Button.dashedRectGapHeight", new XPValue(new Integer(6), new Integer(8)),
	    "Button.textShiftOffset", new XPValue(new Integer(0),
                                                  new Integer(1)),
	    // W2K keyboard navigation hidding.
	    "Button.showMnemonics", showMnemonics, 
            "Button.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                            "SPACE", "pressed",
                   "released SPACE", "released"
                 }),

	    "CheckBox.font", ControlFont,
            "CheckBox.interiorBackground", WindowBackgroundColor,
 	    "CheckBox.background", ControlBackgroundColor,
            "CheckBox.foreground", WindowTextColor,
            "CheckBox.shadow", ControlShadowColor,
            "CheckBox.darkShadow", ControlDarkShadowColor,
            "CheckBox.light", ControlLightColor,
            "CheckBox.highlight", ControlHighlightColor,
            "CheckBox.focus", black,
	    "CheckBox.focusInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		            "SPACE", "pressed",
                   "released SPACE", "released" 
		 }),
            // margin is 2 all the way around, BasicBorders.RadioButtonBorder
            // (checkbox uses RadioButtonBorder) is 2 all the way around too.
            "CheckBox.totalInsets", new Insets(4, 4, 4, 4),

            "CheckBoxMenuItem.font", MenuFont,
	    "CheckBoxMenuItem.background", MenuBackgroundColor,
	    "CheckBoxMenuItem.foreground", MenuTextColor,
	    "CheckBoxMenuItem.selectionForeground", SelectionTextColor,
	    "CheckBoxMenuItem.selectionBackground", SelectionBackgroundColor,
	    "CheckBoxMenuItem.acceleratorForeground", MenuTextColor,
	    "CheckBoxMenuItem.acceleratorSelectionForeground", SelectionTextColor,
	    "CheckBoxMenuItem.commandSound", "win.sound.menuCommand",

	    "ComboBox.font", ControlFont,
	    "ComboBox.background", WindowBackgroundColor,
	    "ComboBox.foreground", WindowTextColor,
	    "ComboBox.buttonBackground", ControlBackgroundColor,
	    "ComboBox.buttonShadow", ControlShadowColor,
	    "ComboBox.buttonDarkShadow", ControlDarkShadowColor,
	    "ComboBox.buttonHighlight", ControlHighlightColor,
            "ComboBox.selectionBackground", SelectionBackgroundColor,
            "ComboBox.selectionForeground", SelectionTextColor,
            "ComboBox.editorBorder", new XPValue(new EmptyBorder(1,2,1,1),
                                                 new EmptyBorder(1,4,1,4)),
            "ComboBox.disabledBackground", 
                        new XPColorValue(Part.CP_COMBOBOX, State.DISABLED,
                        Prop.FILLCOLOR, DisabledTextBackground),
            "ComboBox.disabledForeground", 
                        new XPColorValue(Part.CP_COMBOBOX, State.DISABLED,
                        Prop.TEXTCOLOR, InactiveTextColor),
	    "ComboBox.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] {
		   "ESCAPE", "hidePopup",
		  "PAGE_UP", "pageUpPassThrough",
		"PAGE_DOWN", "pageDownPassThrough",
		     "HOME", "homePassThrough",
		      "END", "endPassThrough",
		     "DOWN", "selectNext2",
		  "KP_DOWN", "selectNext2",
		       "UP", "selectPrevious2",
		    "KP_UP", "selectPrevious2",
		    "ENTER", "enterPressed",
                       "F4", "togglePopup", 
                 "alt DOWN", "togglePopup", 
              "alt KP_DOWN", "togglePopup", 
                   "alt UP", "togglePopup", 
                "alt KP_UP", "togglePopup" 
	      }),

	    // DeskTop.
	    "Desktop.background", new DesktopProperty(
                                                 "win.desktop.backgroundColor",
						  table.get("desktop"),
                                                 toolkit),
	    "Desktop.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		   "ctrl F5", "restore", 
		   "ctrl F4", "close",
		   "ctrl F7", "move", 
		   "ctrl F8", "resize",
		   "RIGHT", "right",
		   "KP_RIGHT", "right",
		   "LEFT", "left",
		   "KP_LEFT", "left",
		   "UP", "up",
		   "KP_UP", "up",
		   "DOWN", "down",
		   "KP_DOWN", "down",
		   "ESCAPE", "escape",
		   "ctrl F9", "minimize", 
		   "ctrl F10", "maximize",
		   "ctrl F6", "selectNextFrame",
		   "ctrl TAB", "selectNextFrame",
		   "ctrl alt F6", "selectNextFrame",
		   "shift ctrl alt F6", "selectPreviousFrame",
                   "ctrl F12", "navigateNext",
                   "shift ctrl F12", "navigatePrevious"
	       }),

            // DesktopIcon
            "DesktopIcon.width", new Integer(160),

	    "EditorPane.font", ControlFont,
	    "EditorPane.background", WindowBackgroundColor,
	    "EditorPane.foreground", WindowTextColor,
	    "EditorPane.selectionBackground", SelectionBackgroundColor,
	    "EditorPane.selectionForeground", SelectionTextColor,
	    "EditorPane.caretForeground", WindowTextColor,
	    "EditorPane.inactiveForeground", InactiveTextColor,
            "EditorPane.inactiveBackground", WindowBackgroundColor,
            "EditorPane.disabledBackground", DisabledTextBackground,

	    "FileChooser.homeFolderIcon",  new LazyWindowsIcon(null,
							       "icons/HomeFolder.gif"),
	    "FileChooser.listFont", IconFont,
	    "FileChooser.listViewBackground", new XPColorValue(Part.LVP_LISTVIEW, null, Prop.FILLCOLOR,
							       WindowBackgroundColor),
	    "FileChooser.listViewBorder", new XPBorderValue(Part.LVP_LISTVIEW,
						  new SwingLazyValue(
							"javax.swing.plaf.BorderUIResource",
							"getLoweredBevelBorderUIResource")),
	    "FileChooser.listViewIcon",    new LazyWindowsIcon("fileChooserIcon ListView",
							       "icons/ListView.gif"),
	    "FileChooser.listViewWindowsStyle", Boolean.TRUE,
	    "FileChooser.detailsViewIcon", new LazyWindowsIcon("fileChooserIcon DetailsView",
							       "icons/DetailsView.gif"),
	    "FileChooser.upFolderIcon",    new LazyWindowsIcon("fileChooserIcon UpFolder",
							       "icons/UpFolder.gif"),
	    "FileChooser.newFolderIcon",   new LazyWindowsIcon("fileChooserIcon NewFolder",
							       "icons/NewFolder.gif"),
	    "FileChooser.useSystemExtensionHiding", Boolean.TRUE,

            "FileChooser.lookInLabelMnemonic", new Integer(KeyEvent.VK_I),
            "FileChooser.fileNameLabelMnemonic", new Integer(KeyEvent.VK_N),
            "FileChooser.filesOfTypeLabelMnemonic", new Integer(KeyEvent.VK_T),
	    "FileChooser.usesSingleFilePane", Boolean.TRUE,
	    "FileChooser.noPlacesBar", new DesktopProperty("win.comdlg.noPlacesBar",
							   Boolean.FALSE, toolkit),
	    "FileChooser.ancestorInputMap", 
	       new UIDefaults.LazyInputMap(new Object[] {
		     "ESCAPE", "cancelSelection",
		     "F2", "editFileName",
		     "F5", "refresh",
		     "BACK_SPACE", "Go Up",
		     "ENTER", "approveSelection",
		"ctrl ENTER", "approveSelection"
		 }),

	    "FileView.directoryIcon", SwingUtilities2.makeIcon(getClass(),
                                                               WindowsLookAndFeel.class,
                                                               "icons/Directory.gif"),
            "FileView.fileIcon", SwingUtilities2.makeIcon(getClass(),
                                                          WindowsLookAndFeel.class,
                                                          "icons/File.gif"),
            "FileView.computerIcon", SwingUtilities2.makeIcon(getClass(),
                                                              WindowsLookAndFeel.class,
                                                              "icons/Computer.gif"),
            "FileView.hardDriveIcon", SwingUtilities2.makeIcon(getClass(),
                                                               WindowsLookAndFeel.class,
                                                               "icons/HardDrive.gif"),
            "FileView.floppyDriveIcon", SwingUtilities2.makeIcon(getClass(),
                                                                 WindowsLookAndFeel.class,
                                                                 "icons/FloppyDrive.gif"),

            "FormattedTextField.font", ControlFont,
            "InternalFrame.titleFont", WindowFont,
            "InternalFrame.titlePaneHeight",   TitlePaneHeight,
            "InternalFrame.titleButtonWidth",  TitleButtonWidth,
            "InternalFrame.titleButtonHeight", TitleButtonHeight,
            "InternalFrame.titleButtonToolTipsOn", hotTrackingOn,
	    "InternalFrame.borderColor", ControlBackgroundColor,
	    "InternalFrame.borderShadow", ControlShadowColor,
	    "InternalFrame.borderDarkShadow", ControlDarkShadowColor,
	    "InternalFrame.borderHighlight", ControlHighlightColor,
	    "InternalFrame.borderLight", ControlLightColor,
            "InternalFrame.borderWidth", WindowBorderWidth,
            "InternalFrame.minimizeIconBackground", ControlBackgroundColor,
            "InternalFrame.resizeIconHighlight", ControlLightColor,
            "InternalFrame.resizeIconShadow", ControlShadowColor,
            "InternalFrame.activeBorderColor", new DesktopProperty(
                                                       "win.frame.activeBorderColor",
                                                       table.get("windowBorder"),
                                                       toolkit),
            "InternalFrame.inactiveBorderColor", new DesktopProperty(
                                                       "win.frame.inactiveBorderColor",
                                                       table.get("windowBorder"),
                                                       toolkit),
	    "InternalFrame.activeTitleBackground", new DesktopProperty(
                                                        "win.frame.activeCaptionColor",
							 table.get("activeCaption"),
                                                        toolkit),
	    "InternalFrame.activeTitleGradient", new DesktopProperty(
		                                        "win.frame.activeCaptionGradientColor",
							 table.get("activeCaption"),
                                                        toolkit),
	    "InternalFrame.activeTitleForeground", new DesktopProperty(
                                                        "win.frame.captionTextColor",
							 table.get("activeCaptionText"),
                                                        toolkit),
	    "InternalFrame.inactiveTitleBackground", new DesktopProperty(
                                                        "win.frame.inactiveCaptionColor",
							 table.get("inactiveCaption"),
                                                        toolkit),
	    "InternalFrame.inactiveTitleGradient", new DesktopProperty(
                                                        "win.frame.inactiveCaptionGradientColor",
							 table.get("inactiveCaption"),
                                                        toolkit),
	    "InternalFrame.inactiveTitleForeground", new DesktopProperty(
                                                        "win.frame.inactiveCaptionTextColor",
							 table.get("inactiveCaptionText"),
                                                        toolkit),
            
            "InternalFrame.maximizeIcon", 
                WindowsIconFactory.createFrameMaximizeIcon(),
            "InternalFrame.minimizeIcon", 
                WindowsIconFactory.createFrameMinimizeIcon(),
            "InternalFrame.iconifyIcon", 
                WindowsIconFactory.createFrameIconifyIcon(),
            "InternalFrame.closeIcon", 
                WindowsIconFactory.createFrameCloseIcon(),
            "InternalFrame.icon",
		new SwingLazyValue(
	"com.sun.java.swing.plaf.windows.WindowsInternalFrameTitlePane$ScalableIconUIResource",
		    // The constructor takes one arg: an array of UIDefaults.LazyValue
		    // representing the icons
		    new Object[][] { {
			SwingUtilities2.makeIcon(getClass(), BasicLookAndFeel.class, "icons/JavaCup16.png"),
			SwingUtilities2.makeIcon(getClass(), WindowsLookAndFeel.class, "icons/JavaCup32.png")
		    } }),

	    // Internal Frame Auditory Cue Mappings
            "InternalFrame.closeSound", "win.sound.close",
            "InternalFrame.maximizeSound", "win.sound.maximize",
            "InternalFrame.minimizeSound", "win.sound.minimize",
            "InternalFrame.restoreDownSound", "win.sound.restoreDown",
            "InternalFrame.restoreUpSound", "win.sound.restoreUp",

	    "InternalFrame.windowBindings", new Object[] {
		"shift ESCAPE", "showSystemMenu",
		  "ctrl SPACE", "showSystemMenu",
		      "ESCAPE", "hideSystemMenu"},
	    
	    // Label
	    "Label.font", ControlFont,
	    "Label.background", ControlBackgroundColor,
            "Label.foreground", WindowTextColor,
	    "Label.disabledForeground", InactiveTextColor,
	    "Label.disabledShadow", ControlHighlightColor,

	    // List.
	    "List.font", ControlFont,
	    "List.background", WindowBackgroundColor,
	    "List.foreground", WindowTextColor,
	    "List.selectionBackground", SelectionBackgroundColor,
	    "List.selectionForeground", SelectionTextColor,
	    "List.lockToPositionOnScroll", Boolean.TRUE,
	    "List.focusInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", "copy",
                           "ctrl V", "paste",
                           "ctrl X", "cut",
                             "COPY", "copy",
                            "PASTE", "paste",
                              "CUT", "cut",
                   "control INSERT", "copy",
                     "shift INSERT", "paste",
                     "shift DELETE", "cut",
		               "UP", "selectPreviousRow",
		            "KP_UP", "selectPreviousRow",
		         "shift UP", "selectPreviousRowExtendSelection",
		      "shift KP_UP", "selectPreviousRowExtendSelection",
                    "ctrl shift UP", "selectPreviousRowExtendSelection",
                 "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                          "ctrl UP", "selectPreviousRowChangeLead",
                       "ctrl KP_UP", "selectPreviousRowChangeLead",
		             "DOWN", "selectNextRow",
		          "KP_DOWN", "selectNextRow",
		       "shift DOWN", "selectNextRowExtendSelection",
		    "shift KP_DOWN", "selectNextRowExtendSelection",
                  "ctrl shift DOWN", "selectNextRowExtendSelection",
               "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                        "ctrl DOWN", "selectNextRowChangeLead",
                     "ctrl KP_DOWN", "selectNextRowChangeLead",
		             "LEFT", "selectPreviousColumn",
		          "KP_LEFT", "selectPreviousColumn",
		       "shift LEFT", "selectPreviousColumnExtendSelection",
		    "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                  "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
               "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                        "ctrl LEFT", "selectPreviousColumnChangeLead",
                     "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
		            "RIGHT", "selectNextColumn",
		         "KP_RIGHT", "selectNextColumn",
		      "shift RIGHT", "selectNextColumnExtendSelection",
		   "shift KP_RIGHT", "selectNextColumnExtendSelection",
                 "ctrl shift RIGHT", "selectNextColumnExtendSelection",
              "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                       "ctrl RIGHT", "selectNextColumnChangeLead",
                    "ctrl KP_RIGHT", "selectNextColumnChangeLead",
		             "HOME", "selectFirstRow",
		       "shift HOME", "selectFirstRowExtendSelection",
                  "ctrl shift HOME", "selectFirstRowExtendSelection",
                        "ctrl HOME", "selectFirstRowChangeLead",
		              "END", "selectLastRow",
		        "shift END", "selectLastRowExtendSelection",
                   "ctrl shift END", "selectLastRowExtendSelection",
                         "ctrl END", "selectLastRowChangeLead",
		          "PAGE_UP", "scrollUp",
		    "shift PAGE_UP", "scrollUpExtendSelection",
               "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl PAGE_UP", "scrollUpChangeLead",
		        "PAGE_DOWN", "scrollDown",
		  "shift PAGE_DOWN", "scrollDownExtendSelection",
             "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl PAGE_DOWN", "scrollDownChangeLead",
		           "ctrl A", "selectAll",
		       "ctrl SLASH", "selectAll",
		  "ctrl BACK_SLASH", "clearSelection",
                            "SPACE", "addToSelection",
                       "ctrl SPACE", "toggleAndAnchor",
                      "shift SPACE", "extendTo",
                 "ctrl shift SPACE", "moveSelectionTo"
		 }),

	    // PopupMenu
	    "PopupMenu.font", MenuFont,
	    "PopupMenu.background", MenuBackgroundColor,
	    "PopupMenu.foreground", MenuTextColor,
            "PopupMenu.popupSound", "win.sound.menuPopup",
            "PopupMenu.consumeEventOnClose", Boolean.TRUE,

	    // Menus
            "Menu.font", MenuFont,
            "Menu.foreground", MenuTextColor,
            "Menu.background", MenuBackgroundColor,
	    "Menu.useMenuBarBackgroundForTopLevel", Boolean.TRUE,
            "Menu.selectionForeground", SelectionTextColor,
            "Menu.selectionBackground", SelectionBackgroundColor,
	    "Menu.acceleratorForeground", MenuTextColor,
	    "Menu.acceleratorSelectionForeground", SelectionTextColor,
	    "Menu.menuPopupOffsetX", new Integer(0),
	    "Menu.menuPopupOffsetY", new Integer(0),
	    "Menu.submenuPopupOffsetX", new Integer(-4),
	    "Menu.submenuPopupOffsetY", new Integer(-3),
            "Menu.crossMenuMnemonic", Boolean.FALSE,

	    // MenuBar.
	    "MenuBar.font", MenuFont,
	    "MenuBar.background", new XPValue(MenuBarBackgroundColor,
					      MenuBackgroundColor),
	    "MenuBar.foreground", MenuTextColor,
	    "MenuBar.shadow", ControlShadowColor,
	    "MenuBar.highlight", ControlHighlightColor,
	    "MenuBar.height", menuBarHeight,
	    "MenuBar.rolloverEnabled", hotTrackingOn,
	    "MenuBar.windowBindings", new Object[] {
		"F10", "takeFocus" },

            "MenuItem.font", MenuFont,
            "MenuItem.acceleratorFont", MenuFont,
            "MenuItem.foreground", MenuTextColor,
            "MenuItem.background", MenuBackgroundColor,
            "MenuItem.selectionForeground", SelectionTextColor,
            "MenuItem.selectionBackground", SelectionBackgroundColor,
	    "MenuItem.disabledForeground", InactiveTextColor,
	    "MenuItem.acceleratorForeground", MenuTextColor,
	    "MenuItem.acceleratorSelectionForeground", SelectionTextColor,
	    "MenuItem.acceleratorDelimiter", menuItemAcceleratorDelimiter,
	         // Menu Item Auditory Cue Mapping
	    "MenuItem.commandSound", "win.sound.menuCommand",
             // indicates that keyboard navigation won't skip disabled menu items
            "MenuItem.disabledAreNavigable", Boolean.TRUE,

	    "RadioButton.font", ControlFont,
            "RadioButton.interiorBackground", WindowBackgroundColor,
            "RadioButton.background", ControlBackgroundColor,
            "RadioButton.foreground", WindowTextColor,
            "RadioButton.shadow", ControlShadowColor,
            "RadioButton.darkShadow", ControlDarkShadowColor,
            "RadioButton.light", ControlLightColor,
	    "RadioButton.highlight", ControlHighlightColor,
            "RadioButton.focus", black,
	    "RadioButton.focusInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                          "SPACE", "pressed",
                 "released SPACE", "released" 
	      }),
            // margin is 2 all the way around, BasicBorders.RadioButtonBorder
            // is 2 all the way around too.
            "RadioButton.totalInsets", new Insets(4, 4, 4, 4),


            "RadioButtonMenuItem.font", MenuFont,
	    "RadioButtonMenuItem.foreground", MenuTextColor,
	    "RadioButtonMenuItem.background", MenuBackgroundColor,
	    "RadioButtonMenuItem.selectionForeground", SelectionTextColor,
	    "RadioButtonMenuItem.selectionBackground", SelectionBackgroundColor,
	    "RadioButtonMenuItem.disabledForeground", InactiveTextColor,
	    "RadioButtonMenuItem.acceleratorForeground", MenuTextColor,
	    "RadioButtonMenuItem.acceleratorSelectionForeground", SelectionTextColor,
	    "RadioButtonMenuItem.commandSound", "win.sound.menuCommand",

	    // OptionPane.
	    "OptionPane.font", MessageFont,
	    "OptionPane.messageFont", MessageFont,
	    "OptionPane.buttonFont", MessageFont,
	    "OptionPane.background", ControlBackgroundColor,
	    "OptionPane.foreground", WindowTextColor,
            "OptionPane.buttonMinimumWidth", new XPDLUValue(50, 50, SwingConstants.EAST),
            "OptionPane.messageForeground", ControlTextColor,
	    "OptionPane.errorIcon",       new LazyWindowsIcon("optionPaneIcon Error",
							      "icons/Error.gif"),
	    "OptionPane.informationIcon", new LazyWindowsIcon("optionPaneIcon Information",
							      "icons/Inform.gif"),
	    "OptionPane.questionIcon",    new LazyWindowsIcon("optionPaneIcon Question",
							      "icons/Question.gif"),
	    "OptionPane.warningIcon",     new LazyWindowsIcon("optionPaneIcon Warning",
							      "icons/Warn.gif"),
	    "OptionPane.windowBindings", new Object[] {
		"ESCAPE", "close" },
	         // Option Pane Auditory Cue Mappings
            "OptionPane.errorSound", "win.sound.hand", // Error
            "OptionPane.informationSound", "win.sound.asterisk", // Info Plain
            "OptionPane.questionSound", "win.sound.question", // Question
            "OptionPane.warningSound", "win.sound.exclamation", // Warning

            "FormattedTextField.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
			   "ctrl C", DefaultEditorKit.copyAction,
			   "ctrl V", DefaultEditorKit.pasteAction,
			   "ctrl X", DefaultEditorKit.cutAction,
			     "COPY", DefaultEditorKit.copyAction,
			    "PASTE", DefaultEditorKit.pasteAction,
			      "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
		       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
		      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
		   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
			"ctrl LEFT", DefaultEditorKit.previousWordAction,
		     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
		       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
		    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
		  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
	       "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
		 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
	      "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
			   "ctrl A", DefaultEditorKit.selectAllAction,
			     "HOME", DefaultEditorKit.beginLineAction,
			      "END", DefaultEditorKit.endLineAction,
		       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
		        "shift END", DefaultEditorKit.selectionEndLineAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
			    "ENTER", JTextField.notifyAction,
		  "ctrl BACK_SLASH", "unselect",
                   "control shift O", "toggle-componentOrientation",
                           "ESCAPE", "reset-field-edit",
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
              }),
            "FormattedTextField.inactiveBackground", ReadOnlyTextBackground,
            "FormattedTextField.disabledBackground", DisabledTextBackground,

	    // *** Panel
	    "Panel.font", ControlFont,
	    "Panel.background", ControlBackgroundColor,
	    "Panel.foreground", WindowTextColor,

	    // *** PasswordField
            "PasswordField.font", ControlFont,
	    "PasswordField.background", TextBackground,
	    "PasswordField.foreground", WindowTextColor,
	    "PasswordField.inactiveForeground", InactiveTextColor,      // for disabled
	    "PasswordField.inactiveBackground", ReadOnlyTextBackground, // for readonly
	    "PasswordField.disabledBackground", DisabledTextBackground, // for disabled
	    "PasswordField.selectionBackground", SelectionBackgroundColor,
	    "PasswordField.selectionForeground", SelectionTextColor,
	    "PasswordField.caretForeground",WindowTextColor,
            "PasswordField.echoChar", new XPValue(new Character((char)0x25CF),
                                                  new Character('*')),

	    // *** ProgressBar
	    "ProgressBar.font", ControlFont,
	    "ProgressBar.foreground",  SelectionBackgroundColor,
	    "ProgressBar.background", ControlBackgroundColor,
	    "ProgressBar.shadow", ControlShadowColor,
	    "ProgressBar.highlight", ControlHighlightColor,
	    "ProgressBar.selectionForeground", ControlBackgroundColor,
	    "ProgressBar.selectionBackground", SelectionBackgroundColor,
            "ProgressBar.cellLength", new Integer(7),
            "ProgressBar.cellSpacing", new Integer(2),
            "ProgressBar.indeterminateInsets", new Insets(3, 3, 3, 3),

	    // *** RootPane.
	    // These bindings are only enabled when there is a default
	    // button set on the rootpane.
	    "RootPane.defaultButtonWindowKeyBindings", new Object[] {
		             "ENTER", "press",
		    "released ENTER", "release",
		        "ctrl ENTER", "press",
	       "ctrl released ENTER", "release"
	      },

	    // *** ScrollBar.
	    "ScrollBar.background", ScrollbarBackgroundColor,
	    "ScrollBar.foreground", ControlBackgroundColor,
	    "ScrollBar.track", white,
	    "ScrollBar.trackForeground", ScrollbarBackgroundColor,
	    "ScrollBar.trackHighlight", black,
	    "ScrollBar.trackHighlightForeground", scrollBarTrackHighlight,
	    "ScrollBar.thumb", ControlBackgroundColor,
	    "ScrollBar.thumbHighlight", ControlHighlightColor,
	    "ScrollBar.thumbDarkShadow", ControlDarkShadowColor,
	    "ScrollBar.thumbShadow", ControlShadowColor,
            "ScrollBar.width", scrollBarWidth,
	    "ScrollBar.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		       "RIGHT", "positiveUnitIncrement",
		    "KP_RIGHT", "positiveUnitIncrement",
		        "DOWN", "positiveUnitIncrement",
		     "KP_DOWN", "positiveUnitIncrement",
		   "PAGE_DOWN", "positiveBlockIncrement",
	      "ctrl PAGE_DOWN", "positiveBlockIncrement",
		        "LEFT", "negativeUnitIncrement",
		     "KP_LEFT", "negativeUnitIncrement",
		          "UP", "negativeUnitIncrement",
		       "KP_UP", "negativeUnitIncrement",
		     "PAGE_UP", "negativeBlockIncrement",
	        "ctrl PAGE_UP", "negativeBlockIncrement",
		        "HOME", "minScroll",
		         "END", "maxScroll"
		 }),

	    // *** ScrollPane.
	    "ScrollPane.font", ControlFont,
	    "ScrollPane.background", ControlBackgroundColor,
	    "ScrollPane.foreground", ControlTextColor,
	    "ScrollPane.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		           "RIGHT", "unitScrollRight",
		        "KP_RIGHT", "unitScrollRight",
		            "DOWN", "unitScrollDown",
		         "KP_DOWN", "unitScrollDown",
		            "LEFT", "unitScrollLeft",
		         "KP_LEFT", "unitScrollLeft",
		              "UP", "unitScrollUp",
		           "KP_UP", "unitScrollUp",
		         "PAGE_UP", "scrollUp",
		       "PAGE_DOWN", "scrollDown",
		    "ctrl PAGE_UP", "scrollLeft",
		  "ctrl PAGE_DOWN", "scrollRight",
		       "ctrl HOME", "scrollHome",
		        "ctrl END", "scrollEnd"
		 }),

	    // *** Separator
            "Separator.background", ControlHighlightColor,
            "Separator.foreground", ControlShadowColor,

	    // *** Slider.
	    "Slider.font", ControlFont,
	    "Slider.foreground", ControlBackgroundColor,
	    "Slider.background", ControlBackgroundColor,
	    "Slider.highlight", ControlHighlightColor,
	    "Slider.shadow", ControlShadowColor,
	    "Slider.focus", ControlDarkShadowColor,
	    "Slider.focusInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		       "RIGHT", "positiveUnitIncrement",
		    "KP_RIGHT", "positiveUnitIncrement",
		        "DOWN", "negativeUnitIncrement",
		     "KP_DOWN", "negativeUnitIncrement",
		   "PAGE_DOWN", "negativeBlockIncrement",
		        "LEFT", "negativeUnitIncrement",
		     "KP_LEFT", "negativeUnitIncrement",
		          "UP", "positiveUnitIncrement",
		       "KP_UP", "positiveUnitIncrement",
		     "PAGE_UP", "positiveBlockIncrement",
		        "HOME", "minScroll",
		         "END", "maxScroll"
		 }),

            // Spinner
            "Spinner.font", ControlFont,
            "Spinner.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
               }),

	    // *** SplitPane
            "SplitPane.background", ControlBackgroundColor,
            "SplitPane.highlight", ControlHighlightColor,
            "SplitPane.shadow", ControlShadowColor,
	    "SplitPane.darkShadow", ControlDarkShadowColor,
	    "SplitPane.dividerSize", new Integer(5),
	    "SplitPane.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		        "UP", "negativeIncrement",
		      "DOWN", "positiveIncrement",
		      "LEFT", "negativeIncrement",
		     "RIGHT", "positiveIncrement",
		     "KP_UP", "negativeIncrement",
		   "KP_DOWN", "positiveIncrement",
		   "KP_LEFT", "negativeIncrement",
		  "KP_RIGHT", "positiveIncrement",
		      "HOME", "selectMin",
		       "END", "selectMax",
		        "F8", "startResize",
		        "F6", "toggleFocus",
		  "ctrl TAB", "focusOutForward",
 	    "ctrl shift TAB", "focusOutBackward"
	       }),

	    // *** TabbedPane
	    "TabbedPane.tabsOverlapBorder", new XPValue(Boolean.TRUE, Boolean.FALSE),
	    "TabbedPane.tabInsets",         new XPValue(new InsetsUIResource(1, 4, 1, 4),
							new InsetsUIResource(0, 4, 1, 4)),
	    "TabbedPane.tabAreaInsets",     new XPValue(new InsetsUIResource(3, 2, 2, 2),
							new InsetsUIResource(3, 2, 0, 2)),
            "TabbedPane.font", ControlFont,
            "TabbedPane.background", ControlBackgroundColor,
            "TabbedPane.foreground", ControlTextColor,
            "TabbedPane.highlight", ControlHighlightColor,
            "TabbedPane.light", ControlLightColor,
            "TabbedPane.shadow", ControlShadowColor,
            "TabbedPane.darkShadow", ControlDarkShadowColor,
            "TabbedPane.focus", ControlTextColor,
	    "TabbedPane.focusInputMap",
	      new UIDefaults.LazyInputMap(new Object[] {
		         "RIGHT", "navigateRight",
	              "KP_RIGHT", "navigateRight",
	                  "LEFT", "navigateLeft",
	               "KP_LEFT", "navigateLeft",
	                    "UP", "navigateUp",
	                 "KP_UP", "navigateUp",
	                  "DOWN", "navigateDown",
	               "KP_DOWN", "navigateDown",
	             "ctrl DOWN", "requestFocusForVisibleComponent",
	          "ctrl KP_DOWN", "requestFocusForVisibleComponent",
		}),
	    "TabbedPane.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                         "ctrl TAB", "navigateNext",
                   "ctrl shift TAB", "navigatePrevious",
		   "ctrl PAGE_DOWN", "navigatePageDown",
	             "ctrl PAGE_UP", "navigatePageUp",
	                  "ctrl UP", "requestFocus",
	               "ctrl KP_UP", "requestFocus",
		 }),

	    // *** Table
	    "Table.font", ControlFont,
	    "Table.foreground", ControlTextColor,  // cell text color
	    "Table.background", WindowBackgroundColor,  // cell background color
            "Table.highlight", ControlHighlightColor,
            "Table.light", ControlLightColor,
            "Table.shadow", ControlShadowColor,
            "Table.darkShadow", ControlDarkShadowColor,
	    "Table.selectionForeground", SelectionTextColor,
	    "Table.selectionBackground", SelectionBackgroundColor,
      	    "Table.gridColor", gray,  // grid line color
            "Table.focusCellBackground", WindowBackgroundColor, 
            "Table.focusCellForeground", ControlTextColor, 
	    "Table.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                               "ctrl C", "copy",
                               "ctrl V", "paste",
                               "ctrl X", "cut",
                                 "COPY", "copy",
                                "PASTE", "paste",
                                  "CUT", "cut",
                       "control INSERT", "copy",
                         "shift INSERT", "paste",
                         "shift DELETE", "cut",
                                "RIGHT", "selectNextColumn",
                             "KP_RIGHT", "selectNextColumn",
                          "shift RIGHT", "selectNextColumnExtendSelection",
                       "shift KP_RIGHT", "selectNextColumnExtendSelection",
                     "ctrl shift RIGHT", "selectNextColumnExtendSelection",
                  "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                           "ctrl RIGHT", "selectNextColumnChangeLead",
                        "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                                 "LEFT", "selectPreviousColumn",
                              "KP_LEFT", "selectPreviousColumn",
                           "shift LEFT", "selectPreviousColumnExtendSelection",
                        "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                      "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
                   "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                            "ctrl LEFT", "selectPreviousColumnChangeLead",
                         "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                                 "DOWN", "selectNextRow",
                              "KP_DOWN", "selectNextRow",
                           "shift DOWN", "selectNextRowExtendSelection",
                        "shift KP_DOWN", "selectNextRowExtendSelection",
                      "ctrl shift DOWN", "selectNextRowExtendSelection",
                   "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                            "ctrl DOWN", "selectNextRowChangeLead",
                         "ctrl KP_DOWN", "selectNextRowChangeLead",
                                   "UP", "selectPreviousRow",
                                "KP_UP", "selectPreviousRow",
                             "shift UP", "selectPreviousRowExtendSelection",
                          "shift KP_UP", "selectPreviousRowExtendSelection",
                        "ctrl shift UP", "selectPreviousRowExtendSelection",
                     "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                              "ctrl UP", "selectPreviousRowChangeLead",
                           "ctrl KP_UP", "selectPreviousRowChangeLead",
                                 "HOME", "selectFirstColumn",
                           "shift HOME", "selectFirstColumnExtendSelection",
                      "ctrl shift HOME", "selectFirstRowExtendSelection",
                            "ctrl HOME", "selectFirstRow",
                                  "END", "selectLastColumn",
                            "shift END", "selectLastColumnExtendSelection",
                       "ctrl shift END", "selectLastRowExtendSelection",
                             "ctrl END", "selectLastRow",
                              "PAGE_UP", "scrollUpChangeSelection",
                        "shift PAGE_UP", "scrollUpExtendSelection",
                   "ctrl shift PAGE_UP", "scrollLeftExtendSelection",
                         "ctrl PAGE_UP", "scrollLeftChangeSelection",
                            "PAGE_DOWN", "scrollDownChangeSelection",
                      "shift PAGE_DOWN", "scrollDownExtendSelection",
                 "ctrl shift PAGE_DOWN", "scrollRightExtendSelection",
                       "ctrl PAGE_DOWN", "scrollRightChangeSelection",
                                  "TAB", "selectNextColumnCell",
                            "shift TAB", "selectPreviousColumnCell",
                                "ENTER", "selectNextRowCell",
                          "shift ENTER", "selectPreviousRowCell",
                               "ctrl A", "selectAll",
                           "ctrl SLASH", "selectAll",
                      "ctrl BACK_SLASH", "clearSelection",
                               "ESCAPE", "cancel",
                                   "F2", "startEditing",
                                "SPACE", "addToSelection",
                           "ctrl SPACE", "toggleAndAnchor",
                          "shift SPACE", "extendTo",
                     "ctrl shift SPACE", "moveSelectionTo",
                                   "F8", "focusHeader"
		 }),
            "Table.sortIconHighlight", ControlShadowColor,
            "Table.sortIconLight", white,

	    "TableHeader.font", ControlFont,
	    "TableHeader.foreground", ControlTextColor, // header text color
	    "TableHeader.background", ControlBackgroundColor, // header background
            "TableHeader.focusCellBackground",
                new XPValue(XPValue.NULL_VALUE,     // use default bg from XP styles
                            WindowBackgroundColor), // or white bg otherwise

	    // *** TextArea
            "TextArea.font", FixedControlFont,
	    "TextArea.background", WindowBackgroundColor,
	    "TextArea.foreground", WindowTextColor,
	    "TextArea.inactiveForeground", InactiveTextColor,
            "TextArea.inactiveBackground", WindowBackgroundColor,
            "TextArea.disabledBackground", DisabledTextBackground,
	    "TextArea.selectionBackground", SelectionBackgroundColor,
	    "TextArea.selectionForeground", SelectionTextColor,
	    "TextArea.caretForeground", WindowTextColor,

	    // *** TextField
	    "TextField.font", ControlFont,
	    "TextField.background", TextBackground,
	    "TextField.foreground", WindowTextColor,
	    "TextField.shadow", ControlShadowColor,
	    "TextField.darkShadow", ControlDarkShadowColor,
	    "TextField.light", ControlLightColor,
	    "TextField.highlight", ControlHighlightColor,
	    "TextField.inactiveForeground", InactiveTextColor,      // for disabled
	    "TextField.inactiveBackground", ReadOnlyTextBackground, // for readonly
	    "TextField.disabledBackground", DisabledTextBackground, // for disabled
	    "TextField.selectionBackground", SelectionBackgroundColor,
	    "TextField.selectionForeground", SelectionTextColor,
	    "TextField.caretForeground", WindowTextColor,

	    // *** TextPane
	    "TextPane.font", ControlFont,
	    "TextPane.background", WindowBackgroundColor,
	    "TextPane.foreground", WindowTextColor,
	    "TextPane.selectionBackground", SelectionBackgroundColor,
	    "TextPane.selectionForeground", SelectionTextColor,
            "TextPane.inactiveBackground", WindowBackgroundColor,
            "TextPane.disabledBackground", DisabledTextBackground,
	    "TextPane.caretForeground", WindowTextColor,

	    // *** TitledBorder
            "TitledBorder.font", ControlFont,
            "TitledBorder.titleColor",
			new XPColorValue(Part.BP_GROUPBOX, null, Prop.TEXTCOLOR,
                                         WindowTextColor),

	    // *** ToggleButton
	    "ToggleButton.font", ControlFont,
            "ToggleButton.background", ControlBackgroundColor,
            "ToggleButton.foreground", ControlTextColor,
	    "ToggleButton.shadow", ControlShadowColor,
            "ToggleButton.darkShadow", ControlDarkShadowColor,
            "ToggleButton.light", ControlLightColor,
            "ToggleButton.highlight", ControlHighlightColor,
            "ToggleButton.focus", ControlTextColor,
	    "ToggleButton.textShiftOffset", new Integer(1),
 	    "ToggleButton.focusInputMap",
	      new UIDefaults.LazyInputMap(new Object[] {
		            "SPACE", "pressed",
                   "released SPACE", "released"
	        }),

	    // *** ToolBar
	    "ToolBar.font", MenuFont,
	    "ToolBar.background", ControlBackgroundColor,
	    "ToolBar.foreground", ControlTextColor,
	    "ToolBar.shadow", ControlShadowColor,
	    "ToolBar.darkShadow", ControlDarkShadowColor,
	    "ToolBar.light", ControlLightColor,
	    "ToolBar.highlight", ControlHighlightColor,
	    "ToolBar.dockingBackground", ControlBackgroundColor,
	    "ToolBar.dockingForeground", red,
	    "ToolBar.floatingBackground", ControlBackgroundColor,
	    "ToolBar.floatingForeground", darkGray,
	    "ToolBar.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		        "UP", "navigateUp",
		     "KP_UP", "navigateUp",
		      "DOWN", "navigateDown",
		   "KP_DOWN", "navigateDown",
		      "LEFT", "navigateLeft",
		   "KP_LEFT", "navigateLeft",
		     "RIGHT", "navigateRight",
		  "KP_RIGHT", "navigateRight"
		 }),
	    "ToolBar.separatorSize", null,

	    // *** ToolTip
            "ToolTip.font", ToolTipFont,
            "ToolTip.background", new DesktopProperty(
                                           "win.tooltip.backgroundColor",
					    table.get("info"), toolkit),
            "ToolTip.foreground", new DesktopProperty(
                                           "win.tooltip.textColor",
					    table.get("infoText"), toolkit),
                
        // *** ToolTipManager
            "ToolTipManager.enableToolTipMode", "activeApplication",

        // *** Tree
	    "Tree.selectionBorderColor", black,
	    "Tree.drawDashedFocusIndicator", Boolean.TRUE,
	    "Tree.lineTypeDashed", Boolean.TRUE,
	    "Tree.font", ControlFont,
	    "Tree.background", WindowBackgroundColor,
            "Tree.foreground", WindowTextColor,
	    "Tree.hash", gray,
            "Tree.leftChildIndent", new Integer(8),
            "Tree.rightChildIndent", new Integer(11),
	    "Tree.textForeground", WindowTextColor,
	    "Tree.textBackground", WindowBackgroundColor,
	    "Tree.selectionForeground", SelectionTextColor,
	    "Tree.selectionBackground", SelectionBackgroundColor,
            "Tree.expandedIcon", treeExpandedIcon,
            "Tree.collapsedIcon", treeCollapsedIcon,
            "Tree.openIcon",   new ActiveWindowsIcon("win.icon.shellIconBPP",
                                   "shell32Icon 5", "icons/TreeOpen.gif"),
            "Tree.closedIcon", new ActiveWindowsIcon("win.icon.shellIconBPP",
                                   "shell32Icon 4", "icons/TreeClosed.gif"),
	    "Tree.focusInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
                                    "ADD", "expand",
                               "SUBTRACT", "collapse",
                                 "ctrl C", "copy",
                                 "ctrl V", "paste",
                                 "ctrl X", "cut",
                                   "COPY", "copy",
                                  "PASTE", "paste",
                                    "CUT", "cut",
                         "control INSERT", "copy",
                           "shift INSERT", "paste",
                           "shift DELETE", "cut",
		                     "UP", "selectPrevious",
		                  "KP_UP", "selectPrevious",
		               "shift UP", "selectPreviousExtendSelection",
		            "shift KP_UP", "selectPreviousExtendSelection",
                          "ctrl shift UP", "selectPreviousExtendSelection",
                       "ctrl shift KP_UP", "selectPreviousExtendSelection",
                                "ctrl UP", "selectPreviousChangeLead",
                             "ctrl KP_UP", "selectPreviousChangeLead",
		                   "DOWN", "selectNext",
		                "KP_DOWN", "selectNext",
		             "shift DOWN", "selectNextExtendSelection",
		          "shift KP_DOWN", "selectNextExtendSelection",
                        "ctrl shift DOWN", "selectNextExtendSelection",
                     "ctrl shift KP_DOWN", "selectNextExtendSelection",
                              "ctrl DOWN", "selectNextChangeLead",
                           "ctrl KP_DOWN", "selectNextChangeLead",
		                  "RIGHT", "selectChild",
		               "KP_RIGHT", "selectChild",
		                   "LEFT", "selectParent",
		                "KP_LEFT", "selectParent",
		                "PAGE_UP", "scrollUpChangeSelection",
		          "shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                           "ctrl PAGE_UP", "scrollUpChangeLead",
		              "PAGE_DOWN", "scrollDownChangeSelection",
		        "shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                         "ctrl PAGE_DOWN", "scrollDownChangeLead",
		                   "HOME", "selectFirst",
		             "shift HOME", "selectFirstExtendSelection",
                        "ctrl shift HOME", "selectFirstExtendSelection",
                              "ctrl HOME", "selectFirstChangeLead",
		                    "END", "selectLast",
		              "shift END", "selectLastExtendSelection",
                         "ctrl shift END", "selectLastExtendSelection",
                               "ctrl END", "selectLastChangeLead",
		                     "F2", "startEditing",
		                 "ctrl A", "selectAll",
		             "ctrl SLASH", "selectAll",
		        "ctrl BACK_SLASH", "clearSelection",
		              "ctrl LEFT", "scrollLeft",
		           "ctrl KP_LEFT", "scrollLeft",
		             "ctrl RIGHT", "scrollRight",
		          "ctrl KP_RIGHT", "scrollRight",
                                  "SPACE", "addToSelection",
                             "ctrl SPACE", "toggleAndAnchor",
                            "shift SPACE", "extendTo",
                       "ctrl shift SPACE", "moveSelectionTo"
		 }),
	    "Tree.ancestorInputMap",
	       new UIDefaults.LazyInputMap(new Object[] {
		     "ESCAPE", "cancel"
		 }),

	    // *** Viewport
	    "Viewport.font", ControlFont,
	    "Viewport.background", ControlBackgroundColor,
	    "Viewport.foreground", WindowTextColor,


        };

        table.putDefaults(defaults);
	table.putDefaults(getLazyValueDefaults());
        initVistaComponentDefaults(table);
    }
    
    static boolean isOnVista() {
        boolean rv = false;
        String osName = System.getProperty("os.name");
        String osVers = System.getProperty("os.version");
        if (osName != null
                && osName.startsWith("Windows")
                && osVers != null
                && osVers.length() > 0) {

            int p = osVers.indexOf('.');
            if (p >= 0) {
                osVers = osVers.substring(0, p);
            }

            try {
                rv = (Integer.parseInt(osVers) >= 6);
            } catch (NumberFormatException nfe) {
            }
        }
        return rv;
    }
    private void initVistaComponentDefaults(UIDefaults table) {
        if (! isOnVista()) {
            return;
        }
        /* START handling menus for Vista */
        String[] menuClasses = { "MenuItem", "Menu", 
                "CheckBoxMenuItem", "RadioButtonMenuItem",
        };

        Object menuDefaults[] = new Object[menuClasses.length * 2];

        /* all the menus need to be non opaque. */
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".opaque";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] = 
                new XPValue(Boolean.FALSE, oldValue);
        }
        table.putDefaults(menuDefaults);

        /* 
         * acceleratorSelectionForeground color is the same as 
         * acceleratorForeground 
         */
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".acceleratorSelectionForeground";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key; 
            menuDefaults[j++] = 
                new XPValue(
                    table.getColor(
                        menuClasses[i] + ".acceleratorForeground"),
                        oldValue);
        }
        table.putDefaults(menuDefaults);

        /* they have the same MenuItemCheckIconFactory */
        VistaMenuItemCheckIconFactory menuItemCheckIconFactory = 
            WindowsIconFactory.getMenuItemCheckIconFactory();
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".checkIconFactory";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] =
                new XPValue(menuItemCheckIconFactory, oldValue);
        }
        table.putDefaults(menuDefaults);
        
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".checkIcon";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] =
                new XPValue(menuItemCheckIconFactory.getIcon(menuClasses[i]), 
                    oldValue);
        }
        table.putDefaults(menuDefaults);
        

        /* height can be even */
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".evenHeight";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] = new XPValue(Boolean.TRUE, oldValue);
        }
        table.putDefaults(menuDefaults);

        /* no margins */
        InsetsUIResource insets = new InsetsUIResource(0, 0, 0, 0);
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".margin";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] = new XPValue(insets, oldValue);
        }
        table.putDefaults(menuDefaults);

        /* set checkIcon offset */
        Integer checkIconOffsetInteger = 
            Integer.valueOf(0);
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".checkIconOffset";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] = 
                new XPValue(checkIconOffsetInteger, oldValue);
        }
        table.putDefaults(menuDefaults);

        /* text is started after this position */
        Object minimumTextOffset = new UIDefaults.ActiveValue() {
            public Object createValue(UIDefaults table) {
                return VistaMenuItemCheckIconFactory.getIconWidth()
                + WindowsPopupMenuUI.getSpanBeforeGutter()
                + WindowsPopupMenuUI.getGutterWidth()
                + WindowsPopupMenuUI.getSpanAfterGutter(); 
            }
        };
        for (int i = 0, j = 0; i < menuClasses.length; i++) {
            String key = menuClasses[i] + ".minimumTextOffset";
            Object oldValue = table.get(key);
            menuDefaults[j++] = key;
            menuDefaults[j++] = new XPValue(minimumTextOffset, oldValue);
        }
        table.putDefaults(menuDefaults);

        /*  
         * JPopupMenu has a bit of free space around menu items
         */
        String POPUP_MENU_BORDER = "PopupMenu.border";
        
        Object popupMenuBorder = new XPBorderValue(Part.MENU,
                new SwingLazyValue(
                  "javax.swing.plaf.basic.BasicBorders",
                  "getInternalFrameBorder"),
                  BorderFactory.createEmptyBorder(2, 2, 2, 2));
        table.put(POPUP_MENU_BORDER, popupMenuBorder);
        /* END handling menus for Vista */

        /* START table handling for Vista */
        table.put("Table.ascendingSortIcon", new XPValue(
            new SkinIcon(Part.HP_HEADERSORTARROW, State.SORTEDDOWN),
            new SwingLazyValue(
                "sun.swing.plaf.windows.ClassicSortArrowIcon",
                null, new Object[] { Boolean.TRUE })));
        table.put("Table.descendingSortIcon", new XPValue(
            new SkinIcon(Part.HP_HEADERSORTARROW, State.SORTEDUP),
            new SwingLazyValue(
                "sun.swing.plaf.windows.ClassicSortArrowIcon",
                null, new Object[] { Boolean.FALSE })));
        /* END table handling for Vista */
    }
    
    /**
     * If we support loading of fonts from the desktop this will return
     * a DesktopProperty representing the font. If the font can't be
     * represented in the current encoding this will return null and
     * turn off the use of system fonts.
     */
    private Object getDesktopFontValue(String fontName, Object backup,
                                       Toolkit kit) {
        if (useSystemFontSettings) {
            return new WindowsFontProperty(fontName, backup, kit);
        }
        return null;
    }

    // When a desktop property change is detected, these classes must be
    // reinitialized in the defaults table to ensure the classes reference
    // the updated desktop property values (colors mostly)
    //
    private Object[] getLazyValueDefaults() {

	Object buttonBorder =
	    new XPBorderValue(Part.BP_PUSHBUTTON,
			      new SwingLazyValue(
			       "javax.swing.plaf.basic.BasicBorders",
			       "getButtonBorder"));

	Object textFieldBorder =
	    new XPBorderValue(Part.EP_EDIT,
			      new SwingLazyValue(
			       "javax.swing.plaf.basic.BasicBorders", 
			       "getTextFieldBorder"));

	Object textFieldMargin =
	    new XPValue(new InsetsUIResource(2, 2, 2, 2),
			new InsetsUIResource(1, 1, 1, 1));

        Object spinnerBorder = 
            new XPBorderValue(Part.EP_EDIT, textFieldBorder,
                              new EmptyBorder(2, 2, 2, 2));

        Object spinnerArrowInsets = 
            new XPValue(new InsetsUIResource(1, 1, 1, 1),
                        null);

	Object comboBoxBorder = new XPBorderValue(Part.CP_COMBOBOX, textFieldBorder);

	// For focus rectangle for cells and trees.
	Object focusCellHighlightBorder = new SwingLazyValue(
			  "com.sun.java.swing.plaf.windows.WindowsBorders",
			  "getFocusCellHighlightBorder");

	Object etchedBorder = new SwingLazyValue(
			  "javax.swing.plaf.BorderUIResource",
			  "getEtchedBorderUIResource");

	Object internalFrameBorder = new SwingLazyValue(
                "com.sun.java.swing.plaf.windows.WindowsBorders", 
		"getInternalFrameBorder");

        Object loweredBevelBorder = new SwingLazyValue(
			  "javax.swing.plaf.BorderUIResource",
			  "getLoweredBevelBorderUIResource");


        Object marginBorder = new SwingLazyValue(
			    "javax.swing.plaf.basic.BasicBorders$MarginBorder");

	Object menuBarBorder = new SwingLazyValue(
                "javax.swing.plaf.basic.BasicBorders", 
		"getMenuBarBorder");


	Object popupMenuBorder = new XPBorderValue(Part.MENU,
			new SwingLazyValue(
			  "javax.swing.plaf.basic.BasicBorders",
			  "getInternalFrameBorder"));

	// *** ProgressBar
	Object progressBarBorder = new SwingLazyValue(
			      "com.sun.java.swing.plaf.windows.WindowsBorders", 
			      "getProgressBarBorder");

	Object radioButtonBorder = new SwingLazyValue(
			       "javax.swing.plaf.basic.BasicBorders", 
			       "getRadioButtonBorder");

	Object scrollPaneBorder =
	    new XPBorderValue(Part.LBP_LISTBOX, textFieldBorder);

	Object tableScrollPaneBorder =
	    new XPBorderValue(Part.LBP_LISTBOX, loweredBevelBorder);

	Object tableHeaderBorder = new SwingLazyValue(
			  "com.sun.java.swing.plaf.windows.WindowsBorders",
			  "getTableHeaderBorder");

	// *** ToolBar
	Object toolBarBorder = new SwingLazyValue(
			      "com.sun.java.swing.plaf.windows.WindowsBorders", 
			      "getToolBarBorder");

        // *** ToolTips
        Object toolTipBorder = new SwingLazyValue(
                              "javax.swing.plaf.BorderUIResource",
			      "getBlackLineBorderUIResource");



        Object checkBoxIcon = new SwingLazyValue(
		     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
		     "getCheckBoxIcon");

        Object radioButtonIcon = new SwingLazyValue(
		     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
		     "getRadioButtonIcon");

        Object radioButtonMenuItemIcon = new SwingLazyValue(
                     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
                     "getRadioButtonMenuItemIcon");

        Object menuItemCheckIcon = new SwingLazyValue(
		     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
		     "getMenuItemCheckIcon");

        Object menuItemArrowIcon = new SwingLazyValue(
		     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
		     "getMenuItemArrowIcon");

        Object menuArrowIcon = new SwingLazyValue(
		     "com.sun.java.swing.plaf.windows.WindowsIconFactory",
		     "getMenuArrowIcon");


        Object[] lazyDefaults = {
	    "Button.border", buttonBorder,
            "CheckBox.border", radioButtonBorder,
            "ComboBox.border", comboBoxBorder,
	    "DesktopIcon.border", internalFrameBorder,
	    "FormattedTextField.border", textFieldBorder,
	    "FormattedTextField.margin", textFieldMargin,
	    "InternalFrame.border", internalFrameBorder,
	    "List.focusCellHighlightBorder", focusCellHighlightBorder,
	    "Table.focusCellHighlightBorder", focusCellHighlightBorder,
	    "Menu.border", marginBorder,
	    "MenuBar.border", menuBarBorder,
            "MenuItem.border", marginBorder,
            "PasswordField.border", textFieldBorder,
            "PasswordField.margin", textFieldMargin,
	    "PopupMenu.border", popupMenuBorder,
	    "ProgressBar.border", progressBarBorder,
            "RadioButton.border", radioButtonBorder,
	    "ScrollPane.border", scrollPaneBorder,
	    "Spinner.border", spinnerBorder,
            "Spinner.arrowButtonInsets", spinnerArrowInsets,
            "Spinner.arrowButtonSize", new Dimension(17, 9),
	    "Table.scrollPaneBorder", tableScrollPaneBorder,
	    "TableHeader.cellBorder", tableHeaderBorder,
            "TextArea.margin", textFieldMargin,
	    "TextField.border", textFieldBorder,
	    "TextField.margin", textFieldMargin,
            "TitledBorder.border",
			new XPBorderValue(Part.BP_GROUPBOX, etchedBorder),
            "ToggleButton.border", radioButtonBorder,
	    "ToolBar.border", toolBarBorder,
            "ToolTip.border", toolTipBorder,

            "CheckBox.icon", checkBoxIcon,
            "Menu.arrowIcon", menuArrowIcon,
            "MenuItem.checkIcon", menuItemCheckIcon,
            "MenuItem.arrowIcon", menuItemArrowIcon,
            "RadioButton.icon", radioButtonIcon,
            "RadioButtonMenuItem.checkIcon", radioButtonMenuItemIcon,
            "InternalFrame.layoutTitlePaneAtOrigin",
			new XPValue(Boolean.TRUE, Boolean.FALSE),
            "Table.ascendingSortIcon", new XPValue(
                  new SwingLazyValue(
		     "sun.swing.icon.SortArrowIcon",
                     null, new Object[] { Boolean.TRUE,
                                          "Table.sortIconColor" }),
                  new SwingLazyValue(
                      "sun.swing.plaf.windows.ClassicSortArrowIcon",
                      null, new Object[] { Boolean.TRUE })),
            "Table.descendingSortIcon", new XPValue(
                  new SwingLazyValue(
		     "sun.swing.icon.SortArrowIcon",
                     null, new Object[] { Boolean.FALSE,
                                          "Table.sortIconColor" }),
                  new SwingLazyValue(
		     "sun.swing.plaf.windows.ClassicSortArrowIcon",
                     null, new Object[] { Boolean.FALSE })),
	};

	return lazyDefaults;
    }

    public void uninitialize() {
        super.uninitialize();
	toolkit = null;

        if (WindowsPopupMenuUI.mnemonicListener != null) {
            MenuSelectionManager.defaultManager().
                removeChangeListener(WindowsPopupMenuUI.mnemonicListener);
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
            removeKeyEventPostProcessor(WindowsRootPaneUI.altProcessor);
        DesktopProperty.flushUnreferencedProperties();
    }


    // Toggle flag for drawing the mnemonic state
    private static boolean isMnemonicHidden = true;

    // Flag which indicates that the Win98/Win2k/WinME features
    // should be disabled.
    private static boolean isClassicWindows = false;

    /**
     * Sets the state of the hide mnemonic flag. This flag is used by the 
     * component UI delegates to determine if the mnemonic should be rendered.
     * This method is a non operation if the underlying operating system
     * does not support the mnemonic hiding feature.
     * 
     * @param hide true if mnemonics should be hidden
     * @since 1.4
     */
    public static void setMnemonicHidden(boolean hide) {
	if (UIManager.getBoolean("Button.showMnemonics") == true) {
	    // Do not hide mnemonics if the UI defaults do not support this
	    isMnemonicHidden = false;
	} else {
	    isMnemonicHidden = hide;
	}
    }

    /**
     * Gets the state of the hide mnemonic flag. This only has meaning 
     * if this feature is supported by the underlying OS.
     *
     * @return true if mnemonics are hidden, otherwise, false
     * @see #setMnemonicHidden
     * @since 1.4
     */
    public static boolean isMnemonicHidden() {
	if (UIManager.getBoolean("Button.showMnemonics") == true) {
	    // Do not hide mnemonics if the UI defaults do not support this
	    isMnemonicHidden = false;
	}
	return isMnemonicHidden;
    }

    /**
     * Gets the state of the flag which indicates if the old Windows
     * look and feel should be rendered. This flag is used by the
     * component UI delegates as a hint to determine which style the component
     * should be rendered.
     *
     * @return true if Windows 95 and Windows NT 4 look and feel should
     *         be rendered
     * @since 1.4
     */
    public static boolean isClassicWindows() {
	return isClassicWindows;
    }

    /**
     * <p>
     * Invoked when the user attempts an invalid operation, 
     * such as pasting into an uneditable <code>JTextField</code> 
     * that has focus.
     * </p>
     * <p>
     * If the user has enabled visual error indication on
     * the desktop, this method will flash the caption bar
     * of the active window. The user can also set the
     * property awt.visualbell=true to achieve the same
     * results.
     * </p>
     *
     * @param component Component the error occured in, may be 
     *			null indicating the error condition is 
     *			not directly associated with a 
     *			<code>Component</code>.
     * 
     * @see javax.swing.LookAndFeel#provideErrorFeedback
     */
     public void provideErrorFeedback(Component component) {
	 super.provideErrorFeedback(component);
     }

    /**
     * {@inheritDoc}
     */
    public LayoutStyle getLayoutStyle() {
        LayoutStyle style = this.style;
        if (style == null) {
            style = new WindowsLayoutStyle();
            this.style = style;
        }
        return style;
    }

    // ********* Auditory Cue support methods and objects *********

    /**
     * Returns an <code>Action</code>.
     * <P>
     * This Action contains the information and logic to render an
     * auditory cue. The <code>Object</code> that is passed to this
     * method contains the information needed to render the auditory 
     * cue. Normally, this <code>Object</code> is a <code>String</code> 
     * that points to a <code>Toolkit</code> <code>desktopProperty</code>.
     * This <code>desktopProperty</code> is resolved by AWT and the 
     * Windows OS.
     * <P>
     * This <code>Action</code>'s <code>actionPerformed</code> method
     * is fired by the <code>playSound</code> method.
     *
     * @return      an Action which knows how to render the auditory
     *              cue for one particular system or user activity
     * @see #playSound(Action)
     * @since 1.4
     */
    protected Action createAudioAction(Object key) {
	if (key != null) {
	    String audioKey = (String)key;
	    String audioValue = (String)UIManager.get(key);
	    return new AudioAction(audioKey, audioValue);
    	} else {
	    return null;
	}
    }

    static void repaintRootPane(Component c) {
        JRootPane root = null;
        for (; c != null; c = c.getParent()) {
            if (c instanceof JRootPane) {
                root = (JRootPane)c;
            }
        }

        if (root != null) {
            root.repaint();
        } else {
            c.repaint();
        }
    }

    /**
     * Pass the name String to the super constructor. This is used 
     * later to identify the Action and decide whether to play it or 
     * not. Store the resource String. It is used to get the audio 
     * resource. In this case, the resource is a <code>Runnable</code> 
     * supplied by <code>Toolkit</code>. This <code>Runnable</code> is
     * effectively a pointer down into the Win32 OS that knows how to
     * play the right sound.
     *
     * @since 1.4
     */
    private static class AudioAction extends AbstractAction {
	private Runnable audioRunnable;
	private String audioResource;
	/**
	 * We use the String as the name of the Action and as a pointer to
	 * the underlying OSes audio resource.
	 */
	public AudioAction(String name, String resource) {
	    super(name);
	    audioResource = resource;
	}
	public void actionPerformed(ActionEvent e) {
	    if (audioRunnable == null) {
		audioRunnable = (Runnable)Toolkit.getDefaultToolkit().getDesktopProperty(audioResource);
	    }
	    if (audioRunnable != null) {
                // Runnable appears to block until completed playing, hence
                // start up another thread to handle playing.
                new Thread(audioRunnable).start();
	    }
	}
    }

    /**
     * Gets an <code>Icon</code> from the native libraries if available,
     * otherwise gets it from an image resource file.
     */
    private static class LazyWindowsIcon implements UIDefaults.LazyValue {
	private String nativeImage;
	private String resource;

	LazyWindowsIcon(String nativeImage, String resource) {
	    this.nativeImage = nativeImage;
	    this.resource = resource;
	}

	public Object createValue(UIDefaults table) {
	    if (nativeImage != null) {
		Image image = (Image)ShellFolder.get(nativeImage);
		if (image != null) {
		    return new ImageIcon(image);
		}
	    }
	    return SwingUtilities2.makeIcon(getClass(),
                                            WindowsLookAndFeel.class,
                                            resource);
	}
    }


    /**
     * Gets an <code>Icon</code> from the native libraries if available.
     * A desktop property is used to trigger reloading the icon when needed.
     */
    private class ActiveWindowsIcon implements UIDefaults.ActiveValue {
	private Icon icon;
	private String nativeImageName;
        private String fallbackName;
	private DesktopProperty desktopProperty;

	ActiveWindowsIcon(String desktopPropertyName,
                            String nativeImageName, String fallbackName) {
	    this.nativeImageName = nativeImageName;
            this.fallbackName = fallbackName;

	    if (System.getProperty("os.name").startsWith("Windows ") &&
		System.getProperty("os.version").compareTo("5.1") < 0) {
		// This desktop property is needed to trigger reloading the icon.
		// It is kept in member variable to avoid GC.
		this.desktopProperty = new TriggerDesktopProperty(desktopPropertyName) {
		    @Override protected void updateUI() {
			icon = null;
			super.updateUI();
		    }		    
		};
	    }
	}

        @Override
	public Object createValue(UIDefaults table) {
	    if (icon == null) {
		Image image = (Image)ShellFolder.get(nativeImageName);
		if (image != null) {
		    icon = new ImageIconUIResource(image);
		}
	    }
	    if (icon == null && fallbackName != null) {
                UIDefaults.LazyValue fallback = (UIDefaults.LazyValue)
                        SwingUtilities2.makeIcon(WindowsLookAndFeel.class,
                            BasicLookAndFeel.class, fallbackName);
		icon = (Icon) fallback.createValue(table);
	    }
	    return icon;
	}
    }
    
    /**
     * Icon backed-up by XP Skin.
     */
    private static class SkinIcon implements Icon, UIResource {
        private final Part part;
        private final State state;
        SkinIcon(Part part, State state) {
            this.part = part;
            this.state = state;
        }

        /**
         * Draw the icon at the specified location.  Icon implementations
         * may use the Component argument to get properties useful for 
         * painting, e.g. the foreground or background color.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            XPStyle xp = XPStyle.getXP();
            assert xp != null;
            if (xp != null) {
                Skin skin = xp.getSkin(null, part);
                skin.paintSkin(g, x, y, state);
            }
        }
        
        /**
         * Returns the icon's width.
         *
         * @return an int specifying the fixed width of the icon.
         */
        public int getIconWidth() {
            int width = 0;
            XPStyle xp = XPStyle.getXP();
            assert xp != null;
            if (xp != null) {
                Skin skin = xp.getSkin(null, part);
                width = skin.getWidth();
            }
            return width;
        }
        
        /**
         * Returns the icon's height.
         *
         * @return an int specifying the fixed height of the icon.
         */
        public int getIconHeight() {
            int height = 0;
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                Skin skin = xp.getSkin(null, part);
                height = skin.getHeight();
            }
            return height;
        }
        
    }

    /**
     * DesktopProperty for fonts. If a font with the name 'MS Sans Serif'
     * is returned, it is mapped to 'Microsoft Sans Serif'.
     */
    private static class WindowsFontProperty extends DesktopProperty {
        WindowsFontProperty(String key, Object backup, Toolkit kit) {
            super(key, backup, kit);
        }

        public void invalidate(LookAndFeel laf) {
            if ("win.defaultGUI.font.height".equals(getKey())) {
                ((WindowsLookAndFeel)laf).style = null;
            }
            super.invalidate(laf);
        }

        protected Object configureValue(Object value) {
            if (value instanceof Font) {
                Font font = (Font)value;
                if ("MS Sans Serif".equals(font.getName())) {
		    int size = font.getSize();
		    // 4950968: Workaround to mimic the way Windows maps the default
		    // font size of 6 pts to the smallest available bitmap font size.
		    // This happens mostly on Win 98/Me & NT.
		    int dpi;
		    try {
			dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		    } catch (HeadlessException ex) {
			dpi = 96;
		    }
		    if (Math.round(size * 72F / dpi) < 8) {
			size = Math.round(8 * dpi / 72F);
		    }
                    Font msFont = new FontUIResource("Microsoft Sans Serif",
                                          font.getStyle(), size);
                    if (msFont.getName() != null &&
                        msFont.getName().equals(msFont.getFamily())) {
                        font = msFont;
		    } else if (size != font.getSize()) {
			font = new FontUIResource("MS Sans Serif",
						  font.getStyle(), size);
                    }
                }
                if (FontManager.fontSupportsDefaultEncoding(font)) {
                    if (!(font instanceof UIResource)) {
                        font = new FontUIResource(font);
                    }
                }
                else {
                    font = FontManager.getCompositeFontUIResource(font);
                }
                return font;

            }
            return super.configureValue(value);
        }
    }


    /**
     * DesktopProperty for fonts that only gets sizes from the desktop,
     * font name and style are passed into the constructor
     */
    private static class WindowsFontSizeProperty extends DesktopProperty {
        private String fontName;
        private int fontSize;
        private int fontStyle;

        WindowsFontSizeProperty(String key, Toolkit toolkit, String fontName,
                                int fontStyle, int fontSize) {
            super(key, null, toolkit);
            this.fontName = fontName;
            this.fontSize = fontSize;
            this.fontStyle = fontStyle;
        }

        protected Object configureValue(Object value) {
            if (value == null) {
                value = new FontUIResource(fontName, fontStyle, fontSize);
            }
            else if (value instanceof Integer) {
                value = new FontUIResource(fontName, fontStyle,
                                           ((Integer)value).intValue());
            }
            return value;
        }
    }


    /**
     * A value wrapper that actively retrieves values from xp or falls back
     * to the classic value if not running XP styles.
     */ 
    private static class XPValue implements UIDefaults.ActiveValue {
	protected Object classicValue, xpValue;
        
        // A constant that lets you specify null when using XP styles.
        private final static Object NULL_VALUE = new Object();

	XPValue(Object xpValue, Object classicValue) {
	    this.xpValue = xpValue;
	    this.classicValue = classicValue;
	}

	public Object createValue(UIDefaults table) {
	    Object value = null;
	    if (XPStyle.getXP() != null) {
		value = getXPValue(table);
	    }
            
            if (value == null) {
                value = getClassicValue(table);
            } else if (value == NULL_VALUE) {
                value = null;
            }
            
	    return value;
	}

	protected Object getXPValue(UIDefaults table) {
	    return recursiveCreateValue(xpValue, table);
	}

	protected Object getClassicValue(UIDefaults table) {
	    return recursiveCreateValue(classicValue, table);
	}

	private Object recursiveCreateValue(Object value, UIDefaults table) {
	    if (value instanceof UIDefaults.LazyValue) {
		value = ((UIDefaults.LazyValue)value).createValue(table);
	    }
	    if (value instanceof UIDefaults.ActiveValue) {
		return ((UIDefaults.ActiveValue)value).createValue(table);
	    } else {
		return value;
	    }
	}
    }

    private static class XPBorderValue extends XPValue {
        private final Border extraMargin;

	XPBorderValue(Part xpValue, Object classicValue) {
            this(xpValue, classicValue, null);
	}

        XPBorderValue(Part xpValue, Object classicValue, Border extraMargin) {
            super(xpValue, classicValue);
            this.extraMargin = extraMargin;
        }

	public Object getXPValue(UIDefaults table) {
            Border xpBorder = XPStyle.getXP().getBorder(null, (Part)xpValue);
            if (extraMargin != null) {
                return new BorderUIResource.
                        CompoundBorderUIResource(xpBorder, extraMargin);
            } else {
                return xpBorder;
            }
	}
    }

    private static class XPColorValue extends XPValue {
	XPColorValue(Part part, State state, Prop prop, Object classicValue) {
	    super(new XPColorValueKey(part, state, prop), classicValue);
	}

	public Object getXPValue(UIDefaults table) {
	    XPColorValueKey key = (XPColorValueKey)xpValue;
	    return XPStyle.getXP().getColor(key.skin, key.prop, null);
	}

	private static class XPColorValueKey {
	    Skin skin;
	    Prop prop;

	    XPColorValueKey(Part part, State state, Prop prop) {
		this.skin = new Skin(part, state);
		this.prop = prop;
	    }
	}
    }
    
    private class XPDLUValue extends XPValue {
        private int direction;

        XPDLUValue(int xpdlu, int classicdlu, int direction) {
            super(new Integer(xpdlu), new Integer(classicdlu));
            this.direction = direction;
        }
        
        public Object getXPValue(UIDefaults table) {
            int px = dluToPixels(((Integer)xpValue).intValue(), direction);
            return new Integer(px);
        }
        
        public Object getClassicValue(UIDefaults table) {
            int px = dluToPixels(((Integer)classicValue).intValue(), direction);
            return new Integer(px);
        }
    }

    private class TriggerDesktopProperty extends DesktopProperty {
	TriggerDesktopProperty(String key) {
	    super(key, null, toolkit);
	    // This call adds a property change listener for the property,
	    // which triggers a call to updateUI(). The value returned
	    // is not interesting here.
	    getValueFromDesktop();
	}

	protected void updateUI() {
	    super.updateUI();

	    // Make sure property change listener is readded each time
	    getValueFromDesktop();
	}
    }

    private class FontDesktopProperty extends TriggerDesktopProperty {
	FontDesktopProperty(String key) {
	    super(key);
	}

	protected void updateUI() {
            Object aaTextInfo = SwingUtilities2.AATextInfo.getAATextInfo(true);
	    UIDefaults defaults = UIManager.getLookAndFeelDefaults();
	    defaults.put(SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);
	    super.updateUI();
	}
    }

    // Windows LayoutStyle.  From:
    // http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dnwue/html/ch14e.asp
    private class WindowsLayoutStyle extends DefaultLayoutStyle {
        @Override
        public int getPreferredGap(JComponent component1,
                JComponent component2, ComponentPlacement type, int position,
                Container parent) {
            // Checks args
            super.getPreferredGap(component1, component2, type, position,
                                  parent);

            switch(type) {
            case INDENT:
                // Windows doesn't spec this
                if (position == SwingConstants.EAST ||
                        position == SwingConstants.WEST) {
                    int indent = getIndent(component1, position);
                    if (indent > 0) {
                        return indent;
                    }
                    return 10;
                }
                // Fall through to related.
            case RELATED:
                if (isLabelAndNonlabel(component1, component2, position)) {
                    // Between text labels and their associated controls (for
                    // example, text boxes and list boxes): 3
                    // NOTE: We're not honoring:
                    // 'Text label beside a button 3 down from the top of
                    // the button,' but I suspect that is an attempt to
                    // enforce a baseline layout which will be handled
                    // separately.  In order to enforce this we would need
                    // this API to return a more complicated type (Insets,
                    // or something else).
                    return getButtonGap(component1, component2, position,
                                        dluToPixels(3, position));
                }
                // Between related controls: 4
                return getButtonGap(component1, component2, position,
                                    dluToPixels(4, position));
            case UNRELATED:
                // Between unrelated controls: 7
                return getButtonGap(component1, component2, position,
                                    dluToPixels(7, position));
            }
            return 0;
        }

        @Override
        public int getContainerGap(JComponent component, int position,
                                   Container parent) {
            // Checks args
            super.getContainerGap(component, position, parent);
            return getButtonGap(component, position, dluToPixels(7, position));
        }

    }

    /**
     * Converts the dialog unit argument to pixels along the specified
     * axis.
     */
    private int dluToPixels(int dlu, int direction) {
        if (baseUnitX == 0) {
            calculateBaseUnits();
        }
        if (direction == SwingConstants.EAST ||
            direction == SwingConstants.WEST) {
            return dlu * baseUnitX / 4;
        }
        assert (direction == SwingConstants.NORTH ||
                direction == SwingConstants.SOUTH);
        return dlu * baseUnitY / 8;
    }

    /**
     * Calculates the dialog unit mapping.
     */
    private void calculateBaseUnits() {
        // This calculation comes from:
        // http://support.microsoft.com/default.aspx?scid=kb;EN-US;125681
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(
                UIManager.getFont("Button.font"));
        baseUnitX = metrics.stringWidth(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        baseUnitX = (baseUnitX / 26 + 1) / 2;
        // The -1 comes from experimentation.
        baseUnitY = metrics.getAscent() + metrics.getDescent() - 1;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.6
     */ 
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        // if the component has a HI_RES_DISABLED_ICON_CLIENT_KEY
        // client property set to Boolean.TRUE, then use the new
        // hi res algorithm for creating the disabled icon (used
        // in particular by the WindowsFileChooserUI class)
        if (icon != null 
                && component != null
                && Boolean.TRUE.equals(component.getClientProperty(HI_RES_DISABLED_ICON_CLIENT_KEY))
                && icon.getIconWidth() > 0
                && icon.getIconHeight() > 0) {
            BufferedImage img = new BufferedImage(icon.getIconWidth(), 
                    icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(component, img.getGraphics(), 0, 0);
            ImageFilter filter = new RGBGrayFilter();
            ImageProducer producer = new FilteredImageSource(img.getSource(), filter);
            Image resultImage = component.createImage(producer);
            return new ImageIconUIResource(resultImage);
        }
        return super.getDisabledIcon(component, icon);
    }
    
    private static class RGBGrayFilter extends RGBImageFilter {
        public RGBGrayFilter() {
            canFilterIndexColorModel = true;
        }
        public int filterRGB(int x, int y, int rgb) {
            // find the average of red, green, and blue
            float avg = (((rgb >> 16) & 0xff) / 255f +
                          ((rgb >>  8) & 0xff) / 255f +
                           (rgb        & 0xff) / 255f) / 3;
            // pull out the alpha channel
            float alpha = (((rgb>>24)&0xff)/255f);
            // calc the average
            avg = Math.min(1.0f, (1f-avg)/(100.0f/35.0f) + avg);
            // turn back into rgb
            int rgbval = (int)(alpha * 255f) << 24 |
                         (int)(avg   * 255f) << 16 |
                         (int)(avg   * 255f) <<  8 |
                         (int)(avg   * 255f);
            return rgbval;
        }
    } 

}
