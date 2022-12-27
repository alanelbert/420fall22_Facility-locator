package cmsc420_f22; // Do not delete this line

import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;
import java.util.Comparator;

// Alan Elbert
// Implementation of the XkdTree

public class XkdTree<LPoint extends LabeledPoint2D> {


	// Lexiographically sorting points
	private class PointSort implements Comparator<LPoint> {

		int sortDim;

		// Selecting main dim
		public PointSort(int how) {
			sortDim = how;
		}


		// We compare by first dim, then by second dim
		public int compare(LPoint p1, LPoint p2) {

			Point2D mVal1 = p1.getPoint2D();
			Point2D mVal2 = p2.getPoint2D();

			int tr = Double.compare(mVal1.get(sortDim), mVal2.get(sortDim));

			if (tr == 0)
				return Double.compare(mVal1.get(1 - sortDim), mVal2.get(1 - sortDim));
			else
				return tr;
		}
	}



	// Our node definition, for all methods that make changes to the tree it returns
	// itself to allow for recursive updates
	private abstract class Node {

	
		abstract LPoint find(Point2D q);
		abstract Node bulkInsert(ArrayList<LPoint> pts);
		abstract ArrayList<String> list();
		abstract Boolean empty();
		abstract Node delete(Point2D td);
		abstract void kNearestNeighbor(MinK<Double, LPoint> heap, Point2D center, Rectangle2D part);

	}

	

	// Internal Node definition
	private class InternalNode extends Node {
		int cutDim;
		double cutVal;
		Node left;
		Node right;
		

		// Initializes a node with left and right already
		// Ext nodes, to make the initial insertion easier
		public InternalNode(int cut, double val) {
			cutDim = cut;
			cutVal = val;


			left = new ExternalNode();
			right = new ExternalNode();

		}


		// Internal nodes are never empty, if they are we have a problem!
		public Boolean empty() {
			return false;
		}


		// Traverses down the tree based on q and the cutting dimension
		public LPoint find(Point2D q) {
			double togo = cutVal - q.get(cutDim);

			if (togo > 0)
				return left.find(q);
			else if (togo < 0)
				return right.find(q);
			else
				return left.find(q) != null ? left.find(q) : right.find(q);

		}


		// Inserts points in bulk
		public Node bulkInsert(ArrayList<LPoint> pts) {

			// If we have no points to insert we stop wasting time and return
			if (pts.size() == 0)
				return this;

		
			// We have two arrays, appropriately named to keep track of nodes smaller and bigger
			// than the cutting val for the cutting dimension
			ArrayList<LPoint> smallChungus = new ArrayList<>();
			ArrayList<LPoint> bigChungus = new ArrayList<>();

			for (LPoint pt : pts)
				if (pt.getPoint2D().get(cutDim) < cutVal)
					smallChungus.add(pt);
				else
					bigChungus.add(pt);

			
			left = left.bulkInsert(smallChungus);
			right = right.bulkInsert(bigChungus);

			return this;
		}


		// Generic preorder traversal (the right to left really tripped me up)
		public ArrayList<String> list() {

			ArrayList<String> tr = new ArrayList<>();

			tr.add("(" + (cutDim == 0 ? 'x' : 'y') + "=" + cutVal + ")");

			tr.addAll(right.list());
			tr.addAll(left.list());

			return tr;
		}

		
		// Assuming that we have verified that the node we want to delete is in our tree
		// This just goes ahead and deletes it
		public Node delete(Point2D q) {

			Boolean deleteLeft = false;
			double togo = cutVal - q.get(cutDim);

			// Determining where the node we want to delete is
			if (togo > 0)
				deleteLeft = true;
			else if (togo < 0)
				deleteLeft = false;
			else
				deleteLeft = left.find(q) != null;


			// Updating our children
			if (deleteLeft)
				left = left.delete(q);
			else
				right = right.delete(q);
			

			// We can't have empty children, so if any child is empty, it is replaced by
			// the nonempty child
			return (deleteLeft ? left : right).empty() ? (deleteLeft ? right : left) : this;

		}


		public void kNearestNeighbor(MinK<Double, LPoint> heap, Point2D center, Rectangle2D subpart) {

			Node travFirst = center.get(cutDim) < cutVal ? left : right;
			Node travSecond = travFirst == left ? right : left;

			Rectangle2D travFirstPart = travFirst == left ? subpart.leftPart(cutDim, cutVal) : subpart.rightPart(cutDim, cutVal);
			Rectangle2D travSecondPart = travSecond == left ? subpart.leftPart(cutDim, cutVal) : subpart.rightPart(cutDim, cutVal);

			travFirst.kNearestNeighbor(heap, center, travFirstPart);

			// Is it possible that there is a better distance in the other half than the furthest point in the heap?
			if (heap.getKth() > travSecondPart.distanceSq(center))
				// If so we check the other heap too
				travSecond.kNearestNeighbor(heap, center, travSecondPart);


		}

	}


	// External Node definition
	private class ExternalNode extends Node {


		// Our store of points
		ArrayList<LPoint> ndPts;

		public ExternalNode() {
			ndPts = new ArrayList<>();
		}

		// If we have no points, our node is empty
		public Boolean empty() {
			return ndPts.isEmpty();
		}

		// Generic array list find
		public LPoint find(Point2D q) {

			for (LPoint p : ndPts)
				if (p.getPoint2D().equals(q))
					return p;
			
			return null;
		}


		public Node bulkInsert(ArrayList<LPoint> pts) {

			// Add all the points to the bucket
			ndPts.addAll(pts);

			// Do nothing if bucket size not exceeded
			if (ndPts.size() <= bucketSize)
				return this;
			else {

				// This section determines cutting dimension
				Rectangle2D allpoints = new Rectangle2D(ndPts.get(0).getPoint2D(), ndPts.get(1).getPoint2D());
				
				for (LPoint p : ndPts)
					allpoints.expand(p.getPoint2D());

				int cutDim = allpoints.getWidth(0) >= allpoints.getWidth(1) ? 0 : 1;


				// Using our fancy comparison function 
				// to sort based on cutting dimension
				Collections.sort(ndPts, new PointSort(cutDim));


				// This hellish block gets the median of the sorted list for the cutting value
				double cutVal = ndPts.size() % 2 == 0 ?
								(ndPts.get(ndPts.size()/2).getPoint2D().get(cutDim) + ndPts.get(ndPts.size()/2 - 1).get(cutDim)) / 2.0 :
								ndPts.get(ndPts.size()/2).getPoint2D().get(cutDim);

				// Node we are going to replace ours with
				InternalNode tr = new InternalNode(cutDim, cutVal);

				
				// Ensuring that the node is balanced
				tr.left = tr.left.bulkInsert(new ArrayList<>(ndPts.subList(0, ndPts.size()/2)));
				tr.right = tr.right.bulkInsert(new ArrayList<>(ndPts.subList(ndPts.size()/2, ndPts.size())));


				return tr;


			}
			
		}


		public ArrayList<String> list() {

			String bConcat[] = new String[ndPts.size()];


			// I did not realise that there were toString() methods in Point2D
			// I am stupid, please take off style points for my stupidity.
			// I am also very lazy and dont feel like changing this now
			for (int i = 0; i < ndPts.size(); i++)
				bConcat[i] = "{" + ndPts.get(i).getLabel() + ": (" + ndPts.get(i).getX() + "," + 
								ndPts.get(i).getY() + ")}";


			// Making sure our strings are sorted lexiographically
			// I dont have to deal with collections.sort again, yay!
			Arrays.sort(bConcat);

			// Compiling our single output string
			String oString = "[ ";

			for (String s : bConcat)
				oString += s + " ";

			oString += ']';

			ArrayList<String> tr = new ArrayList<>();
			tr.add(oString);
			return tr;

		}
		

		// Simple delete on the array list
		public Node delete(Point2D q) {

			LPoint todelete = null;

			for (LPoint p : ndPts)
				if (p.getPoint2D().equals(q)) {
					todelete = p;
					break;
				}

			ndPts.remove(todelete);


			return this;

		}

		public void kNearestNeighbor(MinK<Double, LPoint> heap, Point2D center, Rectangle2D subpart) {

			// Simply attempts to insert all the points into the heap (if they can go in ofc)
			for (LPoint ele : ndPts)
				heap.add(center.distanceSq(ele.getPoint2D()), ele);

		}
	}



	private int bucketSize;
	private Rectangle2D box;
	private Node root;
	private int size = 0;


	// This is just a wrapper for our root node, and does some precondition 
	// checks
	public XkdTree(int bucketSize, Rectangle2D bbox) {

		this.bucketSize = bucketSize;
		this.box = bbox;

		clear();


	}


	// Simple, we reset our root to be an empty ext node, and GC does the rest
	public void clear() {
		root = new ExternalNode();
		size = 0;
	}

	public int size() {
		return size;
	}

	public LPoint find(Point2D q) {
		return root.find(q);
	}

	public void insert(LPoint pt) throws Exception {
		
		ArrayList<LPoint> ts = new ArrayList<>();
		ts.add(pt);
		bulkInsert(ts);
		
	}
	
	public void bulkInsert(ArrayList<LPoint> pts) throws Exception {

		// Precondition checking
		for (LPoint p : pts)
			if(!box.contains(p.getPoint2D()))
				throw new Exception("Attempt to insert a point outside bounding box");
		
		root = root.bulkInsert(pts);

		size += pts.size();

	}

	public ArrayList<String> list() {
		return root.list();
	}


	// Our NN is just a kNN of k = 1
	public LPoint nearestNeighbor(Point2D center) {
		try {
			return kNearestNeighbor(center, 1).get(0);
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}


	public void delete(Point2D pt) throws Exception {

		if (this.find(pt) == null)
			throw new Exception("Deletion of nonexistent point");

		root = root.delete(pt);
		size--;
		
	}



	public ArrayList<LPoint> kNearestNeighbor(Point2D center, int k) {


		// Optimization trick: If we know that there are going to be less points in the tree than k, why expend the work
		// of initializing a heap of more than k?
		MinK<Double, LPoint> mk = new MinK<Double, LPoint>(Math.min(k, size), Double.POSITIVE_INFINITY);


		// Passing heap and center refs down the nodes
		root.kNearestNeighbor(mk, center, this.box);

		// returing sorted list
		return mk.list();

	}
}






