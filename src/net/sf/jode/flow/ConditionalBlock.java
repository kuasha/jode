/* ConditionalBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ConditionalBlock.java 1412 2012-03-01 22:52:08Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.TabbedPrintWriter;
import net.sf.jode.expr.Expression;

/**
 * An ConditionalBlock is the structured block representing an if
 * instruction.  The else part may be null.
 */
public class ConditionalBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before instr is executed.
     */
    VariableStack stack;

    EmptyBlock trueBlock;
    
    public void checkConsistent() {
        super.checkConsistent();
    }

    /**
     * Creates a new if conditional block.
     */
    public ConditionalBlock(Expression cond) {
        super(cond);
        /* cond is a CompareBinary or CompareUnary operator, so no
         * check for LocalVarOperator (for condJump) is needed here.  
         */
        trueBlock = new EmptyBlock();
        trueBlock.outer = this;
    }

    /* The implementation of getNext[Flow]Block is the standard
     * implementation 
     */

    /**
     * Sets the successors of this structured block.  This should be only
     * called once, by FlowBlock.setSuccessors().
     */
    public void setSuccessors(Jump[] jumps) {
	if (jumps.length != 2) {
	    /* A conditional block can only exactly two jumps. */
	    throw new IllegalArgumentException("Not exactly two jumps.");
	}
        trueBlock.setJump(jumps[0]);
	setJump(jumps[1]);
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { trueBlock };
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        throw new InternalError("replaceSubBlock on ConditionalBlock");
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack;
	int params = instr.getFreeOperandCount();
	if (params > 0) {
	    this.stack = stack.peek(params);
	    newStack = stack.pop(params);
	} else 
	    newStack = stack;

	trueBlock.jump.stackMap = newStack;
	if (jump != null) {
	    jump.stackMap = newStack;
	    return null;
	}
	return newStack;
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr);
	trueBlock.removePush();
    }

    /**
     * Print the source code for this structured block.  
     */
    public void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        writer.print("IF (");
	instr.dumpExpression(TabbedPrintWriter.EXPL_PAREN, writer);
	writer.println(")");
        writer.tab();
        trueBlock.dumpSource(writer);
        writer.untab();
    }

    public boolean doTransformations() {
        StructuredBlock last =  flowBlock.lastModified;
        return super.doTransformations()
            || CombineIfGotoExpressions.transform(this, last)
            || CreateIfThenElseOperator.createFunny(this, last);
    }
}

