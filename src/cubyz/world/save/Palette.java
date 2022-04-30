package cubyz.world.save;

import java.util.Arrays;
import java.util.HashMap;

import cubyz.utils.Logger;
import pixelguys.json.JsonObject;
import cubyz.api.Registry;
import cubyz.api.RegistryElement;

/**
 * Basically a bi-directional map.
 */

public class Palette <T extends RegistryElement> {
	private final HashMap<T, Integer> TToInt = new HashMap<T, Integer>();
	private RegistryElement[] intToT = new RegistryElement[0];
	private final WorldIO wio;
	public Palette(JsonObject json, Registry<T> registry, WorldIO wio) {
		this.wio = wio;
		if (json == null) return;
		for (String key : json.map.keySet()) {
			T t = registry.getByID(key);
			if (t != null) {
				TToInt.put(t, json.getInt(key, 0));
			} else {
				Logger.warning("A block with ID " + key + " is used in world but isn't available.");
			}
		}
		intToT = new RegistryElement[TToInt.size()];
		for(T t : TToInt.keySet()) {
			intToT[TToInt.get(t)] = t;
		}
	}
	public JsonObject save() {
		JsonObject json = new JsonObject();
		for(int index = 0; index < intToT.length; index++) {
			json.put(intToT[index].getRegistryID().toString(), index);
		}
		return json;
	}
	@SuppressWarnings("unchecked")
	public T getElement(int index) {
		return (T)intToT[index];
	}
	public int getIndex(T t) {
		if (TToInt.containsKey(t)) {
			return TToInt.get(t);
		} else {
			synchronized(this) { // Might be accessed from multiple threads at the same time.
				if (TToInt.containsKey(t)) { // Check again in case it was just added.
					return TToInt.get(t);
				} else {
					// Create a value:
					int index = intToT.length;
					intToT = Arrays.copyOf(intToT, index+1);
					intToT[index] = t;
					TToInt.put(t, index);
					wio.saveWorldData();
					return index;
				}
			}
		}
	}
}
