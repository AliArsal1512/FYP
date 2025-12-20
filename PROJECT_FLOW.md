# Clarifai Project - Detailed Flow Diagram

## Project Overview
Clarifai is a web-based Java code analysis platform that provides automated code documentation through AI-powered comment generation, along with Abstract Syntax Tree (AST) and Control Flow Graph (CFG) visualizations.

---

## System Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER BROWSER                            │
│                    (React Frontend - SPA)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTP Requests (GET/POST)
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                    FLASK BACKEND SERVER                         │
│                  (Python Web Framework)                          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Route Handler Layer                          │  │
│  │  - Authentication Routes (/auth/*)                       │  │
│  │  - Main Routes (/, /generate-cfg, /ast-json, etc.)      │  │
│  │  - API Routes (/api/*)                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            │                                     │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │              Business Logic Layer                         │  │
│  │  - Code Processing (utils.py)                            │  │
│  │  - AST Generation (javalang parser)                      │  │
│  │  - CFG Generation (cfg_utils.py)                         │  │
│  │  - Comment Generation (Hugging Face Pipeline)           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            │                                     │
│  ┌─────────────────────────▼─────────────────────────────────┐  │
│  │              Data Layer                                   │  │
│  │  - SQLAlchemy ORM                                         │  │
│  │  - SQLite Database (users.db)                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                            │
                            │ Model Inference
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│              HUGGING FACE ML MODEL                               │
│         (SEBIS - Code Comment Generation)                       │
│         - T5-based Transformer Model                            │
│         - Batch Processing Enabled                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Detailed Flow Diagrams

### 1. Application Initialization Flow

```
START
  │
  ├─► Flask App Creation (create_app())
  │   │
  │   ├─► Load Configuration (Config class)
  │   │   ├─► Database URI (SQLite)
  │   │   ├─► Secret Key
  │   │   └─► Model Path
  │   │
  │   ├─► Initialize Extensions
  │   │   ├─► SQLAlchemy (Database ORM)
  │   │   └─► Flask-Login (Authentication)
  │   │
  │   ├─► Register Blueprints
  │   │   ├─► auth_bp (/auth/* routes)
  │   │   └─► main_bp (main routes)
  │   │
  │   ├─► Create Database Tables (db.create_all())
  │   │   ├─► User Table
  │   │   └─► CodeSubmission Table
  │   │
  │   └─► Initialize ML Pipeline
  │       ├─► Load Tokenizer (AutoTokenizer)
  │       ├─► Load Model (AutoModelForSeq2SeqLM)
  │       ├─► Configure Pipeline
  │       │   ├─► Device: GPU (if available) or CPU
  │       │   ├─► max_length: 64
  │       │   ├─► num_beams: 1 (greedy decoding)
  │       │   └─► do_sample: False
  │       └─► Store in app.hf_pipeline
  │
  └─► Server Ready (Port 5000)
```

---

### 2. User Authentication Flow

#### 2.1 Login Flow

```
User → Login Page (React)
  │
  ├─► User enters credentials
  │   ├─► Username
  │   └─► Password
  │
  ├─► POST /auth/login
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Check if already authenticated
  │   │   └─► If yes → Return success, redirect to /dashboard
  │   │
  │   ├─► Query User from Database
  │   │   └─► User.query.filter_by(username=username).first()
  │   │
  │   ├─► Verify Password
  │   │   └─► user.check_password(password)
  │   │       └─► Uses werkzeug.security.check_password_hash()
  │   │
  │   ├─► If valid:
  │   │   ├─► login_user(user) [Flask-Login]
  │   │   ├─► Create session
  │   │   └─► Return JSON: {success: true, redirect: '/dashboard'}
  │   │
  │   └─► If invalid:
  │       └─► Return JSON: {success: false, error: '...'}, 401
  │
  └─► React handles response
      ├─► If success → Navigate to /dashboard
      └─► If error → Display error message
```

#### 2.2 Signup Flow

```
User → Signup Page (React)
  │
  ├─► User enters details
  │   ├─► Username
  │   ├─► Email
  │   └─► Password
  │
  ├─► POST /auth/signup
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Check if already authenticated
  │   │   └─► If yes → Return success, redirect to /dashboard
  │   │
  │   ├─► Validate uniqueness
  │   │   └─► Check if username or email exists
  │   │       └─► If exists → Return error
  │   │
  │   ├─► Create new User
  │   │   ├─► new_user = User(username, email)
  │   │   ├─► new_user.set_password(password)
  │   │   │   └─► Uses werkzeug.security.generate_password_hash()
  │   │   └─► db.session.add(new_user)
  │   │
  │   ├─► Commit to Database
  │   │   ├─► db.session.commit()
  │   │   └─► Handle IntegrityError (rollback if fails)
  │   │
  │   ├─► Auto-login user
  │   │   └─► login_user(new_user)
  │   │
  │   └─► Return JSON: {success: true, redirect: '/dashboard'}
  │
  └─► React handles response
      └─► Navigate to /dashboard
```

#### 2.3 Logout Flow

```
User clicks Logout
  │
  ├─► GET /auth/logout
  │   │
  │   ├─► logout_user() [Flask-Login]
  │   │   └─► Clear session
  │   │
  │   └─► Return JSON: {success: true, redirect: '/'}
  │
  └─► React navigates to Home page
```

---

### 3. Code Submission and Processing Flow

```
User → Model Page (React)
  │
  ├─► User enters/pastes Java code
  │   └─► Monaco Editor (code editor)
  │
  ├─► User clicks "Submit Code"
  │
  ├─► POST / (with JSON: {code: "..."})
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │   └─► @login_required decorator
  │   │       └─► If not authenticated → Return 401
  │   │
  │   ├─► Extract code from request
  │   │   └─► code_input = request.json.get('code', '')
  │   │
  │   ├─► Validate code
  │   │   └─► If empty → Return error
  │   │
  │   ├─► Compute Code Hash
  │   │   └─► compute_hash(code_input)
  │   │       └─► SHA-256 hash for deduplication
  │   │
  │   ├─► Check for Existing Submission
  │   │   └─► Query CodeSubmission by user_id + code_hash
  │   │       └─► If exists → Return cached results
  │   │
  │   ├─► Code Preprocessing
  │   │   ├─► Wrap code in class if needed
  │   │   │   └─► wrap_code_if_needed()
  │   │   │       └─► Uses javalang parser to check syntax
  │   │   └─► Preprocess for ML model
  │   │       └─► preprocess_code()
  │   │           └─► Remove tabs, normalize whitespace
  │   │
  │   ├─► PARALLEL PROCESSING (3 main tasks)
  │   │   │
  │   │   ├─► TASK 1: AST Generation
  │   │   │   ├─► format_ast(code_input)
  │   │   │   │   ├─► Parse with javalang.parse.parse()
  │   │   │   │   ├─► Extract classes, methods, fields
  │   │   │   │   └─► Format as HTML tree structure
  │   │   │   └─► Return HTML formatted AST
  │   │   │
  │   │   ├─► TASK 2: Comment Generation (Batch Processing)
  │   │   │   ├─► Extract classes and methods
  │   │   │   │   ├─► extract_classes(code_input)
  │   │   │   │   └─► extract_methods(code_input)
  │   │   │   │
  │   │   │   ├─► Prepare batch inputs
  │   │   │   │   ├─► Collect all class codes
  │   │   │   │   ├─► Collect all method codes
  │   │   │   │   └─► Preprocess each (preprocess_code)
  │   │   │   │
  │   │   │   ├─► Batch Process with ML Model
  │   │   │   │   ├─► hf_pipeline(all_inputs, batch_size=8)
  │   │   │   │   ├─► Model processes all inputs simultaneously
  │   │   │   │   └─► Returns batch of generated comments
  │   │   │   │
  │   │   │   ├─► Map results back
  │   │   │   │   ├─► clean_comment() for each result
  │   │   │   │   └─► Format as HTML comments
  │   │   │   │
  │   │   │   └─► Group by class
  │   │   │       └─► grouped_comments structure
  │   │   │
  │   │   └─► TASK 3: Code Hash (already computed)
  │   │
  │   ├─► Save to Database
  │   │   ├─► Create CodeSubmission object
  │   │   │   ├─► user_id = current_user.id
  │   │   │   ├─► code_content = code_input
  │   │   │   ├─► submission_name = auto-generated
  │   │   │   ├─► ast_content = ast_output
  │   │   │   ├─► comments_content = comments_output
  │   │   │   ├─► code_hash = code_hash
  │   │   │   └─► is_success = True
  │   │   │
  │   │   ├─► db.session.add(submission)
  │   │   └─► db.session.commit()
  │   │
  │   └─► Return JSON Response
  │       ├─► comments: HTML formatted comments
  │       ├─► ast: HTML formatted AST
  │       └─► cfg_supported: true
  │
  └─► React receives response
      ├─► Display comments in Comments panel
      ├─► Display AST in AST panel
      └─► Enable CFG generation button
```

---

### 4. AST JSON Generation Flow (Graphical View)

```
User → Model Page → Clicks "Graphical AST View"
  │
  ├─► POST /ast-json (with JSON: {code: "..."})
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │   └─► @login_required
  │   │
  │   ├─► Extract code from request
  │   │
  │   ├─► Parse Java code
  │   │   └─► javalang.parse.parse()
  │   │
  │   ├─► Build AST JSON structure
  │   │   └─► build_ast_json(code)
  │   │       ├─► Extract class structure
  │   │       ├─► Extract method structure
  │   │       ├─► Build hierarchical JSON
  │   │       │   └─► {
  │   │       │       "name": "ClassName",
  │   │       │       "type": "class",
  │   │       │       "children": [...]
  │   │       │   }
  │   │       │
  │   │       └─► Generate Comments (Batch Processing)
  │   │           ├─► Prepare all class/method inputs
  │   │           ├─► hf_pipeline(all_inputs, batch_size=8)
  │   │           └─► Attach comments to nodes
  │   │
  │   └─► Return JSON Response
  │       └─► {ast: {...}, comments: {...}}
  │
  └─► React receives response
      ├─► ASTVisualization component renders
      ├─► Uses D3.js to create interactive tree
      └─► Applies theme (light/dark)
```

---

### 5. CFG (Control Flow Graph) Generation Flow

```
User → Model Page → Clicks "Generate CFG"
  │
  ├─► POST /generate-cfg (with JSON: {code: "...", theme: "dark"})
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │   └─► @login_required
  │   │
  │   ├─► Extract code and theme from request
  │   │
  │   ├─► Create CFG Generator
  │   │   └─► generator = CFGGenerator()
  │   │
  │   ├─► Generate CFG
  │   │   └─► cfg = generator.generate(code)
  │   │       ├─► Parse Java code (javalang)
  │   │       ├─► Build control flow graph
  │   │       │   ├─► Identify basic blocks
  │   │       │   ├─► Identify control flow edges
  │   │       │   └─► Handle branches, loops, returns
  │   │       └─► Return NetworkX graph object
  │   │
  │   ├─► Visualize as SVG
  │   │   └─► svg_content = generator.visualize(format="svg", theme=theme)
  │   │       ├─► Use Graphviz (Digraph)
  │   │       ├─► Apply theme colors
  │   │       │   ├─► Light: white background, black text
  │   │       │   └─► Dark: dark background, light text
  │   │       └─► Generate SVG string
  │   │
  │   └─► Return SVG Response
  │       └─► Response(svg_content, mimetype='image/svg+xml')
  │
  └─► React receives SVG
      └─► CFGVisualization component displays SVG
```

---

### 6. Dashboard Flow

```
User → Dashboard Page (React)
  │
  ├─► Component mounts
  │   └─► useEffect hook triggers
  │
  ├─► GET /api/dashboard
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │   └─► @login_required
  │   │
  │   ├─► Query User Submissions
  │   │   └─► CodeSubmission.query
  │   │       ├─► filter_by(user_id=current_user.id)
  │   │       ├─► filter_by(is_success=True)
  │   │       └─► order_by(timestamp.desc())
  │   │
  │   └─► Return JSON Response
  │       └─► {
  │           username: "...",
  │           submissions: [
  │               {id, submission_name, timestamp},
  │               ...
  │           ]
  │       }
  │
  └─► React displays submissions
      ├─► Render list of submissions
      ├─► Each submission is clickable
      └─► Click → Navigate to Model page with submission ID
```

---

### 7. Submission Retrieval Flow

```
User clicks on submission in Dashboard
  │
  ├─► Navigate to /model?submission_id=123
  │
  ├─► Model component loads
  │   └─► useEffect checks for submission_id
  │
  ├─► GET /get-submission/123
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │
  │   ├─► Query Submission
  │   │   └─► CodeSubmission.query.get(id)
  │   │       └─► Verify ownership (user_id match)
  │   │
  │   └─► Return JSON Response
  │       └─► {
  │           code: "...",
  │           ast: "...",
  │           comments: "...",
  │           submission_name: "..."
  │       }
  │
  └─► React populates editor and panels
      ├─► Monaco Editor: code
      ├─► AST Panel: ast
      └─► Comments Panel: comments
```

---

### 8. Folder Upload Flow

```
User → Model Page → Clicks "Upload Java Folder"
  │
  ├─► File input dialog opens
  │
  ├─► User selects folder (multiple .java files)
  │
  ├─► POST /process-folder (FormData with files)
  │   │
  │   ├─► Flask receives request
  │   │
  │   ├─► Authentication Check
  │   │
  │   ├─► Extract files from request
  │   │   └─► request.files.getlist('files[]')
  │   │
  │   ├─► Process each file
  │   │   ├─► For each file:
  │   │   │   ├─► Read file content
  │   │   │   ├─► Extract classes and methods
  │   │   │   ├─► Generate comments (batch processing)
  │   │   │   └─► Format AST
  │   │   │
  │   │   └─► Combine results
  │   │
  │   └─► Return JSON Response
  │       └─► {
  │           files: [
  │               {filename, ast, comments},
  │               ...
  │           ]
  │       }
  │
  └─► React displays results
      └─► FileSidebar component shows file structure
```

---

### 9. Theme Management Flow

```
User clicks Theme Toggle (🌙/☀️)
  │
  ├─► ThemeContext.toggleTheme()
  │   ├─► Toggle theme state (light ↔ dark)
  │   └─► Save to localStorage
  │
  ├─► All components update
  │   ├─► CSS variables change
  │   │   ├─► --bg-primary
  │   │   ├─► --text-primary
  │   │   └─► --bg-secondary
  │   │
  │   ├─► Monaco Editor theme
  │   │   └─► 'vs-dark' or 'vs'
  │   │
  │   ├─► AST Visualization (D3.js)
  │   │   └─► Update node/link colors
  │   │
  │   └─► CFG SVG
  │       └─► Regenerate with new theme
  │
  └─► UI updates immediately
```

---

### 10. Request Routing Flow (Flask → React)

```
Browser Request
  │
  ├─► Is it /api/* or /auth/* POST?
  │   └─► YES → Handle by Flask route
  │       └─► Return JSON response
  │
  ├─► Is it /static/*?
  │   └─► YES → Serve static file
  │
  └─► NO → Catch-all route
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

## Data Flow Diagrams

### Database Schema

```
┌─────────────────────┐
│       User          │
├─────────────────────┤
│ id (PK)             │
│ username (unique)   │
│ email (unique)      │
│ password_hash       │
└──────────┬──────────┘
           │
           │ 1:N relationship
           │
┌──────────▼──────────────────┐
│    CodeSubmission           │
├─────────────────────────────┤
│ id (PK)                     │
│ user_id (FK → User.id)      │
│ code_content (TEXT)         │
│ submission_name              │
│ timestamp                    │
│ is_success (BOOLEAN)        │
│ ast_content (TEXT)          │
│ comments_content (TEXT)      │
│ code_hash (STRING)          │
│ cfg_image (STRING)          │
└─────────────────────────────┘
```

---

### Comment Generation Batch Processing Flow

```
Input: [Class1, Class2, Method1, Method2, Method3, ...]
  │
  ├─► Preprocess each input
  │   └─► preprocess_code() for each
  │
  ├─► Batch into groups of 8
  │   ├─► Batch 1: [Class1, Class2, Method1, ..., Method6]
  │   ├─► Batch 2: [Method7, Method8, ...]
  │   └─► ...
  │
  ├─► Process each batch
  │   └─► hf_pipeline(batch, batch_size=8)
  │       ├─► Tokenize all inputs
  │       ├─► Model forward pass (parallel)
  │       └─► Decode all outputs
  │
  ├─► Collect all results
  │   └─► [Comment1, Comment2, Comment3, ...]
  │
  ├─► Clean each comment
  │   └─► clean_comment() for each
  │
  └─► Map back to classes/methods
      └─► grouped_comments structure
```

---

## Error Handling Flow

```
Any Route Handler
  │
  ├─► Try block
  │   └─► Execute main logic
  │
  ├─► Catch JavaSyntaxError
  │   └─► Return error message
  │       └─► "Invalid Java syntax: ..."
  │
  ├─► Catch Database Error
  │   ├─► Rollback session
  │   └─► Return error message
  │
  ├─► Catch Model Error
  │   └─► Fallback to sequential processing
  │       └─► Or return error message
  │
  └─► Catch Generic Exception
      ├─► Log error (current_app.logger.error)
      ├─► Create error submission (if applicable)
      └─► Return JSON error response
```

---

## Performance Optimizations

### 1. Batch Processing
- **Problem**: Processing each class/method individually is slow
- **Solution**: Batch all inputs together
- **Speedup**: 10-50x faster

### 2. Model Optimization
- **max_length**: Reduced from 512 → 64
- **num_beams**: Set to 1 (greedy decoding)
- **do_sample**: False (deterministic)
- **Speedup**: 4-8x faster per inference

### 3. Code Deduplication
- **Hash-based caching**: Store code_hash
- **Reuse existing submissions**: Skip reprocessing
- **Speedup**: Instant for duplicate code

### 4. Database Indexing
- **Indexed fields**: user_id, code_hash
- **Query optimization**: Filtered queries
- **Speedup**: Faster lookups

---

## Technologies and Tools Used

### Frontend Technologies

1. **React 18.2.0**
   - JavaScript library for building user interfaces
   - Component-based architecture
   - Used for: All UI components, routing, state management

2. **React Router DOM 6.20.0**
   - Client-side routing for React applications
   - Used for: Navigation between pages (Home, Model, Dashboard, etc.)

3. **Vite 5.0.8**
   - Next-generation frontend build tool
   - Fast development server and optimized production builds
   - Used for: Development server, bundling, hot module replacement

4. **Monaco Editor 4.6.0**
   - Web-based code editor (VS Code editor)
   - Syntax highlighting, code completion, themes
   - Used for: Java code input/editing

5. **D3.js 7.8.5**
   - Data visualization library
   - Used for: Interactive AST tree visualization

6. **Bootstrap 5.3.2**
   - CSS framework for responsive design
   - Used for: UI components, grid system, styling

7. **React Bootstrap 2.9.1**
   - Bootstrap components for React
   - Used for: Pre-built React components

8. **Axios 1.6.2**
   - HTTP client library
   - Used for: API requests (though fetch is primarily used)

### Backend Technologies

1. **Flask**
   - Python web framework
   - Used for: Backend API, routing, request handling

2. **Flask-SQLAlchemy**
   - SQL toolkit and ORM for Flask
   - Used for: Database operations, model definitions

3. **Flask-Login**
   - User session management
   - Used for: Authentication, user sessions

4. **SQLite**
   - Lightweight relational database
   - Used for: Storing users and code submissions

5. **Werkzeug**
   - WSGI utility library
   - Used for: Password hashing, security utilities

### Code Analysis Technologies

1. **javalang 0.13.0**
   - Java parser for Python
   - Used for: Parsing Java code, extracting AST structure

2. **NetworkX**
   - Python library for graph analysis
   - Used for: Building control flow graphs

3. **Graphviz**
   - Graph visualization software
   - Used for: Rendering CFG as SVG

### Machine Learning Technologies

1. **Hugging Face Transformers**
   - Library for pre-trained transformer models
   - Used for: Loading and running the SEBIS model

2. **PyTorch**
   - Deep learning framework
   - Used for: Model inference (CPU/GPU)

3. **SEBIS Model**
   - Pre-trained T5-based model for code comment generation
   - Used for: Generating comments for Java classes and methods

### Development Tools

1. **Python 3.11+**
   - Programming language
   - Used for: Backend development

2. **Node.js & npm**
   - JavaScript runtime and package manager
   - Used for: Frontend dependency management, build process

3. **Git**
   - Version control system
   - Used for: Source code management

### Additional Libraries

1. **concurrent.futures**
   - Python module for parallel execution
   - Used for: Batch processing optimization (though batch processing is now preferred)

2. **hashlib**
   - Python module for hashing
   - Used for: Generating code hashes (SHA-256)

3. **uuid**
   - Python module for unique identifiers
   - Used for: Generating unique submission names

4. **datetime**
   - Python module for date/time handling
   - Used for: Timestamp management

### Build and Deployment

1. **Vite Build**
   - Production build tool
   - Used for: Creating optimized React build files

2. **Flask Development Server**
   - Built-in Flask server
   - Used for: Development and production (can be replaced with Gunicorn/uWSGI)

### File Formats

1. **JSON**
   - Data interchange format
   - Used for: API request/response format

2. **SVG**
   - Scalable vector graphics
   - Used for: CFG visualization output

3. **HTML**
   - Markup language
   - Used for: AST and comments display

---

## Summary

The Clarifai project is a full-stack web application that combines:
- **Modern React frontend** with interactive visualizations
- **Flask backend** with RESTful API design
- **Machine learning** for automated code documentation
- **Code analysis tools** for AST and CFG generation
- **Database** for user and submission management

The system is optimized for performance through batch processing, model optimization, and efficient database queries, providing a fast and responsive user experience for Java code analysis and documentation.

