/* SyntheticAnalyzer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: SyntheticAnalyzer.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.jvm;
import net.sf.jode.bytecode.BasicBlocks;
import net.sf.jode.bytecode.Block;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.bytecode.FieldInfo;
import net.sf.jode.bytecode.Handler;
import net.sf.jode.bytecode.Instruction;
import net.sf.jode.bytecode.MethodInfo;
import net.sf.jode.bytecode.Opcodes;
import net.sf.jode.bytecode.Reference;
import net.sf.jode.bytecode.TypeSignature;
import net.sf.jode.type.Type;
import net.sf.jode.type.MethodType;

import java.lang.reflect.Modifier;
import java.io.IOException;

///#def COLLECTIONS java.util
import java.util.Arrays;
import java.util.Iterator;
///#enddef

public class SyntheticAnalyzer implements Opcodes {
    public final static int UNKNOWN = 0;
    public final static int GETCLASS = 1;
    public final static int ACCESSGETFIELD = 2;
    public final static int ACCESSPUTFIELD = 3;
    public final static int ACCESSMETHOD = 4;
    public final static int ACCESSGETSTATIC = 5;
    public final static int ACCESSPUTSTATIC = 6;
    public final static int ACCESSSTATICMETHOD = 7;
    public final static int ACCESSCONSTRUCTOR = 8;
    public final static int ACCESSDUPPUTFIELD = 9;
    public final static int ACCESSDUPPUTSTATIC = 10;
    
    int kind = UNKNOWN;

    int unifyParam = -1;
    Reference reference;
    ClassInfo classInfo;
    MethodInfo method;

    public SyntheticAnalyzer(ClassInfo classInfo, MethodInfo method, 
			     boolean checkName) {
	this.classInfo = classInfo;
	this.method = method;
	if (method.getBasicBlocks() == null)
	    return;
	if (!checkName || method.getName().equals("class$"))
	    if (checkGetClass())
		return;
	if (!checkName || method.getName().startsWith("access$"))
	    if (checkAccess())
		return;
	if (method.getName().equals("<init>"))
	    if (checkConstructorAccess())
		return;
    }

    public int getKind() {
	return kind;
    }

    public Reference getReference() {
	return reference;
    }

    /**
     * Gets the index of the dummy parameter for an ACCESSCONSTRUCTOR.
     * Normally the 1 but for inner classes it may be 2.
     */
    public int getUnifyParam() {
	return unifyParam;
    }

    private static final int[] getClassOpcodes = {
	opc_aload, opc_invokestatic, opc_areturn, 
	opc_astore, opc_new, opc_dup, opc_aload, 
	opc_invokevirtual, opc_invokespecial, opc_athrow
    };

    boolean checkGetClass() {
	if (!method.isStatic()
	    || !(method.getType()
		 .equals("(Ljava/lang/String;)Ljava/lang/Class;")))
	    return false;
	
	BasicBlocks bb = method.getBasicBlocks();

	Block startBlock = bb.getStartBlock();
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (startBlock == null
	    || startBlock.getInstructions().length < 2
	    || startBlock.getInstructions().length > 3
	    || excHandlers.length != 1
	    || excHandlers[0].getStart() != startBlock
	    || excHandlers[0].getEnd() != startBlock
	    || !"java.lang.ClassNotFoundException"
	    .equals(excHandlers[0].getType()))
	    return false;

        for (int i=0; i< 2; i++) {
	    Instruction instr = startBlock.getInstructions()[i];
	    if (instr.getOpcode() != getClassOpcodes[i])
		return false;
	    if (i == 1) {
		Reference ref = instr.getReference();
		if (!ref.getClazz().equals("Ljava/lang/Class;")
		    || !ref.getName().equals("forName"))
		    return false;
	    }
	    if (i == 0 && instr.getLocalSlot() != 0)
		return false;
	}
	if (startBlock.getInstructions().length == 2) {
	    /* JDK 1.4: The return is outside of startBlock */
	    Block nextBlock = startBlock.getSuccs()[0];
	    Instruction[] instrs = nextBlock.getInstructions();
	    if (instrs[0].getOpcode() != opc_areturn)
		return false;
	} else {
	    /* JDK 1.3 */
	    if (startBlock.getInstructions()[2].getOpcode() != opc_areturn)
		return false;
	}

	Block catchBlock = excHandlers[0].getCatcher();
	if (catchBlock.getInstructions().length != 7)
	    return false;
	int excSlot = -1;
	for (int i=0; i< 7; i++) {
	    Instruction instr = catchBlock.getInstructions()[i];
	    if (instr.getOpcode() != getClassOpcodes[3+i])
		return false;
	    if (i == 0)
		excSlot = instr.getLocalSlot();
	    if (i == 1 && !instr.getClazzType().equals
		("Ljava/lang/NoClassDefFoundError;"))
		return false;
	    if (i == 3 && instr.getLocalSlot() != excSlot)
		return false;
	    if (i == 4
		&& !instr.getReference().getName().equals("getMessage"))
		return false;
	    if (i == 5
		&& !instr.getReference().getName().equals("<init>"))
		return false;
	}
	this.kind = GETCLASS;
	return true;
    }

    private final int modifierMask = Modifier.PUBLIC;

    /**
     * Check if this is a field/method access method.  We have only
     * very few checks: The parameter must be loaded in correct order,
     * followed by an get/put/invoke-field/static/special consuming
     * all loaded parameters.  The CodeVerifier has already checked
     * that types of parameters are okay.  
     */
    private boolean checkAccess() {
	BasicBlocks bb = method.getBasicBlocks();
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;
	Block startBlock = bb.getStartBlock();
	if (startBlock == null)
	    return false;
	Block[] succBlocks = startBlock.getSuccs();
	if (succBlocks.length > 1 || 
	    (succBlocks.length == 1 && succBlocks[0] != null))
	    return false;
	Iterator iter = Arrays.asList(startBlock.getInstructions()).iterator();
	boolean dupSeen = false;

	if (!iter.hasNext())
	    return false;
	Instruction instr = (Instruction) iter.next();

	int params = 0, slot = 0;
	while (instr.getOpcode() >= opc_iload
	       && instr.getOpcode() <= opc_aload
	       && instr.getLocalSlot() == slot) {
	    params++;
	    slot += (instr.getOpcode() == opc_lload 
		     || instr.getOpcode() == opc_dload) ? 2 : 1;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	}

	if (instr.getOpcode() == opc_getstatic
	    || instr.getOpcode() == opc_getfield) {
	    boolean isStatic = instr.getOpcode() == opc_getstatic;
	    if (!isStatic)
		params--;
	    if (params != 0)
		return false;
	    Reference ref = instr.getReference();
	    ClassInfo refClazz = TypeSignature
		.getClassInfo(classInfo.getClassPath(), ref.getClazz());
	    try {
		if (!refClazz.superClassOf(classInfo))
		    return false;
	    } catch (IOException ex) {
		/* Can't get enough info to ensure that refClazz is correct */
		return false;
	    }
	    FieldInfo refField
		= refClazz.findField(ref.getName(), ref.getType());
	    if (refField == null
		|| (refField.getModifiers() & modifierMask) != 0)
		return false;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	    if (instr.getOpcode() < opc_ireturn
		|| instr.getOpcode() > opc_areturn)
		return false;
	    /* For valid bytecode the type matches automatically */
	    reference = ref;
	    kind = (isStatic ? ACCESSGETSTATIC : ACCESSGETFIELD);
	    return true;
	}
	if (instr.getOpcode() == (opc_dup - 3) + 3 * slot) {
	    /* This is probably a opc_dup or opc_dup2, 
	     * preceding a opc_putfield
	     */
	    instr = (Instruction) iter.next();
	    if (instr.getOpcode() != opc_putstatic
		&& instr.getOpcode() != opc_putfield)
		return false;
	    dupSeen = true;
	}
	if (instr.getOpcode() == opc_putfield 
	    || instr.getOpcode() == opc_putstatic) {
	    boolean isStatic = instr.getOpcode() == opc_putstatic;
	    if (!isStatic)
		params--;
	    if (params != 1)
		return false;
	    /* For valid bytecode the type of param matches automatically */
	    Reference ref = instr.getReference();
	    ClassInfo refClazz = TypeSignature
		.getClassInfo(classInfo.getClassPath(), ref.getClazz());
	    try {
		if (!refClazz.superClassOf(classInfo))
		    return false;
	    } catch (IOException ex) {
		/* Can't get enough info to ensure that refClazz is correct */
		return false;
	    }
	    FieldInfo refField
		= refClazz.findField(ref.getName(), ref.getType());
	    if (refField == null 
		|| (refField.getModifiers() & modifierMask) != 0)
		return false;
	    if (dupSeen) {
		if (!iter.hasNext())
		    return false;
		instr = (Instruction) iter.next();
		if (instr.getOpcode() < opc_ireturn
		    || instr.getOpcode() > opc_areturn)
		    return false;
		kind = (isStatic ? ACCESSDUPPUTSTATIC : ACCESSDUPPUTFIELD);
	    } else {
		if (iter.hasNext())
		    return false;
		kind = (isStatic ? ACCESSPUTSTATIC : ACCESSPUTFIELD);
	    }
	    reference = ref;
	    return true;
	}
	if (instr.getOpcode() == opc_invokestatic
	    || instr.getOpcode() == opc_invokespecial) {
	    boolean isStatic = instr.getOpcode() == opc_invokestatic;
	    if (!isStatic)
		params--;
	    Reference ref = instr.getReference();
	    ClassInfo refClazz = TypeSignature
		.getClassInfo(classInfo.getClassPath(), ref.getClazz());
	    try {
		if (!refClazz.superClassOf(classInfo))
		    return false;
	    } catch (IOException ex) {
		/* Can't get enough info to ensure that refClazz is correct */
		return false;
	    }
	    MethodInfo refMethod
		= refClazz.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(classInfo.getClassPath(),
					      ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != 0
		|| refType.getParameterTypes().length != params)
		return false;
	    if (refType.getReturnType() == Type.tVoid) {
		if (iter.hasNext())
		    return false;
	    } else {
		if (!iter.hasNext())
		    return false;
		instr = (Instruction) iter.next();
		if (instr.getOpcode() < opc_ireturn
		    || instr.getOpcode() > opc_areturn)
		    return false;
	    }

	    /* For valid bytecode the types matches automatically */
	    reference = ref;
	    kind = (isStatic ? ACCESSSTATICMETHOD : ACCESSMETHOD);
	    return true;
	}
	return false;
    }

    private boolean checkConstructorAccess() {
	BasicBlocks bb = method.getBasicBlocks();
	String[] paramTypes
	    = TypeSignature.getParameterTypes(method.getType());
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;
	Block startBlock = bb.getStartBlock();
	if (startBlock == null)
	    return false;
	Block[] succBlocks = startBlock.getSuccs();
	if (succBlocks.length != 1 || succBlocks[0] != null)
	    return false;
	Iterator iter = Arrays.asList(startBlock.getInstructions()).iterator();
	if (!iter.hasNext())
	    return false;

	unifyParam = -1;
	Instruction instr = (Instruction) iter.next();
	int params = 0, slot = 0;
	while (instr.getOpcode() >= opc_iload
	       && instr.getOpcode() <= opc_aload) {
	    if (instr.getLocalSlot() > slot
		&& unifyParam == -1 && params > 0
		&& paramTypes[params-1].charAt(0) == 'L') {
		unifyParam = params;
		params++;
		slot++;
	    }
	    if (instr.getLocalSlot() != slot)
		return false;

	    params++;
	    slot += (instr.getOpcode() == opc_lload 
		     || instr.getOpcode() == opc_dload) ? 2 : 1;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	}
	if (unifyParam == -1
	    && params > 0 && params <= paramTypes.length
	    && paramTypes[params-1].charAt(0) == 'L') {
	    unifyParam = params;
	    params++;
	    slot++;
	}
	if (params > 0 && instr.getOpcode() == opc_invokespecial) {
	    Reference ref = instr.getReference();
	    ClassInfo refClazz = TypeSignature
		.getClassInfo(classInfo.getClassPath(), ref.getClazz());
	    if (refClazz != classInfo)
		return false;
	    MethodInfo refMethod
		= refClazz.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(classInfo.getClassPath(),
					      ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != 0
		|| !refMethod.getName().equals("<init>")
		|| unifyParam == -1
		|| refType.getParameterTypes().length != params - 2)
		return false;	    
	    if (iter.hasNext())
		return false;
	    /* We don't check if types matches.  No problem since we only
	     * need to make sure, this constructor doesn't do anything 
	     * more than relay to the real one.
	     */
	    reference = ref;
	    kind = ACCESSCONSTRUCTOR;
	    return true;
	}
	return false;
    }
}

