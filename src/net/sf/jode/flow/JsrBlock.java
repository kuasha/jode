/* JsrBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: JsrBlock.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.LocalInfo;
import net.sf.jode.type.Type;

/** 
 * This block represents a jsr instruction.  A jsr instruction is
 * used to call the finally block, or to call the monitorexit block in
 * a synchronized block.
 *
 * @author Jochen Hoenicke
 */
public class JsrBlock extends StructuredBlock {
    /**
     * The inner block that jumps to the subroutine.
     */
    StructuredBlock innerBlock;
    boolean good = false;

    public JsrBlock() {
	innerBlock = new EmptyBlock();
	innerBlock.outer = this;
    }

    public void setGood(boolean g) {
	good = g;
    }

    public boolean isGood() {
	return good;
    }

    /**
     * Sets the successors of this structured block.  This should be only
     * called once, by FlowBlock.setSuccessors().
     */
    public void setSuccessors(Jump[] jumps) {
	if (jumps.length != 2) {
	    /* A conditional block can only exactly two jumps. */
	    throw new IllegalArgumentException("Not exactly two jumps.");
	}
        innerBlock.setJump(jumps[0]);
	setJump(jumps[1]);
    }

    /* The implementation of getNext[Flow]Block is the standard
     * implementation */

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
				   StructuredBlock newBlock) {
        if (innerBlock == oldBlock)
            innerBlock = newBlock;
        else
            return false;
        return true;
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct). <p>
     * The default implementation merges the stack after each sub block.
     * This may not be, what you want. <p>
     *
     * @param initialStack the stackmap at begin of the block
     * @return the stack after the block has executed.
     * @throw RuntimeException if something did get wrong.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	/* There shouldn't be any JSR blocks remaining, but who knows.
	 */
	/* The innerBlock is startet with a new stack entry (return address)
	 * It should GOTO immediately and never complete.
	 */
	LocalInfo retAddr = new LocalInfo();
	retAddr.setType(Type.tUObject);
	innerBlock.mapStackToLocal(stack.push(retAddr));
	if (jump != null) {
	    jump.stackMap = stack;
	    return null;
	}
	return stack;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
	return new StructuredBlock[] { innerBlock };
    }

    public void dumpInstruction(net.sf.jode.decompiler.TabbedPrintWriter writer) 
        throws java.io.IOException 
    {
	writer.println("JSR");
	writer.tab();
	innerBlock.dumpSource(writer);
	writer.untab();
    }
}
