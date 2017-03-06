// Copyright 2017 Google Inc.
//
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

package com.google.cloud.crypto.tink.subtle;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;

import com.google.cloud.crypto.tink.Aead;
import com.google.cloud.crypto.tink.Mac;
import com.google.cloud.crypto.tink.TestUtil;
import com.google.cloud.crypto.tink.subtle.MacJce;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link EncryptThenAuthenticate}. */
@RunWith(JUnit4.class)
public class EncryptThenAuthenticateTest {
  private static class RFCTestVector {
    public byte[] encKey;
    public byte[] macKey;
    public byte[] ciphertext;
    public byte[] aad;
    public String macAlg;
    public int ivSize;
    public int tagLength;
    public RFCTestVector(String macKey, String encKey, String ciphertext, String aad,
        String macAlg, int ivSize, int tagLength) {
      try {
        this.encKey = TestUtil.hexDecode(encKey);
        this.macKey = TestUtil.hexDecode(macKey);
        this.ciphertext = TestUtil.hexDecode(ciphertext);
        this.aad = TestUtil.hexDecode(aad);
        this.macAlg = macAlg;
        this.ivSize = ivSize;
        this.tagLength = tagLength;
      } catch (Exception ignored) {
        // Ignored
      }
    }
  }

  // Test data from https://tools.ietf.org/html/draft-mcgrew-aead-aes-cbc-hmac-sha2-05. As we use
  // CTR while RFC uses CBC mode, it's not possible to compare plaintexts. However, the test is
  // still valueable to make sure that we correcly compute HMAC over ciphertext and aad.
  final RFCTestVector[] rfcTestVectors = {
    new RFCTestVector(
        "000102030405060708090a0b0c0d0e0f",
        "101112131415161718191a1b1c1d1e1f",
        "1af38c2dc2b96ffdd86694092341bc04"
        + "c80edfa32ddf39d5ef00c0b468834279"
        + "a2e46a1b8049f792f76bfe54b903a9c9"
        + "a94ac9b47ad2655c5f10f9aef71427e2"
        + "fc6f9b3f399a221489f16362c7032336"
        + "09d45ac69864e3321cf82935ac4096c8"
        + "6e133314c54019e8ca7980dfa4b9cf1b"
        + "384c486f3a54c51078158ee5d79de59f"
        + "bd34d848b3d69550a67646344427ade5"
        + "4b8851ffb598f7f80074b9473c82e2db"
        + "652c3fa36b0a7c5b3219fab3a30bc1c4",
        "546865207365636f6e64207072696e6369706c65206f662041756775737465204b6572636b686f666673",
        "HMACSHA256", 16, 16),
    new RFCTestVector(
        "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
        "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
        "1af38c2dc2b96ffdd86694092341bc04"
        + "4affaaadb78c31c5da4b1b590d10ffbd"
        + "3dd8d5d302423526912da037ecbcc7bd"
        + "822c301dd67c373bccb584ad3e9279c2"
        + "e6d12a1374b77f077553df829410446b"
        + "36ebd97066296ae6427ea75c2e0846a1"
        + "1a09ccf5370dc80bfecbad28c73f09b3"
        + "a3b75e662a2594410ae496b2e2e6609e"
        + "31e6e02cc837f053d21f37ff4f51950b"
        + "be2638d09dd7a4930930806d0703b1f6"
        + "4dd3b4c088a7f45c216839645b2012bf"
        + "2e6269a8c56a816dbc1b267761955bc5",
        "546865207365636f6e64207072696e6369706c65206f662041756775737465204b6572636b686f666673",
        "HMACSHA512", 16, 32)
  };

  @Test
  public void testRFCVectors() throws Exception {
    for (int i = 0; i < rfcTestVectors.length; i++) {
      RFCTestVector t = rfcTestVectors[i];
      if (Cipher.getMaxAllowedKeyLength("AES") < 256 && t.encKey.length > 16) {
          System.out.println("Unlimited Strength Jurisdiction Policy Files are required"
              + " but not installed. Skip tests with keys larger than 128 bits.");
          continue;
      }
      Aead aead = getAead(t.macKey, t.encKey, t.ivSize, t.tagLength, t.macAlg);
      try {
        aead.decrypt(t.ciphertext, t.aad);
      } catch (GeneralSecurityException e) {
        fail("Ciphertext and aad are valid, shouldn't reach here: " + i + " " + e);
      }
    }
  }

  @Test
  public void testBitFlipCiphertext() throws Exception {
    Aead aead = getAead(Random.randBytes(16), Random.randBytes(16),
        16, 16, "HMACSHA256");
    byte[] plaintext = Random.randBytes(1001);
    byte[] aad = Random.randBytes(13);
    byte[] ciphertext = aead.encrypt(plaintext, aad);
    for (int i = 0; i < ciphertext.length; i++) {
      for (int j = 0; j < 8; j++) {
        byte[] c1 = Arrays.copyOf(ciphertext, ciphertext.length);
        c1[i] = (byte) (c1[i] ^ (1 << j));
        try {
          aead.decrypt(c1, aad);
          fail("Invalid ciphertext, should have failed");
        } catch (GeneralSecurityException expected) {
          // Expected
        }
      }
    }
  }

  @Test
  public void testBitFlipAad() throws Exception {
    Aead aead = getAead(Random.randBytes(16), Random.randBytes(16),
        16, 16, "HMACSHA256");
    byte[] plaintext = Random.randBytes(1001);
    byte[] aad = Random.randBytes(13);
    byte[] ciphertext = aead.encrypt(plaintext, aad);
    for (int i = 0; i < aad.length; i++) {
      for (int j = 0; j < 8; j++) {
        byte[] aad1 = Arrays.copyOf(aad, aad.length);
        aad1[i] = (byte) (aad1[i] ^ (1 << j));
        try {
          aead.decrypt(ciphertext, aad1);
          fail("Invalid aad, should have failed");
        } catch (GeneralSecurityException expected) {
          // Expected
        }
      }
    }
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    Aead aead = getAead(Random.randBytes(16), Random.randBytes(16),
        16, 16, "HMACSHA256");
    byte[] plaintext = Random.randBytes(1001);
    byte[] aad = Random.randBytes(13);
    byte[] ciphertext = aead.encrypt(plaintext, aad);
    try {
      byte[] plaintext1 = aead.decrypt(ciphertext, aad);
      assertArrayEquals(plaintext, plaintext1);
    } catch (GeneralSecurityException e) {
      fail("Valid ciphertext and aad, should have passed");
    }
  }

  @Test
  public void testTruncation() throws Exception {
    Aead aead = getAead(Random.randBytes(16), Random.randBytes(16),
        16, 16, "HMACSHA256");
    byte[] plaintext = Random.randBytes(1001);
    byte[] aad = Random.randBytes(13);
    byte[] ciphertext = aead.encrypt(plaintext, aad);
    for (int i = 1; i < ciphertext.length; i++) {
      byte[] c1 = Arrays.copyOf(ciphertext, ciphertext.length - i);
      try {
        aead.decrypt(c1, aad);
        fail("Invalid ciphertext, should have failed");
      } catch (GeneralSecurityException expected) {
        // Expected
      }
    }
  }

  private Aead getAead(byte[] hmacKey, byte[] encKey, int ivSize, int tagLength,
      String macAlg) throws Exception {
    IndCpaCipher cipher = new AesCtrJceCipher(encKey, ivSize);
    SecretKeySpec keySpec = new SecretKeySpec(hmacKey, "HMAC");
    Mac mac = new MacJce(macAlg, keySpec, tagLength);
    return new EncryptThenAuthenticate(cipher, mac, tagLength);
  }
}
