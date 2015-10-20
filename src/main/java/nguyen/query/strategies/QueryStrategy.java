package nguyen.query.strategies;

import java.util.List;

public interface QueryStrategy {
	String createQuery(List<Integer> addressIds);
}
