package nguyen;

import java.util.ArrayList;
import java.util.List;

import nguyen.query.strategies.ExistsStrategy;
import nguyen.query.strategies.GroupByStrategy;
import nguyen.query.strategies.IntersectStrategy;
import nguyen.query.strategies.QueryStrategy;

public class QueryPlansInMarkdown {

	public static void main(String[] args) throws Exception {
		DatabaseManager dbm = new DatabaseManager(new DatabaseInfo());
		InputGenerator ig = new InputGenerator(dbm.getAddressCount());

		List<QueryStrategy> strategies = new ArrayList<QueryStrategy>();

		strategies.add(new ExistsStrategy());
		strategies.add(new GroupByStrategy());
		strategies.add(new IntersectStrategy());

		for (QueryStrategy strategy : strategies) {
			System.out.println("## "+ strategy.getClass().getSimpleName()+ " ##");
			System.out.println("```");
			System.out.println(dbm.getPlan(strategy, ig.getInputOf(10)));
			System.out.println("```");
		}

	}
}
