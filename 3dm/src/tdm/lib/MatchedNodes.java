// $Id: MatchedNodes.java,v 1.6 2001/09/05 13:21:26 ctl Exp $ D

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/** Container for a set of nodes, matched to the node owning the container. */

public class MatchedNodes {

  private BaseNode owner=null;
  private Set matches=new HashSet();

  /** Create a new conatiner of matched nodes. All nodes in the container are
   *  matched to the owner node.
   *  @param aowner Owner of the container.
   */
  public MatchedNodes(BaseNode aowner) {
    owner = aowner;
  }

  public void addMatch(BranchNode n) {
    matches.add(n);
  }

  public void delMatch(BranchNode n) {
    matches.remove(n);
  }

  public Set getMatches() {
    return matches;
  }

  public int getMatchCount() {
    return matches.size();
  }

  /** Get the first node that is fully matched to the owner. */

  public BranchNode getFullMatch() {
    for( Iterator i=matches.iterator();i.hasNext();) {
      BranchNode fmatch = (BranchNode) i.next();
      if( fmatch.isMatch(BranchNode.MATCH_FULL))
        return fmatch;
    }
    return null;
  }
//$CUT

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    java.util.Iterator i = matches.iterator();
    while( i.hasNext() ) {
      XMLNode n = ((Node) i.next()).content;
      pw.println(ind+ (n  == null ? "(null)" : n.toString() ) );
    }
  }
//$CUT
}