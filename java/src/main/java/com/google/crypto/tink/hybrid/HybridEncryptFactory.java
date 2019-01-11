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
package com.google.crypto.tink.hybrid;

import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeyManager;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PrimitiveSet;
import com.google.crypto.tink.Registry;
import java.security.GeneralSecurityException;

/**
 * Deprecated class to create {@code HybridEncrypt} primitives. Instead of using this class, make
 * sure that the {@code HybridEncryptWrapper} is registered in your binary, then call {@code
 * keysetHandle.GetPrimitive(HybridEncrypt.class)} instead. The required registration happens
 * automatically if you called one of the following in your binary:
 *
 * <ul>
 *   <li>{@code HybridConfig.register()}
 *   <li>{@code TinkConfig.register()}
 * </ul>
 *
 * @deprecated Use {@code keysetHandle.GetPrimitive(HybridEncrypt.class)} after registering the
 *     {@code HybridEncryptWrapper} instead.
 * @since 1.0.0
 */
@Deprecated
public final class HybridEncryptFactory {
  /**
   * @return a HybridEncrypt primitive from a {@code keysetHandle}.
   * @throws GeneralSecurityException
   * @deprecated Use {@code keysetHandle.GetPrimitive(HybridEncrypt.class)} after registering the
   *     {@code HybridEncryptWrapper} instead.
   */
  @Deprecated
  public static HybridEncrypt getPrimitive(KeysetHandle keysetHandle)
      throws GeneralSecurityException {
    return getPrimitive(keysetHandle, /* keyManager= */ null);
  }

  /**
   * @return a HybridEncrypt primitive from a {@code keysetHandle} and a custom {@code keyManager}.
   * @throws GeneralSecurityException
   * @deprecated Use {@code keysetHandle.GetPrimitive(keyManager, HybridEncrypt.class)} after
   *     registering the {@code HybridEncryptWrapper} instead.
   */
  @Deprecated
  public static HybridEncrypt getPrimitive(
      KeysetHandle keysetHandle, final KeyManager<HybridEncrypt> keyManager)
      throws GeneralSecurityException {
    Registry.registerPrimitiveWrapper(new HybridEncryptWrapper());
    final PrimitiveSet<HybridEncrypt> primitives =
        Registry.getPrimitives(keysetHandle, keyManager, HybridEncrypt.class);
    return Registry.wrap(primitives);
  }
}
