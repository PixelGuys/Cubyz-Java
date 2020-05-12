package io.cubyz.blocks;

import org.joml.Vector3i;

import io.cubyz.api.GameRegistry;
import io.cubyz.api.IRegistryElement;
import io.cubyz.api.Resource;
import io.cubyz.items.Inventory;
import io.cubyz.items.Item;
import io.cubyz.items.ItemBlock;
import io.cubyz.world.World;


public class Block implements IRegistryElement {
	
	public static enum BlockClass {
		WOOD, STONE, SAND, UNBREAKABLE, LEAF, FLUID
	};
	
	boolean transparent;
	/**
	 * Used for rendering optimization.<br/>
	 * Do not edit or rely on, as it is not an ID to actually describe the block on a persistent state.
	 */
	public int ID;			// Stores the numerical ID. This ID is generated by the registry. There is no need to fill it manually.

	private Resource id = Resource.EMPTY;
	private float hardness; // Time in seconds to break this block by hand.
	private boolean solid = true;
	private boolean selectable = true;
	private Item blockDrop;
	protected boolean degradable = false; // Meaning undegradable parts of trees or other structures can grow through this block.
	protected BlockClass bc;
	private int light = 0;
	int absorption = 0; // How much light this block absorbs if it is transparent.
	String gui; // GUI that is opened onClick.
	
	public Block() {}
	
	public Block(String id, float hardness, BlockClass bc) {
		setID(id);
		this.bc = bc;
		ItemBlock bd = new ItemBlock(this);
		setBlockDrop(bd);
		this.hardness = hardness;
	}
	
	public void setDegradable(Boolean deg) {
		degradable = deg;
	}
	
	public boolean isDegradable() {
		return degradable;
	}
	
	public boolean isTransparent() {
		return transparent;
	}
	
	public Block setSolid(boolean solid) {
		this.solid = solid;
		return this;
	}
	
	public boolean isSolid() {
		return solid;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}
	
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	public void setBlockClass(BlockClass bc) {
		this.bc = bc;
	}

	public void setAbsorption(int absorption) {
		this.absorption = absorption;
	}

	public boolean isSelectable() {
		return selectable;
	}
	
	public boolean generatesModelAtRuntime() {
		return false;
	}
	
	public void init() {}
	
	public Resource getRegistryID() {
		return id;
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}
	
	/**
	 * The ID can only be changed <b>BEFORE</b> registering the block.
	 * @param id
	 */
	public void setID(String id) {
		setID(new Resource(id));
	}
	
	public void setID(Resource id) {
		this.id = id;
	}
	
	public void setBlockDrop(Item bd) {
		blockDrop = bd;
	}
	
	public Item getBlockDrop() {
		return blockDrop;
	}
	
	public float getHardness() {
		return hardness;
	}
	
	public void setHardness(float hardness) {
		this.hardness = hardness;
	}
	
	public int getLight() {
		return light;
	}
	
	public void setLight(int light) {
		this.light = light;
	}
	
	public BlockEntity createBlockEntity(Vector3i pos) {
		return null;
	}
	
	public boolean hasBlockEntity() {
		return false;
	}
	
	public BlockClass getBlockClass() {
		return bc;
	}
	
	public void setGUI(String id) {
		gui = id;
	}
	
	 // returns true if the block did something on click.
	public boolean onClick(World world, Vector3i pos) {
		if(gui != null) {
			GameRegistry.openGUI("cubyz:workbench", new Inventory(10)); // TODO: Care about the inventory.
			return true;
		}
		return false;
	}
	
	public int getAbsorption() {
		return absorption;
	}
}
