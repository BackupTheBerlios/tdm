// $Id: PathTracker.java,v 1.1 2001/06/08 08:40:38 ctl Exp $

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collections;

public class PathTracker {

  static final char PATHSEP='/';

  private LinkedList path = null;
  private int childPos = -1;

  public PathTracker() {
    resetContext();
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

  public String getPathString() {
    return getPathString(path,-1,false);
  }

  public String getFullPathString() {
    return getPathString(path,childPos,true);
  }

  public String getPathString(int achildPos) {
    return getPathString(path,achildPos,true);
  }

  public static String getPathString(Node n ) {
    return getPathString(makePath(n),-1,false);
  }

  public static String getPathString(Node n,int achildPos) {
    return getPathString(makePath(n),achildPos,true);
  }

  private static String getPathString( LinkedList path, int childPos, boolean useChildPos ) {
    StringBuffer p = new StringBuffer();
    Iterator i=path.iterator(); // Skip artificial root node
    i.next();
    for(;i.hasNext();) {
      p.append(PATHSEP);
      p.append(((Integer) i.next()).toString());
    }
    if( useChildPos ) {
      p.append(PATHSEP);
      p.append(childPos);
    }
    return p.toString();
  }

  private static LinkedList makePath( Node n ) {
    LinkedList path = new LinkedList();
    do {
      path.addLast(new Integer(n.getChildPos()));
    } while( (n = n.getParentAsNode()) != null);
//    path.removeLast(); // We don't want the artificial root node in the path
    Collections.reverse(path);
    return path;
  }

}