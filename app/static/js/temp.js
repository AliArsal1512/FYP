// Dashboard functionality
document.addEventListener('DOMContentLoaded', function() {
  if (document.getElementById('zoomInBtn')) {
        document.getElementById('zoomInBtn').addEventListener('click', zoomIn);
        document.getElementById('zoomOutBtn').addEventListener('click', zoomOut);
        document.getElementById('resetZoomBtn').addEventListener('click', resetZoom);
    }
    
    // Initialize panning
    setupPanning();
    
    // Make container draggable
    const container = document.getElementById('cfgContainer');
    if (container) {
        container.style.cursor = 'grab';
    }
  // Handle navbar scroll effect
  window.addEventListener('scroll', function() {
    if (window.scrollY > 100) {
      document.querySelector('#mainNavbar').classList.add('navbar-shrink');
    } else {
      document.querySelector('#mainNavbar').classList.remove('navbar-shrink');
    }
  });

  // Initialize with scroll position check
  if (window.scrollY > 100) {
    document.querySelector('#mainNavbar').classList.add('navbar-shrink');
  }

  // Only run dashboard code if we're on the dashboard page
  if (document.querySelector('.dashboard-section')) {
    initDashboard();
  }

  // Only run editor code if we're on the home page with editor
  if (document.getElementById('editorContainer')) {
    initEditor();
    // CFG button event listener
    const cfgBtn = document.getElementById('generateCfgBtn');
    if (cfgBtn) {
      cfgBtn.addEventListener('click', generateCFG);
      console.log("CFG button event listener added"); // Debug log
    }
  }
});

// Initialize Monaco Editor on home page
function initEditor() {
  require.config({
    paths: {
      'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.34.1/min/vs'
    }
  });

  // Make editor globally accessible
  window.editor = null;

  require(['vs/editor/editor.main'], function() {
    monaco.editor.defineTheme('customTheme', {
      base: 'vs',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': '#ffffff', 
      }
    });

    window.editor = monaco.editor.create(document.getElementById('editorContainer'), {
      value: `{{ code_input }}`,
      language: 'java',
      theme: 'customTheme',
      minimap: {
        enabled: false
      },
      fontSize: 14,
      roundedSelection: false,
      scrollBeyondLastLine: false,
      automaticLayout: true,
      lineNumbers: 'on',
      contextmenu: false,
      formatOnPaste: true,
      formatOnType: true
    });

    editor.onDidChangeModelContent(function() {
      document.getElementById('hiddenCode').value = editor.getValue();
    });

    window.addEventListener('resize', function() {
      if (window.editor) {
        window.editor.layout();
      }
    });
  });

  // File upload functionality
  if (document.getElementById('fileUpload')) {
    document.getElementById('fileUpload').addEventListener('change', function(e) {
      const file = e.target.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = function(e) {
        const contents = e.target.result;
        if (editor) {
          editor.setValue(contents);
          document.getElementById('hiddenCode').value = contents;
        }
      };
      
      reader.onerror = function(error) {
        console.error('Error reading file:', error);
        alert('Error reading file. Please try again.');
      };

      if (file.type === 'text/x-java-source' || file.name.endsWith('.java')) {
        reader.readAsText(file);
      } else {
        alert('Please upload a Java source file (.java)');
        this.value = ''; // Clear input
      }
    });
  }

  // Form submission for code analysis
  const form = document.querySelector('form');
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const code = editor.getValue();
      if (document.getElementById('fileUpload')) {
        document.getElementById('fileUpload').value = ''; // Clear file input
      }
      
      // Clear previous outputs
      document.getElementById('commentsOutput').innerHTML = '';
      document.getElementById('astOutput').innerHTML = '';
      
      // Show loading states
      document.getElementById('commentsLoading').style.display = 'flex';
      document.getElementById('astLoading').style.display = 'flex';

      try {
        const response = await fetch('/', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({ code: code })
        });

        const data = await response.json().catch(() => ({
          comments: 'Invalid server response',
          ast: 'Invalid server response'
        }));
        
        // Handle all responses as JSON
        document.getElementById('commentsOutput').innerHTML = data.comments || "No output generated";
        document.getElementById('astOutput').innerHTML = data.ast || "No AST generated";

      } catch (error) {
        console.error('Error:', error);
        document.getElementById('commentsOutput').innerHTML = `Network error: ${error.message}`;
        document.getElementById('astOutput').innerHTML = "Could not reach server";
      } finally {
        document.getElementById('commentsLoading').style.display = 'none';
        document.getElementById('astLoading').style.display = 'none';

        // Scroll to output section after results are loaded
        document.getElementById('output-section').scrollIntoView({ 
          behavior: 'smooth',
          block: 'start' // Aligns with top of output section
        });
      }
    });
  }

  // AST interaction functions
  if (document.getElementById('showAllBtn')) {
    document.getElementById('showAllBtn').addEventListener('click', () => {
      document.querySelectorAll('.active, .active-parent').forEach(el => {
        el.classList.remove('active', 'active-parent');
      });
      document.querySelectorAll('.comment-class, .comment-method').forEach(el => {
        el.style.display = 'block';
      });
    });
  }
}

function showClassComments(className) {
  // Remove all highlights
  document.querySelectorAll('.ast-class, .ast-method').forEach(el => {
    el.classList.remove('active');
  });
  
  // Highlight clicked class
  const classNode = document.querySelector(`.ast-class[data-class="${className}"]`);
  if (classNode) {
    classNode.classList.add('active');
    classNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
  
  // Filter comments
  document.querySelectorAll('.comment-class, .comment-method').forEach(el => {
    el.style.display = el.id === `class_${className}` ? 'block' : 'none';
  });
}

function showMethodComments(className, methodName) {
  // Remove all highlights
  document.querySelectorAll('.ast-class, .ast-method').forEach(el => {
    el.classList.remove('active');
  });
  
  // Highlight parent class and method
  const classNode = document.querySelector(`.ast-class[data-class="${className}"]`);
  const methodNode = document.querySelector(`.ast-method[data-class="${className}"][data-method="${methodName}"]`);
  
  if (classNode) classNode.classList.add('active-parent');
  if (methodNode) {
    methodNode.classList.add('active');
    methodNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
  
  // Filter comments
  document.querySelectorAll('.comment-class, .comment-method').forEach(el => {
    el.style.display = el.id === `method_${className}_${methodName}` ? 'block' : 'none';
  });
}

// Dashboard functionality
function initDashboard() {
  let monacoLoaded = false;
  let monacoEditor = null;
  let submissionToDelete = null;
  const deleteModal = document.getElementById('deleteConfirmModal') ? 
                     new bootstrap.Modal(document.getElementById('deleteConfirmModal')) : null;
  
  // View switching functionality
  function setupViewSwitcher() {
    const viewButtons = document.querySelectorAll('[data-view]');
    const views = {
      code: document.getElementById('previewEditor'),
      ast: document.getElementById('previewAST'),
      comments: document.getElementById('previewComments')
    };

    viewButtons.forEach(button => {
      button.addEventListener('click', function() {
        // Toggle active state
        viewButtons.forEach(b => b.classList.remove('active'));
        this.classList.add('active');

        // Hide all views
        Object.values(views).forEach(view => {
          if (view) view.style.display = 'none';
        });

        // Show selected view
        const viewType = this.dataset.view;
        if (views[viewType]) {
          views[viewType].style.display = 'block';
        }

        // Trigger layout update for Monaco when switching back to code
        if (viewType === 'code' && monacoEditor) {
          monacoEditor.layout();
        }
      });
    });
  }

  // Load Monaco editor once
  function loadMonaco() {
    return new Promise((resolve, reject) => {
      if (monacoLoaded) {
        resolve();
        return;
      }
      
      require(['vs/editor/editor.main'], function() {
        monacoLoaded = true;
        resolve();
      }, function(error) {
        console.error('Monaco load error:', error);
        reject(error);
      });
    });
  }

  // Load submission into preview
  async function loadSubmissionPreview(submissionId) {
    const previewContainer = document.getElementById('previewEditor');
    const astContainer = document.getElementById('previewAST');
    const commentsContainer = document.getElementById('previewComments');
    const loadingOverlay = document.getElementById('previewLoading');
    const errorContainer = document.getElementById('previewError');
    
    // Reset error state and containers
    errorContainer.classList.add('d-none');
    astContainer.innerHTML = '';
    commentsContainer.innerHTML = '';
    
    // Show loading indicator
    loadingOverlay.classList.remove('d-none');
    
    try {
      // Load Monaco if not already loaded
      await loadMonaco();
      
      // Fetch submission data
      const response = await fetch(`/get-submission/${submissionId}`);
      
      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }
      
      const data = await response.json();
      
      // Update all content containers
      if (monacoEditor) {
        monacoEditor.setValue(data.code_content);
      } else {
        // Make sure container is visible for proper initialization
        previewContainer.style.display = 'block';
        
        // Change background color in editor theme
        monaco.editor.defineTheme('customTheme', {
          base: 'vs', // or 'vs-dark'
          inherit: true,
          rules: [],
          colors: {
            'editor.background': '#ffffff', 
          }
        });

        monacoEditor = monaco.editor.create(previewContainer, {
          value: data.code_content,
          language: 'java',
          theme: 'customTheme',
          readOnly: true,
          fontSize: 14,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          automaticLayout: true
        });
        
        // Handle resize
        window.addEventListener('resize', function() {
          if (monacoEditor) {
            monacoEditor.layout();
          }
        });
      }
      
      // Populate AST and comments
      astContainer.innerHTML = data.ast_content || 'No AST available';
      commentsContainer.innerHTML = data.comments_content || 'No comments available';
      
      // Default to code view
      document.querySelector('[data-view="code"]').click();
      
    } catch (error) {
      console.error('Preview error:', error);
      errorContainer.textContent = `Error loading preview: ${error.message}`;
      errorContainer.classList.remove('d-none');
    } finally {
      loadingOverlay.classList.add('d-none');
      
      // Trigger layout after content load
      if (monacoEditor) monacoEditor.layout();
    }
  }

  // Function to handle submission deletion
  function deleteSubmission(submissionId) {
    fetch(`/delete-submission/${submissionId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('Failed to delete submission');
      }
      return response.json();
    })
    .then(data => {
      if (data.status === 'success') {
        // Remove the submission item from DOM
        const itemToRemove = document.querySelector(`.submission-item[data-id="${submissionId}"]`);
        if (itemToRemove) {
          itemToRemove.remove();
        }
        
        // Clear the preview if we just deleted the displayed submission
        if (monacoEditor) {
          monacoEditor.setValue('');
          document.getElementById('previewAST').innerHTML = '';
          document.getElementById('previewComments').innerHTML = '';
        }
      } else {
        alert('Error deleting submission: ' + (data.message || 'Unknown error'));
      }
    })
    .catch(error => {
      console.error('Error:', error);
      alert('Error deleting submission: ' + error.message);
    });
  }

  // Function to rename submission
  function renameSubmission(submissionId, newName) {
    fetch(`/rename-submission/${submissionId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ new_name: newName })
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('Failed to rename submission');
      }
      return response.json();
    })
    .then(data => {
      if (data.status !== 'success') {
        throw new Error(data.message || 'Unknown error');
      }
    })
    .catch(error => {
      console.error('Error:', error);
      alert('Error renaming submission: ' + error.message);
    });
  }

  // Add event listeners for edit name buttons
  document.querySelectorAll('.edit-name-btn').forEach(btn => {
    btn.addEventListener('click', function() {
      const submissionItem = this.closest('.submission-item');
      const nameDisplay = submissionItem.querySelector('.submission-name-display');
      const nameInput = submissionItem.querySelector('.submission-name');
      
      // Toggle display/edit mode
      if (nameDisplay.classList.contains('d-none')) {
        // Save mode - update name
        nameDisplay.textContent = nameInput.value;
        nameDisplay.classList.remove('d-none');
        nameInput.classList.add('d-none');
        
        // Only rename if value changed
        if (nameInput.value !== nameInput.dataset.original) {
          renameSubmission(submissionItem.dataset.id, nameInput.value);
          nameInput.dataset.original = nameInput.value;
        }
      } else {
        // Edit mode
        nameDisplay.classList.add('d-none');
        nameInput.classList.remove('d-none');
        nameInput.focus();
        nameInput.select();
      }
    });
  });
  
  // Handle input blur for name editing
  document.querySelectorAll('.submission-name').forEach(input => {
    input.addEventListener('blur', function() {
      const submissionItem = this.closest('.submission-item');
      const nameDisplay = submissionItem.querySelector('.submission-name-display');
      
      // Save on blur
      if (!nameDisplay.classList.contains('d-none')) return;
      
      nameDisplay.textContent = this.value;
      nameDisplay.classList.remove('d-none');
      this.classList.add('d-none');
      
      // Only rename if value changed
      if (this.value !== this.dataset.original) {
        renameSubmission(submissionItem.dataset.id, this.value);
        this.dataset.original = this.value;
      }
    });
    
    // Handle Enter key press
    input.addEventListener('keypress', function(e) {
      if (e.key === 'Enter') {
        this.blur();
      }
    });
  });

  // Add event listeners for preview buttons
  document.querySelectorAll('.preview-btn').forEach(btn => {
    btn.addEventListener('click', function() {
      const submissionId = this.closest('.submission-item').dataset.id;
      loadSubmissionPreview(submissionId);
    });
  });

  // Add event listeners for delete buttons
  document.querySelectorAll('.delete-btn').forEach(btn => {
    btn.addEventListener('click', function() {
      submissionToDelete = this.closest('.submission-item').dataset.id;
      if (deleteModal) {
        deleteModal.show();
      }
    });
  });

  // Add event listener for confirm delete button in modal
  const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
  if (confirmDeleteBtn) {
    confirmDeleteBtn.addEventListener('click', function() {
      if (submissionToDelete) {
        deleteSubmission(submissionToDelete);
        if (deleteModal) {
          deleteModal.hide();
        }
      }
    });
  }

  // Initialize view switcher
  setupViewSwitcher();
}


//function for CFG
// CFG generation and zoom/pan functionality
let currentScale = 1.0;
const scaleStep = 0.2;
const minScale = 0.5;
const maxScale = 3.0;
let isDragging = false;
let startX, startY, translateX = 0, translateY = 0;

async function generateCFG() {
    console.log("Generate CFG button clicked");
    
    const code = window.editor ? window.editor.getValue() : '';
    if (!code.trim()) {
        alert('Please submit code first before generating CFG');
        return;
    }

    const cfgBtn = document.getElementById('generateCfgBtn');
    const cfgLoading = document.getElementById('cfgLoading');
    const cfgImage = document.getElementById('cfgImage');

    try {
        // Disable button and show loading
        cfgBtn.disabled = true;
        cfgLoading.style.display = 'flex';
        cfgImage.src = '';
        cfgImage.alt = 'Generating CFG...';

        const response = await fetch('/generate-cfg', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ code: code })
        });

        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || "Failed to generate CFG");
        }

        // Display CFG image
        cfgImage.src = `${data.image_url}?t=${Date.now()}`;
        cfgImage.alt = 'Control Flow Graph';
        
        // Reset zoom and pan when new image loads
        cfgImage.onload = function() {
            resetZoom();
        };
        
    } catch (error) {
        console.error("CFG Error:", error);
        const cfgContainer = document.getElementById('cfgContainer');
        cfgContainer.innerHTML = `
            <div class="card-body text-center text-danger">
                <p>Error: ${error.message}</p>
            </div>
        `;
    } finally {
        cfgLoading.style.display = 'none';
        cfgBtn.disabled = false;
    }
}

// Zoom functions
function updateZoom() {
    const wrapper = document.getElementById('cfgImageWrapper');
    if (wrapper) {
        wrapper.style.transform = `translate(${translateX}px, ${translateY}px) scale(${currentScale})`;
    }
}

function zoomIn() {
    if (currentScale < maxScale) {
        currentScale += scaleStep;
        updateZoom();
    }
}

function zoomOut() {
    if (currentScale > minScale) {
        currentScale -= scaleStep;
        updateZoom();
    }
}

function resetZoom() {
    currentScale = 1.0;
    translateX = 0;
    translateY = 0;
    updateZoom();
    
    // Center the image
    const container = document.getElementById('cfgContainer');
    const wrapper = document.getElementById('cfgImageWrapper');
    const img = document.getElementById('cfgImage');
    
    if (container && wrapper && img) {
        const containerWidth = container.clientWidth;
        const containerHeight = container.clientHeight;
        const imgWidth = img.naturalWidth;
        const imgHeight = img.naturalHeight;
        
        // Center if image is smaller than container
        if (imgWidth < containerWidth && imgHeight < containerHeight) {
            translateX = (containerWidth - imgWidth) / 2;
            translateY = (containerHeight - imgHeight) / 2;
            updateZoom();
        }
    }
}

// Pan functionality
function setupPanning() {
    const container = document.getElementById('cfgContainer');
    const wrapper = document.getElementById('cfgImageWrapper');
    
    if (!container || !wrapper) return;
    
    container.addEventListener('mousedown', function(e) {
        if (e.button !== 0) return; // Only left mouse button
        isDragging = true;
        startX = e.clientX - translateX;
        startY = e.clientY - translateY;
        container.style.cursor = 'grabbing';
    });
    
    document.addEventListener('mousemove', function(e) {
        if (!isDragging) return;
        e.preventDefault();
        
        translateX = e.clientX - startX;
        translateY = e.clientY - startY;
        updateZoom();
    });
    
    document.addEventListener('mouseup', function() {
        isDragging = false;
        container.style.cursor = 'grab';
    });
    
    container.addEventListener('wheel', function(e) {
        if (e.ctrlKey) {  // Only zoom when Ctrl key is pressed
            e.preventDefault();
            if (e.deltaY < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        }
    });
}


function handleCFGError(error) {
    const cfgContainer = document.getElementById('cfg-container');
    if (cfgContainer) {
        cfgContainer.innerHTML = `<div class="alert alert-danger">${error.message}</div>`;
    }
    console.error('CFG Error:', error);
}