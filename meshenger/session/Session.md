# Session Architecture

## 1. General Principles
The Session Layer serves as the stateful manager for all communications. It abstracts the underlying networking and cryptographic complexities into a unified message bus for the UI and Database layers.

* **Reactive Message Bus:** All sessions maintain a `_messageBus` (`MutableSharedFlow<JsonObject>`) where incoming and outgoing data are published.
* **Data Integrity:** The data type enforced within the `_messageBus` is a `JsonObject`.
* **Buffer Strategy:** Uses `replay = 1` and `extraBufferCapacity = 10` to ensure that the UI Layer (React Native) never misses a message during bridge initialization or high-traffic bursts.
* **Payload Support:** Currently, the system supports **UTF-8 String** message types.

### Standard Bus Schema (`JsonObject`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `PeerID` | Long | The unique identifier of the message source or destination. |
| `Payload` | String | The **encrypted** version of the message (stored in the local database). |
| `Message` | String | The **plaintext** version of the message (rendered in the UI). |
| `Nonce` | Long | A counter/timestamp used with the key to prevent replay attacks. |
| `SessionType` | String | Indicates the context: `GlobalChat` or `TwoPartyChat`. |
| `Action` | String | Indicates the direction: `Send` or `Receive`. |

---

## 2. Session Classifications

### 2.1 Global Chat
* **Instance:** Singleton (only one object exists for the entire application).
* **Lifecycle:** Exists for the entire duration of the app lifetime.
* **Purpose:** Provides a low-barrier-to-entry "public" mesh channel for all discovered peers.

### 2.2 Two-Party Session (P2P)
Two-party sessions handle private, end-to-end encrypted (E2EE) conversations. The system utilizes the **Noise Protocol Framework** to establish secure keys. Multiple session objects can exist simultaneously.

#### Handshake Parameters
To determine the specific security protocol, the following parameters must be provided:

* **`isInitiator` (Boolean):** Defines the handshake role. The initiator sends the first message ($e$).
* **`prologue` (ByteArray):** Application-specific data hashed into the handshake. Both peers must have an identical prologue (e.g., "Meshenger_v1") to successfully connect.
* **`staticKey` (Pair<ByteArray, ByteArray>):** The local peer's long-term identity (Private Key and Public Key).
* **`peerId` (ULong) / `userName` (String):** Metadata used to identify the session participant.
* **`receiverPublicKey` (ByteArray?):** The peer's public key. Required for the **XK** pattern; discovered during the handshake in the **XX** pattern.
* **`chosenPattern` (NoisePattern):** Defaults to **XX** (3-step handshake with identity hiding).

#### Key Management
Layers can extract finalized session keys for persistence in a Secure Keystore or Database:
* **`getSendingKey`:** The symmetric key used to encrypt outbound messages.
* **`getRecievingKey`:** The symmetric key used to decrypt inbound messages.
* **Persistence:** For patterns like **XX** or **XK**, the static key of the remote peer is saved to the database upon successful handshake completion to facilitate future authentication.

### 2.3 Group Chat
* **Status:** Planned / To be implemented.

---

## Technical Appendix: Noise Protocol Explained

For the **Two-Party Session**, we utilize the **Noise XX** pattern by default. Here is the cryptographic significance of each parameter:

1.  **Identity Hiding:** By using the **XX** pattern, we ensure that neither party's static public key is transmitted in the clear. Everything is encrypted after the initial ephemeral key exchange.
2.  **Role Selection (`isInitiator`):** This is essential because the Noise state machine is asymmetric. It dictates which `CipherState` becomes the "sending" vs. "receiving" channel.
3.  **Forward Secrecy:** Since we use ephemeral keys ($e, re$) in every handshake, a compromise of the long-term `staticKey` in the future cannot be used to decrypt past conversations.
4.  **Key Separation:** We derive two separate keys (`getSendingKey` and `getRecievingKey`). This prevents "Reflection Attacks," where an adversary could intercept your encrypted message and send it back to you to trick the system into processing its own data as an incoming message.