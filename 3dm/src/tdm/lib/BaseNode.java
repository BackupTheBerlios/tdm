// $Id: BaseNode.java,v 1.2 2001/03/14 14:03:43 ctl Exp $

public class BaseNode extends Node {

  // Left and right matches
  MatchedNodes left=null;
  MatchedNodes right=null;

  public BaseNode( Node aParent, int achildPos, XMLNode aContent ) {
    super( aParent,achildPos);
    left = new MatchedNodes(this);
    right = new MatchedNodes(this);
    content = aContent;
  }

  public BaseNode getChild( int ix ) {
    return (BaseNode) children.elementAt(ix);
  }

  public BaseNode getParent() {
    return (BaseNode) parent;
  }

  public MatchedNodes getLeft() {
    return left;
  }

  public MatchedNodes getRight() {
    return right;
  }

}