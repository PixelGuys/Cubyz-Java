package cubyz.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cubyz.Logger;
import cubyz.api.Registry;
import cubyz.api.Resource;
import cubyz.rendering.Material;
import cubyz.rendering.Mesh;
import cubyz.rendering.ModelLoader;
import cubyz.rendering.Texture;
import cubyz.rendering.TextureArray;
import cubyz.rendering.models.Model;
import cubyz.utils.ResourceUtilities;
import cubyz.utils.ResourceUtilities.EntityModel;
import cubyz.utils.datastructures.BinaryMaxHeap;
import cubyz.utils.json.JsonObject;
import cubyz.utils.json.JsonParser;
import cubyz.world.ChunkData;
import cubyz.world.blocks.Block;
import cubyz.world.blocks.CustomBlock;
import cubyz.world.entity.EntityType;

/**
 * Used to store active meshes and used to init mesh related lambda functions stored in ClientOnly.
 */

public class Meshes {

	public static final HashMap<Block, Mesh> blockMeshes = new HashMap<>();
	public static final HashMap<EntityType, Mesh> entityMeshes = new HashMap<>();
	
	public static final TextureArray blockTextureArray = new TextureArray();
	

	public static final HashMap<String, Mesh> cachedDefaultModels = new HashMap<>();
	
	public static final Registry<Model> models = new Registry<>();
	
	/** List of meshes that need to be cleaned. */
	public static final ArrayList<ChunkMesh> removableMeshes = new ArrayList<>();

	/** List of meshes that need to be (re-)generated. */
	private static final BinaryMaxHeap<ChunkData> updateQueue = new BinaryMaxHeap<ChunkData>(new ChunkMesh[16]);
	
	/**
	 * Cleans all meshes scheduled for removal.
	 * Needs to be called from an openGL thread!
	 */
	public static void cleanUp() {
		synchronized(removableMeshes) {
			for(ChunkMesh mesh : removableMeshes) {
				mesh.cleanUp();
			}
			removableMeshes.clear();
		}
	}

	/**
	 * Schedules a mesh to be cleaned in the near future.
	 * @param mesh
	 */
	public static void deleteMesh(ChunkMesh mesh) {
		if(mesh == null) return;
		synchronized(removableMeshes) {
			removableMeshes.add(mesh);
		}
	}

	/**
	 * Schedules a mesh to be regenerated in the near future.
	 * @param mesh
	 */
	public static void queueMesh(ChunkMesh mesh) {
		// Calculate the priority, which is determined by distance and resolution/size.
		double dx = Cubyz.player.getPosition().x - mesh.wx;
		double dy = Cubyz.player.getPosition().y - mesh.wy;
		double dz = Cubyz.player.getPosition().z - mesh.wz;
		double dist = dx*dx + dy*dy + dz*dz;
		float priority = -(float)dist/mesh.size;
		mesh.updatePriority(priority);
		updateQueue.add(mesh);
	}

	public static ChunkMesh getNextQueuedMesh() {
		return (ChunkMesh) updateQueue.extractMax();
	}
	
	public static void initMeshCreators() {
		ClientOnly.createBlockMesh = (block) -> {
			Resource rsc = block.getRegistryID();
			Texture tex = null;
			// Try loading it from the assets:
			String model;
			if(block instanceof CustomBlock) {
				model = "cubyz:block.obj";
			} else {
				String path = "assets/"+rsc.getMod()+"/blocks/" + rsc.getID()+".json";
				JsonObject json = JsonParser.parseObjectFromFile(path);
				model = json.getString("model", "cubyz:block.obj");
			}
			
			// Cached meshes
			Mesh mesh = null;
			for (String key : cachedDefaultModels.keySet()) {
				if (key.equals(model)) {
					mesh = cachedDefaultModels.get(key);
				}
			}
			if (mesh == null) {
				Resource rs = new Resource(model);
				mesh = new Mesh(ModelLoader.loadModel(rs, "assets/" + rs.getMod() + "/models/3d/" + rs.getID()));
				//defaultMesh = StaticMeshesLoader.loadInstanced("assets/" + rs.getMod() + "/models/3d/" + rs.getID(), "assets/" + rs.getMod() + "/models/3d/")[0];
				Material material = new Material(tex, 0.6F);
				mesh.setMaterial(material);
				cachedDefaultModels.put(model, mesh);
			}
			
			Meshes.blockMeshes.put(block, mesh);
		};
		
		ClientOnly.createEntityMesh = (type) -> {
			Resource rsc = type.getRegistryID();

			EntityModel model = null;
			try {
				model = ResourceUtilities.loadEntityModel(rsc);
			} catch (IOException e) {
				Logger.warning(rsc + " entity model not found");
				//Logger.throwable(e);
				//model = ResourceUtilities.loadEntityModel(new Resource("cubyz:undefined")); // TODO: load a simple cube with the undefined texture
				return;
			}
			
			// Cached meshes
			Resource rs = new Resource(model.model);
			Mesh mesh = new Mesh(ModelLoader.loadModel(rs, "assets/" + rs.getMod() + "/models/3d/" + rs.getID()));
			Resource texResource = new Resource(model.texture);
			String texture = texResource.getID();
			if (!new File("assets/" + texResource.getMod() + "/textures/entities/" + texture + ".png").exists()) {
				Logger.warning(texResource + " texture not found");
				texture = "blocks/undefined";
			}
			
			Texture tex = Texture.loadFromFile("assets/" + texResource.getMod() + "/textures/entities/" + texture + ".png");
			
			Material material = new Material(tex, 1.0F);
			mesh.setMaterial(material);
			
			Meshes.entityMeshes.put(type, mesh);
		};
	}
}
