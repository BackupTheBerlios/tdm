// $Id: ConflictLog.java,v 1.1 2001/05/25 07:01:24 ctl Exp $

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public class ConflictLog {

  static final char PATHSEP='/';

  static final int UPDATE = 1;
  static final int DELETE = 2;
  static final int INSERT = 3;
  static final int MOVE = 4;

  private LinkedList path = null;
  private int childPos = -1;
  public ConflictLog() {
    resetContext();
  }

  public void add( int type, BranchNode conflictingNode, String text ) {
    System.out.println("CONFLICT:"+text);
    System.out.println("path="+getPath());
  }

  public void addWarning( int type, BranchNode conflictingNode, String text ) {
    System.out.println("CONFLICTW:"+text);
    System.out.println("path="+getPath());
  }

  private String getPath() {
    StringBuffer p = new StringBuffer();
    for( Iterator i=path.iterator();i.hasNext();) {
      p.append(((Integer) i.next()).toString());
      p.append(PATHSEP);
    }
    p.append(childPos);
    return p.toString();
  }

  public void resetContext() {
    path = new LinkedList();
    childPos = 0;
  }

  public void nextChild() {
    childPos++;
  }

  public void enterSubtree() {
    path.addLast(new Integer(childPos));
    childPos = 0;
  }

  public void exitSubtree() {
    Integer oldpos = (Integer) path.removeLast();
    childPos = oldpos.intValue();
  }


}