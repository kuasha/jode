/* CodeVerifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: CodeVerifier.java 1412 2012-03-01 22:52:08Z hoenicke $
 */

package net.sf.jode.jvm;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.BasicBlocks;
import net.sf.jode.bytecode.Block;
import net.sf.jode.bytecode.ClassPath;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.bytecode.Handler;
import net.sf.jode.bytecode.Instruction;
import net.sf.jode.bytecode.MethodInfo;
import net.sf.jode.bytecode.Opcodes;
import net.sf.jode.bytecode.Reference;
import net.sf.jode.bytecode.TypeSignature;

import java.io.IOException;
import java.util.BitSet;
///#def COLLECTIONS java.util
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
///#enddef

/**
 * Verifies a given method.
 * 
 * @author Jochen Hoenicke
 */
public class CodeVerifier implements Opcodes {
    ClassInfo ci;
    MethodInfo mi;
    BasicBlocks bb;
    ClassPath classpath;

    String methodType;

    Type returnType;
    Type tInt;
    Type tLong;
    Type tFloat;
    Type tDouble;
    Type tNone;
    Type tSecondPart;

    Type tString;
    Type tObject;

    Map typeHash = new HashMap();

    private Type tType(String typeSig) {
	Type type = (Type) typeHash.get(typeSig);
	if (type == null) {
	    int obj = typeSig.charAt(0) == 'N' ? 0 : typeSig.indexOf('L');
	    int semi = typeSig.indexOf(';');
	    if (obj != -1 && semi != -1) {
		String subTypeSig = typeSig.substring(0, obj+1)
		    + typeSig.substring(semi+1);
		String className
		    = typeSig.substring(obj+1, semi).replace('/', '.');
		ClassInfo classInfo = classpath.getClassInfo(className);
		type = new Type(subTypeSig, classInfo, null);
	    } else
		type = new Type(typeSig, null, null);
	    typeHash.put(typeSig, type);
	}
	return type;
    }

    private Type tType(Block jsrTarget) {
	Type type = (Type) typeHash.get(jsrTarget);
	if (type == null) {
	    type = new Type("R", null, jsrTarget);
	    typeHash.put(jsrTarget, type);
	}
	return type;
    }

    private Type tType(String head, ClassInfo classInfo) {
	String typeSig = head + classInfo.getName().replace('.', '/') + ';';
	Type type = (Type) typeHash.get(typeSig);
	if (type == null) {
	    type = new Type(head, classInfo, null);
	    typeHash.put(typeSig, type);
	}
	return type;
    }

    /**
     * We need some more types, than mentioned in jvm.
     */
    private static class Type {
	/* "ZBCSIFJD" are the normal primitive types.
	 * "V" stands for void type.
	 * "L" is normal class type, the class is in classInfo field.
	 * "[..." is normal array type
	 * "?" stands for type error
	 * "N" stands for the uninitialized this of a constructor,
	 * and classInfo is set.
	 * "Nxxx" stands for a new uninitialized type, where xxx is
	 *        a unique identifier for the new instruction
	 *        and classInfo is set.
	 * "0" stands for null type.
	 * "R" stands for return address type.
	 * "2" stands for second half of a two word type.
	 */
	private String typeSig;

        /* The classInfo if this is or contains a class type.
	 */
        private ClassInfo classInfo;

	/**
	 * The target block of the jsr if this is a "R" type.
	 */
	private Block jsrTarget;

	public Type(String typeSig, ClassInfo classInfo, Block jsrTarget) {
	    if ((typeSig.indexOf('L') >= 0 || typeSig.charAt(0) == 'N')
		&& classInfo == null)
		throw new IllegalArgumentException();
	    this.typeSig = typeSig;
	    this.classInfo = classInfo;
	    this.jsrTarget = jsrTarget;
	}

	public String getTypeSig() {
	    return typeSig;
	}
	
	public Block getJsrTarget() {
	    return jsrTarget;
	}

	/**
	 * @param t2 the type signature of the type to check for.
	 *   This may be one of the special signatures:
	 *   <dl><dt>"[*"<dt><dd>array of something</dd>
	 *   <dt>"+"</dt><dd>(uninitialized) object/returnvalue type</dd>
	 *   <dt>"L"</dt><dd>(initialized) object/returnvalue type</dd>
	 *   <dt>"2", "R"</dt> <dd>as the typeSig parameter </dd>
	 *   </dl>
	 * @return true, iff this is castable to t2 by a
	 * widening cast.  */
	public boolean isOfType(Type destType) {
	    String thisSig = typeSig;
	    String destSig = destType.typeSig;
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_VERIFIER) != 0)
		GlobalOptions.err.println("isOfType("+thisSig+","+destSig+")");
	    if (thisSig.equals(destSig))
		return true;
	    
	    char c1 = thisSig.charAt(0);
	    char c2 = destSig.charAt(0);
	    switch (c2) {
	    case 'Z': case 'B': case 'C': case 'S': case 'I':
		/* integer type */
		return ("ZBCSI".indexOf(c1) >= 0);
	    case '+':
		return ("L[nNR0".indexOf(c1) >= 0);

	    case '[':
		if (c1 == '0') 
		    return true;
		while (c1 == '[' && c2 == '[') {
		    thisSig = thisSig.substring(1);
		    destSig = destSig.substring(1);
		    c1 = thisSig.charAt(0);
		    c2 = destSig.charAt(0);
		}

		if (c2 == '*')
		    /* destType is array of unknowns */
		    return true;
		/* Note that short[] is only compatible to short[],
		 * therefore we only need to handle Object types specially.
		 */

		if (c2 != 'L')
		    return false;
	    case 'L':
		if (c1 == '0') 
		    return true;
		if ("L[".indexOf(c1) < 0)
		    return false;

		ClassInfo wantedType = destType.classInfo;
		if (wantedType == null
		    || wantedType.getName() == "java.lang.Object")
		    return true;

		try {
		    wantedType.load(ClassInfo.HIERARCHY);
		    if (wantedType.isInterface())
			return true;
		    if (c1 == 'L')
			return wantedType.superClassOf(classInfo);
		} catch (IOException ex) {
		    GlobalOptions.err.println
			("WARNING: Can't get full hierarchy of "
			 + wantedType + ".");
		    return true;
		}
	    case 'N':
		if (typeSig.charAt(0) != 'N')
		    return false;

		/* New types must match exactly ... */
		if (this.classInfo == destType.classInfo)
		    return true;

		/* ... except that a constructor can call the super
		 * constructor */
		if (typeSig.length() > 1)
		    return false;

		try {
		    classInfo.load(ClassInfo.HIERARCHY);
		    return (classInfo.getSuperclass() == destType.classInfo);
		} catch (IOException ex) {
		    /* ignore, type is maybe correct. */
		    return true;
		}
	    }
	    return false;
	}

	/**
	 * @return The common super type of this and type2.
	 */
	public Type mergeType(CodeVerifier cv, Type type2) {
	    String sig1 = typeSig;
	    String sig2 = type2.typeSig;
	    
	    if (this.equals(type2))
		return this;
	    
	    char c1 = sig1.charAt(0);
	    char c2 = sig2.charAt(0);
	    if (c1 == '*')
		return type2;
	    if (c2 == '*')
		return this;
	    if ("ZBCSI".indexOf(c1) >= 0 && "ZBCSI".indexOf(c2) >= 0)
		return this;
	    
	    if (c1 == '0')
		return ("L[0".indexOf(c2) >= 0) ? type2 : cv.tNone;
	    if (c2 == '0')
		return ("L[".indexOf(c1) >= 0) ? this : cv.tNone;


	    int dimensions = 0;
	    /* Note that short[] is only compatible to short[],
	     * therefore we make the array handling after the primitive
	     * type handling.  Also note that we don't allow arrays of 
	     * special types.
	     */
	    while (c1 == '[' && c2 == '[') {
		sig1 = sig1.substring(1);
		sig2 = sig2.substring(1);
		c1 = sig1.charAt(0);
		c2 = sig2.charAt(0);
		dimensions++;
	    }

	    // One of them is array now, the other is an object,
	    // the common super is tObject
	    if ((c1 == '[' && c2 == 'L')
		|| (c1 == 'L' && c2 == '[')) {
		if (dimensions == 0)
		    return cv.tObject;
		StringBuffer result = new StringBuffer(dimensions + 18);
		for (int i=0; i< dimensions; i++)
		    result.append("[");
		result.append("Ljava/lang/Object;");
		return cv.tType(result.toString());
	    }

	    if (c1 == 'L' && c2 == 'L') {
		ClassInfo clazz1 = classInfo;
		ClassInfo clazz2 = type2.classInfo;
		try {
		    if (clazz1.superClassOf(clazz2))
			return this;
		} catch (IOException ex) {
		    /* clazz1 has no complete hierarchy, we can assume
		     * that it extends class2.
		     */
		    return this;
		}
		try {
		    if (clazz2.superClassOf(clazz1))
			return type2;
		} catch (IOException ex) {
		    /* clazz1 has no complete hierarchy, we can assume
		     * that it extends class2.
		     */
		    return this;
		}
		/* Now the complete hierarchy of clazz1 and 
		 * clazz2 is loaded */
		try {
		    do {
			clazz1 = clazz1.getSuperclass();
		    } while (!clazz1.superClassOf(clazz2));
		} catch (IOException ex) {
		    throw new InternalError("Hierarchy vanished?");
		}
		StringBuffer result = new StringBuffer
		    (dimensions + clazz1.getName().length() + 2);
		for (int i=0; i< dimensions; i++)
		    result.append("[");
		result.append("L");
		return cv.tType(result.toString(), clazz1);
	    }

	    // Both were arrays, but of different primitive types.  The
	    // common super is tObject with one dimension less.
	    if (dimensions > 0) {
		if (dimensions == 1)
		    return cv.tObject;
		StringBuffer result = new StringBuffer(dimensions + 17);
		for (int i=0; i< dimensions - 1; i++)
		    result.append("[");
		result.append("Ljava/lang/Object;");
		return cv.tType(result.toString());
	    }
	    return cv.tNone;
	}

	public boolean equals(Object other) {
	    if (other instanceof Type) {
		Type type2 = (Type) other;
		return typeSig.equals(type2.typeSig)
		    && classInfo == type2.classInfo
		    && jsrTarget == type2.jsrTarget;
	    }
	    return false;
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer(typeSig);
	    if (classInfo != null)
		sb.append(classInfo.getName());
	    if (jsrTarget != null)
		sb.append(jsrTarget);
	    return sb.toString();
	}
    }

    /**
     * JLS 4.9.6: Verifying code that contains a finally clause:
     *  - Each instruction keeps track of the list of jsr targets.
     *  - For each instruction and each jsr needed to reach that instruction
     *    a bit vector is maintained of all local vars accessed or modified.
     */
    final class JsrUsedInfo {
	/**
	 * The last jsrTarget that must have been traversed.
	 */
	Block jsrTarget;

	/**
	 * The set of locals changed since the last jsrTarget.
	 */
	BitSet jsrUsed;

	JsrUsedInfo(Block jsrTarget, BitSet jsrUsed) {
	    this.jsrTarget = jsrTarget;
	    this.jsrUsed = jsrUsed;
	}

	JsrUsedInfo(JsrUsedInfo orig) {
	    this.jsrTarget = orig.jsrTarget;
	    this.jsrUsed = orig.jsrUsed;
	}

	public String toString() {
	    return ""+jsrTarget+'-'+jsrUsed;
	}
    }

    class SubroutineInfo {
	/**
	 * The previous used Info, null if this is the outermost jsr.
	 */
	JsrUsedInfo prevInfo;
	/**
	 * Block number of the return.
	 */
	Block retBlock = null;
	/**
	 * The VerifyInfo after the ret.
	 */
	VerifyInfo retInfo;
	/**
	 * The locals used in this subroutine
	 */
	BitSet usedLocals;
	/**
	 * The bitset containing the numbers of the blocks following 
	 * that may follow a JSR to this subroutine.
	 */
	BitSet jsrSuccessors = new BitSet();
    }

    /**
     * The VerifyInfo contains informations about the state of the stack
     * and local variables, as well as the current subroutine.
     *
     * We create a VerifyInfo for every reachable basic block in the
     * verifyInfos array.  For not yet reached basic blocks, that are
     * the successor of an jsr, it contains the state just _before_
     * the jsr.  If later a ret is found, it will take care to correct
     * that.
     *
     * We also have an intermediate VerifyInfo, which is modified
     * while we are "simulating" the instructions in a basic block.
     * After the basic block is fully simulated, we merge that temporary
     * VerifyInfo (cloning() it if necessary) and free it afterwards.
     *
     * Last but not least, we have a VerifyInfo in SubroutineInfo, that
     * records the local/stack state just after the ret.
     *
     * For information about typechecking jsrs:
     *
     * JLS 4.9.6: Verifying code that contains a finally clause:
     *  - Each instruction keeps track of the list of jsr targets.
     *  - For each instruction and each jsr needed to reach that instruction
     *    a bit vector is maintained of all local vars accessed or modified.
     *
     * The difficult part are subroutines inside a method (the jsr and
     * ret instructions).  We remember the last jsrtarget that must be
     * traversed to reach this block (a jsrTarget is a basic block to
     * which a jsr jumps).  Since a jsrTarget has a "R<myblockNr>"
     * type on the stack there is no possibility to reach a jsrTarget
     * on another way.  
     *
     * We only remember the innermost subroutine, but its SubroutineInfo
     * will contain the information about outer subroutines.
     *
     * If we change a local we remember it in jsrUsed.  This is
     * needed for the local merging on a ret as specified in the jvm
     * spec.  
     */
    final class VerifyInfo implements Cloneable {
	/**
	 * The jsr and used info for the innermost surrounding jsr.
	 * Normally this is null.
	 */
	JsrUsedInfo jsrInfo;

	/**
	 * The current stack height
	 */
	int stackHeight = 0;
	/**
	 * The types currently on the stack. The entries at indices 
	 * bigger or equal stackHeight are _undefined_.
	 * @see Type
	 */
	Type[] stack = new Type[bb.getMaxStack()];

	/**
	 * The types currently in local slots.  An entry is null, if
	 * the local may not be used.
	 * @see Type
	 */
	Type[] locals = new Type[bb.getMaxLocals()];

	public Object clone() {
	    try {
		VerifyInfo result = (VerifyInfo) super.clone();
		result.stack = (Type[]) stack.clone();
		result.locals = (Type[]) locals.clone();
		if (jsrInfo != null)
		    result.jsrInfo = new JsrUsedInfo(jsrInfo);
		return result;
	    } catch(CloneNotSupportedException ex) {
		throw new InternalError("Clone not supported?");
	    }
	}

	public final void reserve(int count) throws VerifyException {
	    if (stackHeight + count > stack.length)
		throw new VerifyException("stack overflow");
	}
	
	public final void need(int count) throws VerifyException {
	    if (stackHeight < count)
		throw new VerifyException("stack underflow");
	}
	
	public final void push(Type type) throws VerifyException {
	    reserve(1);
	    stack[stackHeight++] = type; 
	}
	
	public final Type pop() throws VerifyException {
	    need(1);
	    return stack[--stackHeight];
	}
	
	public String toString() {
	    StringBuffer result = new StringBuffer("locals:[");
	    String comma = "";
	    for (int i=0; i<locals.length; i++) {
		result.append(comma).append(i).append(':');
		result.append(locals[i]);
		comma = ",";
	    }
	    result.append("], stack:[");
	    comma = "";
	    for (int i=0; i<stackHeight; i++) {
		result.append(comma).append(stack[i]);
		comma = ",";
	    }
	    if (jsrInfo != null) {
		result.append("], jsrs:[").append(jsrInfo);
	    }
	    return result.append("]").toString();
	}
    }

    Type[] types;
    Type[] arrayTypes;

    VerifyInfo[] verifyInfos;
    VerifyInfo[] beforeJsrs;

    /**
     * For each jsr target (basic block to which a jsr jumps), this 
     * contains the subroutine info.
     */
    SubroutineInfo[] subInfos;

    public CodeVerifier(ClassInfo ci, MethodInfo mi, BasicBlocks bb) {
	this.ci = ci;
	this.mi = mi;
	this.bb = bb;
	this.classpath = ci.getClassPath();

	methodType = mi.getType();

	returnType  = tType(TypeSignature.getReturnType(methodType));
	tInt        = tType("I");
	tLong       = tType("J");
	tFloat      = tType("F");
	tDouble     = tType("D");
	tNone       = tType("?");
	tSecondPart = tType("2");
	tString     = tType("Ljava/lang/String;");
	tObject     = tType("Ljava/lang/Object;");
	types = new Type[] { 
	    tInt, tLong, tFloat, tDouble,
	    tType("+"), 
	    tType("B"), tType("C"), tType("S")
	};
	arrayTypes = new Type[] {
	    tType("[I"), tType("[J"), 
	    tType("[F"), tType("[D"),
	    tType("[Ljava/lang/Object;"),
	    tType("[B"), tType("[C"), tType("[S")
	};
    }

    private void dumpInfo(java.io.PrintWriter output) {
	Block[] blocks = bb.getBlocks();
	for (int i=0; i< blocks.length; i++) {
	    GlobalOptions.err.println("Block "+i+": "+verifyInfos[i]);
	    blocks[i].dumpCode(output);
	}
    }

    private VerifyInfo initInfo() throws VerifyException {
	VerifyInfo info = new VerifyInfo();
	int slot = 0;
	if (!mi.isStatic()) {
	    if (slot >= bb.getMaxLocals())
		throw new VerifyException("Too few local slots");
	    if (mi.getName().equals("<init>"))
		info.locals[slot++] = tType("N", ci);
	    else
		info.locals[slot++] = tType("L", ci);
	}

	String[] paramTypes = TypeSignature.getParameterTypes(methodType);
	for (int i = 0; i < paramTypes.length; i++) {
	    if (slot >= bb.getMaxLocals())
		throw new VerifyException("Too few local slots");
	    info.locals[slot++] = tType(paramTypes[i]);
	    if (TypeSignature.getTypeSize(paramTypes[i]) == 2) {
		if (slot >= bb.getMaxLocals())
		    throw new VerifyException("Too few local slots");
		info.locals[slot++] = tSecondPart;
	    }
	}
	while (slot < bb.getMaxLocals())
	    info.locals[slot++] = tNone;
	return info;
    }

    /**
     * Merges the second JsrUsedInfo into the first.  
     * @return true if first JsrUsedInfo changed in this merge, i.e.
     * got more specific.
     */
    private boolean mergeJsrTarget(JsrUsedInfo first, JsrUsedInfo second) {
	/* trivial cases first. */
	if (first.jsrTarget == second.jsrTarget)
	    return false;
	if (first.jsrTarget == null || second.jsrTarget == null)
	    return false;

	/* Now the bitsets can't be null */
	int firstDepth = 0;
	for (JsrUsedInfo t = first; t != null; 
	     t = subInfos[t.jsrTarget.getBlockNr()].prevInfo)
	    firstDepth++;
	int secondDepth = 0;
	for (JsrUsedInfo t = second; t != null; 
	     t = subInfos[t.jsrTarget.getBlockNr()].prevInfo)
	    secondDepth++;

	boolean changed = false;
	Block secondTarget = second.jsrTarget;
	while (firstDepth > secondDepth) {
	    JsrUsedInfo firstPrev
		= subInfos[first.jsrTarget.getBlockNr()].prevInfo;
	    if (firstPrev == null)
		first.jsrTarget = null;
	    else
		first.jsrTarget = firstPrev.jsrTarget;
	    firstDepth--;
	}
	while (secondDepth > firstDepth) {
	    changed = true;
	    JsrUsedInfo secondPrev
		= subInfos[secondTarget.getBlockNr()].prevInfo;
	    if (secondPrev != null) {
		first.jsrUsed.or(secondPrev.jsrUsed);
		secondTarget = secondPrev.jsrTarget;
	    } else
		secondTarget = null;
	    secondDepth--;
	}
	while (first.jsrTarget != secondTarget) {
	    changed = true;
	    JsrUsedInfo firstPrev
		= subInfos[first.jsrTarget.getBlockNr()].prevInfo;
	    if (firstPrev == null)
		first.jsrTarget = null;
	    else
		first.jsrTarget = firstPrev.jsrTarget;
	    JsrUsedInfo secondPrev
		= subInfos[secondTarget.getBlockNr()].prevInfo;
	    if (secondPrev != null) {
		first.jsrUsed.or(secondPrev.jsrUsed);
		secondTarget = secondPrev.jsrTarget;
	    } else
		secondTarget = null;
	}
	return changed;
    }

    private boolean mergeInfo(Block block, VerifyInfo info) 
	throws VerifyException {
	int blockNr = block.getBlockNr();
	if (verifyInfos[blockNr] == null) {
	    verifyInfos[blockNr] = (VerifyInfo) info.clone();
	    return true;
	}
	boolean changed = false;
	VerifyInfo oldInfo = verifyInfos[blockNr];
	if (oldInfo.stackHeight != info.stackHeight)
	    throw new VerifyException("Stack height differ at: " + blockNr);
	for (int i=0; i < oldInfo.stackHeight; i++) {
	    Type newType = oldInfo.stack[i].mergeType(this, info.stack[i]);
	    if (!newType.equals(oldInfo.stack[i])) {
		if (newType == tNone)
		    throw new VerifyException("Type error while merging: "
					      + oldInfo.stack[i]
					      + " and " + info.stack[i]);
		changed = true;
		oldInfo.stack[i] = newType;
	    }
	}
	for (int i=0; i < bb.getMaxLocals(); i++) {
	    Type newType = oldInfo.locals[i].mergeType(this, info.locals[i]);
	    if (!newType.equals(oldInfo.locals[i])) {
		changed = true;
		oldInfo.locals[i] = newType;
	    }
	}
	if (oldInfo.jsrInfo != null) {
	    if (info.jsrInfo == null) {
		oldInfo.jsrInfo = null;
		changed = true;
	    } else if (mergeJsrTarget(oldInfo.jsrInfo, info.jsrInfo)) {
		if (oldInfo.jsrInfo.jsrTarget == null)
		    oldInfo.jsrInfo = null;
		changed = true;
	    }
	}
	return changed;
    }

    private void modelEffect(Instruction instr, VerifyInfo info) 
	throws VerifyException {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_VERIFIER) != 0)
	    GlobalOptions.err.println(""+info+instr);
	int opcode = instr.getOpcode();
	switch (opcode) {
	case opc_nop:
	case opc_goto:
	    break;
	case opc_ldc: {
	    Type type;
	    Object constant = instr.getConstant();
	    if (constant == null)
		type = tType("0");
	    else if (constant instanceof Integer)
		type = tInt;
	    else if (constant instanceof Float)
		type = tFloat;
	    else
		type = tString;
	    info.push(type);
	    break;
	}
	case opc_ldc2_w: {
	    Type type;
	    Object constant = instr.getConstant();
	    if (constant instanceof Long)
		type = tLong;
	    else
		type = tDouble;
	    info.push(type);
	    info.push(tSecondPart);
	    break;
	}
	case opc_iload: 
	case opc_lload: 
	case opc_fload: 
	case opc_dload:
	case opc_aload: {
	    if (info.jsrInfo != null) {
		info.jsrInfo.jsrUsed.set(instr.getLocalSlot());
		if ((opcode & 0x1) == 0)
		    info.jsrInfo.jsrUsed.set(instr.getLocalSlot() + 1);
	    }
	    if ((opcode & 0x1) == 0
		&& info.locals[instr.getLocalSlot()+1] != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    Type type = info.locals[instr.getLocalSlot()];
	    if (!type.isOfType(types[opcode - opc_iload]))
		throw new VerifyException(instr.getDescription());
	    info.push(type);
	    if ((opcode & 0x1) == 0)
		info.push(tSecondPart);
	    break;
	}
	case opc_iaload: case opc_laload: 
	case opc_faload: case opc_daload: case opc_aaload:
	case opc_baload: case opc_caload: case opc_saload: {
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    Type arrType = info.pop(); 
	    if (!arrType.isOfType(arrayTypes[opcode - opc_iaload])
		&& (opcode != opc_baload
		    || !arrType.isOfType(tType("[Z"))))
		throw new VerifyException(instr.getDescription());
	    
	    String typeSig = arrType.getTypeSig();
	    Type elemType;
	    if (typeSig.charAt(0) == '[') {
		if (arrType.classInfo != null)
		    elemType = tType(typeSig.substring(1), arrType.classInfo);
		else
		    elemType = tType(typeSig.substring(1));
	    } else if(opcode == opc_aaload)
		elemType = tType("0");
	    else
		elemType = types[opcode - opc_iaload];
	    info.push(elemType);
	    if (((1 << opcode - opc_iaload) & 0xa) != 0)
		info.push(tSecondPart);
	    break;
	}
	case opc_istore: case opc_lstore: 
	case opc_fstore: case opc_dstore: case opc_astore: {
	    if (info.jsrInfo != null) {
		info.jsrInfo.jsrUsed.set(instr.getLocalSlot());
		if ((opcode & 0x1) != 0)
		    info.jsrInfo.jsrUsed.set(instr.getLocalSlot()+1);
	    }
	    if ((opcode & 0x1) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    Type type = info.pop();
	    if (!type.isOfType(types[opcode - opc_istore]))
		if (opcode != opc_astore || !type.isOfType(tType("R")))
		    throw new VerifyException(instr.getDescription());
	    info.locals[instr.getLocalSlot()] = type;
	    if ((opcode & 0x1) != 0)
		info.locals[instr.getLocalSlot()+1] = tSecondPart;
	    break;
	}
	case opc_iastore: case opc_lastore:
	case opc_fastore: case opc_dastore: case opc_aastore:
	case opc_bastore: case opc_castore: case opc_sastore: {
	    if (((1 << opcode - opc_iastore) & 0xa) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    Type type = info.pop();
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    Type arrType = info.pop();
	    if (!arrType.isOfType(arrayTypes[opcode - opc_iastore])
		&& (opcode != opc_bastore || !arrType.isOfType(tType("[Z"))))
		throw new VerifyException(instr.getDescription());
	    Type elemType = opcode >= opc_bastore ? tInt 
		: types[opcode - opc_iastore];
	    if (!type.isOfType(elemType))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_pop: case opc_pop2: {
	    int count = opcode - (opc_pop-1);
	    info.need(count);
	    info.stackHeight -= count;
	    break;
	}
	case opc_dup: case opc_dup_x1: case opc_dup_x2: {
	    int depth = opcode - opc_dup;
	    info.reserve(1);
	    info.need(depth+1);
	    if (info.stack[info.stackHeight-1] == tSecondPart)
		throw new VerifyException(instr.getDescription());
	    
	    int stackdepth = info.stackHeight - (depth + 1);
	    if (info.stack[stackdepth] == tSecondPart)
		throw new VerifyException(instr.getDescription()
					  + " on long or double");
	    for (int i=info.stackHeight; i > stackdepth; i--)
		info.stack[i] = info.stack[i-1];
	    info.stack[stackdepth] = info.stack[info.stackHeight++];
	    break;
	}
	case opc_dup2: case opc_dup2_x1: case opc_dup2_x2: {
	    int depth = opcode - opc_dup2;
	    info.reserve(2);
	    info.need(depth+2);
	    if (info.stack[info.stackHeight-2] == tSecondPart)
		throw new VerifyException(instr.getDescription()
					  + " on misaligned long or double");
	    int stacktop = info.stackHeight;
	    int stackdepth = stacktop - (depth + 2);
	    if (info.stack[stackdepth] == tSecondPart)
		throw new VerifyException(instr.getDescription()
					  + " on long or double");
	    for (int i=stacktop; i > stackdepth; i--)
		info.stack[i+1] = info.stack[i-1];
	    info.stack[stackdepth+1] = info.stack[stacktop+1];
	    info.stack[stackdepth] = info.stack[stacktop];
	    info.stackHeight+=2;
	    break;
	}
	case opc_swap: {
	    info.need(2);
	    if (info.stack[info.stackHeight-2] == tSecondPart
		|| info.stack[info.stackHeight-1] == tSecondPart)
		throw new VerifyException(instr.getDescription()
					  + " on misaligned long or double");
	    Type tmp = info.stack[info.stackHeight-1];
	    info.stack[info.stackHeight-1] = 
		info.stack[info.stackHeight-2];
	    info.stack[info.stackHeight-2] = tmp;
	    break;
	}
        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem: {
	    Type type = types[(opcode - opc_iadd) & 3];
	    if ((opcode & 1) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(type))
		throw new VerifyException(instr.getDescription());
	    if ((opcode & 1) != 0) {
		info.need(2);
		if (info.stack[info.stackHeight-1] != tSecondPart
		    || !info.stack[info.stackHeight-2].isOfType(type))
		    throw new VerifyException(instr.getDescription());
	    } else {
		info.need(1);
		if (!info.stack[info.stackHeight-1].isOfType(type))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;
	}
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg: {
	    Type type = types[(opcode - opc_ineg) & 3];
	    if ((opcode & 1) != 0) {
		info.need(2);
		if (info.stack[info.stackHeight-1] != tSecondPart
		    || !info.stack[info.stackHeight-2].isOfType(type))
		    throw new VerifyException(instr.getDescription());
	    } else {
		info.need(1);
		if (!info.stack[info.stackHeight-1].isOfType(type))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;
	}
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr:
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    
	    if ((opcode & 1) != 0) {
		info.need(2);
		if (info.stack[info.stackHeight-1] != tSecondPart ||
		    !info.stack[info.stackHeight-2].isOfType(tLong))
		    throw new VerifyException(instr.getDescription());
	    } else {
		info.need(1);
		if (!info.stack[info.stackHeight-1].isOfType(tInt))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;

        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor:
	    if ((opcode & 1) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(types[opcode & 1]))
		throw new VerifyException(instr.getDescription());
	    if ((opcode & 1) != 0) {
		info.need(2);
		if (info.stack[info.stackHeight-1] != tSecondPart
		    || !info.stack[info.stackHeight-2].isOfType(tLong))
		    throw new VerifyException(instr.getDescription());
	    } else {
		info.need(1);
		if (!info.stack[info.stackHeight-1].isOfType(tInt))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;

	case opc_iinc:
	    if (!info.locals[instr.getLocalSlot()].isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    break;
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
            int from = (opcode-opc_i2l)/3;
            int to   = (opcode-opc_i2l)%3;
            if (to >= from)
                to++;
	    if ((from & 1) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(types[from]))
		throw new VerifyException(instr.getDescription());
		
	    info.push(types[to]);
	    if ((to & 1) != 0)
		info.push(tSecondPart);
	    break;
	}
        case opc_i2b: case opc_i2c: case opc_i2s:
	    info.need(1);
	    if (!info.stack[info.stackHeight-1].isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_lcmp:
	    if (info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tLong))
		throw new VerifyException(instr.getDescription());
	    if (info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tLong))
		throw new VerifyException(instr.getDescription());
	    info.push(tInt);
	    break;
	case opc_dcmpl: case opc_dcmpg:
	    if (info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tDouble))
		throw new VerifyException(instr.getDescription());
	    if (info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tDouble))
		throw new VerifyException(instr.getDescription());
	    info.push(tInt);
	    break;
	case opc_fcmpl: case opc_fcmpg:
	    if (!info.pop().isOfType(tFloat))
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tFloat))
		throw new VerifyException(instr.getDescription());
	    info.push(tInt);
	    break;

	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_tableswitch:
	case opc_lookupswitch:
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tInt))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_if_acmpeq: case opc_if_acmpne:
	    if (!info.pop().isOfType(tType("+")))
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tType("+")))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_ifnull: case opc_ifnonnull:
	    if (!info.pop().isOfType(tType("+")))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_ireturn: case opc_lreturn: 
	case opc_freturn: case opc_dreturn: case opc_areturn: {
	    if (((1 << opcode - opc_ireturn) & 0xa) != 0
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    Type type = info.pop();
	    if (!type.isOfType(types[opcode - opc_ireturn])
		|| !type.isOfType(returnType))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_jsr:
	case opc_ret:
	    // handled in main loop
	    break;
	case opc_return:
	    if (!returnType.typeSig.equals("V"))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_getstatic: {
	    Reference ref = instr.getReference();
	    String type = ref.getType();
	    info.push(tType(type));
	    if (TypeSignature.getTypeSize(type) == 2)
		info.push(tSecondPart);
	    break;
	}
	case opc_getfield: {
	    Reference ref = instr.getReference();
	    Type classType = tType(ref.getClazz());
	    if (!info.pop().isOfType(classType))
		throw new VerifyException(instr.getDescription());
	    String type = ref.getType();
	    info.push(tType(type));
	    if (TypeSignature.getTypeSize(type) == 2)
		info.push(tSecondPart);
	    break;
	}
	case opc_putstatic: {
	    Reference ref = instr.getReference();
	    String type = ref.getType();
	    if (TypeSignature.getTypeSize(type) == 2
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tType(type)))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_putfield: {
	    Reference ref = instr.getReference();
	    String type = ref.getType();
	    if (TypeSignature.getTypeSize(type) == 2
		&& info.pop() != tSecondPart)
		throw new VerifyException(instr.getDescription());
	    if (!info.pop().isOfType(tType(type)))
		throw new VerifyException(instr.getDescription());
	    Type classType = tType(ref.getClazz());
	    Type classOnStack = info.pop();
	    if (!classOnStack.isOfType(classType)) {
		/* Sometimes synthetic code writes to uninitialized classes. */
		classType = tType("N" + ref.getClazz().substring(1));
		if (!classOnStack.isOfType(classType))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;
	}
	case opc_invokevirtual:
	case opc_invokespecial:
	case opc_invokestatic :
	case opc_invokeinterface: {
	    Reference ref = instr.getReference();
	    String refmt = ref.getType();
	    String[] paramTypes = TypeSignature.getParameterTypes(refmt);
	    for (int i=paramTypes.length - 1; i >= 0; i--) {
		if (TypeSignature.getTypeSize(paramTypes[i]) == 2
		    && info.pop() != tSecondPart)
		    throw new VerifyException(instr.getDescription());
		if (!info.pop().isOfType(tType(paramTypes[i])))
		    throw new VerifyException(instr.getDescription());
	    }
	    if (ref.getName().equals("<init>")) {
	        Type clazz = info.pop();
		String refClazzSig = ref.getClazz();
		Type refClazz = tType("N" + refClazzSig.substring(1));
		if (opcode != opc_invokespecial
		    || refClazzSig.charAt(0) != 'L'
		    || !clazz.isOfType(refClazz))
		    throw new VerifyException(instr.getDescription());
		Type newType = tType("L" + clazz.classInfo.getName()
				     .replace('.','/')+";");
		for (int i=0; i< info.stackHeight; i++)
		    if (info.stack[i] == clazz)
			info.stack[i] = newType;
		for (int i=0; i< info.locals.length; i++)
		    if (info.locals[i] == clazz)
			info.locals[i] = newType;
	    } else if (opcode != opc_invokestatic) {
		Type classType = tType(ref.getClazz());
		if (!info.pop().isOfType(classType))
		    throw new VerifyException(instr.getDescription());
	    }
	    String type = TypeSignature.getReturnType(refmt);
	    if (!type.equals("V")) {
		info.push(tType(type));
		if (TypeSignature.getTypeSize(type) == 2)
		    info.push(tSecondPart);
	    }
	    break;
	}
	case opc_new: {
	    String clName = instr.getClazzType();
	    info.stack[info.stackHeight++] = 
		tType("N" + clName.substring(1) + instr.hashCode());
	    break;
	}
	case opc_arraylength: {
	    if (!info.pop().isOfType(tType("[*")))
		throw new VerifyException(instr.getDescription());
	    info.push(tInt);
	    break;
	}
	case opc_athrow: {
	    if (!info.pop().isOfType(tType("Ljava/lang/Throwable;")))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_checkcast: {
	    Type classType = tType(instr.getClazzType());
	    if (!info.pop().isOfType(tType("+")))
		throw new VerifyException(instr.getDescription());
	    info.push(classType);
	    break;
	}
	case opc_instanceof: {
	    if (!info.pop().isOfType(tType("Ljava/lang/Object;")))
		throw new VerifyException(instr.getDescription());
	    info.push(tInt);
	    break;
	}
	case opc_monitorenter:
	case opc_monitorexit:
	    if (!info.pop().isOfType(tType("Ljava/lang/Object;")))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_multianewarray: {
	    int dimension = instr.getDimensions();
	    for (int i=dimension - 1; i >= 0; i--)
		if (!info.pop().isOfType(tInt))
		    throw new VerifyException(instr.getDescription());
	    Type classType = tType(instr.getClazzType());
	    info.push(classType);
	    break;
	}
	default:
	    throw new InternalError("Invalid opcode "+opcode);
	}
    }

    /* We manually program a bitset, since the best features are
     * missing in jdk 1.1.  
     */
    private class MyBitSet {
	int[] data;
	// This is always smaller than the first set bit.
	int   firstBit;
	
	public MyBitSet(int maxLength) {
	    data = new int[(maxLength + 31) / 32];
	    firstBit = 0;
	}

	public void set(int bit) {
	    data[bit >> 5] |= 1 << (bit & 0x1f);
	    if (bit < firstBit)
		firstBit = bit;
	}

	public void clear(int bit) {
	    data[bit >> 5] &= ~(1 << (bit & 0x1f));
	}

	public int findFirst() {
	    int first = firstBit >> 5;
	    while (data[first] == 0) {
		first++;
		firstBit = first << 5;
	    }
	    int bitmask = data[first] >> (firstBit & 0x1f);
	    while ((bitmask & 1) == 0) {
		bitmask >>= 1;
		firstBit++;
	    }
	    return firstBit;
	}

	public boolean isEmpty() {
	    for (int i = firstBit >> 5; i < data.length; i++) {
		if (data[i] != 0)
		    return false;
	    }
	    return true;
	}
    }
    
    private void doVerify() throws VerifyException {
	Block[] blocks = bb.getBlocks();
	int len = blocks.length;
	verifyInfos = new VerifyInfo[len];
	beforeJsrs = new VerifyInfo[len];
	subInfos = new SubroutineInfo[len];

	MyBitSet todoSet = new MyBitSet(blocks.length);
	Block firstBlock = bb.getStartBlock();
	if (firstBlock == null) {
	    /* empty method is okay */
	    return;
	}
	verifyInfos[firstBlock.getBlockNr()] = initInfo();
	todoSet.set(firstBlock.getBlockNr());
	while (!todoSet.isEmpty()) {
	    int blockNr = todoSet.findFirst();
	    todoSet.clear(blockNr);
	    Block block = blocks[blockNr];
	    VerifyInfo info = (VerifyInfo) verifyInfos[blockNr].clone();

	    Handler[] handlers = block.getHandlers();
	    if (handlers.length > 0) {
		VerifyInfo excInfo = (VerifyInfo) info.clone();
		excInfo.stackHeight = 1;		
		for (int i=0; i < handlers.length; i++) {
		    String type = handlers[i].getType();
		    if (type != null)
			excInfo.stack[0] = 
			    tType("L" + type.replace('.', '/') + ";");
		    else
			excInfo.stack[0]
			    = tType("Ljava/lang/Throwable;");
		    Block catcher = handlers[i].getCatcher();
		    if (mergeInfo(catcher, excInfo))
			todoSet.set(catcher.getBlockNr());
		}
	    }


	    Instruction instr = null;
	    Iterator iter = Arrays.asList(block.getInstructions()).iterator();
	    while (iter.hasNext()) {
		instr = (Instruction) iter.next();
		modelEffect(instr, info);

		if (handlers.length > 0 && instr.isStore()) {
		    for (int i=0; i < handlers.length; i++) {
			int slot = instr.getLocalSlot();
			Block catcher = handlers[i].getCatcher();
			int catcherNr = catcher.getBlockNr();
			VerifyInfo oldInfo = verifyInfos[catcherNr];
			Type newType = oldInfo.locals[slot]
			    .mergeType(this, info.locals[slot]);
			if (!newType.equals(oldInfo.locals[slot])) {
			    oldInfo.locals[slot] = newType;
			    todoSet.set(catcherNr);
			}
		    }
		}
	    }
	    
	    int opcode = instr.getOpcode();
	    if (opcode == opc_jsr) {
		Block jsrTarget = block.getSuccs()[0];
		Block nextBlock = block.getSuccs()[1];

		if (info.jsrInfo != null) {
		    // Check for recursive jsrs.
		    for (JsrUsedInfo jui = info.jsrInfo; 
			 jui != null;
			 jui = subInfos[jui.jsrTarget.getBlockNr()]
			     .prevInfo) {
			// Don't assume this is recursive, but assume
			// that the previous rets were left instead.
			//XXXXXXXXXXXXXXXXX
			if (jui.jsrTarget == jsrTarget) {
			    // This is a recursive jsr.  Or the previous
			    // invocation of the jsr terminated without a
			    // ret.  We forbid this!  XXX I think this too
			    // harsh, but doing it right is very difficult,
			    // so I stay on secure side.
			    throw new VerifyException("Recursive JSR!");
			}
		    }
		}

		// Create the VerifyInfo for the state after the jsr
		// is performed.
		VerifyInfo targetInfo = (VerifyInfo) info.clone();
		targetInfo.push(tType(jsrTarget));
		targetInfo.jsrInfo
		    = new JsrUsedInfo(jsrTarget, new BitSet());
		// Merge the target info
		if (mergeInfo(jsrTarget, targetInfo))
		    todoSet.set(jsrTarget.getBlockNr());

		SubroutineInfo subInfo = subInfos[jsrTarget.getBlockNr()];
		// Create the subroutine info if it doesn't yet exists.
		if (subInfo == null) {
		    subInfo = new SubroutineInfo();
		    subInfos[jsrTarget.getBlockNr()] = subInfo;
		    if (info.jsrInfo != null)
			subInfo.prevInfo = new JsrUsedInfo(info.jsrInfo);
		} else {
		    boolean changed;
		    if (info.jsrInfo != null) {
			changed = mergeJsrTarget
			    (subInfo.prevInfo, info.jsrInfo);
			if (subInfo.prevInfo.jsrTarget == null)
			    subInfo.prevInfo = null;
		    } else {
			subInfo.prevInfo = null;
			changed = true;
		    }
		    if (changed
			&& subInfos[jsrTarget.getBlockNr()].retBlock != null)
			todoSet.set(subInfos[jsrTarget.getBlockNr()]
				    .retBlock.getBlockNr());
		}

		if (nextBlock != null) {
		    // Add our successor to the successor list.
		    subInfo.jsrSuccessors.set(nextBlock.getBlockNr());

		    if (subInfo.retInfo != null) {
			// The jsr target already knows its return
			// instruction, we do the ret merging immediately
			VerifyInfo retInfo = subInfo.retInfo;
			info.stack = retInfo.stack;
			info.stackHeight = retInfo.stackHeight;
			if (subInfo.prevInfo != null)
			    info.jsrInfo = new JsrUsedInfo(subInfo.prevInfo);
			else
			    info.jsrInfo = null;
			BitSet usedLocals = subInfo.usedLocals;
			for (int j = 0; j < bb.getMaxLocals(); j++) {
			    if (usedLocals.get(j))
				info.locals[j] = retInfo.locals[j];
			}
			if (mergeInfo(nextBlock, info))
			    todoSet.set(nextBlock.getBlockNr());
		    } else {
			beforeJsrs[nextBlock.getBlockNr()] = info;
		    }
		} 
	    } else if (opcode == opc_ret) {
		Type retVarType = info.locals[instr.getLocalSlot()];
		if (info.jsrInfo == null || !retVarType.isOfType(tType("R")))
		    throw new VerifyException(instr.getDescription());
		Block jsrTarget = retVarType.getJsrTarget();
		BitSet usedLocals = info.jsrInfo.jsrUsed;
		for (Block lastTarget = info.jsrInfo.jsrTarget; 
		     jsrTarget != lastTarget;
		     lastTarget = subInfos[lastTarget.getBlockNr()]
			 .prevInfo.jsrTarget) {
		    if (lastTarget == null) 
			throw new VerifyException("returned to a leaved jsr");
		    usedLocals.or(subInfos[lastTarget.getBlockNr()]
				  .prevInfo.jsrUsed);
		}

		SubroutineInfo subInfo = subInfos[jsrTarget.getBlockNr()];
		if (subInfo.retBlock != null && subInfo.retBlock != block)
		    throw new VerifyException
			("JsrTarget has more than one ret: " + jsrTarget);
		subInfo.retInfo = info;
		subInfo.usedLocals = usedLocals;
		for (int i=0; i < blocks.length; i++) {
		    if (subInfo.jsrSuccessors.get(i)) {
			VerifyInfo afterJsrInfo;
			// If this was the first time, copy the info before
			// the jsr.
			if (subInfo.retBlock == null) {
			    afterJsrInfo = beforeJsrs[i];
			    // Check if the infos are mergeable,
			    // i.e. the items on the stack match.
			    // This isn't specified by the virtual
			    // machine specification, but it would be
			    // really bad, if we have to support such
			    // weird jsrs.  The decompiler doesn't
			    // like them!
			    if (afterJsrInfo.stackHeight
				!= info.stackHeight)
				throw new VerifyException
				    ("Stack height differ after jsr to: "
				     + jsrTarget);
			    for (int k = 0; k < info.stackHeight; k++) {
				if (info.stack[i].mergeType
				    (this, afterJsrInfo.stack[k]) == tNone)
				    throw new VerifyException
					("Type error while"+
					 " merging stacks after jsr");
			    }
			} else
			    afterJsrInfo = (VerifyInfo) verifyInfos[i].clone();

			afterJsrInfo.stack = info.stack;
			afterJsrInfo.stackHeight = info.stackHeight;
			afterJsrInfo.jsrInfo = subInfo.prevInfo;
			if (subInfo.prevInfo != null)
			    afterJsrInfo.jsrInfo
				= new JsrUsedInfo(subInfo.prevInfo);
			else
			    afterJsrInfo.jsrInfo = null;
			for (int j = 0; j < bb.getMaxLocals(); j++) {
			    if (usedLocals.get(j))
				afterJsrInfo.locals[j] = info.locals[j];
			}
			if (mergeInfo(blocks[i], afterJsrInfo))
			    todoSet.set(i);
		    }
		}
		subInfo.retBlock = block;
	    } else {
		Block[] succs = block.getSuccs();
		for (int i=0; i< succs.length; i++) {
		    Block succ = succs[i];
		    if (succ == null) {
			if (info.stackHeight != 0)
			    throw new VerifyException();
			continue;
		    }
		    
		    /* We don't need to check for uninitialized objects
		     * in back-branch.  The reason is the following:
		     * 
		     * An uninitialized object can't merge with anything
		     * else, so if this is really a back-branch to already
		     * analyzed code, the uninitialized object will simply
		     * vanish to unknown on the merge.
		     */
		    if (mergeInfo(succ, info))
			todoSet.set(succ.getBlockNr());
		}
	    }
	}
	
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_VERIFIER) != 0) {
	    dumpInfo(GlobalOptions.err);
	}
    }

    public void verify() throws VerifyException {
	try {
	    doVerify();
	} catch (VerifyException ex) {
	    dumpInfo(GlobalOptions.err);
	    throw ex;
	} catch (RuntimeException ex) {
	    dumpInfo(GlobalOptions.err);
	    throw ex;
	}
    }
}
