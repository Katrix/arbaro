//  #**************************************************************************
//  #
//  #    Copyright (C) 2003-2006  Wolfram Diestel
//  #
//  #    This program is free software; you can redistribute it and/or modify
//  #    it under the terms of the GNU General Public License as published by
//  #    the Free Software Foundation; either version 2 of the License, or
//  #    (at your option) any later version.
//  #
//  #    This program is distributed in the hope that it will be useful,
//  #    but WITHOUT ANY WARRANTY; without even the implied warranty of
//  #    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  #    GNU General Public License for more details.
//  #
//  #    You should have received a copy of the GNU General Public License
//  #    along with this program; if not, write to the Free Software
//  #    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  #
//  #    Send comments and bug fixes to diestel@steloj.de
//  #
//  #**************************************************************************/

package net.katsstuff.arbaro.tree;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import net.katsstuff.arbaro.export.Console;
import net.katsstuff.arbaro.params.LevelParams;
import net.katsstuff.arbaro.params.Params;
import net.katsstuff.arbaro.transformation.Transformation;
import net.katsstuff.arbaro.transformation.Vector;

class ArbaroException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ArbaroException(String errmsg) {
		super(errmsg);
	}
}

class NotYetImplementedError extends ArbaroException {

	private static final long serialVersionUID = 1L;

	public NotYetImplementedError(String errmsg) {
		super(errmsg);
	}
}


/**
 * This class makes a stem (trunk or branch). Most of the Jason&Weber tree generation algorithm is here.
 *
 * @author Wolfram Diestel
 */
class StemImpl implements Stem {

	TreeImpl tree;
	Params par;
	LevelParams lpar;
	StemImpl parent; // the parent stem
	StemImpl clonedFrom = null; // the stem, from which this clone spreads out

	Transformation transf;

	public Transformation getTransformation() {
		return transf;
	}

	Vector minPoint;
	Vector maxPoint;

	/* (non-Javadoc)
	 * @see net.katsstuff.arbaro.tree.TraversableStem#getMinPoint()
	 */
	public Vector getMinPoint() {
		return minPoint;
	}

	/* (non-Javadoc)
	 * @see net.katsstuff.arbaro.tree.TraversableStem#getMaxPoint()
	 */
	public Vector getMaxPoint() {
		return maxPoint;
	}

	// stems shouldn't be shorter than 1/2 mm,
	// and taller than 0.05 mm - otherwise
	// the mesh will be corrupted
	// if you like smaller plants you should
	// design them using cm or mm instead of m
	//final static double MIN_STEM_LEN=0.0005;
	final static double MIN_STEM_LEN = 0.00001;
	final static double MIN_STEM_RADIUS = MIN_STEM_LEN / 10;

	public int stemlevel; // the branch level, could be > 4
	double offset; // how far from the parent's base

	java.util.Vector segments; // the segments forming the stem
	java.util.Vector clones;      // the stem clones (for splitting)
	java.util.Vector substems;    // the substems
	java.util.Vector leaves;     // the leaves

	double length;

	public double getLength() {
		return length;
	}

	double segmentLength;
	int segmentCount;
	double baseRadius;

	double lengthChildMax;
	double substemsPerSegment;
	double substemRotangle;

	double leavesPerSegment;
	double splitCorrection;

	boolean pruneTest; // flag for pruning cycles

	int index; // substem number
	java.util.Vector cloneIndex; // clone number (Integers)

	private static class SectionsEnumerator implements Enumeration {

		private final Enumeration segments;
		private Enumeration subsegments;

		public SectionsEnumerator(StemImpl stem) {
			segments = stem.segments.elements();
		}

		public boolean hasMoreElements() {
			return (subsegments != null && subsegments.hasMoreElements()) ||
				   (segments.hasMoreElements());
		}

		public Object nextElement() {
			if (subsegments == null) {
				// first segment, return it as base section
				SegmentImpl s = (SegmentImpl) segments.nextElement();
				subsegments = s.subsegments.elements();
//				subsegments.nextElement(); // ignore first subsegment,
				// because it's identical with segment base
				return s;
			} else {
				if (subsegments.hasMoreElements()) {
					return subsegments.nextElement();
				} else if (segments.hasMoreElements()) {
					SegmentImpl s = (SegmentImpl) segments.nextElement();
					subsegments = s.subsegments.elements();
//					subsegments.nextElement(); // ignore first subsegment,
					// because it's identical with segment base
					return subsegments.nextElement();
				} else {
					throw new NoSuchElementException("SectionsEnumerator");
				}
			}
		}
	}


	public java.util.Enumeration sections() {
		return new SectionsEnumerator(this);
	}


	/**
	 * Returns an enumeration of the segments of the stem
	 */
	public Enumeration stemSegments() {
		return segments.elements();
	}

	/**
	 * Returns an enumeration of the leaves of the stem, but not the leaves of it's substems.
	 */
	public Enumeration stemLeaves() {
		return leaves.elements();
	}


	/**
	 * Creates a new stem
	 *
	 * @param tr the tree object
	 * @param params the general tree parameters
	 * @param lparams the parameters for the stem level
	 * @param parnt the parent stem, from wich the stems grows out
	 * @param stlev the stem level
	 * @param trf the base transformation of the stem
	 * @param offs the offset of ste stem within the parent stem (0..1)
	 */
	public StemImpl(
		TreeImpl tr, StemImpl growsOutOf, int stlev,
		Transformation trf, double offs
	) /* offs=0 */ {
		tree = tr;
		stemlevel = stlev;
		transf = trf;
		offset = offs;

		if (growsOutOf != null) {
			if (growsOutOf.stemlevel < stemlevel) {
				parent = growsOutOf;
			} else {
				clonedFrom = growsOutOf;
				parent = growsOutOf.parent;
			}
		}

		par = tree.params;
		lpar = par.getLevelParams(stemlevel);

		// initialize lists
		segments = new java.util.Vector(lpar.nCurveRes);

		if (lpar.nSegSplits > 0 || par._0BaseSplits > 0) {
			clones = new java.util.Vector(); // lpar.nSegSplits*lpar.nCurveRes+1);
		}

		if (stemlevel < par.Levels - 1) {
			LevelParams lpar_1 = par.getLevelParams(lpar.level + 1);
			substems = new java.util.Vector(lpar_1.nBranches);
		} else {
			substems = new java.util.Vector();
		}

		if (stemlevel == par.Levels - 1 && par.Leaves != 0) {
			leaves = new java.util.Vector(Math.abs(par.Leaves));
		}

		// inialize other variables
		leavesPerSegment = 0;
		splitCorrection = 0;

		index = 0; // substem number

		cloneIndex = new java.util.Vector();
		pruneTest = false; // flag used for pruning

		//...
		maxPoint = new Vector(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
		minPoint = new Vector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	}

	/**
	 * For debugging: Prints out the transformation to stderr nicely (only if debugging is enabled)
	 *
	 * @param where The position in the tree, i.e. wich stem has this transformation
	 * @param trf The transformation
	 */

	void TRF(String where, Transformation trf) {
		DBG(where + ": " + trf.toString());
	}

	/**
	 * Output debug string, when debugging ist enabled
	 *
	 * @param dbgstr The output string
	 */

	public void DBG(String dbgstr) {
		// print debug string to stderr if debugging is enabled
		Console.debugOutput(getTreePosition() + ":" + dbgstr);
	}

	/* (non-Javadoc)
	 * @see net.katsstuff.arbaro.tree.TraversableStem#getTreePosition()
	 */
	public String getTreePosition() {
		// returns the position of the stem in the tree as a string, e.g. 0c0.1
		// for the second substem of the first clone of the trunk
		StemImpl stem = this;
		int lev = stemlevel;
		StringBuilder pos = new StringBuilder();
		while (lev >= 0) {
			if (stem.cloneIndex.size() > 0) {
				StringBuilder clonestr = new StringBuilder();
				for (int i = 0; i < stem.cloneIndex.size(); i++) {
					clonestr.append("c").append(((Integer) stem.cloneIndex.elementAt(i)).toString());
				}
				pos.insert(0, "" + stem.index + clonestr + ".");
			} else {
				pos.insert(0, "" + stem.index + ".");
			}
			if (lev > 0) {
				stem = stem.parent;
			}
			lev--;
		}
		if (pos.charAt(pos.length() - 1) == '.') {
			pos = new StringBuilder(pos.substring(0, pos.length() - 1));
		}
		return pos.toString();
	}

	/**
	 * @return get the level of this stem, 0 means it is a trunk
	 */
	public int getLevel() {
		return stemlevel;
	}

	/**
	 * @return the radius at the stem base
	 */
	public double getBaseRadius() {
		return ((SegmentImpl) segments.elementAt(0)).rad1;
	}

	/**
	 * @return the radius at the stem peak
	 */
	public double getPeakRadius() {
		return ((SegmentImpl) segments.elementAt(segments.size() - 1)).rad2;
	}


	/**
	 * Make a clone of the stem at this position
	 *
	 * @param trf The base transformation for the clone
	 * @param start_segm Start segment number, i.e. the height, where the clone spreads out
	 * @return The clone stem object
	 */

	StemImpl clone(Transformation trf, int start_segm) {
		// creates a clone stem with same atributes as this stem
		StemImpl clone = new StemImpl(tree, this, stemlevel, trf, offset);
		clone.segmentLength = segmentLength;
		clone.segmentCount = segmentCount;
		clone.length = length;
		clone.baseRadius = baseRadius;
		clone.splitCorrection = splitCorrection;
		clone.pruneTest = pruneTest;
		clone.index = index;

		//DBG("Stem.clone(): clone_index "+clone_index);
		clone.cloneIndex.addAll(cloneIndex);

		//DBG("Stem.clone(): level: "+stemlevel+" clones "+clones);
		clone.cloneIndex.addElement(clones.size());
		if (!pruneTest) {
			clone.lengthChildMax = lengthChildMax;
			//clone.substem_cnt = substem_cnt;
			clone.substemsPerSegment = substemsPerSegment;
			//clone.substemdist = substemdist;
			//clone.substemdistv = substemdistv;
			//clone.seg_splits = self.seg_splits
			// FIXME: for more then one clone this angle should somehow
			// correspond to the rotation angle of the clone
			clone.substemRotangle = substemRotangle + 180;
			clone.leavesPerSegment = leavesPerSegment;
		}
		return clone;
	}


	/**
	 * Makes the stem, i.e. calculates all its segments, clones and substems a.s.o. recursively
	 */

	public boolean make() {

		// makes the stem with all its segments, substems, clones and leaves
		segmentCount = lpar.nCurveRes;
		length = stemLength();
		segmentLength = length / lpar.nCurveRes;
		baseRadius = stemBaseRadius();
		if (stemlevel == 0) {
			double baseWidth = Math.max(baseRadius, stemRadius(0));
			minMaxTest(new Vector(baseWidth, baseWidth, 0));
		}

		if (Console.debug()) {
			DBG("Stem.make(): len: " + length + " sgm_cnt: " + segmentCount + " base_rad: " + baseRadius);
		}

		// FIXME: should pruning occur for the trunk too?
		if (stemlevel > 0 && par.PruneRatio > 0) {
			pruning();
		}

		// FIXME: if length<=MIN_STEM_LEN the stem object persists here but without any segments
		// alternatively make could return an error value, the invoking function
		// then had to delete this stem
		if (length > MIN_STEM_LEN && baseRadius > MIN_STEM_RADIUS) {
			prepareSubstemParams();
			makeSegments(0, segmentCount);
			return true;
		} else {
			DBG("length " + length + " (after pruning?) to small - stem not created");
			return false;
		}

//		tree.minMaxTest(maxPoint);
//		tree.minMaxTest(minPoint);
	}


	/**
	 * Apply pruning to the stem. If it grows out of the pruning envelope, it is shortened.
	 */

	void pruning() {

		// save random state, split and len values
		lpar.saveState();
		double splitcorr = splitCorrection;
		double origlen = length;
		//double seglen = segmentLength;

		// start pruning
		pruneTest = true;

		// test length
		int segm = makeSegments(0, segmentCount);

		while (segm >= 0 && length > 0.001 * par.scale_tree) {

			// restore random state and split values
			lpar.restoreState();
			splitCorrection = splitcorr;

			// delete segments and clones
			if (clones != null) {
				clones.clear();
			}
			segments.clear();

			// get new length
			double minlen = length / 2; // shorten max. half of length
			double maxlen = length - origlen / 15; // shorten min of 1/15 of orig. len
			length = Math.min(Math.max(segmentLength * segm, minlen), maxlen);

			// calc new values dependent from length
			segmentLength = length / lpar.nCurveRes;
			baseRadius = stemBaseRadius();

			if (length > MIN_STEM_LEN && baseRadius < MIN_STEM_RADIUS) {
				Console.errorOutput("WARNING: stem radius ("
									+ baseRadius
									+ ") too small for stem "
									+ getTreePosition());
			}

			// test once more
			if (length > MIN_STEM_LEN) {
				segm = makeSegments(0, segmentCount);
			}
		}
		// this length fits the envelope,
		// diminish the effect corresp. to PruneRatio
		length = origlen - (origlen - length) * par.PruneRatio;

		// restore random state and split values
		lpar.restoreState();
		splitCorrection = splitcorr;
		// delete segments and clones
		if (clones != null) {
			clones.clear();
		}
		segments.clear();
		pruneTest = false;
		//self.DBG("PRUNE-ok: len: %f, segm: %d/%d\n"%(self.length,segm,self.segment_cnt))
	}

	/**
	 * Calcs stem length from parameters and parent length
	 *
	 * @return the stem length
	 */

	double stemLength() {
		if (stemlevel == 0) { // trunk
			return (lpar.nLength + lpar.var(lpar.nLengthV)) * par.scale_tree;
		} else if (stemlevel == 1) {
			double parlen = parent.length;
			double baselen = par.BaseSize * par.scale_tree;
			double ratio = (parlen - offset) / (parlen - baselen);
			if (Console.debug()) {
				DBG("Stem.stem_length(): parlen: "
					+ parlen
					+ " offset: "
					+ offset
					+ " baselen: "
					+ baselen
					+ " ratio: "
					+ ratio);
			}
			return parlen * parent.lengthChildMax * par.getShapeRatio(ratio);
		} else { // higher levels
			return parent.lengthChildMax * (parent.length - 0.6 * offset);
		}
	}

	// makes the segments of the stem
	int makeSegments(int start_seg, int end_seg) {
//		if (start_seg>end_seg) throw new ArbaroException("Error in segment creation end_seg<start_seg.");

		if (stemlevel == 1) {
			tree.updateGenProgress();
		}

		//if (par.verbose) {
		if (!pruneTest) {
			if (stemlevel == 0) {
				Console.progressChar('=');
			} else if (stemlevel == 1 && start_seg == 0) {
				Console.progressChar('/');
			} else if (stemlevel == 2 && start_seg == 0) {
				Console.progressChar();
			}
		}
		//}

		Transformation trf = transf;

		for (int s = start_seg; s < end_seg; s++) {
			if (stemlevel == 0) {
				tree.updateGenProgress();
			}

			if (!pruneTest) {// && par.verbose) {
				if (stemlevel == 0) {
					Console.progressChar('|');
				}
			}

			// curving
			trf = newDirection(trf, s);
			if (Console.debug()) {
				TRF("Stem.make_segments(): after new_direction ", trf);
			}

			// segment radius
			double rad1 = stemRadius(s * segmentLength);
			double rad2 = stemRadius((s + 1) * segmentLength);

			// create new segment
			SegmentImpl segment = new SegmentImpl(this, s, trf, rad1, rad2);
			segment.make();
			segments.addElement(segment);

			// create substems
			// self.DBG("SS-makingsubst? pt: %d, lev: %d\n"%(self.prunetest,self.level))
			if (!pruneTest && lpar.level < par.Levels - 1) {
				// self.DBG("SS-making substems\n")
				makeSubstems(segment);
			}

			if (!pruneTest && stemlevel == par.Levels - 1 && par.Leaves != 0) {
				makeLeaves(segment);
			}

			// shift to next position
			trf = trf.translate(trf.getZ().mul(segmentLength));
			//self.DBG("transf: %s\n"%(transf))
			//self.DBG("pos: %s\n"%(transf.vector))

			// test if too long
			if (pruneTest && !isInsideEnvelope(trf.getT())) {
				// DBG("PRUNE: not inside - return %d\n"%(s))
				return s;
			}

			// splitting (create clones)
			if (s < end_seg - 1) {
				int segm = makeClones(trf, s);
				// trf is changed by make_clones
				// prune test - clone not inside envelope
				if (segm >= 0) {
					//DBG("PRUNE: clone not inside - return %d\n"%(segm))
					return segm;
				}
			}
		}

		return -1;
	}


	/**
	 * Tests if a point is inside the pruning envelope
	 *
	 * @param vector the point to test
	 * @return true if the point is inside the pruning envelope
	 */

	boolean isInsideEnvelope(Vector vector) {
		double r = Math.sqrt(vector.getX() * vector.getX() + vector.getY() * vector.getY());
		double ratio = (par.scale_tree - vector.getZ()) / (par.scale_tree * (1 - par.BaseSize));
		return (r / par.scale_tree) < (par.PruneWidth * par.getShapeRatio(ratio, 8));
	}


	/**
	 * Calcs a new direction for the current segment
	 *
	 * @param trf The transformation of the previous segment
	 * @param nsegm The number of the segment ( for testing, if it's the first stem segment
	 * @return The new transformation of the current segment
	 */

	Transformation newDirection(Transformation trf, int nsegm) {
		// next segments direction

		// The first segment shouldn't get another direction
		// down and rotation angle shouldn't be falsified
		if (nsegm == 0) {
			return trf;
		}

		if (Console.debug()) {
			TRF("Stem.new_direction() before curving", trf);
		}

		// get curving angle
		double delta;
		if (lpar.nCurveBack == 0) {
			delta = lpar.nCurve / lpar.nCurveRes;
		} else {
			if (nsegm < (lpar.nCurveRes + 1) / 2) {
				delta = lpar.nCurve * 2 / lpar.nCurveRes;
			} else {
				delta = lpar.nCurveBack * 2 / lpar.nCurveRes;
			}
		}
		delta += splitCorrection;

		if (Console.debug()) {
			DBG("Stem.new_direction(): delta: " + delta);
		}

		trf = trf.rotx(delta);

		// With Weber/Penn the orientation of the x- and y-axis
		// shouldn't be disturbed (maybe, because proper curving relies on this)
		// so may be such random rotations shouldn't be used, instead nCurveV should
		// add random rotation to rotx, and rotate nCurveV about the tree's z-axis too?

		// add random rotation about z-axis
		if (lpar.nCurveV > 0) {
			//    		if (nsegm==0 && stemlevel==0) { // first_trunk_segment
			//    			// random rotation more moderate
			//    			delta = (Math.abs(lpar.var(lpar.nCurveV)) -
			//    					Math.abs(lpar.var(lpar.nCurveV)))
			//						/ lpar.nCurveRes;
			//    		}	else {
			// full random rotation
			delta = lpar.var(lpar.nCurveV) / lpar.nCurveRes;
			//    		}
			// self.DBG("curvV (delta): %s\n" % str(delta))
			double rho = 180 + lpar.var(180);
			trf = trf.rotaxisz(delta, rho);
		}
		TRF("Stem.new_direction() after curving", trf);

		// attraction up/down
		if (par.AttractionUp != 0 && stemlevel >= 2) {

			double declination = Math.acos(trf.getZ().getZ());

			// 			I don't see, why we need orientation here, may be this avoids
			//          attraction of branches with the x-Axis up and thus avoids
			//			twisting (see below), but why branches in one direction should
			//			be attracted, those with another direction not, this is unnaturally:
			//    		double orient = Math.acos(trf.getY().getZ());
			//    		double curve_up_orig = par.AttractionUp * declination * Math.cos(orient)/lpar.nCurveRes;

			// FIXME: devide by (lpar.nCurveRes-nsegm) if last segment
			// should actually be vertical
			double curve_up = par.AttractionUp *
							  Math.abs(declination * Math.sin(declination)) / lpar.nCurveRes;

			Vector z = trf.getZ();
			// FIXME: the mesh is twisted for high values of AttractionUp
			trf = trf.rotaxis(-curve_up * 180 / Math.PI, new Vector(-z.getY(), z.getX(), 0));
			// trf = trf.rotx(curve_up*180/Math.PI);
		}
		return trf;
	}


	/**
	 * Calcs the base radius of the stem
	 */

	double stemBaseRadius() {

		if (stemlevel == 0) { // trunk
			// radius at the base of the stem
			// I think nScale+-nScaleV should applied to the stem radius but not to base radius(?)
			return length * par.Ratio; // * par._0Scale;
			//+ var(par._0ScaleV))
		} else {
			// max radius is the radius of the parent at offset
			double max_radius = parent.stemRadius(offset);

			// FIXME: RatioPower=0 seems not to work here
			double radius = parent.baseRadius * Math.pow(length / parent.length, par.RatioPower);
			return Math.min(radius, max_radius);
		}
	}


	/**
	 * Calcs the stem radius at a given offset
	 *
	 * @param h the offset at which the radius is calculated
	 * @return the stem radius at this position
	 */

	public double stemRadius(double h) {
		if (Console.debug()) {
			DBG("Stem.stem_radius(" + h + ") base_rad:" + baseRadius);
		}

		double angle = 0; //FIXME: add an argument "angle" for Lobes,
		// but at the moment Lobes are calculated later in mesh creation

		// gets the stem width at a given position within the stem
		double Z = Math.min(h / length, 1.0); // min, to avoid rounding errors
		double taper = lpar.nTaper;

		double unit_taper = 0;
		if (taper <= 1) {
			unit_taper = taper;
		} else if (taper <= 2) {
			unit_taper = 2 - taper;
		}

		double radius = baseRadius * (1 - unit_taper * Z);

		// spherical end or periodic tapering
		double depth;
		if (taper > 1) {
			double Z2 = (1 - Z) * length;
			if (taper < 2 || Z2 < radius) {
				depth = 1;
			} else {
				depth = taper - 2;
			}
			double Z3;
			if (taper < 2) {
				Z3 = Z2;
			} else {
				Z3 = Math.abs(Z2 - 2 * radius * (int) (Z2 / 2 / radius + 0.5));
			}
			if (taper > 2 || Z3 < radius) {
				radius = (1 - depth) * radius + depth * Math.sqrt(radius * radius - (Z3 - radius) * (Z3 - radius));
				//  self.DBG("TAPER>2: Z2: %f, Z3: %f, depth: %f, radius %f\n"%(Z2,Z3,depth,radius))
			}
		}
		if (stemlevel == 0) {
			// add flaring (thicker stem base)
			if (par.Flare != 0) {
				double y = Math.max(0, 1 - 8 * Z);
				double flare = 1 + par.Flare * (Math.pow(100, y) - 1) / 100.0;
				DBG("Stem.stem_radius(): Flare: " + flare + " h: " + h + " Z: " + Z);
				radius = radius * flare;
			}
			// add lobes - this is done in mesh creation not here at the moment
			if (par.Lobes > 0 && angle != 0) {
				// FIXME: use the formular from Segment.create_mesh_section() instead
				radius = radius * (1.0 + par.LobeDepth * Math.sin(par.Lobes * angle * Math.PI / 180));
			}

			// multiply with 0Scale;
			// 0ScaleV is applied only in mesh creation (Segment.create_section_meshpoints)
			radius = radius * par._0Scale;
		}

		DBG("Stem.stem_radius(" + h + ") = " + radius);

		return radius;
	}


	/**
	 * Precalcs some stem parameters used later during when generating the current stem
	 */

	void prepareSubstemParams() {
		//int level = min(stemlevel+1,3);
		LevelParams lpar_1 = par.getLevelParams(stemlevel + 1);

		// maximum length of a substem
		lengthChildMax = lpar_1.nLength + lpar_1.var(lpar_1.nLengthV);

		// maximum number of substems
		double stems_max = lpar_1.nBranches;

		// actual number of substems and substems per segment
		double substem_cnt;
		if (stemlevel == 0) {
			substem_cnt = stems_max;
			substemsPerSegment = substem_cnt / (float) segmentCount / (1 - par.BaseSize);

			if (Console.debug()) {
				DBG("Stem.prepare_substem_params(): stems_max: " + substem_cnt
					+ " substems_per_segment: " + substemsPerSegment);
			}
		} else if (par.preview) {
			substem_cnt = stems_max;
			substemsPerSegment = substem_cnt / (float) segmentCount;
		} else if (stemlevel == 1) {
			substem_cnt = (int) (stems_max *
								 (0.2 + 0.8 * length / parent.length / parent.lengthChildMax));
			substemsPerSegment = substem_cnt / (float) segmentCount;
			DBG("Stem.prepare_substem_params(): substem_cnt: " + substem_cnt
				+ " substems_per_segment: " + substemsPerSegment);
		} else {
			substem_cnt = (int) (stems_max * (1.0 - 0.5 * offset / parent.length));
			substemsPerSegment = substem_cnt / (float) segmentCount;
		}
		substemRotangle = 0;

		// how much leaves for this stem - not really a substem parameter
		if (stemlevel == par.Levels - 1) {
			leavesPerSegment = leavesPerBranch() / segmentCount;
		}
	}

	/**
	 * Number of leaves of the stem
	 */

	double leavesPerBranch() {
		// calcs the number of leaves for a stem
		if (par.Leaves == 0) {
			return 0;
		}
		if (stemlevel == 0) {
			// FIXME: maybe set Leaves=0 when Levels=1 in Params.prepare()
			System.err.println("WARNING: trunk cannot have leaves, no leaves are created");
			return 0;
		}

		return (Math.abs(par.Leaves)
				* par.getShapeRatio(offset / parent.length, par.LeafDistrib)
				* par.LeafQuality);
	}


	/**
	 * Make substems of the current stem
	 */

	void makeSubstems(SegmentImpl segment) {
		if (stemlevel == par.Levels - 1) {
			return;
		}

		// creates substems for the current segment
		LevelParams lpar_1 = par.getLevelParams(stemlevel + 1);

		if (Console.debug()) {
			DBG("Stem.make_substems(): substems_per_segment " + substemsPerSegment);
		}

		double subst_per_segm;
		double offs;

		if (stemlevel > 0) {
			// full length of stem can have substems
			subst_per_segm = substemsPerSegment;

			if (segment.index == 0) {
				offs = parent.stemRadius(offset) / segmentLength;
			} else {
				offs = 0;
			}
		} else if (segment.index * segmentLength > par.BaseSize * length) {
			// segment is fully out of the bare trunk region => normal nb of substems
			subst_per_segm = substemsPerSegment;
			offs = 0;
		} else if ((segment.index + 1) * segmentLength <= par.BaseSize * length) {
			// segment is fully part of the bare trunk region => no substems
			return;
		} else {
			// segment has substems in the upper part only
			offs = (par.BaseSize * length - segment.index * segmentLength) / segmentLength;
			subst_per_segm = substemsPerSegment * (1 - offs);
		}

		// how many substems in this segment
		int substems_eff = (int) (subst_per_segm + lpar.substemErrorValue + 0.5);

		// adapt error value
		lpar.substemErrorValue -= (substems_eff - subst_per_segm);

		if (substems_eff <= 0) {
			return;
		}

		DBG("Stem.make_substems(): substems_eff: " + substems_eff);

		// what distance between the segements substems
		double dist = (1.0 - offs) / substems_eff * lpar_1.nBranchDist;
		double distv = dist * 0.25; // lpar_1.nBranchDistV/2;

		DBG("Stem.make_substems(): offs: " + offs + " dist: " + dist + " distv: " + distv);

		for (int s = 0; s < substems_eff; s++) {
			// where on the segment add the substem
			double where = offs + dist / 2 + s * dist + lpar_1.var(distv);

			//offset from stembase
			double offset = (segment.index + where) * segmentLength;

			DBG("Stem.make_substems(): offset: " + offset + " segminx: " + segment.index
				+ " where: " + where + " seglen: " + segmentLength);

			Transformation trf = substemDirection(segment.transf, offset);
			trf = segment.substemPosition(trf, where);

			// create new substem
			StemImpl substem = new StemImpl(tree, this, stemlevel + 1, trf, offset);
			substem.index = substems.size();
			DBG("Stem.make_substems(): make new substem");
			if (substem.make()) {
				substems.addElement(substem);
//
//if (substem.segments.size()==0)
//	throw new ArbaroException("No segments created for substem "+substem.getTreePosition());
			}
		}
	}


	/**
	 * Calcs the direction of a substem from the parameters
	 *
	 * @param trf The transformation of the current stem segment
	 * @param offset The offset of the substem from the base of the currents stem
	 * @return The direction of the substem
	 */

	Transformation substemDirection(Transformation trf, double offset) {
		LevelParams lpar_1 = par.getLevelParams(stemlevel + 1);
		//lev = min(level+1,3);

		// get rotation angle
		double rotangle;
		if (lpar_1.nRotate >= 0) { // rotating substems
			substemRotangle = (substemRotangle + lpar_1.nRotate + lpar_1.var(lpar_1.nRotateV) + 360) % 360;
			rotangle = substemRotangle;
		} else { // alternating substems
			if (Math.abs(substemRotangle) != 1) {
				substemRotangle = 1;
			}
			substemRotangle = -substemRotangle;
			rotangle = substemRotangle * (180 + lpar_1.nRotate + lpar_1.var(lpar_1.nRotateV));
		}

		// get downangle
		double downangle;
		if (lpar_1.nDownAngleV >= 0) {
			downangle = lpar_1.nDownAngle + lpar_1.var(lpar_1.nDownAngleV);
		} else {
			double len = (stemlevel == 0) ? length * (1 - par.BaseSize) : length;
			downangle = lpar_1.nDownAngle +
						lpar_1.nDownAngleV * (1 - 2 * par.getShapeRatio((length - offset) / len, 0));
		}
		if (Console.debug()) {
			DBG("Stem.substem_direction(): down: " + downangle + " rot: " + rotangle);
		}

		return trf.rotxz(downangle, rotangle);
	}


	/**
	 * Creates the leaves for the current stem segment
	 */

	void makeLeaves(SegmentImpl segment) {
		// creates leaves for the current segment

		if (par.Leaves > 0) { // ### NORMAL MODE, leaves along the stem
			// how many leaves in this segment
			double leaves_eff = (int) (leavesPerSegment + par.leavesErrorValue + 0.5);

			// adapt error value
			par.leavesErrorValue -= (leaves_eff - leavesPerSegment);

			if (leaves_eff <= 0) {
				return;
			}

			double offs;
			if (segment.index == 0) {
				offs = parent.stemRadius(offset) / segmentLength;
			} else {
				offs = 0;
			}

			// what distance between the leaves
			double dist = (1.0 - offs) / leaves_eff;

			for (int s = 0; s < leaves_eff; s++) {
				// where on the segment add the leaf

				// FIXME: may be use the same distribution method (BranchDist) as for substems?
				double where = offs + dist / 2 + s * dist + lpar.var(dist / 2);

				// offset from stembase
				double loffs = (segment.index + where) * segmentLength;
				// get a new direction for the leaf
				Transformation trf = substemDirection(segment.transf, loffs);
				// translate it to its position on the stem
				trf = trf.translate(segment.transf.getZ().mul(where * segmentLength));

				// create new leaf
				LeafImpl leaf = new LeafImpl(trf); // ,loffs);
				leaf.make(par);
				leaves.addElement(leaf);
			}
		}

		// ##### FAN MOD, leaves placed in a fan at stem end
		else if (par.Leaves < 0 && segment.index == segmentCount - 1) {

			LevelParams lpar_1 = par.getLevelParams(stemlevel + 1);
			int cnt = (int) (leavesPerBranch() + 0.5);

			Transformation trf = segment.transf.translate(segment.transf.getZ().mul(segmentLength));
			double distangle = lpar_1.nRotate / cnt;
			double varangle = lpar_1.nRotateV / cnt;
			double downangle = lpar_1.nDownAngle;
			double vardown = lpar_1.nDownAngleV;
			double offsetangle = 0;
			// use different method for odd and even number
			if (cnt % 2 == 1) {
				// create one leaf in the middle
				LeafImpl leaf = new LeafImpl(trf); //,segmentCount*segmentLength);
				leaf.make(par);
				leaves.addElement(leaf);
				offsetangle = distangle;
			} else {
				offsetangle = distangle / 2;
			}
			// create leaves left and right of the middle
			for (int s = 0; s < cnt / 2; s++) {
				for (int rot = 1; rot >= -1; rot -= 2) {
					Transformation transf1 = trf.roty(rot * (offsetangle + s * distangle
															 + lpar_1.var(varangle)));
					transf1 = transf1.rotx(downangle + lpar_1.var(vardown));
					LeafImpl leaf = new LeafImpl(transf1); //,segmentCount*segmentLength);
					leaf.make(par);
					leaves.addElement(leaf);
				}
			}
		}
	}


	/**
	 * Make clones of the current stem at the current segment
	 *
	 * @param trf The current segments's direction
	 * @param nseg The number of the current segment
	 * @return Segments outside the pruning envelope, -1 if stem clone is completely inside the envelope
	 */

	int makeClones(Transformation trf, int nseg) {
		// splitting
		// FIXME: maybe move this calculation to LevelParams
		// but pay attention to saving errorValues and restoring when making prune tests
		int seg_splits_eff;
		if (stemlevel == 0 && nseg == 0 && par._0BaseSplits > 0) {
			seg_splits_eff = par._0BaseSplits;
		} else {
			// how many clones?
			double seg_splits = lpar.nSegSplits;
			seg_splits_eff = (int) (seg_splits + lpar.splitErrorValue + 0.5);

			// adapt error value
			lpar.splitErrorValue -= (seg_splits_eff - seg_splits);
		}

		if (seg_splits_eff < 1) {
			return -1;
		}

		double s_angle = 360 / (seg_splits_eff + 1);

		// make clones
		// if seg_splits_eff > 0:
		for (int i = 0; i < seg_splits_eff; i++) {

			// copy params
			StemImpl clone = clone(trf, nseg + 1);

			// NOTE: its a little bit problematic here
			// when the clone is given as a parent to
			// the substems, it should have the same
			// params for length and segment_cnt like
			// the original stem, but this could be
			// somewhat confusing(?)
			// clone.segment_cnt = remaining_segs;
			// clone.length = remaining_segs * self.segment_len

			// change the direction for the clone
			//if self.debug: sys.stderr.write("-SPLIT_CORE_BEFOR: %s, dir: %s\n" % \
			//	(str(clone.split_corr),str(clone.direction)))

			clone.transf = clone.split(trf, s_angle * (1 + i), nseg, seg_splits_eff);

			//if self.debug: sys.stderr.write("-SPLIT_CORE_AFTER: %s, dir: %s\n" %
			//	(str(clone.split_corr),str(clone.direction)))

			// make segments etc. for the clone
			int segm = clone.makeSegments(nseg + 1, clone.segmentCount);
			if (segm >= 0) { // prune test - clone not inside envelope
				return segm;
			}
			// add clone to the list
			clones.addElement(clone);
		}
		// get another direction for the original stem too
		trf = split(trf, 0, nseg, seg_splits_eff);
		return -1;
	}


	/**
	 * Gives a clone a new direction (splitting)
	 *
	 * @param trf The base transformation of the clone
	 * @param s_angle The splitting angle
	 * @param nseg The segment number, where the clone begins
	 * @param nsplits The number of clones
	 * @return The new direction for the clone
	 */

	Transformation split(
		Transformation trf,
		double s_angle, int nseg, int nsplits
	) {
		// applies a split angle to the stem - the Weber/Penn method
		int remaining_seg = segmentCount - nseg - 1;

		// the splitangle
		// FIXME: don't know if it should be nSplitAngle or nSplitAngle/2
		double declination = Math.acos(trf.getZ().getZ()) * 180 / Math.PI;
		double split_angle = Math.max(0, (lpar.nSplitAngle
										  + lpar.var(lpar.nSplitAngleV) - declination));

		// FIXME: first works better for level 0, second for further levels
		// transf = transf.rotxz(split_angle,s_angle)
		trf = trf.rotx(split_angle);

		// adapt split correction
		splitCorrection -= split_angle / remaining_seg;
		//t_corr = Transformation().rotx(-split_angle/remaining_seg)

		double split_diverge;
		if (s_angle > 0) { // original stem has s_angle==0
			if (par._0BaseSplits > 0 && stemlevel == 0 && nseg == 0) {
				split_diverge = s_angle + lpar.var(lpar.nSplitAngleV);
			} else {
				split_diverge = 20 + 0.75 * (30 + Math.abs(declination - 90))
									 * Math.pow((lpar.var(1) + 1) / 2.0, 2);
				if (lpar.var(1) >= 0) {
					split_diverge = -split_diverge;
				}
			}

			trf = trf.rotaxis(split_diverge, Vector.Z_AXIS);
		} else {
			split_diverge = 0; // for debugging only
		}

		// adjust some parameters
		//split_cnt = split_cnt+1;

		// lower substem prospensity
		if (!pruneTest) {
			substemsPerSegment /= (float) (nsplits + 1);
			// FIXME: same reduction for leaves_per_segment?
		}
		return trf;
	}

	/*
	 # The Weber/Penn splitting method is problematic for big splitting angles, or
	 # may be i misunderstood it, but it seems, that evenly splitting like for
	 # an umbrella formed acacia (don't know the english name of that trees) isn't
	 */

	public boolean traverseTree(TreeTraversal traversal) {
		if (traversal.enterStem(this))  // enter this tree?
		{

			if (leaves != null) {
				Enumeration l = leaves.elements();
				while (l.hasMoreElements()) {
					if (!((Leaf) l.nextElement()).traverseTree(traversal)) {
						break;
					}
				}
			}

			if (substems != null) {
				Enumeration s = substems.elements();
				while (s.hasMoreElements()) {
					if (!((Stem) s.nextElement()).traverseTree(traversal)) {
						break;
					}
				}
			}

			if (clones != null) {
				Enumeration s = clones.elements();
				while (s.hasMoreElements()) {
					if (!((Stem) s.nextElement()).traverseTree(traversal)) {
						break;
					}
				}
			}
		}

		return traversal.leaveStem(this);
	}


	/**
	 * Returns the total number of all the substems and substems of substems a.s.o. of the current stem
	 *
	 * @return totals number of susbstems and all theire children a.s.o
	 */
	long substemTotal() {
		if (substems == null) {
			return 0;
		}

		long sum = substems.size();
		for (int i = 0; i < substems.size(); i++) {
			// FIXME: what about clones?
			sum += ((StemImpl) substems.elementAt(i)).substemTotal();
		}
		return sum;
	}


	// use with TreeTraversal
	/* (non-Javadoc)
	 * @see net.katsstuff.arbaro.tree.TraversableStem#getLeafCount()
	 */
	public long getLeafCount() {
		if (leaves != null) {
			return leaves.size();
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see net.katsstuff.arbaro.tree.TraversableStem#isClone()
	 */
	public boolean isClone() {
		return cloneIndex.size() > 0;
	}

	public boolean isSmooth() {
		return stemlevel <= par.smooth_mesh_level;
	}

	public int getCloneSectionOffset() {
		if (!isClone()) {
			return 0;
		} else {
			// find out how many sections the parent stem
			// has below the first segment of this stem
			int segInx = ((SegmentImpl) segments.elementAt(0)).index;

			return clonedFrom.getSectionCountBelow(segInx);
		}
	}

	protected int getSectionCountBelow(int index) {
		int count = 1; // first segments base section

		Enumeration segs = segments.elements();
		while (segs.hasMoreElements()) {

			SegmentImpl s = ((SegmentImpl) segs.nextElement());
			if (s.index < index) {
				count += s.subsegments.size();
			} else {
				return count - 1;
			}
		}

		return count - 1;
	}


	/**
	 * For every stem there is a box (as minPoint, maxPoint), within the stem with all it's substems of any level should
	 * be contained. The box is calculated by invoking this method for every point limiting a stem segment. If such
	 * point of a stems segment of substems segment is outside of the box, the box is adapted.
	 *
	 * @param pt The point which should be inside of the containing box
	 */

	public void minMaxTest(Vector pt) {
		maxPoint.setMaxCoord(pt);
		minPoint.setMinCoord(pt);

		if (clonedFrom != null) {
			clonedFrom.minMaxTest(pt);
		}
		if (parent != null) {
			parent.minMaxTest(pt);
		} else {
			// no parent, this must be a trunk
			tree.minMaxTest(pt);
		}
	}
}















