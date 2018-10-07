/* IIncOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: IIncOperator.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class IIncOperator extends Operator 
    implements CombineableOperator {
    int value;

    public IIncOperator(LocalStoreOperator localStore, int value, 
			int operator) {
        super(Type.tVoid, operator);
	this.value = value;
	initOperands(1);
	setSubExpressions(0, localStore);
    }

    public LValueExpression getLValue() {
	return (LValueExpression) subExpressions[0];
    }

    public int getValue() {
	return value;
    }

    public int getPriority() {
        return 100;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(type != Type.tVoid ? type : Type.tInt);
    }


    public void updateType() {
	if (type != Type.tVoid)
	    updateParentType(subExpressions[0].getType());
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new InternalError("already non void");
        type = subExpressions[0].getType();
    }

    public boolean lvalueMatches(Operator loadop) {
	return getLValue().matches(loadop);
    }

    public Expression simplify() {
        if (value == 1) {
            int op = (getOperatorIndex() == OPASSIGN_OP+ADD_OP)
                ? INC_OP : DEC_OP;
            return new PrePostFixOperator
                (getType(), op, getLValue(), isVoid()).simplify();
        }
        return super.simplify();
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	subExpressions[0].dumpExpression(writer, 950);
	writer.print(getOperatorString() + value);
    }
}
