// $Id: BaseNode.java,v 1.7 2001/09/05 13:21:24 ctl Exp $ D

/**
 *  Node in a base tree matched witch is matched to two branches. In addition to
 *  the functionality provided by the node class, BaseNode adds matchings. Each
 *  BaseNode can be matched to multipleBranchNodes. Matches to the node in the
 *  left and right branches are accesed with the {@link #getLeft() getLeft} and
 *  {@link #getRight() getRight} methods.
 */

public class BaseNode extends Node {

  // Left and right matches
  private MatchedNodes left=null;
  private MatchedNodes right=null;

  public BaseNode( XMLNode aContent ) {
    super();
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

  public void swapLeftRightMatchings() {
    MatchedNodes t = left;
    left=right;
    right=t;
  }

}