package cl.uchile.fea.segpres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfSignatureAppearance.RenderingMode;
import com.itextpdf.text.pdf.PdfStamper;

import cl.uchile.fea.Utils;
import cl.uchile.fea.segpres.models.SignatureRequest;
import co.elastic.apm.api.CaptureSpan;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;

/**
 * The Segpres service.
 */
public class SegpresService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SegpresService.class);

    /**
     * The PDF Contents bytes.
     */
    public static final int CONTENTS = 15000;

    /**
     * The location.
     */
    private String location;
    /**
     * The reason.
     */
    private String reason;

    /**
     * The username.
     */
    private String username;
    /**
     * The password.
     */
    private String password;

    /**
     * Whether the signature is attended.
     */
    private boolean attended;

    /**
     * Sets the location.
     * @param location The location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the reason.
     * @param reason The reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Sets the username.
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the password, usually known as OTP (One Time Password).
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets whether the signature is attended.
     * @param attended Whether the signature is attended
     */
    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    /**
     * Generates the Segpres PDF signature appearance.
     * @param reader The PDF reader
     * @param baos The output stream
     * @param layout The XML layout
     * @return The PDF signature appearance
     * @throws IOException on error
     * @throws com.itextpdf.text.DocumentException on error
     * @throws org.dom4j.DocumentException if an error occurs during parsing
     * @throws NoSuchAlgorithmException on unsupported signature algorithms
     * @throws NoSuchProviderException if there's no default provider
     * @throws OperatorCreationException
     * @throws CertificateException on encoding errors
     * @throws InvalidKeyException on incorrect key
     * @throws SignatureException on signature errors
     */
    @CaptureSpan
    public PdfSignatureAppearance generate(PdfReader reader, ByteArrayOutputStream baos, String layout) throws
        IOException,
        com.itextpdf.text.DocumentException, org.dom4j.DocumentException,
        NoSuchAlgorithmException, NoSuchProviderException,
        OperatorCreationException,
        CertificateException, InvalidKeyException, SignatureException {
        Span span = ElasticApm.currentSpan();
        span.setName(String.format("Generate: User %s", username));

        PdfStamper stamper = PdfStamper.createSignature(reader, baos, '\0', null, true);

        // Layout parsing

        SAXReader saxReader = new SAXReader();

        try (InputStream in = new ByteArrayInputStream(layout.getBytes())) {
            Document document = saxReader.read(in);

            Element root = document.getRootElement();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Layout XML: {}", document.asXML());
            }

            int page = reader.getNumberOfPages();

            Element pageElement = (Element)root.selectSingleNode(String.format("/AgileSignerConfig/Application[@id='%s']/Signature/Visible/page", LayoutUtil.APPLICATION_ID));
            if (pageElement != null) {
                String text = pageElement.getText();
                if (!"LAST".equalsIgnoreCase(text)) {
                    page = Integer.valueOf(text);
                }

                if (page < 1) {
                    page = 1;
                } else if (page > reader.getNumberOfPages()) {
                    page = reader.getNumberOfPages();
                }
            }

            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

            Element visibleElement = (Element)root.selectSingleNode(String.format("/AgileSignerConfig/Application[@id='%s']/Signature/Visible[@active='true']", LayoutUtil.APPLICATION_ID));
            if (visibleElement != null) {
                float llx = Float.valueOf(visibleElement.selectSingleNode("llx").getText());
                float lly = Float.valueOf(visibleElement.selectSingleNode("lly").getText());
                float urx = Float.valueOf(visibleElement.selectSingleNode("urx").getText());
                float ury = Float.valueOf(visibleElement.selectSingleNode("ury").getText());

                String base64Value = visibleElement.selectSingleNode("BASE64VALUE").getText();

                Image signatureGraphic = Image.getInstance(Base64.getDecoder().decode(base64Value.getBytes()));

                appearance.setSignatureGraphic(signatureGraphic);
                appearance.setVisibleSignature(new Rectangle(llx, lly, urx, ury), page, null);
            }

            appearance.setLocation(location);
            appearance.setReason(reason);

            if (visibleElement != null && visibleElement.selectSingleNode(".[@layer2='true']") == null) {
                appearance.setRenderingMode(RenderingMode.GRAPHIC);

                StringBuffer buf = new StringBuffer();
                buf.append(" ");

                appearance.setLayer2Text(buf.toString());
            } else {
                appearance.setRenderingMode(RenderingMode.GRAPHIC);

                StringBuffer buf = new StringBuffer();
                buf.append(" ");

                //buf.append("Signed by ").append(username).append('\n');
                //SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");
                //buf.append("Date: ").append(sdf.format(new GregorianCalendar().getTime()));

                appearance.setLayer2Text(buf.toString());
            }

            //LOGGER.trace("Layer 2 Text: {}", appearance.getLayer2Text());

            //appearance.setAcro6Layers(true); // deprecated

            PdfSignature dictionary = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));

            dictionary.setLocation(appearance.getLocation());
            dictionary.setReason(appearance.getReason());
            dictionary.setContact(appearance.getContact());
            dictionary.setDate(new PdfDate(appearance.getSignDate()));

            appearance.setCryptoDictionary(dictionary);

            // Generate self signed certificate

            X500Name owner = new X500Name("CN=Temp"); // issuer and subject
            Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // from yesterday
            Date notAfter = new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000); // in 2 years

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(1024, new SecureRandom());

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                owner,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                owner,
                keyPair.getPublic()
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
            X509CertificateHolder certHolder = builder.build(signer);
            X509Certificate signCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

            signCertificate.verify(keyPair.getPublic());

            appearance.setCertificate(signCertificate);

            HashMap<PdfName, Integer> exclusionSizes = new HashMap<>();
            exclusionSizes.put(PdfName.CONTENTS, (CONTENTS * 2 + 2)); // in hexadecimal
            appearance.preClose(exclusionSizes);

            return appearance;
        }
    }

    /**
     * Executes the HTTP request to Segpres.
     * @param request The request
     * @return A custom HTTP response
     * @throws URISyntaxException if the input is not a valid URI
     * @throws IOException on error
     */
    @CaptureSpan
    public CustomHttpResponse execute(SignatureRequest request) throws URISyntaxException, IOException {
        URIBuilder builder = new URIBuilder(System.getenv("SEGPRES_BASE_URL"));
        //builder.appendPath("firma/v2/files/tickets");
        builder.setPath("firma/v2/files/tickets");

        int timeout = 60*1000; // 60 seconds (1 minute)
        try {
            timeout = Utils.getEnv("APP_TIMEOUT", 10*1000, 300*1000, timeout); // between 10 and 300 seconds (5 minutes)
        } catch (NumberFormatException e) {
            LOGGER.warn("Unable to get timeout ({}), using {} ms", e.getMessage(), timeout);
        }

        // ConnectionConfig connConfig = ConnectionConfig.custom()
        //     .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
        //     .setSocketTimeout(timeout, TimeUnit.MILLISECONDS)
        //     .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();

        // BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
        // connManager.setConnectionConfig(connConfig);

        //try (CloseableHttpClient httpClient = HttpClients.createMinimal(connManager)) {
        try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
            HttpPost httpPost = new HttpPost(builder.build());

            httpPost.setConfig(requestConfig);

            if (attended && password != null) {
                httpPost.addHeader("OTP", password.trim());
            }

            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            Gson gson = new Gson();

            Map<String, Object> data = new HashMap<>();
            data.put("api_token_key", request.getApiTokenKey());
            data.put("token", request.getToken());
            data.put("hashes", request.getHashes());

            String body = gson.toJson(data);

            LOGGER.trace("Signature Request: {}", body);

            httpPost.setEntity(new StringEntity(body));

            // HttpClientResponseHandler<CustomHttpResponse> responseHandler = new HttpClientResponseHandler<CustomHttpResponse>() {

            //     @Override
            //     public CustomHttpResponse handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
            //         HttpEntity entity = response.getEntity();
            //         if (entity == null) {
            //             throw new ClientProtocolException("Got null entity");
            //         }

            //         try (ByteArrayOutputStream baos = new ByteArrayOutputStream());
            //             entity.writeTo(baos);

            //             String body = baos.toString();

            //             LOGGER.trace("Signature Response: {}", body);

            //             return new CustomHttpResponse(response.getCode(), body);
            //         }
            //     }
            // };

            // return httpClient.execute(httpPost, responseHandler);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new ClientProtocolException("Got null entity");
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    entity.writeTo(baos);

                    body = baos.toString();

                    LOGGER.trace("Signature Response: {}", body);

                    return new CustomHttpResponse(response.getStatusLine().getStatusCode(), body);
                }
            }
        }
    }
}
