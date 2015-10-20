package nguyen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;

import org.postgresql.ds.PGSimpleDataSource;

import nguyen.query.strategies.QueryStrategy;

public class DatabaseManager {
	private static final int PERSON_COUNT = 1000;
	private static final int MAX_ADDRESS = 150;
	private static final int MIN_ADDRESS = 100;

	private Random rand = new Random();
	private DatabaseInfo dbinfo;
	private PGSimpleDataSource ds = new PGSimpleDataSource();

	public DatabaseManager(DatabaseInfo databaseInfo) {
		dbinfo = databaseInfo;
		ds = new PGSimpleDataSource();
		ds.setServerName("localhost");
		ds.setDatabaseName(dbinfo.getDbName());
		ds.setUser(dbinfo.getUser());
		ds.setPassword(dbinfo.getPasswd());

	}

	public int getAddressCount() throws SQLException {
		Connection con = ds.getConnection();
		Statement st = con.createStatement();

		try {
			ResultSet rs = st.executeQuery("SELECT count(*) from address");
			rs.next();
			return rs.getInt(1);
		} finally {
			con.close();
		}
	}

	public void createfkIndex() throws SQLException {
		Connection con = ds.getConnection();
		Statement st = con.createStatement();

		try {
			st.execute("CREATE INDEX address_idx ON address (person_id)");
		} finally {
			con.close();
		}
	}
	public void dropFkIndex() throws SQLException {
		Connection con = ds.getConnection();
		Statement st = con.createStatement();

		try {
			st.execute("DROP INDEX address_idx");
		} finally {
			con.close();
		}
	}

	public void loadData() throws Exception {
		Connection con = ds.getConnection();
		Statement st = con.createStatement();

		try {
			st.execute("DROP TABLE address");
			st.execute("DROP TABLE person");
			dropFkIndex();
		} catch (Exception e) {
			e.printStackTrace();
		}
		st.execute("CREATE TABLE person(id SERIAL PRIMARY KEY, name varchar(40))");
		st.execute(
				"CREATE TABLE address(id SERIAL PRIMARY KEY, person_id integer REFERENCES person(id), street varchar(40))");
		createfkIndex();
		
		PreparedStatement personInsert = con.prepareStatement("INSERT INTO person values(?,?)");
		PreparedStatement address = con.prepareStatement("INSERT INTO address (person_id, street) values(?,?)");
		for (int i = 0; i < PERSON_COUNT; i++) {
			personInsert.setInt(1, i);
			personInsert.setString(2, "Person" + i);
			personInsert.execute();

			for (int k = 0; k < random(MIN_ADDRESS, MAX_ADDRESS); k++) {
				address.setInt(1, i);
				address.setString(2, "Address" + k);
				address.execute();
			}
		}

		con.close();
	}

	private int random(int minAddress, int maxAddress) {
		return minAddress + rand.nextInt(maxAddress - minAddress);
	}

	public QueryExplanation explainQuery(QueryStrategy queryStrategy, List<Integer> ids) throws SQLException {
		Connection con = null;
		try {
			con = ds.getConnection();
			Statement st = con.createStatement();

			st.executeQuery("EXPLAIN (ANALYZE ON, VERBOSE ON, COSTS ON, TIMING ON, FORMAT JSON) ("
					+ queryStrategy.createQuery(ids) + " )");

			return buildExplanation(st.getResultSet());
		} finally {
			con.close();
		}

	}
	public String getPlan(QueryStrategy queryStrategy, List<Integer> ids) throws SQLException {
		Connection con = null;
		try {
			con = ds.getConnection();
			Statement st = con.createStatement();

			st.executeQuery("EXPLAIN (ANALYZE ON, VERBOSE ON, COSTS ON, TIMING ON, FORMAT TEXT) ("
					+ queryStrategy.createQuery(ids) + " )");

			
			StringBuilder sb = new StringBuilder();
			ResultSet rs = st.getResultSet();
			while (rs.next())
				sb.append(rs.getString(1)+"\n");
			return sb.toString();
		} finally {
			con.close();
		}
	}
	private QueryExplanation buildExplanation(ResultSet resultSet) throws SQLException {
		ResultSetMetaData metadata = resultSet.getMetaData();
		String json = null;
		if (resultSet.next() || metadata.getColumnCount() != 1) {
			json = resultSet.getString(1);
		} else {
			throw new RuntimeException("Error running explain");
		}
		if (resultSet.next()) {
			throw new RuntimeException("There are more results then that EXPLAIN returned");
		}
		return new QueryExplanation(json);
	}



}
