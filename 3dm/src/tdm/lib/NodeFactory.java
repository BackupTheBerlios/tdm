// $Id: NodeFactory.java,v 1.3 2001/04/19 13:59:03 ctl Exp $

public abstract class NodeFactory {

  public NodeFactory() {
  }

  public abstract Node makeNode( XMLNode content );
}