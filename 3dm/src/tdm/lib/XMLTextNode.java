// $Id: XMLTextNode.java,v 1.1 2001/03/14 08:23:55 ctl Exp $

public class XMLTextNode extends XMLNode {

  public String text=null;

  public XMLTextNode(String value) {
    text = value;
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof XMLTextNode )
      return ((XMLTextNode) a).text.equals(text);
    else
      return false;
  }

  public String toString() {
    return text;
  }
}