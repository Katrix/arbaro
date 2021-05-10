package net.katsstuff.arbaro.tree;

import net.katsstuff.arbaro.transformation.Transformation;
import net.katsstuff.arbaro.transformation.Vector;

/**
 * The stem interface used from outside the tree generator, e.g. for mesh creation and exporting
 *
 * @author wolfram
 */
public interface Stem {

	/**
	 * @return an enumeration of the stems sections
	 */
	java.util.Enumeration sections();

	/**
	 * @return an section offset for clones, because uv-Coordinates should be at same coordinates for stems and theire
	 * 	clones
	 */
	int getCloneSectionOffset();

	/**
	 * a vector with the smalles coordinates of the stem
	 */
	Vector getMinPoint();

	/**
	 * a vector with the heighest coordinates of the stem
	 */
	Vector getMaxPoint();

	/**
	 * The position of the stem in the tree. 0.1c2.3 means: fourth twig of the third clone of the second branch growing
	 * out of the first (only?) trunk
	 *
	 * @return The stem position in the tree as a string
	 */
	String getTreePosition();

	/**
	 * @return the stem length
	 */
	double getLength();

	/**
	 * @return the radius at the stem base
	 */
	double getBaseRadius();

	/**
	 * @return the radius at the stem peak
	 */
	double getPeakRadius();

	/**
	 * @return the stem level, 0 means it is a trunk
	 */
	int getLevel();

	/**
	 * used with TreeTraversal interface
	 *
	 * @return when false stop traverse tree at this level
	 */
	boolean traverseTree(TreeTraversal traversal);

	/**
	 * @return the number leaves of the stem
	 */
	long getLeafCount();

	/**
	 * @return true, if this stem is a clone of another stem
	 */
	boolean isClone();

	/**
	 * @return this stem should be smoothed, so output normals for Povray meshes
	 */
	boolean isSmooth();

	/**
	 * @return the transformation of the stem, containing the position vector and the rotation matrix of the stem base
	 */
	Transformation getTransformation();
}