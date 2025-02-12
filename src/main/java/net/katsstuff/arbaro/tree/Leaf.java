package net.katsstuff.arbaro.tree;

import net.katsstuff.arbaro.transformation.Transformation;

/**
 * The leaf interface for accessing a tree's leaf from outside of the tree generator, e.g. for mesh generation and
 * exporting
 *
 * @author wolfram
 */

public interface Leaf {

	/**
	 * used with TreeTraversal interface
	 *
	 * @return when false stop travers tree at this level
	 */
	boolean traverseTree(TreeTraversal traversal);

	/**
	 * @return the leaf's transformation matrix containing the position vector and the rotation matrix.
	 */
	Transformation getTransformation();
}