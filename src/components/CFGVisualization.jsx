import React, { useState, useRef, useEffect } from 'react';
import './CFGVisualization.css';

const CFGVisualization = ({ code, editorRef, theme, isLoading, setIsLoading }) => {
  const [cfgSvg, setCfgSvg] = useState(null);
  const [currentScale, setCurrentScale] = useState(1.0);
  const [translateX, setTranslateX] = useState(0);
  const [translateY, setTranslateY] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  const [isFullscreen, setIsFullscreen] = useState(false);
  const containerRef = useRef(null);
  const wrapperRef = useRef(null);
  const imageRef = useRef(null);

  const scaleStep = 0.2;
  const minScale = 0.5;
  const maxScale = 3.0;

  const generateCFG = async () => {
    const codeToUse = editorRef.current?.getValue() || code;
    if (!codeToUse.trim()) {
      alert('Please submit code first before generating CFG');
      return;
    }

    setIsLoading(true);
    setCfgSvg(null);

    try {
      const response = await fetch('/generate-cfg', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'image/svg+xml',
        },
        credentials: 'include',
        body: JSON.stringify({ code: codeToUse, theme: theme }),
      });

      if (!response.ok) {
        throw new Error(await response.text());
      }

      let svgText = await response.text();
      
      // Apply dark theme to SVG if needed
      if (theme === 'dark') {
        svgText = applyDarkThemeToSVG(svgText);
      }

      setCfgSvg(svgText);
      resetZoom();
    } catch (error) {
      console.error('CFG Error:', error);
      setCfgSvg(`<div style="color: red; padding: 20px;">Error: ${error.message}</div>`);
    } finally {
      setIsLoading(false);
    }
  };

  const applyDarkThemeToSVG = (svgText) => {
    // Parse SVG and apply dark theme
    const parser = new DOMParser();
    const svgDoc = parser.parseFromString(svgText, 'image/svg+xml');
    const svgElement = svgDoc.documentElement;

    // Set dark background
    svgElement.style.backgroundColor = 'var(--cfg-bg)';
    
    // Update node colors for dark theme
    const nodes = svgElement.querySelectorAll('g.node');
    nodes.forEach(node => {
      const rect = node.querySelector('rect, polygon');
      if (rect) {
        const currentFill = rect.getAttribute('fill');
        // Keep method-specific colors but adjust brightness for dark theme
        if (currentFill && currentFill !== '#e0f7fa') {
          // Keep original color but make it darker
          rect.setAttribute('fill', adjustColorForDarkTheme(currentFill));
        } else {
          rect.setAttribute('fill', '#2d2d2d'); // Dark background for default nodes
        }
        rect.setAttribute('stroke', '#5a9fd4'); // Light stroke for dark theme
      }
      const text = node.querySelector('text');
      if (text) {
        text.setAttribute('fill', '#e0e0e0'); // Light text for dark theme
      }
    });

    // Update edge colors
    const edges = svgElement.querySelectorAll('g.edge path, g.edge polygon');
    edges.forEach(edge => {
      edge.setAttribute('stroke', '#8b8b8b'); // Lighter edges for dark theme
      if (edge.tagName === 'polygon') {
        edge.setAttribute('fill', '#8b8b8b');
      }
    });

    return new XMLSerializer().serializeToString(svgElement);
  };

  const adjustColorForDarkTheme = (color) => {
    // Simple color adjustment - make colors darker but keep hue
    // This is a basic implementation; you might want more sophisticated color manipulation
    if (color.startsWith('#')) {
      // Convert hex to RGB, darken, convert back
      const r = parseInt(color.slice(1, 3), 16);
      const g = parseInt(color.slice(3, 5), 16);
      const b = parseInt(color.slice(5, 7), 16);
      
      // Darken by 40%
      const newR = Math.max(0, Math.floor(r * 0.6));
      const newG = Math.max(0, Math.floor(g * 0.6));
      const newB = Math.max(0, Math.floor(b * 0.6));
      
      return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
    }
    return color;
  };

  const zoomIn = () => {
    if (currentScale < maxScale) {
      setCurrentScale(prev => Math.min(maxScale, prev + scaleStep));
    }
  };

  const zoomOut = () => {
    if (currentScale > minScale) {
      setCurrentScale(prev => Math.max(minScale, prev - scaleStep));
    }
  };

  const resetZoom = () => {
    setCurrentScale(1.0);
    setTranslateX(0);
    setTranslateY(0);
    
    // Center the image
    if (containerRef.current && imageRef.current) {
      const container = containerRef.current;
      const img = imageRef.current;
      const containerWidth = container.clientWidth;
      const containerHeight = container.clientHeight;
      const imgWidth = img.naturalWidth || img.clientWidth;
      const imgHeight = img.naturalHeight || img.clientHeight;
      
      if (imgWidth < containerWidth && imgHeight < containerHeight) {
        setTranslateX((containerWidth - imgWidth) / 2);
        setTranslateY((containerHeight - imgHeight) / 2);
      }
    }
  };

  useEffect(() => {
    if (wrapperRef.current) {
      wrapperRef.current.style.transform = `translate(${translateX}px, ${translateY}px) scale(${currentScale})`;
    }
  }, [currentScale, translateX, translateY]);

  // Pan functionality
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleMouseDown = (e) => {
      if (e.button !== 0) return;
      e.preventDefault(); // Prevent text selection
      setIsDragging(true);
      setStartPos({ x: e.clientX - translateX, y: e.clientY - translateY });
      container.style.cursor = 'grabbing';
    };

    const handleSelectStart = (e) => {
      // Prevent text selection in CFG container area
      if (container && container.contains(e.target)) {
        e.preventDefault();
      }
    };

    const handleDragStart = (e) => {
      // Prevent drag of selected text
      if (container && container.contains(e.target)) {
        e.preventDefault();
      }
    };

    const handleMouseMove = (e) => {
      if (!isDragging) return;
      e.preventDefault();
      setTranslateX(e.clientX - startPos.x);
      setTranslateY(e.clientY - startPos.y);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
      if (container) container.style.cursor = 'grab';
    };

    const handleWheel = (e) => {
      if (e.ctrlKey) {
        e.preventDefault();
        if (e.deltaY < 0) {
          zoomIn();
        } else {
          zoomOut();
        }
      }
    };

    container.addEventListener('mousedown', handleMouseDown);
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    container.addEventListener('wheel', handleWheel);
    document.addEventListener('selectstart', handleSelectStart);
    document.addEventListener('dragstart', handleDragStart);

    return () => {
      container.removeEventListener('mousedown', handleMouseDown);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      container.removeEventListener('wheel', handleWheel);
      document.removeEventListener('selectstart', handleSelectStart);
      document.removeEventListener('dragstart', handleDragStart);
    };
  }, [isDragging, startPos, translateX, translateY]);

  // Re-apply dark theme when theme changes
  useEffect(() => {
    if (cfgSvg && theme === 'dark') {
      const updatedSvg = applyDarkThemeToSVG(cfgSvg);
      setCfgSvg(updatedSvg);
    }
  }, [theme]);

  const downloadCFG = () => {
    if (!cfgSvg) return;
    
    const blob = new Blob([cfgSvg], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `cfg-${new Date().toISOString()}.svg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const toggleFullscreen = () => {
    const container = containerRef.current;
    if (!container) return;

    if (!isFullscreen) {
      if (container.requestFullscreen) {
        container.requestFullscreen();
      } else if (container.webkitRequestFullscreen) {
        container.webkitRequestFullscreen();
      } else if (container.msRequestFullscreen) {
        container.msRequestFullscreen();
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      } else if (document.msExitFullscreen) {
        document.msExitFullscreen();
      }
    }
  };

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement || !!document.webkitFullscreenElement || !!document.msFullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('msfullscreenchange', handleFullscreenChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.removeEventListener('msfullscreenchange', handleFullscreenChange);
    };
  }, []);

  return (
    <div className="cfg-section col-lg-12 d-flex flex-column position-relative">
      <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
        <label className="form-label mb-0">Control Flow Graph</label>
      </div>
      <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
        <button
          id="generateCfgBtn"
          className="btn btn-sm clarifai-btn"
          onClick={generateCFG}
          disabled={isLoading}
        >
          Generate CFG
        </button>
        <div className="d-flex gap-2 flex-wrap align-items-center">
          <button
            id="downloadCfgBtn"
            className="btn btn-sm clarifai-btn"
            onClick={downloadCFG}
            disabled={!cfgSvg}
          >
            <i className="bi bi-download me-1"></i>Download CFG
          </button>
          <div className="btn-group" role="group">
            <button className="zoom-btn btn btn-sm btn-outline-secondary" onClick={zoomIn}>
              <i className="bi bi-zoom-in"></i>
            </button>
            <button className="zoom-btn btn btn-sm btn-outline-secondary" onClick={zoomOut}>
              <i className="bi bi-zoom-out"></i>
            </button>
            <button className="zoom-btn btn btn-sm btn-outline-secondary" onClick={resetZoom}>
              <i className="bi bi-zoom-reset"></i> Reset
            </button>
            <button 
              className="zoom-btn btn btn-sm btn-outline-secondary" 
              onClick={toggleFullscreen}
              title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
            >
              <i className={`bi ${isFullscreen ? 'bi-fullscreen-exit' : 'bi-fullscreen'}`}></i>
            </button>
          </div>
        </div>
      </div>
      {isLoading && (
        <div className="loading-overlay">
          <div className="spinner-border text-primary" role="status"></div>
          <span className="loading-text">Generating CFG...</span>
        </div>
      )}
      <div
        id="cfgContainer"
        ref={containerRef}
        className="form-control cfg-container"
        style={{
          height: '600px',
          overflow: 'auto',
          position: 'relative',
          border: '1px solid var(--border-color)',
          backgroundColor: theme === 'dark' ? 'var(--cfg-bg)' : '#ffffff',
          cursor: 'grab',
        }}
      >
        {cfgSvg && (
          <div
            id="cfgImageWrapper"
            ref={wrapperRef}
            style={{
              position: 'absolute',
              transformOrigin: '0 0',
            }}
            dangerouslySetInnerHTML={{ __html: cfgSvg }}
          />
        )}
        {!cfgSvg && !isLoading && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
            }}
          >
            <p>Click "Generate CFG" to create a control flow graph</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default CFGVisualization;

