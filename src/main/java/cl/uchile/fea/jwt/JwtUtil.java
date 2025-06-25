package cl.uchile.fea.jwt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

/**
 * The JSON Web Token utility.
 */
public final class JwtUtil {

    /**
     * The expiration date pattern.
     */
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss"; // YYYY-MM-DDTHH:MM:SS

    /**
     * Generates the signature using the HMAC-SHA256 algorithm.
     * @param data The data (header and payload) encode in base64
     * @param secret The secret key
     * @return The HS256 signature
     * @throws NoSuchAlgorithmException if no Provider supports a MacSpi implementation for the specified algorithm
     * @throws InvalidKeyException if the given key is inappropriate for initializing this MAC
     */
    private static byte[] getSignature(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(key);

        return mac.doFinal(data.getBytes());
    }

    /**
     * Generates the JSON Web Token.
     * @param rut The RUT (Rol Único Tributario) in {@code 0XXXXXXXXX} format (e.g. 0123456785)
     * @param attended Whether the signature is attended
     * @return The token
     * @throws NoSuchAlgorithmException if no Provider supports a MacSpi implementation for the specified algorithm
     * @throws InvalidKeyException if the given key is inappropriate for initializing this MAC
     */
    public static String generate(String rut, boolean attended) throws NoSuchAlgorithmException, InvalidKeyException {
        Map<String, Object> header = new HashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "HS256");

        String run = rut.replaceFirst("^0*", ""); // remove the leading zeros
        run = run.substring(0, run.length() - 1); // remove the last character (verification digit)

        Map<String, Object> payload = new HashMap<>();
        payload.put("run", run);
        payload.put("entity", "Universidad de Chile");

        String purpose;
        if (attended) {
            purpose = "Propósito General";
        } else {
            purpose = "Desatendido";
        }
        payload.put("purpose", purpose);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 5); // should not be more than 30 minutes from the current time (CLT)

        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN);

        String timezone = System.getenv("APP_TIMEZONE");
        if (timezone != null && !timezone.isEmpty()) {
            sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        }

        String expiration = sdf.format(cal.getTime());

        payload.put("expiration", expiration);

        Gson gson = new Gson();

        String data = String.format("%s.%s",
            Base64.getEncoder().encodeToString(gson.toJson(header).getBytes()),
            Base64.getEncoder().encodeToString(gson.toJson(payload).getBytes())
        );

        return String.format("%s.%s",
            data,
            Base64.getEncoder().encodeToString(getSignature(data, System.getenv("SEGPRES_SECRET")))
        );
    }
}
