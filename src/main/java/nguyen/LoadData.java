package nguyen;

public class LoadData {

	public static void main(String[] args) throws Exception {
		DatabaseManager dbm = new DatabaseManager(new DatabaseInfo());
		dbm.loadData();
	}

}
