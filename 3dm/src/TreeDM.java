//$Id: TreeDM.java,v 1.12 2001/04/02 07:37:55 ctl Exp $
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
import java.io.*;

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
    (new TreeDM()).runHarness( args );
  }

  public void runHarness( String[] args ) {
   if( args.length < 1 ) {
      System.out.println("Usage: TreeDM case_dir");
      System.exit(0);
   }
   System.out.println("3DM test harness..Starting test");
   File topDir = new File(args[0]);
   File[] caseDirs = topDir.listFiles();
   if( caseDirs == null ) {
     System.out.println("No cases found in " + topDir.getName());
     System.exit(0);
   } else
     Arrays.sort(caseDirs);
   int casesRun=0;
   for(int ifile=0;ifile<caseDirs.length;ifile++) {
    if( caseDirs[ifile].isDirectory() && Character.isDigit( caseDirs[ifile].getName().charAt(1) ) ) {
      runCase(caseDirs[ifile] );
      casesRun++;
    }
   }
   if( casesRun == 0) {
     runCase( topDir );
   } else
     System.out.println(casesRun + " cases run.");
  }

  private void runCase( File dir ) {
    final boolean SHOWDIFF = true;
    File base = new File( dir, "b.xml");
    File b1 = new File( dir, "1.xml");
    File b2 = new File( dir, "2.xml");
    File merged = new File( dir, "m.xml");
    File notes = new File( dir,"notes" );
    ElementNode docA=null, docB=null, docBase=null;
    System.out.println("Running case " + dir.getName() );
    if( notes.exists() ) {
      try {
        BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( notes ) ) );
        String s="";
        while((s=in.readLine())!=null)
          System.out.println("NOTES: "+s);
      } catch (Exception e ) {
          System.out.println("NOTES: READ EXCEPTED ");
      }
    }
    try {
      Parser p = new Parser();
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docBase = p.parse( base.getCanonicalPath() );
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docA = p.parse( b1.getCanonicalPath() );
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docB = p.parse( b2.getCanonicalPath() );

/*      System.out.println("Parsing " + args [0]+"\\1.xml");
      docA = p.parse(args[0]+PSEP+"1.xml");
      System.out.println("Parsing " + args [0]+"\\2.xml");
      docB = p.parse(args[0]+PSEP+"2.xml");
      System.out.println("OK.");
*/
    } catch ( Exception e ) {
      System.out.println("PARSE ERROR IN CASE, giving up." );
      //e.printStackTrace();
      return;
    }
    ProtoBestMatching m1 = new ProtoBestMatching( docBase, docA, "docA" );
     File mf1 = new File( dir, "match1");
     if( mf1.exists() ) {
      System.out.println("Additional matchings from "+mf1.getName());
      m1.matchFromFile(mf1);
     }

    ProtoBestMatching m2 = new ProtoBestMatching( docBase, docB, "docB" );
     File mf2 = new File( dir, "match2");
     if( mf2.exists() ) {
      System.out.println("Additional matchings from "+mf2.getName());
      m2.matchFromFile(mf2);
     }

    Merge merge1 = new Merge( new TriMatching( docA, m1, docBase, m2, docB ) );
    Merge merge2 = new Merge( new TriMatching( docB, m2, docBase, m1, docA ) );
    PrintWriter p1 = null, p2=null;
    try {
      p1 = new PrintWriter( new FileOutputStream( "m1.xml" ) );
      p2 = new PrintWriter( new FileOutputStream( "m2.xml" ) );
    } catch (java.io.FileNotFoundException e ) {
      System.out.println("ERROR: Can't write merged files");
      return;
    }
    try {
      merge1.merge( new MergePrinter(p1) );
      merge2.merge( new MergePrinter(p2) );
    } catch ( org.xml.sax.SAXException e ) {
      System.out.println("SAXException while merging.. and it was yoyr lousy content handler that threw it");
    }
    p1.close();p2.close();
    System.out.print("Checking merges... " );
    ElementNode facit=null,mr1=null,mr2=null;
    try {
      Parser p = new Parser();
      facit = p.parse( merged.getCanonicalPath() );
      mr1 = p.parse( "m1.xml" );
      mr2 = p.parse( "m2.xml" );
    } catch ( Exception e ) {
      System.out.println("PARSE ERROR IN CASE when parsing merged files. Giving up." +e.getMessage() );
      //e.printStackTrace();
      return;
    }
    boolean symmetry = treesIdentical(mr1,mr2,SHOWDIFF),
            likefacit = treesIdentical(facit,mr1,SHOWDIFF);
    if( !symmetry )
      System.out.print("SYMMETRY failed ");
    if( ((ElementNode) facit.getChild(0)).name.equalsIgnoreCase("conflict") ) {
      System.out.println("Facit says this was a CONFLICT" );
      return;
    }
    if( !likefacit )
      System.out.print("FACIT failed " + ( symmetry || (!symmetry && !treesIdentical(facit,mr2,false)) ?
        "(on both)" : "(on one only)" ) );
    if( likefacit && symmetry )
      System.out.println("Ok ");
    else {
      PrintWriter pw = new PrintWriter( System.out );
      pw.println( !symmetry ? "\nMerge 1: " : "\nMerge" );
      try {
        merge1.merge( new MergePrinter(pw) );
        if( !symmetry ) {
          pw.println( "Merge 2:"  );
          merge2.merge( new MergePrinter(pw) );
        }
      } catch ( Exception e ) {
        System.out.println("PARSE ERROR..." );
      }
      pw.flush();
    }
    return;
  }

  private boolean treesIdentical( ONode a, ONode b, boolean searchAll ) {
    boolean result = true;
    if( !a.contentEquals(b) ) {
      if(searchAll) {
        System.out.println("IDENTITY Failed");
        System.out.println(a.toString());
        System.out.println(b.toString());
      }
     result = false;
    }
    if( a.getChildCount() != b.getChildCount() ) {
      if(searchAll) System.out.println("IDENTITY failed: childcount failed below "+ a.toString());
       result = false;
    } else if( result || searchAll )
      for( int i=0;i<a.getChildCount();i++)
        if( !treesIdentical( a.getChild(i), b.getChild(i), searchAll ) )
          result = false;
    return result;
  }

  // Run Best Matcher
  public void runBM( String[] args ) {
   ElementNode docA=null, docBase=null;
   final String OTHER = "2";
    if( args.length < 2 ) {
      System.out.println("Usage: TreeDM base.xml deriv.xml");
      System.exit(0);
   }
   try {
      Parser p = new Parser();
      System.out.println("Parsing " + args [0]);
      docBase = p.parse(args[0] + PSEP + "b.xml");
      System.out.println("Parsing " + OTHER + ".xml");
      docA = p.parse(args[0] + PSEP+ OTHER+".xml");
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
//   System.exit(0);
   ProtoBestMatching m1 = new ProtoBestMatching( docBase, docA, args[0] );
   java.io.File mf = new java.io.File( args[0] + PSEP+ "match"+OTHER );
   if( mf.exists() ) {
    System.out.println("Additional matchings from match"+OTHER);
    m1.matchFromFile(mf);
   }

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
   Merge merge2 = new Merge( new TriMatching( docB, m2, docBase, m1, docA ) );

   java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
   java.io.PrintWriter pw = new java.io.PrintWriter( result );
    try {
   merge.merge( new MergePrinter(pw) );
    } catch ( org.xml.sax.SAXException e ) {
     System.out.println("SAXException while merging.. and it was yoyr lousy content handler that threw it");
    }
  pw.flush();
    try {
   merge2.merge( new MergePrinter(pw) );
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


   public InputSource resolveEntity (String publicId, String systemId)
   {
     System.out.println("!!!!!!!!!!!!!!!!!!!!!!!Never gets here!");
     if (systemId.equals("http://www.myhost.com/today")) {
              // return a special input source
       //MyReader reader = new MyReader();
       //return new InputSource(reader);
        return null;
     } else {
              // use the default behaviour
       return null;
     }
   }

    public ElementNode parse( String file ) throws Exception {
      XMLReader xr = (XMLReader)Class.forName(DEFAULT_PARSER_NAME).newInstance();
      xr.setContentHandler(this);
      xr.setErrorHandler(this);
      try {
       xr.setFeature("http://xml.org/sax/features/namespaces",false);
       xr.setFeature("http://xml.org/sax/features/validation",false);
//       xr.setFeature("http://xml.org/sax/features/external-general-entities",false);
//       xr.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
       xr.setFeature("http://apache.org/xml/features/continue-after-fatal-error",true);

      } catch (SAXException e) {
       System.out.println("Error setting features:" + e.getMessage());
      }
//      xr.setEntityResolver(this);

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
         if( currentText != null )
           currentText.text = currentText.text.trim();
         ElementNode n = new ElementNode( uri, qName, atts );
         currentNode.addChild( n );
         treestack.push( currentNode );
         currentNode = n;
         currentText = null;
         //         System.out.println("Start element: {" + uri + "}" + name);
     }


     public void endElement (String uri, String name, String qName)
     {
         if( currentText != null )
           currentText.text = currentText.text.trim();
         currentText = null;
         currentNode = (ElementNode) treestack.pop();
//         System.out.println("End element:   {" + uri + "}" + name);
     }


     public void characters (char ch[], int start, int length)
     {
        boolean lastIsWS = currentText == null || currentText.text.endsWith(" ");
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
//        String chars = new String( ch, start, length ).trim();
        if( chars.trim().length() == 0 )
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

     public void characters (char ch[], int startpos, int length)
     {
        if(childcounter!=HAS_CONTENT)
           pw.println(">");
        childcounter = HAS_CONTENT;
        String chars = new String( ch, startpos, length ).trim();
        if( chars.length() == 0 )
          return;/*
        int start=0,next=-1;
        do {
          next=chars.indexOf("\n",start);
          if( next==-1)
            pw.println(chars.substring(start));
          else {
            pw.println(chars.substring(start,next));
            start=next+1;
          }
        } while( next != -1 );*/
        pw.print(chars);
     }


  }



}