package net.katsstuff.arbaro.tree;

import net.katsstuff.arbaro.transformation.Transformation;
import net.katsstuff.arbaro.transformation.Vector;

/**
 * A stem seen from outside is made from several section, each consisting of a circle of section points. A mesh
 * generator can iterate over the stem sections creating a vertex for each section point and connecting adjacent
 * sections with faces (triangles or quads)
 *
 * @author wolfram
 */
public interface StemSection {

	/**
	 * @return the midpoint of the section
	 */
	Vector getPosition();

	/**
	 * @return the radius of the section (point distance from midpoint can vary when lobes are used)
	 */
	double getRadius();

	/**
	 * @return relative distance from stem origin
	 */
	double getDistance();

	/**
	 * @return the transformation for the section, giving it's position vector and rotation matrix
	 */
	Transformation getTransformation();

	/**
	 * @return the z-direction vector, it is orthogonal to the section layer
	 */
	Vector getZ();

	/**
	 * @return the vertex points of the section. It's number is influenced by the stem level and smooth value. It's
	 * 	distance from midpoint can vary about the radius (when lobes are used).
	 */
	Vector[] getSectionPoints();
}
