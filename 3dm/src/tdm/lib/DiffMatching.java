// $Id: DiffMatching.java,v 1.1 2001/04/27 16:59:09 ctl Exp $

import Matching;
import java.util.Vector;
import java.util.Iterator;


public class DiffMatching extends Matching {

  public DiffMatching(BaseNode abase, BranchNode abranch ) {
    super( abase, abranch );
  }

  protected void buildMatching( BaseNode base, BranchNode branch ) {
    match( base, branch );
  }

  // We never match fuzzy when diffing
  protected boolean dfsTryFuzzyMatch( Node a, Node b) {
    return false;
  }

  // Only finde exact here too...
  protected Vector findCandidates( BaseNode tree, BranchNode key ) {
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    return candidates;
  }

  protected CandidateEntry getBestCandidate( BaseNode base, BranchNode branch,
    Vector bestCandidates ) {
    // Try to return a node who is next to the previously matched node
    // (better sequenceing of bfs!)
    if( bestCandidates.size() > 1 ) {
      for( Iterator i = bestCandidates.iterator();i.hasNext();) {
        CandidateEntry ce = (CandidateEntry) i.next();
        BranchNode left = (BranchNode) branch.getLeftSibling();
        BaseNode cand = ce.candidate;
        if( left != null && left.hasBaseMatch() && left.getBaseMatch() == cand.getLeftSibling() )
          return ce;
      }
    }
    if( bestCandidates.isEmpty() )
      return null;
    else
      return (CandidateEntry) bestCandidates.elementAt(0);
  }

}
