# 📁 Project Structure

## Clean, Professional Organization

```
blockchain-certificate-validator/
├── 📁 src/main/java/com/certificatevalidator/
│   ├── 📁 application/           # Main application entry points
│   │   ├── CertificateValidatorApp.java    # Console application
│   │   ├── SimpleWebApp.java              # Web server launcher
│   │   └── DemoApp.java                   # Automated demo
│   ├── 📁 web/                   # Web interface
│   │   └── SimpleWebServer.java  # HTTP web server (no DB)
│   ├── 📁 blockchain/            # Blockchain implementation
│   │   ├── Block.java            # Individual block class
│   │   ├── Blockchain.java       # Blockchain management
│   │   └── HashUtils.java        # SHA-256 utilities
│   └── 📁 entities/              # Data models
│       └── Certificate.java      # Certificate entity
├── 📁 docs/                      # Documentation
│   ├── ARCHITECTURE.md
│   ├── SECURITY.md
│   └── USER_GUIDE.md
├── 📄 README.md                  # Project overview
├── 📄 WEB_INTERFACE.md           # Web interface guide
├── 📄 PROJECT_STRUCTURE.md       # This file
├── 📄 run.bat                    # Console launcher
├── 📄 run-web.bat                # Web interface launcher
└── 📄 pom.xml                    # Maven configuration
```

## 🎯 Key Features

### ✅ **Multiple Interfaces**
- **SimpleWebApp.java** - Modern web interface (Recommended)
- **CertificateValidatorApp.java** - Professional console interface
- **DemoApp.java** - Automated demonstration
- Clean, organized structure

### ✅ **Core Components**
- **Blockchain Implementation** - Real blockchain with SHA-256
- **Certificate Management** - Issue, validate, and manage certificates
- **In-Memory Storage** - No database dependencies required
- **Security Features** - Tamper detection and chain validation

### ✅ **Easy to Run**
- **run.bat** - Double-click to start
- **Command line** - Simple java commands
- **Demo mode** - Automated showcase

## 🚀 How to Run

### 🌐 Web Interface (Recommended)
**Double-click run-web.bat** - Opens modern web interface at `http://localhost:8080`
**No database required** - Uses in-memory storage

### 💻 Console Interface
**Double-click run.bat** - Opens professional console interface

### 🎬 Demo Mode
```bash
java -cp "target/classes" com.certificatevalidator.application.DemoApp
```

## 🎓 Perfect for Portfolio

This project demonstrates:
- **Advanced Java Programming** - OOP, Collections, Exception Handling
- **Blockchain Technology** - Real cryptographic implementation
- **Database Integration** - SQLite with DAO pattern
- **Security Concepts** - SHA-256, Tamper detection
- **Professional Code** - Clean, well-documented, modular
- **Complete Solution** - Working application with documentation

## 📚 Documentation

- **README.md** - Complete project overview
- **ARCHITECTURE.md** - System architecture details
- **SECURITY.md** - Security implementation
- **USER_GUIDE.md** - Complete user manual

---

**Clean, professional, and ready to showcase! 🎉**
