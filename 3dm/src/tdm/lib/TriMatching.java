// $Id: TriMatching.java,v 1.11 2001/09/05 21:22:30 ctl Exp $ D

/** Matching between a base and two branch trees. */
public class TriMatching extends Matching {

  private BranchNode leftRoot = null;
  private BranchNode rightRoot = null;

  /** Create matching */
  public TriMatching( BranchNode left, BaseNode base, BranchNode right ) {
    super( base, right );
    leftRoot =left;
    rightRoot = right;
    swapLeftRight( base );
    buildMatching( base, left );
    setPartners( left, false );
    setPartners( right, true );
  }


  // Swap left and right matching fields in base nodes. The superclass
  // always fills in left matchings, so we need to call this when making
  // the right (no pun intended) matchings
  protected void swapLeftRight( BaseNode base ) {
    base.swapLeftRightMatchings();
    for( int i=0;i<base.getChildCount();i++)
      swapLeftRight(base.getChild(i));
  }

  // Set partner fields of branch nodes
  protected void setPartners( BranchNode n, boolean partnerInLeft ) {
    BaseNode baseMatch = n.getBaseMatch();
    if( baseMatch != null ) {
      if( partnerInLeft )
        n.setPartners(baseMatch.getLeft());
      else
        n.setPartners(baseMatch.getRight());
    } else
        n.setPartners(null);
    for( int i=0;i<n.getChildCount();i++)
      setPartners(n.getChild(i),partnerInLeft);
  }

  public BranchNode getLeftRoot() {
    return leftRoot;
  }

  public BranchNode getRightRoot() {
    return rightRoot;
  }
}