# ClarifAI

A web application for analyzing Java code with Abstract Syntax Tree (AST) and Control Flow Graph (CFG) visualizations, powered by machine learning for code documentation generation.

## Features

- **Code Analysis**: Upload or paste Java code for analysis
- **AST Visualization**: Interactive Abstract Syntax Tree visualization with node expansion
- **CFG Visualization**: Control Flow Graph generation and visualization
- **User Authentication**: Secure login and signup system
- **Dashboard**: View submission history and documentation
- **ML-Powered Documentation**: Automatic code documentation generation using fine-tuned models
- **Theme Support**: Light and dark theme options
- **Code Editor**: Monaco editor with syntax highlighting

## Tech Stack

### Frontend
- React 18.2.0
- React Router DOM 6.20.0
- Monaco Editor
- D3.js for graph visualizations
- Bootstrap & React Bootstrap
- Vite for build tooling

### Backend
- Flask (Python)
- SQLAlchemy for database management
- Flask-Login for authentication
- Javalang for Java code parsing
- NetworkX & Graphviz for graph generation
- Hugging Face Transformers for ML model inference

## Prerequisites

- **Python**: Version 3.11 or lower (not higher than 3.11)
- **Node.js**: Latest LTS version
- **npm**: Comes with Node.js
- **SentencePiece**: Required Python package (install separately)

## Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd clarifai
   ```

2. **Install Python dependencies**
   ```bash
   pip install flask flask-sqlalchemy flask-login javalang networkx graphviz transformers torch sentencepiece
   ```
   
   Note: Make sure to install SentencePiece separately if it's not included in the above command.

3. **Install Node.js dependencies**
   ```bash
   npm install
   ```

4. **Configure the application**
   - Update `app/config.py` with your model path if different from the default
   - Set environment variables if needed (SECRET_KEY, DATABASE_URL, MODEL_PATH)

5. **Build the React frontend**
   ```bash
   npm run build
   ```

## Usage

1. **Start the Flask backend server**
   ```bash
   python run.py
   ```
   The server will start on `http://0.0.0.0:5000` (or `http://localhost:5000`)

2. **Access the application**
   - Open your browser and navigate to `http://localhost:5000`
   - The ClarifAI website will be served locally

3. **Using the application**
   - **Sign up/Login**: Create an account or login with existing credentials
   - **Dashboard**: View your submission history and documentation
   - **Code Submission**: 
     - Copy code into the editor, or
     - Upload a file/folder
     - Click "Submit Code" to send the code to the backend
   - **AST Visualization**: 
     - Click on a node to focus it
     - Click again to expand its children
     - Use fullscreen buttons for better navigation
   - **CFG Visualization**: 
     - View the generated Control Flow Graph
     - Use fullscreen mode for detailed exploration

## Project Structure

```
clarifai/
├── app/                    # Flask backend application
│   ├── __init__.py        # Flask app factory
│   ├── config.py          # Configuration settings
│   ├── models.py          # Database models
│   ├── utils.py           # Utility functions for code processing
│   ├── cfg_utils.py       # CFG generation utilities
│   ├── auth/              # Authentication routes
│   │   └── routes.py
│   ├── main/              # Main application routes
│   │   └── routes.py
│   └── static/            # Static files
│       ├── cfg_images/    # Generated CFG images
│       └── react-build/   # Built React app (after npm run build)
├── src/                   # React frontend source
│   ├── components/        # React components
│   │   ├── ASTVisualization.jsx
│   │   ├── CFGVisualization.jsx
│   │   ├── FileSidebar.jsx
│   │   └── Navbar.jsx
│   ├── pages/             # Page components
│   │   ├── Home.jsx
│   │   ├── Model.jsx
│   │   ├── Dashboard.jsx
│   │   ├── Login.jsx
│   │   ├── Signup.jsx
│   │   └── Settings.jsx
│   ├── contexts/          # React contexts
│   │   └── ThemeContext.jsx
│   ├── App.jsx            # Main App component
│   └── main.jsx           # Entry point
├── java test cases/       # Sample Java test files
├── run.py                 # Application entry point
├── package.json           # Node.js dependencies
├── vite.config.js         # Vite configuration
└── users.db              # SQLite database (created automatically)
```

## Configuration

### Model Path
Update the `MODEL_PATH` in `app/config.py` to point to your fine-tuned model directory:
```python
MODEL_PATH = "path/to/your/model"
```

### Database
The application uses SQLite by default. To use a different database, set the `DATABASE_URL` environment variable or update `app/config.py`.

### Secret Key
For production, set a secure `SECRET_KEY` environment variable instead of using the default.

## Development

### Frontend Development
```bash
npm run dev    # Start Vite dev server
```

### Backend Development
The Flask app runs in debug mode by default when using `python run.py`.

## Notes

- The application requires Python 3.11 or lower
- SentencePiece must be installed separately
- The React build must be generated before running the Flask server
- The ML model path must be configured correctly for documentation generation to work
