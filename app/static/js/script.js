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

// script.js
// Add after the initEditor function
let astData = null;
let isGraphicalView = false;

// Add inside initEditor function
document.getElementById('switchViewBtn').addEventListener('click', toggleAstView);

function toggleAstView() {
    isGraphicalView = !isGraphicalView;
    renderAst();
}

function renderAst() {
    const container = document.getElementById('astTreeContainer');
    const textOutput = document.getElementById('astOutput');
    
    if (isGraphicalView) {
        textOutput.style.display = 'none';
        container.style.display = 'block';
        renderAstTree();
    } else {
        container.style.display = 'none';
        textOutput.style.display = 'block';
    }
}

async function renderAstTree() {
    const container = document.getElementById('astTreeContainer');
    container.innerHTML = '<div class="text-center mt-5"><div class="spinner-border" role="status"></div></div>';
    
    try {
        const code = window.editor.getValue();
        const response = await fetch('/ast-json', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ code: code })
        });
        
        const data = await response.json();
        astData = data;
        
        if (data.error) {
            container.innerHTML = `<div class="alert alert-danger">${data.error}</div>`;
            return;
        }
        
        drawTree(container, data);
    } catch (error) {
        container.innerHTML = `<div class="alert alert-danger">Error loading AST: ${error.message}</div>`;
    }
}

function drawTree(container, rootNode) {
  // --- capture existing transform (if any) before we clear the container ---
  const prevSvgSelection = d3.select(container).select('svg');
  let prevTransform = null;
  if (prevSvgSelection.node()) {
    try {
      prevTransform = d3.zoomTransform(prevSvgSelection.node());
    } catch (e) {
      prevTransform = null;
    }
  }

  // clear container
  container.innerHTML = '';

  // ---- dimensions ----
  const width = container.clientWidth || 1000;
  const height = container.clientHeight || 700;
  const margin = { top: 20, right: 40, bottom: 20, left: 40 };

  // ---- create svg + main group ----
  const svg = d3.select(container)
    .append('svg')
    .attr('width', width)
    .attr('height', height);

  const g = svg.append('g')
    .attr('transform', `translate(${margin.left},${margin.top})`);

  // ---- text measurement helper ----
  function measureTextWidth(text = '', fontSize = 24, fontFamily = 'sans-serif') {
    const ctx = measureTextWidth._ctx || (measureTextWidth._ctx = document.createElement('canvas').getContext('2d'));
    ctx.font = `${fontSize}px ${fontFamily}`;
    return ctx.measureText(text).width;
  }

  // ---- tunables: bigger text + padding ----
  const fontSize = 24;               // readable text size
  const horizontalPadding = 14;      // px padding left+right inside rect
  const nodeRectHeight = 44;         // rect visual height
  const nodeVerticalSpacing = 90;    // vertical spacing between rows (d.x difference)
  const minColumnWidth = 90;        // minimum horizontal slot width (column)

  // ---- collect labels and compute widest label ----
  const labels = [];
  (function collect(n){
    if (!n) return;
    labels.push(n.name || '');
    if (n.children) n.children.forEach(collect);
    if (n._children) n._children.forEach(collect);
  })(rootNode);

  const maxLabelWidth = labels.length ? Math.max(...labels.map(l => measureTextWidth(l, fontSize))) : 40;

  // horizontal slot (column) width determined by widest label + padding, but clamped to minColumnWidth
  const nodeHorizontalSpacing = Math.max(minColumnWidth, Math.ceil(maxLabelWidth + 2 * horizontalPadding + 28));

  // ---- tree layout: nodeSize([vertical, horizontal]) for horizontal (depth -> y) orientation ----
  const baseSibling = 1.0;
  const baseNonSibling = 1.6;

  const treeLayout = d3.tree()
    .nodeSize([nodeVerticalSpacing, nodeHorizontalSpacing])
    .separation((a, b) => {
      const base = (a.parent === b.parent) ? baseSibling : baseNonSibling;
      const wa = measureTextWidth(a.data?.name || '', fontSize);
      const wb = measureTextWidth(b.data?.name || '', fontSize);
      const requiredCols = (wa + wb + 4 * horizontalPadding) / nodeHorizontalSpacing;
      const extra = Math.max(1, requiredCols);
      return base * extra;
    });

  // ---- build hierarchy and run layout ----
  const root = d3.hierarchy(rootNode);
  treeLayout(root);

  // ---- link generator (horizontal layout: x -> y swap) ----
  const linkGen = d3.linkHorizontal()
    .x(d => d.y)
    .y(d => d.x);

  // ---- draw links ----
  g.append('g').attr('class', 'links')
    .selectAll('path.link')
    .data(root.links())
    .join('path')
    .attr('class', 'link')
    .attr('d', linkGen)
    .attr('fill', 'none')
    .attr('stroke-width', 1.6)
    .attr('stroke', '#6b7280');

  // ---- draw nodes ----
  const nodeG = g.append('g').attr('class', 'nodes')
    .selectAll('g.node')
    .data(root.descendants())
    .join('g')
    .attr('class', d => `node ${d.data.type ? d.data.type : ''}`)
    .attr('transform', d => `translate(${d.y},${d.x})`)
    .style('cursor', 'pointer')
    .on('click', (event, d) => {
      // toggle by mutating original data (so collapse persists)
      if (d.data.children) {
        d.data._children = d.data.children;
        d.data.children = null;
      } else if (d.data._children) {
        d.data.children = d.data._children;
        d.data._children = null;
      }
      // redraw (preserve view because we captured prevTransform at function start)
      drawTree(container, rootNode);
    });

  // rect sized by measured text width
  nodeG.append('rect')
    .attr('class', 'node-rect')
    .attr('x', d => {
      const w = Math.max(80, Math.ceil(measureTextWidth(d.data.name || '', fontSize) + 2 * horizontalPadding));
      return -w / 2;
    })
    .attr('y', -nodeRectHeight / 2)
    .attr('width', d => Math.max(80, Math.ceil(measureTextWidth(d.data.name || '', fontSize) + 2 * horizontalPadding)))
    .attr('height', nodeRectHeight)
    .attr('rx', 8)
    .attr('ry', 8)
    .attr('fill', '#ccccccff')
    .attr('stroke', '#2b6cb0')
    .attr('stroke-width', 1.4);

  // centered text using the same fontSize
  nodeG.append('text')
    .attr('class', 'ast-node-text')
    .attr('dy', '0.25em')
    .attr('text-anchor', 'middle')
    .style('font-size', fontSize + 'px')
    .style('pointer-events', 'none')
    .text(d => d.data.name);

  // small toggle indicator for nodes with children/_children
  nodeG.filter(d => d.data.children || d.data._children)
    .append('path')
    .attr('class', 'node-toggle')
    .attr('d', d => d.data.children ? 'M -6 8 L 0 2 L 6 8 Z' : 'M -3 -6 L 3 0 L -3 6 Z')
    .attr('transform', d => {
      const textW = Math.ceil(measureTextWidth(d.data.name || '', fontSize));
      return `translate(${Math.max(18, Math.ceil(textW / 2) + horizontalPadding)}, -6)`;
    })
    .style('fill', '#0077ffff');

  // ---- zoom/pan behaviour (applied to svg) ----
  const zoom = d3.zoom()
    .scaleExtent([0.1, 4])
    .on('zoom', (event) => {
      g.attr('transform', event.transform);
    });

  svg.call(zoom);

  // ---- restore previous transform if present, otherwise initial centering once ----
  if (prevTransform) {
    // reapply the same transform so view doesn't jump
    svg.call(zoom.transform, prevTransform);
  } else if (!drawTree._initialized) {
    // initial one-time transform so tree starts near left and vertically centered
    const allX = root.descendants().map(d => d.x);
    const allY = root.descendants().map(d => d.y);
    const minX = Math.min(...allX), maxX = Math.max(...allX);
    const minY = Math.min(...allY), maxY = Math.max(...allY);

    const treeWidth = maxY - minY + nodeHorizontalSpacing;
    const treeHeight = maxX - minX + nodeVerticalSpacing;

    const tx = margin.left + 20 - minY;
    const ty = margin.top + (height - treeHeight) / 2 - minX;

    svg.call(zoom.transform, d3.zoomIdentity.translate(tx, ty).scale(1));
    drawTree._initialized = true;
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
                'Accept': 'image/svg+xml'
            },
            body: JSON.stringify({ code: code })
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        // Get SVG blob and create object URL
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        
        // Display CFG image
        cfgImage.src = url;
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

// Global variable to store the file structure
let fileStructure = {};

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
  // ... existing initialization code ...
  
  // Add event listeners for folder upload and sidebar
  if (document.getElementById('folderUpload')) {
    document.getElementById('folderUpload').addEventListener('change', handleFolderUpload);
  }
  
  if (document.getElementById('toggleSidebar')) {
    document.getElementById('toggleSidebar').addEventListener('click', toggleSidebar);
  }
  
  if (document.getElementById('closeSidebar')) {
    document.getElementById('closeSidebar').addEventListener('click', toggleSidebar);
  }
  
  if (document.getElementById('refreshFileTree')) {
    document.getElementById('refreshFileTree').addEventListener('click', refreshFileTree);
  }
});

// Handle folder upload
function handleFolderUpload(e) {
  const files = e.target.files;
  if (!files.length) return;
  
  // Reset file structure
  fileStructure = {};
  
  // Process each file
  Array.from(files).forEach(file => {
    if (file.name.endsWith('.java')) {
      addFileToStructure(file.webkitRelativePath || file.name, file);
    }
  });
  
  // Display file tree in sidebar
  renderFileTree();
  
  // Show the sidebar
  document.getElementById('fileSidebar').style.display = 'block';
  
  // Read the first Java file and load it into the editor
  const firstJavaFile = findFirstJavaFile(fileStructure);
  if (firstJavaFile) {
    readFileContent(firstJavaFile.file);
  }
}

// Add file to the hierarchical structure
function addFileToStructure(path, file) {
  const parts = path.split('/');
  let currentLevel = fileStructure;
  
  for (let i = 0; i < parts.length - 1; i++) {
    const part = parts[i];
    if (!currentLevel[part]) {
      currentLevel[part] = { _type: 'folder' };
    }
    currentLevel = currentLevel[part];
  }
  
  const fileName = parts[parts.length - 1];
  currentLevel[fileName] = { 
    _type: 'file', 
    file: file,
    path: path
  };
}

// Find the first Java file in the structure
function findFirstJavaFile(structure) {
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
}

// Render the file tree in the sidebar
function renderFileTree() {
  const fileTreeElement = document.getElementById('fileTree');
  fileTreeElement.innerHTML = '';
  
  if (Object.keys(fileStructure).length === 0) {
    fileTreeElement.innerHTML = '<p class="text-muted p-2">No files uploaded</p>';
    return;
  }
  
  const tree = buildFileTreeHTML(fileStructure);
  fileTreeElement.appendChild(tree);
}

// Build HTML for the file tree
function buildFileTreeHTML(structure, level = 0) {
  const ul = document.createElement('ul');
  ul.style.paddingLeft = level > 0 ? '16px' : '0';
  
  for (const key in structure) {
    if (key === '_type') continue;
    
    const li = document.createElement('li');
    const item = structure[key];
    
    if (item._type === 'folder') {
      li.innerHTML = `
        <div class="folder" data-name="${key}">
          <span class="folder-name">${key}</span>
        </div>
      `;
      
      // Add click event to toggle folder
      li.querySelector('.folder').addEventListener('click', function(e) {
        e.stopPropagation();
        this.classList.toggle('expanded');
        const children = this.nextElementSibling;
        if (children) {
          children.style.display = children.style.display === 'none' ? 'block' : 'none';
        }
      });
      
      // Create children container (initially hidden)
      const childrenContainer = buildFileTreeHTML(item, level + 1);
      childrenContainer.style.display = 'none';
      li.appendChild(childrenContainer);
      
    } else if (item._type === 'file' && key.endsWith('.java')) {
      li.innerHTML = `
        <div class="file java" data-path="${item.path}">
          ${key}
        </div>
      `;
      
      // Add click event to load file content
      li.querySelector('.file').addEventListener('click', function() {
        readFileContent(item.file);
        
        // Highlight selected file
        document.querySelectorAll('.file-tree .file').forEach(el => {
          el.classList.remove('selected');
        });
        this.classList.add('selected');
      });
    }
    
    ul.appendChild(li);
  }
  
  return ul;
}

// Read file content and set it in the editor
function readFileContent(file) {
  const reader = new FileReader();
  reader.onload = function(e) {
    const contents = e.target.result;
    if (window.editor) {
      window.editor.setValue(contents);
      document.getElementById('hiddenCode').value = contents;
    }
  };
  reader.readAsText(file);
}

// Toggle sidebar visibility
function toggleSidebar() {
  const sidebar = document.getElementById('fileSidebar');
  if (sidebar.style.display === 'none') {
    sidebar.style.display = 'block';
  } else {
    sidebar.style.display = 'none';
  }
}

// Refresh file tree
function refreshFileTree() {
  renderFileTree();
}