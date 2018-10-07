/* UnaryOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: UnaryOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class UnaryOperator extends Operator {
    public UnaryOperator(Type type, int op) {
        super(type, op);
	initOperands(1);
    }
    
    public int getPriority() {
        return 700;
    }

    public Expression negate() {
        if (getOperatorIndex() == LOG_NOT_OP) {
	    if (subExpressions != null)
		return subExpressions[0];
	    else
		return new NopOperator(Type.tBoolean);
        }
	return super.negate();
    }

    public void updateSubTypes() {
        subExpressions[0].setType(Type.tSubType(type));
    }

    public void updateType() {
	updateParentType(Type.tSuperType(subExpressions[0].getType()));
    }

    public boolean opEquals(Operator o) {
	return (o instanceof UnaryOperator)
	    && o.operatorIndex == operatorIndex;
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	writer.print(getOperatorString());
	writer.printOptionalSpace();
	subExpressions[0].dumpExpression(writer, 700);
    }
}
