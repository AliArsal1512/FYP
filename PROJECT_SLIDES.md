# ClarifAI - Presentation Slides Material
## Final Year Project Presentation

---

## Slide 1: Title Slide

**Project Title:** ClarifAI – Automated Java Code Analysis and Documentation Platform

**Team Members:** 
- Zawar Ahmed Farooqi
- Syed Ali Arsal

**Supervisor:** Dr. Junaid Akram

**Session:** 2022–2026

**Department of Computer Science**
**COMSATS University Islamabad, Lahore Campus**

---

## Slide 2: Introduction

Understanding unfamiliar **Java code** is time-consuming for developers and students.

**Lack of documentation** and unclear code logic hinder learning and maintenance.

**ClarifAI** is a web-based platform that:
- **Visualizes** code structure (AST & CFG)
- **Generates** AI-powered documentation comments
- **Provides** interactive analysis interface

---

## Slide 3: Problem Statement

**Developers** spend excessive time understanding **undocumented code**.

**Existing tools** focus on either visualization OR summarization, **not both**.

Need for **integrated solution** combining:
- Structural analysis (AST, CFG)
- Automated documentation generation
- Interactive visualization

---

## Slide 4: Proposed Solution – ClarifAI

**ClarifAI** is a comprehensive web platform that:

✓ **Parses** Java code and extracts structure

✓ **Generates** Abstract Syntax Trees (AST) with graphical view

✓ **Creates** Control Flow Graphs (CFG) for execution paths

✓ **Uses SEBIS model** (T5-based) to generate natural language comments

✓ **Provides** interactive, user-friendly React-based interface

---

## Slide 5: Objectives

**Primary Objectives:**

• Develop platform for **Java code upload** and analysis

• Generate **structural visualizations** (AST & CFG)

• Produce **contextual summaries** using SEBIS transformer model

• Support **file upload** and direct code input

• Assist students and professionals in **code understanding**

• Reduce time spent on **manual code review**

---

## Slide 6: Methodology Overview

**ClarifAI follows layered architecture:**

```
┌─────────────────────┐
│  User Interface     │  React Frontend
│  Layer              │
├─────────────────────┤
│  Application        │  Flask Backend
│  Layer              │
├─────────────────────┤
│  Processing         │  AST/CFG/ML
│  Layer              │
├─────────────────────┤
│  Data Layer         │  SQLite Database
└─────────────────────┘
```

**Key Components:**
- **Frontend:** React SPA with Monaco Editor, D3.js
- **Backend:** Flask REST API
- **Processing:** javalang, NetworkX, Graphviz
- **ML:** Hugging Face Transformers (SEBIS model)
- **Storage:** SQLAlchemy ORM with SQLite

---

## Slide 7: Step 1 – Code Upload & Parsing

**User Actions:**
- Upload single Java file
- Upload folder of Java files
- Paste code directly in Monaco Editor

**System Processing:**
1. **Validate** code syntax using javalang parser
2. **Parse** code into Abstract Syntax Tree
3. **Wrap** code in class if needed (auto-detection)
4. **Extract** structure (classes, methods, fields)

**Error Handling:**
- Display **meaningful error messages** with line numbers
- **Highlight** syntax issues

---

## Slide 8: Step 2 – AST Generation

**Abstract Syntax Tree Generation:**

1. **Parse** Java code using javalang library
2. **Extract** hierarchical structure:
   - Classes → Methods → Variables → Loops
3. **Format** as HTML tree structure
4. **Generate** JSON for graphical visualization

**Two Views:**
- **Text View:** HTML-formatted tree with expandable nodes
- **Graphical View:** Interactive D3.js tree with zoom/pan

**Features:**
- **Click nodes** to view details
- **Navigate** hierarchy easily
- **Theme-aware** (light/dark mode)

---

## Slide 9: Step 3 – CFG Generation

**Control Flow Graph Generation:**

1. **Parse** method bodies using javalang
2. **Identify** basic blocks (statement sequences)
3. **Track** control flow edges:
   - Sequential flow
   - Branch edges (if/else)
   - Loop edges (for/while)
   - Return edges
4. **Build** NetworkX directed graph
5. **Visualize** using Graphviz (SVG output)

**Features:**
- **Method-level** CFG visualization
- **Theme support** (light/dark colors)
- **Clear labels** for branches and loops

---

## Slide 10: Step 4 – Code Summarization with SEBIS Model

**Comment Generation Process:**

1. **Extract** classes and methods from code
2. **Preprocess** code snippets (normalize whitespace)
3. **Batch process** all inputs together (8 at a time)
4. **SEBIS model** (T5-based) generates comments:
   - Input: Preprocessed Java code
   - Output: Natural language comment
5. **Clean** and format comments
6. **Group** by class and method

**Model Details:**
- **Architecture:** T5 (Text-to-Text Transfer Transformer)
- **Task:** Code-to-comment generation
- **Configuration:** Greedy decoding, max_length=64
- **Performance:** Batch processing for speed

---

## Slide 11: Step 5 – Frontend Integration

**React-Based Frontend:**

**Technologies:**
- **React 18.2.0** – Component-based UI
- **React Router** – Client-side routing
- **Monaco Editor** – VS Code-like code editor
- **D3.js** – Interactive AST visualization
- **Bootstrap** – Responsive design

**Communication:**
- **REST API** calls to Flask backend
- **JSON** data exchange
- **Real-time** updates

**Features:**
- **Dark/Light mode** toggle
- **Interactive** visualizations
- **Responsive** design

---

## Slide 12: Step 6 – History & Data Management

**Database Management:**

**SQLite Database** stores:
- User accounts (authentication)
- Code submissions with analysis results
- Submission history per user

**Features:**
- **Deduplication:** Code hashing (SHA-256) prevents reprocessing
- **Retrieval:** Load previous submissions instantly
- **Management:** Delete, rename submissions
- **Search:** Filter submissions by name

**Data Structure:**
- **User Table:** Authentication info
- **CodeSubmission Table:** Code, AST, comments, CFG

---

## Slide 13: Architecture Diagram

**System Architecture:**

```
┌─────────────────────────────────────────┐
│      CLIENT LAYER (Browser)            │
│      React SPA                         │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
│  │ Home │ │Model │ │Dash  │ │ Auth │ │
│  └──────┘ └──────┘ └──────┘ └──────┘ │
└──────────────┬──────────────────────────┘
               │ HTTP/HTTPS (REST API)
┌──────────────▼──────────────────────────┐
│      SERVER LAYER (Flask)              │
│  ┌──────────────────────────────────┐ │
│  │ Route Handlers (Blueprints)      │ │
│  └──────────┬───────────────────────┘ │
│  ┌──────────▼───────────────────────┐ │
│  │ Business Logic Layer             │ │
│  │ - Code Processing                │ │
│  │ - AST/CFG Generation             │ │
│  └──────────┬───────────────────────┘ │
│  ┌──────────▼───────────────────────┐ │
│  │ Data Layer (SQLAlchemy)          │ │
│  └──────────┬───────────────────────┘ │
└──────────────┼──────────────────────────┘
               │ Model Inference
┌──────────────▼──────────────────────────┐
│ ML LAYER (Hugging Face Transformers)   │
│ SEBIS Model (T5-based)                 │
└─────────────────────────────────────────┘
```

---

## Slide 14: Tools & Technologies

**Backend Technologies:**
- **Flask** – Python web framework
- **SQLAlchemy** – ORM for database
- **Flask-Login** – Session management

**Code Analysis:**
- **javalang** – Java parser for Python
- **NetworkX** – Graph analysis (CFG)
- **Graphviz** – Graph visualization

**Machine Learning:**
- **Hugging Face Transformers** – Model framework
- **PyTorch** – Deep learning backend
- **SEBIS Model** – Pre-trained T5-based model

**Frontend Technologies:**
- **React 18.2.0** – UI library
- **Monaco Editor** – Code editor
- **D3.js** – Data visualization
- **Vite** – Build tool

**Database:**
- **SQLite** – Lightweight database

---

## Slide 15: Implementation Screenshot 1 – Login Page

**[SCREENSHOT: Login Page]**

**Features Shown:**
- Clean authentication interface
- User login form
- Sign up option
- Modern UI design

**Note:** Include actual screenshot from the application showing the login page interface.

---

## Slide 16: Implementation Screenshot 2 – Code Upload

**[SCREENSHOT: Model Page with Code Editor]**

**Features Shown:**
- Monaco Editor with Java code
- File upload buttons (single file / folder)
- Code submission interface
- Syntax highlighting

**Note:** Include screenshot showing code editor with file upload options.

---

## Slide 17: Implementation Screenshot 3 – AST with Comments

**[SCREENSHOT: AST Visualization with Generated Comments]**

**Features Shown:**
- Interactive AST tree (text or graphical view)
- Generated comments displayed
- Node expansion/collapse
- Comment indicators

**Note:** Include screenshot showing AST tree with comments visible.

---

## Slide 18: Implementation Screenshot 4 – Comments Panel

**[SCREENSHOT: Generated Comments Display]**

**Features Shown:**
- Class-level comments
- Method-level comments
- HTML-formatted output
- Organized by class structure

**Note:** Include screenshot showing the comments panel with generated documentation.

---

## Slide 19: Implementation Screenshot 5 – CFG Diagram

**[SCREENSHOT: Control Flow Graph Visualization]**

**Features Shown:**
- CFG SVG visualization
- Method execution flow
- Branch and loop indicators
- Clean graph layout

**Note:** Include screenshot showing a control flow graph for a method.

---

## Slide 20: Implementation Screenshot 6 – Dashboard

**[SCREENSHOT: User Dashboard with Submissions]**

**Features Shown:**
- Submission history sidebar
- Stats cards (total submissions, account level)
- Search functionality
- Code preview panel

**Note:** Include screenshot showing the dashboard with user submissions.

---

## Slide 21: Implementation Screenshot 7 – Dark Mode

**[SCREENSHOT: Application in Dark Mode]**

**Features Shown:**
- Dark theme applied
- Theme toggle button
- Consistent dark colors across components
- AST/CFG in dark mode

**Note:** Include screenshot showing the application with dark theme enabled.

---

## Slide 22: Results – Performance Optimizations

**Performance Improvements:**

**Batch Processing:**
- Process 8 code snippets simultaneously
- **10-50x faster** than sequential processing

**Model Configuration:**
- Greedy decoding (num_beams=1)
- Reduced max_length (64 tokens)
- **4-8x faster** per inference

**Code Deduplication:**
- SHA-256 hashing for duplicate detection
- **Instant** retrieval for repeated code

---

## Slide 23: Results – System Capabilities

**Functionality Results:**

✓ **Java Code Parsing:** Successfully parses complex Java code

✓ **AST Generation:** Accurate hierarchical structure extraction

✓ **CFG Generation:** Clear control flow visualization

✓ **Comment Generation:** Contextually relevant comments

✓ **User Management:** Secure authentication and history

✓ **Performance:** Fast processing with batch optimization

---

## Slide 24: Results – Technology Integration

**Successfully Integrated:**

**Frontend-Backend Communication:**
- RESTful API design
- JSON data exchange
- Real-time updates

**ML Model Integration:**
- Hugging Face pipeline integration
- Batch processing enabled
- GPU/CPU automatic detection

**Code Analysis Integration:**
- javalang parser integration
- NetworkX graph construction
- Graphviz visualization

---

## Slide 25: Discussion of Findings

**Key Findings:**

**ClarifAI** successfully combines:
- **Structural visualization** (AST & CFG)
- **AI-powered documentation** (SEBIS model)
- **Interactive interface** (React frontend)

**Benefits:**
- **Reduces** time for code understanding
- **Automates** documentation generation
- **Provides** multiple visualization formats
- **Accessible** web-based platform

**Limitations:**
- **Java-only** support (currently)
- **Limited** training data for model
- **Desktop-focused** visualization

---

## Slide 26: Challenges & Solutions

**Challenge 1: Code Parsing Errors**
- **Solution:** Added validation layer and code wrapping
- **Result:** Handles incomplete code snippets

**Challenge 2: Model Performance**
- **Solution:** Batch processing and model optimization
- **Result:** Faster inference (10-50x speedup)

**Challenge 3: Frontend Performance**
- **Solution:** React optimization, lazy loading, caching
- **Result:** Smooth user experience

**Challenge 4: Large Code Files**
- **Solution:** Efficient parsing and memory management
- **Result:** Handles complex code structures

---

## Slide 27: Conclusion

**ClarifAI** successfully delivers:

✓ **Integrated platform** for code analysis and documentation

✓ **Multiple visualizations** (AST text/graphical, CFG)

✓ **AI-generated comments** using SEBIS transformer model

✓ **User-friendly interface** with modern React frontend

✓ **Performance optimized** with batch processing

**Impact:**
- Assists developers and students in **code comprehension**
- Reduces **manual documentation** effort
- Provides **interactive learning** tool

---

## Slide 28: Future Work

**Enhancements Planned:**

• **Multi-language support** (Python, C++, JavaScript)

• **Enhanced CFG** with data flow analysis

• **Larger training datasets** for improved model accuracy

• **Cloud deployment** for better scalability

• **Collaborative features** (sharing, commenting)

• **Code quality metrics** integration

• **Version control** integration (Git)

---

## Slide 29: References

**Key References:**

1. **SEBIS Model** – Pre-trained transformer for code comment generation

2. **Hugging Face Transformers** – Library for transformer models

3. **javalang** – Java parser for Python

4. **T5 Architecture** – Text-to-Text Transfer Transformer

5. **React Documentation** – React.js official documentation

6. **Flask Documentation** – Flask web framework documentation

7. **D3.js Documentation** – Data visualization library

8. **NetworkX Documentation** – Graph analysis library

---

## Slide 30: Acknowledgements & Q&A

**Acknowledgements:**

We thank **Dr. Junaid Akram** for continuous guidance and support throughout the project.

Thanks to **COMSATS University** for providing resources and platform for research.

**Thank You!**

**Questions?**

---

## Slide 31: Contact Information (Optional)

**Team Members:**

**Zawar Ahmed Farooqi**
- Email: sp22-bcs-109@cuilahore.edu.pk

**Syed Ali Arsal**
- Email: sp22-bcs-037@cuilahore.edu.pk

**Project Repository:** [GitHub Link]

**Supervisor:**
- Dr. Junaid Akram
- COMSATS University Islamabad, Lahore Campus

---

## Notes for Presentation:

1. **Slide Count:** 30 slides (including 7 implementation screenshots)

2. **Keywords in Red:** For slides with more than 2 lines of text, highlight key terms in red color

3. **Font Size:** Use minimum 24pt font for body text, 32pt+ for headings

4. **Diagrams:** Include architecture diagrams, flow diagrams, and system diagrams to reduce text

5. **Screenshots:** Ensure all screenshots have readable text - resize if needed

6. **Keep It Concise:** Each slide should have maximum 5-7 bullet points

7. **Visual Appeal:** Use icons, colors, and consistent formatting

8. **Practice:** Rehearse presentation to ensure smooth flow and timing
