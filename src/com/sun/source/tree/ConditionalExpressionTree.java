/*
 * @(#)ConditionalExpressionTree.java	1.3 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * Use and Distribution is subject to the Java Research License available
 * at <http://wwws.sun.com/software/communitysource/jrl.html>.
 */

package com.sun.source.tree;

/**
 * A tree node for the conditional operator ? :.
 *
 * For example:
 * <pre>
 *   <em>condition</em> ? <em>trueExpression</em> : <em>falseExpression</em>
 * </pre>
 *
 * @see "The Java Language Specification, 3rd ed, section 15.25"
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface ConditionalExpressionTree extends ExpressionTree {
    ExpressionTree getCondition();
    ExpressionTree getTrueExpression();
    ExpressionTree getFalseExpression();
}
