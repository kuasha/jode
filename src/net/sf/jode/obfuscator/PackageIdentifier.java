/* PackageIdentifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: PackageIdentifier.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.obfuscator;
import net.sf.jode.GlobalOptions;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

///#def COLLECTIONS java.util
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;
///#enddef

public class PackageIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier parent;
    String name;
    String fullName;

    boolean loadOnDemand;
    boolean initialized = false;
    Map loadedClasses;
    List swappedClasses;
    Random rand = new Random();

    public PackageIdentifier(ClassBundle bundle, 
			     PackageIdentifier parent,
			     String fullName, String name) {
	super(name);
	this.bundle = bundle;
	this.parent = parent;
	this.fullName = fullName;
	this.name = name;
	this.loadedClasses = new HashMap();
    }

    /**
     * Marks the parent package as preserved, too.
     */
    protected void setSinglePreserved() {
	if (parent != null)
	    parent.setPreserved();
    }

    public void setLoadOnDemand() {
	if (loadOnDemand)
	    return;
	loadOnDemand = true;
	if ((Main.stripping & Main.STRIP_UNREACH) == 0) {
	    String fullNamePrefix =
		(fullName.length() > 0) ? fullName + "." : "";

	    // Load all classes and packages now, so they don't get stripped
	    Enumeration enumeration = 
		bundle.getClassPath().listClassesAndPackages(getFullName());
	    while (enumeration.hasMoreElements()) {
		String subclazz = ((String)enumeration.nextElement()).intern();
		if (loadedClasses.containsKey(subclazz))
		    continue;
		String subFull = (fullNamePrefix + subclazz).intern();
		
		if (bundle.getClassPath().isPackage(subFull)) {
		    PackageIdentifier ident = new PackageIdentifier
			(bundle, this, subFull, subclazz);
		    loadedClasses.put(subclazz, ident);
		    swappedClasses = null;
		    ident.setLoadOnDemand();
		} else {
		    ClassIdentifier ident = new ClassIdentifier
			(this, subFull, subclazz, 
			 bundle.getClassPath().getClassInfo(subFull));
		    
		    if (GlobalOptions.verboseLevel > 1)
			GlobalOptions.err.println("preloading Class "
						  + subFull);
		    loadedClasses.put(subclazz, ident);
		    swappedClasses = null;
		    bundle.addClassIdentifier(ident);
		}
	    }
	    // Everything is loaded, we don't need to load on demand anymore.
	    loadOnDemand = false;
	}
    }

    public void loadMatchingClasses(IdentifierMatcher matcher) {
	String component = matcher.getNextComponent(this);
	if (component != null) {
	    Identifier ident = (Identifier) loadedClasses.get(component);
	    if (ident == null) {
		component = component.intern();
		String subFull = (fullName.length() > 0)
		    ? fullName + "."+ component : component;
		subFull = subFull.intern();
		if (bundle.getClassPath().isPackage(subFull)) {
		    ident = new PackageIdentifier(bundle, this, 
						  subFull, component);
		    loadedClasses.put(component, ident);
		    swappedClasses = null;
		    if (loadOnDemand)
			((PackageIdentifier) ident).setLoadOnDemand();
		    if (initialized)
			((PackageIdentifier) ident).initialize();
		} else if (bundle.getClassPath().existsClass(subFull)) {
		    if (GlobalOptions.verboseLevel > 1)
			GlobalOptions.err.println("loading Class " +subFull);
		    ident = new ClassIdentifier(this, subFull, component, 
						bundle.getClassPath()
						.getClassInfo(subFull));
		    if (loadOnDemand || matcher.matches(ident)) {
			loadedClasses.put(component, ident);
			if (initialized)
			    ((ClassIdentifier) ident).initClass();
			swappedClasses = null;
			bundle.addClassIdentifier(ident);
		    }
		} else {
		    GlobalOptions.err.println
			("Warning: Can't find class/package " + subFull);
		}
	    }
	    if (ident instanceof PackageIdentifier) {
		if (matcher.matches(ident)) {
		    if (GlobalOptions.verboseLevel > 0)
			GlobalOptions.err.println("loading Package "
						  +ident.getFullName());
		    ((PackageIdentifier) ident).setLoadOnDemand();
		}

		if (matcher.matchesSub(ident, null))
		    ((PackageIdentifier) ident).loadMatchingClasses(matcher);
	    }
	} else {
	    String fullNamePrefix = 
		(fullName.length() > 0) ? fullName + "." : "";
	    /* Load all matching classes and packages */
	    Enumeration enumeration = 
		bundle.getClassPath().listClassesAndPackages(getFullName());
	    while (enumeration.hasMoreElements()) {
		String subclazz = ((String)enumeration.nextElement()).intern();
		if (loadedClasses.containsKey(subclazz))
		    continue;
		String subFull = (fullNamePrefix + subclazz).intern();
		
		if (matcher.matchesSub(this, subclazz)) {
		    if (bundle.getClassPath().isPackage(subFull)) {
			if (GlobalOptions.verboseLevel > 0)
			    GlobalOptions.err.println("loading Package "
						      + subFull);
			PackageIdentifier ident = new PackageIdentifier
			    (bundle, this, subFull, subclazz);
			loadedClasses.put(subclazz, ident);
			swappedClasses = null;
			if (loadOnDemand || matcher.matches(ident))
			    ident.setLoadOnDemand();
			if (initialized)
			    ident.initialize();
		    } else {
			ClassIdentifier ident = new ClassIdentifier
			    (this, subFull, subclazz, 
			     bundle.getClassPath().getClassInfo(subFull));

			if (loadOnDemand || matcher.matches(ident)) {
			    if (GlobalOptions.verboseLevel > 1)
				GlobalOptions.err.println("loading Class "
							  + subFull);
			    loadedClasses.put(subclazz, ident);
			    swappedClasses = null;
			    bundle.addClassIdentifier(ident);
			    if (initialized)
				ident.initClass();
			}
		    }
		}
	    }
	    List list = new ArrayList();
	    list.addAll(loadedClasses.values());
	    for (Iterator i = list.iterator(); i.hasNext(); ) {
		Identifier ident = (Identifier) i.next();
		if (ident instanceof PackageIdentifier) {
		    if (matcher.matches(ident))
			((PackageIdentifier) ident).setLoadOnDemand();
		    
		    if (matcher.matchesSub(ident, null))
			((PackageIdentifier) ident)
			    .loadMatchingClasses(matcher);
		}
	    }
	}
    }

    public void initialize() {
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ident instanceof ClassIdentifier)
		((ClassIdentifier) ident).initClass();
	    else
		((PackageIdentifier) ident).initialize();
	}
	initialized = true;
    }

    public Identifier getIdentifier(String name) {
	if (loadOnDemand) {
	    return loadClass(name);
	}
	int index = name.indexOf('.');
	if (index == -1) {
	    return (Identifier) loadedClasses.get(name);
	} else {
	    PackageIdentifier pack = (PackageIdentifier) 
		loadedClasses.get(name.substring(0, index));
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    private Identifier loadClass(String name) {
	int index = name.indexOf('.');
	if (index == -1) {
	    Identifier ident = (Identifier) loadedClasses.get(name);
	    if (ident == null) {
		String subFull = 
		   (fullName.length() > 0) ? fullName + "."+ name : name;
		subFull = subFull.intern();
		if (bundle.getClassPath().isPackage(subFull)) {
		    PackageIdentifier pack
			= new PackageIdentifier(bundle, this, subFull, name);
		    loadedClasses.put(name, pack);
		    swappedClasses = null;
		    pack.setLoadOnDemand();
		    ident = pack;
		} else if (!bundle.getClassPath().existsClass(subFull)) {
		    GlobalOptions.err.println("Warning: Can't find class "
					      + subFull);
		    Thread.dumpStack();
		} else {
		    ident = new ClassIdentifier(this, subFull, name, 
						bundle.getClassPath()
						.getClassInfo(subFull));
		    loadedClasses.put(name, ident);
		    ((ClassIdentifier) ident).initClass();
		    swappedClasses = null;
		    bundle.addClassIdentifier(ident);
		}
	    }
	    return ident;
	} else {
	    String subpack = name.substring(0, index);
	    PackageIdentifier pack = 
		(PackageIdentifier) loadedClasses.get(subpack);
	    if (pack == null) {
		String subFull = (fullName.length() > 0)
		    ? fullName + "."+ subpack : subpack;
		subFull = subFull.intern();
		if (bundle.getClassPath().isPackage(subFull)) {
		    pack = new PackageIdentifier(bundle, this, 
						 subFull, subpack);
		    loadedClasses.put(subpack, pack);
		    swappedClasses = null;
		    if (loadOnDemand)
			pack.setLoadOnDemand();
		}
	    }
	    
	    if (pack != null)
		return pack.loadClass(name.substring(index+1));
	    else
		return null;
	}
    }

    public void applyPreserveRule(IdentifierMatcher preserveRule) {
	if (loadOnDemand)
	    loadMatchingClasses(preserveRule);
	super.applyPreserveRule(preserveRule);
    }

    /**
     * @return the full qualified name.
     */
    public String getFullName() {
	return fullName;
    }

    /**
     * @return the full qualified alias.
     */
    public String getFullAlias() {
	if (parent != null) {
	    String parentAlias = parent.getFullAlias();
	    String alias = getAlias();
	    if (alias.length() == 0)
		return parentAlias;
	    else if (parentAlias.length() == 0)
		return alias;
	    else
		return parentAlias + "." + alias;
	}
	return "";
    }

    public String findAlias(String className) {
	int index = className.indexOf('.');
	if (index == -1) {
	    Identifier ident = getIdentifier(className);
	    if (ident != null)
		return ident.getFullAlias();
	} else {
	    Identifier pack = getIdentifier(className.substring(0, index));
	    if (pack != null)
		return ((PackageIdentifier)pack)
		    .findAlias(className.substring(index+1));
	}
	return className;
    }

    public void buildTable(Renamer renameRule) {
	loadOnDemand = false;
	super.buildTable(renameRule);
    }

    public void doTransformations() {
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ident instanceof ClassIdentifier) {
		((ClassIdentifier) ident).doTransformations();
	    } else
		((PackageIdentifier) ident).doTransformations();
	}
    }

    public void readTable(Map table) {
	if (parent != null)
	    setAlias((String) table.get(getFullName()));
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| ident.isReachable())
		ident.readTable(table);
	}
    }

    public Identifier getParent() {
	return parent;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "package";
    }

    public Iterator getChilds() {
	/* Since loadedClasses is somewhat sorted by the hashcode
	 * of the _original_ names, swap it here to prevent to guess
	 * even parts of the names.
	 */
	if (swappedClasses == null) {
	    swappedClasses = Arrays.asList(loadedClasses.values().toArray());
	    Collections.shuffle(swappedClasses, rand);
	}
	return swappedClasses.iterator();
    }

    public void storeClasses(ZipOutputStream zip) {
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0
		&& !ident.isReachable()) {
		if (GlobalOptions.verboseLevel > 4)
		    GlobalOptions.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident).storeClasses(zip);
	    else {
		try {
		    String filename = ident.getFullAlias().replace('.','/')
			+ ".class";
		    zip.putNextEntry(new ZipEntry(filename));
		    DataOutputStream out = new DataOutputStream
			(new BufferedOutputStream(zip));
		    ((ClassIdentifier) ident).storeClass(out);
		    out.flush();
		    zip.closeEntry();
		} catch (java.io.IOException ex) {
		    GlobalOptions.err.println("Can't write Class "
					      + ident.getName());
		    ex.printStackTrace(GlobalOptions.err);
		}
	    }
	}
    }

    public void storeClasses(File destination) {
	File newDest = (parent == null) ? destination 
	    : new File(destination, getAlias());
	if (!newDest.exists() && !newDest.mkdir()) {
	    GlobalOptions.err.println("Could not create directory "
				   +newDest.getPath()+", check permissions.");
	}
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0
		&& !ident.isReachable()) {
		if (GlobalOptions.verboseLevel > 4)
		    GlobalOptions.err.println("Class/Package "
					      + ident.getFullName()
					      + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident)
		    .storeClasses(newDest);
	    else {
		try {
		    File file = new File(newDest, ident.getAlias()+".class");
// 		    if (file.exists()) {
// 			GlobalOptions.err.println
// 			    ("Refuse to overwrite existing class file "
// 			     +file.getPath()+".  Remove it first.");
// 			return;
// 		    }
		    DataOutputStream out = new DataOutputStream
			(new BufferedOutputStream
			 (new FileOutputStream(file)));
		    ((ClassIdentifier) ident).storeClass(out);
		    out.close();
		} catch (java.io.IOException ex) {
		    GlobalOptions.err.println("Can't write Class "
					   + ident.getName());
		    ex.printStackTrace(GlobalOptions.err);
		}
	    }
	}
    }

    public String toString() {
	return (parent == null) ? "base package" : getFullName();
    }

    public boolean contains(String newAlias, Identifier except) {
	for (Iterator i = loadedClasses.values().iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier)i.next();
	    if (ident != except) {
		if (((Main.stripping & Main.STRIP_UNREACH) == 0
		     || ident.isReachable())
			&& ident.getAlias().equalsIgnoreCase(newAlias))
		    return true;
		if (ident instanceof PackageIdentifier
		    && ident.getAlias().length() == 0
		    && (((PackageIdentifier) ident)
			.contains(newAlias, this)))
		    return true;
	    }
	}
	if (getAlias().length() == 0
	    && parent != null
	    && parent != except
	    && parent.contains(newAlias, this))
	    return true;
	return false;
    }

    public boolean conflicting(String newAlias) {
	return parent.contains(newAlias, this);
    }
}
