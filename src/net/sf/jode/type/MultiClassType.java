/* MultiClassType Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: MultiClassType.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.type;
import java.util.Stack;
import java.util.Vector;

/**
 * This class represents a type aproximation, consisting of multiple
 * interfaces and a class type.<p>
 *
 * If this is the bottom boundary, this specifies, which class our
 * type must extend and which interfaces it must implement.
 *
 * If this is the top boundary, this gives all interfaces and classes
 * that may extend the type.  I.e. at least one interface or class extends
 * the searched type.
 *
 * @author Jochen Hoenicke */
public class MultiClassType extends ReferenceType {

    ClassType classes[];

    private MultiClassType(ClassType[] classes) {
        super(TC_CLASSIFACE);
        this.classes = classes;
    }

    public static ReferenceType create(ClassType[] classes) {
	if (classes.length == 0)
	    return tObject;
	if (classes.length == 1)
	    return classes[0];
        return new MultiClassType(classes);
    }

    public Type getSubType() {
	/* We don't implement the set of types, that are castable to some 
	 * of the given classes or interfaces.
	 */
	throw new InternalError
	    ("getSubType called on set of classes and interfaces!");
    }

    /**
     * Returns true, iff this type implements all interfaces in type 
     * and extends all objects in type.
     */
    public boolean isSubTypeOf(Type type) {
	for (int i = 0; i < classes.length; i++)
	    if (!classes[i].isSubTypeOf(type))
		return false;
	return true;
    }

    /**
     * Returns true, iff this type implements all interfaces in type 
     * and extends all objects in type.
     */
    public boolean maybeSubTypeOf(Type type) {
	for (int i = 0; i < classes.length; i++)
	    if (!classes[i].maybeSubTypeOf(type))
		return false;
	return true;
    }

    public Type getHint() {
	return getCanonic();
    }

    public Type getCanonic() {
	return classes[0];
    }

    /**
     * Create the type corresponding to the range from bottomType to
     * this.  This removes all classes that doesn't extend all classes
     * in bottom.  If no class remains, this is a type error.
     * @param bottom the start point of the range
     * @return the range type, or tError if range is empty.  
     */
    public Type createRangeType(ReferenceType bottomType) {
	ReferenceType topType;
	/**
	 * Check if we fully implement the bottom type.
	 */
	int j;
	for (j=0; j < classes.length; j++) {
	    if (!bottomType.maybeSubTypeOf(classes[j]))
		break;
	}
	    
	if (j == classes.length)
	    topType = this;
	else {
	    /* Now we have at least one class, that doesn't implement
	     * bottomType, remove all such classes.
	     */
	    ClassType[] topClasses = new ClassType[classes.length - 1];
	    System.arraycopy(classes, 0, topClasses, 0, j);
	    int count = j;
	    for (j++; j < classes.length; j++) {
		if (bottomType.isSubTypeOf(classes[j]))
		    topClasses[count++] = classes[j];
	    }
	    
	    if (count == 0)
		return tError;
	    if (count < topClasses.length - 1) {
		ClassType[] shortClasses = new ClassType[count];
		System.arraycopy(topClasses, 0, shortClasses, 0, count);
		topClasses = shortClasses;
	    }
	    topType = create(topClasses);
	}
	if (topType.isSubTypeOf(bottomType))
	    /* This means that topType contains only classes that are also
	     * in bottomType.  So topType is the whole range.
	     */
	    return topType;
	return tRange(bottomType, topType);
    }
    
    boolean containsSuperTypeOf(Type type) {
	for (int i = 0; i < classes.length; i++)
	    if (type.isSubTypeOf(classes[i]))
		return true;
	return false;
    }

    /**
     * Returns the specialized type of this and type.  
     * We simple unify the lists of classes, but simplify them, to remove
     * all classes that are already subtypes of some other class in the
     * other list.
     */
    public Type getSpecializedType(Type type) {
	if (type instanceof RangeType)
	    type = ((RangeType) type).getBottom();

        /* Most times (almost always) one of the two types is
         * already more specialized.  Optimize for this case.  
	 */
	if (type.isSubTypeOf(this))
	    return this;
	if (this.isSubTypeOf(type))
	    return type;

	ClassType[] otherClasses;
	if (type instanceof MultiClassType) {
	    otherClasses = ((MultiClassType) type).classes;
	} else if (type instanceof ClassType) {
	    otherClasses = new ClassType[] { (ClassType) type };
	} else
	    return tError;

	/* The classes are simply the union of both classes set.  But
	 * we can simplify this, if a class is implemented by another
	 * class in the other list, we can omit it.  
	 */
	Vector destClasses = new Vector();
	for (int i=0; i< classes.length; i++) {
	    ClassType clazz = classes[i];
	    if (!clazz.isSubTypeOf(type)) {
		/* This interface is not implemented by any of the other
		 * classes.  Add it to the destClasses.
		 */
		destClasses.addElement(clazz);
	    }
	}
	for (int i=0; i< otherClasses.length; i++) {
	    ClassType clazz = otherClasses[i];
	    if (!clazz.isSubTypeOf(this)) {
		/* This interface is not implemented by any of the other
		 * classes.  Add it to the destClasses.
		 */
		destClasses.addElement(clazz);
	    }
	}
	
	ClassType[] classArray = new ClassType[destClasses.size()];
	destClasses.copyInto(classArray);
	return create(classArray);
    }

    /**
     * Returns the generalized type of this and type.  We have two
     * classes and multiple interfaces.  The result should be the
     * object that is the the super class of both objects and all
     * interfaces, that one class or interface of each type 
     * implements.  
     */
    public Type getGeneralizedType(Type type) {
	if (type instanceof RangeType)
	    type = ((RangeType) type).getTop();

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
	for (int i = 0; i < classes.length; i++)
	    classTypes.push(classes[i]);
	return ((ReferenceType)type).findCommonClassTypes(classTypes);
    }

    public String toString()
    {
	StringBuffer sb = new StringBuffer("{");
	String comma = "";
	for (int i=0; i< classes.length; i++) {
	    sb.append(comma).append(classes[i]);
	    comma = ", ";
	}
	return sb.append("}").toString();
    }

    public String getTypeSignature() {
	return getCanonic().getTypeSignature();
    }

    public Class getTypeClass() throws ClassNotFoundException {
	return getCanonic().getTypeClass();
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return getCanonic().getCastHelper(fromType);
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return false;
    }

    /**
     * Checks if this is a class or array type (but not a null type).
     * @XXX remove this?
     * @return true if this is a class or array type.
     */
    public boolean isClassType() {
        return true;
    }

    /**
     * Generates the default name, that is the `natural' choice for
     * local of this type.
     * @return the default name of a local of this type.  
     */
    public String getDefaultName() {
        return getCanonic().getDefaultName();
    }
    
    public int hashCode() {
	int hash = 0;
	for (int i=0; i < classes.length; i++) {
	    hash ^= classes[i].hashCode();
	}
	return hash;
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof MultiClassType) {
            MultiClassType type = (MultiClassType) o;
            if (type.classes.length == classes.length) {
                big_loop:
                for (int i=0; i< type.classes.length; i++) {
                    for (int j=0; j<classes.length; j++) {
                        if (type.classes[i] == classes[j])
                            continue big_loop;
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
