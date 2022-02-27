
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.sdu.subscription.ServiceLayer.InvoiceService.InvoiceServiceProvider;
import dk.sdu.subscription.Interfaces.*;
import dk.sdu.subscription.ServiceLayer.DatabaseService.DatabaseServiceProvider;
import dk.sdu.subscription.ServiceLayer.PaymentService.PaymentServiceProvider;
import dk.sdu.subscription.ServiceLayer.SecretService.SecretServiceProvider;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.Date;

public class DatabaseServiceTest {

    private ISecretService secretService;
    private IPaymentService paymentService;
    private IDatabaseService databaseService;
    private IInvoiceService invoiceService;
    private Connection connection = null;
    private String url;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        secretService = SecretServiceProvider.getSecretService();

        url = secretService.getStringSecret("database_url");
        port = Integer.parseInt(secretService.getStringSecret("database_port"));
        databaseName = secretService.getStringSecret("database_name");
        username = secretService.getStringSecret("database_username");
        password = secretService.getStringSecret("database_password");

        paymentService = PaymentServiceProvider.getPaymentService(secretService);
        databaseService = DatabaseServiceProvider.getDatabaseService(secretService);
        invoiceService = InvoiceServiceProvider.getInvoiceService();
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + url + ":" + port + "/" + databaseName, username, password);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Get user")
    void getUserById() throws Exception {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO users(id, name, role, email, testdata) VALUES('useridtest', 'testname', 2,'test@email.com', true)");
        stmt.executeUpdate();
        PreparedStatement subscription = connection.prepareStatement("INSERT INTO subscriptions(id, userid, tier, subscription_id) VALUES(-10,'useridtest',1,'sub_useridtest')");
        subscription.executeUpdate();
        try {
            IUser testUser = databaseService.getUserFromId("useridtest");
            assertEquals("useridtest", testUser.getId());
            assertEquals("testname", testUser.getName());
            assertEquals("Artist", testUser.getRole());
            assertEquals("test@email.com", testUser.getEmail());
            assertEquals("Student", testUser.getSubtype());
            System.out.println("Get User test -success");
        }catch(AssertionError error) {
            System.out.println("Something failed");
            throw error;
        } finally {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM subscriptions WHERE userid = 'useridtest'");
            delete.executeUpdate();
            delete = connection.prepareStatement("DELETE FROM users WHERE id = 'useridtest'");
            delete.executeUpdate();
        }
    }

    @Test
    @DisplayName("Get Invoice")
    void getInvoiceFromId() throws Exception {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO invoices(id, timestamp, amount, userid, subscriptionTierId) VALUES(-19, '2001-01-01 21:21:21', '17900', 'unit-test', 1)");
        stmt.executeUpdate();
        try {
            IInvoiceData testInvoice = databaseService.getInvoiceFromId(-19);
            assertEquals(-19, testInvoice.getId());
            assertEquals(17900, testInvoice.getTotalAmount());
            assertEquals("Student", testInvoice.getSubtype());
            assertEquals("unit-test", testInvoice.getUserId());
            System.out.println("Get Invoice test -success");
        }catch (AssertionError error) {
            System.out.println("Something failed");
            throw error;
        } finally {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM invoices WHERE id = -19");
            delete.executeUpdate();
        }
    }
}
