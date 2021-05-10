package net.katsstuff.arbaro.mesh;

import net.katsstuff.arbaro.export.Progress;
import net.katsstuff.arbaro.tree.Tree;

public interface MeshGenerator {

	public abstract Mesh createStemMesh(Tree tree, Progress progress);

	public abstract Mesh createStemMeshByLevel(Tree tree, Progress progress);

	public abstract LeafMesh createLeafMesh(Tree tree, boolean useQuads);

	public boolean getUseQuads();
}