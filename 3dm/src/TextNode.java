// $Id: TextNode.java,v 1.1 2001/03/14 08:23:54 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

import java.io.PrintWriter;

public class TextNode extends ONode {
  public String text=null;

  TextNode( String value ) {
    text = value;
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof TextNode )
      return ((TextNode) a).text.equals(text);
    else
      return false;
  }

  public String toString() {
    return text;
  }

  public void printXML1( PrintWriter pw, int level, boolean assumeNoChildren ) {
    pw.println("                                               ".substring(0,2*level) + text);
  }

}