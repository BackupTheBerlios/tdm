// $Id

import java.util.Iterator;

public class BranchNode extends Node {

  public static final int MATCH_FULL = 3;
  public static final int MATCH_CONTENT = 1;
  public static final int MATCH_CHILDREN = 2;

  private MatchedNodes partners = null;
  private BaseNode baseMatch = null;
  private int matchType = 0;


  public BranchNode( Node aParent, int achildPos, XMLNode aContent ) {
    super( aParent,achildPos);
    content = aContent;
  }


  public BranchNode getChild( int ix ) {
    return (BranchNode) children.elementAt(ix);
  }

  public BranchNode getParent() {
    return (BranchNode) parent;
  }

  // Possibly not needed in final version!
  public void setPartners(MatchedNodes p) {
    partners = p;
  }

  public MatchedNodes getPartners() {
    return partners;
  }

  // Possibly not needed in final version! (move to constructor?)
  public void setBaseMatch(BaseNode p, int amatchType) {
    if( amatchType < MATCH_CONTENT || amatchType > MATCH_FULL )
      throw new IllegalArgumentException();
    baseMatch = p;
    matchType = amatchType;
  }

  public void delBaseMatch() {
    baseMatch = null;
    matchType = 0;
  }
  public int getBaseMatchType() {
    return matchType;
  }

  public BaseNode getBaseMatch() {
    return baseMatch;
  }

  // Tells if this node is in the left tree.
  // notice the recursive structure, enabling us to tell this even if the node is
  // unmatched. Assumes that at least the root is matched.
  public boolean isLeftTree() {
    if( baseMatch != null )
      return baseMatch.getLeft().getMatches().contains(this);
    else
      return getParent().isLeftTree();
  }

  public boolean isMatch( int type) {
    return ((matchType & type) != 0);
  }

  // Remeber to check both steps! The canidate's match type is only from base
  // if A should match B structurally we need
  // A---------Base---------B
  //    struct      struct

  public BranchNode getFirstPartner( int typeFlags ) {
    if( ( matchType & typeFlags) == 0 )
      return null;
    MatchedNodes m= getPartners();
    if( m == null )
      return null;
    for( Iterator i = m.getMatches().iterator();i.hasNext();) {
      BranchNode candidate = (BranchNode) i.next();
      if((candidate.matchType & typeFlags) != 0)
        return candidate;
    }
    return null;
  }
  public void debug( java.io.PrintWriter pw, int indent ) {
    super.debug(pw, indent);
    String ind = "                                                   ".substring(0,indent+1);
    pw.println(ind+(partners != null ? "Partners are:" : "(no partners)"));
    if(partners != null ) {
      partners.debug(pw,indent+1);
      pw.println(ind+"---");
    }
  }

}