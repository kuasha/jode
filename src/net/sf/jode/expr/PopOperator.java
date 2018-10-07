/* PopOperator Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: PopOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class PopOperator extends Operator {

    Type popType;

    public PopOperator(Type argtype) {
        super(Type.tVoid, 0);
	popType = argtype;
	initOperands(1);
    }

    public int getPriority() {
        return 0;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(Type.tSubType(popType));
    }
    public void updateType() {
    }

    public int getBreakPenalty() {
	if (subExpressions[0] instanceof Operator)
	    return ((Operator) subExpressions[0]).getBreakPenalty();
	return 0;
    }
    
    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	/* Don't give a priority; we can't allow parents around
	 * a statement.
	 */
	subExpressions[0].dumpExpression(writer);
    }
}
