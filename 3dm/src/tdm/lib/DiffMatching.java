// $Id: DiffMatching.java,v 1.3 2001/09/05 13:21:25 ctl Exp $ D

import Matching;
import java.util.Vector;
import java.util.Iterator;

/** Tree matching suitable for producing diffs. Compared to the standard
 *  matching, DiffMatching does not match nodes with similar content (there's
 *  no point to that), and tries to find matches that form uninterrupted runs
 *  of src attributes => more efficient encoding of the diff.
 */

public class DiffMatching extends Matching {

  /** Construct a matching between a base and branch tree. */
  public DiffMatching(BaseNode abase, BranchNode abranch ) {
    super( abase, abranch );
  }

  protected void buildMatching( BaseNode base, BranchNode branch ) {
    matchSubtrees( base, branch );
  }

  // We never match fuzzy when diffing
  protected boolean dfsTryFuzzyMatch( Node a, Node b) {
    return false;
  }

  // Only find exact candidates
  protected Vector findCandidates( BaseNode tree, BranchNode key ) {
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    return candidates;
  }

  protected CandidateEntry getBestCandidate(  BranchNode branch,
                                        Vector bestCandidates ) {
    // Try to return a node who is next to the previously matched node
    // (sequencing of src nodes!)
    if( bestCandidates.size() > 1 ) {
      for( Iterator i = bestCandidates.iterator();i.hasNext();) {
        CandidateEntry ce = (CandidateEntry) i.next();
        BranchNode left = (BranchNode) branch.getLeftSibling();
        BaseNode cand = ce.candidate;
        if( left != null && left.hasBaseMatch() && left.getBaseMatch() ==
            cand.getLeftSibling() )
          return ce;
      }
    }
    if( bestCandidates.isEmpty() )
      return null;
    else
      return (CandidateEntry) bestCandidates.elementAt(0);
  }
}
