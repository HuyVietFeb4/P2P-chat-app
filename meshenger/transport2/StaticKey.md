# Static Key Management

## 1. X25519 (Key Agreement)
Used exclusively for the **Noise Protocol** handshake to establish session keys.

* **Characteristics:**
    * **Asymmetric Pair:** Consists of a Public Key (shared over the network) and a Private Key (**strictly local**).
    * **Persistence:** Generated once per app installation.
    * **Generation:** Managed via **BouncyCastle** (Software-backed).
    * **Storage:** Encrypted and stored in the local Database.
* **Access Workflow:**
    1.  Check for existing keys in the Database.
    2.  If absent: Generate a new pair, encrypt, and save to DB.
    3.  If present: Generate the **Key Alias** using the standard formula.
    4.  Retrieve the **AES Secret Key** from the `AndroidKeyStore` using that alias.
    5.  Retrieve the encrypted X25519 blob from the DB.
    6.  Decrypt the X25519 keys using the AES key.
* **Storage Workflow:**
    1.  Generate a unique **Key Alias** via the formula.
    2.  Generate a new **AES Secret Key** within the `AndroidKeyStore` (Hardware-backed) using the alias.
    3.  Encrypt the X25519 Private/Public pair using this AES key (AES/GCM/NoPadding).
    4.  Persist the encrypted ciphertext to the Database.

## 2. Ed25519 (Identity & Signing)
Used for generating the **MPAddress**, signing packets, and verifying authenticity in Two-Party chats.

* **Characteristics:**
    * **Security:** Generated, stored, and managed entirely by the **AndroidKeyStore (StrongBox/TEE)**.
    * **Exposure:** The Public Key is accessible for network sharing; the Private Key never leaves the hardware security module.
    * **Persistence:** Unique to the device installation.
* **Access Workflow:**
    1.  Construct the **Key Alias** using the standard formula.
    2.  Query the `AndroidKeyStore` using the alias.
    3.  If the key exists, retrieve the `KeyStore.Entry` for cryptographic operations.

## 3. AES/GCM/NoPadding (Master Encryption)
The "Key-Wrapping" layer used to protect the software-based X25519 keys.

* **Role:** Provides authenticated encryption for local sensitive data.
* **Management:** Hardware-backed via `AndroidKeyStore`. 
* **Logic:** Software logic maintains the mapping between specific aliases and their intended purpose.
* **Access Workflow:**
    1.  Construct the **Key Alias** using the standard formula.
    2.  Query the `AndroidKeyStore` using the alias.
    3.  If the key exists, retrieve the `KeyStore.Entry` for cryptographic operations.
## 4. Alias Generation Formula Recommendation
A deterministic formula used to generate unique `KeyStore` identifiers.

* **Input Components:** `OwnerID` + `Timestamp` + `AlgorithmName` + `KeyType`.
* **Process:** 1.  Concatenate input components into a raw string.
    2.  Pass the string through a cryptographic hash function (e.g., **SHA-256**).
* **Output:** The resulting **Hash Value** is used as the unique string alias for the `AndroidKeyStore`.