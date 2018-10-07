/* GrowableConstantPool Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: GrowableConstantPool.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This class represent a constant pool, where new constants can be
 * added to.  Normally you wont need to touch this class, as ClassInfo
 * already does all the hard work.  You will only need it if you want
 * to add your own custom attributes that use the constant pool.
 *
 * @author Jochen Hoenicke
 */
public class GrowableConstantPool extends ConstantPool {
    Hashtable entryToIndex = new Hashtable(); 
    boolean written;

    /**
     * This class is used as key to the entryToIndex hashtable
     */
    private class Key {
	int tag;
	Object objData;
	int intData;

	public Key(int tag, Object objData, int intData) {
	    this.tag = tag;
	    this.objData = objData;
	    this.intData = intData;
	}

	public int hashCode() {
	    return tag ^ objData.hashCode() ^ intData;
	}

	public boolean equals(Object o) {
	    if (o instanceof Key) {
		Key k = (Key) o;
		return tag == k.tag && intData == k.intData
		    && objData.equals(k.objData);
	    }
	    return false;
	}
    }

    /**
     * Create a new growable constant pool
     */
    public GrowableConstantPool () {
	count = 1;
	tags = new int[128];
	indices1 = new int[128];
	indices2 = new int[128];  
	constants = new Object[128];
	written = false;
    }

    private final void grow(int wantedSize) {
	if (written)
	    throw new IllegalStateException("adding to written ConstantPool");
	if (wantedSize > 65535)
	    throw new IllegalArgumentException("Too many constants added");
	if (tags.length < wantedSize) {
	    int newSize = Math.min(65535, Math.max(tags.length*2, wantedSize));
	    int[] tmpints = new int[newSize];
	    System.arraycopy(tags, 0, tmpints, 0, count);
	    tags = tmpints;
	    tmpints = new int[newSize];
	    System.arraycopy(indices1, 0, tmpints, 0, count);
	    indices1 = tmpints;
	    tmpints = new int[newSize];
	    System.arraycopy(indices2, 0, tmpints, 0, count);
	    indices2 = tmpints;
	    Object[] tmpobjs = new Object[newSize];
	    System.arraycopy(constants, 0, tmpobjs, 0, count);
	    constants = tmpobjs;
	}
    }

    private int putConstant(int tag, Object constant) {
	Key key = new Key(tag, constant, 0);
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	int newIndex = count;
	grow(count + 1);
	tags[newIndex] = tag;
	constants[newIndex] = constant;
	entryToIndex.put(key, new Integer(newIndex));
	count++;
	return newIndex;
    }

    private int putLongConstant(int tag, Object constant) {
	Key key = new Key(tag, constant, 0);
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	int newIndex = count;
	grow(count + 2);
	tags[newIndex] = tag;
	tags[newIndex+1] = -tag;
	constants[newIndex] = constant;
	entryToIndex.put(key, new Integer(newIndex));
	count += 2;
	return newIndex;
    }

    private int putIndexed(int tag, Object obj1, int index1, int index2) {
	Key key = new Key(tag, obj1, index2);
	Integer indexObj = (Integer) entryToIndex.get(key);
	if (indexObj != null) {
	    /* Maybe this was a reserved, but not filled entry */
	    int index = indexObj.intValue();
	    indices1[index] = index1;
	    indices2[index] = index2;
	    return index;
	}
	grow(count+1);
	tags[count] = tag;
	indices1[count] = index1;
	indices2[count] = index2;
	entryToIndex.put(key, new Integer(count));
	return count++;
    }

    /**
     * Adds a new UTF8 entry to the constant pool and returns the index.
     * If it already exists it will reuse the previous entry.
     * @param utf the UTF8 string.
     * @return the index of the pool entry.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public final int putUTF8(String utf) {
	return putConstant(UTF8, utf);
    }    

    /**
     * Adds a new class name entry to the constant pool and returns
     * the index.  If it already exists it will reuse the previous
     * entry.
     * @param name the dot separated full qualified class name.
     * @return the index of the pool entry.
     * @exception IllegalArgumentException if the class name is illegal.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public int putClassName(String name) {
	name = name.replace('.','/');
	TypeSignature.checkTypeSig("L"+name+";");
	return putIndexed(CLASS, name, putUTF8(name), 0);
    }

    /**
     * Adds a new class entry to the constant pool and returns
     * the index.  If it already exists it will reuse the previous
     * entry.  This is the same as putClassName, except for the format
     * of the parameter and that it can also handle arrays.
     * @param name the type signature of the class to add.
     * @return the index of the pool entry.
     * @exception IllegalArgumentException if the class name is illegal.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public int putClassType(String name) {
	TypeSignature.checkTypeSig(name);
	if (name.charAt(0) == 'L')
	    name = name.substring(1, name.length()-1);
	else if (name.charAt(0) != '[')
	    throw new IllegalArgumentException("wrong class type: "+name);
	return putIndexed(CLASS, name, putUTF8(name), 0);
    }

    /**
     * Adds a new field/method or interface reference to the constant
     * pool and returns the index.  If it already exists it will reuse
     * the previous entry.
     * @param tag the tag of the reference, one of FIELDREF, METHODREF
     * or INTERFACEMETHODREF.
     * @param ref the reference.
     * @return the index of the pool entry.
     * @exception IllegalArgumentException if the reference type or
     * class name is illegal.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public int putRef(int tag, Reference ref) {
	String className = ref.getClazz();
	String typeSig = ref.getType();
	if (tag == FIELDREF)
	    TypeSignature.checkTypeSig(typeSig);
	else
	    TypeSignature.checkMethodTypeSig(typeSig);


	int classIndex = putClassType(className);
	int nameIndex  = putUTF8(ref.getName());
	int typeIndex  = putUTF8(typeSig);
	int nameTypeIndex = putIndexed(NAMEANDTYPE, 
				       ref.getName(), nameIndex, typeIndex);
	return putIndexed(tag, className, classIndex, nameTypeIndex);
    }

    /**
     * Puts a "one slot" constant into this constant pool.  If it
     * already exists it will reuse the previous entry.
     * @param c the constant; must be of type 
     *    Integer, Float or String
     * @return the index of the pool entry.
     * @exception IllegalArgumentException if the constant is of wrong type.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public int putConstant(Object c) {
	if (c instanceof String) {
	    return putIndexed(STRING, c, putUTF8((String) c), 0);
	} else {
	    int tag;
	    if (c instanceof Integer)
		tag = INTEGER;
	    else if (c instanceof Float)
		tag = FLOAT;
	    else
		throw new IllegalArgumentException
		    ("illegal constant " + c + " of type: " + c.getClass());
	    return putConstant(tag, c);
        }
    }

    /**
     * Puts a "double slot" constant into this constant pool.  If it
     * already exists it will reuse the previous entry.
     * @param c the constant; must be of type Long or Double
     * @return the index of the pool entry.
     * @exception IllegalArgumentException if the constant is of wrong type.
     * @exception IllegalStateException if the pool was already written,
     * but the entry was not added before.
     */
    public int putLongConstant(Object c) {
	int tag;
	if (c instanceof Long)
	    tag = LONG;
	else if (c instanceof Double)
	    tag = DOUBLE;
	else
	    throw new IllegalArgumentException
		("illegal long constant " + c + " of type: " + c.getClass());
	return putLongConstant(tag, c);
    }

    /**
     * Reserves an entry in this constant pool for a constant (for ldc).
     * The constant must still be put into the pool, before the pool may
     * be written.
     * @param c the constant, must be of type 
     *    Integer, Float or String
     * @return the reserved index into the pool of this constant.
     */
    public int reserveConstant(Object c) {
	if (c instanceof String) {
	    return putIndexed(STRING, c, -1, 0);
	} else {
	    return putConstant(c);
        }
    }

    /**
     * Writes the constant pool to the stream.  This is normally called
     * by {@link ClassInfo#write}.  
     * @param stream the stream to write to.
     * @exception IOException if it occured while writing.
     */
    public void write(DataOutputStream stream) 
	throws IOException {
	written = true;
	stream.writeShort(count);
	for (int i=1; i< count; i++) {
	    int tag = tags[i];
            stream.writeByte(tag);
            switch (tag) {
	    case CLASS:
		stream.writeShort(indices1[i]);
		break;
	    case FIELDREF:
	    case METHODREF:
	    case INTERFACEMETHODREF:
		stream.writeShort(indices1[i]);
		stream.writeShort(indices2[i]);
		break;
	    case STRING:
		stream.writeShort(indices1[i]);
		break;
	    case INTEGER:
		stream.writeInt(((Integer)constants[i]).intValue());
		break;
	    case FLOAT:
		stream.writeFloat(((Float)constants[i]).floatValue());
		break;
	    case LONG:
		stream.writeLong(((Long)constants[i]).longValue());
		i++;
		break;
	    case DOUBLE:
		stream.writeDouble(((Double)constants[i]).doubleValue());
		i++;
		break;
	    case NAMEANDTYPE:
		stream.writeShort(indices1[i]);
		stream.writeShort(indices2[i]);
		break;
	    case UTF8:
		stream.writeUTF((String)constants[i]);
		break;
	    default:
		throw new ClassFormatException("unknown constant tag");
            }
	}
    }
}
