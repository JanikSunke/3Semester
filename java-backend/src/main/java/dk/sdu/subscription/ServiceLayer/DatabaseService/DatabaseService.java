package dk.sdu.subscription.ServiceLayer.DatabaseService;

import dk.sdu.subscription.Interfaces.*;
import dk.sdu.subscription.Interfaces.Exceptions.UserFetchException;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.ISubscription;
import dk.sdu.subscription.Interfaces.PaymentInterfaces.SubscriptionType;
import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IInvoiceData;
import dk.sdu.subscription.Interfaces.ISecretService;
import dk.sdu.subscription.Interfaces.IUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatabaseService implements IDatabaseService {

    private static DatabaseService instance;
    private String url;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private Connection connection = null;

    public DatabaseService(ISecretService secretService) {
        url = secretService.getStringSecret("database_url");
        port = Integer.parseInt(secretService.getStringSecret("database_port"));
        databaseName = secretService.getStringSecret("database_name");
        username = secretService.getStringSecret("database_username");
        password = secretService.getStringSecret("database_password");

        initializePostgresqlDatabase();
    }

    public void saveUser(IUser user) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO users(id, name, role, email, testdata) VALUES(?,?,?,?,false) ON CONFLICT DO NOTHING");
        stmt.setString(1, user.getId());
        stmt.setString(2, user.getName());
        stmt.setInt(3, 1);
        stmt.setString(4, user.getEmail());
        stmt.execute();
    }

    public IUser getUserFromId(String id) throws Exception {
        try {
            PreparedStatement stmt = getConnection().prepareStatement("SELECT u.id, u.name, r.description as role, u.email, s2.description, f.familyid FROM users u\n" +
                    "    INNER JOIN roles r on u.role = r.id\n" +
                    "    LEFT OUTER JOIN families f on u.id = f.userid\n" +
                    "    LEFT JOIN subscriptions s on u.id = s.userid\n" +
                    "    LEFT OUTER JOIN subscriptiontiers s2 on s.tier = s2.id\n" +
                    "WHERE u.id = ?;");
            stmt.setString(1, id);
            ResultSet sqlReturnValues = stmt.executeQuery();
            if (!sqlReturnValues.next()) {
                throw new UserFetchException("Could not fetch internal user: User does not exist.");
            }
            return new UserData(
                    sqlReturnValues.getString(1),
                    sqlReturnValues.getString(2),
                    sqlReturnValues.getString(4),
                    sqlReturnValues.getString(3),
                    sqlReturnValues.getString(5),
                    sqlReturnValues.getString(6));
        }catch(Exception e){
            throw new Exception("Failed to fetch internal user: " + e.getMessage());
        }
    }

    public List<IUser> getAllUsers() throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT u.id, u.name, r.description as role, u.email, s2.description FROM users u\n" +
                "     INNER JOIN roles r on u.role = r.id\n" +
                "     LEFT JOIN Subscriptions s on u.id = s.userid\n" +
                "     LEFT OUTER JOIN Subscriptiontiers s2 on s.tier = s2.id");
        ResultSet sqlReturnValues = stmt.executeQuery();
        List<IUser> userList = new ArrayList<>();

        while (sqlReturnValues.next()) {
            userList.add(new UserData(
                    sqlReturnValues.getString(1),
                    sqlReturnValues.getString(2),
                    sqlReturnValues.getString(4),
                    sqlReturnValues.getString(3),
                    sqlReturnValues.getString(5)
            ));
        }
        return userList;
    }

    public IInvoiceData getInvoiceFromId(int id) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT i.id, i.amount, s.description, i.timestamp, i.userid FROM invoices i\n" +
                "INNER JOIN subscriptiontiers s on i.subscriptiontierid = s.id where i.id = ?;");
        stmt.setInt(1, id);
        ResultSet sqlReturnValues = stmt.executeQuery();
        if (!sqlReturnValues.next()) {
            return null;
        }
        return new InvoiceData(
                sqlReturnValues.getInt(1),
                sqlReturnValues.getLong(2),
                sqlReturnValues.getString(3),
                sqlReturnValues.getDate(4),
                sqlReturnValues.getString(5)
        );
    }

    public List<IInvoiceData> getInvoicesFromUserId(String userid) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT i.id, i.amount, s.description, i.timestamp, i.userid FROM invoices i\n" +
                "INNER JOIN subscriptiontiers s on i.subscriptiontierid = s.id where i.userid = ?;");
        stmt.setString(1, userid);
        ResultSet sqlReturnValues = stmt.executeQuery();
        List<IInvoiceData> invoiceList = new ArrayList<>();

        while (sqlReturnValues.next()) {
            invoiceList.add(new InvoiceData(
                    sqlReturnValues.getInt(1),
                    sqlReturnValues.getLong(2),
                    sqlReturnValues.getString(3),
                    sqlReturnValues.getDate(4),
                    sqlReturnValues.getString(5)

            ));
        }
        return invoiceList;
    }

    public void saveUserSubscribed(IUser user, ISubscription subscription) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO subscriptions(userid, tier, subscription_id) VALUES(?, subTybeByName(?), ?)");
        stmt.setString(1, user.getId());
        stmt.setString(2, subscription.getSubscriptionType().name());
        stmt.setString(3, subscription.getId());
        stmt.execute();
    }

    @Override
    public void deleteSubscription(ISubscription subscription) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM subscriptions WHERE subscription_id = ?");
        stmt.setString(1, subscription.getId());
        stmt.execute();
    }

    @Override
    public void updateSubscription(ISubscription subscription) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("UPDATE subscriptions SET tier = subtybebyname(?) WHERE subscription_id = ?");
        stmt.setString(1, subscription.getSubscriptionType().name());
        stmt.setString(2, subscription.getId());
        stmt.execute();
    }

    @Override
    public void refreshSubscriptionTable(List<ISubscription> subscriptions) throws Exception {
        try {
            getConnection().setAutoCommit(false);
            PreparedStatement truncateSubscriptions = getConnection().prepareStatement("TRUNCATE subscriptions");
            truncateSubscriptions.execute();
            truncateSubscriptions.close();

            PreparedStatement insertSubscriptions = getConnection().prepareStatement("INSERT INTO subscriptions (userid, tier, subscription_id) VALUES(?, subTybeByName(?), ?)");
            for(ISubscription subscription : subscriptions){
                insertSubscriptions.setString(1, subscription.getSubscriptionOwner());
                insertSubscriptions.setString(2, subscription.getSubscriptionType().name());
                insertSubscriptions.setString(3, subscription.getId());
                insertSubscriptions.addBatch();
            }

            insertSubscriptions.executeBatch();
            getConnection().commit();
        } catch (Exception e) {
            getConnection().rollback();
            throw new Exception("Refreshing subscription table went wrong. Rolling back! | " + e.getMessage());
        } finally {
            getConnection().setAutoCommit(true);
        }
    }

    public void saveInvoice(IInvoiceData invoice, IUser user, ISubscription subscription) throws Exception {
        this.saveInvoice(invoice, user, subscription.getSubscriptionType().name());
    }

    public void saveInvoice(IInvoiceData invoice, IUser user, String desc) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("INSERT into invoices (timestamp, amount, userid, subscriptiontierid) VALUES (?,?,?, subTybeByName(?))", Statement.RETURN_GENERATED_KEYS);
        stmt.setDate(1, new java.sql.Date(invoice.getDate().getTime()));
        stmt.setLong(2, invoice.getTotalAmount());
        stmt.setString(3, user.getId());
        stmt.setString(4, desc);
        stmt.execute();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            invoice.setId(generatedKeys.getInt(1));
        }
    }

    public List<IInvoiceData> getAllInvoices() throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT i.id, i.amount, s.description, i.timestamp, i.userid FROM invoices i\n" +
                "INNER JOIN subscriptiontiers s on i.subscriptiontierid = s.id");
        ResultSet sqlReturnValues = stmt.executeQuery();
        List<IInvoiceData> invoiceList = new ArrayList<>();

        while (sqlReturnValues.next()) {
            invoiceList.add(new InvoiceData(
                    sqlReturnValues.getInt(1),
                    sqlReturnValues.getLong(2),
                    sqlReturnValues.getString(3),
                    sqlReturnValues.getDate(4),
                    sqlReturnValues.getString(5)
            ));
        }
        return invoiceList;
    }

    public void updateUser(IUser user) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT u.id, u.name, r.description as role, u.email, s2.description, f.familyid FROM users u\n" +
                "INNER JOIN families f on u.id = f.familyid\n" +
                "INNER JOIN roles r on u.role = r.id\n" +
                "LEFT JOIN subscriptions s on u.id = s.userid\n" +
                "LEFT OUTER JOIN subscriptiontiers s2 on s.tier = s2.id WHERE u.id = ?");

    }

    public boolean userHasPremium(IUser user) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT userHasPremiumFeatures(?);");
        stmt.setString(1, user.getId());
        ResultSet resultSet = stmt.executeQuery();
        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        } else {
            throw new Exception("Database error. Could not determine if user had premium");
        }
    }

    // Families
    @Override
    public IFamily createFamily(IUser user) throws Exception {
        RandomString randomString = new RandomString(8);
        String newFamilyId = randomString.nextString();
        PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO families(familyId, userId) VALUES(?,?)");
        stmt.setString(1, newFamilyId);
        stmt.setString(2, user.getId());
        stmt.execute();
        if (stmt.getUpdateCount() < 1) {
            return null;
        }

        return new FamilyData(newFamilyId, List.of(user));
    }

    @Override
    public boolean joinFamily(String familyId, IUser user) throws Exception {
        PreparedStatement queryStmt = getConnection().prepareStatement("SELECT COUNT(*) FROM families WHERE familyId = ?");
        queryStmt.setString(1, familyId);
        ResultSet resultSet = queryStmt.executeQuery();
        if (!resultSet.next()) {
            return false;
        }

        int currentFamilyCount = resultSet.getInt(1);
        if (currentFamilyCount >= 6) { //Maxmimum 6 in a family.
            return false;
        }

        PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO families(familyId, userId) VALUES(?,?)");
        stmt.setString(1, familyId);
        stmt.setString(2, user.getId());
        stmt.execute();
        return stmt.getUpdateCount() > 0;
    }

    @Override
    public boolean leaveFamily(IUser user) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM families WHERE userId = ?");
        stmt.setString(1, user.getId());
        stmt.execute();
        return stmt.getUpdateCount() > 0;
    }

    @Override
    public IFamily getFamily(IUser user) throws Exception {
        PreparedStatement stmt = getConnection().prepareStatement("SELECT DISTINCT F.familyid, F.userid, u.name, s2.description as hasFamilySubscription FROM families F\n" +
                "    INNER JOIN users u on u.id = F.userid\n" +
                "    LEFT OUTER JOIN subscriptions s on u.id = s.userid\n" +
                "    LEFT OUTER JOIN subscriptiontiers s2 on s.tier = s2.id\n" +
                "WHERE F.familyid = (SELECT F.familyid FROM families F WHERE F.userid = ?)");
        stmt.setString(1, user.getId());
        ResultSet resultSet = stmt.executeQuery();

        String familyId = "";
        HashMap<String, IUser> users = new HashMap<>();
        while (resultSet.next()) {
            familyId = resultSet.getString(1);
            String userId = resultSet.getString(2);
            if (users.containsKey(userId)) {
                //If the user already got fetched (because that user had more than 1 subscription)
                // we'll ignore the user, unless the newfound tier is family.
                //Ideally we shouldn't hit this, but as we have support for multiple subscriptions, we need to have it.
                if (!users.get(userId).getSubtype().equalsIgnoreCase(SubscriptionType.FAMILY.name())) {
                    continue;
                }
            }
            String tier = resultSet.getString(4);
            if (tier == null) {
                tier = SubscriptionType.FREE.name();
            }
            String name = resultSet.getString(3);
            users.put(userId, new UserData(userId, name, "", "", tier.toUpperCase(), familyId));
        }

        return new FamilyData(familyId, new ArrayList<>(users.values()));
    }

    //Private Methods

    private Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            initializePostgresqlDatabase();
        }
        return this.connection;
    }

    private void initializePostgresqlDatabase() {
        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
            connection = DriverManager.getConnection("jdbc:postgresql://" + url + ":" + port + "/" + databaseName, username, password);
            System.out.println("Connection made");
        } catch (SQLException | IllegalArgumentException ex) {
            ex.printStackTrace(System.err);
        } finally {
            if (connection == null) System.exit(1);
        }
    }

}
