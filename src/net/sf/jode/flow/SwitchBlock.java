/* SwitchBlock Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: SwitchBlock.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.flow;
import net.sf.jode.decompiler.TabbedPrintWriter;
import net.sf.jode.expr.Expression;

///#def COLLECTIONS java.util
import java.util.Arrays;
import java.util.Comparator;
///#enddef

/**
 * This is the structured block for an empty block.
 */
public class SwitchBlock extends InstructionContainer 
implements BreakableBlock {
    CaseBlock[] caseBlocks;
    VariableStack exprStack;
    VariableStack breakedStack;

    public SwitchBlock(Expression instr, int[] cases) {
	super(instr);
	this.caseBlocks = new CaseBlock[cases.length + 1];
	for (int i=0; i< cases.length; i++) {
	    caseBlocks[i] = new CaseBlock(cases[i]);
	    caseBlocks[i].outer = this;
	}
	caseBlocks[cases.length] = new CaseBlock(true);
	caseBlocks[cases.length].outer = this;
        isBreaked = false;
    }

    /**
     * Sets the successors of this structured block.  This should be only
     * called once, by FlowBlock.setSuccessors().
     */
    public void setSuccessors(Jump[] jumps) {
	if (jumps.length != caseBlocks.length) {
	    /* A conditional block can only exactly two jumps. */
	    throw new IllegalArgumentException("Wrong number of jumps.");
	}
	for (int i=0; i < caseBlocks.length; i++)
	    caseBlocks[i].subBlock.setJump(jumps[i]);
	doJumpTrafo();
    }

    public boolean doTransformations() {
	return super.doTransformations();
    }

    public void doJumpTrafo() {
	/* First remember the default destination */
	FlowBlock defaultDest
	    = caseBlocks[caseBlocks.length-1].subBlock.jump.destination;
	Comparator caseBlockComparator = new Comparator() {
	    public int compare(Object o1, Object o2) {
		CaseBlock c1 = (CaseBlock) o1;
		CaseBlock c2 = (CaseBlock) o2;
		int d1 = c1.subBlock.jump.destination.getBlockNr();
		int d2 = c2.subBlock.jump.destination.getBlockNr();
		if (d1 != d2)
		    return d1 - d2;
		if (c2.isDefault)
		    return -1;
		if (c1.isDefault)
		    return 1;
		if (c1.value < c2.value)
		    return -1;
		if (c1.value > c2.value)
		    return 1;
		return 0;
	    }
	};
	Arrays.sort(caseBlocks, caseBlockComparator);

	int newCases = 0;
	for (int i=0; i < caseBlocks.length; i++) {
	    Jump jump = caseBlocks[i].subBlock.jump;
	    if (i < caseBlocks.length - 1
		&& jump.destination
		== caseBlocks[i+1].subBlock.jump.destination) {
		// This case falls into the next one.
		caseBlocks[i].subBlock.removeJump();
		flowBlock.removeSuccessor(jump);

		if (caseBlocks[i+1].subBlock.jump.destination == defaultDest)
		    continue; // remove this case, it jumps to the default.
	    }
	    caseBlocks[newCases++] = caseBlocks[i];
	}
	caseBlocks[newCases-1].isLastBlock = true;

	CaseBlock[] newCaseBlocks = new CaseBlock[newCases];
	System.arraycopy(caseBlocks, 0, newCaseBlocks, 0, newCases);
	caseBlocks = newCaseBlocks;
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
	    exprStack = stack.peek(params);
	    newStack = stack.pop(params);
	} else 
	    newStack = stack;
	VariableStack lastStack = newStack;
	for (int i=0; i< caseBlocks.length; i++) {
	    if (lastStack != null)
		newStack.merge(lastStack);
	    lastStack = caseBlocks[i].mapStackToLocal(newStack);
	}
	if (lastStack != null)
	    mergeBreakedStack(lastStack);
	if (jump != null) {
	    jump.stackMap = breakedStack;
	    return null;
	}
	return breakedStack;
    }

    /**
     * Is called by BreakBlock, to tell us what the stack can be after a
     * break.
     */
    public void mergeBreakedStack(VariableStack stack) {
	if (breakedStack != null)
	    breakedStack.merge(stack);
	else
	    breakedStack = stack;
    }

    public void removePush() {
	if (exprStack != null)
	    instr = exprStack.mergeIntoExpression(instr);
	super.removePush();
    }

    /**
     * Find the case that jumps directly to destination.
     * @return The sub block of the case block, which jumps to destination.
     */
    public StructuredBlock findCase(FlowBlock destination) {
	for (int i=0; i < caseBlocks.length; i++) {
	    if (caseBlocks[i].subBlock != null
		&& caseBlocks[i].subBlock instanceof EmptyBlock
		&& caseBlocks[i].subBlock.jump != null
		&& caseBlocks[i].subBlock.jump.destination == destination)
		
		return caseBlocks[i].subBlock;
	}
	return null;
    }

    /**
     * Find the case that precedes the given case.
     * @param block The sub block of the case, whose predecessor should
     * be returned.
     * @return The sub block of the case precedes the given case.
     */
    public StructuredBlock prevCase(StructuredBlock block) {
	for (int i=caseBlocks.length-1; i>=0; i--) {
	    if (caseBlocks[i].subBlock == block) {
		for (i--; i>=0; i--) {
		    if (caseBlocks[i].subBlock != null)
			return caseBlocks[i].subBlock;
		}
	    }
	}
	return null;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
	for (int i=0; i< caseBlocks.length-1; i++) {
	    if (subBlock == caseBlocks[i]) {
		return caseBlocks[i+1];
	    }
	}
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
	for (int i=0; i< caseBlocks.length-1; i++) {
	    if (subBlock == caseBlocks[i]) {
		return null;
	    }
	}
        return getNextFlowBlock();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (label != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }
        writer.print("switch (");
	instr.dumpExpression(writer);
	writer.print(")");
	writer.openBrace();
	for (int i=0; i < caseBlocks.length; i++)
	    caseBlocks[i].dumpSource(writer);
	writer.closeBrace();
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return caseBlocks;
    }

    boolean isBreaked = false;

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "switch_"+(serialno++)+"_";
        return label;
    }

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked() {
	isBreaked = true;
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return !isBreaked 
            && (caseBlocks[caseBlocks.length-1].jump != null
                || caseBlocks[caseBlocks.length-1].jumpMayBeChanged());
    }
}
