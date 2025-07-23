package utils

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/go-goll/libsignal-protocol-go/ecc"
)

func VerifySignature(uid string, base64Signature string, base64PublicKey string) error {
	pubBytes, err := base64.StdEncoding.DecodeString(base64PublicKey)
	if len(pubBytes) == 33 && pubBytes[0] == 0x05 {
		pubBytes = pubBytes[1:]
	}
	if err != nil {
		return err
	}

	if len(pubBytes) != 32 {
		return errors.New("public key must be 32 bytes")
	}

	var pubArray [32]byte
	copy(pubArray[:], pubBytes)

	sigBytes, err := base64.StdEncoding.DecodeString(base64Signature)
	if err != nil {
		return err
	}

	pubKey := ecc.NewDjbECPublicKey(pubArray)

	if !ecc.VerifySignature(pubKey, []byte(uid), signatureArray(sigBytes)) {
		return errors.New("signature verification failed")
	}

	return nil
}

func signatureArray(bs []byte) [64]byte {
	var arr [64]byte
	copy(arr[:], bs)
	return arr
}

func FetchIdentityPublicKey(accountServiceBaseURL string, uid string) (string, error) {
	url := fmt.Sprintf("http://"+"%s/account/fetch/identitykey/%s", accountServiceBaseURL, uid)

	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("error fetching identity public key: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("account service responded with %d", resp.StatusCode)
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	var parsed struct {
		UID               string `json:"uid"`
		IdentityPublicKey string `json:"identityPublicKey"`
	}
	if err := json.Unmarshal(body, &parsed); err != nil {
		return "", err
	}

	return parsed.IdentityPublicKey, nil
}
