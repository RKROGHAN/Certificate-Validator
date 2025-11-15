# 🔗 Blockchain Certificate Validator

A comprehensive Java application that implements a blockchain-based certificate validation system. This project demonstrates advanced Java concepts including OOP, Collections, Database integration, and blockchain technology.

## 🚀 Features

### Core Functionality
- **Issue Certificates**: Generate digital certificates with SHA-256 hashing
- **Blockchain Integration**: Each certificate is stored as a block in a mini-blockchain
- **Certificate Validation**: Verify certificate authenticity through blockchain verification
- **Chain Integrity**: Validate the entire blockchain for tamper detection
- **Modern UI**: JavaFX-based dashboard with intuitive interface
- **Console Interface**: Command-line application for system administration

### Security Features
- **SHA-256 Hashing**: Cryptographic security for all certificate data
- **Immutable Blocks**: Once created, blocks cannot be altered
- **Chain Validation**: Automatic integrity checking of the entire blockchain
- **Database Persistence**: SQLite database for reliable data storage

## 🛠️ Tech Stack

- **Java 11+**: Core programming language
- **JavaFX**: Modern desktop UI framework
- **SQLite**: Lightweight database for data persistence
- **Maven**: Dependency management and build tool
- **SHA-256**: Cryptographic hashing algorithm

## 📁 Project Structure

```
src/main/java/com/certificatevalidator/
├── application/           # Main application classes
│   ├── Main.java         # JavaFX application entry point
│   └── ConsoleApplication.java  # Console interface
├── blockchain/           # Blockchain implementation
│   ├── Block.java        # Individual block class
│   ├── Blockchain.java   # Blockchain management
│   └── HashUtils.java    # SHA-256 utilities
├── controllers/          # JavaFX controllers
│   └── DashboardController.java
├── db/                   # Database layer
│   ├── DatabaseConnection.java
│   ├── CertificateDAO.java
│   └── BlockchainDAO.java
├── entities/             # Data models
│   └── Certificate.java
└── services/             # Business logic
    └── CertificateService.java
```

## 🗄️ Database Schema

### Certificates Table
```sql
CREATE TABLE certificates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_name TEXT NOT NULL,
    course TEXT NOT NULL,
    issue_date TEXT NOT NULL,
    hash TEXT NOT NULL UNIQUE
);
```

### Blockchain Table
```sql
CREATE TABLE blockchain (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    block_index INTEGER NOT NULL,
    timestamp TEXT NOT NULL,
    certificate_hash TEXT NOT NULL,
    previous_hash TEXT NOT NULL,
    current_hash TEXT NOT NULL UNIQUE
);
```

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Windows, macOS, or Linux

### Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd blockchain-certificate-validator
   ```

2. **Run the application**

   **🌐 Web Interface (Recommended)**
   - Double-click `run-web.bat` for web interface
   - Open browser to `http://localhost:8080`
   - Modern, responsive web interface
   - No database required - uses in-memory storage
   
   **💻 Console Interface**
   - Double-click `run.bat` for console interface
   - Command line: `java -cp "target/classes" com.certificatevalidator.application.CertificateValidatorApp`
   
   **🎬 Demo Mode**
   - Command line: `java -cp "target/classes" com.certificatevalidator.application.DemoApp`

## 📖 Usage Guide

### 🌐 Web Interface (Recommended)

The web interface provides a modern, responsive interface accessible through any web browser:

#### Features:
- **📜 Issue Certificates** - Web form for creating new certificates
- **🔍 Validate Certificates** - Validate by ID or hash with instant results  
- **📋 View All Certificates** - Browse certificates in a clean table
- **🔗 Blockchain Explorer** - Visual blockchain exploration
- **📱 Responsive Design** - Works on desktop, tablet, and mobile

#### How to Use:
1. Start the web server: Double-click `run-web.bat`
2. Open your browser: Go to `http://localhost:8080`
3. Use the tabbed interface to manage certificates

### 💻 Console Interface

The console interface provides a command-line interface with the following features:

#### 1. 📜 Issue Certificate
- Enter student name, course, and issue date
- System generates unique SHA-256 hash
- Certificate is automatically added to blockchain

#### 2. 🔍 Validate Certificate
- Validate by Certificate ID or Hash
- View detailed validation results
- Check blockchain integrity and authenticity

#### 3. 📋 View All Certificates
- Browse all issued certificates
- View certificate details and hashes
- See complete certificate information

#### 4. 🔗 View Blockchain
- Explore all blocks in the blockchain
- View block details and chain structure
- Understand blockchain implementation

#### 5. 🔒 Validate Blockchain Integrity
- Check entire blockchain for tampering
- Verify chain integrity
- View security status

#### 6. 🎬 Run Demo
- Automated demonstration of all features
- Perfect for showcasing the system
- Shows complete workflow

### Console Application

The console application provides a command-line interface:

```bash
📋 Main Menu:
1. Issue Certificate
2. Validate Certificate by ID
3. Validate Certificate by Hash
4. View All Certificates
5. View Blockchain
6. Validate Blockchain Integrity
7. Exit
```

## 🔐 How Blockchain Ensures Certificate Authenticity

### 1. Cryptographic Hashing
- Each certificate generates a unique SHA-256 hash
- Hash includes student name, course, and issue date
- Any modification to certificate data changes the hash

### 2. Block Chain Structure
- Each certificate becomes a block in the blockchain
- Each block contains:
  - Certificate hash
  - Previous block's hash
  - Current block's hash
  - Timestamp

### 3. Integrity Validation
- **Block Validation**: Each block's hash is recalculated and verified
- **Chain Validation**: Previous hash of each block must match the current hash of the previous block
- **Certificate Validation**: Certificate hash must exist in the blockchain

### 4. Tamper Detection
- If any certificate data is modified, its hash changes
- Modified hash won't match the hash stored in the blockchain
- Chain integrity check will fail if any block is tampered with

## 🧪 Example Workflow

1. **Issue Certificate**
   ```
   Student: John Doe
   Course: Java Programming
   Date: 2024-01-15
   → Generates hash: a1b2c3d4e5f6...
   → Creates Block 1 in blockchain
   ```

2. **Validate Certificate**
   ```
   Certificate ID: 1
   → Retrieves certificate from database
   → Recalculates hash from certificate data
   → Checks if hash exists in blockchain
   → Validates blockchain integrity
   → Returns: ✅ AUTHENTIC or ❌ INVALID
   ```

## 🔧 Development

### Building from Source
```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run with specific profile
mvn javafx:run
```

### Adding New Features
1. Create new classes in appropriate packages
2. Update database schema if needed
3. Add UI components for JavaFX interface
4. Update service layer for business logic
5. Add comprehensive tests

## 🧪 Testing

The project includes comprehensive testing for:
- Blockchain integrity validation
- Certificate hash generation
- Database operations
- UI component functionality

Run tests with:
```bash
mvn test
```

## 📊 Performance Considerations

- **Database Indexing**: Optimized queries with proper indexes
- **Memory Management**: Efficient blockchain storage
- **Hash Calculation**: Optimized SHA-256 implementation
- **UI Responsiveness**: Non-blocking operations for better UX

## 🔒 Security Features

- **Immutable Data**: Once added to blockchain, data cannot be modified
- **Cryptographic Security**: SHA-256 hashing ensures data integrity
- **Chain Validation**: Automatic detection of tampering
- **Database Security**: SQLite with proper constraints and indexes

## 🎯 Use Cases

### Educational Institutions
- Issue digital certificates for courses and programs
- Verify student achievements
- Prevent certificate forgery

### Professional Certifications
- Issue professional certifications
- Employer verification of credentials
- Maintain certification integrity

### Corporate Training
- Internal training certificates
- Skill verification
- Compliance documentation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- JavaFX community for excellent UI framework
- SQLite team for lightweight database solution
- Blockchain community for cryptographic concepts
- Open source contributors

## 📞 Support

For questions, issues, or contributions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**Built with ❤️ using Java, JavaFX, and Blockchain Technology**
