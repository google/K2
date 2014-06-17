/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.k2crypto.keyversions;

import com.google.k2crypto.KeyVersionBuilder;
import com.google.k2crypto.SymmetricKey;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class represents an AES key version in K2. It allows you to encrypt and decrypt messaged
 * using AES symmetric key encryption
 *
 * @author John Maheswaran (maheswaran@google.com)
 */
public class AESKeyVersion extends SymmetricKey {
  /**
   * The key length in bytes (128 bits / 8 = 16 bytes) Can be 16, 24 or 32 (NO OTHER VALUES)
   */
  private int keyVersionLength = 16;

  /**
   * SecretKey object representing the key matter in the AES key
   */
  private SecretKey secretKey;

  /**
   * The actual key matter of the AES key used by encKey.
   */
  private byte[] keyVersionMatter = new byte[keyVersionLength];


  /**
   * initialization vector used for encryption and decryption
   */
  private byte[] initvector = new byte[16];

  /**
   * Supported modes: CBC, ECB, OFB, CFB, CTR Unsupported modes: XTS, OCB
   */
  private String mode = "CTR";

  /**
   * Supported padding: PKCS5PADDING Unsupported padding: PKCS7Padding, ISO10126d2Padding,
   * X932Padding, ISO7816d4Padding, ZeroBytePadding
   */
  private String padding = "PKCS5PADDING";

  /**
   * represents the algorithm, mode, and padding to use TODO: change this to allow different modes
   * and paddings (NOT algos - AES ONLY)
   *
   */
  private String algModePadding = "AES/" + this.mode + "/" + padding;


  /**
   * The main method to test the other methods in this class
   *
   * @param args Command line parameters (unused)
   * @throws NoSuchAlgorithmException
   * @throws BadPaddingException
   * @throws IllegalBlockSizeException
   * @throws InvalidAlgorithmParameterException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   */
  public static void main(String[] args)
      throws NoSuchAlgorithmException,
      InvalidKeyException,
      NoSuchPaddingException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {


    // test string that we will encrypt and then decrypt
    String testinput = "weak";
    // print input string
    System.out.println("Input: " + testinput);
    System.out.println();
    // create AES key
    AESKeyVersion key = new AESKeyVersion();
    // encrypt the test string
    byte[] encTxt = key.encryptString(testinput);
    // print out the encrypted string
    System.out.println("Encrypted string: " + encTxt);
    System.out.println();
    // decrypt the message
    String result = key.decryptString(encTxt);
    // print out decrypted message
    System.out.println("Decrypted string: " + result);


  }

  /**
   * Constructor for AESKey. Uses JCE crypto libraries to initialize key matter.
   *
   * @throws NoSuchAlgorithmException This exception is only thrown if someone changes "AES" to an
   *         invalid encryption algorithm. This should never be changed.
   */
  public AESKeyVersion() throws NoSuchAlgorithmException {
    // Generate the key using JCE crypto libraries
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(this.keyLengthInBits());
    secretKey = keyGen.generateKey();
    // save the keyVersionMatter to the local variable keyVersionMatter
    this.keyVersionMatter = secretKey.getEncoded();

    // use this secure random number generator to initialize the vector with random bytes
    SecureRandom prng = new SecureRandom();
    prng.nextBytes(initvector);
    // create the SecretKey object from the byte array
    secretKey = new SecretKeySpec(this.keyVersionMatter, 0, this.keyLengthInBytes(), "AES");

  }

  /**
   * Create an AESKey from saved key matter byte array
   *
   * @param keyVersionMatter The byte array representing the key matter
   */
  public AESKeyVersion(byte[] keyVersionMatter, byte[] initvector) {
    this.setkeyVersionMatter(keyVersionMatter, initvector);
  }

  /**
   * Method to give length of key in BITS. Used to prevent mixing up bytes and bits
   *
   * @return Key length in BITS
   */
  private int keyLengthInBits() {
    return this.keyVersionLength * 8;
  }

  /**
   * Takes an array of bytes and encrypts it using the AES key
   *
   * @param data The byte array of data that we want to encrypt
   * @return Byte array representation of the encrypted data
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws InvalidAlgorithmParameterException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  public byte[] encryptBytes(byte[] data)
      throws NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {
    /**
     * TODO: Change this so we can use different modes of operation (instead of CBC) and different
     * paddings instead of PKCS7 padding
     */
    // make an AES cipher that we can use for encryption
    Cipher encCipher = Cipher.getInstance(algModePadding);


    if (this.mode.equals("CBC") || this.mode.equals("OFB") || this.mode.equals("CFB")
        || this.mode.equals("CTR")) {
      // Initialize the cipher using the secret key of this class and the initialization vector
      encCipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new IvParameterSpec(this.initvector));
    } else if (this.mode.equals("ECB")) {
      // Initialize the cipher using the secret key - ECB does NOT use an initialization vector
      encCipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
    }

    // encrypt the data
    byte[] encryptedData = encCipher.doFinal(data);

    // return the encrypted data
    return encryptedData;
  }

  /**
   * Method to decrypt an encrypted byte array using the AES key
   *
   * @param data The encrypted input data that we want to decrypt
   * @return The byte array representation of the decrypted data
   * @throws InvalidKeyException
   * @throws InvalidAlgorithmParameterException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   */
  public byte[] decryptBytes(byte[] data)
      throws InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      NoSuchAlgorithmException,
      NoSuchPaddingException {
    /**
     * TODO: Change this so we can use different modes of operation (instead of CBC) and different
     * paddings instead of PKCS7 padding
     */
    // make an AES cipher that we can use for decryption
    Cipher decCipher = Cipher.getInstance(algModePadding);


    if (this.mode.equals("CBC") || this.mode.equals("OFB") || this.mode.equals("CFB")
        || this.mode.equals("CTR")) {
      // Initialize the cipher using the secret key of this class and the initialization vector
      decCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initvector));
    } else if (this.mode.equals("ECB")) {
      // Initialize the cipher using the secret key - ECB does NOT use an initialization vector
      decCipher.init(Cipher.DECRYPT_MODE, secretKey);
    }


    // decrypt the data
    byte[] decryptedData = decCipher.doFinal(data);
    // return decrypted byte array
    return decryptedData;
  }

  /**
   * Method that decrypts an encrypted string
   *
   * @param input byte array representation of encrypted message
   * @return String representation of decrypted message
   * @throws InvalidKeyException
   * @throws InvalidAlgorithmParameterException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   */
  public String decryptString(byte[] input)
      throws InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      NoSuchAlgorithmException,
      NoSuchPaddingException {
    // call decrypt bytes method
    byte[] outputData = this.decryptBytes(input);
    // convert to string
    String result = new String(outputData);
    // return result
    return result;
  }

  /**
   * Method to encrypt a string using the AES key
   *
   * @param input The input string that we want to encrypt
   * @return The byte array representation of the AES encrypted version of the string
   * @throws BadPaddingException
   * @throws IllegalBlockSizeException
   * @throws InvalidAlgorithmParameterException
   * @throws NoSuchPaddingException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   */
  public byte[] encryptString(String input)
      throws InvalidKeyException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {
    // Convert the input string to bytes
    byte[] data = input.getBytes();
    // call the encrypt bytes method to encrypt the data
    byte[] encData = this.encryptBytes(data);

    // return the encrypted string
    return encData;
  }

  /**
   * Method to return the key matter to other classes.
   *
   * @return The byte array representation of the key matter
   */
  public byte[] getkeyVersionMatter() {
    return this.keyVersionMatter;
  }

  /**
   * Initializes the key using key matter and initialization vector parameters.
   *
   * @param keyVersionMatter Byte array representation of a key we want to use
   * @param initvector Byte array representation of initialization vector.
   */
  public void setkeyVersionMatter(byte[] keyVersionMatter, byte[] initvector) {

    // save key matter byte array in this object
    this.keyVersionMatter = keyVersionMatter;
    // load the initialization vector
    this.initvector = initvector;

    // initialize secret key using key matter byte array
    secretKey = new SecretKeySpec(this.keyVersionMatter, 0, this.keyLengthInBytes(), "AES");

  }

  /**
   * Method to return the initialization vector to other classes.
   *
   * @return The byte array representation of the initialization vector
   */
  public byte[] getInitVector() {
    return this.initvector;
  }

  /**
   *
   * Method to give length of key in BYTES. Used to prevent mixing up bytes and bits
   *
   * @return Key length in BYTES
   */
  private int keyLengthInBytes() {
    return this.keyVersionLength;
  }


  /**
   * Constructor to make an AESKeyVersion using the AESKeyVersionBuilder. Private to prevent use
   * unless through the AESKeyVersionBuilder
   *
   * @param builder An AESKeyVersionBuilder with all the variables set according to how you want the
   *        AESKeyVersion to be setup.
   * @throws NoSuchAlgorithmException
   */
  private AESKeyVersion(AESKeyVersionBuilder builder) throws NoSuchAlgorithmException {
    // set key version length, mode and padding based on the key version builder
    this.keyVersionLength = builder.keyVersionLength;
    this.mode = builder.mode;
    this.padding = builder.padding;

    // IMPORTANT! update the algorithm/mode/padding string to reflect the new mode and padding
    this.algModePadding = "AES/" + this.mode + "/" + padding;

    // set the key matter and initialization vector from input if is was provided
    if (builder.keyVersionMatterInitVectorProvided) {
      // set key matter and init vector according to provided key matter and init vector
      this.setkeyVersionMatter(builder.keyVersionMatter, builder.initVector);
    } else {
      // Generate the key using JCE crypto libraries
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(this.keyLengthInBits());
      secretKey = keyGen.generateKey();
      // save the keyVersionMatter to the local variable keyVersionMatter
      this.keyVersionMatter = secretKey.getEncoded();

      // use this secure random number generator to initialize the vector with random bytes
      SecureRandom prng = new SecureRandom();
      prng.nextBytes(initvector);
      // create the SecretKey object from the byte array
      secretKey = new SecretKeySpec(this.keyVersionMatter, 0, this.keyLengthInBytes(), "AES");

    }
  }


  /**
   * This class represents a key version builder for AES key versions.
   *
   * @author John Maheswaran (maheswaran@google.com)
   */
  public static class AESKeyVersionBuilder extends KeyVersionBuilder {
    /**
     * key size can be 16, 24 or 32
     */
    private int keyVersionLength = 16;
    /**
     * Supported modes: CBC, ECB, OFB, CFB, CTR Unsupported modes: XTS, OCB
     */
    private String mode = "CTR";

    /**
     * TODO: Supported paddings depends on Java implementation. Upgrade java implementation to
     * support more paddings
     */
    /**
     * Supported padding: PKCS5PADDING Unsupported padding: PKCS7Padding, ISO10126d2Padding,
     * X932Padding, ISO7816d4Padding, ZeroBytePadding
     */
    private String padding = "PKCS5PADDING";

    /**
     * Byte array that will represent the key matter
     */
    private byte[] keyVersionMatter;
    /**
     * Byte array that will represent the initialization vector
     */
    private byte[] initVector;

    /**
     * Flag to indicate to the parent class (AESKeyVersion) whether the key matter and
     * initialization vector have been manually set (true if and only if they have been manually
     * set)
     */
    private boolean keyVersionMatterInitVectorProvided = false;

    /**
     * Public constructor
     *
     * @throws NoSuchAlgorithmException
     */
    public AESKeyVersionBuilder() throws NoSuchAlgorithmException {

    }

    /**
     * Set the key version length
     *
     * @param keyVersionLength Integer representing key version length in BYTES, can be 16, 24, 32
     * @return This object with keyVersionLength updated
     */
    public AESKeyVersionBuilder keyVersionLength(int keyVersionLength) {
      this.keyVersionLength = keyVersionLength;
      return this;
    }

    /**
     * Set the encryption mode
     *
     * @param mode String representing the encryption mode. Supported modes: CBC, ECB, OFB, CFB, CTR
     * @return This object with mode updated
     */
    public AESKeyVersionBuilder mode(String mode) {
      this.mode = mode;
      return this;
    }


    /**
     * Set the padding
     *
     * @param padding String representing the padding. Supported padding: PKCS5PADDING
     * @return This object with padding updated
     */
    public AESKeyVersionBuilder padding(String padding) {
      this.padding = padding;
      return this;
    }

    /**
     *
     * @param keyVersionMatter Byte array representing the key matter
     * @param initVector Byte array representing the initialization vector
     * @return This object with key matter, initialization vector set
     */
    public AESKeyVersionBuilder matterVector(byte[] keyVersionMatter, byte[] initVector) {
      // This flag indicates to the parent class (AESKeyVersion) that the key matter and
      // initialization vector have been manually set
      keyVersionMatterInitVectorProvided = true;
      // set the key matter
      this.keyVersionMatter = keyVersionMatter;
      // set the initialization vector
      this.initVector = initVector;
      return this;
    }


    /**
     * Method to build a new AESKeyVersion
     *
     * @return An AESKeyVersion with the parameters set according to the AESKeyVersionBuilder
     * @throws NoSuchAlgorithmException
     */
    public AESKeyVersion build() throws NoSuchAlgorithmException {
      return new AESKeyVersion(this);
    }

  }
}
