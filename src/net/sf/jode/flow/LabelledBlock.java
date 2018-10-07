/* LoopBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: LoopBlock.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.TabbedPrintWriter;

/**
 * This is the structured block for an Loop block.
 */
public class LabelledBlock extends StructuredBlock implements BreakableBlock {

    /**
     * The body of this labelled block.  This is always a valid block and not null.
     */
    StructuredBlock bodyBlock;

    /**
     * The stack after the break.
     */
    VariableStack breakedStack;

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    public LabelledBlock() {
    }

    public void setBody(StructuredBlock body) {
        bodyBlock = body;
        bodyBlock.outer = this;
        body.setFlowBlock(flowBlock);
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (bodyBlock != oldBlock)
            return false;
        
        bodyBlock = newBlock;
	newBlock.outer = this;
	oldBlock.outer = null;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { bodyBlock };
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        super.dumpSource(writer);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (label != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }
        boolean needBrace = bodyBlock.needsBraces();
	if (needBrace)
	    writer.openBrace();
	else
	    writer.println();
        writer.tab();
        bodyBlock.dumpSource(writer);
        writer.untab();
        writer.closeBrace();
    }

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "label_"+(serialno++);
        return label;
    }

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked() {
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct).
     * @param stack the stack at begin of the block
     * @return null if there is no way to the end of this block,
     * otherwise the stack after the block has executed.  
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack afterBody = bodyBlock.mapStackToLocal(stack);
	mergeBreakedStack(afterBody);
	return breakedStack;
    }

    /**
     * Is called by BreakBlock, to tell us what the stack can be after a
     * break.
     * @return false if the stack is inconsistent.
     */
    public void mergeBreakedStack(VariableStack stack) {
	if (breakedStack != null)
	    breakedStack.merge(stack);
	else
	    breakedStack = stack;
    }

    public void removePush() {
	bodyBlock.removePush();
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return false;
    }
}

