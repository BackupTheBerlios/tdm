// $Id: ProtoBestMatching.java,v 1.9 2001/03/31 15:32:08 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

//import TreeMatching;
import java.util.*;
import java.io.*;

// This class aims to implement the definition of a best matching
// in a naive manner. It main purpose is prototyping a best matching
// definition

public class ProtoBestMatching  {

  public String name;
  ONode rootA=null, rootB=null;
  protected HashMap nodeMap = new HashMap();

  public AreaNode atRoot = null;

  public ProtoBestMatching( ONode docA, ONode docB, String aName ) {
//    System.out.println("Finding corresponing nodes in the trees...");
    rootA=docA;
    rootB=docB;
    match( docA, docB );
    name = aName;

/*    System.out.println("Building nodemap...");
    mapRoot = buildMapping( null, docB );
*/
  }

  public void matchFromFile( File f ) {
    try{
      BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream(f) ));
      String line = "";
      while( (line = r.readLine()) != null ) {
        if(line.length() == 0)
          continue;
        if(line.startsWith("#") )
          continue;
        int wspos = line.indexOf(";"),wstart=0;
        String branchPath = line.substring(wstart,wspos).trim();
        wstart=wspos+1;
        wspos = line.indexOf(";",wstart);
        int mtype = Integer.parseInt(line.substring(wstart,wspos).trim());
        wstart=wspos+1;
//        wspos = line.indexOf(";",wstart);
        String basePath = line.substring(wstart).trim();
        ONode base = getNode(rootA,basePath);
        ONode target = getNode(rootB,branchPath);
        ONode oldbase = getFirstMapping(target);
        if( oldbase != null ) {
          delMatching(oldbase,target);
          delMatching(target,oldbase);
        }
        addMatching(base,target);
        addMatching(target,base);
        target.matchType = mtype;
      }
    } catch ( Exception e ) {
      System.out.println("Read matching from file excepted");
    }

  }

  private ONode getNode(ONode root, String path ) {
    if( path.length() == 0 )
      return root;
    else {
      return getNode( root.getChild(path.charAt(0)-'1'),path.substring(1) );
    }
  }

  public void match( ONode base, ONode derived ) {
    /*AreaNode */atRoot = new AreaNode(derived);
    buildAreaTree( derived, atRoot, base );
    resolveAmbiguities( atRoot, base, derived );
    nodeMap.clear(); // Free all mappings
    makeMap( atRoot );
    matchSamePosUnmatched( derived, base.getChild(0));
    //
  }

  private void makeMap( AreaNode n ) {
    if( n.getCandidateCount() == 0)
      ; // Unmapped
    else if( n.getCandidateCount() < 2)
      dfsExactMatch( n.getNode(), n.getCandidate(0), true, 0, null,null );
    else {
      System.out.println("makeMap: leaving ambiguity...:" + n.getNode().toString() );
      for(int i=0;i<n.getCandidateCount();i++)
        dfsExactMatch( n.getNode(), n.getCandidate(i), true, 0, null,null );
    }
    for( int i=0;i<n.getChildCount();i++)
      makeMap( n.getChild(i) );
  }


  private void resolveAmbiguities( AreaNode areaTree, ONode baseDoc, ONode destDoc ) {
    // Chop up children in blocks that have the same parent. Assumes that children with
    // same parent are consequitve children of areaTree
    //
    //         +---Area---+
    //         |   n1     |
    //         |  /  \    |
    //         | n2   n3  |
    //         +-/\---|---+
    //          c1 c2 c3
    if( areaTree.getCandidateCount() == 1 ) {
      int startPos = 0, stopPos = -1;
      while( startPos < areaTree.getChildCount() ) {
        ONode parent = areaTree.getChild(startPos).getNode().parent;
        // Scan for block end
        stopPos = startPos;
        for(;(stopPos + 1) < areaTree.getChildCount() &&
          areaTree.getChild(stopPos+1).getNode().parent == parent; stopPos++)
          ;
        resolveAmbiguitiesCommonRoot( areaTree, baseDoc, destDoc, startPos, stopPos );
        fuzzResolveAmbiguitiesCommonRoot( areaTree, baseDoc, destDoc, startPos, stopPos );
        startPos = stopPos + 1;
      }
    } else if ( areaTree.getChildCount() > 0 )
      System.out.println(areaTree.getNode().toString() + " is ambigious, cannot solve immediate children.");
    // Recurse
    for( int i = 0; i<areaTree.getChildCount(); i ++ )
      resolveAmbiguities( areaTree.getChild(i), baseDoc, destDoc );
  }

  private static final TextNode START = new TextNode("start marker");
  private static final TextNode END = new TextNode("end marker");
  private static final TextNode DONT_CARE = new TextNode("don't care marker");

  private void resolveAmbiguitiesCommonRoot( AreaNode areaTree, ONode baseDoc, ONode destDoc,
    int startPos, int stopPos ) {
//    System.out.println("Solving ambiguities under " + areaTree.getNode().toString() + " pos " + startPos + "-" + stopPos );
    // Scan for runs of ambigious children, having at least one candidate with
    // areaTree.getNode() as root
    ONode srcParent =  getFirstMapping(null, areaTree.getChild(startPos).getNode().parent );
    int runStart = startPos, runStop = stopPos;
    while( runStart <= stopPos ) {
      // Scan for block start
      if( areaTree.getChild(runStart).getCandidateCount() <= 1 ) {
        runStart++;
        continue;
      }
      // Possible start, does it have a suitable candidate
      boolean rightParent = false;
      for( int i = 0; !rightParent && i < areaTree.getChild(runStart).getCandidateCount(); i ++ ) {
        //System.out.println("parent = "+  areaTree.getChild(runStart).getCandidate(i).parent+", atmatch=" +srcParent);
       rightParent = areaTree.getChild(runStart).getCandidate(i).parent == srcParent;
      }
      if( !rightParent ) {
        runStart++;
        continue;
      }
      // We have a start! -- now scan for end
      runStop = runStart;
      while( rightParent && (runStop+1) <= stopPos ) {
        if( areaTree.getChild(runStop+1).getCandidateCount() <= 1 )
          break; // end of ambigous run...
        rightParent = false;
        for( int i = 0; !rightParent && i < areaTree.getChild(runStop+1).getCandidateCount(); i ++ )
          rightParent = areaTree.getChild(runStop+1).getCandidate(i).parent == srcParent;
        if( rightParent)
          runStop ++;
      }
      // Run is from runStart to runStop (both inclusive)
      solveAmbiguity( areaTree, runStart, runStop, startPos, stopPos, (ElementNode) srcParent );
      runStart = runStop + 2; // +2 because there must a a marker/dont_care/END symbol to the right of
                              // runStop
    }

  }

  // WARNING: Currently does not handle case
  // src:   AxxBCxxD
  //         \    /
  // dst:     A??D
  // where xx are deleted nodes

  private void solveAmbiguity( AreaNode areaTree, int runStart, int runStop, int startPos, int stopPos,
    ElementNode srcParent ) {
    System.out.println("$$$Solving ambiguity " + runStart  +
        "-" + runStop+ ": " +areaTree.getChild(runStart).toString());
    ONode left=null, right=null;
    if( runStart == startPos )
      left = START;
    else if (areaTree.getChild(runStart-1).getCandidateCount() != 1 )
      left = DONT_CARE; // left of run is ambigous (=not below srcParent) => can't be used for anchoring
    else
      left = areaTree.getChild(runStart-1).getCandidate(0);

    if( runStop == stopPos )
      right = END;
    else if (areaTree.getChild(runStop+1).getCandidateCount() != 1 )
      right = DONT_CARE; // left of run is ambigous (=not below srcParent) => can't be used for anchoring
    else
      right = areaTree.getChild(runStop+1).getCandidate(0);
    if( left == DONT_CARE && right == DONT_CARE ) {
      System.out.println("Both sides DC => leaving unsolved... CAN MAYBE BE SOLVED, THINK!");
      return;
    }
    int start=-1,end=-1, runLength = runStop-runStart; // 0 based: 0 => run length = 1
    if( left == START )
      start = 0;
    else if (left != DONT_CARE )
      start = left.childNo+1;
    if( right == END )
      end = srcParent.children.size()-1;
    else if (right != DONT_CARE )
      end = right.childNo-1;
    if( left == DONT_CARE )
      start = end - runLength;
    else if (right==DONT_CARE)
      end = start + runLength;
    // Check if the ambiguity can be solved
    Vector solved = new  Vector();
    int runPos = runStart;
    for( int pos = start; pos <= end; pos++) {
      int foundIx = -1;
      for( int i = 0;foundIx == -1 && i<areaTree.getChild(runPos).getCandidateCount(); i ++ ) {
        ONode c= areaTree.getChild(runPos).getCandidate(i);
        if( c == srcParent.getChild(pos) ) {
          solved.addElement(c);
          foundIx = i;
        }
      }
      if( foundIx == -1 )
        return; // No match for this one, give up...
      else
        runPos++;
    }
    System.out.println("!!!Solved ambiguity!");
    // Found match for all, lets resolve
    for( int i =0;i<solved.size();i++) {
      areaTree.getChild(runStart+i).clearCandidates();
      areaTree.getChild(runStart+i).addCandidate((ONode) solved.elementAt(i));
    }
  }

  class AreaBottomNode{
    public ONode src=null, dst=null;
    AreaBottomNode( ONode asrc, ONode adst ) {
      src=asrc;dst=adst;
    }
  }

  class MatchNumbers implements Comparable {
    double[] unums =new double[4];
    double[] nums = new double[4];
    final double EPSILON = 1e-09;
    ONode node = null;
    public int compareTo( Object o ) {
      MatchNumbers m = (MatchNumbers) o;
      for( int i=0;i<nums.length;i++) {
        if( Math.abs(nums[i]-m.nums[i]) > EPSILON )
          return nums[i]-m.nums[i] < 0 ? -1:1;
      }
      return 0; // No differences encounted...
    }

    public String toString() {
      return "Nums=[" + unums[0] +","+ unums[1] +","+ unums[2] +","+ unums[3] +"]";
    }
  }

  private MatchNumbers getBestMatch(ONode src, ONode dstTree,MatchNumbers m) {
    MatchNumbers m2 = getMatchNumbers(null,src,dstTree,true);
    m2.node = dstTree;
    if( m2.compareTo(m) < 0 )
      m= m2; // new minimum found
    for(int i=0;i<dstTree.getChildCount();i++) {
      MatchNumbers m3 = getBestMatch( src, dstTree.getChild(i), m);
      if( m3.compareTo(m) < 0 )
        m= m3; // new minimum found
    }
    return m;
  }

  private MatchNumbers getMatchNumbers(AreaNode an, ONode src, ONode dst, boolean useDstChild ) {
    MatchNumbers mn = new MatchNumbers();
    mn.node = dst;
    // Left...
    if( src.childNo > 0 && dst.childNo > 0 ) {
        mn.nums[0] = (new MisCorrelation()).correlate(src.parent.getChild(src.childNo-1),
                                                 dst.parent.getChild(dst.childNo-1) ).getValue();
    } else // Maybe we should give end matches some points, I dunno?
        mn.nums[0] = 1.0;
    // Right ...
    if( src.parent == null ) {
     System.out.println("IMPOSSIBLE: parent is null of " + src.toString() );
     mn.nums[0]=1.0; mn.nums[1]=1.0;mn.nums[2]=1.0;mn.nums[3]=1.0;
      return mn;
    }
    if( src.childNo < src.parent.getChildCount() -1 && dst.childNo < dst.parent.getChildCount() -1 ) {
        mn.nums[1] = (new MisCorrelation()).correlate(src.parent.getChild(src.childNo+1),
                                                 dst.parent.getChild(dst.childNo+1) ).getValue();
    } else // Maybe we should give end matches some points, I dunno?
      mn.nums[1] = 1.0;
    // Top...
    mn.nums[2] = 1.0; // (new MisCorrelation()).correlate(src.parent,dst.parent ).getValue();
    // Children
    if( !useDstChild ) {
      mn.nums[3]=1.0;
      for( int i=0;i<an.bottomNodes.size();i++) {
        AreaBottomNode ab = (AreaBottomNode) an.bottomNodes.elementAt(i);
        double childmatch = childListDist(ab.src,ab.dst);
        mn.nums[3]= childmatch < mn.nums[3] ? childmatch : mn.nums[3];
      }
    } else {
      mn.nums[3] = childListDist(src,dst);
    }
    System.arraycopy(mn.nums,0,mn.unums,0,4);
    Arrays.sort(mn.nums); // Best match (smallest number) first
    return mn;
  }

  private void fuzzResolveAmbiguitiesCommonRoot( AreaNode areaTree, ONode baseDoc, ONode destDoc,
    int startPos, int stopPos ) {
//    System.out.println("Fuzz Solving ambiguities under " + areaTree.getNode().toString() + " pos " + startPos + "-" + stopPos );
    // Scan for runs of ambigious children, having at least one candidate with
    // areaTree.getNode() as root
    ONode srcParent =  getFirstMapping(null, areaTree.getChild(startPos).getNode().parent );
    int runStart = startPos, runStop = stopPos;
    for( runStart = startPos;runStart <= stopPos; runStart++ ) {
      // Scan for ambigous node
      AreaNode ch = areaTree.getChild(runStart);
      if( ch.getCandidateCount() <= 1 ) {
        continue;
      }
      // We have an ambigous node
      // Look-ee up,left,right and down, and see which one has the best ratio
      double leftr=1.0, rightr=1.0, upr=1.0, downr=1.0;
      MatchNumbers[] cands = new MatchNumbers[ ch.getCandidateCount() ];
      for( int i=0;i<ch.getCandidateCount();i++)
        cands[i]=getMatchNumbers( ch, ch.getNode(), ch.getCandidate(i) , false);
      // Sort so that best candidate is in ix 0
      Arrays.sort(cands);
      ch.clearCandidates();
      ch.addCandidate(cands[0].node);
    }

  }


  private void buildAreaTree( ONode dstTreeNode, AreaNode atNode, ONode baseDoc ) {
    HashSet candidates = new HashSet();
    Vector bestCandidates = new Vector();
    double bestCount = 0;
    findCandidates( candidates, dstTreeNode, baseDoc ); // candidates in baseDoc
    if( candidates.isEmpty() ) {
//      System.out.println("!!!!: No exact candidate for " + dstTreeNode.toString());
      // No exact candidates, try fuzzy search
      double minmc = fuzzyFindCandidates( candidates, dstTreeNode, baseDoc, 1e+99 ); // candidates in baseDoc
// FUZZY CAND SEARCH -- currently disabled...
     if( minmc > 0.0 )
        candidates.clear(); // All candidates bad
      else {
        Set c2= candidates;
        candidates = new HashSet();
        for( Iterator i2=c2.iterator();i2.hasNext();) {
          Candidate c = (Candidate) i2.next();
          if( c.mc < 2*minmc ) {
            candidates.add(c.node);
            System.out.println("!!!!: Fuzzy candidate (mc="+ c.mc + "):");// + c.node.toString());
          }
        }
      }
    }

    for( Iterator i = candidates.iterator(); i.hasNext(); ) {
      ONode candidate = (ONode) i.next();
      double thisCount = dfsExactMatch( dstTreeNode, candidate , false, 0, null, null );
      if( thisCount == bestCount )
        bestCandidates.add( candidate );
      else if( thisCount > bestCount ) {
        bestCount = thisCount;
        bestCandidates.clear();
        bestCandidates.add( candidate );
      }
    }
    // Now we should have 0 to n candidate nodes in bestCandidates, all matching exactly
    // bestCount nodes
    // PROBLEM!!!! Matches may not be isomorphic, how should this be solved??
//    System.out.println("bestCandidates are:" + bestCandidates.toString() );
    Vector stopNodes = null;
    if( bestCandidates.size() > 0 ) {
      stopNodes = new Vector();
      atNode.matchCount=bestCount; // Just for info purposes, not used
      for( int i=0;i<bestCandidates.size();i++)
        atNode.addCandidate((ONode) bestCandidates.elementAt(i));
      dfsExactMatch( dstTreeNode, (ONode) bestCandidates.elementAt(0), false, 0, stopNodes, atNode.bottomNodes );
    // KLUDGE!!! Add mappings for unambigious areas at this point, they're needed when running
    // resolveambiguities
    if( bestCandidates.size() == 1)
        dfsExactMatch( dstTreeNode, (ONode) bestCandidates.elementAt(0), true, 0, null, null );

    } else {
      // No matching node in baseDoc
      if( dstTreeNode instanceof ElementNode )
        stopNodes = ((ElementNode) dstTreeNode).children;
    }
    if( stopNodes != null ) {
      //System.out.println("stopNodes are:" +stopNodes.toString() );
      for( int i=0;i<stopNodes.size();i++) {
        AreaNode atChild = new AreaNode((ONode) stopNodes.elementAt(i));
        atNode.addChild( atChild );
        buildAreaTree( (ONode) stopNodes.elementAt(i), atChild, baseDoc );
      }
    } else
      ;//System.out.println("stopNodes are: (null)" );

  }

  //  stopnodes in docA
  // Only recurses if ALL children of docA and docB match
  private double dfsExactMatch(ONode docA, ONode docB, boolean addMatchings, double count,
    Vector stopNodes, Vector bottomNodes ) {
/*DEBUG to get breakpoint   if( docA instanceof ElementNode && ((ElementNode) docA).name.endsWith("text:p") )
      count = count + 0.00001;*/
/*    double matchGoodness = matchGoodness(docA,docB);
    if( !(matchGoodness >0.0) )
      throw new RuntimeException("dfsExactMatch: Assertion failed, docA != docB");
*/
    if( addMatchings ) {
      addMatching( docA,docB );
      addMatching( docB, docA );
    }
    if( !(docA instanceof ElementNode))
      return count+1; //+matchGoodness; // No more matches, but docA and docB matched
    boolean childrenMatch = true;
    Vector children = ((ElementNode) docA).children;
    if( children.size() == ((ElementNode) docB).children.size() ) {
      // Only match children, if there are equally many
      for( int i=0; childrenMatch && i<children.size(); i ++ ) {
        childrenMatch = matches(docA.getChild(i),docB.getChild(i));
        if( !childrenMatch ) {
//          double bestCorrelation = bestCorrelation( docA.getChild(i),
          double mc = (new MisCorrelation()).correlate( (ONode) ((ElementNode) docA).children.elementAt(i),
                                (ONode) ((ElementNode) docB).children.elementAt(i) ).getValue();

//          System.out.println("!!!!: Fuzzmatching (mc=" + mc + "): " + docA.getChild(i).toString() +"," +
//              docB.getChild(i).toString());
          childrenMatch =  mc < 0.2;
          if(childrenMatch)
            System.out.println("!!!!: Fuzzmatched (mc=" + mc + "): " + docA.getChild(i).toString() +"," +
              docB.getChild(i).toString());
        }
      }
    } else
      childrenMatch = false;
    if( !childrenMatch ) {
      // Mark all children as stopnodes
      if( bottomNodes != null )
        bottomNodes.add( new AreaBottomNode( docA, docB ));
      for( int i=0; stopNodes!=null && i<children.size(); i ++ )
        stopNodes.add( children.elementAt(i) );
    } else {
      // All children match
      for( int i=0; i<children.size(); i ++ )
      count += dfsExactMatch( (ONode) ((ElementNode) docA).children.elementAt(i),
                                (ONode) ((ElementNode) docB).children.elementAt(i), addMatchings, 0, stopNodes, bottomNodes );

    }
  return count + 1; // +1 because docA and docB match
  }

  protected double matchGoodness( ONode a, ONode b ) {
    double value = 0.0;
  //  if( (a instanceof TextNode) && (b instanceof TextNode) )
      value= a.contentEquals(b) ? 1.0 : 0.0; // Use some more sophisticated formula later
   /* else if( (a instanceof ElementNode) && (b instanceof ElementNode) ) {
      ElementNode ea = (ElementNode ) a, eb = (ElementNode ) b;
      if( ea.name.equalsIgnoreCase(eb.name) )
        value += 0.5;
      // don't get 0.5 free if 0 attributes match!
      if( ea.attributes != null && ea.attributes.size() > 0 && ea.compareAttributes( ea.attributes, eb.attributes ) )
        value += 0.5;
      //return value;
    }*/
    //if( (a instanceof ElementNode) && ((ElementNode) a).name.startsWith("text"))
    //  System.out.println("Goodness = "+ value +"\na = "+ a.toString() +"\nb=" + b.toString() );
    return value;
  }

  class Candidate {
    double mc=-1;
    ONode node=null;
  }
 // Candidates in treeRoot tree
  protected double fuzzyFindCandidates( HashSet candidates, ONode key, ONode treeRoot, double mmc ) {
    // Threshold should probably depend on distance from previous match etc.
    MisCorrelation mc = new MisCorrelation();
    mc.correlate(key,treeRoot);
    double cval =  mc.getValue();
    if( cval < 1.0 ) { // 1.0 Magic correlation for candidates
      Candidate c = new Candidate();
      c.mc=cval;
      c.node=treeRoot;
      candidates.add(c);
      if( cval < mmc )
        mmc = cval;
    }

    if( treeRoot instanceof ElementNode ) {
      Vector children = ((ElementNode) treeRoot).children;
      for(int i=0;i<children.size();i++) {
        double thatmin = fuzzyFindCandidates(candidates, key, (ONode) children.elementAt(i), mmc );
        mmc = thatmin < mmc ? thatmin : mmc;
      }
    }
    return mmc;
  }


  // A very simple heuristic - if there exists a pair of unmatched nodes with the
  // same address, they're matched
  public void matchSamePosUnmatched( ONode a, ONode rootB) {
    if( getFirstMapping(null,a) == null ) {
      // Unmapped...
      System.out.println("Umapped="+ a.toString());
      /*
      AreaNode senitel = new AreaNode(a);
      if( a instanceof ElementNode ) {
        senitel.bottomNodes.addAll( new AreaBottomNode( a,  )
      }*/
      MatchNumbers m = new MatchNumbers();
      m.nums[0]=1.0;  m.nums[1]=1.0;m.nums[2]=1.0;m.nums[3]=1.0;
      m=getBestMatch(a,rootB,m);
      if( m.nums[0] < 0.2 ) { // Threshold val when insert = match
        System.out.println("Fuzzed with :"+ m.node.toString());
        addMatching( a, m.node );
        addMatching( m.node, a );
      }
/*
      Vector p = TreePath.buildNodePath(a);
      ONode b = rootB;
      try {
        for(int i=1;i<p.size();i++)
          b = (ONode) ((ElementNode) b).children.elementAt( ((ONode) p.elementAt(i)).childNo );
      } catch ( Exception e ) {
        b = null;
      }
      if( b!=null && getFirstMapping(null,b) == null ) {
         addMatching( a,b );
         addMatching( b,a );
      }
  */
    }
    if( a instanceof ElementNode ) {
      // process children
      for( int i=0;i<((ElementNode) a).children.size();i++)
        matchSamePosUnmatched( (ONode) ((ElementNode) a).children.elementAt(i) , rootB);
    }
  }


  private Iterator nodeIter = null;

  // Wrapper for getFirstMapping(doc,src), which has unnecessary arg doc!
  public ONode getFirstMapping(  ONode src ) {
    return getFirstMapping( null, src );
  }

  public ONode getFirstMapping( ONode doc, ONode src ) {
    Object mapsto = nodeMap.get(src);
    nodeIter = null;
    if ( mapsto == null )
      return null;
    else if ( mapsto instanceof ONode ) {
      return (ONode) mapsto;
    } else {
      // maps to several
      nodeIter = ((Set) mapsto).iterator();
      if( nodeIter.hasNext() )
        return (ONode) nodeIter.next();
      else
        return null;
    }
  }

  public ONode getNextMapping() {
    if( nodeIter == null || !nodeIter.hasNext() )
      return null;
    else
      return (ONode) nodeIter.next();
  }


  protected void delMatching( ONode a, ONode b ) {
    if( !nodeMap.containsKey(a) )
      return; // No mapping
    else {
      Object match = nodeMap.get(a);
      if( match instanceof Set ) {
        ((Set) match).remove(b);
        if( ((Set) match).isEmpty() )
          nodeMap.remove(a);
      } else {
          nodeMap.remove(a);
      }
    }
  }


  // Adds matching a->b; usually called both ways to add b->a as well
  protected void addMatching( ONode a, ONode b ) {
    if( !nodeMap.containsKey(a) )
      nodeMap.put(a,b);
    else {
      // Uh-uh, already a match; we need to add it to a set/make a new set
      Object match = nodeMap.get(a);
      if( match instanceof Set )
        ((Set) match).add(b);
      else {
        // Purge the node, and put a fresh set with the two nodes
        Set nodes = new HashSet();
        nodes.add(match);
        nodes.add(b);
        nodeMap.remove(a);
        nodeMap.put(a,nodes);
      }
    }
  }

 // Candidates in treeRoot tree
  protected void findCandidates( HashSet candidates, ONode key, ONode treeRoot ) {

    if( key.contentEquals(treeRoot) )
      candidates.add(treeRoot);
    // Process children
    if( treeRoot instanceof ElementNode ) {
      Vector children = ((ElementNode) treeRoot).children;
      for(int i=0;i<children.size();i++)
        findCandidates(candidates, key, (ONode) children.elementAt(i) );
    }
  }

  protected boolean matches( ONode a, ONode b ) {
    return a.contentEquals(b);
  }

  class MisCorrelation {
    int mismatched=0;
    int total=0;
    static final int C= 10; // Correlation damping value (default no of mismatched bytes)
                            // A perfect match of C bytes gets correlation 0.5
                            // as matches ->perfect, miscorrelation -> 0
    public MisCorrelation correlate( ONode a, ONode b ) {
      if( ( a instanceof TextNode && b instanceof ElementNode ) ||
          ( b instanceof TextNode && a instanceof ElementNode ) ) {
          total=1;
          mismatched=1; // Totallly different
          return this;
      }
      if( a instanceof TextNode ) {
        TextNode ta = (TextNode) a,tb=(TextNode) b;
        total+=Math.max( ta.text.length(), tb.text.length() );
        mismatched += stringDistance( ta.text, tb.text );
      } else {
        // Assume elementNode
        ElementNode ea = (ElementNode) a,eb=(ElementNode) b;
        total+=1;
        mismatched += ea.name.equals(eb.name) ? 0 : 1; // Tag is 2 bytes of info
        for( Iterator i=ea.attributes.keySet().iterator();i.hasNext();) {
          String aname = (String) i.next();
          if( eb.attributes.containsKey(aname) ) {
            String v1 = (String) ea.attributes.get(aname), v2 = (String)  eb.attributes.get(aname);
            int maxvallen =  v1.length() > v2.length() ? v1.length() : v2.length();
            if( maxvallen > 5 ) {
              total+=maxvallen;
              mismatched += stringDistance(v1,v2);
            } else {
              mismatched += v1.equals(v2) ? 0 : 2;
              total+=2;
            }
          } else {
            total += 2; // A missing attribute costs 2 bytes
            mismatched +=2;
          }
        }
        // Penalties for missing attributes in ea
        for( Iterator i=eb.attributes.keySet().iterator();i.hasNext();) {
          String aname = (String) i.next();
          if( !ea.attributes.containsKey(aname) ) {
              mismatched += 2;
              total+=2;
           }
        }
      } // Elementnode
      return this;
    }

    void addMC( MisCorrelation b ) {
      total += b.total;
      mismatched += b.mismatched;
    }

    double getValue() {
      double penalty = Math.max(0.0,1.0-((double) total)/((double) C));
      return penalty+(1.0-penalty)*((double) (mismatched))/((double) total);
    }
  }

  public void printCorr( ONode a, ONode b) {
    MisCorrelation m = new MisCorrelation();
    m.correlate(a,b);
    System.out.println("MC= " + m.getValue() );
    System.out.println("MNs= " + getMatchNumbers(null,a,b,true).toString() );
  }

  //
  // String similarity code...Very adhoc just for testing
  //

  private int stringDistance( String s1,String s2) {
    if( s1 == s2 )
      return 0;
    if( s1 == null || s1.length() == 0)
      return s2.length();
    if( s2 == null || s2.length() == 0)
      return s1.length();
    if( s2.equals(s1) )
      return 0;
    if( s1.length() > s2.length() ) {
      String t= s1; s1=s2;s2=t;
    }
    int distance = 0;
    int s1len =s1.length();
    for(int i=0;i<s1len;i+=4) {
     String chunk = s1.substring(i,((i+4) > s1len ? s1len : i+4) );
      if( s2.indexOf(chunk) == -1 )
        distance +=chunk.length();
    }
//    System.out.println("Distance is " + distance  );
//    System.out.println("Lengths " + s2.length() + " and " + s1.length() );
    return distance + s2.length()-s1.length();
  }

  // 1.0 total mismatch
  // % of nodes below a not found below b

  private double childListDist( ONode a, ONode b) {
    // Make child lists representing children on a & b
    StringBuffer ac = new StringBuffer();
    StringBuffer bc = new StringBuffer();
    for( int i=0;i<a.getChildCount();i++) {
      int hash = a.getChild(i).toString().hashCode();
//      ac.append((char) (hash >> 16));
      ac.append((char) (hash & 0xffff));
    }
    for( int i=0;i<b.getChildCount();i++) {
      int hash = b.getChild(i).toString().hashCode();
//      bc.append((char) (hash >> 16));
      bc.append((char) (hash & 0xffff));
    }
    String as=ac.toString(), bs=bc.toString();
    int matches=0;
    if( as.length()==0)
      return 1.0;
    else if( as.length()==1)
      return bs.indexOf(as) == -1 ? 1.0 : 0.0;
    for( int i=0;i<as.length()-1;i++) {
      if( bs.indexOf(as.substring(0,i+2)) != -1 )
        matches++;
    }
    return 1.0 - ((double) matches)/((double) as.length()-1);
  }

}