//$Id: TreeDM.java,v 1.3 2001/03/15 13:09:14 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

import java.io.FileReader;
import java.util.*;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

public class TreeDM {

  static final char PSEP = java.io.File.separatorChar;

  public TreeDM() {
  }

  public static void main(String[] args) throws Exception {
    (new TreeDM()).run( args );
  }

  // Run Best Matcher
  public void runBM( String[] args ) {
   ElementNode docA=null, docBase=null;
   if( args.length < 2 ) {
      System.out.println("Usage: TreeDM base.xml deriv.xml");
      System.exit(0);
   }
   try {
      Parser p = new Parser();
      System.out.println("Parsing " + args [0]);
      docBase = p.parse(args[0] + PSEP + "b.xml");
      System.out.println("Parsing " + args [1]);
      docA = p.parse(args[0] + PSEP+ "2.xml");
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
//   System.exit(0);
   ProtoBestMatching m1 = new ProtoBestMatching( docBase, docA, args[0] );

   //   System.out.println("Showing area tree..." );
//   java.awt.Frame treeView = new TreeView(m1.atRoot , null, m1, null );
   System.out.println("Showing mapping..." );
   java.awt.Frame treeView = new TreeView(docBase , docA , m1  );

   treeView.setVisible(true);
  }


  public void run( String[] args ) {
//    TreeDM handler = new TreeDM();

    // Parse each file provided on the
   // command line.
   ElementNode docA=null, docB=null, docBase=null;
   if( args.length < 3 ) {
      System.out.println("Usage: TreeDM base.xml base_v1.xml base_v2.xml");
      System.exit(0);
   }
   try {
      Parser p = new Parser();
      System.out.println("Parsing " + args [0]+"\\b.xml");
      docBase = p.parse(args[0]+PSEP+"b.xml");
      System.out.println("Parsing " + args [0]+"\\1.xml");
      docA = p.parse(args[0]+PSEP+"1.xml");
      System.out.println("Parsing " + args [0]+"\\2.xml");
      docB = p.parse(args[0]+PSEP+"2.xml");
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
//   System.exit(0);
   ProtoBestMatching m1 = new ProtoBestMatching( docBase, docA, "docA" );
   ProtoBestMatching m2 = new ProtoBestMatching( docBase, docB, "docB" );
  //m1.merge2( docA, docB, m2 );
   System.out.println("Should look like this:");
   Merge merge = new Merge( new TriMatching( docA, m1, docBase, m2, docB ) );
   java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
   java.io.PrintWriter pw = new java.io.PrintWriter( result );
    try {
   merge.merge( new MergePrinter(pw) );
    } catch ( org.xml.sax.SAXException e ) {
     System.out.println("SAXException while merging.. and it was yoyr lousy content handler that threw it");
    }
  pw.flush();
  try {result.close(); } catch (Exception e ) {}
  System.out.println("Merged XML:");
  System.out.print(result.toString());
    /*

   MapNode merged = 1==0 ? m1.merge(m2) : m1.mapRoot;
   System.out.println("SHowing tree(s)" );
   java.awt.Frame treeView = new TreeView( docB, docBase, m2, 1==0 ? null : m2.mapRoot );
   treeView.setVisible(true);

   /*   treeView = new TreeView( docB );
   treeView.setVisible(true);
*
   System.out.println("Merged XML is" );
   java.io.PrintWriter pw = new java.io.PrintWriter( System.out );
   m1.printXML( pw, merged, 0 );
  pw.flush();
  try {
    pw = new java.io.PrintWriter( new java.io.FileOutputStream("merged.xml"));
     m1.printXML( pw, merged, 0 );
    pw.close();
  } catch (java.io.FileNotFoundException e ) {
    e.printStackTrace();
  }
*/
  }

  class Parser extends DefaultHandler {

    /** Default parser name. */
    private static final String
    DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private TextNode currentText = null;
    private ElementNode currentNode = null;
    private Stack treestack = new Stack();

    public ElementNode parse( String file ) throws Exception {
      XMLReader xr = (XMLReader)Class.forName(DEFAULT_PARSER_NAME).newInstance();
      xr.setContentHandler(this);
      xr.setErrorHandler(this);
      try {
       xr.setFeature("http://xml.org/sax/features/namespaces",false);
      } catch (SAXException e) {
       System.out.println("Error setting features:" + e.getMessage());
      }

      FileReader r = new FileReader(file);
      xr.parse(new InputSource(r));
      ElementNode root = currentNode;
      currentNode = null; // Don't leave a ptr to the parsed tree; it can't be freed then!
      return root;
    }

     ////////////////////////////////////////////////////////////////////
     // Event handlers.
     ////////////////////////////////////////////////////////////////////


     public void startDocument ()
     {
         currentNode = new ElementNode( "", "$", null );
     }


     public void endDocument ()
     {
         //System.out.println("End document");
     }


     public void startElement (String uri, String name,
                               String qName, Attributes atts)
     {
         ElementNode n = new ElementNode( uri, qName, atts );
         currentNode.addChild( n );
         treestack.push( currentNode );
         currentNode = n;
         currentText = null;
         //         System.out.println("Start element: {" + uri + "}" + name);
     }


     public void endElement (String uri, String name, String qName)
     {
         currentText = null;
         currentNode = (ElementNode) treestack.pop();
//         System.out.println("End element:   {" + uri + "}" + name);
     }


     public void characters (char ch[], int start, int length)
     {
        String chars = new String( ch, start, length ).trim();
        if( chars.length() == 0 )
          return;
        if( currentText != null )
          currentText.text += chars;
        else {
          currentText = new TextNode( chars );
          currentNode.addChild( currentText );
        }
/*
         System.out.print("Characters:    \"");
         for (int i = start; i < start + length; i++) {
             switch (ch[i]) {
             case '\\':
                 System.out.print("\\\\");
                 break;
             case '"':
                 System.out.print("\\\"");
                 break;
             case '\n':
                 System.out.print("\\n");
                 break;
             case '\r':
                 System.out.print("\\r");
                 break;
             case '\t':
                 System.out.print("\\t");
                 break;
             default:
                 System.out.print(ch[i]);
                 break;
             }
         }
         System.out.print("\"\n");
 */
     }


  }



  class MergePrinter extends DefaultHandler {


    int indent = 0;
    private static final String IND = "                                                      ";
    private java.io.PrintWriter pw = null;

    MergePrinter( java.io.PrintWriter apw ) {
      pw=apw;
    }
     ////////////////////////////////////////////////////////////////////
     // Event handlers.
     ////////////////////////////////////////////////////////////////////


     public void startDocument ()
     {
        childcounter =HAS_CONTENT;
     }


     public void endDocument ()
     {
         //System.out.println("End document");
     }

      java.util.Stack csstack = new java.util.Stack();
     Integer childcounter = null;
     public void startElement (String uri, String name,
                               String qName, Attributes atts)

     {
      if( childcounter == null ) {
         pw.println(">");
        childcounter =HAS_CONTENT;
      }
      StringBuffer tagopen = new StringBuffer();
      tagopen.append('<');
      tagopen.append( qName );
    //    tagopen.append(' ');
      if( atts != null && atts.getLength() != 0 ) {
        for( int i = 0;i<atts.getLength();i++ ) {
          tagopen.append(' ');
          tagopen.append(atts.getQName(i));
          tagopen.append('=');
          tagopen.append('"');
          tagopen.append(atts.getValue(i));
          tagopen.append('"');
        }
      }
      csstack.push( childcounter );
      childcounter = null;
//      if( assumeNoChildren )
//        tagopen.append("/>");
//      else
//        tagopen.append('>');
      pw.print(IND.substring(0,indent)  + tagopen.toString());
      indent ++;
     }


     public void endElement (String uri, String name, String qName)
     {
        indent--;
          if( childcounter == null )
            pw.println(" />");
          else
            pw.println(IND.substring(0,indent)+ "</"+qName+">");
        childcounter = (Integer) csstack.pop();
     }

     final Integer HAS_CONTENT = new Integer(0);

     public void characters (char ch[], int start, int length)
     {
        pw.println(">");
        childcounter = HAS_CONTENT;
        String chars = new String( ch, start, length ).trim();
        if( chars.length() == 0 )
          return;
        pw.print(chars);
     }


  }



}