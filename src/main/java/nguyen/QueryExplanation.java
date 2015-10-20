package nguyen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryExplanation {
	private ArrayList listExplanations = new ArrayList();
	
	public QueryExplanation(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			listExplanations = mapper.readValue(json.trim(), ArrayList.class);
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public Map<String, Object> getMap(Object o){
		return (Map<String, Object>)o;
	}

	public List<Object> getList(Object o){
		return (List)o;
	}
	
	public String getString(Object o){
		return (String)o;
	}

	public Double getFloat (Object o){
		return (Double)o;
	}
	
	@Override
	public String toString() {
		return listExplanations.toString();
	}

	public Double getTotalRuntime() {
		return getFloat(getMap(listExplanations.get(0)).get("Total Runtime"));
	}
	
	

}
