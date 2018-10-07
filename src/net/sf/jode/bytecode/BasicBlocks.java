/* BasicBlocks Copyright (C) 2000-2002 Jochen Hoenicke.
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
 * $Id: BasicBlocks.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.bytecode;

import net.sf.jode.GlobalOptions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.BitSet;
import java.util.Stack;
///#def COLLECTIONS java.util
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
///#enddef

/**
 * <p>Represents the byte code of a method in form of basic blocks.  A
 * basic block is a bunch of instructions, that must always execute in
 * sequential order.  Every basic block is represented by an Block
 * object.</p>
 *
 * <p>All jump instructions must be at the end of the block, and the
 * jump instructions doesn't have to remember where they jump to.
 * Instead this information is stored inside the blocks. See
 * <code>Block</code> for details.</p>
 *
 * <p>Exception Handlers are represented by the Handler class. Their
 * start/end range must span over some consecutive BasicBlocks and
 * there handler must be another basic block.</p>
 *
 * <!-- <p>Future work: A subroutine block, i.e. a block where some jsr
 * instructions may jump to, must store its return address in a local
 * variable immediately.  There must be exactly one block with the
 * corresponding <code>opc_ret</code> instruction and all blocks that
 * belong to this subroutine must point to the ret block.  Bytecode
 * that doesn't have this condition is automatically transformed on
 * reading.</p> -->
 *
 * <p>When the code is written to a class file, the blocks are written
 * in the given order.  Goto and return instructions are inserted as
 * necessary, you don't need to care about that.</p>
 *
 * <h3>Creating new BasicBlocks</h3>
 *
 * <p>If you want to create a new BasicBlocks object, first create the
 * Block objects, then initialize them (you need to have all successor
 * blocks created for this).  Afterwards create a new BasicBlock and
 * fill its sub blocks: </p>
 *
 * <pre>
 *   MethodInfo myMethod = new MethodInfo("foo", "()V", PUBLIC);
 *   Block blocks = new Block[10];
 *   for (int i = 0; i < 10; i++) blocks[i] = new Block();
 *   blocks[0].setCode(new Instruction[] {...}, 
 *                     new Block[] {blocks[3], blocks[1]});
 *   ...
 *   Handler[] excHandlers = new Handler[1];
 *   excHandlers[0] = new Handler(blocks[2], blocks[5], blocks[6],
 *                                "java.lang.NullPointerException");
 *   BasicBlocks bb = new BasicBlocks(myMethod);
 *   bb.setCode(blocks, blocks[0], excHandlers);
 *   classInfo.setMethods(new MethodInfo[] { myMethod });
 * </pre>
 *
 * @see net.sf.jode.bytecode.Block
 * @see net.sf.jode.bytecode.Instruction
 */
public class BasicBlocks extends BinaryInfo implements Opcodes {
    
    /**
     * The method info which contains the basic blocks.
     */
    private MethodInfo methodInfo;
    /**
     * The maximal number of stack entries, that may be used in this
     * method.  
     */
    int maxStack;
    /**
     * The maximal number of local slots, that may be used in this
     * method.  
     */
    int maxLocals;

    /**
     * This is an array of blocks, which are arrays
     * of Instructions.
     */
    private Block[] blocks;

    /**
     * The start block. Normally the first block, but differs if method start
     * with a goto, e.g a while.  This may be null, if this method is empty.
     */
    private Block startBlock;

    /**
     * The local variable infos for the method parameters.
     */
    private LocalVariableInfo[] paramInfos;

    /**
     * The array of exception handlers.
     */
    private Handler[] exceptionHandlers;

    public BasicBlocks(MethodInfo mi) {
	methodInfo = mi;
	int paramSize = (mi.isStatic() ? 0 : 1)
	    + TypeSignature.getParameterSize(mi.getType());
	paramInfos = new LocalVariableInfo[paramSize];
	for (int i=0; i< paramSize; i++)
	    paramInfos[i] = LocalVariableInfo.getInfo(i);
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public MethodInfo getMethodInfo() {
	return methodInfo;
    }

    public Block getStartBlock() {
	return startBlock;
    }
   
    public Block[] getBlocks() {
	return blocks;
    }

    /**
     * @return the exception handlers, or null if the method has no
     * exception handlers.
     */
    public Handler[] getExceptionHandlers() {
	return exceptionHandlers;
    }

    public LocalVariableInfo getParamInfo(int i) {
	return paramInfos[i];
    }

    public int getParamCount() {
	return paramInfos.length;
    }

    /**
     * Updates the maxStack and maxLocals according to the current code.
     * Call this every time you change the code.
     */
    public void updateMaxStackLocals() {
	maxLocals = getParamCount();
	maxStack = 0;

	if (startBlock == null)
	    return;

	BitSet visited = new BitSet();
	Stack todo = new Stack();

	startBlock.stackHeight = 0;
	todo.push(startBlock);
	while (!todo.isEmpty()) {
	    Block block = (Block) todo.pop();
	    int stackHeight = block.stackHeight;
	    if (stackHeight + block.maxpush > maxStack)
		maxStack = stackHeight + block.maxpush;
	    stackHeight += block.delta;

	    Block[] succs = block.getSuccs();
	    Instruction[] instr = block.getInstructions();
	    for (int i = 0; i < instr.length; i++) {
		if (instr[i].hasLocal()) {
		    int slotlimit = instr[i].getLocalSlot() + 1;
		    int opcode = instr[i].getOpcode();
		    if (opcode == opc_lstore || opcode == opc_dstore
			|| opcode == opc_lload || opcode == opc_dload)
			slotlimit++;
		    if (slotlimit > maxLocals)
			maxLocals = slotlimit;
		}
	    }
	    if (instr.length > 0 
		&& instr[instr.length-1].getOpcode() == opc_jsr) {
		if (!visited.get(succs[0].blockNr)) {
		    succs[0].stackHeight = stackHeight + 1;
		    todo.push(succs[0]);
		    visited.set(succs[0].blockNr);
		} else if (succs[0].stackHeight != stackHeight + 1)
		    throw new IllegalArgumentException
			("Block has two different stack heights.");

		if (succs[1] != null && !visited.get(succs[1].blockNr)) {
		    succs[1].stackHeight = stackHeight;
		    todo.push(succs[1]);
		    visited.set(succs[1].blockNr);
		} else if ((succs[1] == null ? 0 : succs[1].stackHeight)
			   != stackHeight)
		    throw new IllegalArgumentException
			("Block has two different stack heights.");
	    } else {
		for (int i = 0; i < succs.length; i++) {
		    if (succs[i] != null && !visited.get(succs[i].blockNr)) {
			succs[i].stackHeight = stackHeight;
			todo.push(succs[i]);
			visited.set(succs[i].blockNr);
		    } else if ((succs[i] == null ? 0 : succs[i].stackHeight)
			       != stackHeight)
			throw new IllegalArgumentException
			    ("Block has two different stack heights.");
		}
	    }
	    Handler[] handler = block.getHandlers();
	    for (int i = 0; i < handler.length; i++) {
		if (!visited.get(handler[i].getCatcher().blockNr)) {
		    handler[i].getCatcher().stackHeight = 1;
		    todo.push(handler[i].getCatcher());
		    visited.set(handler[i].getCatcher().blockNr);
		} else if (handler[i].getCatcher().stackHeight != 1)
		    throw new IllegalArgumentException
			("Block has two different stack heights.");
	    }
	}
    }

    public void setBlocks(Block[] blocks, Block startBlock, 
			  Handler[] handlers) {
	this.blocks = blocks;
	this.startBlock = startBlock;

	exceptionHandlers = handlers.length == 0 ? Handler.EMPTY : handlers;
	ArrayList activeHandlers = new ArrayList();
	for (int i = 0; i < blocks.length; i++) {
	    blocks[i].blockNr = i;
	    for (int j = 0; j < handlers.length; j++) {
		if (handlers[j].getStart() == blocks[i])
		    activeHandlers.add(handlers[j]);
	    }
	    if (activeHandlers.size() == 0)
		blocks[i].catchers = Handler.EMPTY;
	    else
		blocks[i].catchers = 
		    (Handler[]) activeHandlers.toArray(Handler.EMPTY);
	    for (int j = 0; j < handlers.length; j++) {
		if (handlers[j].getEnd() == blocks[i])
		    activeHandlers.remove(handlers[j]);
	    }
	}
	/* Check if all successor blocks are in this basic block */
	for (int i = 0; i < blocks.length; i++) {
	    Block[] succs = blocks[i].getSuccs();
	    for (int j = 0; j < succs.length; j++) {
		if (succs[j] != null
		    && succs[j] != blocks[succs[j].blockNr])
		    throw new IllegalArgumentException
			("Succ " + j + " of block " + i
			 + " not in basicblocks");
	    }
	}
	updateMaxStackLocals();
//  	TransformSubroutine.createSubroutineInfo(this);
    }

    /**
     * Sets the name and type of a method parameter. This overwrites
     * any previously set parameter info for this slot.
     * @param info a local variable info mapping a slot nr to a name
     * and a type.
     */
    public void setParamInfo(LocalVariableInfo info) {
	paramInfos[info.getSlot()] = info;
    }

    private BasicBlockReader reader;
    void read(ConstantPool cp, 
		     DataInputStream input, 
		     int howMuch) throws IOException {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_BYTECODE) != 0)
	    GlobalOptions.err.println("Reading "+methodInfo);
	reader = new BasicBlockReader(this);
	reader.readCode(cp, input);
	readAttributes(cp, input, howMuch);
  	reader.convert();
	reader = null;
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_BYTECODE) != 0)
	    dumpCode(GlobalOptions.err);
    }

    protected void readAttribute(String name, int length, ConstantPool cp,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	if (howMuch >= ClassInfo.ALMOSTALL
	    && name.equals("LocalVariableTable")) {
	    reader.readLVT(length, cp, input);
	} else if (howMuch >= ClassInfo.ALMOSTALL
		   && name.equals("LineNumberTable")) {
	    reader.readLNT(length, cp, input);
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }


    void reserveSmallConstants(GrowableConstantPool gcp) {
	for (int i=0; i < blocks.length; i++) {
	next_instr:
	    for (Iterator iter
		     = Arrays.asList(blocks[i].getInstructions()).iterator(); 
		 iter.hasNext(); ) {
		Instruction instr = (Instruction) iter.next();
		if (instr.getOpcode() == Opcodes.opc_ldc) {
		    Object constant = instr.getConstant();
		    if (constant == null)
			continue next_instr;
		    for (int j=1; j < Opcodes.constants.length; j++) {
			if (constant.equals(Opcodes.constants[j]))
			    continue next_instr;
		    }
		    if (constant instanceof Integer) {
			int value = ((Integer) constant).intValue();
			if (value >= Short.MIN_VALUE
			    && value <= Short.MAX_VALUE)
			    continue next_instr;
		    }
		    gcp.reserveConstant(constant);
		}
	    }
	}
    }

    BasicBlockWriter bbw;
    void prepareWriting(GrowableConstantPool gcp) {
	bbw = new BasicBlockWriter(this, gcp);
	prepareAttributes(gcp);
    }

    protected int getAttributeCount() {
	return super.getAttributeCount() + bbw.getAttributeCount();
    }

    protected void writeAttributes(GrowableConstantPool gcp,
				   DataOutputStream output)
	throws IOException {
	super.writeAttributes(gcp, output);
	bbw.writeAttributes(gcp, output);
    }

    void write(GrowableConstantPool gcp, 
	       DataOutputStream output) throws IOException {
	output.writeInt(bbw.getSize() + getAttributeSize());
	bbw.write(gcp, output);
	writeAttributes(gcp, output);
	bbw = null;
    }

    public void dumpCode(PrintWriter output) {
	output.println(methodInfo.getName()+methodInfo.getType()+":");
	if (startBlock == null)
	    output.println("\treturn");
	else if (startBlock != blocks[0])
	    output.println("\tgoto "+startBlock);

	for (int i=0; i< blocks.length; i++) {
	    blocks[i].dumpCode(output);
	}
	for (int i=0; i< exceptionHandlers.length; i++) {
	    output.println("catch " + exceptionHandlers[i].type 
			   + " from " + exceptionHandlers[i].start
			   + " to " + exceptionHandlers[i].end
			   + " catcher " + exceptionHandlers[i].catcher);
	}
    }

    public String toString() {
        return "BasicBlocks["+methodInfo+"]";
    }
}
