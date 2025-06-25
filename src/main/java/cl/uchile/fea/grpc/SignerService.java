package cl.uchile.fea.grpc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.http.HttpStatus;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfString;

import cl.uchile.fea.HttpStatusCode;
import cl.uchile.fea.jwt.JwtUtil;
import cl.uchile.fea.lib.proto.SignReply;
import cl.uchile.fea.lib.proto.SignRequest;
import cl.uchile.fea.lib.proto.SignerGrpc;
import cl.uchile.fea.segpres.CustomHttpResponse;
import cl.uchile.fea.segpres.LayoutUtil;
import cl.uchile.fea.segpres.SegpresService;
import cl.uchile.fea.segpres.models.ErrorResponse;
import cl.uchile.fea.segpres.models.HashRequest;
import cl.uchile.fea.segpres.models.HashResponse;
import cl.uchile.fea.segpres.models.SignatureRequest;
import cl.uchile.fea.segpres.models.SignatureResponse;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import io.grpc.stub.StreamObserver;

/**
 * The signer service.
 * @see <a href="https://grpc.io/docs/languages/java/basics/#server">Creating the server</a>
 */
public class SignerService extends SignerGrpc.SignerImplBase {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SignerService.class);

    /**
     * Reads the RSA private key in PKCS #8 standard.
     * @param file The key file
     * @return The private key
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    private PrivateKey getPrivateKey(File file) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (FileReader keyReader = new FileReader(file); PemReader pemReader = new PemReader(keyReader)) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);

            return factory.generatePrivate(privKeySpec);
        }
    }

    /**
     * Executes the HTTP request.
     * @param builder The builder
     * @param service The service
     * @param request The signature request
     * @return The PDF dictionary
     * @throws InterruptedException
     * @throws SignException
     */
    private PdfDictionary execute(SignReply.Builder builder, SegpresService service, SignatureRequest request) throws InterruptedException, SignException {
        PdfDictionary dictionary = new PdfDictionary();

        CustomHttpResponse response = null;
        for (int i = 0; i < 3; i++) { // TODO: the maximum attempts should be an environment variable?
            try {
                response = service.execute(request);

                break;
            } catch (Exception e) {
                builder.setMessage(e.getMessage());
            }

            Thread.sleep(1000);
        }

        if (response != null) {
            Gson gson = new Gson();

            int code = response.getCode();
            String body = response.getBody();
            if (code == HttpStatus.SC_OK) {
                SignatureResponse signatureResponse = gson.fromJson(body, SignatureResponse.class); // should not fail

                List<HashResponse> hashes = signatureResponse.getHashes();
                if (hashes != null && !hashes.isEmpty()) {
                    String content = hashes.get(0).getContent();
                    if (content != null && !content.isEmpty()) {
                        //LOGGER.trace("Content: {}", content);

                        byte[] decoded = Base64.getDecoder().decode(content);
                        byte[] padded = new byte[SegpresService.CONTENTS];
                        System.arraycopy(decoded, 0, padded, 0, decoded.length);

                        dictionary.put(PdfName.CONTENTS, new PdfString(padded).setHexWriting(true));

                        builder.setSuccess(true);
                        builder.setMessage("We are ready!");

                        LOGGER.debug(builder.getMessage());
                    } else {
                        throw new SignException("Got null or empty content");
                    }
                } else {
                    throw new SignException("Got null or empty hashes");
                }
            } else {
                String error;
                try {
                    ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);

                    error = errorResponse.getError();
                } catch (Exception e) {
                    //LOGGER.warn("Unable to parse error response", e);

                    error = HttpStatusCode.getByCode(code).getDescription(); // should not fail
                }

                LOGGER.warn("Got invalid status code {} ({})", code, error);

                builder.setMessage(String.format("%d - %s", code, error));

                switch (code) {
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_PRECONDITION_FAILED:
                    if ("El OTP ingresado no es válido".equals(error) ||
                        "ERROR : Verificación de OTP fallido. Por favor vuelve a intentar".equals(error) ||
                        "El OTP ingresado no es válido. Favor validar la configuración de su dispositivo e intentar nuevamente".equals(error)) {
                        builder.setInvalidPassword(true);
                    }

                    break;
                default:
                    // BUG: this should be a 4xx error code, but Sepgres returns 500 Internal Server Error
                    if (code >= HttpStatus.SC_INTERNAL_SERVER_ERROR && !"ERROR : Formato de contenido o layout incorrecto".equals(error)) {
                        builder.setRetry(true);
                    }

                    break;
                }
            }
        } else {
            LOGGER.error("Unable to send request ({})", builder.getMessage());

            builder.setRetry(true);
        }

        return dictionary;
    }

    /**
     * Generates the PDF signature appearance and executes the HTTP request.
     * @param builder The builder
     * @param rut The RUT
     * @param password The password
     * @param file The file
     * @param layout The XML layout
     * @param attended Whether the signature is attended
     * @throws IOException on error
     * @throws com.itextpdf.text.DocumentException on error
     * @throws org.dom4j.DocumentException if an error occurs during parsing
     * @throws NoSuchAlgorithmException on unsupported signature algorithms
     * @throws NoSuchProviderException if there's no default provider
     * @throws OperatorCreationException
     * @throws CertificateException on encoding errors
     * @throws InvalidKeyException on incorrect key
     * @throws SignatureException on signature errors
     * @throws InterruptedException
     * @throws SignException
     */
    private void generateAndExecute(SignReply.Builder builder, String rut, String password, ByteString file, String layout, boolean attended) throws
        IOException,
        com.itextpdf.text.DocumentException, org.dom4j.DocumentException,
        NoSuchAlgorithmException, NoSuchProviderException,
        OperatorCreationException,
        CertificateException, InvalidKeyException, SignatureException,
        InterruptedException,
        SignException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfDictionary dictionary = null;
        PdfSignatureAppearance appearance = null;
        try {
            PdfReader reader = new PdfReader(file.toByteArray());

            SegpresService service = new SegpresService();

            service.setReason(""); // reason?
            service.setLocation(""); // location?

            service.setUsername(rut);
            service.setPassword(password);

            service.setAttended(attended);

            appearance = service.generate(reader, baos, layout);

            InputStream rs = appearance.getRangeStream();

            byte[] input = new byte[8192];
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            int n;
            while ((n = rs.read(input)) > 0) {
                md.update(input, 0, n);
            }

            HashRequest hash = new HashRequest();
            hash.setContent(Base64.getEncoder().encodeToString(md.digest()));

            List<HashRequest> hashes = new ArrayList<>();
            hashes.add(hash);

            SignatureRequest request = new SignatureRequest();
            request.setApiTokenKey(System.getenv("SEGPRES_API_TOKEN_KEY"));
            request.setToken(JwtUtil.generate(rut, attended));
            request.setHashes(hashes);

            dictionary = execute(builder, service, request);
        } finally {
            if (appearance != null) {
                if (dictionary == null) {
                    dictionary = new PdfDictionary();
                }

                if (!builder.getSuccess() || dictionary.size() == 0) {
                    dictionary.put(PdfName.CONTENTS, new PdfString()); // empty
                }

                appearance.close(dictionary);

                if (builder.getSuccess()) {
                    builder.setFile(ByteString.copyFrom(baos.toByteArray()));
                }
            }

            baos.close();
        }
    }

    /**
     * Sends a file to sign.
     * @param request The sign request
     * @param responseObserver The response observer
     */
    @Override
    public void send(SignRequest request, StreamObserver<SignReply> responseObserver) {
        SignReply.Builder builder = SignReply.newBuilder();

        // BEWARE: uncomment this only for debugging
        // if (LOGGER.isTraceEnabled()) {
        //     LOGGER.trace("Sign Request: {}", request.toString());
        // }

        builder.setSuccess(false);
        builder.setInvalidPassword(false);
        builder.setRetry(false);

        try {
            String rut = request.getRut();
            if (rut == null || rut.isEmpty()) {
                throw new SignException("Got null or empty RUT");
            }

            Transaction transaction = ElasticApm.currentTransaction();
            transaction.setUser(rut, null, null);

            String password = null;
            String encryptedPassword = request.getPassword();
            if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                File file = new File("secret/private.pem");

                PrivateKey key = getPrivateKey(file);

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, key);

                byte[] decryptedPassword = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));

                password = new String(decryptedPassword, StandardCharsets.UTF_8);

                //LOGGER.debug("Password: {}", password);
            }

            ByteString file = request.getFile();
            if (file == null || file.isEmpty()) {
                throw new SignException("Got null or empty file");
            }

            String layout;
            ByteString signature = request.getSignature();
            if (signature == null || signature.isEmpty()) {
                layout = LayoutUtil.getInvisibleLayout();
            } else {
                layout = LayoutUtil.getVisibleLayout(
                    request.getLlx(),
                    request.getLly(),
                    request.getUrx(),
                    request.getUry(),
                    request.getPage(),
                    Base64.getEncoder().encodeToString(signature.toByteArray())
                );
            }

            generateAndExecute(builder, rut, password, file, layout, request.getAttended());
        } catch (Exception e) {
            LOGGER.error("Unable to sign", e);

            builder.setMessage(e.getMessage());

            //responseObserver.onError(e);
            //return;
        }

        // BEWARE: uncomment this only for debugging
        // if (LOGGER.isTraceEnabled()) {
        //     LOGGER.trace("Sign Reply: {}", builder.toString());
        // }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
