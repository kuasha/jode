/* ClassBundle Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ClassBundle.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.obfuscator;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.ClassPath;
import net.sf.jode.bytecode.Reference;
import net.sf.jode.obfuscator.modules.WildCard;
import net.sf.jode.obfuscator.modules.MultiIdentifierMatcher;
import net.sf.jode.obfuscator.modules.SimpleAnalyzer;
import net.sf.jode.obfuscator.modules.IdentityRenamer;
import java.io.*;
import java.util.zip.ZipOutputStream;

///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
///#enddef

///#ifdef JDK12
///#def COLLECTIONS java.util
import java.util.WeakHashMap;
///#enddef
///#endif

public class ClassBundle implements OptionHandler {
    PackageIdentifier basePackage;

    /**
     * the identifiers that must be analyzed.
     */
    Set toAnalyze = new HashSet();

    ClassPath classPath;
    String destDir;

    String tableFile;
    String toTableFile;

    IdentifierMatcher loading;
    IdentifierMatcher preserving;
    IdentifierMatcher reaching;
    CodeTransformer[] preTrafos;
    CodeAnalyzer      analyzer;
    CodeTransformer[] postTrafos;
    Renamer           renamer;


    public ClassBundle() {
	destDir = ".";
	basePackage = new PackageIdentifier(this, null, "", "");
	basePackage.setReachable();
	basePackage.setPreserved();
    }

///#ifdef JDK12
    private static final Map aliasesHash = new WeakHashMap();
///#else
///    private static final Map aliasesHash = new HashMap();
///#endif
    private static final Map clazzCache = new HashMap();
    private static final Map referenceCache = new HashMap();

    public static void setStripOptions(Collection stripString) {
    }
    public void setOption(String option, Collection values) {
	if (option.equals("classpath")) {
	    Iterator i = values.iterator();
	    StringBuffer sb = new StringBuffer((String) i.next());
	    while (i.hasNext()) {
		sb.append(ClassPath.altPathSeparatorChar)
		    .append((String)i.next());
	    }
	    classPath = new ClassPath(sb.toString());
	    return;
	}
	    
	if (option.equals("dest")) {
	    if (values.size() != 1)
		throw new IllegalArgumentException
		    ("Only one destination path allowed");
	    destDir = (String) values.iterator().next();
	    return;
	}

	if (option.equals("table")) {
	    if (values.size() != 1)
		throw new IllegalArgumentException
		    ("Only one destination path allowed");
	    tableFile = (String) values.iterator().next();
	    return;
	}

	if (option.equals("revtable")) {
	    if (values.size() != 1)
		throw new IllegalArgumentException
		    ("Only one destination path allowed");
	    toTableFile = (String) values.iterator().next();
	    return;
	}
	if (option.equals("strip")) {
	next_token:
	    for (Iterator iter = values.iterator(); iter.hasNext(); ) {
		String token = (String) iter.next();
		for (int i=0; i < Main.stripNames.length; i++) {
		    if (token.equals(Main.stripNames[i])) {
			Main.stripping |= 1 << i;
			continue next_token;
		    }
		}
		throw new IllegalArgumentException("Unknown strip option: `"
						   +token+"'");
	    }
	    return;
	}

	if (option.equals("load")) {
	    if (values.size() == 1) {
		Object value = values.iterator().next();
		if (value instanceof String)
		    loading = new WildCard((String)value);
		else
		    loading = (IdentifierMatcher) value;
	    } else {
		IdentifierMatcher[] matchers
		    = new IdentifierMatcher[values.size()];
		int j = 0;
		for (Iterator i = values.iterator(); i.hasNext(); ) {
		    Object value = i.next();
		    matchers[j++] = (value instanceof String
				     ? new WildCard((String)value)
				     : (IdentifierMatcher) value);
		}
		loading = new MultiIdentifierMatcher
		    (MultiIdentifierMatcher.OR, matchers);
	    }
	    return;
	}

	if (option.equals("preserve")) {
	    if (values.size() == 1) {
		Object value = values.iterator().next();
		if (value instanceof String)
		    preserving = new WildCard((String)value);
		else
		    preserving = (IdentifierMatcher) value;
	    } else {
		IdentifierMatcher[] matchers
		    = new IdentifierMatcher[values.size()];
		int j = 0;
		for (Iterator i = values.iterator(); i.hasNext(); ) {
		    Object value = i.next();
		    matchers[j++] = (value instanceof String
				     ? new WildCard((String)value)
				     : (IdentifierMatcher) value);
		}
		preserving = new MultiIdentifierMatcher
		    (MultiIdentifierMatcher.OR, matchers);
	    }
	    return;
	}

	if (option.equals("reach")) {
	    // NOT IMPLEMENTED YET
	    if (values.size() == 1) {
		Object value = values.iterator().next();
		if (value instanceof String)
		    reaching = new WildCard((String)value);
		else
		    reaching = (IdentifierMatcher) value;
	    } else {
		IdentifierMatcher[] matchers
		    = new IdentifierMatcher[values.size()];
		int j = 0;
		for (Iterator i = values.iterator(); i.hasNext(); ) {
		    Object value = i.next();
		    matchers[j++] = (value instanceof String
				     ? new WildCard((String)value)
				     : (IdentifierMatcher) value);
		}
		reaching = new MultiIdentifierMatcher
		    (MultiIdentifierMatcher.OR, matchers);
	    }
	}

	if (option.equals("pre")) {
	    preTrafos = (CodeTransformer[])
		values.toArray(new CodeTransformer[values.size()]);
	    return;
	}
	if (option.equals("analyzer")) {
	    if (values.size() != 1)
		throw new IllegalArgumentException
		    ("Only one analyzer is allowed");
	    analyzer = (CodeAnalyzer) values.iterator().next();
	    return;
	}
	if (option.equals("post")) {
	    postTrafos = (CodeTransformer[])
		values.toArray(new CodeTransformer[values.size()]);
	    return;
	}

	if (option.equals("renamer")) {
	    if (values.size() != 1)
		throw new IllegalArgumentException
		    ("Only one renamer allowed");
	    renamer = (Renamer) values.iterator().next();
	    return;
	}
	throw new IllegalArgumentException("Invalid option `"+option+"'.");
    }

    public Reference getReferenceAlias(Reference ref) {
	Reference alias = (Reference) aliasesHash.get(ref);
	if (alias == null) {
	    Identifier ident = getIdentifier(ref);
	    String newType = getTypeAlias(ref.getType());
	    if (ident == null)
		alias = Reference.getReference
		    (ref.getClazz(), ref.getName(), newType);
	    else 
		alias = Reference.getReference
		    ("L"+ident.getParent().getFullAlias().replace('.','/')+';',
		     ident.getAlias(), newType);
	    aliasesHash.put(ref, alias);
	}
	return alias;
    }

    public String getTypeAlias(String typeSig) {
	String alias = (String) aliasesHash.get(typeSig);
	if (alias == null) { 
	    StringBuffer newSig = new StringBuffer();
	    int index = 0, nextindex;
	    while ((nextindex = typeSig.indexOf('L', index)) != -1) {
		newSig.append(typeSig.substring(index, nextindex+1));
		index = typeSig.indexOf(';', nextindex);
		String typeAlias = basePackage.findAlias
		    (typeSig.substring(nextindex+1, index).replace('/','.'));
		newSig.append(typeAlias.replace('.', '/'));
	    }
	    alias = newSig.append(typeSig.substring(index))
		.toString().intern();
	    aliasesHash.put(typeSig, alias);
	}
	return alias;
    }

    public void addClassIdentifier(Identifier ident) {
    }

    public ClassPath getClassPath() {
	return classPath;
    }

    public ClassIdentifier getClassIdentifier(String name) {
	if (clazzCache.containsKey(name))
	    return (ClassIdentifier) clazzCache.get(name);
	ClassIdentifier ident
	    = (ClassIdentifier) basePackage.getIdentifier(name);
	clazzCache.put(name, ident);
	return ident;
    }

    public Identifier getIdentifier(Reference ref) {
	if (referenceCache.containsKey(ref))
	    return (Identifier) referenceCache.get(ref);

	String clName = ref.getClazz();
	if (clName.charAt(0) == '[')
	    /* Can't represent arrays */
	    return null;
	ClassIdentifier clazzIdent =
	    getClassIdentifier(clName.substring(1, clName.length()-1)
			       .replace('/','.'));
	Identifier ident = 
	    clazzIdent == null ? null
	    : clazzIdent.getIdentifier(ref.getName(), ref.getType());
	referenceCache.put(ref, ident);
	return ident;
    }

    public void reachableClass(String clazzName) {
	ClassIdentifier ident = getClassIdentifier(clazzName);
	if (ident != null)
	    ident.setReachable();
    }

    public void reachableReference(Reference ref, boolean isVirtual) {
	String clName = ref.getClazz();
	if (clName.charAt(0) == '[')
	    /* Can't represent arrays */
	    return;
	ClassIdentifier ident =
	    getClassIdentifier(clName.substring(1, clName.length()-1)
			       .replace('/','.'));
	if (ident != null)
	    ident.reachableReference(ref, isVirtual);
    }

    public void analyzeIdentifier(Identifier ident) {
	if (ident == null)
	    throw new NullPointerException();
	toAnalyze.add(ident);
    }

    public void analyze() {
	while(!toAnalyze.isEmpty()) {
	    Identifier ident = (Identifier) toAnalyze.iterator().next();
	    toAnalyze.remove(ident);
	    ident.analyze();
	}
    }

    public IdentifierMatcher getPreserveRule() {
	return preserving;
    }

    public CodeAnalyzer getCodeAnalyzer() {
	return analyzer;
    }

    public CodeTransformer[] getPreTransformers() {
	return preTrafos;
    }
    
    public CodeTransformer[] getPostTransformers() {
	return postTrafos;
    }
    
    public void buildTable(Renamer renameRule) {
	basePackage.buildTable(renameRule);
    }

    public void readTable() {
	try {
	    TranslationTable table = new TranslationTable();
	    InputStream input = new FileInputStream(tableFile);
	    table.load(input);
	    input.close();
	    basePackage.readTable(table);
	} catch (java.io.IOException ex) {
	    GlobalOptions.err.println("Can't read rename table " + tableFile);
	    ex.printStackTrace(GlobalOptions.err);
	}
    }

    public void writeTable() {
	TranslationTable table = new TranslationTable();
	basePackage.writeTable(table);
	try {
	    OutputStream out = new FileOutputStream(toTableFile);
	    table.store(out);
	    out.close();
	} catch (java.io.IOException ex) {
	    GlobalOptions.err.println("Can't write rename table "+toTableFile);
	    ex.printStackTrace(GlobalOptions.err);
	}
    }

    public void doTransformations() {
	basePackage.doTransformations();
    }
    
    public void storeClasses() {
	if (destDir.endsWith(".jar") ||
	    destDir.endsWith(".zip")) {
	    try {
		ZipOutputStream zip = new ZipOutputStream
		    (new FileOutputStream(destDir));
		basePackage.storeClasses(zip);
		zip.close();
	    } catch (IOException ex) {
		GlobalOptions.err.println
		    ("Can't write zip file: "+destDir);
		ex.printStackTrace(GlobalOptions.err);
	    }
	} else {
	    File directory = new File(destDir);
	    if (!directory.exists()) {
		GlobalOptions.err.println("Destination directory "
					  +directory.getPath()
					  +" doesn't exists.");
		return;
	    }
	    basePackage.storeClasses(new File(destDir));
	}
    }

    public void run() {
	if (classPath == null) {
	    String cp = System.getProperty("java.class.path")
		.replace(File.pathSeparatorChar, 
			 ClassPath.altPathSeparatorChar);
	    classPath = new ClassPath(cp);
	}

	if (analyzer == null)
	    analyzer = new SimpleAnalyzer();
	if (preTrafos == null)
	    preTrafos = new CodeTransformer[0];
	if (postTrafos == null)
	    postTrafos = new CodeTransformer[0];
	if (renamer == null)
	    renamer = new IdentityRenamer();

	Runtime runtime = Runtime.getRuntime();
	long free = runtime.freeMemory();
	long last;
	do {
	    last = free;
	    runtime.gc();
	    runtime.runFinalization();
	    free = runtime.freeMemory();
	} while (free < last);
	System.err.println("used before: "+(runtime.totalMemory()- free));

	GlobalOptions.err.println("Loading and preserving classes");

	long time = System.currentTimeMillis();
	basePackage.loadMatchingClasses(loading);
	basePackage.initialize();
	basePackage.applyPreserveRule(preserving);
	System.err.println("Time used: "+(System.currentTimeMillis() - time));


	GlobalOptions.err.println("Computing reachability");
	time = System.currentTimeMillis();
	analyze();
	System.err.println("Time used: "+(System.currentTimeMillis() - time));

	free = runtime.freeMemory();
	do {
	    last = free;
	    runtime.gc();
	    runtime.runFinalization();
	    free = runtime.freeMemory();
	} while (free < last);
	System.err.println("used after analyze: "
			   + (runtime.totalMemory() - free));

	GlobalOptions.err.println("Renaming methods");
	time = System.currentTimeMillis();
	if (tableFile != null)
            readTable();
	buildTable(renamer);
        if (toTableFile != null)
            writeTable();
	System.err.println("Time used: "+(System.currentTimeMillis() - time));

	GlobalOptions.err.println("Transforming the classes");
	time = System.currentTimeMillis();
	doTransformations();
	System.err.println("Time used: "+(System.currentTimeMillis() - time));

	free = runtime.freeMemory();
	do {
	    last = free;
	    runtime.gc();
	    runtime.runFinalization();
	    free = runtime.freeMemory();
	} while (free < last);
	System.err.println("used after transform: "
			   + (runtime.totalMemory() - free));

	GlobalOptions.err.println("Writing new classes");
	time = System.currentTimeMillis();
        storeClasses();
	System.err.println("Time used: "+(System.currentTimeMillis() - time));
    }
}
