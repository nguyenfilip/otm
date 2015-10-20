package nguyen.query.strategies;

import java.util.List;

public class GroupByStrategy implements QueryStrategy {

	public String createQuery(List<Integer> addressIds) {
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT p.name FROM person p INNER JOIN address a ON p.id=a.person_id WHERE ");
		
		boolean first = true;
		for (Integer aId : addressIds) {
			if (first) {
				first = false;
			} else {
				sb.append(" OR ");
			}

			sb.append(" a.id = " +aId+ " ");
		}
		
		sb.append(" GROUP BY p.name HAVING count(*) >= " + addressIds.size());

		return sb.toString();
	}

}
