💬 Chatterbox \n
A modern JavaFX-based desktop chat application built with Maven.
It supports database integration, secure authentication, icons, and webcam features.

📌 Features
💬 Real-time chat interface
🔐 Secure password hashing (jBCrypt)
🗄️ MongoDB database integration
🎨 Modern UI with JavaFX
📷 Webcam support
⭐ Icon support via Ikonli

⚙️ Tech Stack
Java 21
JavaFX
Maven
MongoDB
JUnit 5

📂 Project Structure
Chatterbox/
├── src/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

🚀 Getting Started
🔧 Prerequisites

Make sure you have:

✅ Java JDK 21
✅ Maven (or use wrapper)
✅ MongoDB running

▶️ Run the Application
🔹 Using Maven Wrapper (Recommended)

Windows
mvnw.cmd clean javafx:run

Linux/macOS
./mvnw clean javafx:run

🔹 Using Installed Maven
mvn clean javafx:run

🧠 Main Entry Point
com.chatterbox.lan.MainApp

🗄️ Database Setup

Make sure MongoDB is running:
mongodb://localhost:27017

Update connection settings in code if needed.

🛠️ Build & Test

Build:
mvn clean install

Run Tests:
mvn test

⚠️ Important Notes
⚡ Uses Java preview features (enabled via Maven)
🔄 Always run mvn clean install if dependencies fail
🧩 JavaFX issues? Re-import Maven dependencies



