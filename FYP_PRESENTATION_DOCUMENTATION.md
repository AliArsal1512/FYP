# ClarifAI - Final Year Project (FYP) Documentation
## Comprehensive Technical Documentation for Project Evaluation

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Database Structure](#database-structure)
4. [Technology Stack & Justifications](#technology-stack--justifications)
5. [Frontend Architecture](#frontend-architecture)
6. [Backend Architecture](#backend-architecture)
7. [Code Processing Pipeline](#code-processing-pipeline)
8. [Comment Generation System](#comment-generation-system)
9. [AST Generation System](#ast-generation-system)
10. [CFG Generation System](#cfg-generation-system)
11. [Graphical AST Visualization](#graphical-ast-visualization)
12. [Component Interactions](#component-interactions)
13. [Data Flow Diagrams](#data-flow-diagrams)
14. [Performance Optimizations](#performance-optimizations)
15. [Security Implementation](#security-implementation)

---

## Project Overview

### What is ClarifAI?
ClarifAI is a web-based Java code analysis and documentation platform that combines:
- **AI-Powered Comment Generation**: Automatically generates documentation comments for Java classes and methods using a pre-trained transformer model
- **Abstract Syntax Tree (AST) Visualization**: Displays the hierarchical structure of Java code in both text and interactive graphical formats
- **Control Flow Graph (CFG) Visualization**: Generates visual representations of program control flow
- **User Management**: Secure authentication and submission history tracking

### Core Functionality
1. Users upload or paste Java code
2. System parses and analyzes the code structure
3. AI model generates documentation comments
4. AST and CFG visualizations are created
5. All results are saved for future reference

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER (Browser)                   │
│                  React SPA (Single Page App)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │   Home   │  │  Model   │  │ Dashboard│  │  Auth   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/HTTPS (REST API)
                        │ JSON Data Exchange
┌───────────────────────▼─────────────────────────────────────┐
│                  SERVER LAYER (Flask)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Route Handlers (Blueprints)              │   │
│  │  - Authentication Routes (/auth/*)                   │   │
│  │  - Main Routes (/, /generate-cfg, /ast-json)        │   │
│  │  - API Routes (/api/*)                               │   │
│  └───────────────────────┬──────────────────────────────┘   │
│                          │                                   │
│  ┌───────────────────────▼──────────────────────────────┐   │
│  │          Business Logic Layer                        │   │
│  │  - Code Processing (utils.py)                        │   │
│  │  - AST Generation (javalang parser)                   │   │
│  │  - CFG Generation (cfg_utils.py)                     │   │
│  │  - Comment Generation (Hugging Face Pipeline)       │   │
│  └───────────────────────┬──────────────────────────────┘   │
│                          │                                   │
│  ┌───────────────────────▼──────────────────────────────┐   │
│  │              Data Access Layer                        │   │
│  │  - SQLAlchemy ORM                                     │   │
│  │  - SQLite Database                                    │   │
│  └───────────────────────────────────────────────────────┘   │
└───────────────────────┬───────────────────────────────────────┘
                        │
                        │ Model Inference
                        │
┌───────────────────────▼───────────────────────────────────────┐
│         MACHINE LEARNING LAYER                                │
│         Hugging Face Transformers                             │
│         SEBIS Model (T5-based)                                │
│         - Text-to-Text Generation                             │
│         - Batch Processing Enabled                             │
└───────────────────────────────────────────────────────────────┘
```

### Request Flow Architecture

```
Browser Request
    │
    ├─► Is it /api/* or /auth/* POST?
    │   └─► YES → Flask Route Handler
    │       └─► Process Request
    │       └─► Return JSON Response
    │
    ├─► Is it /static/*?
    │   └─► YES → Serve Static File
    │
    └─► NO → Catch-all Route
        └─► serve_react_app(path)
            ├─► Check if file exists in react-build/
            │   └─► YES → Serve file (JS, CSS, images)
            │
            └─► NO → Serve index.html
                └─► React Router handles routing
                    ├─► / → Home component
                    ├─► /model → Model component
                    ├─► /dashboard → Dashboard component
                    └─► etc.
```

---

## Database Structure

### Database Technology
- **Database Engine**: SQLite (lightweight, file-based relational database)
- **ORM**: SQLAlchemy (Python SQL toolkit and Object-Relational Mapping)
- **Database File**: `users.db` (created automatically on first run)

### Entity Relationship Diagram

```
┌─────────────────────┐
│       User          │
├─────────────────────┤
│ id (PK)             │  INTEGER PRIMARY KEY
│ username            │  VARCHAR(100) UNIQUE NOT NULL
│ email               │  VARCHAR(100) UNIQUE NOT NULL
│ password_hash       │  VARCHAR(200) NOT NULL
└──────────┬──────────┘
           │
           │ 1:N relationship
           │ (One user can have many submissions)
           │
┌──────────▼──────────────────┐
│    CodeSubmission           │
├─────────────────────────────┤
│ id (PK)                     │  INTEGER PRIMARY KEY
│ user_id (FK)                │  INTEGER → User.id
│ code_content                │  TEXT NOT NULL
│ submission_name             │  VARCHAR(120) NOT NULL
│ timestamp                   │  DATETIME (UTC)
│ is_success                  │  BOOLEAN NOT NULL
│ ast_content                 │  TEXT (nullable)
│ comments_content            │  TEXT (nullable)
│ code_hash                   │  VARCHAR(64) (nullable)
│ cfg_image                   │  VARCHAR(255) (nullable)
└─────────────────────────────┘
```

### Database Schema Details

#### User Table
```python
class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), unique=True, nullable=False)
    email = db.Column(db.String(100), unique=True, nullable=False)
    password_hash = db.Column(db.String(200))
    
    # Relationship: One user has many submissions
    submissions = relationship("CodeSubmission", backref="user", 
                              cascade="all, delete-orphan")
```

**Purpose**: Stores user authentication information
- `id`: Primary key for user identification
- `username`: Unique identifier for login
- `email`: Unique email address
- `password_hash`: Securely hashed password (using Werkzeug's PBKDF2)
- `submissions`: One-to-many relationship with CodeSubmission

#### CodeSubmission Table
```python
class CodeSubmission(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    code_content = db.Column(db.Text, nullable=False)
    submission_name = db.Column(db.String(120), nullable=False)
    timestamp = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))
    is_success = db.Column(db.Boolean, default=True, nullable=False)
    ast_content = db.Column(db.Text)
    comments_content = db.Column(db.Text)
    code_hash = db.Column(db.String(64))
    cfg_image = db.Column(db.String(255))
```

**Purpose**: Stores code submissions and their analysis results
- `id`: Primary key
- `user_id`: Foreign key linking to User table
- `code_content`: The original Java code submitted
- `submission_name`: User-defined or auto-generated name
- `timestamp`: When the submission was created (UTC timezone)
- `is_success`: Whether processing succeeded (False for error cases)
- `ast_content`: HTML-formatted AST representation
- `comments_content`: HTML-formatted generated comments
- `code_hash`: SHA-256 hash for deduplication
- `cfg_image`: Path to generated CFG SVG (if generated)

### Database Operations

#### Creating Tables
```python
# In app/__init__.py, create_app() function
with app.app_context():
    db.create_all()  # Creates all tables defined in models.py
```

#### Querying Examples
```python
# Get user by username
user = User.query.filter_by(username=username).first()

# Get all successful submissions for a user
submissions = CodeSubmission.query.filter_by(
    user_id=current_user.id,
    is_success=True
).order_by(CodeSubmission.timestamp.desc()).all()

# Check for duplicate code (by hash)
existing = CodeSubmission.query.filter_by(
    user_id=current_user.id,
    code_hash=code_hash,
    is_success=True
).first()
```

---

## Technology Stack & Justifications

### Frontend Technologies

#### 1. React 18.2.0
**What it is**: JavaScript library for building user interfaces
**Why chosen**:
- **Component-based architecture**: Enables reusable, maintainable code
- **Virtual DOM**: Efficient updates and rendering
- **Large ecosystem**: Extensive library support
- **Industry standard**: Widely used, well-documented
- **State management**: Built-in hooks (useState, useEffect) for managing component state

**Usage in project**:
- All UI components (Home, Model, Dashboard, Auth pages)
- State management for user data, submissions, theme preferences
- Component lifecycle management

#### 2. React Router DOM 6.20.0
**What it is**: Declarative routing for React applications
**Why chosen**:
- **Client-side routing**: No page reloads, smooth navigation
- **SPA support**: Essential for single-page application architecture
- **URL management**: Clean URLs for different pages
- **Programmatic navigation**: Navigate programmatically in code

**Usage in project**:
- Route definitions: `/`, `/model`, `/dashboard`, `/auth/login`, etc.
- Navigation between pages
- Protected routes (authentication checks)

#### 3. Vite 5.0.8
**What it is**: Next-generation frontend build tool
**Why chosen**:
- **Fast development**: Instant server start, hot module replacement
- **Optimized builds**: Tree-shaking, code splitting
- **Modern tooling**: ES modules, TypeScript support
- **Better than Webpack**: Faster build times, simpler configuration

**Usage in project**:
- Development server (`npm run dev`)
- Production builds (`npm run build`)
- Asset bundling and optimization

#### 4. Monaco Editor 4.6.0
**What it is**: Web-based code editor (same as VS Code)
**Why chosen**:
- **Professional editor**: Industry-standard code editing experience
- **Java syntax highlighting**: Built-in support
- **Code completion**: IntelliSense-like features
- **Themes**: Light/dark mode support
- **Accessibility**: Keyboard shortcuts, line numbers

**Usage in project**:
- Main code input area in Model page
- Code preview in Dashboard
- Theme-aware (switches between 'vs' and 'vs-dark')

#### 5. D3.js 7.8.5
**What it is**: Data visualization library
**Why chosen**:
- **Powerful visualization**: Industry-standard for data visualization
- **Interactive trees**: Perfect for AST visualization
- **Customizable**: Full control over rendering
- **SVG-based**: Scalable, crisp graphics
- **Animation support**: Smooth transitions

**Usage in project**:
- Interactive AST tree visualization
- Node positioning and layout algorithms
- Theme-aware color schemes

#### 6. Bootstrap 5.3.2 & React Bootstrap 2.9.1
**What it is**: CSS framework and React components
**Why chosen**:
- **Responsive design**: Mobile-first approach
- **Pre-built components**: Buttons, cards, modals, etc.
- **Consistent styling**: Professional appearance
- **Grid system**: Easy layout management
- **Accessibility**: ARIA attributes, keyboard navigation

**Usage in project**:
- UI components (buttons, cards, forms)
- Responsive grid layouts
- Modal dialogs (submission naming)
- Navigation components

### Backend Technologies

#### 1. Flask
**What it is**: Lightweight Python web framework
**Why chosen**:
- **Minimal and flexible**: No unnecessary features, easy to customize
- **RESTful API support**: Perfect for JSON-based communication
- **Blueprint architecture**: Modular route organization
- **Extensible**: Large ecosystem of extensions
- **Python-native**: Easy integration with ML libraries

**Usage in project**:
- Main web server
- Route handling (authentication, API endpoints)
- Request/response management
- Session management

#### 2. Flask-SQLAlchemy
**What it is**: SQL toolkit and ORM for Flask
**Why chosen**:
- **Object-Relational Mapping**: Work with Python objects instead of SQL
- **Database abstraction**: Easy to switch databases
- **Relationship management**: Handle foreign keys easily
- **Migration support**: Database schema versioning
- **Query builder**: Type-safe queries

**Usage in project**:
- Database model definitions (User, CodeSubmission)
- Query operations (filtering, ordering, relationships)
- Transaction management

#### 3. Flask-Login
**What it is**: User session management for Flask
**Why chosen**:
- **Session management**: Handle user login/logout
- **User context**: Access current user in routes
- **Security**: CSRF protection, secure sessions
- **Decorators**: Easy route protection (@login_required)

**Usage in project**:
- User authentication
- Session management
- Protected routes (require login)

#### 4. SQLite
**What it is**: Lightweight, file-based relational database
**Why chosen**:
- **Zero configuration**: No server setup required
- **File-based**: Easy deployment, backup
- **Sufficient for project**: Handles user and submission data efficiently
- **ACID compliant**: Data integrity guaranteed
- **Python support**: Built-in sqlite3 module

**Usage in project**:
- User data storage
- Code submission history
- No external database server needed

#### 5. Werkzeug
**What it is**: WSGI utility library (comes with Flask)
**Why chosen**:
- **Password hashing**: Secure password storage (PBKDF2)
- **Security utilities**: URL encoding, request handling
- **WSGI support**: Web server interface

**Usage in project**:
- Password hashing (generate_password_hash, check_password_hash)
- Security utilities

### Code Analysis Technologies

#### 1. javalang 0.13.0
**What it is**: Java parser for Python
**Why chosen**:
- **Java parsing**: Parse Java source code into AST
- **Python integration**: Works seamlessly with Python codebase
- **AST access**: Navigate code structure programmatically
- **Error handling**: Detailed syntax error reporting

**Usage in project**:
- Parsing Java code: `javalang.parse.parse(code)`
- AST generation: Extract classes, methods, fields
- Syntax validation: Catch Java syntax errors
- CFG generation: Understand code structure for control flow

**Key Functions**:
```python
# Parse Java code
tree = javalang.parse.parse(java_code)

# Extract classes
for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
    class_name = class_node.name
    # Access class methods, fields, etc.

# Extract methods
for _, method_node in tree.filter(javalang.tree.MethodDeclaration):
    method_name = method_node.name
    # Access method body, parameters, return type
```

#### 2. NetworkX
**What it is**: Python library for graph analysis
**Why chosen**:
- **Graph data structure**: Represent CFG as directed graph
- **Graph algorithms**: Path finding, traversal
- **Visualization support**: Works with Graphviz
- **Python-native**: Easy integration

**Usage in project**:
- CFG representation: `nx.DiGraph()` (directed graph)
- Node and edge management
- Graph traversal for CFG generation

#### 3. Graphviz
**What it is**: Graph visualization software
**Why chosen**:
- **SVG output**: Scalable vector graphics
- **Automatic layout**: Handles node positioning
- **Professional appearance**: Clean, readable graphs
- **Python binding**: `graphviz` library for Python

**Usage in project**:
- CFG visualization: Convert NetworkX graph to SVG
- Theme support: Light/dark mode colors
- File output: Save CFG as SVG

### Machine Learning Technologies

#### 1. Hugging Face Transformers
**What it is**: Library for pre-trained transformer models
**Why chosen**:
- **Pre-trained models**: Access to state-of-the-art models
- **Easy integration**: Simple API for model loading and inference
- **Batch processing**: Efficient processing of multiple inputs
- **GPU support**: Automatic GPU detection and usage

**Usage in project**:
- Loading SEBIS model: `AutoModelForSeq2SeqLM.from_pretrained()`
- Tokenization: `AutoTokenizer.from_pretrained()`
- Pipeline creation: `pipeline("text2text-generation", ...)`
- Batch inference: Process multiple code snippets simultaneously

#### 2. PyTorch
**What it is**: Deep learning framework
**Why chosen**:
- **Model execution**: Runs transformer models
- **GPU acceleration**: CUDA support for faster inference
- **Automatic differentiation**: Required by transformer models
- **Industry standard**: Most ML models use PyTorch

**Usage in project**:
- Model inference backend
- GPU/CPU device selection
- Tensor operations for text generation

#### 3. SEBIS Model
**What it is**: Pre-trained T5-based model for code comment generation
**Why chosen**:
- **Specialized for code**: Trained specifically on code-comment pairs
- **Java support**: Works well with Java code
- **Quality output**: Generates meaningful comments
- **Research-backed**: Published model with good results

**Model Details**:
- **Architecture**: T5 (Text-to-Text Transfer Transformer)
- **Task**: Text-to-text generation (code → comment)
- **Input**: Preprocessed Java code
- **Output**: Natural language comment

**Model Configuration**:
```python
app.hf_pipeline = hf_pipeline(
    "text2text-generation",
    model=model,
    tokenizer=tokenizer,
    device=DEVICE,  # GPU if available, else CPU
    max_length=64,  # Maximum output length
    truncation=True,
    num_beams=1,  # Greedy decoding (faster)
    do_sample=False,  # Deterministic
    early_stopping=True
)
```

### Development Tools

#### 1. Python 3.11+
**Why chosen**:
- **ML ecosystem**: Best support for ML libraries
- **Performance**: Fast execution
- **Libraries**: Rich ecosystem (Flask, SQLAlchemy, etc.)

#### 2. Node.js & npm
**Why chosen**:
- **React development**: Required for React ecosystem
- **Package management**: npm for frontend dependencies
- **Build tools**: Vite requires Node.js

#### 3. Git
**Why chosen**:
- **Version control**: Track code changes
- **Collaboration**: Share code, manage versions
- **Industry standard**: Essential tool

---

## Frontend Architecture

### Project Structure

```
src/
├── main.jsx                 # React entry point
├── App.jsx                  # Main app component with routing
├── index.css                # Global styles, CSS variables
├── App.css                  # App-level styles
│
├── components/              # Reusable components
│   ├── Navbar.jsx          # Navigation bar with theme toggle
│   ├── Navbar.css
│   ├── FileSidebar.jsx     # File explorer for folder uploads
│   ├── FileSidebar.css
│   ├── ASTVisualization.jsx # Interactive AST tree (D3.js)
│   ├── ASTVisualization.css
│   ├── CFGVisualization.jsx  # CFG display component
│   └── CFGVisualization.css
│
├── pages/                   # Page components
│   ├── Home.jsx            # Landing page
│   ├── Home.css
│   ├── Model.jsx           # Main code editor page
│   ├── Model.css
│   ├── Dashboard.jsx       # User submissions dashboard
│   ├── Dashboard.css
│   ├── Login.jsx           # Login page
│   ├── Signup.jsx          # Signup page
│   ├── Auth.css            # Shared auth styles
│   ├── Settings.jsx         # User settings
│   └── Settings.css
│
└── contexts/                # React contexts
    └── ThemeContext.jsx     # Theme management (light/dark)
```

### Component Hierarchy

```
App (Router)
│
├── Navbar (always visible)
│   ├── Brand (ClarifAI)
│   ├── Navigation Links
│   └── Theme Toggle Button
│
├── Routes
│   ├── / → Home
│   │   ├── Hero Section
│   │   └── "How it Works" Section
│   │
│   ├── /model → Model
│   │   ├── FileSidebar (optional)
│   │   ├── Code Editor (Monaco)
│   │   ├── AST Panel
│   │   │   ├── Text View
│   │   │   └── Graphical View (ASTVisualization)
│   │   ├── Comments Panel
│   │   └── CFG Section (CFGVisualization)
│   │
│   ├── /dashboard → Dashboard
│   │   ├── Stats Cards
│   │   ├── Submission Sidebar
│   │   └── Code Preview
│   │
│   └── /auth/* → Auth Pages
│       ├── Login
│       └── Signup
```

### Key Frontend Components

#### 1. App.jsx - Main Application Component
**Purpose**: Root component with routing setup
**Key Features**:
- React Router configuration
- Theme provider setup
- Authentication state management
- Route protection

**Code Structure**:
```jsx
function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/model" element={<Model />} />
          <Route path="/dashboard" element={<Dashboard />} />
          {/* ... */}
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}
```

#### 2. Model.jsx - Code Editor Page
**Purpose**: Main page for code input and analysis
**Key Features**:
- Monaco Editor integration
- File upload (single file or folder)
- Code submission to backend
- Display AST, comments, and CFG
- Submission naming modal

**State Management**:
```jsx
const [code, setCode] = useState('');
const [astOutput, setAstOutput] = useState('');
const [commentsOutput, setCommentsOutput] = useState('');
const [astData, setAstData] = useState(null);
const [isGraphicalView, setIsGraphicalView] = useState(false);
const [fileStructure, setFileStructure] = useState({});
const [currentFilePath, setCurrentFilePath] = useState(null);
const [showNamingModal, setShowNamingModal] = useState(false);
```

**Key Functions**:
- `handleSubmit()`: Submits code to backend, shows naming modal
- `handleConfirmSubmit()`: Actually submits with name
- `handleFileUpload()`: Handles single Java file upload
- `handleFolderUpload()`: Handles folder upload, builds file structure
- `loadFileFromStructure()`: Loads file from sidebar into editor

#### 3. Dashboard.jsx - User Dashboard
**Purpose**: Display and manage user submissions
**Key Features**:
- Fetch and display user submissions
- Search functionality
- Stats cards (total submissions, account creation, account level)
- Code preview with Monaco Editor
- Submission management (delete, rename, load)

**State Management**:
```jsx
const [submissions, setSubmissions] = useState([]);
const [filteredSubmissions, setFilteredSubmissions] = useState([]);
const [selectedSubmission, setSelectedSubmission] = useState(null);
const [stats, setStats] = useState(null);
const [searchQuery, setSearchQuery] = useState('');
```

**Key Functions**:
- `fetchSubmissions()`: Fetches user submissions and stats from API
- `loadSubmission()`: Loads a submission for preview
- `deleteSubmission()`: Deletes a submission
- `renameSubmission()`: Renames a submission

#### 4. ASTVisualization.jsx - Interactive AST Tree
**Purpose**: Render AST as interactive tree using D3.js
**Key Features**:
- D3.js tree layout
- Interactive nodes (expand/collapse)
- Theme-aware colors
- Comment indicators
- Zoom and pan support

**D3.js Implementation**:
```jsx
useEffect(() => {
  if (!astData) return;
  
  // Clear previous visualization
  d3.select(svgRef.current).selectAll("*").remove();
  
  // Create D3 tree layout
  const tree = d3.tree().size([height, width]);
  const root = d3.hierarchy(astData);
  tree(root);
  
  // Draw nodes and links
  // Apply theme colors
  // Add interactivity
}, [astData, theme]);
```

#### 5. CFGVisualization.jsx - CFG Display
**Purpose**: Display Control Flow Graph
**Key Features**:
- Fetches CFG SVG from backend
- Theme-aware (requests CFG with theme parameter)
- Loading states
- Error handling

**Implementation**:
```jsx
const generateCFG = async () => {
  setIsLoading(true);
  try {
    const response = await fetch('/generate-cfg', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, theme })
    });
    const svgText = await response.text();
    setCfgSvg(svgText);
  } catch (error) {
    // Handle error
  } finally {
    setIsLoading(false);
  }
};
```

#### 6. ThemeContext.jsx - Theme Management
**Purpose**: Global theme state management
**Key Features**:
- Light/dark mode toggle
- localStorage persistence
- CSS variable updates
- Document attribute setting

**Implementation**:
```jsx
const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
```

### Frontend-Backend Communication

#### API Endpoints Used

1. **POST /** - Submit code for analysis
   ```javascript
   fetch('/', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ 
       code: codeToSubmit, 
       submission_name: nameToUse 
     })
   })
   ```

2. **POST /ast-json** - Get AST JSON for graphical view
   ```javascript
   fetch('/ast-json', {
     method: 'POST',
     body: JSON.stringify({ code: codeToSubmit })
   })
   ```

3. **POST /generate-cfg** - Generate CFG SVG
   ```javascript
   fetch('/generate-cfg', {
     method: 'POST',
     body: JSON.stringify({ code, theme })
   })
   ```

4. **GET /api/dashboard** - Get user submissions and stats
   ```javascript
   fetch('/api/dashboard', {
     credentials: 'include'
   })
   ```

5. **GET /get-submission/:id** - Get specific submission
   ```javascript
   fetch(`/get-submission/${submissionId}`, {
     credentials: 'include'
   })
   ```

---

## Backend Architecture

### Project Structure

```
app/
├── __init__.py              # Flask app factory, ML pipeline initialization
├── config.py                # Configuration settings
├── models.py                # Database models (User, CodeSubmission)
├── utils.py                 # Code processing utilities
├── cfg_utils.py            # CFG generation class
│
├── auth/                   # Authentication blueprint
│   ├── __init__.py
│   └── routes.py           # Login, signup, logout routes
│
├── main/                   # Main application blueprint
│   ├── __init__.py
│   └── routes.py           # Main routes (/, /generate-cfg, /ast-json, etc.)
│
└── static/                 # Static files
    ├── react-build/        # React production build (generated)
    └── cfg_images/         # Generated CFG SVG files
```

### Flask Application Factory Pattern

**File**: `app/__init__.py`

**Purpose**: Create and configure Flask application instance

**Key Steps**:

1. **Initialize Extensions**:
```python
db = SQLAlchemy()
login_manager = LoginManager()
```

2. **Create App Function**:
```python
def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)
    
    # Initialize extensions
    db.init_app(app)
    login_manager.init_app(app)
    
    # Register blueprints
    from .auth.routes import auth_bp
    from .main.routes import main_bp
    app.register_blueprint(auth_bp, url_prefix='/auth')
    app.register_blueprint(main_bp)
    
    # Create database tables
    with app.app_context():
        db.create_all()
    
    # Initialize ML Pipeline
    # ... (load model, tokenizer, create pipeline)
    
    return app
```

3. **ML Pipeline Initialization**:
```python
# Load tokenizer
tokenizer = AutoTokenizer.from_pretrained(
    MODEL_PATH,
    model_max_length=64,
    truncation=True,
    padding="max_length"
)

# Load model
model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_PATH)

# Create pipeline
app.hf_pipeline = hf_pipeline(
    "text2text-generation",
    model=model,
    tokenizer=tokenizer,
    device=DEVICE,  # GPU if available
    max_length=64,
    num_beams=1,  # Greedy decoding
    do_sample=False
)
```

**Why Factory Pattern?**
- **Testing**: Easy to create multiple app instances for testing
- **Configuration**: Different configs for development/production
- **Modularity**: Clean separation of concerns

### Blueprint Architecture

#### Auth Blueprint (`app/auth/routes.py`)
**Purpose**: Handle authentication routes

**Routes**:
- `POST /auth/login` - User login
- `POST /auth/signup` - User registration
- `GET /auth/logout` - User logout

**Login Flow**:
```python
@auth_bp.route('/login', methods=['POST'])
def login():
    username = request.json.get('username')
    password = request.json.get('password')
    
    user = User.query.filter_by(username=username).first()
    
    if user and user.check_password(password):
        login_user(user)  # Flask-Login
        return jsonify({'success': True, 'redirect': '/dashboard'})
    else:
        return jsonify({'success': False, 'error': 'Invalid credentials'}), 401
```

#### Main Blueprint (`app/main/routes.py`)
**Purpose**: Handle main application routes

**Routes**:
- `POST /` - Code submission and analysis
- `POST /generate-cfg` - CFG generation
- `POST /ast-json` - AST JSON for graphical view
- `GET /api/dashboard` - Get user submissions
- `GET /get-submission/:id` - Get specific submission
- `DELETE /delete-submission/:id` - Delete submission
- `POST /rename-submission/:id` - Rename submission

---

## Code Processing Pipeline

### Complete Flow Diagram

```
User Submits Code
    │
    ├─► 1. Code Validation
    │   └─► Check if code is empty
    │
    ├─► 2. Code Hashing
    │   └─► compute_hash(code_input)
    │       └─► SHA-256 hash for deduplication
    │
    ├─► 3. Check for Existing Submission
    │   └─► Query database by user_id + code_hash
    │       └─► If exists → Return cached results
    │
    ├─► 4. Code Preprocessing
    │   ├─► wrap_code_if_needed()
    │   │   └─► Wrap in class if needed
    │   │   └─► Validate syntax
    │   └─► preprocess_code()
    │       └─► Normalize whitespace, remove tabs
    │
    ├─► 5. Parallel Processing
    │   │
    │   ├─► TASK A: AST Generation
    │   │   └─► format_ast(code_input)
    │   │       ├─► Parse with javalang
    │   │       ├─► Extract structure
    │   │       └─► Format as HTML
    │   │
    │   ├─► TASK B: Extract Code Structure
    │   │   ├─► extract_classes(code_input)
    │   │   └─► extract_methods(code_input)
    │   │
    │   └─► TASK C: Comment Generation (uses TASK B results)
    │       ├─► Batch process all classes and methods
    │       ├─► Generate comments via ML model
    │       └─► Format as HTML
    │
    ├─► 6. Save to Database
    │   └─► Create CodeSubmission record
    │
    └─► 7. Return Results
        └─► JSON response with AST and comments
```

### Step-by-Step Code Processing

#### Step 1: Code Validation
**Location**: `app/main/routes.py`, `home()` function

```python
code_input = request.json.get('code', '')
if not code_input.strip() or code_input == '{{ code_input }}':
    return jsonify({
        'comments': '<div class="comment-error">Error: No Code Submitted</div>',
        'ast': '<div class="ast-error">Error: No Code Submitted</div>'
    })
```

**Purpose**: Ensure code is provided and not empty

#### Step 2: Code Hashing
**Location**: `app/utils.py`, `compute_hash()` function

```python
def compute_hash(code: str) -> str:
    return hashlib.sha256(code.encode('utf-8')).hexdigest()
```

**Purpose**: 
- Create unique identifier for code
- Enable deduplication (same code = same hash)
- Fast lookup in database

**Why SHA-256?**
- Cryptographic hash function
- Collision-resistant (extremely unlikely two different codes have same hash)
- Fixed length output (64 hex characters)

#### Step 3: Deduplication Check
**Location**: `app/main/routes.py`

```python
existing_submission = CodeSubmission.query.filter_by(
    user_id=current_user.id,
    code_hash=code_hash,
    is_success=True
).first()

if existing_submission:
    # Return cached results - no reprocessing needed
    ast_output = existing_submission.ast_content
    comments_output = existing_submission.comments_content
```

**Purpose**: 
- Avoid reprocessing identical code
- Instant response for duplicate submissions
- Save computational resources

#### Step 4: Code Wrapping
**Location**: `app/utils.py`, `wrap_code_if_needed()` function

**Problem**: Some Java code snippets don't have a class declaration (e.g., just a method)

**Solution**: Automatically wrap code in a class if needed

```python
def wrap_code_if_needed(java_code: str) -> tuple[str, bool]:
    # Check if code already has class declaration
    if code starts with 'class ' or 'public class ':
        return java_code, False  # No wrapping needed
    
    # Try parsing as-is
    try:
        javalang.parse.parse(java_code)
        return java_code, False  # Parses successfully
    except JavaSyntaxError:
        # Wrap in class and try again
        wrapped_code = f"public class nan {{\n{java_code}\n}}"
        javalang.parse.parse(wrapped_code)  # Validate
        return wrapped_code, True  # Was wrapped
```

**Why "nan" class name?**
- Placeholder name for auto-wrapped code
- Doesn't conflict with actual class names
- Clearly indicates code was wrapped

#### Step 5: Code Preprocessing
**Location**: `app/utils.py`, `preprocess_code()` function

```python
def preprocess_code(code: str) -> str:
    return (
        code.replace('\t', ' ')      # Replace tabs with spaces
        .replace('\n', ' ')          # Replace newlines with spaces
        .replace('\r', ' ')          # Replace carriage returns
        .replace('  ', ' ')          # Collapse multiple spaces
        .strip()                     # Remove leading/trailing whitespace
    )
```

**Purpose**: 
- Normalize code format for ML model
- Remove formatting differences
- Single-line format (model expects this)

**Why preprocessing?**
- ML models are sensitive to input format
- Consistent format = better results
- Removes noise (whitespace variations)

#### Step 6: Structure Extraction

##### Extract Classes
**Location**: `app/utils.py`, `extract_classes()` function

```python
def extract_classes(java_code: str) -> dict:
    tree = javalang.parse.parse(wrapped_code)
    class_map = {}
    
    for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
        class_name = class_node.name
        # Extract class code by finding braces
        # ... (complex line-by-line extraction)
        class_map[class_name] = class_code
    
    return class_map
```

**Process**:
1. Parse code with javalang
2. Find all class declarations
3. For each class, extract full code (including body)
4. Use brace counting to find class boundaries

**Why extract full code?**
- ML model needs complete context
- Class-level comments describe entire class
- Includes fields, methods, etc.

##### Extract Methods
**Location**: `app/utils.py`, `extract_methods()` function

```python
def extract_methods(java_code: str) -> dict:
    tree = javalang.parse.parse(wrapped_code)
    method_map = {}
    
    for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
        for method in class_node.methods:
            # Extract method code
            method_map[class_name].append({
                'name': method.name,
                'code': method_code
            })
    
    return method_map
```

**Process**:
1. Parse code with javalang
2. Find all method declarations
3. Extract method body (between braces)
4. Store with class and method name

---

## Comment Generation System

### Overview

The comment generation system uses a pre-trained transformer model (SEBIS) to automatically generate documentation comments for Java code.

### Model Details

**Model Name**: SEBIS (Code Comment Generation Model)
**Architecture**: T5 (Text-to-Text Transfer Transformer)
**Task**: Text-to-text generation (code → comment)
**Input Format**: Preprocessed Java code (single line)
**Output Format**: Natural language comment

### Comment Generation Flow

```
Extract Code Structure
    │
    ├─► Classes → [Class1, Class2, ...]
    └─► Methods → [Method1, Method2, Method3, ...]
    │
    ├─► Preprocess Each Code Snippet
    │   └─► preprocess_code() for each
    │
    ├─► Batch All Inputs Together
    │   └─► [Class1, Class2, Method1, Method2, ...]
    │
    ├─► Process with ML Model (Batch)
    │   └─► hf_pipeline(all_inputs, batch_size=8)
    │       ├─► Tokenize all inputs
    │       ├─► Model forward pass (parallel)
    │       └─► Decode all outputs
    │
    ├─► Clean Comments
    │   └─► clean_comment() for each result
    │
    └─► Group by Class
        └─► Format as HTML
```

### Detailed Implementation

#### Step 1: Prepare Inputs
**Location**: `app/main/routes.py`, `home()` function

```python
# Collect all classes
all_inputs = []
input_mapping = []  # Track which input is which

for class_name, class_code in class_structure.items():
    processed_class = preprocess_code(class_code)
    all_inputs.append(processed_class)
    input_mapping.append(('class', class_name, None))

# Collect all methods
for class_name, methods in method_structure.items():
    for method in methods:
        processed_method = preprocess_code(method['code'])
        all_inputs.append(processed_method)
        input_mapping.append(('method', class_name, method['name']))
```

**Why batch?**
- **Speed**: Process 8 inputs simultaneously vs 1 at a time
- **Efficiency**: GPU can process batches in parallel
- **Performance**: 10-50x faster than sequential processing

#### Step 2: Batch Processing
**Location**: `app/main/routes.py`

```python
# Process all inputs in one batch
batch_results = hf_pipeline(
    all_inputs, 
    batch_size=min(8, len(all_inputs))
)
```

**What happens inside**:
1. **Tokenization**: Convert text to token IDs
   ```python
   # Example: "public class MyClass {" → [1234, 5678, 9012, ...]
   tokens = tokenizer.encode(code, max_length=64, truncation=True, padding="max_length")
   ```

2. **Model Forward Pass**: Generate comment tokens
   ```python
   # Model processes tokens
   # T5 architecture: Encoder-Decoder
   # Encoder: Understands input code
   # Decoder: Generates output comment
   output_tokens = model.generate(input_tokens)
   ```

3. **Decoding**: Convert tokens back to text
   ```python
   comment = tokenizer.decode(output_tokens, skip_special_tokens=True)
   ```

#### Step 3: Map Results Back
**Location**: `app/main/routes.py`

```python
for idx, (input_type, class_name, method_name) in enumerate(input_mapping):
    if idx < len(batch_results):
        result = batch_results[idx]
        comment = clean_comment(result['generated_text'])
        
        if input_type == 'class':
            grouped_comments[class_name]['class_comment'] = \
                f'<div class="comment-class">📦 Class: {class_name}\n{comment}</div>'
        else:  # method
            grouped_comments[class_name]['method_comments'].append(
                f'<div class="comment-method">◆ {class_name}.{method_name}:\n{comment}</div>'
            )
```

#### Step 4: Clean Comments
**Location**: `app/utils.py`, `clean_comment()` function

```python
def clean_comment(raw_comment: str) -> str:
    # Split into sentences
    sentences = [s.strip() for s in raw_comment.split('.') if s.strip()]
    
    # Capitalize first letter of each sentence
    filtered = []
    for sentence in sentences:
        if sentence:
            filtered.append(sentence[0].upper() + sentence[1:])
    
    # Join with periods
    return '. '.join(filtered) + '.' if filtered else "No comment generated"
```

**Purpose**:
- Format comments consistently
- Capitalize sentences properly
- Handle edge cases (empty comments)

**Example**:
- Input: `"this method calculates the sum of two numbers"`
- Output: `"This method calculates the sum of two numbers."`

#### Step 5: Format as HTML
**Location**: `app/main/routes.py`

```python
comments_output_list = []
for class_data in grouped_comments.values():
    if class_data['class_comment']:
        comments_output_list.append(class_data['class_comment'])
    comments_output_list.extend(class_data['method_comments'])

comments_output = '\n'.join(comments_output_list)
```

**Output Format**:
```html
<div class="comment-class">📦 Class: Calculator
This class performs mathematical calculations.</div>
<div class="comment-method">◆ Calculator.add:
This method adds two numbers together.</div>
```

### Model Configuration Details

**Why these settings?**

1. **max_length: 64**
   - Comments are usually short (1-2 sentences)
   - Shorter = faster generation
   - Sufficient for most comments

2. **num_beams: 1 (Greedy Decoding)**
   - Faster than beam search (num_beams=4)
   - Deterministic results
   - Quality is still good

3. **do_sample: False**
   - Deterministic generation
   - Same input = same output
   - Better for reproducibility

4. **batch_size: 8**
   - Balance between speed and memory
   - GPU can handle 8 inputs efficiently
   - Can be adjusted based on GPU memory

### Error Handling

**Fallback to Sequential Processing**:
```python
try:
    batch_results = hf_pipeline(all_inputs, batch_size=8)
except Exception as e:
    # Fallback: process one at a time
    for class_name, class_code in class_structure.items():
        result = hf_pipeline(preprocess_code(class_code))
        # ... process result
```

**Why fallback?**
- Some edge cases might fail in batch
- Sequential is slower but more reliable
- Ensures system always works

---

## AST Generation System

### What is an AST?

**Abstract Syntax Tree (AST)**: A tree representation of the abstract syntactic structure of source code. Each node represents a construct in the source code.

### AST Generation Flow

```
Java Code Input
    │
    ├─► Parse with javalang
    │   └─► javalang.parse.parse(code)
    │       └─► Returns parse tree
    │
    ├─► Traverse Parse Tree
    │   ├─► Find Classes
    │   ├─► Find Methods
    │   ├─► Find Fields
    │   └─► Find Variables, Loops
    │
    └─► Format as HTML Tree
        └─► format_ast() function
```

### Detailed Implementation

#### Step 1: Parse Java Code
**Location**: `app/utils.py`, `format_ast()` function

```python
def format_ast(java_code: str) -> str:
    # Wrap code if needed
    wrapped_code, was_wrapped = wrap_code_if_needed(java_code)
    
    # Parse Java code
    tree = javalang.parse.parse(wrapped_code)
```

**What `javalang.parse.parse()` does**:
- Tokenizes Java code
- Parses tokens according to Java grammar
- Builds parse tree (AST structure)
- Returns tree object with nodes

**Tree Structure**:
```
CompilationUnit
└── ClassDeclaration (MyClass)
    ├── FieldDeclaration (fields)
    └── MethodDeclaration (methods)
        └── BlockStatement (method body)
            └── Statements
```

#### Step 2: Extract Classes
**Location**: `app/utils.py`, `format_ast()` function

```python
for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
    class_name = class_node.name
    output.append(f'<div class="ast-class">📦 Class: {class_name}</div>')
```

**What `tree.filter()` does**:
- Traverses entire tree
- Finds all nodes of specified type
- Returns iterator of (path, node) tuples

**Class Node Properties**:
- `class_node.name`: Class name
- `class_node.fields`: List of field declarations
- `class_node.methods`: List of method declarations
- `class_node.modifiers`: Access modifiers (public, private, etc.)

#### Step 3: Extract Fields
**Location**: `app/utils.py`, `format_ast()` function

```python
if class_node.fields:
    output.append('<div class="ast-section">├─ 🟣 Fields:')
    for field in class_node.fields:
        modifiers = " ".join(field.modifiers) if field.modifiers else ""
        field_type = field.type.name if field.type else "Unknown"
        for declarator in field.declarators:
            output.append(
                f'<div class="ast-field">│   ├─ {modifiers} {field_type} {declarator.name}</div>'
            )
```

**Field Structure**:
- `field.modifiers`: [public, private, static, etc.]
- `field.type`: Type (int, String, etc.)
- `field.declarators`: List of variable names

**Example**:
```java
private int count;
```
Extracted as: `private int count`

#### Step 4: Extract Methods
**Location**: `app/utils.py`, `format_ast()` function

```python
if class_node.methods:
    output.append('<div class="ast-section">└─ 🔧 Methods:')
    for method in class_node.methods:
        modifiers = " ".join(method.modifiers) if method.modifiers else ""
        return_type = method.return_type.name if method.return_type else "void"
        params = ", ".join([f"{p.type.name} {p.name}" for p in method.parameters])
        
        output.append(
            f'<div class="ast-method">🔹 {modifiers} {return_type} {method.name}({params})</div>'
        )
```

**Method Structure**:
- `method.name`: Method name
- `method.modifiers`: [public, private, static, etc.]
- `method.return_type`: Return type
- `method.parameters`: List of parameters
- `method.body`: Method body (statements)

#### Step 5: Extract Method Body Details
**Location**: `app/utils.py`, `_process_method_body()` function

```python
def _process_method_body(body):
    method_vars = []
    loops = []
    
    # Extract local variables
    for stmt in statements:
        if isinstance(stmt, javalang.tree.LocalVariableDeclaration):
            method_vars.extend([f"{stmt.type.name} {d.name}" 
                              for d in stmt.declarators])
        
        # Extract loops
        if isinstance(stmt, (javalang.tree.ForStatement,
                            javalang.tree.WhileStatement,
                            javalang.tree.DoStatement)):
            loop_type = stmt.__class__.__name__.replace("Statement", "")
            loops.append({"type": loop_type, "vars": _collect_loop_vars(stmt)})
    
    return method_vars, loops
```

**What it extracts**:
- **Local Variables**: Variables declared in method
- **Loops**: For, While, Do-While loops
- **Loop Variables**: Variables used in loops

#### Step 6: Format as HTML Tree
**Location**: `app/utils.py`, `format_ast()` function

**Output Format**:
```html
<div class="ast-tree">
  <div class="ast-class">📦 Class: Calculator</div>
  <div class="ast-section">├─ 🟣 Fields:
    <div class="ast-field">│   ├─ private int count</div>
  </div>
  <div class="ast-section">└─ 🔧 Methods:
    <div class="ast-method">🔹 public int add(int a, int b)</div>
    <div class="ast-subsection">│ └─ 🟡 Variables:
      <div class="ast-var">│     ├─ int result</div>
    </div>
    <div class="ast-subsection">└─ 🔁 Loops:
      <div class="ast-loop">├─ For Loop</div>
    </div>
  </div>
</div>
```

**Tree Structure Visualization**:
```
📦 Class: Calculator
├─ 🟣 Fields:
│   ├─ private int count
│
└─ 🔧 Methods:
    ├─ 🔹 public int add(int a, int b)
    │   ├─ 🟡 Variables:
    │   │   └─ int result
    │   └─ 🔁 Loops:
    │       └─ For Loop
    │
    └─ 🔹 public void reset()
```

### AST Node Types (javalang)

**Common Node Types**:
- `ClassDeclaration`: Class definition
- `MethodDeclaration`: Method definition
- `FieldDeclaration`: Field/variable declaration
- `LocalVariableDeclaration`: Local variable
- `ForStatement`: For loop
- `WhileStatement`: While loop
- `IfStatement`: If condition
- `ReturnStatement`: Return statement
- `BlockStatement`: Code block `{ ... }`

**How to Access**:
```python
# Filter for specific node type
for path, node in tree.filter(javalang.tree.MethodDeclaration):
    print(node.name)  # Access node properties
```

---

## CFG Generation System

### What is a CFG?

**Control Flow Graph (CFG)**: A directed graph representation of all paths that might be traversed during program execution. Nodes represent basic blocks (sequences of statements), and edges represent control flow (branches, loops, etc.).

### CFG Generation Flow

```
Java Code Input
    │
    ├─► Parse with javalang
    │   └─► Get parse tree
    │
    ├─► Identify Basic Blocks
    │   ├─► Entry block (method start)
    │   ├─► Statement blocks
    │   ├─► Condition blocks (if, while, etc.)
    │   └─► Exit block (method end)
    │
    ├─► Identify Control Flow Edges
    │   ├─► Sequential flow (statement → statement)
    │   ├─► Branch edges (if true/false)
    │   ├─► Loop edges (while true/false)
    │   └─► Return edges (return → exit)
    │
    ├─► Build NetworkX Graph
    │   └─► Add nodes and edges
    │
    └─► Visualize with Graphviz
        └─► Convert to SVG
```

### Detailed Implementation

#### CFGGenerator Class
**Location**: `app/cfg_utils.py`

**Class Structure**:
```python
class CFGGenerator:
    def __init__(self):
        self.cfg = nx.DiGraph()  # Directed graph
        self.current_block = None
        self.block_counter = 0
        self.break_targets = []  # For break statements
        self.continue_targets = []  # For continue statements
        self.line_map = {}  # Map statements to line numbers
        self.method_map = {}  # Map method names to nodes
```

**Key Attributes**:
- `cfg`: NetworkX directed graph (nodes = basic blocks, edges = control flow)
- `current_block`: Currently being built block
- `block_counter`: Unique ID for each block
- `break_targets`: Stack for handling break statements
- `continue_targets`: Stack for handling continue statements

#### Step 1: Parse and Build Line Map
**Location**: `app/cfg_utils.py`, `generate()` method

```python
def generate(self, java_code: str) -> nx.DiGraph:
    self.java_code = java_code
    tree = javalang.parse.parse(java_code)
    self._build_line_map(java_code)  # Map code to line numbers
    self._process_tree(tree)  # Process parse tree
    return self.cfg
```

**Line Map Purpose**:
- Map AST nodes to source code line numbers
- Used for displaying line numbers in CFG nodes
- Helps with debugging

#### Step 2: Process Tree
**Location**: `app/cfg_utils.py`, `_process_tree()` method

```python
def _process_tree(self, tree):
    # Find all methods
    for path, method_node in tree.filter(javalang.tree.MethodDeclaration):
        if method_node.body:
            self._process_method(method_node)
```

**Process**:
1. Find all method declarations
2. For each method, process its body
3. Build CFG for each method separately

#### Step 3: Process Method Body
**Location**: `app/cfg_utils.py`, `_process_method()` method

```python
def _process_method(self, method_node):
    method_name = method_node.name
    
    # Create entry block
    entry_block = self._create_block(f"{method_name}_entry")
    
    # Process method body
    exit_block = self._process_statement(method_node.body)
    
    # Create exit block
    exit_block_id = self._create_block(f"{method_name}_exit")
    self.cfg.add_edge(exit_block, exit_block_id)
```

**Basic Block Creation**:
- **Entry Block**: First block (method start)
- **Statement Blocks**: Sequences of statements
- **Exit Block**: Last block (method end)

#### Step 4: Process Statements
**Location**: `app/cfg_utils.py`, `_process_statement()` method

**Handles Different Statement Types**:

1. **BlockStatement** (code block `{ ... }`):
```python
if isinstance(stmt, javalang.tree.BlockStatement):
    for sub_stmt in stmt.statements:
        self._process_statement(sub_stmt)
```

2. **IfStatement**:
```python
if isinstance(stmt, javalang.tree.IfStatement):
    # Create condition block
    condition_block = self._create_block("if_condition")
    
    # Process true branch
    true_block = self._process_statement(stmt.then_statement)
    self.cfg.add_edge(condition_block, true_block, label="true")
    
    # Process false branch (if exists)
    if stmt.else_statement:
        false_block = self._process_statement(stmt.else_statement)
        self.cfg.add_edge(condition_block, false_block, label="false")
    else:
        # No else: connect to next block
        self.cfg.add_edge(condition_block, next_block, label="false")
```

3. **WhileStatement**:
```python
if isinstance(stmt, javalang.tree.WhileStatement):
    # Create loop header block
    loop_header = self._create_block("while_condition")
    
    # Process loop body
    loop_body = self._process_statement(stmt.body)
    
    # Connect: header → body → header (loop back)
    self.cfg.add_edge(loop_header, loop_body, label="true")
    self.cfg.add_edge(loop_body, loop_header)  # Loop back
    
    # Connect: header → next (exit loop)
    self.cfg.add_edge(loop_header, next_block, label="false")
```

4. **ForStatement**:
```python
if isinstance(stmt, javalang.tree.ForStatement):
    # Create init block (for loop initialization)
    init_block = self._create_block("for_init")
    
    # Create condition block
    condition_block = self._create_block("for_condition")
    self.cfg.add_edge(init_block, condition_block)
    
    # Process loop body
    loop_body = self._process_statement(stmt.body)
    
    # Create update block (for loop increment)
    update_block = self._create_block("for_update")
    self.cfg.add_edge(loop_body, update_block)
    self.cfg.add_edge(update_block, condition_block)  # Loop back
    
    # Connect: condition → next (exit loop)
    self.cfg.add_edge(condition_block, next_block, label="false")
```

5. **ReturnStatement**:
```python
if isinstance(stmt, javalang.tree.ReturnStatement):
    # Create return block
    return_block = self._create_block("return")
    
    # Connect directly to exit
    self.cfg.add_edge(return_block, exit_block)
```

#### Step 5: Create Basic Blocks
**Location**: `app/cfg_utils.py`, `_create_block()` method

```python
def _create_block(self, label=""):
    block_id = f"block_{self.block_counter}"
    self.block_counter += 1
    
    # Add node to graph
    self.cfg.add_node(block_id, label=label, statements=[])
    
    return block_id
```

**Block Properties**:
- `label`: Human-readable label
- `statements`: List of statements in this block
- `line_numbers`: Source code line numbers

#### Step 6: Visualize CFG
**Location**: `app/cfg_utils.py`, `visualize()` method

```python
def visualize(self, format="svg", theme="light"):
    # Create Graphviz digraph
    dot = Digraph(comment='CFG')
    dot.attr(rankdir='TB')  # Top to bottom layout
    
    # Apply theme colors
    if theme == "dark":
        dot.attr(bgcolor='#2d2d2d')
        dot.attr(fontcolor='#f5f5f5')
    else:
        dot.attr(bgcolor='white')
        dot.attr(fontcolor='black')
    
    # Add nodes
    for node_id in self.cfg.nodes():
        label = self.cfg.nodes[node_id].get('label', node_id)
        dot.node(node_id, label=label)
    
    # Add edges
    for source, target in self.cfg.edges():
        edge_label = self.cfg.edges[source, target].get('label', '')
        dot.edge(source, target, label=edge_label)
    
    # Generate SVG
    return dot.pipe(format='svg').decode('utf-8')
```

**Graphviz Features Used**:
- `Digraph`: Directed graph
- `rankdir='TB'`: Top-to-bottom layout
- `node()`: Add nodes with labels
- `edge()`: Add edges with labels
- `pipe(format='svg')`: Generate SVG output

**Theme Support**:
- **Light Mode**: White background, black text
- **Dark Mode**: Dark background (#2d2d2d), light text (#f5f5f5)

### CFG Example

**Java Code**:
```java
public int calculate(int x, int y) {
    if (x > 0) {
        return x + y;
    } else {
        return x - y;
    }
}
```

**CFG Structure**:
```
[Entry] → [if_condition: x > 0]
            ├─ true → [return: x + y] → [Exit]
            └─ false → [return: x - y] → [Exit]
```

**Visual Representation**:
```
        [Entry]
           │
           ▼
    [if_condition]
      ╱        ╲
   true        false
    ╱            ╲
[return x+y]  [return x-y]
    ╲            ╱
      ╲        ╱
        [Exit]
```

---

## Graphical AST Visualization

### Overview

The graphical AST visualization uses D3.js to create an interactive tree visualization of the Abstract Syntax Tree.

### Implementation

#### Component: ASTVisualization.jsx
**Location**: `src/components/ASTVisualization.jsx`

#### Step 1: Build AST JSON
**Location**: `app/utils.py`, `build_ast_json()` function

**Purpose**: Convert javalang parse tree to JSON structure for D3.js

```python
def build_ast_json(java_code: str) -> dict:
    tree = javalang.parse.parse(wrapped_code)
    root = {
        "name": "Root",
        "type": "root",
        "children": []
    }
    
    # Extract classes
    for _, class_node in tree.filter(javalang.tree.ClassDeclaration):
        class_json = {
            "name": class_node.name,
            "type": "class",
            "children": []
        }
        
        # Extract methods
        for method in class_node.methods:
            method_json = {
                "name": method.name,
                "type": "method",
                "children": []
            }
            class_json["children"].append(method_json)
        
        root["children"].append(class_json)
    
    return root
```

**JSON Structure**:
```json
{
  "name": "Root",
  "type": "root",
  "children": [
    {
      "name": "Calculator",
      "type": "class",
      "children": [
        {
          "name": "add",
          "type": "method",
          "children": []
        }
      ]
    }
  ]
}
```

#### Step 2: Generate Comments for Nodes
**Location**: `app/utils.py`, `build_ast_json()` function

```python
# Generate comments for classes and methods
all_inputs = []
input_mapping = []

# Add classes
for class_node in classes:
    all_inputs.append(preprocess_code(class_code))
    input_mapping.append(('class', class_name))

# Add methods
for method in methods:
    all_inputs.append(preprocess_code(method_code))
    input_mapping.append(('method', class_name, method_name))

# Batch process
batch_results = hf_pipeline(all_inputs, batch_size=8)

# Attach comments to nodes
for idx, result in enumerate(batch_results):
    comment = clean_comment(result['generated_text'])
    # Attach to corresponding node in JSON
```

**Why generate comments here?**
- Graphical view shows comments on hover
- Interactive experience
- Users can see comments directly on nodes

#### Step 3: D3.js Tree Layout
**Location**: `src/components/ASTVisualization.jsx`

```javascript
useEffect(() => {
  if (!astData) return;
  
  const svg = d3.select(svgRef.current);
  svg.selectAll("*").remove();  // Clear previous
  
  const width = 1200;
  const height = 800;
  
  // Create D3 tree layout
  const tree = d3.tree()
    .size([height - 40, width - 200]);
  
  // Convert JSON to D3 hierarchy
  const root = d3.hierarchy(astData);
  tree(root);
  
  // Draw links (edges)
  svg.selectAll(".link")
    .data(root.links())
    .enter()
    .append("path")
    .attr("class", "link")
    .attr("d", d3.linkHorizontal()
      .x(d => d.y)
      .y(d => d.x));
  
  // Draw nodes
  const node = svg.selectAll(".node")
    .data(root.descendants())
    .enter()
    .append("g")
    .attr("class", "node")
    .attr("transform", d => `translate(${d.y},${d.x})`);
  
  // Add circles for nodes
  node.append("circle")
    .attr("r", 10)
    .attr("fill", d => getNodeColor(d.data.type, theme));
  
  // Add text labels
  node.append("text")
    .attr("dy", ".35em")
    .attr("x", d => d.children ? -13 : 13)
    .text(d => d.data.name);
}, [astData, theme]);
```

**D3.js Concepts**:

1. **Hierarchy**: `d3.hierarchy(data)`
   - Converts JSON to D3 hierarchy object
   - Adds parent/children relationships
   - Calculates depth

2. **Tree Layout**: `d3.tree()`
   - Calculates x, y positions for nodes
   - Automatic spacing
   - Handles tree structure

3. **Links**: `root.links()`
   - Returns array of link objects
   - Each link has source and target nodes

4. **Selection**: `d3.select()`
   - Selects DOM elements
   - Chainable operations
   - Data binding

#### Step 4: Interactive Features

**Expand/Collapse**:
```javascript
node.on("click", (event, d) => {
  if (d.children) {
    d._children = d.children;
    d.children = null;
  } else {
    d.children = d._children;
    d._children = null;
  }
  update(root);  // Redraw tree
});
```

**Tooltips (Comments)**:
```javascript
node.append("title")
  .text(d => d.data.comment || "No comment available");
```

**Zoom and Pan**:
```javascript
const zoom = d3.zoom()
  .scaleExtent([0.1, 4])
  .on("zoom", (event) => {
    svg.attr("transform", event.transform);
  });

svg.call(zoom);
```

#### Step 5: Theme Support
**Location**: `src/components/ASTVisualization.jsx`

```javascript
const getNodeColor = (type, theme) => {
  if (theme === 'dark') {
    switch(type) {
      case 'class': return '#6bb3e8';
      case 'method': return '#88aaff';
      default: return '#a0a0a0';
    }
  } else {
    switch(type) {
      case 'class': return '#2b6cb0';
      case 'method': return '#4565f0';
      default: return '#6b7280';
    }
  }
};
```

**Theme Colors**:
- **Light Mode**: 
  - Classes: `#2b6cb0` (blue)
  - Methods: `#4565f0` (lighter blue)
  - Links: `#6b7280` (gray)
- **Dark Mode**:
  - Classes: `#6bb3e8` (bright blue)
  - Methods: `#88aaff` (lighter blue)
  - Links: `#a0a0a0` (light gray)

---

## Component Interactions

### Frontend Component Communication

#### 1. Model Page Component Flow

```
Model.jsx (Main Component)
    │
    ├─► FileSidebar.jsx
    │   ├─► Receives: fileStructure, currentFilePath
    │   ├─► Calls: onFileSelect(file, filePath)
    │   └─► Updates: currentFilePath in Model.jsx
    │
    ├─► Monaco Editor
    │   ├─► Receives: code, theme
    │   ├─► Calls: onChange(value)
    │   └─► Updates: code state in Model.jsx
    │
    ├─► AST Panel
    │   ├─► Text View: Displays astOutput (HTML)
    │   └─► Graphical View: ASTVisualization.jsx
    │       ├─► Receives: astData, theme
    │       └─► Fetches: /ast-json endpoint
    │
    ├─► Comments Panel
    │   └─► Displays: commentsOutput (HTML)
    │
    └─► CFG Section
        └─► CFGVisualization.jsx
            ├─► Receives: code, theme
            ├─► Calls: /generate-cfg endpoint
            └─► Displays: SVG result
```

#### 2. Dashboard Component Flow

```
Dashboard.jsx
    │
    ├─► Fetches: /api/dashboard
    │   └─► Receives: submissions[], stats{}
    │
    ├─► Submission Sidebar
    │   ├─► Displays: filteredSubmissions[]
    │   ├─► Search: filters by submission_name
    │   └─► On Click: loadSubmission(id)
    │
    ├─► Stats Cards
    │   ├─► Total Submissions
    │   ├─► Account Creation Date
    │   └─► Account Level
    │
    └─► Code Preview
        ├─► Monaco Editor
        │   └─► Displays: selectedSubmission.code_content
        ├─► AST View
        │   └─► Displays: selectedSubmission.ast_content
        └─► Comments View
            └─► Displays: selectedSubmission.comments_content
```

#### 3. Theme Management Flow

```
ThemeContext.jsx (Provider)
    │
    ├─► Stores: theme state ('light' | 'dark')
    ├─► Provides: {theme, toggleTheme}
    │
    └─► All Components Access Theme
        ├─► Navbar.jsx
        │   └─► Theme toggle button
        ├─► Model.jsx
        │   ├─► Monaco Editor theme
        │   ├─► ASTVisualization theme
        │   └─► CFGVisualization theme
        └─► All CSS
            └─► [data-theme="dark"] selectors
```

### Backend Component Communication

#### 1. Route Handler → Business Logic

```
Route Handler (routes.py)
    │
    ├─► Calls: utils.py functions
    │   ├─► format_ast()
    │   ├─► extract_classes()
    │   ├─► extract_methods()
    │   ├─► preprocess_code()
    │   └─► build_ast_json()
    │
    ├─► Calls: cfg_utils.py
    │   └─► CFGGenerator.generate()
    │
    ├─► Calls: ML Pipeline
    │   └─► app.hf_pipeline()
    │
    └─► Calls: Database (models.py)
        ├─► CodeSubmission.query
        ├─► db.session.add()
        └─► db.session.commit()
```

#### 2. ML Pipeline Access

```python
# Pipeline is stored on app object
app.hf_pipeline = hf_pipeline(...)

# Access in routes
from flask import current_app
hf_pipeline = current_app.hf_pipeline
batch_results = hf_pipeline(all_inputs, batch_size=8)
```

**Why store on app object?**
- **Single instance**: Load model once, use everywhere
- **Performance**: Avoid reloading model on each request
- **Memory efficient**: Shared across all requests

#### 3. Database Operations

```python
# Create
new_submission = CodeSubmission(...)
db.session.add(new_submission)
db.session.commit()

# Read
submission = CodeSubmission.query.get(id)

# Update
submission.submission_name = new_name
db.session.commit()

# Delete
db.session.delete(submission)
db.session.commit()
```

---

## Data Flow Diagrams

### Complete Request-Response Cycle

#### Code Submission Flow

```
┌─────────────┐
│   Browser    │
│  (React)    │
└──────┬──────┘
       │
       │ 1. User types code in Monaco Editor
       │
       │ 2. User clicks "Submit Code"
       │
       │ 3. Shows naming modal
       │    └─► User enters name (or uses default)
       │
       │ 4. POST / (JSON)
       │    {
       │      "code": "...",
       │      "submission_name": "..."
       │    }
       │
       ▼
┌─────────────────┐
│  Flask Server   │
│  (routes.py)    │
└──────┬──────────┘
       │
       │ 5. Validate code
       │
       │ 6. Compute hash
       │
       │ 7. Check for existing submission
       │    └─► If exists: return cached
       │
       │ 8. Parse code (javalang)
       │
       │ 9. Extract structure
       │    ├─► Classes
       │    └─► Methods
       │
       │ 10. Generate AST (format_ast)
       │
       │ 11. Generate Comments
       │     ├─► Preprocess code snippets
       │     ├─► Batch process with ML model
       │     └─► Clean and format comments
       │
       │ 12. Save to database
       │
       │ 13. Return JSON
       │     {
       │       "ast": "...",
       │       "comments": "...",
       │       "cfg_supported": true
       │     }
       │
       ▼
┌─────────────┐
│   Browser   │
│  (React)    │
└─────────────┘
       │
       │ 14. Update state
       │     ├─► setAstOutput(data.ast)
       │     └─► setCommentsOutput(data.comments)
       │
       │ 15. Render in UI
       │     ├─► AST Panel
       │     └─► Comments Panel
```

#### CFG Generation Flow

```
┌─────────────┐
│   Browser   │
│  (React)    │
└──────┬──────┘
       │
       │ 1. User clicks "Generate CFG"
       │
       │ 2. POST /generate-cfg
       │    {
       │      "code": "...",
       │      "theme": "dark"
       │    }
       │
       ▼
┌─────────────────┐
│  Flask Server   │
│  (routes.py)    │
└──────┬──────────┘
       │
       │ 3. Create CFGGenerator
       │
       │ 4. CFGGenerator.generate(code)
       │    ├─► Parse code
       │    ├─► Build basic blocks
       │    ├─► Identify control flow
       │    └─► Create NetworkX graph
       │
       │ 5. CFGGenerator.visualize(theme)
       │    ├─► Convert to Graphviz
       │    ├─► Apply theme colors
       │    └─► Generate SVG
       │
       │ 6. Return SVG
       │    Content-Type: image/svg+xml
       │
       ▼
┌─────────────┐
│   Browser   │
│  (React)    │
└─────────────┘
       │
       │ 7. Display SVG in CFGVisualization
```

#### Authentication Flow

```
┌─────────────┐
│   Browser   │
│  (React)    │
└──────┬──────┘
       │
       │ 1. User enters credentials
       │
       │ 2. POST /auth/login
       │    {
       │      "username": "...",
       │      "password": "..."
       │    }
       │
       ▼
┌─────────────────┐
│  Flask Server   │
│  (auth/routes)  │
└──────┬──────────┘
       │
       │ 3. Query User from database
       │    User.query.filter_by(username=...).first()
       │
       │ 4. Verify password
       │    user.check_password(password)
       │    └─► Uses werkzeug.security.check_password_hash()
       │
       │ 5. If valid:
       │    ├─► login_user(user)  [Flask-Login]
       │    ├─► Create session
       │    └─► Return {success: true, redirect: '/dashboard'}
       │
       │ 6. If invalid:
       │    └─► Return {success: false, error: '...'}, 401
       │
       ▼
┌─────────────┐
│   Browser   │
│  (React)    │
└─────────────┘
       │
       │ 7. If success:
       │    └─► Navigate to /dashboard
       │
       │ 8. If error:
       │    └─► Display error message
```

---

## Performance Optimizations

### 1. Batch Processing for Comments

**Problem**: Processing each class/method individually is slow

**Solution**: Batch all inputs together

**Implementation**:
```python
# Instead of:
for class_code in classes:
    result = hf_pipeline(class_code)  # Slow: one at a time

# Do this:
all_inputs = [class1, class2, method1, method2, ...]
batch_results = hf_pipeline(all_inputs, batch_size=8)  # Fast: all at once
```

**Performance Gain**: 10-50x faster

**Why it works**:
- GPU can process multiple inputs in parallel
- Model forward pass is optimized for batches
- Reduces overhead (one call vs many calls)

### 2. Model Configuration Optimization

**Settings**:
- `max_length: 64` (reduced from 512)
  - Comments are short, don't need 512 tokens
  - Faster generation
- `num_beams: 1` (greedy decoding)
  - Faster than beam search (num_beams=4)
  - Still produces good quality
- `do_sample: False` (deterministic)
  - No random sampling overhead
  - Consistent results

**Performance Gain**: 4-8x faster per inference

### 3. Code Deduplication

**Implementation**:
```python
code_hash = compute_hash(code_input)
existing = CodeSubmission.query.filter_by(
    user_id=current_user.id,
    code_hash=code_hash,
    is_success=True
).first()

if existing:
    return cached_results  # Instant response
```

**Performance Gain**: Instant for duplicate code (no processing)

**Why it works**:
- SHA-256 hash is fast to compute
- Database lookup is fast (indexed)
- Avoids expensive ML inference

### 4. Database Indexing

**Indexed Fields**:
- `user_id`: Fast filtering by user
- `code_hash`: Fast duplicate detection
- `timestamp`: Fast ordering

**Performance Gain**: Faster queries, especially with many submissions

### 5. Lazy Loading

**Frontend**:
- Components load only when needed
- Code splitting (Vite automatically does this)
- Images loaded on demand

**Backend**:
- ML model loaded once at startup
- Database connections pooled
- Static files served efficiently

---

## Security Implementation

### 1. Password Security

**Hashing Algorithm**: PBKDF2 (via Werkzeug)

```python
# Hashing (signup)
password_hash = generate_password_hash(password)
# Uses: PBKDF2 with SHA-256, 260,000 iterations

# Verification (login)
is_valid = check_password_hash(password_hash, password)
```

**Why PBKDF2?**
- **Cryptographically secure**: Resistant to brute force
- **Slow by design**: Prevents rainbow table attacks
- **Industry standard**: Used by many frameworks

**Security Features**:
- Passwords never stored in plain text
- Salt automatically added (unique per password)
- High iteration count (260,000) slows down attacks

### 2. Session Management

**Flask-Login**:
- Secure session cookies
- Session expiration
- CSRF protection (can be added)

**Implementation**:
```python
login_user(user)  # Creates secure session
# Session stored in encrypted cookie
# Automatically expires
```

### 3. Authentication Checks

**Route Protection**:
```python
@login_required
def protected_route():
    # Only authenticated users can access
    pass
```

**Manual Checks**:
```python
if not current_user.is_authenticated:
    return jsonify({'error': 'Authentication required'}), 401
```

### 4. Input Validation

**Code Validation**:
- Check for empty code
- Validate Java syntax (javalang parser)
- Sanitize user input

**SQL Injection Prevention**:
- SQLAlchemy ORM uses parameterized queries
- No raw SQL with user input

**XSS Prevention**:
- HTML output is generated server-side
- User code is not directly rendered (processed first)

### 5. Error Handling

**Graceful Degradation**:
```python
try:
    # Process code
except JavaSyntaxError:
    return error_message  # Don't crash
except Exception as e:
    logger.error(e)  # Log for debugging
    return generic_error  # Don't expose internals
```

**Why important?**
- Prevents information leakage
- Better user experience
- Easier debugging (logged errors)

---

## Conclusion

### Project Summary

ClarifAI is a comprehensive Java code analysis platform that combines:
- **Modern web technologies** (React, Flask)
- **Machine learning** (Transformer models for comment generation)
- **Code analysis** (AST and CFG generation)
- **User management** (Authentication, submission history)

### Key Achievements

1. **Automated Documentation**: AI-powered comment generation
2. **Visual Analysis**: Interactive AST and CFG visualizations
3. **Performance**: Optimized batch processing and caching
4. **User Experience**: Modern UI with dark mode support
5. **Scalability**: Efficient database design and query optimization

### Technical Highlights

- **Full-stack architecture**: React frontend + Flask backend
- **ML integration**: Hugging Face Transformers pipeline
- **Code parsing**: javalang for Java code analysis
- **Graph visualization**: D3.js for AST, Graphviz for CFG
- **Database**: SQLAlchemy ORM with SQLite
- **Security**: Password hashing, session management

### Future Enhancements

Potential improvements:
- Support for more programming languages
- Enhanced ML model fine-tuning
- Real-time collaboration
- Code quality metrics
- Integration with version control systems

---

## Appendix: File Reference

### Key Files and Their Purposes

#### Backend Files
- `app/__init__.py`: Flask app factory, ML pipeline initialization
- `app/models.py`: Database models (User, CodeSubmission)
- `app/utils.py`: Code processing utilities (AST, extraction, preprocessing)
- `app/cfg_utils.py`: CFG generation class
- `app/main/routes.py`: Main application routes
- `app/auth/routes.py`: Authentication routes
- `app/config.py`: Configuration settings
- `run.py`: Application entry point

#### Frontend Files
- `src/main.jsx`: React entry point
- `src/App.jsx`: Main app component with routing
- `src/pages/Model.jsx`: Code editor page
- `src/pages/Dashboard.jsx`: User dashboard
- `src/components/ASTVisualization.jsx`: Interactive AST tree
- `src/components/CFGVisualization.jsx`: CFG display
- `src/contexts/ThemeContext.jsx`: Theme management

#### Configuration Files
- `package.json`: Frontend dependencies
- `vite.config.js`: Vite build configuration
- `app/config.py`: Backend configuration

---

**End of Documentation**
