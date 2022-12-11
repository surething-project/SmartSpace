package org.ds2os.vsl.sphinx.datastructures;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The is an optimized list.
 *
 * @author francois
 *
 * @param <K>
 *          key class
 * @param <V>
 *          indexed class
 */
public class HashList<K, V> extends Dictionary<K, V>
    implements Map<K, V>, Cloneable, Iterable<V>, Serializable { // Iterator<V>,

  /** The hash table data. */
  private transient Entry[] table;

  /** The list of the existing keys. */
  private transient List<K> keyList = new ArrayList<K>();
  /** The list of the existing hashes. */
  private transient List<K> hashList = new ArrayList<K>();

  /** The total number of entries in the hash table. */
  private transient int count;

  /**
   * The table is rehashed when its size exceeds this threshold. (The value of
   * this field is (int)(capacity * loadFactor).)
   *
   * @serial
   */
  private int threshold;

  /**
   * The load factor for the hashtable.
   *
   * @serial
   */
  private float loadFactor;

  /**
   * The number of times this Hashtable has been structurally modified
   * Structural modifications are those that change the number of entries in the
   * Hashtable or otherwise modify its internal structure (e.g., rehash). This
   * field is used to make iterators on Collection-views of the Hashtable
   * fail-fast. (See ConcurrentModificationException).
   */
  private transient int modCount = 0;

  /** use serialVersionUID from JDK 1.0.2 for interoperability. */
  private static final long serialVersionUID = 1421746759512286392L;

  /**
   * Constructs a new, empty hashtable with the specified initial capacity and
   * the specified load factor.
   *
   * @param initialCapacity
   *          the initial capacity of the hashtable.
   * @param loadFactor
   *          the load factor of the hashtable.
   * @exception IllegalArgumentException
   *              if the initial capacity is less than zero, or if the load
   *              factor is nonpositive.
   */
  public HashList(final int initialCapacity, final float loadFactor) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    }
    if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
      throw new IllegalArgumentException("Illegal Load: " + loadFactor);
    }
    int otherinitialCapacity = initialCapacity;
    if (otherinitialCapacity == 0) {
      otherinitialCapacity = 1;
    }
    this.loadFactor = loadFactor;
    table = new Entry[otherinitialCapacity];
    threshold = (int) (otherinitialCapacity * loadFactor);
  }

  /**
   * Constructs a new, empty hashtable with the specified initial capacity and
   * default load factor (0.75).
   *
   * @param initialCapacity
   *          the initial capacity of the hashtable.
   * @exception IllegalArgumentException
   *              if the initial capacity is less than zero.
   */
  public HashList(final int initialCapacity) {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty hashtable with a default initial capacity (11) and
   * load factor (0.75).
   */
  public HashList() {
    this(11, 0.75f);
  }

  /**
   * Constructs a new hashtable with the same mappings as the given Map. The
   * hashtable is created with an initial capacity sufficient to hold the
   * mappings in the given Map and a default load factor (0.75).
   *
   * @param t
   *          the map whose mappings are to be placed in this map.
   * @throws NullPointerException
   *           if the specified map is null.
   * @since 1.2
   */
  public HashList(final Map<? extends K, ? extends V> t) {
    this(Math.max(2 * t.size(), 11), 0.75f);
    putAll(t);
  }

  /**
   * Returns the number of keys in this hashtable.
   *
   * @return the number of keys in this hashtable.
   */
  @Override
  public synchronized int size() {
    return count;
  }

  /**
   * Tests if this hashtable maps no keys to values.
   *
   * @return <code>true</code> if this hashtable maps no keys to values;
   *         <code>false</code> otherwise.
   */
  @Override
  public synchronized boolean isEmpty() {
    return count == 0;
  }

  /**
   * Returns an enumeration of the keys in this hashtable.
   *
   * @return an enumeration of the keys in this hashtable.
   * @see Enumeration
   * @see #elements()
   * @see #keySet()
   * @see Map
   */
  @Override
  public synchronized Enumeration<K> keys() {
    return this.<K>getEnumeration(KEYS);
  }

  /**
   * Returns an enumeration of the values in this hashtable. Use the Enumeration
   * methods on the returned object to fetch the elements sequentially.
   *
   * @return an enumeration of the values in this hashtable.
   * @see java.util.Enumeration
   * @see #keys()
   * @see #values()
   * @see Map
   */
  @Override
  public synchronized Enumeration<V> elements() {
    return this.<V>getEnumeration(VALUES);
  }

  /**
   * Tests if some key maps into the specified value in this hashtable. This
   * operation is more expensive than the {@link #containsKey containsKey}
   * method. O(n)
   *
   * <p>
   * Note that this method is identical in functionality to
   * {@link #containsValue containsValue}, (which is part of the {@link Map}
   * interface in the collections framework).
   *
   * @param value
   *          a value to search for
   * @return <code>true</code> if and only if some key maps to the
   *         <code>value</code> argument in this hashtable as determined by the
   *         <tt>equals</tt> method; <code>false</code> otherwise.
   * @exception NullPointerException
   *              if the value is <code>null</code>
   */
  public synchronized boolean contains(final Object value) {
    if (value == null) {
      throw new NullPointerException();
    }

    Entry[] tab = table;
    for (int i = tab.length; i-- > 0;) {
      for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
        if (e.value.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if this hashtable maps one or more keys to this value.
   *
   * <p>
   * Note that this method is identical in functionality to {@link #contains
   * contains} (which predates the {@link Map} interface).
   *
   * @param value
   *          value whose presence in this hashtable is to be tested
   * @return <tt>true</tt> if this map maps one or more keys to the specified
   *         value
   * @throws NullPointerException
   *           if the value is <code>null</code>
   * @since 1.2
   */
  @Override
  public boolean containsValue(final Object value) {
    return contains(value);
  }

  /**
   * Tests if the specified object is a key in this hashtable. O(1) (if table
   * well balanced)
   *
   * @param key
   *          possible key
   * @return <code>true</code> if and only if the specified object is a key in
   *         this hashtable, as determined by the <tt>equals</tt> method;
   *         <code>false</code> otherwise.
   * @throws NullPointerException
   *           if the key is <code>null</code>
   * @see #contains(Object)
   */
  @Override
  public synchronized boolean containsKey(final Object key) {
    Entry[] tab = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<K, V> e = tab[index]; e != null; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the value to which the specified key is mapped, or {@code null} if
   * this map contains no mapping for the key. O(1) (if table well balanced)
   *
   * <p>
   * More formally, if this map contains a mapping from a key {@code k} to a
   * value {@code v} such that {@code (key.equals(k))}, then this method returns
   * {@code v}; otherwise it returns {@code null}. (There can be at most one
   * such mapping.)
   *
   * @param key
   *          the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or {@code null} if
   *         this map contains no mapping for the key
   * @throws NullPointerException
   *           if the specified key is null
   * @see #put(Object, Object)
   */
  @Override
  public synchronized V get(final Object key) {
    Entry[] tab = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<K, V> e = tab[index]; e != null; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        return e.value;
      }
    }
    return null;
  }

  /**
   * Increases the capacity of and internally reorganizes this hashtable, in
   * order to accommodate and access its entries more efficiently. This method
   * is called automatically when the number of keys in the hashtable exceeds
   * this hashtable's capacity and load factor.
   */
  protected void rehash() {
    int oldCapacity = table.length;
    Entry[] oldMap = table;

    int newCapacity = oldCapacity * 2 + 1;
    Entry[] newMap = new Entry[newCapacity];

    modCount++;
    threshold = (int) (newCapacity * loadFactor);
    table = newMap;

    for (int i = oldCapacity; i-- > 0;) {
      for (Entry<K, V> old = oldMap[i]; old != null;) {
        Entry<K, V> e = old;
        old = old.next;

        int index = (e.hash & 0x7FFFFFFF) % newCapacity;
        e.next = newMap[index];
        newMap[index] = e;
      }
    }
  }

  /**
   * Maps the specified <code>key</code> to the specified <code>value</code> in
   * this hashtable. Neither the key nor the value can be <code>null</code>.
   * <p>
   *
   * The value can be retrieved by calling the <code>get</code> method with a
   * key that is equal to the original key.
   *
   * @param key
   *          the hashtable key
   * @param value
   *          the value
   * @return the previous value of the specified key in this hashtable, or
   *         <code>null</code> if it did not have one
   * @exception NullPointerException
   *              if the key or value is <code>null</code>
   * @see Object#equals(Object)
   * @see #get(Object)
   */
  @Override
  public synchronized V put(final K key, final V value) {
    // Make sure the value is not null
    if (value == null) {
      throw new NullPointerException();
    }

    // System.out.println("just added:" + key + ", " + value);

    // add the key to the key list
    keyList.add(key);

    // Makes sure the key is not already in the hashtable.
    Entry[] tab = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<K, V> e = tab[index]; e != null; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        V old = e.value;
        e.value = value;
        return old;
      }
    }

    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // Creates the new entry.
    Entry<K, V> e = tab[index];
    tab[index] = new Entry<K, V>(hash, key, value, e);
    count++;

    return null;
  }

  /**
   * Removes the key (and its corresponding value) from this hashtable. This
   * method does nothing if the key is not in the hashtable.
   *
   * @param key
   *          the key that needs to be removed
   * @return the value to which the key had been mapped in this hashtable, or
   *         <code>null</code> if the key did not have a mapping
   * @throws NullPointerException
   *           if the key is <code>null</code>
   */
  @Override
  public synchronized V remove(final Object key) {
    Entry[] tab = table;

    keyList.remove(key);

    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        modCount++;
        if (prev != null) {
          prev.next = e.next;
        } else {
          tab[index] = e.next;
        }
        count--;
        V oldValue = e.value;
        e.value = null;
        return oldValue;
      }
    }
    return null;
  }

  /**
   * Copies all of the mappings from the specified map to this hashtable. These
   * mappings will replace any mappings that this hashtable had for any of the
   * keys currently in the specified map.
   *
   * @param t
   *          mappings to be stored in this map
   * @throws NullPointerException
   *           if the specified map is null
   * @since 1.2
   */
  @Override
  public synchronized void putAll(final Map<? extends K, ? extends V> t) {
    for (Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  /**
   * Clears this hashtable so that it contains no keys.
   */
  @Override
  public synchronized void clear() {
    Entry[] tab = table;
    modCount++;
    for (int index = tab.length; --index >= 0;) {
      tab[index] = null;
    }
    count = 0;
  }

  // ................................. other methods
  // .................................

  /**
   * Creates a shallow copy of this hashtable. All the structure of the
   * hashtable itself is copied, but the keys and values are not cloned. This is
   * a relatively expensive operation.
   *
   * @return a clone of the hashtable
   */
  @Override
  public synchronized Object clone() {
    try {
      HashList<K, V> t = (HashList<K, V>) super.clone();
      t.table = new Entry[table.length];
      for (int i = table.length; i-- > 0;) {
        t.table[i] = (table[i] != null) ? (Entry<K, V>) table[i].clone() : null;
      }
      t.keySet = null;
      t.entrySet = null;
      t.values = null;
      t.modCount = 0;
      return t;
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns a string representation of this <tt>Hashtable</tt> object in the
   * form of a set of entries, enclosed in braces and separated by the ASCII
   * characters "<tt>,&nbsp;</tt>" (comma and space). Each entry is rendered as
   * the key, an equals sign <tt>=</tt>, and the associated element, where the
   * <tt>toString</tt> method is used to convert the key and element to strings.
   *
   * @return a string representation of this hashtable
   */
  @Override
  public synchronized String toString() {
    int max = size() - 1;
    if (max == -1) {
      return "{}";
    }

    StringBuilder sb = new StringBuilder();
    Iterator<Map.Entry<K, V>> it = entrySet().iterator();

    sb.append('{');
    for (int i = 0;; i++) {
      Map.Entry<K, V> e = it.next();
      K key = e.getKey();
      V value = e.getValue();
      sb.append(key == this ? "(this Map)" : key.toString());
      sb.append('=');
      sb.append(value == this ? "(this Map)" : value.toString());

      if (i == max) {
        return sb.append('}').toString();
      }
      sb.append(", ");
    }
  }

  /**
   * Enumarator.
   * 
   * @param <T>
   *          class
   * @param type
   *          type
   * @return enumerator
   */
  private <T> Enumeration<T> getEnumeration(final int type) {
    if (count == 0) {
      return Collections.emptyEnumeration();
    } else {
      return new Enumerator<T>(type, false);
    }
  }

  /**
   * Iterator.
   * 
   * @param <T>
   *          class
   * @param type
   *          type
   * @return enumerator
   */
  private <T> Iterator<T> getIterator(final int type) {
    if (count == 0) {
      return Collections.emptyIterator();
    } else {
      return new Enumerator<T>(type, true);
    }
  }

  // Views

  /**
   * Each of these fields are initialized to contain an instance of the
   * appropriate view the first time this view is requested. The views are
   * stateless, so there's no reason to create more than one of each.
   */
  private transient volatile Set<K> keySet = null;
  /** */
  private transient volatile Set<Map.Entry<K, V>> entrySet = null;
  /** */
  private transient volatile Collection<V> values = null;

  /**
   * Returns a {@link Set} view of the keys contained in this map. The set is
   * backed by the map, so changes to the map are reflected in the set, and
   * vice-versa. If the map is modified while an iteration over the set is in
   * progress (except through the iterator's own <tt>remove</tt> operation), the
   * results of the iteration are undefined. The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt>, and <tt>clear</tt> operations. It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @since 1.2
   */
  @Override
  public Set<K> keySet() {
    // if (keySet == null)
    // keySet = Collections.synchronizedSet(new KeySet(), this);
    return keySet;
  }

  /**
   *
   *
   */
  private class KeySet extends AbstractSet<K> {
    @Override
    public Iterator<K> iterator() {
      return getIterator(KEYS);
    }

    @Override
    public int size() {
      return count;
    }

    @Override
    public boolean contains(final Object o) {
      return containsKey(o);
    }

    @Override
    public boolean remove(final Object o) {
      return HashList.this.remove(o) != null;
    }

    @Override
    public void clear() {
      HashList.this.clear();
    }
  }

  /**
   * Returns a {@link Set} view of the mappings contained in this map. The set
   * is backed by the map, so changes to the map are reflected in the set, and
   * vice-versa. If the map is modified while an iteration over the set is in
   * progress (except through the iterator's own <tt>remove</tt> operation, or
   * through the <tt>setValue</tt> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined. The set supports
   * element removal, which removes the corresponding mapping from the map, via
   * the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @since 1.2
   */

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    // if (entrySet==null)
    // entrySet = Collections.synchronizedSet(new EntrySet(), this);
    return entrySet;
  }

  /**
   *
   */
  private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return getIterator(ENTRIES);
    }

    @Override
    public boolean add(final Map.Entry<K, V> o) {
      return super.add(o);
    }

    @Override
    public boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry entry = (Map.Entry) o;
      Object key = entry.getKey();
      Entry[] tab = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;

      for (Entry e = tab[index]; e != null; e = e.next) {
        if (e.hash == hash && e.equals(entry)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean remove(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
      K key = entry.getKey();
      Entry[] tab = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;

      for (Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
        if (e.hash == hash && e.equals(entry)) {
          modCount++;
          if (prev != null) {
            prev.next = e.next;
          } else {
            tab[index] = e.next;
          }

          count--;
          e.value = null;
          return true;
        }
      }
      return false;
    }

    @Override
    public int size() {
      return count;
    }

    @Override
    public void clear() {
      HashList.this.clear();
    }
  }

  /**
   * Returns a {@link Collection} view of the values contained in this map. The
   * collection is backed by the map, so changes to the map are reflected in the
   * collection, and vice-versa. If the map is modified while an iteration over
   * the collection is in progress (except through the iterator's own
   * <tt>remove</tt> operation), the results of the iteration are undefined. The
   * collection supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
   * <tt>clear</tt> operations. It does not support the <tt>add</tt> or
   * <tt>addAll</tt> operations.
   *
   * @since 1.2
   */
  @Override
  public Collection<V> values() {
    // if (values==null)
    // values = Collections.synchronizedCollection(new ValueCollection(), this);
    return values;
  }

  /** */
  private class ValueCollection extends AbstractCollection<V> {
    @Override
    public Iterator<V> iterator() {
      return getIterator(VALUES);
    }

    @Override
    public int size() {
      return count;
    }

    @Override
    public boolean contains(final Object o) {
      return containsValue(o);
    }

    @Override
    public void clear() {
      HashList.this.clear();
    }
  }

  // Comparison and hashing

  /**
   * Compares the specified Object with this Map for equality, as per the
   * definition in the Map interface.
   *
   * @param o
   *          object to be compared for equality with this hashtable
   * @return true if the specified Object is equal to this Map
   * @see Map#equals(Object)
   * @since 1.2
   */
  @Override
  public synchronized boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Map)) {
      return false;
    }
    Map<K, V> t = (Map<K, V>) o;
    if (t.size() != size()) {
      return false;
    }

    try {
      Iterator<Map.Entry<K, V>> i = entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry<K, V> e = i.next();
        K key = e.getKey();
        V value = e.getValue();
        if (value == null) {
          if (!(t.get(key) == null && t.containsKey(key))) {
            return false;
          }
        } else {
          if (!value.equals(t.get(key))) {
            return false;
          }
        }
      }
    } catch (ClassCastException unused) {
      return false;
    } catch (NullPointerException unused) {
      return false;
    }

    return true;
  }

  /**
   * Returns the hash code value for this Map as per the definition in the Map
   * interface.
   *
   * @see Map#hashCode()
   * @since 1.2
   */
  @Override
  public synchronized int hashCode() {
    /*
     * This code detects the recursion caused by computing the hash code of a
     * self-referential hash table and prevents the stack overflow that would
     * otherwise result. This allows certain 1.1-era applets with
     * self-referential hash tables to work. This code abuses the loadFactor
     * field to do double-duty as a hashCode in progress flag, so as not to
     * worsen the space performance. A negative load factor indicates that hash
     * code computation is in progress.
     */
    int h = 0;
    if (count == 0 || loadFactor < 0) {
      return h; // Returns zero
    }

    loadFactor = -loadFactor; // Mark hashCode computation in progress
    Entry[] tab = table;
    for (int i = 0; i < tab.length; i++) {
      for (Entry e = tab[i]; e != null; e = e.next) {
        h += e.key.hashCode() ^ e.value.hashCode();
      }
    }
    loadFactor = -loadFactor; // Mark hashCode computation complete

    return h;
  }

  /**
   * Hashtable collision list.
   */
  /**
   *
   * @param <K>
   * @param <V>
   */
  private static class Entry<K, V> implements Map.Entry<K, V> {
    /** */
    int hash;
    /** */
    K key;
    /** */
    V value;
    /** */
    Entry<K, V> next;

    /**
     * @param hash
     *          a
     * @param key
     *          a
     * @param value
     *          a
     * @param next
     *          a
     */
    protected Entry(final int hash, final K key, final V value, final Entry<K, V> next) {
      this.hash = hash;
      this.key = key;
      this.value = value;
      this.next = next;
    }

    @Override
    protected Object clone() {
      return new Entry<K, V>(hash, key, value, (next == null ? null : (Entry<K, V>) next.clone()));
    }

    // Map.Entry Ops

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(final V value) {
      if (value == null) {
        throw new NullPointerException();
      }

      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry e = (Map.Entry) o;

      return (key == null ? e.getKey() == null : key.equals(e.getKey()))
          && (value == null ? e.getValue() == null : value.equals(e.getValue()));
    }

    @Override
    public int hashCode() {
      return hash ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return key.toString() + "=" + value.toString();
    }
  }

  // Types of Enumerations/Iterations
  /** */
  private static final int KEYS = 0;
  /** */
  private static final int VALUES = 1;
  /** */
  private static final int ENTRIES = 2;

  /**
   * A hashtable enumerator class. This class implements both the Enumeration
   * and Iterator interfaces, but individual instances can be created with the
   * Iterator methods disabled. This is necessary to avoid unintentionally
   * increasing the capabilities granted a user by passing an Enumeration.
   * 
   * @param <T>
   */
  private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
    /** */
    Entry[] table = HashList.this.table;
    /** */
    int index = table.length;
    /** */
    Entry<K, V> entry = null;
    /** */
    Entry<K, V> lastReturned = null;
    /** */
    int type;

    /**
     * Indicates whether this Enumerator is serving as an Iterator or an
     * Enumeration. (true -> Iterator).
     */
    boolean iterator;

    /**
     * The modCount value that the iterator believes that the backing Hashtable
     * should have. If this expectation is violated, the iterator has detected
     * concurrent modification.
     */
    int expectedModCount = modCount;

    /**
     * @param type
     *          a
     * @param iterator
     *          a
     */
    Enumerator(final int type, final boolean iterator) {
      this.type = type;
      this.iterator = iterator;
    }

    @Override
    public boolean hasMoreElements() {
      Entry<K, V> e = entry;
      int i = index;
      Entry[] t = table;
      /* Use locals for faster loop iteration */
      while (e == null && i > 0) {
        e = t[--i];
      }
      entry = e;
      index = i;
      return e != null;
    }

    @Override
    public T nextElement() {
      Entry<K, V> et = entry;
      int i = index;
      Entry[] t = table;
      /* Use locals for faster loop iteration */
      while (et == null && i > 0) {
        et = t[--i];
      }
      entry = et;
      index = i;
      if (et != null) {
        Entry<K, V> e = lastReturned = entry;
        entry = e.next;
        return type == KEYS ? (T) e.key : (type == VALUES ? (T) e.value : (T) e);
      }
      throw new NoSuchElementException("Hashtable Enumerator");
    }

    // Iterator methods
    @Override
    public boolean hasNext() {
      return hasMoreElements();
    }

    @Override
    public T next() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      return nextElement();
    }

    @Override
    public void remove() {
      if (!iterator) {
        throw new UnsupportedOperationException();
      }
      if (lastReturned == null) {
        throw new IllegalStateException("Hashtable Enumerator");
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }

      synchronized (HashList.this) {
        Entry[] tab = HashList.this.table;
        int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

        for (Entry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
          if (e == lastReturned) {
            modCount++;
            expectedModCount++;
            if (prev == null) {
              tab[index] = e.next;
            } else {
              prev.next = e.next;
            }
            count--;
            lastReturned = null;
            return;
          }
        }
        throw new ConcurrentModificationException();
      }
    }
  }

  // ......................... The iterator methods
  // ....................................
  /*
   * @Override public boolean hasNext() { // TODO Auto-generated method stub
   * return false; }
   *
   * @Override public V next() { // TODO Auto-generated method stub return null;
   * }
   */

  @Override
  public Iterator<V> iterator() {
    // keyList = keyList.stream().distinct().collect(Collectors.toList()); //
    // find the reason why
    // double are here???
    return new Itr();
  }

  /**
   *
   *
   */
  private class Itr implements Iterator<V> {
    /** */
    int cursor; // index of next element to return
    /** */
    int lastRet = -1; // index of last element returned; -1 if no such
    /** */
    int expectedModCount = modCount;

    @Override
    public boolean hasNext() {
      return cursor != count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V next() {
      checkForComodification();
      int i = cursor;
      if (i >= count) {
        throw new NoSuchElementException();
      }

      // Object[] elementData = keyList; //ArrayList.this.elementData;
      // if (i >= elementData.length)
      // throw new ConcurrentModificationException();
      for (K name : keyList) {
        break;
        // System.out.println(name);
      }

      cursor = i + 1;
      // System.out.println(keyList.get(lastRet = i) + ":" +keyList.get(lastRet
      // = i).hashCode());
      return get(keyList.get(lastRet = i)); // elementData[lastRet = i];
    }

    /**
     * Does not work right now, should be implemented if needed.
     *
     */
    @Override
    public void remove() {
      if (lastRet < 0) {
        throw new IllegalStateException();
      }
      checkForComodification();

      try {
        // ArrayList.this.remove(lastRet);
        // remove((Object)keyList.get(lastRet));
        cursor = lastRet;
        lastRet = -1;
        expectedModCount = modCount;
      } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }

    /**
     */
    final void checkForComodification() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  /**
   * @return the keyList
   */
  public final ArrayList<K> keyList() {
    // return keyList.listIterator(); //new keyList();
    return (ArrayList<K>) keyList;
  }

}