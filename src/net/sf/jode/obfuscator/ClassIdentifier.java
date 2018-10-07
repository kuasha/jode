/* ClassIdentifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ClassIdentifier.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.obfuscator;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.*;
import net.sf.jode.obfuscator.modules.ModifierMatcher;
///#def COLLECTIONS java.util
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.UnsupportedOperationException;
///#enddef

import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClassIdentifier extends Identifier {
    PackageIdentifier pack;
    String name;
    String fullName;
    ClassInfo info;
    String superName;
    String[] ifaceNames;

    List fieldIdents, methodIdents;
    List knownSubClasses = new LinkedList();
    List virtualReachables = new LinkedList();

    boolean initialized;

    public ClassIdentifier(PackageIdentifier pack, String fullName,
			   String name, ClassInfo info) {
	super(name);
	this.pack = pack;
	this.fullName = fullName;
	this.name = name;
	this.info = info;
    }

    public void addSubClass(ClassIdentifier ci) {
	knownSubClasses.add(ci);
	for(Iterator i = virtualReachables.iterator(); i.hasNext(); )
	    ci.reachableReference((Reference) i.next(), true);
    }

    private FieldIdentifier findField(String name, String typeSig) {
	for (Iterator i = fieldIdents.iterator(); i.hasNext(); ) {
	    FieldIdentifier ident = (FieldIdentifier) i.next();
	    if (ident.getName().equals(name)
		&& ident.getType().equals(typeSig))
		return ident;
	}
	return null;
    }

    private MethodIdentifier findMethod(String name, String typeSig) {
	for (Iterator i = methodIdents.iterator(); i.hasNext(); ) {
	    MethodIdentifier ident = (MethodIdentifier) i.next();
	    if (ident.getName().equals(name)
		&& ident.getType().equals(typeSig))
		return ident;
	}
	return null;
    }

    public void reachableReference(Reference ref, boolean isVirtual) {
	boolean found = false;
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ref.getName().equals(ident.getName())
		&& ref.getType().equals(ident.getType())) {
		ident.setReachable();
		found = true;
	    }
	}
	if (!found) {
	    // This means that the method is inherited from parent and 
	    // must be marked as reachable there, (but not virtual).
	    // Consider following:
	    // A method in Collection and AbstractCollection is not reachable
	    // but it is reachable in Set and not implemented in AbstractSet
	    // In that case the method must be marked reachable in 
	    // AbstractCollection.
	    ClassIdentifier superIdent = Main.getClassBundle()
		.getClassIdentifier(info.getSuperclass().getName());
	    if (superIdent != null)
		superIdent.reachableReference(ref, false);
	}
	    
	if (isVirtual) {
	    for(Iterator i = virtualReachables.iterator(); i.hasNext(); ) {
		Reference prevRef = (Reference) i.next();
		if (prevRef.getName().equals(ref.getName())
		    && prevRef.getType().equals(ref.getType()))
		    // already handled.
		    return;
	    }
	    for (Iterator i = knownSubClasses.iterator(); i.hasNext(); )
		((ClassIdentifier)i.next())
		    .reachableReference(ref, false);
	    virtualReachables.add(ref);
	}
    }

    public void chainMethodIdentifier(Identifier chainIdent) {
	String name = chainIdent.getName();
	String typeSig = chainIdent.getType();
	for (Iterator i = methodIdents.iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ident.getName().equals(name)
		&& ident.getType().equals(typeSig))
		chainIdent.addShadow(ident);
	}
    }

    /**
     * This is partly taken from the classpath project.
     */
    public long calcSerialVersionUID() {
	final MessageDigest md;
	try {
	    md = MessageDigest.getInstance("SHA");
	} catch (NoSuchAlgorithmException ex) {
	    ex.printStackTrace();
	    GlobalOptions.err.println("Can't calculate serialVersionUID");
	    return 0L;
	}
	OutputStream digest = new OutputStream() {

	    public void write(int b) {
		md.update((byte) b);
	    }

	    public void write(byte[] data, int offset, int length) {
		md.update(data, offset, length);
	    }
	};
	DataOutputStream out = new DataOutputStream(digest);
	try {
	    out.writeUTF(info.getName());
	    
	    int modifiers = info.getModifiers();
	    // just look at interesting bits
	    modifiers = modifiers & ( Modifier.ABSTRACT | Modifier.FINAL
				      | Modifier.INTERFACE | Modifier.PUBLIC );
	    out.writeInt(modifiers);
	    
	    ClassInfo[] interfaces
		= (ClassInfo[]) info.getInterfaces().clone();
	    Arrays.sort(interfaces, new Comparator() {
		public int compare( Object o1, Object o2 ) {
		    return ((ClassInfo)o1).getName()
			.compareTo(((ClassInfo)o2).getName());
		}
	    });
	    for( int i=0; i < interfaces.length; i++ ) {
		out.writeUTF(interfaces[i].getName());
	    }
	    
	    
	    Comparator identCmp = new Comparator() {
		public int compare(Object o1, Object o2)  {
		    Identifier i1 = (Identifier)o1;
		    Identifier i2 = (Identifier)o2;
		    String name1 = i1.getName();
		    String name2 = i2.getName();
		    boolean special1 = (name1.equals("<init>")
					|| name1.equals("<clinit>"));
		    boolean special2 = (name2.equals("<init>")
					|| name2.equals("<clinit>"));
		    // Put constructors at the beginning
		    if (special1 != special2) {
			return special1 ? -1 : 1;
		    }

		    int comp = i1.getName().compareTo(i2.getName());
		    if (comp != 0) {
			return comp;
		    } else {
			return i1.getType().compareTo(i2.getType());
		    }
		}
	    };

	    List fields = Arrays.asList(fieldIdents.toArray());
	    List methods = Arrays.asList(methodIdents.toArray());
	    Collections.sort(fields, identCmp);
	    Collections.sort(methods, identCmp);
	    
	    for (Iterator i = fields.iterator(); i.hasNext();) {
		FieldIdentifier field = (FieldIdentifier) i.next();
		modifiers = field.info.getModifiers();
		if ((modifiers & Modifier.PRIVATE) != 0
		    && (modifiers & (Modifier.STATIC 
				     | Modifier.TRANSIENT)) != 0)
		    continue;
		
		out.writeUTF(field.getName());
		out.writeInt(modifiers);
		out.writeUTF(field.getType());
	    }
	    for (Iterator i = methods.iterator(); i.hasNext(); ) {
		MethodIdentifier method = (MethodIdentifier) i.next();
		modifiers = method.info.getModifiers();
		if (Modifier.isPrivate(modifiers))
		    continue;
		
		out.writeUTF(method.getName());
		out.writeInt(modifiers);
		
		// the replacement of '/' with '.' was needed to make computed
		// SUID's agree with those computed by JDK
		out.writeUTF(method.getType().replace('/', '.'));
	    }
	    
	    out.close();

	    byte[] sha = md.digest();
	    long result = 0;
	    for (int i=0; i < 8; i++) {
		result += (long)(sha[i] & 0xFF) << (8 * i);
	    }
	    return result;
	} catch (IOException ex) {
	    ex.printStackTrace();
	    GlobalOptions.err.println("Can't calculate serialVersionUID");
	    return 0L;
	}
    }

    public void addSUID() {
	/* add a field serializableVersionUID if not existent */
	long serialVersion = calcSerialVersionUID();
	FieldInfo UIDField = new FieldInfo
	    ("serialVersionUID", "J", 
	     Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
	UIDField.setConstant(new Long(serialVersion));
	FieldIdentifier UIDident = new FieldIdentifier(this, UIDField);
	fieldIdents.add(UIDident);
	UIDident.setPreserved();
    }

    public boolean isSerializable() {
	try {
	    return info.getClassPath().getClassInfo("java.lang.Serializable")
		.implementedBy(info);
	} catch (IOException ex) {
	    throw new RuntimeException("Can't load full hierarchy of "+info);
	}
    }
    public boolean hasSUID() {
	return (findField("serialVersionUID", "J") != null);
    }
	    
    /**
     * Marks the package as preserved, too.
     */
    protected void setSinglePreserved() {
	pack.setPreserved();
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	Main.getClassBundle().analyzeIdentifier(this);
    }
    
    public void analyzeSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superclass.getName());
	    if (superident != null) {
		superident.addSubClass(this);
	    } else {
		// all virtual methods in superclass are reachable now!
		String clazzType = ("L"+superclass.getName().replace('.', '/')
				    +";").intern();
		MethodInfo[] topmethods = superclass.getMethods();
		for (int i=0; i< topmethods.length; i++) {
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			reachableReference
			    (Reference.getReference(clazzType, 
						    topmethods[i].getName(), 
						    topmethods[i].getType()), 
			     true);
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		analyzeSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void analyze() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Reachable: "+this);

	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++)
	    analyzeSuperClasses(ifaces[i]);
	analyzeSuperClasses(info.getSuperclass());
    }

    public void initSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superclass.getName());
	    if (superident != null) {
		superident.initClass();
		for (Iterator i = superident.getMethodIdents().iterator(); 
		     i.hasNext(); ) {
		    MethodIdentifier mid = (MethodIdentifier) i.next();
		    // all virtual methods in superclass must be chained.
		    int modif = mid.info.getModifiers();
		    if (((Modifier.PRIVATE 
			  | Modifier.STATIC
			  | Modifier.FINAL) & modif) == 0
			&& !(mid.getName().equals("<init>"))) {
			// chain the preserved/same name lists.
			chainMethodIdentifier(mid);
		    }
		}
	    } else {
		// all methods and fields in superclass are preserved!
		try {
		    superclass.load(ClassInfo.DECLARATIONS);
		} catch (IOException ex) {
		    throw new RuntimeException
			("Can't read declarations of class "
			 + superclass.getName()
			 + ": " + ex.getMessage());
		}
		    
		MethodInfo[] topmethods = superclass.getMethods();
		for (int i=0; i< topmethods.length; i++) {
		    // all virtual methods in superclass may be
		    // virtually reachable
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			Identifier method = findMethod
			    (topmethods[i].getName(), topmethods[i].getType());
			if (method != null)
			    method.setPreserved();
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		initSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void initClass() {
        if (initialized)
	    return;
	initialized = true;

	try {
	    info.load(ClassInfo.ALL);
	} catch (IOException ex) {
	    throw new RuntimeException("Can't read class " + info.getName()
				       + ": " + ex.getMessage());
	}

	FieldInfo[] finfos   = info.getFields();
	MethodInfo[] minfos  = info.getMethods();
	if (Main.swapOrder) {
	    Random rand = new Random();
	    Collections.shuffle(Arrays.asList(finfos), rand);
	    Collections.shuffle(Arrays.asList(minfos), rand);
	}
	fieldIdents = new ArrayList(finfos.length);
	methodIdents = new ArrayList(minfos.length);
	for (int i=0; i< finfos.length; i++)
	    fieldIdents.add(new FieldIdentifier(this, finfos[i]));

	for (int i=0; i< minfos.length; i++) {
	    MethodIdentifier ident = new MethodIdentifier(this, minfos[i]);
	    methodIdents.add(ident);
	    if (ident.getName().equals("<clinit>")) {
		/* If there is a static initializer, it is automatically
		 * reachable (even if this class wouldn't be otherwise).
		 */
		ident.setPreserved();
		ident.setReachable();
	    } else if (ident.getName().equals("<init>"))
		ident.setPreserved();
	}

	// preserve / chain inherited methods and fields.
	ClassInfo[] ifaces = info.getInterfaces();
	ifaceNames = new String[ifaces.length];
	for (int i=0; i < ifaces.length; i++) {
	    ifaceNames[i] = ifaces[i].getName();
	    initSuperClasses(ifaces[i]);
	}

	if (info.getSuperclass() != null) {
	    superName = info.getSuperclass().getName();
	    initSuperClasses(info.getSuperclass());
	}

	if ((Main.stripping & Main.STRIP_SOURCE) != 0) {
	    info.setSourceFile(null);
	}
	if ((Main.stripping & Main.STRIP_INNERINFO) != 0) {
	    info.setClasses(new ClassInfo[0]);
	    info.setOuterClass(null);
	}
	// load inner classes
	ClassInfo outerClass   = info.getOuterClass();
	ClassInfo[] innerClasses = info.getClasses();
	if (outerClass != null)
	    Main.getClassBundle().getClassIdentifier(outerClass.getName());

	if (innerClasses != null) {
	    for (int i=0; i < innerClasses.length; i++) {
		Main.getClassBundle()
		    .getClassIdentifier(innerClasses[i].getName());
	    }
	}
    }

    /**
     * Add the ClassInfo objects of the interfaces of ancestor.  But if
     * an interface of ancestor is not reachable it will add its interfaces
     * instead.
     * @param result The Collection where the interfaces should be added to.
     * @param ancestor The ancestor whose interfaces should be added.
     */
    public void addIfaces(Collection result, ClassIdentifier ancestor) {
	String[] ifaces = ancestor.ifaceNames;
	ClassInfo[] ifaceInfos = ancestor.info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident
		= Main.getClassBundle().getClassIdentifier(ifaces[i]);
	    if (ifaceident != null && !ifaceident.isReachable())
		addIfaces(result, ifaceident);
	    else
		result.add(ifaceInfos[i]);
	}
    }

    /**
     * Generates the new super class and interfaces, removing super
     * classes and interfaces that are not reachable.
     * @return an array of class names (full qualified, dot separated)
     * where the first entry is the super class (may be null) and the
     * other entries are the interfaces.
     */
    public void transformSuperIfaces() {
	if ((Main.stripping & Main.STRIP_UNREACH) == 0)
	    return;

	Collection newIfaces = new LinkedList();
	ClassIdentifier ancestor = this;
	while(true) {
	    addIfaces(newIfaces, ancestor);
	    ClassIdentifier superident 
		= Main.getClassBundle().getClassIdentifier(ancestor.superName);
	    if (superident == null || superident.isReachable())
		break;
	    ancestor = superident;
	}
	ClassInfo superInfo = ancestor.info.getSuperclass();
	ClassInfo[] ifaces = (ClassInfo[]) 
	    newIfaces.toArray(new ClassInfo[newIfaces.size()]);
	info.setSuperclass(superInfo);
	info.setInterfaces(ifaces);
    }

    public void transformInnerClasses() {
	ClassInfo outerClass = info.getOuterClass();
	if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
	    while (outerClass != null) {
		ClassIdentifier outerIdent = Main.getClassBundle()
		    .getClassIdentifier(outerClass.getName());
		if (outerIdent != null && outerIdent.isReachable())
		    break;
		outerClass = outerClass.getOuterClass();
	    }
	}
	info.setOuterClass(outerClass);

	ClassInfo[] innerClasses = info.getClasses();
	if (innerClasses != null) {
	    int newInnerCount = innerClasses.length;
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
		for (int i=0; i < innerClasses.length; i++) {
		    ClassIdentifier innerIdent = Main.getClassBundle()
			.getClassIdentifier(innerClasses[i].getName());
		    if (innerIdent != null && !innerIdent.isReachable()) {
			innerClasses[i] = null;
			newInnerCount--;
		    }
		}
	    }
	    
	    ClassInfo[] newInners = new ClassInfo[newInnerCount];
	    int pos = 0;
	    for (int i=0; i<innerClasses.length; i++) {
		if (innerClasses[i] != null)
		    newInners[pos++] = innerClasses[i];
	    }
	    info.setClasses(newInners);
	}
    }

    public void doTransformations() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Transforming "+this);
	info.setName(getFullAlias());
	transformSuperIfaces();
	transformInnerClasses();

	Collection newFields = new ArrayList(fieldIdents.size());
	Collection newMethods = new ArrayList(methodIdents.size());

	for (Iterator i = fieldIdents.iterator(); i.hasNext(); ) {
	    FieldIdentifier ident = (FieldIdentifier)i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| ident.isReachable()) {
		ident.doTransformations();
		newFields.add(ident.info);
	    } else if (GlobalOptions.verboseLevel > 2) {
	        GlobalOptions.err.println("Field "+ ident+" not reachable");
	    }
	}
	for (Iterator i = methodIdents.iterator(); i.hasNext(); ) {
	    MethodIdentifier ident = (MethodIdentifier)i.next();
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| ident.isReachable()) {
		ident.doTransformations();
		newMethods.add(ident.info);
	    } else if (GlobalOptions.verboseLevel > 2) {
	        GlobalOptions.err.println("Method "+ ident+" not reachable");
	    }
	}

	info.setFields((FieldInfo[]) newFields.toArray
		       (new FieldInfo[newFields.size()]));
	info.setMethods((MethodInfo[]) newMethods.toArray
			(new MethodInfo[newMethods.size()]));
    }
    
    public void storeClass(DataOutputStream out) throws IOException {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Writing "+this);
	info.write(out);
	info = null;
	fieldIdents = methodIdents = null;
    }

    public Identifier getParent() {
	return pack;
    }

    /**
     * @return the full qualified name, excluding trailing dot.
     */
    public String getFullName() {
	return fullName;
    }

    /**
     * @return the full qualified alias, excluding trailing dot.
     */
    public String getFullAlias() {
	if (pack.parent == null)
	    return getAlias();
	else 
	    return pack.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "Ljava/lang/Class;";
    }
    
    public int getModifiers() {
	return info.getModifiers();
    }

    public List getFieldIdents() {
	return fieldIdents;
    }

    public List getMethodIdents() {
	return methodIdents;
    }	

    public Iterator getChilds() {
	final Iterator fieldIter = fieldIdents.iterator();
	final Iterator methodIter = methodIdents.iterator();
	    
	return new Iterator() {
	    boolean fieldsNext = fieldIter.hasNext();
	    public boolean hasNext() {
		return fieldsNext ? true : methodIter.hasNext();
	    }

	    public Object next() {
		if (fieldsNext) {
		    Object result = fieldIter.next();
		    fieldsNext = fieldIter.hasNext();
		    return result;
		}
		return methodIter.next();
	    }

	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }

    public String toString() {
	return "ClassIdentifier "+getFullName();
    }

    public Identifier getIdentifier(String fieldName, String typeSig) {
	for (Iterator i = getChilds(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (ident.getName().equals(fieldName)
		&& ident.getType().startsWith(typeSig))
		return ident;
	}
	
	if (superName != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superName);
	    if (superident != null) {
		Identifier ident 
		    = superident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}
	return null;
    }

    public boolean containsFieldAliasDirectly(String fieldName, String typeSig,
					      IdentifierMatcher matcher) {
	for (Iterator i = fieldIdents.iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || ident.isReachable())
		&& ident.wasAliased()
		&& ident.getAlias().equals(fieldName)
		&& ident.getType().startsWith(typeSig)
		&& matcher.matches(ident))
		return true;
	}
	return false;
    }

    public boolean containsMethodAliasDirectly(String methodName, 
					       String paramType,
					       IdentifierMatcher matcher) {
	for (Iterator i = methodIdents.iterator(); i.hasNext(); ) {
	    Identifier ident = (Identifier) i.next();
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || ident.isReachable())
		&& ident.wasAliased()
		&& ident.getAlias().equals(methodName)
		&& ident.getType().startsWith(paramType)
		&& matcher.matches(ident))
		return true;
	}
	return false;
    }

    public boolean fieldConflicts(FieldIdentifier field, String newAlias) {
	String typeSig = (Main.options & Main.OPTION_STRONGOVERLOAD) != 0
	    ? field.getType() : "";

	/* Fields are similar to static methods: They are not
	 * overriden but hidden.  We must only take care, that the
	 * reference of every getfield/putfield opcode points to the
	 * exact class, afterwards we can use doubled name as much as
	 * we want (even the decompiler can handle this).  
	 */

	ModifierMatcher mm = ModifierMatcher.allowAll;
	if (containsFieldAliasDirectly(newAlias, typeSig, mm))
	    return true;
	return false;
    }

    public boolean methodConflicts(MethodIdentifier method, String newAlias) {
	String paramType = method.getType();
	if ((Main.options & Main.OPTION_STRONGOVERLOAD) == 0)
	    paramType = paramType.substring(0, paramType.indexOf(')')+1);

	ModifierMatcher matcher = ModifierMatcher.allowAll;
	if (containsMethodAliasDirectly(newAlias, paramType, matcher))
	    return true;
	
	ModifierMatcher packMatcher = matcher.forceAccess(0, true);
	if (method.info.isStatic()) {
	    /* A static method does not conflict with static methods
	     * in super classes or sub classes.
	     */
	    packMatcher.forbidModifier(Modifier.STATIC);
	}
	/* We don't have to check interfaces:  sub classes must always
	 * implement all methods in the interface (maybe abstract, but
	 * they must be there!).
	 */
	ClassInfo superInfo = info.getSuperclass();
	while (superInfo != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superInfo.getName());
	    if (superident != null) {
		if (superident.containsMethodAliasDirectly
		    (newAlias, paramType, packMatcher))
		    return true;
	    } else {
		MethodInfo[] minfos = superInfo.getMethods();
		for (int i=0; i< minfos.length; i++) {
		    if (minfos[i].getName().equals(newAlias)
			&& minfos[i].getType().startsWith(paramType)
			&& packMatcher.matches(minfos[i].getModifiers()))
			return true;
		}
	    }
	    superInfo = superInfo.getSuperclass();
	}
	if (packMatcher.matches(method)) {
	    for (Iterator i = knownSubClasses.iterator(); i.hasNext(); ) {
		ClassIdentifier ci = (ClassIdentifier) i.next();
		if (ci.containsMethodAliasDirectly(newAlias, paramType, 
						   packMatcher))
		    return true;
	    }
	}
	return false;
    }
    
    public boolean conflicting(String newAlias) {
	return pack.contains(newAlias, this);
    }
}
