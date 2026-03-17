import java.util.UUID

object BleConstants {
    // This is your "Network ID"
    val MESH_SERVICE_UUID: UUID = UUID.fromString("4f28d271-4ed6-4bd8-9e38-496f3e1e747d")

    // Characteristic for the Noise Protocol Handshake
    val CHARACTERISTIC_HANDSHAKE_UUID: UUID = UUID.fromString("08851fc7-13f5-41c2-b9aa-733a3db1b19c")

    // Characteristic for actual Encrypted Messages
    val CHARACTERISTIC_DATA_UUID: UUID = UUID.fromString("d45d07f0-e89b-44d6-bc27-eb83b29f8c8e")
}