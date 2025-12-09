import React, { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { useTheme } from '../contexts/ThemeContext';
import './Dashboard.css';

const Dashboard = () => {
  const { theme } = useTheme();
  const [submissions, setSubmissions] = useState([]);
  const [selectedSubmission, setSelectedSubmission] = useState(null);
  const [viewType, setViewType] = useState('code');
  const [isLoading, setIsLoading] = useState(false);
  const [username, setUsername] = useState('');

  useEffect(() => {
    fetchSubmissions();
  }, []);

  const fetchSubmissions = async () => {
    try {
      const response = await fetch('/api/dashboard', {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        setSubmissions(data.submissions || []);
        setUsername(data.username || '');
      }
    } catch (error) {
      console.error('Failed to fetch submissions:', error);
    }
  };

  const loadSubmission = async (submissionId) => {
    setIsLoading(true);
    try {
      const response = await fetch(`/get-submission/${submissionId}`, {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        setSelectedSubmission(data);
        setViewType('code');
      }
    } catch (error) {
      console.error('Failed to load submission:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const deleteSubmission = async (submissionId) => {
    if (!window.confirm('Are you sure you want to delete this submission?')) {
      return;
    }

    try {
      const response = await fetch(`/delete-submission/${submissionId}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (response.ok) {
        setSubmissions(submissions.filter(s => s.id !== submissionId));
        if (selectedSubmission && selectedSubmission.id === submissionId) {
          setSelectedSubmission(null);
        }
      }
    } catch (error) {
      console.error('Failed to delete submission:', error);
    }
  };

  const renameSubmission = async (submissionId, newName) => {
    try {
      const response = await fetch(`/rename-submission/${submissionId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ new_name: newName }),
      });
      if (response.ok) {
        setSubmissions(submissions.map(s => 
          s.id === submissionId ? { ...s, submission_name: newName } : s
        ));
      }
    } catch (error) {
      console.error('Failed to rename submission:', error);
    }
  };

  return (
    <div className="container-fluid dashboard-section" style={{ maxWidth: '1400px', width: '100%' }}>
      <section className="dashboard-section">
        <div className="welcome-text align-self-start mb-4">
          <h2>Welcome, <span className="text-gradient">{username}</span></h2>
          <div className="align-self-start mb-4">
            <p>View and manage your code submissions.</p>
          </div>
        </div>

        <div className="row g-4">
          <div className="col-md-4">
            <div className="card h-100">
              <div className="card-header-blue bg-primary text-white">
                <h5 className="mb-0">Code History</h5>
              </div>
              <div className="card-body" style={{ overflowY: 'auto', maxHeight: '500px' }}>
                {submissions.length === 0 ? (
                  <p className="text-muted">No submissions yet</p>
                ) : (
                  submissions.map(sub => (
                    <SubmissionItem
                      key={sub.id}
                      submission={sub}
                      onSelect={() => loadSubmission(sub.id)}
                      onDelete={() => deleteSubmission(sub.id)}
                      onRename={(newName) => renameSubmission(sub.id, newName)}
                    />
                  ))
                )}
              </div>
            </div>
          </div>

          <div className="col-md-8">
            <div className="card h-100">
              <div className="card-header-blue bg-primary text-white">
                <h5 className="mb-0 d-inline">Code Preview</h5>
                <div className="btn-group float-end" role="group">
                  <button
                    type="button"
                    className={`btn btn-sm btn-outline-light ${viewType === 'code' ? 'active' : ''}`}
                    onClick={() => setViewType('code')}
                  >
                    Code
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm btn-outline-light ${viewType === 'ast' ? 'active' : ''}`}
                    onClick={() => setViewType('ast')}
                  >
                    AST
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm btn-outline-light ${viewType === 'comments' ? 'active' : ''}`}
                    onClick={() => setViewType('comments')}
                  >
                    Comments
                  </button>
                </div>
              </div>
              <div className="card-body p-0 position-relative">
                {isLoading && (
                  <div className="loading-overlay">
                    <div className="spinner-border text-primary"></div>
                    <span className="loading-text">Loading...</span>
                  </div>
                )}
                {selectedSubmission ? (
                  <>
                    {viewType === 'code' && (
                      <div style={{ height: '500px', width: '100%' }}>
                        <Editor
                          height="500px"
                          language="java"
                          value={selectedSubmission.code_content}
                          theme={theme === 'dark' ? 'vs-dark' : 'vs'}
                          options={{
                            readOnly: true,
                            minimap: { enabled: false },
                            fontSize: 14,
                            scrollBeyondLastLine: false,
                            automaticLayout: true,
                          }}
                        />
                      </div>
                    )}
                    {viewType === 'ast' && (
                      <div
                        style={{
                          height: '500px',
                          overflow: 'auto',
                          padding: '15px',
                          backgroundColor: theme === 'dark' ? 'var(--ast-bg)' : '#ffffff',
                          color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
                        }}
                        dangerouslySetInnerHTML={{ __html: selectedSubmission.ast_content }}
                      />
                    )}
                    {viewType === 'comments' && (
                      <div
                        style={{
                          height: '500px',
                          overflow: 'auto',
                          padding: '15px',
                          backgroundColor: theme === 'dark' ? 'var(--bg-secondary)' : '#ffffff',
                          color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
                        }}
                        dangerouslySetInnerHTML={{ __html: selectedSubmission.comments_content }}
                      />
                    )}
                  </>
                ) : (
                  <div
                    style={{
                      height: '500px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
                    }}
                  >
                    <p>Select a submission to preview</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

const SubmissionItem = ({ submission, onSelect, onDelete, onRename }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(submission.submission_name);

  const handleSave = () => {
    if (name !== submission.submission_name) {
      onRename(name);
    }
    setIsEditing(false);
  };

  return (
    <div className="submission-item mb-3 card p-2">
      <div className="d-flex justify-content-between align-items-center">
        {isEditing ? (
          <input
            type="text"
            className="form-control submission-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={handleSave}
            onKeyPress={(e) => e.key === 'Enter' && handleSave()}
            autoFocus
          />
        ) : (
          <div className="submission-name-display" style={{ color: 'var(--text-primary)' }}>
            {submission.submission_name}
          </div>
        )}
        <div className="btn-group">
          <button
            className="btn btn-sm btn-outline-secondary"
            onClick={() => setIsEditing(!isEditing)}
            title="Rename"
          >
            <i className="bi bi-pencil"></i>
          </button>
          <button className="btn btn-sm btn-outline-primary" onClick={onSelect}>
            Preview
          </button>
          <button
            className="btn btn-sm btn-outline-danger"
            onClick={onDelete}
            title="Delete"
          >
            <i className="bi bi-trash"></i>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

