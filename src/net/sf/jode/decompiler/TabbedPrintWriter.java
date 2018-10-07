/* TabbedPrintWriter Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: TabbedPrintWriter.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.decompiler;
import java.io.*;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.type.*;

public class TabbedPrintWriter {
    /* The indentation size. */
    private int indentsize;
    /* The size of a tab, 0 if we shouldn't use tabs at all. */
    private int tabWidth;
    private int style;
    private int lineWidth;
    private int currentIndent = 0;
    private String indentStr = "";
    private PrintWriter pw;
    private ImportHandler imports;
    private Stack scopes = new Stack();

    public static final int BRACE_AT_EOL     = 0x10;
    public static final int INDENT_BRACES    = 0x20;
    public static final int GNU_SPACING      = 0x40;
    public static final int CODD_FORMATTING =  0x80; // allow trailing CLOSING braces as well

    /**
     * This string contains a few tab characters followed by tabWidth - 1
     * spaces.  It is used to quickly calculate the indentation string.
     */
    private String tabSpaceString;
    private StringBuffer currentLine;
    private BreakPoint currentBP;

    public final static int EXPL_PAREN = 0;
    public final static int NO_PAREN   = 1;
    public final static int IMPL_PAREN = 2;
    public final static int DONT_BREAK = 3;

    /**
     * The amount of tabs for which we can use the tabSpaceString.
     */
    private final static int FASTINDENT = 20;

    /**
     * Convert the numeric indentation to a string.
     */
    protected String makeIndentStr(int indent) {
	if (indent < 0)
	    return "NEGATIVEINDENT"+indent;

	int tabs = indent / tabWidth;
	indent -= tabs * tabWidth;
	if (tabs <= FASTINDENT) {
	    /* The fast way. */
	    return tabSpaceString.substring(FASTINDENT - tabs,
					    FASTINDENT + indent);
	}
	/* the not so fast way */
	StringBuffer sb = new StringBuffer(tabs + indent);
        while (tabs > FASTINDENT) {
	    sb.append(tabSpaceString.substring(0, FASTINDENT));
	    tabs -= 20;
	}
	sb.append(tabSpaceString.substring(FASTINDENT - tabs,
					   FASTINDENT + indent)); 
	return sb.toString();
    }

    class BreakPoint {
	int options;
	int breakPenalty;
	int breakPos;
	int startPos;
	BreakPoint parentBP;
	Vector childBPs;
	int nesting = 0;
	int endPos;
	int whatBreak = 0;

	public BreakPoint(BreakPoint parent, int position) {
	    this.breakPos = position;
	    this.parentBP = parent;
	    this.options = DONT_BREAK;
	    this.breakPenalty = 0;
	    this.startPos = -1;
	    this.endPos = -1;
	    this.whatBreak = 0;
	    this.childBPs = null;
	}

	public void startOp(int opts, int penalty, int pos) {
	    if (startPos != -1)
		throw new InternalError("missing breakOp");
	    startPos = pos;
	    options = opts;
	    breakPenalty = penalty;
	    childBPs = new Vector();
	    breakOp(pos);
	}

	public void breakOp(int pos) {
	    childBPs.addElement (new BreakPoint(this, pos));
	}

	public void endOp(int pos) {
	    endPos = pos;
	    if (childBPs.size() == 1) {
		/* There is no breakpoint in this op, replace this with
		 * our child, if possible.
		 */
		BreakPoint child = (BreakPoint) childBPs.elementAt(0);
		options = Math.min(options, child.options);
		startPos = child.startPos;
		endPos = child.endPos;
		breakPenalty = child.breakPenalty;
		childBPs = child.childBPs;
	    }
	}

	public void dump(String line) {
	    if (startPos == -1) {
		pw.print(line);
	    } else {
		pw.print(line.substring(0, startPos));
		dumpRegion(line);
		pw.print(line.substring(endPos));
	    }
	}

	public void dumpRegion(String line) {
	    String parens = "{\010{}\010}<\010<>\010>[\010[]\010]`\010`'\010'"
		.substring(options*6, options*6+6);
	    pw.print(parens.substring(0,3));
	    Enumeration enumeration = childBPs.elements();
	    int cur = startPos;
	    BreakPoint child = (BreakPoint) enumeration.nextElement();
	    if (child.startPos >= 0) {
		pw.print(line.substring(cur, child.startPos));
		child.dumpRegion(line);
		cur = child.endPos;
	    }
	    while (enumeration.hasMoreElements()) {
		child = (BreakPoint) enumeration.nextElement();
		pw.print(line.substring(cur, child.breakPos));
		pw.print("!\010!"+breakPenalty);
		cur = child.breakPos;
		if (child.startPos >= 0) {
		    pw.print(line.substring(child.breakPos, child.startPos));
		    child.dumpRegion(line);
		    cur = child.endPos;
		}
	    }
	    pw.print(line.substring(cur, endPos));
	    pw.print(parens.substring(3));
	}

	public void printLines(int indent, String line) {
	    if (startPos == -1) {
		pw.print(line);
	    } else {
		pw.print(line.substring(0, startPos));
		printRegion(indent + startPos, line);
		pw.print(line.substring(endPos));
	    }
	}

	public void printRegion(int indent, String line) {
	    if (options == IMPL_PAREN) {
		pw.print("(");
		indent++;
	    }

	    Enumeration enumeration = childBPs.elements();
	    int cur = startPos;
	    BreakPoint child = (BreakPoint) enumeration.nextElement();
	    if (child.startPos >= 0) {
		pw.print(line.substring(cur, child.startPos));
		child.printRegion(indent + child.startPos - cur, line);
		cur = child.endPos;
	    }
	    if (options == NO_PAREN)
		indent += indentsize;
	    String indentStr = makeIndentStr(indent);
	    while (enumeration.hasMoreElements()) {
		child = (BreakPoint) enumeration.nextElement();
		pw.print(line.substring(cur, child.breakPos));
		pw.println();
		pw.print(indentStr);
		cur = child.breakPos;
		if (cur < endPos && line.charAt(cur) == ' ')
		    cur++;
		if (child.startPos >= 0) {
		    pw.print(line.substring(cur, child.startPos));
		    child.printRegion(indent + child.startPos - cur, line);
		    cur = child.endPos;
		}
	    }
	    pw.print(line.substring(cur, endPos));
	    if (options == IMPL_PAREN)
		pw.print(")");
	}

        public BreakPoint commitMinPenalty(int space, int lastSpace, 
					   int minPenalty) {
	    if (startPos == -1 || lastSpace > endPos - startPos
		|| minPenalty == 10 * (endPos - startPos - lastSpace)) {
		/* We don't have to break anything */
		startPos = -1;
		childBPs = null;
		return this;
	    }

	    int size = childBPs.size();
	    if (size > 1 && options != DONT_BREAK) {
		/* penalty if we are breaking the line here. */
		int breakPen
		    = getBreakPenalty(space, lastSpace, minPenalty + 1);
		if (minPenalty == breakPen) {
		    commitBreakPenalty(space, lastSpace, breakPen);
		    return this;
		}
	    }

	    /* penalty if we are breaking only one child */
	    for (int i=0; i < size; i++) {
		BreakPoint child = (BreakPoint) childBPs.elementAt(i);
		int front = child.startPos - startPos;
		int tail  = endPos - child.endPos;
		int needPenalty = minPenalty - (i < size - 1 ? 1 : 0);
		if (needPenalty ==
		    child.getMinPenalty(space - front, 
					lastSpace - front - tail,
					needPenalty + 1)) {
		    child = child.commitMinPenalty(space - front, 
						   lastSpace - front - tail, 
						   needPenalty);
		    child.breakPos = breakPos;
		    return child;
		}
	    }
	    throw new IllegalStateException("Can't commit line break!");
	}

        public int getMinPenalty(int space, int lastSpace, int minPenalty) {
	    if (10 * -lastSpace >= minPenalty) {
		return minPenalty;
	    }

	    if (startPos == -1)
		return 10 * -lastSpace;

	    if (lastSpace > endPos - startPos) {
		return 0;
	    }

	    if (minPenalty <= 1) {
		return minPenalty;
	    }

	    if (minPenalty > 10 * (endPos - startPos - lastSpace))
		minPenalty = 10 * (endPos - startPos - lastSpace);

	    int size = childBPs.size();
	    if (size == 0)
		return minPenalty;

	    if (size > 1 && options != DONT_BREAK) {
		/* penalty if we are breaking at this level. */
		minPenalty = getBreakPenalty(space, lastSpace, minPenalty);
	    }
		
	    /* penalty if we are breaking only one child */
	    for (int i=0; i < size; i++) {
		BreakPoint child = (BreakPoint) childBPs.elementAt(i);
		int front = child.startPos - startPos;
		int tail  = endPos - child.endPos;
		int penalty = (i < size - 1 ? 1 : 0);
		minPenalty = penalty +
		    child.getMinPenalty(space - front, 
					lastSpace - front - tail,
					minPenalty - penalty);
	    }
	    return minPenalty;
	}

	public void commitBreakPenalty(int space, int lastSpace, 
				       int minPenalty) {
	    if (options == IMPL_PAREN) {
		space--;
		lastSpace -= 2;
	    }

	    Enumeration enumeration = childBPs.elements();
	    childBPs = new Vector();
	    int currInd = 0;
	    BreakPoint lastChild, nextChild;
	    boolean indentNext = options == NO_PAREN;
	    for (lastChild = (BreakPoint) enumeration.nextElement();
		 enumeration.hasMoreElements(); lastChild = nextChild) {
		nextChild = (BreakPoint) enumeration.nextElement();
		int childStart = lastChild.breakPos;
		int childEnd = nextChild.breakPos;

		if (currInd > 0) {
		    currInd += childEnd - childStart;
		    if (currInd <= space)
			continue;
		}
		if (childStart < endPos
		    && currentLine.charAt(childStart) == ' ')
		    childStart++;

		if (childEnd - childStart > space) {
		    int front = lastChild.startPos - childStart;
		    int tail = childEnd - lastChild.endPos;
		    int childPenalty = lastChild.getMinPenalty
			(space - front, space - front - tail, minPenalty);
		    currInd = 0;
		    childBPs.addElement
			(lastChild.commitMinPenalty
			 (space - front, space - front - tail, childPenalty));
		} else {
		    lastChild.startPos = -1;
		    lastChild.childBPs = null;
		    childBPs.addElement(lastChild);
		    currInd = childEnd - childStart;
		}

		if (indentNext) {
		    space -= indentsize;
		    lastSpace -= indentsize;
		    indentNext = false;
		}
	    }
	    int childStart = lastChild.breakPos;
	    if (currInd > 0 && currInd + endPos - childStart <= lastSpace) 
		return;

	    if (childStart < endPos
		&& currentLine.charAt(childStart) == ' ')
		childStart++;
	    if (endPos - childStart > lastSpace) {
		int front = lastChild.startPos - childStart;
		int tail = endPos - lastChild.endPos;
		int childPenalty = lastChild.getMinPenalty
		    (space - front, lastSpace - front - tail, minPenalty + 1);
		childBPs.addElement
		    (lastChild.commitMinPenalty
		     (space - front, lastSpace - front - tail, childPenalty));
	    } else {
		lastChild.startPos = -1;
		lastChild.childBPs = null;
		childBPs.addElement(lastChild);
	    }
	}

	public int getBreakPenalty(int space, int lastSpace, int minPenalty) {
	    int penalty = breakPenalty;
	    int currInd = 0;
	    if (options == IMPL_PAREN) {
		space--;
		lastSpace -= 2;
	    }
	    if (space < 0)
		return minPenalty;
	    Enumeration enumeration = childBPs.elements();
	    BreakPoint lastChild, nextChild;
	    boolean indentNext = options == NO_PAREN;
	    for (lastChild = (BreakPoint) enumeration.nextElement();
		 enumeration.hasMoreElements(); lastChild = nextChild) {
		nextChild = (BreakPoint) enumeration.nextElement();
		int childStart = lastChild.breakPos;
		int childEnd = nextChild.breakPos;

		if (currInd > 0) {
		    currInd += childEnd - childStart;
		    if (currInd <= space)
			continue;

		    penalty++;
		    if (indentNext) {
			space -= indentsize;
			lastSpace -= indentsize;
			indentNext = false;
		    }
		}

		if (childStart < endPos 
		    && currentLine.charAt(childStart) == ' ')
		    childStart++;

		if (childEnd - childStart > space) {
		    int front = lastChild.startPos - childStart;
		    int tail = childEnd - lastChild.endPos;
		    penalty += 1 + lastChild.getMinPenalty
			(space - front, space - front - tail,
			 minPenalty - penalty - 1);

		    if (indentNext) {
			space -= indentsize;
			lastSpace -= indentsize;
			indentNext = false;
		    }
		    currInd = 0;
		} else
		    currInd = childEnd - childStart;

		if (penalty >= minPenalty)
		    return minPenalty;
	    }
	    int childStart = lastChild.breakPos;
	    if (currInd > 0) {
		if (currInd + endPos - childStart <= lastSpace)
		    return penalty;

		penalty++;
		if (indentNext) {
		    space -= indentsize;
		    lastSpace -= indentsize;
		    indentNext = false;
		}
	    }
	    if (childStart < endPos
		&& currentLine.charAt(childStart) == ' ')
		childStart++;
	    if (endPos - childStart > lastSpace) {
		int front = lastChild.startPos - childStart;
		int tail = endPos - lastChild.endPos;
		penalty += lastChild.getMinPenalty
		    (space - front, lastSpace - front - tail,
		     minPenalty - penalty);
	    }
	    if (penalty < minPenalty)
		return penalty;
	    return minPenalty;
	}
    }

    public TabbedPrintWriter (OutputStream os, ImportHandler imports,
			      boolean autoFlush, int style,
			      int indentSize, int tabWidth, int lineWidth) {
        if ((style & CODD_FORMATTING) != 0)
            pw = new PrintWriter(new NlRemover(new OutputStreamWriter(os), tabWidth), autoFlush);
        else
            pw = new PrintWriter(os, autoFlush);
	this.imports = imports;
	this.style = style;
	this.indentsize = indentSize;
	this.tabWidth = tabWidth;
	this.lineWidth = lineWidth;
	init();
    }

    public TabbedPrintWriter (Writer os, ImportHandler imports, boolean autoFlush, int style, int indentSize, int tabWidth, int lineWidth) {
        if ((style & CODD_FORMATTING) != 0)
            pw = new PrintWriter(new NlRemover(os, tabWidth), autoFlush);
        else {
            pw = new PrintWriter(os, autoFlush); }
	this.imports = imports;
	this.style = style;
	this.indentsize = indentSize;
	this.tabWidth = tabWidth;
	this.lineWidth = lineWidth;
	init();
    }

    public TabbedPrintWriter (OutputStream os, ImportHandler imports,
			      boolean autoFlush) {
	this(os, imports, autoFlush, BRACE_AT_EOL, 4, 8, 79);
    }

    public TabbedPrintWriter (Writer os, ImportHandler imports, 
			      boolean autoFlush) {
	this(os, imports, autoFlush, BRACE_AT_EOL, 4, 8, 79);
    }

    public TabbedPrintWriter (OutputStream os, ImportHandler imports) {
	this(os, imports, true);
    }

    public TabbedPrintWriter (Writer os, ImportHandler imports) {
	this(os, imports, true);
    }

    public TabbedPrintWriter (OutputStream os) {
	this(os, null);
    }

    public TabbedPrintWriter (Writer os) {
	this(os, null);
    }

    private void init() {
	currentLine = new StringBuffer();
	currentBP = new BreakPoint(null, 0);
	currentBP.startOp(DONT_BREAK, 1, 0);
	initTabString();
    }

    private void initTabString() {
	char tabChar = '\t';
	if (tabWidth <= 1) {
	    /* If tabWidth is 0 use spaces instead of tabs. */
	    tabWidth = 1;
	    tabChar = ' ';
	}
	StringBuffer sb = new StringBuffer(FASTINDENT + tabWidth - 1);
	for (int i = 0; i < FASTINDENT; i++)
	    sb.append(tabChar);
	for (int i = 0; i < tabWidth - 1; i++)
	    sb.append(' ');
	tabSpaceString = sb.toString();
    }

    public void tab() {
	currentIndent += indentsize;
	indentStr = makeIndentStr(currentIndent);
    }

    public void untab() {
	currentIndent -= indentsize;
	indentStr = makeIndentStr(currentIndent);
    }

    public void startOp(int options, int penalty) {
	currentBP = (BreakPoint) currentBP.childBPs.lastElement();
	currentBP.startOp(options, penalty, currentLine.length());
    }

    public void breakOp() {
	int pos = currentLine.length();
	if (pos > currentBP.startPos && currentLine.charAt(pos-1) == ' ')
	    pos--;
	currentBP.breakOp(pos);
    }

    public void endOp() {
	currentBP.endOp(currentLine.length());
	currentBP = currentBP.parentBP;
	if (currentBP == null)
	    throw new NullPointerException();
    }

    public Object saveOps() {
	Stack state = new Stack();
	int pos = currentLine.length();
	while (currentBP.parentBP != null) {
	    state.push(new Integer(currentBP.breakPenalty));
	    /* We don't want parentheses or unconventional line breaking */
	    currentBP.options = DONT_BREAK;
	    currentBP.endPos = pos;
	    currentBP = currentBP.parentBP;
	}
	return state;
    }

    public void restoreOps(Object s) {
	Stack state = (Stack) s;
	while (!state.isEmpty()) {
	    int penalty = ((Integer) state.pop()).intValue();
	    startOp(DONT_BREAK, penalty);
	}
    }

    public void println(String str) {
	print(str);
	println();
    }

    public void flushLine() {
	currentBP.endPos = currentLine.length();

//  	pw.print(indentStr);
//  	currentBP.dump(currentLine.toString());
//  	pw.println();

	int lw = lineWidth - currentIndent;
	int minPenalty = currentBP.getMinPenalty(lw, lw, Integer.MAX_VALUE/2);
	currentBP = currentBP.commitMinPenalty(lw, lw, minPenalty);

//  	pw.print(indentStr);
//  	currentBP.dump(currentLine.toString());
//  	pw.println();
	pw.print(indentStr);
	currentBP.printLines(currentIndent, currentLine.toString());

	currentLine.setLength(0);
	currentBP = new BreakPoint(null, 0);
	currentBP.startOp(DONT_BREAK, 1, 0);
    }

    public void println() {
	flushLine();
	pw.println();
    }

    public void print(String str) {
	currentLine.append(str);
    }

    public void printType(Type type) {
	print(getTypeString(type));
    }

    public void pushScope(Scope scope) {
	scopes.push(scope);
    }

    public void popScope() {
	scopes.pop();
    }

    /**
     * Checks if the name in inScope conflicts with an identifier in a
     * higher scope.
     */
    public boolean conflicts(String name, Scope inScope, int context) {
	int dot = name.indexOf('.');
	if (dot >= 0)
	    name = name.substring(0, dot);
	int count = scopes.size();
	for (int ptr = count; ptr-- > 0; ) {
	    Scope scope = (Scope) scopes.elementAt(ptr);
	    if (scope == inScope)
		return false;
	    if (scope.conflicts(name, context)) {
		return true;
	    }
	}
	return false;
    }

    public Scope getScope(Object obj, int scopeType) {
	int count = scopes.size();
	for (int ptr = count; ptr-- > 0; ) {
	    Scope scope = (Scope) scopes.elementAt(ptr);
	    if (scope.isScopeOf(obj, scopeType))
		return scope;
	}
	return null;
    }

    public String getClassString(ClassInfo clazz, int scopeType) {
	try {
	    clazz.load(ClassInfo.OUTERCLASS);
	} catch (IOException ex) {
	    clazz.guess(ClassInfo.OUTERCLASS);
	}
	if ((Options.options & Options.OPTION_INNER) != 0
	    && clazz.getOuterClass() != null) {
	    
	    String className = clazz.getClassName();
	    Scope scope = getScope(clazz.getOuterClass(), Scope.CLASSSCOPE);
	    if (scope != null && 
		!conflicts(className, scope, scopeType))
		return className;

	    return getClassString(clazz.getOuterClass(), scopeType)
		+ "." + className;
	}

	if ((Options.options & Options.OPTION_ANON) != 0
	    && clazz.isMethodScoped()) {

	    String className = clazz.getClassName();
	    if (className == null)
		return "ANONYMOUS CLASS "+clazz.getName();

	    Scope scope = getScope(clazz, Scope.METHODSCOPE);
	    if (scope != null && 
		!conflicts(className, scope, scopeType))
		return className;

	    if (scope != null)
		return "NAME CONFLICT " + className;
	    return "UNREACHABLE " + className;
	}
	if (imports != null) {
	    String importedName = imports.getClassString(clazz);
	    if (!conflicts(importedName, null, scopeType))
		return importedName;
	}
	String name = clazz.getName();
	if (conflicts(name, null, Scope.AMBIGUOUSNAME))
	    return "PKGNAMECONFLICT "+ name;
	return name;
    }

    public String getTypeString(Type type) {
	if (type instanceof ArrayType)
	    return getTypeString(((ArrayType) type).getElementType()) + "[]";
	else if (type instanceof ClassInfoType) {
	    ClassInfo clazz = ((ClassInfoType) type).getClassInfo();
	    return getClassString(clazz, Scope.CLASSNAME);
	} else if (type instanceof ClassType) {
	    String name = ((ClassType) type).getClassName();
	    if (imports != null) {
		String importedName = imports.getClassString(name);
		if (!conflicts(importedName, null, Scope.CLASSNAME))
		    return importedName;
	    }
	    if (conflicts(name, null, Scope.AMBIGUOUSNAME))
		return "PKGNAMECONFLICT "+ name;
	    return name;
	} else if (type instanceof NullType)
	    return "Object";
	else
	    return type.toString();
    }
    
    public void printOptionalSpace() {
	if ((style & GNU_SPACING) != 0)
	    print(" ");
    }

    /**
     * Print a opening brace with the current indentation style.
     * Called at the end of the line of the instance that opens the
     * brace.  It doesn't do a tab stop after opening the brace.
     */
    public void openBrace() {
	boolean bracePrinted = false;
	if (currentLine.length() > 0) {
	    if ((style & BRACE_AT_EOL) != 0) {
		print(" {");
		bracePrinted = true;
	    }
	    println();
	}
	if ((style & INDENT_BRACES) != 0 && currentIndent > 0)
	    tab();

	if (!bracePrinted)
	    println("{");
    }

    public void openBraceClass() {
	openBraceNoIndent();
    }

    /**
     * Print a opening brace with the current indentation style.
     * Called at the end the line of a method declaration.
     */
    public void openBraceNoIndent() {
	if (currentLine.length() > 0) {
	    if ((style & BRACE_AT_EOL) != 0)
		print(" ");
	    else
		println();
	}
	println("{");
    }

    /**
     * Print an opening brace with the current indentation style.
     * Called at the end of the line of the instance that opens the
     * brace.  It doesn't do a tab stop after opening the brace.
     */
    public void openBraceNoSpace() {
	boolean bracePrinted = false;
	if (currentLine.length() > 0) {
	    if ((style & BRACE_AT_EOL) != 0) {
		print("{");
		bracePrinted = true;
	    }
	    println();
	}
	if ((style & INDENT_BRACES) != 0 && currentIndent > 0) 
	    tab();
	if (!bracePrinted)
	    println("{");
    }

    public void closeBraceContinue() {
	if ((style & (BRACE_AT_EOL|CODD_FORMATTING)) == BRACE_AT_EOL)
	    print("} ");
	else
	    println("}");
	if ((style & INDENT_BRACES) != 0 && currentIndent > 0)
	    untab();
    }

    public void closeBraceClass() {
	print("}");
    }

    public void closeBrace() {
	println("}");
	if ((style & INDENT_BRACES) != 0 && currentIndent > 0)
	    untab();
    }

    public void closeBraceNoIndent() {
	println("}");
    }

    public void flush() {
	flushLine();
	pw.flush();
    }

    public void close() {
	flushLine();
	pw.close();
    }
}


class NlRemover extends Writer {
    private int tabWidth;
    private Writer out;
    private int pendingNL;
    private boolean lastWasClBr;
    private int pendingSpace;

    public NlRemover(Writer to, int tabWidth) {
	this.out = to; 
	this.tabWidth = tabWidth; }

    public void close() throws IOException {
	if (out != null) {
	    while (pendingNL > 0) {
		out.write('\n');
		pendingNL--;
	    }
	    out.close();
	    out = null;
	}
    }

    public void flush() throws IOException {
	if (out != null) {
	    while (pendingNL > 0) {
		out.write('\n');
		pendingNL--;
	    }
	    out.flush();
        }
    }

    public void write(int x) throws IOException  {
	switch ((char)x) {

	case '}':
	    if (! lastWasClBr && pendingSpace > 0)
		out.write(' ');
	    out.write('}');
		pendingSpace = 0;
		pendingNL = 0;
		lastWasClBr = true;
		return;
	case '\r':
	    pendingSpace = 0;
	    return;
	case '\n':
	    if (pendingNL > 0) {
		out.write('\n'); }
	    else
		pendingNL++;
	    pendingSpace = 0;
	    return;
	case '\t':
	    pendingSpace = (pendingSpace + tabWidth) / tabWidth * tabWidth;
	    return;
	case ' ':
	    pendingSpace += 1;
	    return;
	default: 
	    while (pendingNL > 0) {
		out.write('\n');
		pendingNL--;
	    }
	    while (pendingSpace > 0) {
		out.write(' ');
		pendingSpace--;
	    }
	    out.write(x);
	    lastWasClBr = false;
	}
    }

    public void write(char[] cbuf, int off, int len) throws IOException  {
	len+=off;
	while (off < len)
	    write(cbuf[off++]);
    }
}
