// $Id: NodeFactory.java,v 1.4 2001/04/19 20:45:50 ctl Exp $

public abstract class NodeFactory {

  public NodeFactory() {
  }

  public abstract Node makeNode( Node parent, int childPos, XMLNode content );
}