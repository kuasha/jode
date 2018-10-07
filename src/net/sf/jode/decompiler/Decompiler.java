/* Decompiler Copyright (C) 2000-2002 Jochen Hoenicke.
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
 * $Id: Decompiler.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.decompiler;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.ClassPath;
import net.sf.jode.bytecode.ClassInfo;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * This is the interface that other java classes may use to decompile
 * classes.  Feel free to use it in your own GNU GPL'ed project.
 * Please tell me about your project.<br>
 *
 * Note that the GNU GPL doesn't allow you to use this interface in
 * commercial programs. 
 * 
 * @author <a href="mailto:jochen@gnu.org">Jochen Hoenicke</a>
 * @version 1.0
 */
public class Decompiler {
    private ClassPath classPath = null;
    private int importPackageLimit = ImportHandler.DEFAULT_PACKAGE_LIMIT;
    private int importClassLimit = ImportHandler.DEFAULT_CLASS_LIMIT;

    private int tabWidth    = 8;
    private int indentSize  = 4;
    private int outputStyle = TabbedPrintWriter.BRACE_AT_EOL;
    private int lineWidth   = 79;

    /**
     * We need a different pathSeparatorChar, since ':' (used for most
     * UNIX System) is used a protocol separator in URLs.  
     *
     * We currently allow both pathSeparatorChar and
     * altPathSeparatorChar and decide if it is a protocol separator
     * by context.
     */
    public static final char altPathSeparatorChar
	= ClassPath.altPathSeparatorChar;

    /**
     * Create a new decompiler.  Normally you need only one, but you
     * can have more around, with different options and different
     * class paths.
     */
    public Decompiler() {
    }
    
    /**
     * Sets the class path.  Should be called once before decompile is
     * called, otherwise the system class path is used.
     * @param classpath A comma separated classpath.
     * @exception NullPointerException if classpath is null.
     * @see #setClassPath(String[])
     */
    public void setClassPath(String classpath) {
	this.classPath = new ClassPath(classpath);
    }

    /**
     * Set the class path.  Should be called once before decompile is
     * called, otherwise the system class path is used.
     * @param classpath a non empty array of jar files and directories; 
     *                  URLs are allowed, too.
     * @exception NullPointerException if classpath is null.
     * @exception IndexOutOfBoundsException if classpath array is empty.
     * @see #setClassPath(String)
     */
    public void setClassPath(String[] classpath) {
	this.classPath = new ClassPath(classpath);
    }

    /**
     * Set the class path.  Should be called once before decompile is
     * called, otherwise the system class path is used.
     * @param classpath a classpath object.
     * @exception NullPointerException if classpath is null.
     * @exception IndexOutOfBoundsException if classpath array is empty.
     * @see #setClassPath(String)
     */
    public void setClassPath(ClassPath classpath) {
	this.classPath = classpath;
    }

    private static final String[] optionStrings = {
	"lvt", "inner", "anonymous", "push", "pretty", "decrypt",
	"onetime", "immediate", "verify", "contrafo"
    };

    /**
     * Set an option.
     * @param option the option (pretty, style, decrypt, verify, etc.)
     * @param value ("1"/"0" for on/off, "sun"/"gnu" for style)
     * @exception IllegalArgumentException if option or value is invalid.
     */
    public void setOption(String option, String value) {
	if (option.equals("style")) {
	    if (value.equals("gnu")) {
		outputStyle = TabbedPrintWriter.GNU_SPACING
		    | TabbedPrintWriter.INDENT_BRACES;
		indentSize = 2;
	    } else if (value.equals("sun")) {
		outputStyle = TabbedPrintWriter.BRACE_AT_EOL;
		indentSize = 4;
	    } else if (value.equals("pascal")) {
		outputStyle = 0;
		indentSize = 4;
	    } else
		throw new IllegalArgumentException("Invalid style "+value);
	    return;
	}
	if (option.equals("tabwidth")) {
	    tabWidth = Integer.parseInt(value);
	    return;
	}
	if (option.equals("indent")) {
	    indentSize = Integer.parseInt(value);
	    return;
	}
	if (option.equals("linewidth")) {
	    lineWidth = Integer.parseInt(value);
	    return;
	}
	if (option.equals("import")) {
	    int comma = value.indexOf(',');
	    int packLimit = Integer.parseInt(value.substring(0, comma));
	    if (packLimit == 0)
		packLimit = Integer.MAX_VALUE;
	    int clazzLimit = Integer.parseInt(value.substring(comma+1));
	    if (clazzLimit == 0)
		clazzLimit = Integer.MAX_VALUE;
	    if (clazzLimit < 0 || packLimit < 0)
		throw new IllegalArgumentException
		    ("Option import doesn't allow negative parameters");
	    importPackageLimit = packLimit;
	    importClassLimit = clazzLimit;
	    return;
	}
	if (option.equals("verbose")) {
	    GlobalOptions.verboseLevel = Integer.parseInt(value);
	    return;
	}
	if (option.equals("debug")) {
	    GlobalOptions.setDebugging(value);
	    return;
	}
	for (int i=0; i < optionStrings.length; i++) {
	    if (option.equals(optionStrings[i])) {
		if (value.equals("0") 
		    || value.equals("off")
		    || value.equals("no"))
		    Options.options &= ~(1 << i);
		else if (value.equals("1") 
			 || value.equals("on")
			 || value.equals("yes"))
		    Options.options |= 1 << i;
		else
		    throw new IllegalArgumentException("Illegal value for "+
						       option);
		return;
	    }
	}
	throw new IllegalArgumentException("Illegal option: "+option);
    }
    
    
    /**
     * Set the stream where copyright and warnings/errors are printed
     * to.
     * @param errorStream the error stream. Note that this is a
     * PrintWriter, not a PrintStream (which are deprecated since 1.1).
     */
    public void setErr(PrintWriter errorStream) {
	GlobalOptions.err = errorStream;
    }

   /**
    * Decompile a class.
    * @param className full-qualified classname, dot separated, e.g. 
    *             "java.lang.Object"
    * @param writer The stream where the decompiled code should be
    *        written.  Hint:  Use a BufferedWriter for good performance.
    * @param progress A progress listener (see below).  Null if you
    *        don't need information about progress.
    * @exception IllegalArgumentException if className isn't correct.
    * @exception IOException if writer throws an exception.
    * @exception RuntimeException If jode has a bug ;-)
    */
   public void decompile(String className, Writer writer,	
			 ProgressListener progress) 
     throws java.io.IOException {
       if (classPath == null) {
	   String cp = System.getProperty("java.class.path");
	   String bootcp = System.getProperty("sun.boot.class.path");
	   if (bootcp != null)
	       cp = bootcp + altPathSeparatorChar + cp;
	   cp = cp.replace(File.pathSeparatorChar, altPathSeparatorChar);
	   classPath = new ClassPath(cp);
       }

       ClassInfo clazz = classPath.getClassInfo(className);
       ImportHandler imports = new ImportHandler(classPath,
						 importPackageLimit,
						 importClassLimit);
       TabbedPrintWriter tabbedWriter = 
	   new TabbedPrintWriter(writer, imports, false, 
				 outputStyle, indentSize, 
				 tabWidth, lineWidth);
       ClassAnalyzer clazzAna = new ClassAnalyzer(null, clazz, imports);
       clazzAna.dumpJavaFile(tabbedWriter, progress);
       writer.flush();
   }
}
