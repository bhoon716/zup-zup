package bhoon.sugang_helper.crawling.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JbnuJumpRequestEncoderTest {

    @Test
    void serializesDataMapWithJumpEnvelopeAndEncryptsIt() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", "2026");
        values.put("strSemstrCd", "SUSR016.010");
        values.put("strCertDivCd", "6");

        String encoded = JbnuJumpRequestEncoder.encode(values);

        assertThat(encoded).startsWith("xb_data=");
        String cipherText = URLDecoder.decode(encoded.substring("xb_data=".length()), StandardCharsets.UTF_8);
        String store = JbnuJumpRequestEncoder.derivedStore();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(store.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(new StringBuilder(store).reverse().substring(0, 16)
                        .getBytes(StandardCharsets.UTF_8)));
        String plainText = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(cipherText)),
                StandardCharsets.UTF_8);

        assertThat(plainText).contains("%40d1%23strYrsa=2026");
        assertThat(plainText).contains("%40d1%23strSemstrCd=SUSR016.010");
        assertThat(plainText).contains("%40d1%23strCertDivCd=6");
        assertThat(plainText).endsWith("%40d1%23tp=dm");
    }

    @Test
    void mapsLegacySemesterCodesToJumpCodes() {
        assertThat(JbnuCourseApiClient.toJumpSemesterCode("U211600010")).isEqualTo("SUSR016.010");
        assertThat(JbnuCourseApiClient.toJumpSemesterCode("U211600020")).isEqualTo("SUSR016.020");
        assertThat(JbnuCourseApiClient.toJumpSemesterCode("SUSR016.015")).isEqualTo("SUSR016.015");
    }
}
