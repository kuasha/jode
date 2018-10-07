/* ArrayLoadOperator Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayLoadOperator.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.type.ArrayType;
import net.sf.jode.decompiler.TabbedPrintWriter;

public class ArrayLoadOperator extends Operator {

    public ArrayLoadOperator(Type type) {
        super(type, 0);
	initOperands(2);
    }

    public int getPriority() {
        return 950;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(Type.tSubType(Type.tArray(type)));
	subExpressions[1].setType(Type.tSubType(Type.tInt));
    }

    public void updateType() {
	Type subType = Type.tSuperType(subExpressions[0].getType())
	    .intersection(Type.tArray(type));
	if (!(subType instanceof ArrayType))
	    updateParentType(Type.tError);
	else 
	    updateParentType(((ArrayType)subType).getElementType());
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	subExpressions[0].dumpExpression(writer, 950);
	writer.breakOp();
	writer.print("[");
	subExpressions[1].dumpExpression(writer, 0);
	writer.print("]");
    }
}
