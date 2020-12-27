// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////
package com.google.crypto.tink.jwt;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTypeManager;
import com.google.crypto.tink.proto.JwtEcdsaAlgorithm;
import com.google.crypto.tink.proto.JwtEcdsaKeyFormat;
import com.google.crypto.tink.proto.JwtEcdsaPrivateKey;
import com.google.crypto.tink.proto.JwtEcdsaPublicKey;
import com.google.crypto.tink.proto.KeyData.KeyMaterialType;
import com.google.crypto.tink.subtle.EllipticCurves;
import com.google.crypto.tink.subtle.Random;
import com.google.crypto.tink.testing.TestUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistryLite;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.Set;
import java.util.TreeSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for JwtEcdsaSignKeyManager. */
@RunWith(JUnitParamsRunner.class)
public class JwtEcdsaSignKeyManagerTest {
  private final JwtEcdsaSignKeyManager manager = new JwtEcdsaSignKeyManager();
  private final KeyTypeManager.KeyFactory<JwtEcdsaKeyFormat, JwtEcdsaPrivateKey> factory =
      manager.keyFactory();

  private static JwtEcdsaKeyFormat createKeyFormat(JwtEcdsaAlgorithm algorithm) {
    return JwtEcdsaKeyFormat.newBuilder().setAlgorithm(algorithm).build();
  }

  private static Object[] parametersAlgos() {
    return new Object[] {JwtEcdsaAlgorithm.ES256, JwtEcdsaAlgorithm.ES384, JwtEcdsaAlgorithm.ES512};
  }

  private static final String algorithmToString(JwtEcdsaAlgorithm algo)
      throws GeneralSecurityException {
    switch (algo) {
      case ES256:
        return "ES256";
      case ES384:
        return "ES384";
      case ES512:
        return "ES512";
      default: // fall out
    }
    throw new GeneralSecurityException("unknown algorithm " + algo.name());
  }

  static final EllipticCurves.CurveType algorithmToCurve(JwtEcdsaAlgorithm algorithmProto)
      throws GeneralSecurityException {

    switch (algorithmProto) {
      case ES256:
        return EllipticCurves.CurveType.NIST_P256;
      case ES384:
        return EllipticCurves.CurveType.NIST_P384;
      case ES512:
        return EllipticCurves.CurveType.NIST_P521;
      default:
        throw new GeneralSecurityException("unknown algorithm " + algorithmProto.name());
    }
  }

  @Test
  public void basics() throws Exception {
    assertThat(manager.getKeyType())
        .isEqualTo("type.googleapis.com/google.crypto.tink.JwtEcdsaPrivateKey");
    assertThat(manager.getVersion()).isEqualTo(0);
    assertThat(manager.keyMaterialType()).isEqualTo(KeyMaterialType.ASYMMETRIC_PRIVATE);
  }

  @Test
  public void validateKeyFormat_empty_throw() throws Exception {
    assertThrows(
        GeneralSecurityException.class,
        () -> factory.validateKeyFormat(JwtEcdsaKeyFormat.getDefaultInstance()));
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void validateKeyFormat_ok(JwtEcdsaAlgorithm algorithm) throws GeneralSecurityException {
    JwtEcdsaKeyFormat format = createKeyFormat(algorithm);
    factory.validateKeyFormat(format);
  }

  private static void checkConsistency(JwtEcdsaPrivateKey privateKey, JwtEcdsaKeyFormat keyFormat) {
    assertThat(privateKey.getPublicKey().getAlgorithm()).isEqualTo(keyFormat.getAlgorithm());
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void createKeys_ok(JwtEcdsaAlgorithm algorithm) throws Exception {

    JwtEcdsaKeyFormat format = createKeyFormat(algorithm);
    JwtEcdsaPrivateKey key = factory.createKey(format);
    checkConsistency(key, format);
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void createKey_alwaysNewElement_ok(JwtEcdsaAlgorithm algorithm) throws Exception {

    JwtEcdsaKeyFormat format = createKeyFormat(algorithm);
    Set<String> keys = new TreeSet<>();
    // Calls newKey multiple times and make sure that they generate different keys -- takes about a
    // second per key.
    int numTests = 5;
    for (int i = 0; i < numTests; i++) {
      JwtEcdsaPrivateKey key = factory.createKey(format);
      keys.add(TestUtil.hexEncode(key.getKeyValue().toByteArray()));
    }
    assertThat(keys).hasSize(numTests);
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void getPublicKey_checkValues(JwtEcdsaAlgorithm algorithm) throws Exception {
    JwtEcdsaPrivateKey privateKey = factory.createKey(createKeyFormat(algorithm));
    JwtEcdsaPublicKey publicKey = manager.getPublicKey(privateKey);

    assertThat(publicKey).isEqualTo(privateKey.getPublicKey());
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void createPrimitive_ok(JwtEcdsaAlgorithm algorithm) throws Exception {

    JwtEcdsaKeyFormat format = createKeyFormat(algorithm);
    JwtEcdsaPrivateKey key = factory.createKey(format);
    JwtEcdsaPublicKey pubKey = key.getPublicKey();
    JwtPublicKeySign signer = manager.getPrimitive(key, JwtPublicKeySign.class);
    ECPublicKey publicKey =
        EllipticCurves.getEcPublicKey(
            algorithmToCurve(algorithm), pubKey.getX().toByteArray(), pubKey.getY().toByteArray());
    JwtPublicKeyVerify verifier = new JwtEcdsaVerify(publicKey, algorithmToString(algorithm));
    RawJwt token = new RawJwt.Builder().build();
    JwtValidator validator = new JwtValidator.Builder().build();
    verifier.verify(signer.sign(token), validator);
  }

  @Test
  @Parameters(method = "parametersAlgos")
  public void createCorruptedPublicKeyPrimitive_throws(JwtEcdsaAlgorithm algorithm)
      throws Exception {

    JwtEcdsaKeyFormat format = createKeyFormat(algorithm);
    JwtEcdsaPrivateKey originalKey = factory.createKey(format);
    byte[] originalPubX = originalKey.getPublicKey().getX().toByteArray();
    byte[] originalPubY = originalKey.getPublicKey().getY().toByteArray();
    originalPubX[0] = (byte) (originalPubX[0] ^ 0x01);
    ByteString corruptedPubX = ByteString.copyFrom(originalPubX);
    JwtEcdsaPublicKey corruptedPub =
        JwtEcdsaPublicKey.newBuilder()
            .setVersion(originalKey.getPublicKey().getVersion())
            .setAlgorithm(algorithm)
            .setX(corruptedPubX)
            .setY(ByteString.copyFrom(originalPubY))
            .build();
    JwtEcdsaPrivateKey corruptedKey =
        JwtEcdsaPrivateKey.newBuilder()
            .setVersion(originalKey.getVersion())
            .setPublicKey(corruptedPub)
            .setKeyValue(originalKey.getKeyValue())
            .build();
    assertThrows(
        GeneralSecurityException.class,
        () -> manager.getPrimitive(corruptedKey, JwtPublicKeySign.class));
  }

  @Test
  public void testDeriveKey_throw() throws Exception {
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            factory.deriveKey(
                JwtEcdsaKeyFormat.getDefaultInstance(),
                new ByteArrayInputStream(Random.randBytes(100))));
  }

  private static void checkTemplate(KeyTemplate template, JwtEcdsaAlgorithm algorithm)
      throws Exception {
    assertThat(template.getTypeUrl()).isEqualTo(new JwtEcdsaSignKeyManager().getKeyType());
    assertThat(template.getOutputPrefixType()).isEqualTo(KeyTemplate.OutputPrefixType.RAW);
    JwtEcdsaKeyFormat format =
        JwtEcdsaKeyFormat.parseFrom(template.getValue(), ExtensionRegistryLite.getEmptyRegistry());
    assertThat(format.getAlgorithm()).isEqualTo(algorithm);
  }

  @Test
  public void testJwtES256Template_ok() throws Exception {
    KeyTemplate template = JwtEcdsaSignKeyManager.jwtES256Template();
    checkTemplate(template, JwtEcdsaAlgorithm.ES256);
  }

  @Test
  public void testJwtES384Template_ok() throws Exception {
    KeyTemplate template = JwtEcdsaSignKeyManager.jwtES384Template();
    checkTemplate(template, JwtEcdsaAlgorithm.ES384);
  }

  @Test
  public void testJwtES512Template_ok() throws Exception {
    KeyTemplate template = JwtEcdsaSignKeyManager.jwtES512Template();
    checkTemplate(template, JwtEcdsaAlgorithm.ES512);
  }

  @Test
  public void testJwtES256TemplateWithManager_ok() throws Exception {
    JwtEcdsaKeyFormat format =
        JwtEcdsaKeyFormat.parseFrom(
            JwtEcdsaSignKeyManager.jwtES256Template().getValue(),
            ExtensionRegistryLite.getEmptyRegistry());
    new JwtEcdsaSignKeyManager().keyFactory().validateKeyFormat(format);
  }

  @Test
  public void testJwtES384TemplateWithManager_ok() throws Exception {
    JwtEcdsaKeyFormat format =
        JwtEcdsaKeyFormat.parseFrom(
            JwtEcdsaSignKeyManager.jwtES384Template().getValue(),
            ExtensionRegistryLite.getEmptyRegistry());
    new JwtEcdsaSignKeyManager().keyFactory().validateKeyFormat(format);
  }

  @Test
  public void testJwtES512TemplateWithManager_ok() throws Exception {
    JwtEcdsaKeyFormat format =
        JwtEcdsaKeyFormat.parseFrom(
            JwtEcdsaSignKeyManager.jwtES512Template().getValue(),
            ExtensionRegistryLite.getEmptyRegistry());
    new JwtEcdsaSignKeyManager().keyFactory().validateKeyFormat(format);
  }
}
