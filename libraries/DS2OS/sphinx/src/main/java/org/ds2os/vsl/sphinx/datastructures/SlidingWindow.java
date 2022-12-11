package org.ds2os.vsl.sphinx.datastructures;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * This is a kind of ring buffer.
 *
 * This is a sliding window where the data should always be listed from the
 * newest to the oldest.
 *
 * The size of the buffer can be adjusted.
 *
 *
 * @author francois
 *
 * @param <E> any type
 */

public class SlidingWindow<E> extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 8683452581122892189L;

  /**
   * The array buffer into which the elements of the ArrayList are stored. The
   * capacity of the ArrayList is the length of this array buffer.
   */
  private transient Object[] elementData;

  /**
   * @return elementData
   */
  public final Object[] getElementData() {
    return elementData;
  }

  /**
   * The size of the ArrayList (the number of elements it contains).
   *
   * @serial
   */
  private int size;

  /**
   * The maximum number of elements allowed in the list.
   */
  private int windowSize;

  /**
   * The "reader head" of the ring buffer, points allways on the newest element.
   * The oldest ist then on the right of this head.
   */
  private int newestElement = 0;

  /**
   * @return size
   */
  public int getSize() {
    return size;
  }

  /**
   * Constructs an empty list with the specified initial capacity.
   *
   * @param windowSize
   *          the initial window size
   * @exception IllegalArgumentException
   *              if the specified initial capacity is negative
   */
  public SlidingWindow(final int windowSize) {
    super();
    if (windowSize < 0) {
      throw new IllegalArgumentException("Illegal Capacity: " + windowSize);
    }
    this.elementData = new Object[windowSize];
    newestElement = -1;
    this.windowSize = windowSize;
    size = 0;
  }

  /**
   * Constructs an empty list with an initial capacity of ten.
   */
  /*
   * public SlidingWindow() { this(10); }
   */

  /**
   * Changes the window size to the given value.
   * 
   * @param newSize
   *          the new size
   */
  public void setWindowSize(final int newSize) {
    if (newSize < windowSize) { // delete all the oldest element
      Object[] newElementData;

      if (newestElement < newSize) { // tested, works perfectly
        Object[] tmpArray1 = Arrays.copyOfRange(elementData, newestElement + 1, newSize);
        Object[] tmpArray2 = Arrays.copyOfRange(elementData, 0, newestElement + 1);
        elementData = concatenate(tmpArray1, tmpArray2);

      } else { // tested, works perfectly
        newElementData = Arrays.copyOfRange(elementData, newestElement, elementData.length);
        Object[] tmpArray = Arrays.copyOfRange(elementData, (elementData.length - newestElement),
            newSize);
        elementData = concatenate(newElementData, tmpArray);
        newestElement = 0;
      }

      windowSize = newSize;
      size = windowSize;
    } else { // bigger windowSize, tested, works perfectly
      Object[] tmpArray1 = Arrays.copyOfRange(elementData, newestElement + 1, windowSize);
      Object[] tmpArray2 = Arrays.copyOfRange(elementData, 0, newestElement + 1);
      newestElement = windowSize - 1;
      elementData = concatenate(tmpArray1, tmpArray2);
      elementData = Arrays.copyOf(elementData, newSize);
      windowSize = newSize;
    }

  }

  /**
   * @param <T>
   *          e
   * @param a
   *          a
   * @param b
   *          b
   * @return cancatenated
   */
  public final <T> T[] concatenate(final T[] a, final T[] b) {
    int aLen = a.length;
    int bLen = b.length;

    @SuppressWarnings("unchecked")
    T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);

    return c;
  }

  /**
   * Trims the capacity of this <tt>ArrayList</tt> instance to be the list's
   * current size. An application can use this operation to minimize the storage
   * of an <tt>ArrayList</tt> instance.
   */
  public void trimToSize() {
    modCount++;
    int oldCapacity = elementData.length;
    if (size < oldCapacity) {
      elementData = Arrays.copyOf(elementData, size);
    }
  }

  /**
   * Increases the capacity of this <tt>ArrayList</tt> instance, if necessary,
   * to ensure that it can hold at least the number of elements specified by the
   * minimum capacity argument.
   *
   * @param minCapacity
   *          the desired minimum capacity
   */
  public void ensureCapacity(final int minCapacity) {
    modCount++;
    int oldCapacity = elementData.length;
    if (minCapacity > oldCapacity) {
      Object[] oldData = elementData;
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if (newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      // minCapacity is usually close to size, so this is a win:
      elementData = Arrays.copyOf(elementData, newCapacity);
    }
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of elements in this list
   */
  @Override
  public int size() {
    return size;
  }

  /**
   * Returns <tt>true</tt> if this list contains no elements.
   *
   * @return <tt>true</tt> if this list contains no elements
   */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified element. More
   * formally, returns <tt>true</tt> if and only if this list contains at least
   * one element <tt>e</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
   *
   * @param o
   *          element whose presence in this list is to be tested
   * @return <tt>true</tt> if this list contains the specified element
   */
  @Override
  public boolean contains(final Object o) {
    return indexOf(o) >= 0;
  }

  /**
   * Returns the index of the first occurrence of the specified element in this
   * list, or -1 if this list does not contain the element. More formally,
   * returns the lowest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   */
  @Override
  public int indexOf(final Object o) {
    if (o == null) {
      for (int i = 0; i < size; i++) {
        if (elementData[i] == null) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (o.equals(elementData[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index of the last occurrence of the specified element in this
   * list, or -1 if this list does not contain the element. More formally,
   * returns the highest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   */
  @Override
  public int lastIndexOf(final Object o) {
    if (o == null) {
      for (int i = size - 1; i >= 0; i--) {
        if (elementData[i] == null) {
          return i;
        }
      }
    } else {
      for (int i = size - 1; i >= 0; i--) {
        if (o.equals(elementData[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns an array containing all of the elements in this list in proper
   * sequence (from first to last element).
   *
   * <p>
   * The returned array will be "safe" in that no references to it are
   * maintained by this list. (In other words, this method must allocate a new
   * array). The caller is thus free to modify the returned array.
   *
   * <p>
   * This method acts as bridge between array-based and collection-based APIs.
   *
   * @return an array containing all of the elements in this list in proper
   *         sequence
   */
  @Override
  public Object[] toArray() {
    return Arrays.copyOf(elementData, size);
  }

  /**
   * Returns an array containing all of the elements in this list in proper
   * sequence (from first to last element); the runtime type of the returned
   * array is that of the specified array. If the list fits in the specified
   * array, it is returned therein. Otherwise, a new array is allocated with the
   * runtime type of the specified array and the size of this list.
   *
   * <p>
   * If the list fits in the specified array with room to spare (i.e., the array
   * has more elements than the list), the element in the array immediately
   * following the end of the collection is set to <tt>null</tt>. (This is
   * useful in determining the length of the list <i>only</i> if the caller
   * knows that the list does not contain any null elements.)
   *
   * @param a
   *          the array into which the elements of the list are to be stored, if
   *          it is big enough; otherwise, a new array of the same runtime type
   *          is allocated for this purpose.
   * @return an array containing the elements of the list
   * @throws ArrayStoreException
   *           if the runtime type of the specified array is not a supertype of
   *           the runtime type of every element in this list
   * @throws NullPointerException
   *           if the specified array is null
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(final T[] a) {
    if (a.length < size) {
      // Make a new array of a's runtime type, but my contents:
      return (T[]) Arrays.copyOf(elementData, size, a.getClass());
    }
    System.arraycopy(elementData, 0, a, 0, size);
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  // Positional Access Operations

  /**
   *
   * @param index
   *          index
   * @return data
   */
  @SuppressWarnings("unchecked")
  final E elementData(final int index) {
    return (E) elementData[index];
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index
   *          index of the element to return
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   */
  @Override
  public E get(final int index) {
    rangeCheck(index);

    return elementData(index);
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element.
   *
   * @param index
   *          index of the element to replace
   * @param element
   *          element to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   */
  @Override
  public E set(final int index, final E element) {
    rangeCheck(index);

    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * @param e
   *          element to be appended to this list
   * @return <tt>true</tt> (as specified by {@link Collection#add})
   */
  @Override
  public boolean add(final E e) {
    // ensureCapacity(size + 1); // Increments modCount!!
    // elementData[size++] = e;

    if (size < windowSize) {
      size++;
    }

    int positionNext = (newestElement + 1) % windowSize;
    // System.out.println("window wize: " + windowSize + " , newestElement : " +
    // newestElement + " ,
    // next position:" + positionNext);

    elementData[positionNext] = e;
    newestElement = positionNext;

    return true;
  }

  /**
   * Inserts the specified element at the specified position in this list.
   * Shifts the element currently at that position (if any) and any subsequent
   * elements to the right (adds one to their indices).
   *
   * @param index
   *          index at which the specified element is to be inserted
   * @param element
   *          element to be inserted
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   */
  /*
   * public void add(int index, E element) { rangeCheckForAdd(index);
   *
   * ensureCapacity(size+1); // Increments modCount!!
   * System.arraycopy(elementData, index, elementData, index + 1, size - index);
   * elementData[index] = element; size++; }
   */

  /**
   * Removes the element at the specified position in this list. Shifts any
   * subsequent elements to the left (subtracts one from their indices).
   *
   * @param index
   *          the index of the element to be removed
   * @return the element that was removed from the list
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   */
  @Override
  public E remove(final int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0) {
      System.arraycopy(elementData, index + 1, elementData, index, numMoved);
    }
    elementData[--size] = null; // Let gc do its work

    return oldValue;
  }

  /**
   * Removes the first occurrence of the specified element from this list, if it
   * is present. If the list does not contain the element, it is unchanged. More
   * formally, removes the element with the lowest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
   * (if such an element exists). Returns <tt>true</tt> if this list contained
   * the specified element (or equivalently, if this list changed as a result of
   * the call).
   *
   * @param o
   *          element to be removed from this list, if present
   * @return <tt>true</tt> if this list contained the specified element
   */
  @Override
  public boolean remove(final Object o) {
    if (o == null) {
      for (int index = 0; index < size; index++) {
        if (elementData[index] == null) {
          fastRemove(index);
          return true;
        }
      }
    } else {
      for (int index = 0; index < size; index++) {
        if (o.equals(elementData[index])) {
          fastRemove(index);
          return true;
        }
      }
    }
    return false;
  }

  /*
   * Private remove method that skips bounds checking and does not return the
   * value removed.
   */
  /**
   *
   * @param index
   *          index
   */
  private void fastRemove(final int index) {
    modCount++;
    int numMoved = size - index - 1;
    if (numMoved > 0) {
      System.arraycopy(elementData, index + 1, elementData, index, numMoved);
    }
    elementData[--size] = null; // Let gc do its work
  }

  /**
   * Removes all of the elements from this list. The list will be empty after
   * this call returns.
   */
  @Override
  public void clear() {
    modCount++;

    // Let gc do its work
    for (int i = 0; i < size; i++) {
      elementData[i] = null;
    }

    size = 0;
  }

  /**
   * Appends all of the elements in the specified collection to the end of this
   * list, in the order that they are returned by the specified collection's
   * Iterator. The behavior of this operation is undefined if the specified
   * collection is modified while the operation is in progress. (This implies
   * that the behavior of this call is undefined if the specified collection is
   * this list, and this list is nonempty.)
   *
   * @param c
   *          collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @throws NullPointerException
   *           if the specified collection is null
   */
  @Override
  public boolean addAll(final Collection<? extends E> c) {
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacity(size + numNew); // Increments modCount
    System.arraycopy(a, 0, elementData, size, numNew);
    size += numNew;
    return numNew != 0;
  }

  /**
   * Inserts all of the elements in the specified collection into this list,
   * starting at the specified position. Shifts the element currently at that
   * position (if any) and any subsequent elements to the right (increases their
   * indices). The new elements will appear in the list in the order that they
   * are returned by the specified collection's iterator.
   *
   * @param index
   *          index at which to insert the first element from the specified
   *          collection
   * @param c
   *          collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   * @throws NullPointerException
   *           if the specified collection is null
   */
  @Override
  public boolean addAll(final int index, final Collection<? extends E> c) {
    rangeCheckForAdd(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacity(size + numNew); // Increments modCount

    int numMoved = size - index;
    if (numMoved > 0) {
      System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
    }

    System.arraycopy(a, 0, elementData, index, numNew);
    size += numNew;
    return numNew != 0;
  }

  /**
   * Removes from this list all of the elements whose index is between
   * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive. Shifts any
   * succeeding elements to the left (reduces their index). This call shortens
   * the list by {@code (toIndex - fromIndex)} elements. (If
   * {@code toIndex==fromIndex}, this operation has no effect.)
   *
   * @throws IndexOutOfBoundsException
   *           if {@code fromIndex} or {@code toIndex} is out of range
   *           ({@code fromIndex < 0 ||
  *          fromIndex >= size() ||
  *          toIndex > size() ||
  *          toIndex < fromIndex})
   */
  @Override
  protected void removeRange(final int fromIndex, final int toIndex) {
    modCount++;
    int numMoved = size - toIndex;
    System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

    // Let gc do its work
    int newSize = size - (toIndex - fromIndex);
    while (size != newSize) {
      elementData[--size] = null;
    }
  }

  /**
   * Checks if the given index is in range. If not, throws an appropriate
   * runtime exception. This method does *not* check if the index is negative:
   * It is always used immediately prior to an array access, which throws an
   * ArrayIndexOutOfBoundsException if index is negative.
   * 
   * @param index
   *          index at which to insert the first element from the specified
   *          collection
   */
  private void rangeCheck(final int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
  }

  /**
   * A version of rangeCheck used by add and addAll.
   * 
   * @param index
   *          index at which to insert the first element from the specified
   *          collection
   */
  private void rangeCheckForAdd(final int index) {
    if (index > size || index < 0) {
      throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
  }

  /**
   * Constructs an IndexOutOfBoundsException detail message. Of the many
   * possible refactorings of the error handling code, this "outlining" performs
   * best with both server and client VMs.
   * 
   * @param index
   *          index at which to insert the first element from the specified
   *          collection
   * @return msg
   */
  private String outOfBoundsMsg(final int index) {
    return "Index: " + index + ", Size: " + size;
  }

  /**
   * Removes from this list all of its elements that are contained in the
   * specified collection.
   *
   * @param c
   *          collection containing elements to be removed from this list
   * @return {@code true} if this list changed as a result of the call
   * @throws ClassCastException
   *           if the class of an element of this list is incompatible with the
   *           specified collection (optional)
   * @throws NullPointerException
   *           if this list contains a null element and the specified collection
   *           does not permit null elements (optional), or if the specified
   *           collection is null
   * @see Collection#contains(Object)
   */
  @Override
  public boolean removeAll(final Collection<?> c) {
    return batchRemove(c, false);
  }

  /**
   * Retains only the elements in this list that are contained in the specified
   * collection. In other words, removes from this list all of its elements that
   * are not contained in the specified collection.
   *
   * @param c
   *          collection containing elements to be retained in this list
   * @return {@code true} if this list changed as a result of the call
   * @throws ClassCastException
   *           if the class of an element of this list is incompatible with the
   *           specified collection (optional)
   * @throws NullPointerException
   *           if this list contains a null element and the specified collection
   *           does not permit null elements (optional), or if the specified
   *           collection is null
   * @see Collection#contains(Object)
   */
  @Override
  public boolean retainAll(final Collection<?> c) {
    return batchRemove(c, true);
  }

  /**
   *
   * @param c
   *          c
   * @param complement
   *          complement
   * @return boolean
   */
  private boolean batchRemove(final Collection<?> c, final boolean complement) {
    /** */
    final Object[] elementData = this.elementData;
    int r = 0, w = 0;
    boolean modified = false;
    try {
      for (; r < size; r++) {
        if (c.contains(elementData[r]) == complement) {
          elementData[w++] = elementData[r];
        }
      }
    } finally {
      // Preserve behavioral compatibility with AbstractCollection,
      // even if c.contains() throws.
      if (r != size) {
        System.arraycopy(elementData, r, elementData, w, size - r);
        w += size - r;
      }
      if (w != size) {
        for (int i = w; i < size; i++) {
          elementData[i] = null;
        }
        modCount += size - w;
        size = w;
        modified = true;
      }
    }
    return modified;
  }

  /**
   * Save the state of the <tt>ArrayList</tt> instance to a stream (that is,
   * serialize it).
   *
   * @serialData The length of the array backing the <tt>ArrayList</tt> instance
   *             is emitted (int), followed by all of its elements (each an
   *             <tt>Object</tt>) in the proper order.
   * @param s
   *          s
   * @throws java.io.IOException
   *           e
   */
  private void writeObject(final java.io.ObjectOutputStream s) throws java.io.IOException {
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out array length
    s.writeInt(elementData.length);

    // Write out all elements in the proper order.
    for (int i = 0; i < size; i++) {
      s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
      throw new ConcurrentModificationException();
    }

  }

  /**
   * Reconstitute the <tt>ArrayList</tt> instance from a stream (that is,
   * deserialize it).
   * 
   * @param s
   *          s
   * @throws java.io.IOException
   *           e
   * @throws ClassNotFoundException
   *           e
   */
  private void readObject(final java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in array length and allocate array
    int arrayLength = s.readInt();
    /** */
    Object[] a = elementData = new Object[arrayLength];

    // Read in all elements in the proper order.
    for (int i = 0; i < size; i++) {
      a[i] = s.readObject();
    }
  }

  /**
   * Returns a list iterator over the elements in this list (in proper
   * sequence), starting at the specified position in the list. The specified
   * index indicates the first element that would be returned by an initial call
   * to {@link ListIterator#next next}. An initial call to
   * {@link ListIterator#previous previous} would return the element with the
   * specified index minus one.
   *
   * <p>
   * The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
   *
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   */
  @Override
  public ListIterator<E> listIterator(final int index) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("Index: " + index);
    }
    return new ListItr(index);
  }

  /**
   * Returns a list iterator over the elements in this list (in proper
   * sequence).
   *
   * <p>
   * The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
   *
   * @see #listIterator(int)
   */
  @Override
  public ListIterator<E> listIterator() {
    return new ListItr(0);
  }

  /**
   * Returns an iterator over the elements in this list in proper sequence.
   *
   * <p>
   * The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
   *
   * @return an iterator over the elements in this list in proper sequence
   */
  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * An optimized version of AbstractList.Itr.
   */
  private class Itr implements Iterator<E> {
    /** */
    int cursor; // index of next element to return
    /** */
    int lastRet = -1; // index of last element returned; -1 if no such
    /** */
    int expectedModCount = modCount;

    @Override
    public boolean hasNext() {
      return cursor != size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E next() {
      checkForComodification();
      int i = cursor;
      if (i >= size) {
        throw new NoSuchElementException();
      }
      Object[] elementData = SlidingWindow.this.elementData;
      if (i >= elementData.length) {
        throw new ConcurrentModificationException();
      }
      cursor = i + 1;
      return (E) elementData[lastRet = i];
    }

    @Override
    public void remove() {
      if (lastRet < 0) {
        throw new IllegalStateException();
      }
      checkForComodification();

      try {
        SlidingWindow.this.remove(lastRet);
        cursor = lastRet;
        lastRet = -1;
        expectedModCount = modCount;
      } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }

    /** */
    final void checkForComodification() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  /**
   * An optimized version of AbstractList.ListItr.
   */
  private class ListItr extends Itr implements ListIterator<E> {

    /**
     * @param index
     *          i
     */
    ListItr(final int index) {
      super();
      cursor = index;
    }

    @Override
    public boolean hasPrevious() {
      return cursor != 0;
    }

    @Override
    public int nextIndex() {
      return cursor;
    }

    @Override
    public int previousIndex() {
      return cursor - 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E previous() {
      checkForComodification();
      int i = cursor - 1;
      if (i < 0) {
        throw new NoSuchElementException();
      }
      Object[] elementData = SlidingWindow.this.elementData;
      if (i >= elementData.length) {
        throw new ConcurrentModificationException();
      }
      cursor = i;
      return (E) elementData[lastRet = i];
    }

    @Override
    public void set(final E e) {
      if (lastRet < 0) {
        throw new IllegalStateException();
      }
      checkForComodification();

      try {
        SlidingWindow.this.set(lastRet, e);
      } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public void add(final E e) {
      checkForComodification();

      try {
        int i = cursor;
        SlidingWindow.this.add(i, e);
        cursor = i + 1;
        lastRet = -1;
        expectedModCount = modCount;
      } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }
  }

  /**
   * Returns a view of the portion of this list between the specified
   * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive. (If
   * {@code fromIndex} and {@code toIndex} are equal, the returned list is
   * empty.) The returned list is backed by this list, so non-structural changes
   * in the returned list are reflected in this list, and vice-versa. The
   * returned list supports all of the optional list operations.
   *
   * <p>
   * This method eliminates the need for explicit range operations (of the sort
   * that commonly exist for arrays). Any operation that expects a list can be
   * used as a range operation by passing a subList view instead of a whole
   * list. For example, the following idiom removes a range of elements from a
   * list:
   *
   * <pre>
   * list.subList(from, to).clear();
   * </pre>
   *
   * Similar idioms may be constructed for {@link #indexOf(Object)} and
   * {@link #lastIndexOf(Object)}, and all of the algorithms in the
   * {@link Collections} class can be applied to a subList.
   *
   * <p>
   * The semantics of the list returned by this method become undefined if the
   * backing list (i.e., this list) is <i>structurally modified</i> in any way
   * other than via the returned list. (Structural modifications are those that
   * change the size of this list, or otherwise perturb it in such a fashion
   * that iterations in progress may yield incorrect results.)
   *
   * @throws IndexOutOfBoundsException
   *           {@inheritDoc}
   * @throws IllegalArgumentException
   *           {@inheritDoc}
   */
  /*
   * public List<E> subList(int fromIndex, int toIndex) {
   * subListRangeCheck(fromIndex, toIndex, size); return new SubList(this, 0,
   * fromIndex, toIndex); }
   *
   * static void subListRangeCheck(int fromIndex, int toIndex, int size) { if
   * (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " +
   * fromIndex); if (toIndex > size) throw new
   * IndexOutOfBoundsException("toIndex = " + toIndex); if (fromIndex > toIndex)
   * throw new IllegalArgumentException("fromIndex(" + fromIndex +
   * ") > toIndex(" + toIndex + ")"); }
   *
   * private class SubList extends AbstractList<E> implements RandomAccess {
   * private final AbstractList<E> parent; private final int parentOffset;
   * private final int offset; private int size;
   *
   * SubList(AbstractList<E> parent, int offset, int fromIndex, int toIndex) {
   * this.parent = parent; this.parentOffset = fromIndex; this.offset = offset +
   * fromIndex; this.size = toIndex - fromIndex; this.modCount =
   * ArrayList.this.modCount; }
   *
   * public E set(int index, E e) { rangeCheck(index); checkForComodification();
   * E oldValue = ArrayList.this.elementData(offset + index);
   * ArrayList.this.elementData[offset + index] = e; return oldValue; }
   *
   * public E get(int index) { rangeCheck(index); checkForComodification();
   * return ArrayList.this.elementData(offset + index); }
   *
   * public int size() { checkForComodification(); return this.size; }
   *
   * public void add(int index, E e) { rangeCheckForAdd(index);
   * checkForComodification(); parent.add(parentOffset + index, e);
   * this.modCount = parent.modCount; this.size++; }
   *
   * public E remove(int index) { rangeCheck(index); checkForComodification(); E
   * result = parent.remove(parentOffset + index); this.modCount =
   * parent.modCount; this.size--; return result; }
   *
   * protected void removeRange(int fromIndex, int toIndex) {
   * checkForComodification(); parent.removeRange(parentOffset + fromIndex,
   * parentOffset + toIndex); this.modCount = parent.modCount; this.size -=
   * toIndex - fromIndex; }
   *
   * public boolean addAll(Collection<? extends E> c) { return addAll(this.size,
   * c); }
   *
   * public boolean addAll(int index, Collection<? extends E> c) {
   * rangeCheckForAdd(index); int cSize = c.size(); if (cSize==0) return false;
   *
   * checkForComodification(); parent.addAll(parentOffset + index, c);
   * this.modCount = parent.modCount; this.size += cSize; return true; }
   *
   * public Iterator<E> iterator() { return listIterator(); }
   *
   * public ListIterator<E> listIterator(final int index) {
   * checkForComodification(); rangeCheckForAdd(index);
   *
   * return new ListIterator<E>() { int cursor = index; int lastRet = -1; int
   * expectedModCount = ArrayList.this.modCount;
   *
   * public boolean hasNext() { return cursor != SubList.this.size; }
   *
   * @SuppressWarnings("unchecked") public E next() { checkForComodification();
   * int i = cursor; if (i >= SubList.this.size) throw new
   * NoSuchElementException(); Object[] elementData =
   * ArrayList.this.elementData; if (offset + i >= elementData.length) throw new
   * ConcurrentModificationException(); cursor = i + 1; return (E)
   * elementData[offset + (lastRet = i)]; }
   *
   * public boolean hasPrevious() { return cursor != 0; }
   *
   * @SuppressWarnings("unchecked") public E previous() {
   * checkForComodification(); int i = cursor - 1; if (i < 0) throw new
   * NoSuchElementException(); Object[] elementData =
   * ArrayList.this.elementData; if (offset + i >= elementData.length) throw new
   * ConcurrentModificationException(); cursor = i; return (E)
   * elementData[offset + (lastRet = i)]; }
   *
   * public int nextIndex() { return cursor; }
   *
   * public int previousIndex() { return cursor - 1; }
   *
   * public void remove() { if (lastRet < 0) throw new IllegalStateException();
   * checkForComodification();
   *
   * try { SubList.this.remove(lastRet); cursor = lastRet; lastRet = -1;
   * expectedModCount = ArrayList.this.modCount; } catch
   * (IndexOutOfBoundsException ex) { throw new
   * ConcurrentModificationException(); } }
   *
   * public void set(E e) { if (lastRet < 0) throw new IllegalStateException();
   * checkForComodification();
   *
   * try { ArrayList.this.set(offset + lastRet, e); } catch
   * (IndexOutOfBoundsException ex) { throw new
   * ConcurrentModificationException(); } }
   *
   * public void add(E e) { checkForComodification();
   *
   * try { int i = cursor; SubList.this.add(i, e); cursor = i + 1; lastRet = -1;
   * expectedModCount = ArrayList.this.modCount; } catch
   * (IndexOutOfBoundsException ex) { throw new
   * ConcurrentModificationException(); } }
   *
   * final void checkForComodification() { if (expectedModCount !=
   * ArrayList.this.modCount) throw new ConcurrentModificationException(); } };
   * }
   *
   * public List<E> subList(int fromIndex, int toIndex) {
   * subListRangeCheck(fromIndex, toIndex, size); return new SubList(this,
   * offset, fromIndex, toIndex); }
   *
   * private void rangeCheck(int index) { if (index < 0 || index >= this.size)
   * throw new IndexOutOfBoundsException(outOfBoundsMsg(index)); }
   *
   * private void rangeCheckForAdd(int index) { if (index < 0 || index >
   * this.size) throw new IndexOutOfBoundsException(outOfBoundsMsg(index)); }
   *
   * private String outOfBoundsMsg(int index) { return
   * "Index: "+index+", Size: "+this.size; }
   *
   * private void checkForComodification() { if (ArrayList.this.modCount !=
   * this.modCount) throw new ConcurrentModificationException(); } }
   */

}