/* CreateCheckNull Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: CreateCheckNull.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.expr.*;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.LocalInfo;

public class CreateCheckNull {

    /* Situation:
     * 
     * javac: 
     *  DUP
     *  POP.getClass();
     *
     * jikes:
     *  DUP
     *  if (POP == null)
     *    throw null;
     */

    /**
     * Transforms the code
     * <pre>
     *   DUP
     *   POP.getClass()
     * </pre>
     * to a CheckNullOperator.  This is what javac generates when it
     * calls ".new" on an operand.
     */
    public static boolean transformJavac(InstructionContainer ic,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(ic.getInstruction() instanceof Operator)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	Operator ce = (Operator) ic.getInstruction();

	if (!(ce.getOperator() instanceof PopOperator)
	    || !(ce.getSubExpressions()[0] instanceof InvokeOperator))
	    return false;

        InvokeOperator getClassCall
	    = (InvokeOperator) ce.getSubExpressions()[0];
	if (!getClassCall.getMethodName().equals("getClass")
	    || !(getClassCall.getMethodType().toString()
		 .equals("()Ljava/lang/Class;")))
	    return false;

	LocalInfo li = new LocalInfo();
	ic.setInstruction(new CheckNullOperator(Type.tUObject, li));
	last.replace(last.outer);
        return true;
    }

    /**
     * Transforms the code
     * <pre>
     *   DUP
     *   if (POP == null) {
     *       throw null
     *   }
     * </pre>
     * to a CheckNullOperator.  This is what jikes generates when it
     * calls ".new" on an operand.
     */
    public static boolean transformJikes(IfThenElseBlock ifBlock,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock)
	    || ifBlock.elseBlock != null
	    || !(ifBlock.thenBlock instanceof ThrowBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	if (!(ifBlock.cond instanceof CompareUnaryOperator))
	    return false;
	CompareUnaryOperator cmpOp = (CompareUnaryOperator) ifBlock.cond;
	if (cmpOp.getOperatorIndex() != Operator.EQUALS_OP
	    || !(cmpOp.getCompareType().isOfType(Type.tUObject)))
	    return false;

	LocalInfo li = new LocalInfo();
	InstructionContainer ic = 
	    new InstructionBlock(new CheckNullOperator(Type.tUObject, li));
	ic.moveJump(ifBlock.jump);
	if (last == ifBlock) {
	    ic.replace(last.outer);
	    last = ic;
	} else {
	    ic.replace(ifBlock);
	    last.replace(last.outer);
	}
        return true;
    }
}
