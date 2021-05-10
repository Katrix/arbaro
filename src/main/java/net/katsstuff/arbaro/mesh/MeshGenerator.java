package net.katsstuff.arbaro.mesh;

import net.katsstuff.arbaro.export.Progress;
import net.katsstuff.arbaro.tree.Tree;

public interface MeshGenerator {

	Mesh createStemMesh(Tree tree, Progress progress);

	Mesh createStemMeshByLevel(Tree tree, Progress progress);

	LeafMesh createLeafMesh(Tree tree, boolean useQuads);

	boolean getUseQuads();
}