// $Id: ONode.java,v 1.1 2001/03/14 08:23:54 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

import java.io.PrintWriter;

public abstract class ONode implements Cloneable {
  public ONode parent=null;
  public ProtoBestMatching matchTag = null;
  public int childNo=-1; // Child number
  //  abstract public String toString();

  public Object clone() {
    Object o = null;
    try {  o= super.clone(); } catch (CloneNotSupportedException e) {}
    return o;
  }

  public void printTree(int level) {
    System.out.println("                                               ".substring(0,2*level)+toString());
  }

  public int getChildCount() {
    return 0;
  }

  public ONode getChild( int ix ) {
    throw new RuntimeException("Node.getChild(): the node type has no children!");
  }

  public abstract boolean contentEquals( Object a );
  /* {
      return a == this;
  }*/
  public void printXML1( PrintWriter pw, int level, boolean assumeNoChildren ) {}
  public void printXML2( PrintWriter pw, int level ) {}

}



