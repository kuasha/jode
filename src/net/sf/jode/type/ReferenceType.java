/* ReferenceType Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: ReferenceType.java 1418 2013-05-06 19:36:33Z hoenicke $
 */

package net.sf.jode.type;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.ClassInfo;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This is an abstract super class of all reference types.  Reference
 * types are NullType, MultiClassType, and ClassType with its sub types
 * ClassInfoType, SystemClassType, and ArrayType. <p>
 *
 * To do intersection on range types, the reference types need three
 * more operations: specialization, generalization and
 * createRange. <p>
 *
 * specialization chooses all common sub type of two types.  It is
 * used to find the bottom of the intersected interval. <p>
 *
 * generalization chooses the common super type of two types. It
 * is used to find the top of the intersected interval. <p>
 *
 * When the new interval is created with <code>createRangeType</code>
 * the bottom and top are adjusted so that they only consists of
 * possible types.  It then decides, if it needs a range type, or if
 * the reference types already represents all types.
 *
 * @author Jochen Hoenicke 
 */
public abstract class ReferenceType extends Type {
    public ReferenceType(int typecode) {
	super(typecode);
    }
    
    /**
     * Returns the specialized type set of this and type.  The result
     * should be a type set, so that every type, extends all types in
     * type and this, iff it extends all types in the resulting type
     * set.
     * @param type the other type.
     * @return the specialized type.  */
    public abstract Type getSpecializedType(Type type);

    /**
     * Returns the generalized type set of this and type.  The result
     * should be a type set, so that every type, is extended/implemented
     * by one type in this and one type in <code>type</code>, iff it is
     * extended/implemented by one type in the resulting type set.
     * @param type the other type.
     * @return the generalized type
     */
    public abstract Type getGeneralizedType(Type type);

    public Type findCommonClassTypes(Stack otherTypes) {
	/* Consider each class and interface implemented by this.
	 * If any clazz or interface in other implements it, add it to
	 * the classes vector.  Otherwise consider all sub interfaces.  
	 */
        Vector classes = new Vector();

    type_loop:
        while (!otherTypes.isEmpty()) {
            ClassType type = (ClassType) otherTypes.pop();
	    if (type.equals(tObject))
		/* tObject is always implied. */
		continue type_loop;

	    for (Enumeration enumeration = classes.elements(); 
		 enumeration.hasMoreElements(); ) {
		if (type.isSubTypeOf((Type) enumeration.nextElement()))
		    /* We can skip this, as another class already
		     * implies it.  */
		    continue type_loop;
	    }
	    
            if (type.isSubTypeOf(this)) {
		classes.addElement(type);
                continue type_loop;
            }

            /* This clazz/interface is not implemented by this object.
             * Try its parents now.
             */
            ClassType ifaces[] = type.getInterfaces();
            for (int i=0; i < ifaces.length; i++)
                otherTypes.push(ifaces[i]);
	    ClassType superClass = type.getSuperClass();
	    if (superClass != null)
		otherTypes.push(superClass);
        }
        ClassType[] classArray = new ClassType[classes.size()];
        classes.copyInto(classArray);
        return MultiClassType.create(classArray);
    }

    /**
     * Creates a range type set of this and bottom.  The resulting type set
     * contains all types, that extend all types in bottom and are extended
     * by at least one type in this. <br>
     * Note that a RangeType will do this, but we normalize the bottom and
     * top set.
     * @param bottom the bottom type.
     * @return the range type set.
     */
    public abstract Type createRangeType(ReferenceType bottom);

    /**
     * Tells if all otherIfaces, are implemented by at least one
     * ifaces or by clazz.
     * 
     * This is a useful function for generalizing/specializing interface
     * types or arrays.
     *
     * If it can't find all classes in the hierarchy, it will catch this
     * error and return false, i.e. it assumes that the class doesn't
     * implement all interfaces.
     *
     * @param clazz The clazz, can be null.
     * @param ifaces The ifaces.
     * @param otherifaces The other ifaces, that must be implemented.
     * @return true, if all otherIfaces are implemented, false if unsure or
     * if not all otherIfaces are implemented.
     */
    protected static boolean implementsAllIfaces(ClassInfo clazz,
						 ClassInfo[] ifaces,
						 ClassInfo[] otherIfaces) {
	try {
	big:
	    for (int i=0; i < otherIfaces.length; i++) {
		ClassInfo iface = otherIfaces[i];
		if (clazz != null && iface.implementedBy(clazz))
		    continue big;
		for (int j=0; j < ifaces.length; j++) {
		    if (iface.implementedBy(ifaces[j]))
                        continue big;
		}
		return false;
	    }
	    return true;
	} catch (IOException ex) {
	    /* Class Hierarchy can't be fully gotten. */
	    return false;
	}
    }

    public Type getSuperType() {
	return (this == tObject) ? tObject : tRange(tObject, this);
    }

    public abstract Type getSubType();

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	if (type == tError)
	    return type;
	if (type == Type.tUnknown)
	    return this;

	Type newBottom = getSpecializedType(type);
	Type newTop    = getGeneralizedType(type);
	Type result;
	if (newTop.equals(newBottom))
	    result = newTop;
	else if (newTop instanceof ReferenceType
		 && newBottom instanceof ReferenceType)
	    result = ((ReferenceType) newTop)
		.createRangeType((ReferenceType) newBottom);
	else
	    result = tError;

        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0) {
	    GlobalOptions.err.println("intersecting "+ this +" and "+ type + 
				      " to " + result);
	}	    
        return result;
    }
}
