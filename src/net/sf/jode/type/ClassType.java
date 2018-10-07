/* ClassType Copyright (C) 2000-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: ClassType.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.type;
import java.util.Stack;
import java.util.Hashtable;

/**
 * This class is the base class of all types representing a class type.<p>
 *
 * @author Jochen Hoenicke 
 */
public abstract class ClassType extends ReferenceType {
    protected String className;

    public String getClassName() {
	return className;
    }

    public ClassType(int typecode, String clazzName) {
	super(typecode);
	className = clazzName;
    }

    /**
     * Checks if this type represents an interface.
     * @return true if this is an interface, false if this is a class or
     * if unsure.
     */
    public abstract boolean isUnknown();

    /**
     * Checks if this type represents an interface.
     * @return true if this is an interface, false if this is a class or
     * if unsure.
     */
    public abstract boolean isInterface();

    /**
     * Checks if this type represents an interface.
     * @return true if this is an interface, false if this is a class or
     * if unsure.
     */
    public abstract boolean isFinal();

    /**
     * Returns the reference type representing the super class, or null
     * if this reference type represents java.lang.Object, or for interfaces.
     * @return the super class' type.
     */
    public abstract ClassType getSuperClass();

    /**
     * Returns the reference type representing the interfaces this class
     * implements.  This may of course be an empty array.
     * @return the interfaces' types.
     */
    public abstract ClassType[] getInterfaces();

    public boolean isSubTypeOf(Type type) {
	if (type == tNull)
	    return true;
	if (type instanceof MultiClassType)
	    return ((MultiClassType)type).containsSuperTypeOf(this);
	if (!(type instanceof ClassType))
	    return false;
	if (this.equals(tObject))
	    return true;
	
	ClassType ctype = (ClassType) type;

	if (isFinal())
	    return ctype.equals(this);
	
	while (ctype != null) {
	    if (ctype.equals(this))
		return true;

	    if (isInterface()) {
		ClassType[] typeIfaces = ctype.getInterfaces();
		for (int i = 0; i < typeIfaces.length; i++)
		    if (isSubTypeOf(typeIfaces[i]))
			return true;
	    }
	    ctype = ctype.getSuperClass();
	}
	return false;
    }

    public boolean maybeSubTypeOf(Type type) {
	if (type == tNull)
	    return true;
	if (!(type instanceof ClassType))
	    return false;
	if (this.equals(tObject))
	    return true;
	
	ClassType ctype = (ClassType) type;

	if (isFinal())
	    return ctype.equals(this);
	
	while (ctype != null) {
	    if (ctype.equals(this))
		return true;

	    if (ctype.isUnknown())
		return true;
	    
	    if (isInterface()) {
		ClassType[] typeIfaces = ctype.getInterfaces();
		for (int i = 0; i < typeIfaces.length; i++)
		    if (isSubTypeOf(typeIfaces[i]))
			return true;
	    }
	    ctype = ctype.getSuperClass();
	}
	return false;
    }

    public Type getSubType() {
	return tRange(this, tNull);
    }

    public Type getHint() {
	return this;
    }

    public Type getCanonic() {
	return this;
    }

    /**
     * Create the type corresponding to the range from bottomType to
     * this.  Checks if the given type range may be not empty.  This
     * means, that bottom.clazz is extended by this.clazz and that all
     * interfaces in bottom are implemented by an interface or by
     * clazz.
     * @param bottom the start point of the range
     * @return the range type, or tError if range is empty.  
     */
    public Type createRangeType(ReferenceType bottomType) {
	if (!bottomType.maybeSubTypeOf(this))
	    return tError;

	if (this.isSubTypeOf(bottomType))
	    /* bottomType contains a class equal to this.
	     */
	    return this;
	return tRange(bottomType, this);
    }
    
    /**
     * Returns the specialized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that extends both objects
     * and the union of all interfaces.
     */
    public Type getSpecializedType(Type type) {
	if (type instanceof RangeType) {
	    type = ((RangeType) type).getBottom();
	}

        /* Most times (almost always) one of the two classes is
         * already more specialized.  Optimize for this case.  
	 */
	if (type.isSubTypeOf(this))
	    return this;
	if (this.isSubTypeOf(type))
	    return type;

	if (type instanceof MultiClassType)
	    return ((MultiClassType) type).getSpecializedType(this);

        if (!(type instanceof ClassType))
            return tError;
        ClassType other = (ClassType) type;
	return MultiClassType.create(new ClassType[] {this, other});
    }

    /**
     * Returns the generalized type of this and type.  We have two
     * classes and multiple interfaces.  The result should be the
     * object that is the the super class of both objects and all
     * interfaces, that one class or interface of each type 
     * implements.  
     */
    public Type getGeneralizedType(Type type) {
        int code = type.typecode;
	if (code == TC_RANGE) {
	    type = ((RangeType) type).getTop();
	    code = type.typecode;
	}
        if (code == TC_NULL)
            return this;

        /* Often one of the two classes is already more generalized.
         * Optimize for this case.  
	 */
	if (type.isSubTypeOf(this))
	    return type;
	if (this.isSubTypeOf(type))
	    return this;

	if (!(type instanceof ReferenceType))
	    return tError;

	Stack classTypes = new Stack();
	classTypes.push(this);
	return ((ReferenceType) type).findCommonClassTypes(classTypes);
    }

    public String getTypeSignature() {
	return "L" + className + ";";
    }

    public Class getTypeClass() throws ClassNotFoundException {
	return Class.forName(className);
    }

    public String toString()
    {
	return className;	
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	if (isInterface() || fromType == tNull
	    || (fromType instanceof RangeType
		&& ((RangeType)fromType).getBottom() == tNull))
	    return null;
	Type hint = fromType.getHint();
	if (hint.isSubTypeOf(this)
	    || (hint instanceof ClassType
		&& ((ClassType) hint).isInterface()))
	    return null;
	return tObject;
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return true;
    }

    /**
     * Checks if this is a class or array type (but not a null type).
     * @XXX remove this?
     * @return true if this is a class or array type.
     */
    public boolean isClassType() {
        return true;
    }

    private final static Hashtable keywords = new Hashtable();
    static {
	keywords.put("abstract", Boolean.TRUE);
	keywords.put("default", Boolean.TRUE);
	keywords.put("if", Boolean.TRUE);
	keywords.put("private", Boolean.TRUE);
	keywords.put("throw", Boolean.TRUE);
	keywords.put("boolean", Boolean.TRUE);
	keywords.put("do", Boolean.TRUE);
	keywords.put("implements", Boolean.TRUE);
	keywords.put("protected", Boolean.TRUE);
	keywords.put("throws", Boolean.TRUE);
	keywords.put("break", Boolean.TRUE);
	keywords.put("double", Boolean.TRUE);
	keywords.put("import", Boolean.TRUE);
	keywords.put("public", Boolean.TRUE);
	keywords.put("transient", Boolean.TRUE);
	keywords.put("byte", Boolean.TRUE);
	keywords.put("else", Boolean.TRUE);
	keywords.put("instanceof", Boolean.TRUE);
	keywords.put("return", Boolean.TRUE);
	keywords.put("try", Boolean.TRUE);
	keywords.put("case", Boolean.TRUE);
	keywords.put("extends", Boolean.TRUE);
	keywords.put("int", Boolean.TRUE);
	keywords.put("short", Boolean.TRUE);
	keywords.put("void", Boolean.TRUE);
	keywords.put("catch", Boolean.TRUE);
	keywords.put("final", Boolean.TRUE);
	keywords.put("interface", Boolean.TRUE);
	keywords.put("static", Boolean.TRUE);
	keywords.put("volatile", Boolean.TRUE);
	keywords.put("char", Boolean.TRUE);
	keywords.put("finally", Boolean.TRUE);
	keywords.put("long", Boolean.TRUE);
	keywords.put("super", Boolean.TRUE);
	keywords.put("while", Boolean.TRUE);
	keywords.put("class", Boolean.TRUE);
	keywords.put("float", Boolean.TRUE);
	keywords.put("native", Boolean.TRUE);
	keywords.put("switch", Boolean.TRUE);
	keywords.put("const", Boolean.TRUE);
	keywords.put("for", Boolean.TRUE);
	keywords.put("new", Boolean.TRUE);
	keywords.put("synchronized", Boolean.TRUE);
	keywords.put("continue", Boolean.TRUE);
	keywords.put("goto", Boolean.TRUE);
	keywords.put("package", Boolean.TRUE);
	keywords.put("this", Boolean.TRUE);
	keywords.put("strictfp", Boolean.TRUE);
	keywords.put("null", Boolean.TRUE);
	keywords.put("true", Boolean.TRUE);
	keywords.put("false", Boolean.TRUE);
    }

    /**
     * Generates the default name, that is the `natural' choice for
     * local of this type.
     * @return the default name of a local of this type.  
     */
    public String getDefaultName() {
        String name = className;
        int dot = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
        if (dot >= 0)
            name = name.substring(dot+1);
        if (Character.isUpperCase(name.charAt(0))) {
	    name = name.toLowerCase();
	    if (keywords.get(name) != null)
		return "var_" + name;
            return name;
        } else
            return "var_" + name;
    }

    public int hashCode() {
	return className.hashCode();
    }

    public boolean equals(Object o) {
	if (o instanceof ClassType)
	    return ((ClassType) o).className.equals(className);
	return false;
    }
}
