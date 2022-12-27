package cmsc420_f22; // Do not delete this line



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


// Alan Elbert
// Implementation of the MinK datastructure

public class MinK<Key extends Comparable<Key>, Value> {


	// Special class for keeping the keys and values together in the heap
	private class KeyValueBox {
		
		public Key k;
		public Value v;

		public KeyValueBox(Key k, Value v) {
			this.k = k;
			this.v = v;
		}

	}

	Comparator<KeyValueBox> kvbComp;

	private int k;
	private KeyValueBox maxKeyValue;

	private ArrayList<KeyValueBox> heap;
	private int size;



	// Simple methods for getting the index of the left/right child during a siftdown
	private int ilChild(int i) { return (i * 2) + 1; }
	private int irChild(int i) { return (i * 2) + 2; }


	// Simple methods for getting an out of bounds protected left/right child (we will have imaginary leaf null nodes that are smaller than
	// all other nodes, to make insertions go more smoothly and save us some edge case checks)
	private KeyValueBox lChild(int i) { return ilChild(i) >= k ? null : heap.get(ilChild(i)); }
	private KeyValueBox rChild(int i) { return irChild(i) >= k ? null : heap.get(irChild(i)); }

	public MinK(int k, Key maxKey) {

		this.k = k;
		this.maxKeyValue = new KeyValueBox(maxKey, null);



		// Our custom compare function for comparing Key Value boxes,
		// We treat MaxKey boxes as greater than all other boxes, and out of bounds (null)
		// boxes as being smaller than all other boxes
		kvbComp = new Comparator<MinK<Key,Value>.KeyValueBox>() {


			public int compare(KeyValueBox a, KeyValueBox b) {

				// Checking for equality for all of our special nodes (null and maxKeyValue)
				if (a == b)
					return 0;
				
				if (a == maxKeyValue || b == null)
					return 1;
				
				if (b == maxKeyValue || a == null)
					return -1;
				
				return a.k.compareTo(b.k);	

			}
		};
		

		// Clear reinitializes everything, so we just have to call it
		clear();

		
	}


	public int size() { return size; }

	public void clear() {


		// Reinits the arraylist and refills it with max value
		// The reason the array list is full of max value by default is so getKth returns infinity whenever
		// the heap is not full
		heap = new ArrayList<>(k);

		for (int i = 0; i < k; i++)
			heap.add(maxKeyValue);
		
		size = 0;

	}

	public Key getKth() {
		return heap.get(0).k;
	}

	public void add(Key x, Value v) {

		// Our array cant grow beyond k,
		// however every time an add occurs the max key is replaced if there
		// are max keys still in the array
		size = Math.min(k, size + 1);

		KeyValueBox nkvb = new KeyValueBox(x, v);

		
		if (k > 0 && kvbComp.compare(nkvb, heap.get(0)) < 0) {

			heap.set(0, nkvb);
			siftDown(0);

		}


	}

	// Simple siftdown, if any of the children are greater, swap with the greatest of the children and recurse down the tree
	private void siftDown(int i) {
		if (kvbComp.compare(heap.get(i), lChild(i)) < 0 || kvbComp.compare(heap.get(i), rChild(i)) < 0) {

			int toSwap = kvbComp.compare(lChild(i), rChild(i)) >= 0 ? ilChild(i) : irChild(i);
			Collections.swap(heap, i, toSwap);	
			siftDown(toSwap);
		}
	}

	public ArrayList<Value> list() {

		int savedK = k;

		// Simple heapsort, we swap with k - 1 and decrement
		while (k > 1) {
			Collections.swap(heap, k - 1, 0);
			k--;
			siftDown(0);
		}
			
		// We restore k once heapsorted
		k = savedK;

		

		ArrayList<Value> tr = new ArrayList<>();


		for (KeyValueBox kvb : heap)
			if (kvb == maxKeyValue)
				break;
			else
				tr.add(kvb.v);

		
		// Reverse the sorted heap, restoring the max heap property
		Collections.reverse(heap);
		
		return tr;	
	}
}
