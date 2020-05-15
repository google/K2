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
"""Cross-language tests for Hybrid Encryption."""

from absl.testing import absltest
from absl.testing import parameterized

import tink
from tink import hybrid

from tools.testing import supported_key_types
from tools.testing.cross_language.util import cli_hybrid
from tools.testing.cross_language.util import keyset_manager


def setUpModule():
  hybrid.register()


class HybridEncryptionTest(parameterized.TestCase):

  @parameterized.parameters(
      supported_key_types.test_cases(
          supported_key_types.HYBRID_PRIVATE_KEY_TYPES))
  def test_encrypt_decrypt(self, key_template_name, supported_langs):
    key_template = supported_key_types.KEY_TEMPLATE[key_template_name]
    private_handle = keyset_manager.new_keyset_handle(key_template)
    supported_decs = [
        cli_hybrid.CliHybridDecrypt(lang, private_handle)
        for lang in supported_langs
    ]
    unsupported_decs = [
        cli_hybrid.CliHybridDecrypt(lang, private_handle)
        for lang in cli_hybrid.LANGUAGES
        if lang not in supported_langs
    ]
    public_handle = private_handle.public_keyset_handle()
    supported_encs = [
        cli_hybrid.CliHybridEncrypt(lang, public_handle)
        for lang in supported_langs
    ]
    unsupported_encs = [
        cli_hybrid.CliHybridEncrypt(lang, public_handle)
        for lang in cli_hybrid.LANGUAGES
        if lang not in supported_langs
    ]
    for enc in supported_encs:
      plaintext = (
          b'This is some plaintext message to be encrypted using key_template '
          b'%s in %s.' % (key_template_name.encode('utf8'),
                          enc.lang.encode('utf8')))
      context_info = (
          b'Some context info for %s using %s for encryption.' %
          (key_template_name.encode('utf8'), enc.lang.encode('utf8')))
      ciphertext = enc.encrypt(plaintext, context_info)
      for dec in supported_decs:
        output = dec.decrypt(ciphertext, context_info)
        self.assertEqual(output, plaintext)
      for dec in unsupported_decs:
        with self.assertRaises(tink.TinkError):
          dec.decrypt(ciphertext, context_info)
    for enc in unsupported_encs:
      with self.assertRaises(tink.TinkError):
        enc.encrypt(b'plaintext', b'context_info')


if __name__ == '__main__':
  absltest.main()
