//$Id: MatchArea.java,v 1.2 2001/06/18 07:30:41 ctl Exp $

public class MatchArea {

  private int infoBytes = 0;
  private BranchNode root = null;
  public MatchArea( BranchNode aRoot ) {
    if( aRoot == null )
      System.out.println("NULL ROOTT!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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