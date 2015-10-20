package nguyen;

import java.util.Arrays;

import nguyen.query.strategies.IntersectStrategy;

/**
 * Hello world!
 *
 */
public class DebugStrategy {
	public static void main(String[] args) throws Exception {
		IntersectStrategy strategy = new IntersectStrategy();
		System.out.println(strategy.createQuery(Arrays.asList(1,2,3)));
		
	}
}
