// $Id: TreeView.java,v 1.1 2001/03/14 08:23:55 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.util.*;

/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

public class TreeView extends Frame  {

  private TreeCanvas tc = null;

  public TreeView( ONode r1, ONode r2, ProtoBestMatching m ) {
//    root = r;
    setBounds(100,100,1100,800);
    setLayout( new BorderLayout() );
    ScrollPane sp = new ScrollPane();
    //if( opTree == null ) {
      tc = new MappingCanvas( r1, r2, m );
      tc.root = r1;
/*    } else {
      tc = new TreeCanvas();
      tc.root = opTree;
    //}
*/
    tc.setSize(1000,5000);
    sp.add(tc);
    Button btnPrint = new Button("Print");
    add( sp, BorderLayout.CENTER );
    add( btnPrint, BorderLayout.SOUTH );
    addWindowListener( new WindowAdapter() {
        public void windowClosing( WindowEvent e ) {
          dispose();
          System.exit(0);
        }
      });
    btnPrint.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
        PrinterJob pj = PrinterJob.getPrinterJob();
                PageFormat pf = pj.pageDialog(pj.defaultPage());
     Paper p = pf.getPaper();
     p.setImageableArea( 12,12,p.getWidth()-12,p.getHeight()-12);
    pf.setPaper(p);

        pj.setPrintable(tc,pf);
         if (pj.printDialog()) {
             try {
                 pj.print();
             } catch (Exception ex) {
                 ex.printStackTrace();
             }
         }
      }
    }
    );
  }


  class TreeCanvas extends Canvas implements Printable {
    int counter = 0,leafno=0;
    Object root = null;
    Color tagColor = new Color( 0,127,0);

    public int print(Graphics g, PageFormat pf, int pi)
                                   throws PrinterException {
     if (pi >= 1) {
         return Printable.NO_SUCH_PAGE;
     }
     ((Graphics2D) g).translate(20,20);
     ((Graphics2D) g).scale(0.5,0.4);

     paint(g);
     return Printable.PAGE_EXISTS;
   }

    public void paint( Graphics g ) {
      counter = 0;
      leafno=3;
      if( root instanceof ElementNode )
        drawTree( (Node) root ,0,g);
      else {
        depthDelta = 128;
        //drawOpTree( (MapNode) root,0,g);
      }
    }

    // Draw control vars
    int depthDelta = 64;
    boolean mirror = false;

    protected void makePoint( Object n, int x, int y ) {
    }

    public int drawTree( Object n, int depth, Graphics g ) {
/*      counter++;
      if( counter > 5000 )
        return leafno;
 */
      if(n == null )
        return 0;
      if( n instanceof TextNode ) {
        int x = depth * depthDelta, y= leafno * 12 + 6;
        g.drawOval(x-4,y-4,8,8);
        makePoint( n, x, y);
        String text = ((TextNode) n).text;
        if( text.length() > 10 )
          text = text.substring(0,9) + "...";
        if( mirror )
          g.drawString(text,x-5-g.getFontMetrics().stringWidth(text),y+4);
        else
          g.drawString(text,x+5,y+4);
        leafno++;
        return y;
      } else  {
        // it's an element node
        // first draw the children
        int x=0,y=0;
        //ElementNode en = (ElementNode) n;
        Vector children = null;
        if( n instanceof ElementNode )
          children = ((ElementNode) n).children;
        else if( n instanceof AreaNode )
          children = ((AreaNode) n).children;

        if( children.size() > 0 ) {
          int[] ys = new int[children.size()];
          for( int i =0;i< children.size();i++) {
            ys[i] = drawTree( (ONode) children.elementAt(i), depth + (mirror ? -1 : 1), g );
          }
          int midy = (ys[0] + ys[ys.length-1])/2;
          for( int i =0;i< children.size();i++) {
            g.drawLine(depth*depthDelta,midy,(depth + (mirror ? -1 : 1))*depthDelta,ys[i]);
          }
          x = depth * depthDelta;
          y= midy;
        } else {
          x= depth * depthDelta;
          y = leafno * 12 + 6;
          leafno++;
        }
        // then myself
  //      int x = depth * 64, y= midleaf * 12 + 6;
        g.drawRect(x-4,y-4,8,8);
        makePoint( n, x, y);
        g.setColor( tagColor );
        String label = "";
        if( n instanceof ElementNode )
          label =  ((ElementNode) n).name +"(" + ((ElementNode) n).childNo +")";
        else
          label = n.toString();
        if( mirror )
          g.drawString( label, x-5-g.getFontMetrics().stringWidth(label), y+4);
        else
          g.drawString( label, x+5, y+4);
        g.setColor( Color.black );
        return y;
      }
    }

/*    public int drawOpTree( MapNode en, int depth, Graphics g ) {
      // it's an element node
      // first draw the children
      int x=0,y=0;
      if( en.getChildCount() > 0 ) {
        int[] ys = new int[en.getChildCount()];
        for( int i =0;i< en.getChildCount();i++) {
          ys[i] = drawOpTree( en.getChild(i), depth + (mirror ? -1 : 1), g );
        }
        int midy = (ys[0] + ys[ys.length-1])/2;
        for( int i =0;i< en.getChildCount();i++) {
          g.drawLine(depth*depthDelta,midy,(depth + (mirror ? -1 : 1))*depthDelta,ys[i]);
        }
        x = depth * depthDelta;
        y= midy;
      } else {
        x= depth * depthDelta;
        y = leafno * 12 + 6;
        leafno++;
      }
      // then myself
//      int x = depth * 64, y= midleaf * 12 + 6;
      g.drawRect(x-4,y-4,8,8);
      g.setColor( tagColor );
      String label = en.operations.toString();
      if( mirror )
        g.drawString( label, x-5-g.getFontMetrics().stringWidth(label), y+4);
      else
        g.drawString( label, x+5, y+4);
      g.setColor( Color.black );
      return y;
    }
*/

  }


  class MappingCanvas extends TreeCanvas {
    private ProtoBestMatching m = null;
    private ONode rootA, rootB;
    private HashMap nodesMap = null;
    private HashSet visibleMappings = new HashSet();

    abstract class VisibleMapping {
      abstract public void  draw( Graphics g );
    }

    class LineMapping extends VisibleMapping {
      private int x1,y1,x2,y2;
      LineMapping( Point p1, Point p2 ) {
        x1 = p1.x; y1 = p1.y;
        x2 = p2.x; y2 = p2.y;
      }

      public void draw( Graphics g ) {
        g.setColor(Color.blue);
        g.drawLine(x1,y1,x2,y2);
      }
    }

    class NoMapping extends VisibleMapping {
      private int x1,y1;
      NoMapping( Point p1 ) {
        x1 = p1.x; y1 = p1.y;
      }

      public void draw( Graphics g ) {
        g.setColor(Color.red);
              g.drawLine( x1-4,y1-4,x1+4,y1+4);
              g.drawLine( x1+4,y1-4,x1-4,y1+4);
      }
    }


    public MappingCanvas( ONode aRootA, ONode aRootB, ProtoBestMatching aM ) {
      super();
      rootA = aRootA;
      rootB = aRootB;
      m = aM;
      addMouseListener( new  MouseAdapter () {
        public void mouseClicked( MouseEvent e ) {
          // Find clicked on node
          if( nodesMap == null )
            return;
          ONode n = null;
          Point p = e.getPoint();
          Graphics g = getGraphics();
          g.setColor(Color.blue);
          for( Iterator i = nodesMap.keySet().iterator();i.hasNext();) {
            ONode temp = (ONode) i.next();
            Point p2 = (Point) nodesMap.get(temp);
            int dx = p2.x-p.x, dy = p2.y-p.y;
            if( dx*dx+dy*dy < 16 ) {
              n = temp;
              p = p2;
              break;
            }
          }
          if( n != null ) {
            ONode n2 = m.getFirstMapping(rootA,n);
            if( n2 == null ) {
              VisibleMapping vm = new  NoMapping(p);
              visibleMappings.add(vm);
              vm.draw(g);
            } else {
              while( n2 != null ) {
                Point dst = (Point) nodesMap.get( n2 );
                VisibleMapping vm = new  LineMapping(p,dst);
                visibleMappings.add(vm);
                vm.draw(g);
                n2 = m.getNextMapping();
              }
            }
          }
          g.setColor(Color.black);
        }
      }
      );
    }

    protected void makePoint( Object n, int x, int y ) {
        //System.out.println("Added "+x+" "+y);
     if( nodesMap != null ) {
        nodesMap.put(n,new Point(x,y));
     }

    }

    public void paint( Graphics g ) {
      nodesMap = new HashMap();
      leafno=3;
      mirror = false;
      drawTree( rootA ,0,g);
      leafno=3;
      mirror = true;
      drawTree( rootB ,16, g);
      for( Iterator i = visibleMappings.iterator();i.hasNext();)
        ((VisibleMapping) i.next()).draw(g);

    }


  }

}
