// $Id: TriMatching.java,v 1.3 2001/03/15 13:09:14 ctl Exp $

public class TriMatching {

  protected BranchNode leftRoot = null;
  protected BaseNode baseRoot = null;
  protected BranchNode rightRoot = null;

  // PROTOYPE CONSTRUCTOR!!!!!

  java.util.HashMap XMLn2n = null ;

  class NodeFactory {
    NodeFactory() { }
    Node makeNode(Node p,int c, XMLNode cont) { return null; }
  }

  public TriMatching( ONode left, ProtoBestMatching ml, ONode base, ProtoBestMatching mr, ONode right) {
     XMLn2n = new java.util.HashMap();
    leftRoot=(BranchNode) buildTree(null,-1,left, new NodeFactory() {
        Node makeNode(Node p,int c, XMLNode cont) {
          return new BranchNode(p,c,cont);
        }});
    baseRoot=(BaseNode) buildTree(null,-1,base, new NodeFactory() {
        Node makeNode(Node p,int c, XMLNode cont) {
          return new BaseNode(p,c,cont);
        }});
    rightRoot=(BranchNode) buildTree(null,-1,right, new NodeFactory() {
        Node makeNode(Node p,int c, XMLNode cont) {
          return new BranchNode(p,c,cont);
        }});

    addMappings( baseRoot, ml,mr );
    XMLn2n = null; // Free some memory...
/*    java.io.PrintWriter pw = new java.io.PrintWriter(System.out );
    leftRoot.debugTree(pw,0);
    pw.println("<<-----------------");
    rightRoot.debugTree(pw,0);
    pw.flush();*/
  }

  void addMappings( BaseNode base, ProtoBestMatching ml, ProtoBestMatching mr ) {
    ONode _base = (ONode) XMLn2n.get(base);
    // Find all mappings to the left, then to the right
    ONode match = ml.getFirstMapping(_base);
    do {
     BranchNode n2 = (BranchNode) XMLn2n.get(match);
     base.getLeft().addMatch( n2 );
     n2.setPartners( base.getRight() );
     n2.setBaseMatch( base, BranchNode.MATCH_FULL );
    } while( (match = ml.getNextMapping() ) != null );
    //Saftey check- no glus?
    ml.getFirstMapping(match);
    if( ml.getNextMapping() != null )
      throw new RuntimeException("FATAL: GLU mappings encounted!");
    match = mr.getFirstMapping(_base);
    do {
     BranchNode n2 = (BranchNode) XMLn2n.get(match);
     base.getRight().addMatch( n2 );
     n2.setPartners( base.getLeft() );
     n2.setBaseMatch( base, BranchNode.MATCH_FULL );
    } while( (match = mr.getNextMapping() ) != null );
    //Saftey check- no glus?
    mr.getFirstMapping(match);
    if( mr.getNextMapping() != null )
      throw new RuntimeException("FATAL: GLU mappings encounted!");
    // Recurse
    for(int i=0;i<base.getChildCount();i++)
      addMappings(base.getChild(i),ml,mr);
  }



  Node buildTree(Node parent, int childno, ONode n, NodeFactory f ) {
    XMLNode content = null;
    if( n instanceof ElementNode ) {
      content = new XMLElementNode(((ElementNode) n).name,((ElementNode) n).attributes );
    } else {
      content = new XMLTextNode(((TextNode) n).text );
    }
    Node root = f.makeNode(parent,childno,content);
    XMLn2n.put(root,n);
    XMLn2n.put(n,root);
    for( int i = 0;i<n.getChildCount();i++) {
      root.addChild(buildTree(root,i,n.getChild(i),f));
    }
    return root;
  }
  // END PROTO CODE

}