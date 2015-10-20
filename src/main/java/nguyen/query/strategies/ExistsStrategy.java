package nguyen.query.strategies;

import java.util.List;

public class ExistsStrategy implements QueryStrategy  {

	public String createQuery(List<Integer> addressIds) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT p.name FROM person p  WHERE ");
		
		boolean first = true;
		
		for (Integer aId : addressIds) {
			
			if (first){
				first = false;
			}else {
				sb.append(" AND ");
			}
				
			sb.append(" EXISTS ( SELECT a.id FROM address a WHERE a.person_id = p.id AND a.id = "+aId+") ");
		}
		
		return sb.toString();
	}

}
