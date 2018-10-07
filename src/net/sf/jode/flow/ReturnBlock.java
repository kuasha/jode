/* ReturnBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ReturnBlock.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.TabbedPrintWriter;
import net.sf.jode.expr.Expression;

/**
 * This is the structured block for a Return block.
 */
public class ReturnBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before instr is executed.
     */
    VariableStack stack;

    public ReturnBlock() {
	super(null);
    }

    public ReturnBlock(Expression instr) {
        super(instr);
    }

    public boolean jumpMayBeChanged() {
	return true;
    }

    public void checkConsistent() {
	super.checkConsistent();
	if (jump != null && jump.destination != FlowBlock.END_OF_METHOD)
	    throw new InternalError("Inconsistency");
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack = stack;
	if (instr != null) {
	    int params = instr.getFreeOperandCount();
	    if (params > 0) {
		this.stack = stack.peek(params);
		newStack = stack.pop(params);
	    }
	}
	return newStack;
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr);
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return declare != null && !declare.isEmpty();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.print("return");
	if (instr != null) {
	    writer.print(" ");
	    instr.dumpExpression(TabbedPrintWriter.IMPL_PAREN, writer);
	}
	writer.println(";");
    }
}
