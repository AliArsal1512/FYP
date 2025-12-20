import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import './ASTVisualization.css';

const ASTVisualization = ({ astData, theme }) => {
  const containerRef = useRef(null);
  const wrapperRef = useRef(null);
  const svgRef = useRef(null);
  const zoomScaleRef = useRef(1);
  const savedViewportCenterRef = useRef(null);
  const animatingNodeRef = useRef(null);
  const isInitialRenderRef = useRef(true);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [updateTrigger, setUpdateTrigger] = useState(0);

  useEffect(() => {
    if (!astData || !containerRef.current) {
      // Clear container if no data
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
      // Reset initial render flag when data is cleared (new submission)
      isInitialRenderRef.current = true;
      return;
    }

    const container = containerRef.current;
    
    // Save current transform before clearing (for expand/collapse position preservation)
    if (svgRef.current) {
      const currentTransform = d3.zoomTransform(svgRef.current);
      savedViewportCenterRef.current = {
        transform: currentTransform,
        scale: currentTransform.k
      };
    }
    
    container.innerHTML = '';

    const width = container.clientWidth || 1000;
    const height = container.clientHeight || 600;
    const margin = { top: 20, right: 40, bottom: 20, left: 40 };

    const svg = d3.select(container)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .style('background-color', theme === 'dark' ? 'var(--ast-bg)' : '#ffffff');

    const zoomLayer = svg.append('g').attr('class', 'ast-zoom-layer');
    const g = zoomLayer.append('g')
      .attr('class', 'ast-tree-root')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    function measureTextWidth(text = '', fontSize = 24, fontFamily = 'sans-serif') {
      const ctx = measureTextWidth._ctx || (measureTextWidth._ctx = document.createElement('canvas').getContext('2d'));
      ctx.font = `${fontSize}px ${fontFamily}`;
      return ctx.measureText(text).width;
    }

    const fontSize = 24;
    const horizontalPadding = 14;
    const nodeRectHeight = 44;
    const nodeVerticalSpacing = 90;
    const minColumnWidth = 90;

    const labels = [];
    (function collect(n) {
      if (!n) return;
      labels.push(n.name || '');
      if (n.children) n.children.forEach(collect);
      if (n._children) n._children.forEach(collect);
    })(astData);

    const maxLabelWidth = labels.length ? Math.max(...labels.map(l => measureTextWidth(l, fontSize))) : 40;
    const nodeHorizontalSpacing = Math.max(minColumnWidth, Math.ceil(maxLabelWidth + 2 * horizontalPadding + 28));

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

    const root = d3.hierarchy(astData);
    
    // Store old positions before layout (for smooth transitions on update)
    root.eachBefore(d => {
      if (d.x0 === undefined) d.x0 = d.x;
      if (d.y0 === undefined) d.y0 = d.y;
    });
    
    treeLayout(root);
    
    // Update old positions after layout
    root.eachBefore(d => {
      d.x0 = d.x;
      d.y0 = d.y;
    });

    const linkGen = d3.linkHorizontal()
      .x(d => d.y)
      .y(d => d.x);

    // Helper function to check if a node should be animated
    // - On initial render (no animatingNodeRef): animate everything
    // - On expand/collapse (animatingNodeRef is set): only animate the clicked node and its descendants
    // - Otherwise: don't animate
    const shouldAnimateNode = (hierarchyNode) => {
      // Individual node expand/collapse: only animate affected subtree (takes precedence)
      if (animatingNodeRef.current) {
        // Traverse up the parent chain to see if this node is a descendant of the clicked node
        let current = hierarchyNode;
        while (current) {
          if (current.data === animatingNodeRef.current) {
            return true;
          }
          current = current.parent;
        }
        return false;
      }
      
      // Initial render: animate everything (only when no animatingNodeRef is set)
      if (isInitialRenderRef.current) return true;
      
      // Other updates: no animation
      return false;
    };

    // Draw links with selective animation
    const link = g.append('g').attr('class', 'links')
      .selectAll('path.link')
      .data(root.links(), d => `${d.source.data.name}-${d.target.data.name}`);

    // Enter: new links fade in (only if connected to animating node)
    const linkEnter = link.enter()
      .append('path')
      .attr('class', 'link')
      .attr('d', d => {
        const o = { x: d.source.x0 || d.source.x, y: d.source.y0 || d.source.y };
        return linkGen({ source: o, target: o });
      })
      .attr('fill', 'none')
      .attr('stroke-width', 1.6)
      .attr('stroke', theme === 'dark' ? 'var(--ast-link-stroke)' : '#6b7280');

    // Animate only links connected to the animating node
    linkEnter
      .filter(d => shouldAnimateNode(d.source) || shouldAnimateNode(d.target))
      .attr('opacity', 0)
      .transition()
      .duration(750)
      .attr('opacity', 1)
      .attr('d', linkGen);

    // Non-animating links appear immediately
    linkEnter
      .filter(d => !shouldAnimateNode(d.source) && !shouldAnimateNode(d.target))
      .attr('opacity', 1)
      .attr('d', linkGen);

    // Update: existing links animate to new position (only if connected to animating node)
    link
      .filter(d => shouldAnimateNode(d.source) || shouldAnimateNode(d.target))
      .transition()
      .duration(750)
      .attr('d', linkGen);

    // Non-animating links update immediately
    link
      .filter(d => !shouldAnimateNode(d.source) && !shouldAnimateNode(d.target))
      .attr('d', linkGen);

    // Exit: removed links fade out (only if connected to animating node)
    link.exit()
      .filter(d => shouldAnimateNode(d.source) || shouldAnimateNode(d.target))
      .transition()
      .duration(750)
      .attr('opacity', 0)
      .attr('d', d => {
        const o = { x: d.source.x, y: d.source.y };
        return linkGen({ source: o, target: o });
      })
      .remove();

    // Non-animating links removed immediately
    link.exit()
      .filter(d => !shouldAnimateNode(d.source) && !shouldAnimateNode(d.target))
      .remove();

    // Store old positions for smooth transitions (must be before layout)
    root.eachBefore(d => {
      if (d.x0 === undefined) d.x0 = d.x;
      if (d.y0 === undefined) d.y0 = d.y;
    });

    // Draw nodes with animation
    const nodeSelection = g.append('g').attr('class', 'nodes')
      .selectAll('g.node')
      .data(root.descendants(), d => d.data.name || d.data.type || Math.random());

    // Enter: new nodes fade in and slide to position
    const nodeEnter = nodeSelection.enter()
      .append('g')
      .attr('class', d => `node ${d.data.type ? d.data.type : ''}`)
      .attr('transform', d => `translate(${d.parent ? (d.parent.y0 || d.parent.y) : (d.y0 || d.y)},${d.parent ? (d.parent.x0 || d.parent.x) : (d.x0 || d.x)})`)
      .style('cursor', 'pointer');

    // Set initial opacity for animating nodes only
    nodeEnter
      .filter(d => shouldAnimateNode(d))
      .style('opacity', 0);

    // Non-animating nodes start fully visible
    nodeEnter
      .filter(d => !shouldAnimateNode(d))
      .style('opacity', 1);

    // Update: merge enter and update, then animate selectively
    const nodeG = nodeEnter.merge(nodeSelection);
    
    // Animate only the clicked node and its descendants
    nodeG
      .filter(d => shouldAnimateNode(d))
      .transition()
      .duration(750)
      .attr('transform', d => `translate(${d.y},${d.x})`)
      .style('opacity', 1);

    // Non-animating nodes update immediately
    nodeG
      .filter(d => !shouldAnimateNode(d))
      .attr('transform', d => `translate(${d.y},${d.x})`)
      .style('opacity', 1);

    // Exit: removed nodes fade out and collapse to parent (only if animating)
    nodeSelection.exit()
      .filter(d => shouldAnimateNode(d))
      .transition()
      .duration(750)
      .attr('transform', d => `translate(${d.parent ? d.parent.y : d.y},${d.parent ? d.parent.x : d.x})`)
      .style('opacity', 0)
      .remove();

    // Non-animating nodes removed immediately
    nodeSelection.exit()
      .filter(d => !shouldAnimateNode(d))
      .remove();
    
    // Clear animating node reference after animations complete
    if (animatingNodeRef.current) {
      setTimeout(() => {
        animatingNodeRef.current = null;
      }, 750);
    }
    
    // Mark that initial render is complete
    if (isInitialRenderRef.current) {
      setTimeout(() => {
        isInitialRenderRef.current = false;
      }, 750);
    }
    
    // Add click handler to nodes
    nodeG.on('click', (event, d) => {
        // Save current transform to preserve viewport position before changing tree structure
        const currentTransform = d3.zoomTransform(svg.node());
        savedViewportCenterRef.current = {
          transform: currentTransform,
          scale: currentTransform.k
        };
        
        // Save the clicked node to animate only it and its children
        animatingNodeRef.current = d.data;
        
        if (d.data.children) {
          d.data._children = d.data.children;
          d.data.children = null;
        } else if (d.data._children) {
          d.data.children = d.data._children;
          d.data._children = null;
        }
        // Re-render
        const event2 = new Event('ast-update');
        container.dispatchEvent(event2);
      });

    // Node rectangles with theme-aware colors
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
      .attr('fill', theme === 'dark' ? 'var(--ast-node-bg)' : '#ccccccff')
      .attr('stroke', theme === 'dark' ? 'var(--ast-node-stroke)' : '#2b6cb0')
      .attr('stroke-width', 1.4);

    // Node text with theme-aware color
    nodeG.append('text')
      .attr('class', 'ast-node-text')
      .attr('dy', '0.25em')
      .attr('text-anchor', 'middle')
      .style('font-size', fontSize + 'px')
      .style('fill', theme === 'dark' ? 'var(--text-primary)' : '#000000')
      .style('pointer-events', 'none')
      .text(d => d.data.name);

    // Comment indicators
    nodeG.filter(d => d.data.comment && d.data.comment !== "No comment available")
      .append('circle')
      .attr('class', 'comment-indicator')
      .attr('r', 10)
      .attr('fill', '#ff9900')
      .attr('transform', d => {
        const textW = Math.ceil(measureTextWidth(d.data.name || '', fontSize));
        const offset = Math.max(24, Math.ceil(textW / 2) + horizontalPadding + 16);
        return `translate(${offset}, -8)`;
      })
      .style('cursor', 'pointer')
      .on('click', function(event, d) {
        event.stopPropagation();
        // Get coordinates relative to the container
        const containerRect = container.getBoundingClientRect();
        const x = event.clientX - containerRect.left;
        const y = event.clientY - containerRect.top;
        showCommentTooltip(d.data.comment, x, y);
      });

    // Zoom/pan behavior
    const zoom = d3.zoom()
      .scaleExtent([0.1, 4])
      .on('zoom', (event) => {
        zoomScaleRef.current = event.transform.k; // Store current scale
        zoomLayer.attr('transform', event.transform);
        // Update existing tooltips to new scale
        d3.selectAll('.comment-tooltip').each(function() {
          const tooltip = d3.select(this);
          const baseFontSize = 14;
          const basePadding = 8;
          const baseMaxWidth = 300;
          const scale = event.transform.k;
          tooltip.style('font-size', `${baseFontSize * scale}px`);
          tooltip.style('padding', `${basePadding * scale}px`);
          tooltip.style('max-width', `${baseMaxWidth * scale}px`);
          tooltip.style('border-radius', `${4 * scale}px`);
          tooltip.style('line-height', `${1.4 * scale}`);
          // Update close button
          tooltip.select('button')
            .style('font-size', `${16 * scale}px`)
            .style('top', `${5 * scale}px`)
            .style('right', `${5 * scale}px`);
          // Update header
          tooltip.select('.comment-tooltip-header')
            .style('padding-bottom', `${5 * scale}px`)
            .style('margin-bottom', `${5 * scale}px`)
            .style('font-size', `${baseFontSize * scale}px`);
          // Update content div - ensure it scales
          tooltip.select('.comment-tooltip-content')
            .style('font-size', `${baseFontSize * scale}px`)
            .style('line-height', `${1.4 * scale}`);
        });
      });

    svg.call(zoom);

    // Apply saved transform if available (from expand/collapse), otherwise center initially
    if (savedViewportCenterRef.current && savedViewportCenterRef.current.transform) {
      // Restore the exact transform to preserve viewport position (no repositioning)
      const savedTransform = savedViewportCenterRef.current.transform;
      // Apply transform immediately (no transition) to keep viewport stable
      svg.call(zoom.transform, savedTransform);
      zoomScaleRef.current = savedTransform.k;
      // Clear saved transform after using it
      savedViewportCenterRef.current = null;
    } else {
      // Initial centering (first render)
      const allX = root.descendants().map(d => d.x);
      const allY = root.descendants().map(d => d.y);
      const minX = Math.min(...allX), maxX = Math.max(...allX);
      const minY = Math.min(...allY), maxY = Math.max(...allY);

      const treeWidth = maxY - minY + nodeHorizontalSpacing;
      const treeHeight = maxX - minX + nodeVerticalSpacing;

      const tx = margin.left + 20 - minY;
      const ty = margin.top + (height - treeHeight) / 2 - minX;

      const initialTransform = d3.zoomIdentity.translate(tx, ty).scale(1);
      zoomScaleRef.current = initialTransform.k; // Initialize zoom scale
      svg.call(zoom.transform, initialTransform);
    }

    svgRef.current = svg.node();

    // Handle updates
    const handleUpdate = () => {
      if (astData) {
        // Force re-render by updating state
        setUpdateTrigger(prev => prev + 1);
      }
    };

    container.addEventListener('ast-update', handleUpdate);

    return () => {
      container.removeEventListener('ast-update', handleUpdate);
    };
  }, [astData, theme, isFullscreen, updateTrigger]);

  const showCommentTooltip = (comment, x, y) => {
    // Remove any existing tooltips
    d3.selectAll('.comment-tooltip').remove();
    
    // Append tooltip to the container (works in both normal and fullscreen modes)
    const container = containerRef.current;
    if (!container) return;
    
    // Get current zoom scale
    const scale = zoomScaleRef.current || 1;
    const baseFontSize = 14;
    const basePadding = 8;
    const baseMaxWidth = 300;
    const baseBorderRadius = 4;
    const baseOffset = 10;
    
    const tooltip = d3.select(container)
      .append('div')
      .attr('class', 'comment-tooltip')
      .style('position', 'absolute')
      .style('left', `${x + baseOffset * scale}px`)
      .style('top', `${y - baseOffset * scale}px`)
      .style('background', theme === 'dark' ? 'var(--bg-secondary)' : '#fff')
      .style('border', `1px solid ${theme === 'dark' ? 'var(--border-color)' : '#ccc'}`)
      .style('border-radius', `${baseBorderRadius * scale}px`)
      .style('padding', `${basePadding * scale}px`)
      .style('box-shadow', `0 ${2 * scale}px ${4 * scale}px rgba(0,0,0,0.2)`)
      .style('max-width', `${baseMaxWidth * scale}px`)
      .style('font-size', `${baseFontSize * scale}px`)
      .style('line-height', `${1.4 * scale}`)
      .style('z-index', '10000')
      .style('color', theme === 'dark' ? 'var(--text-primary)' : '#000')
      .style('pointer-events', 'auto');
    
    tooltip.append('div')
      .attr('class', 'comment-tooltip-header')
      .style('font-weight', 'bold')
      .style('margin-bottom', `${5 * scale}px`)
      .style('padding-bottom', `${5 * scale}px`)
      .style('font-size', `${baseFontSize * scale}px`)
      .text('Comment');
    
    tooltip.append('div')
      .attr('class', 'comment-tooltip-content')
      .style('font-size', `${baseFontSize * scale}px`)
      .style('line-height', `${1.4 * scale}`)
      .text(comment);
    
    tooltip.append('button')
      .attr('class', 'comment-tooltip-close')
      .style('position', 'absolute')
      .style('top', `${5 * scale}px`)
      .style('right', `${5 * scale}px`)
      .style('background', 'none')
      .style('border', 'none')
      .style('cursor', 'pointer')
      .style('font-size', `${16 * scale}px`)
      .style('line-height', '1')
      .text('×')
      .on('click', function() {
        tooltip.remove();
      });
    
    // Close tooltip when clicking outside of it
    const handleClickOutside = function(event) {
      if (!event.target.closest('.comment-tooltip') && !event.target.closest('.comment-indicator')) {
        tooltip.remove();
        container.removeEventListener('click', handleClickOutside);
        document.removeEventListener('click', handleClickOutside);
      }
    };
    
    // Use setTimeout to avoid immediate closure from the click that opened it
    setTimeout(() => {
      container.addEventListener('click', handleClickOutside);
      document.addEventListener('click', handleClickOutside);
    }, 0);
  };

  const toggleFullscreen = () => {
    const wrapper = wrapperRef.current;
    if (!wrapper) return;

    if (!isFullscreen) {
      if (wrapper.requestFullscreen) {
        wrapper.requestFullscreen();
      } else if (wrapper.webkitRequestFullscreen) {
        wrapper.webkitRequestFullscreen();
      } else if (wrapper.msRequestFullscreen) {
        wrapper.msRequestFullscreen();
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
      const newFullscreenState = !!document.fullscreenElement || !!document.webkitFullscreenElement || !!document.msFullscreenElement;
      setIsFullscreen(newFullscreenState);
      
      // Trigger re-render to resize SVG when fullscreen state changes
      // Node expansion state is preserved because it's stored in astData object
      if (containerRef.current && astData) {
        setTimeout(() => {
          const event = new Event('ast-update');
          containerRef.current.dispatchEvent(event);
        }, 100); // Small delay to ensure DOM has updated
      }
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('msfullscreenchange', handleFullscreenChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.removeEventListener('msfullscreenchange', handleFullscreenChange);
    };
  }, [astData]);

  if (!astData) {
    return (
      <div
        className="ast-container"
        ref={containerRef}
        style={{
          height: '600px',
          border: '1px solid var(--border-color)',
          backgroundColor: theme === 'dark' ? 'var(--ast-bg)' : '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: theme === 'dark' ? 'var(--text-primary)' : '#000000',
          position: 'relative',
        }}
      >
        <button
          onClick={toggleFullscreen}
          className="btn btn-sm btn-outline-secondary"
          style={{
            position: 'absolute',
            top: '10px',
            right: '10px',
            zIndex: 10000,
            backgroundColor: theme === 'dark' ? 'var(--bg-secondary)' : '#ffffff',
            border: `1px solid ${theme === 'dark' ? 'var(--border-color)' : '#ccc'}`,
            pointerEvents: 'auto',
            boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
          }}
          title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
        >
          <i className={`bi ${isFullscreen ? 'bi-fullscreen-exit' : 'bi-fullscreen'}`}></i>
        </button>
        <p>No AST data available. Submit code to generate AST.</p>
      </div>
    );
  }

  return (
    <div
      ref={wrapperRef}
      style={{
        position: 'relative',
        height: isFullscreen ? '100vh' : '600px',
        width: isFullscreen ? '100vw' : '100%',
        border: '1px solid var(--border-color)',
        backgroundColor: theme === 'dark' ? 'var(--ast-bg)' : '#ffffff',
      }}
    >
      <div
        id="astTreeContainer"
        ref={containerRef}
        className="ast-container"
        style={{
          height: '100%',
          width: '100%',
          overflow: 'auto',
          position: 'relative',
        }}
      />
      <button
        onClick={toggleFullscreen}
        className="btn btn-sm btn-outline-secondary"
        style={{
          position: 'absolute',
          top: '10px',
          right: '10px',
          zIndex: 10000,
          backgroundColor: theme === 'dark' ? 'var(--bg-secondary)' : '#ffffff',
          border: `1px solid ${theme === 'dark' ? 'var(--border-color)' : '#ccc'}`,
          pointerEvents: 'auto',
          boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
        }}
        title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
      >
        <i className={`bi ${isFullscreen ? 'bi-fullscreen-exit' : 'bi-fullscreen'}`}></i>
      </button>
    </div>
  );
};

export default ASTVisualization;

