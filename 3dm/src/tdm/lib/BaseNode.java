// $Id: BaseNode.java,v 1.6 2001/06/20 13:25:58 ctl Exp $

public class BaseNode extends Node {

  // Left and right matches
  MatchedNodes left=null;
  MatchedNodes right=null;

  public BaseNode( /*Node aParent, int achildPos,*/ XMLNode aContent ) {
    super();// aParent,achildPos);
    left = new MatchedNodes(this);
    right = new MatchedNodes(this);
    content = aContent;
  }

/*
  public boolean isMatched() {
    return !left.getMatches().isEmpty() || !right.getMatches().isEmpty();
  }
*/

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

  public void swapLeftRightMatchings() {
    MatchedNodes t = left;
    left=right;
    right=t;
  }

}