// $Id: TriMatching.java,v 1.8 2001/04/26 17:27:16 ctl Exp $

public class TriMatching extends Matching {

  private BranchNode leftRoot = null;
  private BranchNode rightRoot = null;

  public TriMatching( BranchNode left, BaseNode base, BranchNode right ) {
    super( base, right );
    leftRoot =left;
    rightRoot = right;
    swapLeftRight( base );
    buildMatching( base, left );
    setPartners( left, false );
    setPartners( right, true );
  }

  private void swapLeftRight( BaseNode base ) {
    base.swapLeftRightMatchings();
    for( int i=0;i<base.getChildCount();i++)
      swapLeftRight(base.getChild(i));
  }

  private void setPartners( BranchNode n, boolean partnerInLeft ) {
    BaseNode baseMatch = n.getBaseMatch();
    if( baseMatch != null ) {
      if( partnerInLeft )
        n.setPartners(baseMatch.getLeft());
      else
        n.setPartners(baseMatch.getRight());
    }
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