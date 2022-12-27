package cmsc420_f22;


import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.regex.*;


// Simple class for visualizing operations on an XkdTree. Draws the tree, and even shows results of kNN queries
public class Visualizer extends Frame implements WindowListener, ActionListener {

    public void update() {
        update("Xkd Tree visualization");
    }

    // This is called by the command handler at the end of a command
    // Updates tree with any changes that may have occured,
    // and the the run button has not been pressed, runs through the test at reduced speed
    public void update(String msg) {


        // Updating graphics and title with most recent command
        setTitle(msg);
        paint(g);
        
        
       // Blocks until a button press unlocks the lock
        synchronized(lock) {
            while(!continueExec && !runThrough)
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            // We're going to slow down the execution if run through is enabled however
            if (runThrough)
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }


        continueExec = false;

        // Setting knn to be null so results only appear through one step
        knn = null;
    }

    // Passes the results of a knn query to the visualizer
    public void passKNN(ArrayList<LabeledPoint2D> knn, Point2D center) {
        this.knn = knn;
        this.centerKNN = center;
    }

    Graphics g;

    Object lock;

    // Bounding box passed in along with rectangle
    Rectangle2D bbox;


    // Width height, and upper coords of rectangle
    int xAnchor;
    int yAnchor;
    int width;
    int height;


    Boolean continueExec;
    Boolean runThrough;

    Button cButton;
    Button rButton;
    

    ArrayList<LabeledPoint2D> knn;
    Point2D centerKNN;
    

    XkdTree kdTree;
    

    
    public Visualizer(Rectangle2D bbox, XkdTree ktree) {
        this();
        this.bbox = bbox;
        this.kdTree = ktree;

        // Scales box to be about 700x700
        if (bbox.getWidth(0) > bbox.getWidth(1)) {
            width = 700;
            height = (int) (700 * (bbox.getWidth(1) / bbox.getWidth(0)));
           
        } else {
            width = (int) (700 * (bbox.getWidth(0) / bbox.getWidth(1)));
            height = 700;
            
        }

        paint(g);
    }

    public Visualizer() {
        setTitle("Xkd Tree visualization");

        setBounds(0, 0, 1100, 1100);
        setResizable(false);
        addWindowListener(this);
        setBackground(Color.GRAY);
        setLayout(new FlowLayout());

        continueExec = false;
        runThrough = false;

        lock = new Object();

        cButton = new Button("Continue");
        cButton.addActionListener(this);

        rButton = new Button("Run");
        rButton.addActionListener(this);

        add(cButton);
        add(rButton);

        setVisible(true);
        knn = null;
        xAnchor = 100;
        yAnchor = 100;

        g = this.getGraphics();
        

    }


    // This converts a double coord to an int coord to paint via the following formula:
    // If x : int(((x - lowValX) / (highValX - lowValX)) * intWidth of rectangle) + rectange X Anchor
    // If y : int((1 - (y - lowValY) / (highValY - lowValY)) * intWidth of rectangle) + rectange Y Anchor (We need to flip the Y values to draw them)
    private int scaleDouble(int dim, Double dp) {

        int relVal = dim == 0 ? width : height;
        int relAnchor = dim == 0 ? xAnchor : yAnchor;

        double lowVal = bbox.low.get(dim);
        double highVal = bbox.high.get(dim);

        double propVal = (dp - lowVal) / (highVal - lowVal);


        return relAnchor + (int) (relVal * (dim == 0 ? propVal : 1.0 - propVal));

    }


    // Draws the k nearest neighbors curcle
    private void drawKNN() {

        if (knn.size() == 0)
            drawKNNCircle(centerKNN, centerKNN);
        else {
            drawKNNCircle(centerKNN, knn.get(knn.size() - 1).getPoint2D());

            // Highlighting all points inside the circle
            for (LabeledPoint2D lp : knn)
                drawKNNPoint(lp.getPoint2D());

        }
        
    }


    // Draws a circle, with the first argument being the middle point of the circle, and the second being a point on the perimeter
    private void drawKNNCircle(Point2D center, Point2D outer) {

        int centerX = scaleDouble(0, center.getX());
        int centerY = scaleDouble(1, center.getY());
        
        // Really roundabout way of getting the int radius, but im lazy
        int radius = (int) (new Point2D(centerX, centerY))
                    .distance(new Point2D(scaleDouble(0, outer.getX()), scaleDouble(1, outer.getY())));

        g.setColor(Color.PINK);

        g.drawOval(centerX - radius, centerY - radius, radius*2, radius*2);

        g.setColor(Color.darkGray);
        g.fillOval(centerX - 2, centerY - 2, 4, 4);
    }

    
    // Drawing a point (no text)
    private void drawKNNPoint(Point2D pt) {
        int xCoord = scaleDouble(0, pt.getX());
        int yCoord = scaleDouble(1, pt.getY());

        g.setColor(Color.red);
        g.fillOval(xCoord - 2, yCoord - 2, 4, 4);


    }

    // Simple function for drawing a point
    private void drawPoint(String label, Point2D pt) {

        g.setColor(Color.GREEN);

        int xCoord = scaleDouble(0, pt.getX());
        int yCoord = scaleDouble(1, pt.getY());

        // Slight offset to keep oval centered on point
        g.fillOval(xCoord - 2, yCoord - 2, 4, 4);

        // Slight offset so label doesnt overlap point
        g.setColor(Color.BLACK);
        g.drawString(label + ":" + pt.toString(), xCoord + 5, yCoord);

    }

    // Draws the cutting line (for internal nodes)
    private void drawLine(Rectangle2D subpart, int dim, Double where) {

        int lineCord = scaleDouble(dim, where);

        

        // We draw the line from the bounds of the rectangle
        g.setColor(Color.BLACK);
        if (dim == 0)
            g.drawLine(lineCord, scaleDouble(1, subpart.low.getY()) , lineCord, scaleDouble(1, subpart.high.getY()));
        else
            g.drawLine(scaleDouble(0, subpart.low.getX()) , lineCord, scaleDouble(0, subpart.high.getX()), lineCord);

        
        g.setColor(Color.blue);
        if (dim == 0)
            g.drawString("x=" + where, lineCord + 2, (scaleDouble(1, subpart.low.getY()) + scaleDouble(1, subpart.high.getY())) / 2);
        else
            g.drawString("y=" + where, (scaleDouble(0, subpart.low.getX()) + scaleDouble(0, subpart.high.getX())) / 2, lineCord - 4);
        
    }


    // Recursive tree drawer
    private void drawXkdTree(Rectangle2D bbox, ArrayList<String> curr) {

        // If empty we just stop, shouldn't happen but just in case
        if (curr.isEmpty())
            return;

        String currNode = curr.get(0);
        curr.remove(0);

        // For parsing internal nodes
        Matcher internalMatcher = Pattern.compile("\\((x|y)=(\\d+\\.\\d+)\\)").matcher(currNode);

        if (internalMatcher.find()) {

            int dim = internalMatcher.group(1).equals("x") ? 0 : 1;
            double value = Double.parseDouble(internalMatcher.group(2));
            drawLine(bbox, dim, value);

            drawXkdTree(bbox.rightPart(dim, value), curr);
            drawXkdTree(bbox.leftPart(dim, value), curr);
        

        } else {

            // For parsing external nodes
            Matcher extPoint = Pattern.compile("\\{(\\w+): \\((\\d+\\.\\d+)\\,(\\d+\\.\\d+)\\)\\} ").matcher(currNode);

            while (extPoint.find()) {
                String label = extPoint.group(1);
                double xCoord = Double.parseDouble(extPoint.group(2));
                double yCoord = Double.parseDouble(extPoint.group(3));

                drawPoint(label, new Point2D(xCoord, yCoord));
            }

        }

    }


    public void paint(Graphics g) {

        try {
            g.clearRect(0, 0, 1600, 1600);
            if (bbox != null) {

                
                g.setColor(Color.WHITE);
                


                // Drawing main rect
                g.fillRect(xAnchor, yAnchor, width, height);

                // Drawing high and low
                g.setColor(Color.BLACK);
                g.drawString(bbox.high.toString(), xAnchor + width - 40, yAnchor - 5);
                g.drawString(bbox.low.toString(), xAnchor, yAnchor + height + 10);

                // Drawing kd tree
                drawXkdTree(bbox, kdTree.list());

                // If there are new knn queries to report, draw them now.
                if (knn != null)
                    drawKNN();
                

            }
        } catch (Exception e) {}
    }

    

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        this.dispose();
        g = null;


        /*If we get rid of the window, we need to set runThrough to true otherwise we will */
        synchronized (lock) {
            runThrough = true;
            continueExec = true;

            lock.notifyAll();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {

        // Depending on the source, set the appropriate block condition to true
        synchronized(lock) {
            if (e.getSource().equals(cButton))
                continueExec = true;
            else if (e.getSource().equals(rButton))
                runThrough = runThrough ? false : true;
            
            if (runThrough)
                rButton.setBackground(Color.green);
            else
                rButton.setBackground(Color.red);
            
            lock.notifyAll();
        }
        
    }
    
}
