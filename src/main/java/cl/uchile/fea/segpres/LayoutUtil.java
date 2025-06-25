package cl.uchile.fea.segpres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The layout utility.
 */
public final class LayoutUtil {

    /**
     * The charset.
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * The application ID.
     */
    public static final String APPLICATION_ID = "THIS-CONFIG";

    /**
     * Transforms the document to XML layout.
     * @param document The document
     * @return The XML layout
     * @throws TransformerException
     * @throws IOException if an I/O error occurs
     */
    private static String toXml(Document document) throws TransformerException, IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty("omit-xml-declaration", "no");
            transformer.setOutputProperty("method", "xml");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("encoding", CHARSET.name());
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(document), new StreamResult(new OutputStreamWriter(baos, CHARSET)));

            return new String(baos.toByteArray(), CHARSET);
        }
    }

    /**
     * Gets the Application element.
     * <pre>
     * &lt;AgileSignerConfig&gt;
     *     &lt;Application id=&quot;THIS-CONFIG&quot; /&gt;
     * &lt;/AgileSignerConfig&gt;
     * </pre>
     * @param document The document
     * @return The application element
     */
    private static Element getApplicationElement(Document document) {
        Element root = document.createElement("AgileSignerConfig");

        document.appendChild(root);

        Element applicationElement = document.createElement("Application");

        applicationElement.setAttribute("id", APPLICATION_ID);
        applicationElement.setIdAttribute("id", true);

        root.appendChild(applicationElement);

        // TODO: pdfPassword element?

        return applicationElement;
    }

    /**
     * Gets the XML with an Invisible element.
     * <pre>
     * &lt;AgileSignerConfig&gt;
     *     &lt;Application id=&quot;THIS-CONFIG&quot;&gt;
     *         &lt;Signature&gt;
     *             &lt;Invisible /&gt;
     *         &lt;/Signature&gt;
     *     &lt;/Application&gt;
     * &lt;/AgileSignerConfig&gt;
     * </pre>
     * @return The XML layout
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested
     * @throws TransformerException
     * @throws IOException if an I/O error occurs
     */
    public static String getInvisibleLayout() throws ParserConfigurationException, TransformerException, IOException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element applicationElement = getApplicationElement(document);

        Element signatureElement = document.createElement("Signature");

        applicationElement.appendChild(signatureElement);

        Element invisibleElement = document.createElement("Invisible");

        signatureElement.appendChild(invisibleElement);

        return toXml(document);
    }

    /**
     * Gets the XML with a Visible element.
     * <pre>
     * &lt;AgileSignerConfig&gt;
     *     &lt;Application id=&quot;THIS-CONFIG&quot;&gt;
     *         &lt;Signature&gt;
     *             &lt;Visible active=&quot;true&quot; layer2=&quot;false&quot; label=&quot;true&quot; pos=&quot;1&quot;&gt;
     *                 &lt;llx /&gt;
     *                 &lt;lly /&gt;
     *                 &lt;urx /&gt;
     *                 &lt;ury /&gt;
     *                 &lt;page /&gt;
     *                 &lt;image&gt;BASE64&lt;/image&gt;
     *                 &lt;BASE64VALUE /&gt;
     *             &lt;/Visible&gt;
     *         &lt;/Signature&gt;
     *     &lt;/Application&gt;
     * &lt;/AgileSignerConfig&gt;
     * </pre>
     * @param llx The lower left X coordinate
     * @param lly The lower left Y coordinate
     * @param urx The upper right X coordinate
     * @param ury The upper right Y coordinate
     * @param page The page (0 or less to set the LAST page)
     * @param base64Value The signature image encoded in base64
     * @return The XML layout
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested
     * @throws TransformerException
     * @throws IOException if an I/O error occurs
     */
    public static String getVisibleLayout(int llx, int lly, int urx, int ury, int page, String base64Value) throws ParserConfigurationException, TransformerException, IOException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element applicationElement = getApplicationElement(document);

        Element signatureElement = document.createElement("Signature");

        applicationElement.appendChild(signatureElement);

        Element visibleElement = document.createElement("Visible");

        signatureElement.appendChild(visibleElement);

        visibleElement.setAttribute("active", "true");
        visibleElement.setAttribute("layer2", "false");
        visibleElement.setAttribute("label", "true");
        visibleElement.setAttribute("pos", "1");

        Element llxElement = document.createElement("llx");
        llxElement.setTextContent(String.valueOf(llx < 0 ? 0 : llx));
        visibleElement.appendChild(llxElement);

        Element llyElement = document.createElement("lly");
        llyElement.setTextContent(String.valueOf(lly < 0 ? 0 : lly));
        visibleElement.appendChild(llyElement);

        Element urxElement = document.createElement("urx");
        urxElement.setTextContent(String.valueOf(urx < 0 ? 0 : urx));
        visibleElement.appendChild(urxElement);

        Element uryElement = document.createElement("ury");
        uryElement.setTextContent(String.valueOf(ury < 0 ? 0 : ury));
        visibleElement.appendChild(uryElement);

        Element pageElement = document.createElement("page");
        if (page < 1) {
            pageElement.setTextContent("LAST");
        } else {
            pageElement.setTextContent(String.valueOf(page));
        }
        visibleElement.appendChild(pageElement);

        Element imageElement = document.createElement("image");
        imageElement.setTextContent("BASE64");
        visibleElement.appendChild(imageElement);

        Element base64ValueElement = document.createElement("BASE64VALUE");
        base64ValueElement.setTextContent(base64Value);
        visibleElement.appendChild(base64ValueElement);

        return toXml(document);
    }
}
