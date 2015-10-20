package nguyen;

import java.util.ArrayList;
import java.util.List;

import nguyen.query.strategies.ExistsStrategy;
import nguyen.query.strategies.GroupByStrategy;
import nguyen.query.strategies.IntersectStrategy;
import nguyen.query.strategies.QueryStrategy;

/**
 * Hello world!
 *
 */
public class Test {
	public static void main(String[] args) throws Exception {
		DatabaseManager dbm = new DatabaseManager(new DatabaseInfo());
		InputGenerator ig = new InputGenerator(dbm.getAddressCount());

		List<QueryStrategy> strategies = new ArrayList<QueryStrategy>();

		strategies.add(new ExistsStrategy());
		strategies.add(new GroupByStrategy());
		strategies.add(new IntersectStrategy());

		// dbm.createfkIndex();
		for (QueryStrategy strategy : strategies) {
			System.out.println("****************************************");
			System.out.println(strategy.getClass().getSimpleName());
			for (int i = 1; i <= 50; i++) {
				QueryExplanation explanation = dbm.explainQuery(strategy, ig.getInputOf(i));
				System.out.println(explanation.getTotalRuntime());
			}
		}

	}
}
