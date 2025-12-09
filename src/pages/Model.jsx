import React, { useState, useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { useTheme } from '../contexts/ThemeContext';
import ASTVisualization from '../components/ASTVisualization';
import CFGVisualization from '../components/CFGVisualization';
import FileSidebar from '../components/FileSidebar';
import './Model.css';

const Model = () => {
  const { theme } = useTheme();
  const [code, setCode] = useState('');
  const [astOutput, setAstOutput] = useState('');
  const [commentsOutput, setCommentsOutput] = useState('');
  const [astData, setAstData] = useState(null);
  const [isGraphicalView, setIsGraphicalView] = useState(false);
  const [isLoading, setIsLoading] = useState({ ast: false, comments: false, cfg: false });
  const [fileStructure, setFileStructure] = useState({});
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const editorRef = useRef(null);

  const handleEditorDidMount = (editor) => {
    editorRef.current = editor;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const codeToSubmit = editorRef.current?.getValue() || code;
    
    if (!codeToSubmit.trim()) {
      alert('Please enter some code');
      return;
    }

    setIsLoading({ ast: true, comments: true, cfg: false });
    setAstOutput('');
    setCommentsOutput('');
    setAstData(null); // Reset AST data when submitting new code
    setIsGraphicalView(false); // Reset to text view when submitting new code

    try {
      const response = await fetch('/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ code: codeToSubmit }),
      });

      const data = await response.json();
      setAstOutput(data.ast || 'No AST generated');
      setCommentsOutput(data.comments || 'No comments generated');
      
      // Load AST JSON for graphical view
      if (data.cfg_supported) {
        try {
          const astResponse = await fetch('/ast-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ code: codeToSubmit }),
          });
          const astJson = await astResponse.json();
          setAstData(astJson);
        } catch (error) {
          console.error('Failed to load AST JSON:', error);
          setAstData(null);
        }
      } else {
        setAstData(null);
      }
    } catch (error) {
      console.error('Error:', error);
      setAstOutput('Error: ' + error.message);
      setCommentsOutput('Error: ' + error.message);
    } finally {
      setIsLoading({ ast: false, comments: false, cfg: false });
    }
  };

  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file && file.name.endsWith('.java')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const contents = e.target.result;
        setCode(contents);
        if (editorRef.current) {
          editorRef.current.setValue(contents);
        }
      };
      reader.readAsText(file);
    } else {
      alert('Please upload a Java source file (.java)');
    }
  };

  const handleFolderUpload = (event) => {
    const files = Array.from(event.target.files);
    const structure = {};
    
    files.forEach(file => {
      if (file.name.endsWith('.java')) {
        const path = file.webkitRelativePath || file.name;
        const parts = path.split('/');
        let current = structure;
        
        for (let i = 0; i < parts.length - 1; i++) {
          if (!current[parts[i]]) {
            current[parts[i]] = { _type: 'folder' };
          }
          current = current[parts[i]];
        }
        
        current[parts[parts.length - 1]] = {
          _type: 'file',
          file: file,
          path: path,
        };
      }
    });
    
    setFileStructure(structure);
    setIsSidebarOpen(true);
    
    // Load first Java file
    const firstFile = findFirstJavaFile(structure);
    if (firstFile) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const contents = e.target.result;
        setCode(contents);
        if (editorRef.current) {
          editorRef.current.setValue(contents);
        }
      };
      reader.readAsText(firstFile.file);
    }
  };

  const findFirstJavaFile = (structure) => {
    for (const key in structure) {
      if (key === '_type') continue;
      if (structure[key]._type === 'file' && key.endsWith('.java')) {
        return structure[key];
      } else if (structure[key]._type === 'folder') {
        const result = findFirstJavaFile(structure[key]);
        if (result) return result;
      }
    }
    return null;
  };

  const loadFileFromStructure = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const contents = e.target.result;
      setCode(contents);
      if (editorRef.current) {
        editorRef.current.setValue(contents);
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="model-section" style={{ paddingTop: '70px' }}>
      <div className="container-fluid" style={{ maxWidth: '1400px', width: '100%' }}>
        <FileSidebar
          isOpen={isSidebarOpen}
          onClose={() => setIsSidebarOpen(false)}
          fileStructure={fileStructure}
          onFileSelect={loadFileFromStructure}
        />

        <section className="model-section-content">
          <form onSubmit={handleSubmit} className="code-form">
            <div className="mb-3">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <label className="form-label">Java Code Input</label>
                <div className="d-flex gap-2">
                  <input
                    type="file"
                    id="fileUpload"
                    accept=".java"
                    className="d-none"
                    onChange={handleFileUpload}
                  />
                  <input
                    type="file"
                    id="folderUpload"
                    webkitdirectory
                    directory
                    multiple
                    className="d-none"
                    onChange={handleFolderUpload}
                  />
                  <label htmlFor="fileUpload" className="btn btn-sm clarifai-btn" style={{ margin: 0, marginBottom: '10px', fontSize: '0.9rem', padding: '6px 12px' }}>
                    📁 Upload Java File
                  </label>
                  <label htmlFor="folderUpload" className="btn btn-sm clarifai-btn" style={{ margin: 0, marginBottom: '10px', fontSize: '0.9rem', padding: '6px 12px' }}>
                    📂 Upload Folder
                  </label>
                  <button
                    type="button"
                    className="btn btn-sm clarifai-btn"
                    style={{ margin: 0, marginBottom: '10px', fontSize: '0.9rem', padding: '6px 12px' }}
                    onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                  >
                    <i className="bi bi-folder2-open"></i>
                  </button>
                </div>
              </div>
              <div id="editorContainer" style={{ height: '450px', border: '1px solid var(--border-color)', borderRadius: '4px' }}>
                <Editor
                  height="450px"
                  defaultLanguage="java"
                  value={code}
                  theme={theme === 'dark' ? 'vs-dark' : 'vs'}
                  onChange={(value) => setCode(value || '')}
                  onMount={handleEditorDidMount}
                  options={{
                    minimap: { enabled: false },
                    fontSize: 14,
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    lineNumbers: 'on',
                    formatOnPaste: true,
                    formatOnType: true,
                  }}
                />
              </div>
            </div>
            <div className="d-flex justify-content-center">
              <button type="submit" className="btn btn-primary clarifai-btn">
                Submit Code
              </button>
            </div>
          </form>
        </section>

        {/* Output Section */}
        <section className="output-section" id="output-section">
          <div className="container-fluid code-form" style={{ marginBottom: '40px', borderRadius: '10px', boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}>
            <div className="output-container">
              {/* AST Column */}
              <div className="ast-section d-flex flex-column position-relative">
                <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                  <label className="form-label mb-0">Generated AST</label>
                  <div className="d-flex gap-2 flex-wrap align-items-center">
                    <button
                      className="btn btn-sm clarifai-btn"
                      onClick={() => setIsGraphicalView(!isGraphicalView)}
                    >
                      {isGraphicalView ? 'Switch to Text View' : 'Switch to Graphical View'}
                    </button>
                  </div>
                </div>
                {isLoading.ast && (
                  <div className="loading-overlay">
                    <div className="spinner-border text-primary" role="status"></div>
                    <span className="loading-text">Generating AST...</span>
                  </div>
                )}
                {isGraphicalView ? (
                  <ASTVisualization astData={astData} theme={theme} />
                ) : (
                  <div
                    className="form-control ast-output"
                    style={{
                      fontFamily: 'monospace',
                      height: '600px',
                      backgroundColor: theme === 'dark' ? 'var(--ast-bg)' : '#ffffff',
                      color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
                    }}
                    dangerouslySetInnerHTML={{ __html: astOutput }}
                  />
                )}
              </div>

              {/* Comments Column */}
              <div className="comments-section d-flex flex-column position-relative">
                <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                  <label className="form-label mb-0">Generated Comments</label>
                </div>
                {isLoading.comments && (
                  <div className="loading-overlay">
                    <div className="spinner-border text-primary" role="status"></div>
                    <span className="loading-text">Generating comments...</span>
                  </div>
                )}
                <div
                  className="form-control comments-output"
                  style={{
                    fontFamily: 'monospace',
                    height: '600px',
                    backgroundColor: theme === 'dark' ? 'var(--bg-secondary)' : '#ffffff',
                    color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
                  }}
                  dangerouslySetInnerHTML={{ __html: commentsOutput }}
                />
              </div>

              {/* CFG Section */}
              <CFGVisualization
                code={code}
                editorRef={editorRef}
                theme={theme}
                isLoading={isLoading.cfg}
                setIsLoading={(loading) => setIsLoading(prev => ({ ...prev, cfg: loading }))}
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default Model;

