// $Id: XMLTextNode.java,v 1.2 2001/03/15 13:09:14 ctl Exp $

public class XMLTextNode extends XMLNode {

// NOTE!! Consider buffering char[] instead
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

  public String getText() {
    return text;
  }

  public String toString() {
    return text;
  }
}