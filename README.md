# Meshenger
**Meshenger** is an encrypted, peer-to-peer (P2P) messaging application built over a Bluetooth Mesh network. Designed for off-grid environments with no internet connectivity, Meshenger operates completely serverless: routing, storing, and delivering messages through nearby participating devices.

## Quick Start
1. Ensure Bluetooth and Location services are enabled on your device.
2. Download the latest release from the repository: [Download ver1.2.0.apk](./APK/ver1.2.0.apk)
3. Install the APK and launch the app to automatically discover nearby peers.

## Tech Stack
* **Frontend / Bridge**: React Native (Expo)
* **Native Modules**: Kotlin 2.0.0 & C/C++

## Minimum Requirement
* **Minimum Android Version**: Android 8.0 (API Level 26 / `minSdk 26`)
* **Target SDK**: Android 14 (API Level 34 / `compileSdk 34`)

## Project Architecture

The project follows a decoupled multi-module architecture:

| Module | Description |
| :--- | :--- |
| `:app` | React Native / Expo user interface |
| `:backend:application` | Bridge layer coordinating UI and core protocols |
| `:backend:session` | Session state management and peer handshake protocols |
| `:backend:network` | Epidemic routing, packet formation and parser, fragmentation and reassembly  |
| `:backend:transport2` | Mesh discovery, topology auto-healing, sending and receiving raw data|
| `:backend:security_native` | Native C/C++ cryptographic implementations management

## Core Features
- **Boostrapping**: An outsider device discovers, authenticates, and joins the Bluetooth mesh network.
- **Open Single Communication Channel**: Establishing a secure one-to-one communication channel between two users in the mesh network.
- **Process Message**: Receiving, validating, and routing incoming packets
through the mesh network
- **Fragmentation**: Breaking large data into smaller chunks to fit Bluetooth
mesh packet size limits.
- **Form Packet**: Creating network packet with headers, encryption, and metadata for transmission.
- **Reassembly**: Collecting fragmented packets and reconstructing the
original payload before delivery to the upper layer.
- **Mesh Topology Maintenance**
    - **Real-time Monitoring**: Continuously tracks active peer-to-peer connections.
    - **Auto-healing Network**: Automatically restores connectivity when active links drop below the required threshold.
- **Profile & Identity Customization**
    - **Flexible Usernames**: Update display names seamlessly at any time.
    - **Preset Avatars**: Select profile pictures from a predefined collection.
    - **Instant Synchronization**: Changes propagate across the network in real time.
- **Store-and-Forward Messaging**
    - **Progressive Relaying**: Pass messages hop-by-hop through intermediate nodes to reach target peers.
    - **Asynchronous Caching**: Temporarily hold messages on nearby devices when routes are unavailable.
    - **Offline Resilience**: Guarantee delivery even when the target recipient is temporarily disconnected or out of range.