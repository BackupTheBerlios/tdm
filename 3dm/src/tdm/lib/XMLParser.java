// $Id: XMLParser.java,v 1.4 2001/09/05 21:22:30 ctl Exp $ D

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import java.util.Stack;
import java.io.FileReader;
import java.io.FileNotFoundException;

/** 3DM wrapper for a generic XML SAX parser. */

public class XMLParser extends DefaultHandler {

  /** Default parser name. */
  private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
  private  XMLReader xr = null;

  public XMLParser() throws Exception {
    this(DEFAULT_PARSER_NAME);
  }

  /** Create new parser using supplied SAX parser class name. */
  public XMLParser( String saxParserName ) throws Exception {
    try {
      xr = (XMLReader)Class.forName(saxParserName).newInstance();
    } catch (Exception e ) {
      new Exception("Unable to instantiate parser " + saxParserName );
    }
    xr.setContentHandler(this);
    xr.setErrorHandler(this);
    try {
     xr.setFeature("http://xml.org/sax/features/namespaces",false);
     xr.setFeature("http://xml.org/sax/features/validation",false);
    } catch (SAXException e) {
     throw new Exception("Error setting features:" + e.getMessage());
    }

  }

  // Parser state
  private String currentText = null;
  private Node currentNode = null;
  private NodeFactory factory = null;
  private Stack treestack = new Stack();


  /** Parse an XML file. Returns a parse tree of the XML file.
   *  @param file Input XML file
   *  @param aFactory Factory for creating nodes in the tree.
   */
  public Node parse( String file, NodeFactory aFactory ) throws ParseException,
          java.io.FileNotFoundException, java.io.IOException {
    factory = aFactory;
    FileReader r = new FileReader(file);
    try {
      xr.parse(new InputSource(r));
    } catch ( org.xml.sax.SAXException x ) {
      throw new ParseException(x.getMessage());
    }
    Node root = currentNode;
    // Don't leave a ptr to the parsed tree; it can't be GC'd then!
    currentNode = null;
    factory = null; // forget factory and allow GC
    return root;
  }

   public void startDocument () {
     currentNode = factory.makeNode( new XMLElementNode("$ROOT$",
       new AttributesImpl() ) );
   }

   public void endDocument () {
   }

   public void startElement (String uri, String name,
                             String qName, Attributes atts) {
     if( currentText != null )
       currentNode.addChild( factory.makeNode(
        new XMLTextNode( currentText.trim().toCharArray() )  ) );
     currentText = null;
     Node n = factory.makeNode( new XMLElementNode( qName, atts ) );
     currentNode.addChild(  n  );
     treestack.push( currentNode );
     currentNode = n;
///       //         System.out.println("Start element: {" + uri + "}" + name);
   }


   public void endElement (String uri, String name, String qName)
   {
     if( currentText != null )
       currentNode.addChild( factory.makeNode(
              new XMLTextNode( currentText.trim().toCharArray() ) ) );
     currentText = null;
     currentNode = (Node) treestack.pop();
   }


   public void characters (char ch[], int start, int length)
   {
      // The method trims whitespace from start and end of character data
      boolean lastIsWS = currentText == null || currentText.endsWith(" ");
      StringBuffer sb = new StringBuffer();
      for( int i=start;i<start+length;i++) {
        if( Character.isWhitespace(ch[i]) ){
          if( lastIsWS )
            continue;
          sb.append(" ");
          lastIsWS = true;
        } else {
          sb.append(ch[i]);
          lastIsWS = false;
        }
      }
      String chars = sb.toString(); //.trim();
///        String chars = new String( ch, start, length ).trim();
      if( chars.trim().length() == 0 )
        return;
      if( currentText != null )
        currentText += chars;
      else {
        currentText = chars;
///        //currentNode.addChild( currentText );
      }
   }
}