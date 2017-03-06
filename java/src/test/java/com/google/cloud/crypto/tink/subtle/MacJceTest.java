package com.google.cloud.crypto.tink.subtle;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import com.google.cloud.crypto.tink.Mac;
import com.google.cloud.crypto.tink.TestUtil;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link MacJce}. */
@RunWith(JUnit4.class)
public class MacJceTest {
  private static class NISTTestVector {
    String algName;
    public byte[] key;
    public byte[] message;
    public byte[] tag;

    public NISTTestVector(String algName, String key, String message, String tag) {
      this.algName = algName;
      this.key = TestUtil.hexDecode(key);
      this.message = TestUtil.hexDecode(message);
      this.tag = TestUtil.hexDecode(tag);
    }
  }

  // Test data from http://csrc.nist.gov/groups/STM/cavp/message-authentication.html#testing.
  final NISTTestVector[] nistTestVectors = {
    new NISTTestVector(
        "HMACSHA1",
        "816aa4c3ee066310ac1e6666cf830c375355c3c8ba18cfe1f50a48c988b46272",
        "220248f5e6d7a49335b3f91374f18bb8b0ff5e8b9a5853f3cfb293855d78301d837a0a2eb9e4f056f06c08361"
      + "bd07180ee802651e69726c28910d2baef379606815dcbab01d0dc7acb0ba8e65a2928130da0522f2b2b3d05260"
      + "885cf1c64f14ca3145313c685b0274bf6a1cb38e4f99895c6a8cc72fbe0e52c01766fede78a1a",
        "17cb2e9e98b748b5ae0f7078ea5519e5"),
    new NISTTestVector(
        "HMACSHA256",
        "6f35628d65813435534b5d67fbdb54cb33403d04e843103e6399f806cb5df95febbdd61236f33245",
        "752cff52e4b90768558e5369e75d97c69643509a5e5904e0a386cbe4d0970ef73f918f675945a9aefe26daea27"
      + "587e8dc909dd56fd0468805f834039b345f855cfe19c44b55af241fff3ffcd8045cd5c288e6c4e284c3720570b"
      + "58e4d47b8feeedc52fd1401f698a209fccfa3b4c0d9a797b046a2759f82a54c41ccd7b5f592b",
        "05d1243e6465ed9620c9aec1c351a186"),
    new NISTTestVector(
        "HMACSHA512",
        "726374c4b8df517510db9159b730f93431e0cd468d4f3821eab0edb93abd0fba46ab4f1ef35d54fec3d85fa89e"
      + "f72ff3d35f22cf5ab69e205c10afcdf4aaf11338dbb12073474fddb556e60b8ee52f91163ba314303ee0c910e6"
      + "4e87fbf302214edbe3f2",
        "ac939659dc5f668c9969c0530422e3417a462c8b665e8db25a883a625f7aa59b89c5ad0ece5712ca17442d1798"
      + "c6dea25d82c5db260cb59c75ae650be56569c1bd2d612cc57e71315917f116bbfa65a0aeb8af7840ee83d3e710"
      + "1c52cf652d2773531b7a6bdd690b846a741816c860819270522a5b0cdfa1d736c501c583d916",
        "bd3d2df6f9d284b421a43e5f9cb94bc4ff88a88243f1f0133bad0fb1791f6569"),
  };

  @Test
  public void testNistVectors() throws Exception {
    for (int i = 0; i < nistTestVectors.length; i++) {
      NISTTestVector t = nistTestVectors[i];
      SecretKeySpec keySpec = new SecretKeySpec(t.key, "HMAC");
      Mac mac = new MacJce(t.algName, keySpec, t.tag.length);
      assertArrayEquals(t.tag, mac.computeMac(t.message));
      assertTrue(mac.verifyMac(t.tag, t.message));
    }
  }

  @Test
  public void testTagTruncation() throws Exception {
    for (int i = 0; i < nistTestVectors.length; i++) {
      NISTTestVector t = nistTestVectors[i];
      SecretKeySpec keySpec = new SecretKeySpec(t.key, "HMAC");
      Mac mac = new MacJce(t.algName, keySpec, t.tag.length);
      for (int j = 1; j < t.tag.length; j++) {
        byte[] modifiedTag = Arrays.copyOf(t.tag, t.tag.length - j);
        boolean verified = true;
        try {
          verified = mac.verifyMac(modifiedTag, t.message);
        } catch (GeneralSecurityException expected) {
          verified = false;
        }
        assertFalse(verified);
      }
    }
  }

  @Test
  public void testBitFlipMessage() throws Exception {
    for (int i = 0; i < nistTestVectors.length; i++) {
      NISTTestVector t = nistTestVectors[i];
      SecretKeySpec keySpec = new SecretKeySpec(t.key, "HMAC");
      Mac mac = new MacJce(t.algName, keySpec, t.tag.length);
      for (int b = 0; b < t.message.length; b++) {
        for (int bit = 0; bit < 8; bit++) {
          byte[] modifiedMessage = Arrays.copyOf(t.message, t.message.length);
          modifiedMessage[b] = (byte) (modifiedMessage[b] ^ (1 << bit));
          boolean verified = true;
          try {
            verified = mac.verifyMac(t.tag, modifiedMessage);
          } catch (GeneralSecurityException expected) {
            verified = false;
          }
          assertFalse(verified);
        }
      }
    }
  }

  @Test
  public void testBitFlipTag() throws Exception {
    for (int i = 0; i < nistTestVectors.length; i++) {
      NISTTestVector t = nistTestVectors[i];
      SecretKeySpec keySpec = new SecretKeySpec(t.key, "HMAC");
      Mac mac = new MacJce(t.algName, keySpec, t.tag.length);
      for (int b = 0; b < t.tag.length; b++) {
        for (int bit = 0; bit < 8; bit++) {
          byte[] modifiedTag = Arrays.copyOf(t.tag, t.tag.length);
          modifiedTag[b] = (byte) (modifiedTag[b] ^ (1 << bit));
          boolean verified = true;
          try {
            verified = mac.verifyMac(modifiedTag, t.message);
          } catch (GeneralSecurityException expected) {
            verified = false;
          }
          assertFalse(verified);
        }
      }
    }
  }
}
