package nguyen.query.strategies;

import java.util.List;

public class TestStrategy implements QueryStrategy {

	public String createQuery(List<Integer> addressIds) {
		return "SELECT * from PERSON LEFT JOIN ADDRESS on PERSON.id = ADDRESS.person_id";
	}

}
