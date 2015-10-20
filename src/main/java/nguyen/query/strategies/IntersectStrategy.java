package nguyen.query.strategies;

import java.util.List;

public class IntersectStrategy implements QueryStrategy {

	public String createQuery(List<Integer> addressIds) {
		StringBuilder sb = new StringBuilder();

		
		
		boolean first = true;
		for (Integer aId : addressIds) {
			if (first) {
				first = false;
			} else {
				sb.append(" INTERSECT ");
			}

			sb.append("SELECT p.name FROM person p INNER JOIN address a on p.id = a.person_id WHERE a.id = " + aId+"");
		}
		

		return sb.toString();
	}

}
