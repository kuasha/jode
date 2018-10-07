/* MethodAnalyzer Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: MethodAnalyzer.java 1414 2012-03-02 11:20:15Z hoenicke $
 */

package net.sf.jode.decompiler;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.BasicBlocks;
import net.sf.jode.bytecode.Block;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.bytecode.Handler;
import net.sf.jode.bytecode.Instruction;
import net.sf.jode.bytecode.LocalVariableInfo;
import net.sf.jode.bytecode.MethodInfo;
import net.sf.jode.jvm.SyntheticAnalyzer;
import net.sf.jode.type.*;
import net.sf.jode.expr.Expression;
import net.sf.jode.expr.CheckNullOperator;
import net.sf.jode.expr.ThisOperator;
import net.sf.jode.expr.LocalLoadOperator;
import net.sf.jode.expr.OuterLocalOperator;
import net.sf.jode.expr.InvokeOperator;
import net.sf.jode.flow.StructuredBlock;
import net.sf.jode.flow.FlowBlock;
import net.sf.jode.flow.TransformExceptionHandlers;
import net.sf.jode.jvm.CodeVerifier;
import net.sf.jode.jvm.VerifyException;

import java.lang.reflect.Modifier;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
///#enddef

/**
 * A method analyzer is the main class for analyzation of methods.
 * There is exactly one MethodAnalyzer object for each method (even
 * for abstract methods), that should be decompiled.
 *
 * Method analyzation is done in three passes:
 * <dl>
 * <dt><code>analyze()</code></dt>
 * <dd>the main analyzation, decompiles the code of the method</dd>
 * <dt><code>analyzeInners()</code></dt>
 * <dd>This will analyze method scopes classes by calling their
 * <code>analyze()</code> and <code>analyzeInners()</code>
 * methods.</dd>
 * <dt><code>makeDeclaration()</code></dt>
 * <dd>This will determine when to declare variables. For constructors
 * it will do special transformations like field initialization.</dd>
 */
public class MethodAnalyzer implements Scope, ClassDeclarer {
    /**
     * The minimal visible complexity.
     */
    private static double STEP_COMPLEXITY = 0.01;
    /**
     * The value of the strictfp modifier.
     * JDK1.1 doesn't define it.
     */
    private static int STRICTFP = 0x800;
    /**
     * The import handler where we should register our types.
     */
    ImportHandler imports;
    /**
     * The class analyzer of the class that contains this method.
     */
    ClassAnalyzer classAnalyzer;
    /**
     * The method info structure for this method.
     */
    MethodInfo minfo;
    /**
     * This is the basic blocks structure, or null if this method has 
     * no code (abstract or native).
     */
    BasicBlocks bb;

    /**
     * The method name.
     */
    String methodName;
    /**
     * The type of this method (parameter types + return type).
     */
    MethodType methodType;
    /**
     * True, iff this method is a constructor, i.e. methodName == <(cl)?init>
     */
    boolean isConstructor;

    /**
     * The exceptions this method may throw.
     */
    Type[] exceptions;

    /**
     * If the method is synthetic (access$, class$, etc.), this is the
     * synthetic analyzer describing the function of this method, otherwise
     * this is null.
     */
    SyntheticAnalyzer synth;

    /**
     * This is the first flow block of the method code.  If this
     * method has no code, this is null.  This is initialized at the
     * end of the <code>analyze()</code> phase.  
     */
    FlowBlock methodHeader;
    /**
     * A list of all locals contained in this method.
     */
    Vector allLocals = new Vector();

    /**
     * This array contains the locals in the parameter list, including
     * the implicit <i>this</i> parameter for nonstatic methods.  
     */
    LocalInfo[] param;

    /**
     * If this method is the special constructor, that is generated
     * by jikes (constructor$xx), this points to the real constructor.
     * If this is the real constructor and calls a constructor$xx, it
     * points to this. Otherwise this is null.
     */
    MethodAnalyzer jikesConstructor;
    /**
     * True, iff this method is the special constructor, and its first
     * parameter is a reference to the outer class.
     */
    boolean hasJikesOuterValue;
    /**
     * True, iff this method is an anonymous constructor, that is
     * omitted even if it has parameters.
     */
    boolean isAnonymousConstructor;
    /**
     * True, if this method is the special block$ method generated by jikes
     * to initialize field members.
     */
    boolean isJikesBlockInitializer;

    /**
     * This list contains the InvokeOperator objects in the code of
     * this method, that create method scoped classes.  */
    Vector anonConstructors = new Vector();

    /**
     * This list contains the class analyzers of all method scoped
     * classes that should be declared in this method or in a class
     * that is declared in this method.
     */
    Vector innerAnalyzers;
    /**
     * This list contains the class analyzers of all method scoped
     * classes that are used in this method.
     */
    Collection usedAnalyzers;

    /**
     * This is the default constructor.
     * @param cla the ClassAnalyzer of the class that contains this method.
     * @param minfo the method info structure for this method.
     * @param imports the import handler that should be informed about types.
     */
    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          ImportHandler imports) {
        this.classAnalyzer = cla;
        this.imports = imports;
	this.minfo = minfo;
        this.methodName = minfo.getName();
        this.methodType = Type.tMethod(cla.getClassPath(), minfo.getType());
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
        
	if (minfo.getBasicBlocks() != null)
	    bb = minfo.getBasicBlocks();
	
        String[] excattr = minfo.getExceptions();
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
	    int excCount = excattr.length;
	    this.exceptions = new Type[excCount];
	    for (int i=0; i< excCount; i++)
		exceptions[i] = Type.tClass(classAnalyzer.getClassPath(),
					    excattr[i]);
        }
	if (minfo.isSynthetic() || methodName.indexOf('$') != -1)
	    synth = new SyntheticAnalyzer(cla.getClazz(), minfo, true);
    }

    /**
     * Returns the name of this method.
     */
    public String getName() {
	return methodName;
    }

    /**
     * Returns the type of this method.
     * @return the type of this method.
     */
    public MethodType getType() {
	return methodType;
    }

    /**
     * Returns the first flow block of the code.
     * @return the first flow block of the code.
     */
    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    /**
     * Returns the bytecode info for this method.
     * @return the bytecode info for this method, or null if it is
     * abstract or native.
     */
    public final BasicBlocks getBasicBlocks() {
	return bb;
    }

    /**
     * Returns the import handler. The import handler should be informed
     * about all types we (or an expression in this method) use, so that
     * the corresponding class can be imported.
     * @return the import handler.
     */
    public final ImportHandler getImportHandler() {
	return imports;
    }

    /**
     * Registers a type at the import handler.  This should be called
     * if an expression needs to print the type name to the code.  The
     * corresponding class will be imported in that case (if used
     * often enough).
     * @param type the type that should be registered.
     */
    public final void useType(Type type) {
	imports.useType(type);
    }

    /**
     * Inserts a structured block to the beginning of the method.
     * This is called by transform constructors, to move the super
     * call from the real constructor to the constructor$xx method
     * (the jikes constructor).
     * @param insertBlock the structured block that should be inserted.
     */
    public void insertStructuredBlock(StructuredBlock insertBlock) {
	if (methodHeader != null) {
	    methodHeader.prependBlock(insertBlock);
	} else {
	    throw new IllegalStateException();
	}
    }

    /**
     * Checks if this method is a constructor, i.e. getName() returns
     * "<init>" or "<clinit>".
     * @return true, iff this method is a real constructor.  
     */
    public final boolean isConstructor() {
        return isConstructor;
    }

    /**
     * Checks if this method is static.
     * @return true, iff this method is static.
     */
    public final boolean isStatic() {
        return minfo.isStatic();
    }

    /**
     * Checks if this method is synthetic, i.e. a synthetic attribute is
     * present.
     * @return true, iff this method is synthetic.
     */
    public final boolean isSynthetic() {
	return minfo.isSynthetic();
    }

    /**
     * Checks if this method is strictfp
     * @return true, iff this method is synthetic.
     */
    public final boolean isStrictFP() {
	return (minfo.getModifiers() & STRICTFP) != 0;
    }

    /**
     * Tells if this method is the constructor$xx method generated by jikes.
     * @param value true, iff this method is the jikes constructor.
     */
    public final void setJikesConstructor(MethodAnalyzer realConstr) {
	jikesConstructor = realConstr;
    }

    /**
     * Tells if this method is the block$xx method generated by jikes.
     * @param value true, iff this method is the jikes block initializer.
     */
    public final void setJikesBlockInitializer(boolean value) {
	isJikesBlockInitializer = value;
    }

    /**
     * Tells if this (constructor$xx) method has as first (implicit)
     * parameter the instance of the outer class.  
     * @param value true, this method has the implicit parameter.
     */
    public final void setHasOuterValue(boolean value) {
	hasJikesOuterValue = value;
    }

    /**
     * Tells if this constructor can be omited, since it is implicit.
     * @param value true, this method is the implicit constructor.
     */
    public final void setAnonymousConstructor(boolean value) {
	isAnonymousConstructor = value;
    }

    /**
     * Checks if this constructor can be omited, since it is implicit.
     * @return true, this method is the implicit constructor.
     */
    public final boolean isAnonymousConstructor() {
	return isAnonymousConstructor;
    }

    /**
     * Get the synthetic analyzer for this method.
     * @return the synthetic analyzer, or null if this method isn't
     * synthetic.
     */
    public final SyntheticAnalyzer getSynthetic() {
	return synth;
    }

    /**
     * Get the return type of this method.
     */
    public Type getReturnType() {
        return methodType.getReturnType();
    }

    /**
     * Get the class analyzer for the class containing this method.
     */
    public ClassAnalyzer getClassAnalyzer() {
	return classAnalyzer;
    }

    /**
     * Get the class info for the class containing this method.
     */
    public ClassInfo getClazz() {
        return classAnalyzer.clazz;
    }

    /**
     * Get the local info for a parameter.  This call is valid after
     * the analyze pass.
     * @param nr the index of the parameter (start by zero and
     * count the implicit this param for nonstatic method).
     * @return the local info for the specified parameter.
     * @see #getLocalInfo
     */
    public final LocalInfo getParamInfo(int nr) {
	return param[nr];
    }

    /**
     * Return the number of parameters for this method. This call is
     * valid after the analyze pass.
     */
    public final int getParamCount() {
	return param.length;
    }

    /**
     * Create a local info for a local variable located at an
     * instruction with the given address.
     * @param lvi the local variable info of the bytecode package.
     * @return a new local info representing that local.
     */
    public LocalInfo getLocalInfo(LocalVariableInfo lvi) {
        LocalInfo li = new LocalInfo(this, lvi.getSlot());
	if ((Options.options & Options.OPTION_LVT) != 0
	    && lvi.getName() != null)
	    li.addHint(lvi.getName(), Type.tType(classAnalyzer.getClassPath(),
						 lvi.getType()));
	allLocals.addElement(li);
        return li;
    }

    /**
     * Gets the complexity of this class.  Must be called after it has
     * been initialized.  This is used for a nice progress bar.
     */
    public double getComplexity() {
	if (bb == null)
	    return 0.0;
	else {
	    int count = 0;
	    Block[] blocks = bb.getBlocks();
	    for (int i=0; i < blocks.length; i++)
		count += blocks[i].getInstructions().length;
	    return count;
	}
    }

    /**
     * Analyzes the code of this method.  This creates the
     * flow blocks (including methodHeader) and analyzes them.  
     */
    private void analyzeCode(ProgressListener pl, double done, double scale) 
    {
	int instrsPerStep = Integer.MAX_VALUE;
	double instrScale = (scale * 0.9) / getComplexity();
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.print(methodName+": ");

	if (pl != null)
	    instrsPerStep = (int) (STEP_COMPLEXITY / instrScale);

	Block[] blocks = bb.getBlocks();
	FlowBlock[] flows = new FlowBlock[blocks.length];
        TransformExceptionHandlers excHandlers; 
	{
	    for (int i=0; i < blocks.length; i++)
		flows[i] = new FlowBlock(this, i, i > 0 ? flows[i-1]: null);

            /* We transform every basic block into a FlowBlock 
             */
	    int count = 0;
	    for (int i=0; i < blocks.length; i++) {
		int mark = 100;
		Instruction[] instrs = blocks[i].getInstructions();
		for (int j=0; j < instrs.length; j++) {
		    if (GlobalOptions.verboseLevel > 0 && j > mark) {
			GlobalOptions.err.print('.');
			mark += 100;
		    }
		    if (++count >= instrsPerStep) {
			done += count * instrScale;
			pl.updateProgress(done, methodName);
			count = 0;
		    }
		    Opcodes.addOpcode(flows[i], instrs[j], this);
		}
		Block[] succs = blocks[i].getSuccs();
		FlowBlock[] flowSuccs;
		int lastOpcode = instrs.length > 0
		    ? instrs[instrs.length-1].getOpcode() : Opcodes.opc_nop;
		if (lastOpcode >= Opcodes.opc_ireturn
		    && lastOpcode <= Opcodes.opc_areturn) {
		    flowSuccs = new FlowBlock[] { FlowBlock.END_OF_METHOD };
		} else {
		    flowSuccs = new FlowBlock[succs.length];
		    for (int j=0; j< succs.length; j++) {
			if (succs[j] == null)
			    flowSuccs[j] = FlowBlock.END_OF_METHOD;
			else
			    flowSuccs[j] = flows[succs[j].getBlockNr()];
		    }
		}
		flows[i].setSuccessors(flowSuccs);
	    }

	    done += count * instrScale;
	    Block startBlock = bb.getStartBlock();
	    if (startBlock == null)
		methodHeader = new FlowBlock(this, 0, null);
	    else
		methodHeader = flows[startBlock.getBlockNr()];
	    methodHeader.addStartPred();

	    Handler[] handlers = bb.getExceptionHandlers();
	    excHandlers = new TransformExceptionHandlers(flows);
            for (int i=0; i<handlers.length; i++) {
                Type type = null;
                int start = handlers[i].getStart().getBlockNr();
                int end   = handlers[i].getEnd().getBlockNr();
                FlowBlock handler = flows[handlers[i].getCatcher().getBlockNr()];
                if (handlers[i].getType() != null)
                    type = Type.tClass(classAnalyzer.getClassPath(),
				       handlers[i].getType());
                for (int j = start; j <= end; j++) {
                    if (flows[j] != handler)
                	flows[j].addExceptionHandler(type, handler);
                }
            }
        }

        if (GlobalOptions.verboseLevel > 0)
            GlobalOptions.err.print('-');
            
        //excHandlers.analyze();
        methodHeader.analyze();
	methodHeader.removeStartPred();

	if ((Options.options & Options.OPTION_PUSH) == 0
	    && methodHeader.mapStackToLocal())
	    methodHeader.removePush();
	if ((Options.options & Options.OPTION_ONETIME) != 0)
	    methodHeader.removeOnetimeLocals();

	methodHeader.mergeParams(param);

	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("");
	if (pl != null) {
	    done += 0.1 * scale;
	    pl.updateProgress(done, methodName);
	}
    } 

    /**
     * This is the first pass of the analyzation.  It will analyze the
     * code of this method, but not the method scoped classes.  
     */
    public void analyze(ProgressListener pl, double done, double scale) 
	throws ClassFormatError
    {
	if (pl != null)
	    pl.updateProgress(done, methodName);
	if (bb != null) {
	    if ((Options.options & Options.OPTION_VERIFY) != 0) {
		CodeVerifier verifier
		    = new CodeVerifier(getClazz(), minfo, bb);
		try {
		    verifier.verify();
		} catch (VerifyException ex) {
		    ex.printStackTrace(GlobalOptions.err);
		    throw new InternalError("Verification error");
		}
	    }
	}

        Type[] paramTypes = getType().getParameterTypes();
	int paramCount = (isStatic() ? 0 : 1) + paramTypes.length;
	param = new LocalInfo[paramCount];

	int offset = 0;
	int slot = 0;
	if (!isStatic()) {
	    ClassInfo classInfo = classAnalyzer.getClazz();
	    param[offset] = getLocalInfo(bb != null
					 ? bb.getParamInfo(slot)
					 : LocalVariableInfo.getInfo(slot));
	    param[offset].setExpression(new ThisOperator(classInfo, true));
	    slot++;
	    offset++;
	}
	
	for (int i=0; i< paramTypes.length; i++) {
	    param[offset] = getLocalInfo(bb != null 
					 ? bb.getParamInfo(slot)
					 : LocalVariableInfo.getInfo(slot));
	    param[offset].setType(paramTypes[i]);
	    slot += paramTypes[i].stackSize();
	    offset++;
	}

        for (int i= 0; i< exceptions.length; i++)
            imports.useType(exceptions[i]);
    
        if (!isConstructor)
            imports.useType(methodType.getReturnType());

	if (bb != null)
	    analyzeCode(pl, done, scale);
    }

    /**
     * This is the second pass of the analyzation.  It will analyze
     * the method scoped classes.  
     */
    public void analyzeInnerClasses()
      throws ClassFormatError
    {
        Enumeration elts = anonConstructors.elements();
        while (elts.hasMoreElements()) {
	    InvokeOperator cop = (InvokeOperator) elts.nextElement();
	    analyzeInvokeOperator(cop);
	}
    }

    /**
     * This is the third and last pass of the analyzation.  It will analyze
     * the types and names of the local variables and where to declare them.
     * It will also determine where to declare method scoped local variables.
     */
    public void makeDeclaration(Set done) {
	if (innerAnalyzers != null) {
	    for (Enumeration enumeration = innerAnalyzers.elements();
		 enumeration.hasMoreElements(); ) {
		ClassAnalyzer classAna = (ClassAnalyzer) enumeration.nextElement();
		if (classAna.getParent() == this) {
		    OuterValues innerOV = classAna.getOuterValues();
		    for (int i=0; i < innerOV.getCount(); i++) {
			Expression value = innerOV.getValue(i);
			if (value instanceof OuterLocalOperator) {
			    LocalInfo li = ((OuterLocalOperator) 
					    value).getLocalInfo();
			    if (li.getMethodAnalyzer() == this)
				li.markFinal();
			}
		    }
		}
	    }
	}

        for (Enumeration enumeration = allLocals.elements();
	     enumeration.hasMoreElements(); ) {
            LocalInfo li = (LocalInfo)enumeration.nextElement();
            if (!li.isShadow())
                imports.useType(li.getType());
        }

	for (int i=0; i < param.length; i++) {
	    param[i].guessName();
	    Iterator doneIter = done.iterator();
	    while (doneIter.hasNext()) {
		Declarable previous = (Declarable) doneIter.next();
		if (param[i].getName().equals(previous.getName())) {
		    /* A name conflict happened. */
		    param[i].makeNameUnique();
		    break;
		}
	    }
	    done.add(param[i]);
	}
	
	if (bb != null) {
	    methodHeader.makeDeclaration(done);
	    methodHeader.simplify();
	}
	for (int i=0; i < param.length; i++) {
	    done.remove(param[i]);
	    // remove the parameters, since we leave the scope
	}
    }

    /**
     * Tells if this method is synthetic or implicit or something else, so
     * that it doesn't have to be written to the source code.
     * @return true, iff it shouldn't be written to the source code.
     */
    public boolean skipWriting() {
	if (isSynthetic()
	    && (minfo.getModifiers() & 0x0040 /*ACC_BRIDGE*/) != 0)
	    return true;
	
	if (synth != null) {
	    // We don't need this class anymore (hopefully?)
	    if (synth.getKind() == SyntheticAnalyzer.GETCLASS)
		return true;
	    if (synth.getKind() >= SyntheticAnalyzer.ACCESSGETFIELD
		&& synth.getKind() <= SyntheticAnalyzer.ACCESSDUPPUTSTATIC
		&& (Options.options & Options.OPTION_INNER) != 0
		&& (Options.options & Options.OPTION_ANON) != 0)
		return true;
	}

	if (jikesConstructor == this) {
	    // This is the first empty part of a jikes constructor
	    return true;
	}

	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	int modifiedModifiers = minfo.getModifiers();
	if (isConstructor() && !isStatic()
	    && classAnalyzer.outerValues != null)
	    skipParams = classAnalyzer.outerValues.getCount();

	if (jikesConstructor != null) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = hasJikesOuterValue
		&& classAnalyzer.outerValues.getCount() > 0 ? 1 : 0;
	    // get the modifiers of the real constructor
	    modifiedModifiers = jikesConstructor.minfo.getModifiers();
	}

	if (isJikesBlockInitializer)
	    return true;

	/* The default constructor must be empty 
	 * and mustn't throw exceptions */
	if (getMethodHeader() == null
	    || !(getMethodHeader().getBlock() instanceof net.sf.jode.flow.EmptyBlock)
	    || !getMethodHeader().hasNoJumps()
	    || exceptions.length > 0)
	    return false;

	if (declareAsConstructor
	    /* The access rights of default constructor should
	     * be public, iff the class is public, otherwise package.
	     * But this rule doesn't necessarily apply for anonymous
	     * classes...
	     */
	    && ((modifiedModifiers
		 & (Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE
		    | Modifier.SYNCHRONIZED | Modifier.STATIC
		    | Modifier.ABSTRACT | Modifier.NATIVE))
		== (classAnalyzer.getModifiers()
		    & (Modifier.PROTECTED | Modifier.PUBLIC))
		|| classAnalyzer.getName() == null)
	    && classAnalyzer.constructors.length == 1) {

	    // If the constructor doesn't take parameters (except outerValues)
	    // or if it is the anonymous constructor it can be removed.
	    if (methodType.getParameterTypes().length == skipParams
		|| isAnonymousConstructor)
		return true;
	}

        if (isConstructor() && isStatic())
            return true;

	return false;
    }
    
    /**
     * Dumps the source code for this method to the specified writer.
     * @param writer the tabbed print writer the code should be written to.
     * @exception IOException, if writer throws an exception.
     */
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	int modifiedModifiers = minfo.getModifiers();

	if (isConstructor() && !isStatic()
	    && (Options.options & Options.OPTION_CONTRAFO) != 0) {
	    if (classAnalyzer.outerValues != null)
		skipParams = classAnalyzer.outerValues.getCount();
	    else if (classAnalyzer.getOuterInstance() != null)
		skipParams = 1;
	}

	if (jikesConstructor != null) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = hasJikesOuterValue
		&& classAnalyzer.outerValues.getCount() > 0 ? 1 : 0;
	    // get the modifiers of the real constructor
	    modifiedModifiers = jikesConstructor.minfo.getModifiers();
	}

	if (minfo.isDeprecated()) {
	    writer.println("/**");
	    writer.println(" * @deprecated");
	    writer.println(" */");
	}

	writer.pushScope(this);

	/*
	 * JLS-1.0, section 9.4:
	 *
	 * For compatibility with older versions of Java, it is
	 * permitted but discouraged, as a matter of style, to
	 * redundantly specify the abstract modifier for methods
	 * declared in interfaces.
	 *
	 * Every method declaration in the body of an interface is
	 * implicitly public. It is permitted, but strongly
	 * discouraged as a matter of style, to redundantly specify
	 * the public modifier for interface methods.  We don't
	 * follow this second rule and mark this method explicitly
	 * as public.
	 */
	if (classAnalyzer.getClazz().isInterface())
	    modifiedModifiers &= ~Modifier.ABSTRACT;

	/* Don't ask me why, but jikes declares the static constructor
	 * as final.
	 */
	if (isConstructor() && isStatic())
  	    modifiedModifiers &= ~(Modifier.FINAL | Modifier.PUBLIC
  				   | Modifier.PROTECTED | Modifier.PRIVATE);
	modifiedModifiers &= ~STRICTFP;

	writer.startOp(TabbedPrintWriter.NO_PAREN, 0);
	writer.startOp(TabbedPrintWriter.NO_PAREN, 5);

	String delim ="";
	if (minfo.isSynthetic()) {
	    writer.print("/*synthetic*/");
	    delim = " ";
	}

	String modif = Modifier.toString(modifiedModifiers);
	writer.print(delim + modif);
	if (modif.length() > 0)
	    delim = " ";
	if (isStrictFP()) {
	    /* The STRICTFP modifier is set.
	     * We handle it, since java.lang.reflect.Modifier is too dumb.
	     */

	    /* If STRICTFP is already set for class don't set it for method.
	     * And don't set STRICTFP for native methods or constructors.
	     */
	    if (!classAnalyzer.isStrictFP()
		&& !isConstructor()
		&& (modifiedModifiers & Modifier.NATIVE) == 0) {
		writer.print(delim + "strictfp");
		delim = " ";
	    }
	}

        if (isConstructor
	    && (isStatic()
		|| (classAnalyzer.getName() == null
		    && skipParams == methodType.getParameterTypes().length))) {
            /* static block or unnamed constructor */
        } else { 
	    writer.print(delim);
            if (declareAsConstructor)
		writer.print(classAnalyzer.getName());
            else {
                writer.printType(getReturnType());
		writer.print(" " + methodName);
	    }
	    writer.breakOp();
	    writer.printOptionalSpace();
            writer.print("(");
	    writer.startOp(TabbedPrintWriter.EXPL_PAREN, 0);
            int offset = skipParams + (isStatic() ? 0 : 1);
            for (int i = offset; i < param.length; i++) {
                if (i > offset) {
                    writer.print(", ");
		    writer.breakOp();
		}
		param[i].dumpDeclaration(writer);
            }
	    writer.endOp();
            writer.print(")");
        }
	writer.endOp();
        if (exceptions.length > 0) {
	    writer.breakOp();
            writer.print(" throws ");
	    writer.startOp(TabbedPrintWriter.NO_PAREN, 0);
            for (int i= 0; i< exceptions.length; i++) {
                if (i > 0) {
                    writer.print(", ");
		    writer.breakOp();
		}
                writer.printType(exceptions[i]);
            }
	    writer.endOp();
        }
	writer.endOp();
        if (bb != null) {
	    writer.openBraceNoIndent();
            writer.tab();
	    methodHeader.dumpSource(writer);
            writer.untab();
	    writer.closeBraceNoIndent();
        } else
            writer.println(";");
	writer.popScope();
    }

    /**
     * Checks if the variable set contains a local with the given name.
     * @return the local info the has the given name, or null if it doesn't
     * exists.
     */
    public LocalInfo findLocal(String name) {
        Enumeration enumeration = allLocals.elements();
        while (enumeration.hasMoreElements()) {
            LocalInfo li = (LocalInfo) enumeration.nextElement();
            if (li.getName().equals(name))
                return li;
        }
        return null;
    }

    /**
     * Checks if a method scoped class with the given name exists in this
     * method (not in a parent method).
     * @return the class analyzer with the given name, or null if it
     * doesn' exists.  
     */
    public ClassAnalyzer findAnonClass(String name) {
	if (innerAnalyzers != null) {
	    Enumeration enumeration = innerAnalyzers.elements();
	    while (enumeration.hasMoreElements()) {
		ClassAnalyzer classAna = (ClassAnalyzer) enumeration.nextElement();
		if (classAna.getParent() == this
		    && classAna.getName() != null
		    && classAna.getName().equals(name)) {
		    return classAna;
		}
	    }
	}
        return null;
    }

    /**
     * Checks if the specified object lies in this scope.
     * @param obj the object.
     * @param scopeType the type of this object.
     */
    public boolean isScopeOf(Object obj, int scopeType) {
	if (scopeType == METHODSCOPE
	    && obj instanceof ClassInfo) {
	    ClassAnalyzer ana = getClassAnalyzer((ClassInfo)obj);
	    if (ana != null)
		return ana.getParent() == this;
	}
	return false;
    }

    /**
     * Checks if the specified name conflicts with an object in this scope.
     * @param name the name to check.
     * @param scopeType the usage type of this name, AMBIGUOUSNAME if it is
     * ambiguous.
     */
    public boolean conflicts(String name, int usageType) {
	if (usageType == AMBIGUOUSNAME || usageType == LOCALNAME)
	    return findLocal(name) != null;
	if (usageType == AMBIGUOUSNAME || usageType == CLASSNAME)
	    return findAnonClass(name) != null;
	return false;
    }

    /**
     * Gets the parent scope, i.e. the class analyzer for the class containing
     * this method.
     * @XXX needed?
     */
    public ClassDeclarer getParent() {
	return getClassAnalyzer();
    }

    /**
     * Registers an anonymous constructor invokation.  This should be called
     * in the analyze or analyzeInner pass by invoke subexpressions.
     * @param cop the constructor invokation, that creates the method scoped
     * class.
     */
    public void addAnonymousConstructor(InvokeOperator cop) {
	anonConstructors.addElement(cop);
    }

    public void analyzeInvokeOperator(InvokeOperator cop) {
	ClassInfo clazz = cop.getClassInfo();
	ClassAnalyzer anonAnalyzer = getParent().getClassAnalyzer(clazz);
	
	if (anonAnalyzer == null) {
	    /* Create a new outerValues array corresponding to the
	     * first constructor invocation.
	     */
	    Expression[] outerValueArray;
	    Expression[] subExprs = cop.getSubExpressions();
	    outerValueArray = new Expression[subExprs.length-1];
	    
	    for (int j=0; j < outerValueArray.length; j++) {
		Expression expr = subExprs[j+1].simplify();
		if (expr instanceof CheckNullOperator)
		    expr = ((CheckNullOperator) 
			    expr).getSubExpressions()[0];
		if (expr instanceof ThisOperator) {
		    outerValueArray[j] = 
			new ThisOperator(((ThisOperator) expr).getClassInfo());
		    continue;
		}
		LocalInfo li = null;
		if (expr instanceof LocalLoadOperator) {
		    li = ((LocalLoadOperator) expr).getLocalInfo();
		    if (!li.isConstant())
			li = null;
		}
		if (expr instanceof OuterLocalOperator)
		    li = ((OuterLocalOperator) expr).getLocalInfo();
		
		if (li != null) {
		    outerValueArray[j] = new OuterLocalOperator(li);
		    continue;
		}
		
		Expression[] newOuter = new Expression[j];
		System.arraycopy(outerValueArray, 0, newOuter, 0, j);
		outerValueArray = newOuter;
		break;
	    }
	    try {
		anonAnalyzer = new ClassAnalyzer(this, clazz, imports,
						 outerValueArray);
	    } catch (IOException ex) {
		GlobalOptions.err.println
		    ("Error while reading anonymous class "+clazz+".");
		return;
	    }
	    addClassAnalyzer(anonAnalyzer);
	    anonAnalyzer.initialize();
	    anonAnalyzer.analyze(null, 0.0, 0.0);
	    anonAnalyzer.analyzeInnerClasses(null, 0.0, 0.0);
	} else {
	    /*
	     * Get the previously created outerValues and
	     * its length.  
	     */
	    OuterValues outerValues = anonAnalyzer.getOuterValues();
	    /*
	     * Merge the other constructor invocation and
	     * possibly shrink outerValues array.  
	     */
	    Expression[] subExprs = cop.getSubExpressions();
	    for (int j=0; j < outerValues.getCount(); j++) {
		if (j+1 < subExprs.length) {
		    Expression expr = subExprs[j+1].simplify();
		    if (expr instanceof CheckNullOperator)
			expr = ((CheckNullOperator) expr)
			    .getSubExpressions()[0];

		    if (outerValues.unifyOuterValues(j, expr))
			continue;
		    
		}
		outerValues.setCount(j);
		break;
	    }
	}

	if (usedAnalyzers == null)
	    usedAnalyzers = new ArrayList();
	usedAnalyzers.add(anonAnalyzer);
    }
    
    /**
     * Get the class analyzer for the given class info.  This searches
     * the method scoped/anonymous classes in this method and all
     * outer methods and the outer classes for the class analyzer.
     * @param cinfo the classinfo for which the analyzer is searched.
     * @return the class analyzer, or null if there is not an outer
     * class that equals cinfo, and not a method scope/inner class in
     * an outer method.
     */
    public ClassAnalyzer getClassAnalyzer(ClassInfo cinfo) {
	if (innerAnalyzers != null) {
	    Enumeration enumeration = innerAnalyzers.elements();
	    while (enumeration.hasMoreElements()) {
		ClassAnalyzer classAna = (ClassAnalyzer) enumeration.nextElement();
		if (classAna.getClazz().equals(cinfo)) {
		    if (classAna.getParent() != this) {
			ClassDeclarer declarer = classAna.getParent();
			while (declarer != this) {
			    if (declarer instanceof MethodAnalyzer)
				((MethodAnalyzer) declarer)
				    .innerAnalyzers.removeElement(classAna);
			    declarer = declarer.getParent();
			}
			classAna.setParent(this);
		    }
		    return classAna;
		}
	    }
	}
	return getParent().getClassAnalyzer(cinfo);
    }

    public void addClassAnalyzer(ClassAnalyzer clazzAna) {
	if (innerAnalyzers == null)
	    innerAnalyzers = new Vector();
	innerAnalyzers.addElement(clazzAna);
	getParent().addClassAnalyzer(clazzAna);
    }

    /**
     * We add the named method scoped classes to the declarables.
     */
    public void fillDeclarables(Collection used) {
	if (usedAnalyzers != null)
	    used.addAll(usedAnalyzers);
	if (innerAnalyzers != null) {
	    Enumeration enumeration = innerAnalyzers.elements();
	    while (enumeration.hasMoreElements()) {
		ClassAnalyzer classAna = (ClassAnalyzer) enumeration.nextElement();
		if (classAna.getParent() == this)
		    classAna.fillDeclarables(used);
	    }
	}
    }

    public boolean isMoreOuterThan(ClassDeclarer declarer) {
	ClassDeclarer ancestor = declarer;
	while (ancestor != null) {
	    if (ancestor == this)
		return true;
	    ancestor = ancestor.getParent();
	}
	return false;
    }

    public String toString() {
	return getClass().getName()+"["+getClazz()+"."+getName()+"]";
    }
}
