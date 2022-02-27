package dk.sdu.subscription.Fulfillment;

import dk.sdu.subscription.Interfaces.*;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Route;
import spark.RouteGroup;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.get;

public class InvoiceController implements RouteGroup {
    private static final String VIEW_INVOICE_BASE_PATH = "/invoice/";
    private final IInvoiceService invoiceService;
    private final IDatabaseService databaseService;
    private final IPaymentService paymentService;
    private final IUserService userService;

    public InvoiceController(IInvoiceService invoiceService, IDatabaseService databaseService, IPaymentService paymentService, IUserService userService){
        this.invoiceService = invoiceService;
        this.databaseService = databaseService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @Override
    public void addRoutes() {
        get("/", index());
        get("/list", list());
        get("/:invoiceId/view", viewInvoice());
        get("/:invoiceId", getInvoiceData());
    }

    private Route list() {
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            boolean fetchAll = request.queryParams().contains("all");

            //Fetching invoices
            List<IInvoice> invoices = new ArrayList<>();
            List<IInvoiceData> invoiceDataList = fetchAll && user.hasAdminRights() ? this.databaseService.getAllInvoices() : this.databaseService.getInvoicesFromUserId(user.getId());
            for(IInvoiceData invoiceData : invoiceDataList){
                invoices.add(this.invoiceService.createInvoice(invoiceData, this.databaseService.getUserFromId(invoiceData.getUserId())));
            }

            JSONArray jsonArray = new JSONArray();
            for(IInvoice invoice : invoices){
                JSONObject jsonInvoice = new JSONObject();
                jsonInvoice.put("id", invoice.getId());
                jsonInvoice.put("date", (int) invoice.getDate().getTime()/1000);
                jsonInvoice.put("userId", invoice.getUserId());
                jsonInvoice.put("name", invoice.getUserName());
                jsonInvoice.put("email", invoice.getUserEmail());
                jsonInvoice.put("subType", invoice.getSubtype());
                jsonInvoice.put("totalPrice", invoice.getTotalAmount());
                jsonInvoice.put("currency", "DKK");
                jsonInvoice.put("tax", invoice.getTax());
                jsonInvoice.put("viewInvoiceLink", VIEW_INVOICE_BASE_PATH + invoice.getId() + "/view?user="+user.getId());
                jsonArray.put(jsonInvoice);
            }
            return jsonArray.toString();
        };
    }

    /**
     * Returns an invoice for streaming through the browser
     * @return
     */
    private Route viewInvoice(){
        return (request, response) -> {
            if(!request.queryParams().contains("user")){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "Missing `user` GET parameter. Specifically for this endpoint, you need to put ?user=<userid> on the request.");
            }
            String userId = "";
            try{
                userId = request.queryParams("user");
            }catch(NumberFormatException e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "`user` GET parameter is not a number.");
            }
            IUser user = this.databaseService.getUserFromId(userId);

            IInvoice invoice;
            try {
                int invoiceId = Integer.parseInt(request.params(":invoiceId"));
                IInvoiceData invoiceData = this.databaseService.getInvoiceFromId(invoiceId);
                if(!invoiceData.getUserId().equals(user.getId()) && !user.hasAdminRights()){
                    response.header("Content-Type", "application/json");
                    return RequestUtils.notAcceptable(response, "You cannot access invoices not addressed to you.");
                }

                invoice = this.invoiceService.createInvoice(invoiceData, this.databaseService.getUserFromId(invoiceData.getUserId()));
            }catch(NumberFormatException e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "InvoiceId is not a number");
            }catch(Exception e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "Invalid invoice id: " + e.getMessage());
            }

            ByteArrayOutputStream out;
            try {
                out = invoice.saveToStream();
            }catch(Exception e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "Failed to generate invoice: " + e.getMessage());
            }

            byte[] bytes = out.toByteArray();
            response.header("Content-Type", "application/pdf");
            response.header("Content-Disposition","inline; filename=\"Invoice-"+invoice.getId()+".pdf\"");
            response.header("Content-Length",  String.valueOf(bytes.length));
            response.header("Expires",  "0");

            HttpServletResponse raw = response.raw();
            raw.getOutputStream().write(bytes);
            raw.getOutputStream().flush();
            raw.getOutputStream().close();

            return response.raw();
        };
    }

    /**
     * Gets data about an invoice in JSON format
     * @return
     */
    private Route getInvoiceData(){
        return (request, response) -> {
            response.header("Content-Type", "application/json");
            //Temporary while we don't have any user authentication implemented.
            IUser user = RequestUtils.getCurrentUser(request, response, this.userService);
            boolean fetchAll = request.queryParams().contains("all");

            JSONObject jsonResponse = new JSONObject();
            IInvoice invoice;
            try {
                int invoiceId = Integer.parseInt(request.params(":invoiceId"));
                IInvoiceData invoiceData = this.databaseService.getInvoiceFromId(invoiceId);
                if(invoiceData.getUserId() != user.getId() && !user.hasAdminRights()){
                    response.header("Content-Type", "application/json");
                    return RequestUtils.notAcceptable(response, "You cannot access invoices not addressed to you.");
                }

                invoice = this.invoiceService.createInvoice(invoiceData, this.databaseService.getUserFromId(invoiceData.getUserId()));
            }catch(NumberFormatException e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "InvoiceId is not a number");
            }catch(Exception e){
                response.header("Content-Type", "application/json");
                return RequestUtils.badRequest(response, "Invalid invoice id: " + e.getMessage());
            }

            jsonResponse.put("id", invoice.getId());
            jsonResponse.put("date", (int) invoice.getDate().getTime()/1000);
            jsonResponse.put("userId", invoice.getUserId());
            jsonResponse.put("name", invoice.getUserName());
            jsonResponse.put("email", invoice.getUserEmail());
            jsonResponse.put("subType", invoice.getSubtype());
            jsonResponse.put("totalPrice", invoice.getTotalAmount());
            jsonResponse.put("currency", "DKK");
            jsonResponse.put("tax", invoice.getTax());
            jsonResponse.put("viewInvoiceLink", VIEW_INVOICE_BASE_PATH + invoice.getId() + "/view?user="+user.getId());
            return jsonResponse.toString();
        };
    }

    private Route index() {
        return (Route) (request, response) -> {
            response.header("Content-Type", "application/json");
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("message", "Invoice endpoint endpoint. Use this endpoint to create, send and get invoices");
            jsonObject.put("paymentSuccess",createEndpointDescriptor("Creates an invoice and sends a mail to the user with the invoice attached", "/event", "POST"));
            jsonObject.put("list",createEndpointDescriptor("Returns a list of invoices", "/list", "GET"));
            jsonObject.put("viewInvoice",createEndpointDescriptor("Returns an invoice for streaming through the browser", "/<invoiceID>/view", "GET"));
            jsonObject.put("getInvoiceData",createEndpointDescriptor("Returns data about an invoice in JSON format", "/<invoiceId>", "GET"));



            return jsonObject.toString();
        };
    }

    private static JSONObject createEndpointDescriptor(String description, String endpoint, String method) {
        JSONObject json = new JSONObject();
        json.put("description", description);
        json.put("endpoint", endpoint);
        json.put("method", method);
        return json;
    }

}
