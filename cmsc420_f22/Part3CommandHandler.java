package cmsc420_f22;

// YOU SHOULD NOT MODIFY THIS FILE

import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Command handler. This reads a single command line, processes the command (by
 * invoking the appropriate method(s) on the data structure), and returns the
 * result as a string.
 */

public class Part3CommandHandler {

	private boolean initialized; // have we initialized the structure yet?
	private XkdTree<Airport> kdTree; // the kd-tree
	private HashMap<String, Airport> airports; // airport codes seen so far
	private ArrayList<Airport> pendingBulkInsert; // waiting to be inserted
	private MinK<Double, String> minK; // minK structure - just for testing
	private KCapFL<Airport> kCapCluster; // k-capacitated facility locator


	private Visualizer viz; // Our visualizer

	/**
	 * Initialize command handler
	 */
	public Part3CommandHandler() {
		initialized = false;
		kdTree = null;
		airports = new HashMap<String, Airport>();
		pendingBulkInsert = new ArrayList<Airport>();
		kCapCluster = null;
	}

	/**
	 * Process a single command and return the string output. Each command begins
	 * with a command followed by a list of arguments. The arguments are separated
	 * by colons (":").
	 */

	public String processCommand(String inputLine) throws Exception {
		String output = new String(); // for storing summary output
		Scanner line = new Scanner(inputLine);
		try {
			line.useDelimiter(":"); // use ":" to separate arguments
			String cmd = (line.hasNext() ? line.next() : ""); // next command
			// -----------------------------------------------------
			// INITIALIZE
			// - this command must come first in the input
			// - sets the bounding box
			// -----------------------------------------------------
			if (cmd.compareTo("initialize") == 0) {
				int bucketSize = line.nextInt(); // bucket size
				double xMin = line.nextDouble(); // bounding box
				double xMax = line.nextDouble();
				double yMin = line.nextDouble();
				double yMax = line.nextDouble();
				int capacity = line.nextInt(); // minK capacity
				if (bucketSize <= 0 || capacity <= 0) {
					throw new Exception("Error - bucket size and capacity must be positive");
				}
				if (xMin > xMax || yMin > yMax) {
					throw new Exception("Error - invalid bounding box dimensions");
				}
				if (initialized) {
					throw new Exception("Error - Structure already initialized");
				} else {
					Rectangle2D bbox = new Rectangle2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
					kdTree = new XkdTree<Airport>(bucketSize, bbox); // create a new tree
					minK = new MinK<Double, String>(capacity, Double.POSITIVE_INFINITY);
					kCapCluster = new KCapFL<Airport>(capacity, bucketSize, bbox);
					output += "initialize: bucket-size = " + bucketSize + " bounding-box = " + bbox + " capacity = "
							+ capacity + System.lineSeparator();
					initialized = true;

					
					viz = new Visualizer(bbox, kdTree);
				}
			}
			// -----------------------------------------------------
			// COMMENT string
			// - comment line for the output
			// -----------------------------------------------------
			else if (cmd.compareTo("comment") == 0) {
				String comment = line.next(); // read the comment
				output += "[" + comment + "]" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// INSERT code city x y
			// - insert a point
			// -----------------------------------------------------
			else if (cmd.compareTo("insert") == 0) {
				confirmInitialized(); // confirm that we are initialized
				String code = line.next(); // get parameters
				String city = line.next();
				double x = line.nextDouble();
				double y = line.nextDouble();
				Airport ap = new Airport(code, city, x, y); // create airport object
				output += "insert(" + code + "): ";
				Airport ap2 = airports.get(code);
				if (ap2 != null) { // code already exists?
					throw new Exception("Insertion of duplicate airport code");
				}
				kdTree.insert(ap); // insert into kd-tree
				airports.put(code, ap); // add to dictionary
				output += "successful {" + ap.getString("attributes") + "}" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// INSERT-LATER code city x y
			// - store for later insertion
			// -----------------------------------------------------
			else if (cmd.compareTo("insert-later") == 0) {
				confirmInitialized(); // confirm that we are initialized
				String code = line.next(); // get parameters
				String city = line.next();
				double x = line.nextDouble();
				double y = line.nextDouble();
				Airport ap = new Airport(code, city, x, y); // create airport object
				output += "insert-later(" + code + "): ";
				Airport ap2 = airports.get(code);
				if (ap2 != null) { // code already exists?
					throw new Exception("Insertion of duplicate airport code");
				} else {
					airports.put(ap.getCode(), ap); // add to dictionary
				}
				pendingBulkInsert.add(ap); // add to waiting list
				output += "okay {" + ap.getString("attributes") + "}" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// BULK-INSERT
			// - apply all the pending insertions
			// -----------------------------------------------------
			else if (cmd.compareTo("bulk-insert") == 0) {
				confirmInitialized(); // confirm that we are initialized
				int m = pendingBulkInsert.size(); // number to insert
				if (m < 1) {
					throw new Exception("bulk-insert needs at least one point");
				}
				output += "bulk-insert[" + m + "]: ";
				kdTree.bulkInsert(pendingBulkInsert); // insert all into kd-tree
				pendingBulkInsert.clear(); // clear the list
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// DELETE
			// - delete a point given its code
			// -----------------------------------------------------
			else if (cmd.compareTo("delete") == 0) {
				confirmInitialized(); // confirm that we are initialized
				String code = line.next(); // get parameters
				output += "delete(" + code + "): ";
				Airport ap = airports.get(code); // look up the airport
				if (ap == null) { // no such airport?
					throw new Exception("Deletion of nonexistent airport code");
				}
				kdTree.delete(ap.getPoint2D()); // delete from kd-tree
				airports.remove(code); // delete from dictionary
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// DELETE-POINT
			// - delete a point given its coordinates
			// -----------------------------------------------------
			else if (cmd.compareTo("delete-point") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double x = line.nextDouble();
				double y = line.nextDouble();
				output += "delete-point(" + x + "," + y + "): ";
				kdTree.delete(new Point2D(x, y)); // delete from kd-tree
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// CLEAR
			// -----------------------------------------------------
			else if (cmd.compareTo("clear") == 0) {
				confirmInitialized(); // confirm that we are initialized
				kdTree.clear(); // clear the kd-tree
				airports.clear(); // clear the airports map
				output += "clear: successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// SIZE
			// -----------------------------------------------------
			else if (cmd.compareTo("size") == 0) {
				confirmInitialized(); // confirm that we are initialized
				int size = kdTree.size(); // get the tree's current size
				output += "size: " + size + System.lineSeparator();
			}
			// -----------------------------------------------------
			// FIND code
			// -----------------------------------------------------
			else if (cmd.compareTo("find") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double x = line.nextDouble();
				double y = line.nextDouble();
				Point2D q = new Point2D(x, y);
				Airport result = kdTree.find(q);
				output += summarizeFind(q, result); // summarize result

				/*Visualizer code, highlighting found point */
				if (result != null)
					viz.passKNN(new ArrayList<>(), q);
			}
			// -----------------------------------------------------
			// NEAREST-NEIGHBOR
			// Find the nearest neighbor to a query point
			// -----------------------------------------------------
			else if (cmd.compareTo("nearest-neighbor") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double x = line.nextDouble();
				double y = line.nextDouble();
				Point2D q = new Point2D(x, y);
				Airport result = kdTree.nearestNeighbor(q);
				output += summarizeNNSearch(q, result); // summarize result

				/*Visualizer code, showing knn circle */

				if (result != null) {
					ArrayList<LabeledPoint2D> tp = new ArrayList<LabeledPoint2D>();
					tp.add(result);
					viz.passKNN(tp, q);
				}

			}
			// -----------------------------------------------------
			// K-NEAREST-NEIGHBOR
			// Find the k nearest neighbors to a query point
			// -----------------------------------------------------
			else if (cmd.compareTo("k-nearest-neighbor") == 0) {
				confirmInitialized(); // confirm that we are initialized
				int k = line.nextInt();
				if (k <= 0) {
					throw new Exception("For k-nearest-neighbor, k must be at least 1");
				}
				double x = line.nextDouble();
				double y = line.nextDouble();
				Point2D q = new Point2D(x, y);
				ArrayList<Airport> result = kdTree.kNearestNeighbor(q, k);
				output += summarizeKNNSearch(q, k, result); // summarize result

				viz.passKNN(new ArrayList<LabeledPoint2D>(result), q);
			}
			// -----------------------------------------------------
			// LIST - get a preorder list of entries and print
			// the tree with indentation
			// -----------------------------------------------------
			else if (cmd.compareTo("list") == 0) {
				confirmInitialized(); // confirm that we are initialized
				ArrayList<String> list = kdTree.list();
				if (list == null)
					throw new Exception("Error - list returned a null result");
				Iterator<String> iter = list.iterator(); // iterator for the list
				output += "list:" + System.lineSeparator();
				while (iter.hasNext()) { // output the preorder list (flat)
					output += "  " + iter.next() + System.lineSeparator();
				}
				output += treeStructure(list); // summarize tree contents (indented)
			}
			// -----------------------------------------------------
			// MINK-ADD key value
			// - add pair to minK structure
			// -----------------------------------------------------
			else if (cmd.compareTo("minK-add") == 0) {
				confirmInitialized(); // confirm that we are initialized
				Double key = line.nextDouble(); // get parameters
				String value = line.next();
				output += "minK-add(" + key + "," + value + "): ";
				minK.add(key, value); // add to the minK structure
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// MINK-CLEAR
			// -----------------------------------------------------
			else if (cmd.compareTo("minK-clear") == 0) {
				confirmInitialized(); // confirm that we are initialized
				minK.clear(); // clear the minK structure
				output += "minK-clear: successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// MINK-GET-KTH
			// -----------------------------------------------------
			else if (cmd.compareTo("minK-get-kth") == 0) {
				confirmInitialized(); // confirm that we are initialized
				Double result = minK.getKth();
				output += summarizeGetKth(result); // summarize result
			}
			// -----------------------------------------------------
			// MINK-LIST
			// -----------------------------------------------------
			else if (cmd.compareTo("minK-list") == 0) {
				confirmInitialized(); // confirm that we are initialized
				ArrayList<String> list = minK.list();
				if (list == null)
					throw new Exception("Error - list returned a null result");
				Iterator<String> iter = list.iterator(); // iterator for the list
				output += "minK-list:" + System.lineSeparator();
				while (iter.hasNext()) { // output the list
					output += "  " + iter.next() + System.lineSeparator();
				}
			}
			// -----------------------------------------------------
			// KFL-BUILD
			// - build the kcc structure from pending points
			// -----------------------------------------------------
			else if (cmd.compareTo("kfl-build") == 0) {
				confirmInitialized(); // confirm that we are initialized
				int m = pendingBulkInsert.size(); // number to insert
				if (m < 1) {
					throw new Exception("kfl-build needs at least one point");
				}
				output += "kfl-build[" + m + "]: ";
				kCapCluster.build(pendingBulkInsert); // insert all into kd-tree
				pendingBulkInsert.clear(); // clear the list
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// KFL-EXTRACT-CLUSTER
			// - extract the next service cluster
			// -----------------------------------------------------
			else if (cmd.compareTo("kfl-extract-cluster") == 0) {
				confirmInitialized(); // confirm that we are initialized
				ArrayList<Airport> result = kCapCluster.extractCluster();
				if (result == null) {
					output += "kfl-extract-cluster: [No more points remaining]" + System.lineSeparator();
				} else {
					output += "kfl-extract-cluster:" + System.lineSeparator();
					for (int i = 0; i < result.size(); i++) { // output list of points
						double sqDist = result.get(0).getPoint2D().distanceSq(result.get(i).getPoint2D());
						output += "  " + result.get(i).getString("full") + " sqDist = " + sqDist
								+ System.lineSeparator();
					}
				}
			}
			// -----------------------------------------------------
			// KFL-LIST-KD-TREE
			// - list the kd-tree structure
			// -----------------------------------------------------
			else if (cmd.compareTo("kfl-list-kd-tree") == 0) {
				confirmInitialized(); // confirm that we are initialized
				ArrayList<String> list = kCapCluster.listKdTree();
				if (list == null)
					throw new Exception("Error - kCapCluster.listKdTree returned a null result");
				output += "kfl-list-kd-tree:" + System.lineSeparator();
				for (String s : list) { // output the list
					output += "  " + s + System.lineSeparator();
				}
				output += treeStructure(list); // summarize tree contents (indented)
			}
			// -----------------------------------------------------
			// KFL-LIST-HEAP
			// - list the heap contents
			// -----------------------------------------------------
			else if (cmd.compareTo("kfl-list-heap") == 0) {
				ArrayList<String> list = kCapCluster.listHeap();
				if (list == null) {
					throw new Exception("kCapCluster.listHeap returned a null result");
				}
				output += "kfl-list-heap:" + System.lineSeparator();
				for (String s : list) {
					output += "  " + s + System.lineSeparator();
				}
				output += treeStructure(list); // summarize heap contents (indented)
			}
			// -----------------------------------------------------
			// Invalid command or empty
			// -----------------------------------------------------
			else {
				if (cmd.compareTo("") == 0)
					System.err.println("Error: Empty command line (Ignored)");
				else
					System.err.println("Error: Invalid command - \"" + cmd + "\" (Ignored)");
			}
			line.close();

			/*Visualizer code:*/
			if (viz != null)
				viz.update(inputLine);

		} catch (Exception e) { // exception thrown?
			if (e.getMessage() == null) {
				output += "Failure due to unexpected exception (probably runtime error)" + System.lineSeparator();
			} else {
				output += "Failure due to exception: \"" + e.getMessage() + "\"" + System.lineSeparator();
			}
			e.printStackTrace(System.err); // print stack trace
			pendingBulkInsert.clear(); // clear out any pending points
		} catch (Error e) { // error occurred?
			System.err.print("Operation failed due to error: " + e.getMessage());
			e.printStackTrace(System.err);
		} finally { // always executed
			line.close(); // close the input scanner
		}

		

		return output; // return summary output
	}

	/**
	 * Confirm that the data structure has been initialized, or throw an exception.
	 */
	void confirmInitialized() throws Exception {
		if (!initialized) {
			throw new Exception("Error: First command must be 'initialize'.");
		}
	}

	/**
	 * Summarize the results of a find command.
	 */
	static String summarizeFind(Point2D q, Airport result) {
		String output = new String("find" + q + ": ");
		if (result != null) {
			output += result.getLabel() + System.lineSeparator();
		} else {
			output += "[not found]" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Summarize the results of a search command.
	 */
	static String summarizeNNSearch(Point2D q, Airport result) {
		String output = new String("nearest-neighbor" + q + ": ");
		if (result != null) {
			output += result.getLabel() + " (d=" + q.distanceSq(result.getPoint2D()) + ")" + System.lineSeparator();
		} else {
			output += "[not found]" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Summarize the results of a k-nearest-neighbor search command.
	 */
	static String summarizeKNNSearch(Point2D q, int k, ArrayList<Airport> result) {
		String output = new String("k-nearest-neighbor(" + k + "," + q + ") = [");
		for (Airport ap : result) {
			output += " " + ap.getLabel() + " (d=" + q.distanceSq(ap.getPoint2D()) + ")";
		}
		output += " ]" + System.lineSeparator();
		return output;
	}

	/**
	 * Summarize the results of minK getKth.
	 */
	static String summarizeGetKth(Double result) {
		String output = new String("minK-get-kth: ");
		if (result == Double.POSITIVE_INFINITY) {
			output += "[infinity]" + System.lineSeparator();
		} else {
			output += result + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Print the tree contents with indentation.
	 */
	static String treeStructure(ArrayList<String> entries) {
		String output = "Tree structure:" + System.lineSeparator();
		Iterator<String> iter = entries.iterator(); // iterator for the list
		if (iter.hasNext()) { // tree is nonempty
			output += treeStructureHelper(iter, "  "); // print everything
		}
		return output;
	}

	/**
	 * Recursive helper for treeStructure. The argument iterator specifies the next
	 * node from the preorder list to be printed, and the argument indent indicates
	 * the indentation to be performed (of the form "| | | ...").
	 */
	static String treeStructureHelper(Iterator<String> iter, String indent) {
		final String levelIndent = "| "; // the indentation for each level of the tree
		String output = "";
		if (iter.hasNext()) {
			String entry = iter.next(); // get the next entry
			Boolean isExtern = (entry.length() > 0 && entry.charAt(0) == '['); // external?
			if (isExtern) { // print external node entry
				output += indent + entry + System.lineSeparator();
			} else {
				output += treeStructureHelper(iter, indent + levelIndent); // print right subtree
				output += indent + entry + System.lineSeparator(); // print this node
				output += treeStructureHelper(iter, indent + levelIndent); // print left subtree
			}
		} else {
			System.err.println("Unexpected trailing elements in entries list"); // shouldn't get here!
		}
		return output;
	}

	/*Closing Window when done */
	public void closeWindow() {
		if (viz != null)
			viz.windowClosing(null);
	}
}
