// $Id: BaseNode.java,v 1.5 2001/04/27 16:59:08 ctl Exp $

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

  public boolean isMatched() {
    return !left.getMatches().isEmpty() || !left.getMatches().isEmpty();
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

  public void swapLeftRightMatchings() {
    MatchedNodes t = left;
    left=right;
    right=t;
  }

}