import java.util.UUID

object BleUUIDConstants {
    val MESH_SERVICE_UUID: UUID = UUID.fromString("4f28d271-4ed6-4bd8-0000-496f3e1e747d")

    // Characteristic for the Noise Protocol Handshake
    val CHARACTERISTIC_NOISE_UUID: UUID = UUID.fromString("4f28d271-4ed6-4bd8-0001-496f3e1e747d")

    // Characteristic for actual Encrypted Messages Client -> Server
    val CHARACTERISTIC_DATA_WRITE_UUID: UUID = UUID.fromString("4f28d271-4ed6-4bd8-0002-496f3e1e747d")

    // Characteristic for actual Encrypted Messages Server -> Client
    val CHARACTERISTIC_DATA_NOTIFY_UUID: UUID = UUID.fromString("4f28d271-4ed6-4bd8-0003-496f3e1e747d")

    // Client Characteristic Configuration Descriptor (Required for Notifications)
    val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}