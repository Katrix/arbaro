package net.katsstuff.arbaro.tree;

import java.io.InputStream;
import java.io.PrintWriter;
import net.katsstuff.arbaro.export.Progress;
import net.katsstuff.arbaro.params.AbstractParam;
import net.katsstuff.arbaro.params.Params;

public interface TreeGenerator {

	Tree makeTree(Progress progress);

	void setSeed(int seed);

	int getSeed();

	Params getParams();

	void setParam(String param, String value);

	// TODO: not used at the moment, may be the GUI
	// should get a TreeGenerator as a ParamContainer
	// and tree maker, and not work directly with Params
	// class
	AbstractParam getParam(String param);

	/**
	 * Returns a parameter group
	 *
	 * @param level The branch level (0..3)
	 * @param group The parameter group name
	 * @return A hash table with the parameters
	 */
	// TODO: not used at the moment, may be the GUI
	// should get a TreeGenerator as a ParamContainer
	// and tree maker, and not work directly with Params
	// class
	java.util.TreeMap getParamGroup(int level, String group);

	/**
	 * Writes out the parameters to an XML definition file
	 *
	 * @param out The output stream
	 */
	// TODO: not used at the moment, may be the GUI
	// should get a TreeGenerator as a ParamContainer
	// and tree maker, and not work directly with Params
	// class
	void writeParamsToXML(PrintWriter out);

	/**
	 * Clear all parameter values of the tree.
	 */
	void clearParams();

	/**
	 * Read parameter values from an XML definition file
	 *
	 * @param is The input XML stream
	 */
	void readParamsFromXML(InputStream is);

	/**
	 * Read parameter values from an Config style definition file
	 *
	 * @param is The input text stream
	 */
	void readParamsFromCfg(InputStream is);
}