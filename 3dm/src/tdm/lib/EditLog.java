// $Id: EditLog.java,v 1.1 2001/06/08 08:40:38 ctl Exp $

public class EditLog {

  private PathTracker pt = null;
  public EditLog(PathTracker apt) {
    pt = apt;
  }

  public void insert( BranchNode n, int childPos ) {
    System.out.println("INSERT; " + pt.getPathString(childPos));
    System.out.println(n.getContent().toString());
  }

  public void move( BaseNode n, int childPos ) {
    System.out.println("MOVE: " + PathTracker.getPathString(n)+"->" + pt.getPathString(childPos));
  }

  public void copy( BaseNode n, int childPos ) {
    System.out.println("COPY: " + PathTracker.getPathString(n)+"->" + pt.getPathString(childPos));
  }

  public void update( BranchNode n ) {
    System.out.println("UPDATE: " + pt.getFullPathString());
    System.out.println(n.getContent().toString());
  }

  public void delete( BaseNode n ) {
    System.out.println("DELETE: " + PathTracker.getPathString(n));
  }

  public void checkPoint() {
  }
  public void rewind() {
  }
  public void commit() {
  }

}