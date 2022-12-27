package cmsc420_f22;

import java.util.ArrayList;

// Alan Elbert
// Init of KCapFL

public class KCapFL<LPoint extends LabeledPoint2D> {
    
    private int capacity;
    private XkdTree<LPoint> kdTree;
    private LeftistHeap<Double, ArrayList<LPoint>> heap;



    public KCapFL(int capacity, int bucketSize, Rectangle2D bbox) {

        kdTree = new XkdTree<LPoint>(bucketSize, bbox);
        this.capacity = capacity;
        heap = new LeftistHeap<>();
    }

    public void clear() {

        kdTree.clear();
        heap.clear();

    }


    // Gets KNN of size for all points in the tree and builds a heap
    public void build(ArrayList<LPoint> pts) throws Exception {
        
        if (pts.size() == 0 || pts.size() % capacity != 0)
            throw new Exception("Invalid point set size");
        
        kdTree.bulkInsert(pts);

        for (LPoint p : pts) {
            ArrayList<LPoint> knn = kdTree.kNearestNeighbor(p.getPoint2D(), capacity);

            heap.insert(knn.get(capacity - 1).getPoint2D()
            .distanceSq(p.getPoint2D()), knn);
        }
    }


    // Extracts a cluster recursively
    public ArrayList<LPoint> extractCluster() throws Exception {

        if (kdTree.size() == 0)
            return null;

        ArrayList<LPoint> min = heap.extractMin();

        Boolean allInHeap = true;

        for (LPoint p : min) {

            if (kdTree.find(p.getPoint2D()) == null) {
                allInHeap = false;
                break;
            }

        }

        // If all points in heap, delete all and return
        if (allInHeap) {

            for (LPoint todel : min)
                kdTree.delete(todel.getPoint2D());

            return min;
        }
        

        // Otherwise recompute minK for node if min in tree, and reinsert if root in tree
        if (kdTree.find(min.get(0).getPoint2D()) != null) {
            ArrayList<LPoint> knn = kdTree.kNearestNeighbor(min.get(0).getPoint2D(), capacity);

            heap.insert(knn.get(capacity - 1).getPoint2D()
            .distanceSq(min.get(0).getPoint2D()), knn);

            
        }

        // We re run code until a cluster is extracted or null is returned
        return extractCluster();
    }

    public ArrayList<String> listKdTree() {
        return kdTree.list();
    }

    public ArrayList<String> listHeap() {
        return heap.list();
    }

}




