// $Id: Node.java,v 1.7 2001/04/27 16:59:10 ctl Exp $

import java.util.Vector;

public abstract class Node {

  protected Vector children = new Vector();
  protected XMLNode content = null;
  protected Node parent = null;
  protected int childPos=-1; // zero-based, i.e. first child = 0
  protected MatchArea area = null;

  public Node( /*Node aParent, int achildPos */) {
    parent = null; //aParent;
    childPos = -1; //achildPos;
  }

  public void addChild( Node n) {
    n.parent=this;
    n.childPos=children.size();
    children.add(n);
  }

  public int getChildCount() {
    return children.size();
  }

  public Node getChildAsNode(int ix) {
    return (Node) children.elementAt(ix);
  }

  public boolean hasLeftSibling() {
    return childPos > 0;
  }

  public boolean hasRightSibling() {
    return parent != null && childPos < parent.children.size()-1;
  }


  public Node getLeftSibling() {
    if( parent == null || childPos == 0 )
      return null;
    else
      return parent.getChildAsNode(childPos-1);
  }

  public Node getRightSibling() {
    if( parent == null || childPos == parent.children.size() -1 )
      return null;
    else
      return parent.getChildAsNode(childPos+1);
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

  public MatchArea getMatchArea() {
    return area;
  }

  public void setMatchArea(MatchArea anArea) {
    area=anArea;
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