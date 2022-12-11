package org.ds2os.vsl.sphinx.model;

/**
 * This enum gives the type of operation. Other is not a clear type. Maybe a
 * type should be created for the registration of subscription.
 *
 * @author francois
 */
public enum OperationType {

  /**
   *
   */
  READ, WRITE, SUBSCRIBE, UNSUBSCRIBE, NOTIFYCALLBACK, REGISTERVIRTUALNODE,
  /**
   *
   */
  UNREGISTERVIRTUALNODE, LOCKSUBTREE, COMMITSUBTREE, ROLLBACKSUBTREE, REGISTERSERVICE, OTHER;


  /*
   * These are the binary array describing each of the operation types.
   */
  /** */
  private static final double[] READBINARY = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  /** */
  private static final double[] WRITEBINARY = {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  /** */
  private static final double[] SUBSCRIBEBINARY = {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0};
  /** */
  private static final double[] UNSUBSCRIBEBINARY = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  /** */
  private static final double[] NOTIFYCALLBACKBINARY = {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
  /** */
  private static final double[] REGISTERVIRSTUALNODEBINARY = {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0};
  /** */
  private static final double[] UNREGISTERVIRSTUALNODEBINARY = {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0};
  /** */
  private static final double[] LOCKSUBTREEBINARY = {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  /** */
  private static final double[] COMMITSUBTREEBINARY = {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0};
  /** */
  private static final double[] ROLLBACKSUBTREEBINARY = {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0};
  /** */
  private static final double[] REGISTERSERVICEBINARY = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0};
  /** */
  private static final double[] OTHERBINARY = {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0};

  /**
   * Over types are a simplification of the different types in just the 4 main
   * types.
   */
  public static final boolean USEOVERTYPE = true;

  /** */
  private static final double[] READOVERBINARY = {1, 0, 0, 0};
  /** */
  private static final double[] WRITEOVERBINARY = {0, 1, 0, 0};
  /** */
  private static final double[] LOCKSUBTREEOVERBINARY = {0, 0, 1, 0};
  /** */
  private static final double[] OTHEROVERBINARY = {0, 0, 0, 1};

  /** */
  public static final int DIMENSION = USEOVERTYPE ? 4 : 12;
  // public final static int dimension = 12;

  /**
   * parsen.
   * @param operation operation
   * @return OperationType
   */
  public static OperationType parse(final String operation) {

    if (operation.equals("read")) {
      return READ;
    }
    if (operation.equals("write")) {
      return WRITE;
    }
    if (operation.equals("subscribe")) {
      return SUBSCRIBE;
    }
    if (operation.equals("unsubscribe")) {
      return UNSUBSCRIBE;
    }
    if (operation.equals("notifyCallback")) {
      return NOTIFYCALLBACK;
    }
    if (operation.equals("registerVirtualNode")) {
      return REGISTERVIRTUALNODE;
    }
    if (operation.equals("unregisterVirtualNode")) {
      return UNREGISTERVIRTUALNODE;
    }
    if (operation.equals("lockSubtree")) {
      return LOCKSUBTREE;
    }
    if (operation.equals("commitSubtree")) {
      return COMMITSUBTREE;
    }
    if (operation.equals("rollbackSubtree")) {
      return ROLLBACKSUBTREE;
    }
    if (operation.equals("registerService")) {
      return REGISTERSERVICE;
    }

    return OTHER;

  }

  /**
   * getOverType.
   * @return OperationType
   */
  public OperationType getOverType() {
    switch (this) {
      case READ:
        return READ;
      case SUBSCRIBE:
        return READ;
      case UNSUBSCRIBE:
        return READ;
      case NOTIFYCALLBACK:
        return READ;
      case WRITE:
        return WRITE;
      case REGISTERVIRTUALNODE:
        return WRITE;
      case UNREGISTERVIRTUALNODE:
        return WRITE;
      case LOCKSUBTREE:
        return LOCKSUBTREE;
      case COMMITSUBTREE:
        return LOCKSUBTREE;
      case ROLLBACKSUBTREE:
        return LOCKSUBTREE;
      case REGISTERSERVICE:
        return WRITE;
      default:
        return OTHER;
    }
  }

  /**
   * binarization.
   * @return double[]
   */
  public double[] binarization() {
    OperationType toUse = USEOVERTYPE ? this.getOverType() : this;

    if (USEOVERTYPE) {
      switch (toUse) {
        case READ:
          return READOVERBINARY;
        case WRITE:
          return WRITEOVERBINARY;
        case LOCKSUBTREE:
          return LOCKSUBTREEOVERBINARY;
        case OTHER:
          return OTHEROVERBINARY;
        default:
          break;
      }
    }

    switch (toUse) {
      case READ:
        return READBINARY;
      case SUBSCRIBE:
        return SUBSCRIBEBINARY;
      case UNSUBSCRIBE:
        return UNSUBSCRIBEBINARY;
      case NOTIFYCALLBACK:
        return NOTIFYCALLBACKBINARY;
      case WRITE:
        return WRITEBINARY;
      case REGISTERVIRTUALNODE:
        return REGISTERVIRSTUALNODEBINARY;
      case UNREGISTERVIRTUALNODE:
        return UNREGISTERVIRSTUALNODEBINARY;
      case LOCKSUBTREE:
        return LOCKSUBTREEBINARY;
      case COMMITSUBTREE:
        return COMMITSUBTREEBINARY;
      case ROLLBACKSUBTREE:
        return ROLLBACKSUBTREEBINARY;
      case REGISTERSERVICE:
        return REGISTERSERVICEBINARY;
      default:
        return OTHERBINARY;
    }
  }

}
