# Copyright 2019 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Python wrapper of the CLIF-wrapped C++ Public Key Signature key manager."""

from __future__ import absolute_import
from __future__ import division
# Placeholder for import for type annotations
from __future__ import print_function

from typing import Text

from tink import core
from tink.cc.pybind import cc_key_manager
from tink.cc.pybind import public_key_sign as cc_public_key_sign
from tink.signature import _public_key_sign


class _PublicKeySignCcToPyWrapper(_public_key_sign.PublicKeySign):
  """Transforms cliffed C++ PublicKeySign into a Python primitive."""

  def __init__(self, cc_primitive: cc_public_key_sign.PublicKeySign):
    self._public_key_sign = cc_primitive

  @core.use_tink_errors
  def sign(self, data: bytes) -> bytes:
    return self._public_key_sign.sign(data)


def from_cc_registry(
    type_url: Text
) -> core.PrivateKeyManager[_public_key_sign.PublicKeySign]:
  return core.PrivateKeyManagerCcToPyWrapper(
      cc_key_manager.PublicKeySignKeyManager.from_cc_registry(type_url),
      _public_key_sign.PublicKeySign, _PublicKeySignCcToPyWrapper)
