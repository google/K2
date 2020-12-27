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

import static java.nio.charset.StandardCharsets.US_ASCII;

import com.google.crypto.tink.Mac;
import com.google.crypto.tink.subtle.PrfHmacJce;
import com.google.crypto.tink.subtle.PrfMac;
import com.google.errorprone.annotations.Immutable;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import org.json.JSONObject;

/** An implementation of {@link JwtMac} using HMAC. */
@Immutable
public final class JwtHmac implements JwtMac {
  private static final int MIN_KEY_SIZE_IN_BYTES = 32;

  @SuppressWarnings("Immutable") // We do not mutate the mac.
  private final Mac mac;

  private final String algo;

  public JwtHmac(String algo, java.security.Key key) throws GeneralSecurityException {
    if (key.getEncoded().length < MIN_KEY_SIZE_IN_BYTES) {
      throw new InvalidAlgorithmParameterException(
          "key size too small, need at least " + MIN_KEY_SIZE_IN_BYTES + " bytes");
    }

    this.algo = algo;
    PrfHmacJce prf = new PrfHmacJce(getHmacAlgo(algo), key);
    this.mac = new PrfMac(prf, prf.getMaxOutputLength());
  }

  @Override
  public String createCompact(RawJwt token) throws GeneralSecurityException {
    String unsignedCompact = JwtFormat.createUnsignedCompact(this.algo, token.getPayload());
    return JwtFormat.createSignedCompact(
        unsignedCompact, mac.computeMac(unsignedCompact.getBytes(US_ASCII)));
  }

  @Override
  public VerifiedJwt verifyCompact(String compact, JwtValidator validator)
      throws GeneralSecurityException {
    String[] parts = compact.split("\\.", -1);
    if (parts.length != 3) {
      throw new JwtInvalidException(
          "only tokens in JWS compact serialization format are supported");
    }

    String unsignedCompact = parts[0] + "." + parts[1];
    byte[] expectedTag = JwtFormat.decodeSignature(parts[2]);
    mac.verifyMac(expectedTag, unsignedCompact.getBytes(US_ASCII));
    JwtFormat.validateHeader(this.algo, JwtFormat.decodeHeader(parts[0]));
    JSONObject payload = JwtFormat.decodePayload(parts[1]);
    RawJwt token = new RawJwt.Builder(payload).build();
    return validator.validate(token);
  }

  private static String getHmacAlgo(String algo) {
    switch (algo) {
      case "HS256":
        return "HMACSHA256";
      case "HS384":
        return "HMACSHA384";
      case "HS512":
        return "HMACSHA512";
      default:
        throw new IllegalArgumentException("unsupported JWT HMAC algorithm: " + algo);
    }
  }
}
