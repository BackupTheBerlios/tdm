//$Id: MatchArea.java,v 1.3 2001/09/05 13:21:26 ctl Exp $ D

/** Class used to tag nodes in the same matched subtree. The class also
 *  contains fields for the root and information size of the subtree. */

public class MatchArea {

  private int infoBytes = 0;
  private BranchNode root = null;

  public MatchArea( BranchNode aRoot ) {
    root = aRoot;
  }

  public BranchNode getRoot() {
    return root;
  }

  public void addInfoBytes( int i) {
    infoBytes+=i;
  }

  public int getInfoBytes() {
    return infoBytes;
  }
}