/* MethodIdentifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: MethodIdentifier.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.obfuscator;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.*;

///#def COLLECTIONS java.util
import java.util.Collections;
import java.util.Iterator;
///#enddef

import java.util.BitSet;

public class MethodIdentifier extends Identifier implements Opcodes {
    ClassIdentifier clazz;
    MethodInfo info;
    String name;
    String type;

    boolean globalSideEffects;
    BitSet localSideEffects;

    /**
     * The code analyzer of this method, or null if there isn't any.
     */
    CodeAnalyzer codeAnalyzer;

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.name = info.getName();
	this.type = info.getType();
	this.clazz = clazz;
	this.info  = info;

	BasicBlocks bb = info.getBasicBlocks();
	if (bb != null) {
	    if ((Main.stripping &
		 (Main.STRIP_LVT | Main.STRIP_LNT)) != 0) {
		Block[] blocks = bb.getBlocks();
		for (int i = 0; i < blocks.length; i++) {
		    Instruction[] instrs = blocks[i].getInstructions();
		    for (int j = 0; j < instrs.length; j++) {
			if ((Main.stripping & Main.STRIP_LVT) != 0
			    && instrs[j].hasLocal())
			    instrs[j].setLocalInfo
				(LocalVariableInfo
				 .getInfo(instrs[j].getLocalSlot()));
			if ((Main.stripping & Main.STRIP_LNT) != 0)
			    instrs[j].setLineNr(-1);
		    }
		}
	    }
	    codeAnalyzer = Main.getClassBundle().getCodeAnalyzer();

	    CodeTransformer[] trafos
		= Main.getClassBundle().getPreTransformers();
	    for (int i = 0; i < trafos.length; i++) {
		trafos[i].transformCode(bb);
	    }
	}
    }

    public Iterator getChilds() {
	return Collections.EMPTY_LIST.iterator();
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	Main.getClassBundle().analyzeIdentifier(this);
    }

    public void analyze() {
	if (GlobalOptions.verboseLevel > 1)
	    GlobalOptions.err.println("Analyze: "+this);

	String type = getType();
	int index = type.indexOf('L');
	while (index != -1) {
	    int end = type.indexOf(';', index);
	    Main.getClassBundle().reachableClass
		(type.substring(index+1, end).replace('/', '.'));
	    index = type.indexOf('L', end);
	}

	String[] exceptions = info.getExceptions();
	if (exceptions != null) {
	    for (int i=0; i< exceptions.length; i++)
		Main.getClassBundle()
		    .reachableClass(exceptions[i]);
	}

	BasicBlocks bb = info.getBasicBlocks();
	if (bb != null)
	    codeAnalyzer.analyzeCode(this, bb);
    }

    public Identifier getParent() {
	return clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName() + "." + getType();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias() + "."
	    + Main.getClassBundle().getTypeAlias(getType());
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return type;
    }

    public int getModifiers() {
	return info.getModifiers();
    }

    public boolean conflicting(String newAlias) {
	return clazz.methodConflicts(this, newAlias);
    }

    public String toString() {
	return "MethodIdentifier "+getFullName();
    }

    public boolean hasGlobalSideEffects() {
	return globalSideEffects;
    }

    public boolean getLocalSideEffects(int paramNr) {
	return globalSideEffects || localSideEffects.get(paramNr);
    }

    public void setGlobalSideEffects() {
	globalSideEffects = true;
    }

    public void setLocalSideEffects(int paramNr) {
	localSideEffects.set(paramNr);
    }

    /**
     * This method does the code transformation.  This include
     * <ul><li>new slot distribution for locals</li>
     *     <li>obfuscating transformation of flow</li>
     *     <li>renaming field, method and class references</li>
     * </ul>
     */
    boolean wasTransformed = false;
    public void doTransformations() {
	if (wasTransformed)
	    throw new InternalError
		("doTransformation called on transformed method");
	wasTransformed = true;
	info.setName(getAlias());
	ClassBundle bundle = Main.getClassBundle();
	info.setType(bundle.getTypeAlias(type));
	if (codeAnalyzer != null) {
	    BasicBlocks bb = info.getBasicBlocks();
	    try {
		codeAnalyzer.transformCode(bb);
		CodeTransformer[] trafos = bundle.getPostTransformers();
		for (int i = 0; i < trafos.length; i++) {
		    trafos[i].transformCode(bb);
		}
	    } catch (RuntimeException ex) {
		ex.printStackTrace(GlobalOptions.err);
		bb.dumpCode(GlobalOptions.err);
	    }
	    
	    Block[] blocks = bb.getBlocks();
	    for (int i = 0; i < blocks.length; i++) {
		Instruction[] instrs = blocks[i].getInstructions();
		for (int j = 0; j < instrs.length; j++) {
		    switch (instrs[j].getOpcode()) {
		    case opc_invokespecial:
		    case opc_invokestatic:
		    case opc_invokeinterface:
		    case opc_invokevirtual: {
			instrs[j].setReference
			    (Main.getClassBundle()
			     .getReferenceAlias(instrs[j].getReference()));
			break;
			
		    }
		    case opc_putstatic:
		    case opc_putfield:
		    case opc_getstatic:
		    case opc_getfield: {
			instrs[j].setReference
			    (Main.getClassBundle()
			     .getReferenceAlias(instrs[j].getReference()));
			break;
		    }
		    case opc_new:
		    case opc_checkcast:
		    case opc_instanceof:
		    case opc_multianewarray: {
			instrs[j].setClazzType
			    (Main.getClassBundle()
			 .getTypeAlias(instrs[j].getClazzType()));
			break;
		    }
		    }
		}
	    }
		
	    Handler[] handlers = bb.getExceptionHandlers();
	    for (int i=0; i< handlers.length; i++) {
		if (handlers[i].getType() != null) {
		    ClassIdentifier ci = Main.getClassBundle()
			.getClassIdentifier(handlers[i].getType());
		    if (ci != null)
			handlers[i].setType(ci.getFullAlias());
		}
	    }
	}

	String[] exceptions = info.getExceptions();
	if (exceptions != null) {
	    for (int i=0; i< exceptions.length; i++) {
		ClassIdentifier ci = Main.getClassBundle()
		    .getClassIdentifier(exceptions[i]);
		if (ci != null)
		    exceptions[i] = ci.getFullAlias();
	    }
	}
    }
}
