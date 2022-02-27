package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.IDatabaseService;
import dk.sdu.subscription.Interfaces.IPaymentService;
import dk.sdu.subscription.Interfaces.IUser;
import dk.sdu.subscription.Interfaces.IUserService;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;
import spark.RouteGroup;

import java.text.DecimalFormat;

import static spark.Spark.*;

public class CashFlowController implements RouteGroup {
    private final IPaymentService paymentService;
    private final IDatabaseService databaseService;
    private final IUserService userService;

    int TWO_HOURS_MILLIS = 7200000;
    double balance = 0;
    double pendingIncome = 0;
    double expectedIncomeFromSubscriptions = 0;
    double lastMonthIncomeFromSubscriptions = 0;
    double lastMonthIncomeSum = 0;
    long timeSinceCached = 0;
    double adsAmount = 0;

    public CashFlowController(IPaymentService paymentService, IDatabaseService databaseService, IUserService userService) {
        this.paymentService = paymentService;
        this.databaseService = databaseService;
        this.userService = userService;
    }

    @Override
    public void addRoutes() {
        get("/", cashflow());
    }

    private Route cashflow() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            if (user.hasAdminRights()) {
                if (timeSinceCached < System.currentTimeMillis() + TWO_HOURS_MILLIS) {
                    timeSinceCached = System.currentTimeMillis();
                    balance = paymentService.getBalance();
                    pendingIncome = paymentService.getPendingIncome();
                    expectedIncomeFromSubscriptions = paymentService.getExpectedIncomeFromSubscriptions();
                    lastMonthIncomeFromSubscriptions = paymentService.getLastMonthIncomeFromSubscriptions();
                    lastMonthIncomeSum = paymentService.getLastMonthIncomeSum();
                    adsAmount = paymentService.getLastMonthIncomeFromAds();
                }
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("currentMonthIncomeSubscriptions", lastMonthIncomeFromSubscriptions);
                jsonObject.put("expectedIncomeSubscriptions", expectedIncomeFromSubscriptions);
                jsonObject.put("pendingIncome", pendingIncome);
                jsonObject.put("balance", balance);
                jsonObject.put("currentMonthIncomeAdds", adsAmount);
                jsonObject.put("currentMonthIncomeSum", lastMonthIncomeSum);
                jsonArray.put(jsonObject);
                return jsonArray.toString();
            } else {
                return RequestUtils.notAcceptable(response, "Only admins can access CashFlow");
            }
        };

    }

}
