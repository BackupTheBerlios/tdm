// $Id: TriMatching.java,v 1.1 2001/03/14 08:23:55 ctl Exp $

public class TriMatching {

  protected BranchNode leftRoot = null;
  protected BaseNode baseRoot = null;
  protected BranchNode rightRoot = null;

  // PROTOYPE CONSTRUCTOR!!!!!

  java.util.HashMap XMLn2n = null ;

  class NodeFactory {
    NodeFactory() { }
    Node makeNode(Node p,int c) { return null; }
  }

  public TriMatching( ONode left, ProtoBestMatching ml, ONode base, ProtoBestMatching mr, ONode right) {
     XMLn2n = new java.util.HashMap();
    leftRoot=(BranchNode) buildTree(null,-1,left, new NodeFactory() {
        Node makeNode(Node p,int c) {
          return new BranchNode(p,c);
        }});
    baseRoot=(BaseNode) buildTree(null,-1,base, new NodeFactory() {
        Node makeNode(Node p,int c) {
          return new BaseNode(p,c);
        }});
    rightRoot=(BranchNode) buildTree(null,-1,right, new NodeFactory() {
        Node makeNode(Node p,int c) {
          return new BranchNode(p,c);
        }});

    addMappings( baseRoot, ml,mr );
    XMLn2n = null; // Free some memory...
    java.io.PrintWriter pw = new java.io.PrintWriter(System.out );
    leftRoot.debugTree(pw,0);
    pw.println("<<-----------------");
    rightRoot.debugTree(pw,0);
    pw.flush();
  }

  void addMappings( BaseNode base, ProtoBestMatching ml, ProtoBestMatching mr ) {
    ONode _base = (ONode) XMLn2n.get(base);
    // Find all mappings to the left, then to the right
    ONode match = ml.getFirstMapping(_base);
    do {
     BranchNode n2 = (BranchNode) XMLn2n.get(match);
     base.getLeft().addMatch( n2 );
     n2.setPartners( base.getRight() );
    } while( (match = ml.getNextMapping() ) != null );
    match = mr.getFirstMapping(_base);
    do {
     BranchNode n2 = (BranchNode) XMLn2n.get(match);
     base.getRight().addMatch( n2 );
     n2.setPartners( base.getLeft() );
    } while( (match = mr.getNextMapping() ) != null );
    // Recurse
    for(int i=0;i<base.getChildCount();i++)
      addMappings(base.getChild(i),ml,mr);
  }



  Node buildTree(Node parent, int childno, ONode n, NodeFactory f ) {
    Node root = f.makeNode(parent,childno);
    XMLNode content = null;
    if( n instanceof ElementNode ) {
      content = new XMLElementNode(((ElementNode) n).name,((ElementNode) n).attributes );
    } else {
      content = new XMLTextNode(((TextNode) n).text );
    }
    root.setContent(content);
    XMLn2n.put(root,n);
    XMLn2n.put(n,root);
    for( int i = 0;i<n.getChildCount();i++) {
      root.addChild(buildTree(root,i,n.getChild(i),f));
    }
    return root;
  }
  // END PROTO CODE

}