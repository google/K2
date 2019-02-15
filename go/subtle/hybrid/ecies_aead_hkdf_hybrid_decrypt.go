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

package hybrid

import (
	"errors"

	"github.com/google/tink/go/tink"
)

// EciesAeadHkdfHybridDecrypt is an instance of ECIES decryption with HKDF-KEM (key encapsulation mechanism)
// and AEAD-DEM (data encapsulation mechanism).
type EciesAeadHkdfHybridDecrypt struct {
	privateKey   *ECPrivateKey
	hkdfSalt     []byte
	hkdfHMACAlgo string
	pointFormat  string
	demHelper    EciesAeadHkdfDemHelper
}

var _ tink.HybridDecrypt = (*EciesAeadHkdfHybridDecrypt)(nil)

// NewEciesAeadHkdfHybridDecrypt returns ECIES decryption construct with HKDF-KEM (key encapsulation mechanism)
// and AEAD-DEM (data encapsulation mechanism).
func NewEciesAeadHkdfHybridDecrypt(pvt *ECPrivateKey, hkdfSalt []byte, hkdfHMACAlgo string, ptFormat string, demHelper EciesAeadHkdfDemHelper) (*EciesAeadHkdfHybridDecrypt, error) {
	return &EciesAeadHkdfHybridDecrypt{
		privateKey:   pvt,
		hkdfSalt:     hkdfSalt,
		hkdfHMACAlgo: hkdfHMACAlgo,
		pointFormat:  ptFormat,
		demHelper:    demHelper,
	}, nil
}

// Decrypt is used to decrypt using ECIES with a HKDF-KEM and AEAD-DEM mechanisms.
func (e *EciesAeadHkdfHybridDecrypt) Decrypt(ciphertext, contextInfo []byte) ([]byte, error) {
	curve := e.privateKey.PublicKey.Curve

	headerSize, err := encodingSizeInBytes(curve, e.pointFormat)
	if err != nil {
		return nil, err
	}
	if len(ciphertext) < headerSize {
		return nil, errors.New("ciphertext too short")
	}
	kemBytes := ciphertext[:headerSize]
	rKem := &ECIESHKDFRecipientKem{
		recipientPrivateKey: e.privateKey,
	}
	symmetricKey, err := rKem.decapsulate(kemBytes, e.hkdfHMACAlgo, e.hkdfSalt, contextInfo, e.demHelper.getSymmetricKeySize(), e.pointFormat)
	if err != nil {
		return nil, err
	}
	aead, err := e.demHelper.getAead(symmetricKey)
	if err != nil {
		return nil, err
	}
	return aead.Decrypt(ciphertext[headerSize:], []byte{})
}
