package dk.sdu.subscription.ServiceLayer.InvoiceService;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.VerticalPositionMark;
import dk.sdu.subscription.Interfaces.Exceptions.InvoiceException;
import dk.sdu.subscription.Interfaces.IInvoice;
import dk.sdu.subscription.Interfaces.IInvoiceData;
import dk.sdu.subscription.Interfaces.IUser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Invoice implements IInvoice, IInvoiceData {
    private int id;
    private Date date;
    private long totalAmount;
    private String subtype;
    private String userName;
    private String userEmail;
    private String userId;

    // <editor-fold defaulstate="collapsed" desc="Getters / Setters">
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserId() {
        return userId;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }
    // </editor-fold>

    public Invoice(int id, long totalAmount, String subtype, Date date, String userName, String userEmail, String userId) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.subtype = subtype;
        this.date = date;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userId  = userId;

        if(logo == null) {
            try {
                BufferedImage image = ImageIO.read(Invoice.class.getResourceAsStream("/public/images/logo.png"));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                logo = Image.getInstance(baos.toByteArray());
                logo.scaleToFit(100, 100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BadElementException e) {
                e.printStackTrace();
            }
        }
    }

    public Invoice(int id, long totalAmount, String subtype, Date date, IUser user) {
        this(id, totalAmount, subtype, date, user.getName(), user.getEmail(), user.getId());
    }


    private static Image logo;
    private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18,
            Font.BOLD);
    private static Font bold = new Font(Font.FontFamily.TIMES_ROMAN, 12,
            Font.BOLD);
    private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 10,
            Font.BOLD);

    private Document createInvoice(OutputStream out) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        addMetaData(document);
        addContent(document);
        document.close();
        return document;
    }

    public void saveToFile(String path) throws InvoiceException {
        try {
            createInvoice(new FileOutputStream(path));
        } catch (IOException e) {
            throw new InvoiceException("Failed to load pdf resources or save file: " + e.getMessage());
        } catch (DocumentException e) {
            e.printStackTrace();
            throw new InvoiceException("Failed ot create PDF document: " + e.getMessage());
        }
    }

    public ByteArrayOutputStream saveToStream() throws InvoiceException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            createInvoice(out);
        } catch (IOException e) {
            throw new InvoiceException("Failed to load pdf resources");
        } catch (DocumentException e) {
            e.printStackTrace();
            throw new InvoiceException("Failed ot create PDF document: " + e.getMessage());
        }
        return out;
    }

    public double getTax(){
        return (this.getTotalAmount()/100d) * (1 - 1 / (1d + 0.25));
    }

    private static void addMetaData(Document document) {
        document.addTitle("Invoice");
        document.addAuthor("Subscription Team");
        document.addSubject("Your invoice");
        document.addKeywords("Java, PDF, iText");
    }

    private void addContent(Document document) throws DocumentException, IOException {

        // Create empty paragraphs of different sizes
        Paragraph emptyLine3 = new Paragraph();
        addEmptyLine(emptyLine3, 3);
        Paragraph emptyLine1 = new Paragraph();
        addEmptyLine(emptyLine1, 1);
        Paragraph emptyLine6 = new Paragraph();
        addEmptyLine(emptyLine6, 6);

        // Title
        Paragraph title = new Paragraph("Subscription Team ApS\n Campusvej 55, 5230 Odense M", catFont);
        title.setAlignment(Element.ALIGN_RIGHT);

        // Info on firm + logo image
        Paragraph firmInfo = new Paragraph("Billed to: \n" + this.getUserName() + " \n" + this.getUserEmail());
        firmInfo.add(addImage(new Paragraph()));

        // Date of invoice + invoice no.
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Paragraph pDate = new Paragraph("Date: " + formatter.format(this.getDate()));
        Chunk glue = new Chunk(new VerticalPositionMark());
        pDate.add(new Chunk(glue));
        pDate.add("Invoice no. " + this.getId());

        // Table
        PdfPTable table = createTable();
        table.setWidthPercentage(100);
        Paragraph tableTitle = new Paragraph("Invoice");

        // Bank info lines
        Paragraph pBankInfo = new Paragraph("Amount is paid to: \nBank / Reg no. 1234 / Account no. 1234567890 \nInvoice no. " + this.getId() + "\nSubscription Team ApS: DK-VAT: 29283958", smallBold);

        // Adding all the paragraphs to the document
        document.add(title);
        document.add(emptyLine6);
        document.add(firmInfo);
        document.add(emptyLine3);
        document.add(pDate);
        document.add(emptyLine3);
        document.add(tableTitle);
        document.add(emptyLine1);
        document.add(table);
        document.add(emptyLine6);
        document.add(emptyLine6);
        document.add(pBankInfo);
    }

    private Paragraph addImage(Paragraph pImage) throws BadElementException, IOException {
        pImage.add(new Chunk(logo, 0, 0));
        pImage.setAlignment(Element.ALIGN_RIGHT);
        return pImage;
    }

    private PdfPTable createTable() throws DocumentException {
        // Create table
        PdfPTable table = new PdfPTable(3);
        table.setWidths(new float[]{5, 2, 2});

        // Row 1
        PdfPCell c1 = new PdfPCell(new Phrase("Description", bold));
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setBackgroundColor(new BaseColor(211, 211, 211, 100));
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Qty", bold));
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setBackgroundColor(new BaseColor(211, 211, 211, 100));
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Amount", bold));
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setBackgroundColor(new BaseColor(211, 211, 211, 100));
        table.addCell(c1);
        table.setHeaderRows(1);

        // Row 2
        c1 = new PdfPCell(new Phrase(this.getSubtype()));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("1"));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("kr. " + String.format("%.2f", this.getTotalAmount()/100d).replace(".", ",")));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);

        // Row 3
        PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
        PdfPCell emptyCellWithBorder = new PdfPCell(new Phrase(" "));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(emptyCell);
        table.addCell(emptyCell);
        table.addCell(emptyCell);

        // Row 4
        emptyCellWithBorder.setBorder(Rectangle.TOP);
        table.addCell(emptyCellWithBorder);

        c1 = new PdfPCell(new Phrase("Subtotal"));
        c1.setBorder(Rectangle.TOP);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("kr. " + String.format("%.2f", this.getTotalAmount()/100d).replace(".", ",")));
        c1.setBorder(Rectangle.TOP);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);

        // Row 5
        table.addCell(emptyCell);

        c1 = new PdfPCell(new Phrase("Tax(25%)"));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("kr. " + String.format("%.2f", getTax()).replace('.', ',')));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);

        // Row 6
        table.addCell(emptyCell);

        c1 = new PdfPCell(new Phrase("Total DKK", bold));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c1.setBackgroundColor(new BaseColor(211, 211, 211, 80));
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("kr. " + String.format("%.2f", this.getTotalAmount()/100d).replace('.', ','), bold));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setBackgroundColor(new BaseColor(211, 211, 211, 80));
        table.addCell(c1);

        return table;
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }
}
