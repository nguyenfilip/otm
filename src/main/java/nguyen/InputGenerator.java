package nguyen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class InputGenerator {
	private int maxAddressId;
	private Random rand = new Random();
	private Map<Integer, List<Integer>> cache = new HashMap<Integer, List<Integer>>();

	public InputGenerator(int maxAddressId) {
		this.maxAddressId = maxAddressId;
	}

	public List<Integer> getInputOf(int n) {
		if (cache.containsKey(n))
			return cache.get(n);
		
		List<Integer> addressIds = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			addressIds.add(1 + rand.nextInt(maxAddressId));
		}

		cache.put(n, addressIds);
		return addressIds;
	}
}
