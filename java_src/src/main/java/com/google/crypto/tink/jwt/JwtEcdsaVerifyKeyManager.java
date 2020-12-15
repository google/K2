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
package com.google.crypto.tink.jwt;

import com.google.crypto.tink.KeyTypeManager;
import com.google.crypto.tink.proto.JwtEcdsaAlgorithm;
import com.google.crypto.tink.proto.JwtEcdsaPublicKey;
import com.google.crypto.tink.proto.KeyData.KeyMaterialType;
import com.google.crypto.tink.subtle.EllipticCurves;
import com.google.crypto.tink.subtle.Validators;
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;

/**
 * This key manager produces new instances of {@code JwtEcdsaVerify}. It doesn't support key
 * generation.
 */
class JwtEcdsaVerifyKeyManager extends KeyTypeManager<JwtEcdsaPublicKey> {

  static final EllipticCurves.CurveType getCurve(JwtEcdsaAlgorithm algorithmProto)
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

  static final void validateEcdsaAlgorithm(JwtEcdsaAlgorithm algorithmProto)
      throws GeneralSecurityException {
    // Purposely ignore the result. This function will throw if the algorithm is invalid.
    JwtSigUtil.hashForEcdsaAlgorithm(algorithmProto.name());
  }

  private static class JwtPublicKeyVerifyFactory
      extends KeyTypeManager.PrimitiveFactory<JwtPublicKeyVerify, JwtEcdsaPublicKey> {
    public JwtPublicKeyVerifyFactory() {
      super(JwtPublicKeyVerify.class);
    }

    @Override
    public JwtPublicKeyVerify getPrimitive(JwtEcdsaPublicKey keyProto)
        throws GeneralSecurityException {

      // This will throw an exception is protocol is invalid
      EllipticCurves.CurveType curve = getCurve(keyProto.getAlgorithm());

      ECPublicKey publicKey =
          EllipticCurves.getEcPublicKey(
              curve, keyProto.getX().toByteArray(), keyProto.getY().toByteArray());
      return new JwtEcdsaVerify(publicKey, keyProto.getAlgorithm().name());
    }
  }

  public JwtEcdsaVerifyKeyManager() {
    super(JwtEcdsaPublicKey.class, new JwtPublicKeyVerifyFactory());
  }

  @Override
  public String getKeyType() {
    return "type.googleapis.com/google.crypto.tink.JwtEcdsaPublicKey";
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public KeyMaterialType keyMaterialType() {
    return KeyMaterialType.ASYMMETRIC_PUBLIC;
  }

  @Override
  public JwtEcdsaPublicKey parseKey(ByteString byteString) throws InvalidProtocolBufferException {
    return JwtEcdsaPublicKey.parseFrom(byteString, ExtensionRegistryLite.getEmptyRegistry());
  }

  @Override
  public void validateKey(JwtEcdsaPublicKey pubKey) throws GeneralSecurityException {
    Validators.validateVersion(pubKey.getVersion(), getVersion());
    validateEcdsaAlgorithm(pubKey.getAlgorithm());
  }
}
