/* UnifyHash Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: UnifyHash.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.util;
///#ifdef JDK12
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
///#endif

///#def COLLECTIONS java.util
import java.util.Comparator;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.UnsupportedOperationException;
///#enddef

public class UnifyHash extends AbstractCollection {
    /** 
     * the default capacity
     */
    private static final int DEFAULT_CAPACITY = 11;

    /** the default load factor of a HashMap */
    private static final float DEFAULT_LOAD_FACTOR = 0.75F;

///#ifdef JDK12
    private ReferenceQueue queue = new ReferenceQueue();
///#endif

    static class Bucket
///#ifdef JDK12
	extends WeakReference
///#endif
    {
///#ifdef JDK12
	public Bucket(Object o, ReferenceQueue q) {
	    super(o, q);
	}
///#else
///	public Bucket(Object o) {
///	    this.obj = o;
///	}
///
///	Object obj;
///
///	public Object get() {
///	    return obj;
///	}
///#endif

	int hash;
	Bucket next;
    }

    private Bucket[] buckets;
    int modCount = 0;
    int size = 0;
    int threshold;
    float loadFactor;

    public UnifyHash(int initialCapacity, float loadFactor) {
	this.loadFactor = loadFactor;
	buckets = new Bucket[initialCapacity];
	threshold = (int) (loadFactor * initialCapacity);
    }

    public UnifyHash(int initialCapacity) {
	this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public UnifyHash() {
	this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    private void grow() {
	Bucket[] oldBuckets = buckets;
	int newCap = buckets.length * 2 + 1;
	threshold = (int) (loadFactor * newCap);
	buckets = new Bucket[newCap];
	for (int i = 0; i < oldBuckets.length; i++) {
	    Bucket nextBucket;
	    for (Bucket b = oldBuckets[i]; b != null; b = nextBucket) {
		if (i != Math.abs(b.hash % oldBuckets.length))
		    throw new RuntimeException(""+i+", hash: "+b.hash+", oldlength: "+oldBuckets.length);
		int newSlot = Math.abs(b.hash % newCap);
		nextBucket = b.next;
		b.next = buckets[newSlot];
		buckets[newSlot] = b;
	    }
	}
    }

///#ifdef JDK12
    public final void cleanUp() {
	Bucket died;
	while ((died = (Bucket)queue.poll()) != null) {
	    int diedSlot = Math.abs(died.hash % buckets.length);
	    if (buckets[diedSlot] == died)
		buckets[diedSlot] = died.next;
	    else {
		Bucket b = buckets[diedSlot];
		while (b.next != died)
		    b = b.next;
		b.next = died.next;
	    }
	    size--;
	}
    }
///#endif


    public int size() {
	return size;
    }

    public Iterator iterator() {
///#ifdef JDK12
	cleanUp();
///#endif

	return new Iterator() {
	    private int bucket = 0;
	    private int known = modCount;
	    private Bucket nextBucket;
	    private Object nextVal;

	    {
		internalNext();
	    }

	    private void internalNext() {
		while (true) {
		    while (nextBucket == null) {
			if (bucket == buckets.length)
			    return;
			nextBucket = buckets[bucket++];
		    }
		    
		    nextVal = nextBucket.get();
		    if (nextVal != null)
			return;

		    nextBucket = nextBucket.next;
		}
	    }

	    public boolean hasNext() {
		return nextBucket != null;
	    }

	    public Object next() {
		if (known != modCount)
		    throw new ConcurrentModificationException();
		if (nextBucket == null)
		    throw new NoSuchElementException();
		Object result = nextVal;
		nextBucket = nextBucket.next;
		internalNext();
		return result;
	    }

	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }

    public Iterator iterateHashCode(final int hash) {
///#ifdef JDK12
	cleanUp();
///#endif
	return new Iterator() {
	    private int known = modCount;
	    private boolean removeOk = false;
	    private Bucket removeBucket = null;
	    private Bucket prevBucket   = null;
	    private Bucket nextBucket
		= buckets[Math.abs(hash % buckets.length)];
	    private Object nextVal;

	    {
		internalNext();
	    }

	    private void internalNext() {
		while (nextBucket != null) {
		    if (nextBucket.hash == hash) {
			nextVal = nextBucket.get();
			if (nextVal != null)
			    return;
		    }
		    prevBucket = nextBucket;
		    nextBucket = nextBucket.next;
		}
	    }

	    public boolean hasNext() {
		return nextBucket != null;
	    }

	    public Object next() {
		if (known != modCount)
		    throw new ConcurrentModificationException();
		if (nextBucket == null)
		    throw new NoSuchElementException();
		Object result = nextVal;
		removeBucket = prevBucket;
		removeOk = true;
		prevBucket = nextBucket;
		nextBucket = nextBucket.next;
		internalNext();
		return result;
	    }

	    public void remove() {
		if (known != modCount)
		    throw new ConcurrentModificationException();
		if (!removeOk)
		    throw new IllegalStateException();
		if (removeBucket == null)
		    buckets[Math.abs(hash % buckets.length)]
			= buckets[Math.abs(hash % buckets.length)].next;
		else
		    removeBucket.next = removeBucket.next.next;
		known = ++modCount;
		size--;
	    }
	};
    }

    public void put(int hash, Object o) {
	if (size++ > threshold)
	    grow();
	modCount++;

	int slot = Math.abs(hash % buckets.length);
///#ifdef JDK12
	Bucket b = new Bucket(o, queue);
///#else
///	Bucket b = new Bucket(o);
///#endif
	b.hash = hash;
	b.next = buckets[slot];
	buckets[slot] = b;
    }

    public boolean remove(int hash, Object o) {
	Iterator i = iterateHashCode(hash);
	while (i.hasNext()) {
	    if (i.next() == o) {
		i.remove();
		return true;
	    }
	}
	return false;
    }

    public Object unify(Object o, int hash, Comparator comparator) {
///#ifdef JDK12
	cleanUp();
///#endif
	int slot = Math.abs(hash % buckets.length);
	for (Bucket b = buckets[slot]; b != null; b = b.next) {
	    Object old = b.get();
	    if (old != null && comparator.compare(o, old) == 0)
		return old;
	}

	put(hash, o);
	return o;
    }
}

