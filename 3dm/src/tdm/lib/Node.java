// $Id: Node.java,v 1.4 2001/04/20 14:47:50 ctl Exp $

import java.util.Vector;

public abstract class Node {

  protected Vector children = new Vector();
  protected XMLNode content = null;
  protected Node parent = null;
  protected int childPos=-1; // zero-based, i.e. first child = 0

  public Node( Node aParent, int achildPos ) {
    parent = aParent;
    childPos = achildPos;
  }

  public void addChild( Node n) {
    children.add(n);
  }

  public int getChildCount() {
    return children.size();
  }

  public Node getChildAsNode(int ix) {
    return (Node) children.elementAt(ix);
  }

/* Hopefully not needed
  public void setContent( XMLNode n ) {
    content = n;
  }
*/
  public XMLNode getContent( ) {
    return content;
  }


  public int getChildPos() {
    return childPos;
  }

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    pw.println( ind + content );
  }

  public void debugTree( java.io.PrintWriter pw, int indent ) {
    debug(pw,indent );
    for( int i=0;i<getChildCount();i++)
      ((Node) children.elementAt(i)).debugTree(pw,indent+1);
  }
}