package com.google.k2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Tests for AESKey class
 *
 * @author John Maheswaran (maheswaran@google.com)
 */
public class AESKeyTest {


  /**
   * This tests the encryption and decryption methods of the AESKey class.
   *
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws NoSuchPaddingException
   * @throws InvalidAlgorithmParameterException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  @Test
  public void testEncryptDecrypt()
      throws NoSuchAlgorithmException,
      InvalidKeyException,
      NoSuchPaddingException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {

    // test text string that we will encrypt and then decrypt
    String testinput = "weak";
    // create AES key
    AESKey key = new AESKey();
    // encrypt the test string
    byte[] encTxt = key.encryptString(testinput);
    // decrypt the message
    String result = key.decryptString(encTxt);
    // test that the decrypted result is the same as the encryption input
    assertEquals(testinput, result);

    // empty string test
    testinput = "";
    // create AES key
    key = new AESKey();
    // encrypt the test string
    encTxt = key.encryptString(testinput);
    // decrypt the message
    result = key.decryptString(encTxt);
    // test that the decrypted result is the same as the encryption input
    assertEquals(testinput, result);



    // fail("Not yet implemented");

  }
}
