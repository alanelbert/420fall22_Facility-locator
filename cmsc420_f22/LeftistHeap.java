package cmsc420_f22; // Do not delete this line

import java.util.ArrayList;

public class LeftistHeap<Key extends Comparable<Key>, Value> {


	// Class that contains definitions for a LHEAP Node
	protected class Node {
		Key k;
		Value v;
		Node left;
		Node right;
		int npl;

		public Node(Key k, Value v) {
			this.k = k;
			this.v = v;
			left = null;
			right = null;
			npl = 0;
		}
	}

	// Root of the tree
	Node root;

	// Inits the class by setting root to null
	public LeftistHeap() {
		clear();
		
	}

	// Gets the root for LH merging
	protected Node getRoot() {
		return root;
	}

	// If root is null heap is empty
	public boolean isEmpty() {
		return root == null;
	}

	// Sets root to null and lets GC do its thing
	public void clear() {
		root = null;
	}


	// Inserts a node by declaring a LH of size one and using merge function
	public void insert(Key x, Value v) {
		root = merge(root, new Node(x, v));
	}

	
	// Function for merging two LHs, implemented as per lecture
	protected Node merge(Node u, Node v) {
		if (u == null)
			return v;
		if (v == null)
			return u;
		
		Node temp;

		if (u.k.compareTo(v.k) >= 0) {
			temp = u;
			u = v;
			v = temp;
		}
			 

		if (u.left == null)
			u.left = v;
		else {
			u.right = merge(u.right, v);

			if (u.left.npl < u.right.npl) {
				temp = u.right;
				u.right = u.left;
				u.left = temp;
			}

			u.npl = u.right.npl + 1;
		}

		return u;
		
	}
	

	// Merge but with extra steps and checks for merging leftist heaps
	public void mergeWith(LeftistHeap<Key, Value> h2) {

		if (h2 == this)
			return;
		
		root = merge(root, h2.root);
		h2.root = null;



	}


	// Recursive split helper
	private Node splitHelper(Node root, Key x, ArrayList<Node> ar) {


		if (root == null)
			return null;

		
		if (x.compareTo(root.k) >= 0) {

			// Handling node unlinking, if null is returned then the node must be part of the new heap
			root.left = splitHelper(root.left, x, ar);
			root.right = splitHelper(root.right, x, ar);
			return root;

		} else {

			// Adding nodes to the AR
			ar.add(root);
			return null;
		}

	}


	// Getting the npl for a node without risking null pointer exceptions
	private int getNPL(Node n) {
		return n == null ? -1 : n.npl;
	}

	// Postorder Recurses through and fixes the leftist property of a tree incase it is broken,
	// For example via a split
	private void fixLeftistProperty(Node root) {

		if (root == null)
			return;

		fixLeftistProperty(root.left);
		fixLeftistProperty(root.right);

		if (getNPL(root.left) < getNPL(root.right)) {
			Node temp = root.left;
			root.left = root.right;
			root.right = temp;
		}

		root.npl = getNPL(root.right) + 1;

	}

	public LeftistHeap<Key, Value> split(Key x) {

		// Our store of nodes
		ArrayList<Node> nodeStore = new ArrayList<>();

		root = splitHelper(root, x, nodeStore);

		LeftistHeap<Key, Value> h2 = new LeftistHeap<>();

		// Merging all LHs into one
		for (Node node : nodeStore) {
			
			h2.root = h2.merge(h2.root, node);
		}


		// Repairing our original LH
		fixLeftistProperty(root);


		return h2;
	}

	// Gets min Key
	public Key getMinKey()  {
		return isEmpty() ? null : root.k;
	}



	// Gets min and removes it
	public Value extractMin() throws Exception { 

		if (isEmpty())
			throw new Exception("Empty heap");

		Value temp = root.v;

		// Merges left and right nodes in order to form new LH
		root = merge(root.left, root.right);

		return temp;

	
	}

	// Preorder RL traversal with a shared pointer to an arraylist
	private void listHelper(Node croot,ArrayList<String> ar) {
		if (croot == null)  {
			ar.add("[]");
			return;
		}

		ar.add("(" + croot.k + ", " + croot.v + ")" + " [" + croot.npl + "]");

		listHelper(croot.right, ar);
		listHelper(croot.left, ar);
	}

	public ArrayList<String> list() {
		ArrayList<String> tr = new ArrayList<>();

		listHelper(root, tr);

		return tr;

	}
}
