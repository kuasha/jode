/* ArrayType Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ArrayType.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.type;

/** 
 * This type represents an array type.
 *
 * @author Jochen Hoenicke 
 */
public class ArrayType extends ClassType {
    Type elementType;

    ArrayType(Type elementType) {
        super(TC_ARRAY, elementType + "[]");
        this.elementType = elementType;
    }

    public boolean isInterface() {
	return false;
    }

    public boolean isUnknown() {
	if (elementType instanceof ClassType)
	    return ((ClassType) elementType).isUnknown();
	return false;
    }

    public boolean isFinal() {
	if (elementType instanceof ClassType)
	    return ((ClassType) elementType).isFinal();
	return false;
    }

    public ClassType getSuperClass() {
	return tObject;
    }

    public ClassType[] getInterfaces() {
	return arrayIfaces;
    }

    public Type getElementType() {
        return elementType;
    }

    public Type getSuperType() {
	if (elementType instanceof IntegerType)
	    return tRange(tObject, this);
	else
	    return tRange(tObject, 
			  (ReferenceType) tArray(elementType.getSuperType()));
    }

    public Type getSubType() {
	if (elementType instanceof IntegerType)
	    return this;
	else
	    return tArray(elementType.getSubType());
    }

    public Type getHint() {
	return tArray(elementType.getHint());
    }
    
    public Type getCanonic() {
	return tArray(elementType.getCanonic());
    }
    
    public boolean isSubTypeOf(Type type) {
	if (type == tNull)
	    return true;
	if (type instanceof ArrayType)
	    return elementType.isSubTypeOf(((ArrayType) type).elementType);
	return false;
    }

    /**
     * Create the type corresponding to the range from bottomType to this.
     * @param bottomType the start point of the range
     * @return the range type, or tError if not possible.
     */
    public Type createRangeType(ReferenceType bottom) {
        /*
         *  tArray(y), tArray(x) -> tArray( y.intersection(x) )
         *  obj      , tArray(x) -> <obj, tArray(x)>
	 *    iff tArray extends and implements obj
         */
	if (bottom instanceof ArrayType)
	    return tArray(elementType.intersection
			  (((ArrayType)bottom).elementType));
	
	return super.createRangeType(bottom);
    }

    /**
     * Returns the common sub type of this and type.
     * @param type the other type.
     * @return the common sub type.
     */
    public Type getSpecializedType(Type type) {
        /*  
	 *  tArray(x), iface     -> tArray(x) iff tArray implements iface
	 *  tArray(x), tArray(y) -> tArray(x.intersection(y))
         *  tArray(x), other     -> tError
         */
	if (type.getTypeCode() == TC_RANGE) {
	    type = ((RangeType) type).getBottom();
	}
	if (type == tNull)
	    return this;
	if (type.getTypeCode() == TC_ARRAY) {
	    Type elType = elementType.intersection
		(((ArrayType)type).elementType);
	    return elType != tError ? tArray(elType) : tError;
	}
	if (type.isSubTypeOf(this))
	    return this;
	return tError;
    }

    /**
     * Returns the common super type of this and type.
     * @param type the other type.
     * @return the common super type.
     */
    public Type getGeneralizedType(Type type) {
	/*  tArray(x), tNull     -> tArray(x)
	 *  tArray(x), tClass(y) -> common ifaces of tArray and tClass
	 *  tArray(x), tArray(y) -> tArray(x.intersection(y)) or tObject
	 *  tArray(x), other     -> tError
         */
	if (type.getTypeCode() == TC_RANGE) {
	    type = ((RangeType) type).getTop();
	}
	if (type == tNull)
            return this;
        if (type.getTypeCode() == TC_ARRAY) {
	    Type elType = elementType.intersection
		(((ArrayType)type).elementType);
	    if (elType != tError)
		return tArray(elType);
	    return MultiClassType.create(arrayIfaces);
	}
	if (!(type instanceof ReferenceType))
	    return tError;

	return ((ReferenceType)type).getGeneralizedType(this);
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return elementType.isValidType();
    }

    public String getTypeSignature() {
	return "["+elementType.getTypeSignature();
    }

    public Class getTypeClass() throws ClassNotFoundException {
	return Class.forName(getTypeSignature());
    }

    private static String pluralize(String singular) {
        return singular + 
            ((singular.endsWith("s") || singular.endsWith("x")
              || singular.endsWith("sh") || singular.endsWith("ch")) 
             ? "es" : "s");
    }

    public String getDefaultName() {
	if (elementType instanceof ArrayType)
	    return elementType.getDefaultName();
        return pluralize(elementType.getDefaultName());
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof ArrayType) {
            ArrayType type = (ArrayType) o;
            return type.elementType.equals(elementType);
        }
        return false;
    }
}
