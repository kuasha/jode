/* LocalOptimizer Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: LocalOptimizer.java 1390 2005-07-25 17:14:53Z hoenicke $
 */

package net.sf.jode.obfuscator.modules;
import java.util.*;
import net.sf.jode.bytecode.*;
import net.sf.jode.obfuscator.*;
import net.sf.jode.GlobalOptions;

///#def COLLECTIONS java.util
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
///#enddef

/**
 * This class takes some bytecode and tries to minimize the number
 * of locals used.  It will also remove unnecessary stores. <br>
 *
 * This class can only work on verified code.  There should also be no
 * dead code, since the verifier doesn't check that dead code behaves
 * okay.   <br>
 *
 * This is done in two phases.  First we determine which locals are
 * the same, and which locals have a overlapping life time. In the
 * second phase we will then redistribute the locals with a coloring
 * graph algorithm. <br>
 *
 * The idea for the first phase is: For each read we follow the
 * instruction flow backward to find the corresponding writes.  We can
 * also merge with another control flow that has a different read, in
 * this case we merge with that read, too. <br>
 *
 * The tricky part is the subroutine handling.  We follow the local
 * that is used in a ret and find the corresponding jsr target (there
 * must be only one, if the verifier should accept this class).  While
 * we do this we remember in the info of the ret, which locals are
 * used in that subroutine. <br>
 *
 * When we know the jsr target<->ret correlation, we promote from the
 * nextByAddr of every jsr the locals that are accessed by the
 * subroutine to the corresponding ret and the others to the jsr.  Also
 * we will promote all reads from the jsr targets to the jsr. <br>
 *
 * If you think this might be to complicated, keep in mind that jsr's
 * are not only left by the ret instructions, but also "spontanously"
 * (by not reading the return address again). <br>
 */
public class LocalOptimizer implements Opcodes, CodeTransformer {

    /**
     * This class keeps track for each local variables:
     * <ul>
     *  <li>which name and type this local has 
     *       (if there is a local variable table),</li>
     *  <li>which other locals must be the same,</li>
     *  <li>which other locals have an intersecting life time.</li>
     * </ul>
     */
    class LocalInfo {
	LocalInfo shadow = null;

	public LocalInfo getReal() {
	    LocalInfo real = this;
	    while (real.shadow != null)
		real = real.shadow;
	    return real;
	}

	String name;
	String type;
	Vector conflictingLocals = new Vector();
	String id; // for debugging purposes only.
	int size;
	int newSlot = -1;

	LocalInfo() {
	}
	
	void conflictsWith(LocalInfo l) {
	    if (shadow != null) {
		getReal().conflictsWith(l);
	    } else {
		l = l.getReal();
		if (!conflictingLocals.contains(l)) {
		    conflictingLocals.addElement(l);
		    l.conflictingLocals.addElement(this);
		}
	    }
	}
	
	void combineInto(LocalInfo l) {
	    if (shadow != null) {
		getReal().combineInto(l);
		return;
	    }
	    l = l.getReal();
	    if (this == l)
		return;
	    shadow = l;
	    if (l.name == null) {
		l.name = name;
		l.type = type;
	    }
	    if (id.compareTo(l.id) < 0)
		l.id = id;
	}

	void generateID(int blockNr, int instrNr) {
	    char[] space = new char[5];
	    space[0] = (char) ('0' + blockNr / 10);
	    space[1] = (char) ('0' + blockNr % 10);
	    space[2] = (char) ('a' + instrNr / (26*26));
	    space[3] = (char) ('a' + (instrNr / 26) % 26);
	    space[4] = (char) ('a' + instrNr % 26);
	    id = new String(space);
	}

	public String toString() {
	    return id;
	}
    }

    private static class TodoQueue {
	BlockInfo first = null;
	BlockInfo last = null;

	public void add(BlockInfo info) {
	    if (info.nextTodo == null && info != last) {
		/* only enqueue if not already on queue */
		info.nextTodo = first;
		first = info;
		if (first == null)
		    last = info;
	    }
	}

	public boolean isEmpty() {
	    return first == null;
	}

	public BlockInfo remove() {
	    BlockInfo result = first;
	    first = result.nextTodo;
	    result.nextTodo = null;
	    if (first == null)
		last = null;
	    return result;
	}
    }

    BasicBlocks bb;

    TodoQueue changedInfos;
    InstrInfo firstInfo;
    LocalInfo[] paramLocals;
    BlockInfo[] blockInfos;
    int maxlocals;

    /**
     * This class contains information for each basic block.
     */
    class BlockInfo {
	/**
	 * The next Instruction in the todo list; null, if this block
	 * info is not on todo list, or if it is the last one.  
	 */
	BlockInfo nextTodo;

	/**
	 * The local infos for each Instruction. The index is the
	 * instruction number.
	 */
	LocalInfo[] instrLocals;

	/**
	 * The LocalInfo, whose values are read from a previous block.
	 * Index is the slot number.
	 */
	LocalInfo[] ins;

	/**
	 * The LocalInfo, written in this block.
	 * Index is the slot number.
	 */
	LocalInfo[] gens;

	/**
	 * The predecessors for this block.
	 */
	Collection preds = new ArrayList();

	/**
	 * The tryBlocks, for which this block is the catcher.
	 */
	Collection tryBlocks = new ArrayList();

	/**
	 * For each slot, this contains the InstrInfo of one of the
	 * next Instruction, that may read from that slot, without
	 * prior writing.  
	 */
	InstrInfo[] nextReads;

	/**
	 * This only has a value for blocks countaining a ret.  In
	 * that case this bitset contains all locals, that may be used
	 * between jsr and ret.  
	 */
	BitSet usedBySub;
	/**
	 * For each slot if get() is true, no instruction may read
	 * this slot, since it may contain different locals, depending
	 * on flow.  
	 */
	LocalInfo[] lifeLocals;
	/**
	 * If instruction is the destination of a jsr, this contains
	 * the single allowed ret instruction info, or null if there
	 * is no ret at all (or not yet detected).  
	 */
	InstrInfo retInfo;
	/**
	 * If this instruction is a ret, this contains the single
	 * allowed jsr target to which this ret belongs.  
	 */
	BlockInfo jsrTargetInfo;
	/**
	 * The underlying basic block.
	 */
	Block block;


	public BlockInfo(int blockNr, Block block) {
	    this.block = block;
	    ins = new LocalInfo[bb.getMaxLocals()];
	    gens = new LocalInfo[bb.getMaxLocals()];
	    Instruction[] instrs = block.getInstructions();
	    instrLocals = new LocalInfo[instrs.length];
	    for (int instrNr = 0; instrNr < instrs.length; instrNr++) {
		Instruction instr = instrs[instrNr];
		if (instr.hasLocal()) {
		    int slot = instr.getLocalSlot();
		    LocalInfo local = new LocalInfo();
		    instrLocals[instrNr] = local;
		    LocalVariableInfo lvi = instr.getLocalInfo();
		    local.name = lvi.getName();
		    local.type = lvi.getType();
		    local.size = 1;
		    local.generateID(blockNr, instrNr);
		    switch (instr.getOpcode()) {
		    case opc_lload: case opc_dload:
			local.size = 2;
			/* fall through */
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_iinc:
			/* this is a load instruction */
			if (gens[slot] == null) {
			    ins[slot] = local;
			    gens[slot] = local;
			    changedInfos.add(this);
			} else {
			    gens[slot].combineInto(local);
			}
			break;

		    case opc_ret:
			/* this is a ret instruction */
			usedBySub = new BitSet();
			if (gens[slot] == null) {
			    ins[slot] = local;
			    gens[slot] = local;
			    changedInfos.add(this);
			} else {
			    gens[slot].combineInto(local);
			}
			break;

		    case opc_lstore: case opc_dstore:
			local.size = 2;
			/* fall through */
		    case opc_istore: case opc_fstore: case opc_astore:
			gens[slot] = local;
			break;

		    default:
			throw new InternalError
			    ("Illegal opcode for SlotInstruction");
		    }
		}
	    }
	}

	
	void promoteIn(int slot, LocalInfo local) {
	    if (gens[slot] == null) {
		changedInfos.add(this);
		gens[slot] = local;
		ins[slot] = local;
	    } else {
		gens[slot].combineInto(local);
	    }
	}
	
	void promoteInForTry(int slot, LocalInfo local) {
	    if (ins[slot] == null) {
		gens[slot] = local;
		ins[slot] = local;
		changedInfos.add(this);
	    } else
		ins[slot].combineInto(local);

	    if (gens[slot] != null) {
		gens[slot].combineInto(local);
		for (int i=0; i< instrLocals.length; i++) {
		    if (instrLocals[i] != null
			&& block.getInstructions()[i].getLocalSlot() == slot)
			instrLocals[i].combineInto(local);
		}
	    }
	}
	
	public void promoteIns() {
	    for (int i=0; i < ins.length; i++) {
		if (ins[i] != null) {
		    for (Iterator iter = preds.iterator(); iter.hasNext();) {
			BlockInfo pred = (BlockInfo) iter.next();
			pred.promoteIn(i, ins[i]);
		    }

		    for (Iterator iter = tryBlocks.iterator(); 
			 iter.hasNext();) {
			BlockInfo pred = (BlockInfo) iter.next();
			pred.promoteInForTry(i, ins[i]);
		    }
		}
	    }
//  	    if (prevInstr.getOpcode() == opc_jsr) {
//  		/* Prev instr is a jsr, promote reads to the
//  		 * corresponding ret.
//  		 */
//  		InstrInfo jsrInfo = 
//  			(InstrInfo) instrInfos.get(prevInstr.getSingleSucc());
//  		if (jsrInfo.retInfo != null) {
//  		    /* Now promote reads that are modified by the
//  		     * subroutine to the ret, and those that are not
//  		     * to the jsr instruction.
//  		     */
//  		    promoteReads(info, jsrInfo.retInfo.instr,
//  				 jsrInfo.retInfo.usedBySub, false);
//  		    promoteReads(info, prevInstr, 
//  				 jsrInfo.retInfo.usedBySub, true);
//  		}
//  	    }
	}

	public void generateConflicts() {
	    LocalInfo[] active = (LocalInfo[]) ins.clone();
	    Instruction[] instrs = block.getInstructions();
	    for (int instrNr = 0; instrNr < instrs.length; instrNr++) {
		Instruction instr = instrs[instrNr];
		if (instr.isStore()) {
		    /* This is a store.  It conflicts with every local, which
		     * is active at this point.
		     */
		    for (int i=0; i < maxlocals; i++) {
			if (i != instr.getLocalSlot()
			    && active[i] != null)
			    instrLocals[instrNr].conflictsWith(active[i]);
			

			if (info.nextInfo.nextReads[i] != null
			    && info.nextInfo.nextReads[i].jsrTargetInfo != null) {
			    Instruction[] jsrs = info.nextInfo.nextReads[i]
				.jsrTargetInfo.instr.getPreds();
			    for (int j=0; j< jsrs.length; j++) {
				InstrInfo jsrInfo 
				    = (InstrInfo) instrInfos.get(jsrs[j]);
				for (int k=0; k < maxlocals; k++) {
				    if (!info.nextInfo.nextReads[i].usedBySub
					.get(k)
					&& jsrInfo.nextReads[k] != null)
					info.local.conflictsWith
					    (jsrInfo.nextReads[k].local);
				}
			    }
			}
		    }
		}
	    }
	}
    }
    
    public LocalOptimizer() {
    }


    /**
     * Merges the given vector to a new vector.  Both vectors may
     * be null in which case they are interpreted as empty vectors.
     * The vectors will never changed, but the result may be one
     * of the given vectors.
     */
    Vector merge(Vector v1, Vector v2) {
	if (v1 == null || v1.isEmpty())
	    return v2;
	if (v2 == null || v2.isEmpty())
	    return v1;
	Vector result = (Vector) v1.clone();
	Enumeration enumeration = v2.elements();
	while (enumeration.hasMoreElements()) {
	    Object elem = enumeration.nextElement();
	    if (!result.contains(elem))
		result.addElement(elem);
	}
	return result;
    }

    public void calcLocalInfo() {
	maxlocals = bb.getMaxLocals();
	Block[] blocks = bb.getBlocks();

	/* Initialize paramLocals */
	{
	    String methodType = bb.getMethodInfo().getType();
	    int paramCount = (bb.getMethodInfo().isStatic() ? 0 : 1)
		+ TypeSignature.getArgumentSize(methodType);
	    paramLocals = new LocalInfo[paramCount];
	    int slot = 0;
	    if (!bb.getMethodInfo().isStatic()) {
		LocalInfo local = new LocalInfo();
		LocalVariableInfo lvi = bb.getParamInfo(slot);
		local.type = "L" + (bb.getMethodInfo().getClazzInfo()
				    .getName().replace('.', '/'))+";";
		if (local.type.equals(lvi.getType()))
		    local.name = lvi.getName();
		local.size = 1;
		local.id = " this";
		paramLocals[slot++] = local;
	    }
	    int pos = 1;
	    while (pos < methodType.length()
		   && methodType.charAt(pos) != ')') {
		int start = pos;
		pos = TypeSignature.skipType(methodType, pos);

		LocalInfo local = new LocalInfo();
		LocalVariableInfo lvi = bb.getParamInfo(slot);
		local.type = methodType.substring(start, pos);
		if (local.type.equals(lvi.getType()))
		    local.name = lvi.getName();
		local.size = TypeSignature.getTypeSize(local.type);
		local.id = " parm";
		paramLocals[slot] = local;
		slot += local.size;
	    }
	}

	/* Initialize the InstrInfos and LocalInfos
	 */
	changedInfos = new TodoQueue();
	blockInfos = new BlockInfo[blocks.length];
	for (int i=0; i< blocks.length; i++)
	    blockInfos[i] = new BlockInfo(i, blocks[i]);

	for (int i=0; i< blocks.length; i++) {
	    int[] succs = blocks[i].getSuccs();
	    for (int j=0; j< succs.length; j++) {
		if (succs[j] >= 0)
		    blockInfos[succs[j]].preds.add(blockInfos[i]);
	    }
	    BasicBlocks.Handler[] handlers = blocks[i].getCatcher();
	    for (int j=0; j< handlers.length; j++) {
		blockInfos[handlers[j].getCatcher()]
		    .tryBlocks.add(blockInfos[i]);
	    }
	}

	/* find out which locals are the same.
	 */
	while (!changedInfos.isEmpty()) {
	    BlockInfo info = changedInfos.remove();
	    info.promoteIns();

	    if (instr.getPreds() != null) {
		for (int i = 0; i < instr.getPreds().length; i++) {
		    Instruction predInstr = instr.getPreds()[i];
		    if (instr.getPreds()[i].getOpcode() == opc_jsr) {
			/* This is the target of a jsr instr.
			 */
			if (info.instr.getOpcode() != opc_astore) {
			    /* XXX Grrr, the bytecode verifier doesn't
			     * test if a jsr starts with astore.  So
			     * it is possible to do something else
			     * before putting the ret address into a
			     * local.  */
			    throw new InternalError("Non standard jsr");
			}
			InstrInfo retInfo = info.nextInfo.nextReads
			    [info.instr.getLocalSlot()];

			if (retInfo != null) {
			    if (retInfo.instr.getOpcode() != opc_ret)
				throw new InternalError
				    ("reading return address");

			    info.retInfo = retInfo;
			    retInfo.jsrTargetInfo = info;

			    /* Now promote reads from the instruction
			     * after the jsr to the ret instruction if
			     * they are modified by the subroutine,
			     * and to the jsr instruction otherwise.  
			     */
			    Instruction nextInstr = predInstr.getNextByAddr();
			    InstrInfo nextInfo 
				= (InstrInfo) instrInfos.get(nextInstr);

			    promoteReads(nextInfo, retInfo.instr,
					 retInfo.usedBySub, false);

			    promoteReads(nextInfo, predInstr, 
					 retInfo.usedBySub, true);
			}
		    }
		    promoteReads(info, instr.getPreds()[i]);
		}
	    }
	}
	changedInfos = null;

	/* Now merge with the parameters
	 * The params should be the locals in firstInfo.nextReads
	 */
	int startBlock = bb.getStartBlock();
	if (startBlock >= 0) {
	    LocalInfo[] ins = blockInfos[startBlock].ins;
	    for (int i=0; i< paramLocals.length; i++) {
		if (ins[i] != null)
		    paramLocals[i].combineInto(ins[i]);
	    }
	}
    }

    public void stripLocals() {
	Block[] blocks = bb.getBlocks();
	for (int i = 0; i < blocks.length; i++) {
	    Instruction[] instrs = blocks[i].getInstructions();
	    for (int j = 0; j < instrs.length; j++) {
		Instruction instr = instrs[j];
		if (info.local != null
		    && info.local.usingBlocks.size() == 1) {
		    /* If this is a store, whose value is never read; it can
		     * be removed, i.e replaced by a pop. 
		     */
		    switch (instr.getOpcode()) {
		    case opc_istore:
		    case opc_fstore:
		    case opc_astore:
			instrs[j] = Instruction.forOpcode(opc_pop);
			break;
		    case opc_lstore:
		    case opc_dstore:
			instrs[j] = Instruction.forOpcode(opc_pop2);
			break;
		    default:
		    }
		}
	    }
	}
    }

    void distributeLocals(Vector locals) {
	if (locals.size() == 0)
	    return;

	/* Find the local with the least conflicts. */
	int min = Integer.MAX_VALUE;
	LocalInfo bestLocal = null;
	Enumeration enumeration = locals.elements();
	while (enumeration.hasMoreElements()) {
	    LocalInfo li = (LocalInfo) enumeration.nextElement();
	    int conflicts = 0;
	    Enumeration conflenum = li.conflictingLocals.elements();
	    while (conflenum.hasMoreElements()) {
		if (((LocalInfo)conflenum.nextElement()).newSlot != -2)
		    conflicts++;
	    }
	    if (conflicts < min) {
		min = conflicts;
		bestLocal = li;
	    }
	}
	/* Mark the local as taken */
	locals.removeElement(bestLocal);
	bestLocal.newSlot = -2;
	/* Now distribute the remaining locals recursively. */
	distributeLocals(locals);

	/* Finally find a new slot */
    next_slot:
	for (int slot = 0; ; slot++) {
	    Enumeration conflenum = bestLocal.conflictingLocals.elements();
	    while (conflenum.hasMoreElements()) {
		LocalInfo conflLocal = (LocalInfo)conflenum.nextElement();
		if (bestLocal.size == 2 && conflLocal.newSlot == slot+1) {
		    slot++;
		    continue next_slot;
		}
		if (conflLocal.size == 2 && conflLocal.newSlot+1 == slot)
		    continue next_slot;
		if (conflLocal.newSlot == slot) {
		    if (conflLocal.size == 2)
			slot++;
		    continue next_slot;
		}
	    }
	    bestLocal.newSlot = slot;
	    break;
	}
    }
    
    public void distributeLocals() {
	/* give locals new slots.  This is a graph coloring 
	 * algorithm (the optimal solution is NP complete, but this
	 * should be a good approximation).
	 */

	/* first give the params the same slot as they had before.
	 */
	for (int i=0; i<paramLocals.length; i++)
	    if (paramLocals[i] != null)
		paramLocals[i].newSlot = i;

	/* Now calculate the conflict settings.
	 */
	for (int blockNr = 0; blockNr < blockInfos.length; blockNr++) {
	    blockInfos[blockNr].generateConflicts();
	}

	/* Now put the locals that need a color into a vector.
	 */
	Vector locals = new Vector();
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null 
		&& info.local.newSlot == -1
		&& !locals.contains(info.local))
		locals.addElement(info.local);
	}

	/* Now distribute slots recursive.
	 */
	distributeLocals(locals);

	/* Update the instructions and calculate new maxlocals.
	 */
	maxlocals = paramLocals.length;
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null) {
		if (info.local.newSlot+info.local.size > maxlocals)
		    maxlocals = info.local.newSlot + info.local.size;
		info.instr.setLocalSlot(info.local.newSlot);
	    }
	}
	bc.setMaxLocals(maxlocals);
    }

    private InstrInfo CONFLICT = new InstrInfo();

    boolean promoteLifeLocals(LocalInfo[] newLife, InstrInfo nextInfo) {
	if (nextInfo.lifeLocals == null) {
	    nextInfo.lifeLocals = (LocalInfo[]) newLife.clone();
	    return true;
	}
	boolean changed = false;
	for (int i=0; i< maxlocals; i++) {
	    LocalInfo local = nextInfo.lifeLocals[i];
	    if (local == null)
		/* A conflict has already happened, or this slot
		 * may not have been initialized. */
		continue;

	    local = local.getReal();
	    LocalInfo newLocal = newLife[i];
	    if (newLocal != null)
		newLocal = newLocal.getReal();
	    if (local != newLocal) {
		nextInfo.lifeLocals[i] = null;
		changed = true;
	    }
	}
	return changed;
    }

    public void dumpLocals() {
	Vector locals = new Vector();
	for (int blockNr=0; blockNr < blockInfos.length; blockNr++) {
	    BlockInfo info = blockInfos[blockNr];
	    GlobalOptions.err.print("ins: [");
	    for (int i=0; i<maxlocals; i++) {
		if (info.ins[i] == null)
		    GlobalOptions.err.print("-----,");
		else
		    GlobalOptions.err.print(info.ins[i].toString()+",");
	    }
	    GlobalOptions.err.println("]");
	    blockInfos.block.dumpCode(GlobalOptions.err);
	    GlobalOptions.err.print("gens: [");
	    for (int i=0; i<maxlocals; i++) {
		if (info.ins[i] == null)
		    GlobalOptions.err.print("-----,");
		else
		    GlobalOptions.err.print(info.ins[i].toString()+",");
	    }
	    GlobalOptions.err.println("]");
	    if (info.usedBySub != null)
		GlobalOptions.err.println("  usedBySub: "+info.usedBySub);
	    if (info.retInfo != null)
		GlobalOptions.err.println("  ret info: "
					+info.retInfo.instr.getAddr());
	    if (info.jsrTargetInfo != null)
		GlobalOptions.err.println("  jsr info: "
					+info.jsrTargetInfo.instr.getAddr());
	    if (info.local != null && !locals.contains(info.local))
		locals.addElement(info.local);
	}
//  	Enumeration enumeration = locals.elements();
//  	while (enumeration.hasMoreElements()) {
//  	    LocalInfo li = (LocalInfo) enumeration.nextElement();
//  	    int slot = ((InstrInfo)li.usingInstrs.elementAt(0))
//  		.instr.getLocalSlot();
//  	    GlobalOptions.err.print("Slot: "+slot+" conflicts:");
//  	    Enumeration enum1 = li.conflictingLocals.elements();
//  	    while (enum1.hasMoreElements()) {
//  		LocalInfo cfl = (LocalInfo)enum1.nextElement();
//  		GlobalOptions.err.print(cfl.getAddr()+", ");
//  	    }
//  	    GlobalOptions.err.println();
//  	    GlobalOptions.err.print(li.getAddr());
//  	    GlobalOptions.err.print("     instrs: ");
//  	    Enumeration enum2 = li.usingInstrs.elements();
//  	    while (enum2.hasMoreElements())
//  		GlobalOptions.err.print(((InstrInfo)enum2.nextElement())
//  					.instr.getAddr()+", ");
//  	    GlobalOptions.err.println();
//  	}
//  	GlobalOptions.err.println("-----------");
    }

    public void transformCode(BasicBlocks bb) {
	this.bb = bb;
	calcLocalInfo();
	if ((GlobalOptions.debuggingFlags 
	     & GlobalOptions.DEBUG_LOCALS) != 0) {
	    GlobalOptions.err.println("Before Local Optimization: ");
	    dumpLocals();
	}
	stripLocals();
	distributeLocals();
	
	if ((GlobalOptions.debuggingFlags 
	     & GlobalOptions.DEBUG_LOCALS) != 0) {
	    GlobalOptions.err.println("After Local Optimization: ");
	    dumpLocals();
	}

	firstInfo = null;
	changedInfos = null;
	instrInfos = null;
	paramLocals = null;
    }
}
