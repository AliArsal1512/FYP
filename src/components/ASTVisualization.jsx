import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';
import './ASTVisualization.css';

const ASTVisualization = ({ astData, theme }) => {
  const containerRef = useRef(null);
  const svgRef = useRef(null);

  useEffect(() => {
    if (!astData || !containerRef.current) {
      // Clear container if no data
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
      return;
    }

    const container = containerRef.current;
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
    treeLayout(root);

    const linkGen = d3.linkHorizontal()
      .x(d => d.y)
      .y(d => d.x);

    // Draw links with theme-aware color
    g.append('g').attr('class', 'links')
      .selectAll('path.link')
      .data(root.links())
      .join('path')
      .attr('class', 'link')
      .attr('d', linkGen)
      .attr('fill', 'none')
      .attr('stroke-width', 1.6)
      .attr('stroke', theme === 'dark' ? 'var(--ast-link-stroke)' : '#6b7280');

    // Draw nodes
    const nodeG = g.append('g').attr('class', 'nodes')
      .selectAll('g.node')
      .data(root.descendants())
      .join('g')
      .attr('class', d => `node ${d.data.type ? d.data.type : ''}`)
      .attr('transform', d => `translate(${d.y},${d.x})`)
      .style('cursor', 'pointer')
      .on('click', (event, d) => {
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
        showCommentTooltip(d.data.comment, event.pageX, event.pageY);
      });

    // Zoom/pan behavior
    const zoom = d3.zoom()
      .scaleExtent([0.1, 4])
      .on('zoom', (event) => {
        zoomLayer.attr('transform', event.transform);
      });

    svg.call(zoom);

    // Initial centering
    const allX = root.descendants().map(d => d.x);
    const allY = root.descendants().map(d => d.y);
    const minX = Math.min(...allX), maxX = Math.max(...allX);
    const minY = Math.min(...allY), maxY = Math.max(...allY);

    const treeWidth = maxY - minY + nodeHorizontalSpacing;
    const treeHeight = maxX - minX + nodeVerticalSpacing;

    const tx = margin.left + 20 - minY;
    const ty = margin.top + (height - treeHeight) / 2 - minX;

    svg.call(zoom.transform, d3.zoomIdentity.translate(tx, ty).scale(1));

    svgRef.current = svg.node();

    // Handle updates
    const handleUpdate = () => {
      if (astData) {
        // Re-render
        const event = new Event('ast-update');
        container.dispatchEvent(event);
      }
    };

    container.addEventListener('ast-update', handleUpdate);

    return () => {
      container.removeEventListener('ast-update', handleUpdate);
    };
  }, [astData, theme]);

  const showCommentTooltip = (comment, x, y) => {
    d3.selectAll('.comment-tooltip').remove();
    
    const tooltip = d3.select('body')
      .append('div')
      .attr('class', 'comment-tooltip')
      .style('position', 'absolute')
      .style('left', `${x + 10}px`)
      .style('top', `${y - 10}px`)
      .style('background', theme === 'dark' ? 'var(--bg-secondary)' : '#fff')
      .style('border', `1px solid ${theme === 'dark' ? 'var(--border-color)' : '#ccc'}`)
      .style('border-radius', '4px')
      .style('padding', '8px')
      .style('box-shadow', '0 2px 4px rgba(0,0,0,0.2)')
      .style('max-width', '300px')
      .style('z-index', '1000')
      .style('color', theme === 'dark' ? 'var(--text-primary)' : '#000');
    
    tooltip.append('div')
      .attr('class', 'comment-tooltip-header')
      .style('font-weight', 'bold')
      .style('margin-bottom', '5px')
      .text('Comment');
    
    tooltip.append('div')
      .attr('class', 'comment-tooltip-content')
      .text(comment);
    
    tooltip.append('button')
      .attr('class', 'comment-tooltip-close')
      .style('position', 'absolute')
      .style('top', '5px')
      .style('right', '5px')
      .style('background', 'none')
      .style('border', 'none')
      .style('cursor', 'pointer')
      .text('×')
      .on('click', function() {
        tooltip.remove();
      });
    
    d3.select('body').on('click.comment-tooltip', function(event) {
      if (!event.target.closest('.comment-tooltip')) {
        tooltip.remove();
        d3.select('body').on('click.comment-tooltip', null);
      }
    });
  };

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
        }}
      >
        <p>No AST data available. Submit code to generate AST.</p>
      </div>
    );
  }

  return (
    <div
      id="astTreeContainer"
      ref={containerRef}
      className="ast-container"
      style={{
        height: '600px',
        border: '1px solid var(--border-color)',
        backgroundColor: theme === 'dark' ? 'var(--ast-bg)' : '#ffffff',
        overflow: 'auto',
      }}
    />
  );
};

export default ASTVisualization;

