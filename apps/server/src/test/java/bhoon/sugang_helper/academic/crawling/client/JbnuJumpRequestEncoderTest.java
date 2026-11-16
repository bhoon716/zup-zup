package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JbnuJumpRequestEncoderTest {

    private static final String XB_DATA_PREFIX = "xb_data=";

    @Test
    @DisplayName("조회 데이터를 JUMP 요청 형식으로 직렬화하고 암호화한다")
    void serializesDataMapWithJumpEnvelopeAndEncryptsIt() throws Exception {
        // given
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", "2026");
        values.put("strSemstrCd", "SUSR016.010");
        values.put("strCertDivCd", "6");

        // when
        String encoded = JbnuJumpRequestEncoder.encode(values);

        // then
        assertThat(encoded).startsWith(XB_DATA_PREFIX);
        String cipherText = URLDecoder.decode(encoded.substring(XB_DATA_PREFIX.length()), StandardCharsets.UTF_8);
        String store = JbnuJumpRequestEncoder.derivedStore();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(store.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(new StringBuilder(store).reverse().substring(0, 16)
                        .getBytes(StandardCharsets.UTF_8)));
        String plainText = new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)),
                StandardCharsets.UTF_8);

        assertThat(plainText).contains("%40d1%23strYrsa=2026");
        assertThat(plainText).contains("%40d1%23strSemstrCd=SUSR016.010");
        assertThat(plainText).contains("%40d1%23strCertDivCd=6");
        assertThat(plainText).endsWith("%40d1%23tp=dm");
    }

    @Test
    @DisplayName("기존 학기 코드를 JUMP 학기 코드로 변환한다")
    void mapsLegacySemesterCodesToJumpCodes() {
        // when & then
        assertThat(SemesterType.toJumpCode("U211600010")).isEqualTo("SUSR016.010");
        assertThat(SemesterType.toJumpCode("U211600020")).isEqualTo("SUSR016.020");
        assertThat(SemesterType.toJumpCode("U211600015")).isEqualTo("SUSR016.015");
        assertThat(SemesterType.toJumpCode("U211600025")).isEqualTo("SUSR016.025");
        assertThat(SemesterType.toJumpCode("U211600016")).isEqualTo("SUSR016.016");
        assertThat(SemesterType.toJumpCode("U211600026")).isEqualTo("SUSR016.026");
        assertThat(SemesterType.toJumpCode("U211600009")).isEqualTo("SUSR016.009");
        assertThat(SemesterType.toJumpCode("U211600008")).isEqualTo("SUSR016.008");
        assertThat(SemesterType.toJumpCode("SUSR016.015")).isEqualTo("SUSR016.015");
        assertThat(SemesterType.toJumpCode("UNKNOWN")).isEqualTo("UNKNOWN");
        assertThat(SemesterType.toJumpCode("")).isEmpty();
        assertThatNullPointerException().isThrownBy(() -> SemesterType.toJumpCode(null));
    }

    @Test
    @DisplayName("인증 구분이 비어 있어도 JUMP 요청을 직렬화하고 암호화한다")
    void serializesDataMapWithEmptyCertDivisionAndEncryptsIt() throws Exception {
        // given
        Map<String, String> values = new LinkedHashMap<>();
        values.put("strYrsa", "2026");
        values.put("strSemstrCd", "SUSR016.010");
        values.put("strCertDivCd", "");

        // when
        String encoded = JbnuJumpRequestEncoder.encode(values);

        // then
        assertThat(encoded).startsWith(XB_DATA_PREFIX);
        String cipherText = URLDecoder.decode(encoded.substring(XB_DATA_PREFIX.length()), StandardCharsets.UTF_8);
        String store = JbnuJumpRequestEncoder.derivedStore();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(store.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(new StringBuilder(store).reverse().substring(0, 16)
                        .getBytes(StandardCharsets.UTF_8)));
        String plainText = new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)),
                StandardCharsets.UTF_8);

        assertThat(plainText).contains("%40d1%23strCertDivCd=");
        assertThat(plainText).contains("%40d1%23strYrsa=2026");
    }
}
