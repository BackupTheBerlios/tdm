// $Id: Node.java,v 1.9 2001/09/05 13:21:26 ctl Exp $ D

import java.util.Vector;

/** Node in the parse tree. Each node in the parse trees has 0-n children,
 * content and a tag to identify nodes in the same matching subtrees (the
 * <code>matchArea</code> field). In addition, all nodes except the root
 * have a parent.
 */

public abstract class Node {

  protected Vector children = new Vector();
  protected XMLNode content = null;
  protected Node parent = null;
  protected int childPos=-1; // zero-based, i.e. first child = 0
  protected MatchArea area = null;

  public Node() {
    parent = null;
    childPos = -1;
  }

  public void addChild( Node n) {
    n.parent=this;
    n.childPos=children.size();
    children.add(n);
  }

  public Node getParentAsNode() {
    return parent;
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
//$CUT

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    pw.println( ind + content );
  }

  public void debugTree( java.io.PrintWriter pw, int indent ) {
    debug(pw,indent );
    for( int i=0;i<getChildCount();i++)
      ((Node) children.elementAt(i)).debugTree(pw,indent+1);
  }
//$CUT
}