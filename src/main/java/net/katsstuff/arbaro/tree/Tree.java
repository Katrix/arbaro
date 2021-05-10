package net.katsstuff.arbaro.tree;

import java.io.PrintWriter;
import net.katsstuff.arbaro.transformation.Vector;

/**
 * The tree interface to access a tree from outside of this package
 *
 * @author wolfram
 */

public interface Tree {

	/**
	 * used with the TreeTraversal interface
	 *
	 * @return when false stop travers the tree
	 */
	boolean traverseTree(TreeTraversal traversal);

	/**
	 * @return the number of all stems of all levels of the tree
	 */
	long getStemCount();

	/**
	 * @return the number of leaves of the tree
	 */
	long getLeafCount();

	/**
	 * @return a vector with the highest coordinates of the tree. (Considering all stems of all levels)
	 */
	Vector getMaxPoint();

	/**
	 * @return a vector with the lowest coordinates of the tree. (Considering all stems of all levels)
	 */
	Vector getMinPoint();

	/**
	 * @return the seed of the tree. It is used for randomnization.
	 */
	int getSeed();

	/**
	 * @return the height of the tree (highest z-coordinate)
	 */
	double getHeight();

	/**
	 * @return the widht of the tree (highest value of sqrt(x*x+y*y))
	 */
	double getWidth();

	/**
	 * Writes the trees parameters to a stream
	 */
	void paramsToXML(PrintWriter w);

	/**
	 * @return the tree species name
	 */
	String getSpecies();

	/**
	 * @return the tree scale
	 */
	double getScale();

	/**
	 * @return the tree stem levels
	 */
	int getLevels();

	/**
	 * @return the leaf shape name
	 */
	String getLeafShape();

	/**
	 * @return the leaf width
	 */
	double getLeafWidth();

	/**
	 * @return the leaf length
	 */
	double getLeafLength();

	/**
	 * @return the virtual leaf stem length, i.e. the distance of the leaf from the stem center line
	 */
	double getLeafStemLength();

	/**
	 * Use this for verbose output when generating a mesh or exporting a tree
	 *
	 * @return an information string with the number of section points for this stem level and if smoothing should be
	 * 	used
	 */
	String getVertexInfo(int level);
}
